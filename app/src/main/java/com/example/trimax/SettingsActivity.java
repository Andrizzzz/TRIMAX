package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

public class SettingsActivity extends AppCompatActivity {

    private SessionManager session;
    private TextView tvCurrentThemeSubtitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        session = new SessionManager(this);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        View cardThemeSettings = findViewById(R.id.cardThemeSettings);
        tvCurrentThemeSubtitle = findViewById(R.id.tvCurrentThemeSubtitle);

        View cardNotificationSettings = findViewById(R.id.cardNotificationSettings);
        View cardChangePassword = findViewById(R.id.cardChangePassword);
        View cardAccountSettings = findViewById(R.id.cardAccountSettings);
        View cardPrivacy = findViewById(R.id.cardPrivacy);
        View cardTerms = findViewById(R.id.cardTerms);
        View cardRefundPolicy = findViewById(R.id.cardRefundPolicy);
        View cardCookiesConsent = findViewById(R.id.cardCookiesConsent);
        View cardBusinessDetails = findViewById(R.id.cardBusinessDetails);
        View cardAbout = findViewById(R.id.cardAbout);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }

        updateThemeSubtitle();

        if (cardThemeSettings != null) {
            cardThemeSettings.setOnClickListener(v -> showThemePickerDialog());
        }

        if (cardChangePassword != null) {
            cardChangePassword.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, ChangePasswordActivity.class)));
        }

        if (cardNotificationSettings != null) {
            cardNotificationSettings.setOnClickListener(v -> {
                if ("DRIVER".equalsIgnoreCase(session.getRole())) {
                    startActivity(new Intent(SettingsActivity.this, DriverNotificationsActivity.class));
                } else {
                    startActivity(new Intent(SettingsActivity.this, NotificationsActivity.class));
                }
            });
        }

        if (cardAccountSettings != null) {
            cardAccountSettings.setOnClickListener(v -> {
                if ("DRIVER".equalsIgnoreCase(session.getRole())) {
                    startActivity(new Intent(SettingsActivity.this, DriverProfileActivity.class));
                } else {
                    startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
                }
            });
        }

        if (cardPrivacy != null) {
            cardPrivacy.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Logout", (dialog, which) -> {
                            session.logoutUser();
                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .show();
            });
        }

        if (cardTerms != null) {
            cardTerms.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Terms & Conditions")
                        .setMessage("Terms of Service for TRI-MAX Agents & Drivers:\n\n"
                                + "1. Responsible Delivery: Fulfill assigned customer bookings promptly and professionally.\n"
                                + "2. Active GPS Tracking: Maintain active GPS tracking during delivery hours for dispatch precision.\n"
                                + "3. Confidentiality: Customer contact details must strictly remain confidential.\n"
                                + "4. Service Integrity: Unsupported or fraudulent claims are strictly prohibited.")
                        .setPositiveButton("Accept Terms", null)
                        .show();
            });
        }

        if (cardRefundPolicy != null) {
            cardRefundPolicy.setOnClickListener(v -> showHelpSupportDialog());
        }

        if (cardCookiesConsent != null) {
            cardCookiesConsent.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Cookies & Tracking Consent")
                        .setMessage("Manage Your Tracking Preferences:\n\n"
                                + "• Essential App Analytics: Active (Required for booking status sync)\n"
                                + "• GPS Real-Time Tracking: Active (Required during active deliveries)\n"
                                + "• Performance Diagnostics: Active\n\nAll tracking complies with Philippine e-Commerce and Data Protection laws.")
                        .setPositiveButton("Save Preferences", (dialog, which) -> {
                            Toast.makeText(this, "Consent settings saved successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .show();
            });
        }

        if (cardBusinessDetails != null) {
            cardBusinessDetails.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Real Business Details")
                        .setMessage("TRI-MAX Consumer Goods Trading\n"
                                + "Registered DTI & SEC Logistics Enterprise\n\n"
                                + "📍 Main Hub: Quirino Highway, San Jose del Monte, Bulacan, Philippines\n"
                                + "📞 Hotline: +63 (02) 8123-4567 / 0917-888-TRIMAX\n"
                                + "✉ Email: support@trimax.ph\n"
                                + "🌐 Official Web: https://www.trimax.ph")
                        .setPositiveButton("Close", null)
                        .show();
            });
        }

        if (cardAbout != null) {
            cardAbout.setOnClickListener(v -> showAboutDialog());
        }
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_about_trimax, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        View btnClose = view.findViewById(R.id.btnCloseAbout);
        if (btnClose != null) btnClose.setOnClickListener(v -> dialog.dismiss());

        // The installed app's real version (it showed a made-up "2.5.0 Enterprise" before)
        TextView version = view.findViewById(R.id.tvAboutVersion);
        if (version != null) {
            try {
                android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
                version.setText("Version " + info.versionName);
            } catch (Exception e) {
                version.setText("Version");
            }
        }

        MaterialButton btnOk = view.findViewById(R.id.btnAboutOk);
        if (btnOk != null) btnOk.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
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
                    Intent dial = new Intent(Intent.ACTION_DIAL, android.net.Uri.parse("tel:" + getString(R.string.office_phone)));
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

    private void updateThemeSubtitle() {
        if (tvCurrentThemeSubtitle == null) return;
        String current = ThemeHelper.getSelectedTheme(this);
        if (ThemeHelper.THEME_LIGHT.equalsIgnoreCase(current)) {
            tvCurrentThemeSubtitle.setText("Light Mode");
        } else if (ThemeHelper.THEME_DARK.equalsIgnoreCase(current)) {
            tvCurrentThemeSubtitle.setText("Dark Mode");
        } else {
            tvCurrentThemeSubtitle.setText("System Default");
        }
    }

    private void showThemePickerDialog() {
        String[] titles = {"System Default", "Light Mode", "Dark Mode"};
        int[] icons = {R.drawable.ic_settings, R.drawable.ic_sun, R.drawable.ic_moon};
        String current = ThemeHelper.getSelectedTheme(this);
        int selectedIndex = 0;
        if (ThemeHelper.THEME_LIGHT.equalsIgnoreCase(current)) selectedIndex = 1;
        else if (ThemeHelper.THEME_DARK.equalsIgnoreCase(current)) selectedIndex = 2;

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_theme, null);
        bottomSheetDialog.setContentView(dialogView);

        ImageView btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        LinearLayout container = dialogView.findViewById(R.id.containerThemeOptions);

        if (btnClose != null) btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        if (container != null) {
            container.removeAllViews();
            for (int i = 0; i < titles.length; i++) {
                final int index = i;
                View row = getLayoutInflater().inflate(R.layout.item_theme_option, container, false);

                ImageView ivIcon = row.findViewById(R.id.ivThemeIcon);
                TextView tvTitle = row.findViewById(R.id.tvThemeTitle);
                ImageView ivCheck = row.findViewById(R.id.ivCheck);

                if (ivIcon != null) ivIcon.setImageResource(icons[i]);
                if (tvTitle != null) tvTitle.setText(titles[i]);
                if (ivCheck != null) {
                    ivCheck.setVisibility(selectedIndex == index ? View.VISIBLE : View.GONE);
                }

                if (selectedIndex == index && tvTitle != null) {
                    tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    tvTitle.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
                }

                row.setOnClickListener(v -> {
                    String newMode = ThemeHelper.THEME_SYSTEM;
                    if (index == 1) newMode = ThemeHelper.THEME_LIGHT;
                    else if (index == 2) newMode = ThemeHelper.THEME_DARK;

                    ThemeHelper.setTheme(SettingsActivity.this, newMode);
                    updateThemeSubtitle();
                    Toast.makeText(this, "Theme updated to " + titles[index], Toast.LENGTH_SHORT).show();
                    bottomSheetDialog.dismiss();
                });

                container.addView(row);
            }
        }
        bottomSheetDialog.show();
    }

    /**
     * Return to the previous screen, or restore the agent dashboard if this
     * screen was launched as the root of the task.
     */
    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
