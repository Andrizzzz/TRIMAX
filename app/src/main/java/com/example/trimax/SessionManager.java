package com.example.trimax;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "TrimaxSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userID";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_ROLE = "role";
    private static final String KEY_IS_DUTY_ONLINE = "isDutyOnline";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(User user) {
        String officialRole = user != null && user.getRole() != null ? user.getRole().toUpperCase() : "AGENT";
        if ("RIDER".equalsIgnoreCase(officialRole)) {
            officialRole = "AGENT"; // Standardize legacy Rider role to official Agent role
        }

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, user != null ? user.getId() : 0);
        editor.putString(KEY_USERNAME, user != null ? user.getUsername() : "");
        editor.putString(KEY_FULL_NAME, user != null ? user.getFullName() : "Agent");
        editor.putString(KEY_EMAIL, user != null ? user.getEmail() : "");
        editor.putString(KEY_PHONE, user != null ? user.getPhone() : "");
        editor.putString(KEY_ROLE, officialRole);
        editor.putBoolean(KEY_IS_DUTY_ONLINE, false);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public int getUserId() {
        return pref.getInt(KEY_USER_ID, 0);
    }

    public String getUsername() {
        return pref.getString(KEY_USERNAME, "");
    }

    public String getFullName() {
        return pref.getString(KEY_FULL_NAME, "Agent");
    }

    public String getEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getPhone() {
        return pref.getString(KEY_PHONE, "");
    }

    public String getRole() {
        String role = pref.getString(KEY_ROLE, "AGENT");
        if ("RIDER".equalsIgnoreCase(role)) {
            role = "AGENT";
            editor.putString(KEY_ROLE, "AGENT");
            editor.apply();
        }
        return role;
    }

    public void updateProfile(String fullName, String email, String phone) {
        if (fullName != null && !fullName.trim().isEmpty()) {
            editor.putString(KEY_FULL_NAME, fullName.trim());
        }
        if (email != null && !email.trim().isEmpty()) {
            editor.putString(KEY_EMAIL, email.trim());
        }
        if (phone != null) {
            editor.putString(KEY_PHONE, phone.trim());
        }
        editor.apply();
    }

    public boolean isDutyOnline() {
        return pref.getBoolean(KEY_IS_DUTY_ONLINE, false);
    }

    public void setDutyOnline(boolean isOnline) {
        editor.putBoolean(KEY_IS_DUTY_ONLINE, isOnline);
        editor.apply();
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();
    }
}
