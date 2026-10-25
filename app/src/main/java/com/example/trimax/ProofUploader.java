package com.example.trimax;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Sends drop-off photos to the office after a delivery is marked DELIVERED. Completing a delivery
 * never waits for the photo: each photo is queued on the phone (file path, booking, recipient, where
 * and when) and sent in the background, again whenever a driver screen opens, until the server has
 * it. The storage name and request id are fixed when the photo is queued, so a retry never stores it
 * twice. The photo file and the phone's own proof record are never deleted by this class.
 */
final class ProofUploader {

    private static final String PREFS = "trimax_proof_outbox";
    private static final String KEY = "pending";
    private static final int MAX_QUEUED = 100;
    private static final int MAX_ATTEMPTS = 40;
    private static final Set<String> IN_FLIGHT = new HashSet<>();

    private ProofUploader() {}

    /** Queues one drop-off photo for the booking and starts sending it. */
    static void queue(Context context, int bookingId, File photo, String recipientName, Double latitude, Double longitude) {
        if (bookingId <= 0 || photo == null) return;
        Context app = context.getApplicationContext();
        JSONObject o = new JSONObject();
        try {
            o.put("id", UUID.randomUUID().toString());
            o.put("user", nz(new SessionManager(app).getUsername()));
            o.put("booking", bookingId);
            o.put("file", photo.getAbsolutePath());
            o.put("name", objectName(bookingId, System.currentTimeMillis(), UUID.randomUUID().toString()));
            o.put("recipient", nz(recipientName));
            if (latitude != null && longitude != null) {
                o.put("lat", latitude.doubleValue());
                o.put("lng", longitude.doubleValue());
            }
            o.put("taken", iso(photo.lastModified() > 0 ? photo.lastModified() : System.currentTimeMillis()));
            o.put("tries", 0);
        } catch (Exception e) {
            return;
        }
        add(app, o);
        flush(app);
    }

    /** Sends the signed-in driver's queued photos. Quiet; call it whenever a driver screen opens. */
    static void flush(Context context) {
        Context app = context.getApplicationContext();
        if (!TrimaxApi.get(app).hasSession()) return;
        String username = nz(new SessionManager(app).getUsername());
        JSONArray queue = load(app);
        for (int i = 0; i < queue.length(); i++) {
            JSONObject o = queue.optJSONObject(i);
            if (o != null && !username.isEmpty() && username.equals(o.optString("user"))) send(app, o);
        }
    }

    /** Photos still waiting to reach the office for this driver. */
    static int pendingCount(Context context) {
        String username = nz(new SessionManager(context).getUsername());
        JSONArray queue = load(context.getApplicationContext());
        int n = 0;
        for (int i = 0; i < queue.length(); i++) {
            JSONObject o = queue.optJSONObject(i);
            if (o != null && username.equals(o.optString("user"))) n++;
        }
        return n;
    }

    private static void send(Context app, JSONObject o) {
        String id = o.optString("id");
        synchronized (IN_FLIGHT) {
            if (!IN_FLIGHT.add(id)) return;
        }
        Double lat = o.has("lat") && o.has("lng") ? o.optDouble("lat") : null;
        Double lng = lat != null ? o.optDouble("lng") : null;
        TrimaxApi.get(app).attachDeliveryProof(o.optInt("booking"), new File(o.optString("file")), o.optString("name"),
                o.optString("recipient", null), lat, lng, o.optString("taken", null), id,
                new TrimaxApi.Callback<Long>() {
                    @Override
                    public void onSuccess(Long proofId) {
                        done(id);
                        remove(app, id);
                    }

                    @Override
                    public void onError(ApiException error) {
                        done(id);
                        if (error.getKind() == ApiException.Kind.INVALID && error.getMessage() != null
                                && error.getMessage().contains("not been uploaded")) {
                            // Uploaded long ago but never attached (the server takes uploads from the last
                            // 24 hours): send it again under a new name.
                            if (bumpTries(app, id) < MAX_ATTEMPTS) rename(app, id);
                            else remove(app, id);
                            return;
                        }
                        switch (whatNext(error)) {
                            case RETRY:
                                return;
                            case RETRY_COUNTED:
                                if (bumpTries(app, id) < MAX_ATTEMPTS) return;
                                remove(app, id);
                                return;
                            default:
                                remove(app, id);   // refused for good: not this driver's, 5 photos already, file gone
                        }
                    }
                });
    }

    enum Next { RETRY, RETRY_COUNTED, DROP }

    /**
     * Offline, server busy or not set up yet, signed out: always try again later.
     * Storage refusing the upload: try again a limited number of times. Anything else: stop.
     */
    static Next whatNext(ApiException error) {
        if (error == null || error.requiresSignIn()) return Next.RETRY;
        switch (error.getKind()) {
            case NETWORK:
            case TIMEOUT:
            case SERVER:
                return Next.RETRY;
            case FORBIDDEN:
                return Next.RETRY_COUNTED;
            default:
                return Next.DROP;
        }
    }

    /** proof_12_1729850000000_ab12cd34.jpg: matches the server's file-name rule [A-Za-z0-9_.-]{1,120}.jpg */
    static String objectName(int bookingId, long millis, String uuid) {
        String tail = uuid == null ? "x" : uuid.replaceAll("[^A-Za-z0-9]", "");
        if (tail.length() > 8) tail = tail.substring(0, 8);
        return "proof_" + bookingId + "_" + millis + "_" + tail + ".jpg";
    }

    static String iso(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(millis));
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static void done(String id) {
        synchronized (IN_FLIGHT) {
            IN_FLIGHT.remove(id);
        }
    }

    // ------------------------------------------------------------------ outbox

    private static synchronized JSONArray load(Context app) {
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            return new JSONArray(prefs.getString(KEY, "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static synchronized void save(Context app, JSONArray queue) {
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, queue.toString()).apply();
    }

    private static synchronized void add(Context app, JSONObject o) {
        JSONArray queue = load(app);
        if (queue.length() >= MAX_QUEUED) return;
        queue.put(o);
        save(app, queue);
    }

    private static synchronized void remove(Context app, String id) {
        JSONArray queue = load(app), kept = new JSONArray();
        for (int i = 0; i < queue.length(); i++) {
            JSONObject o = queue.optJSONObject(i);
            if (o != null && !id.equals(o.optString("id"))) kept.put(o);
        }
        if (kept.length() != queue.length()) save(app, kept);
    }

    private static synchronized void rename(Context app, String id) {
        JSONArray queue = load(app);
        for (int i = 0; i < queue.length(); i++) {
            JSONObject o = queue.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) {
                try {
                    o.put("name", objectName(o.optInt("booking"), System.currentTimeMillis(), UUID.randomUUID().toString()));
                } catch (Exception ignored) {
                    // plain value
                }
            }
        }
        save(app, queue);
    }

    /** Counts one more failed try and returns the new count. */
    private static synchronized int bumpTries(Context app, String id) {
        JSONArray queue = load(app);
        int tries = 0;
        for (int i = 0; i < queue.length(); i++) {
            JSONObject o = queue.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) {
                tries = o.optInt("tries") + 1;
                try {
                    o.put("tries", tries);
                } catch (Exception ignored) {
                    // plain value
                }
            }
        }
        save(app, queue);
        return tries;
    }
}
