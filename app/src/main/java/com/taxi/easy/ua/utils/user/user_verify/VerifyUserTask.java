package com.taxi.easy.ua.utils.user.user_verify;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.cost_json_parser.CostJSONParserRetrofit;
import com.taxi.easy.ua.utils.log.Logger;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyUserTask {
    private static final String TAG = "VerifyUserTask";
    @SuppressLint("StaticFieldLeak")
    private final Context context;

    public VerifyUserTask(Context context) {
        this.context = context;
    }

    public void execute() {
        Logger.d(context, TAG, "execute() started");

        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, this.context).get(3);
        Logger.d(context, TAG, "Fetched user email: " + userEmail);

        String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        Logger.d(context, TAG, "Base URL: " + baseUrl);

        String url = baseUrl + "/android/verifyBlackListUser/" + userEmail + "/" + context.getString(R.string.application);
        Logger.d(context, TAG, "Constructed URL: " + url);

        CostJSONParserRetrofit parser = new CostJSONParserRetrofit();

        try {
            parser.sendURL(url, new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    Logger.d(context, TAG, "HTTP Response received: " + response.code());

                    if (response.isSuccessful() && response.body() != null) {
                        Logger.d(context, TAG, "Response body: " + response.body().toString());
                        Map<String, String> sendUrlMap = response.body();
                        onPostExecute(sendUrlMap);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    Logger.e(context, TAG, "Failed to fetch data: " + t.getMessage());
                }
            });
        } catch (MalformedURLException e) {
            Logger.e(context, TAG, "MalformedURLException: " + e.getMessage());
        }
    }


    protected void onPostExecute(Map<String, String> sendUrlMap) {
        if (sendUrlMap == null) {
            Logger.e(context, TAG, "Result is null");
            return;
        }

        String message = sendUrlMap.get("Message");
        ContentValues cv = new ContentValues();



        if (message != null && message.equals("В черном списке")) {
            cv.put("verifyOrder", "0");
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
        }

        if (message != null && !message.equals("В черном списке")) {
            cv.put("verifyOrder", "1");
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
        }
    }

    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                for (String cn : c.getColumnNames()) {
                    list.add(c.getString(c.getColumnIndex(cn)));
                }
            } while (c.moveToNext());
        }
        c.close();
        database.close();
        return list;
    }
}
