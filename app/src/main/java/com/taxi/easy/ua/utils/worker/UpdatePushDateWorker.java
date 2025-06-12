package com.taxi.easy.ua.utils.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.utils.PushDateUpdater;

public class UpdatePushDateWorker extends Worker {
    private static final String TAG = "UpdatePushDateWorker";

    public UpdatePushDateWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            PushDateUpdater.updatePushDate(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Logger.e(MyApplication.getContext(), TAG, "Ошибка в updatePushDate: " + e.getMessage());
            return Result.failure();
        }
    }
}