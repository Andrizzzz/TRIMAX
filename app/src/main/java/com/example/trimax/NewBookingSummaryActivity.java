package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class NewBookingSummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_summary);

        Intent intent = getIntent();
        String customerName = intent.getStringExtra("CUSTOMER_NAME");
        String customerPhone = intent.getStringExtra("CUSTOMER_PHONE");
        String service = intent.getStringExtra("SERVICE");
        int quantity = intent.getIntExtra("QUANTITY", 1);
        String date = intent.getStringExtra("DATE");
        String time = intent.getStringExtra("TIME");
        String address = intent.getStringExtra("ADDRESS");

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvCustomerName = findViewById(R.id.tvCustomerName);
        TextView tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        TextView tvService = findViewById(R.id.tvService);
        TextView tvQuantity = findViewById(R.id.tvQuantity);
        TextView tvDateTime = findViewById(R.id.tvDateTime);
        TextView tvAddress = findViewById(R.id.tvAddress);
        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);

        MaterialButton btnEdit = findViewById(R.id.btnEdit);
        MaterialButton btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        btnBack.setOnClickListener(v -> finish());
        btnEdit.setOnClickListener(v -> finish());

        if (customerName != null) tvCustomerName.setText(customerName);
        if (customerPhone != null) tvCustomerPhone.setText(customerPhone);
        if (service != null) tvService.setText("Service: " + service);
        tvQuantity.setText("Quantity: " + quantity);
        tvDateTime.setText("Date & Time: " + (date != null ? date : "July 19, 2026") + " • " + (time != null ? time : "10:00 AM"));
        if (address != null) tvAddress.setText(address);

        // Simulated total price calculation
        double pricePerUnit = 512.00;
        double total = pricePerUnit * quantity;
        tvTotalAmount.setText(String.format("₱ %,.2f", total));

        btnConfirmBooking.setOnClickListener(v -> {
            Intent confirmedIntent = new Intent(NewBookingSummaryActivity.this, BookingConfirmedActivity.class);
            confirmedIntent.putExtra("BOOKING_ID", "BK-2026-00123");
            startActivity(confirmedIntent);
            finishAffinity(); // Close creation flow stack
        });
    }
}