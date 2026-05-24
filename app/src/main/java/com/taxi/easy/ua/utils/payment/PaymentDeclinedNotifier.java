package com.taxi.easy.ua.utils.payment;



import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;



import android.content.Context;

import android.util.Log;



import androidx.annotation.Nullable;



import com.taxi.easy.ua.MainActivity;

import com.taxi.easy.ua.R;

import com.taxi.easy.ua.utils.notify.NotificationHelper;



/**

 * Declined: push — один раз на uid заказа; шторка — при каждом заходе на финиш (короткий in-memory dedupe).

 */

public final class PaymentDeclinedNotifier {



    private static final String TAG = "PaymentDeclinedNotifier";

    /** Один FCM/local push на uid (не путать с показом шторки). */

    private static final String KEY_PUSH_SENT_UID = "payment_error_push_uid";

    /** Старый ключ — сбрасываем, чтобы не блокировать шторку после обновления. */

    private static final String KEY_LEGACY_DECLINED = "declined_invoice";



    private static final long SHEET_DEBOUNCE_MS = 1500L;



    private static long lastSheetShownMs;

    private static String lastSheetOrderRef = "";



    private PaymentDeclinedNotifier() {

    }



    static {

        clearLegacyDeclinedBlock();

    }



    private static void clearLegacyDeclinedBlock() {

        Object legacy = sharedPreferencesHelperMain.getValue(KEY_LEGACY_DECLINED, "**");

        if (legacy != null && !"**".equals(String.valueOf(legacy))) {

            sharedPreferencesHelperMain.saveValue(KEY_LEGACY_DECLINED, "**");

        }

    }



    public static String activeOrderRef() {

        if (MainActivity.uid != null && !MainActivity.uid.isEmpty()) {

            return MainActivity.uid;

        }

        if (MainActivity.order_id != null && !MainActivity.order_id.isEmpty()) {

            return MainActivity.order_id;

        }

        if (MainActivity.invoiceId != null && !MainActivity.invoiceId.isEmpty()) {

            return MainActivity.invoiceId;

        }

        return "";

    }



    public static void prepareDeclinedOrderState() {

        sharedPreferencesHelperMain.saveValue("add_show_flag", false);

        if (MainActivity.viewModel != null) {

            MainActivity.viewModel.setCancelStatus(true);

        }

    }



    /** Короткий debounce только от двойного события в одну секунду (Centrifugo + WFP). */

    public static boolean shouldShowSheetNow() {

        String ref = activeOrderRef();

        long now = System.currentTimeMillis();

        if (!ref.isEmpty()

                && ref.equals(lastSheetOrderRef)

                && (now - lastSheetShownMs) < SHEET_DEBOUNCE_MS) {

            return false;

        }

        return true;

    }



    public static void markSheetShown() {

        lastSheetShownMs = System.currentTimeMillis();

        lastSheetOrderRef = activeOrderRef();

    }

    public static void clearSheetDebounce() {

        lastSheetShownMs = 0L;

        lastSheetOrderRef = "";

    }

    public static boolean shouldSendPush(@Nullable String orderUid) {

        String ref = (orderUid != null && !orderUid.isEmpty()) ? orderUid : activeOrderRef();

        if (ref.isEmpty()) {

            return true;

        }

        String sent = String.valueOf(sharedPreferencesHelperMain.getValue(KEY_PUSH_SENT_UID, ""));

        return !ref.equals(sent);

    }



    public static void markPushSent(@Nullable String orderUid) {

        String ref = (orderUid != null && !orderUid.isEmpty()) ? orderUid : activeOrderRef();

        if (!ref.isEmpty()) {

            sharedPreferencesHelperMain.saveValue(KEY_PUSH_SENT_UID, ref);

        }

    }



    public static void resetPushDedupeForNewOrder() {

        sharedPreferencesHelperMain.saveValue(KEY_PUSH_SENT_UID, "");

        lastSheetOrderRef = "";

        lastSheetShownMs = 0L;

    }



    /**

     * Локальное уведомление об ошибке оплаты (не чаще одного раза на uid).

     */

    public static void maybeSendPaymentErrorPush(Context context, @Nullable String orderUid) {

        if (context == null) {

            return;

        }

        if (!shouldSendPush(orderUid)) {

            Log.d(TAG, "push skipped (already sent) uid=" + orderUid);

            return;

        }

        markPushSent(orderUid);

        showPaymentErrorNotification(context);

    }



    public static void showPaymentErrorNotification(Context context) {

        NotificationHelper.sendPaymentErrorNotification(

                context,

                context.getString(R.string.paymentErrMes),

                context.getString(R.string.pay_failure_mes)

        );

    }



    /** @deprecated используйте {@link #maybeSendPaymentErrorPush} / шторку на финиш */

    @Deprecated

    public static boolean shouldHandleDeclined() {

        return shouldShowSheetNow();

    }



    /** @deprecated */

    @Deprecated

    public static void markDeclinedHandled() {

        markSheetShown();

    }



    /** @deprecated — только для совместимости; предпочтительно presentDeclinedUiOnFinish */

    public static void notifyDeclined(Context context, @Nullable Runnable showBottomSheet) {

        prepareDeclinedOrderState();

        if (showBottomSheet != null && shouldShowSheetNow()) {

            markSheetShown();

            showBottomSheet.run();

        }

    }

}


