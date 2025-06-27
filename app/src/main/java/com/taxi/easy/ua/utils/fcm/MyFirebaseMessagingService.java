package com.taxi.easy.ua.utils.fcm;

import static com.taxi.easy.ua.MainActivity.TABLE_USER_INFO;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.notify.NotificationHelper;
import com.taxi.easy.ua.utils.worker.SendTokenWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessagingService";
    private static final String PREFS_NAME = "UserTokenPrefs";
    private static final String TOKEN_KEY = "token";
    Constraints constraints;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Logger.d(this, TAG, "New token: " + token);
        updateUserTokenPrefs(token);
    }

    private void updateUserTokenPrefs(String token) {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(TOKEN_KEY, token);
        editor.apply();

        constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        String userEmail = logCursor(TABLE_USER_INFO).get(3);
        Logger.d(this, TAG, "newUser: " + userEmail);

        OneTimeWorkRequest sendTokenRequest = new OneTimeWorkRequest.Builder(SendTokenWorker.class)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putString("userEmail", userEmail)
                        .build())
                .build();

        // Запуск задач через WorkManager
        WorkManager.getInstance(this)
                .beginWith(sendTokenRequest)
                .enqueue();
    }
    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = MyApplication.getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = db.query(table, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            String str;
            do {
                str = "";
                for (String cn : c.getColumnNames()) {
                    str = str.concat(cn + " = " + c.getString(c.getColumnIndex(cn)) + "; ");
                    list.add(c.getString(c.getColumnIndex(cn)));

                }

            } while (c.moveToNext());
        }
        db.close();
        return list;
    }
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();

        if (!data.isEmpty()) {
            String locale = LocaleHelper.getLocale();
            Logger.d(this, TAG, "Locale: " + locale);

            // Пример: передаём разные тексты в зависимости от локали
            String message = data.get("message_" + locale);
            if (message == null) {
                message = data.get("message_uk"); // fallback
            }

            Logger.d(getApplicationContext(), TAG, "Message: " + message);
            notifyUser(message);
        }
    }

    private void notifyUser(String message) {
        Context context = getApplicationContext();
        String localeCode = LocaleHelper.getLocale();

        // Создание локализованного контекста
        Context localizedContext = getLocalizedContext(context, localeCode);

        // Показ уведомления
        NotificationHelper.showNotificationFindAutoMessage(localizedContext, message);
    }

    private Context getLocalizedContext(Context context, String localeCode) {
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

}
