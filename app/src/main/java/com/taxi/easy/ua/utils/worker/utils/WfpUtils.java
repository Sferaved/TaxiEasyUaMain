package com.taxi.easy.ua.utils.worker.utils;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseSetActivCardWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.worker.GetCardTokenWfpWorker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.taxi.easy.ua.utils.db.CursorReadHelper;

public class WfpUtils {

    private static final String TAG = "WfpUtils";

    public static final String CARD_TOKEN_FETCH_WORK = "wfp_card_token_fetch";

    public interface CardFetchCallback {
        void onComplete(boolean success);
    }

    public static boolean isCityValidForCardFetch(String city) {
        return city != null && !city.isEmpty() && !"all".equalsIgnoreCase(city);
    }

    public static final String PREF_USER_SELECTED_WFP_RECTOKEN = "user_selected_wfp_rectoken";

    public static void saveUserSelectedWfpRectoken(Context context, String rectoken) {
        if (rectoken == null || rectoken.isEmpty()) {
            return;
        }
        sharedPreferencesHelperMain.saveValue(PREF_USER_SELECTED_WFP_RECTOKEN, rectoken);
        String city = getCurrentCity(context);
        if (isCityValidForCardFetch(city)) {
            sharedPreferencesHelperMain.saveValue(cityScopedRectokenKey(city), rectoken);
        }
        Logger.d(context, TAG, "saveUserSelectedWfpRectoken: " + rectoken);
    }

    @NonNull
    private static String cityScopedRectokenKey(@NonNull String city) {
        return PREF_USER_SELECTED_WFP_RECTOKEN + "_" + city;
    }

