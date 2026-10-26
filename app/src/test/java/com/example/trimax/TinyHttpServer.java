package com.example.trimax;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Test-only HTTP server on java.net.ServerSocket (available in Android unit tests, unlike the JDK's
 * com.sun.net.httpserver). One request per connection; replies come from {@link #reply}.
 */
final class TinyHttpServer {

    static final class Request {
        String method, path, apikey, authorization, body;
    }

    interface Replier {
        /** Returns {status, json body}. */
        String[] reply(Request request);
    }

    final List<Request> requests = Collections.synchronizedList(new ArrayList<>());
    volatile Replier reply = r -> new String[]{"200", "{}"};
    private final ServerSocket server;

    TinyHttpServer() throws IOException {
        server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
        Thread t = new Thread(() -> {
            while (!server.isClosed()) {
                try (Socket socket = server.accept()) {
                    answer(socket);
                } catch (IOException closed) {
                    // stopped
                }
            }
        }, "tiny-http");
        t.setDaemon(true);
        t.start();
    }

    String url() {
        return "http://127.0.0.1:" + server.getLocalPort();
    }

    Request last() {
        return requests.isEmpty() ? null : requests.get(requests.size() - 1);
    }

    void stop() throws IOException {
        server.close();
    }

    private static String line(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        for (int c; (c = in.read()) != -1 && c != '\n'; ) {
            if (c != '\r') buf.write(c);
        }
        return buf.toString("ISO-8859-1");
    }

    private void answer(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        String[] first = line(in).split(" ");
        Request r = new Request();
        r.method = first[0];
        r.path = first.length > 1 ? first[1] : "";
        int length = 0;
        for (String h = line(in); !h.isEmpty(); h = line(in)) {
            int colon = h.indexOf(':');
            if (colon < 0) continue;
            String name = h.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = h.substring(colon + 1).trim();
            if (name.equals("apikey")) r.apikey = value;
            if (name.equals("authorization")) r.authorization = value;
            if (name.equals("content-length")) length = Integer.parseInt(value);
        }
        byte[] data = new byte[length];
        for (int read = 0; read < length; ) {
            int n = in.read(data, read, length - read);
            if (n < 0) break;
            read += n;
        }
        r.body = new String(data, StandardCharsets.UTF_8);
        requests.add(r);
        String[] answer = reply.reply(r);
        byte[] out = answer[1].getBytes(StandardCharsets.UTF_8);
        OutputStream o = socket.getOutputStream();
        o.write(("HTTP/1.1 " + answer[0] + " X\r\nContent-Type: application/json\r\nContent-Length: " + out.length
                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.ISO_8859_1));
        o.write(out);
        o.flush();
    }
}
