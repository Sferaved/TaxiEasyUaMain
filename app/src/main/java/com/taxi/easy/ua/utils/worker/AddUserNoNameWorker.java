package com.taxi.easy.ua.utils.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.utils.UserUtils;

public class AddUserNoNameWorker extends Worker {
    private static final String TAG = "AddUserNoNameWorker";

    public AddUserNoNameWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String emailUser = getInputData().getString("emailUser");
        Logger.e(getApplicationContext(), TAG, "AddUserNoNameWorker started with emailUser: " + emailUser);

        try {
            boolean result = UserUtils.addUserNoName(emailUser, getApplicationContext(), this);
            Logger.e(getApplicationContext(), TAG, "Work result: " + result);
            return Result.success();
        } catch (InterruptedException ie) {
            Logger.e(getApplicationContext(), TAG, "Work cancelled: " + ie.getMessage());
            return Result.failure();
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Ошибка в addUserNoName: " + e.getMessage());
            return Result.failure();
        }
    }
}
