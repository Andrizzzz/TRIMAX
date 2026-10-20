package com.example.trimax;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
    private SessionManager session;
    private String currentBookingId;
    private Delivery currentDelivery;

    // View References
    private TextView tvHeaderTitle;
    private TextView tvStatusBadge;
    private TextView tvService;
    private TextView tvTotalAmount;
    private TextView tvCustomerName;
    private TextView tvPhone;
    private TextView tvPickupAddress;
    private TextView tvLocationAddress;
    private TextView tvParcelDetails;
    private TextView tvSpecialInstructions;
    private View dividerNotes;
    private LinearLayout llNotesSection;
    private TextView tvBookingIdValue;
    private TextView tvScheduleTime;
    private TextView tvAssignedDriverValue;

    private MaterialButton btnViewLocation;
    private MaterialButton btnConfirmBooking;
    private MaterialButton btnCancelBooking;

    private LinearLayout llMainContent;
    private LinearLayout llErrorState;
    private TextView tvErrorTitle;
    private TextView tvErrorMessage;

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
        session = new SessionManager(this);

        Intent intent = getIntent();
        currentBookingId = intent.getStringExtra("BOOKING_ID");
        if (TextUtils.isEmpty(currentBookingId)) {
            currentBookingId = intent.getStringExtra("DELIVERY_ID");
        }
        if (currentBookingId != null) {
            currentBookingId = currentBookingId.trim();
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvService = findViewById(R.id.tvService);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvPhone = findViewById(R.id.tvPhone);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvLocationAddress = findViewById(R.id.tvLocationAddress);
        tvParcelDetails = findViewById(R.id.tvParcelDetails);
        tvSpecialInstructions = findViewById(R.id.tvSpecialInstructions);
        dividerNotes = findViewById(R.id.dividerNotes);
        llNotesSection = findViewById(R.id.llNotesSection);
        tvBookingIdValue = findViewById(R.id.tvBookingIdValue);
        tvScheduleTime = findViewById(R.id.tvScheduleTime);
        tvAssignedDriverValue = findViewById(R.id.tvAssignedDriverValue);

        btnViewLocation = findViewById(R.id.btnViewLocation);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnCancelBooking = findViewById(R.id.btnCancelBooking);

        llMainContent = findViewById(R.id.llMainContent);
        llErrorState = findViewById(R.id.llErrorState);
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        MaterialButton btnErrorBack = findViewById(R.id.btnErrorBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBackToDashboard());
        }

        if (btnErrorBack != null) {
            btnErrorBack.setOnClickListener(v -> navigateBackToDashboard());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateBackToDashboard();
            }
        });

        loadBookingDetails();

        // Phone Dial Action
        if (tvPhone != null) {
            tvPhone.setOnClickListener(v -> {
                String phoneStr = tvPhone.getText() != null ? tvPhone.getText().toString().replaceAll("[^0-9+]", "") : "";
                if (!TextUtils.isEmpty(phoneStr)) {
                    try {
                        Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneStr));
                        startActivity(dialIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(BookingDetailActivity.this, "Dialer app not available on this device", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(BookingDetailActivity.this, "No phone number available for this customer", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // View Location on Map Action
        if (btnViewLocation != null) {
            btnViewLocation.setOnClickListener(v -> {
                if (currentDelivery == null) return;
                Intent locIntent = new Intent(BookingDetailActivity.this, TrackingMapActivity.class);
                locIntent.putExtra("BOOKING_ID", currentDelivery.getBookingId());
                locIntent.putExtra("DELIVERY_ID", currentDelivery.getBookingId());
                locIntent.putExtra("CUSTOMER_NAME", currentDelivery.getCustomerName());
                locIntent.putExtra("DESTINATION", currentDelivery.getDestination());
                locIntent.putExtra("DEST_LAT", currentDelivery.getDestinationLatitude());
                locIntent.putExtra("DEST_LNG", currentDelivery.getDestinationLongitude());
                try {
                    startActivity(locIntent);
                } catch (Exception e) {
                    Toast.makeText(BookingDetailActivity.this, "Unable to open map view", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Confirm Booking Action
        if (btnConfirmBooking != null) {
            btnConfirmBooking.setOnClickListener(v -> {
                if (currentDelivery == null) return;
                String currentStatus = currentDelivery.getStatus() != null ? currentDelivery.getStatus().toUpperCase(Locale.US) : "";
                if (!"PENDING".equals(currentStatus)) {
                    Toast.makeText(this, "Only PENDING bookings can be confirmed", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnConfirmBooking.setEnabled(false);
                boolean success = dbHelper.confirmBooking(currentDelivery.getBookingId());
                btnConfirmBooking.setEnabled(true);

                if (success) {
                    dbHelper.addNotification(session.getUsername(), "Booking Confirmed", "Booking #" + currentDelivery.getBookingId() + " was confirmed.");
                    Toast.makeText(this, "Booking confirmed successfully!", Toast.LENGTH_SHORT).show();
                    loadBookingDetails();
                } else {
                    Toast.makeText(this, "Failed to update booking status in database", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Cancel Booking Action
        if (btnCancelBooking != null) {
            btnCancelBooking.setOnClickListener(v -> {
                if (currentDelivery == null) return;
                String currentStatus = currentDelivery.getStatus() != null ? currentDelivery.getStatus().toUpperCase(Locale.US) : "";
                if (!"PENDING".equals(currentStatus) && !"CONFIRMED".equals(currentStatus)) {
                    Toast.makeText(this, "Only pending or confirmed bookings can be cancelled", Toast.LENGTH_SHORT).show();
                    return;
                }

                new AlertDialog.Builder(this)
                        .setTitle("Cancel Booking")
                        .setMessage(String.format("Are you sure you want to cancel Booking #%s?", currentDelivery.getBookingId()))
                        .setPositiveButton("Confirm Cancellation", (dialog, which) -> {
                            btnCancelBooking.setEnabled(false);
                            boolean success = dbHelper.cancelBooking(currentDelivery.getBookingId());
                            btnCancelBooking.setEnabled(true);

                            if (success) {
                                dbHelper.addNotification(session.getUsername(), "Booking Cancelled", "Booking #" + currentDelivery.getBookingId() + " was cancelled.");
                                Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                                loadBookingDetails();
                            } else {
                                Toast.makeText(this, "Failed to cancel booking in database", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Keep Booking", null)
                        .show();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBookingDetails();
    }

    private void loadBookingDetails() {
        if (!TextUtils.isEmpty(currentBookingId)) {
            currentDelivery = dbHelper.getDeliveryByCode(currentBookingId);
        }

        if (currentDelivery == null) {
            showErrorState("Booking Not Found", "The requested booking ID (" + (currentBookingId != null ? currentBookingId : "None") + ") was not found in database records.");
            return;
        }

        hideErrorState();

        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText("Booking Details");
        }

        if (tvBookingIdValue != null) {
            tvBookingIdValue.setText(currentDelivery.getBookingId());
        }

        if (tvCustomerName != null) {
            tvCustomerName.setText(TextUtils.isEmpty(currentDelivery.getCustomerName()) ? "Customer" : currentDelivery.getCustomerName());
        }

        if (tvPhone != null) {
            String phone = currentDelivery.getCustomerPhone();
            tvPhone.setText(TextUtils.isEmpty(phone) ? "No phone provided" : "📞 " + phone);
        }

        if (tvService != null) {
            String details = currentDelivery.getPackageDetails();
            tvService.setText(TextUtils.isEmpty(details) ? "Package details unavailable" : details);
        }

        if (tvPickupAddress != null) {
            String pickup = currentDelivery.getPickupLocation();
            tvPickupAddress.setText(TextUtils.isEmpty(pickup) ? "Not specified" : pickup);
        }

        if (tvLocationAddress != null) {
            String dest = currentDelivery.getDestination();
            tvLocationAddress.setText(TextUtils.isEmpty(dest) ? "Not specified" : dest);
        }

        if (tvParcelDetails != null) {
            String details = currentDelivery.getPackageDetails();
            tvParcelDetails.setText(TextUtils.isEmpty(details) ? "No parcel details provided" : details);
        }

        if (tvScheduleTime != null) {
            String sched = currentDelivery.getScheduledTime();
            if (TextUtils.isEmpty(sched)) {
                sched = currentDelivery.getCreatedAt();
            }
            tvScheduleTime.setText(TextUtils.isEmpty(sched) ? "Not scheduled" : sched);
        }

        if (tvSpecialInstructions != null) {
            String notes = currentDelivery.getSpecialInstructions();
            if (!TextUtils.isEmpty(notes) && !notes.trim().equalsIgnoreCase("none")) {
                tvSpecialInstructions.setText(notes);
                if (llNotesSection != null) llNotesSection.setVisibility(View.VISIBLE);
                if (dividerNotes != null) dividerNotes.setVisibility(View.VISIBLE);
            } else {
                tvSpecialInstructions.setText("None");
                if (llNotesSection != null) llNotesSection.setVisibility(View.GONE);
                if (dividerNotes != null) dividerNotes.setVisibility(View.GONE);
            }
        }

        if (tvAssignedDriverValue != null) {
            String driver = currentDelivery.getDriverId();
            if (TextUtils.isEmpty(driver) || "Unassigned".equalsIgnoreCase(driver)) {
                tvAssignedDriverValue.setText("Not yet assigned");
                tvAssignedDriverValue.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary));
            } else {
                tvAssignedDriverValue.setText(driver);
                tvAssignedDriverValue.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
            }
        }

        if (tvTotalAmount != null) {
            tvTotalAmount.setText(String.format(Locale.US, "₱ %,.2f", currentDelivery.getCurrentTotal()));
        }

        updateStatusUi(currentDelivery.getStatus());
    }

    private void updateStatusUi(String status) {
        if (tvStatusBadge == null) return;

        String upperStatus = status != null ? status.trim().toUpperCase(Locale.US) : "PENDING";

        if ("PENDING".equals(upperStatus) || upperStatus.isEmpty()) {
            tvStatusBadge.setText("PENDING");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatusBadge.setTextColor(0xFFD97706);
            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.VISIBLE);
            if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.VISIBLE);
            return;
        }

        if ("CANCELLED".equals(upperStatus)) {
            tvStatusBadge.setText("CANCELLED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_cancelled);
            tvStatusBadge.setTextColor(0xFFEF4444);
            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
            if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.GONE);
            return;
        }

        if ("COMPLETED".equals(upperStatus) || "DELIVERED".equals(upperStatus)) {
            tvStatusBadge.setText("COMPLETED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_completed);
            tvStatusBadge.setTextColor(0xFF2563EB);
            if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
            if (btnCancelBooking != null) btnCancelBooking.setVisibility(View.GONE);
            return;
        }

        // Confirmed or in-progress deliveries must never be displayed as PENDING.
        tvStatusBadge.setText(upperStatus.replace('_', ' '));
        tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
        tvStatusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary_blue));
        if (btnConfirmBooking != null) btnConfirmBooking.setVisibility(View.GONE);
        if (btnCancelBooking != null) {
            btnCancelBooking.setVisibility("CONFIRMED".equals(upperStatus) ? View.VISIBLE : View.GONE);
        }
    }

    private void showErrorState(String title, String message) {
        if (llMainContent != null) llMainContent.setVisibility(View.GONE);
        if (llErrorState != null) llErrorState.setVisibility(View.VISIBLE);
        if (tvErrorTitle != null) tvErrorTitle.setText(title);
        if (tvErrorMessage != null) tvErrorMessage.setText(message);
        if (tvHeaderTitle != null) tvHeaderTitle.setText("Booking Error");
        if (tvStatusBadge != null) tvStatusBadge.setVisibility(View.GONE);
    }

    private void hideErrorState() {
        if (llMainContent != null) llMainContent.setVisibility(View.VISIBLE);
        if (llErrorState != null) llErrorState.setVisibility(View.GONE);
        if (tvStatusBadge != null) tvStatusBadge.setVisibility(View.VISIBLE);
    }

    private void navigateBackToDashboard() {
        // Preserve the screen that opened Booking Details (e.g. Bookings list).
        if (isTaskRoot()) {
            Intent intent = new Intent(BookingDetailActivity.this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
