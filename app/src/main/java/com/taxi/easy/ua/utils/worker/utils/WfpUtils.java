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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WfpUtils {

    private static final String TAG = "WfpUtils";

    public static void getCardTokenWfp(String city, Context context) {
        Logger.d(context, TAG, "getCardTokenWfp: ");
        String  baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");


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

        CallbackServiceWfp service = retrofit.create(CallbackServiceWfp.class);

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
                CallbackResponseWfp callbackResponse = response.body();
                List<CardInfo> cards = callbackResponse.getCards();
                Logger.d(context, TAG, "onResponse: cards" + cards);

                SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.execSQL("DELETE FROM " + MainActivity.TABLE_WFP_CARDS + ";");

                if (cards != null && !cards.isEmpty()) {
                    for (CardInfo cardInfo : cards) {
                        String masked_card = cardInfo.getMasked_card();
                        String card_type = cardInfo.getCard_type();
                        String bank_name = cardInfo.getBank_name();
                        String rectoken = cardInfo.getRectoken();
                        String merchant = cardInfo.getMerchant();
                        String active = cardInfo.getActive();

                        Logger.d(context, TAG, "onResponse: card_token: " + rectoken);
                        ContentValues cv = new ContentValues();
                        cv.put("masked_card", masked_card);
                        cv.put("card_type", card_type);
                        cv.put("bank_name", bank_name);
                        cv.put("rectoken", rectoken);
                        cv.put("merchant", merchant);
                        cv.put("rectoken_check", active);
                        database.insert(MainActivity.TABLE_WFP_CARDS, null, cv);
                    }
                }
                database.close();
            } else {
                // Обработка случаев, когда ответ не 200 OK
                Logger.d(context, TAG, "onResponse: not successful, code: " + response.code());
            }
        } catch (Exception e) {
            Logger.d(context, TAG, "onResponse: failure " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        }
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
                    str = str.concat(cn + " = " + c.getString(c.getColumnIndex(cn)) + "; ");
                    list.add(c.getString(c.getColumnIndex(cn)));

                }

            } while (c.moveToNext());
        }
        db.close();
        return list;
    }

}
