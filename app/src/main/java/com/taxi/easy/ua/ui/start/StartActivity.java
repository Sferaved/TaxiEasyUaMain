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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.maps.Kyiv1;
import com.taxi.easy.ua.ui.maps.Kyiv10;
import com.taxi.easy.ua.ui.maps.Kyiv2;
import com.taxi.easy.ua.ui.maps.Kyiv3;
import com.taxi.easy.ua.ui.maps.Kyiv4;
import com.taxi.easy.ua.ui.maps.Kyiv5;
import com.taxi.easy.ua.ui.maps.Kyiv6;
import com.taxi.easy.ua.ui.maps.Kyiv7;
import com.taxi.easy.ua.ui.maps.Kyiv8;
import com.taxi.easy.ua.ui.maps.Kyiv9;

import org.json.JSONException;
import org.osmdroid.util.GeoPoint;

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
    private static final String DB_NAME = "data_04042023_1";
    public static final String TABLE_USER_INFO = "userInfo";
    public static final String TABLE_SETTINGS_INFO = "settingsInfo";
    public static final String TABLE_ORDERS_INFO = "ordersInfo";
    public static final String TABLE_SERVICE_INFO = "serviceInfo";

    public static SQLiteDatabase database;
    public static Cursor cursorDb;
    static FloatingActionButton fab;

    Button btn_again;

    public static final int READ_CALL_PHONE = 0;

    Intent intent;
    public static String userEmail, displayName;
