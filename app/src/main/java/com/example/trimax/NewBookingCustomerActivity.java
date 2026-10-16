package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class NewBookingCustomerActivity extends AppCompatActivity {

    private TextInputEditText etCustomerName, etPhone, etEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_new_booking_customer);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        etCustomerName = findViewById(R.id.etCustomerName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        MaterialButton btnNext = findViewById(R.id.btnNext);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                String name = etCustomerName != null && etCustomerName.getText() != null ? etCustomerName.getText().toString().trim() : "";
                String phone = etPhone != null && etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
                String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

                if (name.isEmpty()) {
                    if (etCustomerName != null) {
                        etCustomerName.setError("Customer name is required");
                        etCustomerName.requestFocus();
                    }
                } else if (phone.isEmpty() || !Patterns.PHONE.matcher(phone).matches() || phone.length() < 7) {
                    if (etPhone != null) {
                        etPhone.setError("Valid phone number is required");
                        etPhone.requestFocus();
                    }
                } else if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (etEmail != null) {
                        etEmail.setError("Valid email address is required");
                        etEmail.requestFocus();
                    }
                } else {
                    btnNext.setEnabled(false);

                    Intent intent = new Intent(NewBookingCustomerActivity.this, NewBookingDetailsActivity.class);
                    intent.putExtra("CUSTOMER_NAME", name);
                    intent.putExtra("CUSTOMER_PHONE", phone);
                    intent.putExtra("CUSTOMER_EMAIL", email);
                    startActivity(intent);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MaterialButton btnNext = findViewById(R.id.btnNext);
        if (btnNext != null) {
            btnNext.setEnabled(true);
        }
    }
}
