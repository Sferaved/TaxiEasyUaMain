package com.taxi.easy.ua.utils.activ_push;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.NotificationHelper;
import com.taxi.easy.ua.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class PushAlarmReceiver extends BroadcastReceiver {

    private static final String PREFS_NAME = "UserActivityPrefs";
    private static final String LAST_ACTIVITY_KEY = "lastActivityTimestamp";
    private String TAG = "TAG_CHECK";

    @Override
    public void onReceive(Context context, Intent intent) {

//         Выполните проверку активности пользователя
        boolean isUserActive = checkUserActivity(context);
        Log.d(TAG, "onReceive: isUserActive " + isUserActive);

        if (!isUserActive) {
            // Если пользователь не активен, отправьте уведомление
            sendNotification(context);
        }
    }

    private boolean checkUserActivity(Context context) {
        // Получение состояния приложения (в переднем плане или фоне)
        boolean isAppInForeground = ((MyApplication) context.getApplicationContext()).isAppInForeground();
        Log.d(TAG, "checkUserActivity " + isAppInForeground);

        // Если приложение в переднем плане, считаем его активным
        if (isAppInForeground) {
            return true;
        }

        // Приложение в фоновом режиме, выполняем логику проверки времени активности пользователя
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        long lastActivityTimestamp = prefs.getLong(LAST_ACTIVITY_KEY, 0);
        long currentTime = System.currentTimeMillis();

        Log.d(TAG, "lastActivit: CHECK " + timeFormatter(lastActivityTimestamp));
        Log.d(TAG, "currentTime: CHECK " + timeFormatter(currentTime));
        // Проверка, прошло ли более 25 дней с последней активности
//        return (currentTime - lastActivityTimestamp) <= (60 * 1000);
        return (currentTime - lastActivityTimestamp) < (25 * 24 * 60 * 60 * 1000);
    }
    private String timeFormatter(long timeMsec) {
        Date formattedTime = new Date(timeMsec);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(formattedTime);
    }
    private void sendNotification(Context context) {
        // Ваш текст и заголовок уведомления
        String title = context.getString(R.string.new_message) + " " + context.getString(R.string.app_name);
        String message = context.getString(R.string.new_order_notify);

        // Создайте интент для открытия MainActivity при нажатии на уведомление
        Intent openMainActivityIntent = new Intent(context, MainActivity.class);
        openMainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // PendingIntent для открытия MainActivity
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openMainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Используйте ваш класс NotificationHelper для отправки уведомления
        NotificationHelper.showNotificationMessageOpen(context, title, message, pendingIntent);
        updateLastActivityTimestamp(context);
    }

    public static void scheduleAlarm(Context context) {
        Intent intent = new Intent(context, PushAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Установка будильника для выполнения каждую минуту
        long intervalMillis = 60 * 1000;
        long triggerMillis = SystemClock.elapsedRealtime() + intervalMillis;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerMillis, intervalMillis, pendingIntent);
    }

    public static void cancelAlarm(Context context) {
        Intent intent = new Intent(context, PushAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    private void updateLastActivityTimestamp(Context context) {

        // Обновление времени последней активности в SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Log.d(TAG, "updateLastActivityTimestamp: " + timeFormatter(System.currentTimeMillis()));
        editor.putLong(LAST_ACTIVITY_KEY, System.currentTimeMillis());
        editor.apply();
    }
}