//    public static String[] arrayStreet = Odessa.street();
//    public static String api = "apiTest";
//    public static GeoPoint initialGeoPoint = new GeoPoint(46.4825, 30.7233); // Координаты Одесса
    public static String api = "api157";

    public static GeoPoint initialGeoPoint = new GeoPoint(50.4501, 30.5234); // Координаты Киева

    public static String[] arrayStreet = join(Kyiv1.street(),
            Kyiv2.street(),
            Kyiv3.street(),
            Kyiv4.street(),
            Kyiv5.street(),
            Kyiv6.street(),
            Kyiv7.street(),
            Kyiv8.street(),
            Kyiv9.street(),
            Kyiv10.street());


    public static String[] join(String[] a1,
                                String [] a2,
                                String [] a3,
                                String [] a4,
                                String [] a5,
                                String [] a6,
                                String [] a7,
                                String [] a8,
                                String [] a9,
                                String [] a10
    )
    {
        String [] c = new String[a1.length +
                a2.length +
                a3.length +
                a4.length +
                a5.length +
                a6.length +
                a7.length +
                a8.length +
                a9.length +
                a10.length];

        System.arraycopy(a1, 0, c, 0, a1.length);
        System.arraycopy(a2, 0, c, a1.length, a2.length);
        System.arraycopy(a3, 0, c, a1.length
                + a2.length, a3.length);
        System.arraycopy(a4, 0, c, a1.length
                + a2.length
                + a3.length, a4.length);
        System.arraycopy(a5, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length, a5.length);
        System.arraycopy(a6, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length, a6.length);
        System.arraycopy(a7, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length
                + a6.length, a7.length);
        System.arraycopy(a8, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length
                + a6.length
                + a7.length, a8.length);
        System.arraycopy(a9, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length
                + a6.length
                + a7.length
                + a8.length, a9.length);
        System.arraycopy(a10, 0, c, a1.length
                + a2.length
                + a3.length
                + a4.length
                + a5.length
                + a6.length
                + a7.length
                + a8.length
                + a9.length, a10.length);

        return c;
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);
        ImageView mImageView = findViewById(R.id.imageView2);
        Animation sunRiseAnimation = AnimationUtils.loadAnimation(this, R.anim.sun_rise);
        // Подключаем анимацию к нужному View
        mImageView.startAnimation(sunRiseAnimation);

    }

    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onResume() {
        super.onResume();

        fab = findViewById(R.id.fab);
        btn_again = findViewById(R.id.btn_again);

        intent = new Intent(this, MainActivity.class);

        try {
            initDB();
        } catch (MalformedURLException | JSONException | InterruptedException e) {
            throw new RuntimeException(e);
        }


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
//               finish();
                intent = new Intent(StartActivity.this, StartActivity.class);
                startActivity(intent);
            }
        });

        if(!hasConnection()) {
            btn_again.setVisibility(View.VISIBLE);
            Toast.makeText(StartActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
        } else {
            try {
                startIp();
                intent = new Intent(this, FirebaseSignIn.class);
                startActivity(intent);
            } catch (MalformedURLException e) {
                btn_again.setVisibility(View.VISIBLE);
                Toast.makeText(this, R.string.error_firebase_start, Toast.LENGTH_SHORT).show();
            }

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

    public void startIp() throws MalformedURLException {
        String urlString = "https://m.easy-order-taxi.site/" +  StartActivity.api + "/android/startIP";
        Log.d("TAG", "startIp: " + urlString);
        URL url = new URL(urlString);

        AsyncTask.execute(() -> {
            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.getResponseCode();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            urlConnection.disconnect();
        });

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

        ResultFromThread first = new ResultFromThread(exchanger);

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
                " from_lat text," +
                " from_lng text," +
                " to_street text," +
                " to_number text," +
                " to_lat text," +
                " to_lng text);");
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
        } else {
            Log.d("TAG", "initDB:" + logCursor(TABLE_SERVICE_INFO));
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

    public static void insertRecordsOrders( String from, String to,
                                            String from_number, String to_number,
                                            String from_lat, String from_lng,
                                            String to_lat, String to_lng) {
        Log.d("TAG", "insertRecordsOrders: from, to, from_number,  to_number " + from + " - " + to + " - " + from_number + " - " + to_number);
        String selection = "from_street = ?";
        String[] selectionArgs = new String[] {from};

        Cursor cursor_from = database.query(TABLE_ORDERS_INFO,
                null, selection, selectionArgs, null, null, null);
        Log.d("TAG", "insertRecordsOrders: cursor_from.getCount()" + cursor_from.getCount());
        selection = "to_street = ?";
        selectionArgs = new String[] {to};

        Cursor cursor_to = database.query(TABLE_ORDERS_INFO,
                null, selection, selectionArgs, null, null, null);
        Log.d("TAG", "insertRecordsOrders: cursor_to.getCount()"  + cursor_to.getCount());

//        " from_street text," +
//                " from_number text," +
//        String from_lat = "46.482525";
//
//        String from_lng = "30.723308333333332";
//        String to_street text," +
//                " to_number text," +
//        String to_lat = "46.40424965602867";
//        String to_lng = "30.71605682373047";


        if (cursor_from.getCount() == 0 || cursor_to.getCount() == 0) {

            String sql = "INSERT INTO " + TABLE_ORDERS_INFO + " VALUES(?,?,?,?,?,?,?,?,?);";
            SQLiteStatement statement = database.compileStatement(sql);
            database.beginTransaction();
            try {
                statement.clearBindings();
                statement.bindString(2, from);
                statement.bindString(3, from_number);
                statement.bindString(4, from_lat);
                statement.bindString(5, from_lng);
                statement.bindString(6, to);
                statement.bindString(7, to_number);
                statement.bindString(8, to_lat);
                statement.bindString(9, to_lng);

                statement.execute();
                database.setTransactionSuccessful();

            } finally {
                database.endTransaction();
            }
            Log.d("TAG", "insertRecordsOrders 654654654: " + logCursor(TABLE_ORDERS_INFO));
        }

        cursor_from.close();
        cursor_to.close();

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
        rout.put("from_lat", c.getString(c.getColumnIndexOrThrow ("from_lat")));
        rout.put("from_lng", c.getString(c.getColumnIndexOrThrow ("from_lng")));
        rout.put("to_lat", c.getString(c.getColumnIndexOrThrow ("to_lat")));
        rout.put("to_lng", c.getString(c.getColumnIndexOrThrow ("to_lng")));
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

    public static void reIndexOrders() {


        database.execSQL("CREATE TABLE  temp_table" + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " from_lat text," +
                " from_lng text," +
                " to_street text," +
                " to_number text," +
                " to_lat text," +
                " to_lng text);");
        // Копирование данных из старой таблицы во временную
        database.execSQL("INSERT INTO temp_table SELECT * FROM " + TABLE_ORDERS_INFO);

        // Удаление старой таблицы
        database.execSQL("DROP TABLE " + TABLE_ORDERS_INFO);

        // Создание новой таблицы
        database.execSQL("CREATE TABLE " + TABLE_ORDERS_INFO + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " from_lat text," +
                " from_lng text," +
                " to_street text," +
                " to_number text," +
                " to_lat text," +
                " to_lng text);");

        String query = "INSERT INTO " + TABLE_ORDERS_INFO + " (from_street, from_number, from_lat, from_lng, to_street, to_number, to_lat, to_lng) " +
                "SELECT from_street, from_number, from_lat, from_lng, to_street, to_number, to_lat, to_lng FROM temp_table";

        // Копирование данных из временной таблицы в новую
        database.execSQL(query);

        // Удаление временной таблицы
        database.execSQL("DROP TABLE temp_table");

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

        builder.setTitle(getString(R.string.sms_code))
                .setPositiveButton(getString(R.string.sent_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String urlCost = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/approvedPhones/" + phoneNumber + "/" + code.getText();
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
