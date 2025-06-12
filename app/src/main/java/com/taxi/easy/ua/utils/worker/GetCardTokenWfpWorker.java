package com.taxi.easy.ua.utils.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.utils.WfpUtils;
public class GetCardTokenWfpWorker extends Worker {
    private static final String TAG = "GetCardTokenWfpWorker";

    public GetCardTokenWfpWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String city = getInputData().getString("city");
        Logger.e(getApplicationContext(), TAG, "doWork: " + city);
        if (city != null) {
            try {
                WfpUtils.getCardTokenWfp(city, getApplicationContext());
                return Result.success();
            } catch (Exception e) {
                Logger.e(getApplicationContext(), TAG, "Ошибка в getCardTokenWfp: " + e.getMessage());
                return Result.failure();
            }
        } else {
            Logger.e(getApplicationContext(), TAG, "City is null, пропуск getCardTokenWfp");
            return Result.success(); // Пропускаем без ошибок
        }
    }
}