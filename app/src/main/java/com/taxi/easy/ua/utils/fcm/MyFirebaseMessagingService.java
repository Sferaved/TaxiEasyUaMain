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

import org.json.JSONObject;

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
//    @Override
//    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
//        super.onMessageReceived(remoteMessage);
//        Map<String, String> data = remoteMessage.getData();
//        Logger.d(this, TAG, "Получено сообщение: " + data);
//        if (!data.isEmpty()) {
//            String locale = LocaleHelper.getLocale();
//            Logger.d(this, TAG, "Locale: " + locale);
//            String message = data.get("message_" + locale);
//            if (message == null) {
//                message = data.get("message_uk");
//                Logger.d(this, TAG, "Fallback to message_uk: " + message);
//            }
//            String uid = data.get("uid");
//            if (message == null || message.isEmpty()) {
//                message = "Найдено авто (по умолчанию)";
//                Logger.d(this, TAG, "Сообщение пустое, установлено значение по умолчанию");
//            }
//            Logger.d(getApplicationContext(), TAG, "Message: " + message);
//            Logger.d(getApplicationContext(), TAG, "uid: " + uid);
//            notifyUser(message, uid);
//        } else {
//            Logger.d(this, TAG, "Данные пуш-уведомления пусты");
//        }
//    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        Logger.d(this, TAG, "Получено сообщение: " + data);

        // Check if the message contains order cost data
        if (data.containsKey("order_cost")) {
            handleOrderCostMessage(data);
        } else if (!data.isEmpty()) {
            String locale = LocaleHelper.getLocale();
            Logger.d(this, TAG, "Locale: " + locale);
            String message = data.get("message_" + locale);
            if (message == null) {
                message = data.get("message_uk");
                Logger.d(this, TAG, "Fallback to message_uk: " + message);
            }
            String uid = data.get("uid");
            if (message == null || message.isEmpty()) {
                message = "Найдено авто (по умолчанию)";
                Logger.d(this, TAG, "Сообщение пустое, установлено значение по умолчанию");
            }
            Logger.d(getApplicationContext(), TAG, "Message: " + message);
            Logger.d(getApplicationContext(), TAG, "uid: " + uid);
            notifyUser(message, uid);
        } else {
            Logger.d(this, TAG, "Данные пуш-уведомления пусты");
        }
    }

    private void notifyUser(String message,String uid) {
        Context context = getApplicationContext();
        String localeCode = LocaleHelper.getLocale();

        // Создание локализованного контекста
        Context localizedContext = getLocalizedContext(context, localeCode);

        // Показ уведомления
        NotificationHelper.showNotificationFindAutoMessage(localizedContext, message, uid);
    }

    private Context getLocalizedContext(Context context, String localeCode) {
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
    private void handleOrderCostMessage(Map<String, String> data) {
        Context context = getApplicationContext();
        Logger.d(context, TAG, "Received order cost message: " + data.toString());

        JSONObject eventData = new JSONObject(data);
        String orderCost = eventData.optString("order_cost", "0");
        Logger.d(context, TAG, "order_cost: " + orderCost);

        // Проверяем, инициализирован ли OrderViewModel
        if (MainActivity.orderViewModel != null) {
            MainActivity.orderViewModel.setOrderCost(orderCost);
            Logger.d(context, TAG, "Order cost updated in ViewModel");
        } else {
            // Если ViewModel ещё нет — сохраняем в SharedPreferences на будущее
            Logger.e(context, TAG, "OrderViewModel is null, saving order cost for later");
        }
    }

}
