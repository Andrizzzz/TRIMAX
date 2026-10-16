package com.example.trimax;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        SessionManager session = new SessionManager(this);

        // Delay 2 seconds then navigate based on official authenticated session status
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (session.isLoggedIn()) {
                if ("DRIVER".equalsIgnoreCase(session.getRole())) {
                    intent = new Intent(SplashActivity.this, DriverDashboardActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, DashboardActivity.class);
                }
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 2000);
    }
}
