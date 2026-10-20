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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.List;

public class TrackingMapActivity extends AppCompatActivity {


    private MapView mapView;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        Configuration.getInstance().load(this,
                android.preference.PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());
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

        TextView subtitle = new TextView(this);
        subtitle.setText("Customer locations are shown as pins. Tap a pin to view client details.");
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setTextSize(13);
        subtitle.setPadding(dp(18), 0, dp(18), dp(12));
        root.addView(subtitle);

        mapView = new MapView(this);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
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

        showCustomerPins(subtitle);
    }

    private void showCustomerPins(TextView subtitle) {
        List<DatabaseHelper.SavedCustomer> customers = dbHelper.getSavedCustomers("");
        int pinCount = 0;
        GeoPoint firstPoint = null;
        for (DatabaseHelper.SavedCustomer customer : customers) {
            double lat = customer.getLatitude();
            double lng = customer.getLongitude();
            if (!isValidCoordinate(lat, lng)) continue;

            GeoPoint point = new GeoPoint(lat, lng);
            if (firstPoint == null) firstPoint = point;
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(customer.getName() == null ? "Client" : customer.getName());
            marker.setSnippet((customer.getAddress() == null ? "" : customer.getAddress())
                    + (customer.getPhone() == null ? "" : "\n" + customer.getPhone()));
            marker.setOnMarkerClickListener((clicked, map) -> {
                Toast.makeText(this, clicked.getTitle() + "\n" + clicked.getSnippet(),
                        Toast.LENGTH_LONG).show();
                clicked.showInfoWindow();
                return true;
            });
            mapView.getOverlays().add(marker);
            pinCount++;
        }

        if (firstPoint != null) {
            mapView.getController().setZoom(pinCount == 1 ? 15.0 : 12.0);
            mapView.getController().setCenter(firstPoint);
            subtitle.setText(pinCount + " client location pin(s) shown. Tap a pin for details.");
        } else {
            mapView.getController().setCenter(new GeoPoint(14.8115, 121.0453));
            subtitle.setText(customers.isEmpty()
                    ? "No saved clients yet. Add customers and save their map locations to see pins here."
                    : "No client coordinates saved yet. Open a booking, choose the exact location on the map, and save it to show a pin.");
        }
        mapView.invalidate();
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180
                && !(lat == 0 && lng == 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
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