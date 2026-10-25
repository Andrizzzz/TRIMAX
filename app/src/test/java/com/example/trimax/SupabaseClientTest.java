package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * SupabaseClient against a local MockWebServer that imitates Supabase's /auth/v1 and /rest/v1 endpoints.
 * Nothing here talks to the real Supabase project.
 */
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

    private MockWebServer server;
    private boolean serverStopped;
    private String base;
    private final List<Seen> requests = Collections.synchronizedList(new ArrayList<>());
    private final MemoryStore store = new MemoryStore();
    private long now = 1_000_000L;
    private final AtomicInteger issued = new AtomicInteger();
    /** Access tokens the fake REST endpoint accepts. */
    private final List<String> validTokens = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean refreshBroken;
    /** Storage: object paths already stored, and whether the bucket exists. */
    private final java.util.Set<String> storedObjects = Collections.synchronizedSet(new java.util.HashSet<>());
    private volatile boolean bucketMissing;

    @Before
    public void start() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return handle(request);
            }
        });
        // Bind to the loopback address and build the URL from it. (MockWebServer.url() uses the canonical
        // host name, which some Windows setups resolve to a name other than localhost.)
        server.start(InetAddress.getByName("127.0.0.1"), 0);
        base = "http://127.0.0.1:" + server.getPort();
    }

    @After
    public void stop() throws Exception {
        if (!serverStopped) server.shutdown();
    }

    /** Stands in for Supabase: records every request, then answers like GoTrue / PostgREST would. */
    private MockResponse handle(RecordedRequest request) {
        String body = request.getBody().readUtf8();
        String path = request.getPath();
        String auth = request.getHeader("Authorization");
        requests.add(new Seen(request.getMethod(), path, request.getHeader("apikey"), auth, body));
        try {
            if ("/auth/v1/token?grant_type=password".equals(path)) {
                JSONObject in = new JSONObject(body);
                if (!"secret-pw".equals(in.getString("password"))) {
                    return reply(400, "{\"code\":\"invalid_credentials\",\"msg\":\"Invalid login credentials\"}");
                }
                return reply(200, session(in.getString("email")));
            } else if ("/auth/v1/token?grant_type=refresh_token".equals(path)) {
                if (refreshBroken) return reply(400, "{\"code\":\"refresh_token_not_found\",\"msg\":\"Invalid Refresh Token\"}");
                return reply(200, session("ana@example.test"));
            } else if ("/auth/v1/logout".equals(path)) {
                return reply(204, null);
            } else if (path != null && path.startsWith("/rest/v1/rpc/")) {
                String token = auth == null ? "" : auth.replace("Bearer ", "");
                if (!validTokens.contains(token)) {
                    return reply(401, "{\"code\":\"PGRST301\",\"message\":\"JWT expired\"}");
                } else if (path.endsWith("/trimax_fail")) {
                    return reply(400, "{\"code\":\"TM409\",\"details\":\"STARTED\",\"hint\":null,\"message\":\"Changed meanwhile.\"}");
                }
                return reply(200, "{\"echo\":" + body + "}");
            } else if (path != null && path.startsWith("/storage/v1/object/")) {
                // Supabase Storage answers 400 with the real status inside the JSON body.
                if (bucketMissing) {
                    return reply(400, "{\"statusCode\":\"404\",\"error\":\"Bucket not found\",\"message\":\"Bucket not found\"}");
                }
                if (!storedObjects.add(path)) {
                    return reply(400, "{\"statusCode\":\"409\",\"error\":\"Duplicate\",\"message\":\"The resource already exists\"}");
                }
                return reply(200, "{\"Key\":\"" + path + "\"}");
            }
            return reply(404, "{}");
        } catch (Exception e) {
            return reply(500, "{}");
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

    /** A JSON response; a null body means no content (used for 204). */
    private static MockResponse reply(int status, String body) {
        MockResponse response = new MockResponse().setResponseCode(status);
        if (body != null) {
            response.setHeader("Content-Type", "application/json");
            response.setBody(body);
        }
        return response;
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
        server.shutdown();                      // nothing listens on that port any more
        serverStopped = true;
        try {
            client().rpc("trimax_my_account", new JSONObject());
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.NETWORK, e.getKind());
        }
        assertTrue("going offline does not sign the user out", store.load() != null);
    }

    @Test
    public void retryOfAnAlreadyStoredPhotoIsNotAnError() throws Exception {
        SupabaseClient c = client();
        c.signIn("ana@example.test", "secret-pw");
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, 1, (byte) 0xFF, (byte) 0xD9};
        c.uploadJpeg("delivery-proofs", "00000000-0000-0000-0000-00000000a0a0/proof_12_1_abcd1234.jpg", jpeg);
        c.uploadJpeg("delivery-proofs", "00000000-0000-0000-0000-00000000a0a0/proof_12_1_abcd1234.jpg", jpeg);
        assertEquals(1, storedObjects.size());
        Seen upload = requests.get(requests.size() - 1);
        assertEquals("/storage/v1/object/delivery-proofs/00000000-0000-0000-0000-00000000a0a0/proof_12_1_abcd1234.jpg", upload.path);
        assertEquals("Bearer access-1", upload.authorization);
    }

    @Test
    public void missingBucketMeansTheServerIsNotSetUpYet() throws Exception {
        SupabaseClient c = client();
        c.signIn("ana@example.test", "secret-pw");
        bucketMissing = true;
        try {
            c.uploadJpeg("delivery-proofs", "00000000-0000-0000-0000-00000000a0a0/p.jpg", new byte[]{1, 2, 3});
            fail();
        } catch (ApiException e) {
            assertEquals("kept for a later retry, not a permission problem", ApiException.Kind.SERVER, e.getKind());
        }
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
