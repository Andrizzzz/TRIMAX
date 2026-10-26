package com.example.trimax;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

/** Downloading a customer photo from the private customer-photos bucket, as the signed-in agent. */
public class CustomerPhotoDownloadTest {

    private static final String KEY = "sb_publishable_test";
    private static final String UID = "11111111-2222-4333-8444-555555555555";
    private static final String PATH = UID + "/customer_1761490000000_ab12cd34.jpg";

    private static final class MemoryStore implements AuthSession.Store {
        AuthSession s;

        @Override public AuthSession load() { return s; }
        @Override public void save(AuthSession session) { s = session; }
        @Override public void clear() { s = null; }
    }

    private TinyHttpServer server;
    private SupabaseClient client;
    private volatile String[] photoReply = {"200", "JPEGDATA"};

    @Before
    public void start() throws Exception {
        server = new TinyHttpServer();
        server.reply = r -> r.path.startsWith("/auth/v1/token")
                ? new String[]{"200", "{\"access_token\":\"a.b.c\",\"refresh_token\":\"r\",\"expires_in\":3600,"
                        + "\"user\":{\"id\":\"" + UID + "\",\"email\":\"agent@example.test\"}}"}
                : photoReply;
        client = new SupabaseClient(server.url(), KEY, new MemoryStore(), () -> 1_000_000L);
        client.signIn("agent@example.test", "Sunflower7");
    }

    @After
    public void stop() throws Exception {
        server.stop();
    }

    @Test
    public void downloadsTheAgentsOwnPhotoWithTheSignIn() throws Exception {
        byte[] data = client.downloadJpeg("customer-photos", PATH);
        assertArrayEquals("JPEGDATA".getBytes(StandardCharsets.UTF_8), data);
        TinyHttpServer.Request r = server.last();
        assertEquals("GET", r.method);
        assertEquals("/storage/v1/object/authenticated/customer-photos/" + PATH, r.path);
        assertEquals("Bearer a.b.c", r.authorization);
        assertEquals(KEY, r.apikey);
        assertEquals(UID, client.currentUserId());
    }

    @Test
    public void aMissingPhotoIsNotFound() {
        photoReply = new String[]{"404", "{\"statusCode\":\"404\",\"error\":\"not_found\",\"message\":\"Object not found\"}"};
        try {
            client.downloadJpeg("customer-photos", PATH);
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.NOT_FOUND, e.getKind());
        }
    }

    @Test
    public void anotherAgentsPhotoIsRefused() {
        photoReply = new String[]{"403", "{\"statusCode\":\"403\",\"error\":\"Unauthorized\",\"message\":\"new row violates\"}"};
        try {
            client.downloadJpeg("customer-photos", PATH);
            fail();
        } catch (ApiException e) {
            assertEquals(ApiException.Kind.FORBIDDEN, e.getKind());
            assertTrue(e.getMessage().length() > 0);
        }
    }

    @Test
    public void badPathsNeverReachTheServer() {
        int before = server.requests.size();
        for (String bad : new String[]{"../x.jpg", "a/../../b.jpg", "", null}) {
            try {
                client.downloadJpeg("customer-photos", bad);
                fail("accepted " + bad);
            } catch (IllegalArgumentException expected) {
                // refused on the phone
            } catch (ApiException e) {
                fail("sent " + bad);
            }
        }
        assertEquals(before, server.requests.size());
    }
}
