package com.taxi.easy.ua.utils.retrofit.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.retrofit.worker.tasks.CostRequestTask;

public class RetrofitWorker extends Worker {

    public static final String KEY_TASK_TYPE = "taskType";
    public static final String KEY_ERROR = "error";
    private static final String TAG = "RetrofitWorker";

    public RetrofitWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String taskType = getInputData().getString(KEY_TASK_TYPE);
        if (taskType == null) {
            Logger.e(getApplicationContext(), TAG, "taskType missing");
            return failureWithMessage("taskType missing");
        }

        try {
            if ("costRequest".equals(taskType)) {
                Logger.d(getApplicationContext(), TAG, "Starting CostRequestTask");
                return new CostRequestTask(getInputData()).run();
            } else {
                Logger.e(getApplicationContext(), TAG, "Unknown taskType: " + taskType);
                return failureWithMessage("Unknown taskType: " + taskType);
            }
        } catch (Throwable t) {
            Logger.e(getApplicationContext(), TAG, "Exception in doWork: " + t.getMessage());
            return failureWithMessage("Exception: " + t.getMessage());
        }
    }

    private Result failureWithMessage(String message) {
        Logger.d(getApplicationContext(), TAG, "Returning failure: " + message);
        return Result.failure(new Data.Builder()
                .putString(KEY_ERROR, message)
                .build());
    }

    @Override
    public void onStopped() {
        super.onStopped();
        Logger.w(getApplicationContext(), TAG, "Worker stopped by system! InputData: " + getInputData().getKeyValueMap());
        MyApplication.sharedPreferencesHelperMain.saveValue("last_url", getInputData().getString("url"));
    }
}