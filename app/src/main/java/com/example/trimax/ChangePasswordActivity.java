package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
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

public class ChangePasswordActivity extends AppCompatActivity {

    private SessionManager session;
    private DatabaseHelper dbHelper;

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

        if (btnBack != null) btnBack.setOnClickListener(v -> navigateBack());

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> {
                // Passwords are used exactly as typed (spaces count), matching the sign-in screen.
                String current = etCurrentPassword != null && etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString() : "";
                String newPwd = etNewPassword != null && etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
                String confirmPwd = etConfirmNewPassword != null && etConfirmNewPassword.getText() != null ? etConfirmNewPassword.getText().toString() : "";

                if (current.isEmpty()) {
                    if (etCurrentPassword != null) {
                        etCurrentPassword.setError("Current password required");
                        etCurrentPassword.requestFocus();
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
                    TrimaxApi.get(this).changePassword(session.getEmail(), current, newPwd, new TrimaxApi.Callback<Boolean>() {
                        @Override
                        public void onSuccess(Boolean ok) {
                            if (SessionGuard.isGone(ChangePasswordActivity.this)) return;
                            Toast.makeText(ChangePasswordActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
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

    private void navigateBack() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }
}
