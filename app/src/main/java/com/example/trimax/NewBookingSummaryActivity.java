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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Package price calculation
        double total = selectedPrice * quantity;
        tvTotalAmount.setText(String.format(Locale.US, "₱ %,.2f", total));

        btnConfirmBooking.setOnClickListener(v -> {
            if (address == null || address.trim().isEmpty()
                    || !Double.isFinite(destLat) || !Double.isFinite(destLng)
                    || destLat < -90 || destLat > 90 || destLng < -180 || destLng > 180
                    || (destLat == 0 && destLng == 0)) {
                Toast.makeText(this, "Please select a valid delivery destination before confirming.", Toast.LENGTH_LONG).show();
                return;
            }
            // Disable immediately after validation to prevent duplicate bookings from rapid taps.
            btnConfirmBooking.setEnabled(false);
            SessionManager session = new SessionManager(this);
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            String generatedBookingId = dbHelper.generateNextBookingId();
            String notes = intent.getStringExtra("NOTES");
            String scheduledTimeStr = resolvedTime.isEmpty()
                    ? resolvedDate
                    : resolvedDate + " • " + resolvedTime;
            String packageDetailsStr = (service != null ? service : "Basic Delivery") + " (x" + quantity + ")";

            Delivery newBooking = new Delivery();
            newBooking.setBookingId(generatedBookingId);
            newBooking.setDriverId(session.getUsername() != null && !session.getUsername().isEmpty() ? session.getUsername() : "agent01");
            newBooking.setCustomerName(customerName != null ? customerName : "Guest Customer");
            newBooking.setCustomerPhone(customerPhone != null ? customerPhone : "N/A");
            newBooking.setPickupLocation("TRIMAX Hub (14.782700, 121.047587)");
            newBooking.setPickupLatitude(pickupLat);
            newBooking.setPickupLongitude(pickupLng);
            newBooking.setDestination(address != null ? address : "San Jose del Monte, Bulacan");
            newBooking.setDestinationLatitude(destLat);
            newBooking.setDestinationLongitude(destLng);
            newBooking.setPackageDetails(packageDetailsStr);
            newBooking.setScheduledTime(scheduledTimeStr);
            newBooking.setSpecialInstructions(notes != null ? notes : "");
            newBooking.setOriginalAmount(total);
            newBooking.setCurrentTotal(total);
            newBooking.setStatus("PENDING");
            newBooking.setPriority("NORMAL");

            dbHelper.saveCustomer(customerName, customerPhone, customerEmail, address, destLat, destLng);
            newBooking.setCustomerId(dbHelper.getCustomerCodeByPhone(customerPhone));
            if (!dbHelper.addBooking(newBooking)) {
                btnConfirmBooking.setEnabled(true);
                Toast.makeText(this, "Booking could not be saved. Please try again.", Toast.LENGTH_LONG).show();
                return;
            }
            dbHelper.addNotification(session.getUsername(), "Booking Request Submitted", "Booking #" + generatedBookingId + " is pending admin confirmation.");

            Intent confirmedIntent = new Intent(NewBookingSummaryActivity.this, BookingConfirmedActivity.class);
            confirmedIntent.putExtra("BOOKING_ID", generatedBookingId);
            startActivity(confirmedIntent);
            finishAffinity(); // Close creation flow stack
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
