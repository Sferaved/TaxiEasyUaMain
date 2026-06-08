package com.taxi.easy.ua.utils.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.utils.OrderStatusUtils;

import java.util.concurrent.TimeUnit;

public class OrderStatusWorker extends Worker {
    private static final String TAG = "OrderStatusWorker";
    private static final long POLL_INTERVAL_SECONDS = 10;
    private static final long ERROR_RETRY_INTERVAL_SECONDS = 30;

    public OrderStatusWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.d(getApplicationContext(), TAG, "OrderStatusWorker started");

        try {
            Logger.d(getApplicationContext(), TAG, "Выполняем checkOrders...");
            boolean success = OrderStatusUtils.checkOrders(getApplicationContext(), this);
            Logger.d(getApplicationContext(), TAG, "checkOrders result: " + success);

            if (success) {
                scheduleNextRun(POLL_INTERVAL_SECONDS);
            } else {
                Logger.d(getApplicationContext(), TAG, "Заказов нет — опрос остановлен");
            }
            return Result.success();
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Ошибка в OrderStatusWorker: " + e.getMessage());
            boolean isTimeout = "Request timed out".equals(e.getMessage());
            if (!isTimeout) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
            scheduleNextRun(ERROR_RETRY_INTERVAL_SECONDS);
            return Result.success();
        }
    }

    private void scheduleNextRun(long delaySeconds) {
        Logger.d(getApplicationContext(), TAG, "Планируем следующий запуск через " + delaySeconds + " секунд");

        OneTimeWorkRequest nextRun = new OneTimeWorkRequest.Builder(OrderStatusWorker.class)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, nextRun);

        Logger.d(getApplicationContext(), TAG, "Следующий запуск успешно запланирован");
    }
}
