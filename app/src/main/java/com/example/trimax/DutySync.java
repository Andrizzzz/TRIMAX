package com.example.trimax;

import android.app.Activity;
import android.graphics.Bitmap;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

/**
 * Duty (attendance) shared by the agent and driver dashboards.
 *
 * The server is the source of truth: a start or end is uploaded (photo + event) first, and the phone
 * switches state only once the server has accepted it, so the admin always sees who is on duty.
 */
final class DutySync {

    interface Listener {
        /** The server accepted the change (or already had that state). */
        void onDutyChanged(boolean onDuty);

        /** Called once the request is over, success or not (re-enable buttons here). */
        void onFinished();
    }

    interface StateListener {
        void onServerState(boolean onDuty);
    }

    private DutySync() {}

    /** How long a start/end waits for a first GPS position (GPS was started when the button was tapped). */
    private static final long GPS_WAIT_MS = 8000;

    static void record(Activity activity, boolean start, File photo, Bitmap fallback,
                       LocationHelper location, Listener listener) {
        byte[] jpeg = readJpeg(photo, fallback);
        if (jpeg == null) {
            Toast.makeText(activity, "Could not read the attendance photo. Take it again.", Toast.LENGTH_LONG).show();
            listener.onFinished();
            return;
        }
        // The admin's Attendance screen shows where the person was: give the GPS a few seconds if it is
        // running but has no position yet. Without permission or GPS, the record is sent without one.
        if (location != null && !location.hasValidLocationFix() && location.isTrackingActive()) {
            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            long until = android.os.SystemClock.uptimeMillis() + GPS_WAIT_MS;
            Runnable poll = new Runnable() {
                @Override
                public void run() {
                    if (SessionGuard.isGone(activity)) return;
                    if (location.hasValidLocationFix() || android.os.SystemClock.uptimeMillis() >= until) {
                        send(activity, start, jpeg, location, listener);
                    } else {
                        main.postDelayed(this, 400);
                    }
                }
            };
            main.postDelayed(poll, 400);
            return;
        }
        send(activity, start, jpeg, location, listener);
    }

    private static void send(Activity activity, boolean start, byte[] jpeg, LocationHelper location, Listener listener) {
        boolean fix = location != null && location.hasValidLocationFix();
        Double lat = fix ? location.getLatitude() : null;
        Double lng = fix ? location.getLongitude() : null;

        TrimaxApi.get(activity).recordDutyEvent(start, jpeg, lat, lng, new TrimaxApi.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean onDuty) {
                if (SessionGuard.isGone(activity)) return;
                listener.onFinished();
                listener.onDutyChanged(start);
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(activity)) return;
                listener.onFinished();
                if (SessionGuard.handle(activity, error)) return;
                // The server already has the state that was asked for: just catch up.
                if (error.getKind() == ApiException.Kind.CONFLICT && error.getMessage() != null) {
                    String m = error.getMessage().toLowerCase(Locale.ROOT);
                    if ((start && m.contains("already on duty")) || (!start && m.contains("not on duty"))) {
                        listener.onDutyChanged(start);
                        return;
                    }
                }
                Toast.makeText(activity, (start ? "Could not start duty: " : "Could not end duty: ")
                        + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Asks the server whether this person is on duty; silent when offline. */
    static void loadServerState(Activity activity, StateListener listener) {
        TrimaxApi.get(activity).loadDutyStatus(new TrimaxApi.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean onDuty) {
                if (SessionGuard.isGone(activity)) return;
                listener.onServerState(Boolean.TRUE.equals(onDuty));
            }

            @Override
            public void onError(ApiException error) {
                // Offline or the server is busy: keep what the phone has.
            }
        });
    }

    static byte[] readJpeg(File file, Bitmap fallback) {
        try {
            if (file != null && file.isFile() && file.length() > 0 && file.length() < 8_000_000) {
                byte[] data = new byte[(int) file.length()];
                try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
                    in.readFully(data);
                }
                return data;
            }
        } catch (Exception ignored) {
            // fall back to the in-memory photo
        }
        if (fallback == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        fallback.compress(Bitmap.CompressFormat.JPEG, 90, out);
        return out.size() > 0 ? out.toByteArray() : null;
    }
}
