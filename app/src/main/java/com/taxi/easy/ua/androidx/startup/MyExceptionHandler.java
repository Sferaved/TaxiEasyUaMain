package com.taxi.easy.ua.androidx.startup;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class MyExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler defaultUEH;

    public MyExceptionHandler() {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // Log the exception
        Log.e("ExceptionHandler", "Uncaught exception: ", throwable);

        // Record the exception with Firebase Crashlytics
        FirebaseCrashlytics.getInstance().recordException(throwable);

        // Call the default uncaught exception handler
        defaultUEH.uncaughtException(thread, throwable);
    }
}

