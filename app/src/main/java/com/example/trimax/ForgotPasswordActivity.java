package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
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

/**
 * "Set up or reset password": the person enters the email on their TRIMAX account, the one-time code the
 * TRIMAX desktop emailed them (when the account was created, or "Send new setup code") and a new password.
 * The server checks the code and sets the password of their mobile sign-in; then they sign in as usual.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    /** Extras for LoginActivity after a successful setup. */
    static final String EXTRA_EMAIL = "com.example.trimax.EMAIL";
    static final String EXTRA_NOTICE = "com.example.trimax.NOTICE";

    private TextInputEditText etResetEmail, etResetCode, etNewPassword, etConfirmPassword;
    private MaterialButton btnSetPassword;
    private String buttonLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        View root = findViewById(android.R.id.content);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        etResetEmail = findViewById(R.id.etResetEmail);
        etResetCode = findViewById(R.id.etResetCode);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSetPassword = findViewById(R.id.btnSendResetLink);
        ImageView btnBack = findViewById(R.id.btnBack);
        TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        String prefill = getIntent() != null ? getIntent().getStringExtra(EXTRA_EMAIL) : null;
        if (prefill != null && etResetEmail != null) etResetEmail.setText(prefill);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        if (tvLoginLink != null) tvLoginLink.setOnClickListener(v -> openLogin(null, null));
        if (btnSetPassword != null) {
            buttonLabel = btnSetPassword.getText().toString();
            btnSetPassword.setOnClickListener(v -> submit());
        }
    }

    private static String text(TextInputEditText field) {
        return field != null && field.getText() != null ? field.getText().toString() : "";
    }

    private void submit() {
        String email = text(etResetEmail).trim();
        String code = text(etResetCode);
        String password = text(etNewPassword);
        String confirm = text(etConfirmPassword);
        String problem = PasswordSetupForm.problem(email, code, password, confirm);
        if (problem != null) {
            Toast.makeText(this, problem, Toast.LENGTH_LONG).show();
            return;
        }
        btnSetPassword.setEnabled(false);
        btnSetPassword.setText("Setting password...");
        TrimaxApi.get(this).setPasswordWithCode(email, code, password, new TrimaxApi.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean ignored) {
                if (SessionGuard.isGone(ForgotPasswordActivity.this)) return;
                if (etNewPassword != null) etNewPassword.setText("");
                if (etConfirmPassword != null) etConfirmPassword.setText("");
                openLogin(email, "Password set. Sign in with your email and the new password.");
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.isGone(ForgotPasswordActivity.this)) return;
                btnSetPassword.setEnabled(true);
                btnSetPassword.setText(buttonLabel);
                Toast.makeText(ForgotPasswordActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openLogin(String email, String notice) {
        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (email != null) intent.putExtra(EXTRA_EMAIL, email);
        if (notice != null) intent.putExtra(EXTRA_NOTICE, notice);
        startActivity(intent);
        finish();
    }
}
