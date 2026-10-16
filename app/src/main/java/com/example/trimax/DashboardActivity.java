package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

public class DashboardActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private MockMapView dashboardMapView;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        session = new SessionManager(this);

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
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), 0);
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

        CardView cardDashboardBooking1 = findViewById(R.id.cardDashboardBooking1);
        CardView cardDashboardBooking2 = findViewById(R.id.cardDashboardBooking2);

        // FoodPanda Style Live Map Tracking Views
        dashboardMapView = findViewById(R.id.dashboardMapView);
        MaterialButton btnTrackFullMap = findViewById(R.id.btnTrackFullMap);

        if (dashboardMapView != null) {
            dashboardMapView.startLiveDeliveryAnimation();
        }

        if (btnTrackFullMap != null) {
            btnTrackFullMap.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, DriverMapActivity.class);
                intent.putExtra("DELIVERY_ID", "BK-2026-00123");
                startActivity(intent);
            });
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        if (bottomNavigation != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
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
            // Update Drawer Header text
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

        // 2. NOTIFICATION BELL
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> startActivity(new Intent(DashboardActivity.this, NotificationsActivity.class)));
        }

        // 4. DASHBOARD STATS CARDS
        if (statTotalBookings != null) {
            statTotalBookings.setOnClickListener(v -> openBookingsWithFilter("ALL"));
        }
        if (statPendingBookings != null) {
            statPendingBookings.setOnClickListener(v -> openBookingsWithFilter("PENDING"));
        }
        if (statConfirmedBookings != null) {
            statConfirmedBookings.setOnClickListener(v -> openBookingsWithFilter("CONFIRMED"));
        }

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
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            setupBottomNavigation(bottomNavigation);
        }

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dashboardMapView != null) {
            dashboardMapView.onResume();
            dashboardMapView.startLiveDeliveryAnimation();
        }
        updateHeaderUI();
        updateDashboardStats();
        loadRecentBookings();
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
        if (dashboardMapView != null) {
            dashboardMapView.onPause();
        }
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

        card.setOnClickListener(v -> openBookingDetail(delivery.getBookingId(), delivery.getCustomerName(), delivery.getStatus(), String.format("₱ %,.2f", delivery.getCurrentTotal())));
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

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingsActivity.class));
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, ClientsActivity.class));
                return true;
            } else if (itemId == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }
}
