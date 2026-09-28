package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ClientsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clients);

        CardView cardClient1 = findViewById(R.id.cardClient1);
        CardView cardClient2 = findViewById(R.id.cardClient2);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setSelectedItemId(R.id.nav_clients);

        cardClient1.setOnClickListener(v -> openClientDetail("Juan Dela Cruz", "0917 123 4567", "juan@gmail.com", "3"));
        cardClient2.setOnClickListener(v -> openClientDetail("Maria Santos", "0918 987 6543", "maria@gmail.com", "5"));

        setupBottomNavigation(bottomNavigation);
    }

    private void openClientDetail(String name, String phone, String email, String count) {
        Intent intent = new Intent(ClientsActivity.this, ClientDetailActivity.class);
        intent.putExtra("CLIENT_NAME", name);
        intent.putExtra("CLIENT_PHONE", phone);
        intent.putExtra("CLIENT_EMAIL", email);
        intent.putExtra("BOOKINGS_COUNT", count);
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
                startActivity(new Intent(this, BookingsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_clients) {
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