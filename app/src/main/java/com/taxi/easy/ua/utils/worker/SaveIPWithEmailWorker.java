package com.taxi.easy.ua.utils.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.api.IPApiService;
import com.taxi.easy.ua.utils.worker.api.RetrofitClient;

import retrofit2.Call;
import retrofit2.Response;

public class SaveIPWithEmailWorker extends Worker {
    private static final String TAG = "AddUserNoNameWorker";

    public SaveIPWithEmailWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String emailUser = getInputData().getString("emailUser");
        String page = getInputData().getString("page");

        // Если page не передан, используем значение по умолчанию
        if (page == null) {
            page = "PAS4";
        }

        Logger.e(getApplicationContext(), TAG, "AddUserNoNameWorker started with emailUser: " + emailUser + ", page: " + page);

        try {
            boolean result = sendRequestWithRetrofit(page, emailUser);
            Logger.e(getApplicationContext(), TAG, "Work result: " + result);

            if (result) {
                return Result.success();
            } else {
                return Result.retry();
            }
        } catch (InterruptedException ie) {
            Logger.e(getApplicationContext(), TAG, "Work cancelled: " + ie.getMessage());
            return Result.failure();
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Ошибка: " + e.getMessage());
            return Result.failure();
        }
    }

    private boolean sendRequestWithRetrofit(String page, String email) throws Exception {
        IPApiService apiService = RetrofitClient.getApiService();
        Call<Void> call = apiService.saveIPWithEmail(page, email);

        Response<Void> response = call.execute();

        if (response.isSuccessful()) {
            Logger.e(getApplicationContext(), TAG, "Request successful - code: " + response.code());
            return true;
        } else {
            Logger.e(getApplicationContext(), TAG, "Request failed - code: " + response.code());
            return false;
        }
    }
}