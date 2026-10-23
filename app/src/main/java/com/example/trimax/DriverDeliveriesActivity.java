package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class DriverDeliveriesActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private LinearLayout containerDeliveries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_deliveries);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        containerDeliveries = findViewById(R.id.containerDeliveries);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View topBar = findViewById(R.id.topBar);
            if (topBar != null) {
                int baseTopPadding = dp(16);
                topBar.setPadding(topBar.getPaddingLeft(), systemBars.top + baseTopPadding,
                        topBar.getPaddingRight(), topBar.getPaddingBottom());
            }

            // Keep the floating navigation above Android's gesture/3-button navigation area.
            View bottomNav = findViewById(R.id.cardBottomNavContainer);
            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                int desiredBottomMargin = systemBars.bottom + dp(16);
                if (params.bottomMargin != desiredBottomMargin) {
                    params.bottomMargin = desiredBottomMargin;
                    bottomNav.setLayoutParams(params);
                }
            }
            return insets;
        });

        setupBottomNavigation();

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(DriverDeliveriesActivity.this, DriverDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    private void navigateToTab(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        intent.putExtra("PREV_TAB_INDEX", 1);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private void setupBottomNavigation() {
        highlightBottomNavItem(R.id.nav_bookings);

        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) home.setOnClickListener(v -> navigateToTab(DriverDashboardActivity.class));
        if (bookings != null) bookings.setOnClickListener(v -> highlightBottomNavItem(R.id.nav_bookings));
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

    @Override
    protected void onResume() {
        super.onResume();
        loadDeliveries();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private boolean loading;
    private boolean showingRows;

    /**
     * Only deliveries the TRIMAX server says are assigned to the signed-in driver. The server decides who
     * that is from the sign-in token; this screen never sends a driver name.
     */
    private void loadDeliveries() {
        if (containerDeliveries == null || loading) return;
        loading = true;
        if (!showingRows) showMessage("Loading your deliveries...", false);
        TrimaxApi.get(this).listDriverDeliveries("active", new TrimaxApi.Callback<List<BookingRecord>>() {
            @Override
            public void onSuccess(List<BookingRecord> records) {
                loading = false;
                if (SessionGuard.isGone(DriverDeliveriesActivity.this)) return;
                renderDeliveries(records);
            }

            @Override
            public void onError(ApiException error) {
                loading = false;
                if (SessionGuard.isGone(DriverDeliveriesActivity.this)) return;
                if (SessionGuard.handle(DriverDeliveriesActivity.this, error)) return;
                if (showingRows) {
                    android.widget.Toast.makeText(DriverDeliveriesActivity.this,
                            "Could not refresh: " + error.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                } else {
                    showMessage(error.getMessage() + "\nTap here to try again.", true);
                }
            }
        });
    }

    private void showMessage(String text, boolean retry) {
        showingRows = false;
        containerDeliveries.removeAllViews();
        TextView tvEmpty = new TextView(this);
        tvEmpty.setText(text);
        tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
        tvEmpty.setTextSize(16);
        tvEmpty.setPadding(dp(20), dp(40), dp(20), dp(20));
        if (retry) tvEmpty.setOnClickListener(v -> loadDeliveries());
        containerDeliveries.addView(tvEmpty);
    }

    private void renderDeliveries(List<BookingRecord> records) {
        if (records.isEmpty()) {
            showMessage("No active assigned bookings found.", false);
            return;
        }
        containerDeliveries.removeAllViews();
        showingRows = true;
        List<Delivery> deliveries = new java.util.ArrayList<>();
        for (BookingRecord r : records) deliveries.add(r.toDelivery(true));

        for (int i = 0; i < deliveries.size(); i++) {
            Delivery d = deliveries.get(i);
            BookingRecord record = records.get(i);

            View cardView = android.view.LayoutInflater.from(this).inflate(R.layout.item_driver_delivery_card, containerDeliveries, false);

            TextView tvDeliveryStage = cardView.findViewById(R.id.tvDeliveryStage);
            TextView tvSequenceBadge = cardView.findViewById(R.id.tvSequenceBadge);
            TextView tvBookingId = cardView.findViewById(R.id.tvBookingId);
            TextView tvCustomerInfo = cardView.findViewById(R.id.tvCustomerInfo);
            TextView tvPickupLocation = cardView.findViewById(R.id.tvPickupLocation);
            TextView tvDestination = cardView.findViewById(R.id.tvDestination);
            TextView tvSpecialInstructions = cardView.findViewById(R.id.tvSpecialInstructions);
            TextView tvDeliveryStatus = cardView.findViewById(R.id.tvDeliveryStatus);

            if (i == 0) {
                tvDeliveryStage.setText("▶ CURRENT DELIVERY");
                tvDeliveryStage.setTextColor(0xFF2563EB);
            } else if (i == 1) {
                tvDeliveryStage.setText("⏭ NEXT DELIVERY");
                tvDeliveryStage.setTextColor(0xFFD97706);
            } else {
                tvDeliveryStage.setText("🗓 UPCOMING DELIVERY");
                tvDeliveryStage.setTextColor(0xFF64748B);
            }

            tvSequenceBadge.setText(String.format("Seq #%d", d.getSequenceNumber()));
            tvBookingId.setText(String.format("Booking #%s", d.getBookingId()));
            tvCustomerInfo.setText(String.format("👤 %s • %s", d.getCustomerName(), d.getCustomerPhone()));
            tvPickupLocation.setText(String.format("📍 Pickup: %s", d.getPickupLocation()));
            tvDestination.setText(String.format("🏁 Destination: %s", d.getDestination()));

            if (!TextUtils.isEmpty(d.getSpecialInstructions())) {
                tvSpecialInstructions.setVisibility(View.VISIBLE);
                tvSpecialInstructions.setText(String.format("⚠️ Instructions: %s", d.getSpecialInstructions()));
            } else {
                tvSpecialInstructions.setVisibility(View.GONE);
            }

            boolean awaitingAdmin = DeliveryProgress.PENDING.equals(record.status);
            tvDeliveryStatus.setText(String.format("Status: %s%s", d.getStatus(),
                    awaitingAdmin ? " \u2022 waiting for admin confirmation" : ""));
            tvDeliveryStatus.setTextColor("ASSIGNED".equalsIgnoreCase(d.getStatus()) ? 0xFF2563EB : 0xFF059669);

            final String bookingId = d.getBookingId();
            cardView.setOnClickListener(v -> {
                Intent intent = new Intent(DriverDeliveriesActivity.this, DriverDeliveryDetailsActivity.class);
                intent.putExtra("DELIVERY_ID", bookingId);
                startActivity(intent);
            });

            containerDeliveries.addView(cardView);
        }
    }
}
