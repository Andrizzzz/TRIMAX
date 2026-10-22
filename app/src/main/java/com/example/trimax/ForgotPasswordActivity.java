package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etResetEmail;
    private MaterialButton btnSendResetLink;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        dbHelper = new DatabaseHelper(this);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> {
                Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }

        if (btnSendResetLink != null) {
            btnSendResetLink.setOnClickListener(v -> {
                String email = etResetEmail != null && etResetEmail.getText() != null ? etResetEmail.getText().toString().trim() : "";
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    if (etResetEmail != null) {
                        etResetEmail.setError("Valid email address is required");
                        etResetEmail.requestFocus();
                    }
                    Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                } else {
                    // This mobile build has no verified password-reset backend or email sender.
                    // Do not claim an email was sent or reveal whether an account exists.
                    Toast.makeText(this,
                            "Password reset is not available yet. Please contact your TRIMAX administrator.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
