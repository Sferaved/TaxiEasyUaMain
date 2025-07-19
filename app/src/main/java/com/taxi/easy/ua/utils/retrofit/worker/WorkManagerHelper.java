package com.taxi.easy.ua.utils.retrofit.worker;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;

import java.util.concurrent.TimeUnit;

public class WorkManagerHelper {

    public static OneTimeWorkRequest scheduleCostRequest(Context context, String url) {
        Data inputData = new Data.Builder()
                .putString(RetrofitWorker.KEY_TASK_TYPE, "costRequest")
                .putString("url", url)
                .build();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        return new OneTimeWorkRequest.Builder(RetrofitWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
    }
}
