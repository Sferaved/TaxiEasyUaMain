package com.taxi.easy.ua.service;

import static com.taxi.easy.ua.MainActivity.CITY_INFO;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.utils.OrderStatusDialogActivity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderStatusService extends Service {
    private static final String TAG = "OrderStatusService";
    private static final int NOTIFICATION_ID = 12345;
    private static final String CHANNEL_ID = "order_status_channel";

    private Handler handler;
    private Runnable statusTask;
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        Logger.d(context, TAG, "Сервис создан");

        createNotificationChannel();

        handler = new Handler();
        statusTask = new Runnable() {
            @Override
            public void run() {
                checkOrderStatus();
                handler.postDelayed(this, 10000); // каждые 10 секунд
            }
        };
        handler.post(statusTask);
    }

    /**
     * Создаем канал уведомлений с минимальным приоритетом
     * и без звука/вибрации, чтобы уведомление было "невидимым".
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Order Status Service",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setSound(null, null);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Создаем максимально "тихое" уведомление.
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("") // пустой заголовок
                .setContentText("")  // пустой текст
                .setSmallIcon(R.drawable.transparent_icon) // сделайте прозрачную иконку 1x1px
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setSilent(true)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build();
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    private void checkOrderStatus() {
        Logger.d(context, TAG, "Вызов statusOrder() из сервиса");

        try {
            statusOrderAll();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                for (String cn : c.getColumnNames()) {
                    list.add(c.getString(c.getColumnIndex(cn)));
                }
            } while (c.moveToNext());
        }
        c.close();
        database.close();
        return list;
    }

    private void statusOrderAll() throws ParseException {
        Logger.d(context, "Pusher", "statusOrderAll: ");

        String api = logCursor(CITY_INFO, context).get(2);
        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

        String baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        String url = baseUrl + api + "/android/searchAutoOrderServiceAll/" + email + "/" + context.getString(R.string.application) + "/yes_mes";

        Call<AutoOrderResponse> call = ApiClient.getApiService().searchAutoOrderServiceAll(url);
        Logger.d(context, TAG, "statusOrder url: " + url);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<AutoOrderResponse> call, @NonNull Response<AutoOrderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AutoOrderResponse result = response.body();

                    Logger.d(context, TAG, "Total orders: " + result.getTotal_orders());
                    if (result.getTotal_orders() == 0) {
                        sharedPreferencesHelperMain.saveValue("uid_fcm", "");
                    } else {
                        for (AutoOrderResponse.OrderItem item : result.getOrders()) {
                            String uid = item.getDispatching_order_uid();
                            Logger.d(context, TAG, "Номер: " + item.getNumber() + ", UID: " + uid);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<AutoOrderResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Error: " + t.getMessage());
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }

    private void openBottomSheet(String message) {
        Intent intent = new Intent(context, OrderStatusDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("bottomSheetMessage", message);
        context.startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) handler.removeCallbacks(statusTask);
        Logger.d(context, TAG, "Сервис остановлен");
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
