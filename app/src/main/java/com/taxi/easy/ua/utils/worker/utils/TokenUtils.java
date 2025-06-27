package com.taxi.easy.ua.utils.worker.utils;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.fcm.token_send.ApiServiceToken;
import com.taxi.easy.ua.utils.fcm.token_send.RetrofitClientToken;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TokenUtils {
    private static final String TAG = "TokenUtils";

    public static void sendToken(Context context, String email, String token) {
        Logger.d(context, TAG, "sendToken email " + email);
        Logger.d(context, TAG, "sendToken token " + token);

        if (!token.isEmpty()) {
            String baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
            ApiServiceToken apiService = RetrofitClientToken.getClient(baseUrl).create(ApiServiceToken.class);
            String app = context.getString(R.string.application);

            Call<Void> call = apiService.sendToken(email, app, token, LocaleHelper.getLocale());
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    Logger.d(context, TAG, "response.code: " + response.code());
                    if (!response.isSuccessful()) {
                        try {
                            Logger.e(context, TAG, "Сервер вернул ошибку: " + response.code() + ", " + response.message());
                            Logger.e(context, TAG, "Тело ошибки: " + (response.errorBody() != null ? response.errorBody().string() : "Нет тела ошибки"));
                        } catch (IOException e) {
                            Logger.e(context, TAG, "Ошибка чтения тела ошибки: " + e.toString());
                            FirebaseCrashlytics.getInstance().recordException(e);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Logger.e(context, TAG, "Ошибка отправки токена на сервер: " + t.toString());
                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });
        } else {
            Logger.e(context, TAG, "Токен пустой, запрос не отправлен");
        }
    }
}