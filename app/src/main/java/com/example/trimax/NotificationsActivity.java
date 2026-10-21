package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;

    private CardView cardNotif1, cardNotif2;
    private TextView tvNotif1Title, tvNotif1Message;
    private TextView tvNotif2Title, tvNotif2Message;
    private TextView tvNotif1Time, tvNotif2Time;
    private final List<CardView> extraNotificationCards = new ArrayList<>();
    private LinearLayout notificationContainer;
    private View.OnClickListener notificationClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
                return insets;
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        TextView tvMarkAllRead = findViewById(R.id.tvMarkAllRead);
        cardNotif1 = findViewById(R.id.cardNotif1);
        cardNotif2 = findViewById(R.id.cardNotif2);
        tvNotif1Title = findViewById(R.id.tvNotif1Title);
        tvNotif2Title = findViewById(R.id.tvNotif2Title);
        tvNotif1Message = findViewById(R.id.tvNotif1Message);
        tvNotif2Message = findViewById(R.id.tvNotif2Message);
        tvNotif1Time = findViewById(R.id.tvNotif1Time);
        tvNotif2Time = findViewById(R.id.tvNotif2Time);
        if (cardNotif1 != null && cardNotif1.getParent() instanceof LinearLayout) notificationContainer = (LinearLayout) cardNotif1.getParent();

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
            setupBottomNavigation(bottomNavigation);
        }

        if (tvMarkAllRead != null) {
            tvMarkAllRead.setOnClickListener(v -> {
                dbHelper.markAllNotificationsAsRead(session.getUsername());
                loadNotifications();
                Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
            });
        }

        View.OnClickListener notificationClick = v -> {
            Object value = v.getTag();
            if (value instanceof String && !((String) value).isEmpty()) {
                Intent intent = new Intent(NotificationsActivity.this, BookingDetailActivity.class);
                intent.putExtra("BOOKING_ID", (String) value);
                startActivity(intent);
            } else {
                Toast.makeText(this, "This notification is not linked to a booking.", Toast.LENGTH_SHORT).show();
            }
        };
        if (cardNotif1 != null) cardNotif1.setOnClickListener(notificationClick);
        if (cardNotif2 != null) cardNotif2.setOnClickListener(notificationClick);
        notificationClickListener = notificationClick;

        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        if (dbHelper == null || session == null) return;
        List<DatabaseHelper.NotificationItem> notifications = dbHelper.getNotificationsForUser(session.getUsername());

        bindNotification(0, notifications != null && notifications.size() > 0 ? notifications.get(0) : null);
        bindNotification(1, notifications != null && notifications.size() > 1 ? notifications.get(1) : null);
        clearExtraNotificationCards();
        if (notificationContainer != null && notifications != null) {
            int insertIndex = notificationContainer.indexOfChild(cardNotif2) + 1;
            for (int i = 2; i < notifications.size(); i++) {
                CardView card = createNotificationCard(notifications.get(i));
                notificationContainer.addView(card, insertIndex++);
                extraNotificationCards.add(card);
            }
        }
    }

    private void clearExtraNotificationCards() {
        if (notificationContainer != null) for (CardView card : extraNotificationCards) notificationContainer.removeView(card);
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
        String searchable = (notification.getTitle() == null ? "" : notification.getTitle()) + " " + (notification.getMessage() == null ? "" : notification.getMessage());
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("BK-[A-Za-z0-9-]+", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(searchable);
        card.setTag(matcher.find() ? matcher.group() : null);
        if (notificationClickListener != null) card.setOnClickListener(notificationClickListener);
        return card;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void bindNotification(int index, DatabaseHelper.NotificationItem notification) {
        CardView card = index == 0 ? cardNotif1 : cardNotif2;
        TextView title = index == 0 ? tvNotif1Title : tvNotif2Title;
        TextView message = index == 0 ? tvNotif1Message : tvNotif2Message;
        TextView time = index == 0 ? tvNotif1Time : tvNotif2Time;
        if (card == null) return;

        if (notification == null) {
            card.setVisibility(View.GONE);
            return;
        }

        card.setVisibility(View.VISIBLE);
        if (title != null) title.setText((notification.isRead() ? "⚪ " : "🔴 ") + notification.getTitle());
        if (message != null) message.setText(notification.getMessage() == null ? "" : notification.getMessage());
        if (time != null) time.setText(notification.getTimestamp() == null ? "" : notification.getTimestamp());

        String searchableText = (notification.getTitle() == null ? "" : notification.getTitle()) + " "
                + (notification.getMessage() == null ? "" : notification.getMessage());
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("BK-[A-Za-z0-9-]+", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(searchableText);
        card.setTag(matcher.find() ? matcher.group() : null);
    }

    private void handleBackNavigation() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, ClientsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_packages) {
                startActivity(new Intent(this, PackagesActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
