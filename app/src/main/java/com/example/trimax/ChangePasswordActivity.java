package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Change Password (from Settings), and the same screen right after the first sign-in with a temporary
 * password from the administrator ({@link #EXTRA_FIRST_SIGN_IN}): the person must choose their own
 * password before the app opens; leaving signs them out.
 */
public class ChangePasswordActivity extends AppCompatActivity {

    static final String EXTRA_FIRST_SIGN_IN = "com.example.trimax.FIRST_SIGN_IN";

    private SessionManager session;
    private DatabaseHelper dbHelper;
    private boolean firstSignIn;
    private String firstSignInEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        session = new SessionManager(this);
        dbHelper = new DatabaseHelper(this);

        View root = findViewById(R.id.topBar) != null ? (View) findViewById(R.id.topBar).getParent() : findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        MaterialButton btnChangePassword = findViewById(R.id.btnChangePassword);
        TextInputEditText etCurrentPassword = findViewById(R.id.etCurrentPassword);
        TextInputEditText etNewPassword = findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);

        firstSignIn = getIntent() != null && getIntent().getBooleanExtra(EXTRA_FIRST_SIGN_IN, false);
        firstSignInEmail = getIntent() != null ? getIntent().getStringExtra(ForgotPasswordActivity.EXTRA_EMAIL) : null;
        if (firstSignIn) {
            TextView screenTitle = findViewById(R.id.tvScreenTitle);
            TextView securityTitle = findViewById(R.id.tvSecurityTitle);
            TextInputLayout currentLayout = findViewById(R.id.tilCurrentPassword);
            if (screenTitle != null) screenTitle.setText("Choose your password");
            if (securityTitle != null) securityTitle.setText("Choose your own password");
            if (currentLayout != null) currentLayout.setHint("Temporary password (from the email)");
            // Back (button or gesture) leaves without a new password: sign out.
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    openLogin(firstSignInEmail, "Choose your own password to use the app.");
                }
            });
        }

        if (btnBack != null) btnBack.setOnClickListener(v -> navigateBack());

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                // Passwords are used exactly as typed (spaces count), matching the sign-in screen.
                String current = etCurrentPassword != null && etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString() : "";
                String newPwd = etNewPassword != null && etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
                String confirmPwd = etConfirmNewPassword != null && etConfirmNewPassword.getText() != null ? etConfirmNewPassword.getText().toString() : "";

                String firstProblem = firstSignIn ? PasswordSetupForm.newPasswordProblem(newPwd, confirmPwd) : null;
                if (firstProblem == null && firstSignIn && newPwd.equals(current)) {
                    firstProblem = "Choose a password different from the temporary one.";
                }
                if (current.isEmpty()) {
                    if (etCurrentPassword != null) {
                        etCurrentPassword.setError(firstSignIn ? "Temporary password required" : "Current password required");
                        etCurrentPassword.requestFocus();
                    }
                } else if (firstProblem != null) {
                    if (etNewPassword != null) {
                        etNewPassword.setError(firstProblem);
                        etNewPassword.requestFocus();
                    }
                } else if (newPwd.isEmpty() || newPwd.length() < 6) {
                    if (etNewPassword != null) {
                        etNewPassword.setError("New password must be at least 6 characters");
                        etNewPassword.requestFocus();
                    }
                } else if (!newPwd.equals(confirmPwd)) {
                    if (etConfirmNewPassword != null) {
                        etConfirmNewPassword.setError("Passwords do not match");
                        etConfirmNewPassword.requestFocus();
                    }
                } else {
                    // The password belongs to the TRIMAX sign-in on the server, not to this phone.
                    btnChangePassword.setEnabled(false);
                    String email = firstSignIn ? firstSignInEmail : session.getEmail();
                    TrimaxApi.get(this).changePassword(email, current, newPwd, new TrimaxApi.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean ok) {
                            if (SessionGuard.isGone(ChangePasswordActivity.this)) return;
                            Toast.makeText(ChangePasswordActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                            if (firstSignIn) {
                                openApp(email);
                                return;
                            }
                            finish();
                        }

                        @Override
                        public void onError(ApiException error) {
                            if (SessionGuard.isGone(ChangePasswordActivity.this)) return;
                            btnChangePassword.setEnabled(true);
                            if (SessionGuard.handle(ChangePasswordActivity.this, error)) return;
                            if (error.getKind() == ApiException.Kind.BAD_CREDENTIALS && etCurrentPassword != null) {
                                etCurrentPassword.setError("Incorrect current password");
                                etCurrentPassword.requestFocus();
                            }
                            Toast.makeText(ChangePasswordActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    }

    /** After the first password change: save the session and open the dashboard for this role. */
    private void openApp(String email) {
        TrimaxApi.get(this).loadMyAccount(new TrimaxApi.Callback<MobileAccount>() {
            @Override
            public void onSuccess(MobileAccount account) {
                if (SessionGuard.isGone(ChangePasswordActivity.this)) return;
                session.createLoginSession(account.toUser());
                Intent intent = new Intent(ChangePasswordActivity.this,
                        account.isDriver() ? DriverDashboardActivity.class : DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(ChangePasswordActivity.this)) return;
                // The new password is saved; signing in again with it opens the app.
                openLogin(email, "Password changed. Sign in with your new password.");
            }
        });
    }

    /** Leaving the first-sign-in screen without a new password: signed out, back to sign-in. */
    private void openLogin(String email, String notice) {
        TrimaxApi.get(this).signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (email != null) intent.putExtra(ForgotPasswordActivity.EXTRA_EMAIL, email);
        if (notice != null) intent.putExtra(ForgotPasswordActivity.EXTRA_NOTICE, notice);
        startActivity(intent);
        finish();
    }

    private void navigateBack() {
        if (firstSignIn) {
            openLogin(firstSignInEmail, "Choose your own password to use the app.");
            return;
        }
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
