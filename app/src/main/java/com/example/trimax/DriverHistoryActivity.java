package com.example.trimax;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class DriverHistoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private LinearLayout containerHistory;
    private String activeFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_history);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        containerHistory = findViewById(R.id.containerHistory);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View topBar = findViewById(R.id.topBar);
            if (topBar != null) {
                int baseTopPadding = Math.round(16 * getResources().getDisplayMetrics().density);
                topBar.setPadding(topBar.getPaddingLeft(), systemBars.top + baseTopPadding, topBar.getPaddingRight(), topBar.getPaddingBottom());
            }
            // Match the shared driver-screen layout: keep the floating navigation clear of system bars.
            View bottomNav = findViewById(R.id.cardBottomNavContainer);
            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                int desiredBottomMargin = systemBars.bottom + Math.round(16 * getResources().getDisplayMetrics().density);
                if (params.bottomMargin != desiredBottomMargin) {
                    params.bottomMargin = desiredBottomMargin;
                    bottomNav.setLayoutParams(params);
                }
            }
            return insets;
        });

        MaterialButton chipAll = findViewById(R.id.chipAll);
        MaterialButton chipCompleted = findViewById(R.id.chipCompleted);
        MaterialButton chipFailed = findViewById(R.id.chipFailed);



        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(DriverHistoryActivity.this, DriverDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        if (chipAll != null) chipAll.setOnClickListener(v -> setFilter("ALL"));
        if (chipCompleted != null) chipCompleted.setOnClickListener(v -> setFilter("DELIVERED"));
        if (chipFailed != null) chipFailed.setOnClickListener(v -> setFilter("FAILED"));

        updateFilterChipStyles();
        loadHistory();

        setupBottomNavigation();
    }

    private void navigateToTab(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        intent.putExtra("PREV_TAB_INDEX", 2);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private void setupBottomNavigation() {
        highlightBottomNavItem(R.id.nav_packages);

        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) home.setOnClickListener(v -> navigateToTab(DriverDashboardActivity.class));
        if (bookings != null) bookings.setOnClickListener(v -> navigateToTab(DriverDeliveriesActivity.class));
        if (packages != null) packages.setOnClickListener(v -> highlightBottomNavItem(R.id.nav_packages));
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



    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void setFilter(String filter) {
        this.activeFilter = filter;
        updateFilterChipStyles();
        loadHistory();
    }

    private void updateFilterChipStyles() {
        MaterialButton chipAll = findViewById(R.id.chipAll);
        MaterialButton chipCompleted = findViewById(R.id.chipCompleted);
        MaterialButton chipFailed = findViewById(R.id.chipFailed);
        styleFilterChip(chipAll, "ALL".equals(activeFilter));
        styleFilterChip(chipCompleted, "DELIVERED".equals(activeFilter));
        styleFilterChip(chipFailed, "FAILED".equals(activeFilter));
    }

    private void styleFilterChip(MaterialButton chip, boolean selected) {
        if (chip == null) return;
        int fillColor = getResources().getColor(selected ? R.color.primary_blue : R.color.card_bg);
        int textColor = getResources().getColor(selected ? android.R.color.white : R.color.text_primary);
        int strokeColor = getResources().getColor(selected ? R.color.primary_blue : R.color.border_subtle);
        chip.setBackgroundTintList(ColorStateList.valueOf(fillColor));
        chip.setTextColor(textColor);
        chip.setStrokeColor(ColorStateList.valueOf(strokeColor));
        chip.setStrokeWidth(dp(selected ? 0 : 1));
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setAlpha(1f);
    }

    /** This driver's finished deliveries from the TRIMAX server; the chips filter them on the phone. */
    private List<BookingRecord> historyRecords;
    private boolean loadingHistory;

    private void loadHistory() {
        if (containerHistory == null) return;
        if (historyRecords != null) {
            renderHistory();      // filter chip changed; the list is fetched fresh each time the screen opens
            return;
        }
        if (loadingHistory) return;
        loadingHistory = true;
        if (historyRecords == null) showHistoryMessage("Loading delivery history...", false);
        TrimaxApi.get(this).listDriverDeliveries("history", new TrimaxApi.Callback<List<BookingRecord>>() {
            @Override
            public void onSuccess(List<BookingRecord> records) {
                loadingHistory = false;
                if (SessionGuard.isGone(DriverHistoryActivity.this)) return;
                historyRecords = records;
                renderHistory();
            }

            @Override
            public void onError(ApiException error) {
                loadingHistory = false;
                if (SessionGuard.isGone(DriverHistoryActivity.this)) return;
                if (SessionGuard.handle(DriverHistoryActivity.this, error)) return;
                if (historyRecords == null) showHistoryMessage(error.getMessage() + "\nTap here to try again.", true);
                else android.widget.Toast.makeText(DriverHistoryActivity.this, "Could not refresh: " + error.getMessage(),
                        android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showHistoryMessage(String text, boolean retry) {
        containerHistory.removeAllViews();
        TextView tvEmpty = new TextView(this);
        tvEmpty.setText(text);
        tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
        tvEmpty.setTextSize(16);
        tvEmpty.setPadding(dp(20), dp(40), dp(20), dp(20));
        if (retry) tvEmpty.setOnClickListener(v -> loadHistory());
        containerHistory.addView(tvEmpty);
    }

    private void renderHistory() {
        containerHistory.removeAllViews();
        List<Delivery> list = new java.util.ArrayList<>();
        for (BookingRecord r : historyRecords) {
            Delivery d = r.toDelivery(true);
            String s = d.getStatus();
            boolean delivered = "DELIVERED".equals(s) || "COMPLETED".equals(s);
            if ("DELIVERED".equals(activeFilter) && !delivered) continue;
            if ("FAILED".equals(activeFilter) && !"FAILED".equals(s)) continue;
            list.add(d);
        }

        if (list.isEmpty()) {
            showHistoryMessage("No delivery history records found.", false);
            return;
        }

        for (Delivery d : list) {
            CardView card = new CardView(this);
            CardView.LayoutParams cardParams = new CardView.LayoutParams(
                    CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(16));
            card.setLayoutParams(cardParams);
            card.setRadius(18);
            card.setCardBackgroundColor(getResources().getColor(R.color.card_bg));
            card.setCardElevation(3);
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dp(18), dp(18), dp(18), dp(18));

            TextView tvCode = new TextView(this);
            tvCode.setText(String.format("Booking #%s", d.getBookingId()));
            tvCode.setTextColor(getResources().getColor(R.color.text_primary));
            tvCode.setTextSize(16);
            tvCode.setMaxLines(2);
            tvCode.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tvCode.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            tvCode.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvCustomer = new TextView(this);
            tvCustomer.setText(String.format("Customer: %s", d.getCustomerName()));
            tvCustomer.setTextColor(getResources().getColor(R.color.primary_blue));
            tvCustomer.setTextSize(14);
            tvCustomer.setMaxLines(3);
            tvCustomer.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            tvCustomer.setPadding(0, dp(6), 0, 0);

            TextView tvDest = new TextView(this);
            tvDest.setText(String.format("Destination: %s", d.getDestinationAddress()));
            tvDest.setTextColor(getResources().getColor(R.color.text_secondary));
            tvDest.setTextSize(13);
            tvDest.setMaxLines(3);
            tvDest.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            tvDest.setPadding(0, dp(6), 0, 0);

            TextView tvStatus = new TextView(this);
            tvStatus.setText(String.format("Status: %s", d.getStatus()));
            tvStatus.setTextColor(getResources().getColor("DELIVERED".equalsIgnoreCase(d.getStatus()) ? R.color.status_delivered : R.color.status_cancelled));
            tvStatus.setTextSize(13);
            tvStatus.setMaxLines(2);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setPadding(0, dp(8), 0, 0);

            layout.addView(tvCode);
            layout.addView(tvCustomer);
            layout.addView(tvDest);
            layout.addView(tvStatus);

            card.addView(layout);

            card.setOnClickListener(v -> {
                Intent intent = new Intent(DriverHistoryActivity.this, DriverDeliveryDetailsActivity.class);
                intent.putExtra("DELIVERY_ID", d.getBookingId());
                startActivity(intent);
            });

            containerHistory.addView(card);
        }
    }
}
