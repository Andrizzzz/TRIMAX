package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class DriverNotificationsActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper dbHelper;

    private TextView tvNotif1Title, tvNotif2Title;
    private TextView tvNotif1Message, tvNotif2Message, tvNotif1Time, tvNotif2Time;
    private CardView cardDriverNotif1, cardDriverNotif2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_driver_notifications);

        session = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        cardDriverNotif1 = findViewById(R.id.cardDriverNotif1);
        cardDriverNotif2 = findViewById(R.id.cardDriverNotif2);
        tvNotif1Title = findViewById(R.id.tvNotif1Title);
        tvNotif2Title = findViewById(R.id.tvNotif2Title);
        tvNotif1Message = findViewById(R.id.tvNotif1Message);
        tvNotif2Message = findViewById(R.id.tvNotif2Message);
        tvNotif1Time = findViewById(R.id.tvNotif1Time);
        tvNotif2Time = findViewById(R.id.tvNotif2Time);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

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
    }

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

        card.setOnClickListener(v -> {
            dbHelper.markNotificationAsRead(notification.getId());
            loadNotifications();
        });
    }

    private void handleBackNavigation() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DriverDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
