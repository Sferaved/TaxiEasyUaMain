package com.taxi.easy.ua.utils.user.del_server;

import android.util.Log;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private final ApiService apiService;
    private final String TAG = "UserRepository";

    public UserRepository() {
        apiService = RetrofitClient.getRetrofitInstance().create(ApiService.class);
    }

    public void destroyEmail(String email) {
        Call<ServerResponse> call = apiService.destroyEmail(email);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ServerResponse> call, @NonNull Response<ServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ServerResponse serverResponse = response.body();
                    if (serverResponse != null) {
                        if (serverResponse.getMessage() != null) {
                            // Успешный ответ
                            Log.d(TAG, "Success: " + serverResponse.getMessage());
                        } else {
                            // Ошибка на сервере
                            Log.d(TAG, "Error: " + serverResponse.getError());
                        }
                    }
                } else {
                    Log.d(TAG, "Request failed with code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ServerResponse> call, @NonNull Throwable t) {
                Log.d(TAG, "Failure: " + t.getMessage());
            }
        });
    }
}

