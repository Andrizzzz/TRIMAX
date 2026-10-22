package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class BookingsActivity extends AppCompatActivity implements BookingAdapter.OnBookingClickListener {

    private DatabaseHelper dbHelper;
    private BookingAdapter bookingAdapter;

    private RecyclerView rvBookings;
    private TextView tvEmptyBookings;
    private TextInputEditText etSearchBookings;

    private MaterialButton chipAll, chipPending, chipConfirmed, chipCancelled, chipCompleted;

    private String currentFilter = "ALL";
    private String currentSearchQuery = "";

    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bookings);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), 0);
                return insets;
            });
        }

        dbHelper = new DatabaseHelper(this);

        // Apply incoming Intent filter extra if passed from Dashboard
        String initialFilter = getIntent().getStringExtra("FILTER");
        if (initialFilter != null && !initialFilter.isEmpty()) {
            currentFilter = initialFilter.toUpperCase();
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }

        MaterialButton btnNewBooking = findViewById(R.id.btnNewBooking);
        etSearchBookings = findViewById(R.id.etSearchBookings);
        rvBookings = findViewById(R.id.rvBookings);
        tvEmptyBookings = findViewById(R.id.tvEmptyBookings);

        chipAll = findViewById(R.id.chipAll);
        chipPending = findViewById(R.id.chipPending);
        chipConfirmed = findViewById(R.id.chipConfirmed);
        chipCancelled = findViewById(R.id.chipCancelled);
        chipCompleted = findViewById(R.id.chipCompleted);

        View cardBottomNavContainer = findViewById(R.id.cardBottomNavContainer);
        if (cardBottomNavContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(cardBottomNavContainer, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                int baseMargin = Math.round(16 * getResources().getDisplayMetrics().density);
                params.bottomMargin = baseMargin + systemBars.bottom;
                v.setLayoutParams(params);
                return insets;
            });
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_bookings);
            setupBottomNavigation(bottomNavigation);
        }

        // RecyclerView Setup
        bookingAdapter = new BookingAdapter(this);
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        rvBookings.setAdapter(bookingAdapter);

        // New Booking Button Listener
        if (btnNewBooking != null) {
            btnNewBooking.setOnClickListener(v -> {
                Intent intent = new Intent(BookingsActivity.this, NewBookingCustomerActivity.class);
                startActivity(intent);
            });
        }

        // Chip Filter Listeners
        if (chipAll != null) chipAll.setOnClickListener(v -> setFilter("ALL"));
        if (chipPending != null) chipPending.setOnClickListener(v -> setFilter("PENDING"));
        if (chipConfirmed != null) chipConfirmed.setOnClickListener(v -> setFilter("CONFIRMED"));
        if (chipCancelled != null) chipCancelled.setOnClickListener(v -> setFilter("CANCELLED"));
        if (chipCompleted != null) chipCompleted.setOnClickListener(v -> setFilter("COMPLETED"));

        // Search text watcher with debouncing to prevent UI freeze and ANR
        if (etSearchBookings != null) {
            etSearchBookings.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearchQuery = s != null ? s.toString().trim() : "";
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> loadBookingsFromDatabase();
                    searchHandler.postDelayed(searchRunnable, 250);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookingsFromDatabase();
    }

    private void setFilter(String filter) {
        currentFilter = filter;
        updateChipStyles();
        loadBookingsFromDatabase();
    }

    private void loadBookingsFromDatabase() {
        if (isFinishing() || isDestroyed()) return;
        if (dbHelper == null || bookingAdapter == null || rvBookings == null || tvEmptyBookings == null) return;
        try {
            List<Delivery> bookings = dbHelper.getBookingsByFilter(currentFilter, currentSearchQuery);
            if (bookings == null) bookings = new java.util.ArrayList<>();
            bookingAdapter.setBookingList(bookings);

            if (bookings.isEmpty()) {
                tvEmptyBookings.setVisibility(View.VISIBLE);
                rvBookings.setVisibility(View.GONE);
            } else {
                tvEmptyBookings.setVisibility(View.GONE);
                rvBookings.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateChipStyles();
    }

    private void updateChipStyles() {
        resetChipStyle(chipAll);
        resetChipStyle(chipPending);
        resetChipStyle(chipConfirmed);
        resetChipStyle(chipCancelled);
        resetChipStyle(chipCompleted);

        if ("PENDING".equalsIgnoreCase(currentFilter)) {
            setSelectedChipStyle(chipPending);
        } else if ("CONFIRMED".equalsIgnoreCase(currentFilter)) {
            setSelectedChipStyle(chipConfirmed);
        } else if ("CANCELLED".equalsIgnoreCase(currentFilter)) {
            setSelectedChipStyle(chipCancelled);
        } else if ("COMPLETED".equalsIgnoreCase(currentFilter)) {
            setSelectedChipStyle(chipCompleted);
        } else {
            setSelectedChipStyle(chipAll);
        }
    }

    private void resetChipStyle(MaterialButton chip) {
        if (chip == null) return;
        // Keep filter chips visually neutral unless selected. Remove the default
        // hover overlay that can make All/Pending look selected unexpectedly.
        chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(this, R.color.surface_alt)));
        chip.setRippleColor(android.content.res.ColorStateList.valueOf(0x00000000));
        chip.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
        chip.setStrokeColor(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(this, R.color.border_subtle)));
        chip.setStrokeWidth(2);
    }

    private void setSelectedChipStyle(MaterialButton chip) {
        if (chip == null) return;
        chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2563EB)); // Selected blue
        chip.setTextColor(0xFFFFFFFF);
        chip.setStrokeWidth(0);
    }

    @Override
    public void onBookingClick(Delivery delivery) {
        if (delivery == null) return;
        Intent intent = new Intent(BookingsActivity.this, BookingDetailActivity.class);
        intent.putExtra("BOOKING_ID", delivery.getBookingId());
        intent.putExtra("CUSTOMER_NAME", delivery.getCustomerName());
        intent.putExtra("STATUS", delivery.getStatus());
        intent.putExtra("TOTAL_AMOUNT", String.format(java.util.Locale.US, "₱ %,.2f", delivery.getCurrentTotal()));
        startActivity(intent);
    }

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent dashboardIntent = new Intent(this, DashboardActivity.class);
                dashboardIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(dashboardIntent);
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, ClientsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_packages) {
                startActivity(new Intent(this, PackagesActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
