package com.example.trimax;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The app's only way to the shared TRIMAX data. Every call runs off the main thread and reports back on
 * the main thread. Identity is never sent by the app: the server works out who is calling from the
 * sign-in token, and checks role, ownership and allowed status changes itself.
 */
public final class TrimaxApi {

    public interface Callback<T> {
        void onSuccess(T result);

        void onError(ApiException error);
    }

    private interface Call<T> {
        T run() throws Exception;
    }

    private static TrimaxApi instance;

    private final SupabaseClient client;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());

    private TrimaxApi(Context context) {
        client = new SupabaseClient(SupabaseConfig.PROJECT_URL, SupabaseConfig.PUBLISHABLE_KEY,
                new AuthSessionStore(context), null);
    }

    public static synchronized TrimaxApi get(Context context) {
        if (instance == null) instance = new TrimaxApi(context.getApplicationContext());
        return instance;
    }

    public boolean hasSession() {
        return client.hasSession();
    }

    // ------------------------------------------------------------------ account

    /** Signs in with the email and password of the Supabase sign-in, then loads the TRIMAX account. */
    public void signIn(String email, String password, Callback<MobileAccount> cb) {
        run(() -> {
            client.signIn(email, password);
            try {
                MobileAccount account = MobileAccount.parse(client.rpc("trimax_my_account", new JSONObject()));
                account.mustChangePassword = client.lastSignInMustChangePassword();
                return account;
            } catch (Exception e) {
                client.signOut();          // signed in to Supabase, but not allowed into TRIMAX: drop the tokens
                throw e;
            }
        }, cb);
    }

    public void loadMyAccount(Callback<MobileAccount> cb) {
        run(() -> MobileAccount.parse(client.rpc("trimax_my_account", new JSONObject())), cb);
    }

    /** Checks the current password by signing in with it, then sets the new one. */
    public void changePassword(String email, String currentPassword, String newPassword, Callback<Boolean> cb) {
        run(() -> {
            try {
                client.signIn(email, currentPassword);
            } catch (ApiException e) {
                if (e.getKind() == ApiException.Kind.BAD_CREDENTIALS) {
                    throw new ApiException(ApiException.Kind.BAD_CREDENTIALS, "Your current password is not correct.");
                }
                throw e;
            }
            client.updatePassword(newPassword);
            return Boolean.TRUE;
        }, cb);
    }

    /** Sets the password with the one-time code from the TRIMAX email (first sign-in or forgotten password). */
    public void setPasswordWithCode(String email, String code, String newPassword, Callback<Boolean> cb) {
        run(() -> {
            String problem = PasswordSetupForm.problem(email, code, newPassword, newPassword);
            if (problem != null) throw new ApiException(ApiException.Kind.INVALID, problem);
            client.setPasswordWithCode(email.trim(), PasswordSetupForm.normalizeCode(code), newPassword);
            return Boolean.TRUE;
        }, cb);
    }

    /** Signs out on this phone immediately and tells the server in the background. */
    public void signOut() {
        AuthSession previous = client.clearLocalSession();
        if (previous != null) executor.execute(() -> client.revokeOnServer(previous));
    }

    // ------------------------------------------------------------------ agent

    public void listProducts(Callback<List<Product>> cb) {
        run(() -> Product.parseList(client.rpc("trimax_list_products", new JSONObject())), cb);
    }

    public void createBooking(CreateBookingRequest request, Callback<BookingRecord> cb) {
        run(() -> {
            String problem = request.validate();
            if (problem != null) throw new ApiException(ApiException.Kind.INVALID, problem);
            return BookingRecord.parse(client.rpc("trimax_agent_create_booking", request.toRpcArgs()));
        }, cb);
    }

    public void listMyBookings(Callback<List<BookingRecord>> cb) {
        run(() -> {
            JSONObject args = new JSONObject();
            args.put("p_limit", 200);
            args.put("p_offset", 0);
            return BookingRecord.parseList(client.rpc("trimax_agent_list_bookings", args));
        }, cb);
    }

    public void getMyBooking(int bookingId, Callback<BookingRecord> cb) {
        run(() -> {
            JSONObject args = new JSONObject();
            args.put("p_booking_id", bookingId);
            return BookingRecord.parse(client.rpc("trimax_agent_get_booking", args));
        }, cb);
    }

    // ------------------------------------------------------------------ agent: customers with a photo

    /**
     * Saves a customer on the server. A new customer needs a photo: the JPEG is uploaded first into this
     * agent's own folder of the private customer-photos bucket, then trimax_agent_save_customer records it
     * (the server checks that the upload is really there). jpeg may be null to update an existing
     * customer without a new photo.
     */
    public void saveCustomer(String name, String contact, String address, double latitude, double longitude,
                             byte[] jpeg, Callback<ServerCustomer> cb) {
        run(() -> {
            String photoPath = null;
            if (jpeg != null) {
                String uid = client.currentUserId();
                if (uid == null) throw ApiException.sessionExpired();
                photoPath = uid + "/" + CustomerPhotos.newObjectName();
                client.uploadJpeg("customer-photos", photoPath, jpeg);
            }
            JSONObject args = new JSONObject();
            args.put("p_name", name == null ? "" : name.trim());
            args.put("p_contact", contact == null ? "" : contact.trim());
            args.put("p_address", address == null ? "" : address.trim());
            args.put("p_latitude", latitude);
            args.put("p_longitude", longitude);
            args.put("p_photo_path", photoPath != null ? photoPath : JSONObject.NULL);
            return ServerCustomer.parse(client.rpc("trimax_agent_save_customer", args));
        }, cb);
    }

    /** This agent's customers on the server, with their photo paths. */
    public void listCustomers(Callback<List<ServerCustomer>> cb) {
        run(() -> ServerCustomer.parseList(client.rpc("trimax_agent_list_customers", new JSONObject())), cb);
    }

    /** Downloads a customer photo this agent took (own folder only). */
    public void downloadCustomerPhoto(String photoPath, Callback<byte[]> cb) {
        run(() -> client.downloadJpeg("customer-photos", photoPath), cb);
    }

    // ------------------------------------------------------------------ agent: discounts

    /** Per item: may it be discounted, and the most per piece (never the puhunan itself). */
    public void discountQuote(List<CreateBookingRequest.Line> lines, Callback<DiscountQuote> cb) {
        run(() -> {
            JSONArray items = new JSONArray();
            for (CreateBookingRequest.Line l : lines) {
                JSONObject i = new JSONObject();
                i.put("product_id", l.productId);
                i.put("quantity", l.quantity);
                items.put(i);
            }
            JSONObject args = new JSONObject();
            args.put("p_items", items);
            return DiscountQuote.parse(client.rpc("trimax_agent_discount_quote", args));
        }, cb);
    }

    /**
     * Sets the discount per piece of each item of one of this agent's Pending bookings
     * ({product id -> pesos off one piece}; items not in the map go back to full price; empty = none).
     */
    public void setItemDiscounts(int bookingId, java.util.Map<Integer, java.math.BigDecimal> perUnit,
                                 Callback<BookingRecord> cb) {
        run(() -> {
            JSONArray items = new JSONArray();
            for (java.util.Map.Entry<Integer, java.math.BigDecimal> e : ItemDiscounts.nonZero(perUnit).entrySet()) {
                JSONObject i = new JSONObject();
                i.put("product_id", e.getKey());
                i.put("discount_per_unit", e.getValue().toPlainString());
                items.put(i);
            }
            JSONObject args = new JSONObject();
            args.put("p_booking_id", bookingId);
            args.put("p_items", items);
            return BookingRecord.parse(client.rpc("trimax_agent_set_item_discounts", args));
        }, cb);
    }

    // ------------------------------------------------------------------ driver

    /** @param scope "active" (to do, in working order) or "history" (delivered, failed, completed, cancelled) */
    public void listDriverDeliveries(String scope, Callback<List<BookingRecord>> cb) {
        run(() -> {
            JSONObject args = new JSONObject();
            args.put("p_scope", scope);
            return BookingRecord.parseList(client.rpc("trimax_driver_list_deliveries", args));
        }, cb);
    }

    public void getDriverDelivery(int bookingId, Callback<BookingRecord> cb) {
        run(() -> {
            JSONObject args = new JSONObject();
            args.put("p_booking_id", bookingId);
            return BookingRecord.parse(client.rpc("trimax_driver_get_delivery", args));
        }, cb);
    }

    /**
     * Moves a delivery one step. expected is the status the driver is looking at; if the server has
     * something else, nothing changes and the error is CONFLICT. The result is what the server saved.
     */
    public void updateDeliveryStatus(int bookingId, String expected, String next, Double latitude, Double longitude,
                                     String note, String recipientName, Callback<BookingRecord> cb) {
        run(() -> {
            JSONObject args = new JSONObject();
            args.put("p_booking_id", bookingId);
            args.put("p_expected_status", expected);
            args.put("p_new_status", next);
            boolean hasFix = latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)
                    && !(latitude == 0 && longitude == 0);
            args.put("p_latitude", hasFix ? latitude : JSONObject.NULL);
            args.put("p_longitude", hasFix ? longitude : JSONObject.NULL);
            args.put("p_note", note != null && !note.trim().isEmpty() ? note.trim() : JSONObject.NULL);
            args.put("p_recipient_name", recipientName != null && !recipientName.trim().isEmpty() ? recipientName.trim() : JSONObject.NULL);
            return BookingRecord.parse(client.rpc("trimax_driver_update_delivery_status", args));
        }, cb);
    }

    // ------------------------------------------------------------------ live location

    /**
     * Pushes live GPS coordinates to Supabase for the signed-in Agent or Driver.
     * The server updates user_locations for Admin map tracking.
     */
    public void updateLiveLocation(double latitude, double longitude, Float accuracy, Float speed, Float bearing,
                                   boolean isOnDuty, Callback<Boolean> cb) {
        run(() -> {
            boolean hasFix = Double.isFinite(latitude) && Double.isFinite(longitude) && !(latitude == 0 && longitude == 0);
            if (!hasFix) {
                throw new ApiException(ApiException.Kind.INVALID, "Invalid GPS fix");
            }
            JSONObject args = new JSONObject();
            args.put("p_latitude", latitude);
            args.put("p_longitude", longitude);
            args.put("p_accuracy", accuracy != null ? accuracy : JSONObject.NULL);
            args.put("p_speed", speed != null ? speed : JSONObject.NULL);
            args.put("p_bearing", bearing != null ? bearing : JSONObject.NULL);
            args.put("p_is_on_duty", isOnDuty);
            client.rpc("trimax_update_location", args);
            return Boolean.TRUE;
        }, cb);
    }

    // ------------------------------------------------------------------ driver: assigned vehicle

    /**
     * The signed-in driver's assigned vehicle(s) as the server's JSON
     * ({"vehicles":[{"plate_number","vehicle_type","status"}],"checked_at":...}); parse with
     * {@link DriverVehicle#parse(String)}. Raw JSON so the screen can keep the last answer for offline use.
     */
    public void loadMyVehicle(Callback<String> cb) {
        run(() -> {
            String json = client.rpc("trimax_driver_my_vehicle", new JSONObject());
            DriverVehicle.parse(json);   // fail here (not on screen) if the answer is not what we expect
            return json;
        }, cb);
    }

    // ------------------------------------------------------------------ duty (attendance)

    /** Whether the server has the signed-in person on duty (false when they never started one). */
    public void loadDutyStatus(Callback<Boolean> cb) {
        run(() -> new JSONObject(client.rpc("trimax_my_duty_status", new JSONObject())).optBoolean("on_duty", false), cb);
    }

    /**
     * Starts or ends duty on the server: uploads the attendance photo to the private duty-photos
     * bucket (into the user's own folder), then records the event so the admin sees it.
     * The result is whether the server now has this person on duty.
     */
    public void recordDutyEvent(boolean start, byte[] jpeg, Double latitude, Double longitude, Callback<Boolean> cb) {
        run(() -> {
            String uid = client.currentUserId();
            if (uid == null) throw ApiException.sessionExpired();
            String name = (start ? "start_" : "end_") + System.currentTimeMillis() + "_"
                    + java.util.UUID.randomUUID().toString().substring(0, 8) + ".jpg";
            String objectPath = uid + "/" + name;
            client.uploadJpeg("duty-photos", objectPath, jpeg);

            boolean hasFix = latitude != null && longitude != null && Double.isFinite(latitude)
                    && Double.isFinite(longitude) && !(latitude == 0 && longitude == 0);
            JSONObject args = new JSONObject();
            args.put("p_event", start ? "START" : "END");
            args.put("p_photo_path", objectPath);
            args.put("p_latitude", hasFix ? latitude : JSONObject.NULL);
            args.put("p_longitude", hasFix ? longitude : JSONObject.NULL);
            args.put("p_client_event_id", java.util.UUID.randomUUID().toString());
            return new JSONObject(client.rpc("trimax_record_duty_event", args)).optBoolean("on_duty", start);
        }, cb);
    }

    // ------------------------------------------------------------------ delivery proof photos

    /**
     * Uploads a drop-off photo into the private delivery-proofs bucket (the driver's own folder) and
     * attaches it to the booking (public.trimax_driver_attach_proof). objectName and requestId stay the
     * same on every retry, so a retry never stores the photo twice. The result is the server's proof id.
     */
    public void attachDeliveryProof(int bookingId, java.io.File photo, String objectName, String recipientName,
                                    Double latitude, Double longitude, String takenAtIso, String requestId,
                                    Callback<Long> cb) {
        run(() -> {
            byte[] jpeg = DutySync.readJpeg(photo, null);      // read here, off the main thread
            if (jpeg == null) throw new ApiException(ApiException.Kind.INVALID, "The photo is no longer on this phone.");
            String uid = client.currentUserId();
            if (uid == null) throw ApiException.sessionExpired();
            String objectPath = uid + "/" + objectName;
            client.uploadJpeg("delivery-proofs", objectPath, jpeg);

            boolean hasFix = latitude != null && longitude != null && Double.isFinite(latitude)
                    && Double.isFinite(longitude) && !(latitude == 0 && longitude == 0);
            JSONObject args = new JSONObject();
            args.put("p_booking_id", bookingId);
            args.put("p_photo_path", objectPath);
            args.put("p_recipient_name", recipientName != null && !recipientName.trim().isEmpty()
                    ? recipientName.trim() : JSONObject.NULL);
            args.put("p_latitude", hasFix ? latitude : JSONObject.NULL);
            args.put("p_longitude", hasFix ? longitude : JSONObject.NULL);
            args.put("p_taken_at", takenAtIso != null ? takenAtIso : JSONObject.NULL);
            args.put("p_client_request_id", requestId);
            return new JSONObject(client.rpc("trimax_driver_attach_proof", args)).getLong("proof_id");
        }, cb);
    }

    // ------------------------------------------------------------------ SOS / problem reports

    /**
     * Sends an SOS or a problem report to the admin's Notifications (public.trimax_report_issue).
     * requestId makes retries safe: sending the same id twice never creates a second alert.
     * The result is the server's notification id.
     */
    public void reportIssue(boolean sos, String message, Integer bookingId, Double latitude, Double longitude,
                            String requestId, Callback<Long> cb) {
        run(() -> {
            boolean hasFix = latitude != null && longitude != null && Double.isFinite(latitude)
                    && Double.isFinite(longitude) && !(latitude == 0 && longitude == 0);
            JSONObject args = new JSONObject();
            args.put("p_kind", sos ? "SOS" : "PROBLEM");
            args.put("p_message", message == null ? "" : message.trim());
            args.put("p_booking_id", bookingId != null ? bookingId : JSONObject.NULL);
            args.put("p_latitude", hasFix ? latitude : JSONObject.NULL);
            args.put("p_longitude", hasFix ? longitude : JSONObject.NULL);
            args.put("p_client_request_id", requestId);
            return new JSONObject(client.rpc("trimax_report_issue", args)).getLong("notification_id");
        }, cb);
    }

    // ------------------------------------------------------------------ plumbing

    private <T> void run(Call<T> call, Callback<T> cb) {
        executor.execute(() -> {
            try {
                T result = call.run();
                main.post(() -> cb.onSuccess(result));
            } catch (ApiException e) {
                main.post(() -> cb.onError(e));
            } catch (Exception e) {
                ApiException unexpected = new ApiException(ApiException.Kind.SERVER,
                        "Unexpected response from the server. Please try again.");
                main.post(() -> cb.onError(unexpected));
            }
        });
    }
}
