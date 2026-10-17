package com.example.trimax;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trimax.db";
    private static final int DATABASE_VERSION = 4; // Incremented for Official Agent Role Migration

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

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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

        // Populate Initial Seed Data
        seedInitialData(db);
        
        // Ensure role standardization migration
        db.execSQL("UPDATE " + TABLE_USERS + " SET role='AGENT' WHERE UPPER(role)='RIDER'");
    }

    private void seedInitialData(SQLiteDatabase db) {
        // Driver User Account
        ContentValues driverUser = new ContentValues();
        driverUser.put("username", "driver01");
        driverUser.put("password", "driver123");
        driverUser.put("role", "DRIVER");
        driverUser.put("full_name", "Test Driver");
        driverUser.put("email", "driver@trimax.com");
        driverUser.put("phone", "09123456789");
        db.insertWithOnConflict(TABLE_USERS, null, driverUser, SQLiteDatabase.CONFLICT_REPLACE);

        // Official Agent User Account
        ContentValues agentUser = new ContentValues();
        agentUser.put("username", "agent01");
        agentUser.put("password", "agent123");
        agentUser.put("role", "AGENT");
        agentUser.put("full_name", "Test Agent");
        agentUser.put("email", "agent@trimax.com");
        agentUser.put("phone", "09170001122");
        db.insertWithOnConflict(TABLE_USERS, null, agentUser, SQLiteDatabase.CONFLICT_REPLACE);

        // Legacy Rider User Account (mapped to official AGENT role)
        ContentValues riderUser = new ContentValues();
        riderUser.put("username", "rider01");
        riderUser.put("password", "rider123");
        riderUser.put("role", "AGENT");
        riderUser.put("full_name", "Test Agent (Former Rider)");
        riderUser.put("email", "rider@trimax.com");
        riderUser.put("phone", "09987654321");
        db.insertWithOnConflict(TABLE_USERS, null, riderUser, SQLiteDatabase.CONFLICT_REPLACE);

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
        db.execSQL("UPDATE " + TABLE_USERS + " SET role='AGENT' WHERE UPPER(role)='RIDER'");
        if (oldVersion < 4) {
            seedInitialData(db);
        }
    }

    // Authenticate User
    public User authenticateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE (username=? OR email=?) AND password=?",
                new String[]{username, username, password});

        User user = null;
        if (cursor != null && cursor.moveToFirst()) {
            String role = cursor.getString(cursor.getColumnIndexOrThrow("role"));
            if ("RIDER".equalsIgnoreCase(role)) {
                role = "AGENT"; // Normalize former Rider role to AGENT
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
            cursor.close();
        }
        return user;
    }

    // Get Assigned Bookings for currently logged-in driver ordered by sequence_number
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

    // Get Next Assigned Booking for Driver
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

    // Get Delivery History for Driver
    public List<Delivery> getDeliveryHistory(String driverUsername, String filterStatus) {
        List<Delivery> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_DELIVERIES + " WHERE driver_id=?";
        List<String> argsList = new ArrayList<>();
        argsList.add(driverUsername);

        if (filterStatus != null && !"ALL".equalsIgnoreCase(filterStatus)) {
            query += " AND status=?";
            argsList.add(filterStatus);
        } else {
            query += " AND (status='DELIVERED' OR status='FAILED' OR status='CANCELLED')";
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

    // Get Delivery by Code/ID
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

    // Update Delivery Status & Log History
    public boolean updateDeliveryStatus(String bookingId, String status, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("status", status);
        cv.put("updated_at", String.valueOf(System.currentTimeMillis()));

        int rows = db.update(TABLE_DELIVERIES, cv, "booking_id=?", new String[]{bookingId});
        if (rows > 0) {
            // Get driver_id for history log
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

    // Log Driver Location
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

    // Save Delivery Proof
    public boolean saveDeliveryProof(String bookingId, String driverId, String photoPath, String recipientName, String signature, String otpVerified, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();
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
            updateDeliveryStatus(bookingId, "DELIVERED", lat, lng);
            return true;
        }
        return false;
    }

    // Add Financial Adjustment
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

    // Recalculate Booking Total: Final Total = Original Amount + Additional Charges + Damage Charges - Deductions - Refunds
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

    // Get Order Adjustments
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

    // Update Deposit Status
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

    // Save Problem Report for Admin Review
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

    // Check Active Delivery
    public boolean hasActiveDelivery(String driverUsername) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_DELIVERIES + " WHERE driver_id=? AND status!='ASSIGNED' AND status!='DELIVERED' AND status!='FAILED' AND status!='CANCELLED'",
                new String[]{driverUsername});
        boolean hasActive = cursor != null && cursor.getCount() > 0;
        if (cursor != null) cursor.close();
        return hasActive;
    }

    // Cursor extractor helper
    private Delivery extractDeliveryFromCursor(Cursor cursor) {
        return new Delivery(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("booking_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("driver_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("customer_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("customer_name")),
                cursor.getString(cursor.getColumnIndexOrThrow("customer_phone")),
                cursor.getString(cursor.getColumnIndexOrThrow("pickup_location")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("pickup_latitude")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("pickup_longitude")),
                cursor.getString(cursor.getColumnIndexOrThrow("destination")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("destination_latitude")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("destination_longitude")),
                cursor.getString(cursor.getColumnIndexOrThrow("special_instructions")),
                cursor.getString(cursor.getColumnIndexOrThrow("package_details")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("original_amount")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("current_total")),
                cursor.getDouble(cursor.getColumnIndexOrThrow("deposit_amount")),
                cursor.getString(cursor.getColumnIndexOrThrow("deposit_status")),
                cursor.getString(cursor.getColumnIndexOrThrow("status")),
                cursor.getInt(cursor.getColumnIndexOrThrow("sequence_number")),
                cursor.getString(cursor.getColumnIndexOrThrow("scheduled_time")),
                cursor.getString(cursor.getColumnIndexOrThrow("priority")),
                cursor.getString(cursor.getColumnIndexOrThrow("created_at")),
                cursor.getString(cursor.getColumnIndexOrThrow("updated_at"))
        );
    }

    // -------------------------------------------------------------
    // AGENT BOOKING DATABASE METHODS
    // -------------------------------------------------------------

    // Add a New Agent Booking
    public boolean addBooking(Delivery delivery) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("booking_id", delivery.getBookingId());
        cv.put("driver_id", delivery.getDriverId() != null ? delivery.getDriverId() : "agent01");
        cv.put("customer_id", delivery.getCustomerId() != null ? delivery.getCustomerId() : "CUST-" + (System.currentTimeMillis() % 1000));
        cv.put("customer_name", delivery.getCustomerName());
        cv.put("customer_phone", delivery.getCustomerPhone());
        cv.put("pickup_location", delivery.getPickupLocation() != null ? delivery.getPickupLocation() : "TRI-MAX Hub, San Jose del Monte");
        cv.put("pickup_latitude", delivery.getPickupLatitude() != 0 ? delivery.getPickupLatitude() : 14.8115);
        cv.put("pickup_longitude", delivery.getPickupLongitude() != 0 ? delivery.getPickupLongitude() : 121.0453);
        cv.put("destination", delivery.getDestination() != null ? delivery.getDestination() : "San Jose del Monte, Bulacan");
        cv.put("destination_latitude", delivery.getDestinationLatitude() != 0 ? delivery.getDestinationLatitude() : 14.8150);
        cv.put("destination_longitude", delivery.getDestinationLongitude() != 0 ? delivery.getDestinationLongitude() : 121.0500);
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

    // Generate Next Booking ID (e.g. BK-2026-00124)
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

    // Get All Bookings for Agent (sorted newest first)
    public List<Delivery> getAllBookings() {
        return getBookingsByFilter("ALL", null);
    }

    // Filter Bookings by status and query string
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

    // Get Booking Count by Status
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

    // Cancel Booking
    public boolean cancelBooking(String bookingId) {
        return updateDeliveryStatus(bookingId, "CANCELLED");
    }

    // Confirm Booking
    public boolean confirmBooking(String bookingId) {
        return updateDeliveryStatus(bookingId, "CONFIRMED");
    }

    // -------------------------------------------------------------
    // USER ACCOUNT MANAGEMENT & PROFILE UPDATES
    // -------------------------------------------------------------

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
                if (existingPwd != null && existingPwd.equals(currentPassword)) {
                    validCurrent = true;
                }
            }
            cursor.close();
        }

        if (!validCurrent) {
            return false;
        }

        ContentValues cv = new ContentValues();
        cv.put("password", newPassword);
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
        cv.put("password", newPassword);
        int rows = db.update(TABLE_USERS, cv, "LOWER(email)=? OR LOWER(username)=?",
                new String[]{email.trim().toLowerCase(), email.trim().toLowerCase()});
        return rows > 0;
    }

    // -------------------------------------------------------------
    // NOTIFICATION MANAGEMENT
    // -------------------------------------------------------------

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

    // -------------------------------------------------------------
    // CLIENT MANAGEMENT
    // -------------------------------------------------------------

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

    // -------------------------------------------------------------
    // AGENT ATTENDANCE & START DUTY MANAGEMENT
    // -------------------------------------------------------------

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
