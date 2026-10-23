package com.example.trimax;

import android.content.Intent;
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
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;


import java.util.ArrayList;
import java.util.List;

public class DriverNotificationsActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper dbHelper;

    private TextView tvNotif1Title, tvNotif2Title;
    private TextView tvNotif1Message, tvNotif2Message, tvNotif1Time, tvNotif2Time;
    private CardView cardDriverNotif1, cardDriverNotif2;
    private final List<CardView> extraNotificationCards = new ArrayList<>();
    private LinearLayout notificationContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_notifications);

        session = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        View topBar = findViewById(R.id.topBar);
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top + (int)(12 * getResources().getDisplayMetrics().density), v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        cardDriverNotif1 = findViewById(R.id.cardDriverNotif1);
        cardDriverNotif2 = findViewById(R.id.cardDriverNotif2);
        tvNotif1Title = findViewById(R.id.tvNotif1Title);
        tvNotif2Title = findViewById(R.id.tvNotif2Title);
        tvNotif1Message = findViewById(R.id.tvNotif1Message);
        tvNotif2Message = findViewById(R.id.tvNotif2Message);
        tvNotif1Time = findViewById(R.id.tvNotif1Time);
        tvNotif2Time = findViewById(R.id.tvNotif2Time);
        if (cardDriverNotif1 != null && cardDriverNotif1.getParent() instanceof LinearLayout) {
            notificationContainer = (LinearLayout) cardDriverNotif1.getParent();
        }



        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(DriverNotificationsActivity.this, DriverDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        setupDriverBottomNavigation();

        loadNotifications();
    }



    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        if (dbHelper == null || session == null) return;
        List<DatabaseHelper.NotificationItem> notifications =
                dbHelper.getNotificationsForUser(session.getUsername());

        bindNotification(0, notifications != null && notifications.size() > 0 ? notifications.get(0) : null);
        bindNotification(1, notifications != null && notifications.size() > 1 ? notifications.get(1) : null);
        clearExtraNotificationCards();
        if (notificationContainer != null && notifications != null) {
            int insertIndex = notificationContainer.indexOfChild(cardDriverNotif2) + 1;
            for (int i = 2; i < notifications.size(); i++) {
                CardView card = createNotificationCard(notifications.get(i));
                notificationContainer.addView(card, insertIndex++);
                extraNotificationCards.add(card);
            }
        }
    }

    private void clearExtraNotificationCards() {
        if (notificationContainer != null) {
            for (CardView card : extraNotificationCards) {
                notificationContainer.removeView(card);
            }
        }
        extraNotificationCards.clear();
    }

    private CardView createNotificationCard(DatabaseHelper.NotificationItem notification) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(12);
        card.setLayoutParams(params);
        card.setRadius(dp(16));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(getColor(R.color.card_bg));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        TextView title = new TextView(this);
        title.setText((notification.isRead() ? "⚪ " : "🔴 ") + (notification.getTitle() == null ? "" : notification.getTitle()));
        title.setTextColor(getColor(R.color.text_primary));
        title.setTextSize(15);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        TextView time = new TextView(this);
        time.setText(notification.getTimestamp() == null ? "" : notification.getTimestamp());
        time.setTextColor(getColor(R.color.text_secondary));
        time.setTextSize(11);
        time.setPadding(0, dp(4), 0, 0);
        TextView message = new TextView(this);
        message.setText(notification.getMessage() == null ? "" : notification.getMessage());
        message.setTextColor(getColor(R.color.text_secondary));
        message.setTextSize(13);
        message.setPadding(0, dp(4), 0, 0);
        content.addView(title);
        content.addView(time);
        content.addView(message);
        card.addView(content);
        card.setOnClickListener(v -> handleNotificationClick(notification));
        return card;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void bindNotification(int index, DatabaseHelper.NotificationItem notification) {
        CardView card = index == 0 ? cardDriverNotif1 : cardDriverNotif2;
        TextView title = index == 0 ? tvNotif1Title : tvNotif2Title;
        TextView message = index == 0 ? tvNotif1Message : tvNotif2Message;
        TextView time = index == 0 ? tvNotif1Time : tvNotif2Time;
        if (card == null) return;

        if (notification == null) {
            card.setVisibility(View.GONE);
            card.setOnClickListener(null);
            return;
        }

        card.setVisibility(View.VISIBLE);
        if (title != null) {
            title.setText((notification.isRead() ? "⚪ " : "🔴 ") + notification.getTitle());
        }
        if (message != null) message.setText(notification.getMessage());
        if (time != null) time.setText(notification.getTimestamp() != null
                ? notification.getTimestamp() : "");

        card.setOnClickListener(v -> handleNotificationClick(notification));
    }

    private void handleNotificationClick(DatabaseHelper.NotificationItem notification) {
        dbHelper.markNotificationAsRead(notification.getId());
        String searchable = (notification.getTitle() == null ? "" : notification.getTitle()) + " " + (notification.getMessage() == null ? "" : notification.getMessage());
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("BK-[A-Za-z0-9-]+", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(searchable);
        if (matcher.find()) {
            Intent intent = new Intent(this, DriverDeliveryDetailsActivity.class);
            intent.putExtra("BOOKING_ID", matcher.group());
            startActivity(intent);
        } else {
            loadNotifications();
        }
    }
    private void navigateToTab(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        intent.putExtra("PREV_TAB_INDEX", 3);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
    }

    private void setupDriverBottomNavigation() {
        highlightBottomNavItem(R.id.nav_clients);

        View home = findViewById(R.id.nav_home);
        View bookings = findViewById(R.id.nav_bookings);
        View packages = findViewById(R.id.nav_packages);
        View clients = findViewById(R.id.nav_clients);
        View profile = findViewById(R.id.nav_profile);

        if (home != null) home.setOnClickListener(v -> navigateToTab(DriverDashboardActivity.class));
        if (bookings != null) bookings.setOnClickListener(v -> navigateToTab(DriverDeliveriesActivity.class));
        if (packages != null) packages.setOnClickListener(v -> navigateToTab(DriverHistoryActivity.class));
        if (clients != null) clients.setOnClickListener(v -> highlightBottomNavItem(R.id.nav_clients));
        if (profile != null) profile.setOnClickListener(v -> navigateToTab(DriverProfileActivity.class));
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
                            .setDuration(140)
                            .setInterpolator(new FastOutSlowInInterpolator())
                            .start();
                });
            }
        }
    }

}
