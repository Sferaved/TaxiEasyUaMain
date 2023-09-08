package com.taxi.easy.ua;

import static com.taxi.easy.ua.R.string.cancel_button;
import static com.taxi.easy.ua.R.string.format_phone;
import static com.taxi.easy.ua.R.string.verify_internet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.taxi.easy.ua.databinding.ActivityMainBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.ApiService;
import com.taxi.easy.ua.ui.finish.City;
import com.taxi.easy.ua.ui.home.MyPhoneDialogFragment;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.start.FirebaseSignIn;

import org.json.JSONException;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    public static final String DB_NAME = "data_08092023_4";
    public static final String TABLE_USER_INFO = "userInfo";
    public static final String TABLE_SETTINGS_INFO = "settingsInfo";
    public static final String TABLE_ORDERS_INFO = "ordersInfo";
    public static final String TABLE_SERVICE_INFO = "serviceInfo";
    public static final String TABLE_ADD_SERVICE_INFO = "serviceAddInfo";
    public static final String CITY_INFO = "cityInfo";

    public static final String TABLE_POSITION_INFO = "myPosition";
    public static Cursor cursorDb;
    public static boolean verifyPhone;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    NetworkChangeReceiver networkChangeReceiver;
    String  cityNew;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    public static final String  apiTest = "apiTest";
    public static final String  apiKyiv = "apiPas1001";
    public static final String  apiDnipro = "apiPas1001_Dnipro";
    public static final String  apiOdessa = "apiPas1001_Odessa";
    public static final String  apiZaporizhzhia = "apiPas1001_Zaporizhzhia";
    public static final String  apiCherkasy = "apiPas1001_Cherkasy";
