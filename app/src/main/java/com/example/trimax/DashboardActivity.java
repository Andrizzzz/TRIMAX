package com.example.trimax;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private SessionManager session;
    private DatabaseHelper dbHelper;
    private LocationHelper locationHelper;

    private ActivityResultLauncher<Intent> attendanceCameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);
        locationHelper = new LocationHelper(this);

        // Role-based Navigation Access Protection Guard
        if (session.isLoggedIn() && "DRIVER".equalsIgnoreCase(session.getRole())) {
            Intent redirectIntent = new Intent(DashboardActivity.this, DriverDashboardActivity.class);
            startActivity(redirectIntent);
            finish();
            return;
        }

        drawerLayout = findViewById(R.id.drawerLayout);
        View mainContent = findViewById(R.id.mainContent);

        if (mainContent != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainContent, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Keep the entire dashboard (including the bottom navigation container)
                // inside the safe area above Android's system navigation bar.
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }

        ImageView btnMenu = findViewById(R.id.btnMenu);
        NavigationView navigationView = findViewById(R.id.navigationView);

        TextView tvWelcomeAgent = findViewById(R.id.tvWelcomeAgent);
        ImageView btnNotifications = findViewById(R.id.btnNotifications);
        TextView tvViewAll = findViewById(R.id.tvViewAll);
        MaterialButton btnQuickNewBooking = findViewById(R.id.btnQuickNewBooking);

        LinearLayout statTotalBookings = findViewById(R.id.statTotalBookings);
        LinearLayout statPendingBookings = findViewById(R.id.statPendingBookings);
        LinearLayout statConfirmedBookings = findViewById(R.id.statConfirmedBookings);

        MaterialButton btnAgentStartDuty = findViewById(R.id.btnAgentStartDuty);

        // Camera Attendance Launcher
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
                                    DashboardActivity.this,
                                    photoBitmap,
                                    "AGENT_DUTY_ATTENDANCE",
                                    locationHelper.getLatitude(),
                                    locationHelper.getLongitude()
                            );

                            String path = savedFile != null ? savedFile.getAbsolutePath() : "";
                            boolean saved = dbHelper.saveAttendance(session.getUsername(), path, locationHelper.getLatitude(), locationHelper.getLongitude());

                            if (saved) {
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US);
                                String startTimeStr = sdf.format(new Date());

                                session.setDutyOnline(true);
                                session.setDutyStartTime(startTimeStr);

                                updateAttendanceUI();
                                Toast.makeText(DashboardActivity.this, "✓ Attendance Verified! Duty Started: " + startTimeStr, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(DashboardActivity.this, "Failed to save attendance record in database.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(DashboardActivity.this, "Failed to retrieve attendance photo.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(DashboardActivity.this, "Attendance photo capture cancelled.", Toast.LENGTH_SHORT).show();
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
                                .setMessage("Camera access is needed to take your attendance verification photo. Enable Camera permission in App Settings to start duty.")
                                .setPositiveButton("Open Settings", (dialog, which) -> {
                                    Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:" + getPackageName()));
                                    startActivity(settingsIntent);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }
        );

        if (btnAgentStartDuty != null) {
            btnAgentStartDuty.setOnClickListener(v -> toggleAgentDutyState());
        }

        // Dashboard tracking opens a client overview map; avoid initializing a hidden map here.
        View cardTrackingMap = findViewById(R.id.cardLiveTrackingMap);
        View trackingLink = findViewById(R.id.tvTrackLink);
        View trackingFullBtn = findViewById(R.id.btnTrackFullMap);
        View.OnClickListener openTracking = v ->
                startActivity(new Intent(DashboardActivity.this, TrackingMapActivity.class));
        if (cardTrackingMap != null) cardTrackingMap.setOnClickListener(openTracking);
        if (trackingLink != null) trackingLink.setOnClickListener(openTracking);
        if (trackingFullBtn != null) trackingFullBtn.setOnClickListener(openTracking);

        SwipeRefreshLayout swipeRefresh = findViewById(R.id.swipeRefreshDashboard);
        if (swipeRefresh != null) {
            swipeRefresh.setColorSchemeResources(R.color.primary_blue);
            swipeRefresh.setOnRefreshListener(() -> {
                refreshDashboardContent();
                swipeRefresh.setRefreshing(false);
            });
        }


        String agentName = session.getFullName();
        String agentEmail = session.getEmail();

        if (tvWelcomeAgent != null) {
            tvWelcomeAgent.setText(String.format("Hi, Agent %s! 👋", agentName));
        }

        // 1. HAMBURGER MENU / NAVIGATION DRAWER
        if (btnMenu != null && drawerLayout != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvNavName = headerView.findViewById(R.id.tvNavHeaderName);
                TextView tvNavEmail = headerView.findViewById(R.id.tvNavHeaderEmail);
                if (tvNavName != null) tvNavName.setText(String.format("Agent %s", agentName));
                if (tvNavEmail != null) tvNavEmail.setText(agentEmail);

                headerView.setOnClickListener(v -> {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
                });
            }

            navigationView.setNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                drawerLayout.closeDrawer(GravityCompat.START);

                if (itemId == R.id.drawer_home) {
                    refreshDashboardContent();
                    return true;
                } else if (itemId == R.id.drawer_bookings) {
                    startActivity(new Intent(DashboardActivity.this, BookingsActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_clients) {
                    startActivity(new Intent(DashboardActivity.this, ClientsActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_notifications) {
                    startActivity(new Intent(DashboardActivity.this, NotificationsActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_reports) {
                    startActivity(new Intent(DashboardActivity.this, ReportsActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_profile) {
                    startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_settings) {
                    startActivity(new Intent(DashboardActivity.this, SettingsActivity.class));
                    return true;
                } else if (itemId == R.id.drawer_logout) {
                    showLogoutDialog();
                    return true;
                }
                return false;
            });
        }

        // Dark Mode Dashboard Toggle
        ImageView btnThemeToggle = findViewById(R.id.btnThemeToggle);
        if (btnThemeToggle != null) {
            updateThemeToggleIcon(btnThemeToggle);
            btnThemeToggle.setOnClickListener(v -> toggleDarkMode());
        }

        // 2. NOTIFICATION BELL
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, NotificationsActivity.class)));
        }

        // Dashboard quick actions
        View cardPendingBookings = findViewById(R.id.cardPendingBookings);
        if (statTotalBookings != null) {
            statTotalBookings.setOnClickListener(v -> openBookingsWithFilter("ALL"));
        }
        if (cardPendingBookings != null) {
            cardPendingBookings.setOnClickListener(openTracking);
        }
        if (statPendingBookings != null) {
            statPendingBookings.setOnClickListener(openTracking);
        }
        if (statConfirmedBookings != null) {
            statConfirmedBookings.setOnClickListener(v -> openBookingsWithFilter("COMPLETED"));
        }
        View supportCard = findViewById(R.id.cardAgentAttendance);
        if (supportCard != null) {
            supportCard.setOnClickListener(v -> new AlertDialog.Builder(DashboardActivity.this)
                    .setTitle("TRIMAX Support")
                    .setMessage("Need help with a booking or delivery? Open Notifications for the latest updates, or contact your TRIMAX administrator for assistance.")
                    .setPositiveButton("Open Notifications", (dialog, which) ->
                            startActivity(new Intent(DashboardActivity.this, NotificationsActivity.class)))
                    .setNegativeButton("Close", null)
                    .show());
        }

        // 4. DASHBOARD STATS CARDS

        // 5. RECENT BOOKING CARDS
        loadRecentBookings();

        // 6. "VIEW ALL" & 7. "NEW BOOKING" BUTTONS
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, BookingsActivity.class)));
        }

        if (btnQuickNewBooking != null) {
            btnQuickNewBooking.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, NewBookingCustomerActivity.class)));
        }

        // 8. BOTTOM NAVIGATION
        setupBottomNavigation();

        // Back Press Handler for Navigation Drawer
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    remove();
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        updateAttendanceUI();
    }

    private void updateThemeToggleIcon(ImageView toggle) {
        boolean darkMode = ThemeHelper.THEME_DARK.equalsIgnoreCase(ThemeHelper.getSelectedTheme(this));
        toggle.setImageResource(darkMode ? R.drawable.ic_moon : R.drawable.ic_sun);
        toggle.setContentDescription(darkMode ? "Dark mode is on. Tap to switch to light mode." : "Light mode is on. Tap to switch to dark mode.");
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

    private void toggleAgentDutyState() {
        if (session.isDutyOnline()) {
            new AlertDialog.Builder(this)
                    .setTitle("End Duty Shift")
                    .setMessage("Are you sure you want to end your active duty shift?")
                    .setPositiveButton("End Duty Shift", (dialog, which) -> {
                        dbHelper.endDuty(session.getUsername());
                        session.setDutyOnline(false);
                        session.setDutyStartTime("");
                        updateAttendanceUI();
                        Toast.makeText(this, "Agent Duty shift ended.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchAttendanceCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchAttendanceCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            attendanceCameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Camera app unavailable on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAttendanceUI() {
        TextView tvDutyBadge = findViewById(R.id.tvDutyBadge);
        TextView tvDutyTimeInfo = findViewById(R.id.tvDutyTimeInfo);
        MaterialButton btnAgentStartDuty = findViewById(R.id.btnAgentStartDuty);

        if (session.isDutyOnline()) {
            if (tvDutyBadge != null) {
                tvDutyBadge.setText("● ON DUTY");
                tvDutyBadge.setTextColor(0xFF059669); // Green
                tvDutyBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            }
            if (tvDutyTimeInfo != null) {
                String startTime = session.getDutyStartTime();
                if (startTime.isEmpty()) startTime = "Just now";
                tvDutyTimeInfo.setText("On Duty shift active since " + startTime);
            }
            if (btnAgentStartDuty != null) {
                btnAgentStartDuty.setText("END DUTY 🛑");
                btnAgentStartDuty.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFEF4444)); // Red
            }
        } else {
            if (tvDutyBadge != null) {
                tvDutyBadge.setText("● OFF DUTY");
                tvDutyBadge.setTextColor(0xFFEF4444); // Red
                tvDutyBadge.setBackgroundResource(R.drawable.bg_status_pending);
            }
            if (tvDutyTimeInfo != null) {
                tvDutyTimeInfo.setText("Camera photo verification required to Start Duty");
            }
            if (btnAgentStartDuty != null) {
                btnAgentStartDuty.setText("START DUTY 📷");
                btnAgentStartDuty.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2563EB)); // Blue
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationView dashboardDrawer = findViewById(R.id.navigationView);
        if (dashboardDrawer != null) {
            dashboardDrawer.setCheckedItem(R.id.drawer_home);
        }
        refreshDashboardContent();
    }

    private void updateHeaderUI() {
        if (session == null) return;
        String agentName = session.getFullName();
        String agentEmail = session.getEmail();

        TextView tvWelcomeAgent = findViewById(R.id.tvWelcomeAgent);
        if (tvWelcomeAgent != null) {
            tvWelcomeAgent.setText(String.format("Hi, Agent %s! 👋", agentName));
        }

        NavigationView navigationView = findViewById(R.id.navigationView);
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvNavName = headerView.findViewById(R.id.tvNavHeaderName);
                TextView tvNavEmail = headerView.findViewById(R.id.tvNavHeaderEmail);
                if (tvNavName != null) tvNavName.setText(String.format("Agent %s", agentName));
                if (tvNavEmail != null) tvNavEmail.setText(agentEmail);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void loadRecentBookings() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        List<Delivery> bookings = dbHelper.getAllBookings();

        CardView card1 = findViewById(R.id.cardDashboardBooking1);
        CardView card2 = findViewById(R.id.cardDashboardBooking2);

        if (bookings != null && !bookings.isEmpty()) {
            if (card1 != null) {
                if (bookings.size() > 0) {
                    card1.setVisibility(View.VISIBLE);
                    bindBookingCard(card1, bookings.get(0), 1);
                } else {
                    card1.setVisibility(View.GONE);
                }
            }
            if (card2 != null) {
                if (bookings.size() > 1) {
                    card2.setVisibility(View.VISIBLE);
                    bindBookingCard(card2, bookings.get(1), 2);
                } else {
                    card2.setVisibility(View.GONE);
                }
            }
        }
    }

    private void bindBookingCard(CardView card, Delivery delivery, int index) {
        TextView tvId = card.findViewById(index == 1 ? R.id.tvCard1Id : R.id.tvCard2Id);
        TextView tvStatus = card.findViewById(index == 1 ? R.id.tvCard1Status : R.id.tvCard2Status);
        TextView tvCustomerDate = card.findViewById(index == 1 ? R.id.tvCard1CustomerDate : R.id.tvCard2CustomerDate);
        TextView tvAmount = card.findViewById(index == 1 ? R.id.tvCard1Amount : R.id.tvCard2Amount);

        if (tvId != null) tvId.setText(delivery.getBookingId());
        if (tvStatus != null) {
            tvStatus.setText(delivery.getStatus());
            if ("CONFIRMED".equalsIgnoreCase(delivery.getStatus())) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                tvStatus.setTextColor(android.graphics.Color.parseColor("#059669"));
            } else if ("PENDING".equalsIgnoreCase(delivery.getStatus())) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatus.setTextColor(android.graphics.Color.parseColor("#D97706"));
            } else if ("CANCELLED".equalsIgnoreCase(delivery.getStatus())) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_cancelled);
                tvStatus.setTextColor(android.graphics.Color.parseColor("#DC2626"));
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                tvStatus.setTextColor(android.graphics.Color.parseColor("#2563EB"));
            }
        }
        if (tvCustomerDate != null) {
            tvCustomerDate.setText(String.format("%s • %s", delivery.getCustomerName(), delivery.getScheduledTime()));
        }
        if (tvAmount != null) {
            tvAmount.setText(String.format("₱ %,.2f", delivery.getCurrentTotal()));
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, BookingDetailActivity.class);
            intent.putExtra("BOOKING_ID", delivery.getBookingId());
            intent.putExtra("CUSTOMER_NAME", delivery.getCustomerName());
            intent.putExtra("STATUS", delivery.getStatus());
            intent.putExtra("TOTAL_AMOUNT", String.format("₱ %,.2f", delivery.getCurrentTotal()));
            startActivity(intent);
        });
    }

    private void refreshDashboardContent() {
        updateHeaderUI();
        updateAttendanceUI();
        updateDashboardStats();
        loadRecentBookings();
    }

    private void updateDashboardStats() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        TextView tvStatTotalCount = findViewById(R.id.tvStatTotalCount);
        TextView tvStatPendingCount = findViewById(R.id.tvStatPendingCount);
        TextView tvStatConfirmedCount = findViewById(R.id.tvStatConfirmedCount);

        if (tvStatTotalCount != null) {
            tvStatTotalCount.setText(String.valueOf(dbHelper.getBookingCountByStatus("ALL")));
        }
        if (tvStatPendingCount != null) {
            tvStatPendingCount.setText(String.valueOf(dbHelper.getBookingCountByStatus("PENDING")));
        }
        if (tvStatConfirmedCount != null) {
            tvStatConfirmedCount.setText(String.valueOf(dbHelper.getBookingCountByStatus("CONFIRMED")));
        }
    }

    private void openBookingsWithFilter(String filter) {
        Intent intent = new Intent(DashboardActivity.this, BookingsActivity.class);
        intent.putExtra("FILTER", filter);
        startActivity(intent);
    }

    private void openBookingDetail(String id, String customer, String status, String amount) {
        Intent intent = new Intent(DashboardActivity.this, BookingDetailActivity.class);
        intent.putExtra("BOOKING_ID", id);
        intent.putExtra("CUSTOMER_NAME", customer);
        intent.putExtra("STATUS", status);
        intent.putExtra("TOTAL_AMOUNT", amount);
        startActivity(intent);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    session.logoutUser();

                    Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupBottomNavigation() {
        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View clients = findViewById(R.id.nav_clients);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) {
            home.setSelected(true);
            home.setOnClickListener(v -> {
                // Already on Home.
            });
        }
        if (bookings != null) {
            bookings.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, BookingsActivity.class));
                finish();
            });
        }
        if (packages != null) {
            packages.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, PackagesActivity.class));
                finish();
            });
        }
        if (clients != null) {
            clients.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, ClientsActivity.class));
                finish();
            });
        }
        if (profile != null) {
            profile.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
                finish();
            });
        }
    }
}
