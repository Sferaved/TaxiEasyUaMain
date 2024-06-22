package com.taxi.easy.ua.androidx.startup;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.anrwatchdog.ANRWatchDog;
import com.google.firebase.FirebaseApp;
import com.taxi.easy.ua.R;


public class MyApplication extends Application {

    private boolean isAppInForeground = false;
    private final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        new ANRWatchDog().setANRListener(error -> {
            // Используем Handler, чтобы показать Toast на главном потоке
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), R.string.anr_message, Toast.LENGTH_LONG).show();
                }
            });
            // Логирование ошибки
            Log.d(TAG, "onCreate: " + error.toString());
        }).start();
        // Регистрация слушателя жизненного цикла активности
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                // Приложение активно в переднем плане
                isAppInForeground = true;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // Приложение ушло в фоновый режим
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

    public boolean isAppInForeground() {
        return isAppInForeground;
    }
}
