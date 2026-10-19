package com.example.trimax;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ClientDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_client_detail);

        // Keep screen content clear of the status and navigation bars.
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        String name = intent.getStringExtra("CLIENT_NAME");
        String phone = intent.getStringExtra("CLIENT_PHONE");
        String email = intent.getStringExtra("CLIENT_EMAIL");

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvClientName = findViewById(R.id.tvClientName);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvEmail = findViewById(R.id.tvEmail);

        View cardHistory1 = findViewById(R.id.tvHist1Id) != null ? (View) findViewById(R.id.tvHist1Id).getParent().getParent() : null;
        View cardHistory2 = findViewById(R.id.tvHist2Id) != null ? (View) findViewById(R.id.tvHist2Id).getParent().getParent() : null;

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }

        if (name != null) tvClientName.setText(name);
        if (phone != null) tvPhone.setText(String.format("📞 %s", phone));
        if (email != null) tvEmail.setText(String.format("✉ %s", email));

        // Phone call intent action
        if (tvPhone != null) {
            tvPhone.setOnClickListener(v -> {
                String cleanPhone = phone != null ? phone.replaceAll("[^0-9]", "") : "09171234567";
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + cleanPhone));
                startActivity(dialIntent);
            });
        }

        // Email intent action
        if (tvEmail != null) {
            tvEmail.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + (email != null ? email : "juan@gmail.com")));
                startActivity(emailIntent);
            });
        }

        // Booking History item clicks opening Booking Details
        if (cardHistory1 != null) {
            cardHistory1.setOnClickListener(v -> openBookingDetail("BK-2026-00123", name != null ? name : "Juan Dela Cruz", "PENDING", "₱ 1,024.00"));
        }

        if (cardHistory2 != null) {
            cardHistory2.setOnClickListener(v -> openBookingDetail("BK-2026-00110", name != null ? name : "Juan Dela Cruz", "COMPLETED", "₱ 3,150.00"));
        }
    }

    private void openBookingDetail(String id, String customer, String status, String amount) {
        Intent intent = new Intent(ClientDetailActivity.this, BookingDetailActivity.class);
        intent.putExtra("BOOKING_ID", id);
        intent.putExtra("CUSTOMER_NAME", customer);
        intent.putExtra("STATUS", status);
        intent.putExtra("TOTAL_AMOUNT", amount);
        startActivity(intent);
    }

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}