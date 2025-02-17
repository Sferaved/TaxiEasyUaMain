package com.taxi.easy.ua.utils.time_ut;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

public class IdleTimeoutManager {
    private static final long TIMEOUT_DURATION = 30 * 60 * 1000; // 30 минут бездействия
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable;

    public IdleTimeoutManager(Activity activity) {
        timeoutRunnable = () -> {
            // Завершаем приложение при простое
            activity.finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        };
    }

    public void resetTimer() {
        handler.removeCallbacks(timeoutRunnable);
        handler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);
    }
}

