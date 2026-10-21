package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
        // Match DashboardActivity system-bar handling; do not apply the status-bar inset twice.
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
            bottomNavigation.setSelectedItemId(R.id.nav_packages);
            setupBottomNavigation(bottomNavigation);
        }

        MaterialButton btnExportCsv = findViewById(R.id.btnExportCsv);
        MaterialButton btnExportPdf = findViewById(R.id.btnExportPdf);
        if (btnExportCsv != null) btnExportCsv.setOnClickListener(v -> exportCsv());
        if (btnExportPdf != null) btnExportPdf.setOnClickListener(v -> exportPdf());

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

        TextView tvInsights = findViewById(R.id.tvOperationalInsights);
        if (tvInsights != null) {
            int failed = dbHelper.getBookingCountByStatus("FAILED");
            int completed = dbHelper.getBookingCountByStatus("COMPLETED");
            int completionRate = total == 0 ? 0 : Math.round(completed * 100f / total);
            StringBuilder insights = new StringBuilder();
            insights.append("Completion rate: ").append(completionRate).append("%\n");
            if (pending > 0) insights.append("Action needed: ").append(pending).append(" booking(s) are still pending.\n");
            if (failed > 0) insights.append("Review needed: ").append(failed).append(" booking(s) have failed.\n");
            if (cancelled > 0) insights.append("Cancelled bookings: ").append(cancelled).append(". Review cancellation reasons if recorded.\n");
            if (total == 0) insights.append("No bookings found for this period. Insights will update when records are added.");
            else if (pending == 0 && failed == 0 && cancelled == 0) insights.append("No pending, failed, or cancelled bookings detected in this overview.");
            tvInsights.setText(insights.toString().trim());
        }
    }

    private File getReportDirectory() throws Exception {
        File base = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (base == null) throw new Exception("App documents folder is unavailable.");
        File dir = new File(base, "TRIMAX-Reports");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Could not create report folder.");
        return dir;
    }

    private String reportTimestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
    }

    private String csvCell(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private void exportCsv() {
        try {
            List<Delivery> bookings = dbHelper.getAllBookings();
            File file = new File(getReportDirectory(), "trimax_report_" + reportTimestamp() + ".csv");
            StringBuilder csv = new StringBuilder("Booking ID,Customer,Driver,Status,Pickup,Destination,Scheduled Time,Created At,Amount\n");
            for (Delivery d : bookings) {
                csv.append(csvCell(d.getBookingId())).append(',')
                    .append(csvCell(d.getCustomerName())).append(',')
                    .append(csvCell(d.getDriverId())).append(',')
                    .append(csvCell(d.getStatus())).append(',')
                    .append(csvCell(d.getPickupLocation())).append(',')
                    .append(csvCell(d.getDestination())).append(',')
                    .append(csvCell(d.getScheduledTime())).append(',')
                    .append(csvCell(d.getCreatedAt())).append(',')
                    .append(d.getCurrentTotal()).append('\n');
            }
            try (FileOutputStream out = new FileOutputStream(file)) { out.write(csv.toString().getBytes(StandardCharsets.UTF_8)); }
            Toast.makeText(this, "CSV report saved in app Documents/TRIMAX-Reports", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "CSV export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String safePdf(String value, int maxLength) {
        String safe = value == null ? "" : value.replaceAll("[\\r\\n]+", " ");
        return safe.length() > maxLength ? safe.substring(0, maxLength) : safe;
    }

    private void exportPdf() {
        PdfDocument document = new PdfDocument();
        try {
            List<Delivery> bookings = dbHelper.getAllBookings();
            final int pageWidth = 595, pageHeight = 842, margin = 36, lineHeight = 18;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            PdfDocument.Page page = null;
            Canvas canvas = null;
            int y = 0, pageNumber = 1;
            for (int i = 0; i < Math.max(1, bookings.size()); i++) {
                if (page == null || y > pageHeight - margin - lineHeight) {
                    if (page != null) document.finishPage(page);
                    page = document.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create());
                    canvas = page.getCanvas(); y = margin;
                    paint.setColor(Color.rgb(30, 64, 110)); paint.setTextSize(18); paint.setFakeBoldText(true);
                    canvas.drawText("TRIMAX Reports & Analytics", margin, y, paint); y += 24;
                    paint.setColor(Color.DKGRAY); paint.setTextSize(10); paint.setFakeBoldText(false);
                    canvas.drawText("Generated: " + new Date().toString(), margin, y, paint); y += 20;
                    canvas.drawText("Total bookings: " + bookings.size(), margin, y, paint); y += 22;
                    paint.setFakeBoldText(true); canvas.drawText("Booking ID | Customer | Status | Driver | Amount", margin, y, paint); y += lineHeight;
                    paint.setFakeBoldText(false);
                }
                if (bookings.isEmpty()) { canvas.drawText("No booking records available.", margin, y, paint); }
                else {
                    Delivery d = bookings.get(i);
                    String line = safePdf(d.getBookingId(), 14) + " | " + safePdf(d.getCustomerName(), 17) + " | " + safePdf(d.getStatus(), 12) + " | " + safePdf(d.getDriverId(), 10) + " | " + String.format(Locale.US, "%.2f", d.getCurrentTotal());
                    canvas.drawText(line, margin, y, paint);
                }
                y += lineHeight;
            }
            if (page != null) document.finishPage(page);
            File file = new File(getReportDirectory(), "trimax_report_" + reportTimestamp() + ".pdf");
            try (FileOutputStream out = new FileOutputStream(file)) { document.writeTo(out); }
            Toast.makeText(this, "PDF report saved in app Documents/TRIMAX-Reports", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "PDF export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally { document.close(); }
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
