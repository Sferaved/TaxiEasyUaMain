package com.taxi.easy.ua.utils.blacklist;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BlacklistManager {
    private static final String TAG = "BlacklistManager";
    private final BlacklistService blacklistService;

    public BlacklistManager() {
        // Initialize Retrofit and create an instance of the API interface
        blacklistService = RetrofitClientBlackList.getRetrofitInstance().create(BlacklistService.class);
    }

    // Method to add an email to the blacklist
    public void addToBlacklist(String email) {
        Call<Void> call = blacklistService.addToBlacklist(email);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Email added to blacklist successfully.");
                } else {
                    Log.d(TAG, "Failed to add email to blacklist. Response code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Error occurred: " + t.getMessage());
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }
}
