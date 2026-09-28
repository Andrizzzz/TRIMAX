package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class NewBookingCustomerActivity extends AppCompatActivity {

    private TextInputEditText etCustomerName, etPhone, etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_customer);

        ImageView btnBack = findViewById(R.id.btnBack);
        etCustomerName = findViewById(R.id.etCustomerName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        MaterialButton btnNext = findViewById(R.id.btnNext);

        btnBack.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            String name = etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please enter customer name and phone number", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(NewBookingCustomerActivity.this, NewBookingDetailsActivity.class);
                intent.putExtra("CUSTOMER_NAME", name);
                intent.putExtra("CUSTOMER_PHONE", phone);
                intent.putExtra("CUSTOMER_EMAIL", email);
                startActivity(intent);
            }
        });
    }
}