//    public static String  api = "apiPas2";
    public static String  api;
    public static SQLiteDatabase database;

    private final String[] cityList = new String[]{
            "Київ",
            "Дніпро",
            "Одеса",
            "Запоріжжя",
            "Черкаси",
            "Тест"
    };
   private final String[] cityCode = new String[]{
            "Kyiv City",
            "Dnipropetrovsk Oblast",
            "Odessa",
            "Zaporizhzhia",
            "Cherkasy Oblast",
            "OdessaTest"
    };
    String message;
    private Semaphore semaphore = new Semaphore(0);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initDB();
        } catch (MalformedURLException | JSONException | InterruptedException ignored) {

        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            setSupportActionBar(binding.appBarMain.toolbar);

            DrawerLayout drawer = binding.drawerLayout;
            NavigationView navigationView = binding.navView;
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home, R.id.nav_gallery, R.id.nav_about)
                    .setOpenableLayout(drawer)
                    .build();

            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
            networkChangeReceiver = new NetworkChangeReceiver();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }
    private boolean databaseExists(String dbName) {
        File dbFile = getDatabasePath(dbName);
        return dbFile.exists();
    }
    public void initDB() throws MalformedURLException, JSONException, InterruptedException {
//        this.deleteDatabase(DB_NAME);

        database = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);

        Log.d("TAG", "initDB: " + database);

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER_INFO + "(id integer primary key autoincrement," +
                " verifyOrder text," +
                " phone_number text," +
                " email text," +
                " username text);");

        cursorDb = database.query(TABLE_USER_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            insertUserInfo();
            if (cursorDb != null && !cursorDb.isClosed())
                cursorDb.close();
        }


        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS_INFO + "(id integer primary key autoincrement," +
                " type_auto text," +
                " tarif text," +
                " discount text);");

        cursorDb = database.query(TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            List<String> settings = new ArrayList<>();
            settings.add("usually");
            settings.add(" ");
            settings.add("0");
            insertFirstSettings(settings);
            if (cursorDb != null && !cursorDb.isClosed())
                cursorDb.close();
        }
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_POSITION_INFO + "(id integer primary key autoincrement," +
                " startLat double," +
                " startLan double," +
                " position text);");
        cursorDb = database.query(TABLE_POSITION_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            insertMyPosition();
            if (cursorDb != null && !cursorDb.isClosed())
                cursorDb.close();
        }

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ORDERS_INFO + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " from_lat text," +
                " from_lng text," +
                " to_street text," +
                " to_number text," +
                " to_lat text," +
                " to_lng text);");


        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SERVICE_INFO + "(id integer primary key autoincrement," +
                " BAGGAGE text," +
                " ANIMAL text," +
                " CONDIT text," +
                " MEET text," +
                " COURIER text," +
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
            if (cursorDb != null && !cursorDb.isClosed())
                cursorDb.close();
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
        if (cursorDb != null && !cursorDb.isClosed())
            cursorDb.close();

        database.execSQL("CREATE TABLE IF NOT EXISTS " + CITY_INFO + "(id integer primary key autoincrement," +
                " city text);");

        cursorDb = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
        verifyPhone = cursorDb.getCount() == 1;
        if (cursorDb != null && !cursorDb.isClosed())
            cursorDb.close();

        cursorDb = database.query(CITY_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            getLocalIpAddress();
        }

        newUser();

    }

    private void insertFirstSettings(List<String> settings) {
        String sql = "INSERT INTO " + TABLE_SETTINGS_INFO + " VALUES(?,?,?,?);";
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, settings.get(0));
            statement.bindString(3, settings.get(1));
            statement.bindString(4, settings.get(2));


            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();
    }
    private void insertServices() {
        String sql = "INSERT INTO " + TABLE_SERVICE_INFO + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();
    }
    private void insertAddServices() {
        String sql = "INSERT INTO " + TABLE_ADD_SERVICE_INFO + " VALUES(?,?,?,?);";
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        database.close();
    }
    private void insertUserInfo() {




        String sql = "INSERT INTO " + TABLE_USER_INFO + " VALUES(?,?,?,?,?);";
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, "0");
            statement.bindString(3, "+380");
            statement.bindString(4, "email");
            statement.bindString(5, "username");

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();
    }
    public void resetRecordsAddServices() {
        ContentValues cv = new ContentValues();

        cv.put("time", "no_time");
        cv.put("comment", "no_comment");
        cv.put("date", "no_date");

        // обновляем по id
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

    private void getLocalIpAddress() {

        List<String> city = logCursor(CITY_INFO);


        if(city.size() == 0) {
            ApiService apiService = ApiClient.getApiService();

            Call<City> call = apiService.cityOrder();

            call.enqueue(new Callback<City>() {
                @Override
                public void onResponse(Call<City> call, Response<City> response) {
                    if (response.isSuccessful()) {
                        City status = response.body();
                        if (status != null) {
                            String result = status.getResponse();
                            String message =getString(R.string.your_city);

                            insertCity(result);
                            List<String> stringList = logCursor(CITY_INFO);
                            Log.d("TAG", "onCreate: insertCity"  + stringList);
                            Log.d("TAG", "onResponse: " + result);
                            switch (result){
                                case "Kyiv City":
                                    message += getString(R.string.Kyiv_city);
                                    break;
                                case "Dnipropetrovsk Oblast":
                                    message += getString(R.string.Dnipro_city);
                                    break;
                                case "Odessa":
                                    message += getString(R.string.Odessa);
                                    break;
                                case "Zaporizhzhia":
                                    message += getString(R.string.Zaporizhzhia);
                                    break;
                                case "Cherkasy Oblast":
                                    message += getString(R.string.Cherkasy);
                                    break;
                                default:
                                    message += result;
                                    break;
                            }
                            database.close();
                            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            finishAffinity();
                            startActivity(new Intent(MainActivity.this, MainActivity.class));
                        }
                    }
                    else {
                        Toast.makeText(MainActivity.this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
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

    }
//
    private void insertCity(String city) {
        String sql = "INSERT INTO " + CITY_INFO + " VALUES(?,?);";
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        database.close();

    }
    private void insertMyPosition() {
        String sql = "INSERT INTO " + MainActivity.TABLE_POSITION_INFO + " VALUES(?,?,?,?);";

        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindDouble(2, 50.4398);
            statement.bindDouble(3, 30.7233);
            statement.bindString(4, "Палац Спорту, м.Киів");

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();
    }

    @SuppressLint("Range")

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.phone_settings) {
                phoneNumberChange();
        }
        if (item.getItemId() == R.id.nav_driver) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxieasyua.job"));
            startActivity(browserIntent);
        }
        if (item.getItemId() == R.id.action_exit) {
            finishAffinity();
        }
        if (item.getItemId() == R.id.gps) {
            eventGps();
        }

        if (item.getItemId() == R.id.nav_city) {
            cityChange();
        }

        if (item.getItemId() == R.id.send_like) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxi.easy.ua"));
            startActivity(browserIntent);
        }

        if (item.getItemId() == R.id.send_email) {
            String subject = getString(R.string.android);
            String body = getString(R.string.good_day);

            String[] CC = {""};
            Intent emailIntent = new Intent(Intent.ACTION_SEND);

            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_CC, CC);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);

            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.share)));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, getString(R.string.no_email_agent), Toast.LENGTH_SHORT).show();
            }

        }
        if (item.getItemId() == R.id.send_email_admin) {
            sendEmailAdmin();
        }

        return false;
    }
    private String generateRandomString(int length) {
        String characters = "012345678901234567890123456789";
        StringBuilder randomString = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomString.append(randomChar);
        }

        return randomString.toString();
    }

    private void sendEmailAdmin () {
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city;
        switch (stringList.get(1)){
            case "Dnipropetrovsk Oblast":
                city = getString(R.string.Dnipro_city);
                break;
            case "Zaporizhzhia":
                city = getString(R.string.Zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                city = getString(R.string.Cherkasy);
                break;
            case "Odessa":
                city = getString(R.string.Odessa);
                break;
            case "OdessaTest":
                city = getString(R.string.OdessaTest);
                break;
            default:
                city = getString(R.string.Kyiv_city);
                break;
        }


        List<String> userList = logCursor(MainActivity.TABLE_USER_INFO);

        String subject = getString(R.string.SA_subject) + generateRandomString(10);

        String body =getString(R.string.SA_message_start) + "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n"+ "\n" +
                getString(R.string.SA_info_pas)+ "\n" +
                getString(R.string.SA_info_city) + " " + city + "\n" +
                getString(R.string.SA_pas_text) + " " + getString(R.string.version) + "\n" +
                getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
                getString(R.string.SA_email) + " " + userList.get(3) + "\n" +
                getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n"+"\n";

        String[] CC = {"cartaxi4@gmail.com"};
        String[] TO = {"taxi.easy.ua@gmail.com"};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(emailIntent, subject));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, getString(R.string.no_email_agent), Toast.LENGTH_SHORT).show();
        }


    }
    private void cityChange() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.city_change_layout, null);
        builder.setView(view);


        ArrayAdapter<String> adapterCity = new ArrayAdapter<String>(this, R.layout.my_simple_spinner_item, cityList);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Spinner spinner = view.findViewById(R.id.list_city);
        spinner.setAdapter(adapterCity);
        spinner.setPrompt(getString(R.string.city_change));
        spinner.setBackgroundResource(R.drawable.spinner_border);

        String cityOld = logCursor(CITY_INFO).get(1);
        for (int i = 0; i < cityList.length; i++) {
            if (cityCode[i].equals(cityOld)) {
                spinner.setSelection(i);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cityNew =  cityCode[position];
                message = getString(R.string.your_city) + cityList[position];
                ContentValues cv = new ContentValues();
                SQLiteDatabase database = view.getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

                cv.put("tarif", " ");
                database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                        new String[] { "1" });
                database.close();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        builder.setPositiveButton(R.string.cheng, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues cv = new ContentValues();

                        cv.put("city", cityNew);
                        // обновляем по id
                        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                new String[] { "1" });
                        database.close();

                        Toast.makeText(MainActivity.this, getString(R.string.change_message) + message   , Toast.LENGTH_SHORT).show();
                        finish();
                        startActivity(new Intent(MainActivity.this, MainActivity.class));

                    }
                }).setNegativeButton(cancel_button, null)
                .show();

    }

    public void eventGps() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        Log.d("TAG", "onOptionsItemSelected gps_enabled: " + gps_enabled);
        Log.d("TAG", "onOptionsItemSelected network_enabled: " + network_enabled);
        if(!gps_enabled || !network_enabled) {
            // notify user
            MaterialAlertDialogBuilder builder =  new MaterialAlertDialogBuilder(MainActivity.this, R.style.AlertDialogTheme);
            LayoutInflater inflater = MainActivity.this.getLayoutInflater();

            View view_cost = inflater.inflate(R.layout.message_layout, null);
            builder.setView(view_cost);
            TextView message = view_cost.findViewById(R.id.textMessage);
            message.setText(R.string.gps_info);
            builder.setMessage(R.string.gps_info)
                    .setPositiveButton(R.string.gps_on, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.cancel_button,null)
                    .show();
        } else {
            Toast.makeText(this, getString(R.string.gps_ok), Toast.LENGTH_SHORT).show();
        }
    }
    public void phoneNumberChange() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);

        LayoutInflater inflater = this.getLayoutInflater();

        View view = inflater.inflate(R.layout.phone_settings_layout, null);

        builder.setView(view);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText phoneNumber = view.findViewById(R.id.phoneNumber);

        List<String> stringList =  logCursor(MainActivity.TABLE_USER_INFO);

        if(stringList.size() != 0) {
            phoneNumber.setText(stringList.get(2));


//        String result = phoneNumber.getText().toString();
        builder
                .setPositiveButton(R.string.cheng, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                            Log.d("TAG", "onClick befor validate: ");
                            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                            boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                            Log.d("TAG", "onClick No validate: " + val);
                            if (val == false) {
                                Toast.makeText(MainActivity.this, getString(format_phone) , Toast.LENGTH_SHORT).show();
                                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());

                            } else {
                               updateRecordsUser(phoneNumber.getText().toString());
                            }
                        }
                    }
                }).setNegativeButton(cancel_button, null)
                .show();
        } else {
            if (!verifyPhone()) {
                getPhoneNumber();
            }
            if (!verifyPhone()) {
                MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment();
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        }
    }
    private void updateRecordsUser(String result) {
        ContentValues cv = new ContentValues();

        cv.put("phone_number", result);

        // обновляем по id
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }
    private boolean verifyPhone() {
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(MainActivity.TABLE_USER_INFO).get(2).equals("+380")) {
                verify = false;
            }
            cursor.close();
        }

        return verify;
    }

    private void getPhoneNumber () {
        String mPhoneNumber;
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "Manifest.permission.READ_PHONE_NUMBERS: " + ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS));
            Log.d("TAG", "Manifest.permission.READ_PHONE_STATE: " + ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE));
            return;
        }
        mPhoneNumber = tMgr.getLine1Number();
