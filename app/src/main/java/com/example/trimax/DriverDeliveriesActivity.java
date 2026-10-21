package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

public class DriverDeliveriesActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SessionManager session;
    private LinearLayout containerDeliveries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_deliveries);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        containerDeliveries = findViewById(R.id.containerDeliveries);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
            bottomNavigation.setSelectedItemId(R.id.nav_bookings);
            setupBottomNavigation(bottomNavigation);
        }

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

    private void setupBottomNavigation(BottomNavigationView bottomNavigation) {
        if (bottomNavigation.getMenu().findItem(R.id.nav_bookings) != null) {
            bottomNavigation.getMenu().findItem(R.id.nav_bookings).setTitle("Deliveries");
        }
        if (bottomNavigation.getMenu().findItem(R.id.nav_clients) != null) {
            bottomNavigation.getMenu().findItem(R.id.nav_clients).setTitle("Notifications");
        }
        if (bottomNavigation.getMenu().findItem(R.id.nav_packages) != null) {
            bottomNavigation.getMenu().findItem(R.id.nav_packages).setTitle("Delivery History");
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, DriverDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            } else if (itemId == R.id.nav_bookings) {
                return true;
            } else if (itemId == R.id.nav_packages) {
                startActivity(new Intent(this, DriverHistoryActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_clients) {
                startActivity(new Intent(this, DriverNotificationsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, DriverProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDeliveries();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void loadDeliveries() {
        if (containerDeliveries == null) return;
        containerDeliveries.removeAllViews();

        List<Delivery> deliveries = dbHelper.getAssignedDeliveries(session.getUsername());

        if (deliveries.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No active assigned bookings found.");
            tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
            tvEmpty.setTextSize(16);
            tvEmpty.setPadding(dp(20), dp(40), dp(20), dp(20));
            containerDeliveries.addView(tvEmpty);
            return;
        }

        for (int i = 0; i < deliveries.size(); i++) {
            Delivery d = deliveries.get(i);

            CardView card = new CardView(this);
            CardView.LayoutParams cardParams = new CardView.LayoutParams(
                    CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(16));
            card.setLayoutParams(cardParams);
            card.setRadius(20);
            card.setCardElevation(4);
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(dp(18), dp(18), dp(18), dp(18));

            // Header Row: Sequence Badge & Category Tag
            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView tvCategory = new TextView(this);
            if (i == 0) {
                tvCategory.setText("▶ CURRENT DELIVERY");
                tvCategory.setTextColor(0xFF2563EB);
            } else if (i == 1) {
                tvCategory.setText("⏭ NEXT DELIVERY");
                tvCategory.setTextColor(0xFFD97706);
            } else {
                tvCategory.setText("🗓 UPCOMING DELIVERY");
                tvCategory.setTextColor(0xFF64748B);
            }
            tvCategory.setTextSize(12);
            tvCategory.setTypeface(null, android.graphics.Typeface.BOLD);
            tvCategory.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView tvSeqBadge = new TextView(this);
            tvSeqBadge.setText(String.format("Seq #%d", d.getSequenceNumber()));
            tvSeqBadge.setBackgroundResource(R.drawable.bg_status_confirmed);
            tvSeqBadge.setTextColor(0xFF059669);
            tvSeqBadge.setTextSize(11);
            tvSeqBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            tvSeqBadge.setPadding(dp(12), dp(6), dp(12), dp(6));

            headerRow.addView(tvCategory);
            headerRow.addView(tvSeqBadge);

            // Booking ID & Customer
            TextView tvCode = new TextView(this);
            tvCode.setText(String.format("Booking #%s", d.getBookingId()));
            tvCode.setTextColor(getResources().getColor(R.color.text_primary));
            tvCode.setTextSize(18);
            tvCode.setMaxLines(2);
            tvCode.setEllipsize(android.text.TextUtils.TruncateAt.END);
            tvCode.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            tvCode.setPadding(0, dp(8), 0, 0);
            tvCode.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvCustomer = new TextView(this);
            tvCustomer.setText(String.format("Customer: %s • %s", d.getCustomerName(), d.getCustomerPhone()));
            tvCustomer.setTextColor(getResources().getColor(R.color.primary_blue));
            tvCustomer.setTextSize(14);
            tvCustomer.setMaxLines(3);
            tvCustomer.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            tvCustomer.setPadding(0, dp(4), 0, 0);

            TextView tvPickup = new TextView(this);
            tvPickup.setText(String.format("Pickup: %s", d.getPickupLocation()));
            tvPickup.setTextColor(getResources().getColor(R.color.text_secondary));
            tvPickup.setTextSize(13);
            tvPickup.setMaxLines(3);
            tvPickup.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            tvPickup.setPadding(0, dp(6), 0, 0);

            TextView tvDest = new TextView(this);
            tvDest.setText(String.format("Destination: %s", d.getDestination()));
            tvDest.setTextColor(getResources().getColor(R.color.text_primary));
            tvDest.setTextSize(13);
            tvDest.setMaxLines(3);
            tvDest.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
            tvDest.setTypeface(null, android.graphics.Typeface.BOLD);
            tvDest.setPadding(0, dp(6), 0, 0);

            // Special instructions if present
            if (!TextUtils.isEmpty(d.getSpecialInstructions())) {
                TextView tvInstructions = new TextView(this);
                tvInstructions.setText(String.format("⚠️ Instructions: %s", d.getSpecialInstructions()));
                tvInstructions.setTextColor(getResources().getColor(R.color.text_primary));
                tvInstructions.setBackgroundResource(R.drawable.bg_pill_unselected);
                tvInstructions.setTextSize(12);
                tvInstructions.setMaxLines(4);
                tvInstructions.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
                tvInstructions.setPadding(dp(12), dp(8), dp(12), dp(8));
                layout.addView(headerRow);
                layout.addView(tvCode);
                layout.addView(tvCustomer);
                layout.addView(tvPickup);
                layout.addView(tvDest);
                layout.addView(tvInstructions);
            } else {
                layout.addView(headerRow);
                layout.addView(tvCode);
                layout.addView(tvCustomer);
                layout.addView(tvPickup);
                layout.addView(tvDest);
            }

            TextView tvStatus = new TextView(this);
            tvStatus.setText(String.format("Status: %s", d.getStatus()));
            tvStatus.setTextColor("ASSIGNED".equalsIgnoreCase(d.getStatus()) ? 0xFF2563EB : 0xFF059669);
            tvStatus.setTextSize(13);
            tvStatus.setMaxLines(2);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setPadding(0, dp(8), 0, 0);
            layout.addView(tvStatus);

            card.addView(layout);

            final String bookingId = d.getBookingId();
            card.setOnClickListener(v -> {
                Intent intent = new Intent(DriverDeliveriesActivity.this, DriverDeliveryDetailsActivity.class);
                intent.putExtra("DELIVERY_ID", bookingId);
                startActivity(intent);
            });

            containerDeliveries.addView(card);
        }
    }
}
