package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private DatabaseHelper dbHelper;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);
        session = new SessionManager(this);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String usernameInput = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String passwordInput = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

                if (usernameInput.isEmpty()) {
                    if (etEmail != null) {
                        etEmail.setError("Username or Email is required");
                        etEmail.requestFocus();
                    }
                    return;
                }
                if (passwordInput.isEmpty()) {
                    if (etPassword != null) {
                        etPassword.setError("Password is required");
                        etPassword.requestFocus();
                    }
                    return;
                }

                btnLogin.setEnabled(false);

                // Authenticate with local SQLite database
                User authenticatedUser = dbHelper.authenticateUser(usernameInput, passwordInput);

                if (authenticatedUser != null) {
                    session.createLoginSession(authenticatedUser);

                    Toast.makeText(LoginActivity.this, "Welcome " + authenticatedUser.getFullName() + "!", Toast.LENGTH_SHORT).show();

                    Intent intent;
                    if ("DRIVER".equalsIgnoreCase(authenticatedUser.getRole())) {
                        intent = new Intent(LoginActivity.this, DriverDashboardActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    }
                    startActivity(intent);
                    finish();
                } else {
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Invalid username or password.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
