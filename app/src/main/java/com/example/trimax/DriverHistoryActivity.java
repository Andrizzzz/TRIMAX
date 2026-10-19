package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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

        ImageView btnBack = findViewById(R.id.btnBack);
        containerHistory = findViewById(R.id.containerHistory);

        MaterialButton chipAll = findViewById(R.id.chipAll);
        MaterialButton chipCompleted = findViewById(R.id.chipCompleted);
        MaterialButton chipFailed = findViewById(R.id.chipFailed);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBackNavigation());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        if (chipAll != null) chipAll.setOnClickListener(v -> setFilter("ALL"));
        if (chipCompleted != null) chipCompleted.setOnClickListener(v -> setFilter("DELIVERED"));
        if (chipFailed != null) chipFailed.setOnClickListener(v -> setFilter("FAILED"));

        loadHistory();
    }

    private void setFilter(String filter) {
        this.activeFilter = filter;
        loadHistory();
    }

    private void loadHistory() {
        if (containerHistory == null) return;
        containerHistory.removeAllViews();

        List<Delivery> list = dbHelper.getDeliveryHistory(session.getUsername(), activeFilter);

        if (list.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No delivery history records found.");
            tvEmpty.setTextColor(0xFF64748B);
            tvEmpty.setTextSize(16);
            tvEmpty.setPadding(20, 40, 20, 20);
            containerHistory.addView(tvEmpty);
            return;
        }

        for (Delivery d : list) {
            CardView card = new CardView(this);
            CardView.LayoutParams cardParams = new CardView.LayoutParams(
                    CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 24);
            card.setLayoutParams(cardParams);
            card.setRadius(18);
            card.setCardElevation(3);
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(24, 24, 24, 24);

            TextView tvCode = new TextView(this);
            tvCode.setText(String.format("Delivery #%s", d.getDeliveryId()));
            tvCode.setTextColor(0xFF0F172A);
            tvCode.setTextSize(16);
            tvCode.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvCustomer = new TextView(this);
            tvCustomer.setText(String.format("Customer: %s", d.getCustomerName()));
            tvCustomer.setTextColor(0xFF2563EB);
            tvCustomer.setTextSize(14);
            tvCustomer.setPadding(0, 6, 0, 0);

            TextView tvDest = new TextView(this);
            tvDest.setText(String.format("Destination: %s", d.getDestinationAddress()));
            tvDest.setTextColor(0xFF64748B);
            tvDest.setTextSize(13);
            tvDest.setPadding(0, 4, 0, 0);

            TextView tvStatus = new TextView(this);
            tvStatus.setText(String.format("Status: %s", d.getStatus()));
            tvStatus.setTextColor("DELIVERED".equalsIgnoreCase(d.getStatus()) ? 0xFF059669 : 0xFFEF4444);
            tvStatus.setTextSize(13);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setPadding(0, 6, 0, 0);

            layout.addView(tvCode);
            layout.addView(tvCustomer);
            layout.addView(tvDest);
            layout.addView(tvStatus);

            card.addView(layout);

            card.setOnClickListener(v -> {
                Intent intent = new Intent(DriverHistoryActivity.this, DriverDeliveryDetailsActivity.class);
                intent.putExtra("DELIVERY_ID", d.getDeliveryId());
                startActivity(intent);
            });

            containerHistory.addView(card);
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