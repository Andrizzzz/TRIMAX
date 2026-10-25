package com.example.trimax;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

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
                return MobileAccount.parse(client.rpc("trimax_my_account", new JSONObject()));
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
