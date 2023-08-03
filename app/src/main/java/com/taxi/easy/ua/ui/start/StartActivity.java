package com.taxi.easy.ua.ui.start;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import  com.taxi.easy.ua.R;
import com.taxi.easy.ua.ServerConnection;
import  com.taxi.easy.ua.ui.finish.ApiClient;
import  com.taxi.easy.ua.ui.finish.ApiService;
import  com.taxi.easy.ua.ui.finish.City;

import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.HttpsURLConnection;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StartActivity extends Activity {
    public static final String DB_NAME = "data_02082023_0";
    public static final String TABLE_USER_INFO = "userInfo";
    public static final String TABLE_SETTINGS_INFO = "settingsInfo";
    public static final String TABLE_ORDERS_INFO = "ordersInfo";
    public static final String TABLE_SERVICE_INFO = "serviceInfo";
    public static final String TABLE_ADD_SERVICE_INFO = "serviceAddInfo";
    public static final String CITY_INFO = "cityInfo";

    public static SQLiteDatabase database;
    public static Cursor cursorDb;
    static FloatingActionButton fab, btn_again;

    Intent intent;
    public static String userEmail, displayName;

    public static final String  apiTest = "apiTest";
    public static final String  apiKyiv = "apiPas1700";


    public static long addCost, cost;
    public static boolean verifyPhone;
    Button try_again_button;
    private BroadcastReceiver connectivityReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);


        try_again_button = findViewById(R.id.try_again_button);
        try_again_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(new Intent(StartActivity.this, StartActivity.class));
                }
            });

