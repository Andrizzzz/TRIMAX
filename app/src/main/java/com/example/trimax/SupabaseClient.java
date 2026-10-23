package com.example.trimax;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal Supabase Auth/PostgREST client for TRIMAX.
 * The publishable key is intended for client-side use; RLS remains mandatory.
 * Never put a service_role or sb_secret key in this class.
 */
public final class SupabaseClient {
    private static final String TAG = "TrimaxSupabase";
    private static final String PROJECT_URL = "https://dfxjblrletoeukoarxlz.supabase.co";
    private static final String PUBLISHABLE_KEY = "sb_publishable_FwMnGl1O1qmsIfoscL2BPw_JJkf31UQ";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private SupabaseClient() {}

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(String message);
    }

    public static final class AuthResult {
        public final String accessToken;
        public final String refreshToken;
        public final String authUserId;
        public final String email;

        AuthResult(String accessToken, String refreshToken, String authUserId, String email) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.authUserId = authUserId;
            this.email = email;
        }
    }

    public static final class Profile {
        public final String authUserId;
        public final String username;
        public final String displayName;
        public final String role;
        public final String legacyUsername;

        Profile(String authUserId, String username, String displayName, String role, String legacyUsername) {
            this.authUserId = authUserId;
            this.username = username;
            this.displayName = displayName;
            this.role = role;
            this.legacyUsername = legacyUsername;
        }
    }

    public static void signIn(String email, String password, Callback<AuthResult> callback) {
        EXECUTOR.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                JSONObject result = requestJson(
                        "POST", "/auth/v1/token?grant_type=password", body, null, null);
                JSONObject user = result.getJSONObject("user");
                AuthResult auth = new AuthResult(
                        result.getString("access_token"),
                        result.optString("refresh_token", ""),
                        user.getString("id"),
                        user.optString("email", email));
                callback.onSuccess(auth);
            } catch (Exception e) {
                Log.e(TAG, "Supabase sign-in failed", e);
                callback.onError(safeMessage(e));
            }
        });
    }

    public static void loadMyProfile(String accessToken, String authUserId, Callback<Profile> callback) {
        EXECUTOR.execute(() -> {
            try {
                String path = "/rest/v1/profiles?id=eq." + authUserId
                        + "&select=id,username,display_name,role,legacy_username,is_active";
                JSONArray rows = requestArray("GET", path, null, accessToken);
                if (rows.length() == 0) {
                    callback.onError("Walang TRIMAX profile para sa account na ito. Ipa-configure muna sa admin.");
                    return;
                }
                JSONObject row = rows.getJSONObject(0);
                if (!row.optBoolean("is_active", true)) {
                    callback.onError("Deactivated ang account na ito. Makipag-ugnayan sa admin.");
                    return;
                }
                String role = row.optString("role", "").toUpperCase();
                if (!role.equals("ADMIN") && !role.equals("AGENT") && !role.equals("DRIVER")) {
                    callback.onError("Invalid TRIMAX role. Ipa-check sa admin.");
                    return;
                }
                callback.onSuccess(new Profile(
                        row.getString("id"),
                        row.optString("username", ""),
                        row.optString("display_name", ""),
                        role,
                        row.optString("legacy_username", "")));
            } catch (Exception e) {
                Log.e(TAG, "Supabase profile load failed", e);
                callback.onError(safeMessage(e));
            }
        });
    }

    private static JSONObject requestJson(String method, String path, JSONObject body,
                                          String accessToken, String prefer) throws Exception {
        String response = request(method, path, body, accessToken, prefer);
        return new JSONObject(response);
    }

    private static JSONArray requestArray(String method, String path, JSONObject body,
                                          String accessToken) throws Exception {
        return new JSONArray(request(method, path, body, accessToken, null));
    }

    private static String request(String method, String path, JSONObject body,
                                  String accessToken, String prefer) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(PROJECT_URL + path);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("apikey", PUBLISHABLE_KEY);
            connection.setRequestProperty("Accept", "application/json");
            if (accessToken != null && !accessToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            } else {
                connection.setRequestProperty("Authorization", "Bearer " + PUBLISHABLE_KEY);
            }
            if (prefer != null) connection.setRequestProperty("Prefer", prefer);
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            StringBuilder response = new StringBuilder();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }
            }
            if (status < 200 || status >= 300) {
                String detail = response.toString();
                try {
                    JSONObject error = new JSONObject(detail);
                    detail = error.optString("msg",
                            error.optString("message", error.optString("error_description",
                                    error.optString("error", "HTTP " + status))));
                } catch (Exception ignored) {
                    detail = "HTTP " + status;
                }
                throw new Exception(detail);
            }
            return response.toString();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) return "Hindi makakonekta sa Supabase. Subukan ulit.";
        if (message.toLowerCase().contains("invalid login credentials")) return "Mali ang email o password.";
        if (message.toLowerCase().contains("email not confirmed")) return "Hindi pa verified ang email account.";
        if (message.toLowerCase().contains("failed to connect") || message.toLowerCase().contains("timed out"))
            return "Hindi maabot ang Supabase. Tingnan ang internet connection.";
        return message;
    }
}
