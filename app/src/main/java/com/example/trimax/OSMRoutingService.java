package com.example.trimax;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OSMRoutingService implements RoutingService {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void calculateRoute(GeoPoint startPoint, GeoPoint endPoint, RouteCallback callback) {
        if (startPoint == null || endPoint == null) {
            if (callback != null) callback.onRouteFailed("Start or end point is null.");
            return;
        }

        executorService.execute(() -> {
            try {
                // Public OSRM API endpoint for routing
                String urlString = String.format(Locale.US,
                        "https://router.project-osrm.org/route/v1/driving/%.6f,%.6f;%.6f,%.6f?overview=full&geometries=geojson",
                        startPoint.getLongitude(), startPoint.getLatitude(),
                        endPoint.getLongitude(), endPoint.getLatitude());

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "TRIMAX-Logistics-DeliveryApp/1.0 (Android; contact: support@trimax.app)");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray routes = jsonResponse.optJSONArray("routes");
                    if (routes != null && routes.length() > 0) {
                        JSONObject route = routes.getJSONObject(0);
                        double distanceMeters = route.optDouble("distance", 0);
                        double durationSeconds = route.optDouble("duration", 0);

                        JSONObject geometry = route.optJSONObject("geometry");
                        List<GeoPoint> pointList = new ArrayList<>();

                        if (geometry != null) {
                            JSONArray coordinates = geometry.optJSONArray("coordinates");
                            if (coordinates != null) {
                                for (int i = 0; i < coordinates.length(); i++) {
                                    JSONArray coord = coordinates.getJSONArray(i);
                                    double lng = coord.getDouble(0);
                                    double lat = coord.getDouble(1);
                                    pointList.add(new GeoPoint(lat, lng));
                                }
                            }
                        }

                        if (pointList.isEmpty()) {
                            pointList.add(startPoint);
                            pointList.add(endPoint);
                        }

                        double distanceKm = distanceMeters / 1000.0;
                        double durationMin = durationSeconds / 60.0;

                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onRouteCalculated(pointList, distanceKm, durationMin, true);
                            }
                        });
                        return;
                    }
                }
            } catch (Exception ignored) {
                // Fallback to geodesic estimation if OSRM is offline / timed out
            }

            // Fallback estimation using GeoPoint distance
            double distanceMeters = startPoint.distanceToAsDouble(endPoint);
            double distanceKm = distanceMeters / 1000.0;
            // Estimated speed in city traffic: 25 km/h
            double durationMinutes = (distanceKm / 25.0) * 60.0;

            List<GeoPoint> fallbackPoints = new ArrayList<>();
            fallbackPoints.add(startPoint);
            fallbackPoints.add(endPoint);

            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onRouteCalculated(fallbackPoints, distanceKm, durationMinutes, false);
                }
            });
        });
    }
}
