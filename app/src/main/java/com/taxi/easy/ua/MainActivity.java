package com.taxi.easy.ua;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.cities.api.CityApiClient;
import com.taxi.easy.ua.cities.api.CityApiTestClient;
import com.taxi.easy.ua.cities.api.CityLastAddressResponse;
import com.taxi.easy.ua.cities.api.CityResponse;
import com.taxi.easy.ua.cities.api.CityResponseMerchantFondy;
import com.taxi.easy.ua.cities.api.CityService;
import com.taxi.easy.ua.cities.check.CityCheckActivity;
import com.taxi.easy.ua.databinding.ActivityMainBinding;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.clear.AppDataUtils;
import com.taxi.easy.ua.ui.finish.RouteResponse;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.ui.settings.SettingsActivity;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetCityFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.ui.open_map.mapbox.key_mapbox.ApiClientMapbox;
import com.taxi.easy.ua.ui.open_map.mapbox.key_mapbox.ApiResponseMapbox;
import com.taxi.easy.ua.ui.visicom.visicom_search.key_visicom.ApiResponse;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;

import com.taxi.easy.ua.utils.LocaleHelper;
import com.taxi.easy.ua.utils.connect.NetworkUtils;

import com.taxi.easy.ua.utils.download.AppUpdater;
import com.taxi.easy.ua.utils.fcm.token_send.ApiServiceToken;
import com.taxi.easy.ua.utils.fcm.token_send.RetrofitClientToken;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.notify.NotificationHelper;
import com.taxi.easy.ua.utils.permissions.UserPermissions;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;
import com.taxi.easy.ua.utils.user.save_firebase.FirebaseUserManager;
import com.taxi.easy.ua.utils.user.save_server.ApiServiceUser;
import com.taxi.easy.ua.utils.user.save_server.UserResponse;
import com.taxi.easy.ua.utils.user.user_verify.VerifyUserTask;

import org.json.JSONException;

import java.io.File;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static String order_id;
    public static String invoiceId;

    public static final String DB_NAME = "data_30072024_0";

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
    public static final String TABLE_WFP_CARDS = "tableWfpCards";
    public static final String TABLE_FONDY_CARDS = "tableFondyCards";
    public static final String TABLE_MONO_CARDS = "tableMonoCards";

    public static final String TABLE_LAST_PUSH = "tableLastPush";
    public static Cursor cursorDb;
    public static boolean firstStart;
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

    public static String apiKeyMapBox;
    public static String apiKey;



    private List<RouteResponse> routeList;

    private boolean gps_upd;
    VisicomFragment visicomFragment;
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences sharedPreferencesCount;
    public static final String PERMISSIONS_PREF_NAME = "Permissions";
    public static final String PERMISSION_REQUEST_COUNT_KEY = "PermissionRequestCount";
    public static boolean location_update;

    private static final String PREFS_NAME_VERSION = "MyPrefsFileNew";
    private static final String LAST_NOTIFICATION_TIME_KEY = "lastNotificationTimeNew";
    //    private static final long ONE_DAY_IN_MILLISECONDS = 0; // 24 часа в миллисекундах
    private static final long ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000; // 24 часа в миллисекундах
//    private static final long ONE_DAY_IN_MILLISECONDS = 60 * 1000; // 1 минута в миллисекундах

    @SuppressLint("StaticFieldLeak")
    public static NavController navController;
    private FirebaseUserManager userManager;

    private String cityMenu;
    private String city;
    private String newTitle;
    public static List<Call<?>> activeCalls = new ArrayList<>();
    public static NavigationView navigationView;

    String baseUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        String localeCode = (String) sharedPreferencesHelperMain.getValue("locale", "uk");
        Logger.i(this, "locale Main", localeCode);
        applyLocale(localeCode);
        super.onCreate(savedInstanceState);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(binding.getRoot());


        deleteOldLogFile();
        Logger.i(this, TAG, "MainActivity started");

        Logger.i(this, TAG, getString(R.string.application));
        Logger.i(this, TAG, getString(R.string.version));

        String model = Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;

        Logger.i(this, TAG, "device: " + model);
        Logger.i(this, TAG, "android_version: " + androidVersion);
        Logger.i(this, TAG, "Build.VERSION.SDK_INT: " + sdkVersion);


        setSupportActionBar(binding.appBarMain.toolbar);

        sharedPreferencesHelperMain = new SharedPreferencesHelper(this);




        DrawerLayout drawer = binding.drawerLayout;
        navigationView = binding.navView;
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
              R.id.nav_visicom, R.id.nav_home, R.id.nav_cancel,
