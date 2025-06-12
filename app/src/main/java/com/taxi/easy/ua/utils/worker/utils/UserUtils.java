package com.taxi.easy.ua.utils.worker.utils;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.user.save_server.ApiServiceUser;
import com.taxi.easy.ua.utils.user.save_server.UserResponse;
import com.taxi.easy.ua.utils.worker.AddUserNoNameWorker;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserUtils {
    private static final String TAG = "UserUtils";

    public static boolean addUserNoName(String email, Context context, AddUserNoNameWorker worker) throws Exception {
        String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        Logger.d(context, TAG, "Base URL: " + baseUrl + ", Email: " + email);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiServiceUser apiService = retrofit.create(ApiServiceUser.class);

        Call<UserResponse> call = apiService.addUserNoName(email, context.getString(R.string.application));
        Logger.d(context, TAG, "Sending request to add user with email: " + email);

        if (worker.isStopped()) {
            Logger.e(context, TAG, "Worker stopped before request");
            throw new InterruptedException("Work cancelled before request");
        }

        CountDownLatch latch = new CountDownLatch(1);
        final Exception[] requestException = {null};
        final String[] userName = {"no_name"};
        final boolean[] success = {false};

        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        userName[0] = response.body().getUserName();
                        Logger.d(context, TAG, "UserName received: " + userName[0]);
                        success[0] = true;
                    } else {
                        Logger.e(context, TAG, "Request failed: " + response.code() + ", " + response.message());
                        if (response.errorBody() != null) {
                            try {
                                Logger.e(context, TAG, "Error body: " + response.errorBody().string());
                            } catch (IOException e) {
                                Logger.e(context, TAG, "Error reading error body: " + e.toString());
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.e(context, TAG, "Exception in onResponse: " + e.toString());
                    requestException[0] = e;
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Logger.e(context, TAG, "Network error: " + t.toString());
                requestException[0] = new Exception("Network error", t);
                latch.countDown();
            }
        });

        boolean completed = latch.await(15, TimeUnit.SECONDS);
        if (!completed) {
            Logger.e(context, TAG, "Request timed out");
            throw new Exception("Request timed out");
        }

        if (requestException[0] != null) {
            throw requestException[0];
        }

        updateRecordsUserInfo("username", userName[0], context);
        return success[0];
    }

    public static void updateRecordsUserInfo(String userInfo, String result, Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        ContentValues cv = new ContentValues();

        cv.put(userInfo, result);

        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
        Logger.d(context, TAG, "Updated user info: " + userInfo + " = " + result);
    }
}