package com.example.trimax;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns changes on the TRIMAX server (admin confirms or cancels a booking, assigns a driver, the
 * driver delivers) into notifications on this phone, so the Notifications screens show what the
 * admin and the other side actually did.
 *
 * The phone remembers the last state it saw for each booking. The very first sync only records
 * that baseline, so a new install does not get a flood of old news.
 */
final class BookingNotifications {

    private static final String PREFS = "trimax_booking_states";
    private static final Pattern NUMERIC_ID = Pattern.compile("(?:booking|delivery)\\s*#?\\s*(\\d{1,9})\\b|#(\\d{1,9})\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEGACY_ID = Pattern.compile("BK-[A-Za-z0-9-]+", Pattern.CASE_INSENSITIVE);

    private BookingNotifications() {}

    /** Agent: their own bookings, as the server has them now. */
    static void syncAgent(Context context, String username, List<BookingRecord> records) {
        sync(context, username, records, false);
    }

    /** Driver: deliveries assigned to them (active and/or history), as the server has them now. */
    static void syncDriver(Context context, String username, List<BookingRecord> records) {
        sync(context, username, records, true);
    }

    /**
     * The booking a notification is about: the server's number ("Booking #12") when there is one,
     * otherwise an old phone-only code (BK-2026-00125), otherwise null.
     */
    static String bookingIdFrom(String text) {
        if (text == null) return null;
        Matcher m = NUMERIC_ID.matcher(text);
        if (m.find()) return m.group(1) != null ? m.group(1) : m.group(2);
        Matcher legacy = LEGACY_ID.matcher(text);
        return legacy.find() ? legacy.group() : null;
    }

    private static void sync(Context context, String username, List<BookingRecord> records, boolean driver) {
        if (context == null || username == null || username.isEmpty() || records == null) return;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String prefix = (driver ? "d:" : "a:") + username + ":";
        String baselineKey = prefix + "baseline";
        boolean firstSync = !prefs.getBoolean(baselineKey, false);

        DatabaseHelper db = new DatabaseHelper(context);
        SharedPreferences.Editor edit = prefs.edit();
        for (BookingRecord r : records) {
            if (r == null || r.bookingId <= 0) continue;
            String key = prefix + r.bookingId;
            String now = state(r);
            String before = prefs.getString(key, null);
            if (now.equals(before)) continue;
            edit.putString(key, now);
            if (firstSync) continue;

            String[] message = driver ? driverMessage(r, before) : agentMessage(r, before);
            if (message != null) db.addNotification(username, message[0], message[1]);
        }
        edit.putBoolean(baselineKey, true);
        edit.apply();
    }

    private static String state(BookingRecord r) {
        return nz(r.status) + "|" + nz(r.deliveryStatus) + "|" + nz(r.driverUsername);
    }

    private static String[] agentMessage(BookingRecord r, String before) {
        if (before == null) return null; // a booking this agent just made
        String[] old = before.split("\\|", -1);
        String oldStatus = old.length > 0 ? old[0] : "";
        String oldDelivery = old.length > 1 ? old[1] : "";
        String oldDriver = old.length > 2 ? old[2] : "";
        String id = "Booking #" + r.bookingId;
        String who = r.customerName != null && !r.customerName.trim().isEmpty() ? " for " + r.customerName.trim() : "";

        if (!nz(r.status).equals(oldStatus)) {
            switch (nz(r.status)) {
                case "Confirmed":   return msg(id + " confirmed", "The admin confirmed " + id + who + ".");
                case "In Progress": return msg(id + " in progress", id + who + " is being prepared for delivery.");
                case "Completed":   return msg(id + " completed", id + who + " has been completed.");
                case "Cancelled":   return msg(id + " cancelled", "The admin cancelled " + id + who + ".");
                default: break;
            }
        }
        if (!nz(r.driverUsername).equals(oldDriver) && !nz(r.driverUsername).isEmpty()) {
            String driverName = r.driverName != null && !r.driverName.trim().isEmpty() ? r.driverName.trim() : r.driverUsername;
            return msg("Driver assigned to " + id, driverName + " will deliver " + id + who + ".");
        }
        if (!nz(r.deliveryStatus).equals(oldDelivery)) {
            switch (nz(r.deliveryStatus)) {
                case "STARTED":   return msg(id + " on the way", "The driver started the delivery of " + id + who + ".");
                case "DELIVERED": return msg(id + " delivered", id + who + " was delivered.");
                case "FAILED":    return msg(id + " not delivered", "The driver could not deliver " + id + who + ".");
                default: break;
            }
        }
        return null;
    }

    private static String[] driverMessage(BookingRecord r, String before) {
        String id = "Booking #" + r.bookingId;
        String who = r.customerName != null && !r.customerName.trim().isEmpty() ? " for " + r.customerName.trim() : "";
        String where = r.deliveryAddress != null && !r.deliveryAddress.trim().isEmpty() ? " to " + r.deliveryAddress.trim() : "";
        if (before == null) {
            // First time this phone sees it: only news if it is still to be delivered.
            boolean finished = "Completed".equals(r.status) || "Cancelled".equals(r.status)
                    || "DELIVERED".equals(r.deliveryStatus) || "FAILED".equals(r.deliveryStatus);
            return finished ? null : msg("New delivery: " + id, "The admin assigned you " + id + who + where + ".");
        }
        String oldStatus = before.split("\\|", -1)[0];
        if (!nz(r.status).equals(oldStatus)) {
            if ("Cancelled".equals(r.status)) return msg(id + " cancelled", "The admin cancelled " + id + who + ". Do not deliver it.");
            if ("Confirmed".equals(r.status)) return msg(id + " confirmed", "The admin confirmed " + id + who + ". You can start it.");
        }
        return null;
    }

    private static String[] msg(String title, String body) {
        return new String[]{title, body};
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