//                R.id.nav_gallery,
              R.id.nav_about, R.id.nav_uid, R.id.nav_bonus, R.id.nav_card,
              R.id.nav_account, R.id.nav_author, R.id.nav_finish_separate
        )
             .setOpenableLayout(drawer)
             .build();
        navMenu = navigationView.getMenu();
        navVisicomMenuItem = navMenu.findItem(R.id.nav_visicom);



        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        networkChangeReceiver = new NetworkChangeReceiver();

        sharedPreferences = getSharedPreferences(MainActivity.PERMISSIONS_PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferencesCount = getSharedPreferences(MainActivity.PERMISSION_REQUEST_COUNT_KEY, Context.MODE_PRIVATE);
// Обработка отсутствия необходимых разрешений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Обработка отсутствия необходимых разрешений
                MainActivity.location_update = true;
            }
        } else MainActivity.location_update = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
       // Проверка обновления


        try {
            initDB();
        } catch (MalformedURLException | JSONException | InterruptedException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

       // Устанавливаем Action Bar, если он доступен
       if (getSupportActionBar() != null) {
           // Устанавливаем пользовательский макет в качестве заголовка Action Bar
           getSupportActionBar().setDisplayShowCustomEnabled(true);
           getSupportActionBar().setDisplayShowTitleEnabled(false); // Отключаем стандартный заголовок
           getSupportActionBar().setCustomView(R.layout.custom_action_bar_title);

           // Доступ к TextView в пользовательском заголовке
           View customView = getSupportActionBar().getCustomView();
           TextView titleTextView = customView.findViewById(R.id.action_bar_title);

           setCityAppbar();

           titleTextView.setText(newTitle);
           // Установка обработчика нажатий
           titleTextView.setOnClickListener(v -> {
               Logger.d(this, TAG, " Установка обработчика нажатий" + NetworkUtils.isNetworkAvailable(getApplicationContext()));
               if (NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                   // Ваш код при нажатии на заголовок
                   MyBottomSheetCityFragment bottomSheetDialogFragment = new MyBottomSheetCityFragment(city, MainActivity.this);
                   bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
               } else {
                   MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                   bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
               }

           });
       }

    }
    private void applyLocale(String localeCode) {
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
    private void setCityAppbar()
    {
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        city = stringList.get(1);
        switch (city){
            case "Kyiv City":
                cityMenu = getString(R.string.city_kyiv);
                break;
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
                cityMenu = getString(R.string.city_cherkassy);
                break;
            case "Lviv":
                cityMenu = getString(R.string.city_lviv);
                break;
            case "Ivano_frankivsk":
                cityMenu = getString(R.string.city_ivano_frankivsk);
                break;
            case "Vinnytsia":
                cityMenu = getString(R.string.city_vinnytsia);
                break;
            case "Poltava":
                cityMenu = getString(R.string.city_poltava);
                break;
            case "Sumy":
                cityMenu = getString(R.string.city_sumy);
                break;
            case "Kharkiv":
                cityMenu = getString(R.string.city_kharkiv);
                break;
            case "Chernihiv":
                cityMenu = getString(R.string.city_chernihiv);
                break;
            case "Rivne":
                cityMenu = getString(R.string.city_rivne);
                break;
            case "Ternopil":
                cityMenu = getString(R.string.city_ternopil);
                break;
            case "Khmelnytskyi":
                cityMenu = getString(R.string.city_khmelnytskyi);
                break;
            case "Zakarpattya":
                cityMenu = getString(R.string.city_zakarpattya);
                break;
            case "Zhytomyr":
                cityMenu = getString(R.string.city_zhytomyr);
                break;
            case "Kropyvnytskyi":
                cityMenu = getString(R.string.city_kropyvnytskyi);
                break;
            case "Mykolaiv":
                cityMenu = getString(R.string.city_mykolaiv);
                break;
            case "Сhernivtsi":
                cityMenu = getString(R.string.city_chernivtsi);
                break;
            case "Lutsk":
                cityMenu = getString(R.string.city_lutsk);
                break;
            case "OdessaTest":
                cityMenu = "Test";
                break;
            default:
                cityMenu = getString(R.string.foreign_countries);
        }
        newTitle =  getString(R.string.menu_city) + " " + cityMenu;
        sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Передаем результаты обратно вашему фрагменту для обработки
        if (visicomFragment != null) {
            visicomFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
            // Ваш код при нажатии на заголовок

            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

        } else  {
            visicomKeyFromFb();
            mapboxKeyFromFb ();
            newUser();
//        if (!sharedPreferencesHelperMain.getValue("CityCheckActivity", "**").equals("run")) {
//            startActivity(new Intent(this, CityCheckActivity.class));
//        } else if (NetworkUtils.isNetworkAvailable(this)) {
//
//        }
            baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                gps_upd = getIntent().getBooleanExtra("gps_upd", true);
            } else {
                gps_upd = false;
            }
            sharedPreferencesHelperMain.saveValue("gps_upd", gps_upd);
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

        Logger.d(this, TAG, "initDB: " + database);

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER_INFO + "(id integer primary key autoincrement," +
                " verifyOrder text," +
                " phone_number text," +
                " email text," +
                " username text," +
                " bonus text," +
                " card_pay text," +
                " bonus_pay text);");

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

//            cityMaxPay("Kyiv City");
//            merchantFondy("Kyiv City");
            if (MainActivity.navVisicomMenuItem != null) {
                // Новый текст элемента меню
                String cityMenu = getString(R.string.city_kyiv);
                String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
                // Изменяем текст элемента меню
                MainActivity.navVisicomMenuItem.setTitle(newTitle);
            }


        }

        database.execSQL("CREATE TABLE IF NOT EXISTS " + ROUT_HOME + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " to_street text," +
                " to_number text);");
        cursorDb = database.query(ROUT_HOME, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            Logger.d(this, TAG, "initDB: ROUT_HOME");
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

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WFP_CARDS + "(id integer primary key autoincrement," +
                " masked_card text," +
                " card_type text," +
                " bank_name text," +
                " rectoken text," +
                " merchant text," +
                " rectoken_check text);");

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_FONDY_CARDS + "(id integer primary key autoincrement," +
                " masked_card text," +
                " card_type text," +
                " bank_name text," +
                " rectoken text," +
                " merchant text," +
                " rectoken_check text);");


        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MONO_CARDS + "(id integer primary key autoincrement," +
                " masked_card text," +
                " card_type text," +
                " bank_name text," +
                " rectoken text," +
                " merchant text," +
                " rectoken_check text);");
        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LAST_PUSH + "(id integer primary key autoincrement," +
                " push_date DATETIME);");


        database.close();

