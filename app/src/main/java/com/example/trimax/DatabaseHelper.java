package com.example.trimax;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trimax.db";
    private static final int DATABASE_VERSION = 9; // Fixed TRIMAX pickup hub and destination validation

    // Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_DELIVERIES = "deliveries"; // Acts as Bookings table
    public static final String TABLE_LOCATION_LOGS = "location_logs";
    public static final String TABLE_DELIVERY_PROOF = "delivery_proof";
    public static final String TABLE_ORDER_ADJUSTMENTS = "order_adjustments";
    public static final String TABLE_STATUS_HISTORY = "delivery_status_history";
    public static final String TABLE_PROBLEM_REPORTS = "driver_problem_reports";
    public static final String TABLE_NOTIFICATIONS = "notifications";
    public static final String TABLE_ATTENDANCE = "attendance_records";
    public static final String TABLE_CUSTOMERS = "customers";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // -------------------------------------------------------------
    // SECURE PASSWORD HASHING (PBKDF2WithHmacSHA256)
    // -------------------------------------------------------------
    public static String hashPassword(String password) {
        if (password == null) return "";
        try {
            int iterations = 65536;
            char[] chars = password.toCharArray();
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return iterations + ":" + toHex(salt) + ":" + toHex(hash);
        } catch (Exception e) {
            return password; // Fallback
        }
    }

    public static boolean verifyPassword(String originalPassword, String storedPassword) {
        if (storedPassword == null) return false;
        if (!storedPassword.contains(":")) {
            // Legacy plaintext password fallback
            return originalPassword.equals(storedPassword);
        }
        try {
            String[] parts = storedPassword.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = fromHex(parts[1]);
            byte[] hash = fromHex(parts[2]);

            PBEKeySpec spec = new PBEKeySpec(originalPassword.toCharArray(), salt, iterations, hash.length * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] testHash = skf.generateSecret(spec).getEncoded();

            int diff = hash.length ^ testHash.length;
            for (int i = 0; i < hash.length && i < testHash.length; i++) {
                diff |= hash[i] ^ testHash[i];
            }
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String toHex(byte[] array) {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        }
        return hex;
    }

    private static byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Users Table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT UNIQUE, "
                + "password TEXT, "
                + "role TEXT, "
                + "full_name TEXT, "
                + "email TEXT, "
                + "phone TEXT)";
        db.execSQL(createUsersTable);

        // 2. Deliveries / Bookings Table
        String createDeliveriesTable = "CREATE TABLE " + TABLE_DELIVERIES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "booking_id TEXT UNIQUE, "
                + "driver_id TEXT, "
                + "customer_id TEXT, "
                + "customer_name TEXT, "
                + "customer_phone TEXT, "
                + "pickup_location TEXT, "
                + "pickup_latitude REAL, "
                + "pickup_longitude REAL, "
                + "destination TEXT, "
                + "destination_latitude REAL, "
                + "destination_longitude REAL, "
                + "special_instructions TEXT, "
                + "package_details TEXT, "
                + "original_amount REAL, "
                + "current_total REAL, "
                + "deposit_amount REAL, "
                + "deposit_status TEXT, "
                + "status TEXT, "
                + "sequence_number INTEGER, "
                + "scheduled_time TEXT, "
                + "priority TEXT, "
                + "created_at TEXT, "
                + "updated_at TEXT)";
        db.execSQL(createDeliveriesTable);

        // 3. Location Logs Table
        String createLocationLogsTable = "CREATE TABLE " + TABLE_LOCATION_LOGS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "driver_id TEXT, "
                + "booking_id TEXT, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "accuracy REAL, "
                + "speed REAL, "
                + "bearing REAL, "
                + "timestamp TEXT)";
        db.execSQL(createLocationLogsTable);

        // 4. Delivery Proof Table
        String createDeliveryProofTable = "CREATE TABLE " + TABLE_DELIVERY_PROOF + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "booking_id TEXT, "
                + "driver_id TEXT, "
                + "photo_path TEXT, "
                + "recipient_name TEXT, "
                + "signature TEXT, "
                + "otp_verified TEXT, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "timestamp TEXT)";
        db.execSQL(createDeliveryProofTable);

        // 5. Order Adjustments Table
        String createAdjustmentsTable = "CREATE TABLE " + TABLE_ORDER_ADJUSTMENTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "booking_id TEXT, "
                + "driver_id TEXT, "
                + "type TEXT, "
                + "amount REAL, "
                + "reason TEXT, "
                + "description TEXT, "
                + "created_at TEXT)";
        db.execSQL(createAdjustmentsTable);

        // 6. Delivery Status History Table
        String createHistoryTable = "CREATE TABLE " + TABLE_STATUS_HISTORY + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "booking_id TEXT, "
                + "driver_id TEXT, "
                + "status TEXT, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "timestamp TEXT)";
        db.execSQL(createHistoryTable);

        // 7. Driver Problem Reports Table
        String createProblemTable = "CREATE TABLE " + TABLE_PROBLEM_REPORTS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "booking_id TEXT, "
                + "driver_id TEXT, "
                + "problem_type TEXT, "
                + "description TEXT, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "timestamp TEXT, "
                + "status TEXT)";
        db.execSQL(createProblemTable);

        // 8. Notifications Table
        String createNotificationsTable = "CREATE TABLE " + TABLE_NOTIFICATIONS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT, "
                + "title TEXT, "
                + "message TEXT, "
                + "is_read INTEGER DEFAULT 0, "
                + "timestamp TEXT)";
        db.execSQL(createNotificationsTable);

        // Saved customer records are independent from individual bookings.
        createCustomersTable(db);

        // 9. Agent Duty Attendance Table
        String createAttendanceTable = "CREATE TABLE " + TABLE_ATTENDANCE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "agent_id TEXT, "
                + "duty_status TEXT, "
                + "start_time TEXT, "
                + "end_time TEXT, "
                + "photo_path TEXT, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "created_at TEXT)";
        db.execSQL(createAttendanceTable);

        // Populate Initial Seed Data with Hashed Passwords
        seedInitialData(db);
        syncCustomersFromBookings(db);
    }

    private void createCustomersTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CUSTOMERS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "full_name TEXT NOT NULL, phone TEXT NOT NULL, email TEXT, address TEXT, "
                + "latitude REAL DEFAULT 0, longitude REAL DEFAULT 0, created_at TEXT, updated_at TEXT)");
    }

    private void syncCustomersFromBookings(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT customer_name, customer_phone, MAX(destination), "
                + "MAX(destination_latitude), MAX(destination_longitude) FROM " + TABLE_DELIVERIES
                + " WHERE customer_name IS NOT NULL AND TRIM(customer_name)<>'' "
                + "GROUP BY customer_name, customer_phone", null);
        if (cursor == null) return;
        while (cursor.moveToNext()) {
            String name = cursor.getString(0), phone = cursor.getString(1), address = cursor.getString(2);
            double latitude = cursor.getDouble(3), longitude = cursor.getDouble(4);
            if (name == null || name.trim().isEmpty() || phone == null || phone.trim().isEmpty()) continue;

            Cursor existing = db.rawQuery("SELECT id, address, latitude, longitude FROM " + TABLE_CUSTOMERS
                    + " WHERE phone=? ORDER BY id LIMIT 1", new String[]{phone.trim()});
            long existingId = -1;
            String existingAddress = "";
            double existingLatitude = 0, existingLongitude = 0;
            if (existing != null) {
                if (existing.moveToFirst()) {
                    existingId = existing.getLong(0);
                    existingAddress = existing.getString(1);
                    existingLatitude = existing.getDouble(2);
                    existingLongitude = existing.getDouble(3);
                }
                existing.close();
            }

            if (existingId < 0) {
                ContentValues values = new ContentValues();
                values.put("full_name", name.trim());
                values.put("phone", phone.trim());
                values.put("email", "");
                values.put("address", address != null ? address : "");
                values.put("latitude", latitude);
                values.put("longitude", longitude);
                values.put("created_at", String.valueOf(System.currentTimeMillis()));
                values.put("updated_at", String.valueOf(System.currentTimeMillis()));
                db.insert(TABLE_CUSTOMERS, null, values);
            } else if ((existingLatitude == 0 || existingLongitude == 0)
                    && latitude != 0 && longitude != 0) {
                ContentValues values = new ContentValues();
                values.put("latitude", latitude);
                values.put("longitude", longitude);
                if (existingAddress == null || existingAddress.trim().isEmpty()) {
                    values.put("address", address != null ? address : "");
                }
                values.put("updated_at", String.valueOf(System.currentTimeMillis()));
                db.update(TABLE_CUSTOMERS, values, "id=?", new String[]{String.valueOf(existingId)});
            }
        }
        cursor.close();
    }

    private void seedInitialData(SQLiteDatabase db) {
        // Never create accounts with hard-coded credentials in the mobile database.
        // User accounts must be provisioned through the authorized TRIMAX account workflow.

        // Sample Admin-Assigned Bookings for driver01
        ContentValues b1 = new ContentValues();
        b1.put("booking_id", "TRX-1024");
        b1.put("driver_id", "driver01");
        b1.put("customer_id", "CUST-101");
        b1.put("customer_name", "Juan Dela Cruz");
        b1.put("customer_phone", "09171234567");
        b1.put("pickup_location", "TRI-MAX Hub, San Jose del Monte");
        b1.put("pickup_latitude", 14.8115);
        b1.put("pickup_longitude", 121.0453);
        b1.put("destination", "San Jose del Monte, Bulacan");
        b1.put("destination_latitude", 14.8150);
        b1.put("destination_longitude", 121.0500);
        b1.put("special_instructions", "Handle with care. ₱500 deposit required.");
        b1.put("package_details", "Standard Freight Box (12 kg)");
        b1.put("original_amount", 1000.0);
        b1.put("current_total", 1000.0);
        b1.put("deposit_amount", 500.0);
        b1.put("deposit_status", "PENDING");
        b1.put("status", "ASSIGNED");
        b1.put("sequence_number", 1);
        b1.put("scheduled_time", "Today • 10:00 AM");
        b1.put("priority", "NORMAL");
        b1.put("created_at", "2026-10-08 08:00:00");
        b1.put("updated_at", "2026-10-08 08:00:00");
        db.insertWithOnConflict(TABLE_DELIVERIES, null, b1, SQLiteDatabase.CONFLICT_REPLACE);

        ContentValues b2 = new ContentValues();
        b2.put("booking_id", "TRX-1025");
        b2.put("driver_id", "driver01");
        b2.put("customer_id", "CUST-102");
        b2.put("customer_name", "Maria Santos");
        b2.put("customer_phone", "09189876543");
        b2.put("pickup_location", "TRI-MAX Hub, San Jose del Monte");
        b2.put("pickup_latitude", 14.8115);
        b2.put("pickup_longitude", 121.0453);
        b2.put("destination", "Bulacan Central, Muzon");
        b2.put("destination_latitude", 14.8200);
        b2.put("destination_longitude", 121.0550);
        b2.put("special_instructions", "Urgent commercial cargo. Keep package upright.");
        b2.put("package_details", "Priority Commercial Pallet (25 kg)");
        b2.put("original_amount", 1500.0);
        b2.put("current_total", 1500.0);
        b2.put("deposit_amount", 0.0);
        b2.put("deposit_status", "N/A");
        b2.put("status", "ASSIGNED");
        b2.put("sequence_number", 2);
        b2.put("scheduled_time", "Today • 02:00 PM");
        b2.put("priority", "HIGH");
        b2.put("created_at", "2026-10-08 09:00:00");
        b2.put("updated_at", "2026-10-08 09:00:00");
        db.insertWithOnConflict(TABLE_DELIVERIES, null, b2, SQLiteDatabase.CONFLICT_REPLACE);

        ContentValues b3 = new ContentValues();
        b3.put("booking_id", "TRX-1026");
        b3.put("driver_id", "driver01");
        b3.put("customer_id", "CUST-103");
        b3.put("customer_name", "Pedro Cruz");
        b3.put("customer_phone", "09191112233");
        b3.put("pickup_location", "TRI-MAX Hub, San Jose del Monte");
        b3.put("pickup_latitude", 14.8115);
        b3.put("pickup_longitude", 121.0453);
        b3.put("destination", "Novaliches, Quezon City");
        b3.put("destination_latitude", 14.7300);
        b3.put("destination_longitude", 121.0400);
        b3.put("special_instructions", "Call customer 15 minutes before arrival.");
        b3.put("package_details", "Standard Package (5 kg)");
        b3.put("original_amount", 800.0);
        b3.put("current_total", 800.0);
        b3.put("deposit_amount", 200.0);
        b3.put("deposit_status", "PENDING");
        b3.put("status", "ASSIGNED");
        b3.put("sequence_number", 3);
        b3.put("scheduled_time", "Today • 04:30 PM");
        b3.put("priority", "NORMAL");
        b3.put("created_at", "2026-10-08 09:30:00");
        b3.put("updated_at", "2026-10-08 09:30:00");
        db.insertWithOnConflict(TABLE_DELIVERIES, null, b3, SQLiteDatabase.CONFLICT_REPLACE);

        // Sample Agent Bookings
        ContentValues b4 = new ContentValues();
        b4.put("booking_id", "BK-2026-00123");
        b4.put("driver_id", "agent01");
        b4.put("customer_id", "CUST-123");
        b4.put("customer_name", "Juan Dela Cruz");
        b4.put("customer_phone", "09171234567");
        b4.put("pickup_location", "TRI-MAX Hub, San Jose del Monte");
        b4.put("pickup_latitude", 14.8115);
        b4.put("pickup_longitude", 121.0453);
        b4.put("destination", "San Jose del Monte, Bulacan");
        b4.put("destination_latitude", 14.8150);
        b4.put("destination_longitude", 121.0500);
        b4.put("special_instructions", "Please call upon arrival.");
        b4.put("package_details", "Standard Cargo Express (x1)");
        b4.put("original_amount", 1024.0);
        b4.put("current_total", 1024.0);
        b4.put("deposit_amount", 0.0);
        b4.put("deposit_status", "N/A");
        b4.put("status", "PENDING");
        b4.put("sequence_number", 4);
        b4.put("scheduled_time", "July 19, 2026 • 10:00 AM");
        b4.put("priority", "NORMAL");
        b4.put("created_at", "2026-10-08 10:00:00");
        b4.put("updated_at", "2026-10-08 10:00:00");
        db.insertWithOnConflict(TABLE_DELIVERIES, null, b4, SQLiteDatabase.CONFLICT_REPLACE);

        ContentValues b5 = new ContentValues();
        b5.put("booking_id", "BK-2026-00122");
        b5.put("driver_id", "agent01");
        b5.put("customer_id", "CUST-122");
        b5.put("customer_name", "Maria Santos");
        b5.put("customer_phone", "09189876543");
        b5.put("pickup_location", "TRI-MAX Hub, San Jose del Monte");
        b5.put("pickup_latitude", 14.8115);
        b5.put("pickup_longitude", 121.0453);
        b5.put("destination", "Bulacan Central, Muzon");
        b5.put("destination_latitude", 14.8200);
        b5.put("destination_longitude", 121.0550);
        b5.put("special_instructions", "Fragile cargo, take extra care.");
        b5.put("package_details", "Fragile Package Handler (x2)");
        b5.put("original_amount", 2450.0);
        b5.put("current_total", 2450.0);
        b5.put("deposit_amount", 0.0);
        b5.put("deposit_status", "N/A");
        b5.put("status", "CONFIRMED");
        b5.put("sequence_number", 5);
        b5.put("scheduled_time", "July 18, 2026 • 02:00 PM");
        b5.put("priority", "HIGH");
        b5.put("created_at", "2026-10-08 10:30:00");
        b5.put("updated_at", "2026-10-08 10:30:00");
        db.insertWithOnConflict(TABLE_DELIVERIES, null, b5, SQLiteDatabase.CONFLICT_REPLACE);

        // Initial Notifications
        ContentValues n1 = new ContentValues();
        n1.put("username", "driver01");
        n1.put("title", "Admin Booking Assignment");
        n1.put("message", "Admin assigned #TRX-1024, #TRX-1025, #TRX-1026 to your route.");
        n1.put("is_read", 0);
        n1.put("timestamp", "Just now");
        db.insertWithOnConflict(TABLE_NOTIFICATIONS, null, n1, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ATTENDANCE + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "agent_id TEXT, "
                + "duty_status TEXT, "
                + "start_time TEXT, "
                + "end_time TEXT, "
                + "photo_path TEXT, "
                + "latitude REAL, "
                + "longitude REAL, "
                + "created_at TEXT)");
        db.execSQL("UPDATE " + TABLE_USERS + " SET role='AGENT' WHERE UPPER(role)='RIDER'");
        createCustomersTable(db);
        if (oldVersion < 6) {
            try { seedInitialData(db); } catch (Exception ignored) {}
        }
        if (oldVersion < 8) {
            try { db.execSQL("ALTER TABLE " + TABLE_CUSTOMERS + " ADD COLUMN latitude REAL DEFAULT 0"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_CUSTOMERS + " ADD COLUMN longitude REAL DEFAULT 0"); } catch (Exception ignored) {}
        }
        if (oldVersion < 8) syncCustomersFromBookings(db);
        if (oldVersion < 9) {
            ContentValues pickupValues = new ContentValues();
            pickupValues.put("pickup_location", "TRIMAX Hub (14.782700, 121.047587)");
            pickupValues.put("pickup_latitude", 14.7826996);
            pickupValues.put("pickup_longitude", 121.0475865);
            db.update(TABLE_DELIVERIES, pickupValues, null, null);
        }
    }

    // Authenticate User with Secure Password Verification & Legacy Plaintext Upgrade
    public User authenticateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE (username=? OR email=?)",
                new String[]{username, username});

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            String storedPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            if (verifyPassword(password, storedPassword)) {
                if (!storedPassword.contains(":")) {
                    SQLiteDatabase wDb = this.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("password", hashPassword(password));
                    wDb.update(TABLE_USERS, cv, "id=?", new String[]{String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("id")))});
                }

                String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
                if ("RIDER".equalsIgnoreCase(role)) {
                    role = "AGENT";
                }

                user = new User(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("password")),
                        role,
                        cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("email")),
                        cursor.getString(cursor.getColumnIndexOrThrow("phone"))
                );
            }
            cursor.close();
        }
        return user;
    }

    public List<Delivery> getAssignedDeliveries(String driverUsername) {
        List<Delivery> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DELIVERIES + " WHERE driver_id=? AND status!='DELIVERED' AND status!='FAILED' AND status!='CANCELLED' ORDER BY sequence_number ASC",
                new String[]{driverUsername});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(extractDeliveryFromCursor(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public Delivery getNextAssignedDelivery(String driverUsername) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DELIVERIES + " WHERE driver_id=? AND status!='DELIVERED' AND status!='FAILED' AND status!='CANCELLED' ORDER BY sequence_number ASC LIMIT 1",
                new String[]{driverUsername});

        Delivery d = null;
        if (cursor != null && cursor.moveToFirst()) {
            d = extractDeliveryFromCursor(cursor);
            cursor.close();
        }
        return d;
    }

    public List<Delivery> getDeliveryHistory(String driverUsername, String filterStatus) {
        List<Delivery> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_DELIVERIES + " WHERE driver_id=?";
        List<String> argsList = new ArrayList<>();
        argsList.add(driverUsername);

        if (filterStatus != null && !"ALL".equalsIgnoreCase(filterStatus)) {
            if ("DELIVERED".equalsIgnoreCase(filterStatus) || "COMPLETED".equalsIgnoreCase(filterStatus)) {
                // Historical data may use either DELIVERED or COMPLETED for successful deliveries.
                query += " AND UPPER(status) IN ('DELIVERED', 'COMPLETED')";
            } else if ("FAILED".equalsIgnoreCase(filterStatus)) {
                query += " AND UPPER(status)='FAILED'";
            } else {
                query += " AND UPPER(status)=?";
                argsList.add(filterStatus.toUpperCase(Locale.ROOT));
            }
        } else {
            query += " AND UPPER(status) IN ('DELIVERED', 'COMPLETED', 'FAILED', 'CANCELLED')";
        }

        query += " ORDER BY sequence_number ASC, id DESC";

        Cursor cursor = db.rawQuery(query, argsList.toArray(new String[0]));

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(extractDeliveryFromCursor(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public Delivery getDeliveryByCode(String bookingId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DELIVERIES + " WHERE booking_id=?", new String[]{bookingId});

        Delivery d = null;
        if (cursor != null && cursor.moveToFirst()) {
            d = extractDeliveryFromCursor(cursor);
            cursor.close();
        }
        return d;
    }

    public boolean updateDeliveryStatus(String bookingId, String status, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("status", status);
        cv.put("updated_at", String.valueOf(System.currentTimeMillis()));

        int rows = db.update(TABLE_DELIVERIES, cv, "booking_id=?", new String[]{bookingId});
        if (rows > 0) {
            Cursor cursor = db.rawQuery("SELECT driver_id FROM " + TABLE_DELIVERIES + " WHERE booking_id=?", new String[]{bookingId});
            String driverId = "driver01";
            if (cursor != null && cursor.moveToFirst()) {
                driverId = cursor.getString(cursor.getColumnIndexOrThrow("driver_id"));
                cursor.close();
            }

            ContentValues histCv = new ContentValues();
            histCv.put("booking_id", bookingId);
            histCv.put("driver_id", driverId);
            histCv.put("status", status);
            histCv.put("latitude", lat);
            histCv.put("longitude", lng);
            histCv.put("timestamp", String.valueOf(System.currentTimeMillis()));
            db.insert(TABLE_STATUS_HISTORY, null, histCv);
            return true;
        }
        return false;
    }

    public boolean updateDeliveryStatus(String bookingId, String status) {
        return updateDeliveryStatus(bookingId, status, 14.8115, 121.0453);
    }

    public boolean logLocation(String driverUsername, String bookingId, double lat, double lng, float accuracy, float speed, float bearing) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("driver_id", driverUsername);
        cv.put("booking_id", bookingId != null ? bookingId : "");
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("accuracy", accuracy);
        cv.put("speed", speed);
        cv.put("bearing", bearing);
        cv.put("timestamp", String.valueOf(System.currentTimeMillis()));

        long id = db.insert(TABLE_LOCATION_LOGS, null, cv);
        return id != -1;
    }

    public boolean logLocation(String driverUsername, double lat, double lng, float accuracy) {
        return logLocation(driverUsername, "", lat, lng, accuracy, 0.0f, 0.0f);
    }

    // Save Delivery Proof Atomically in Transaction
    public boolean saveDeliveryProof(String bookingId, String driverId, String photoPath, String recipientName, String signature, String otpVerified, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("booking_id", bookingId);
            cv.put("driver_id", driverId);
            cv.put("photo_path", photoPath);
            cv.put("recipient_name", recipientName);
            cv.put("signature", signature);
            cv.put("otp_verified", otpVerified);
            cv.put("latitude", lat);
            cv.put("longitude", lng);
            cv.put("timestamp", String.valueOf(System.currentTimeMillis()));

            long id = db.insert(TABLE_DELIVERY_PROOF, null, cv);
            if (id != -1) {
                boolean statusUpdated = updateDeliveryStatus(bookingId, "DELIVERED", lat, lng);
                if (statusUpdated) {
                    db.setTransactionSuccessful();
                    return true;
                }
            }
        } catch (Exception ignored) {
        } finally {
            db.endTransaction();
        }
        return false;
    }

    public boolean addOrderAdjustment(String bookingId, String driverId, String type, double amount, String reason, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("booking_id", bookingId);
        cv.put("driver_id", driverId);
        cv.put("type", type);
        cv.put("amount", amount);
        cv.put("reason", reason);
        cv.put("description", description);
        cv.put("created_at", String.valueOf(System.currentTimeMillis()));

        long id = db.insert(TABLE_ORDER_ADJUSTMENTS, null, cv);
        if (id != -1) {
            recalculateBookingTotal(bookingId);
            return true;
        }
        return false;
    }

    public void recalculateBookingTotal(String bookingId) {
        Delivery d = getDeliveryByCode(bookingId);
        if (d == null) return;

        double original = d.getOriginalAmount();
        double addCharges = 0;
        double damageCharges = 0;
        double deductions = 0;
        double refunds = 0;

        List<OrderAdjustment> adjustments = getOrderAdjustments(bookingId);
        for (OrderAdjustment adj : adjustments) {
            String type = adj.getType();
            if ("ADDITIONAL_CHARGE".equalsIgnoreCase(type)) {
                addCharges += adj.getAmount();
            } else if ("DAMAGE_CHARGE".equalsIgnoreCase(type)) {
                damageCharges += adj.getAmount();
            } else if ("DEDUCTION".equalsIgnoreCase(type) || "DEPOSIT".equalsIgnoreCase(type)) {
                deductions += adj.getAmount();
            } else if ("REFUND".equalsIgnoreCase(type)) {
                refunds += adj.getAmount();
            }
        }

        double newTotal = original + addCharges + damageCharges - deductions - refunds;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("current_total", newTotal);
        cv.put("updated_at", String.valueOf(System.currentTimeMillis()));
        db.update(TABLE_DELIVERIES, cv, "booking_id=?", new String[]{bookingId});
    }

    public List<OrderAdjustment> getOrderAdjustments(String bookingId) {
        List<OrderAdjustment> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ORDER_ADJUSTMENTS + " WHERE booking_id=?", new String[]{bookingId});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                OrderAdjustment adj = new OrderAdjustment(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("booking_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("driver_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("type")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                        cursor.getString(cursor.getColumnIndexOrThrow("reason")),
                        cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                );
                list.add(adj);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public boolean updateDepositStatus(String bookingId, String driverId, double amount, String depositStatus, String reason) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("deposit_amount", amount);
        cv.put("deposit_status", depositStatus);
        cv.put("updated_at", String.valueOf(System.currentTimeMillis()));

        int rows = db.update(TABLE_DELIVERIES, cv, "booking_id=?", new String[]{bookingId});
        if (rows > 0) {
            addOrderAdjustment(bookingId, driverId, "DEPOSIT", amount, reason, "Deposit status changed to " + depositStatus);
            return true;
        }
        return false;
    }

    public boolean saveProblemReport(String bookingId, String driverId, String problemType, String description, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("booking_id", bookingId);
        cv.put("driver_id", driverId);
        cv.put("problem_type", problemType);
        cv.put("description", description);
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("timestamp", String.valueOf(System.currentTimeMillis()));
        cv.put("status", "SUBMITTED");

        long id = db.insert(TABLE_PROBLEM_REPORTS, null, cv);
        return id != -1;
    }

    public boolean hasActiveDelivery(String driverUsername) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DELIVERIES + " WHERE driver_id=? AND status!='ASSIGNED' AND status!='DELIVERED' AND status!='FAILED' AND status!='CANCELLED'",
                new String[]{driverUsername});
        boolean hasActive = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        return hasActive;
    }

    private Delivery extractDeliveryFromCursor(Cursor cursor) {
        Delivery d = new Delivery();
        try {
            d.setId(getSafeInt(cursor, "id"));
            d.setBookingId(getSafeString(cursor, "booking_id"));
            d.setDriverId(getSafeString(cursor, "driver_id"));
            d.setCustomerId(getSafeString(cursor, "customer_id"));
            d.setCustomerName(getSafeString(cursor, "customer_name"));
            d.setCustomerPhone(getSafeString(cursor, "customer_phone"));
            d.setPickupLocation(getSafeString(cursor, "pickup_location"));
            d.setPickupLatitude(getSafeDouble(cursor, "pickup_latitude"));
            d.setPickupLongitude(getSafeDouble(cursor, "pickup_longitude"));
            d.setDestination(getSafeString(cursor, "destination"));
            d.setDestinationLatitude(getSafeDouble(cursor, "destination_latitude"));
            d.setDestinationLongitude(getSafeDouble(cursor, "destination_longitude"));
            d.setSpecialInstructions(getSafeString(cursor, "special_instructions"));
            d.setPackageDetails(getSafeString(cursor, "package_details"));
            d.setOriginalAmount(getSafeDouble(cursor, "original_amount"));
            d.setCurrentTotal(getSafeDouble(cursor, "current_total"));
            d.setDepositAmount(getSafeDouble(cursor, "deposit_amount"));
            d.setDepositStatus(getSafeString(cursor, "deposit_status"));
            d.setStatus(getSafeString(cursor, "status"));
            d.setSequenceNumber(getSafeInt(cursor, "sequence_number"));
            d.setScheduledTime(getSafeString(cursor, "scheduled_time"));
            d.setPriority(getSafeString(cursor, "priority"));
            d.setCreatedAt(getSafeString(cursor, "created_at"));
            d.setUpdatedAt(getSafeString(cursor, "updated_at"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return d;
    }

    private String getSafeString(Cursor c, String colName) {
        int idx = c.getColumnIndex(colName);
        return (idx != -1 && !c.isNull(idx)) ? c.getString(idx) : "";
    }

    private int getSafeInt(Cursor c, String colName) {
        int idx = c.getColumnIndex(colName);
        return (idx != -1 && !c.isNull(idx)) ? c.getInt(idx) : 0;
    }

    private double getSafeDouble(Cursor c, String colName) {
        int idx = c.getColumnIndex(colName);
        return (idx != -1 && !c.isNull(idx)) ? c.getDouble(idx) : 0.0;
    }

    public boolean addBooking(Delivery delivery) {
        if (delivery == null || delivery.getDestination() == null || delivery.getDestination().trim().isEmpty()) {
            return false;
        }
        double destinationLatitude = delivery.getDestinationLatitude();
        double destinationLongitude = delivery.getDestinationLongitude();
        if (!Double.isFinite(destinationLatitude) || !Double.isFinite(destinationLongitude)
                || destinationLatitude < -90 || destinationLatitude > 90
                || destinationLongitude < -180 || destinationLongitude > 180
                || (destinationLatitude == 0 && destinationLongitude == 0)) {
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("booking_id", delivery.getBookingId());
        cv.put("driver_id", delivery.getDriverId() != null ? delivery.getDriverId() : "agent01");
        cv.put("customer_id", delivery.getCustomerId() != null ? delivery.getCustomerId() : "CUST-" + (System.currentTimeMillis() % 1000));
        cv.put("customer_name", delivery.getCustomerName());
        cv.put("customer_phone", delivery.getCustomerPhone());
        cv.put("pickup_location", "TRIMAX Hub (14.782700, 121.047587)");
        cv.put("pickup_latitude", 14.7826996);
        cv.put("pickup_longitude", 121.0475865);
        cv.put("destination", delivery.getDestination().trim());
        cv.put("destination_latitude", destinationLatitude);
        cv.put("destination_longitude", destinationLongitude);
        cv.put("special_instructions", delivery.getSpecialInstructions());
        cv.put("package_details", delivery.getPackageDetails());
        cv.put("original_amount", delivery.getOriginalAmount());
        cv.put("current_total", delivery.getCurrentTotal());
        cv.put("deposit_amount", delivery.getDepositAmount());
        cv.put("deposit_status", delivery.getDepositStatus() != null ? delivery.getDepositStatus() : "PENDING");
        cv.put("status", delivery.getStatus() != null ? delivery.getStatus() : "PENDING");
        cv.put("sequence_number", delivery.getSequenceNumber() > 0 ? delivery.getSequenceNumber() : 1);
        cv.put("scheduled_time", delivery.getScheduledTime());
        cv.put("priority", delivery.getPriority() != null ? delivery.getPriority() : "NORMAL");
        cv.put("created_at", delivery.getCreatedAt() != null ? delivery.getCreatedAt() : String.valueOf(System.currentTimeMillis()));
        cv.put("updated_at", delivery.getUpdatedAt() != null ? delivery.getUpdatedAt() : String.valueOf(System.currentTimeMillis()));

        long result = db.insert(TABLE_DELIVERIES, null, cv);
        return result != -1;
    }

    public String generateNextBookingId() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_DELIVERIES, null);
        int count = 124;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count += cursor.getInt(0);
            }
            cursor.close();
        }
        return String.format(Locale.US, "BK-2026-%05d", count);
    }

    public List<Delivery> getBookingsForCustomer(String customerPhone) {
        return getBookingsForCustomer(customerPhone, null);
    }

    public List<Delivery> getBookingsForCustomer(String customerPhone, String customerName) {
        List<Delivery> list = new ArrayList<>();
        String phone = customerPhone != null ? customerPhone.trim() : "";
        String name = customerName != null ? customerName.trim() : "";
        if (phone.isEmpty() && name.isEmpty()) return list;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DELIVERIES
                + " WHERE (customer_phone IS NOT NULL AND customer_phone<>'' AND customer_phone=?) "
                + " OR (customer_name IS NOT NULL AND customer_name<>'' AND LOWER(customer_name)=LOWER(?)) "
                + " ORDER BY id DESC",
                new String[]{phone, name});
        try {
            while (cursor != null && cursor.moveToNext()) {
                list.add(extractDeliveryFromCursor(cursor));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return list;
    }

    public List<Delivery> getAllBookings() {
        return getBookingsByFilter("ALL", null);
    }

    public List<Delivery> getBookingsByFilter(String statusFilter, String searchQuery) {
        List<Delivery> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder query = new StringBuilder("SELECT * FROM " + TABLE_DELIVERIES + " WHERE 1=1");
        List<String> argsList = new ArrayList<>();

        if (statusFilter != null && !"ALL".equalsIgnoreCase(statusFilter)) {
            if ("COMPLETED".equalsIgnoreCase(statusFilter)) {
                query.append(" AND (UPPER(status)='COMPLETED' OR UPPER(status)='DELIVERED')");
            } else {
                query.append(" AND UPPER(status)=?");
                argsList.add(statusFilter.toUpperCase());
            }
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String q = "%" + searchQuery.trim().toLowerCase() + "%";
            query.append(" AND (LOWER(booking_id) LIKE ? OR LOWER(customer_name) LIKE ? OR LOWER(customer_phone) LIKE ? OR LOWER(destination) LIKE ?)");
            argsList.add(q);
            argsList.add(q);
            argsList.add(q);
            argsList.add(q);
        }

        query.append(" ORDER BY id DESC");

        Cursor cursor = db.rawQuery(query.toString(), argsList.toArray(new String[0]));
        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(extractDeliveryFromCursor(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public int getBookingCountByStatus(String status) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query;
        List<String> argsList = new ArrayList<>();

        if ("ALL".equalsIgnoreCase(status) || status == null || status.isEmpty()) {
            query = "SELECT COUNT(*) FROM " + TABLE_DELIVERIES;
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            query = "SELECT COUNT(*) FROM " + TABLE_DELIVERIES + " WHERE UPPER(status)='COMPLETED' OR UPPER(status)='DELIVERED'";
        } else {
            query = "SELECT COUNT(*) FROM " + TABLE_DELIVERIES + " WHERE UPPER(status)=?";
            argsList.add(status.toUpperCase());
        }

        Cursor cursor = db.rawQuery(query, argsList.toArray(new String[0]));
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        }
        return count;
    }


    /**
     * Counts bookings by status within an inclusive date range.
     * created_at can be stored either as epoch milliseconds or an ISO-like date string.
     */
    public int getBookingCountByStatusAndDateRange(String status, long startMillis, long endMillis) {
        SQLiteDatabase db = this.getReadableDatabase();
        String createdAtEpochSeconds = "CASE "
                + "WHEN CAST(created_at AS INTEGER) > 100000000000 "
                + "THEN CAST(CAST(created_at AS INTEGER) / 1000 AS INTEGER) "
                + "ELSE CAST(strftime('%s', created_at) AS INTEGER) END";
        StringBuilder query = new StringBuilder("SELECT COUNT(*) FROM " + TABLE_DELIVERIES
                + " WHERE " + createdAtEpochSeconds + " BETWEEN ? AND ?");
        List<String> argsList = new ArrayList<>();
        argsList.add(String.valueOf(startMillis / 1000L));
        argsList.add(String.valueOf(endMillis / 1000L));

        if ("ALL".equalsIgnoreCase(status) || status == null || status.isEmpty()) {
            // No additional status condition.
        } else if ("COMPLETED".equalsIgnoreCase(status)) {
            query.append(" AND (UPPER(status)='COMPLETED' OR UPPER(status)='DELIVERED')");
        } else {
            query.append(" AND UPPER(status)=?");
            argsList.add(status.toUpperCase());
        }

        Cursor cursor = db.rawQuery(query.toString(), argsList.toArray(new String[0]));
        int count = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    public boolean cancelBooking(String bookingId) {
        return updateDeliveryStatus(bookingId, "CANCELLED");
    }

    public boolean confirmBooking(String bookingId) {
        return updateDeliveryStatus(bookingId, "CONFIRMED");
    }

    public boolean updateUserProfile(int userId, String fullName, String email, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if (fullName != null) cv.put("full_name", fullName.trim());
        if (email != null) cv.put("email", email.trim());
        if (phone != null) cv.put("phone", phone.trim());

        int rows = db.update(TABLE_USERS, cv, "id=?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    public boolean changeUserPassword(int userId, String currentPassword, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT password FROM " + TABLE_USERS + " WHERE id=?", new String[]{String.valueOf(userId)});
        boolean validCurrent = false;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String existingPwd = cursor.getString(0);
                if (verifyPassword(currentPassword, existingPwd)) {
                    validCurrent = true;
                }
            }
            cursor.close();
        }

        if (!validCurrent) {
            return false;
        }

        ContentValues cv = new ContentValues();
        cv.put("password", hashPassword(newPassword));
        int rows = db.update(TABLE_USERS, cv, "id=?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    public User getUserByEmailOrUsername(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE LOWER(username)=? OR LOWER(email)=?",
                new String[]{identifier.trim().toLowerCase(), identifier.trim().toLowerCase()});
        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
            if ("RIDER".equalsIgnoreCase(role)) role = "AGENT";
            user = new User(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("username")),
                    cursor.getString(cursor.getColumnIndexOrThrow("password")),
                    role,
                    cursor.getString(cursor.getColumnIndexOrThrow("full_name")),
                    cursor.getString(cursor.getColumnIndexOrThrow("email")),
                    cursor.getString(cursor.getColumnIndexOrThrow("phone"))
            );
            cursor.close();
        }
        return user;
    }

    public boolean updatePasswordByEmail(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", hashPassword(newPassword));
        int rows = db.update(TABLE_USERS, cv, "LOWER(email)=? OR LOWER(username)=?",
                new String[]{email.trim().toLowerCase(), email.trim().toLowerCase()});
        return rows > 0;
    }

    public static class NotificationItem {
        private final int id;
        private final String username;
        private final String title;
        private final String message;
        private final boolean isRead;
        private final String timestamp;

        public NotificationItem(int id, String username, String title, String message, boolean isRead, String timestamp) {
            this.id = id;
            this.username = username;
            this.title = title;
            this.message = message;
            this.isRead = isRead;
            this.timestamp = timestamp;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public boolean isRead() { return isRead; }
        public String getTimestamp() { return timestamp; }
    }

    public boolean addNotification(String username, String title, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("title", title);
        cv.put("message", message);
        cv.put("is_read", 0);
        cv.put("timestamp", "Just now");
        long id = db.insert(TABLE_NOTIFICATIONS, null, cv);
        return id != -1;
    }

    public List<NotificationItem> getNotificationsForUser(String username) {
        List<NotificationItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NOTIFICATIONS + " WHERE username=? OR username='ALL' ORDER BY id DESC",
                new String[]{username != null ? username : ""});

        if (cursor != null && cursor.moveToFirst()) {
            do {
                list.add(new NotificationItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("message")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) == 1,
                        cursor.getString(cursor.getColumnIndexOrThrow("timestamp"))
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public boolean markNotificationAsRead(int notificationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        int rows = db.update(TABLE_NOTIFICATIONS, cv, "id=?", new String[]{String.valueOf(notificationId)});
        return rows > 0;
    }

    public boolean markAllNotificationsAsRead(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        int rows = db.update(TABLE_NOTIFICATIONS, cv, "username=? OR username='ALL'", new String[]{username != null ? username : ""});
        return rows > 0;
    }

    public static class ClientItem {
        private final String name;
        private final String phone;
        private final String email;
        private final int bookingCount;

        public ClientItem(String name, String phone, String email, int bookingCount) {
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.bookingCount = bookingCount;
        }

        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public int getBookingCount() { return bookingCount; }
    }

    public static class SavedCustomer {
        private final long id;
        private final String name, phone, email, address;
        private final double latitude, longitude;
        private final int bookingCount;
        public SavedCustomer(long id, String name, String phone, String email, String address,
                             double latitude, double longitude, int bookingCount) {
            this.id=id; this.name=name; this.phone=phone; this.email=email; this.address=address;
            this.latitude=latitude; this.longitude=longitude; this.bookingCount=bookingCount;
        }
        public long getId(){return id;}
        public String getName(){return name;}
        public String getPhone(){return phone;}
        public String getEmail(){return email;}
        public String getAddress(){return address;}
        public double getLatitude(){return latitude;}
        public double getLongitude(){return longitude;}
        public int getBookingCount(){return bookingCount;}
        @Override public String toString(){return name+" • "+phone;}
    }

    public List<SavedCustomer> getSavedCustomers(String searchQuery) {
        List<SavedCustomer> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT c.id, c.full_name, c.phone, c.email, c.address, "
                + "COALESCE(NULLIF(c.latitude, 0), (SELECT d.destination_latitude FROM " + TABLE_DELIVERIES + " d WHERE (d.customer_phone=c.phone OR d.customer_name=c.full_name) AND d.destination_latitude<>0 ORDER BY d.id DESC LIMIT 1), (SELECT d.pickup_latitude FROM " + TABLE_DELIVERIES + " d WHERE (d.customer_phone=c.phone OR d.customer_name=c.full_name) AND d.pickup_latitude<>0 ORDER BY d.id DESC LIMIT 1), 0) AS lat, "
                + "COALESCE(NULLIF(c.longitude, 0), (SELECT d.destination_longitude FROM " + TABLE_DELIVERIES + " d WHERE (d.customer_phone=c.phone OR d.customer_name=c.full_name) AND d.destination_longitude<>0 ORDER BY d.id DESC LIMIT 1), (SELECT d.pickup_longitude FROM " + TABLE_DELIVERIES + " d WHERE (d.customer_phone=c.phone OR d.customer_name=c.full_name) AND d.pickup_longitude<>0 ORDER BY d.id DESC LIMIT 1), 0) AS lng, "
                + "(SELECT COUNT(*) FROM " + TABLE_DELIVERIES + " d WHERE d.customer_phone=c.phone OR d.customer_name=c.full_name) "
                + "FROM " + TABLE_CUSTOMERS + " c";
        List<String> args = new ArrayList<>();
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            query += " WHERE LOWER(c.full_name) LIKE ? OR LOWER(c.phone) LIKE ? OR LOWER(COALESCE(c.email,'')) LIKE ?";
            String pattern = "%" + searchQuery.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(pattern); args.add(pattern); args.add(pattern);
        }
        query += " ORDER BY c.full_name COLLATE NOCASE ASC";
        Cursor cursor = db.rawQuery(query, args.toArray(new String[0]));
        if (cursor != null) {
            while (cursor.moveToNext()) list.add(new SavedCustomer(cursor.getLong(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getDouble(5),
                    cursor.getDouble(6), cursor.getInt(7)));
            cursor.close();
        }
        return list;
    }

    public String getCustomerCodeByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return null;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_CUSTOMERS + " WHERE phone=? ORDER BY id LIMIT 1",
                new String[]{phone.trim()});
        String code = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) code = "CUST-" + cursor.getLong(0);
            cursor.close();
        }
        return code;
    }

    public boolean saveCustomer(String name, String phone, String email, String address) {
        return saveCustomer(name, phone, email, address, 0, 0);
    }

    public boolean saveCustomer(String name, String phone, String email, String address, double latitude, double longitude) {
        if (name == null || name.trim().isEmpty() || phone == null || phone.trim().isEmpty()) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("full_name", name.trim()); values.put("phone", phone.trim());
        values.put("email", email != null ? email.trim() : "");
        values.put("address", address != null ? address.trim() : "");
        values.put("updated_at", String.valueOf(System.currentTimeMillis()));
        Cursor cursor = db.rawQuery("SELECT id, address, email, latitude, longitude FROM " + TABLE_CUSTOMERS
                + " WHERE phone=? ORDER BY id LIMIT 1", new String[]{phone.trim()});
        long existingId = -1; String existingAddress = "", existingEmail = "";
        double existingLatitude = 0, existingLongitude = 0;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                existingId=cursor.getLong(0); existingAddress=cursor.getString(1); existingEmail=cursor.getString(2);
                existingLatitude=cursor.getDouble(3); existingLongitude=cursor.getDouble(4);
            }
            cursor.close();
        }
        if (existingId >= 0) {
            if (address == null || address.trim().isEmpty()) values.put("address", existingAddress != null ? existingAddress : "");
            if (email == null || email.trim().isEmpty()) values.put("email", existingEmail != null ? existingEmail : "");
            if (latitude != 0 || longitude != 0) { values.put("latitude", latitude); values.put("longitude", longitude); }
            else { values.put("latitude", existingLatitude); values.put("longitude", existingLongitude); }
            return db.update(TABLE_CUSTOMERS, values, "id=?", new String[]{String.valueOf(existingId)}) > 0;
        }
        values.put("latitude", latitude); values.put("longitude", longitude);
        values.put("created_at", String.valueOf(System.currentTimeMillis()));
        return db.insert(TABLE_CUSTOMERS, null, values) != -1;
    }

    public List<ClientItem> getUniqueClients() {
        List<ClientItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT customer_name, customer_phone, COUNT(*) as cnt FROM " + TABLE_DELIVERIES + " GROUP BY customer_name, customer_phone ORDER BY customer_name ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("customer_name"));
                String phone = cursor.getString(cursor.getColumnIndexOrThrow("customer_phone"));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow("cnt"));
                String email = (name != null ? name.toLowerCase().replaceAll("\\s+", "") : "client") + "@gmail.com";
                list.add(new ClientItem(name, phone, email, count));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public static class AttendanceRecord {
        private final int id;
        private final String agentId;
        private final String dutyStatus;
        private final String startTime;
        private final String endTime;
        private final String photoPath;
        private final double latitude;
        private final double longitude;

        public AttendanceRecord(int id, String agentId, String dutyStatus, String startTime, String endTime, String photoPath, double latitude, double longitude) {
            this.id = id;
            this.agentId = agentId;
            this.dutyStatus = dutyStatus;
            this.startTime = startTime;
            this.endTime = endTime;
            this.photoPath = photoPath;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public int getId() { return id; }
        public String getAgentId() { return agentId; }
        public String getDutyStatus() { return dutyStatus; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getPhotoPath() { return photoPath; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }

    public boolean saveAttendance(String agentId, String photoPath, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("agent_id", agentId);
        cv.put("duty_status", "ON_DUTY");
        String nowStr = String.valueOf(System.currentTimeMillis());
        cv.put("start_time", nowStr);
        cv.put("end_time", "");
        cv.put("photo_path", photoPath != null ? photoPath : "");
        cv.put("latitude", lat);
        cv.put("longitude", lng);
        cv.put("created_at", nowStr);

        long id = db.insert(TABLE_ATTENDANCE, null, cv);
        return id != -1;
    }

    public boolean endDuty(String agentId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("duty_status", "OFF_DUTY");
        cv.put("end_time", String.valueOf(System.currentTimeMillis()));

        int rows = db.update(TABLE_ATTENDANCE, cv, "agent_id=? AND duty_status='ON_DUTY'", new String[]{agentId != null ? agentId : ""});
        return rows > 0;
    }

    public AttendanceRecord getActiveAttendance(String agentId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ATTENDANCE + " WHERE agent_id=? AND duty_status='ON_DUTY' ORDER BY id DESC LIMIT 1",
                new String[]{agentId != null ? agentId : ""});

        AttendanceRecord record = null;
        if (cursor != null && cursor.moveToFirst()) {
            record = new AttendanceRecord(
                    cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("agent_id")),
                    cursor.getString(cursor.getColumnIndexOrThrow("duty_status")),
                    cursor.getString(cursor.getColumnIndexOrThrow("start_time")),
                    cursor.getString(cursor.getColumnIndexOrThrow("end_time")),
                    cursor.getString(cursor.getColumnIndexOrThrow("photo_path")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
            );
            cursor.close();
        }
        return record;
    }
}
