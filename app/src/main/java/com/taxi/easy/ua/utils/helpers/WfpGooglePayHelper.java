package com.taxi.easy.ua.utils.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.button.ButtonConstants;
import com.google.android.gms.wallet.button.ButtonOptions;
import com.google.android.gms.wallet.button.PayButton;
import com.taxi.easy.ua.BuildConfig;
import com.taxi.easy.ua.utils.log.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class WfpGooglePayHelper {

    private static final String TAG = "WfpGooglePayHelper";
    private static final String GATEWAY = "wayforpay";

    public interface ReadyCallback {
        void onReady(boolean ready);
    }

    public interface PaymentResultCallback {
        void onSuccess(@NonNull String paymentDataJson);

        void onCancelled();

        void onError(@NonNull String message);
    }

    private WfpGooglePayHelper() {
    }

    /** TEST on debug builds and emulators — Google test card suite, no live charge. */
    public static boolean usesTestEnvironment() {
        return BuildConfig.DEBUG || isEmulator();
    }

    public static PaymentsClient createPaymentsClient(@NonNull Fragment fragment) {
        return createPaymentsClient(fragment.requireContext());
    }

    public static PaymentsClient createPaymentsClient(@NonNull Context context) {
        int environment = usesTestEnvironment()
                ? WalletConstants.ENVIRONMENT_TEST
                : WalletConstants.ENVIRONMENT_PRODUCTION;
        Logger.d(context, TAG, "Google Pay environment: "
                + (environment == WalletConstants.ENVIRONMENT_TEST ? "TEST" : "PRODUCTION"));
        return Wallet.getPaymentsClient(
                context,
                new Wallet.WalletOptions.Builder()
                        .setEnvironment(environment)
                        .build()
        );
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static void initializePayButton(@NonNull PayButton payButton) {
        try {
            JSONArray paymentMethods = new JSONArray().put(baseCardPaymentMethod());
            payButton.initialize(
                    ButtonOptions.newBuilder()
                            .setButtonTheme(ButtonConstants.ButtonTheme.DARK)
                            .setButtonType(ButtonConstants.ButtonType.PLAIN)
                            .setCornerRadius(12)
                            .setAllowedPaymentMethods(paymentMethods.toString())
                            .build()
            );
        } catch (JSONException e) {
            Logger.e(payButton.getContext(), TAG, "PayButton init failed: " + e.getMessage());
        }
    }

    public static void checkReady(
            @NonNull PaymentsClient paymentsClient,
            @NonNull ReadyCallback callback
    ) {
        try {
            IsReadyToPayRequest request = IsReadyToPayRequest.fromJson(
                    new JSONObject()
                            .put("apiVersion", 2)
                            .put("apiVersionMinor", 0)
                            .put("allowedPaymentMethods", new JSONArray().put(baseCardPaymentMethod()))
                            .toString()
            );
            Task<Boolean> task = paymentsClient.isReadyToPay(request);
            task.addOnCompleteListener(completed -> {
                if (completed.isSuccessful() && Boolean.TRUE.equals(completed.getResult())) {
                    callback.onReady(true);
                } else {
                    callback.onReady(false);
                }
            });
        } catch (JSONException e) {
            callback.onReady(false);
        }
    }

    public static void requestPayment(
            @NonNull Fragment fragment,
            @NonNull PaymentsClient paymentsClient,
            @NonNull String merchantAccount,
            @NonNull String amountUah,
            @NonNull ActivityResultLauncher<IntentSenderRequest> launcher,
            @NonNull PaymentResultCallback callback
    ) {
        try {
            PaymentDataRequest request = PaymentDataRequest.fromJson(
                    buildPaymentDataRequest(merchantAccount, amountUah).toString()
            );
            paymentsClient.loadPaymentData(request).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    PaymentData paymentData = task.getResult();
                    if (paymentData != null) {
                        callback.onSuccess(paymentData.toJson());
                    } else {
                        callback.onError("empty_payment_data");
                    }
                    return;
                }
                Exception exception = task.getException();
                if (exception instanceof ResolvableApiException resolvable) {
                    try {
                        launcher.launch(new IntentSenderRequest.Builder(
                                resolvable.getResolution()).build());
                    } catch (Exception e) {
                        callback.onError(e.getMessage() != null ? e.getMessage() : "launcher_error");
                    }
                } else {
                    String message = exception != null ? exception.getMessage() : "google_pay_error";
                    Logger.e(fragment.requireContext(), TAG, "loadPaymentData failed: " + message);
                    callback.onError(message != null ? message : "google_pay_error");
                }
            });
        } catch (JSONException e) {
            callback.onError(e.getMessage() != null ? e.getMessage() : "json_error");
        }
    }

    public static void handlePaymentResult(
            int resultCode,
            @Nullable Intent data,
            @NonNull android.content.Context context,
            @NonNull PaymentResultCallback callback
    ) {
        if (resultCode == Activity.RESULT_OK) {
            PaymentData paymentData = PaymentData.getFromIntent(data);
            if (paymentData == null) {
                callback.onError("empty_payment_data");
                return;
            }
            callback.onSuccess(paymentData.toJson());
        } else {
            callback.onCancelled();
        }
    }

    private static JSONObject buildPaymentDataRequest(String merchantAccount, String amountUah) throws JSONException {
        return new JSONObject()
                .put("apiVersion", 2)
                .put("apiVersionMinor", 0)
                .put("allowedPaymentMethods", new JSONArray().put(cardPaymentMethod(merchantAccount)))
                .put("transactionInfo", new JSONObject()
                        .put("totalPrice", amountUah)
                        .put("totalPriceStatus", "FINAL")
                        .put("countryCode", "UA")
                        .put("currencyCode", "UAH"))
                .put("merchantInfo", new JSONObject().put("merchantName", "Easy Taxi"));
    }

    private static JSONObject baseCardPaymentMethod() throws JSONException {
        return new JSONObject()
                .put("type", "CARD")
                .put("parameters", new JSONObject()
                        .put("allowedAuthMethods", new JSONArray()
                                .put("PAN_ONLY")
                                .put("CRYPTOGRAM_3DS"))
                        .put("allowedCardNetworks", new JSONArray()
                                .put("MASTERCARD")
                                .put("VISA")));
    }

    private static JSONObject cardPaymentMethod(String merchantAccount) throws JSONException {
        return new JSONObject()
                .put("type", "CARD")
                .put("parameters", new JSONObject()
                        .put("allowedAuthMethods", new JSONArray()
                                .put("PAN_ONLY")
                                .put("CRYPTOGRAM_3DS"))
                        .put("allowedCardNetworks", new JSONArray()
                                .put("MASTERCARD")
                                .put("VISA")))
                .put("tokenizationSpecification", new JSONObject()
                        .put("type", "PAYMENT_GATEWAY")
                        .put("parameters", new JSONObject()
                                .put("gateway", GATEWAY)
                                .put("gatewayMerchantId", merchantAccount)));
    }
}
