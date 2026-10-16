package com.example.trimax;

import android.app.Application;

public class TrimaxApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.applyTheme(this);
    }
}