//        if(hasConnection()) {
//            getLocalIpAddress();
//        } else {
//            finish();
//            startActivity(new Intent(StartActivity.this, StopActivity.class));
//        }
    }
    private void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    // Создаем метод для установки повторяющегося будильника
    private void setRepeatingAlarm() {
        // Получаем системный сервис AlarmManager
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Создаем намерение для запуска StartActivity
        Intent intent = new Intent(this, StartActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Устанавливаем повторяющийся будильник с интервалом 60 секунд
        long intervalMillis = 60 * 1000; // 60 секунд
        long triggerTimeMillis = System.currentTimeMillis() + intervalMillis;
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTimeMillis, intervalMillis, pendingIntent);

        // Проверяем наличие интернет-соединения
        connectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {


                if (hasConnectionAlarm()) {
                    // Если есть подключение к интернету, отменяем повторяющийся будильник
                    alarmManager.cancel(pendingIntent);
                    try_again_button.setVisibility(View.INVISIBLE);
                    startActivity(new Intent(StartActivity.this, StartActivity.class));
                    if (connectivityReceiver != null) {
                        unregisterReceiver(connectivityReceiver);
                    }
                }
            }
        };

        // Регистрируем BroadcastReceiver для изменений состояния сети
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, intentFilter);

    }
    private boolean hasConnectionAlarm() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();

    }
    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onResume() {
        super.onResume();

        if(hasConnection()) {
            isConnectedToGoogle();
            try {

                if(hasServer()) {
                    initDB();
                } else {
                    Toast.makeText(this, R.string.server_error_connected, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, StopActivity.class));
                }
            } catch (MalformedURLException | JSONException | InterruptedException e) {
                Log.d("TAG", "onResume:  new RuntimeException(e)");
            }
        }
        else  {
            Toast.makeText(this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
            try_again_button.setVisibility(View.VISIBLE);
            setRepeatingAlarm();
        }

        fab = findViewById(R.id.fab);
        btn_again = findViewById(R.id.btn_again);

        fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:0674443804"));
                    startActivity(intent);
                }
            });


       btn_again.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               intent = new Intent(StartActivity.this, StartActivity.class);
               startActivity(intent);
           }
       });

       if(!hasConnection()) {
           btn_again.setVisibility(View.VISIBLE);
           Toast.makeText(StartActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
       } else {
           try {
               if(hasServer()) {
                   startIp();
                   startActivity(new Intent(this, FirebaseSignIn.class));
               } else {
                   Toast.makeText(this, R.string.server_error_connected, Toast.LENGTH_SHORT).show();
                   startActivity(new Intent(this, StopActivity.class));
               }
           } catch (MalformedURLException e) {
               btn_again.setVisibility(View.VISIBLE);
               Toast.makeText(this, R.string.error_firebase_start, Toast.LENGTH_SHORT).show();
           }
       }


    }

    public CompletableFuture<Boolean> checkConnectionAsync() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        ServerConnection.checkConnection("https://m.easy-order-taxi.site/", new ServerConnection.ConnectionCallback() {
            @Override
            public void onConnectionResult(boolean isConnected) {
                future.complete(isConnected);
            }
        });

        return future;
    }

    private boolean hasServer() {
        CompletableFuture<Boolean> connectionFuture = checkConnectionAsync();
        boolean isConnected = false;
        try {
            isConnected = connectionFuture.get();
        } catch (Exception e) {

        }
        return  isConnected;
    };

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
    public boolean isConnectedToGoogle() {
        Toast.makeText(this, R.string.check_message, Toast.LENGTH_LONG).show();
        ImageView mImageView = findViewById(R.id.imageView2);
        Animation sunRiseAnimation = AnimationUtils.loadAnimation(this, R.anim.sun_rise);
        // Подключаем анимацию к нужному View
        mImageView.startAnimation(sunRiseAnimation);

            AsyncTask.execute(() -> {

                try {
                    String googleEndpoint = "https://www.google.com";
                    long startTime = System.currentTimeMillis();

                    URL url = new URL(googleEndpoint);
                    HttpsURLConnection connection = null;
                    connection = (HttpsURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(2000); // Установите тайм-аут подключения в миллисекундах
                    connection.connect();

                    long endTime = System.currentTimeMillis();
                    long responseTime = endTime - startTime;

                        // Проверка успешности ответа и времени подключения
                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            Log.d("TAG", "isConnectedToGoogle: Подключение к Google выполнено успешно. Время ответа: " + responseTime + " мс");
                            if (responseTime >= 2000) {
                                Toast.makeText(this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(StartActivity.this, StopActivity.class);
                                startActivity(intent);

                            }
                        } else {
                            Toast.makeText(this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(StartActivity.this, StopActivity.class);
                            startActivity(intent);
                        }
                    connection.disconnect();
                } catch (IOException e) {
                    Toast.makeText(this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StartActivity.this, StopActivity.class);
                    startActivity(intent);
                }

            });



        return false;
    }
    public void startIp() throws MalformedURLException {
        String api;
        List<String> stringList = logCursor(StartActivity.CITY_INFO);
        switch (stringList.get(1)){
            case "Kyiv City":
                api = StartActivity.apiKyiv;
                break;
            case "Odessa":
                api = StartActivity.apiTest;
                break;
            default:
                api = StartActivity.apiTest;
                break;
        }



        String urlString = "https://m.easy-order-taxi.site/" +  api + "/android/startIP";
        Log.d("TAG", "startIp: " + urlString);
        URL url = new URL(urlString);

        AsyncTask.execute(() -> {
            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.getResponseCode();
            } catch (IOException e) {

            }
            urlConnection.disconnect();
        });

    }
    private void initDB() throws MalformedURLException, JSONException, InterruptedException {
//        this.deleteDatabase(DB_NAME);
        database = this.openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        Log.d("TAG", "initDB: " + database);

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER_INFO + "(id integer primary key autoincrement," +
                " verifyOrder text," +
                " phone_number text);");

        cursorDb = database.query(TABLE_USER_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            insertUserInfo();
        }


        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS_INFO + "(id integer primary key autoincrement," +
                " type_auto text," +
                " tarif text);");
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ORDERS_INFO + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " from_lat text," +
                " from_lng text," +
                " to_street text," +
                " to_number text," +
                " to_lat text," +
                " to_lng text);");
