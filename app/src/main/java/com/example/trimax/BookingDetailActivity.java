package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class BookingDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking_detail);

        Intent intent = getIntent();
        String bookingId = intent.getStringExtra("BOOKING_ID");
        String customerName = intent.getStringExtra("CUSTOMER_NAME");
        String status = intent.getStringExtra("STATUS");
        String amount = intent.getStringExtra("TOTAL_AMOUNT");

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        TextView tvStatusBadge = findViewById(R.id.tvStatusBadge);
        TextView tvCustomerName = findViewById(R.id.tvCustomerName);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);
        MaterialButton btnViewLocation = findViewById(R.id.btnViewLocation);
        MaterialButton btnCancelBooking = findViewById(R.id.btnCancelBooking);

        btnBack.setOnClickListener(v -> finish());

        if (bookingId != null) tvHeaderTitle.setText("Booking #" + bookingId);
        if (customerName != null) tvCustomerName.setText(customerName);
        if (amount != null) tvTotalAmount.setText(amount);

        if ("CONFIRMED".equalsIgnoreCase(status)) {
            tvStatusBadge.setText("CONFIRMED");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvStatusBadge.setTextColor(0xFF059669);
        } else {
            tvStatusBadge.setText("PENDING");
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending);
            tvStatusBadge.setTextColor(0xFFD97706);
        }

        btnViewLocation.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:14.8115,121.0453?q=San+Jose+del+Monte+Bulacan");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(this, "Opening map location...", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelBooking.setOnClickListener(v -> {
            Toast.makeText(this, "Booking cancellation requested", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}