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

        android.view.View main = findViewById(android.R.id.content);
        if (main != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(main, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }

        String bookingId = getIntent().getStringExtra("BOOKING_ID");
        TextView tvBookingId = findViewById(R.id.tvBookingId);
        MaterialButton btnViewBooking = findViewById(R.id.btnViewBooking);
        MaterialButton btnBackToDashboard = findViewById(R.id.btnBackToDashboard);

        if (bookingId != null && !bookingId.isEmpty()) {
            tvBookingId.setText("Booking #" + bookingId);
        }

        btnViewBooking.setOnClickListener(v -> {
            // Without the new booking's number, open the bookings list (it used to open a sample "BK-2026-00123")
            Intent intent = bookingId != null && !bookingId.isEmpty()
                    ? new Intent(BookingConfirmedActivity.this, BookingDetailActivity.class).putExtra("BOOKING_ID", bookingId)
                    : new Intent(BookingConfirmedActivity.this, BookingsActivity.class);
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