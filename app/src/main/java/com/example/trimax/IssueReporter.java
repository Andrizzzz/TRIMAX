package com.example.trimax;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * SOS and problem reports from drivers reach the admin (Desktop > Notifications, as an Alert):
 * each report is kept on the phone (as before), sent to the server, and — when the phone is offline
 * or the sign-in has expired — queued with its request id and sent again later. The request id
 * makes the retry safe: the server never creates a second alert for the same report.
 */
final class IssueReporter {

    private static final String PREFS = "trimax_issue_outbox";
    private static final String KEY = "pending";
    private static final int MAX_QUEUED = 50;
    private static final Set<String> IN_FLIGHT = new HashSet<>();

    private IssueReporter() {}

    /**
     * Saves the report on this phone, then sends it to the admin.
     *
     * @param bookingText the booking it is about ("12", "#12", "Booking #12"), or null for a general report
     * @param type        short label put in front of the admin's message, e.g. "Vehicle breakdown"
     */
    static void send(Activity activity, boolean sos, String bookingText, String type, String description,
                     LocationHelper location) {
        String username = new SessionManager(activity).getUsername();
        boolean fix = location != null && location.hasValidLocationFix();
        double lat = fix ? location.getLatitude() : 0, lng = fix ? location.getLongitude() : 0;

        // 1. The phone's own log, as before.
        new DatabaseHelper(activity).saveProblemReport(
                bookingText != null ? bookingText : (sos ? "EMERGENCY_SOS" : "GENERAL"),
                username, sos ? "EMERGENCY_SOS" : type, description, lat, lng);

        // 2. The admin, through the server.
        Pending p = new Pending(UUID.randomUUID().toString(), username, sos,
                message(type, description, sos), bookingNumber(bookingText), fix ? lat : null, fix ? lng : null);
        submit(activity.getApplicationContext(), p, activity);
    }

    /** Sends the signed-in person's queued reports. Quiet: call it whenever a driver screen opens. */
    static void flush(Context context) {
        Context app = context.getApplicationContext();
        if (!TrimaxApi.get(app).hasSession()) return;
        String username = new SessionManager(app).getUsername();
        JSONArray queue = load(app);
        for (int i = 0; i < queue.length(); i++) {
            Pending p = Pending.from(queue.optJSONObject(i));
            if (p != null && p.username != null && p.username.equals(username)) submit(app, p, null);
        }
    }

    /** Number of reports still waiting to be sent for this person (for a "pending" hint). */
    static int pendingCount(Context context) {
        String username = new SessionManager(context).getUsername();
        JSONArray queue = load(context.getApplicationContext());
        int n = 0;
        for (int i = 0; i < queue.length(); i++) {
            JSONObject o = queue.optJSONObject(i);
            if (o != null && username != null && username.equals(o.optString("user"))) n++;
        }
        return n;
    }

