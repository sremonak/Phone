package com.aurora.phone;

import android.app.Application;

/** Application entry point. */
public class PhoneApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.init(this);
    }
}
