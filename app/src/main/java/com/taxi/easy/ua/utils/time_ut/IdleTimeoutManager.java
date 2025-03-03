package com.taxi.easy.ua.utils.time_ut;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.taxi.easy.ua.utils.clear.ClearTaskActivity;

public class IdleTimeoutManager {
    private static final long TIMEOUT_DURATION = 30 * 60 * 1000; // 30 минут бездействия
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Activity activity;
    private final Runnable timeoutRunnable;

    public IdleTimeoutManager(Activity activity) {
        this.activity = activity;
        timeoutRunnable = () -> {
            try {
                // Закрываем все активности в текущем стеке
                activity.finishAffinity();

                // Очищаем стек задач через запуск "пустой" активности с флагом CLEAR_TASK
                Intent intent = new Intent(activity, ClearTaskActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                activity.startActivity(intent);

                // Немедленно завершаем текущую активность
                activity.finish();

                // Даём системе небольшую задержку для обработки очистки стека
                handler.postDelayed(() -> {
                    // Принудительно завершаем процесс
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(0);
                }, 100); // Задержка 100 мс для завершения очистки
            } catch (Exception e) {
                e.printStackTrace();
                // В случае ошибки принудительно завершаем процесс
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }
        };
    }

    public void resetTimer() {
        handler.removeCallbacks(timeoutRunnable);
        handler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);
    }
}