    private static void submit(Context app, Pending p, Activity ui) {
        synchronized (IN_FLIGHT) {
            if (!IN_FLIGHT.add(p.requestId)) return;   // already on its way
        }
        TrimaxApi.get(app).reportIssue(p.sos, p.message, p.bookingId, p.lat, p.lng, p.requestId,
                new TrimaxApi.Callback<Long>() {
                    @Override
                    public void onSuccess(Long notificationId) {
                        done();
                        remove(app, p.requestId);
                        if (ui != null && !SessionGuard.isGone(ui)) {
                            Toast.makeText(ui, p.sos ? "SOS sent. The office has been alerted."
                                    : "Report sent to the admin.", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(ApiException error) {
                        done();
                        ApiException.Kind kind = error.getKind();
                        if (kind == ApiException.Kind.NOT_FOUND && p.bookingId != null) {
                            // The booking is no longer this driver's (reassigned or cancelled): still tell the admin.
                            remove(app, p.requestId);
                            submit(app, new Pending(UUID.randomUUID().toString(), p.username, p.sos,
                                    clip("Booking #" + p.bookingId + " - " + p.message), null, p.lat, p.lng), ui);
                            return;
                        }
                        boolean retryLater = kind == ApiException.Kind.NETWORK || kind == ApiException.Kind.TIMEOUT
                                || kind == ApiException.Kind.SERVER || error.requiresSignIn();
                        if (retryLater) add(app, p); else remove(app, p.requestId);

                        if (ui == null || SessionGuard.isGone(ui)) return;
                        if (SessionGuard.handle(ui, error)) return;
                        String text;
                        if (retryLater) {
                            text = p.sos ? "No connection: SOS saved and will be sent automatically. CALL THE OFFICE NOW."
                                    : "No connection: report saved and will be sent automatically.";
                        } else {
                            text = (p.sos ? "SOS not sent: " : "Report not sent: ") + error.getMessage();
                        }
                        Toast.makeText(ui, text, Toast.LENGTH_LONG).show();
                    }

                    private void done() {
                        synchronized (IN_FLIGHT) {
                            IN_FLIGHT.remove(p.requestId);
                        }
                    }
                });
    }

    /** "Vehicle breakdown" + "flat tyre" -> "Vehicle breakdown: flat tyre", trimmed to what the server accepts. */
    static String message(String type, String description, boolean sos) {
        String t = type == null ? "" : type.trim();
        String d = description == null ? "" : description.trim();
        String m = t.isEmpty() || d.toLowerCase(java.util.Locale.ROOT).contains(t.toLowerCase(java.util.Locale.ROOT))
                ? (d.isEmpty() ? t : d)          // "Reported: Customer not home" already names the type
                : (d.isEmpty() ? t : t + ": " + d);
        if (m.length() < 3) m = sos ? "Emergency SOS from the mobile app." : "Problem reported from the mobile app.";
        return clip(m);
    }

    private static String clip(String m) {
        return m.length() > 1000 ? m.substring(0, 1000) : m;
    }

    /** "12", "#12", "Booking #12" -> 12; old phone-only codes (BK-...) and blanks -> null. */
    static Integer bookingNumber(String text) {
        if (text == null) return null;
        String t = text.trim();
        String id = t.matches("#?\\d{1,9}") ? t.replace("#", "") : BookingNotifications.bookingIdFrom(t);
        if (id == null || !id.matches("\\d{1,9}")) return null;
        int n = Integer.parseInt(id);
        return n > 0 ? n : null;
    }

    // ------------------------------------------------------------------ outbox

    private static final class Pending {
        final String requestId;
        final String username;
        final boolean sos;
        final String message;
        final Integer bookingId;
        final Double lat, lng;

        Pending(String requestId, String username, boolean sos, String message, Integer bookingId,
                Double lat, Double lng) {
            this.requestId = requestId;
            this.username = username;
            this.sos = sos;
            this.message = message;
            this.bookingId = bookingId;
            this.lat = lat;
            this.lng = lng;
        }

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", requestId);
                o.put("user", username == null ? "" : username);
                o.put("sos", sos);
                o.put("message", message);
                if (bookingId != null) o.put("booking", bookingId.intValue());
                if (lat != null && lng != null) {
                    o.put("lat", lat.doubleValue());
                    o.put("lng", lng.doubleValue());
                }
            } catch (Exception ignored) {
                // plain values only
            }
            return o;
        }

        static Pending from(JSONObject o) {
            if (o == null || o.optString("id", "").isEmpty()) return null;
            Integer booking = o.has("booking") ? Integer.valueOf(o.optInt("booking")) : null;
            Double lat = o.has("lat") && o.has("lng") ? Double.valueOf(o.optDouble("lat")) : null;
            Double lng = lat != null ? Double.valueOf(o.optDouble("lng")) : null;
            return new Pending(o.optString("id"), o.optString("user", null), o.optBoolean("sos"),
                    o.optString("message"), booking, lat, lng);
        }
    }

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

    private static synchronized void add(Context app, Pending p) {
        JSONArray queue = load(app);
        for (int i = 0; i < queue.length(); i++) {
            JSONObject o = queue.optJSONObject(i);
            if (o != null && p.requestId.equals(o.optString("id"))) return;   // already queued
        }
        if (queue.length() >= MAX_QUEUED) return;
        queue.put(p.toJson());
        save(app, queue);
    }

    private static synchronized void remove(Context app, String requestId) {
        JSONArray queue = load(app), kept = new JSONArray();
        for (int i = 0; i < queue.length(); i++) {
            JSONObject o = queue.optJSONObject(i);
            if (o != null && !requestId.equals(o.optString("id"))) kept.put(o);
        }
        if (kept.length() != queue.length()) save(app, kept);
    }
}
