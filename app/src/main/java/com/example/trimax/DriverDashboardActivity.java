package com.example.trimax;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private ActivityResultLauncher<Intent> attendanceCameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private boolean isEndingDuty = false;

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

        attendanceCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Bitmap photoBitmap = null;
                        if (result.getData() != null && result.getData().getExtras() != null) {
                            photoBitmap = (Bitmap) result.getData().getExtras().get("data");
                        }

                        if (photoBitmap != null) {
                            File savedFile = ImageTimestampHelper.saveBitmapWithTimestamp(
                                    DriverDashboardActivity.this,
                                    photoBitmap,
                                    "DRIVER_DUTY_ATTENDANCE",
                                    locationHelper.getLatitude(),
                                    locationHelper.getLongitude()
                            );

                            String path = savedFile != null ? savedFile.getAbsolutePath() : "";
                            boolean saved;
                            if (isEndingDuty) {
                                saved = dbHelper.endDutyWithPhoto(session.getUsername(), path, locationHelper.getLatitude(), locationHelper.getLongitude());
                            } else {
                                saved = dbHelper.saveAttendance(session.getUsername(), path, locationHelper.getLatitude(), locationHelper.getLongitude());
                            }

                            if (saved) {
                                if (isEndingDuty) {
                                    session.setDutyOnline(false);
                                    locationHelper.stopLocationUpdates();
                                    updateDutyUI();
                                    Toast.makeText(DriverDashboardActivity.this, "✓ Attendance Verified! Duty Ended.", Toast.LENGTH_LONG).show();
                                } else {
                                    session.setDutyOnline(true);
                                    locationHelper.startLocationUpdates((latitude, longitude, accuracy, speed, bearing) -> {
                                        dbHelper.logLocation(session.getUsername(), currentHeroDelivery != null ? currentHeroDelivery.getBookingId() : "", latitude, longitude, accuracy, speed, bearing);
                                        updateDutyUI();
                                    });
                                    updateDutyUI();
                                    Toast.makeText(DriverDashboardActivity.this, "✓ Attendance Verified! Duty Started.", Toast.LENGTH_LONG).show();
                                    dbHelper.logLocation(session.getUsername(), currentHeroDelivery != null ? currentHeroDelivery.getBookingId() : "", locationHelper.getLatitude(), locationHelper.getLongitude(), locationHelper.getAccuracy(), 0f, 0f);
                                }
                            } else {
                                Toast.makeText(DriverDashboardActivity.this, "Failed to save attendance record in database.", Toast.LENGTH_SHORT).show();
                            }
                            isEndingDuty = false;
                        } else {
                            Toast.makeText(DriverDashboardActivity.this, "Failed to retrieve attendance photo.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(DriverDashboardActivity.this, "Attendance photo capture cancelled.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        launchAttendanceCamera();
                    } else {
                        new AlertDialog.Builder(this)
                                .setTitle("Camera Permission Required")
                                .setMessage("Camera access is needed to take your attendance verification photo. Enable Camera permission in App Settings.")
                                .setPositiveButton("Open Settings", (dialog, which) -> {
                                    try {
                                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                                        startActivity(intent);
                                    } catch (Exception ignored) {}
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View topBar = findViewById(R.id.topBar);
            if (topBar != null) {
                int baseTopPadding = Math.round(16 * getResources().getDisplayMetrics().density);
                topBar.setPadding(topBar.getPaddingLeft(), systemBars.top + baseTopPadding, topBar.getPaddingRight(), topBar.getPaddingBottom());
            }
            return insets;
        });

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
                if (currentHeroDelivery == null) {
                    Toast.makeText(this, "No active delivery assigned yet.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, DriverMapActivity.class);
                intent.putExtra("DELIVERY_ID", currentHeroDelivery.getBookingId());
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
            tvDriverGreeting.setText(String.format("%s, %s 👋", getTimeBasedGreeting(), driverName));
        }

        // Navigation Drawer
        if (btnMenu != null && drawerLayout != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        SwipeRefreshLayout swipeRefresh = findViewById(R.id.swipeRefreshDashboard);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.primary_blue);
            swipeRefresh.setOnRefreshListener(() -> {
                refreshDriverDashboard();
                swipeRefresh.setRefreshing(false);
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

        LinearLayout bottomNavigation = findViewById(R.id.bottomNavigation);
        setupBottomNavigation(bottomNavigation);

        if (navigationView != null) {
            // The shared agent drawer includes Clients, which has no driver destination.
            // Hide it for drivers and label the history destination accurately.
            if (navigationView.getMenu().findItem(R.id.drawer_clients) != null) {
                navigationView.getMenu().findItem(R.id.drawer_clients).setVisible(false);
            }
            // Notifications already has a dedicated bottom-navigation destination for drivers.
            if (navigationView.getMenu().findItem(R.id.drawer_notifications) != null) {
                navigationView.getMenu().findItem(R.id.drawer_notifications).setVisible(false);
            }
            if (navigationView.getMenu().findItem(R.id.drawer_reports) != null) {
                navigationView.getMenu().findItem(R.id.drawer_reports).setTitle("Delivery History");
            }

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
                    refreshDriverDashboard();
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
            updateThemeToggleIcon(btnThemeToggle);
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
            cardDriverMap.setOnClickListener(v -> {
                Delivery next = currentHeroDelivery;
                if (next == null) {
                    Toast.makeText(this, "No active delivery assigned yet.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, DriverMapActivity.class);
                intent.putExtra("DELIVERY_ID", next.getBookingId());
                intent.putExtra("DEST_ADDRESS", next.getDestination());
                intent.putExtra("DEST_LAT", next.getDestinationLatitude());
                intent.putExtra("DEST_LNG", next.getDestinationLongitude());
                intent.putExtra("CUSTOMER_NAME", next.getCustomerName());
                startActivity(intent);
            });
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
                if (currentHeroDelivery == null || currentHeroRecord == null) return;
                final String id = currentHeroDelivery.getBookingId();
                DeliveryProgress.NextStep step = DeliveryProgress.nextStep(currentHeroRecord.status, currentHeroRecord.deliveryStatus);
                if (!DeliveryProgress.STARTED.equals(step.target)) {
                    // Already under way (or waiting for the admin): continue on the details screen.
                    Intent intent = new Intent(this, DriverDeliveryDetailsActivity.class);
                    intent.putExtra("DELIVERY_ID", id);
                    startActivity(intent);
                    return;
                }
                btnHeroStartDelivery.setEnabled(false);
                Double lat = locationHelper.hasValidLocationFix() ? locationHelper.getLatitude() : null;
                Double lng = locationHelper.hasValidLocationFix() ? locationHelper.getLongitude() : null;
                TrimaxApi.get(this).updateDeliveryStatus(currentHeroRecord.bookingId, DeliveryProgress.ASSIGNED,
                        DeliveryProgress.STARTED, lat, lng, null, null, new TrimaxApi.Callback<BookingRecord>() {
                            @Override
                            public void onSuccess(BookingRecord saved) {
                                if (SessionGuard.isGone(DriverDashboardActivity.this)) return;
                                dbHelper.logLocation(session.getUsername(), id, locationHelper.getLatitude(), locationHelper.getLongitude(), locationHelper.getAccuracy(), 0f, 0f);
                                Toast.makeText(DriverDashboardActivity.this, "Delivery #" + id + " STARTED! GPS Tracking Active.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(DriverDashboardActivity.this, DriverDeliveryDetailsActivity.class);
                                intent.putExtra("DELIVERY_ID", id);
                                startActivity(intent);
                            }

                            @Override
                            public void onError(ApiException error) {
                                if (SessionGuard.isGone(DriverDashboardActivity.this)) return;
                                if (SessionGuard.handle(DriverDashboardActivity.this, error)) return;
                                btnHeroStartDelivery.setEnabled(true);
                                Toast.makeText(DriverDashboardActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                                loadAssignedHeroDelivery();
                            }
                        });
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
        refreshDriverDashboard();

        NavigationView dashboardDrawer = findViewById(R.id.navigationView);
        if (dashboardDrawer != null) {
            dashboardDrawer.setCheckedItem(R.id.drawer_home);
        }

        // Whenever a driver screen closes and returns here, restore the Dashboard tab highlight.
        LinearLayout bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            highlightBottomNavItem(R.id.nav_home);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (driverDashboardMapView != null) {
            driverDashboardMapView.onPause();
        }
    }

    private String getTimeBasedGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Good morning";
        if (hour < 17) return "Good afternoon";
        return "Good evening";
    }

    private void refreshDriverDashboard() {
        updateDutyUI();
        loadAssignedHeroDelivery();
        TextView tvDriverGreeting = findViewById(R.id.tvDriverGreeting);
        if (tvDriverGreeting != null && session != null) {
            tvDriverGreeting.setText(String.format("%s, %s 👋", getTimeBasedGreeting(), session.getFullName()));
        }
    }

    /** The server's view of this driver's unfinished deliveries (null until the first answer). */
    private List<BookingRecord> activeRecords;
    private BookingRecord currentHeroRecord;

    private void loadAssignedHeroDelivery() {
        TrimaxApi.get(this).listDriverDeliveries("active", new TrimaxApi.Callback<List<BookingRecord>>() {
            @Override
            public void onSuccess(List<BookingRecord> records) {
                if (SessionGuard.isGone(DriverDashboardActivity.this)) return;
                activeRecords = records;
                currentHeroRecord = records.isEmpty() ? null : records.get(0);
                currentHeroDelivery = currentHeroRecord == null ? null : currentHeroRecord.toDelivery(true);
                bindHeroDelivery(records.size());
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(DriverDashboardActivity.this)) return;
                if (SessionGuard.handle(DriverDashboardActivity.this, error)) return;
                Toast.makeText(DriverDashboardActivity.this, "Could not load deliveries: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindHeroDelivery(int activeCount) {
        if (tvDelivBadge != null) {
            tvDelivBadge.setText(String.valueOf(activeCount));
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
        if (currentHeroRecord != null && DeliveryProgress.PENDING.equals(currentHeroRecord.status)) {
            tvHeroStatusPill.setBackgroundResource(R.drawable.bg_status_pending);
            tvHeroStatusPill.setTextColor(0xFFD97706);
            tvHeroStatusPill.setText("AWAITING ADMIN");
            btnHeroStartDelivery.setText("VIEW DETAILS");
        } else if ("ASSIGNED".equalsIgnoreCase(status)) {
            tvHeroStatusPill.setBackgroundResource(R.drawable.bg_status_pending);
            tvHeroStatusPill.setTextColor(0xFF2563EB);
            btnHeroStartDelivery.setText("START DELIVERY");
        } else {
            tvHeroStatusPill.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvHeroStatusPill.setTextColor(0xFF059669);
            btnHeroStartDelivery.setText("CONTINUE DELIVERY");
        }
    }

    private void updateThemeToggleIcon(ImageView toggle) {
        boolean darkMode = ThemeHelper.THEME_DARK.equalsIgnoreCase(ThemeHelper.getSelectedTheme(this));
        toggle.setImageResource(darkMode ? R.drawable.ic_moon : R.drawable.ic_sun);
        toggle.setContentDescription(darkMode
                ? "Dark mode is on. Tap to switch to light mode."
                : "Light mode is on. Tap to switch to dark mode.");
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
            boolean underWay = false;
            if (activeRecords != null) {
                for (BookingRecord r : activeRecords) underWay |= DeliveryProgress.isUnderWay(r.deliveryStatus);
            }
            if (underWay) {
                Toast.makeText(this, "Cannot end duty while you have an active delivery in progress!", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle("End Duty Shift")
                    .setMessage("Photo verification is required to end your active duty shift.")
                    .setPositiveButton("Take Photo & End Duty", (dialog, which) -> {
                        isEndingDuty = true;
                        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            launchAttendanceCamera();
                        } else {
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            isEndingDuty = false;
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launchAttendanceCamera();
            } else {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
            }
        }
    }

    private void launchAttendanceCamera() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            attendanceCameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Camera app unavailable on this device.", Toast.LENGTH_SHORT).show();
        }
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

    private void navigateToTab(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        intent.putExtra("PREV_TAB_INDEX", 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private void setupBottomNavigation(LinearLayout bottomNavigation) {
        highlightBottomNavItem(R.id.nav_home);

        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) home.setOnClickListener(v -> highlightBottomNavItem(R.id.nav_home));
        if (bookings != null) bookings.setOnClickListener(v -> navigateToTab(DriverDeliveriesActivity.class));
        if (packages != null) packages.setOnClickListener(v -> navigateToTab(DriverHistoryActivity.class));
        if (profile != null) profile.setOnClickListener(v -> navigateToTab(DriverProfileActivity.class));
    }

    private void highlightBottomNavItem(int activeId) {
        int[] ids = {R.id.nav_home, R.id.nav_bookings, R.id.nav_packages, R.id.nav_profile};
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
        if (indicator != null) {
            final int targetIndex = activeIndex;
            final int initialIndex = (prevIndex >= 0 && prevIndex != targetIndex) ? prevIndex : targetIndex;

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int totalWidth = screenWidth - (int) (52 * getResources().getDisplayMetrics().density);
            final int[] effectiveTabWidth = {totalWidth / 4};
            int marginHorizontal = (int) (4 * getResources().getDisplayMetrics().density);

            ViewGroup.LayoutParams lp = indicator.getLayoutParams();
            if (lp != null) {
                lp.width = effectiveTabWidth[0] - (marginHorizontal * 2);
                indicator.setLayoutParams(lp);
            }
            float startX = (navContainer != null ? navContainer.getPaddingLeft() : (int)(20 * getResources().getDisplayMetrics().density)) + (initialIndex * effectiveTabWidth[0]) + marginHorizontal;
            indicator.setTranslationX(startX);

            if (navContainer != null && prevIndex >= 0 && prevIndex != targetIndex) {
                navContainer.post(() -> {
                    int measuredWidth = navContainer.getWidth() - navContainer.getPaddingLeft() - navContainer.getPaddingRight();
                    if (measuredWidth > 0) {
                        effectiveTabWidth[0] = measuredWidth / 4;
                        if (lp != null) {
                            lp.width = effectiveTabWidth[0] - (marginHorizontal * 2);
                            indicator.setLayoutParams(lp);
                        }
                    }

                    float targetX = navContainer.getPaddingLeft() + (targetIndex * effectiveTabWidth[0]) + marginHorizontal;

                    indicator.animate()
                            .translationX(targetX)
                            .setDuration(140)
                            .setInterpolator(new FastOutSlowInInterpolator())
                            .start();
                });
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
