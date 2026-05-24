package com.taxi.easy.ua.utils.fcm;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.notify.NotificationHelper;
import com.taxi.easy.ua.utils.payment.PaymentDeclinedNotifier;
import com.taxi.easy.ua.utils.worker.utils.TokenUtils;

import java.util.Locale;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFCMService";

    // ============================================================
    // 1. Обработка нового FCM-токена
    // ============================================================
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Новый FCM-токен получен: " + token);
        Logger.d(this, TAG, "Новый FCM-токен: " + token);

        String userEmail = getSavedUserEmail();

        // Если пользователь уже залогинен — отправляем сразу
        if (userEmail != null && !userEmail.isEmpty() && !userEmail.equals("no_email")) {
            String lastToken = (String) MyApplication.sharedPreferencesHelperMain.getValue("last_fcm_token", "");

            Log.d(TAG, "Отправляем новый токен на сервер для пользователя: " + userEmail);
            TokenUtils.sendToken(this, userEmail, token);
            saveLastSentToken(token);
//
//            if (!token.equals(lastToken)) {
//                Log.d(TAG, "Отправляем новый токен на сервер для пользователя: " + userEmail);
//                TokenUtils.sendToken(this, userEmail, token);
//                saveLastSentToken(token);  // сохраняем ТОЛЬКО после успешной отправки
//            } else {
//                Log.d(TAG, "Токен не изменился — пропускаем отправку");
//            }
        } else {
            // Пользователь ещё не залогинен
            Log.w(TAG, "Пользователь не залогинен — токен НЕ сохраняем как last_fcm_token, отправим после логина");
            // ← НЕ вызываем saveLastSentToken(token)!
            // Токен будет получен заново в MainActivity.sendCurrentFcmToken() после логина
        }
    }

    private String getSavedUserEmail() {
        return (String) MyApplication.sharedPreferencesHelperMain.getValue("userEmail", "no_email");

    }

    private void saveLastSentToken(String token) {
        MyApplication.sharedPreferencesHelperMain.saveValue("last_fcm_token", token);
    }

    // ============================================================
    // 2. Обработка входящих push-уведомлений (data messages)
    // ============================================================
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        Logger.d(this, TAG, "Получено data-сообщение: " + data);

        if (data.isEmpty()) {
            Logger.d(this, TAG, "Данные пуш-уведомления пусты");
            return;
        }

        // Специальная обработка пуша с стоимостью заказа
        if (data.containsKey("order_cost")) {
            handleOrderCostMessage(data);
            return;
        }

        // Автоотмена заказа (sendNotificationCancel с бэкенда)
        if (isCancelMessage(data)) {
            handleCancelMessage(data);
            return;
        }

        // Ошибка оплаты (sendNotificationPaymentError с бэкенда)
        if (isPaymentErrorMessage(data)) {
            handlePaymentErrorMessage(data);
            return;
        }

        // Обычное уведомление "Найдено авто"
        String locale = LocaleHelper.getLocale();
        Logger.d(this, TAG, "Текущая локаль: " + locale);

        String message = data.get("message_" + locale);
        if (message == null) {
            message = data.get("message_uk"); // fallback
            Logger.d(this, TAG, "Fallback на message_uk: " + message);
        }

        String uid = data.get("uid");

        if (message == null || message.isEmpty()) {
            message = "Найдено авто (по умолчанию)";
            Logger.d(this, TAG, "Сообщение пустое — использовано значение по умолчанию");
        }

        Logger.d(this, TAG, "Текст уведомления: " + message);
        Logger.d(this, TAG, "uid: " + uid);

        notifyUser(message, uid);
    }

    private boolean isCancelMessage(Map<String, String> data) {
        String status = data.get("status");
        if (status != null && (status.equalsIgnoreCase("cancelled") || status.equalsIgnoreCase("canceled"))) {
            return true;
        }
        String messageUk = data.get("message_uk");
        if (messageUk != null) {
            String lower = messageUk.toLowerCase(Locale.ROOT);
            return lower.contains("скасован") || lower.contains("отмен");
        }
        return false;
    }

    /**
     * Пуш об отмене заказа (AutoCancelJob → sendNotificationCancel).
     */
    private void handleCancelMessage(Map<String, String> data) {
        String locale = LocaleHelper.getLocale();
        String message = data.get("message_" + locale);
        if (message == null || message.isEmpty()) {
            message = data.get("message_uk");
        }
        if (message == null || message.isEmpty()) {
            message = getString(R.string.ex_st_canceled);
        }

        String uid = data.get("uid");
        Logger.d(this, TAG, "Отмена заказа FCM: " + message + ", uid=" + uid);

        notifyCancel(message, uid);
        applyCanceledStatusToActiveOrder(uid);
    }

    private void applyCanceledStatusToActiveOrder(String uid) {
        if (uid == null || uid.isEmpty()) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (MainActivity.uid != null && MainActivity.uid.equals(uid) && MainActivity.viewModel != null) {
                MainActivity.viewModel.setCanceledStatus("canceled");
                Logger.d(this, TAG, "setCanceledStatus(canceled) для uid=" + uid);
            } else {
                Logger.d(this, TAG, "Отмена FCM: uid не совпадает с активным заказом (active=" + MainActivity.uid + ")");
            }
        });
    }

    private boolean isPaymentErrorMessage(Map<String, String> data) {
        if ("payment_error".equals(data.get("type"))) {
            return true;
        }
        return "Declined".equals(data.get("transactionStatus"))
                || "Declined".equals(data.get("status"));
    }

    /**
     * FCM об отклонённой оплате (PaymentStatusNotifier → sendNotificationPaymentError).
     */
    private void handlePaymentErrorMessage(Map<String, String> data) {
        String locale = LocaleHelper.getLocale();
        Context localizedContext = getLocalizedContext(getApplicationContext(), locale);

        String message = data.get("message_" + locale);
        if (message == null || message.isEmpty()) {
            message = data.get("message_uk");
        }
        if (message == null || message.isEmpty()) {
            message = localizedContext.getString(R.string.pay_failure_mes);
        }

        String uid = data.get("uid");

        Logger.d(this, TAG, "Ошибка оплаты FCM: " + message + ", uid=" + uid);

        if (!MyApplication.isInForeground()) {
            PaymentDeclinedNotifier.maybeSendPaymentErrorPush(localizedContext, uid);
        }
        applyDeclinedToActiveOrder(uid);
    }

    private void applyDeclinedToActiveOrder(String uid) {
        if (uid == null || uid.isEmpty()) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (MainActivity.uid != null
                    && MainActivity.uid.equals(uid)
                    && MainActivity.viewModel != null) {
                MainActivity.viewModel.setTransactionStatus("Declined");
                Logger.d(this, TAG, "setTransactionStatus(Declined) для uid=" + uid);
            }
        });
    }

    private void notifyCancel(String message, String uid) {
        Context context = getApplicationContext();
        String localeCode = LocaleHelper.getLocale();
        Context localizedContext = getLocalizedContext(context, localeCode);
        NotificationHelper.showNotificationCancelMessage(localizedContext, message, uid);
    }

    // ============================================================
    // 3. Показ уведомления "Найдено авто"
    // ============================================================
    private void notifyUser(String message, String uid) {
        Context context = getApplicationContext();
        String localeCode = LocaleHelper.getLocale();

        Context localizedContext = getLocalizedContext(context, localeCode);

        NotificationHelper.showNotificationFindAutoMessage(localizedContext, message, uid);
    }

    private Context getLocalizedContext(Context context, String localeCode) {
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    // ============================================================
    // 4. Обработка пуша с order_cost
    // ============================================================
    private void handleOrderCostMessage(Map<String, String> data) {
        Context context = getApplicationContext();
        Logger.d(context, TAG, "Получено сообщение со стоимостью заказа: " + data);

        String orderCost = data.get("order_cost");
        if (orderCost == null) {
            orderCost = "0";
        }

        Logger.d(context, TAG, "order_cost: " + orderCost);

        if (MainActivity.orderViewModel != null) {
            MainActivity.orderViewModel.setOrderCost(orderCost);
            Logger.d(context, TAG, "Стоимость заказа обновлена в OrderViewModel");
        } else {
            Logger.w(context, TAG, "OrderViewModel ещё не инициализирован — стоимость будет обновлена позже");
            // При необходимости можно временно сохранить в SharedPreferences
        }
    }
}