package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        MaterialButton btnCreateAccount = findViewById(R.id.btnCreateAccount);
        MaterialButton btnGoogle = findViewById(R.id.btnGoogleRegister);
        MaterialButton btnFacebook = findViewById(R.id.btnFacebookRegister);
        android.widget.TextView tvLoginLink = findViewById(R.id.tvLoginLink);

        tvLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // 1. Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                // .requestIdToken("YOUR_WEB_CLIENT_ID") // Put your web client id here for production backend authentication
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
                // Proceed to entry screen dashboard application layer
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

        btnCreateAccount.setOnClickListener(v -> {
            // Placeholder standard validation flow check logic
            Toast.makeText(this, "Standard account registration triggered", Toast.LENGTH_SHORT).show();
            navigateToDashboard();
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
        // Pass Facebook login activity action responses directly to Facebook SDK callback manager
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}