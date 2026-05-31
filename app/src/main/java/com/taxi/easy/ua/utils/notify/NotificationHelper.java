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
    private static final String CHANNEL_ID = "general";
    private static final String CHANNEL_ID_CANCEL = "order_cancel";
    private static final String CHANNEL_ID_CAR_FOUND = "car_found";
    private static final int REQUEST_CODE_OPEN_URL = 1;
    private static final String TAG = "NotificationHelper";
    /** UID заказа, для которого уже показали push «авто найдено» (отдельно от uid_fcm). */
    private static final String PREF_LAST_CAR_FOUND_NOTIFY_UID = "last_car_found_notify_uid";

    private static void ensureNotificationChannel(Context context) {
        ensureChannel(context, CHANNEL_ID, R.string.notification_channel_general, NotificationManager.IMPORTANCE_DEFAULT);
    }

    private static void ensureCancelChannel(Context context) {
        ensureChannel(context, CHANNEL_ID_CANCEL, R.string.notification_channel_cancel, NotificationManager.IMPORTANCE_HIGH);
    }

    private static void ensureCarFoundChannel(Context context) {
        ensureChannel(context, CHANNEL_ID_CAR_FOUND, R.string.notification_channel_car_found, NotificationManager.IMPORTANCE_HIGH);
    }

    private static void ensureChannel(Context context, String channelId, int nameResId, int importance) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel channel = new NotificationChannel(
                        channelId, context.getString(nameResId), importance);
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
                Logger.d(context, TAG, "Создан канал уведомлений: " + channelId);
            }
        }
    }

    public static void showNotification(Context context, String title, String message, String url) {
        ensureNotificationChannel(context);

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
        ensureNotificationChannel(context);

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
        ensureCarFoundChannel(context);
        Logger.d(context, TAG, "Вызван showNotificationFindAutoMessage()");
        Logger.d(context, TAG, "Текст уведомления: " + message);
        Logger.d(context, TAG, "uid: " + uid);

        String lastCarFoundUid = (String) sharedPreferencesHelperMain.getValue(PREF_LAST_CAR_FOUND_NOTIFY_UID, "");
        if (uid != null && uid.equals(lastCarFoundUid)) {
            Logger.d(context, TAG, "Push «авто найдено» уже показывали для uid=" + uid);
            return;
        }
        if (uid != null && !uid.isEmpty()) {
            sharedPreferencesHelperMain.saveValue(PREF_LAST_CAR_FOUND_NOTIFY_UID, uid);
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_CAR_FOUND)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_car_found_title, context.getString(R.string.app_name)))
                .setContentText(message)
                .setLargeIcon(largeIcon)
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(largeIcon)
                        .bigLargeIcon((Bitmap) null))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        Logger.d(context, TAG, "NotificationCompat.Builder создан с CHANNEL_ID = " + CHANNEL_ID_CAR_FOUND);

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
     * Уведомление об отмене заказа (FCM status=cancelled).
     */
    public static void showNotificationCancelMessage(Context context, String message, String uid) {
        Logger.d(context, TAG, "showNotificationCancelMessage: " + message + ", uid=" + uid);
        ensureCancelChannel(context);

        int notificationId = generateUniqueNotificationId();

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("notification_id", notificationId);
        intent.putExtra("fcm_action", "order_cancelled");
        if (uid != null) {
            intent.putExtra("order_uid", uid);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = context.getString(R.string.notification_cancel_title, context.getString(R.string.app_name));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_CANCEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Logger.d(context, TAG, "POST_NOTIFICATIONS не предоставлено — уведомление об отмене не показано");
            return;
        }

        notificationManager.notify(notificationId, builder.build());
        Logger.d(context, TAG, "Уведомление об отмене показано, id=" + notificationId);
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
        ensureNotificationChannel(context);

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


    private static final String PAYMENT_ERROR_CHANNEL_ID = "payment_error_channel";
    /** Один слот в шторке уведомлений — повторный push обновляет, а не дублирует. */
    private static final int PAYMENT_ERROR_NOTIFICATION_ID = 91042;

    public static void sendPaymentErrorNotification(Context context, String title, String message) {
        ensurePaymentErrorChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("notification_id", PAYMENT_ERROR_NOTIFICATION_ID);
        intent.putExtra("fcm_action", "payment_error");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                PAYMENT_ERROR_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PAYMENT_ERROR_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_error)
                .setContentTitle(context.getString(R.string.notification_payment_error_title, context.getString(R.string.app_name)))
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Logger.d(context, TAG, "POST_NOTIFICATIONS не предоставлено — уведомление об ошибке оплаты не показано");
            return;
        }

        notificationManager.notify(PAYMENT_ERROR_NOTIFICATION_ID, builder.build());
        Logger.d(context, TAG, "Уведомление об ошибке оплаты показано, id=" + PAYMENT_ERROR_NOTIFICATION_ID);
    }

    private static void ensurePaymentErrorChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager.getNotificationChannel(PAYMENT_ERROR_CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        PAYMENT_ERROR_CHANNEL_ID,
                        context.getString(R.string.notification_channel_payment),
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(context.getString(R.string.pay_failure_mes));
                notificationManager.createNotificationChannel(channel);
            }
        }
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
