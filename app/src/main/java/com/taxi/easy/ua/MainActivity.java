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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taxi.easy.ua.cities.api.CityApiClient;
import com.taxi.easy.ua.cities.api.CityResponse;
import com.taxi.easy.ua.cities.api.CityResponseMerchantFondy;
import com.taxi.easy.ua.cities.api.CityService;
import com.taxi.easy.ua.databinding.ActivityMainBinding;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.fondy.callback.CallbackResponse;
import com.taxi.easy.ua.ui.fondy.callback.CallbackService;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetCityFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TAG_MAIN";
    public static String order_id;
    public static String invoiceId;



    public static final String DB_NAME = "data_10122023_0";

    /**
     * Table section
     */
    public static final String TABLE_USER_INFO = "userInfo";
    public static final String TABLE_SETTINGS_INFO = "settingsInfo";
    public static final String TABLE_ORDERS_INFO = "ordersInfo";
    public static final String TABLE_SERVICE_INFO = "serviceInfo";
    public static final String TABLE_ADD_SERVICE_INFO = "serviceAddInfo";
    public static final String CITY_INFO = "cityInfo";
    public static final String ROUT_HOME = "routHome";
    public static final String ROUT_GEO = "routGeo";
    public static final String ROUT_MARKER = "routMarker";

    public static final String TABLE_POSITION_INFO = "myPosition";
    public static final String TABLE_FONDY_CARDS = "tableFondyCards";
    public static final String TABLE_MONO_CARDS = "tableMonoCards";
    public static Cursor cursorDb;
    public static boolean verifyPhone;
    private AppBarConfiguration mAppBarConfiguration;
    private NetworkChangeReceiver networkChangeReceiver;
    /**
     * Api section
     */

    public static final String  api = "apiTest";

    /**
     * Phone section
     */
    public static final String Kyiv_City_phone = "tel:0674443804";

    public static SQLiteDatabase database;
    public static Menu navMenu;
    public static MenuItem navVisicomMenuItem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initDB();
        } catch (MalformedURLException | JSONException | InterruptedException ignored) {

        }
        com.taxi.easy.ua.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
              R.id.nav_visicom, R.id.nav_home, R.id.nav_gallery, R.id.nav_about, R.id.nav_uid, R.id.nav_bonus, R.id.nav_card)
             .setOpenableLayout(drawer)
             .build();
        navMenu = navigationView.getMenu();
        navVisicomMenuItem = navMenu.findItem(R.id.nav_visicom);


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        networkChangeReceiver = new NetworkChangeReceiver();


    }
    @Override
    protected void onResume() {
        super.onResume();
         // Получаем путь к базе данных
        String dbPath = getDatabasePath(MainActivity.DB_NAME).getAbsolutePath();
        File dbFile = new File(dbPath);
        // Проверяем существование файла базы данных
        if (dbFile.exists()) {
            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

            Cursor c = database.query(CITY_INFO, null, null, null, null, null, null);
            if(c.getCount() != 0) {
                List<String> listCity = logCursor(CITY_INFO);
                String city = listCity.get(1);

                String cityMenu;
                switch (city) {
                    case "Dnipropetrovsk Oblast":
                        cityMenu = getString(R.string.city_dnipro);
                        break;
                    case "Odessa":
                        cityMenu = getString(R.string.city_odessa);
                        break;
                    case "Zaporizhzhia":
                        cityMenu = getString(R.string.city_zaporizhzhia);
                        break;
                    case "Cherkasy Oblast":
                        cityMenu = getString(R.string.city_cherkasy);
                        break;
                    case "OdessaTest":
                        cityMenu = "Test";
                        break;
                    default:
                        cityMenu = getString(R.string.city_kyiv);
                }


                if (MainActivity.navVisicomMenuItem != null) {
                    // Новый текст элемента меню
                    String newTitle =  getString(R.string.menu_city) + " " + cityMenu;

                    // Изменяем текст элемента меню
                    MainActivity.navVisicomMenuItem.setTitle(newTitle);
                }
            }

            database.close();
        }

    }
    @Override
    protected void onRestart() {
        super.onRestart();
        if(HomeFragment.progressBar != null) {
            HomeFragment.progressBar.setVisibility(View.INVISIBLE);
        }
        if(VisicomFragment.progressBar != null) {
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }

    }
    @SuppressLint("SuspiciousIndentation")
    public void initDB() throws MalformedURLException, JSONException, InterruptedException {
//        this.deleteDatabase(DB_NAME);

        database = openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);

        Log.d("TAG", "initDB: " + database);

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER_INFO + "(id integer primary key autoincrement," +
                " verifyOrder text," +
                " phone_number text," +
                " email text," +
                " username text," +
                " bonus text);");

        cursorDb = database.query(TABLE_USER_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            insertUserInfo();
            if (cursorDb != null && !cursorDb.isClosed())
                cursorDb.close();
        }


        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS_INFO + "(id integer primary key autoincrement," +
                " type_auto text," +
                " tarif text," +
                " discount text," +
                " payment_type text," +
                " addCost text);");

        cursorDb = database.query(TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            List<String> settings = new ArrayList<>();
            settings.add("usually");
            settings.add(" ");
            settings.add("0");
            settings.add("nal_payment");
            settings.add("0");
            insertFirstSettings(settings);
            if (cursorDb != null && !cursorDb.isClosed())
                cursorDb.close();
        }
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_POSITION_INFO + "(id integer primary key autoincrement," +
                " startLat double," +
                " startLan double," +
                " position text," +
                " newZoomLevel double);");
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
                " city text," +
                " api text," +
                " phone text," +
                " card_max_pay text," +
                " bonus_max_pay text," +
                " merchant_fondy text," +
                " fondy_key_storage text);");
        cursorDb = database.query(CITY_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            List<String> settings = new ArrayList<>();
            settings.add(""); //1
            settings.add(api); //2
            settings.add(Kyiv_City_phone); //3
            settings.add("5000"); //4
            settings.add("500000"); //5
            settings.add(""); //6
            settings.add(""); //7
            insertCity(settings);

            cityMaxPay("Kyiv City");
            merchantFondy("Kyiv City");
            if (MainActivity.navVisicomMenuItem != null) {
                // Новый текст элемента меню
                String cityMenu = getString(R.string.city_kyiv);
                String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
                // Изменяем текст элемента меню
                MainActivity.navVisicomMenuItem.setTitle(newTitle);
            }


        }
        cursorDb = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
        verifyPhone = cursorDb.getCount() == 1;
        if (cursorDb != null && !cursorDb.isClosed())
            cursorDb.close();

        database.execSQL("CREATE TABLE IF NOT EXISTS " + ROUT_HOME + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " to_street text," +
                " to_number text);");
        cursorDb = database.query(ROUT_HOME, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            Log.d("TAG", "initDB: ROUT_HOME");
            insertRoutHome();
        }
        if (cursorDb != null && !cursorDb.isClosed())
        cursorDb.close();

        database.execSQL("CREATE TABLE IF NOT EXISTS " + ROUT_GEO + "(id integer primary key autoincrement," +
                " startLat double," +
                " startLan double," +
                " toCost text," +
                " to_numberCost text);");
        cursorDb = database.query(ROUT_GEO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            insertRoutGeo();
        }
        if (cursorDb != null && !cursorDb.isClosed())
            cursorDb.close();

        database.execSQL("CREATE TABLE IF NOT EXISTS " + ROUT_MARKER + "(id integer primary key autoincrement," +
                " startLat double," +
                " startLan double," +
                " to_lat double," +
                " to_lng double," +
                " start text," +
                " finish text);");
        cursorDb = database.query(ROUT_MARKER, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            insertRoutMarker();
        }
        if (cursorDb != null && !cursorDb.isClosed())
            cursorDb.close();

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FONDY_CARDS + "(id integer primary key autoincrement," +
                " masked_card text," +
                " card_type text," +
                " bank_name text," +
                " rectoken text," +
                " rectoken_check text);");


        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MONO_CARDS + "(id integer primary key autoincrement," +
                " masked_card text," +
                " card_type text," +
                " bank_name text," +
                " rectoken text," +
                " rectoken_check text);");

        database.close();
        newUser();
    }

    private void insertFirstSettings(List<String> settings) {
        String sql = "INSERT INTO " + TABLE_SETTINGS_INFO + " VALUES(?,?,?,?,?,?);";
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, settings.get(0));
            statement.bindString(3, settings.get(1));
            statement.bindString(4, settings.get(2));
            statement.bindString(5, settings.get(3));
            statement.bindString(6, settings.get(4));
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

        String sql = "INSERT INTO " + TABLE_USER_INFO + " VALUES(?,?,?,?,?,?);";
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, "0");
            statement.bindString(3, "+380");
            statement.bindString(4, "email");
            statement.bindString(5, "username");
            statement.bindString(6, "0");

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

    private void insertCity(List<String> settings) {
        String sql = "INSERT INTO " + CITY_INFO + " VALUES(?,?,?,?,?,?,?,?);";
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, settings.get(0));
            statement.bindString(3, settings.get(1));
            statement.bindString(4, settings.get(2));
            statement.bindString(5, settings.get(3));
            statement.bindString(6, settings.get(4));
            statement.bindString(7, settings.get(5));
            statement.bindString(8, settings.get(6));

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();

    }
    private void insertMyPosition() {
        String sql = "INSERT INTO " + MainActivity.TABLE_POSITION_INFO + " VALUES(?,?,?,?,?);";

        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindDouble(2, 0);
            statement.bindDouble(3,0 );
            statement.bindString(4, "вул.Хрещатик, буд.22, місто Київ");
            statement.bindDouble(5, 19.0);

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


    @SuppressLint("IntentReset")
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
//            cityChange();
            MyBottomSheetCityFragment bottomSheetDialogFragment = new MyBottomSheetCityFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
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
            } catch (android.content.ActivityNotFoundException ignored) {

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

    @SuppressLint("IntentReset")
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
        } catch (android.content.ActivityNotFoundException ignored) {

        }


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
            MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        } else {
            String message = getString(R.string.gps_ok);
            MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
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
    private boolean connected() {

        boolean hasConnect = false;

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
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeReceiver);
        super.onStop();
    }
    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = db.query(table, null, null, null, null, null, null);
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
        db.close();
        return list;
    }
    public void newUser() {
        String userEmail = logCursor(TABLE_USER_INFO).get(3);
        Log.d("TAG", "newUser: " + userEmail);
        if(userEmail.equals("email")) {
            startFireBase();
        } else {
            new VerifyUserTask().execute();

        }

    }

    private void startFireBase() {
        startSignInInBackground();
    }
    private void startSignInInBackground() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // Инициализация FirebaseApp
                FirebaseApp.initializeApp(MainActivity.this);

                // Choose authentication providers
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.GoogleBuilder().build());

                // Create and launch sign-in intent
                Intent signInIntent = AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build();
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            signInLauncher.launch(signInIntent);
                        }
                    });
                } catch (NullPointerException ignored) {

                }
            }
        });
        thread.start();
    }


    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    try {
                        onSignInResult(result);
                    } catch (MalformedURLException | JSONException | InterruptedException e) {
                        Log.d("TAG", "onCreate:" + new RuntimeException(e));
                    }
                }
            }
    );


    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) throws MalformedURLException, JSONException, InterruptedException {
        ContentValues cv = new ContentValues();
        try {
            if (result.getResultCode() == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                updateRecordsUserInfo("email", user.getEmail());
                updateRecordsUserInfo("username", user.getDisplayName());

                addUser(user.getDisplayName(), user.getEmail()) ;



//                fetchBonus(user.getEmail());

                getCardToken("fondy", TABLE_FONDY_CARDS, user.getEmail());
                getCardToken("mono", TABLE_MONO_CARDS, user.getEmail());

                cv.put("verifyOrder", "1");
                SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                database.close();

            } else {

                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.firebase_error));
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                cv.put("verifyOrder", "0");
                SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                database.close();
            }
        } catch (NullPointerException e) {

            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.firebase_error));
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            cv.put("verifyOrder", "0");
            SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
        }
    }

    private void addUser(String displayName , String userEmail) {
        String urlString = "https://m.easy-order-taxi.site/android/addUser/" + displayName  + "/" + userEmail;

        Callable<Void> addUserCallable = () -> {
            URL url = new URL(urlString);
            Log.d("TAG", "sendURL: " + urlString);

            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
//                urlConnection.getResponseCode();
                Log.d("TAG", "addUser: urlConnection.getResponseCode(); " + urlConnection.getResponseCode());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return null;
        };

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<Void> addUserFuture = executorService.submit(addUserCallable);

        // Дождитесь завершения выполнения задачи с тайм-аутом
        try {
            addUserFuture.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Обработка ошибок
            e.printStackTrace();
        } finally {
            // Завершите исполнителя
            executorService.shutdown();
        }
    }

    private void updateRecordsUserInfo(String userInfo, String result) {
        SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        ContentValues cv = new ContentValues();

        cv.put(userInfo, result);

        // обновляем по id
        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }
    private void insertCard(List<String> settings) {
        String sql = "INSERT INTO " + MainActivity.TABLE_FONDY_CARDS + " VALUES(?,?,?,?,?);";

        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, settings.get(0));
            statement.bindString(3, settings.get(1));
            statement.bindString(4, settings.get(2));
            statement.bindString(5, settings.get(3));

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();
    }
    private void insertRoutHome() {
        String sql = "INSERT INTO " + MainActivity.ROUT_HOME + " VALUES(?,?,?,?,?);";

        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, " ");
            statement.bindString(3, " ");
            statement.bindString(4, " ");
            statement.bindString(5, " ");

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();
    }

    private void insertRoutGeo() {
        String sql = "INSERT INTO " + MainActivity.ROUT_GEO + " VALUES(?,?,?,?,?);";

        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindDouble(2, 0);
            statement.bindDouble(3, 0);
            statement.bindString(4, " ");
            statement.bindString(5, " ");

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();

    }

    private void insertRoutMarker() {
        String sql = "INSERT INTO " + MainActivity.ROUT_MARKER + " VALUES(?,?,?,?,?,?,?);";

        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindDouble(2, 0);
            statement.bindDouble(3, 0);
            statement.bindDouble(4, 0);
            statement.bindDouble(5, 0);
            statement.bindString(6, "");
            statement.bindString(7, "");


            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();
    }

    private void getCardToken(String pay_system, String table, String email) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site") // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String baseUrl = retrofit.baseUrl().toString();

        Log.d(TAG, "Base URL: " + baseUrl);
        // Создайте сервис
        CallbackService service = retrofit.create(CallbackService.class);

        Log.d(TAG, "getCardTokenFondy: ");

        // Выполните запрос
        Call<CallbackResponse> call = service.handleCallback(email, pay_system);
        String requestUrl = call.request().toString();
        Log.d(TAG, "Request URL: " + requestUrl);

        call.enqueue(new Callback<CallbackResponse>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponse> call, @NonNull Response<CallbackResponse> response) {
                Log.d(TAG, "onResponse: " + response.body());
                if (response.isSuccessful()) {
                    CallbackResponse callbackResponse = response.body();
                    if (callbackResponse != null) {
                        List<CardInfo> cards = callbackResponse.getCards();
                        Log.d(TAG, "onResponse: cards" + cards);
                        if (cards != null && !cards.isEmpty()) {
                            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            for (CardInfo cardInfo : cards) {
                                ContentValues cv = new ContentValues();
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты

                                Log.d(TAG, "onResponse: card_token: " + rectoken);

                                cv.put("masked_card", masked_card);
                                cv.put("card_type", card_type);
                                cv.put("bank_name", bank_name);
                                cv.put("rectoken", rectoken);
                                cv.put("rectoken_check", "-1");
                                database.insert(table, null, cv);
                            }
                            ContentValues cv = new ContentValues();
                            cv.put("rectoken_check", "1");
                            database.update(table, cv, "id = ?",
                                    new String[] { "1" });
                            database.close();
                        }
                    }

                } else {
                    // Обработка случаев, когда ответ не 200 OK
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponse> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
                Log.d(TAG, "onResponse: failure " + t.toString());
            }
        });

    }
    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class VerifyUserTask extends AsyncTask<Void, Void, Map<String, String>> {
        private Exception exception;
        @Override
        protected Map<String, String> doInBackground(Void... voids) {
            String userEmail = logCursor(TABLE_USER_INFO).get(3);

            String url = "https://m.easy-order-taxi.site/android/verifyBlackListUser/" + userEmail + "/" + "com.taxi.easy.ua";
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
//    private static final long ONE_DAY_IN_MILLISECONDS = 0; // 24 часа в миллисекундах
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

    private void cityMaxPay(String $city) {
        CityService cityService = CityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponse> call = cityService.getMaxPayValues($city);

        call.enqueue(new Callback<CityResponse>() {
            @Override
            public void onResponse(Call<CityResponse> call, Response<CityResponse> response) {
                if (response.isSuccessful()) {
                    CityResponse cityResponse = response.body();
                    if (cityResponse != null) {
                        int cardMaxPay = cityResponse.getCardMaxPay();
                        int bonusMaxPay = cityResponse.getBonusMaxPay();

                        ContentValues cv = new ContentValues();
                        cv.put("card_max_pay", cardMaxPay);
                        cv.put("bonus_max_pay", bonusMaxPay);

                        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                new String[] { "1" });

                        database.close();

                        Log.d(TAG, "onResponse: cardMaxPay" + cardMaxPay);
                        Log.d(TAG, "onResponse: bonus_max_pay" + bonusMaxPay);
                        Log.d(TAG, "onResponse: " + logCursor(CITY_INFO).toString());

                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                    Log.e("Request", "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CityResponse> call, Throwable t) {
                Log.e("Request", "Failed. Error message: " + t.getMessage());
            }
        });
    }
    private void merchantFondy(String $city) {
        CityService cityService = CityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponseMerchantFondy> call = cityService.getMerchantFondy($city);

        call.enqueue(new Callback<CityResponseMerchantFondy>() {
            @Override
            public void onResponse(Call<CityResponseMerchantFondy> call, Response<CityResponseMerchantFondy> response) {
                if (response.isSuccessful()) {
                    CityResponseMerchantFondy cityResponse = response.body();
                    Log.d(TAG, "onResponse: cityResponse" + cityResponse);
                    if (cityResponse != null) {
                        String merchant_fondy = cityResponse.getMerchantFondy();
                        String fondy_key_storage = cityResponse.getFondyKeyStorage();

                        ContentValues cv = new ContentValues();
                        cv.put("merchant_fondy", merchant_fondy);
                        cv.put("fondy_key_storage", fondy_key_storage);

                        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                new String[] { "1" });

                        database.close();

                        Log.d(TAG, "onResponse: merchant_fondy" + merchant_fondy);
                        Log.d(TAG, "onResponse: fondy_key_storage" + fondy_key_storage);

                        Log.d(TAG, "onResponse: " + logCursor(CITY_INFO).toString());

                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                    Log.e("Request", "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CityResponseMerchantFondy> call, Throwable t) {
                Log.e("Request", "Failed. Error message: " + t.getMessage());
            }
        });
    }

}