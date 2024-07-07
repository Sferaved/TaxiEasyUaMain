package com.taxi.easy.ua.utils.notify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.home.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.utils.download.AppUpdater;
import com.taxi.easy.ua.utils.log.Logger;


public class NotificationHelper {
    private static final String CHANNEL_ID = "my_channel_id";
    private static final String CHANNEL_NAME = "My Channel";
    private static final String CHANNEL_DESCRIPTION = "This is my notification channel";
    private static final int REQUEST_CODE_OPEN_URL = 1;
    private static final String TAG = "NotificationHelper";

    public static void showNotification(Context context, String title, String message, String url) {
        // Создание канала уведомлений для Android 8.0 (API level 26) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Интент для открытия URL-адреса при нажатии на уведомление
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        PendingIntent pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE_OPEN_URL, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Построение уведомления с кнопкой действия
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_view, context.getString(R.string.update_url), pendingIntent);

        // Отображение уведомления
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = generateUniqueNotificationId(); // Generate a unique ID for each notification

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        notificationManager.notify(notificationId, builder.build());
    }

    public static void showNotificationMessage(Context context, String title, String message) {
        // Создание канала уведомлений для Android 8.0 (API level 26) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Интент для открытия URL-адреса при нажатии на уведомление

        // Построение уведомления с кнопкой действия
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Отображение уведомления
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = generateUniqueNotificationId(); // Generate a unique ID for each notification

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        notificationManager.notify(notificationId, builder.build());
    }

    public static void showNotificationMessageOpen(Context context, String title, String message, PendingIntent pendingIntent) {
        // Создание канала уведомлений для Android 8.0 (API level 26) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Построение уведомления с кнопкой действия
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent); // Добавление действия при нажатии на всё уведомление

        // Отображение уведомления
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = generateUniqueNotificationId(); // Generate a unique ID for each notification

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        notificationManager.notify(notificationId, builder.build());
    }

    private static int generateUniqueNotificationId() {
        // Generate a unique ID based on the current time
        return (int) System.currentTimeMillis();
    }

    @SuppressLint("ObsoleteSdkInt")
    public static void showNotificationUpdate(Context context) {
        String title = context.getString(R.string.new_version);
        String message =  context.getString(R.string.news_of_version);
        // Создание канала уведомлений для Android 8.0 (API level 26) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Интент для вызова службы для загрузки и установки обновления
        Intent updateIntent = new Intent(context, UpdateService.class);

        PendingIntent updatePendingIntent = PendingIntent.getService(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Построение уведомления без кнопки действия
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Устанавливаем кнопку действия в уведомлении
        builder.addAction(android.R.drawable.ic_menu_view, context.getString(R.string.update_url), updatePendingIntent);

        // Отображение уведомления
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = generateUniqueNotificationId(); // Генерируем уникальный ID для каждого уведомления
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        notificationManager.notify(notificationId, builder.build());
    }
    public static class UpdateService extends Service {
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent != null) {
                checkForUpdate(this);
            }
            return super.onStartCommand(intent, flags, startId);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    private static final int MY_REQUEST_CODE = 1234; // Уникальный код запроса для обновления

    private static void checkForUpdate(Context context) {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            Logger.d(context, TAG, "Update availability: " + appUpdateInfo.updateAvailability());
            Logger.d(context, TAG, "Update priority: " + appUpdateInfo.updatePriority());
            Logger.d(context, TAG, "Client version staleness days: " + appUpdateInfo.clientVersionStalenessDays());

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Доступны обновления
                Logger.d(context, TAG, "Available updates found");

                // Запускаем процесс обновления
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE, // или AppUpdateType.FLEXIBLE
                            (Activity) context, // Используем ссылку на активность
                            MY_REQUEST_CODE); // Код запроса для обновления
                } catch (IntentSender.SendIntentException e) {
                    Logger.e(context, TAG, "Failed to start update flow: " + e.getMessage());
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            } else {
                Logger.d(context, TAG, "No updates available");
                String message = context.getString(R.string.update_ok);
                MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
                FragmentManager fragmentManager = ((FragmentActivity) context).getSupportFragmentManager();
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        }).addOnFailureListener(e -> {
            Logger.e(context, TAG, "Failed to check for updates: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        });
    }
}
