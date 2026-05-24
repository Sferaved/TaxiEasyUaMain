package com.taxi.easy.ua.utils.payment;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import androidx.annotation.Nullable;

/**
 * WFP orderReference и флаг ошибки оплаты для активного заказа (переживает уход с финиша).
 */
public final class PaymentSessionHelper {

    private static final String KEY_PAY_ERROR = "pay_error";
    private static final String KEY_FAILED_ORDER_UID = "payment_failed_order_uid";
    private static final String PREFIX_WFP_REF = "wfp_order_ref_";

    private PaymentSessionHelper() {
    }

    public static void saveWfpOrderRef(@Nullable String uid, @Nullable String orderRef) {
        if (uid == null || uid.isEmpty() || orderRef == null || orderRef.isEmpty()) {
            return;
        }
        sharedPreferencesHelperMain.saveValue(PREFIX_WFP_REF + uid, orderRef);
    }

    @Nullable
    public static String getWfpOrderRef(@Nullable String uid) {
        if (uid == null || uid.isEmpty()) {
            return null;
        }
        Object value = sharedPreferencesHelperMain.getValue(PREFIX_WFP_REF + uid, "");
        String ref = value != null ? String.valueOf(value) : "";
        return ref.isEmpty() ? null : ref;
    }

    public static void markPaymentFailed() {
        sharedPreferencesHelperMain.saveValue(KEY_PAY_ERROR, "pay_error");
    }

    public static void markPaymentFailedForOrder(@Nullable String orderUid) {
        markPaymentFailed();
        if (orderUid != null && !orderUid.isEmpty()) {
            sharedPreferencesHelperMain.saveValue(KEY_FAILED_ORDER_UID, orderUid);
        }
    }

    public static void clearPaymentFailed() {
        sharedPreferencesHelperMain.saveValue(KEY_PAY_ERROR, "**");
        sharedPreferencesHelperMain.saveValue(KEY_FAILED_ORDER_UID, "");
    }

    public static void clearPaymentFailedForOrder(@Nullable String orderUid) {
        if (orderUid == null || orderUid.isEmpty()) {
            clearPaymentFailed();
            return;
        }
        String failedUid = String.valueOf(sharedPreferencesHelperMain.getValue(KEY_FAILED_ORDER_UID, ""));
        if (orderUid.equals(failedUid)) {
            clearPaymentFailed();
        }
    }

    public static boolean hasKnownPaymentFailure() {
        Object flag = sharedPreferencesHelperMain.getValue(KEY_PAY_ERROR, "**");
        return "pay_error".equals(String.valueOf(flag));
    }

    public static boolean hasPaymentFailedForOrder(@Nullable String orderUid) {
        if (orderUid == null || orderUid.isEmpty()) {
            return hasKnownPaymentFailure();
        }
        String failedUid = String.valueOf(sharedPreferencesHelperMain.getValue(KEY_FAILED_ORDER_UID, ""));
        return orderUid.equals(failedUid) || hasKnownPaymentFailure();
    }
}
