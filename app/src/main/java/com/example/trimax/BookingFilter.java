package com.example.trimax;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Filters the agent's bookings (already loaded from the server) by status chip and search text. */
public final class BookingFilter {

    private BookingFilter() {
    }

    /**
     * @param chip  ALL, PENDING, CONFIRMED, CANCELLED or COMPLETED (the Bookings screen chips).
     *              CONFIRMED also includes In Progress: confirmed bookings that are out for delivery.
     * @param query matches booking number, customer name, contact or delivery address; blank = all
     */
    public static List<BookingRecord> apply(List<BookingRecord> all, String chip, String query) {
        List<BookingRecord> out = new ArrayList<>();
        if (all == null) return out;
        String c = chip == null ? "ALL" : chip.trim().toUpperCase(Locale.US);
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        // "#12" means booking number 12 exactly; otherwise search all the visible fields.
        boolean byNumber = q.startsWith("#");
        if (byNumber) q = q.substring(1).trim();
        for (BookingRecord b : all) {
            if (!matchesChip(b.status, c)) continue;
            if (!q.isEmpty() && !(byNumber ? b.idText().equals(q) : matchesQuery(b, q))) continue;
            out.add(b);
        }
        return out;
    }

    static boolean matchesChip(String status, String chip) {
        switch (chip) {
            case "PENDING":   return DeliveryProgress.PENDING.equals(status);
            case "CONFIRMED": return DeliveryProgress.CONFIRMED.equals(status) || DeliveryProgress.IN_PROGRESS.equals(status);
            case "CANCELLED": return DeliveryProgress.CANCELLED.equals(status);
            case "COMPLETED": return DeliveryProgress.COMPLETED.equals(status);
            default:          return true;
        }
    }

    private static boolean matchesQuery(BookingRecord b, String q) {
        return contains(b.idText(), q) || contains(b.customerName, q) || contains(b.customerContact, q)
                || contains(b.deliveryAddress, q);
    }

    private static boolean contains(String field, String q) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(q);
    }

    /**
     * Bookings for one saved customer: same contact number (compared on the last 10 digits, so
     * 0917..., +63 917... and 917... match), or the same name when the booking has no number.
     */
    public static List<BookingRecord> forCustomer(List<BookingRecord> all, String phone, String name) {
        List<BookingRecord> out = new ArrayList<>();
        if (all == null) return out;
        String key = contactKey(phone);
        String n = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        for (BookingRecord b : all) {
            String bk = contactKey(b.customerContact);
            boolean samePhone = key != null && key.equals(bk);
            boolean sameName = bk == null && !n.isEmpty() && b.customerName != null
                    && n.equals(b.customerName.trim().toLowerCase(Locale.ROOT));
            if (samePhone || sameName) out.add(b);
        }
        return out;
    }

    /** Same rule as trimax_private.contact_key on the server. */
    static String contactKey(String phone) {
        if (phone == null) return null;
        String d = phone.replaceAll("[^0-9]", "");
        if (d.length() >= 10) return d.substring(d.length() - 10);
        return d.length() >= 7 ? d : null;
    }

    /** Count of bookings with the given chip (for the dashboard tiles). */
    public static int count(List<BookingRecord> all, String chip) {
        return apply(all, chip, null).size();
    }
}
