package com.example.trimax;

import android.app.Activity;
import android.content.Intent;

/** Wires the driver screens' liquid glass bottom navigation. */
final class DriverNav {

    private DriverNav() {}

    /**
     * Finds the {@code bottomNavigation} bar in the activity's layout, marks the current tab
     * and opens the other driver screens when their tab is tapped.
     */
    static void attach(Activity activity, int currentItemId) {
        LiquidGlassNavBar bar = activity.findViewById(R.id.bottomNavigation);
        if (bar == null) return;

        bar.setSelectedItemId(currentItemId);
        bar.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == currentItemId) return true;

            Class<?> target = targetFor(id);
            if (target == null) return false;

            Intent intent = new Intent(activity, target);
            // Tabs never pile up: going to a tab that is already open returns to it and
            // closes the screens above it. The dashboard keeps its state (map, duty toggle).
            intent.addFlags(id == R.id.nav_driver_home
                    ? Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    : Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
            return true;
        });
    }

    private static Class<?> targetFor(int itemId) {
        if (itemId == R.id.nav_driver_home) return DriverDashboardActivity.class;
        if (itemId == R.id.nav_driver_deliveries) return DriverDeliveriesActivity.class;
        if (itemId == R.id.nav_driver_history) return DriverHistoryActivity.class;
        if (itemId == R.id.nav_driver_notifications) return DriverNotificationsActivity.class;
        if (itemId == R.id.nav_driver_profile) return DriverProfileActivity.class;
        return null;
    }
}
