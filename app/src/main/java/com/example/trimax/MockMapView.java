package com.example.trimax;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MockMapView extends FrameLayout {

    private static final String TAG = "TRIMAX_MAP_TILE";

    // Official OpenStreetMap Germany Tile Server (FOSSGIS e.V. - Standard OSM tiles, 100% permitted open usage, no 403 blocks)
    public static final OnlineTileSourceBase OSM_GERMANY = new XYTileSource(
            "OSM-Germany",
            0, 19, 256, ".png",
            new String[]{
                    "https://a.tile.openstreetmap.de/",
                    "https://b.tile.openstreetmap.de/",
                    "https://c.tile.openstreetmap.de/"
            },
            "© OpenStreetMap contributors"
    );

    // Official OpenStreetMap France Tile Server (OSM France - Standard OSM tiles fallback)
    public static final OnlineTileSourceBase OSM_FRANCE = new XYTileSource(
            "OSM-France",
            0, 19, 256, ".png",
            new String[]{
                    "https://a.tile.openstreetmap.fr/osmfr/",
                    "https://b.tile.openstreetmap.fr/osmfr/",
                    "https://c.tile.openstreetmap.fr/osmfr/"
            },
            "© OpenStreetMap contributors, OSM France"
    );

    public interface OnLocationChangeListener {
        void onLocationSelected(String address, String distanceText);
    }

    private OnLocationChangeListener locationChangeListener;

    private MapView mapView;
    private Marker hubMarker;
    private Marker riderMarker;
    private Marker customerMarker;
    private Polyline routePolyline;

    private OSMRoutingService routingService;

    private Handler liveAnimationHandler;
    private Runnable liveAnimationRunnable;
    private int animationStep = 0;

    private int sampleLocationIndex = 0;
    private boolean hasExplicitDestinationSelection = false;
    private boolean isProgrammaticMove = false;

    private static final GeoPoint RIDER_POINT = new GeoPoint(14.7826996, 121.0475865); // Fixed TRIMAX Hub

    private final GeoPoint[] sampleGeoPoints = {
            new GeoPoint(14.8150, 121.0500), // Tungkong Mangga
            new GeoPoint(14.8200, 121.0550), // Muzon Crossroad
            new GeoPoint(14.8250, 121.0600), // Gaya-gaya Main
            new GeoPoint(14.8100, 121.0400), // Poblacion I
            new GeoPoint(14.8120, 121.0460)  // Graceville
    };

    private final String[] sampleAddresses = {
            "Tungkong Mangga, San Jose del Monte, Bulacan",
            "Muzon Central Crossroad, San Jose del Monte, Bulacan",
            "Gaya-gaya Main Highway, San Jose del Monte, Bulacan",
            "Poblacion I, San Jose del Monte, Bulacan",
            "Graceville Subdivision, San Jose del Monte, Bulacan"
    };

    private final String[] sampleDistances = {
            "1.8 km (approx. 6 mins)",
            "3.2 km (approx. 10 mins)",
            "2.5 km (approx. 8 mins)",
            "4.1 km (approx. 12 mins)",
            "0.9 km (approx. 3 mins)"
    };

    public MockMapView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MockMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MockMapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Configure OSMDroid settings complying with OpenStreetMap Tile Usage Policy
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        
        // Set a valid, unique User-Agent identifying the application
        String descriptiveUserAgent = "TRIMAX-DeliveryApp/1.0 (Android; com.example.trimax; contact: support@trimax.com)";
        Configuration.getInstance().setUserAgentValue(descriptiveUserAgent);

        // Configure Cache Directory safely inside application cache
        File osmdroidCache = new File(context.getCacheDir(), "osmdroid");
        File osmdroidTileCache = new File(context.getCacheDir(), "osmdroid/tiles");

        Configuration.getInstance().setOsmdroidBasePath(osmdroidCache);
        Configuration.getInstance().setOsmdroidTileCache(osmdroidTileCache);

        // Disable debug overlays
        Configuration.getInstance().setDebugMode(false);
        Configuration.getInstance().setDebugTileProviders(false);

        mapView = new MapView(context);
        // Use OpenStreetMap Germany (FOSSGIS e.V.) tile server: high quality, standard OSM tiles, no 403 blocks
        mapView.setTileSource(OSM_GERMANY);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(RIDER_POINT);

        addView(mapView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Add OpenStreetMap Copyright / Attribution Overlay (Single bottom-corner label)
        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(context);
        mapView.getOverlays().add(copyrightOverlay);

        routingService = new OSMRoutingService();

        // Setup Fixed TRIMAX Hub Marker (STATIC - NEVER MOVES)
        hubMarker = new Marker(mapView);
        hubMarker.setPosition(RIDER_POINT);
        hubMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        hubMarker.setTitle("TRIMAX Hub — Fixed Pickup Location");
        mapView.getOverlays().add(hubMarker);

        // Setup Live Driver / GPS Marker (Hidden by default on booking maps)
        riderMarker = new Marker(mapView);
        riderMarker.setPosition(RIDER_POINT);
        riderMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        riderMarker.setTitle("🛵 Driver Location");
        riderMarker.setVisible(false);
        mapView.getOverlays().add(riderMarker);

        // Setup Customer Destination Marker
        customerMarker = new Marker(mapView);
        customerMarker.setPosition(sampleGeoPoints[0]);
        customerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        customerMarker.setTitle("📍 Customer Destination");
        mapView.getOverlays().add(customerMarker);

        // Setup Route Line
        routePolyline = new Polyline(mapView);
        routePolyline.setColor(Color.parseColor("#2563EB")); // Primary Blue
        routePolyline.setWidth(8.0f);
        mapView.getOverlays().add(routePolyline);

        calculateRouteAndNotify();

        // Setup Map Touch Overlay to center map on tapped position
        MapEventsOverlay eventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (mapView != null && p != null) {
                    mapView.getController().animateTo(p);
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                if (mapView != null && p != null) {
                    mapView.getController().animateTo(p);
                }
                return true;
            }
        });
        mapView.getOverlays().add(0, eventsOverlay);

        // Listen for user map scroll/drag/pan to automatically update pin location at map center
        mapView.addMapListener(new org.osmdroid.events.MapListener() {
            @Override
            public boolean onScroll(org.osmdroid.events.ScrollEvent event) {
                onMapMoved();
                return false;
            }

            @Override
            public boolean onZoom(org.osmdroid.events.ZoomEvent event) {
                onMapMoved();
                return false;
            }
        });

        // Apply Dark/Light theme map filter
        applyMapTheme(context);

        // Avoid an extra diagnostic HTTP request on every map creation; let osmdroid load/cache tiles normally.
        checkNetworkStatus(context);
    }

    public void startLiveDeliveryAnimation() {
        stopLiveDeliveryAnimation();
        liveAnimationHandler = new Handler(Looper.getMainLooper());
        animationStep = 0;

        liveAnimationRunnable = new Runnable() {
            @Override
            public void run() {
                if (routePolyline != null) {
                    List<GeoPoint> pts = routePolyline.getActualPoints();
                    if (pts != null && pts.size() > 1) {
                        animationStep = (animationStep + 1) % pts.size();
                        GeoPoint nextPoint = pts.get(animationStep);
                        if (riderMarker != null) {
                            riderMarker.setPosition(nextPoint);
                            if (mapView != null) mapView.invalidate();
                        }
                    }
                }
                if (liveAnimationHandler != null && liveAnimationRunnable != null) {
                    liveAnimationHandler.postDelayed(this, 2500);
                }
            }
        };
        liveAnimationHandler.postDelayed(liveAnimationRunnable, 1000);
    }

    public void setDriverMarkerVisible(boolean visible) {
        if (riderMarker != null) {
            riderMarker.setVisible(visible);
            if (mapView != null) mapView.invalidate();
        }
    }

    public void stopLiveDeliveryAnimation() {
        if (liveAnimationHandler != null && liveAnimationRunnable != null) {
            liveAnimationHandler.removeCallbacks(liveAnimationRunnable);
            liveAnimationHandler = null;
            liveAnimationRunnable = null;
        }
    }

    private void runTileDiagnosticTest(String userAgent) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Test sample tile for San Jose del Monte (zoom 15, x=27393, y=14605)
                String tileUrlStr = "https://a.tile.openstreetmap.de/15/27393/14605.png";
                URL url = new URL(tileUrlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", userAgent);
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                int responseCode = conn.getResponseCode();
                int contentLength = conn.getContentLength();
                Log.d(TAG, "Tile Diagnostic Result -> URL: " + tileUrlStr + " | HTTP Code: " + responseCode + " | Content-Length: " + contentLength);

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Primary OSM Germany tile server returned status " + responseCode + ". Testing fallback OSM France...");
                    URL fallbackUrl = new URL("https://a.tile.openstreetmap.fr/osmfr/15/27393/14605.png");
                    HttpURLConnection fbConn = (HttpURLConnection) fallbackUrl.openConnection();
                    fbConn.setRequestMethod("GET");
                    fbConn.setRequestProperty("User-Agent", userAgent);
                    fbConn.setConnectTimeout(4000);
                    int fbCode = fbConn.getResponseCode();
                    Log.d(TAG, "Fallback OSM France Tile Test -> Code: " + fbCode);
                    if (fbCode == HttpURLConnection.HTTP_OK) {
                        post(() -> {
                            if (mapView != null) mapView.setTileSource(OSM_FRANCE);
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Tile Diagnostic Error: " + e.getMessage(), e);
            }
        });
    }

    private void checkNetworkStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if (!isConnected) {
                Toast.makeText(context, "Network offline: Using cached tiles and offline pin positioning.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onResume() {
        applyMapTheme(getContext());
        if (mapView != null) {
            mapView.onResume();
        }
    }

    private void applyMapTheme(Context context) {
        if (mapView == null || context == null) return;
        try {
            int nightModeFlags = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            boolean isDark = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;

            if (mapView.getOverlayManager() != null && mapView.getOverlayManager().getTilesOverlay() != null) {
                if (isDark) {
                    android.graphics.ColorMatrix matrix = new android.graphics.ColorMatrix(new float[]{
                        -0.75f,  0.00f,  0.00f, 0.00f, 220.0f,
                         0.00f, -0.75f,  0.00f, 0.00f, 225.0f,
                         0.00f,  0.00f, -0.70f, 0.00f, 235.0f,
                         0.00f,  0.00f,  0.00f, 1.00f,   0.0f
                    });
                    android.graphics.ColorMatrixColorFilter filter = new android.graphics.ColorMatrixColorFilter(matrix);
                    mapView.getOverlayManager().getTilesOverlay().setColorFilter(filter);
                } else {
                    mapView.getOverlayManager().getTilesOverlay().setColorFilter(null);
                }
            }
        } catch (Exception ignored) {}
    }

    public void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        stopLiveDeliveryAnimation();
    }

    public void setOnLocationChangeListener(OnLocationChangeListener listener) {
        this.locationChangeListener = listener;
        notifyListener();
    }

    private void notifyListener() {
        if (locationChangeListener != null && hasExplicitDestinationSelection) {
            locationChangeListener.onLocationSelected(
                    sampleAddresses[sampleLocationIndex],
                    sampleDistances[sampleLocationIndex]
            );
        }
    }

    public void zoomIn() {
        if (mapView != null) {
            mapView.getController().zoomIn();
        }
    }

    public void zoomOut() {
        if (mapView != null) {
            mapView.getController().zoomOut();
        }
    }

    public void recenter() {
        if (mapView != null) {
            mapView.getController().setZoom(15.0);
            mapView.getController().animateTo(riderMarker != null ? riderMarker.getPosition() : RIDER_POINT);
        }
    }

    public void useCurrentLocation() {
        hasExplicitDestinationSelection = true;
        sampleLocationIndex = 4; // Graceville / Near Rider Hub
        if (customerMarker != null) {
            customerMarker.setPosition(sampleGeoPoints[4]);
            calculateRouteAndNotify();
            mapView.invalidate();
        }
    }

    public void changeCustomerLocation() {
        hasExplicitDestinationSelection = true;
        sampleLocationIndex = (sampleLocationIndex + 1) % sampleAddresses.length;
        if (customerMarker != null) {
            customerMarker.setPosition(sampleGeoPoints[sampleLocationIndex]);
            calculateRouteAndNotify();
            if (mapView != null) {
                mapView.getController().animateTo(sampleGeoPoints[sampleLocationIndex]);
                mapView.invalidate();
            }
        }
    }

    private void onMapMoved() {
        if (isProgrammaticMove || mapView == null) return;
        org.osmdroid.api.IGeoPoint center = mapView.getMapCenter();
        if (center != null) {
            GeoPoint centerPoint = new GeoPoint(center.getLatitude(), center.getLongitude());
            if (isValidCoordinate(centerPoint.getLatitude(), centerPoint.getLongitude())) {
                hasExplicitDestinationSelection = true;
                if (customerMarker != null) {
                    customerMarker.setPosition(centerPoint);
                    customerMarker.setTitle("📍 Pin Location");
                }
                String locName = String.format(Locale.US, "Pinned Location (%.4f, %.4f)", centerPoint.getLatitude(), centerPoint.getLongitude());
                if (locationChangeListener != null) {
                    locationChangeListener.onLocationSelected(locName, "Map Center Pin");
                }
            }
        }
    }

    public void setDestinationLocation(double lat, double lng, String name, String fullAddress) {
        if (!isValidCoordinate(lat, lng)) return;
        hasExplicitDestinationSelection = true;
        GeoPoint destPoint = new GeoPoint(lat, lng);
        if (customerMarker != null) {
            customerMarker.setPosition(destPoint);
            customerMarker.setTitle("📍 " + (name != null ? name : "Destination"));
        }
        if (sampleAddresses != null && sampleLocationIndex < sampleAddresses.length) {
            sampleAddresses[sampleLocationIndex] = fullAddress != null ? fullAddress : (name != null ? name : "Selected Location");
        }
        if (mapView != null) {
            isProgrammaticMove = true;
            mapView.getController().setZoom(16.0);
            mapView.getController().animateTo(destPoint);
            mapView.postDelayed(() -> isProgrammaticMove = false, 600);
            mapView.invalidate();
        }
        calculateRouteAndNotify();
    }

    public void setLocationByAddress(String address) {
        if (address == null || address.trim().isEmpty()) return;
        for (int i = 0; i < sampleAddresses.length; i++) {
            if (sampleAddresses[i].toLowerCase().contains(address.trim().toLowerCase())) {
                hasExplicitDestinationSelection = true;
                sampleLocationIndex = i;
                if (customerMarker != null) {
                    customerMarker.setPosition(sampleGeoPoints[i]);
                    calculateRouteAndNotify();
                    if (mapView != null) {
                        mapView.getController().animateTo(sampleGeoPoints[i]);
                        mapView.invalidate();
                    }
                }
                return;
            }
        }
        // Do not silently replace an unrecognized address with a sample destination.
    }

    public void updateDriverLocation(double lat, double lng) {
        if (!isValidCoordinate(lat, lng)) return;

        // Live GPS updates may move only the driver marker.
        // Never recenter the map or recalculate the customer destination from GPS:
        // manual pin selection is based on the map center and must remain independent.
        GeoPoint point = new GeoPoint(lat, lng);
        if (riderMarker != null) {
            riderMarker.setPosition(point);
            if (mapView != null) {
                mapView.invalidate();
            }
        }
    }

    private void updateCustomerPosition(GeoPoint point, String address) {
        if (point == null || !isValidCoordinate(point.getLatitude(), point.getLongitude())) return;
        hasExplicitDestinationSelection = true;
        if (customerMarker != null) {
            customerMarker.setPosition(point);
            customerMarker.setTitle("📍 " + address);
            if (sampleAddresses != null && sampleLocationIndex < sampleAddresses.length) {
                sampleAddresses[sampleLocationIndex] = address;
            }
            calculateRouteAndNotify();
            if (mapView != null) {
                mapView.getController().animateTo(point);
                mapView.invalidate();
            }
        }
    }

    private void calculateRouteAndNotify() {
        GeoPoint start = RIDER_POINT;
        GeoPoint end = customerMarker != null ? customerMarker.getPosition() : sampleGeoPoints[0];

        routingService.calculateRoute(start, end, new RoutingService.RouteCallback() {
            @Override
            public void onRouteCalculated(List<GeoPoint> routePoints, double distanceKm, double durationMinutes, boolean isRealRoute) {
                if (routePolyline != null && routePoints != null && !routePoints.isEmpty()) {
                    try {
                        routePolyline.setPoints(routePoints);
                        if (mapView != null) mapView.invalidate();
                    } catch (Exception ignored) {}
                }

                String routeTypeLabel = isRealRoute ? "OSM Route" : "Est.";
                String distText = String.format(Locale.US, "%.1f km (%s ~%.0f mins)", distanceKm, routeTypeLabel, durationMinutes);

                if (sampleDistances != null && sampleLocationIndex < sampleDistances.length) {
                    sampleDistances[sampleLocationIndex] = distText;
                }

                if (locationChangeListener != null) {
                    String addr = (customerMarker != null && customerMarker.getTitle() != null && !customerMarker.getTitle().isEmpty())
                            ? customerMarker.getTitle().replace("📍 ", "")
                            : (sampleAddresses != null && sampleLocationIndex < sampleAddresses.length ? sampleAddresses[sampleLocationIndex] : "Selected Location");
                    locationChangeListener.onLocationSelected(addr, distText);
                }
            }

            @Override
            public void onRouteFailed(String errorMessage) {
                List<GeoPoint> pts = new ArrayList<>();
                pts.add(start);
                pts.add(end);
                if (routePolyline != null) {
                    try {
                        routePolyline.setPoints(pts);
                        if (mapView != null) mapView.invalidate();
                    } catch (Exception ignored) {}
                }
                notifyListener();
            }
        });
    }

    public GeoPoint getPickupGeoPoint() {
        // Never use the movable driver marker as a booking pickup coordinate.
        return new GeoPoint(14.7826996, 121.0475865);
    }

    public GeoPoint getDestinationGeoPoint() {
        return hasExplicitDestinationSelection && customerMarker != null ? customerMarker.getPosition() : null;
    }

    public boolean hasExplicitDestinationSelection() {
        return hasExplicitDestinationSelection;
    }

    /** Clears the previous destination only when the user explicitly chooses GPS. */
    public void clearExplicitDestinationSelection() {
        hasExplicitDestinationSelection = false;
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
                && !(latitude == 0 && longitude == 0);
    }

    public String getSelectedAddress() {
        return sampleAddresses != null && sampleLocationIndex < sampleAddresses.length ?
                sampleAddresses[sampleLocationIndex] : "San Jose del Monte, Bulacan";
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopLiveDeliveryAnimation();
        if (mapView != null) {
            mapView.onDetach();
        }
    }
}
