package com.taxi.easy.ua.utils.activ_push;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.notify.NotificationHelper;
import com.taxi.easy.ua.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MyPeriodicWorker extends Worker {

    private String TAG = "TAG_Per";
    private static final String PREFS_NAME = "UserActivityPrefs";
    private static final String LAST_ACTIVITY_KEY = "lastActivityTimestamp";

    public MyPeriodicWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Выполнить необходимую работу здесь
        // Например, отправить уведомление или выполнить другое задание
        Context context = getApplicationContext();
        boolean isUserActive = checkUserActivity(context);
        Log.d(TAG, "onReceive: isUserActive " + isUserActive);

        if (!isUserActive) {

            // Если пользователь не активен, отправьте уведомление
            sendNotification(context);
        }
        return Result.success(); // Возвращаем Result.success(), если работа выполнена успешно
    }

    public static void schedulePeriodicWork() {
        // Создаем периодическую работу с интервалом 24 часа
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
                MyPeriodicWorker.class,
                24, // интервал
                TimeUnit.HOURS
        ).build();

        // Запускаем периодическую работу
        WorkManager.getInstance().enqueue(periodicWorkRequest);
    }

    private boolean checkUserActivity(Context context) {
        // Получение состояния приложения (в переднем плане или фоне)
        boolean isAppInForeground = ((MyApplication) context.getApplicationContext()).isAppInForeground();
        Log.d(TAG, "checkUserActivity " + isAppInForeground);

        long lastActivityTimestamp = getLastActivityTimestamp(context);
        long currentTime = System.currentTimeMillis();

        Log.d(TAG, "lastActivity: " + timeFormatter(lastActivityTimestamp));
        Log.d(TAG, "currentTime: " + timeFormatter(currentTime));

        // Если приложение в переднем плане, считаем его активным
        if (isAppInForeground) {
            return true;
        }

        // Проверка, прошло ли менее 60 секунд с последней активности
        boolean isActive = (currentTime - lastActivityTimestamp) <= (25 *  24 * 60 * 60 * 1000);
        Log.d(TAG, "checkUserActivity: " + isActive);
        return isActive;
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
        insertOrUpdatePushDate(context);
    }
    private void updateLastActivityTimestamp(Context context) {

        // Обновление времени последней активности в SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Log.d(TAG, "updateLastActivityTimestamp: " + timeFormatter(System.currentTimeMillis()));
        editor.putLong(LAST_ACTIVITY_KEY, System.currentTimeMillis());
        editor.apply();
    }

    public void insertOrUpdatePushDate(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        if (database != null) {
            try {
                // Получаем текущее время и дату
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentDateandTime = sdf.format(new Date());
                Log.d(TAG, "Current date and time: " + currentDateandTime);

                // Создаем объект ContentValues для передачи данных в базу данных
                ContentValues values = new ContentValues();
                values.put("push_date", currentDateandTime);

                // Пытаемся вставить новую запись. Если запись уже существует, выполняется обновление.
                int rowsAffected = database.update(MainActivity.TABLE_LAST_PUSH, values, "ROWID=1", null);
                if (rowsAffected > 0) {
                    Log.d(TAG, "Update successful");
                } else {
                    Log.d(TAG, "Error updating");
                }


            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                database.close();
            }
        }
    }


    @SuppressLint("Range")
    public long getLastActivityTimestamp(Context context) {
        long lastActivityTimestamp = 0;
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        // Выполняем запрос к таблице для получения времени последней активности
        Cursor cursor = database.rawQuery("SELECT push_date FROM " + MainActivity.TABLE_LAST_PUSH, null);

        // Проверяем, есть ли результаты запроса
        if (cursor != null && cursor.moveToFirst()) {
            // Получаем значение времени последней активности из результата запроса
            String dateString = cursor.getString(cursor.getColumnIndex("push_date"));
            try {
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = sdf.parse(dateString);
                assert date != null;
                lastActivityTimestamp = date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
            cursor.close();
        }
        Log.d(TAG, "getLastActivityTimestamp: " + lastActivityTimestamp);
        database.close();
        return lastActivityTimestamp;
    }

}