//        if (NetworkUtils.isNetworkAvailable(this)) {
//            // Действия при наличии интернета
//            newUser();
//        }

    }

    public void insertPushDate(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        if (database != null) {
            try {
                // Получаем текущее время и дату
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentDateandTime = sdf.format(new Date());
                Logger.d(getApplicationContext(), TAG, "Current date and time: " + currentDateandTime);

                // Создаем объект ContentValues для передачи данных в базу данных
                ContentValues values = new ContentValues();
                values.put("push_date", currentDateandTime);

                // Пытаемся вставить новую запись. Если запись уже существует, выполняется обновление.
                long rowId = database.insertWithOnConflict(MainActivity.TABLE_LAST_PUSH, null, values, SQLiteDatabase.CONFLICT_REPLACE);

                if (rowId != -1) {
                    Logger.d(getApplicationContext(), TAG, "Insert or update successful");
                } else {
                    Logger.d(getApplicationContext(), TAG, "Error inserting or updating");
                }
            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                database.close();
            }
        }
        assert database != null;
        database.close();
    }
    public void updatePushDate(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        if (database != null) {
            try {
                // Получаем текущее время и дату
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentDateandTime = sdf.format(new Date());
                Logger.d(this, TAG, "Current date and time: " + currentDateandTime);

                // Создаем объект ContentValues для передачи данных в базу данных
                ContentValues values = new ContentValues();
                values.put("push_date", currentDateandTime);

                // Пытаемся вставить новую запись. Если запись уже существует, выполняется обновление.
                int rowsAffected = database.update(MainActivity.TABLE_LAST_PUSH, values, "ROWID=1", null);
                if (rowsAffected > 0) {
                    Logger.d(this, TAG, "Update successful");
                } else {
                    Logger.d(this, TAG, "Error updating");
                }


            } catch (Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            } finally {
                database.close();
            }
        }
        assert database != null;
        database.close();
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

        String sql = "INSERT INTO " + TABLE_USER_INFO + " VALUES(?,?,?,?,?,?,?,?);";
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
            statement.bindString(7, "1");
            statement.bindString(8, "1");

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
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @SuppressLint("IntentReset")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_exit) {
            deleteOldLogFile();
            System.gc();

            finishAffinity(); // Закрывает все активити
            System.exit(0);
        }

        if (item.getItemId() == R.id.gps) {
            eventGps();
        }

        if (item.getItemId() == R.id.settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        if (item.getItemId() == R.id.send_email_admin) {
            sendEmailAdmin();

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
            } catch (android.content.ActivityNotFoundException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }

        }
        if (item.getItemId() == R.id.update) {
            Logger.d(this, TAG, "onOptionsItemSelected: " + getString(R.string.version));
            if (NetworkUtils.isNetworkAvailable(this)) {
                updateApp();

            } else {
                Toast.makeText(this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
            }
        }
        if (item.getItemId() == R.id.nav_driver) {
            if (NetworkUtils.isNetworkAvailable(this)) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxieasyua.job"));
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
            }
        }


        if (item.getItemId() == R.id.send_like) {
            if (NetworkUtils.isNetworkAvailable(this)) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxi.easy.ua"));
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
            }

        }
        if (item.getItemId() == R.id.uninstal_app) {
            AppDataUtils.clearDataAndUninstall(this);

        }
        return false;
    }


    @SuppressLint("StaticFieldLeak")
    private static AppUpdater appUpdater;

    private boolean isUpdateInProgress = false;  // Переменная для отслеживания состояния обновления

    public void updateApp() {
        // Устанавливаем флаг обновления
        appUpdater = new AppUpdater();
        isUpdateInProgress = true;
        Logger.d(this, TAG, "Starting app update process");
        sharedPreferencesHelperMain.saveValue("visible_shed", "ok");
        // Проверка наличия обновлений
        checkForUpdate(this);

        // Устанавливаем слушателя для завершения обновления
        appUpdater.setOnUpdateListener(() -> {
            // Обновление завершено
            Toast.makeText(this, R.string.update_finish_mes, Toast.LENGTH_SHORT).show();

            // Перезапуск приложения после небольшой задержки
            new Handler(Looper.getMainLooper()).postDelayed(this::restartApplication, 1000);

            // Сброс флага обновления
            isUpdateInProgress = false;
        });

        // Регистрация слушателя
        appUpdater.registerListener();

        // Таймаут на случай зависания обновления
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isUpdateInProgress) {
                // Если обновление не завершилось за 10 минут
                Logger.e(this, TAG, "Update process timed out");
                Toast.makeText(this, R.string.update_timeout, Toast.LENGTH_LONG).show();

                // Остановка обновления (если это поддерживается в вашем API)
                stopUpdate();  // Пример: прекращение обновления (в зависимости от используемого механизма)
                isUpdateInProgress = false;
            }
        }, 10 * 60 * 1000); // 10 минут
    }

    private void stopUpdate() {
        // Проверка и остановка обновления (если поддерживается в API)
        Logger.d(this, TAG, "Stopping update...");
        // Например, для сторонних библиотек:
        // appUpdater.stopUpdate();
    }

    private static final int MY_REQUEST_CODE = 1234; // Уникальный код запроса для обновления

    private void checkForUpdate(Context context) {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            Logger.d(context, TAG, "Update availability: " + appUpdateInfo.updateAvailability());
            Logger.d(context, TAG, "Update priority: " + appUpdateInfo.updatePriority());
            Logger.d(context, TAG, "Client version staleness days: " + appUpdateInfo.clientVersionStalenessDays());

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                Logger.d(context, TAG, "Available updates found");

                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            (Activity) context,
                            MY_REQUEST_CODE
                    );
                } catch (IntentSender.SendIntentException e) {
                    Logger.e(context, TAG, "Failed to start immediate update: " + e.getMessage());
                    FirebaseCrashlytics.getInstance().recordException(e);

                    // Попытка запуска гибкого обновления
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.FLEXIBLE,
                                (Activity) context,
                                MY_REQUEST_CODE
                        );
                    } catch (IntentSender.SendIntentException ex) {
                        Logger.e(context, TAG, "Failed to start flexible update: " + ex.getMessage());
                        FirebaseCrashlytics.getInstance().recordException(ex);
                        Toast.makeText(context, R.string.update_error, Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                Logger.d(context, TAG, "No updates available");
                String message = getString(R.string.update_ok);
                MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        }).addOnFailureListener(e -> {
            Logger.e(context, TAG, "Failed to check for updates: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(context, R.string.update_error, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Logger.d(this, TAG, "Update successful!");
                Toast.makeText(this, R.string.update_successful, Toast.LENGTH_SHORT).show();
            } else {
                Logger.e(this, TAG, "Update flow failed! Result code: " + resultCode);

                // Обработка ошибки обновления
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, R.string.update_canceled, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.update_failed, Toast.LENGTH_LONG).show();
                }

                // Логирование дополнительной информации о процессе
                if (data != null) {
                    Logger.d(this, TAG, "Intent data: " + data.toString());
                }
            }
        }
    }

    private void restartApplication() {
        try {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            int pendingIntentId = 123456;
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    pendingIntentId,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pendingIntent); // Задержка 1 секунда
                Logger.d(this, TAG, "Application restart scheduled.");
            } else {
                Logger.e(this, TAG, "AlarmManager is not available!");
            }

            // Завершение текущего процесса приложения
            System.exit(0); // Завершение текущего процесса
        } catch (Exception e) {
            Logger.e(this, TAG, "Error during app restart: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(this, R.string.restart_error, Toast.LENGTH_LONG).show();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Отмена регистрации слушателя при уничтожении активности
        if (appUpdater != null) {
            appUpdater.unregisterListener();
        }
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

        String body = getString(R.string.SA_message_start) + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" +
                getString(R.string.SA_info_pas) + "\n" +
                getString(R.string.SA_info_city) + " " + city + "\n" +
                getString(R.string.SA_pas_text) + " " + getString(R.string.version) + "\n" +
                getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
                getString(R.string.SA_email) + " " + userList.get(3) + "\n" +
                getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n" + "\n";

        String[] CC = {"cartaxi4@gmail.com"};
        String[] TO = {"taxi.easy.ua@gmail.com"};

        File logFile = new File(getExternalFilesDir(null), "app_log.txt");

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (logFile.exists()) {
            Uri uri = FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", logFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            Logger.e(this, "MainActivity", "Log file does not exist");
        }
        try {
            startActivity(Intent.createChooser(emailIntent, subject));
        } catch (android.content.ActivityNotFoundException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }


    }

    private void deleteOldLogFile() {
        File logFile = new File(getExternalFilesDir(null), "app_log.txt");
        if (logFile.exists()) {
            logFile.delete();
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

        Logger.d(this, TAG, "onOptionsItemSelected gps_enabled: " + gps_enabled);
        Logger.d(this, TAG, "onOptionsItemSelected network_enabled: " + network_enabled);
        if(!gps_enabled || !network_enabled) {
            MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        } else {
            Toast.makeText(this, getString(R.string.gps_ok), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateRecordsUser(String field, String result) {
        ContentValues cv = new ContentValues();

        cv.put(field, result);

        // обновляем по id
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();



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
        Logger.d(this, TAG, "newUser: " + userEmail);

//        new Thread(this::mapboxKey).start();
//        new Thread(this::visicomKey).start();
        new Thread(() -> insertPushDate(getApplicationContext())).start();

        Logger.d(this, TAG, "CityCheckActivity: " + sharedPreferencesHelperMain.getValue("CityCheckActivity", "**"));

        if(userEmail.equals("email")) {
            firstStart = true;

            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(this, R.string.checking, Toast.LENGTH_SHORT).show();
            startFireBase();
        } else {
            firstStart = false;

            new Thread(this::versionFromMarket).start();
            new Thread(this::userPhoneFromFb).start();
            new Thread(() -> updatePushDate(getApplicationContext())).start();

            UserPermissions.getPermissions(userEmail, getApplicationContext());

            Thread wfpCardThread = new Thread(() -> {
                List<String> stringList = logCursor(MainActivity.CITY_INFO);
                String city = stringList.get(1);
                if(city != null) {
                    getCardTokenWfp(city,"wfp", userEmail);
                }
            });
            wfpCardThread.start();

            Thread sendTokenThread = new Thread(() -> {
                sendToken(userEmail);
            });
            sendTokenThread.start();
        }
    }

    private  void getCardTokenWfp(String city, String pay_system, String email) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl) // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        // Создайте сервис
        CallbackServiceWfp service = retrofit.create(CallbackServiceWfp.class);
        Logger.d(this, TAG, "getCardTokenWfp: ");
        // Выполните запрос
        Call<CallbackResponseWfp> call = service.handleCallbackWfp(
                getString(R.string.application),
                city,
                email,
                pay_system
        );
        call.enqueue(new Callback<CallbackResponseWfp>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
                Logger.d(getApplicationContext(), TAG, "onResponse: " + response.body());
                if (response.isSuccessful()) {
                    CallbackResponseWfp callbackResponse = response.body();
                    if (callbackResponse != null) {
                        List<CardInfo> cards = callbackResponse.getCards();
                        Logger.d(getApplicationContext(), TAG, "onResponse: cards" + cards);
                        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.delete(MainActivity.TABLE_WFP_CARDS, "1", null);
                        if (cards != null && !cards.isEmpty()) {
                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты
                                String merchant = cardInfo.getMerchant(); // Токен карты

                                Logger.d(getApplicationContext(), TAG, "onResponse: card_token: " + rectoken);
                                ContentValues cv = new ContentValues();
                                cv.put("masked_card", masked_card);
                                cv.put("card_type", card_type);
                                cv.put("bank_name", bank_name);
                                cv.put("rectoken", rectoken);
                                cv.put("merchant", merchant);
                                cv.put("rectoken_check", "0");
                                database.insert(MainActivity.TABLE_WFP_CARDS, null, cv);
                            }
                            Cursor cursor = database.rawQuery("SELECT * FROM " + MainActivity.TABLE_WFP_CARDS + " ORDER BY id DESC LIMIT 1", null);
                            if (cursor != null && cursor.moveToFirst()) {
                                // Получаем значение ID последней записи
                                @SuppressLint("Range") int lastId = cursor.getInt(cursor.getColumnIndex("id"));
                                cursor.close();

                                // Обновляем строку с найденным ID
                                ContentValues cv = new ContentValues();
                                cv.put("rectoken_check", "1");
                                database.update(MainActivity.TABLE_WFP_CARDS, cv, "id = ?", new String[] { String.valueOf(lastId) });
                            }

                            database.close();
                        }
                        database.close();
                    }

                } else {
                    // Обработка случаев, когда ответ не 200 OK
                    Logger.d(getApplicationContext(), TAG, "onResponse: getCardTokenWfp error ");
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
                Logger.d(getApplicationContext(), TAG, "onResponse:getCardTokenWfp onFailure" + t);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }
    private void startFireBase() {
        Toast.makeText(this, R.string.account_verify, Toast.LENGTH_SHORT).show();
        startSignInInBackground();
    }
    private void startSignInInBackground() {
        Thread thread = new Thread(() -> {
            try {
            Logger.d(getApplicationContext(), TAG, "run: ");
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Collections.singletonList(
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            // Create and launch sign-in intent
            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build();

                runOnUiThread(() -> signInLauncher.launch(signInIntent));
            } catch (Exception e) {
                Logger.e(getApplicationContext(), TAG, "Exception during sign-in launch " + e);
                FirebaseCrashlytics.getInstance().recordException(e);
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            }
        });
        thread.start();
    }

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(

            new FirebaseAuthUIActivityResultContract(),
            result -> {
                try {
                    onSignInResult(result, getSupportFragmentManager());
                } catch (MalformedURLException | JSONException | InterruptedException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    Logger.d(this, TAG, "onCreate:" + new RuntimeException(e));
                }
            }
    );


    private void onSignInResult(FirebaseAuthUIAuthenticationResult result, FragmentManager fm) throws MalformedURLException, JSONException, InterruptedException {
        ContentValues cv = new ContentValues();
        Logger.d(this, TAG, "onSignInResult: ");
        try {
            Logger.d(this, TAG, "onSignInResult: result.getResultCode() " + result.getResultCode());
            if (result.getResultCode() == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                assert user != null;
                settingsNewUser(user.getEmail());

                startActivity(new Intent(this, CityCheckActivity.class));

//                lastAddressUser();

          } else {
                IdpResponse response = result.getIdpResponse();
                if (response == null) {
                    Logger.d(this, TAG, "Sign-in canceled by user.");
                } else {
                    Logger.d(this, TAG, "Sign-in error: " + response.getError().getMessage());
                    FirebaseCrashlytics.getInstance().recordException(response.getError());
                }

                VisicomFragment.progressBar.setVisibility(View.GONE);
                cv.put("verifyOrder", "0");
                SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                database.close();
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
            VisicomFragment.progressBar.setVisibility(View.GONE);
            cv.put("verifyOrder", "0");
            SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void settingsNewUser (String emailUser) {
        // Assuming this code is inside a method or a runnable block

// Task 1: Update user info in a separate thread
        Thread updateUserInfoThread = new Thread(() -> {
            ContentValues cv = new ContentValues();
            updateRecordsUserInfo("email", emailUser, getApplicationContext());
            cv.put("verifyOrder", "1");
            SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
            Logger.d(this, TAG, "settingsNewUser" + emailUser);
        });
        updateUserInfoThread.start();

        Thread sendTokenThread = new Thread(() -> {
            sendToken(emailUser);
        });
        sendTokenThread.start();

// Task 2: Add user with no name in a separate thread
        Thread addUserNoNameThread = new Thread(() -> {
            addUserNoName(emailUser, getApplicationContext());
        });
        addUserNoNameThread.start();

// Task 3: Fetch user phone information from the server in a separate thread
        //            userPhoneFromServer(emailUser);
        new Thread(this::userPhoneFromFb).start();

// Task 4: Get card token for "fondy" in a separate thread
//        Thread fondyCardThread = new Thread(() -> {
//            getCardToken("fondy", TABLE_FONDY_CARDS, emailUser);
//
//        });
//        fondyCardThread.start();
//        Thread wfpCardThread = new Thread(() -> {
//            List<String> stringList = logCursor(MainActivity.CITY_INFO);
//            String city = stringList.get(1);
//            getCardTokenWfp("OdessaTest","wfp", emailUser);
//
//        });
//        wfpCardThread.start();

// Task 5: Get card token for "mono" in a separate thread
//        Thread monoCardThread = new Thread(() -> {
//            getCardToken("mono", TABLE_MONO_CARDS, email);
//        });
//        monoCardThread.start();

// Wait for all threads to finish (optional)
        try {
            updateUserInfoThread.join();
            addUserNoNameThread.join();
//            fondyCardThread.join();
//            monoCardThread.join();
        } catch (InterruptedException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

    }


    public static void addUserNoName(String email, Context context) {
        // Создание объекта Retrofit
        String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Создание экземпляра ApiService
        ApiServiceUser apiService = retrofit.create(ApiServiceUser.class);

        // Вызов метода addUserNoName
        Call<UserResponse> call = apiService.addUserNoName(email, context.getString(R.string.application));

        // Асинхронный вызов
        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful()) {
                    UserResponse userResponse = response.body();
                    if (userResponse != null) {
                        updateRecordsUserInfo("username", userResponse.getUserName(), context);

                    }
                } else {
                    updateRecordsUserInfo("username", "no_name", context);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }
    private static void updateRecordsUserInfo(String userInfo, String result, Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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



    private void userPhoneFromFb ()
    {
        userManager = new FirebaseUserManager();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userManager.getUserPhoneById(userId, new FirebaseUserManager.UserPhoneCallback() {
                @Override
                public void onUserPhoneRetrieved(String phone) {
                    if (phone != null) {
                        // Используйте phone по своему усмотрению
                        Logger.d(getApplicationContext(), TAG, "User phone: " + phone);
                        String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
                        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();

                        if (val) {
                            updateRecordsUser("phone_number", phone);
                        } else {
                            // Handle case where phone doesn't match the pattern
                            Logger.d(getApplicationContext(), TAG, "Phone does not match pattern");
                        }
                    } else {
                        Logger.d(getApplicationContext(), TAG, "Phone is null");
                    }
                }
            });
        }
    }

    private void visicomKeyFromFb()
    {
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.getVisicomKey(new FirestoreHelper.OnVisicomKeyFetchedListener() {
            @Override
            public void onSuccess(String vKey) {
                // Обработка успешного получения ключа
                MainActivity.apiKey = vKey;
                Logger.d(getApplicationContext(),TAG, "Visicom Key: " + vKey);
            }

            @Override
            public void onFailure(Exception e) {
                // Обработка ошибок
                Logger.e(getApplicationContext(),TAG, "Ошибка: " + e.getMessage());
            }
        });

    }

    private void mapboxKeyFromFb()
    {
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.getMapboxKey(new FirestoreHelper.OnMapboxKeyFetchedListener() {
            @Override
            public void onSuccess(String mKey) {
                // Обработка успешного получения ключа
                MainActivity.apiKeyMapBox = mKey;
                Logger.d(getApplicationContext(),TAG, "Mapbox Key: " + apiKeyMapBox);
            }

            @Override
            public void onFailure(Exception e) {
                // Обработка ошибок
                Logger.e(getApplicationContext(),TAG, "Ошибка: " + e.getMessage());
            }
        });

    }

    private void sendToken (String email) {
        // Создаем экземпляр Retrofit

        Logger.d(getApplicationContext(),TAG, "sendToken email " + email);

        SharedPreferences sharedPreferences = getSharedPreferences("UserTokenPrefs", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("token","");

        Logger.d(getApplicationContext(),TAG, "sendToken token" + token );

        if(!token.isEmpty()) {
            baseUrl  = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
            ApiServiceToken apiService = RetrofitClientToken.getClient(baseUrl).create(ApiServiceToken.class);

            String app = getApplicationContext().getString(R.string.application);

            Call<Void> call = apiService.sendToken(email, app, token, LocaleHelper.getLocale());

            // Выполняем асинхронный запрос
            call.enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Logger.d(getApplicationContext(),TAG, "Токен " + token + "успешно отправлен на сервер");
                    } else {
                        Logger.e(getApplicationContext(),TAG, "Ошибка отправки токена на сервер: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Logger.e(getApplicationContext(),TAG, "Ошибка отправки токена на сервер: " + t);
                }
            });
        }
    }


    private void versionFromMarket()  {
        // Получаем SharedPreferences
        SharedPreferences SharedPreferences = getSharedPreferences(PREFS_NAME_VERSION, Context.MODE_PRIVATE);
        // Получаем время последней отправки уведомления
        long lastNotificationTime = SharedPreferences.getLong(LAST_NOTIFICATION_TIME_KEY, 0);
        // Получаем текущее время
        long currentTime = System.currentTimeMillis();
        // Проверяем, прошло ли уже 24 часа с момента последней отправки
        if (currentTime - lastNotificationTime >= ONE_DAY_IN_MILLISECONDS) {
            checkForUpdateForPush(SharedPreferences, currentTime);
        }
    }
    private void checkForUpdateForPush(
            SharedPreferences SharedPreferences,
            long currentTime
    ) {
        // Обновляем время последней отправки уведомления
        SharedPreferences.Editor editor = SharedPreferences.edit();
        editor.putLong(LAST_NOTIFICATION_TIME_KEY, currentTime);
        editor.apply();

        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Доступны обновления
                Logger.d(getApplicationContext(), TAG, "Available updates found");
                String title = getString(R.string.new_version);
                String messageNotif = getString(R.string.news_of_version);

                String urlStr = "https://play.google.com/store/apps/details?id=com.taxi.easy.ua";
                NotificationHelper.showNotification(MainActivity.this, title, messageNotif, urlStr);
            }
        });
    }
    private void checkForUpdateForBtn() {


        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Доступны обновления

                Logger.d(getApplicationContext(), TAG, "Available updates found");
                String title = getString(R.string.new_version);
                String messageNotif = getString(R.string.news_of_version);

                String urlStr = "https://play.google.com/store/apps/details?id=com.taxi.easy.ua";
                NotificationHelper.showNotification(MainActivity.this, title, messageNotif, urlStr);
            }
        });
    }


    private void mapboxKey() {
        ApiClientMapbox.getMapboxKeyInfo(new Callback<ApiResponseMapbox>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponseMapbox> call, @NonNull Response<ApiResponseMapbox> response) {
                if (response.isSuccessful()) {
                    ApiResponseMapbox apiResponse = response.body();
                    if (apiResponse != null) {
                        String keyMaxbox = apiResponse.getKeyMapbox();
                        Logger.d(getApplicationContext(),"ApiResponseMapbox", "keyMapbox: " + keyMaxbox);
                        MainActivity.apiKeyMapBox = keyMaxbox;
                        // Теперь у вас есть ключ Visicom для дальнейшего использования
                    }
                } else {
                    // Обработка ошибки
                    Logger.d(getApplicationContext(),"mapboxKey", "Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponseMapbox> call, @NonNull Throwable t) {
                // Обработка ошибки
                Logger.d(getApplicationContext(),"ApiResponseMapbox", "Failed to make API call" + t);
            }
        }, getString(R.string.application)
        );
    }

    private void visicomKey() {
        com.taxi.easy.ua.ui.visicom.visicom_search.key_visicom.ApiClient.getVisicomKeyInfo(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse != null) {
                        String keyVisicom = apiResponse.getKeyVisicom();
                        Logger.d(getApplicationContext(),"ApiResponse", "keyVisicom: " + keyVisicom);
                        MainActivity.apiKey = keyVisicom;
                    }
                } else {
                    // Обработка ошибки
                    Logger.d(getApplicationContext(),"visicomKey", "Error: " + response.code());

                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                // Обработка ошибки
                Logger.d(getApplicationContext(),"visicomKey", "Failed to make API call" + t);
            }
        },getString(R.string.application)
        );
    }

