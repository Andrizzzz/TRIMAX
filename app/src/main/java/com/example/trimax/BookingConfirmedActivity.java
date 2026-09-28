package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class BookingConfirmedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking_confirmed);

        String bookingId = getIntent().getStringExtra("BOOKING_ID");
        TextView tvBookingId = findViewById(R.id.tvBookingId);
        MaterialButton btnViewBooking = findViewById(R.id.btnViewBooking);
        MaterialButton btnBackToDashboard = findViewById(R.id.btnBackToDashboard);

        if (bookingId != null && !bookingId.isEmpty()) {
            tvBookingId.setText("Booking #" + bookingId);
        }

        btnViewBooking.setOnClickListener(v -> {
            Intent intent = new Intent(BookingConfirmedActivity.this, BookingDetailActivity.class);
            intent.putExtra("BOOKING_ID", bookingId != null ? bookingId : "BK-2026-00123");
            startActivity(intent);
            finish();
        });

        btnBackToDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(BookingConfirmedActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        });
    }
}