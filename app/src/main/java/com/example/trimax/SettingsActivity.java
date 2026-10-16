package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
        CardView cardThemeSettings = findViewById(R.id.cardThemeSettings);
        tvCurrentThemeSubtitle = findViewById(R.id.tvCurrentThemeSubtitle);

        CardView cardNotificationSettings = findViewById(R.id.cardNotificationSettings);
        CardView cardAccountSettings = findViewById(R.id.cardAccountSettings);
        CardView cardPrivacy = findViewById(R.id.cardPrivacy);
        CardView cardTerms = findViewById(R.id.cardTerms);
        CardView cardRefundPolicy = findViewById(R.id.cardRefundPolicy);
        CardView cardCookiesConsent = findViewById(R.id.cardCookiesConsent);
        CardView cardBusinessDetails = findViewById(R.id.cardBusinessDetails);
        CardView cardAbout = findViewById(R.id.cardAbout);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        updateThemeSubtitle();

        if (cardThemeSettings != null) {
            cardThemeSettings.setOnClickListener(v -> showThemePickerDialog());
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
                        .setTitle("Privacy Policy & Data Protection")
                        .setMessage("TRI-MAX Express Logistics Inc. respects your data privacy:\n\n"
                                + "• Data Collection: We only collect necessary data (Name, Email, Phone, GPS Location for deliveries).\n"
                                + "• Security: All customer and agent credentials are encrypted using industry-standard protocols.\n"
                                + "• Compliance: Fully compliant with Republic Act No. 10173 (Data Privacy Act of 2012).\n"
                                + "• No Third-Party Sharing: Your data is never sold or shared with unauthorized third parties.")
                        .setPositiveButton("I Accept & Agree", null)
                        .show();
            });
        }

        if (cardTerms != null) {
            cardTerms.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Terms & Conditions")
                        .setMessage("Terms of Service for TRI-MAX Agents & Riders:\n\n"
                                + "1. Responsible Delivery: Fulfill assigned customer bookings promptly and professionally.\n"
                                + "2. Active GPS Tracking: Maintain active GPS tracking during delivery hours for dispatch precision.\n"
                                + "3. Confidentiality: Customer contact details must strictly remain confidential.\n"
                                + "4. Service Integrity: Unsupported or fraudulent claims are strictly prohibited.")
                        .setPositiveButton("Accept Terms", null)
                        .show();
            });
        }

        if (cardRefundPolicy != null) {
            cardRefundPolicy.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Refund & Cancellation Policy")
                        .setMessage("Booking Cancellation & Refund Rules:\n\n"
                                + "• Cancellation before Dispatch: 100% full refund credited within 24 hours.\n"
                                + "• Cancellation after Rider En Route: Partial fee may apply to cover rider transit costs.\n"
                                + "• Failed Delivery due to Customer Unavailability: Free re-attempt within 12 hours or return refund.")
                        .setPositiveButton("Got It", null)
                        .show();
            });
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
                        .setMessage("TRI-MAX Express Logistics Inc.\n"
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
            cardAbout.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("About TRI-MAX App")
                        .setMessage("TRI-MAX Mobile Agent System v2.4.0\n\nBOOK • TRACK • DELIVER\nOfficial logistics management application for authorized agents and riders.")
                        .setPositiveButton("OK", null)
                        .show();
            });
        }
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
        String[] options = {"System Default 📱", "Light Mode ☀️", "Dark Mode 🌙"};
        String current = ThemeHelper.getSelectedTheme(this);
        int selectedIndex = 0;
        if (ThemeHelper.THEME_LIGHT.equalsIgnoreCase(current)) selectedIndex = 1;
        else if (ThemeHelper.THEME_DARK.equalsIgnoreCase(current)) selectedIndex = 2;

        new AlertDialog.Builder(this)
                .setTitle("Select App Theme")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    String newMode = ThemeHelper.THEME_SYSTEM;
                    if (which == 1) newMode = ThemeHelper.THEME_LIGHT;
                    else if (which == 2) newMode = ThemeHelper.THEME_DARK;

                    ThemeHelper.setTheme(SettingsActivity.this, newMode);
                    updateThemeSubtitle();
                    Toast.makeText(this, "Theme updated to " + options[which], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