//        mPhoneNumber = null;
        if(mPhoneNumber != null) {
            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
            boolean val = Pattern.compile(PHONE_PATTERN).matcher(mPhoneNumber).matches();
            Log.d("TAG", "onClick No validate: " + val);
            if (val == false) {
                Toast.makeText(this, format_phone , Toast.LENGTH_SHORT).show();
                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + mPhoneNumber);
//                getActivity().finish();

            } else {
                 insertRecordsUser(mPhoneNumber);
            }
        }

    }
    private void insertRecordsUser(String phoneNumber) {
        String sql = "INSERT INTO " + MainActivity.TABLE_USER_INFO + " VALUES(?,?,?);";
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        database.close();
    }
    private boolean connected() {

        Boolean hasConnect = false;

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(
                CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            hasConnect = true;
        }
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            hasConnect = true;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            hasConnect = true;
        }

        if (!hasConnect) {
            Toast.makeText(this, verify_internet, Toast.LENGTH_LONG).show();
        }
        Log.d("TAG", "connected: " + hasConnect);
        return hasConnect;
    }


    @Override
    protected void onStart() {
        registerReceiver(networkChangeReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        super.onStart();

        // Создание фильтра намерений для отслеживания изменений подключения к интернету
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        // Регистрация BroadcastReceiver с фильтром намерений
        registerReceiver(networkChangeReceiver, filter);


        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        if(stringList.size()!=0) {
            switch (stringList.get(1)) {
                case "Dnipropetrovsk Oblast":

                    api = MainActivity.apiDnipro;
                    break;
                case "Odessa":

                    api = MainActivity.apiOdessa;
                    break;
                case "Zaporizhzhia":

                    api = MainActivity.apiZaporizhzhia;
                    break;
                case "Cherkasy Oblast":

                    api = MainActivity.apiCherkasy;
                    break;
                case "OdessaTest":

                    api = MainActivity.apiTest;
                    break;
                default:

                    api = MainActivity.apiKyiv;
                    break;
            }


        }
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeReceiver);
        super.onStop();
    }
    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        database.close();
        return list;
    }
    public void newUser() throws MalformedURLException {
        String userEmail = logCursor(TABLE_USER_INFO).get(3);
        Log.d("TAG", "newUser: " + userEmail);
        if(userEmail.equals("email")) {

            startActivity(new Intent(MainActivity.this, FirebaseSignIn.class));
//            startActivity(new Intent(this, GoogleSignInActivity.class));
      } else {
//            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                    && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
//                checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
//            }
//            verifyAccess();
            new VerifyUserTask().execute();
        }


    }
