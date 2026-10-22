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

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), systemBars.bottom);
                return insets;
            });
        }

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

    private void loadHistory() {
        if (containerHistory == null) return;
        containerHistory.removeAllViews();

        List<Delivery> list = dbHelper.getDeliveryHistory(session.getUsername(), activeFilter);

        if (list.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No delivery history records found.");
            tvEmpty.setTextColor(getResources().getColor(R.color.text_secondary));
            tvEmpty.setTextSize(16);
            tvEmpty.setPadding(dp(20), dp(40), dp(20), dp(20));
            containerHistory.addView(tvEmpty);
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
