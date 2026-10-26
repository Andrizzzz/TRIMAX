package com.example.trimax;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The agent's customer list: customers saved on this phone plus everyone the agent has booked for on
 * the TRIMAX server (from any phone), merged by phone number (or name when there is no usable number).
 *
 * Pins: a customer saved on this phone keeps its own pin; one known only from the server takes the
 * destination pin of its most recent booking, so picking it starts the next booking at that place.
 * Booking counts are the larger of the phone's and the server's.
 */
final class CustomerDirectory {

    private CustomerDirectory() {}

    static List<DatabaseHelper.SavedCustomer> list(DatabaseHelper db, List<BookingRecord> serverBookings, String query) {
        return list(db.getSavedCustomers(""), serverBookings, null, query);
    }

    static List<DatabaseHelper.SavedCustomer> list(DatabaseHelper db, List<BookingRecord> serverBookings,
                                                   List<ServerCustomer> serverCustomers, String query) {
        return list(db.getSavedCustomers(""), serverBookings, serverCustomers, query);
    }

    /**
     * @param serverCustomers the agent's customers saved on the server (with photo paths), or null while
     *                        not loaded; they are listed even before their first booking
     */
    static List<DatabaseHelper.SavedCustomer> list(List<DatabaseHelper.SavedCustomer> local, List<BookingRecord> serverBookings,
                                                   List<ServerCustomer> serverCustomers, String query) {
        Map<String, DatabaseHelper.SavedCustomer> merged = new LinkedHashMap<>();
        Map<String, Integer> serverCounts = new LinkedHashMap<>();
        Map<String, BookingRecord> serverLatest = new LinkedHashMap<>();
        if (serverBookings != null) {
            for (BookingRecord r : serverBookings) {   // the server lists newest first
                if (r == null || r.customerName == null || r.customerName.trim().isEmpty()) continue;
                String key = key(r.customerName, r.customerContact);
                serverCounts.put(key, serverCounts.getOrDefault(key, 0) + 1);
                if (!serverLatest.containsKey(key)) serverLatest.put(key, r);
            }
        }
        for (DatabaseHelper.SavedCustomer c : local) {
            String key = key(c.getName(), c.getPhone());
            int count = Math.max(c.getBookingCount(), serverCounts.getOrDefault(key, 0));
            double lat = c.getLatitude(), lng = c.getLongitude();
            BookingRecord latest = serverLatest.get(key);
            if ((lat == 0 && lng == 0) && latest != null && hasPin(latest)) {   // phone has no pin yet: use the server's
                lat = latest.deliveryLatitude;
                lng = latest.deliveryLongitude;
            }
            merged.put(key, new DatabaseHelper.SavedCustomer(c.getId(), c.getName(), c.getPhone(), c.getEmail(),
                    c.getAddress(), lat, lng, count));
        }
        if (serverCustomers != null) {
            for (ServerCustomer sc : serverCustomers) {
                if (sc == null || sc.name == null || sc.name.trim().isEmpty()) continue;
                String key = key(sc.name, sc.contact);
                DatabaseHelper.SavedCustomer known = merged.get(key);
                boolean scPin = sc.latitude != null && sc.longitude != null && !(sc.latitude == 0 && sc.longitude == 0);
                if (known == null) {
                    known = new DatabaseHelper.SavedCustomer(-1, sc.name.trim(), sc.contact, null, sc.address,
                            scPin ? sc.latitude : 0, scPin ? sc.longitude : 0, serverCounts.getOrDefault(key, 0));
                    merged.put(key, known);
                } else if (known.getLatitude() == 0 && known.getLongitude() == 0 && scPin) {
                    DatabaseHelper.SavedCustomer withPin = new DatabaseHelper.SavedCustomer(known.getId(), known.getName(),
                            known.getPhone(), known.getEmail(),
                            isBlank(known.getAddress()) ? sc.address : known.getAddress(),
                            sc.latitude, sc.longitude, known.getBookingCount());
                    merged.put(key, withPin);
                    known = withPin;
                }
                known.setOnServer(true);
                known.setPhotoPath(sc.photoPath);
            }
        }
        for (Map.Entry<String, BookingRecord> e : serverLatest.entrySet()) {
            if (merged.containsKey(e.getKey())) continue;
            BookingRecord r = e.getValue();
            String address = r.deliveryAddress != null && !r.deliveryAddress.trim().isEmpty()
                    ? r.deliveryAddress : r.customerAddress;
            merged.put(e.getKey(), new DatabaseHelper.SavedCustomer(-1, r.customerName.trim(), r.customerContact, null,
                    address, hasPin(r) ? r.deliveryLatitude : 0, hasPin(r) ? r.deliveryLongitude : 0,
                    serverCounts.getOrDefault(e.getKey(), 1)));
        }

        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String qDigits = q.replaceAll("[^0-9]", "");
        List<DatabaseHelper.SavedCustomer> out = new ArrayList<>();
        for (DatabaseHelper.SavedCustomer c : merged.values()) {
            if (q.isEmpty()
                    || contains(c.getName(), q)
                    || contains(c.getAddress(), q)
                    || (!qDigits.isEmpty() && digits(c.getPhone()).contains(qDigits))) {
                out.add(c);
            }
        }
        return out;
    }

    /** Same person = same last 10 phone digits (0917… and +63917… match); no usable number = same name. */
    static String key(String name, String phone) {
        String d = digits(phone);
        if (d.length() >= 7) return "p:" + (d.length() > 10 ? d.substring(d.length() - 10) : d);
        return "n:" + (name == null ? "" : name.trim().toLowerCase(Locale.ROOT));
    }

    static String digits(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** True when the server list has this customer (same phone key); false also when it is not loaded. */
    static boolean onServer(List<ServerCustomer> serverCustomers, String name, String phone) {
        if (serverCustomers == null) return false;
        String key = key(name, phone);
        for (ServerCustomer sc : serverCustomers) {
            if (sc != null && key.equals(key(sc.name, sc.contact))) return true;
        }
        return false;
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private static boolean hasPin(BookingRecord r) {
        return r.deliveryLatitude != null && r.deliveryLongitude != null
                && !(r.deliveryLatitude == 0 && r.deliveryLongitude == 0);
    }
}
