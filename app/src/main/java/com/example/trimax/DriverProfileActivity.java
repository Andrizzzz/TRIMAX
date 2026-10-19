package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class DriverProfileActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_profile);

        // Keep screen content clear of the status and navigation bars.
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        session = new SessionManager(this);

        ActivityResultLauncher<String[]> photoPicker = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (SecurityException ignored) {
                            // Some document providers do not offer persistable URI grants.
                        }
                        session.setProfilePhotoUri(uri.toString());
                        updateProfileHeader();
                    }
                });

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView ivDriverProfilePhoto = findViewById(R.id.ivDriverProfilePhoto);
        if (ivDriverProfilePhoto != null) ivDriverProfilePhoto.setOnClickListener(v -> photoPicker.launch(new String[]{"image/*"}));
        TextView tvDriverName = findViewById(R.id.tvDriverName);
        TextView tvDriverEmail = findViewById(R.id.tvDriverEmail);

        CardView cardEditProfile = findViewById(R.id.cardEditProfile);
        CardView cardChangePassword = findViewById(R.id.cardChangePassword);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        updateProfileHeader();

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

    private void updateProfileHeader() {
        TextView tvDriverName = findViewById(R.id.tvDriverName);
        TextView tvDriverEmail = findViewById(R.id.tvDriverEmail);
        ImageView photo = findViewById(R.id.ivDriverProfilePhoto);
        if (tvDriverName != null) tvDriverName.setText(session.getFullName());
        if (tvDriverEmail != null) tvDriverEmail.setText(session.getEmail());
        if (photo != null) {
            String photoUri = session.getProfilePhotoUri();
            if (photoUri != null && !photoUri.isEmpty()) {
                try {
                    photo.setImageURI(Uri.parse(photoUri));
                    photo.setColorFilter(null);
                } catch (Exception ignored) {
                    photo.setImageResource(R.drawable.ic_user);
                    photo.setColorFilter(getColor(R.color.primary_blue));
                }
            } else {
                photo.setImageResource(R.drawable.ic_user);
                photo.setColorFilter(getColor(R.color.primary_blue));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateProfileHeader();
    }

    private void handleBackNavigation() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DriverDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
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
