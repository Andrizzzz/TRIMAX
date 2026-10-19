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

import java.util.Calendar;

public class ReportsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;

    private TextView tvReportTotalCount;
    private TextView tvReportConfirmedCount;
    private TextView tvReportPendingCount;
    private TextView tvReportCancelledCount;
    private int selectedDateRange = -1; // -1 means all time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reports);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnDateFilter = findViewById(R.id.btnDateFilter);

        tvReportTotalCount = findViewById(R.id.tvReportTotalCount);
        tvReportConfirmedCount = findViewById(R.id.tvReportConfirmedCount);
        tvReportPendingCount = findViewById(R.id.tvReportPendingCount);
        tvReportCancelledCount = findViewById(R.id.tvReportCancelledCount);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }

        if (btnDateFilter != null) {
            btnDateFilter.setOnClickListener(v -> showDateRangePicker());
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

        int total;
        int confirmed;
        int pending;
        int cancelled;

        if (selectedDateRange == -1) {
            total = dbHelper.getBookingCountByStatus("ALL");
            confirmed = dbHelper.getBookingCountByStatus("CONFIRMED");
            pending = dbHelper.getBookingCountByStatus("PENDING");
            cancelled = dbHelper.getBookingCountByStatus("CANCELLED");
        } else {
            long[] range = getSelectedDateBounds(selectedDateRange);
            total = dbHelper.getBookingCountByStatusAndDateRange("ALL", range[0], range[1]);
            confirmed = dbHelper.getBookingCountByStatusAndDateRange("CONFIRMED", range[0], range[1]);
            pending = dbHelper.getBookingCountByStatusAndDateRange("PENDING", range[0], range[1]);
            cancelled = dbHelper.getBookingCountByStatusAndDateRange("CANCELLED", range[0], range[1]);
        }

        if (tvReportTotalCount != null) tvReportTotalCount.setText(String.valueOf(total));
        if (tvReportConfirmedCount != null) tvReportConfirmedCount.setText(String.valueOf(confirmed));
        if (tvReportPendingCount != null) tvReportPendingCount.setText(String.valueOf(pending));
        if (tvReportCancelledCount != null) tvReportCancelledCount.setText(String.valueOf(cancelled));
    }

    private void showDateRangePicker() {
        String[] options = {"All Time", "This Month", "Last Month", "Last 30 Days", "This Quarter", "Year to Date"};
        new AlertDialog.Builder(this)
                .setTitle("Select Report Date Range")
                .setItems(options, (dialog, which) -> {
                    selectedDateRange = which == 0 ? -1 : which;
                    MaterialButton btnDateFilter = findViewById(R.id.btnDateFilter);
                    if (btnDateFilter != null) {
                        btnDateFilter.setText(options[which] + " 📅");
                    }
                    updateMetrics();
                    Toast.makeText(this, "Report data filtered: " + options[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private long[] getSelectedDateBounds(int rangeOption) {
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        switch (rangeOption) {
            case 1: // This Month
                start.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case 2: // Last Month
                // Set day to 1 before changing months to avoid Calendar rollover on dates like March 31.
                start.set(Calendar.DAY_OF_MONTH, 1);
                start.add(Calendar.MONTH, -1);
                end.set(Calendar.DAY_OF_MONTH, 1);
                end.add(Calendar.DAY_OF_MONTH, -1);
                end.set(Calendar.HOUR_OF_DAY, 23);
                end.set(Calendar.MINUTE, 59);
                end.set(Calendar.SECOND, 59);
                end.set(Calendar.MILLISECOND, 999);
                break;
            case 3: // Last 30 Days, including today
                start.add(Calendar.DAY_OF_YEAR, -29);
                break;
            case 4: // This Quarter
                int currentMonth = start.get(Calendar.MONTH);
                int firstMonthOfQuarter = (currentMonth / 3) * 3;
                start.set(Calendar.MONTH, firstMonthOfQuarter);
                start.set(Calendar.DAY_OF_MONTH, 1);
                break;
            case 5: // Year to Date
                start.set(Calendar.MONTH, Calendar.JANUARY);
                start.set(Calendar.DAY_OF_MONTH, 1);
                break;
            default:
                start.set(Calendar.DAY_OF_MONTH, 1);
                break;
        }
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        return new long[]{start.getTimeInMillis(), end.getTimeInMillis()};
    }

    private void openBookingsWithFilter(String filter) {
        Intent intent = new Intent(ReportsActivity.this, BookingsActivity.class);
        intent.putExtra("FILTER", filter);
        startActivity(intent);
    }

    /**
     * Return to the previous screen, or restore the agent dashboard if this
     * screen was launched as the root of the task.
     */
    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
