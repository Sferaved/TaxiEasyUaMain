package com.taxi.easy.ua.utils.worker.utils;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.worker.GetCardTokenWfpWorker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.List;
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

    public interface CardFetchCallback {
        void onComplete(boolean success);
    }

    public static boolean isCityValidForCardFetch(String city) {
        return city != null && !city.isEmpty() && !"all".equalsIgnoreCase(city);
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
                cv.put("rectoken_check", normalizeActiveFlag(cardInfo.getActive()));
                database.insert(MainActivity.TABLE_WFP_CARDS, null, cv);
            }
        } finally {
            database.close();
        }
    }

    public static void enqueueCardTokenFetch(Context context, String city) {
        if (!isCityValidForCardFetch(city)) {
            Logger.d(context, TAG, "enqueueCardTokenFetch: skip invalid city: " + city);
            return;
        }
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(GetCardTokenWfpWorker.class)
                .setInputData(new Data.Builder().putString("city", city).build())
                .build();
        WorkManager.getInstance(context).enqueue(request);
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
        call.enqueue(new Callback<CallbackResponseWfp>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
                boolean success = response.isSuccessful() && response.body() != null;
                if (success) {
                    saveWfpCardsToDatabase(context, response.body().getCards());
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

        try {
            Response<CallbackResponseWfp> response = call.execute();
            Logger.d(context, TAG, "onResponse: " + response.body());
            if (response.isSuccessful() && response.body() != null) {
                List<CardInfo> cards = response.body().getCards();
                Logger.d(context, TAG, "onResponse: cards" + cards);
                saveWfpCardsToDatabase(context, cards);
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
