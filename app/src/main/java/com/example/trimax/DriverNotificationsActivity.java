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
        CardView cardDriverNotif1 = findViewById(R.id.cardDriverNotif1);
        CardView cardDriverNotif2 = findViewById(R.id.cardDriverNotif2);
        tvNotif1Title = findViewById(R.id.tvNotif1Title);
        tvNotif2Title = findViewById(R.id.tvNotif2Title);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        List<Delivery> assigned = dbHelper.getAssignedDeliveries(session.getUsername());
        String deliveryId1 = (assigned != null && assigned.size() > 0) ? assigned.get(0).getBookingId() : "TRX-1024";
        String deliveryId2 = (assigned != null && assigned.size() > 1) ? assigned.get(1).getBookingId() : "TRX-1025";

        if (cardDriverNotif1 != null) {
            cardDriverNotif1.setOnClickListener(v -> {
                dbHelper.markAllNotificationsAsRead(session.getUsername());
                Intent intent = new Intent(this, DriverDeliveryDetailsActivity.class);
                intent.putExtra("DELIVERY_ID", deliveryId1);
                startActivity(intent);
            });
        }

        if (cardDriverNotif2 != null) {
            cardDriverNotif2.setOnClickListener(v -> {
                dbHelper.markAllNotificationsAsRead(session.getUsername());
                Intent intent = new Intent(this, DriverDeliveryDetailsActivity.class);
                intent.putExtra("DELIVERY_ID", deliveryId2);
                startActivity(intent);
            });
        }

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

        if (notifications != null && !notifications.isEmpty()) {
            if (notifications.size() > 0 && tvNotif1Title != null) {
                DatabaseHelper.NotificationItem n1 = notifications.get(0);
                String prefix = n1.isRead() ? "⚪ " : "🔴 ";
                tvNotif1Title.setText(prefix + n1.getTitle());
            }
            if (notifications.size() > 1 && tvNotif2Title != null) {
                DatabaseHelper.NotificationItem n2 = notifications.get(1);
                String prefix = n2.isRead() ? "⚪ " : "🔴 ";
                tvNotif2Title.setText(prefix + n2.getTitle());
            }
        }
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
