package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

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
        DriverNav.attach(this, R.id.nav_driver_deliveries);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        containerDeliveries = findViewById(R.id.containerDeliveries);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDeliveries();
    }

    private void loadDeliveries() {
        if (containerDeliveries == null) return;
        containerDeliveries.removeAllViews();

        List<Delivery> deliveries = dbHelper.getAssignedDeliveries(session.getUsername());

        if (deliveries.isEmpty()) {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No active assigned bookings found.");
            tvEmpty.setTextColor(0xFF64748B);
            tvEmpty.setTextSize(16);
            tvEmpty.setPadding(20, 40, 20, 20);
            containerDeliveries.addView(tvEmpty);
            return;
        }

        for (int i = 0; i < deliveries.size(); i++) {
            Delivery d = deliveries.get(i);

            CardView card = new CardView(this);
            CardView.LayoutParams cardParams = new CardView.LayoutParams(
                    CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 28);
            card.setLayoutParams(cardParams);
            card.setRadius(20);
            card.setCardElevation(4);
            card.setClickable(true);
            card.setFocusable(true);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(28, 28, 28, 28);

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
            tvSeqBadge.setPadding(16, 6, 16, 6);

            headerRow.addView(tvCategory);
            headerRow.addView(tvSeqBadge);

            // Booking ID & Customer
            TextView tvCode = new TextView(this);
            tvCode.setText(String.format("Booking #%s", d.getBookingId()));
            tvCode.setTextColor(0xFF0F172A);
            tvCode.setTextSize(18);
            tvCode.setPadding(0, 8, 0, 0);
            tvCode.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvCustomer = new TextView(this);
            tvCustomer.setText(String.format("Customer: %s • %s", d.getCustomerName(), d.getCustomerPhone()));
            tvCustomer.setTextColor(0xFF2563EB);
            tvCustomer.setTextSize(14);
            tvCustomer.setPadding(0, 4, 0, 0);

            TextView tvPickup = new TextView(this);
            tvPickup.setText(String.format("Pickup: %s", d.getPickupLocation()));
            tvPickup.setTextColor(0xFF475569);
            tvPickup.setTextSize(13);
            tvPickup.setPadding(0, 4, 0, 0);

            TextView tvDest = new TextView(this);
            tvDest.setText(String.format("Destination: %s", d.getDestination()));
            tvDest.setTextColor(0xFF0F172A);
            tvDest.setTextSize(13);
            tvDest.setTypeface(null, android.graphics.Typeface.BOLD);
            tvDest.setPadding(0, 2, 0, 0);

            // Special instructions if present
            if (!TextUtils.isEmpty(d.getSpecialInstructions())) {
                TextView tvInstructions = new TextView(this);
                tvInstructions.setText(String.format("⚠️ Instructions: %s", d.getSpecialInstructions()));
                tvInstructions.setTextColor(0xFF92400E);
                tvInstructions.setBackgroundColor(0xFFFFFBEB);
                tvInstructions.setTextSize(12);
                tvInstructions.setPadding(12, 8, 12, 8);
                tvInstructions.setPadding(12, 8, 12, 8);
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
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setPadding(0, 8, 0, 0);
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
