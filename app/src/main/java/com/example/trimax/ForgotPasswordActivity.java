package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etResetEmail;
    private MaterialButton btnSendResetLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        android.widget.ImageView btnBack = findViewById(R.id.btnBack);
        android.widget.TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        btnBack.setOnClickListener(v -> finish());

        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        btnSendResetLink.setOnClickListener(v -> {
            String email = etResetEmail.getText() != null ? etResetEmail.getText().toString().trim() : "";
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email or username", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Password reset link sent to " + email, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}