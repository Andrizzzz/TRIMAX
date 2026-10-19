package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class ReportsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;

    private TextView tvReportTotalCount;
    private TextView tvReportConfirmedCount;
    private TextView tvReportPendingCount;
    private TextView tvReportCancelledCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnDateFilter = findViewById(R.id.btnDateFilter);

        CardView cardTotalBookings = findViewById(R.id.cardTotalBookings);
        CardView cardConfirmedBookings = findViewById(R.id.cardConfirmedBookings);
        CardView cardPendingBookings = findViewById(R.id.cardPendingBookings);
        CardView cardCancelledBookings = findViewById(R.id.cardCancelledBookings);

        tvReportTotalCount = findViewById(R.id.tvReportTotalCount);
        tvReportConfirmedCount = findViewById(R.id.tvReportConfirmedCount);
        tvReportPendingCount = findViewById(R.id.tvReportPendingCount);
        tvReportCancelledCount = findViewById(R.id.tvReportCancelledCount);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnDateFilter != null) {
            btnDateFilter.setOnClickListener(v -> showDateRangePicker());
        }

        if (cardTotalBookings != null) {
            cardTotalBookings.setOnClickListener(v -> openBookingsWithFilter("ALL"));
        }

        if (cardConfirmedBookings != null) {
            cardConfirmedBookings.setOnClickListener(v -> openBookingsWithFilter("CONFIRMED"));
        }

        if (cardPendingBookings != null) {
            cardPendingBookings.setOnClickListener(v -> openBookingsWithFilter("PENDING"));
        }

        if (cardCancelledBookings != null) {
            cardCancelledBookings.setOnClickListener(v -> openBookingsWithFilter("CANCELLED"));
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
            bottomNavigation.setSelectedItemId(R.id.nav_reports);
            setupBottomNavigation(bottomNavigation);
        }

        updateMetrics();
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, ClientsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_reports) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMetrics();
    }

    private void updateMetrics() {
        if (dbHelper == null) return;

        int total = dbHelper.getBookingCountByStatus("ALL");
        int confirmed = dbHelper.getBookingCountByStatus("CONFIRMED");
        int pending = dbHelper.getBookingCountByStatus("PENDING");
        int cancelled = dbHelper.getBookingCountByStatus("CANCELLED");

        if (tvReportTotalCount != null) tvReportTotalCount.setText(String.valueOf(total));
        if (tvReportConfirmedCount != null) tvReportConfirmedCount.setText(String.valueOf(confirmed));
        if (tvReportPendingCount != null) tvReportPendingCount.setText(String.valueOf(pending));
        if (tvReportCancelledCount != null) tvReportCancelledCount.setText(String.valueOf(cancelled));
    }

    private void showDateRangePicker() {
        String[] options = {"July 2026", "June 2026", "Last 30 Days", "Quarter 2 2026", "Year to Date 2026"};
        new AlertDialog.Builder(this)
                .setTitle("Select Report Date Range")
                .setItems(options, (dialog, which) -> {
                    MaterialButton btnDateFilter = findViewById(R.id.btnDateFilter);
                    if (btnDateFilter != null) {
                        btnDateFilter.setText(String.format("%s 📅", options[which]));
                    }
                    updateMetrics();
                    Toast.makeText(this, "Report data updated for " + options[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void openBookingsWithFilter(String filter) {
        Intent intent = new Intent(ReportsActivity.this, BookingsActivity.class);
        intent.putExtra("FILTER", filter);
        startActivity(intent);
    }
}
