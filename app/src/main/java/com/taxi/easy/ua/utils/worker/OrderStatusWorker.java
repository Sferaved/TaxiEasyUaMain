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

            Logger.d(getApplicationContext(), TAG, "Планируем следующий запуск через 10 секунд");

            OneTimeWorkRequest nextRun = new OneTimeWorkRequest.Builder(OrderStatusWorker.class)
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build();

            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork(
                            TAG,  // уникальное имя работы — чтобы не создавать дубликаты
                            ExistingWorkPolicy.REPLACE,  // заменяем старую, если есть
                            nextRun
                    );

            Logger.d(getApplicationContext(), TAG, "Следующий запуск успешно запланирован");

            return Result.success();
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Ошибка в OrderStatusWorker: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            return Result.failure();
        }
    }
}
