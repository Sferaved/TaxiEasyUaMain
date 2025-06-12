package com.taxi.easy.ua.utils.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.utils.VersionUtils;

public class VersionFromMarketWorker extends Worker {
    private static final String TAG = "VersionFromMarketWorker";

    public VersionFromMarketWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Logger.e(getApplicationContext(), TAG, "doWork: ");
        try {
            VersionUtils.versionFromMarket(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Ошибка в versionFromMarket: " + e.getMessage());
            return Result.failure();
        }
    }
}