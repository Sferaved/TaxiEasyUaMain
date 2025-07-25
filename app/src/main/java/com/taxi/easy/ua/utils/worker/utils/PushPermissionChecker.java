package com.taxi.easy.ua.utils.worker.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.log.Logger;

public class PushPermissionChecker {
    private static final String TAG = "PushPermissionChecker";
    private static final String PREFS_NAME = "push_permission_prefs";
    private static final String LAST_CHECK_KEY = "last_check_time";
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000; // 1 день

    /**
     * Проверяет разрешения на уведомления и открывает системное окно, если они отключены
     * (не чаще 1 раза в день).
     */
    public static void checkAndRequestPushPermission(Context context) {
        Logger.d(context, TAG, "Запуск проверки разрешений на push-уведомления");

        if (areNotificationsEnabled(context)) {
            Logger.d(context, TAG, "Уведомления уже включены — проверка завершена");
            return;
        }

        long lastCheck = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(LAST_CHECK_KEY, 0);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheck < ONE_DAY_MILLIS) {
            Logger.d(context, TAG, "Менее 24 часов с последней проверки — пропуск");
            return;
        }

        // Сохраняем время проверки
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(LAST_CHECK_KEY, currentTime)
                .apply();

        Logger.d(context, TAG, "Открытие окна уведомлений");
        openNotificationSettings(context);
    }

    /**
     * Проверка включены ли уведомления
     */
    public static boolean areNotificationsEnabled(Context context) {
        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
        boolean enabled = managerCompat.areNotificationsEnabled();
        Logger.d(context, TAG, "Статус уведомлений: " + (enabled ? "включены" : "выключены"));
        return enabled;
    }

    /**
     * Открывает окно с BottomSheet или запускает DialogHostActivity, если контекст не Activity
     */
    public static void openNotificationSettings(Context context) {
        Logger.d(context, TAG, "Попытка открытия окна уведомлений (только BottomSheet)");

        if (context instanceof FragmentActivity activity) {
            Logger.d(context, TAG, "Контекст — FragmentActivity, показываем BottomSheet");
            new Handler(Looper.getMainLooper()).post(() -> {
                String sentNotifyMessage = context.getString(R.string.sentNotifyMessage);
                MyBottomSheetErrorFragment bottomSheetDialogFragment =
                        new MyBottomSheetErrorFragment(sentNotifyMessage);

                bottomSheetDialogFragment.show(
                        activity.getSupportFragmentManager(),
                        bottomSheetDialogFragment.getTag()
                );
                Logger.d(context, TAG, "BottomSheetErrorFragment показан");
            });
        } else {
            Logger.d(context, TAG, "Контекст не Activity, запускаем DialogHostActivity");
            Intent intent = new Intent(context, DialogHostActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
