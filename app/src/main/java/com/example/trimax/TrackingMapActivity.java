package com.example.trimax;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackingMapActivity extends AppCompatActivity {

    private MapView mapView;
    private DatabaseHelper dbHelper;
    private TextView subtitleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        Configuration.getInstance().load(this,
                android.preference.PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue("TRIMAX-DeliveryApp/1.0 (Android; com.example.trimax)");
        File base = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(base);
        Configuration.getInstance().setOsmdroidTileCache(new File(base, "tiles"));

        dbHelper = new DatabaseHelper(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setFitsSystemWindows(true);

        TextView title = new TextView(this);
        title.setText("Client Tracking");
        title.setTextColor(Color.rgb(20, 35, 60));
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(dp(18), dp(18), dp(18), dp(4));
        root.addView(title);

        subtitleTextView = new TextView(this);
        subtitleTextView.setText("Customer locations are shown as pins. Tap a pin to view client details.");
        subtitleTextView.setTextColor(Color.DKGRAY);
        subtitleTextView.setTextSize(13);
        subtitleTextView.setPadding(dp(18), 0, dp(18), dp(12));
        root.addView(subtitleTextView);

        mapView = new MapView(this);
        mapView.setTileSource(MockMapView.OSM_GERMANY);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        root.addView(mapView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView back = new TextView(this);
        back.setText("‹  Back to Dashboard");
        back.setTextColor(Color.rgb(37, 99, 235));
        back.setTextSize(15);
        back.setPadding(dp(18), dp(14), dp(18), dp(18));
        back.setOnClickListener(v -> finish());
        root.addView(back);

        setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), bars.top, v.getPaddingRight(), bars.bottom);
            return insets;
        });

        showCustomerPins(subtitleTextView);
    }

    private void showCustomerPins(TextView subtitle) {
        if (mapView == null) return;
        mapView.getOverlays().clear();

        List<DatabaseHelper.SavedCustomer> customers = dbHelper != null ? dbHelper.getSavedCustomers("") : new ArrayList<>();
        int pinCount = 0;
        List<GeoPoint> validPoints = new ArrayList<>();

        if (customers != null) {
            for (DatabaseHelper.SavedCustomer customer : customers) {
                if (customer == null) continue;
                double lat = customer.getLatitude();
                double lng = customer.getLongitude();
                if (!isValidCoordinate(lat, lng)) continue;

                GeoPoint point = new GeoPoint(lat, lng);
                validPoints.add(point);
                Marker marker = new Marker(mapView);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                String name = customer.getName() != null && !customer.getName().trim().isEmpty()
                        ? customer.getName().trim() : "Client";
                marker.setTitle(name);

                StringBuilder snippet = new StringBuilder();
                if (customer.getPhone() != null && !customer.getPhone().trim().isEmpty()) {
                    snippet.append("📞 ").append(customer.getPhone().trim());
                }
                if (customer.getAddress() != null && !customer.getAddress().trim().isEmpty()) {
                    if (snippet.length() > 0) snippet.append("\n");
                    snippet.append("📍 ").append(customer.getAddress().trim());
                }
                if (customer.getBookingCount() > 0) {
                    if (snippet.length() > 0) snippet.append("\n");
                    snippet.append("📦 ").append(customer.getBookingCount()).append(" booking(s)");
                }
                marker.setSnippet(snippet.toString());

                marker.setOnMarkerClickListener((clicked, map) -> {
                    Toast.makeText(this, clicked.getTitle() + (clicked.getSnippet() != null && !clicked.getSnippet().isEmpty() ? "\n" + clicked.getSnippet() : ""),
                            Toast.LENGTH_LONG).show();
                    clicked.showInfoWindow();
                    return true;
                });
                mapView.getOverlays().add(marker);
                pinCount++;
            }
        }

        if (subtitle != null) {
            if (!validPoints.isEmpty()) {
                subtitle.setText(pinCount + " client location pin(s) shown. Tap a pin for details.");
            } else {
                subtitle.setText(customers == null || customers.isEmpty()
                        ? "No saved clients yet. Add customers and save their map locations to see pins here."
                        : "No client coordinates saved yet. Open a booking, choose the exact location on the map, and save it to show a pin.");
            }
        }

        if (!validPoints.isEmpty()) {
            if (validPoints.size() == 1) {
                mapView.getController().setZoom(15.0);
                mapView.getController().setCenter(validPoints.get(0));
            } else {
                final List<GeoPoint> pointsToBound = validPoints;
                mapView.post(() -> {
                    try {
                        mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(pointsToBound), false, dp(56));
                    } catch (Exception ignored) {
                        if (!pointsToBound.isEmpty()) {
                            mapView.getController().setCenter(pointsToBound.get(0));
                        }
                    }
                });
            }
        } else {
            mapView.getController().setCenter(new GeoPoint(14.8115, 121.0453));
        }
        mapView.invalidate();
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180
                && !(lat == 0 && lng == 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (subtitleTextView != null) showCustomerPins(subtitleTextView);
    }

    @Override protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (mapView != null) mapView.onDetach();
        super.onDestroy();
    }
}
