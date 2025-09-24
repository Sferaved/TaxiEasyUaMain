package com.taxi.easy.ua.utils.notify;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
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
        int notificationId = 654321; // ID for each notification

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

    public static void showNotificationFindAutoMessage(Context context, String message, String uid) {
        Logger.d(context, TAG, "Вызван showNotificationFindAutoMessage()");
        Logger.d(context, TAG, "Текст уведомления: " + message);
        Logger.d(context, TAG, "uid: " + uid);

        String uidOld = (String) sharedPreferencesHelperMain.getValue("uid_fcm", "");

        if (uidOld.equals(uid)) {
            Logger.d(context, TAG, "UID совпадает с предыдущим, уведомление не показывается.");
            return;
        } else {
            if (uid != null) {
                sharedPreferencesHelperMain.saveValue("uid_fcm", uid);
            } else {
                Logger.d(context, TAG,  "uid is null, skipping save");
                // Optionally, save a default value or skip saving
                sharedPreferencesHelperMain.saveValue("uid_fcm", "");
            }
        }

        // Генерация уникального ID уведомления
        int notificationId = generateUniqueNotificationId();

        // Создаем Intent для MainActivity и передаем notificationId
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("notification_id", notificationId);
        Logger.d(context, TAG, "Создан Intent для MainActivity с notification_id = " + notificationId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Logger.d(context, TAG, "Создан PendingIntent");

        // Загружаем иконку
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_image);
        Logger.d(context, TAG, "largeIcon загружен: " + (largeIcon != null));

        // Строим уведомление
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentText(message)
                .setLargeIcon(largeIcon)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(largeIcon)
                        .bigLargeIcon((Bitmap) null))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        Logger.d(context, TAG, "NotificationCompat.Builder создан с CHANNEL_ID = " + CHANNEL_ID);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Проверяем разрешение
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Logger.d(context, TAG, "Разрешение POST_NOTIFICATIONS не предоставлено. Уведомление не будет показано.");
            return;
        }

        Logger.d(context, TAG, "Показываем уведомление...");
        notificationManager.notify(notificationId, builder.build());
        Logger.d(context, TAG, "notificationManager.notify() вызван.");
    }

    /**
     * Удаляет уведомление, если в Intent передан notification_id.
     */
    public static void cancelNotificationFromIntent(Context context, Intent intent) {
        if (intent != null && intent.hasExtra("notification_id")) {
            int notificationId = intent.getIntExtra("notification_id", -1);
            if (notificationId != -1) {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                notificationManager.cancel(notificationId);
                Logger.d(context, TAG, "Уведомление с ID " + notificationId + " удалено через cancelNotificationFromIntent()");
            }
        }
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


    public static void sendPaymentErrorNotification(Context context, String title, String message) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel("payment_error", context.getString(R.string.paymentErrMes), NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK); // Уничтожаем старую активность и создаём новую

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "payment_error")
                .setSmallIcon(R.drawable.ic_error)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify(2, builder.build());
    }


//    public static void showNotificationUpdate(Context context) {
//        String title = context.getString(R.string.new_version);
//        String message = context.getString(R.string.news_of_version);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
//            channel.setDescription(CHANNEL_DESCRIPTION);
//            channel.enableLights(true);
//            channel.setLightColor(Color.RED);
//            channel.enableVibration(true);
//            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        Intent updateIntent = new Intent(context, UpdateService.class);
//        PendingIntent updatePendingIntent = PendingIntent.getService(context, 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle(title)
//                .setContentText(message)
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//                .setAutoCancel(true);
//
//        builder.addAction(android.R.drawable.ic_menu_view, context.getString(R.string.update_url), updatePendingIntent);
//
//        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//        int notificationId = 1234569874;
//        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        notificationManager.notify(notificationId, builder.build());
//    }

//    public static class UpdateService extends Service {
//        @Override
//        public int onStartCommand(Intent intent, int flags, int startId) {
//            if (intent != null) {
//                checkForUpdate(this);
//            }
//            return super.onStartCommand(intent, flags, startId);
//        }
//
//        @Nullable
//        @Override
//        public IBinder onBind(Intent intent) {
//            return null;
//        }
//    }

//    private static final int MY_REQUEST_CODE = 1234;
//
//    private void checkForUpdate(Context context) {
//        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);
//        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
//
//        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
//            Logger.d(context, TAG, "Update availability: " + appUpdateInfo.updateAvailability());
//            Logger.d(context, TAG, "Update priority: " + appUpdateInfo.updatePriority());
//            Logger.d(context, TAG, "Client version staleness days: " + appUpdateInfo.clientVersionStalenessDays());
//
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
//                Logger.d(context, TAG, "Available updates found");
//
//                try {
//                    appUpdateManager.startUpdateFlowForResult(
//                            appUpdateInfo,
//                            AppUpdateType.IMMEDIATE,
//                            (Activity) context,
//                            MY_REQUEST_CODE);
//
//                } catch (IntentSender.SendIntentException e) {
//                    Logger.e(context, TAG, "Failed to start update flow: " + e.getMessage());
//                    FirebaseCrashlytics.getInstance().recordException(e);
//                }
//            }
//        }).addOnFailureListener(e -> {
//            Logger.e(context, TAG, "Failed to check for updates: " + e.getMessage());
//            FirebaseCrashlytics.getInstance().recordException(e);
//        });
//    }


}
