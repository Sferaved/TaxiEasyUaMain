package com.taxi.easy.ua.utils.review;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.taxi.easy.ua.BuildConfig;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

/**
 * Клас для управління оцінюванням застосунку в Google Play
 * Використовує In-app Review API (Android 5.0+)
 */
public class AppReviewManager {

    private static final String TAG = "AppReviewManager";
    private static final String PREF_APP_REVIEWED = "app_reviewed";
    private static final String PREF_REVIEW_REQUEST_COUNT = "review_request_count";
    private static final String PREF_COMPLETED_ORDERS_AT_LAST_REQUEST = "completed_orders_at_last_request";
    private static final String PREF_LAST_REVIEW_REQUEST_TIME = "last_review_request_time";

    private static final int MIN_COMPLETED_ORDERS_FOR_REVIEW = 5;  // Мінімум поїздок для запиту
    private static final int REQUEST_COOLDOWN_DAYS = 30;           // Запитувати не частіше ніж раз на 30 днів
    private static final int MAX_REQUESTS_PER_USER = 3;            // Максимум 3 запити на користувача

    private final Context context;
    private final SharedPreferencesHelper prefs;
    private ReviewManager reviewManager;
    private ReviewInfo pendingReviewInfo;

    public AppReviewManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = new SharedPreferencesHelper(this.context);
        this.reviewManager = ReviewManagerFactory.create(this.context);
    }

    /**
     * Основний метод для запиту оцінки
     * @param activity Поточна Activity
     * @param callback Callback для відстеження результату
     */
    public void requestReview(@NonNull Activity activity, @Nullable ReviewCallback callback) {
        Logger.d(context, TAG, "requestReview() called");
        // ПРОВЕРКА НА ЭМУЛЯТОР - сразу используем fallback
        if (isEmulator()) {
            Logger.d(context, TAG, "Emulator detected - opening Play Store directly");
            openPlayStorePage(activity);  // ← ДОБАВИТЬ ЭТО!
            if (callback != null) {
                callback.onReviewCompleted();
            }
            return;  // Выходим, не проверяем остальные условия
        }

        // Остальной код для реальных устройств...
        if (!canRequestReview()) {
            Logger.d(context, TAG, "Cannot request review - conditions not met");
            if (callback != null) {
                callback.onReviewNotAvailable("Conditions not met");
            }
            return;
        }


        // Отримуємо ReviewInfo
        Task<ReviewInfo> request = reviewManager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Отримали ReviewInfo, можна показувати діалог
                ReviewInfo reviewInfo = task.getResult();
                pendingReviewInfo = reviewInfo;

                launchReviewFlow(activity, reviewInfo, callback);
            } else {
                // Якщо In-app Review не доступний (наприклад, старий Android або не Google Play)
                Logger.e(context, TAG, "Request review flow failed: " + task.getException());

                // Використовуємо fallback - відкриваємо сторінку в Google Play
                openPlayStorePage(activity);

                if (callback != null) {
                    callback.onReviewFailed(task.getException());
                }
            }
        });
    }

    /**
     * Запускає діалог оцінювання
     */
    private void launchReviewFlow(@NonNull Activity activity, @NonNull ReviewInfo reviewInfo, @Nullable ReviewCallback callback) {
        Task<Void> flow = reviewManager.launchReviewFlow(activity, reviewInfo);
        flow.addOnCompleteListener(task -> {
            // Діалог закрито (користувач поставив оцінку або закрив)
            Logger.d(context, TAG, "Review flow completed. Task successful: " + task.isSuccessful());

            if (task.isSuccessful()) {
                // Користувач побачив діалог (навіть якщо не поставив оцінку)
                markReviewRequested();
                Logger.d(context, TAG, "User has seen the review dialog");

                if (callback != null) {
                    callback.onReviewCompleted();
                }
            } else {
                Logger.e(context, TAG, "Review flow launch failed: " + task.getException());
                if (callback != null) {
                    callback.onReviewFailed(task.getException());
                }
            }

            pendingReviewInfo = null;
        });
    }

    private boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    /**
     * Перевіряє, чи можна робити запит на оцінку
     */
    private boolean canRequestReview() {
        if (BuildConfig.DEBUG) {
            // В DEBUG режиме используем fallback для эмуляторов
            if (isEmulator()) {
                Logger.d(context, TAG, "Emulator detected - will use fallback");
                // Не возвращаем true, чтобы пойти по пути fallback
                return false;
            }
            return true;
        }
        // 1. Чи вже оцінював користувач?
        if (hasUserReviewed()) {
            Logger.d(context, TAG, "User has already reviewed the app");
            return false;
        }

        // 2. Перевіряємо кількість запитів
        int requestCount = getReviewRequestCount();
        if (requestCount >= MAX_REQUESTS_PER_USER) {
            Logger.d(context, TAG, "Max review requests reached: " + requestCount);
            return false;
        }

        // 3. Перевіряємо cooldown (не частіше ніж раз на N днів)
        long lastRequestTime = getLastRequestTime();
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = REQUEST_COOLDOWN_DAYS * 24L * 60L * 60L * 1000L;

        if (lastRequestTime > 0 && (currentTime - lastRequestTime) < cooldownMillis) {
            long daysLeft = (cooldownMillis - (currentTime - lastRequestTime)) / (24 * 60 * 60 * 1000);
            Logger.d(context, TAG, "Cooldown active. Days left: " + daysLeft);
            return false;
        }

        // 4. Перевіряємо кількість завершених поїздок
        int completedOrders = getCompletedOrdersCount();
        if (completedOrders < MIN_COMPLETED_ORDERS_FOR_REVIEW) {
            Logger.d(context, TAG, "Not enough completed orders: " + completedOrders + "/" + MIN_COMPLETED_ORDERS_FOR_REVIEW);
            return false;
        }

        // 5. Перевіряємо, чи не відхиляли нещодавно (якщо після останнього запиту кількість поїздок не збільшилась)
        int ordersAtLastRequest = getCompletedOrdersAtLastRequest();
        if (ordersAtLastRequest >= completedOrders) {
            Logger.d(context, TAG, "No new orders since last request");
            return false;
        }

        Logger.d(context, TAG, "Can request review - all conditions met");
        return true;
    }

    /**
     * Відкриває сторінку застосунку в Google Play (fallback)
     */
    private void openPlayStorePage(@NonNull Activity activity) {
        Logger.d(context, TAG, "Opening Play Store page (fallback)");

        try {
            // Спроба відкрити через додаток Google Play
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Якщо Google Play додаток не встановлений, відкриваємо в браузері
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        }

        // Відмічаємо, що запит був зроблений (навіть якщо через fallback)
        markReviewRequested();
    }

    // ==================== МЕТОДИ ДЛЯ РОБОТИ ЗІ ЗБЕРЕЖЕНИМИ ДАНИМИ ====================

    /**
     * Позначає, що користувач поставив оцінку
     */
    public void markUserReviewed() {
        prefs.saveValue(PREF_APP_REVIEWED, true);
        Logger.d(context, TAG, "User marked as reviewed");
    }

    /**
     * Перевіряє, чи користувач вже оцінював застосунок
     */
    public boolean hasUserReviewed() {
        return (boolean) prefs.getValue(PREF_APP_REVIEWED, false);
    }

    /**
     * Збільшує лічильник запитів та оновлює час останнього запиту
     */
    private void markReviewRequested() {
        int currentCount = getReviewRequestCount();
        prefs.saveValue(PREF_REVIEW_REQUEST_COUNT, currentCount + 1);
        prefs.saveValue(PREF_LAST_REVIEW_REQUEST_TIME, System.currentTimeMillis());

        // Зберігаємо кількість поїздок на момент запиту
        prefs.saveValue(PREF_COMPLETED_ORDERS_AT_LAST_REQUEST, getCompletedOrdersCount());

        Logger.d(context, TAG, "Review request marked. Count: " + (currentCount + 1));
    }

    /**
     * Отримує кількість зроблених запитів на оцінку
     */
    private int getReviewRequestCount() {
        return (int) prefs.getValue(PREF_REVIEW_REQUEST_COUNT, 0);
    }

    /**
     * Отримує час останнього запиту
     */
    private long getLastRequestTime() {
        return (long) prefs.getValue(PREF_LAST_REVIEW_REQUEST_TIME, 0L);
    }

    /**
     * Отримує кількість поїздок на момент останнього запиту
     */
    private int getCompletedOrdersAtLastRequest() {
        return (int) prefs.getValue(PREF_COMPLETED_ORDERS_AT_LAST_REQUEST, -1);
    }

    /**
     * Отримує кількість завершених поїздок (потрібно реалізувати відповідно до вашої БД)
     */
    public int getCompletedOrdersCount() {
        // Тимчасово повертаємо 5 для тесту (щоб діалог показувався)
        if (BuildConfig.DEBUG) {
            return 5; // В debug режимі завжди показуємо
        }

        // Отримуємо кількість з SharedPreferences
        int savedCount = (int) prefs.getValue("completed_orders_count", -1);
        if (savedCount > 0) {
            return savedCount;
        }

        // Якщо нічого немає, повертаємо 5 для тесту (потім зміните)
        return 5;
    }

    /**
     * Скидає всі дані про оцінки (для тестування або очищення даних)
     */
    public void resetReviewData() {
        prefs.saveValue(PREF_APP_REVIEWED, false);
        prefs.saveValue(PREF_REVIEW_REQUEST_COUNT, 0);
        prefs.saveValue(PREF_LAST_REVIEW_REQUEST_TIME, 0L);
        prefs.saveValue(PREF_COMPLETED_ORDERS_AT_LAST_REQUEST, -1);
        Logger.d(context, TAG, "Review data reset");
    }

    /**
     * Callback для відстеження результату оцінювання
     */
    public interface ReviewCallback {
        void onReviewCompleted();
        void onReviewFailed(Exception e);
        void onReviewNotAvailable(String reason);
    }
}