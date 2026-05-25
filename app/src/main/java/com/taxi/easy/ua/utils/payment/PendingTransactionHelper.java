package com.taxi.easy.ua.utils.payment;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.taxi.easy.ua.MainActivity;

/**
 * Сохранение/чтение transactionStatus при UID mismatch (Centrifugo/Pusher).
 */
public final class PendingTransactionHelper {

    private static final String TAG = "PendingTransactionHelper";
    private static final String KEY_UID = "pending_transaction_uid";
    private static final String KEY_STATUS = "pending_transaction_status";
    private static final String KEY_TIME = "pending_transaction_time";
    private static final long MAX_AGE_MS = 24 * 60 * 60 * 1000L;

    private PendingTransactionHelper() {
    }

    public static void save(String uid, String status) {
        if (uid == null || uid.isEmpty() || status == null || status.isEmpty()) {
            return;
        }
        Log.d(TAG, "save pending uid=" + uid + " status=" + status);
        sharedPreferencesHelperMain.saveValue(KEY_UID, uid);
        sharedPreferencesHelperMain.saveValue(KEY_STATUS, status);
        sharedPreferencesHelperMain.saveValue(KEY_TIME, String.valueOf(System.currentTimeMillis()));
    }

    public static void clear() {
        sharedPreferencesHelperMain.saveValue(KEY_UID, "");
        sharedPreferencesHelperMain.saveValue(KEY_STATUS, "");
        sharedPreferencesHelperMain.saveValue(KEY_TIME, "0");
    }

    public static boolean hasPendingDeclined() {
        PendingData data = peekPending();
        return data != null && "Declined".equals(data.status);
    }

    /** Есть отложенный Declined для текущего заказа (без снятия — до показа шторки). */
    public static boolean hasPendingDeclinedForActiveOrder() {
        PendingData data = peekPending();
        return data != null && "Declined".equals(data.status) && data.matchesActiveOrder();
    }

    /** Снять отложенный Declined без UI (результат применит проверка checkStatus на финиш). */
    public static boolean takePendingDeclinedForActiveOrder() {
        PendingData data = peekPending();
        if (data == null || !"Declined".equals(data.status) || !data.matchesActiveOrder()) {
            return false;
        }
        clear();
        Log.d(TAG, "take pending Declined uid=" + data.uid);
        return true;
    }

    /**
     * Применить отложенный Declined: ViewModel + UI или push.
     *
     * @param showBottomSheet если не null и приложение на переднем плане — bottom sheet
     */
    public static void consumePendingDeclined(Context context, @Nullable Runnable showBottomSheet) {
        PendingData data = peekPending();
        if (data == null || !"Declined".equals(data.status)) {
            return;
        }
        if (!data.matchesActiveOrder()) {
            Log.d(TAG, "pending uid не совпадает с активным заказом, пропуск");
            return;
        }

        clear();
        Log.d(TAG, "consume pending Declined uid=" + data.uid);

        if (showBottomSheet != null && com.taxi.easy.ua.androidx.startup.MyApplication.isInForeground()) {
            PaymentDeclinedNotifier.prepareDeclinedOrderState();
            if (PaymentDeclinedNotifier.shouldShowSheetNow()
                    && PaymentErrorSheetHelper.beginShowAttempt()) {
                PaymentDeclinedNotifier.markSheetShown();
                showBottomSheet.run();
            }
        } else if (MainActivity.viewModel != null) {
            String current = MainActivity.viewModel.getTransactionStatus().getValue();
            if (!"Declined".equals(current)) {
                MainActivity.viewModel.setTransactionStatus("Declined");
            }
        } else if (!com.taxi.easy.ua.androidx.startup.MyApplication.isInForeground()) {
            PaymentDeclinedNotifier.maybeSendPaymentErrorPush(context, data.uid);
        }
    }

    @Nullable
    private static PendingData peekPending() {
        String uid = stringPref(KEY_UID);
        String status = stringPref(KEY_STATUS);
        String timeStr = stringPref(KEY_TIME);

        if (uid.isEmpty() || status.isEmpty()) {
            return null;
        }

        long timeMs = 0L;
        try {
            timeMs = Long.parseLong(timeStr);
        } catch (NumberFormatException ignored) {
        }
        if (timeMs > 0 && System.currentTimeMillis() - timeMs > MAX_AGE_MS) {
            Log.d(TAG, "pending expired, clear");
            clear();
            return null;
        }

        return new PendingData(uid, status);
    }

    private static String stringPref(String key) {
        Object value = sharedPreferencesHelperMain.getValue(key, "");
        return value != null ? String.valueOf(value) : "";
    }

    private static final class PendingData {
        final String uid;
        final String status;

        PendingData(String uid, String status) {
            this.uid = uid;
            this.status = status;
        }

        boolean matchesActiveOrder() {
            if (uid.equals(MainActivity.uid)) {
                return true;
            }
            if (MainActivity.viewModel != null && MainActivity.viewModel.getUid().getValue() != null) {
                return uid.equals(MainActivity.viewModel.getUid().getValue());
            }
            return false;
        }
    }
}
