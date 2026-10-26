package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** "Set up or reset password" against a tiny local HTTP server ({@link TinyHttpServer}). */
public class SetPasswordWithCodeTest {

    private static final String KEY = "sb_publishable_test";

    private static final class MemoryStore implements AuthSession.Store {
        AuthSession s;

        @Override public AuthSession load() { return s; }
        @Override public void save(AuthSession session) { s = session; }
        @Override public void clear() { s = null; }
    }

    private final MemoryStore store = new MemoryStore();
    private TinyHttpServer server;
    private SupabaseClient client;
    private volatile String path, method, apikey, authorization, body;
    private volatile int replyStatus = 200;
    private volatile String replyBody = "{\"ok\":true,\"message\":\"Password set. You can sign in now.\"}";

    @Before
    public void start() throws Exception {
        server = new TinyHttpServer();
        server.reply = r -> {
            path = r.path;
            method = r.method;
            apikey = r.apikey;
            authorization = r.authorization;
            body = r.body;
            return new String[]{String.valueOf(replyStatus), replyBody};
        };
        client = new SupabaseClient(server.url(), KEY, store, () -> 1_000_000L);
    }

    @After
    public void stop() throws Exception {
        server.stop();
    }

    @Test
    public void sendsTheCodeToTheAccountsFunctionWithoutAToken() throws Exception {
        client.setPasswordWithCode("tim@example.test", "ABCD2345", "Sunflower7");
        assertEquals("POST", method);
        assertEquals("/functions/v1/trimax-accounts", path);
        assertEquals(KEY, apikey);
        assertNull("no Bearer token: the code is the authorization", authorization);
        JSONObject sent = new JSONObject(body);
        assertEquals("set_password", sent.getString("action"));
        assertEquals("tim@example.test", sent.getString("email"));
        assertEquals("ABCD2345", sent.getString("code"));
        assertEquals("Sunflower7", sent.getString("password"));
        assertNull("no session is created or stored", store.load());
    }

    @Test
    public void aRefusedCodeShowsTheServersWords() {
        replyStatus = 400;
        replyBody = "{\"error\":\"The email or code is not right, or the code has expired. Ask your administrator for a new code.\"}";
        try {
            client.setPasswordWithCode("tim@example.test", "WRONG234", "Sunflower7");
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.INVALID, e.getKind());
            assertTrue(e.getMessage().startsWith("The email or code is not right"));
        }
    }

    @Test
    public void serverTroubleIsNotBlamedOnTheUser() {
        replyStatus = 502;
        replyBody = "{\"error\":\"The server could not check the code. Try again.\"}";
        try {
            client.setPasswordWithCode("tim@example.test", "ABCD2345", "Sunflower7");
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.SERVER, e.getKind());
            assertTrue(e.getMessage().contains("Try again"));
        }
        replyStatus = 404;
        replyBody = "{}";
        try {
            client.setPasswordWithCode("tim@example.test", "ABCD2345", "Sunflower7");
            fail();
        } catch (ApiException e) {
            assertTrue(e.getMessage().contains("not available yet"));
        }
    }
}