//        Log.d("TAG", "initDB TABLE_ORDERS_INFO:" + logCursor(TABLE_ORDERS_INFO));
        cursorDb = database.query(TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            List<String> settings = new ArrayList<>();
            settings.add("usually");
            settings.add("Базовый");
            insertFirstSettings(settings);
            if (cursorDb != null && !cursorDb.isClosed())
                cursorDb.close();
        }
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SERVICE_INFO + "(id integer primary key autoincrement," +
                " BAGGAGE text," +
                " ANIMAL text," +
                " CONDIT text," +
                " MEET text," +
                " COURIER text," +
                " TERMINAL text," +
                " CHECK_OUT text," +
                " BABY_SEAT text," +
                " DRIVER text," +
                " NO_SMOKE text," +
                " ENGLISH text," +
                " CABLE text," +
                " FUEL text," +
                " WIRES text," +
                " SMOKE text);");
        cursorDb = database.query(TABLE_SERVICE_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            insertServices();
        }

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ADD_SERVICE_INFO + "(id integer primary key autoincrement," +
                " time text," +
                " comment text," +
                " date text);");
        cursorDb = database.query(TABLE_ADD_SERVICE_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            insertAddServices();
        } else {
            resetRecordsAddServices();
        }


        database.execSQL("CREATE TABLE IF NOT EXISTS " + CITY_INFO + "(id integer primary key autoincrement," +
                " city text);");
        if (cursorDb.getCount() == 0) {
            insertCity("Odessa");
        } else {
            getLocalIpAddress();
        }

        Cursor cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
        verifyPhone = cursor.getCount() == 1;
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
    private void insertServices() {
        String sql = "INSERT INTO " + TABLE_SERVICE_INFO + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, "0");
            statement.bindString(3, "0");
            statement.bindString(4, "0");
            statement.bindString(5, "0");
            statement.bindString(6, "0");
            statement.bindString(7, "0");
            statement.bindString(8, "0");
            statement.bindString(9, "0");
            statement.bindString(10,"0");
            statement.bindString(11,"0");
            statement.bindString(12,"0");
            statement.bindString(13,"0");
            statement.bindString(14,"0");
            statement.bindString(15,"0");
            statement.bindString(16,"0");

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
    }
    private void insertAddServices() {
        String sql = "INSERT INTO " + TABLE_ADD_SERVICE_INFO + " VALUES(?,?,?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, "no_time");
            statement.bindString(3, "no_comment");
            statement.bindString(4, "no_date");

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
    }
    private void insertUserInfo() {
        String sql = "INSERT INTO " + TABLE_USER_INFO + " VALUES(?,?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, "0");
            statement.bindString(3, "+380");

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
    }
    public static void resetRecordsAddServices() {
        ContentValues cv = new ContentValues();

        cv.put("time", "no_time");
        cv.put("comment", "no_comment");
        cv.put("date", "no_date");

        // обновляем по id
        database.update(TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                new String[] { "1" });
    }

    public static void insertRecordsUser(String phoneNumber) {
        String sql = "INSERT INTO " + TABLE_USER_INFO + " VALUES(?,?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(3, phoneNumber);

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        fab.setVisibility(View.VISIBLE);
    }

    public static void updateRecordsUser(String result) {
        ContentValues cv = new ContentValues();

        cv.put("phone_number", result);

        // обновляем по id
        int updCount = database.update(TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        Log.d("TAG", "updated rows count = " + updCount);


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
        if (connectivityReceiver != null) {
            unregisterReceiver(connectivityReceiver);
        }
    }

    private void getLocalIpAddress() {
        ApiService apiService = ApiClient.getApiService();

        Call<City> call = apiService.cityOrder();

        call.enqueue(new Callback<City>() {
            @Override
            public void onResponse(Call<City> call, Response<City> response) {
                if (response.isSuccessful()) {
                    City status = response.body();
                    if (status != null) {
                        String result = status.getResponse();
                        String message = getString(R.string.your_city);
                        SQLiteDatabase database = openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
                        ContentValues cv = new ContentValues();
                        switch (result){
                            case "Kyiv City":
                                message += getString(R.string.Kyiv_city);
                                cv = new ContentValues();
                                cv.put("tarif", "Базовий онлайн");
                                database.update(StartActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                        new String[] { "1" });
                                break;
                            case "Odessa":
                                message += getString(R.string.Odessa);
                                cv.put("tarif", "Базовый");
                                database.update(StartActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                        new String[] { "1" });
                                database.close();
                                break;
                            default:
                                message += getString(R.string.Odessa);
                                cv.put("tarif", "Базовый");
                                database.update(StartActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                        new String[] { "1" });
                                database.close();
                                break;
                        }

                        updateCity(result);
                        database.close();
                        Toast.makeText(StartActivity.this, message, Toast.LENGTH_SHORT).show();


                    }
                }
            }

            @Override
            public void onFailure(Call<City> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d("TAG", "onFailure: " + errorMessage);

            }
        });
    }
    private void insertCity(String city) {
        String sql = "INSERT INTO " + CITY_INFO + " VALUES(?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, city);

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
    }

    public static void updateCity(String city) {
        ContentValues cv = new ContentValues();

        cv.put("city", city);
        // обновляем по id
        database.update(CITY_INFO, cv, "id = ?",
                new String[] { "1" });
    }
}