//    private void lastAddressUser() {
//
//        String cityString = logCursor(MainActivity.CITY_INFO).get(1);
//
//        String email = logCursor(MainActivity.TABLE_USER_INFO).get(3);
//        CityService cityService;
//
//        Logger.d(this, TAG, "lastAddressUser: cityString" + cityString);
//        switch (cityString){
//            case "OdessaTest":
//                cityService = CityApiTestClient.getClient().create(CityService.class);
//                break;
//            default:
//                cityService = CityApiClient.getClient().create(CityService.class);
//        }
//
//        Call<CityLastAddressResponse> call = cityService.lastAddressUser(email, cityString, this.getString(R.string.application));
//
//        call.enqueue(new Callback<CityLastAddressResponse>() {
//            @Override
//            public void onResponse(@NonNull Call<CityLastAddressResponse> call, @NonNull Response<CityLastAddressResponse> response) {
//                if (response.isSuccessful()) {
//                    CityLastAddressResponse cityResponse = response.body();
//                    Logger.d(getApplicationContext(), TAG, "onResponse: cityResponse" + cityResponse);
//                    if (cityResponse != null) {
//                        String routefrom = cityResponse.getRoutefrom();
//                        String startLat = cityResponse.getStartLat();
//                        String startLan = cityResponse.getStartLan();
//
//
//                        Logger.d(getApplicationContext(), TAG, "lastAddressUser: routefrom" + routefrom);
//                        Logger.d(getApplicationContext(), TAG, "lastAddressUser: startLat" + startLat);
//                        Logger.d(getApplicationContext(), TAG, "lastAddressUser: startLan" + startLan);
//                        if(routefrom.equals("*") || startLat.equals("0.0") || startLan.equals("0.0")) {
//                            updateMyPosition(cityString);
//                        } else {
//                            updateMyLatsPosition(routefrom, startLat, startLan, cityString);
//                        }
//
//                    }
//                } else {
//                    Logger.d(getApplicationContext(), TAG, "Failed. Error code: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(Call<CityLastAddressResponse> call, Throwable t) {
//                Logger.d(getApplicationContext(), TAG, "Failed. Error message: " + t.getMessage());
//                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
//            }
//        });
//    }
//
//    private void updateMyPosition(String city) {
//
//        double startLat;
//        double startLan;
//        String position;
//        Logger.d(getApplicationContext(), TAG, "updateMyPosition:city "+ city);
//
//
//        switch (city) {
//            case "Kyiv City":
//                position = getApplicationContext().getString(R.string.pos_k);
//                startLat = 50.451107;
//                startLan = 30.524907;
//
//                break;
//            case "Dnipropetrovsk Oblast":
//                // Днепр
//                position = getApplicationContext().getString(R.string.pos_d);
//                startLat = 48.4647;
//                startLan = 35.0462;
//
//                break;
//            case "Odessa":
//                position = getApplicationContext().getString(R.string.pos_o);
//                startLat = 46.4694;
//                startLan = 30.7404;
//
//                break;
//            case "Zaporizhzhia":
//                position = getApplicationContext().getString(R.string.pos_z);
//                startLat = 47.84015;
//                startLan = 35.13634;
//
//                break;
//            case "Cherkasy Oblast":
//                position = getApplicationContext().getString(R.string.pos_c);
//                startLat = 49.44469;
//                startLan = 32.05728;
//
//                break;
//            case "Lviv":
//                position = getApplicationContext().getString(R.string.pos_l);
//                startLat = 49.83993;
//                startLan = 24.02973;
//
//                break;
//            case "Ivano_frankivsk":
//                position = getApplicationContext().getString(R.string.pos_if);
//                startLat = 48.92005;
//                startLan = 24.71067;
//
//                break;
//            case "Vinnytsia":
//                position = getApplicationContext().getString(R.string.pos_v);
//                startLat = 49.23325;
//                startLan = 28.46865;
//
//                break;
//            case "Poltava":
//                position = getApplicationContext().getString(R.string.pos_p);
//                startLat = 49.59325;
//                startLan = 34.54938;
//
//                break;
//            case "Sumy":
//                position = getApplicationContext().getString(R.string.pos_s);
//                startLat = 50.90775;
//                startLan = 34.79865;
//
//                break;
//            case "Kharkiv":
//                position = getApplicationContext().getString(R.string.pos_h);
//                startLat = 49.99358;
//                startLan = 36.23191;
//
//                break;
//            case "Chernihiv":
//                position = getApplicationContext().getString(R.string.pos_ch);
//                startLat = 51.4933;
//                startLan = 31.2972;
//
//                break;
//            case "Rivne":
//                position = getApplicationContext().getString(R.string.pos_r);
//                startLat = 50.6198;
//                startLan = 26.2406;
//
//                break;
//            case "Ternopil":
//                position = getApplicationContext().getString(R.string.pos_t);
//                startLat = 49.54479;
//                startLan = 25.5990;
//
//                break;
//            case "Khmelnytskyi":
//                position = getApplicationContext().getString(R.string.pos_kh);
//                startLat = 49.41548;
//                startLan = 27.00674;
//
//                break;
//
//            case "Zakarpattya":
//                position = getApplicationContext().getString(R.string.pos_uz);
//                startLat = 48.61913;
//                startLan = 22.29475;
//
//                break;
//            case "Zhytomyr":
//                position = getApplicationContext().getString(R.string.pos_zt);
//                startLat = 50.26801;
//                startLan = 28.68026;
//
//                break;
//            case "Kropyvnytskyi":
//                position = getApplicationContext().getString(R.string.pos_kr);
//                startLat = 48.51159;
//                startLan = 32.26982;
//
//                break;
//            case "Mykolaiv":
//                position = getApplicationContext().getString(R.string.pos_m);
//                startLat = 46.97498;
//                startLan = 31.99378;
//
//                break;
//            case "Сhernivtsi":
//                position = getApplicationContext().getString(R.string.pos_chr);
//                startLat = 48.29306;
//                startLan = 25.93484;
//
//                break;
//            case "Lutsk":
//                position = getApplicationContext().getString(R.string.pos_ltk);
//                startLat = 50.73968;
//                startLan = 25.32400;
//
//                break;
//
//            case "OdessaTest":
//                position = getApplicationContext().getString(R.string.pos_o);
//                startLat = 46.4694;
//                startLan = 30.7404;
//
//                break;
//
//            default:
//                position = getApplicationContext().getString(R.string.pos_f);
//                startLat = 52.13472;
//                startLan = 21.00424;
//        }
//
//
//        SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//
//        ContentValues cv = new ContentValues();
//        cv.put("startLat", startLat);
//        cv.put("startLan", startLan);
//        cv.put("position", position);
//        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
//                new String[] { "1" });
//
//        cv = new ContentValues();
//        cv.put("tarif", " ");
//        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
//                new String[] { "1" });
//
//        cv = new ContentValues();
//        cv.put("payment_type", "nal_payment");
//
//        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
//                new String[] { "1" });
//        database.close();
//
//
//        List<String> settings = new ArrayList<>();
//
//
//        settings.add(Double.toString(startLat));
//        settings.add(Double.toString(startLan));
//        settings.add(Double.toString(startLat));
//        settings.add(Double.toString(startLan));
//        settings.add(position);
//        settings.add(position);
//
//        updateRoutMarker(settings);
//
//        String userEmail = logCursor(MainActivity.TABLE_USER_INFO ).get(3);
//        Logger.d(getApplicationContext(), TAG, "newUser: " + userEmail);
//
//    }
//
//    private void updateMyLatsPosition(String routefrom, String startLatString, String startLanString, String city) {
//
//        double startLat = Double.parseDouble(startLatString);;
//        double startLan = Double.parseDouble(startLanString);
//        String position = routefrom;
//        Logger.d(getApplicationContext(), TAG, "updateMyPosition:city "+ city);
//
//
//        SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//
//        ContentValues cv = new ContentValues();
//        cv.put("startLat", startLat);
//        cv.put("startLan", startLan);
//        cv.put("position", position);
//        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
//                new String[] { "1" });
//
//        cv = new ContentValues();
//        cv.put("tarif", " ");
//        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
//                new String[] { "1" });
//
//        cv = new ContentValues();
//        cv.put("payment_type", "nal_payment");
//
//        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
//                new String[] { "1" });
//        database.close();
//
//        List<String> settings = new ArrayList<>();
//
//
//        settings.add(Double.toString(startLat));
//        settings.add(Double.toString(startLan));
//        settings.add(Double.toString(startLat));
//        settings.add(Double.toString(startLan));
//        settings.add(position);
//        settings.add(position);
//
//        updateRoutMarker(settings);
//
//        String userEmail = logCursor(MainActivity.TABLE_USER_INFO ).get(3);
//        Logger.d(getApplicationContext(), TAG, "newUser: " + userEmail);
//
//    }
//
//    private void updateRoutMarker(List<String> settings) {
//        Logger.d(getApplicationContext(), TAG, "updateRoutMarker: " + settings.toString());
//        ContentValues cv = new ContentValues();
//
//        cv.put("startLat", Double.parseDouble(settings.get(0)));
//        cv.put("startLan", Double.parseDouble(settings.get(1)));
//        cv.put("to_lat", Double.parseDouble(settings.get(2)));
//        cv.put("to_lng", Double.parseDouble(settings.get(3)));
//        cv.put("start", settings.get(4));
//        cv.put("finish", settings.get(5));
//
//        // обновляем по id
//        SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
//                new String[]{"1"});
//        database.close();
//    }
}