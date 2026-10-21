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

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
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

        ImageView ivDriverProfilePhoto = findViewById(R.id.ivDriverProfilePhoto);
        if (ivDriverProfilePhoto != null) ivDriverProfilePhoto.setOnClickListener(v -> photoPicker.launch(new String[]{"image/*"}));

        CardView cardEditProfile = findViewById(R.id.cardEditProfile);
        CardView cardChangePassword = findViewById(R.id.cardChangePassword);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
            setupBottomNavigation(bottomNavigation);
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(DriverProfileActivity.this, DriverDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
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

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        if (bottomNavigation.getMenu().findItem(R.id.nav_bookings) != null) {
            bottomNavigation.getMenu().findItem(R.id.nav_bookings).setTitle("Deliveries");
        }
        if (bottomNavigation.getMenu().findItem(R.id.nav_clients) != null) {
            bottomNavigation.getMenu().findItem(R.id.nav_clients).setTitle("Notifications");
        }
        if (bottomNavigation.getMenu().findItem(R.id.nav_packages) != null) {
            bottomNavigation.getMenu().findItem(R.id.nav_packages).setTitle("Delivery History");
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, DriverDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                startActivity(new Intent(this, DriverDeliveriesActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_packages) {
                startActivity(new Intent(this, DriverHistoryActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, DriverNotificationsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
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
