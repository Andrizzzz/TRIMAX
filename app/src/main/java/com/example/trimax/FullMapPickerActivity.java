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

public class FullMapPickerActivity extends AppCompatActivity {

    private MockMapView mockMapView;
    private LocationHelper locationHelper;
    private NominatimSearchService nominatimSearchService;
    private LocationSearchAdapter searchAdapter;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private TextView tvSelectedAddress;
    private AutoCompleteTextView actvSmartLocationSearch;

    private String selectedAddress = "";
    private double selectedLat = 0;
    private double selectedLng = 0;
    private boolean isSelectingFromDropdown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_full_map_picker);

        View root = findViewById(R.id.topBarContainer) != null ? findViewById(R.id.topBarContainer) : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), 0);
                return insets;
            });
        }

        locationHelper = new LocationHelper(this);
        nominatimSearchService = new NominatimSearchService();

        Intent intent = getIntent();
        selectedAddress = intent.getStringExtra("INITIAL_ADDRESS");
        selectedLat = intent.getDoubleExtra("INITIAL_LAT", 0);
        selectedLng = intent.getDoubleExtra("INITIAL_LNG", 0);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView btnClearSearch = findViewById(R.id.btnClearSearch);
        mockMapView = findViewById(R.id.mockMapView);
        actvSmartLocationSearch = findViewById(R.id.actvSmartLocationSearch);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);

        FloatingActionButton btnZoomIn = findViewById(R.id.btnZoomIn);
        FloatingActionButton btnZoomOut = findViewById(R.id.btnZoomOut);
        FloatingActionButton btnMyLocation = findViewById(R.id.btnMyLocation);
        MaterialButton btnConfirmLocation = findViewById(R.id.btnConfirmLocation);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (btnClearSearch != null && actvSmartLocationSearch != null) {
            btnClearSearch.setOnClickListener(v -> actvSmartLocationSearch.setText(""));
        }

        View.OnClickListener chipListener = v -> {
            String query;
            int id = v.getId();
            if (id == R.id.chipTungkongMangga) {
                query = "Tungkong Mangga, San Jose del Monte";
            } else if (id == R.id.chipMuzon) {
                query = "Muzon, San Jose del Monte";
            } else if (id == R.id.chipGayagaya) {
                query = "Gaya-gaya, San Jose del Monte";
            } else if (id == R.id.chipPoblacion) {
                query = "Poblacion, San Jose del Monte";
            } else {
                query = "";
            }

            if (!query.isEmpty() && actvSmartLocationSearch != null) {
                actvSmartLocationSearch.setText(query);
                actvSmartLocationSearch.setSelection(query.length());
            }
        };

        View chipTm = findViewById(R.id.chipTungkongMangga);
        View chipMz = findViewById(R.id.chipMuzon);
        View chipGy = findViewById(R.id.chipGayagaya);
        View chipPb = findViewById(R.id.chipPoblacion);

        if (chipTm != null) chipTm.setOnClickListener(chipListener);
        if (chipMz != null) chipMz.setOnClickListener(chipListener);
        if (chipGy != null) chipGy.setOnClickListener(chipListener);
        if (chipPb != null) chipPb.setOnClickListener(chipListener);

        if (selectedAddress != null && !selectedAddress.trim().isEmpty() && tvSelectedAddress != null) {
            tvSelectedAddress.setText(selectedAddress);
        }

        if (mockMapView != null) {
            if (selectedLat != 0 && selectedLng != 0) {
                mockMapView.setDestinationLocation(selectedLat, selectedLng, "Selected Pin", selectedAddress);
            }
            mockMapView.setOnLocationChangeListener((address, distanceText) -> {
                selectedAddress = address;
                GeoPoint dest = mockMapView.getDestinationGeoPoint();
                if (dest != null) {
                    selectedLat = dest.getLatitude();
                    selectedLng = dest.getLongitude();
                }
                if (tvSelectedAddress != null) {
                    tvSelectedAddress.setText(address);
                }
            });
        }

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (Boolean.TRUE.equals(fineGranted) || Boolean.TRUE.equals(coarseGranted)) {
                        fetchAndApplyGpsLocation();
                    }
                }
        );

        // Smart Search Setup
        if (actvSmartLocationSearch != null) {
            searchAdapter = new LocationSearchAdapter(this, new ArrayList<>());
            actvSmartLocationSearch.setAdapter(searchAdapter);
            actvSmartLocationSearch.setThreshold(2);

            actvSmartLocationSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (isSelectingFromDropdown) return;
                    String query = s != null ? s.toString().trim() : "";
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
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
                            @Override public void onError(String errorMessage) {}
                        });
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            actvSmartLocationSearch.setOnItemClickListener((parent, view, position, id) -> {
                SearchResultItem item = (SearchResultItem) parent.getItemAtPosition(position);
                if (item != null) {
                    isSelectingFromDropdown = true;
                    actvSmartLocationSearch.setText(item.getShortName(), false);
                    isSelectingFromDropdown = false;

                    selectedAddress = item.getFullAddress();
                    selectedLat = item.getLatitude();
                    selectedLng = item.getLongitude();

                    if (tvSelectedAddress != null) tvSelectedAddress.setText(selectedAddress);
                    if (mockMapView != null) {
                        mockMapView.setDestinationLocation(selectedLat, selectedLng, item.getShortName(), selectedAddress);
                    }

                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                }
            });
        }

        if (btnZoomIn != null && mockMapView != null) btnZoomIn.setOnClickListener(v -> mockMapView.zoomIn());
        if (btnZoomOut != null && mockMapView != null) btnZoomOut.setOnClickListener(v -> mockMapView.zoomOut());
        if (btnMyLocation != null) btnMyLocation.setOnClickListener(v -> checkAndFetchGpsLocation());

        // Live GPS startup on map launch
        if (mockMapView != null) {
            locationHelper.startLocationUpdates((latitude, longitude, accuracy, speed, bearing) -> {
                if (mockMapView != null) mockMapView.updateDriverLocation(latitude, longitude);
            });
            if (selectedLat == 0 || selectedLng == 0) {
                checkAndFetchGpsLocation();
            }
        }

        if (btnConfirmLocation != null) {
            btnConfirmLocation.setOnClickListener(v -> {
                if (mockMapView != null) {
                    GeoPoint dest = mockMapView.getDestinationGeoPoint();
                    if (dest != null && isValidCoordinate(dest.getLatitude(), dest.getLongitude())) {
                        selectedLat = dest.getLatitude();
                        selectedLng = dest.getLongitude();
                    }
                }

                if (selectedAddress == null || selectedAddress.trim().isEmpty()
                        || !isValidCoordinate(selectedLat, selectedLng)) {
                    Toast.makeText(this, "Please select or search a valid location on the map.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent resultIntent = new Intent();
                resultIntent.putExtra("SELECTED_ADDRESS", selectedAddress);
                resultIntent.putExtra("SELECTED_LAT", selectedLat);
                resultIntent.putExtra("SELECTED_LNG", selectedLng);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }
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
        double lat = locationHelper.getLatitude();
        double lng = locationHelper.getLongitude();
        if (lat != 0 && lng != 0) {
            selectedLat = lat;
            selectedLng = lng;
            selectedAddress = String.format(Locale.US, "GPS Location (%.6f, %.6f)", lat, lng);
            if (tvSelectedAddress != null) tvSelectedAddress.setText(selectedAddress);
            if (mockMapView != null) {
                mockMapView.setDestinationLocation(lat, lng, "Current GPS Location", selectedAddress);
                mockMapView.recenter();
            }
        }
    }

    private boolean isValidCoordinate(double lat, double lng) {
        return Double.isFinite(lat) && Double.isFinite(lng) && lat >= -90 && lat <= 90 && !(lat == 0 && lng == 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mockMapView != null) mockMapView.onResume();
    }

    @Override
    protected void onPause() {
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        if (nominatimSearchService != null) nominatimSearchService.cancelPendingSearches();
        if (mockMapView != null) mockMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        if (nominatimSearchService != null) nominatimSearchService.cancelPendingSearches();
        super.onDestroy();
    }
}
