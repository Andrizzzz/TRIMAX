package com.example.trimax;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

/** Shared handling for server errors that end the session. */
public final class SessionGuard {

    private SessionGuard() {
    }

    /** True when the activity is gone and a late server answer must be ignored. */
    public static boolean isGone(Activity activity) {
        return activity == null || activity.isFinishing() || activity.isDestroyed();
    }

    /**
     * If the error means the user must sign in again (expired, revoked, unlinked or disabled account),
     * signs out, tells the user why, opens the sign-in screen and returns true.
     */
    public static boolean handle(Activity activity, ApiException error) {
        if (isGone(activity) || error == null || !error.requiresSignIn()) return false;
        new SessionManager(activity).logoutUser();
        Toast.makeText(activity, error.getMessage(), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
        return true;
    }
}
