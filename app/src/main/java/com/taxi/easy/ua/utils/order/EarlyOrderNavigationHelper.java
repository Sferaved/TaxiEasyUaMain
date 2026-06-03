package com.taxi.easy.ua.utils.order;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.navigation.NavOptions;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.utils.payment.PaymentSessionHelper;
import com.taxi.easy.ua.utils.db.CursorReadHelper;
import com.taxi.easy.ua.utils.route.RoutePlaceMatcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ранний переход на экран заказа по Centrifugo {@code order_uid_new}
 * (раньше полного ответа {@code orderClientCostMyApi}).
 */
public final class EarlyOrderNavigationHelper {

    private static final String TAG = "EarlyOrderNav";
    private static final String PREF_SUBMIT_IN_PROGRESS = "order_submit_in_progress";
    private static final String PREF_EARLY_NAV_DONE = "order_early_nav_done";
    private static final String PREF_EARLY_NAV_UID = "order_early_nav_uid";

    private EarlyOrderNavigationHelper() {
    }

    public static void markSubmitStarted(Context context, String payMethod, String displayCostGrivna) {
        if (context == null) {
            return;
        }
        sharedPreferencesHelperMain.saveValue(PREF_SUBMIT_IN_PROGRESS, true);
        sharedPreferencesHelperMain.saveValue(PREF_EARLY_NAV_DONE, false);
        sharedPreferencesHelperMain.saveValue(PREF_EARLY_NAV_UID, "");
        VisicomFragment.sendUrlMap = buildSnapshotMap(context, payMethod, displayCostGrivna);
        Logger.d(context, TAG, "markSubmitStarted pay=" + payMethod + " cost=" + displayCostGrivna);
    }

    public static void clearSubmitState() {
        sharedPreferencesHelperMain.saveValue(PREF_SUBMIT_IN_PROGRESS, false);
        sharedPreferencesHelperMain.saveValue(PREF_EARLY_NAV_DONE, false);
        sharedPreferencesHelperMain.saveValue(PREF_EARLY_NAV_UID, "");
    }

    public static boolean isSubmitInProgress() {
        Object v = sharedPreferencesHelperMain.getValue(PREF_SUBMIT_IN_PROGRESS, false);
        return v instanceof Boolean && (Boolean) v;
    }

    public static boolean isEarlyNavigationDone() {
        Object v = sharedPreferencesHelperMain.getValue(PREF_EARLY_NAV_DONE, false);
        return v instanceof Boolean && (Boolean) v;
    }

    /**
     * @return true если выполнен переход на {@link R.id#nav_finish_separate}
     */
    public static boolean tryEarlyNavigateToFinish(Context context, String orderUid, String paySystemStatus) {
        if (context == null || TextUtils.isEmpty(orderUid)) {
            return false;
        }
        if (!isSubmitInProgress() || isEarlyNavigationDone()) {
            return false;
        }
        if (MainActivity.navController == null) {
            Logger.w(context, TAG, "tryEarlyNavigate: navController null");
            return false;
        }
        if (MainActivity.currentNavDestination == R.id.nav_finish_separate) {
            markEarlyDone(orderUid);
            MainActivity.uid = orderUid;
            if (MainActivity.viewModel != null) {
                MainActivity.viewModel.updateUid(orderUid);
                if (!TextUtils.isEmpty(paySystemStatus)) {
                    MainActivity.viewModel.updatePaySystemStatus(paySystemStatus);
                }
                MainActivity.viewModel.setStatusNalUpdate(true);
            }
            return false;
        }

        Map<String, String> map = VisicomFragment.sendUrlMap;
        if (map == null) {
            map = buildSnapshotMap(context, resolvePayMethod(context), "");
        }
        map = new HashMap<>(map);
        map.put("dispatching_order_uid", orderUid);
        VisicomFragment.sendUrlMap = map;

        String payMethod = map.get("pay_method");
        if (TextUtils.isEmpty(payMethod)) {
            payMethod = resolvePayMethod(context);
            map.put("pay_method", payMethod);
        }

        String displayCost = map.get("order_cost");
        if (TextUtils.isEmpty(displayCost) || "0".equals(displayCost)) {
            Object persisted = sharedPreferencesHelperMain.getValue(
                    ExecutionStatusViewModel.PREF_FINISH_DISPLAY_COST, "");
            if (persisted instanceof String && !((String) persisted).isEmpty()) {
                displayCost = (String) persisted;
                map.put("order_cost", displayCost);
            }
        }

        Bundle bundle = buildFinishBundle(context, map, orderUid, displayCost, payMethod);
        if (bundle == null) {
            return false;
        }

        MainActivity.uid = orderUid;
        sharedPreferencesHelperMain.saveValue("uid_fcm", orderUid);
        sharedPreferencesHelperMain.saveValue("last_car_found_notify_uid", "");
        if (MainActivity.order_id != null && !MainActivity.order_id.isEmpty()) {
            PaymentSessionHelper.saveWfpOrderRef(orderUid, MainActivity.order_id);
        }
        ExecutionStatusViewModel.resetNewOrderSession(orderUid);
        persistDisplayCostGrivna(displayCost);

        if (MainActivity.viewModel != null) {
            MainActivity.viewModel.updateUid(orderUid);
            String payStatus = TextUtils.isEmpty(paySystemStatus) ? payMethod : paySystemStatus;
            MainActivity.viewModel.updatePaySystemStatus(payStatus);
            MainActivity.viewModel.setStatusNalUpdate(true);
        }

        markEarlyDone(orderUid);
        Logger.d(context, TAG, "early navigate uid=" + orderUid);

        MainActivity.navController.navigate(
                R.id.nav_finish_separate,
                bundle,
                new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build()
        );
        return true;
    }

