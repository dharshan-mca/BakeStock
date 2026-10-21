package com.example;

import android.app.Application;
import android.util.Log;

public class BakeryApplication extends Application {

    private static final String TAG = "BakeryApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Bakery Stock Application initialized successfully.");
    }
}
