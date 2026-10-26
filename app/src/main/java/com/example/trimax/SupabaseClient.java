package com.example.trimax;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Minimal, blocking Supabase client for TRIMAX: Auth password sign-in, token refresh, sign-out, and calls
 * to the TRIMAX database functions (/rest/v1/rpc/...). Call it from a background thread (TrimaxApi does).
 *
 * Security notes
 * - Only the publishable (anon) key is used here. It identifies the project; every function call is
 *   authorised by the signed-in user's access token and checked again inside the database.
 * - Tokens and response bodies are never logged.
 * - HTTPS only, except http://localhost / 127.0.0.1 for local tests.
 */
public final class SupabaseClient {

    /** Seconds since the epoch; replaceable in tests. */
    public interface Clock {
        long nowSeconds();
    }

    static final int CONNECT_TIMEOUT_MS = 15_000;
    static final int READ_TIMEOUT_MS = 20_000;
    /** Refresh the access token when it has less than this many seconds left. */
    static final long REFRESH_MARGIN_SECONDS = 60;
    /** Edge Function for employee sign-ins (set the password with an emailed code). */
    static final String ACCOUNTS_FUNCTION = "/functions/v1/trimax-accounts";
    private static final Pattern FUNCTION_NAME = Pattern.compile("[a-z_][a-z0-9_]{0,62}");
    private static final Pattern BUCKET_NAME = Pattern.compile("[a-z0-9][a-z0-9_-]{0,62}");
    private static final Pattern OBJECT_PATH = Pattern.compile("[A-Za-z0-9-]{1,64}/[A-Za-z0-9_.-]{1,120}\\.jpg");

    private final String baseUrl;
    private final String apiKey;
    private final AuthSession.Store store;
    private final Clock clock;
    private final Object refreshLock = new Object();

