package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        ImageView btnNotifications = findViewById(R.id.btnNotifications);
        TextView tvViewAll = findViewById(R.id.tvViewAll);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setSelectedItemId(R.id.nav_home);

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, NotificationsActivity.class)));
        }

        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, BookingsActivity.class)));
        }

        setupBottomNavigation(bottomNavigation);
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingsActivity.class));
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, ClientsActivity.class));
                return true;
            } else if (itemId == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }
}