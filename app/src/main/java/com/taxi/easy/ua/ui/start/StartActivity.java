package com.taxi.easy.ua.ui.start;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Exchanger;

import javax.net.ssl.HttpsURLConnection;

public class StartActivity extends Activity {
    private static final String DB_NAME = "data_1112234568_taxi_122";
    public static final String TABLE_USER_INFO = "userInfo";
    public static final String TABLE_SETTINGS_INFO = "settingsInfo";
    public static final String TABLE_ORDERS_INFO = "ordersInfo";

    public static SQLiteDatabase database;
    public static Cursor cursorDb;
    static FloatingActionButton fab;
    private String from, to;
    public String region =  "Одеса";
    EditText from_number, to_number;
    String messageResult;
    Button btn_again;

    private static final int READ_PHONE_NUMBERS_CODE = 0;
    private static final int READ_PHONE_STATE_CODE = 0;
    public static final int READ_CALL_PHONE = 0;

    Intent intent;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);
    }

    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onResume() {
        super.onResume();
        checkPermission(Manifest.permission.CALL_PHONE,READ_CALL_PHONE);
        checkPermission(Manifest.permission.READ_PHONE_NUMBERS, READ_PHONE_NUMBERS_CODE);
        checkPermission(Manifest.permission.READ_PHONE_STATE, READ_PHONE_STATE_CODE);

            fab = findViewById(R.id.fab);
            btn_again = findViewById(R.id.btn_again);

            intent = new Intent(this, MainActivity.class);

        try {
            initDB();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


       fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:0934066749"));
                    if (ActivityCompat.checkSelfPermission(StartActivity.this,
                            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        checkPermission(Manifest.permission.CALL_PHONE, StartActivity.READ_CALL_PHONE);
                    }
                    startActivity(intent);
                }
            });


       btn_again.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               finish();
               intent = new Intent(StartActivity.this, StartActivity.class);
               startActivity(intent);
           }
       });

       if(!hasConnection()) {
           Toast.makeText(StartActivity.this, "Перевірте інтернет-підключення або зателефонуйте оператору.", Toast.LENGTH_LONG).show();
       } else {
           startActivity(intent);
           Toast.makeText(StartActivity.this, "Ласкаво просимо. Сформуйте маршрут або виберіть улюблений.", Toast.LENGTH_LONG).show();
         Log.d("TAG", "onResume: "  + hasConnection());

       }


    }
    public boolean hasConnection() {
        ConnectivityManager cm = (ConnectivityManager) StartActivity.this.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            return true;
        }
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            return true;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            return true;
        }
        return false;
    }

    public static String verifyConnection(String urlString) throws MalformedURLException, InterruptedException {

        URL url = new URL(urlString);
        final String TAG = "TAG";

        Exchanger<String> exchanger = new Exchanger<>();

        AsyncTask.execute(() -> {
            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                if (urlConnection.getResponseCode() == 200) {

                    StringBuffer buffer = new StringBuffer();
                    InputStream is = urlConnection.getInputStream();
                    byte[] b = new byte[3];
                    while ( is.read(b) != -1)
                        buffer.append(new String(b));
                    exchanger.exchange(buffer.toString());
                } else {
                    exchanger.exchange("400");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            urlConnection.disconnect();
        });

        StartActivity.ResultFromThread first = new ResultFromThread(exchanger);

        return first.message;
    }

    public static class ResultFromThread {
        public String message;

        public ResultFromThread(Exchanger<String> exchanger) throws InterruptedException {
            this.message = exchanger.exchange(message);
        }

    }
    private void initDB() throws MalformedURLException, JSONException, InterruptedException {
//        this.deleteDatabase(DB_NAME);
        database = this.openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        Log.d("TAG", "initDB: " + database);

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER_INFO + "(id integer primary key autoincrement," +
                " phone_number text);");

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS_INFO + "(id integer primary key autoincrement," +
                " type_auto text," +
                " tarif text);");
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ORDERS_INFO + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " to_street text," +
                " to_number text);");

        cursorDb = database.query(TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            List<String> settings = new ArrayList<>();
            settings.add("usually");
            settings.add("Базовый");
            insertFirstSettings(settings);
            if (cursorDb != null && !cursorDb.isClosed())
                cursorDb.close();
        } else {
            Log.d("TAG", "initDB:" + logCursor(TABLE_SETTINGS_INFO));
        }

    }

    private void insertFirstSettings(List<String> settings) {
        String sql = "INSERT INTO " + TABLE_SETTINGS_INFO + " VALUES(?,?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, settings.get(0));
            statement.bindString(3, settings.get(1));

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
    }

    public static void insertRecordsUser(String phoneNumber) {
        String sql = "INSERT INTO " + TABLE_USER_INFO + " VALUES(?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, phoneNumber);

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        fab.setVisibility(View.VISIBLE);
    }

    public static void insertRecordsOrders(Map<String, String> orders, String from_number, String to_number) {

        String selection = "from_street = ?";
        String[] selectionArgs = new String[] {orders.get("from_name")};

        Cursor cursor_from = database.query(TABLE_ORDERS_INFO,
                null, selection, selectionArgs, null, null, null);

        selection = "to_street = ?";
        selectionArgs = new String[] {orders.get("to_name")};

        Cursor cursor_to = database.query(TABLE_ORDERS_INFO,
                null, selection, selectionArgs, null, null, null);

        if (cursor_from.getCount() == 0 && cursor_to.getCount() == 0) {

            String sql = "INSERT INTO " + TABLE_ORDERS_INFO + " VALUES(?,?,?,?,?);";
            SQLiteStatement statement = database.compileStatement(sql);
            database.beginTransaction();
            try {
                statement.clearBindings();
                statement.bindString(2, orders.get("from_name"));
                statement.bindString(3, from_number);
                statement.bindString(4, orders.get("to_name"));
                statement.bindString(5, to_number);

                statement.execute();
                database.setTransactionSuccessful();

            } finally {
                database.endTransaction();
            }
            Log.d("TAG", "insertRecordsOrders: " + logCursor(TABLE_ORDERS_INFO));
        }

        cursor_from.close();
        cursor_to.close();
        Log.d("TAG", "insertRecordsOrders: from_name No" + orders.get("from_name"));
    }

    public static void updateRecordsUser(String result) {
        ContentValues cv = new ContentValues();

        cv.put("phone_number", result);

        // обновляем по id
        int updCount = database.update(TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        Log.d("TAG", "updated rows count = " + updCount);


    }
    public static ArrayList<Map> routMaps() {
        Map <String, String> routs;
        ArrayList<Map> routsArr = new ArrayList<>();
        Cursor c = database.query(TABLE_ORDERS_INFO, null, null, null, null, null, null);
        int i = 0;
        if (c != null) {
            if (c.moveToFirst()) {

                do {
                    routs = new HashMap<>();
                    routs.put("id", c.getString(c.getColumnIndexOrThrow ("id")));
                    routs.put("from_street", c.getString(c.getColumnIndexOrThrow ("from_street")));
                    routs.put("from_number", c.getString(c.getColumnIndexOrThrow ("from_number")));
                    routs.put("to_street", c.getString(c.getColumnIndexOrThrow ("to_street")));
                    routs.put("to_number", c.getString(c.getColumnIndexOrThrow ("to_number")));
                    routsArr.add(i++, routs);
                } while (c.moveToNext());
            }
        }

        Log.d("TAG", "routMaps: " + routsArr);
        return routsArr;
    }

    public static Map <String, String> routChoice(int i) {
        Map <String, String> rout = new HashMap<>();
        Cursor c = database.query(TABLE_ORDERS_INFO, null, null, null, null, null, null);
        c.move(i);
        rout.put("id", c.getString(c.getColumnIndexOrThrow ("id")));
        rout.put("from_street", c.getString(c.getColumnIndexOrThrow ("from_street")));
        rout.put("from_number", c.getString(c.getColumnIndexOrThrow ("from_number")));
        rout.put("to_street", c.getString(c.getColumnIndexOrThrow ("to_street")));
        rout.put("to_number", c.getString(c.getColumnIndexOrThrow ("to_number")));

        Log.d("TAG", "routMaps: " + rout);
        return rout;
    }
    @SuppressLint("Range")
    public static List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c != null) {
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
        }
        return list;
    }


    // Function to check and request permission
    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }
    // This function is called when user accept or decline the permission.
// Request Code is used to check which permission called this function.
// This request code is provided when user is prompt for permission.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
    }



    public void codeVerify(String phoneNumber) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.phone_verify_code_layout, null);
        builder.setView(view);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText code = view.findViewById(R.id.code);

        builder.setTitle("Код перевіркі зі смс-повідомлення")
                .setPositiveButton("Відправити", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String urlCost = "https://m.easy-order-taxi.site/api/android/approvedPhones/" + phoneNumber + "/" + code.getText();
                        Log.d("TAG", "onClick urlCost: " + urlCost);
                        try {
                            Map sendUrlMapCost = ResultSONParser.sendURL(urlCost);
                            Log.d("TAG", "onClick sendUrlMapCost: " + sendUrlMapCost);
                            if(sendUrlMapCost.get("resp_result").equals("200")) {
                                insertRecordsUser(phoneNumber);
                                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                                startActivity(intent);
//                                finish();
                            } else {
                                String message = (String) sendUrlMapCost.get("message");
                                Toast.makeText(StartActivity.this, message, Toast.LENGTH_SHORT).show();
                            }

                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
               .show();

    }



}
