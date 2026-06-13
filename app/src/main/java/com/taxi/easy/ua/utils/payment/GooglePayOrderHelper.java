package com.taxi.easy.ua.utils.payment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.ui.wfp.googlepay.GooglePayChargeRequest;
import com.taxi.easy.ua.ui.wfp.googlepay.GooglePayChargeResponse;
import com.taxi.easy.ua.ui.wfp.googlepay.GooglePayChargeService;
import com.taxi.easy.ua.ui.wfp.googlepay.GooglePayConfigResponse;
import com.taxi.easy.ua.ui.wfp.googlepay.GooglePayConfigService;
import com.taxi.easy.ua.utils.log.Logger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Этап 2 GPay: config → charge hold на сервере до отправки заказа.
 */
public final class GooglePayOrderHelper {

    private static final String TAG = "GooglePayOrderHelper";
    private static final String PRODUCT_NAME = "Інша допоміжна діяльність у сфері транспорту";

    public interface ConfigCallback {
        void onSuccess(@NonNull String merchantAccount);

        void onError(@NonNull String message);
    }

    public interface ChargeCallback {
        void onHoldSuccess(@NonNull String orderReference);

        void onHoldFailed(@NonNull String message);
    }

    private GooglePayOrderHelper() {
    }

    public static boolean isHoldSuccess(@NonNull String transactionStatus) {
        return "Approved".equals(transactionStatus)
                || "WaitingAuthComplete".equals(transactionStatus);
    }

    public static void fetchMerchantConfig(
            @NonNull String baseUrl,
            @NonNull String application,
            @NonNull String city,
            @NonNull ConfigCallback callback
    ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GooglePayConfigService service = retrofit.create(GooglePayConfigService.class);
        service.getConfig(application, city).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<GooglePayConfigResponse> call,
                                   @NonNull Response<GooglePayConfigResponse> response) {
                GooglePayConfigResponse body = response.body();
                if (response.isSuccessful() && body != null
                        && body.getMerchantAccount() != null
                        && !body.getMerchantAccount().isEmpty()) {
                    callback.onSuccess(body.getMerchantAccount());
                    return;
                }
                String err = body != null && body.getError() != null
                        ? body.getError()
                        : "config_error";
                callback.onError(err);
            }

            @Override
            public void onFailure(@NonNull Call<GooglePayConfigResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                callback.onError(t.getMessage() != null ? t.getMessage() : "network_error");
            }
        });
    }

    public static void submitHoldCharge(
            @NonNull Context context,
            @NonNull String baseUrl,
            @NonNull String application,
            @NonNull String city,
            @NonNull String orderReference,
            int amountUah,
            @NonNull String clientEmail,
            @NonNull String clientPhone,
            @NonNull String paymentDataJson,
            @NonNull ChargeCallback callback
    ) {
        if (amountUah <= 0) {
            callback.onHoldFailed("invalid_amount");
            return;
        }
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(normalizeBaseUrl(baseUrl))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GooglePayChargeService service = retrofit.create(GooglePayChargeService.class);
        GooglePayChargeRequest request = new GooglePayChargeRequest(
                application,
                city,
                orderReference,
                amountUah,
                PRODUCT_NAME,
                clientEmail,
                clientPhone,
                paymentDataJson
        );
        service.charge(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<GooglePayChargeResponse> call,
                                   @NonNull Response<GooglePayChargeResponse> response) {
                GooglePayChargeResponse body = response.body();
                if (response.isSuccessful() && body != null) {
                    String status = body.getTransactionStatus();
                    Logger.d(context, TAG, "googlePayCharge status=" + status
                            + " ref=" + orderReference);
                    if (status != null && isHoldSuccess(status)) {
                        callback.onHoldSuccess(orderReference);
                        return;
                    }
                    callback.onHoldFailed(status != null ? status : "charge_declined");
                    return;
                }
                callback.onHoldFailed("charge_http_" + response.code());
            }

            @Override
            public void onFailure(@NonNull Call<GooglePayChargeResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                callback.onHoldFailed(t.getMessage() != null ? t.getMessage() : "network_error");
            }
        });
    }

    public static int parseAmountUah(@NonNull String costText) {
        String normalized = costText.trim().replace(',', '.');
        if (normalized.isEmpty()) {
            return 0;
        }
        try {
            return (int) Math.round(Double.parseDouble(normalized));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String normalizeBaseUrl(@NonNull String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
