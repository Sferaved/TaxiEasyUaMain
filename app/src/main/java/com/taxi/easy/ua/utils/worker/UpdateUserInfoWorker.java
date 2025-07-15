package com.taxi.easy.ua.utils.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.utils.worker.utils.UserUtils;

public class UpdateUserInfoWorker extends Worker {
    private static final String TAG = "UpdateUserInfoWorker";

    public UpdateUserInfoWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String emailUser = getInputData().getString("emailUser");
        try {
            UserUtils.updateRecordsUserInfo("email", emailUser, getApplicationContext());


            Log.d(TAG, "settingsNewUser: " + emailUser);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка в updateUserInfo: " + e.getMessage());
            return Result.failure();
        }
    }
}