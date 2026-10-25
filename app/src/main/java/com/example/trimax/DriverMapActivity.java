package com.example.trimax;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Locale;

public class DriverMapActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private LocationHelper locationHelper;
    private String deliveryCode;
    private MockMapView mockMapView;
    private Delivery currentDelivery;
    private boolean hasCenteredOnLiveFix = false;
    private boolean permissionRequestInProgress = false;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_map);

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    permissionRequestInProgress = false;
                    boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    if (fineGranted || coarseGranted) {
                        startDriverLocationTracking();
                    } else {
                        Toast.makeText(this,
                                "Location permission is needed to show your live GPS location on the driver map.",
                                Toast.LENGTH_LONG).show();
                    }
                }
        );

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);
        locationHelper = new LocationHelper(this);

        deliveryCode = getIntent().getStringExtra("DELIVERY_ID");
        if (deliveryCode == null || deliveryCode.trim().isEmpty()) {
            deliveryCode = getIntent().getStringExtra("BOOKING_ID");
        }
        if (deliveryCode != null) deliveryCode = deliveryCode.trim();

        // The screen that opens the map passes the destination it got from the server. If it is missing,
        // the delivery is fetched from the server below (only deliveries assigned to this driver load).
        String destAddress = getIntent().getStringExtra("DEST_ADDRESS");
        double destLat = getIntent().getDoubleExtra("DEST_LAT", 0);
        double destLng = getIntent().getDoubleExtra("DEST_LNG", 0);
        if (deliveryCode != null && !deliveryCode.isEmpty() && destAddress != null) {
            currentDelivery = new Delivery();
            currentDelivery.setBookingId(deliveryCode);
            currentDelivery.setDestination(destAddress);
            currentDelivery.setDestinationLatitude(destLat);
            currentDelivery.setDestinationLongitude(destLng);
            currentDelivery.setCustomerName(getIntent().getStringExtra("CUSTOMER_NAME"));
        } else if (deliveryCode != null && deliveryCode.matches("\\d{1,9}")) {
            fetchDestination(Integer.parseInt(deliveryCode));
        }

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

        if (currentDelivery != null && tvDestAddress != null) {
            tvDestAddress.setText(currentDelivery.getDestination() != null ? currentDelivery.getDestination() : "No destination specified");
        } else if (tvDestAddress != null) {
            tvDestAddress.setText("No active delivery assigned");
        }

        if (mockMapView != null) {
            mockMapView.setDriverMarkerVisible(true);
            mockMapView.setLiveDriverRouteOrigin(true);
            mockMapView.setDestinationPinEnabled(false);
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

            // Request runtime location permission before starting live GPS tracking.
            ensureLocationPermissionAndStartTracking();
        }

        if (btnZoomIn != null && mockMapView != null) btnZoomIn.setOnClickListener(v -> mockMapView.zoomIn());
        if (btnZoomOut != null && mockMapView != null) btnZoomOut.setOnClickListener(v -> mockMapView.zoomOut());
        if (btnMyLocation != null && mockMapView != null) {
            btnMyLocation.setOnClickListener(v -> {
                if (!hasLocationPermission()) {
                    requestLocationPermission();
                    return;
                }
                startDriverLocationTracking();
                if (locationHelper.hasValidLocationFix()) {
                    mockMapView.updateDriverLocation(locationHelper.getLatitude(), locationHelper.getLongitude());
                    mockMapView.recenter();
                    Toast.makeText(this, "Map centered on current GPS location", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Waiting for a valid GPS fix. Make sure device Location is enabled.", Toast.LENGTH_LONG).show();
                }
            });
        }
        if (btnStartNavigation != null) btnStartNavigation.setOnClickListener(v -> startTurnByTurnNavigation());
    }


    private void fetchDestination(int bookingId) {
        TrimaxApi.get(this).getDriverDelivery(bookingId, new TrimaxApi.Callback<BookingRecord>() {
            @Override
            public void onSuccess(BookingRecord record) {
                if (SessionGuard.isGone(DriverMapActivity.this)) return;
                currentDelivery = record.toDelivery(true);
                TextView tvDestAddress = findViewById(R.id.tvDestAddress);
                if (tvDestAddress != null) {
                    tvDestAddress.setText(currentDelivery.getDestination() != null ? currentDelivery.getDestination() : "No destination specified");
                }
                if (mockMapView != null && isValidCoordinate(currentDelivery.getDestinationLatitude(), currentDelivery.getDestinationLongitude())) {
                    mockMapView.setDestinationLocation(currentDelivery.getDestinationLatitude(),
                            currentDelivery.getDestinationLongitude(), currentDelivery.getCustomerName(),
                            currentDelivery.getDestination());
                }
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(DriverMapActivity.this)) return;
                if (SessionGuard.handle(DriverMapActivity.this, error)) return;
                Toast.makeText(DriverMapActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean hasLocationPermission() {
        boolean fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        return fineGranted || coarseGranted;
    }

    private void requestLocationPermission() {
        if (hasLocationPermission()) {
            startDriverLocationTracking();
            return;
        }
        if (permissionRequestInProgress) return;
        permissionRequestInProgress = true;
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void ensureLocationPermissionAndStartTracking() {
        if (hasLocationPermission()) {
            startDriverLocationTracking();
        } else {
            requestLocationPermission();
        }
    }

    private void startDriverLocationTracking() {
        if (locationHelper == null || mockMapView == null || !hasLocationPermission()) return;

        locationHelper.startLocationUpdates((latitude, longitude, accuracy, speed, bearing) -> {
            if (mockMapView != null) {
                mockMapView.updateDriverLocation(latitude, longitude);
                if (!hasCenteredOnLiveFix) {
                    hasCenteredOnLiveFix = true;
                    mockMapView.recenter();
                }
            }
        });

        if (locationHelper.hasValidLocationFix()) {
            mockMapView.updateDriverLocation(locationHelper.getLatitude(), locationHelper.getLongitude());
            mockMapView.recenter();
            hasCenteredOnLiveFix = true;
        }
    }

    private void startTurnByTurnNavigation() {
        if (currentDelivery == null) {
            Toast.makeText(this, "No assigned delivery found for navigation.", Toast.LENGTH_LONG).show();
            return;
        }

        // Navigate to the saved customer destination. Do not use the TRIMAX Hub
        // coordinates as a fallback destination, because that sends the driver to the wrong place.
        double lat = currentDelivery.getDestinationLatitude();
        double lng = currentDelivery.getDestinationLongitude();
        boolean hasValidCoords = isValidCoordinate(lat, lng);
        String destination = currentDelivery.getDestination() != null
                ? currentDelivery.getDestination().trim() : "";
        boolean hasValidAddress = !destination.isEmpty();

        if (!hasValidCoords && !hasValidAddress) {
            Toast.makeText(this,
                    "This delivery has no saved destination. Please check the booking's pinned location.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String destinationQuery = hasValidCoords
                ? String.format(Locale.US, "%.6f,%.6f", lat, lng)
                : destination;

        Toast.makeText(this, "Navigating to pinned customer destination: " + destination,
                Toast.LENGTH_SHORT).show();

        // Prefer the installed Google Maps app for full turn-by-turn navigation.
        Uri navigationUri = Uri.parse("google.navigation:q=" + Uri.encode(destinationQuery) + "&mode=d");
        Intent navigationIntent = new Intent(Intent.ACTION_VIEW, navigationUri);
        navigationIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(navigationIntent);
            return;
        } catch (ActivityNotFoundException ignored) {
            // Fall back to Google Maps Directions on the web. Coordinates are used
            // whenever available so the route targets the saved booking pin.
        }

        String encodedDestination = Uri.encode(destinationQuery);
        Uri directionsUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination="
                        + encodedDestination + "&travelmode=driving");
        Intent directionsIntent = new Intent(Intent.ACTION_VIEW, directionsUri);
        try {
            startActivity(directionsIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "Unable to open navigation. Please install Google Maps or a browser.",
                    Toast.LENGTH_LONG).show();
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
        if (mockMapView != null) {
            mockMapView.onResume();
            if (locationHelper != null) {
                if (hasLocationPermission()) {
                    startDriverLocationTracking();
                } else {
                    requestLocationPermission();
                }
            }
        }
    }

    @Override protected void onDestroy() {
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        super.onDestroy();
    }
}
