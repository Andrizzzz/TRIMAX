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
        Handler handler = new Handler(Looper.getMainLooper());

        if (!session.isLoggedIn()) {
            handler.postDelayed(() -> open(LoginActivity.class), 2000);
            return;
        }

        // Re-read the account from the server: the role and Active status are managed on the desktop.
        // If the server cannot be reached quickly, continue with the role saved at sign-in; every server
        // call is checked again anyway.
        TrimaxApi.get(this).loadMyAccount(new TrimaxApi.Callback<MobileAccount>() {
            @Override
            public void onSuccess(MobileAccount account) {
                session.createLoginSession(account.toUser());
                accountChecked = true;
                routeWhenReady(session);
            }

            @Override
            public void onError(ApiException error) {
                if (SessionGuard.handle(SplashActivity.this, error)) {
                    routed = true;
                    return;
                }
                accountChecked = true;
                routeWhenReady(session);
            }
        });
        handler.postDelayed(() -> {
            minimumShown = true;
            routeWhenReady(session);
        }, 2000);
        handler.postDelayed(() -> {
            accountChecked = true;      // give up waiting for a slow network
            routeWhenReady(session);
        }, 6000);
    }

    private boolean minimumShown, accountChecked, routed;

    private void routeWhenReady(SessionManager session) {
        if (routed || !minimumShown || !accountChecked || SessionGuard.isGone(this)) return;
        routed = true;
        open("DRIVER".equalsIgnoreCase(session.getRole()) ? DriverDashboardActivity.class : DashboardActivity.class);
    }

    private void open(Class<?> target) {
        if (SessionGuard.isGone(this)) return;
        startActivity(new Intent(SplashActivity.this, target));
        finish();
    }
}
