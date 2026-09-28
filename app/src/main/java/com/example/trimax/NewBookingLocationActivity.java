package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class NewBookingLocationActivity extends AppCompatActivity {

    private TextInputEditText etAddress;

    private String customerName, customerPhone, customerEmail;
    private String service, date, time, notes;
    private int quantity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_location);

        Intent intent = getIntent();
        customerName = intent.getStringExtra("CUSTOMER_NAME");
        customerPhone = intent.getStringExtra("CUSTOMER_PHONE");
        customerEmail = intent.getStringExtra("CUSTOMER_EMAIL");
        service = intent.getStringExtra("SERVICE");
        quantity = intent.getIntExtra("QUANTITY", 1);
        date = intent.getStringExtra("DATE");
        time = intent.getStringExtra("TIME");
        notes = intent.getStringExtra("NOTES");

        ImageView btnBack = findViewById(R.id.btnBack);
        etAddress = findViewById(R.id.etAddress);
        MaterialButton btnConfirmLocation = findViewById(R.id.btnConfirmLocation);

        btnBack.setOnClickListener(v -> finish());

        btnConfirmLocation.setOnClickListener(v -> {
            String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
            if (address.isEmpty()) {
                Toast.makeText(this, "Please enter a valid address", Toast.LENGTH_SHORT).show();
            } else {
                Intent summaryIntent = new Intent(NewBookingLocationActivity.this, NewBookingSummaryActivity.class);
                summaryIntent.putExtra("CUSTOMER_NAME", customerName);
                summaryIntent.putExtra("CUSTOMER_PHONE", customerPhone);
                summaryIntent.putExtra("CUSTOMER_EMAIL", customerEmail);
                summaryIntent.putExtra("SERVICE", service);
                summaryIntent.putExtra("QUANTITY", quantity);
                summaryIntent.putExtra("DATE", date);
                summaryIntent.putExtra("TIME", time);
                summaryIntent.putExtra("NOTES", notes);
                summaryIntent.putExtra("ADDRESS", address);
                startActivity(summaryIntent);
            }
        });
    }
}