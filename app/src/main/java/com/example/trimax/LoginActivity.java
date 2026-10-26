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
    private CharSequence loginLabel;
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
        loginLabel = btnLogin != null ? btnLogin.getText() : "Login";
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                String typed = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                if (typed.contains("@")) intent.putExtra(ForgotPasswordActivity.EXTRA_EMAIL, typed);
                startActivity(intent);
            });
        }
        showSetupResult(getIntent());

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String usernameInput = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
                String passwordInput = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString() : "";

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

                // Sign-in is checked by the TRIMAX server (Supabase Auth); the role and Active status come
                // from the account the administrator manages on the desktop, never from this phone.
                if (!usernameInput.contains("@")) {
                    if (etEmail != null) {
                        etEmail.setError("Sign in with the email address on your TRIMAX account");
                        etEmail.requestFocus();
                    }
                    return;
                }

                btnLogin.setEnabled(false);
                btnLogin.setText("Signing in...");
                TrimaxApi.get(this).signIn(usernameInput, passwordInput, new TrimaxApi.Callback<MobileAccount>() {
                    @Override
                    public void onSuccess(MobileAccount account) {
                        if (SessionGuard.isGone(LoginActivity.this)) return;
                        if (account.mustChangePassword) {
                            // Temporary password from the administrator: choose your own before anything else.
                            Toast.makeText(LoginActivity.this, "Welcome " + account.fullName
                                    + "! Choose your own password first.", Toast.LENGTH_LONG).show();
                            Intent first = new Intent(LoginActivity.this, ChangePasswordActivity.class);
                            first.putExtra(ChangePasswordActivity.EXTRA_FIRST_SIGN_IN, true);
                            first.putExtra(ForgotPasswordActivity.EXTRA_EMAIL, usernameInput);
                            startActivity(first);
                            finish();
                            return;
                        }
                        session.createLoginSession(account.toUser());
                        Toast.makeText(LoginActivity.this, "Welcome " + account.fullName + "!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this,
                                account.isDriver() ? DriverDashboardActivity.class : DashboardActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(ApiException error) {
                        if (SessionGuard.isGone(LoginActivity.this)) return;
                        btnLogin.setEnabled(true);
                        btnLogin.setText(loginLabel);
                        Toast.makeText(LoginActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showSetupResult(intent);
    }

    /** After "Set up or reset password": the email is filled in and the person is told to sign in. */
    private void showSetupResult(Intent intent) {
        if (intent == null) return;
        String email = intent.getStringExtra(ForgotPasswordActivity.EXTRA_EMAIL);
        String notice = intent.getStringExtra(ForgotPasswordActivity.EXTRA_NOTICE);
        if (email != null && etEmail != null) etEmail.setText(email);
        if (notice != null) {
            if (etPassword != null) {
                etPassword.setText("");
                etPassword.requestFocus();
            }
            Toast.makeText(this, notice, Toast.LENGTH_LONG).show();
            intent.removeExtra(ForgotPasswordActivity.EXTRA_NOTICE);   // not again after rotation
        }
    }
}
