package com.taxi.easy.ua.androidx.startup;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.anrwatchdog.ANRWatchDog;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MyApplication extends Application {

    private boolean isAppInForeground = false;
    private final String TAG = "MyApplication";
    private static final String LOG_FILE_NAME = "app_log.txt";
    private static MyApplication instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Установка глобального обработчика исключений
        Thread.setDefaultUncaughtExceptionHandler(new MyUncaughtExceptionHandler(this));

        initializeFirebaseAndCrashlytics();
        setupANRWatchDog();
        registerActivityLifecycleCallbacks();
    }
    public static Context getContext() {
        return instance.getApplicationContext();
    }


    private void initializeFirebaseAndCrashlytics() {
        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Set up Firebase Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
    }

    private void setupANRWatchDog() {
        // Set default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler());

        // Configure ANRWatchDog for ANR detection
        new ANRWatchDog().setANRListener(error -> {
            // Use Handler to show Toast on the main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getApplicationContext(), R.string.anr_message, Toast.LENGTH_LONG).show();
            });
            // Log the error
            Log.d(TAG, "ANR occurred: " + error.toString());

            // Log the ANR event to Firebase Crashlytics
            FirebaseCrashlytics.getInstance().recordException(error);
        }).start();
    }

    private void registerActivityLifecycleCallbacks() {
        // Register ActivityLifecycleCallbacks to track foreground/background state
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // App is in foreground
                isAppInForeground = true;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // App went to background
                isAppInForeground = false;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    /**
     * Method to check if the application is currently in the foreground.
     *
     * @return true if the application is in the foreground; false otherwise.
     */
    public boolean isAppInForeground() {
        return isAppInForeground;
    }

    /**
     * Handler for uncaught exceptions in the application.
     */
    private static class MyExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
            // Handle uncaught exceptions here
            Log.e("MyExceptionHandler", "Uncaught Exception occurred: " + throwable.getMessage(), throwable);

            // Log the exception to Firebase Crashlytics
            FirebaseCrashlytics.getInstance().recordException(throwable);

            // Optionally, restart the application or perform other cleanup actions
            // Note: Restarting the application from here is not recommended in production
        }
    }

    private class MyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        public MyUncaughtExceptionHandler(MyApplication myApplication) {
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            writeLog(Log.getStackTraceString(e));
            System.exit(1); // Перезапуск приложения или завершение работы
        }
    }

    public void writeLog(String log) {
        if (isExternalStorageWritable()) {
            File logFile = new File(getExternalFilesDir(null), LOG_FILE_NAME);
            try {
                FileOutputStream fos = new FileOutputStream(logFile, true);
                OutputStreamWriter osw = new OutputStreamWriter(fos);

                // Установка украинского времени
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));

                osw.write(sdf.format(new Date()) + " - " + log);
                osw.write("\n");
                osw.close();
                fos.close();
                Log.d(TAG, "Log written to " + logFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e("MyAppLogger", "Failed to write log", e);
            }
        } else {
            Log.e("MyAppLogger", "External storage is not writable");
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

}
