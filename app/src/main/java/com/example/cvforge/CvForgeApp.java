package com.example.cvforge;

import android.app.Application;

import com.google.firebase.FirebaseApp;

/** Application class. Starts Firebase once when the app process starts. */
public class CvForgeApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