    /** Дополняет карту заказа ответом HTTP, если UI уже ушёл с Visicom. */
    /**
     * UID заказа: из ответа HTTP, Centrifugo, ранней навигации или сохранённого uid_fcm.
     */
    public static String resolveOrderUid(Map<String, String> sendUrlMap) {
        if (sendUrlMap != null) {
            String uid = sendUrlMap.get("dispatching_order_uid");
            if (!TextUtils.isEmpty(uid)) {
                return uid;
            }
        }
        if (!TextUtils.isEmpty(MainActivity.uid)) {
            if (sendUrlMap != null) {
                sendUrlMap.put("dispatching_order_uid", MainActivity.uid);
            }
            return MainActivity.uid;
        }
        Object earlyUid = sharedPreferencesHelperMain.getValue(PREF_EARLY_NAV_UID, "");
        if (earlyUid instanceof String && !TextUtils.isEmpty((String) earlyUid)) {
            if (sendUrlMap != null) {
                sendUrlMap.put("dispatching_order_uid", (String) earlyUid);
            }
            return (String) earlyUid;
        }
        Object savedUid = sharedPreferencesHelperMain.getValue("uid_fcm", "");
        if (savedUid instanceof String && !TextUtils.isEmpty((String) savedUid)) {
            if (sendUrlMap != null) {
                sendUrlMap.put("dispatching_order_uid", (String) savedUid);
            }
            return (String) savedUid;
        }
        return null;
    }

    public static void applyHttpEnrichment(Context context, Map<String, String> sendUrlMap, String payMethod) {
        if (sendUrlMap == null) {
            return;
        }
        Map<String, String> merged = new HashMap<>();
        if (VisicomFragment.sendUrlMap != null) {
            merged.putAll(VisicomFragment.sendUrlMap);
        }
        merged.putAll(sendUrlMap);
        if (!TextUtils.isEmpty(payMethod)) {
            merged.put("pay_method", payMethod);
        }
        VisicomFragment.sendUrlMap = merged;

        String uid = merged.get("dispatching_order_uid");
        if (!TextUtils.isEmpty(uid)) {
            MainActivity.uid = uid;
            if (MainActivity.viewModel != null) {
                MainActivity.viewModel.updateUid(uid);
            }
            ExecutionStatusViewModel.resetNewOrderSession(uid);
        }
        String cost = merged.get("order_cost");
        persistDisplayCostGrivna(cost);
        Logger.d(context, TAG, "applyHttpEnrichment uid=" + uid);
    }

    private static void markEarlyDone(String orderUid) {
        sharedPreferencesHelperMain.saveValue(PREF_EARLY_NAV_DONE, true);
        sharedPreferencesHelperMain.saveValue(PREF_EARLY_NAV_UID, orderUid);
    }

    private static void persistDisplayCostGrivna(String costGrivna) {
        if (TextUtils.isEmpty(costGrivna) || "0".equals(costGrivna)) {
            return;
        }
        if (MainActivity.viewModel != null) {
            MainActivity.viewModel.persistDisplayCostGrivna(costGrivna);
        } else {
            sharedPreferencesHelperMain.saveValue(
                    ExecutionStatusViewModel.PREF_FINISH_DISPLAY_COST, costGrivna);
        }
    }