    @NonNull
    public static String getCurrentCity(Context context) {
        List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);
        return cityInfo.size() >= 2 ? cityInfo.get(1) : "";
    }

    @NonNull
    public static String getCityScopedSelectedWfpRectoken(Context context) {
        String city = getCurrentCity(context);
        if (isCityValidForCardFetch(city)) {
            Object value = sharedPreferencesHelperMain.getValue(cityScopedRectokenKey(city), "");
            if (value != null && !value.toString().isEmpty()) {
                return value.toString();
            }
        }
        return getUserSelectedWfpRectoken(context);
    }

    @NonNull
    public static String getUserSelectedWfpRectoken(Context context) {
        Object value = sharedPreferencesHelperMain.getValue(PREF_USER_SELECTED_WFP_RECTOKEN, "");
        return value != null ? value.toString() : "";
    }

    private static String resolveRectokenCheckForSync(Context context, CardInfo cardInfo) {
        String selected = getCityScopedSelectedWfpRectoken(context);
        String rectoken = cardInfo.getRectoken();
        if (!selected.isEmpty() && selected.equals(rectoken)) {
            return "1";
        }
        if ("1".equals(normalizeActiveFlag(cardInfo.getActive()))) {
            return "1";
        }
        return "0";
    }

    public static String normalizeActiveFlag(String active) {
        if (active == null) {
            return "0";
        }
        if ("1".equals(active) || "true".equalsIgnoreCase(active)) {
            return "1";
        }
        return "0";
    }

    public static void saveWfpCardsToDatabase(Context context, List<CardInfo> cards) {
        saveWfpCardsToDatabase(context, cards, false);
    }

    public static void saveWfpCardsToDatabase(Context context, List<CardInfo> cards, boolean trustServerActiveForManualLink) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            database.execSQL("DELETE FROM " + MainActivity.TABLE_WFP_CARDS + ";");
            if (cards == null || cards.isEmpty()) {
                Logger.d(context, TAG, "saveWfpCardsToDatabase: no cards for current merchant");
                return;
            }
            for (CardInfo cardInfo : cards) {
                Logger.d(context, TAG, "onResponse: card_token: " + cardInfo.getRectoken());
                ContentValues cv = new ContentValues();
                cv.put("masked_card", cardInfo.getMasked_card());
                cv.put("card_type", cardInfo.getCard_type());
                cv.put("bank_name", cardInfo.getBank_name());
                cv.put("rectoken", cardInfo.getRectoken());
                cv.put("merchant", cardInfo.getMerchant());
                String rectokenCheck;
                if (trustServerActiveForManualLink) {
                    rectokenCheck = normalizeActiveFlag(cardInfo.getActive());
                    if ("1".equals(rectokenCheck)) {
                        saveUserSelectedWfpRectoken(context, cardInfo.getRectoken());
                    }
                } else {
                    rectokenCheck = resolveRectokenCheckForSync(context, cardInfo);
                }
                cv.put("rectoken_check", rectokenCheck);
                database.insert(MainActivity.TABLE_WFP_CARDS, null, cv);
            }
            ensureActiveWfpCardAfterSync(context, cards);
        } finally {
            database.close();
        }
    }

    /**
     * Активная карта для оплаты: checked в БД или авто-выбор единственной / сохранённой по городу.
     */
    @NonNull
    public static String resolveActiveWfpRectoken(Context context) {
        String checked = queryCheckedWfpRectoken(context);
        if (!checked.isEmpty()) {
            return checked;
        }
        ensureActiveWfpCardFromDb(context);
        return queryCheckedWfpRectoken(context);
    }

    @NonNull
    private static String queryCheckedWfpRectoken(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            Cursor cursor = database.query(
                    MainActivity.TABLE_WFP_CARDS,
                    new String[]{"rectoken"},
                    "rectoken_check = ?",
                    new String[]{"1"},
                    null, null, null);
            if (cursor.moveToFirst()) {
                String result = CursorReadHelper.getString(cursor, "rectoken");
                cursor.close();
                return result != null ? result : "";
            }
            cursor.close();
        } finally {
            database.close();
        }
        return "";
    }

    private static void ensureActiveWfpCardFromDb(Context context) {
        if (hasCheckedWfpCard(context)) {
            return;
        }
        ArrayList<String> rectokens = new ArrayList<>();
        ArrayList<String> checks = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            Cursor cursor = database.query(
                    MainActivity.TABLE_WFP_CARDS,
                    new String[]{"rectoken", "rectoken_check"},
                    null, null, null, null, null);
            while (cursor.moveToNext()) {
                rectokens.add(CursorReadHelper.getString(cursor, "rectoken"));
                checks.add(CursorReadHelper.getString(cursor, "rectoken_check"));
            }
            cursor.close();
        } finally {
            database.close();
        }
        if (rectokens.isEmpty()) {
            return;
        }
        String preferred = getCityScopedSelectedWfpRectoken(context);
        if (!preferred.isEmpty()) {
            for (String rectoken : rectokens) {
                if (preferred.equals(rectoken)) {
                    setWfpCardChecked(context, rectoken);
                    Logger.d(context, TAG, "ensureActiveWfpCardFromDb: preferred " + preferred);
                    return;
                }
            }
        }
        for (int i = 0; i < rectokens.size(); i++) {
            if ("1".equals(normalizeActiveFlag(checks.get(i)))) {
                String rectoken = rectokens.get(i);
                setWfpCardChecked(context, rectoken);
                saveUserSelectedWfpRectoken(context, rectoken);
                Logger.d(context, TAG, "ensureActiveWfpCardFromDb: server active " + rectoken);
                return;
            }
        }
        if (rectokens.size() == 1) {
            String only = rectokens.get(0);
            setWfpCardChecked(context, only);
            saveUserSelectedWfpRectoken(context, only);
            Logger.d(context, TAG, "ensureActiveWfpCardFromDb: single card " + only);
        }
    }

    private static boolean hasCheckedWfpCard(Context context) {
        return !queryCheckedWfpRectoken(context).isEmpty();
    }

    private static void setWfpCardChecked(Context context, String rectoken) {
        if (rectoken == null || rectoken.isEmpty()) {
            return;
        }
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            ContentValues clear = new ContentValues();
            clear.put("rectoken_check", "0");
            database.update(MainActivity.TABLE_WFP_CARDS, clear, null, null);
            ContentValues mark = new ContentValues();
            mark.put("rectoken_check", "1");
            database.update(MainActivity.TABLE_WFP_CARDS, mark, "rectoken=?", new String[]{rectoken});
        } finally {
            database.close();
        }
    }

    private static void ensureActiveWfpCardAfterSync(Context context, @Nullable List<CardInfo> cards) {
        if (cards == null || cards.isEmpty() || hasCheckedWfpCard(context)) {
            return;
        }
        String preferred = getCityScopedSelectedWfpRectoken(context);
        if (!preferred.isEmpty()) {
            for (CardInfo cardInfo : cards) {
                if (preferred.equals(cardInfo.getRectoken())) {
                    setWfpCardChecked(context, cardInfo.getRectoken());
                    Logger.d(context, TAG, "ensureActiveWfpCardAfterSync: preferred " + preferred);
                    return;
                }
            }
        }
        for (CardInfo cardInfo : cards) {
            if ("1".equals(normalizeActiveFlag(cardInfo.getActive()))) {
                setWfpCardChecked(context, cardInfo.getRectoken());
                saveUserSelectedWfpRectoken(context, cardInfo.getRectoken());
                Logger.d(context, TAG, "ensureActiveWfpCardAfterSync: server active " + cardInfo.getRectoken());
                return;
            }
        }
        if (cards.size() == 1) {
            String only = cards.get(0).getRectoken();
            setWfpCardChecked(context, only);
            saveUserSelectedWfpRectoken(context, only);
            Logger.d(context, TAG, "ensureActiveWfpCardAfterSync: single card " + only);
        }
    }

    public static void cancelCardTokenFetches(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(CARD_TOKEN_FETCH_WORK);
        Logger.d(context, TAG, "cancelCardTokenFetches");
    }

    public static void prepareForCardDeletion(Context context, String rectoken) {
        WfpCardSyncGuard.invalidatePendingFetches();
        cancelCardTokenFetches(context);
        removeCardFromDatabase(context, rectoken);
        Logger.d(context, TAG, "prepareForCardDeletion: rectoken=" + rectoken);
    }

    public static void removeCardFromDatabase(Context context, String rectoken) {
        if (rectoken == null || rectoken.isEmpty()) {
            return;
        }
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            database.delete(MainActivity.TABLE_WFP_CARDS, "rectoken=?", new String[]{rectoken});
        } finally {
            database.close();
        }
    }

    private static boolean applyFetchResultIfCurrent(Context context, List<CardInfo> cards, long fetchGeneration) {
        if (!WfpCardSyncGuard.shouldApplyFetchResult(fetchGeneration)) {
            Logger.d(context, TAG, "applyFetchResultIfCurrent: skip stale fetch gen=" + fetchGeneration);
            return false;
        }
        saveWfpCardsToDatabase(context, cards);
        return true;
    }

    public static void enqueueCardTokenFetch(Context context, String city) {
        if (!isCityValidForCardFetch(city)) {
            Logger.d(context, TAG, "enqueueCardTokenFetch: skip invalid city: " + city);
            return;
        }
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GetCardTokenWfpWorker.class)
                .setInputData(new Data.Builder().putString("city", city).build())
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                CARD_TOKEN_FETCH_WORK,
                ExistingWorkPolicy.REPLACE,
                request
        );
    }

    private static final ExecutorService CARD_SYNC_EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Синхронно выставить активную карту на сервере перед списанием по токену.
     * Только с фонового потока — на UI вызывать {@link #syncActiveCardBeforeOrderOffMain}.
     */
    public static boolean syncActiveCardBeforeOrder(Context context, String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return true;
        }
        List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);
        if (cityInfo.size() < 2 || !isCityValidForCardFetch(cityInfo.get(1))) {
            Logger.d(context, TAG, "syncActiveCardBeforeOrder: skip invalid city");
            return true;
        }
        String city = cityInfo.get(1);
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        try {
            CallbackServiceWfp service = createCallbackService(context);
            Response<CallbackResponseSetActivCardWfp> response = service.setActiveCard(
                    userEmail,
                    cardId,
                    city,
                    context.getString(R.string.application)
            ).execute();
            boolean ok = response.isSuccessful()
                    && response.body() != null
                    && "ok".equalsIgnoreCase(response.body().getResult());
            Logger.d(context, TAG, "syncActiveCardBeforeOrder: cardId=" + cardId + " ok=" + ok);
            return ok;
        } catch (Exception e) {
            Logger.w(context, TAG, "syncActiveCardBeforeOrder failed: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        }
    }

    /** Синхронизация активной карты в фоне; {@code onDone} — на главном потоке. */
    public static void syncActiveCardBeforeOrderOffMain(
            Context context,
            String cardId,
            @Nullable Runnable onDone) {
        CARD_SYNC_EXECUTOR.execute(() -> {
            syncActiveCardBeforeOrder(context.getApplicationContext(), cardId);
            if (onDone != null) {
                new Handler(Looper.getMainLooper()).post(onDone);
            }
        });
    }

    public static void fetchCardTokenWfpAsync(String city, Context context, @Nullable CardFetchCallback callback) {
        Logger.d(context, TAG, "fetchCardTokenWfpAsync: city=" + city);
        if (!isCityValidForCardFetch(city)) {
            Logger.d(context, TAG, "fetchCardTokenWfpAsync: skip invalid city");
            if (callback != null) {
                callback.onComplete(false);
            }
            return;
        }

        CallbackServiceWfp service = createCallbackService(context);
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        Call<CallbackResponseWfp> call = service.handleCallbackWfpCardsId(
                context.getString(R.string.application),
                city,
                userEmail,
                "wfp"
        );
        final long fetchGeneration = WfpCardSyncGuard.captureFetchGeneration();
        call.enqueue(new Callback<CallbackResponseWfp>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
                boolean success = response.isSuccessful() && response.body() != null;
                if (success) {
                    success = applyFetchResultIfCurrent(context, response.body().getCards(), fetchGeneration);
                } else {
                    Logger.d(context, TAG, "fetchCardTokenWfpAsync: not successful, code: " + response.code());
                }
                if (callback != null) {
                    callback.onComplete(success);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "fetchCardTokenWfpAsync: failure " + t.getMessage());
                FirebaseCrashlytics.getInstance().recordException(t);
                if (callback != null) {
                    callback.onComplete(false);
                }
            }
        });
    }

    public static void getCardTokenWfp(String city, Context context) {
        Logger.d(context, TAG, "getCardTokenWfp: city=" + city);
        if (!isCityValidForCardFetch(city)) {
            Logger.d(context, TAG, "getCardTokenWfp: skip invalid city");
            return;
        }

        CallbackServiceWfp service = createCallbackService(context);
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        Call<CallbackResponseWfp> call = service.handleCallbackWfpCardsId(
                context.getString(R.string.application),
                city,
                userEmail,
                "wfp"
        );

        final long fetchGeneration = WfpCardSyncGuard.captureFetchGeneration();
        try {
            Response<CallbackResponseWfp> response = call.execute();
            Logger.d(context, TAG, "onResponse: " + response.body());
            if (response.isSuccessful() && response.body() != null) {
                List<CardInfo> cards = response.body().getCards();
                Logger.d(context, TAG, "onResponse: cards" + cards);
                applyFetchResultIfCurrent(context, cards, fetchGeneration);
            } else {
                Logger.d(context, TAG, "onResponse: not successful, code: " + response.code());
            }
        } catch (Exception e) {
            Logger.d(context, TAG, "onResponse: failure " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private static CallbackServiceWfp createCallbackService(Context context) {
        String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        return retrofit.create(CallbackServiceWfp.class);
    }


    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = db.query(table, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            String str;
            do {
                str = "";
                for (String cn : c.getColumnNames()) {
                    str = str.concat(cn + " = " + CursorReadHelper.getString(c, cn) + "; ");
                    list.add(CursorReadHelper.getString(c, cn));

                }

            } while (c.moveToNext());
        }
        db.close();
        return list;
    }

}
