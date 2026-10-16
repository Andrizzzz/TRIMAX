package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;

    private CardView cardNotif1, cardNotif2;
    private TextView tvNotif1Title, tvNotif1Message;
    private TextView tvNotif2Title, tvNotif2Message;

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

        TextView tvMarkAllRead = findViewById(R.id.tvMarkAllRead);
        cardNotif1 = findViewById(R.id.cardNotif1);
        cardNotif2 = findViewById(R.id.cardNotif2);
        tvNotif1Title = findViewById(R.id.tvNotif1Title);
        tvNotif2Title = findViewById(R.id.tvNotif2Title);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
            bottomNavigation.setSelectedItemId(R.id.nav_notifications);
            setupBottomNavigation(bottomNavigation);
        }

        if (tvMarkAllRead != null) {
            tvMarkAllRead.setOnClickListener(v -> {
                dbHelper.markAllNotificationsAsRead(session.getUsername());
                loadNotifications();
                Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
            });
        }

        if (cardNotif1 != null) {
            cardNotif1.setOnClickListener(v -> {
                Intent intent = new Intent(NotificationsActivity.this, BookingDetailActivity.class);
                intent.putExtra("BOOKING_ID", "BK-2026-00123");
                startActivity(intent);
            });
        }

        if (cardNotif2 != null) {
            cardNotif2.setOnClickListener(v -> {
                Intent intent = new Intent(NotificationsActivity.this, BookingDetailActivity.class);
                intent.putExtra("BOOKING_ID", "BK-2026-00122");
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
            } else if (itemId == R.id.nav_notifications) {
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
