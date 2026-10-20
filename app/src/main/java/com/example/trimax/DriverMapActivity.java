package com.example.trimax;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DriverMapActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LocationHelper locationHelper;
    private String deliveryCode;
    private MockMapView mockMapView;
    private Delivery currentDelivery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_map);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        dbHelper = new DatabaseHelper(this);
        locationHelper = new LocationHelper(this);
        deliveryCode = getIntent().getStringExtra("DELIVERY_ID");
        if (deliveryCode == null || deliveryCode.trim().isEmpty()) deliveryCode = "TRX-1024";

        ImageView btnBack = findViewById(R.id.btnBack);
        mockMapView = findViewById(R.id.mockMapView);
        FloatingActionButton btnZoomIn = findViewById(R.id.btnZoomIn);
        FloatingActionButton btnZoomOut = findViewById(R.id.btnZoomOut);
        FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        TextView tvDestAddress = findViewById(R.id.tvDestAddress);
        TextView tvEstTime = findViewById(R.id.tvEstTime);
        MaterialButton btnStartNavigation = findViewById(R.id.btnStartNavigation);

        if (btnBack != null) btnBack.setOnClickListener(v -> handleBackNavigation());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { handleBackNavigation(); }
        });

        currentDelivery = dbHelper.getDeliveryByCode(deliveryCode);
        if (currentDelivery != null && tvDestAddress != null) tvDestAddress.setText(currentDelivery.getDestination());

        if (mockMapView != null) {
            mockMapView.setOnLocationChangeListener((address, distanceText) -> {
                if (tvDestAddress != null) tvDestAddress.setText(address);
                if (tvEstTime != null) tvEstTime.setText(distanceText);
            });
            if (currentDelivery != null && isValidCoordinate(currentDelivery.getDestinationLatitude(), currentDelivery.getDestinationLongitude())) {
                mockMapView.setDestinationLocation(currentDelivery.getDestinationLatitude(),
                        currentDelivery.getDestinationLongitude(), currentDelivery.getCustomerName(),
                        currentDelivery.getDestination());
            } else if (currentDelivery != null && currentDelivery.getDestination() != null && tvDestAddress != null) {
                tvDestAddress.setText(currentDelivery.getDestination());
            }
        }

        if (btnZoomIn != null && mockMapView != null) btnZoomIn.setOnClickListener(v -> mockMapView.zoomIn());
        if (btnZoomOut != null && mockMapView != null) btnZoomOut.setOnClickListener(v -> mockMapView.zoomOut());
        if (btnMyLocation != null && mockMapView != null) {
            btnMyLocation.setOnClickListener(v -> {
                locationHelper.startLocationUpdates((latitude, longitude, accuracy, speed, bearing) -> {
                    if (mockMapView != null) mockMapView.updateDriverLocation(latitude, longitude);
                });
                mockMapView.updateDriverLocation(locationHelper.getLatitude(), locationHelper.getLongitude());
                mockMapView.recenter();
                Toast.makeText(this, "Map centered on driver location", Toast.LENGTH_SHORT).show();
            });
        }
        if (btnStartNavigation != null) btnStartNavigation.setOnClickListener(v -> startTurnByTurnNavigation());
    }

    private void startTurnByTurnNavigation() {
        double lat = currentDelivery != null ? currentDelivery.getDestinationLatitude() : 0;
        double lng = currentDelivery != null ? currentDelivery.getDestinationLongitude() : 0;
        String destination = currentDelivery != null ? currentDelivery.getDestination() : null;

        if (!isValidCoordinate(lat, lng) && (destination == null || destination.trim().isEmpty())) {
            Toast.makeText(this, "This delivery has no saved destination. Update its location first.", Toast.LENGTH_LONG).show();
            return;
        }

        String query = isValidCoordinate(lat, lng) ? lat + "," + lng : Uri.encode(destination.trim());
        Intent navigationIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + query + "&mode=d"));
        navigationIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(navigationIntent);
        } catch (ActivityNotFoundException e) {
            String geoQuery = isValidCoordinate(lat, lng) ? lat + "," + lng : Uri.encode(destination.trim());
            Intent fallbackIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + geoQuery));
            try {
                startActivity(Intent.createChooser(fallbackIntent, "Navigate with"));
            } catch (ActivityNotFoundException ignored) {
                Toast.makeText(this, "No maps app is installed. Install Google Maps or another navigation app.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng) && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180 && !(lat == 0 && lng == 0);
    }

    private void handleBackNavigation() {
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DriverDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }

    @Override protected void onPause() {
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        if (mockMapView != null) mockMapView.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (mockMapView != null) mockMapView.onResume();
    }

    @Override protected void onDestroy() {
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        super.onDestroy();
    }
}
