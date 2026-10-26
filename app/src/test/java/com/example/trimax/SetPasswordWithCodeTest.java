package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * "Set up or reset password" against a tiny local HTTP server built on java.net.ServerSocket (available in
 * Android unit tests; the JDK's com.sun.net.httpserver is not).
 */
public class SetPasswordWithCodeTest {

    private ServerSocket server;
    private SupabaseClient client;
    private static final String KEY = "sb_publishable_test";

    private static final class MemoryStore implements AuthSession.Store {
        AuthSession s;

        @Override public AuthSession load() { return s; }
        @Override public void save(AuthSession session) { s = session; }
        @Override public void clear() { s = null; }
    }

    private final MemoryStore store = new MemoryStore();
    private volatile String path, method, apikey, authorization, body;
    private volatile int replyStatus = 200;
    private volatile String replyBody = "{\"ok\":true,\"message\":\"Password set. You can sign in now.\"}";

    @Before
    public void start() throws Exception {
        server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
        Thread t = new Thread(() -> {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    answer(socket);
                } catch (IOException closed) {
                    // the test is over
                }
            }
        }, "fake-supabase");
        t.setDaemon(true);
        t.start();
        client = new SupabaseClient("http://127.0.0.1:" + server.getLocalPort(), KEY, store, () -> 1_000_000L);
    }

    @After
    public void stop() throws IOException {
        server.close();
    }

    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        for (int c; (c = in.read()) != -1 && c != '\n'; ) {
            if (c != '\r') buf.write(c);
        }
        return buf.toString("ISO-8859-1");
    }

    /** Reads one request, remembers it, sends the prepared reply and closes the connection. */
    private void answer(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        String[] request = line(in).split(" ");
        String key = null, auth = null;
        int length = 0;
        for (String h = line(in); !h.isEmpty(); h = line(in)) {
            int colon = h.indexOf(':');
            if (colon < 0) continue;
            String name = h.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = h.substring(colon + 1).trim();
            if (name.equals("apikey")) key = value;
            if (name.equals("authorization")) auth = value;
            if (name.equals("content-length")) length = Integer.parseInt(value);
        }
        byte[] data = new byte[length];
        for (int read = 0; read < length; ) {
            int n = in.read(data, read, length - read);
            if (n < 0) break;
            read += n;
        }
        method = request[0];
        path = request.length > 1 ? request[1] : "";
        apikey = key;
        authorization = auth;
        body = new String(data, StandardCharsets.UTF_8);
        byte[] out = replyBody.getBytes(StandardCharsets.UTF_8);
        OutputStream o = socket.getOutputStream();
        o.write(("HTTP/1.1 " + replyStatus + " X\r\nContent-Type: application/json\r\nContent-Length: " + out.length
                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        o.write(out);
        o.flush();
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
