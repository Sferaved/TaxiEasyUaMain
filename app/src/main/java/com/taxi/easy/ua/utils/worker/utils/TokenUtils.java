package com.taxi.easy.ua.utils.worker.utils;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.fcm.token_send.ApiServiceToken;
import com.taxi.easy.ua.utils.fcm.token_send.RetrofitClientToken;
import com.taxi.easy.ua.utils.helpers.InstallationIdHelper;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TokenUtils {
    private static final String TAG = "TokenUtils";
    private static final String KYIV_TZ = "Europe/Kyiv";

    public static void sendToken(Context context, String email, String token) {

        Logger.d(context, TAG, "sendToken email " + email);
        Logger.d(context, TAG, "sendToken token " + token);

        if (!email.isEmpty() && !email.equals("no_email")) {
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

    /**
     * Регистрируем установку (анонимно) — чтобы можно было прислать напоминание о входе.
     */
    public static void registerInstallationToken(Context context, String token) {
        try {
            String baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
            ApiServiceToken apiService = RetrofitClientToken.getClient(baseUrl).create(ApiServiceToken.class);
            String app = context.getString(R.string.application);
            String installationId = InstallationIdHelper.getOrCreateInstallationId();

            apiService.registerInstallation(installationId, app, token, LocaleHelper.getLocale(), KYIV_TZ)
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            Logger.d(context, TAG, "registerInstallation response.code: " + response.code());
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            Logger.e(context, TAG, "registerInstallation failed: " + t);
                            FirebaseCrashlytics.getInstance().recordException(t);
                        }
                    });
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /** Планируем одно напоминание на завтра 07:00 Europe/Kyiv (на сервере). */
    public static void scheduleLoginReminderIfNeeded(Context context) {
        try {
            String baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
            ApiServiceToken apiService = RetrofitClientToken.getClient(baseUrl).create(ApiServiceToken.class);
            String app = context.getString(R.string.application);
            String installationId = InstallationIdHelper.getOrCreateInstallationId();

            apiService.scheduleLoginReminder(installationId, app, LocaleHelper.getLocale(), KYIV_TZ)
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            Logger.d(context, TAG, "scheduleLoginReminder response.code: " + response.code());
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            Logger.e(context, TAG, "scheduleLoginReminder failed: " + t);
                            FirebaseCrashlytics.getInstance().recordException(t);
                        }
                    });
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /** Отменяем напоминание после успешного входа. */
    public static void cancelLoginReminder(Context context) {
        try {
            String baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
            ApiServiceToken apiService = RetrofitClientToken.getClient(baseUrl).create(ApiServiceToken.class);
            String app = context.getString(R.string.application);
            String installationId = InstallationIdHelper.getOrCreateInstallationId();

            apiService.cancelLoginReminder(installationId, app)
                    .enqueue(new Callback<>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            Logger.d(context, TAG, "cancelLoginReminder response.code: " + response.code());
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            Logger.e(context, TAG, "cancelLoginReminder failed: " + t);
                            FirebaseCrashlytics.getInstance().recordException(t);
                        }
                    });
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
}