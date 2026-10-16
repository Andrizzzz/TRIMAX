package com.example.trimax;

import org.osmdroid.util.GeoPoint;
import java.util.List;

public interface RoutingService {

    interface RouteCallback {
        void onRouteCalculated(List<GeoPoint> routePoints, double distanceKm, double durationMinutes, boolean isRealRoute);
        void onRouteFailed(String errorMessage);
    }

    void calculateRoute(GeoPoint startPoint, GeoPoint endPoint, RouteCallback callback);
}
