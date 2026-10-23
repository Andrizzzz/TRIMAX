package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** SupabaseClient against a local stand-in for Supabase's /auth/v1 and /rest/v1 endpoints. */
public class SupabaseClientTest {

    static final String KEY = "sb_publishable_test";

    /** In-memory session store (the app uses the Keystore-encrypted AuthSessionStore). */
    static final class MemoryStore implements AuthSession.Store {
        AuthSession s;

        @Override public AuthSession load() { return s; }
        @Override public void save(AuthSession session) { s = session; }
        @Override public void clear() { s = null; }
    }

    static final class Seen {
        final String method, path, apikey, authorization, body;

        Seen(String method, String path, String apikey, String authorization, String body) {
            this.method = method;
            this.path = path;
            this.apikey = apikey;
            this.authorization = authorization;
            this.body = body;
        }
    }

    private HttpServer server;
    private String base;
    private final List<Seen> requests = Collections.synchronizedList(new ArrayList<>());
    private final MemoryStore store = new MemoryStore();
    private long now = 1_000_000L;
    private final AtomicInteger issued = new AtomicInteger();
    /** Access tokens the fake REST endpoint accepts. */
    private final List<String> validTokens = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean refreshBroken;

    @Before
    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void stop() {
        server.stop(0);
    }

    private void handle(HttpExchange ex) throws java.io.IOException {
        String body = read(ex.getRequestBody());
        String path = ex.getRequestURI().toString();
        requests.add(new Seen(ex.getRequestMethod(), path, ex.getRequestHeaders().getFirst("apikey"),
                ex.getRequestHeaders().getFirst("Authorization"), body));
        try {
            if (path.equals("/auth/v1/token?grant_type=password")) {
                JSONObject in = new JSONObject(body);
                if (!"secret-pw".equals(in.getString("password"))) {
                    reply(ex, 400, "{\"code\":\"invalid_credentials\",\"msg\":\"Invalid login credentials\"}");
                } else {
                    reply(ex, 200, session(in.getString("email")));
                }
            } else if (path.equals("/auth/v1/token?grant_type=refresh_token")) {
                if (refreshBroken) reply(ex, 400, "{\"code\":\"refresh_token_not_found\",\"msg\":\"Invalid Refresh Token\"}");
                else reply(ex, 200, session("ana@example.test"));
            } else if (path.equals("/auth/v1/logout")) {
                reply(ex, 204, "");
            } else if (path.startsWith("/rest/v1/rpc/")) {
                String auth = ex.getRequestHeaders().getFirst("Authorization");
                String token = auth == null ? "" : auth.replace("Bearer ", "");
                if (!validTokens.contains(token)) {
                    reply(ex, 401, "{\"code\":\"PGRST301\",\"message\":\"JWT expired\"}");
                } else if (path.endsWith("/trimax_fail")) {
                    reply(ex, 400, "{\"code\":\"TM409\",\"details\":\"STARTED\",\"hint\":null,\"message\":\"Changed meanwhile.\"}");
                } else {
                    reply(ex, 200, "{\"echo\":" + body + "}");
                }
            } else {
                reply(ex, 404, "{}");
            }
        } catch (Exception e) {
            reply(ex, 500, "{}");
        }
    }

    private String session(String email) throws Exception {
        String token = "access-" + issued.incrementAndGet();
        validTokens.add(token);
        JSONObject s = new JSONObject();
        s.put("access_token", token);
        s.put("refresh_token", "refresh-" + issued.get());
        s.put("expires_in", 3600);
        s.put("expires_at", now + 3600);
        JSONObject user = new JSONObject();
        user.put("id", "00000000-0000-0000-0000-00000000a0a0");
        user.put("email", email);
        s.put("user", user);
        return s.toString();
    }

