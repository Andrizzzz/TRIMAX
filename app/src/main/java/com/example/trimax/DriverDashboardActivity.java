package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.util.List;

public class DriverDashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private SessionManager session;
    private DatabaseHelper dbHelper;
    private LocationHelper locationHelper;

    private TextView tvDutyStatusPill;
    private MaterialButton btnToggleDuty;
    private TextView tvGpsTrackingLabel;
    private TextView tvLat, tvLng, tvAccuracy;
    private TextView tvDelivBadge;
    private MockMapView driverDashboardMapView;

    // Hero Assigned Delivery Card Views
    private CardView cardAssignedDeliveryHero;
    private TextView tvHeroBookingId, tvHeroSequence, tvHeroStatusPill;
    private TextView tvHeroCustomerName, tvHeroPickup, tvHeroDestination, tvHeroSpecialInstructions;
    private MaterialButton btnHeroViewDetails, btnHeroStartDelivery, btnHeroReportProblem;

    private Delivery currentHeroDelivery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_dashboard);

        session = new SessionManager(this);

        // Role-based Navigation Access Protection Guard
        if (session.isLoggedIn() && "AGENT".equalsIgnoreCase(session.getRole())) {
            Intent redirectIntent = new Intent(DriverDashboardActivity.this, DashboardActivity.class);
            startActivity(redirectIntent);
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        locationHelper = new LocationHelper(this);

        drawerLayout = findViewById(R.id.drawerLayout);
        View mainContent = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);

        if (mainContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Match Rider dashboard: apply top inset to content, but let bottom nav handle bottom inset.
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), 0);
                return insets;
            });
        }

        ImageView btnMenu = findViewById(R.id.btnMenu);
        NavigationView navigationView = findViewById(R.id.navigationView);

        TextView tvDriverGreeting = findViewById(R.id.tvDriverGreeting);
        ImageView btnDriverNotifications = findViewById(R.id.btnDriverNotifications);

        tvDutyStatusPill = findViewById(R.id.tvDutyStatusPill);
        btnToggleDuty = findViewById(R.id.btnToggleDuty);
        tvGpsTrackingLabel = findViewById(R.id.tvGpsTrackingLabel);
        tvLat = findViewById(R.id.tvLat);
        tvLng = findViewById(R.id.tvLng);
        tvAccuracy = findViewById(R.id.tvAccuracy);

        // FoodPanda Style Driver Live Map Views
        driverDashboardMapView = findViewById(R.id.driverDashboardMapView);
        MaterialButton btnTrackFullMap = findViewById(R.id.btnTrackFullMap);

        if (driverDashboardMapView != null) {
            driverDashboardMapView.startLiveDeliveryAnimation();
        }

        if (btnTrackFullMap != null) {
            btnTrackFullMap.setOnClickListener(v -> {
                Intent intent = new Intent(this, DriverMapActivity.class);
                intent.putExtra("DELIVERY_ID", currentHeroDelivery != null ? currentHeroDelivery.getBookingId() : "TRX-1024");
                startActivity(intent);
            });
        }

        // Hero Card
        cardAssignedDeliveryHero = findViewById(R.id.cardAssignedDeliveryHero);
        tvHeroBookingId = findViewById(R.id.tvHeroBookingId);
        tvHeroSequence = findViewById(R.id.tvHeroSequence);
        tvHeroStatusPill = findViewById(R.id.tvHeroStatusPill);
        tvHeroCustomerName = findViewById(R.id.tvHeroCustomerName);
        tvHeroPickup = findViewById(R.id.tvHeroPickup);
        tvHeroDestination = findViewById(R.id.tvHeroDestination);
        tvHeroSpecialInstructions = findViewById(R.id.tvHeroSpecialInstructions);

        btnHeroViewDetails = findViewById(R.id.btnHeroViewDetails);
        btnHeroStartDelivery = findViewById(R.id.btnHeroStartDelivery);
        btnHeroReportProblem = findViewById(R.id.btnHeroReportProblem);

        CardView cardAssignedDeliveries = findViewById(R.id.cardAssignedDeliveries);
        CardView cardDriverMap = findViewById(R.id.cardDriverMap);
        CardView cardDeliveryHistory = findViewById(R.id.cardDeliveryHistory);
        CardView cardEmergencySupport = findViewById(R.id.cardEmergencySupport);

        tvDelivBadge = findViewById(R.id.tvDelivBadge);

        String driverName = session.getFullName();
        if (tvDriverGreeting != null) {
            tvDriverGreeting.setText(String.format("Good morning, %s 👋", driverName));
        }

        // Navigation Drawer
        if (btnMenu != null && drawerLayout != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });

            // This menu is shared with the agent dashboard, so label it for driver tasks.
            bottomNavigation.getMenu().findItem(R.id.nav_bookings).setTitle("Deliveries");
            bottomNavigation.getMenu().findItem(R.id.nav_clients).setTitle("Notifications");
            bottomNavigation.getMenu().findItem(R.id.nav_reports).setTitle("Reports");

            bottomNavigation.setSelectedItemId(R.id.nav_home);
            setupBottomNavigation(bottomNavigation);
        }

        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvNavName = headerView.findViewById(R.id.tvNavHeaderName);
                TextView tvNavEmail = headerView.findViewById(R.id.tvNavHeaderEmail);
                if (tvNavName != null) tvNavName.setText(driverName);
                if (tvNavEmail != null) tvNavEmail.setText(session.getEmail());

                headerView.setOnClickListener(v -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    startActivity(new Intent(this, DriverProfileActivity.class));
                });
            }

            navigationView.setNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                drawerLayout.closeDrawer(GravityCompat.START);

                if (itemId == R.id.drawer_home) {
                    return true;
                } else if (itemId == R.id.drawer_bookings) {
                    startActivity(new Intent(this, DriverDeliveriesActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_notifications) {
                    startActivity(new Intent(this, DriverNotificationsActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_reports) {
                    startActivity(new Intent(this, DriverHistoryActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_profile) {
                    startActivity(new Intent(this, DriverProfileActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_settings) {
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_logout) {
                    showLogoutDialog();
                    return true;
                }
                return false;
            });
        }

        // Duty Toggle
        if (btnToggleDuty != null) {
            btnToggleDuty.setOnClickListener(v -> toggleDriverDuty());
        }

        // Dark Mode Toggle
        ImageView btnThemeToggle = findViewById(R.id.btnThemeToggle);
        if (btnThemeToggle != null) {
            btnThemeToggle.setOnClickListener(v -> toggleDarkMode());
        }

        // Top / Card Clicks
        if (btnDriverNotifications != null) {
            btnDriverNotifications.setOnClickListener(v -> startActivity(new Intent(this, DriverNotificationsActivity.class)));
        }

        if (cardAssignedDeliveries != null) {
            cardAssignedDeliveries.setOnClickListener(v -> startActivity(new Intent(this, DriverDeliveriesActivity.class)));
        }

        if (cardDriverMap != null) {
            cardDriverMap.setOnClickListener(v -> startActivity(new Intent(this, DriverMapActivity.class)));
        }

        if (cardDeliveryHistory != null) {
            cardDeliveryHistory.setOnClickListener(v -> startActivity(new Intent(this, DriverHistoryActivity.class)));
        }

        if (cardEmergencySupport != null) {
            cardEmergencySupport.setOnClickListener(v -> startActivity(new Intent(this, DriverSupportActivity.class)));
        }

        // Hero Card Clicks
        if (btnHeroViewDetails != null) {
            btnHeroViewDetails.setOnClickListener(v -> {
                if (currentHeroDelivery != null) {
                    Intent intent = new Intent(this, DriverDeliveryDetailsActivity.class);
                    intent.putExtra("DELIVERY_ID", currentHeroDelivery.getBookingId());
                    startActivity(intent);
                }
            });
        }

        if (btnHeroStartDelivery != null) {
            btnHeroStartDelivery.setOnClickListener(v -> {
                if (currentHeroDelivery != null) {
                    String status = currentHeroDelivery.getStatus();
                    if ("ASSIGNED".equalsIgnoreCase(status)) {
                        // Start Delivery: ASSIGNED -> STARTED
                        boolean success = dbHelper.updateDeliveryStatus(currentHeroDelivery.getBookingId(), "STARTED", locationHelper.getLatitude(), locationHelper.getLongitude());
                        if (success) {
                            dbHelper.logLocation(session.getUsername(), currentHeroDelivery.getBookingId(), locationHelper.getLatitude(), locationHelper.getLongitude(), locationHelper.getAccuracy(), 0f, 0f);
                            Toast.makeText(this, "Delivery #" + currentHeroDelivery.getBookingId() + " STARTED! GPS Tracking Active.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    Intent intent = new Intent(this, DriverDeliveryDetailsActivity.class);
                    intent.putExtra("DELIVERY_ID", currentHeroDelivery.getBookingId());
                    startActivity(intent);
                }
            });
        }

        if (btnHeroReportProblem != null) {
            btnHeroReportProblem.setOnClickListener(v -> {
                if (currentHeroDelivery != null) {
                    showReportProblemDialog(currentHeroDelivery.getBookingId());
                }
            });
        }

        updateDutyUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (driverDashboardMapView != null) {
            driverDashboardMapView.onResume();
            driverDashboardMapView.startLiveDeliveryAnimation();
        }
        updateDutyUI();
        loadAssignedHeroDelivery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (driverDashboardMapView != null) {
            driverDashboardMapView.onPause();
        }
    }

    private void loadAssignedHeroDelivery() {
        currentHeroDelivery = dbHelper.getNextAssignedDelivery(session.getUsername());
        List<Delivery> allAssigned = dbHelper.getAssignedDeliveries(session.getUsername());

        if (tvDelivBadge != null) {
            tvDelivBadge.setText(String.valueOf(allAssigned.size()));
        }

        if (currentHeroDelivery == null) {
            if (cardAssignedDeliveryHero != null) {
                tvHeroBookingId.setText("No Active Booking");
                tvHeroSequence.setText("Seq #0");
                tvHeroStatusPill.setText("IDLE");
                tvHeroCustomerName.setText("N/A");
                tvHeroPickup.setText("N/A");
                tvHeroDestination.setText("N/A");
                tvHeroSpecialInstructions.setText("No pending bookings assigned by Admin.");
                btnHeroStartDelivery.setEnabled(false);
                btnHeroViewDetails.setEnabled(false);
                btnHeroReportProblem.setEnabled(false);
            }
            return;
        }

        btnHeroStartDelivery.setEnabled(true);
        btnHeroViewDetails.setEnabled(true);
        btnHeroReportProblem.setEnabled(true);

        tvHeroBookingId.setText("Booking #" + currentHeroDelivery.getBookingId());
        tvHeroSequence.setText(String.format("Seq #%d", currentHeroDelivery.getSequenceNumber()));
        tvHeroStatusPill.setText(currentHeroDelivery.getStatus());
        tvHeroCustomerName.setText(currentHeroDelivery.getCustomerName());
        tvHeroPickup.setText(currentHeroDelivery.getPickupLocation());
        tvHeroDestination.setText(currentHeroDelivery.getDestination());

        String instructions = currentHeroDelivery.getSpecialInstructions();
        if (TextUtils.isEmpty(instructions)) {
            instructions = "None";
        }
        tvHeroSpecialInstructions.setText("⚠️ " + instructions);

        String status = currentHeroDelivery.getStatus();
        if ("ASSIGNED".equalsIgnoreCase(status)) {
            tvHeroStatusPill.setBackgroundResource(R.drawable.bg_status_pending);
            tvHeroStatusPill.setTextColor(0xFF2563EB);
            btnHeroStartDelivery.setText("START DELIVERY");
        } else {
            tvHeroStatusPill.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvHeroStatusPill.setTextColor(0xFF059669);
            btnHeroStartDelivery.setText("CONTINUE DELIVERY");
        }
    }

    private void toggleDarkMode() {
        String currentTheme = ThemeHelper.getSelectedTheme(this);
        String newTheme;
        if (ThemeHelper.THEME_DARK.equalsIgnoreCase(currentTheme)) {
            newTheme = ThemeHelper.THEME_LIGHT;
            Toast.makeText(this, "☀️ Light Mode Activated", Toast.LENGTH_SHORT).show();
        } else {
            newTheme = ThemeHelper.THEME_DARK;
            Toast.makeText(this, "🌙 Dark Mode Activated", Toast.LENGTH_SHORT).show();
        }
        ThemeHelper.setTheme(this, newTheme);
    }

    private void toggleDriverDuty() {
        boolean currentDuty = session.isDutyOnline();
        if (currentDuty) {
            if (dbHelper.hasActiveDelivery(session.getUsername())) {
                Toast.makeText(this, "Cannot end duty while you have an active delivery in progress!", Toast.LENGTH_LONG).show();
                return;
            }
            session.setDutyOnline(false);
            locationHelper.stopLocationUpdates();
            Toast.makeText(this, "Driver Duty ended. You are now OFFLINE.", Toast.LENGTH_SHORT).show();
        } else {
            session.setDutyOnline(true);
            locationHelper.startLocationUpdates((latitude, longitude, accuracy, speed, bearing) -> {
                dbHelper.logLocation(session.getUsername(), currentHeroDelivery != null ? currentHeroDelivery.getBookingId() : "", latitude, longitude, accuracy, speed, bearing);
                updateDutyUI();
            });
            Toast.makeText(this, "Driver Duty started! GPS tracking active.", Toast.LENGTH_SHORT).show();
            dbHelper.logLocation(session.getUsername(), currentHeroDelivery != null ? currentHeroDelivery.getBookingId() : "", locationHelper.getLatitude(), locationHelper.getLongitude(), locationHelper.getAccuracy(), 0f, 0f);
        }
        updateDutyUI();
    }

    private void updateDutyUI() {
        boolean isOnline = session.isDutyOnline();
        if (isOnline) {
            if (tvDutyStatusPill != null) {
                tvDutyStatusPill.setText("● ONLINE");
                tvDutyStatusPill.setTextColor(0xFF34D399); // Green
            }
            if (btnToggleDuty != null) {
                btnToggleDuty.setText("END DUTY");
                btnToggleDuty.setTextColor(0xFFEF4444); // Red
            }
            if (tvGpsTrackingLabel != null) {
                tvGpsTrackingLabel.setText("● LIVE");
                tvGpsTrackingLabel.setTextColor(0xFF059669);
                tvGpsTrackingLabel.setBackgroundResource(R.drawable.bg_status_confirmed);
            }
            if (tvLat != null) tvLat.setText(String.format("%.4f", locationHelper.getLatitude()));
            if (tvLng != null) tvLng.setText(String.format("%.4f", locationHelper.getLongitude()));
            if (tvAccuracy != null) tvAccuracy.setText(String.format("±%.1fm", locationHelper.getAccuracy()));
        } else {
            if (tvDutyStatusPill != null) {
                tvDutyStatusPill.setText("● OFFLINE");
                tvDutyStatusPill.setTextColor(0xFFF87171); // Red
            }
            if (btnToggleDuty != null) {
                btnToggleDuty.setText("START DUTY");
                btnToggleDuty.setTextColor(0xFF2563EB); // Blue
            }
            if (tvGpsTrackingLabel != null) {
                tvGpsTrackingLabel.setText("● GPS Off");
                tvGpsTrackingLabel.setTextColor(0xFFEF4444);
                tvGpsTrackingLabel.setBackgroundResource(R.drawable.bg_status_pending);
            }
        }
    }

    private void showReportProblemDialog(String bookingId) {
        String[] problemTypes = {
                "Vehicle problem",
                "Accident/emergency",
                "Cannot reach pickup location",
                "Incorrect pickup information",
                "Customer issue",
                "Package issue",
                "Other problem"
        };

        final int[] selectedIndex = {0};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Report Problem to Admin (#" + bookingId + ")");

        View customView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        final EditText etDescription = new EditText(this);
        etDescription.setHint("Enter problem details for Admin review...");
        etDescription.setPadding(32, 24, 32, 24);

        builder.setSingleChoiceItems(problemTypes, 0, (dialog, which) -> selectedIndex[0] = which);
        builder.setView(etDescription);

        builder.setPositiveButton("Submit Report", (dialog, which) -> {
            String selectedType = problemTypes[selectedIndex[0]];
            String desc = etDescription.getText().toString().trim();

            if (desc.isEmpty()) {
                desc = "Reported: " + selectedType;
            }

            boolean saved = dbHelper.saveProblemReport(bookingId, session.getUsername(), selectedType, desc, locationHelper.getLatitude(), locationHelper.getLongitude());
            if (saved) {
                Toast.makeText(this, "Problem report sent to Admin! Admin will review and process.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to submit problem report", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
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

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_bookings) {
                startActivity(new Intent(this, DriverDeliveriesActivity.class));
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, DriverNotificationsActivity.class));
                return true;
            } else if (itemId == R.id.nav_reports) {
                startActivity(new Intent(this, DriverHistoryActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, DriverProfileActivity.class));
                return true;
            }
            return false;
        });
    }
}
