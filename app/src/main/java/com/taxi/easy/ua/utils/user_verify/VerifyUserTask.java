package com.taxi.easy.ua.utils.user_verify;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;
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
    private final SQLiteDatabase database;

    public VerifyUserTask(Context context) {
        this.context = context;
        this.database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
    }

    public void execute() {
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, this.context).get(3);

        String url = "https://m.easy-order-taxi.site/android/verifyBlackListUser/" + userEmail + "/" + "com.taxi.easy.ua";
        CostJSONParserRetrofit parser = new CostJSONParserRetrofit();

        try {
            parser.sendURL(url, new Callback<Map<String, String>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, String> sendUrlMap = response.body();
                        onPostExecute(sendUrlMap);
                    } else {
                        onPostExecute(null);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    Logger.e(context, TAG, "Failed to fetch data: " + t.getMessage());
                    onPostExecute(null);
                }
            });
        } catch (MalformedURLException e) {
            Logger.e(context, TAG,"MalformedURLException: " + e.getMessage());
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
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
        }
        database.close();
    }

    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    for (String cn : c.getColumnNames()) {
                        list.add(c.getString(c.getColumnIndex(cn)));
                    }
                } while (c.moveToNext());
            }
            c.close();
        }
        database.close();
        return list;
    }
}
