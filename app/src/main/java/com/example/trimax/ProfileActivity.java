package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        CardView cardEditProfile = findViewById(R.id.cardEditProfile);
        CardView cardChangePassword = findViewById(R.id.cardChangePassword);
        CardView cardReports = findViewById(R.id.cardReports);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setSelectedItemId(R.id.nav_profile);

        cardEditProfile.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));
        cardChangePassword.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class)));
        cardReports.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, ReportsActivity.class)));

        btnLogout.setOnClickListener(v -> showLogoutDialog());

        setupBottomNavigation(bottomNavigation);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                startActivity(new Intent(this, ClientsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}