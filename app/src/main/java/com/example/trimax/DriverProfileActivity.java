package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class DriverProfileActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_profile);
        DriverNav.attach(this, R.id.nav_driver_profile);

        session = new SessionManager(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvDriverName = findViewById(R.id.tvDriverName);
        TextView tvDriverEmail = findViewById(R.id.tvDriverEmail);

        CardView cardEditProfile = findViewById(R.id.cardEditProfile);
        CardView cardChangePassword = findViewById(R.id.cardChangePassword);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvDriverName != null) tvDriverName.setText(session.getFullName());
        if (tvDriverEmail != null) tvDriverEmail.setText(session.getEmail());

        if (cardEditProfile != null) {
            cardEditProfile.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        }

        if (cardChangePassword != null) {
            cardChangePassword.setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    session.logoutUser();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}