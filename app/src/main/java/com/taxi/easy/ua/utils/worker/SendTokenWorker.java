package com.taxi.easy.ua.utils.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.messaging.FirebaseMessaging;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.utils.TokenUtils;

public class SendTokenWorker extends Worker {
    private static final String TAG = "SendTokenWorker";

    public SendTokenWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userEmail = getInputData().getString("userEmail");
        Log.e(TAG, "doWork: " + userEmail);
        try {

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                    // Optionally, send the token to your server
                    TokenUtils.sendToken(getApplicationContext(), userEmail, token);
                } else {
                    Log.e(TAG, "Failed to get FCM token", task.getException());
                }
            });


            return Result.success();
        } catch (Exception e) {
            Logger.e(MyApplication.getContext(), TAG, "Ошибка в sendToken: " + e.getMessage());
            return Result.failure();
        }
    }
}