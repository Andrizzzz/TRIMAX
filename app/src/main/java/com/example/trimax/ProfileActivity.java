package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

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

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), 0);
                return insets;
            });
        }

        View cardBottomNavContainer = findViewById(R.id.cardBottomNavContainer);
        if (cardBottomNavContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(cardBottomNavContainer, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                int baseMargin = Math.round(16 * getResources().getDisplayMetrics().density);
                params.bottomMargin = baseMargin + systemBars.bottom;
                v.setLayoutParams(params);
                return insets;
            });
        }

        ImageView ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        if (ivProfilePhoto != null) ivProfilePhoto.setOnClickListener(v -> photoPicker.launch(new String[]{"image/*"}));
        View cardEditProfile = findViewById(R.id.cardEditProfile);
        View cardSettings = findViewById(R.id.cardSettings);
        View cardDeliveryHistory = findViewById(R.id.cardDeliveryHistory);
        View cardHelpSupport = findViewById(R.id.cardHelpSupport);
        View cardReports = findViewById(R.id.cardReports);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        setupBottomNavigation();

        if (cardEditProfile != null) cardEditProfile.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));
        if (cardSettings != null) cardSettings.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, SettingsActivity.class)));
        if (cardDeliveryHistory != null) cardDeliveryHistory.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, BookingsActivity.class)));
        if (cardHelpSupport != null) {
            cardHelpSupport.setOnClickListener(v -> showHelpSupportDialog());
        }
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
        ImageView ivProfilePhoto = findViewById(R.id.ivProfilePhoto);

        String name = session.getFullName();
        String email = session.getEmail();

        if (tvAgentName != null) tvAgentName.setText(name);
        if (tvAgentEmail != null) tvAgentEmail.setText(email);
        if (ivProfilePhoto != null) {
            String photoUri = session.getProfilePhotoUri();
            if (photoUri != null && !photoUri.isEmpty()) {
                try {
                    ivProfilePhoto.setImageURI(Uri.parse(photoUri));
                    ivProfilePhoto.setColorFilter(null);
                } catch (Exception ignored) {
                    ivProfilePhoto.setImageResource(R.drawable.ic_user);
                    ivProfilePhoto.setColorFilter(getColor(R.color.primary_blue));
                }
            } else {
                ivProfilePhoto.setImageResource(R.drawable.ic_user);
                ivProfilePhoto.setColorFilter(getColor(R.color.primary_blue));
            }
        }
    }

    private void handleBackNavigation() {
        // If Profile was opened as the task's first screen, return to the agent dashboard
        // instead of finishing the app and leaving the user on the launcher.
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void showHelpSupportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_help_support, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        View btnClose = view.findViewById(R.id.btnCloseSupport);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        View cardCall = view.findViewById(R.id.cardSupportCall);
        if (cardCall != null) {
            cardCall.setOnClickListener(v -> {
                try {
                    Intent dial = new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:0281234567"));
                    startActivity(dial);
                } catch (Exception ignored) {
                    Toast.makeText(this, "Unable to launch dialer", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View cardEmail = view.findViewById(R.id.cardSupportEmail);
        if (cardEmail != null) {
            cardEmail.setOnClickListener(v -> {
                try {
                    Intent email = new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("mailto:support@trimax.ph"));
                    email.putExtra(Intent.EXTRA_SUBJECT, "TRI-MAX Support Request");
                    startActivity(email);
                } catch (Exception ignored) {
                    Toast.makeText(this, "Unable to launch email app", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
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

    private void setupBottomNavigation() {
        highlightBottomNavItem(R.id.nav_profile);

        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View clients = findViewById(R.id.nav_clients);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) home.setOnClickListener(v -> {
            highlightBottomNavItem(R.id.nav_home);
            v.postDelayed(() -> {
                Intent intent = new Intent(ProfileActivity.this, DashboardActivity.class);
                intent.putExtra("PREV_TAB_INDEX", 4);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 120);
        });
        if (bookings != null) bookings.setOnClickListener(v -> {
            highlightBottomNavItem(R.id.nav_bookings);
            v.postDelayed(() -> {
                Intent intent = new Intent(this, BookingsActivity.class);
                intent.putExtra("PREV_TAB_INDEX", 4);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 120);
        });
        if (packages != null) packages.setOnClickListener(v -> {
            highlightBottomNavItem(R.id.nav_packages);
            v.postDelayed(() -> {
                Intent intent = new Intent(this, PackagesActivity.class);
                intent.putExtra("PREV_TAB_INDEX", 4);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 120);
        });
        if (clients != null) clients.setOnClickListener(v -> {
            highlightBottomNavItem(R.id.nav_clients);
            v.postDelayed(() -> {
                Intent intent = new Intent(this, ClientsActivity.class);
                intent.putExtra("PREV_TAB_INDEX", 4);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }, 120);
        });
        if (profile != null) profile.setOnClickListener(v -> highlightBottomNavItem(R.id.nav_profile));
    }

    private int currentTabIndex = 4;

    private void highlightBottomNavItem(int activeId) {
        int[] ids = {R.id.nav_home, R.id.nav_bookings, R.id.nav_packages, R.id.nav_clients, R.id.nav_profile};
        int activeIndex = 0;
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] == activeId) {
                activeIndex = i;
                break;
            }
        }

        for (int id : ids) {
            View v = findViewById(id);
            if (v != null) {
                boolean isActive = (id == activeId);
                v.setSelected(isActive);
            }
        }

        int prevIndex = getIntent().getIntExtra("PREV_TAB_INDEX", -1);
        getIntent().removeExtra("PREV_TAB_INDEX");

        View indicator = findViewById(R.id.slidingIndicator);
        LinearLayout navContainer = findViewById(R.id.bottomNavigation);
        if (indicator != null && navContainer != null) {
            final int targetIndex = activeIndex;

            navContainer.post(() -> {
                int totalWidth = navContainer.getWidth() - navContainer.getPaddingLeft() - navContainer.getPaddingRight();
                if (totalWidth <= 0) {
                    int screenWidth = getResources().getDisplayMetrics().widthPixels;
                    totalWidth = screenWidth - (int) (52 * getResources().getDisplayMetrics().density);
                }
                int tabWidth = totalWidth / 5;

                ViewGroup.LayoutParams lp = indicator.getLayoutParams();
                int marginHorizontal = (int) (4 * getResources().getDisplayMetrics().density);
                lp.width = tabWidth - (marginHorizontal * 2);
                indicator.setLayoutParams(lp);

                float targetX = navContainer.getPaddingLeft() + (targetIndex * tabWidth) + marginHorizontal;

                if (prevIndex >= 0 && prevIndex != targetIndex) {
                    float startX = navContainer.getPaddingLeft() + (prevIndex * tabWidth) + marginHorizontal;
                    indicator.setTranslationX(startX);
                } else if (indicator.getTranslationX() == 0 && targetIndex > 0) {
                    float startX = navContainer.getPaddingLeft() + (currentTabIndex * tabWidth) + marginHorizontal;
                    indicator.setTranslationX(startX);
                }

                currentTabIndex = targetIndex;

                indicator.animate()
                        .translationX(targetX)
                        .setDuration(260)
                        .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                        .start();
            });
        }
    }
}