    private static void reply(HttpExchange ex, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, status == 204 ? -1 : bytes.length);
        if (status != 204) {
            try (OutputStream out = ex.getResponseBody()) {
                out.write(bytes);
            }
        }
        ex.close();
    }

    private static String read(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) != -1) b.write(chunk, 0, n);
        return new String(b.toByteArray(), StandardCharsets.UTF_8);
    }

    private SupabaseClient client() {
        return new SupabaseClient(base + "/", KEY, store, () -> now);
    }

    // ------------------------------------------------------------------------------------------

    @Test
    public void signInStoresTheSessionAndCallsUseItsToken() throws Exception {
        SupabaseClient c = client();
        AuthSession s = c.signIn("ana@example.test", "secret-pw");
        assertEquals("access-1", s.accessToken);
        assertEquals(now + 3600, s.expiresAt);
        assertTrue(c.hasSession());

        JSONObject args = new JSONObject();
        args.put("p_scope", "active");
        String body = c.rpc("trimax_driver_list_deliveries", args);
        assertEquals("active", new JSONObject(body).getJSONObject("echo").getString("p_scope"));

        Seen rpc = requests.get(requests.size() - 1);
        assertEquals("POST", rpc.method);
        assertEquals("/rest/v1/rpc/trimax_driver_list_deliveries", rpc.path);
        assertEquals(KEY, rpc.apikey);
        assertEquals("Bearer access-1", rpc.authorization);
    }

    @Test
    public void wrongPasswordIsReportedAndNothingIsStored() {
        try {
            client().signIn("ana@example.test", "wrong");
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.BAD_CREDENTIALS, e.getKind());
        }
        assertNull(store.load());
    }

    @Test
    public void tokenAboutToExpireIsRefreshedFirst() throws Exception {
        SupabaseClient c = client();
        c.signIn("ana@example.test", "secret-pw");
        now += 3600 - 30;                       // 30 s left: refresh before calling
        c.rpc("trimax_my_account", new JSONObject());
        assertEquals("access-2", store.load().accessToken);
        Seen refresh = requests.get(requests.size() - 2);
        assertEquals("/auth/v1/token?grant_type=refresh_token", refresh.path);
        assertEquals("refresh-1", new JSONObject(refresh.body).getString("refresh_token"));
        assertEquals("Bearer access-2", requests.get(requests.size() - 1).authorization);
    }

    @Test
    public void rejectedTokenIsRefreshedOnceAndTheCallRetried() throws Exception {
        SupabaseClient c = client();
        c.signIn("ana@example.test", "secret-pw");
        validTokens.clear();                    // the server no longer accepts access-1
        String body = c.rpc("trimax_my_account", new JSONObject());
        assertNotNull(body);
        assertEquals(3, requests.size() - 1);   // 401, refresh, retry (after the sign-in)
        assertEquals("Bearer access-2", requests.get(requests.size() - 1).authorization);
    }

    @Test
    public void deadRefreshTokenEndsTheSession() throws Exception {
        SupabaseClient c = client();
        c.signIn("ana@example.test", "secret-pw");
        validTokens.clear();
        refreshBroken = true;
        try {
            c.rpc("trimax_my_account", new JSONObject());
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.SESSION_EXPIRED, e.getKind());
            assertTrue(e.requiresSignIn());
        }
        assertFalse("tokens are removed from the phone", c.hasSession());
    }

    @Test
    public void serverRuleErrorsComeBackTyped() throws Exception {
        SupabaseClient c = client();
        c.signIn("ana@example.test", "secret-pw");
        try {
            c.rpc("trimax_fail", new JSONObject());
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.CONFLICT, e.getKind());
            assertEquals("STARTED", e.getDetail());
            assertEquals("Changed meanwhile.", e.getMessage());
        }
        assertTrue("a rule error does not end the session", c.hasSession());
    }

    @Test
    public void noSessionMeansSignIn() {
        try {
            client().rpc("trimax_my_account", new JSONObject());
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.SESSION_EXPIRED, e.getKind());
        }
    }

    @Test
    public void signOutForgetsTokensAndTellsTheServer() throws Exception {
        SupabaseClient c = client();
        c.signIn("ana@example.test", "secret-pw");
        c.signOut();
        assertFalse(c.hasSession());
        Seen logout = requests.get(requests.size() - 1);
        assertEquals("/auth/v1/logout", logout.path);
        assertEquals("Bearer access-1", logout.authorization);
    }

    @Test
    public void unreachableServerIsANetworkError() throws Exception {
        store.save(new AuthSession("a", "r", now + 3600, "u", "e"));
        server.stop(0);
        try {
            client().rpc("trimax_my_account", new JSONObject());
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.NETWORK, e.getKind());
        }
        assertTrue("going offline does not sign the user out", store.load() != null);
    }

    @Test
    public void refusesUnsafeConfiguration() {
        assertRejected("http://example.com", KEY);
        assertRejected(base, "sb_secret_abc");
        assertRejected(base, "eyJhbGciOi.service_role.xyz");
        assertRejected(base, " ");
    }

    private void assertRejected(String url, String key) {
        try {
            new SupabaseClient(url, key, store, () -> now);
            fail(url + " / " + key);
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
