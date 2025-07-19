package com.taxi.easy.ua.utils.retrofit.worker.tasks;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.work.Data;
import androidx.work.Worker;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.retrofit.APIService;
import com.taxi.easy.ua.utils.retrofit.RetrofitClient;

import java.io.IOException;
import java.util.Map;

import retrofit2.Response;

public class CostRequestTask {

    private static final String TAG = "CostRequestTask";
    private final Data inputData;

    public CostRequestTask(Data inputData) {
        this.inputData = inputData;
    }

    public Worker.Result run() {
        Logger.d(MyApplication.getContext(), TAG, "Starting run()");

        // Проверка сети
        ConnectivityManager cm = (ConnectivityManager) MyApplication.getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            Logger.e(MyApplication.getContext(), TAG, "No network connection");
            return failure("No network connection");
        }

        String taskType = inputData.getString("taskType");
        if (taskType == null) {
            Logger.e(MyApplication.getContext(), TAG, "taskType missing");
            return failure("taskType missing");
        }
        Logger.d(MyApplication.getContext(), TAG, "taskType: " + taskType);

        String url = inputData.getString("url");
        Logger.d(MyApplication.getContext(), TAG, "Received url: " + url);

        String baseUrl = (String) MyApplication.sharedPreferencesHelperMain
                .getValue("baseUrl", "https://m.easy-order-taxi.site");
        Logger.d(MyApplication.getContext(), TAG, "baseUrl: " + baseUrl);

        if (url == null || url.isEmpty()) {
            Logger.e(MyApplication.getContext(), TAG, "Missing 'url' inputData");
            return failure("Missing 'url'");
        }

        try {
            Logger.d(MyApplication.getContext(), TAG, "Creating APIService and executing request");
            long startTime = System.currentTimeMillis();
            APIService api = RetrofitClient.getClient(baseUrl).create(APIService.class);
            Response<Map<String, String>> response = api.getData(url).execute();
            long duration = System.currentTimeMillis() - startTime;
            Logger.d(MyApplication.getContext(), TAG, "Request completed in: " + duration + " ms");

            if (response.isSuccessful() && response.body() != null) {
                Map<String, String> body = response.body();
                String cost = body.getOrDefault("order_cost", "0");
                String msg = body.getOrDefault("Message", "No message");

                Logger.d(MyApplication.getContext(), TAG, "Successful response: order_cost=" + cost + ", Message=" + msg);

                Data outputData = new Data.Builder()
                        .putString("order_cost", cost)
                        .putString("Message", msg)
                        .build();

                Logger.d(MyApplication.getContext(), TAG, "outputData: " + outputData.getKeyValueMap());
                return Worker.Result.success(outputData);
            } else {
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                String msg = "HTTP error " + response.code() + ", body: " + errorBody;
                Logger.e(MyApplication.getContext(), TAG, msg);
                return failure(msg);
            }

        } catch (IOException e) {
            Logger.e(MyApplication.getContext(), TAG, "IOException during cost fetch: " + e.getMessage());
            return failure("IOException: " + e.getMessage());
        } catch (Exception e) {
            Logger.e(MyApplication.getContext(), TAG, "Unhandled exception: " + e.getMessage());
            return failure("Exception: " + e.getMessage());
        }
    }

    private Worker.Result failure(String msg) {
        Logger.d(MyApplication.getContext(), TAG, "Returning error: " + msg);
        return Worker.Result.failure(new Data.Builder()
                .putString("error", msg)
                .build());
    }
}