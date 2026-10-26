package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Sign-in with a temporary password from the administrator, then the forced password change. */
public class FirstSignInTest {

    private static final String KEY = "sb_publishable_test";

    private static final class MemoryStore implements AuthSession.Store {
        AuthSession s;

        @Override public AuthSession load() { return s; }
        @Override public void save(AuthSession session) { s = session; }
        @Override public void clear() { s = null; }
    }

    private TinyHttpServer server;
    private SupabaseClient client;
    private volatile boolean mustChange;

    private static String session(boolean mustChange, boolean withMeta) {
        return "{\"access_token\":\"a.b.c\",\"refresh_token\":\"r\",\"expires_in\":3600,\"user\":{\"id\":"
                + "\"11111111-2222-4333-8444-555555555555\",\"email\":\"kenjie@example.test\""
                + (withMeta ? ",\"user_metadata\":{\"must_change_password\":" + mustChange + "}" : "") + "}}";
    }

    @Before
    public void start() throws Exception {
        server = new TinyHttpServer();
        server.reply = r -> r.path.startsWith("/auth/v1/token") ? new String[]{"200", session(mustChange, true)}
                : new String[]{"200", "{\"id\":\"11111111-2222-4333-8444-555555555555\"}"};
        client = new SupabaseClient(server.url(), KEY, new MemoryStore(), () -> 1_000_000L);
    }

    @After
    public void stop() throws Exception {
        server.stop();
    }

    @Test
    public void aTemporaryPasswordAsksForANewOne() throws Exception {
        mustChange = true;
        client.signIn("kenjie@example.test", "Kenjie-7Q4M");
        assertTrue(client.lastSignInMustChangePassword());
        mustChange = false;
        client.signIn("kenjie@example.test", "Sunflower7");
        assertFalse(client.lastSignInMustChangePassword());
    }

    @Test
    public void accountsWithoutTheFlagAreNotAsked() {
        assertFalse(SupabaseClient.mustChangePassword(session(false, false)));
        assertFalse(SupabaseClient.mustChangePassword("not json"));
        assertTrue(SupabaseClient.mustChangePassword(session(true, true)));
    }

    @Test
    public void choosingANewPasswordClearsTheFlag() throws Exception {
        mustChange = true;
        client.signIn("kenjie@example.test", "Kenjie-7Q4M");
        client.updatePassword("Sunflower7");
        TinyHttpServer.Request put = server.last();
        assertEquals("PUT", put.method);
        assertEquals("/auth/v1/user", put.path);
        assertEquals("Bearer a.b.c", put.authorization);
        JSONObject body = new JSONObject(put.body);
        assertEquals("Sunflower7", body.getString("password"));
        assertFalse(body.getJSONObject("data").getBoolean("must_change_password"));
    }
}
