package com.taxi.easy.ua.utils.payment;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.ui.finish.fragm.FinishSeparateFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorPaymentFragment;
import com.taxi.easy.ua.utils.db.CursorReadHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Показ шторки ошибки оплаты и статуса «скасовано: оплата не пройшла»
 * вне экрана опроса статуса (карта, профиль и т.д.).
 */
public final class PaymentDeclinedUiHelper {

    private static final String TAG = "PaymentDeclinedUi";

    private PaymentDeclinedUiHelper() {
    }

    /** Centrifugo / FCM / Pusher: Declined для uid заказа. */
    public static void handleDeclined(@Nullable Context context, @Nullable String orderUid) {
        if (context == null || TextUtils.isEmpty(orderUid)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        PaymentSessionHelper.markPaymentFailedForOrder(orderUid);
        PendingTransactionHelper.save(orderUid, "Declined");

        Runnable onMain = () -> {
            syncActiveOrderUid(orderUid);
            if (MainActivity.viewModel != null) {
                MainActivity.viewModel.setTransactionStatus("Declined");
            }
            tryPresentDeclinedUi(appContext, orderUid);
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onMain.run();
        } else {
            new Handler(Looper.getMainLooper()).post(onMain);
        }
    }

    /**
     * Шторка на финише (через checkStatus) или на текущем экране Activity.
     */
    public static void tryPresentDeclinedUi(@NonNull Context context, @Nullable String orderUid) {
        if (TextUtils.isEmpty(orderUid)) {
            orderUid = MainActivity.uid;
        }
        if (TextUtils.isEmpty(orderUid)) {
            return;
        }

        FragmentActivity activity = findFragmentActivity(context);
        if (activity == null) {
            if (!MyApplication.isInForeground()) {
                PaymentDeclinedNotifier.maybeSendPaymentErrorPush(context, orderUid);
            }
            return;
        }

        if (FinishSeparateFragment.dispatchDeclinedToFinish(activity)) {
            return;
        }

        if (!MyApplication.isInForeground()) {
            PaymentDeclinedNotifier.maybeSendPaymentErrorPush(context, orderUid);
            return;
        }

        PaymentDeclinedNotifier.prepareDeclinedOrderState();
        if (!PaymentDeclinedNotifier.shouldShowSheetNow()) {
            return;
        }
        if (!PaymentErrorSheetHelper.beginShowAttempt()) {
            return;
        }
        PaymentDeclinedNotifier.markSheetShown();
        PendingTransactionHelper.clear();
        showPaymentErrorSheet(activity, orderUid);
    }

    /** UID события оплаты относится к активному/ожидаемому заказу. */
    public static boolean isRelevantOrderUid(@Nullable String eventUid) {
        if (TextUtils.isEmpty(eventUid)) {
            return false;
        }
        if (eventUid.equals(MainActivity.uid)) {
            return true;
        }
        if (MainActivity.uid_Double != null && eventUid.equals(MainActivity.uid_Double)) {
            return true;
        }
        String persisted = ExecutionStatusViewModel.getPersistedActiveUid();
        if (eventUid.equals(persisted)) {
            return true;
        }
        String doubleUid = ExecutionStatusViewModel.getPersistedDoubleUid();
        if (eventUid.equals(doubleUid)) {
            return true;
        }
        Object uidFcm = sharedPreferencesHelperMain.getValue(ExecutionStatusViewModel.PREF_UID_FCM, "");
        if (eventUid.equals(String.valueOf(uidFcm))) {
            return true;
        }
        Object earlyUid = sharedPreferencesHelperMain.getValue("order_early_nav_uid", "");
        if (eventUid.equals(String.valueOf(earlyUid))) {
            return true;
        }
        String failedUid = String.valueOf(
                sharedPreferencesHelperMain.getValue("payment_failed_order_uid", ""));
        return eventUid.equals(failedUid);
    }

    /** Текст статуса заказа при отмене из‑за неоплаты. */
    @NonNull
    public static String canceledStatusMessage(@NonNull Context context, @Nullable String orderUid) {
        if (PaymentSessionHelper.hasPaymentFailedForOrder(orderUid)) {
            return context.getString(R.string.ex_st_canceled_payment_failed);
        }
        return context.getString(R.string.ex_st_canceled);
    }

    static void syncActiveOrderUid(@NonNull String orderUid) {
        MainActivity.uid = orderUid;
        sharedPreferencesHelperMain.saveValue(ExecutionStatusViewModel.PREF_UID_FCM, orderUid);
        if (MainActivity.viewModel != null) {
            MainActivity.viewModel.updateUid(orderUid);
        }
    }

    private static void showPaymentErrorSheet(@NonNull FragmentActivity activity, @NonNull String orderUid) {
        String payMethod = resolvePayMethod(activity);
        if ("nal_payment".equals(payMethod)) {
            payMethod = "wfp_payment";
        }
        String amount = resolveDisplayCost();
        String routeMessage = buildRouteMessage(activity);
        Logger.d(activity, TAG,
                "showPaymentErrorSheet uid=" + orderUid
                        + " pay=" + payMethod
                        + " amount=" + amount);
        MyBottomSheetErrorPaymentFragment sheet = new MyBottomSheetErrorPaymentFragment(
                payMethod,
                routeMessage,
                amount,
                activity
        );
        sheet.show(activity.getSupportFragmentManager(), PaymentErrorSheetHelper.SHEET_TAG);
    }

    @Nullable
    private static FragmentActivity findFragmentActivity(@NonNull Context context) {
        if (context instanceof FragmentActivity) {
            return (FragmentActivity) context;
        }
        Context app = MyApplication.getContext();
        if (app instanceof FragmentActivity) {
            return (FragmentActivity) app;
        }
        return null;
    }

    private static String resolvePayMethod(@NonNull Context context) {
        if (MainActivity.viewModel != null) {
            String fromVm = MainActivity.viewModel.getPaySystemStatus().getValue();
            if (!TextUtils.isEmpty(fromVm)) {
                return fromVm;
            }
        }
        if (!TextUtils.isEmpty(MainActivity.paySystemStatus)) {
            return MainActivity.paySystemStatus;
        }
        List<String> settings = readTable(MainActivity.TABLE_SETTINGS_INFO, context);
        return settings.size() > 4 ? safe(settings.get(4)) : "wfp_payment";
    }

    private static String resolveDisplayCost() {
        String cost = ExecutionStatusViewModel.getPersistedDisplayCost();
        if (!TextUtils.isEmpty(cost) && !"0".equals(cost)) {
            return cost;
        }
        Object stored = sharedPreferencesHelperMain.getValue("order_cost", "");
        return stored != null ? String.valueOf(stored).trim() : "0";
    }

    private static String buildRouteMessage(@NonNull Context context) {
        List<String> route = readTable(MainActivity.ROUT_MARKER, context);
        String from = route.size() > 5 ? safe(route.get(5)) : "";
        String to = route.size() > 6 ? safe(route.get(6)) : "";
        if (to.isEmpty() || from.equals(to)) {
            to = context.getString(R.string.on_city_tv);
        }
        if (from.isEmpty()) {
            return context.getString(R.string.pay_failure_mes);
        }
        return from + " -> " + to;
    }

    private static String safe(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> readTable(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                for (String cn : c.getColumnNames()) {
                    list.add(CursorReadHelper.getString(c, cn));
                }
            } while (c.moveToNext());
        }
        c.close();
        database.close();
        return list;
    }
}
