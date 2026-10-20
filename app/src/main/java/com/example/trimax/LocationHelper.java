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

    private double currentLatitude = 14.8115;
    private double currentLongitude = 121.0453;
    private float currentAccuracy = 5.0f;
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
            Location lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location lastNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location best = lastGps != null ? lastGps : lastNet;
            if (best != null) {
                currentLatitude = best.getLatitude();
                currentLongitude = best.getLongitude();
                currentAccuracy = best.getAccuracy();
                currentSpeed = best.getSpeed();
                currentBearing = best.getBearing();
                lastTimestamp = best.getTime();
                hasValidLocationFix = true;
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
