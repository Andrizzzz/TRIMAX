package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class BookingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bookings);

        MaterialButton btnNewBooking = findViewById(R.id.btnNewBooking);
        CardView cardBooking1 = findViewById(R.id.cardBooking1);
        CardView cardBooking2 = findViewById(R.id.cardBooking2);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setSelectedItemId(R.id.nav_bookings);

        btnNewBooking.setOnClickListener(v -> {
            Intent intent = new Intent(BookingsActivity.this, NewBookingCustomerActivity.class);
            startActivity(intent);
        });

        cardBooking1.setOnClickListener(v -> openBookingDetail("BK-2026-00123", "Juan Dela Cruz", "PENDING", "₱ 1,024.00"));
        cardBooking2.setOnClickListener(v -> openBookingDetail("BK-2026-00122", "Maria Santos", "CONFIRMED", "₱ 2,450.00"));

        setupBottomNavigation(bottomNavigation);
    }

    private void openBookingDetail(String id, String customer, String status, String amount) {
        Intent intent = new Intent(BookingsActivity.this, BookingDetailActivity.class);
        intent.putExtra("BOOKING_ID", id);
        intent.putExtra("CUSTOMER_NAME", customer);
        intent.putExtra("STATUS", status);
        intent.putExtra("TOTAL_AMOUNT", amount);
        startActivity(intent);
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, ClientsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
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
}