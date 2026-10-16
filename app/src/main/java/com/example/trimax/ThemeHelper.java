package com.example.trimax;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {

    public static final String PREF_THEME = "appTheme";
    public static final String THEME_SYSTEM = "SYSTEM";
    public static final String THEME_LIGHT = "LIGHT";
    public static final String THEME_DARK = "DARK";

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TrimaxSession", Context.MODE_PRIVATE);
        String theme = prefs.getString(PREF_THEME, THEME_SYSTEM);
        applyThemeMode(theme);
    }

    public static void setTheme(Context context, String themeMode) {
        SharedPreferences prefs = context.getSharedPreferences("TrimaxSession", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_THEME, themeMode).apply();
        applyThemeMode(themeMode);
    }

    public static String getSelectedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("TrimaxSession", Context.MODE_PRIVATE);
        return prefs.getString(PREF_THEME, THEME_SYSTEM);
    }

    private static void applyThemeMode(String themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
}
