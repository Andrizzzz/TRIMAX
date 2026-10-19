package com.example.trimax;

import android.content.Intent;
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
        if (deliveryCode == null || deliveryCode.isEmpty()) {
            deliveryCode = "TRX-1024";
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        mockMapView = findViewById(R.id.mockMapView);
        FloatingActionButton btnZoomIn = findViewById(R.id.btnZoomIn);
        FloatingActionButton btnZoomOut = findViewById(R.id.btnZoomOut);
        FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);

        TextView tvDestAddress = findViewById(R.id.tvDestAddress);
        TextView tvEstTime = findViewById(R.id.tvEstTime);
        MaterialButton btnStartNavigation = findViewById(R.id.btnStartNavigation);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        Delivery d = dbHelper.getDeliveryByCode(deliveryCode);
        if (d != null) {
            if (tvDestAddress != null) tvDestAddress.setText(d.getDestination());
        }

        if (mockMapView != null) {
            mockMapView.setOnLocationChangeListener((address, distanceText) -> {
                if (tvDestAddress != null) tvDestAddress.setText(address);
                if (tvEstTime != null) tvEstTime.setText(distanceText);
            });
        }

        if (btnZoomIn != null && mockMapView != null) {
            btnZoomIn.setOnClickListener(v -> mockMapView.zoomIn());
        }

        if (btnZoomOut != null && mockMapView != null) {
            btnZoomOut.setOnClickListener(v -> mockMapView.zoomOut());
        }

        if (btnMyLocation != null && mockMapView != null) {
            btnMyLocation.setOnClickListener(v -> {
                locationHelper.startLocationUpdates((latitude, longitude, accuracy, speed, bearing) -> {
                    mockMapView.updateDriverLocation(latitude, longitude);
                });
                mockMapView.updateDriverLocation(locationHelper.getLatitude(), locationHelper.getLongitude());
                mockMapView.recenter();
                Toast.makeText(this, "Map centered on Driver GPS location", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnStartNavigation != null) {
            btnStartNavigation.setOnClickListener(v -> {
                Toast.makeText(this, "Starting Live Turn-by-Turn GPS Navigation...", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void handleBackNavigation() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DriverDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
