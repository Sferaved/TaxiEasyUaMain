package com.taxi.easy.ua.utils.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.utils.PushUtils;

public class InsertPushDateWorker extends Worker {
    private static final String TAG = "InsertPushDateWorker";

    public InsertPushDateWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            PushUtils.insertPushDate(getApplicationContext());
            return Result.success();
        } catch (Exception e) {
            Logger.e(MyApplication.getContext(), TAG, "Ошибка в insertPushDate: " + e.getMessage());
            return Result.failure();
        }
    }
}

