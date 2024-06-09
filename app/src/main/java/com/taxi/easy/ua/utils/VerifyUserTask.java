package com.taxi.easy.ua.utils;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;

import java.util.Map;

public class VerifyUserTask extends AsyncTask<Void, Void, Map<String, String>> {
    private Exception exception;
    private String userEmail;
    private String application;
    private Context context;

    public VerifyUserTask(String userEmail, String application, Context context) {
        this.userEmail = userEmail;
        this.application = application;
        this.context = context;
    }

    @Override
    protected Map<String, String> doInBackground(Void... voids) {

        String url = "https://m.easy-order-taxi.site/android/verifyBlackListUser/" + userEmail + "/" + application;
        try {
            return CostJSONParser.sendURL(url);
        } catch (Exception e) {
            exception = e;
//                Toast.makeText(getApplicationContext(), getApplicationContext().getString(verify_internet), Toast.LENGTH_SHORT).show();
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            return null;
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onPostExecute(Map<String, String> sendUrlMap) {
        String message = sendUrlMap.get("Message");
        ContentValues cv = new ContentValues();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        if (message != null) {

            if (message.equals("В черном списке")) {

                cv.put("verifyOrder", "0");
                database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            } else {
                MainActivity.versionServer = message;
                //                        version(message);

                cv.put("verifyOrder", "1");
                database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});

            }
        }
        database.close();
    }
}
