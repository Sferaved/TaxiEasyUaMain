package com.taxi.easy.ua.utils.activ_push;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class MyApplication extends Application {

    private boolean isAppInForeground = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // Регистрация слушателя жизненного цикла активности
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
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
