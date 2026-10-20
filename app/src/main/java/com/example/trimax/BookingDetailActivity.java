package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class BookingDetailActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private String currentBookingId;

    private TextView tvHeaderTitle;
    private TextView tvStatusBadge;
    private TextView tvCustomerName;
    private TextView tvPhone;
    private TextView tvService;
    private TextView tvScheduleTime;
    private TextView tvLocationAddress;
    private TextView tvSpecialInstructions;
    private TextView tvTotalAmount;
    private TextView tvPickupAddress, tvBookingId, tvAssignedDriver;

    private MaterialButton btnViewLocation;
    private MaterialButton btnConfirmBooking;
    private MaterialButton btnCancelBooking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking_detail);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        dbHelper = new DatabaseHelper(this);

        Intent intent = getIntent();
        currentBookingId = intent.getStringExtra("BOOKING_ID");
        if (currentBookingId == null || currentBookingId.isEmpty()) {
            currentBookingId = "BK-2026-00123";
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvPhone = findViewById(R.id.tvPhone);
        tvService = findViewById(R.id.tvService);
        tvScheduleTime = findViewById(R.id.tvScheduleTime);
        tvLocationAddress = findViewById(R.id.tvLocationAddress);
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvBookingId = findViewById(R.id.tvBookingId);
        tvAssignedDriver = findViewById(R.id.tvAssignedDriver);

        btnViewLocation = findViewById(R.id.btnViewLocation);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBackToDashboard());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBackToDashboard();
            }
        });

        loadBookingDetails();

        // Phone dial action
        if (tvPhone != null) {
            tvPhone.setOnClickListener(v -> {
                String phone = tvPhone.getText() != null ? tvPhone.getText().toString().replaceAll("[^0-9+]", "") : "09171234567";
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                startActivity(dialIntent);
            });
        }

        // View location on Map
        if (btnViewLocation != null) {
            btnViewLocation.setOnClickListener(v -> {
                Intent locIntent = new Intent(BookingDetailActivity.this, NewBookingLocationActivity.class);
                locIntent.putExtra("CUSTOMER_NAME", tvCustomerName != null && tvCustomerName.getText() != null ? tvCustomerName.getText().toString() : "Customer");
                startActivity(locIntent);
            });
        }

        // Confirm Booking
        if (btnConfirmBooking != null) {
            btnConfirmBooking.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);
                boolean success = dbHelper.confirmBooking(currentBookingId);
                if (success) {
                    updateStatusUi("CONFIRMED");
                    dbHelper.addNotification(session.getUsername(), "Booking Confirmed", "Booking #" + currentBookingId + " was confirmed.");
                    Toast.makeText(this, "Booking confirmed successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to update booking status", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Cancel Booking with AlertDialog
        if (btnCancelBooking != null) {
            btnCancelBooking.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);
                new AlertDialog.Builder(this)
                        .setTitle("Cancel Booking")
                        .setMessage(String.format("Are you sure you want to cancel Booking #%s?", currentBookingId))
                        .setPositiveButton("Confirm Cancellation", (dialog, which) -> {
                            boolean success = dbHelper.cancelBooking(currentBookingId);
                            if (success) {
                                updateStatusUi("CANCELLED");
                                dbHelper.addNotification(session.getUsername(), "Booking Cancelled", "Booking #" + currentBookingId + " was cancelled.");
                                Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to cancel booking", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Keep Booking", null)
                        .show();
            });
        }
    }

    private void loadBookingDetails() {
        Delivery delivery = dbHelper.getDeliveryByCode(currentBookingId);

        if (delivery != null) {
            if (tvHeaderTitle != null) tvHeaderTitle.setText(String.format("Booking #%s", delivery.getBookingId()));
            if (tvCustomerName != null) tvCustomerName.setText(safeValue(delivery.getCustomerName(), "Customer"));
            if (tvBookingId != null) tvBookingId.setText("Booking ID: " + safeValue(delivery.getBookingId(), currentBookingId));
            if (tvPickupAddress != null) tvPickupAddress.setText(safeValue(delivery.getPickupLocation(), "Pickup address not provided"));
            if (tvAssignedDriver != null) tvAssignedDriver.setText("Assigned Driver: " + safeValue(delivery.getDriverId(), "Not yet assigned"));
            if (tvPhone != null) tvPhone.setText(String.format("📞 %s", delivery.getCustomerPhone()));
            if (tvService != null) tvService.setText(String.format("Service: %s", delivery.getPackageDetails()));
            if (tvScheduleTime != null) tvScheduleTime.setText(String.format("Schedule: %s", delivery.getScheduledTime()));
            if (tvLocationAddress != null) tvLocationAddress.setText(safeValue(delivery.getDestination(), "Delivery address not provided"));

            if (tvSpecialInstructions != null) {
                String notes = delivery.getSpecialInstructions();
                if (notes != null && !notes.trim().isEmpty()) {
                    tvSpecialInstructions.setText(notes);
                } else {
                    tvSpecialInstructions.setText("None");
                }
            }

            if (tvTotalAmount != null) {
                tvTotalAmount.setText(String.format(Locale.US, "₱ %,.2f", delivery.getCurrentTotal()));
            }

            updateStatusUi(delivery.getStatus());
        } else {
            // Intent fallback if not found in database
            Intent intent = getIntent();
            String name = intent.getStringExtra("CUSTOMER_NAME");
            String status = intent.getStringExtra("STATUS");
            String amount = intent.getStringExtra("TOTAL_AMOUNT");

            if (tvHeaderTitle != null) tvHeaderTitle.setText(String.format("Booking #%s", currentBookingId));
            if (tvCustomerName != null && name != null) tvCustomerName.setText(name);
            if (tvTotalAmount != null && amount != null) tvTotalAmount.setText(amount);

            updateStatusUi(status != null ? status : "PENDING");
        }
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void updateStatusUi(String status) {
        if (tvStatusBadge == null) return;

        String upperStatus = status != null ? status.toUpperCase(Locale.US) : "PENDING";

        if ("CONFIRMED".equals(upperStatus)) {
            tvStatusBadge.setText("CONFIRMED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);

            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
            if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.VISIBLE);

        } else if ("CANCELLED".equals(upperStatus)) {
            tvStatusBadge.setText("CANCELLED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_cancelled);
            tvStatusBadge.setTextColor(0xFFEF4444);

            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
            if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.GONE);

        } else if ("COMPLETED".equals(upperStatus) || "DELIVERED".equals(upperStatus)) {
            tvStatusBadge.setText("COMPLETED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed);
            tvStatusBadge.setTextColor(0xFF2563EB);

            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
            if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.GONE);

        } else { // PENDING or default
            tvStatusBadge.setText("PENDING");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatusBadge.setTextColor(0xFFD97706);

            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.VISIBLE);
            if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.VISIBLE);
        }
    }

    private void navigateBackToDashboard() {
        Intent intent = new Intent(BookingDetailActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
