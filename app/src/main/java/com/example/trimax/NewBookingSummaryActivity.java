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

import java.util.Locale;

public class NewBookingSummaryActivity extends AppCompatActivity {

    private static final String STATE_REQUEST_ID = "trimax_request_id";

    /**
     * One id per booking attempt, reused on every retry (and kept across screen rotation), so a retry after
     * a time-out returns the booking the server already saved instead of creating a second one.
     */
    private String requestId;
    private boolean submitting;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_REQUEST_ID, requestId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestId = savedInstanceState != null ? savedInstanceState.getString(STATE_REQUEST_ID) : null;
        if (requestId == null) requestId = java.util.UUID.randomUUID().toString();
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_summary);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        Intent intent = getIntent();
        String customerName = intent.getStringExtra("CUSTOMER_NAME");
        String customerPhone = intent.getStringExtra("CUSTOMER_PHONE");
        String customerEmail = intent.getStringExtra("CUSTOMER_EMAIL");
        String service = intent.getStringExtra("SERVICE");
        int quantity = intent.getIntExtra("QUANTITY", 1);
        double selectedPrice = intent.getDoubleExtra("SELECTED_PRICE", 99.00);
        String date = intent.getStringExtra("DATE");
        String time = intent.getStringExtra("TIME");
        String resolvedAddress = intent.getStringExtra("ADDRESS");
        if (resolvedAddress == null || resolvedAddress.trim().isEmpty()) {
            resolvedAddress = intent.getStringExtra("CUSTOMER_ADDRESS");
        }
        final String address = resolvedAddress;

        double pickupLat = 14.7826996;
        double pickupLng = 121.0475865;
        double destLat = intent.getDoubleExtra("DEST_LAT", 0);
        double destLng = intent.getDoubleExtra("DEST_LNG", 0);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvCustomerName = findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        TextView tvService = findViewById(R.id.tvService);
        TextView tvQuantity = findViewById(R.id.tvQuantity);
        TextView tvDateTime = findViewById(R.id.tvDateTime);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);

        MaterialButton btnEdit = findViewById(R.id.btnEdit);
        MaterialButton btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        btnBack.setOnClickListener(v -> navigateBack());
        btnEdit.setOnClickListener(v -> finish());

        if (customerName != null) tvCustomerName.setText(customerName);
        if (customerPhone != null) tvCustomerPhone.setText(customerPhone);
        if (service != null) tvService.setText(service);
        if (tvQuantity != null) tvQuantity.setText(String.format(Locale.US, "%s • Qty: %d", (service != null ? service : "Parcel"), quantity));
        String resolvedDate = date != null && !date.trim().isEmpty() ? date.trim() : "ASAP";
        String resolvedTime = time != null ? time.trim() : "";
        String dateTimeLabel = resolvedTime.isEmpty()
                ? "Date & Time: " + resolvedDate
                : "Date & Time: " + resolvedDate + " • " + resolvedTime;
        if (tvDateTime != null) tvDateTime.setText(dateTimeLabel);
        if (address != null) {
            if (tvAddress != null) tvAddress.setText(address);
            if (tvDeliveryAddress != null) tvDeliveryAddress.setText(address);
        }

        // Estimated from the prices shown on the previous step; the server prices the booking when it is saved.
        double total = intent.getDoubleExtra("ESTIMATED_TOTAL", selectedPrice * quantity);
        tvTotalAmount.setText(String.format(Locale.US, "₱ %,.2f", total));
        final int[] productIds = intent.getIntArrayExtra("PRODUCT_IDS");
        final int[] productQuantities = intent.getIntArrayExtra("PRODUCT_QUANTITIES");

        btnConfirmBooking.setOnClickListener(v -> {
            if (address == null || address.trim().isEmpty()
                    || !Double.isFinite(destLat) || !Double.isFinite(destLng)
                    || destLat < -90 || destLat > 90 || destLng < -180 || destLng > 180
                    || (destLat == 0 && destLng == 0)) {
                Toast.makeText(this, "Please select a valid delivery destination before confirming.", Toast.LENGTH_LONG).show();
                return;
            }
            if (productIds == null || productQuantities == null || productIds.length == 0
                    || productIds.length != productQuantities.length) {
                Toast.makeText(this, "Please go back and select at least 1 product.", Toast.LENGTH_LONG).show();
                return;
            }
            if (submitting) return;   // ignore rapid repeat taps while the first submission is running

            String notes = intent.getStringExtra("NOTES");
            String scheduledTimeStr = resolvedTime.isEmpty()
                    ? resolvedDate
                    : resolvedDate + " • " + resolvedTime;

            // The booking is created on the TRIMAX server: Pending, with no driver. Only an administrator
            // assigns a driver, from the desktop app.
            CreateBookingRequest request = new CreateBookingRequest(requestId);
            request.customerName = customerName;
            request.customerContact = customerPhone;
            request.deliveryAddress = address;
            request.latitude = destLat;
            request.longitude = destLng;
            request.schedule = scheduledTimeStr;
            request.instructions = notes;
            for (int i = 0; i < productIds.length; i++) {
                request.lines.add(new CreateBookingRequest.Line(productIds[i], productQuantities[i]));
            }
            String problem = request.validate();
            if (problem != null) {
                Toast.makeText(this, problem, Toast.LENGTH_LONG).show();
                return;
            }

            submitting = true;
            final CharSequence confirmLabel = btnConfirmBooking.getText();
            btnConfirmBooking.setEnabled(false);
            btnConfirmBooking.setText("Submitting...");
            btnEdit.setEnabled(false);
            TrimaxApi.get(this).createBooking(request, new TrimaxApi.Callback<BookingRecord>() {
                @Override
                public void onSuccess(BookingRecord saved) {
                    if (SessionGuard.isGone(NewBookingSummaryActivity.this)) return;
                    SessionManager session = new SessionManager(NewBookingSummaryActivity.this);
                    DatabaseHelper dbHelper = new DatabaseHelper(NewBookingSummaryActivity.this);
                    // Local conveniences only: the saved-customers list and a notification on this phone.
                    dbHelper.saveCustomer(customerName, customerPhone, customerEmail, address, destLat, destLng);
                    dbHelper.addNotification(session.getUsername(), "Booking Request Submitted",
                            "Booking #" + saved.idText() + " is pending admin confirmation.");

                    Intent confirmedIntent = new Intent(NewBookingSummaryActivity.this, BookingConfirmedActivity.class);
                    confirmedIntent.putExtra("BOOKING_ID", saved.idText());
                    startActivity(confirmedIntent);
                    finishAffinity(); // Close creation flow stack
                }

                @Override
                public void onError(ApiException error) {
                    if (SessionGuard.isGone(NewBookingSummaryActivity.this)) return;
                    submitting = false;
                    btnConfirmBooking.setEnabled(true);
                    btnConfirmBooking.setText(confirmLabel);
                    btnEdit.setEnabled(true);
                    if (SessionGuard.handle(NewBookingSummaryActivity.this, error)) return;
                    String message = error.getMessage();
                    // A time-out or dropped connection can happen after the server saved the booking.
                    // Confirming again from this screen reuses the same request id, so it cannot duplicate it.
                    boolean unsure = error.getKind() == ApiException.Kind.TIMEOUT
                            || error.getKind() == ApiException.Kind.NETWORK;
                    if (unsure) {
                        message += "\n\nThe booking may already have been saved. Tap Confirm again from this "
                                + "screen (before editing anything): it will not create a second booking.";
                    }
                    new androidx.appcompat.app.AlertDialog.Builder(NewBookingSummaryActivity.this)
                            .setTitle(unsure ? "Not sure it was saved" : "Booking not saved")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show();
                }
            });
        });
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
