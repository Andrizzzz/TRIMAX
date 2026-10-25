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
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private boolean askedForLocation;

    private ActivityResultLauncher<Intent> attendanceCameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private boolean isEndingDuty = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);
        locationHelper = new LocationHelper(this);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                granted -> {
                    if (locationHelper.hasLocationPermission()) {
                        startDutyTracking();
                    } else {
                        Toast.makeText(this, "Location is off: the admin map will not show you while on duty.",
                                Toast.LENGTH_LONG).show();
                    }
                });

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
                // Apply the top inset to content only; keep the floating navigation independently inset.
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

                            onAttendancePhoto(savedFile, photoBitmap);
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
            if (navigationView.getMenu().findItem(R.id.drawer_notifications) != null) {
                navigationView.getMenu().findItem(R.id.drawer_notifications).setVisible(false);
            }
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
            supportCard.setOnClickListener(v -> {
                View supportView = getLayoutInflater().inflate(R.layout.dialog_help_support, null);
                AlertDialog supportDialog = new AlertDialog.Builder(DashboardActivity.this)
                        .setView(supportView)
                        .create();
                supportDialog.show();

                View closeSupport = supportView.findViewById(R.id.btnCloseSupport);
                View callSupport = supportView.findViewById(R.id.cardSupportCall);
                View emailSupport = supportView.findViewById(R.id.cardSupportEmail);

                if (closeSupport != null) {
                    closeSupport.setOnClickListener(close -> supportDialog.dismiss());
                }
                if (callSupport != null) {
                    callSupport.setOnClickListener(call -> {
                        supportDialog.dismiss();
                        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + getString(R.string.office_phone))));
                    });
                }
                if (emailSupport != null) {
                    emailSupport.setOnClickListener(email -> {
                        supportDialog.dismiss();
                        Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@trimax.ph"));
                        mail.putExtra(Intent.EXTRA_SUBJECT, "TRIMAX Help & Support");
                        startActivity(Intent.createChooser(mail, "Contact TRIMAX Support"));
                    });
                }
            });
        }

        // 4. DASHBOARD STATS CARDS

        // 5. RECENT BOOKING CARDS: loaded from the server in onResume (refreshDashboardContent).

        // 6. "VIEW ALL" & 7. "NEW BOOKING" BUTTONS
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, BookingsActivity.class)));
        }

        if (btnQuickNewBooking != null) {
            btnQuickNewBooking.setOnClickListener(v -> {
                boolean isClockedIn = session.isDutyOnline();
                if (!isClockedIn) {
                    new AlertDialog.Builder(DashboardActivity.this)
                            .setTitle("Attendance Required")
                            .setMessage("You must be clocked in (Start Duty / Agent Attendance) before adding a new booking.")
                            .setPositiveButton("OK", null)
                            .show();
                    return;
                }
                startActivity(new Intent(DashboardActivity.this, NewBookingCustomerActivity.class));
            });
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
                    .setMessage("Photo verification is required to end your active duty shift.")
                    .setPositiveButton("Take Photo & End Duty", (dialog, which) -> {
                        isEndingDuty = true;
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchAttendanceCamera();
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            isEndingDuty = false;
            launchAttendanceCamera();
        } else {
            isEndingDuty = false;
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
    protected void onResume() {
        super.onResume();
        NavigationView dashboardDrawer = findViewById(R.id.navigationView);
        if (dashboardDrawer != null) {
            dashboardDrawer.setCheckedItem(R.id.drawer_home);
        }
        refreshDashboardContent();
        if (session.isDutyOnline() && !locationHelper.isTrackingActive()) startDutyTracking();
        DutySync.loadServerState(this, onDuty -> {
            if (onDuty != session.isDutyOnline()) {
                session.setDutyOnline(onDuty);
                if (!onDuty) session.setDutyStartTime("");
                if (onDuty) startDutyTracking(); else locationHelper.stopLocationUpdates();
                updateAttendanceUI();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // The GPS listener holds this screen; a new dashboard restarts it in onResume.
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        super.onDestroy();
    }

    /** The server records the duty change first (photo + event) so the admin sees who is on duty. */
    private void onAttendancePhoto(File savedFile, Bitmap photoBitmap) {
        final boolean ending = isEndingDuty;
        isEndingDuty = false;
        final String path = savedFile != null ? savedFile.getAbsolutePath() : "";
        final double lat = locationHelper.getLatitude();
        final double lng = locationHelper.getLongitude();
        MaterialButton dutyButton = findViewById(R.id.btnAgentStartDuty);
        if (dutyButton != null) {
            dutyButton.setEnabled(false);
            dutyButton.setText("SAVING…");
        }
        DutySync.record(this, !ending, savedFile, photoBitmap, locationHelper, new DutySync.Listener() {
            @Override
            public void onDutyChanged(boolean onDuty) {
                if (onDuty) {
                    dbHelper.saveAttendance(session.getUsername(), path, lat, lng);
                    String startTimeStr = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(new Date());
                    session.setDutyOnline(true);
                    session.setDutyStartTime(startTimeStr);
                    startDutyTracking();
                    Toast.makeText(DashboardActivity.this, "✓ Attendance verified. Duty started: " + startTimeStr, Toast.LENGTH_LONG).show();
                } else {
                    dbHelper.endDutyWithPhoto(session.getUsername(), path, lat, lng);
                    session.setDutyOnline(false);
                    session.setDutyStartTime("");
                    locationHelper.stopLocationUpdates();
                    Toast.makeText(DashboardActivity.this, "✓ Attendance verified. Duty ended.", Toast.LENGTH_LONG).show();
                }
                updateAttendanceUI();
            }

            @Override
            public void onFinished() {
                if (dutyButton != null) dutyButton.setEnabled(true);
                updateAttendanceUI();
            }
        });
    }

    /** While on duty the agent's position is shared with the admin's agent map. */
    private void startDutyTracking() {
        if (!session.isDutyOnline()) return;
        if (!locationHelper.hasLocationPermission()) {
            if (!askedForLocation && locationPermissionLauncher != null) {
                askedForLocation = true;
                locationPermissionLauncher.launch(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION});
            }
            return;
        }
        locationHelper.startLocationUpdates((latitude, longitude, accuracy, speed, bearing) ->
                locationHelper.pushLocationToSupabaseIfOnline(DashboardActivity.this, session.isDutyOnline()));
    }

    /** Tiles and the two most recent cards, from the agent's own bookings on the TRIMAX server. */
    private void loadDashboardBookings() {
        TrimaxApi.get(this).listMyBookings(new TrimaxApi.Callback<List<BookingRecord>>() {
            @Override
            public void onSuccess(List<BookingRecord> records) {
                if (SessionGuard.isGone(DashboardActivity.this)) return;
                BookingNotifications.syncAgent(DashboardActivity.this, session.getUsername(), records);
                updateDashboardStats(records);
                List<Delivery> bookings = new java.util.ArrayList<>();
                for (BookingRecord r : records) bookings.add(r.toDelivery(false));
                loadRecentBookings(bookings);
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(DashboardActivity.this)) return;
                if (SessionGuard.handle(DashboardActivity.this, error)) return;
                android.widget.Toast.makeText(DashboardActivity.this,
                        "Could not load your bookings: " + error.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadRecentBookings(List<Delivery> bookings) {
        CardView card1 = findViewById(R.id.cardDashboardBooking1);
        CardView card2 = findViewById(R.id.cardDashboardBooking2);

        if (bookings == null || bookings.isEmpty()) {
            if (card1 != null) card1.setVisibility(View.GONE);
            if (card2 != null) card2.setVisibility(View.GONE);
        } else {
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

        if (tvId != null) tvId.setText("#" + delivery.getBookingId());
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
        loadDashboardBookings();
    }

    private void updateDashboardStats(List<BookingRecord> records) {
        TextView tvStatTotalCount = findViewById(R.id.tvStatTotalCount);
        TextView tvStatPendingCount = findViewById(R.id.tvStatPendingCount);
        TextView tvStatConfirmedCount = findViewById(R.id.tvStatConfirmedCount);

        if (tvStatTotalCount != null) {
            UiMotion.countTo(tvStatTotalCount, BookingFilter.count(records, "ALL"));
        }
        if (tvStatPendingCount != null) {
            UiMotion.countTo(tvStatPendingCount, BookingFilter.count(records, "PENDING"));
        }
        if (tvStatConfirmedCount != null) {
            UiMotion.countTo(tvStatConfirmedCount, BookingFilter.count(records, "CONFIRMED"));
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
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_logout, null);
        bottomSheetDialog.setContentView(dialogView);

        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelLogout);
        com.google.android.material.button.MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirmLogout);

        if (btnCancel != null) btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                bottomSheetDialog.dismiss();
                session.logoutUser();
                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
        bottomSheetDialog.show();
    }

    private void navigateToTab(Class<?> targetClass) {
        Intent intent = new Intent(DashboardActivity.this, targetClass);
        intent.putExtra("PREV_TAB_INDEX", 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private void setupBottomNavigation() {
        highlightBottomNavItem(R.id.nav_home);

        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View clients = findViewById(R.id.nav_clients);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) home.setOnClickListener(v -> highlightBottomNavItem(R.id.nav_home));
        if (bookings != null) bookings.setOnClickListener(v -> navigateToTab(BookingsActivity.class));
        if (packages != null) packages.setOnClickListener(v -> navigateToTab(PackagesActivity.class));
        if (clients != null) clients.setOnClickListener(v -> navigateToTab(ClientsActivity.class));
        if (profile != null) profile.setOnClickListener(v -> navigateToTab(ProfileActivity.class));
    }

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
        if (indicator != null) {
            final int targetIndex = activeIndex;
            final int initialIndex = (prevIndex >= 0 && prevIndex != targetIndex) ? prevIndex : targetIndex;

            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int totalWidth = screenWidth - (int) (52 * getResources().getDisplayMetrics().density);
            final int[] effectiveTabWidth = {totalWidth / 5};
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
                        effectiveTabWidth[0] = measuredWidth / 5;
                        if (lp != null) {
                            lp.width = effectiveTabWidth[0] - (marginHorizontal * 2);
                            indicator.setLayoutParams(lp);
                        }
                    }

                    float targetX = navContainer.getPaddingLeft() + (targetIndex * effectiveTabWidth[0]) + marginHorizontal;

                    indicator.animate()
                            .translationX(targetX)
                            .setDuration(380)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                            .start();
                });
            }
        }
    }
}
