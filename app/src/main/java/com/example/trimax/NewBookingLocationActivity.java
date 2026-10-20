package com.example.trimax;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
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
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NewBookingLocationActivity extends AppCompatActivity {

    private String customerName, customerPhone, customerEmail;
    private String service, date, time, notes;
    private int quantity;
    private String selectedCustomerAddress = "San Jose del Monte, Bulacan";

    private LocationHelper locationHelper;
    private MockMapView mockMapView;
    private NominatimSearchService nominatimSearchService;
    private LocationSearchAdapter searchAdapter;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private boolean isSelectingFromDropdown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_location);

        View mainContent = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (mainContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }

        locationHelper = new LocationHelper(this);
        nominatimSearchService = new NominatimSearchService();

        Intent intent = getIntent();
        customerName = intent.getStringExtra("CUSTOMER_NAME");
        customerPhone = intent.getStringExtra("CUSTOMER_PHONE");
        customerEmail = intent.getStringExtra("CUSTOMER_EMAIL");
        String savedAddress = intent.getStringExtra("CUSTOMER_ADDRESS");
        double savedCustomerLatitude = intent.getDoubleExtra("CUSTOMER_LAT", 0);
        double savedCustomerLongitude = intent.getDoubleExtra("CUSTOMER_LNG", 0);
        if (savedAddress != null && !savedAddress.trim().isEmpty()) selectedCustomerAddress = savedAddress.trim();
        service = intent.getStringExtra("SERVICE");
        quantity = intent.getIntExtra("QUANTITY", 1);
        date = intent.getStringExtra("DATE");
        time = intent.getStringExtra("TIME");
        notes = intent.getStringExtra("NOTES");

        ImageView btnBack = findViewById(R.id.btnBack);
        mockMapView = findViewById(R.id.mockMapView);
        AutoCompleteTextView actvSmartLocationSearch = findViewById(R.id.actvSmartLocationSearch);

        FloatingActionButton btnZoomIn = findViewById(R.id.btnZoomIn);
        FloatingActionButton btnZoomOut = findViewById(R.id.btnZoomOut);
        FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);

        TextView tvCustomerAddress = findViewById(R.id.tvCustomerAddress);
        if (tvCustomerAddress != null) tvCustomerAddress.setText(selectedCustomerAddress);
        if (mockMapView != null && savedCustomerLatitude != 0 && savedCustomerLongitude != 0) {
            mockMapView.setDestinationLocation(savedCustomerLatitude, savedCustomerLongitude,
                    selectedCustomerAddress, selectedCustomerAddress);
        }
        TextView tvCustomerSubtitle = findViewById(R.id.tvCustomerSubtitle);

        MaterialButton btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);
        MaterialButton btnChangeCustomerLocation = findViewById(R.id.btnChangeCustomerLocation);
        MaterialButton btnConfirmLocation = findViewById(R.id.btnConfirmLocation);

        if (btnBack != null) btnBack.setOnClickListener(v -> navigateBack());

        // Permission Launcher Setup
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                        Toast.makeText(this, "Location permission granted! Fetching GPS position...", Toast.LENGTH_SHORT).show();
                        fetchAndApplyGpsLocation();
                    } else {
                        Toast.makeText(this, "Location permission denied. Using manual map pin selection.", Toast.LENGTH_LONG).show();
                    }
                }
        );

        // SmartSearch AutoComplete Setup with Real OpenStreetMap Nominatim Geocoding
        if (actvSmartLocationSearch != null) {
            searchAdapter = new LocationSearchAdapter(this, new ArrayList<>());
            actvSmartLocationSearch.setAdapter(searchAdapter);
            actvSmartLocationSearch.setThreshold(2);

            actvSmartLocationSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isSelectingFromDropdown) {
                        return;
                    }
                    String query = s != null ? s.toString().trim() : "";
                    if (query.length() >= 2) {
                        nominatimSearchService.searchLocation(query, new NominatimSearchService.SearchCallback() {
                            @Override
                            public void onSearchResults(List<SearchResultItem> results, String searchPattern) {
                                if (results != null && !results.isEmpty()) {
                                    searchAdapter.updateItems(results);
                                    actvSmartLocationSearch.showDropDown();
                                } else {
                                    searchAdapter.updateItems(new ArrayList<>());
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                // Silently handle search network errors
                            }
                        });
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            actvSmartLocationSearch.setOnItemClickListener((parent, view, position, id) -> {
                SearchResultItem selectedItem = (SearchResultItem) parent.getItemAtPosition(position);
                if (selectedItem != null) {
                    isSelectingFromDropdown = true;
                    actvSmartLocationSearch.setText(selectedItem.getShortName(), false);
                    isSelectingFromDropdown = false;

                    selectedCustomerAddress = selectedItem.getFullAddress();
                    if (tvCustomerAddress != null) {
                        tvCustomerAddress.setText(selectedItem.getFullAddress());
                    }

                    if (mockMapView != null) {
                        mockMapView.setDestinationLocation(
                                selectedItem.getLatitude(),
                                selectedItem.getLongitude(),
                                selectedItem.getShortName(),
                                selectedItem.getFullAddress()
                        );
                    }

                    // Hide soft keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }

                    Toast.makeText(this, "Location set: " + selectedItem.getShortName(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // If an older saved customer only has a typed address, try resolving it to a map pin.
        if (savedAddress != null && !savedAddress.trim().isEmpty()
                && (savedCustomerLatitude == 0 || savedCustomerLongitude == 0)) {
            nominatimSearchService.searchLocation(savedAddress, new NominatimSearchService.SearchCallback() {
                @Override
                public void onSearchResults(List<SearchResultItem> results, String searchPattern) {
                    if (results != null && !results.isEmpty() && mockMapView != null) {
                        SearchResultItem result = results.get(0);
                        selectedCustomerAddress = result.getFullAddress();
                        if (tvCustomerAddress != null) tvCustomerAddress.setText(selectedCustomerAddress);
                        mockMapView.setDestinationLocation(result.getLatitude(), result.getLongitude(),
                                result.getShortName(), result.getFullAddress());
                    } else {
                        Toast.makeText(NewBookingLocationActivity.this,
                                "Saved address could not be located automatically. Please search and select it on the map.",
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(NewBookingLocationActivity.this,
                            "Could not locate the saved address. Please select it on the map.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        // Wire Mock Map Location Selection Listener
        if (mockMapView != null) {
            mockMapView.setOnLocationChangeListener((address, distanceText) -> {
                selectedCustomerAddress = address;
                if (tvCustomerAddress != null) {
                    tvCustomerAddress.setText(address);
                }
                if (tvCustomerSubtitle != null) {
                    tvCustomerSubtitle.setText(String.format("Selected customer location • %s", distanceText));
                }
            });
        }

        // Map Overlay Controls
        if (btnZoomIn != null && mockMapView != null) {
            btnZoomIn.setOnClickListener(v -> mockMapView.zoomIn());
        }

        if (btnZoomOut != null && mockMapView != null) {
            btnZoomOut.setOnClickListener(v -> mockMapView.zoomOut());
        }

        if (btnMyLocation != null) {
            btnMyLocation.setOnClickListener(v -> checkAndFetchGpsLocation());
        }

        // Action Buttons
        if (btnUseCurrentLocation != null) {
            btnUseCurrentLocation.setOnClickListener(v -> checkAndFetchGpsLocation());
        }

        if (btnChangeCustomerLocation != null && mockMapView != null) {
            btnChangeCustomerLocation.setOnClickListener(v -> {
                if (actvSmartLocationSearch != null) {
                    actvSmartLocationSearch.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(actvSmartLocationSearch, InputMethodManager.SHOW_IMPLICIT);
                }
                Toast.makeText(this,
                        "Search and select an address, or tap the map to place the pin. The map will not jump to sample locations.",
                        Toast.LENGTH_LONG).show();
            });
        }

        // Confirm Location Button
        if (btnConfirmLocation != null) btnConfirmLocation.setOnClickListener(v -> {
            GeoPoint pickupPoint = mockMapView != null ? mockMapView.getPickupGeoPoint() : new GeoPoint(14.8115, 121.0453);
            GeoPoint destPoint = mockMapView != null ? mockMapView.getDestinationGeoPoint() : new GeoPoint(14.8150, 121.0500);

            Intent summaryIntent = new Intent(NewBookingLocationActivity.this, NewBookingSummaryActivity.class);
            summaryIntent.putExtra("CUSTOMER_NAME", customerName);
            summaryIntent.putExtra("CUSTOMER_PHONE", customerPhone);
            summaryIntent.putExtra("CUSTOMER_EMAIL", customerEmail);
            summaryIntent.putExtra("SERVICE", service);
            summaryIntent.putExtra("QUANTITY", quantity);
            summaryIntent.putExtra("DATE", date);
            summaryIntent.putExtra("TIME", time);
            summaryIntent.putExtra("NOTES", notes);
            summaryIntent.putExtra("ADDRESS", selectedCustomerAddress);
            summaryIntent.putExtra("PICKUP_LAT", pickupPoint.getLatitude());
            summaryIntent.putExtra("PICKUP_LNG", pickupPoint.getLongitude());
            summaryIntent.putExtra("DEST_LAT", destPoint.getLatitude());
            summaryIntent.putExtra("DEST_LNG", destPoint.getLongitude());
            startActivity(summaryIntent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mockMapView != null) {
            mockMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        if (mockMapView != null) mockMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        if (nominatimSearchService != null) nominatimSearchService.cancelPendingSearches();
        if (mockMapView != null) mockMapView.stopLiveDeliveryAnimation();
        super.onDestroy();
    }

    private void checkAndFetchGpsLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchAndApplyGpsLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchAndApplyGpsLocation() {
        locationHelper.startLocationUpdates((latitude, longitude, accuracy, speed, bearing) -> {
            if (mockMapView != null) {
                mockMapView.updateDriverLocation(latitude, longitude);
            }
        });

        if (mockMapView != null) {
            mockMapView.updateDriverLocation(locationHelper.getLatitude(), locationHelper.getLongitude());
            mockMapView.recenter();
        }
        Toast.makeText(this, String.format(Locale.US, "Map centered on device GPS coordinates (%.4f, %.4f)", locationHelper.getLatitude(), locationHelper.getLongitude()), Toast.LENGTH_SHORT).show();
    }

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
