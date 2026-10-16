package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
        String service = intent.getStringExtra("SERVICE");
        int quantity = intent.getIntExtra("QUANTITY", 1);
        String date = intent.getStringExtra("DATE");
        String time = intent.getStringExtra("TIME");
        String address = intent.getStringExtra("ADDRESS");

        double pickupLat = intent.getDoubleExtra("PICKUP_LAT", 14.8115);
        double pickupLng = intent.getDoubleExtra("PICKUP_LNG", 121.0453);
        double destLat = intent.getDoubleExtra("DEST_LAT", 14.8150);
        double destLng = intent.getDoubleExtra("DEST_LNG", 121.0500);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvCustomerName = findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        TextView tvService = findViewById(R.id.tvService);
        TextView tvQuantity = findViewById(R.id.tvQuantity);
        TextView tvDateTime = findViewById(R.id.tvDateTime);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);

        MaterialButton btnEdit = findViewById(R.id.btnEdit);
        MaterialButton btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        btnBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> finish());

        if (customerName != null) tvCustomerName.setText(customerName);
        if (customerPhone != null) tvCustomerPhone.setText(customerPhone);
        if (service != null) tvService.setText(String.format("Service: %s", service));
        tvQuantity.setText(String.format(Locale.US, "Quantity: %d", quantity));
        tvDateTime.setText(String.format("Date & Time: %s • %s", (date != null ? date : "July 19, 2026"), (time != null ? time : "10:00 AM")));
        if (address != null) tvAddress.setText(address);

        // Simulated total price calculation
        double pricePerUnit = 512.00;
        double total = pricePerUnit * quantity;
        tvTotalAmount.setText(String.format(Locale.US, "₱ %,.2f", total));

        btnConfirmBooking.setOnClickListener(v -> {
            SessionManager session = new SessionManager(this);
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            String generatedBookingId = dbHelper.generateNextBookingId();
            String notes = intent.getStringExtra("NOTES");
            String scheduledTimeStr = (date != null ? date : "July 19, 2026") + " • " + (time != null ? time : "10:00 AM");
            String packageDetailsStr = (service != null ? service : "Standard Cargo") + " (x" + quantity + ")";

            Delivery newBooking = new Delivery();
            newBooking.setBookingId(generatedBookingId);
            newBooking.setDriverId(session.getUsername() != null && !session.getUsername().isEmpty() ? session.getUsername() : "agent01");
            newBooking.setCustomerName(customerName != null ? customerName : "Guest Customer");
            newBooking.setCustomerPhone(customerPhone != null ? customerPhone : "N/A");
            newBooking.setPickupLocation("TRI-MAX Hub, San Jose del Monte");
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

            dbHelper.addBooking(newBooking);
            dbHelper.addNotification(session.getUsername(), "New Booking Submitted", "Booking #" + generatedBookingId + " created for " + (customerName != null ? customerName : "Customer"));

            Intent confirmedIntent = new Intent(NewBookingSummaryActivity.this, BookingConfirmedActivity.class);
            confirmedIntent.putExtra("BOOKING_ID", generatedBookingId);
            startActivity(confirmedIntent);
            finishAffinity(); // Close creation flow stack
        });
    }
}