//    private void verifyAccess() throws MalformedURLException {
//     LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//     boolean gpsEnabled = false;
//     try {
//         gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
//     } catch (Exception ignored) {
//     }
//
////                 Если GPS выключен, выводим диалог с предложением его включить
//     if (!gpsEnabled) {
//         openGPSSettings();
//     } else {
//         // Проверяем состояние Location Service с помощью колбэка
//         checkLocationServiceEnabled(new FirebaseSignIn.LocationServiceCallback() {
//             @Override
//             public void onLocationServiceResult(boolean isEnabled) throws MalformedURLException {
//                 if (isEnabled) {
//                     // Проверяем разрешения на местоположение
//                     if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                             && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//
//                         checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
//                         checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
//                     }
//                 }
//
//             }
//         });
//     }
// }
    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }
    private static final int REQUEST_ENABLE_GPS = 1001;
//    private void openGPSSettings() {
//        // Проверяем, включен ли GPS
//        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//
//        if (!isGPSEnabled) {
//            // Если GPS не включен, открываем окно настроек для GPS
//            MaterialAlertDialogBuilder builder =  new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
//            LayoutInflater inflater = this.getLayoutInflater();
//
//            View view_cost = inflater.inflate(R.layout.message_layout, null);
//            builder.setView(view_cost);
//            TextView message = view_cost.findViewById(R.id.textMessage);
//            message.setText(R.string.gps_info);
//            builder.setPositiveButton(R.string.gps_on, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
//                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//                            startActivityForResult(intent, REQUEST_ENABLE_GPS);
//                        }
//                    })
//                    .setNegativeButton(R.string.cancel_button, null)
//                    .show();
//
//        } else {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
//                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                startActivity(new Intent(this, OpenStreetMapActivity.class));
//            } else {
//                startActivity(new Intent(this, MainActivity.class));
//            }
//
//        }
//    }
//    private void checkLocationServiceEnabled(FirebaseSignIn.LocationServiceCallback callback) throws MalformedURLException {
//        Context context = getApplicationContext(); // Получите контекст вашего приложения
//
//        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
//
//        // Проверяем, доступны ли данные о местоположении
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            callback.onLocationServiceResult(false);
//            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
//            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
//            return;
//        }
//
//        Task<Location> locationTask = fusedLocationClient.getLastLocation();
//        locationTask.addOnCompleteListener(task -> {
//            if (task.isSuccessful() && task.getResult() != null) {
//                try {
//                    callback.onLocationServiceResult(true);
//                } catch (MalformedURLException e) {
//                    Log.d("TAG", "onCreate:" + new RuntimeException(e));
//                }
//            } else {
//                try {
//                    callback.onLocationServiceResult(false);
//                } catch (MalformedURLException e) {
//                    Log.d("TAG", "onCreate:" + new RuntimeException(e));
//                }
//            }
//        });
//    }

    @SuppressLint("StaticFieldLeak")
    public class VerifyUserTask extends AsyncTask<Void, Void, Map<String, String>> {
        private Exception exception;
        @Override
        protected Map<String, String> doInBackground(Void... voids) {
            String userEmail = logCursor(TABLE_USER_INFO).get(3);

            String url = "https://m.easy-order-taxi.site/" + MainActivity.apiKyiv  + "/android/verifyBlackListUser/" + userEmail;
            try {
                return CostJSONParser.sendURL(url);
            } catch (Exception e) {
                exception = e;
                return null;
            }

        }

        @Override
        protected void onPostExecute(Map<String, String> sendUrlMap) {
            String message = sendUrlMap.get("message");
            ContentValues cv = new ContentValues();
            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            if (message != null) {

                if (message.equals("В черном списке")) {

                    cv.put("verifyOrder", "0");
                    database.update(TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                } else {
                    try {
                        version(message);
                    } catch (MalformedURLException ignored) {

                    }
                    cv.put("verifyOrder", "1");
                    database.update(TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                }
            }
            database.close();
        }
    }

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String LAST_NOTIFICATION_TIME_KEY = "lastNotificationTime";
    private static final long ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000; // 24 часа в миллисекундах

    private void version(String versionApi) throws MalformedURLException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.POST_NOTIFICATIONS, PackageManager.PERMISSION_GRANTED);
            return;
        }

        // Получаем SharedPreferences
        SharedPreferences SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Получаем время последней отправки уведомления
        long lastNotificationTime = SharedPreferences.getLong(LAST_NOTIFICATION_TIME_KEY, 0);

        // Получаем текущее время
        long currentTime = System.currentTimeMillis();

        // Проверяем, прошло ли уже 24 часа с момента последней отправки
        if (currentTime - lastNotificationTime >= ONE_DAY_IN_MILLISECONDS) {
            if (!versionApi.equals(getString(R.string.version_code))) {
                NotificationHelper notificationHelper = new NotificationHelper();
                String title = getString(R.string.new_version);
                String messageNotif = getString(R.string.news_of_version);
                String urlStr = "https://play.google.com/store/apps/details?id=com.taxi.easy.ua";
                notificationHelper.showNotification(this, title, messageNotif, urlStr);

                // Обновляем время последней отправки уведомления
                SharedPreferences.Editor editor = SharedPreferences.edit();
                editor.putLong(LAST_NOTIFICATION_TIME_KEY, currentTime);
                editor.apply();
            }
        }
    }



}