    private static HashMap<String, String> buildSnapshotMap(
            Context context,
            String payMethod,
            String displayCostGrivna
    ) {
        HashMap<String, String> map = new HashMap<>();
        List<String> route = logCursor(MainActivity.ROUT_MARKER, context);
        String routefrom = route.size() > 5 ? safe(route.get(5)) : "";
        String routeto = route.size() > 6 ? safe(route.get(6)) : "";
        String fromLat = route.size() > 1 ? safe(route.get(1)) : "0";
        String fromLng = route.size() > 2 ? safe(route.get(2)) : "0";
        String toLat = route.size() > 3 ? safe(route.get(3)) : "0";
        String toLng = route.size() > 4 ? safe(route.get(4)) : "0";

        if (routeto.isEmpty() || routefrom.equals(routeto)) {
            routeto = context.getString(R.string.on_city_tv);
            toLat = fromLat;
            toLng = fromLng;
        }

        map.put("routefrom", routefrom);
        map.put("routefromnumber", " ");
        map.put("routeto", routeto);
        map.put("to_number", " ");
        map.put("from_lat", fromLat);
        map.put("from_lng", fromLng);
        map.put("lat", toLat);
        map.put("lng", toLng);
        map.put("order_cost", TextUtils.isEmpty(displayCostGrivna) ? "0" : displayCostGrivna.trim());
        map.put("pay_method", payMethod);
        map.put("required_time", "");
        map.put("flexible_tariff_name", String.valueOf(sharedPreferencesHelperMain.getValue("tarif", " ")));
        map.put("comment_info", "");
        map.put("extra_charge_codes", "");
        map.put("currency", "UAH");
        map.put("message", "");
        return map;
    }

    private static Bundle buildFinishBundle(
            Context context,
            Map<String, String> map,
            String orderUid,
            String displayCost,
            String payMethod
    ) {
        String toNameLocal = resolveDestinationLabel(context, map);
        String routefrom = safe(map.get("routefrom"));
        String messageResult = routefrom + " " + context.getString(R.string.to_message) + toNameLocal + ".";
        messageResult = cleanString(messageResult);

        String payMethodMessage = buildPayMethodSuffix(context, payMethod);
        String messagePayment = safe(displayCost) + " " + context.getString(R.string.UAH) + " " + payMethodMessage;
        String messageFondy = context.getString(R.string.fondy_message) + " " + routefrom + " "
                + context.getString(R.string.to_message) + toNameLocal + ".";

        Bundle bundle = new Bundle();
        bundle.putString("messageResult_key", messageResult);
        bundle.putString("messagePay_key", messagePayment);
        bundle.putString("messageFondy_key", messageFondy);
        bundle.putString("messageCost_key", displayCost);
        bundle.putSerializable("sendUrlMap", new HashMap<>(map));
        bundle.putString("UID_key", orderUid);
        return bundle;
    }

    private static String resolveDestinationLabel(Context context, Map<String, String> map) {
        if (RoutePlaceMatcher.isCityRideOrder(map)) {
            return context.getString(R.string.on_city_tv);
        }
        String routeto = safe(map.get("routeto"));
        if ("Точка на карте".equals(routeto)) {
            return context.getString(R.string.end_point_marker);
        }
        String toName = routeto + " " + safe(map.get("to_number"));
        if (toName.contains("по місту") || toName.contains("по городу") || toName.contains("around the city")) {
            return context.getString(R.string.on_city_tv);
        }
        return toName.trim();
    }

    private static String buildPayMethodSuffix(Context context, String payMethod) {
        String base = context.getString(R.string.pay_method_message_main);
        if (payMethod == null) {
            return base + " " + context.getString(R.string.pay_method_message_nal);
        }
        switch (payMethod) {
            case "bonus_payment":
                return base + " " + context.getString(R.string.pay_method_message_bonus);
            case "card_payment":
            case "fondy_payment":
            case "mono_payment":
            case "wfp_payment":
                return base + " " + context.getString(R.string.pay_method_message_card);
            default:
                return base + " " + context.getString(R.string.pay_method_message_nal);
        }
    }

    private static String resolvePayMethod(Context context) {
        List<String> settings = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        return settings.size() > 4 ? safe(settings.get(4)) : "nal_payment";
    }

    private static String cleanString(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ").replaceAll("\\s{2,}$", " ");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static List<String> logCursor(String table, Context context) {
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
