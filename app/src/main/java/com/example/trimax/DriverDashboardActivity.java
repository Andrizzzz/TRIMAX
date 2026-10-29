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
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private boolean askedForLocation;

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
    /** File the camera app writes the full-size attendance photo into. */
    private File pendingDutyCapture;
    /** Start duty was tapped before location permission was answered: continue to the camera after. */
    private boolean dutyStartWaitingForLocation;

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
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                granted -> {
                    if (locationHelper.hasLocationPermission()) {
                        startDutyTracking();
                    } else {
                        Toast.makeText(this, "Location is off: the admin map will not show you while on duty.",
                                Toast.LENGTH_LONG).show();
                    }
                    if (dutyStartWaitingForLocation) {     // continue the Start duty that asked for it
                        dutyStartWaitingForLocation = false;
                        openAttendanceCameraWithPermission();
                    }
                });
        if (savedInstanceState != null) {
            isEndingDuty = savedInstanceState.getBoolean("duty_ending", false);
            String pending = savedInstanceState.getString("duty_capture", null);
            pendingDutyCapture = pending != null ? new File(pending) : null;
        }

        drawerLayout = findViewById(R.id.drawerLayout);

        attendanceCameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    File rawCapture = pendingDutyCapture;
                    pendingDutyCapture = null;
                    if (result.getResultCode() == RESULT_OK) {
                        // Full-size photo from the file the camera wrote; the camera's small preview otherwise.
                        Bitmap photoBitmap = CameraCapture.readUpright(rawCapture);
                        if (photoBitmap == null && result.getData() != null && result.getData().getExtras() != null) {
                            Object preview = result.getData().getExtras().get("data");
                            if (preview instanceof Bitmap) photoBitmap = (Bitmap) preview;
                        }
                        if (rawCapture != null) rawCapture.delete();   // the stamped copy below is kept

                        if (photoBitmap != null) {
                            File savedFile = ImageTimestampHelper.saveBitmapWithTimestamp(
                                    DriverDashboardActivity.this,
                                    photoBitmap,
                                    "DRIVER_DUTY_ATTENDANCE",
                                    locationHelper.getLatitude(),
                                    locationHelper.getLongitude()
                            );

                            onAttendancePhoto(savedFile, photoBitmap);
                        } else {
                            stopGpsWarmUp();
                            Toast.makeText(DriverDashboardActivity.this, "Failed to retrieve attendance photo.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (rawCapture != null) rawCapture.delete();
                        stopGpsWarmUp();
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
        showGreeting();

        View statEarnings = findViewById(R.id.statEarnings);
        if (statEarnings != null) statEarnings.setOnClickListener(v -> showEarnings());

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
            btnHeroStartDelivery.setOnClickListener(v -> onHeroPrimaryAction());
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
        // After a restart or theme change the activity is new: resume live GPS if still on duty.
        if (session.isDutyOnline() && !locationHelper.isTrackingActive()) startDutyTracking();
        syncDutyFromServer();
        IssueReporter.flush(this);          // SOS / problem reports made while offline
        ProofUploader.flush(this);          // drop-off photos not yet on the server

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

    @Override
    protected void onDestroy() {
        // The GPS listener holds this screen; a new dashboard restarts it in onResume.
        if (locationHelper != null) locationHelper.stopLocationUpdates();
        super.onDestroy();
    }

    /** One greeting: "Good evening," above the driver's name (the two lines used to disagree). */
    private void showGreeting() {
        TextView label = findViewById(R.id.tvDriverGreetingLabel);
        TextView name = findViewById(R.id.tvDriverGreeting);
        if (label != null) label.setText(Greeting.now() + ",");
        if (name != null && session != null) name.setText(Greeting.nameLine(session.getFullName()));
    }

    private void refreshDriverDashboard() {
        updateDutyUI();
        loadAssignedHeroDelivery();
        showGreeting();
        loadSummary();
    }

    /** The last numbers from the server (null until the first answer). */
    private DriverSummary summary;

    /** Delivered today, earnings this month (Payroll) and still to deliver, from the server. */
    private void loadSummary() {
        TrimaxApi.get(this).driverSummary(new TrimaxApi.Callback<DriverSummary>() {
            @Override
            public void onSuccess(DriverSummary s) {
                if (SessionGuard.isGone(DriverDashboardActivity.this)) return;
                summary = s;
                setStat(R.id.tvStatDeliveries, String.valueOf(s.deliveredToday));
                setStat(R.id.tvStatEarnings, DriverSummary.pesoShort(s.payMonth));
                setStat(R.id.tvStatToDeliver, String.valueOf(s.toDeliver));
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(DriverDashboardActivity.this)) return;
                if (SessionGuard.handle(DriverDashboardActivity.this, error)) return;
                // keep the last numbers (or "–"); the dashboard itself still works
            }
        });
    }

    private void setStat(int id, String text) {
        TextView tv = findViewById(id);
        if (tv != null) tv.setText(text);
    }

    /** Earnings: this month's pay and the latest payroll records the admin entered. */
    private void showEarnings() {
        DriverSummary s = summary;
        StringBuilder msg = new StringBuilder();
        if (s == null) {
            msg.append("Your earnings could not be loaded yet. Check the connection and pull down to refresh.");
        } else {
            msg.append("This month: ").append(DriverSummary.peso(s.payMonth)).append("\n");
            msg.append("Delivered this month: ").append(s.deliveredMonth).append(s.deliveredMonth == 1 ? " booking" : " bookings")
               .append(" (").append(DriverSummary.peso(s.deliveredMonthAmount)).append(")\n");
            msg.append("Delivered today: ").append(s.deliveredToday).append(" (").append(DriverSummary.peso(s.deliveredTodayAmount)).append(")\n\n");
            if (s.payRecords.isEmpty()) {
                msg.append("No pay recorded yet. Your pay shows here once the admin records it in Payroll.");
            } else {
                msg.append("Latest pay:\n");
                for (DriverSummary.Pay p : s.payRecords) msg.append("• ").append(p.line()).append("\n");
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("My earnings")
                .setMessage(msg.toString().trim())
                .setPositiveButton("OK", null)
                .show();
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
                BookingNotifications.syncDriver(DriverDashboardActivity.this, session.getUsername(), records);
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

        btnHeroStartDelivery.setEnabled(!heroUpdating);   // not while Start is being sent
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

        if (currentHeroRecord != null && DeliveryProgress.PENDING.equals(currentHeroRecord.status)) {
            tvHeroStatusPill.setBackgroundResource(R.drawable.bg_status_pending);
            tvHeroStatusPill.setTextColor(0xFFD97706);
            tvHeroStatusPill.setText("AWAITING ADMIN");
            btnHeroStartDelivery.setText("VIEW DETAILS");
        } else {
            tvHeroStatusPill.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvHeroStatusPill.setTextColor(0xFF059669);
            btnHeroStartDelivery.setText(heroUpdating ? "STARTING..." : "START NAVIGATION");
        }
    }

    /** True while Start Delivery is being sent, so a second tap or a refresh cannot send it again. */
    private boolean heroUpdating;

    /**
     * The hero card's main button, by the server's status:
     *  - assigned and confirmed: Start Delivery (ASSIGNED -> STARTED), then the in-app navigation opens;
     *  - already started: the navigation opens;
     *  - waiting for the administrator (label VIEW DETAILS), failed, completed or cancelled: the details open.
     * Before, a booking still waiting for the administrator also tried to start (the server refused it).
     */
    private void onHeroPrimaryAction() {
        if (currentHeroDelivery == null || currentHeroRecord == null || heroUpdating) return;
        final BookingRecord record = currentHeroRecord;
        final String id = record.idText();
        DeliveryProgress.NextStep step = DeliveryProgress.nextStep(record.status, record.deliveryStatus);

        if (!DeliveryProgress.STARTED.equals(step.target)) {
            if (DeliveryProgress.isUnderWay(record.deliveryStatus)) {
                openNavigation(id, record.toDelivery(true));
            } else {
                Intent details = new Intent(this, DriverDeliveryDetailsActivity.class);
                details.putExtra("DELIVERY_ID", id);
                startActivity(details);
            }
            return;
        }

        heroUpdating = true;
        btnHeroStartDelivery.setEnabled(false);
        btnHeroStartDelivery.setText("STARTING...");
        String expected = record.deliveryStatus != null ? record.deliveryStatus : DeliveryProgress.ASSIGNED;
        Double lat = locationHelper.hasValidLocationFix() ? locationHelper.getLatitude() : null;
        Double lng = locationHelper.hasValidLocationFix() ? locationHelper.getLongitude() : null;
        TrimaxApi.get(this).updateDeliveryStatus(record.bookingId, expected, DeliveryProgress.STARTED, lat, lng, null, null,
                new TrimaxApi.Callback<BookingRecord>() {
                    @Override
                    public void onSuccess(BookingRecord saved) {
                        heroUpdating = false;
                        if (SessionGuard.isGone(DriverDashboardActivity.this)) return;
                        dbHelper.logLocation(session.getUsername(), id, locationHelper.getLatitude(), locationHelper.getLongitude(), locationHelper.getAccuracy(), 0f, 0f);
                        // Show what the server saved (the card may have moved on to another booking meanwhile).
                        if (currentHeroRecord != null && currentHeroRecord.bookingId == saved.bookingId) {
                            currentHeroRecord = saved;
                            currentHeroDelivery = saved.toDelivery(true);
                        }
                        bindHeroDelivery(activeRecords != null ? activeRecords.size() : 1);
                        Toast.makeText(DriverDashboardActivity.this, "Delivery #" + id + " "
                                + DeliveryProgress.driverDisplayStatus(saved.status, saved.deliveryStatus)
                                + "! Opening Navigation...", Toast.LENGTH_SHORT).show();
                        openNavigation(id, saved.toDelivery(true));
                    }

                    @Override
                    public void onError(ApiException error) {
                        heroUpdating = false;
                        if (SessionGuard.isGone(DriverDashboardActivity.this)) return;
                        if (SessionGuard.handle(DriverDashboardActivity.this, error)) return;
                        bindHeroDelivery(activeRecords != null ? activeRecords.size() : 1);
                        Toast.makeText(DriverDashboardActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        loadAssignedHeroDelivery();   // show what the server has now (a timed-out start may have been saved)
                    }
                });
    }

    private void openNavigation(String id, Delivery d) {
        Intent intent = new Intent(DriverDashboardActivity.this, DriverMapActivity.class);
        intent.putExtra("DELIVERY_ID", id);
        intent.putExtra("DEST_ADDRESS", d.getDestination());
        intent.putExtra("DEST_LAT", d.getDestinationLatitude());
        intent.putExtra("DEST_LNG", d.getDestinationLongitude());
        intent.putExtra("CUSTOMER_NAME", d.getCustomerName());
        startActivity(intent);
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

    /** The server records the duty change first (photo + event) so the admin sees who is on duty. */
    private void onAttendancePhoto(File savedFile, Bitmap photoBitmap) {
        final boolean ending = isEndingDuty;
        isEndingDuty = false;
        final String path = savedFile != null ? savedFile.getAbsolutePath() : "";
        final double lat = locationHelper.getLatitude();
        final double lng = locationHelper.getLongitude();
        setDutyButtonBusy(true);
        DutySync.record(this, !ending, savedFile, photoBitmap, locationHelper, new DutySync.Listener() {
            @Override
            public void onDutyChanged(boolean onDuty) {
                applyDutyChange(onDuty, path, lat, lng);
            }

            @Override
            public void onFinished() {
                setDutyButtonBusy(false);
                stopGpsWarmUp();
            }
        });
    }

    /**
     * GPS for the attendance record itself: started when Start/End duty is tapped, so a position is
     * ready by the time the photo is taken (off duty, the dashboard does not track otherwise).
     */
    private void warmUpGps() {
        if (!locationHelper.isTrackingActive() && locationHelper.hasLocationPermission()) {
            locationHelper.startLocationUpdates(null);
        }
    }

    /** Off duty again (or the start did not happen): stop the GPS the attendance step started. */
    private void stopGpsWarmUp() {
        if (!session.isDutyOnline()) locationHelper.stopLocationUpdates();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("duty_ending", isEndingDuty);
        if (pendingDutyCapture != null) outState.putString("duty_capture", pendingDutyCapture.getAbsolutePath());
    }

    private void applyDutyChange(boolean nowOnDuty, String photoPath, double lat, double lng) {
        // Keep the phone's attendance log too (the profile screen shows it).
        if (nowOnDuty) {
            dbHelper.saveAttendance(session.getUsername(), photoPath, lat, lng);
        } else {
            dbHelper.endDutyWithPhoto(session.getUsername(), photoPath, lat, lng);
        }
        session.setDutyOnline(nowOnDuty);
        if (nowOnDuty) {
            startDutyTracking();
            locationHelper.pushLocationToSupabaseIfOnline(this, true);
        } else {
            locationHelper.stopLocationUpdates();
        }
        updateDutyUI();
        Toast.makeText(this, nowOnDuty ? "✓ Attendance verified. Duty started." : "✓ Attendance verified. Duty ended.",
                Toast.LENGTH_LONG).show();
    }

    /** Live GPS while on duty: logs on the phone and shares with the admin map. */
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
        locationHelper.startLocationUpdates((latitude, longitude, accuracy, speed, bearing) -> {
            dbHelper.logLocation(session.getUsername(), currentHeroDelivery != null ? currentHeroDelivery.getBookingId() : "",
                    latitude, longitude, accuracy, speed, bearing);
            locationHelper.pushLocationToSupabaseIfOnline(DriverDashboardActivity.this, session.isDutyOnline());
            updateDutyUI();
        });
    }

    /** The server is the source of truth for duty (the admin may have a newer state than this phone). */
    private void syncDutyFromServer() {
        DutySync.loadServerState(this, server -> {
            if (server != session.isDutyOnline()) {
                session.setDutyOnline(server);
                if (server) startDutyTracking(); else locationHelper.stopLocationUpdates();
                updateDutyUI();
            }
        });
    }

    private void setDutyButtonBusy(boolean busy) {
        if (btnToggleDuty == null) return;
        btnToggleDuty.setEnabled(!busy);
        btnToggleDuty.animate().alpha(busy ? 0.55f : 1f).setDuration(150).start();
        if (busy) btnToggleDuty.setText("SAVING…");
        else updateDutyUI();
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
            // The start record should carry where the driver is: ask for location once, before the camera.
            if (!locationHelper.hasLocationPermission() && !askedForLocation && locationPermissionLauncher != null) {
                askedForLocation = true;
                dutyStartWaitingForLocation = true;
                locationPermissionLauncher.launch(new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION});
                return;
            }
            openAttendanceCameraWithPermission();
        }
    }

    private void openAttendanceCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchAttendanceCamera();
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void launchAttendanceCamera() {
        warmUpGps();
        pendingDutyCapture = CameraCapture.newTargetFile(this, "DUTY");
        Intent takePictureIntent = CameraCapture.intent(this, pendingDutyCapture);
        try {
            attendanceCameraLauncher.launch(takePictureIntent);
        } catch (Exception e) {
            if (pendingDutyCapture != null) pendingDutyCapture.delete();
            pendingDutyCapture = null;
            stopGpsWarmUp();
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
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_report_problem, null);
        bottomSheetDialog.setContentView(dialogView);

        ImageView btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        android.widget.RadioGroup rg = dialogView.findViewById(R.id.rgProblemTypes);
        com.google.android.material.textfield.TextInputEditText etDesc = dialogView.findViewById(R.id.etProblemDescription);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmitReport);

        if (btnClose != null) btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                int checkedId = rg != null ? rg.getCheckedRadioButtonId() : -1;
                String selectedType = "Other problem";
                if (checkedId == R.id.rbVehicle) selectedType = "Vehicle problem";
                else if (checkedId == R.id.rbCustomer) selectedType = "Customer unreachable";
                else if (checkedId == R.id.rbAddress) selectedType = "Incorrect address";
                else if (checkedId == R.id.rbOther) selectedType = "Other problem";

                String details = etDesc != null && etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                String desc = "Reported: " + selectedType + (details.isEmpty() ? "" : " - " + details);

                // Kept on this phone and sent to the admin's Notifications (queued and retried when offline).
                // The real GPS fix is sent only when there is one; no stand-in location goes to the admin.
                IssueReporter.send(this, false, bookingId, selectedType, desc, locationHelper);
                bottomSheetDialog.dismiss();
            });
        }
        bottomSheetDialog.show();
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
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
        bottomSheetDialog.show();
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
                            .setDuration(380)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f))
                            .start();
                });
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
