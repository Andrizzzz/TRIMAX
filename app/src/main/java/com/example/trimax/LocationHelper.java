package com.example.trimax;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LocationHelper {

    public interface OnLocationUpdateListener {
        void onLocationUpdated(double latitude, double longitude, float accuracy, float speed, float bearing);
    }

    private final Context context;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private OnLocationUpdateListener updateListener;

    // Zero means no real location fix has been received yet; never expose a fake GPS coordinate.
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private float currentAccuracy = 0.0f;
    private float currentSpeed = 0.0f;
    private float currentBearing = 0.0f;
    private long lastTimestamp = System.currentTimeMillis();

    private boolean isTrackingActive = false;
    private boolean hasValidLocationFix = false;

    public LocationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
    }

    public boolean hasLocationPermission() {
        boolean fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine || coarse;
    }

    public void startLocationUpdates(OnLocationUpdateListener listener) {
        // Reuse a single listener even when multiple lifecycle callbacks request tracking.
        stopLocationUpdates();
        this.updateListener = listener;
        if (!hasLocationPermission()) {
            return;
        }

        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        if (locationManager == null) return;

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();
                    currentAccuracy = location.getAccuracy();
                    currentSpeed = location.getSpeed();
                    currentBearing = location.getBearing();
                    lastTimestamp = location.getTime();
                    hasValidLocationFix = true;

                    if (updateListener != null) {
                        updateListener.onLocationUpdated(currentLatitude, currentLongitude, currentAccuracy, currentSpeed, currentBearing);
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 5, locationListener);
            }
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationListener);
            }
            isTrackingActive = true;

            // Fetch last known location as initial value
            Location lastGps = null;
            Location lastNet = null;
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
            } catch (SecurityException ignored) {
                // Permission may have been revoked while this screen was open.
            }
            Location best = lastGps;
            if (lastNet != null && (best == null || lastNet.getTime() > best.getTime())) best = lastNet;
            // Avoid using an old cached position as if it were the current location.
            if (best != null && System.currentTimeMillis() - best.getTime() <= 10 * 60 * 1000L) {
                currentLatitude = best.getLatitude();
                currentLongitude = best.getLongitude();
                currentAccuracy = best.getAccuracy();
                currentSpeed = best.getSpeed();
                currentBearing = best.getBearing();
                lastTimestamp = best.getTime();
                hasValidLocationFix = true;
            } else if (System.currentTimeMillis() - lastTimestamp > 10 * 60 * 1000L) {
                // Do not keep reporting a previously valid but now stale fix as current.
                currentLatitude = 0.0;
                currentLongitude = 0.0;
                currentAccuracy = 0.0f;
                hasValidLocationFix = false;
            }
        } catch (SecurityException ignored) {
            isTrackingActive = false;
        }
    }

    public void stopLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException ignored) {}
        }
        isTrackingActive = false;
        locationListener = null;
        updateListener = null;
    }

    public double getLatitude() {
        return currentLatitude;
    }

    public double getLongitude() {
        return currentLongitude;
    }

    public float getAccuracy() {
        return currentAccuracy;
    }

    public float getSpeed() {
        return currentSpeed;
    }

    public float getBearing() {
        return currentBearing;
    }

    public long getLastTimestamp() {
        return lastTimestamp;
    }

    public String getFormattedLastTimestamp() {
        if (!hasValidLocationFix) return "Awaiting GPS fix";
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        return "Updated at " + sdf.format(new Date(lastTimestamp));
    }

    public boolean isTrackingActive() {
        return isTrackingActive;
    }

    public boolean hasValidLocationFix() {
        return hasValidLocationFix;
    }

    private long lastRemotePushTimestamp = 0;
    private double lastRemoteLat = 0.0;
    private double lastRemoteLng = 0.0;
    private static final long MIN_REMOTE_PUSH_INTERVAL_MS = 10_000L; // 10 seconds

    public void pushLocationToSupabaseIfOnline(Context context, boolean isOnDuty) {
        if (!hasValidLocationFix || currentLatitude == 0 || currentLongitude == 0) return;

        long now = System.currentTimeMillis();
        double distMeters = distanceMeters(lastRemoteLat, lastRemoteLng, currentLatitude, currentLongitude);

        if (now - lastRemotePushTimestamp >= MIN_REMOTE_PUSH_INTERVAL_MS || distMeters >= 15.0) {
            lastRemotePushTimestamp = now;
            lastRemoteLat = currentLatitude;
            lastRemoteLng = currentLongitude;

            if (TrimaxApi.get(context).hasSession()) {
                TrimaxApi.get(context).updateLiveLocation(
                        currentLatitude,
                        currentLongitude,
                        currentAccuracy,
                        currentSpeed,
                        currentBearing,
                        isOnDuty,
                        new TrimaxApi.Callback<Boolean>() {
                            @Override public void onSuccess(Boolean result) {}
                            @Override public void onError(ApiException error) {
                                // Silent retry on next GPS fix
                            }
                        }
                );
            }
        }
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == 0 && lon1 == 0) return 999999.0;
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public void updateSimulatedLocation(double lat, double lng, float accuracy) {
        this.currentLatitude = lat;
        this.currentLongitude = lng;
        this.currentAccuracy = accuracy;
        this.lastTimestamp = System.currentTimeMillis();
        this.hasValidLocationFix = true;
        if (updateListener != null) {
            updateListener.onLocationUpdated(lat, lng, accuracy, currentSpeed, currentBearing);
        }
    }
}
