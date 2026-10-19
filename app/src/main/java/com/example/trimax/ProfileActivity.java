package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        session = new SessionManager(this);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), 0);
                return insets;
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        View cardEditProfile = findViewById(R.id.cardEditProfile);
        View cardChangePassword = findViewById(R.id.cardChangePassword);
        View cardReports = findViewById(R.id.cardReports);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (bottomNavigation != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
            setupBottomNavigation(bottomNavigation);
        }

        if (cardEditProfile != null) cardEditProfile.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));
        if (cardChangePassword != null) cardChangePassword.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class)));
        if (cardReports != null) cardReports.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, ReportsActivity.class)));

        if (btnLogout != null) btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileHeader();
    }

    private void updateProfileHeader() {
        TextView tvAgentName = findViewById(R.id.tvAgentName);
        TextView tvAgentEmail = findViewById(R.id.tvAgentEmail);

        String name = session.getFullName();
        String email = session.getEmail();

        if (tvAgentName != null) tvAgentName.setText(name);
        if (tvAgentEmail != null) tvAgentEmail.setText(email);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    session.logoutUser();

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
            } else if (itemId == R.id.nav_reports) {
                startActivity(new Intent(this, ReportsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}
