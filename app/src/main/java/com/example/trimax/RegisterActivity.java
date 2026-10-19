package com.example.trimax;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Collections;

public class RegisterActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        dbHelper = new DatabaseHelper(this);

        android.view.View root = findViewById(android.R.id.content);
        if (root != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        MaterialButton btnCreateAccount = findViewById(R.id.btnCreateAccount);
        MaterialButton btnGoogle = findViewById(R.id.btnGoogleRegister);
        MaterialButton btnFacebook = findViewById(R.id.btnFacebookRegister);
        android.widget.TextView tvLoginLink = findViewById(R.id.tvLoginLink);
        android.widget.ImageView btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // 1. Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Define Launcher callback intent for Google Sign-In response handler
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    }
                }
        );

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        // 2. Configure Facebook Login
        mCallbackManager = CallbackManager.Factory.create();

        btnFacebook.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(
                    RegisterActivity.this,
                    mCallbackManager,
                    Collections.singletonList("email")
            );
        });

        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast.makeText(RegisterActivity.this, "Facebook Authentication Successful!", Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }

            @Override
            public void onCancel() {
                Toast.makeText(RegisterActivity.this, "Facebook Authentication Cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(RegisterActivity.this, "Facebook Authentication Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        com.google.android.material.textfield.TextInputEditText etFullName = findViewById(R.id.etFullName);
        com.google.android.material.textfield.TextInputEditText etEmail = findViewById(R.id.etEmail);
        com.google.android.material.textfield.TextInputEditText etPassword = findViewById(R.id.etPassword);
        com.google.android.material.textfield.TextInputEditText etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnCreateAccount.setOnClickListener(v -> {
            String fullName = etFullName != null && etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            String email = etEmail != null && etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword != null && etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            String confirmPassword = etConfirmPassword != null && etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

            if (fullName.isEmpty()) {
                if (etFullName != null) {
                    etFullName.setError("Full name is required");
                    etFullName.requestFocus();
                }
            } else if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (etEmail != null) {
                    etEmail.setError("Valid email address is required");
                    etEmail.requestFocus();
                }
            } else if (password.isEmpty() || password.length() < 6) {
                if (etPassword != null) {
                    etPassword.setError("Password must be at least 6 characters");
                    etPassword.requestFocus();
                }
            } else if (!password.equals(confirmPassword)) {
                if (etConfirmPassword != null) {
                    etConfirmPassword.setError("Passwords do not match");
                    etConfirmPassword.requestFocus();
                }
            } else {
                // Check if account already exists
                User existingUser = dbHelper.getUserByEmailOrUsername(email);
                if (existingUser != null) {
                    if (etEmail != null) {
                        etEmail.setError("An account with this email already exists");
                        etEmail.requestFocus();
                    }
                    Toast.makeText(this, "Registration failed: Email already registered.", Toast.LENGTH_LONG).show();
                    return;
                }

                btnCreateAccount.setEnabled(false);

                // Register User into SQLite Database with secure hashed password and AGENT role
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put("username", email);
                cv.put("password", DatabaseHelper.hashPassword(password));
                cv.put("role", "AGENT");
                cv.put("full_name", fullName);
                cv.put("email", email);
                cv.put("phone", "09123456789");
                long rowId = db.insertWithOnConflict(DatabaseHelper.TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_ABORT);

                if (rowId != -1) {
                    SessionManager session = new SessionManager(this);
                    User registeredUser = dbHelper.authenticateUser(email, password);
                    if (registeredUser != null) {
                        session.createLoginSession(registeredUser);
                    }

                    Toast.makeText(this, "Account registered successfully!", Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                } else {
                    btnCreateAccount.setEnabled(true);
                    Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String email = account.getEmail();
                Toast.makeText(this, "Google Sign-In Successful! " + email, Toast.LENGTH_SHORT).show();
                navigateToDashboard();
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Google Sign-In Failed code: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(RegisterActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