    public SupabaseClient(String baseUrl, String apiKey, AuthSession.Store store, Clock clock) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        boolean local = url.startsWith("http://localhost:") || url.startsWith("http://127.0.0.1:");
        if (!url.startsWith("https://") && !local) {
            throw new IllegalArgumentException("Supabase URL must use https://");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing Supabase publishable key");
        }
        if (apiKey.startsWith("sb_secret_") || apiKey.contains("service_role")) {
            throw new IllegalArgumentException("Never put a secret/service_role key in the app");
        }
        this.baseUrl = url;
        this.apiKey = apiKey.trim();
        this.store = store;
        this.clock = clock != null ? clock : () -> System.currentTimeMillis() / 1000L;
    }

    // =====================================================================================
    // Auth
    // =====================================================================================

    /** Password sign-in. On success the session is stored and returned. */
    public AuthSession signIn(String email, String password) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (Exception e) {
            throw new ApiException(ApiException.Kind.INVALID, "Enter your email and password.");
        }
        Response r = send("/auth/v1/token?grant_type=password", body.toString(), null);
        if (!r.ok()) throw ApiException.fromResponse(r.status, r.body, true);
        AuthSession session = parseSession(r.body, email);
        store.save(session);
        return session;
    }

    public boolean hasSession() {
        return store.load() != null;
    }

    /** Ends the session on this phone and, best effort, on the server. Never throws. Blocks on the network. */
    public void signOut() {
        revokeOnServer(clearLocalSession());
    }

    /** Forgets the tokens on this phone right away. Returns what was stored (for revokeOnServer), or null. */
    public AuthSession clearLocalSession() {
        AuthSession s = store.load();
        store.clear();
        return s;
    }

    /** Best-effort server sign-out of a session already removed from the phone. Never throws. */
    public void revokeOnServer(AuthSession s) {
        if (s == null) return;
        try {
            send("/auth/v1/logout", "{}", s.accessToken);
        } catch (ApiException ignored) {
            // Offline: the tokens are already gone from this phone and the access token expires on its own.
        }
    }

    /**
     * Returns a session whose access token is good for at least a minute, refreshing it if needed.
     * Several threads may ask at once; only one refresh is sent.
     */
    AuthSession validSession(AuthSession seen, boolean force) throws ApiException {
        synchronized (refreshLock) {
            AuthSession current = store.load();
            if (current == null) throw ApiException.sessionExpired();
            boolean someoneElseRefreshed = seen != null && !current.accessToken.equals(seen.accessToken);
            if (!current.expiresWithin(clock.nowSeconds(), REFRESH_MARGIN_SECONDS) && (!force || someoneElseRefreshed)) {
                return current;
            }
            JSONObject body = new JSONObject();
            try {
                body.put("refresh_token", current.refreshToken);
            } catch (Exception e) {
                throw ApiException.sessionExpired();
            }
            Response r = send("/auth/v1/token?grant_type=refresh_token", body.toString(), null);
            if (!r.ok()) {
                ApiException e = ApiException.fromResponse(r.status, r.body, true);
                if (r.status >= 400 && r.status < 500) {
                    store.clear();              // the refresh token is no longer valid: sign in again
                    throw ApiException.sessionExpired();
                }
                throw e;                        // server trouble: keep the session, the user can retry
            }
            AuthSession fresh = parseSession(r.body, current.email);
            store.save(fresh);
            return fresh;
        }
    }

    /** Sets a new password for the signed-in user (call right after signIn with the current password). */
    public void updatePassword(String newPassword) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("password", newPassword);
        } catch (Exception e) {
            throw new ApiException(ApiException.Kind.INVALID, "Enter a new password.");
        }
        AuthSession session = validSession(null, false);
        Response r = send("PUT", "/auth/v1/user", body.toString(), session.accessToken);
        if (r.ok()) return;
        if (r.status == 401) throw ApiException.sessionExpired();
        String message = null;
        try {
            JSONObject err = new JSONObject(r.body);
            message = Json.text(err, "msg");
            if (message == null) message = Json.text(err, "message");
            if (message == null) message = Json.text(err, "error_description");
        } catch (Exception ignored) {
            // generic message below
        }
        if (r.status >= 400 && r.status < 500) {
            throw new ApiException(ApiException.Kind.INVALID,
                    message != null ? message : "The new password was not accepted. Try a longer one.");
        }
        throw new ApiException(ApiException.Kind.SERVER, "The server had a problem (" + r.status + "). Please try again.");
    }

    /**
     * First-time setup or reset: sets this person's password with the one-time code the TRIMAX desktop
     * emailed. Needs no session. The Edge Function "trimax-accounts" checks the code (single use, a few
     * tries, expiry) and sets the password in Supabase Auth; TRIMAX never stores it.
     */
    public void setPasswordWithCode(String email, String code, String newPassword) throws ApiException {
        JSONObject body = new JSONObject();
        try {
            body.put("action", "set_password");
            body.put("email", email);
            body.put("code", code);
            body.put("password", newPassword);
        } catch (Exception e) {
            throw new ApiException(ApiException.Kind.INVALID, "Enter your email, the code and a new password.");
        }
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        // The code is the authorization here; no access token, and the key is not sent as a Bearer token.
        Response r = send("POST", ACCOUNTS_FUNCTION, bytes, "application/json; charset=utf-8", null, false);
        if (r.ok()) return;
        String message = null;
        try {
            message = Json.text(new JSONObject(r.body), "error");
        } catch (Exception ignored) {
            // generic message below
        }
        if (r.status == 400) {
            throw new ApiException(ApiException.Kind.INVALID, message != null ? message : "The code was not accepted.");
        }
        if (r.status == 404) {
            throw new ApiException(ApiException.Kind.SERVER, "Password setup is not available yet. Ask your administrator.");
        }
        if (r.status == 429) {
            throw new ApiException(ApiException.Kind.SERVER, "Too many attempts. Wait a minute and try again.");
        }
        throw new ApiException(ApiException.Kind.SERVER,
                message != null ? message : "The server had a problem (" + r.status + "). Please try again.");
    }

    // =====================================================================================
    // Database functions
    // =====================================================================================

    /** Calls public.<function>(args) as the signed-in user and returns the JSON response body. */
    public String rpc(String function, JSONObject args) throws ApiException {
        if (function == null || !FUNCTION_NAME.matcher(function).matches()) {
            throw new IllegalArgumentException("Bad function name");
        }
        String path = "/rest/v1/rpc/" + function;
        String body = args == null ? "{}" : args.toString();

        AuthSession session = validSession(null, false);
        Response r = send(path, body, session.accessToken);
        if (r.status == 401) {
            // Rejected before running (token expired early or revoked): refresh once and retry.
            session = validSession(session, true);
            r = send(path, body, session.accessToken);
        }
        if (!r.ok()) throw ApiException.fromResponse(r.status, r.body, false);
        return r.body;
    }

    // =====================================================================================
    // Storage
    // =====================================================================================

    /** The signed-in user's Supabase id, or null when nobody is signed in on this phone. */
    public String currentUserId() {
        AuthSession s = store.load();
        return s == null ? null : s.userId;
    }

    /**
     * Uploads a JPEG to Storage as the signed-in user (the bucket's policies decide which folders
     * that user may write to). Never overwrites an existing object.
     */
    public void uploadJpeg(String bucket, String objectPath, byte[] data) throws ApiException {
        if (bucket == null || !BUCKET_NAME.matcher(bucket).matches()
                || objectPath == null || !OBJECT_PATH.matcher(objectPath).matches()) {
            throw new IllegalArgumentException("Bad storage path");
        }
        if (data == null || data.length == 0) {
            throw new ApiException(ApiException.Kind.INVALID, "The photo is empty. Take it again.");
        }
        String path = "/storage/v1/object/" + bucket + "/" + objectPath;
        AuthSession session = validSession(null, false);
        Response r = send("POST", path, data, "image/jpeg", session.accessToken);
        if (r.status == 401 || (r.status == 400 && r.body != null && r.body.toLowerCase(java.util.Locale.ROOT).contains("jwt"))) {
            session = validSession(session, true);
            r = send("POST", path, data, "image/jpeg", session.accessToken);
        }
        if (r.ok()) return;
        String body = r.body == null ? "" : r.body.toLowerCase(java.util.Locale.ROOT);
        if (r.status == 409 || body.contains("already exists") || body.contains("duplicate")) {
            return;   // a retry of an upload that reached the server: the photo (unique name) is already there
        }
        if (r.status == 413 || body.contains("payload too large") || body.contains("exceeded the maximum")) {
            throw new ApiException(ApiException.Kind.INVALID, "The photo is too large to upload.");
        }
        if (body.contains("bucket not found")) {
            throw new ApiException(ApiException.Kind.SERVER,
                    "The server is not set up for these photos yet. They stay on this phone and are sent later.");
        }
        if (r.status >= 400 && r.status < 500) {
            throw new ApiException(ApiException.Kind.FORBIDDEN,
                    "The photo could not be uploaded (" + r.status + "). Ask your administrator to check mobile access.");
        }
        throw new ApiException(ApiException.Kind.SERVER, "The server had a problem (" + r.status + "). Please try again.");
    }

    // =====================================================================================
    // HTTP
    // =====================================================================================

    static final class Response {
        final int status;
        final String body;

        Response(int status, String body) {
            this.status = status;
            this.body = body;
        }

        boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    private Response send(String path, String jsonBody, String accessToken) throws ApiException {
        return send("POST", path, jsonBody, accessToken);
    }

    private Response send(String method, String path, String jsonBody, String accessToken) throws ApiException {
        return send(method, path, jsonBody.getBytes(StandardCharsets.UTF_8), "application/json; charset=utf-8", accessToken);
    }

    private Response send(String method, String path, byte[] bytes, String contentType, String accessToken) throws ApiException {
        return send(method, path, bytes, contentType, accessToken, true);
    }

    private Response send(String method, String path, byte[] bytes, String contentType, String accessToken,
                          boolean bearer) throws ApiException {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(baseUrl + path).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
            c.setReadTimeout(READ_TIMEOUT_MS);
            c.setUseCaches(false);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("apikey", apiKey);
            if (bearer || accessToken != null) {
                c.setRequestProperty("Authorization", "Bearer " + (accessToken != null ? accessToken : apiKey));
            }
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Content-Type", contentType);
            c.setDoOutput(true);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) {
                out.write(bytes);
            }
            int status = c.getResponseCode();
            InputStream in = status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
            return new Response(status, readAll(in));
        } catch (SocketTimeoutException e) {
            throw ApiException.timeout();
        } catch (IOException e) {
            throw ApiException.network();
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        try (InputStream input = in) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
                if (buffer.size() > 5_000_000) throw new IOException("Response too large");
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private AuthSession parseSession(String body, String fallbackEmail) throws ApiException {
        try {
            JSONObject json = new JSONObject(body);
            String access = Json.text(json, "access_token");
            String refresh = Json.text(json, "refresh_token");
            JSONObject user = Json.object(json, "user");
            String userId = Json.text(user, "id");
            if (access == null || refresh == null || userId == null) throw new IllegalStateException("incomplete");
            long now = clock.nowSeconds();
            Double expiresAt = Json.number(json, "expires_at");
            Double expiresIn = Json.number(json, "expires_in");
            long exp = expiresAt != null ? expiresAt.longValue() : now + (expiresIn != null ? expiresIn.longValue() : 3600L);
            String email = Json.text(user, "email");
            return new AuthSession(access, refresh, exp, userId, email != null ? email : fallbackEmail);
        } catch (Exception e) {
            throw new ApiException(ApiException.Kind.SERVER, "Unexpected sign-in response from the server.");
        }
    }
}
