package com.taxi.easy.ua.utils.worker.utils;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.taxi.easy.ua.utils.worker.SaveIPWithEmailWorker;

import java.util.concurrent.TimeUnit;

public class SaveIPWithEmailUtils {
    private static final String TAG = "SaveIPWithEmailUtils";
    private static final String DEFAULT_PAGE = "PAS4";

    /**
     * Запустить Worker немедленно
     * @param emailUser email пользователя
     * @param context контекст
     */
    public static void startWorker(String emailUser, Context context) {
        startWorker(emailUser, DEFAULT_PAGE, context);
    }

    /**
     * Запустить Worker немедленно с указанием страницы
     * @param emailUser email пользователя
     * @param page страница
     * @param context контекст
     */
    public static void startWorker(String emailUser, String page, Context context) {
        // Требования к сети
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Создаем запрос
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SaveIPWithEmailWorker.class)
                .setConstraints(constraints)
                .setInputData(
                        new androidx.work.Data.Builder()
                                .putString("emailUser", emailUser)
                                .putString("page", page)
                                .build()
                )
                .build();

        // Запускаем
        WorkManager.getInstance(context).enqueue(workRequest);

        android.util.Log.e(TAG, "Worker started - email: " + emailUser + ", page: " + page);
    }

    /**
     * Запустить Worker с задержкой
     * @param emailUser email пользователя
     * @param page страница
     * @param delaySeconds задержка в секундах
     * @param context контекст
     */
    public static void startWorkerWithDelay(String emailUser, String page, long delaySeconds, Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SaveIPWithEmailWorker.class)
                .setConstraints(constraints)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(
                        new androidx.work.Data.Builder()
                                .putString("emailUser", emailUser)
                                .putString("page", page)
                                .build()
                )
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);

        android.util.Log.e(TAG, "Worker delayed for " + delaySeconds + "s - email: " + emailUser + ", page: " + page);
    }

    /**
     * Запустить Worker с уникальным тегом (можно отменить позже)
     * @param emailUser email пользователя
     * @param page страница
     * @param uniqueTag уникальный тег
     * @param context контекст
     */
    public static void startWorkerWithTag(String emailUser, String page, String uniqueTag, Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SaveIPWithEmailWorker.class)
                .setConstraints(constraints)
                .addTag(uniqueTag)
                .setInputData(
                        new androidx.work.Data.Builder()
                                .putString("emailUser", emailUser)
                                .putString("page", page)
                                .build()
                )
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);

        android.util.Log.e(TAG, "Worker with tag started - tag: " + uniqueTag + ", email: " + emailUser);
    }

    /**
     * Запустить Worker с политикой повторных попыток
     * @param emailUser email пользователя
     * @param page страница
     * @param context контекст
     */
    public static void startWorkerWithRetry(String emailUser, String page, Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SaveIPWithEmailWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        10, // начальная задержка
                        TimeUnit.SECONDS
                )
                .setInputData(
                        new androidx.work.Data.Builder()
                                .putString("emailUser", emailUser)
                                .putString("page", page)
                                .build()
                )
                .build();

        WorkManager.getInstance(context).enqueue(workRequest);

        android.util.Log.e(TAG, "Worker with retry started - email: " + emailUser + ", page: " + page);
    }

    /**
     * Отменить все Workers с определенным тегом
     * @param tag тег
     * @param context контекст
     */
    public static void cancelWorkersByTag(String tag, Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag);
        android.util.Log.e(TAG, "Cancelled all workers with tag: " + tag);
    }

    /**
     * Отменить все Workers
     * @param context контекст
     */
    public static void cancelAllWorkers(Context context) {
        WorkManager.getInstance(context).cancelAllWork();
        android.util.Log.e(TAG, "Cancelled all workers");
    }
}