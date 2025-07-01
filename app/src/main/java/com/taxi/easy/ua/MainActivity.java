package com.taxi.easy.ua;

import static android.view.View.GONE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.ActivityMainBinding;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.cities.api.CityApiClient;
import com.taxi.easy.ua.ui.cities.api.CityResponse;
import com.taxi.easy.ua.ui.cities.api.CityService;
import com.taxi.easy.ua.ui.cities.check.CityCheckActivity;
import com.taxi.easy.ua.ui.clear.AppDataUtils;
import com.taxi.easy.ua.ui.finish.OrderResponse;
import com.taxi.easy.ua.ui.finish.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.utils.connect.NetworkMonitor;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.download.AppUpdater;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.notify.NotificationHelper;
import com.taxi.easy.ua.utils.permissions.UserPermissions;
import com.taxi.easy.ua.utils.pusher.PusherManager;
import com.taxi.easy.ua.utils.user.del_server.ApiUserService;
import com.taxi.easy.ua.utils.user.del_server.CallbackUser;
import com.taxi.easy.ua.utils.user.del_server.RetrofitClient;
import com.taxi.easy.ua.utils.user.del_server.UserFindResponse;
import com.taxi.easy.ua.utils.worker.AddUserNoNameWorker;
import com.taxi.easy.ua.utils.worker.GetCardTokenWfpWorker;
import com.taxi.easy.ua.utils.worker.InsertPushDateWorker;
import com.taxi.easy.ua.utils.worker.SendTokenWorker;
import com.taxi.easy.ua.utils.worker.UpdatePushDateWorker;
import com.taxi.easy.ua.utils.worker.UpdateUserInfoWorker;
import com.taxi.easy.ua.utils.worker.UserPhoneFromFbWorker;
import com.taxi.easy.ua.utils.worker.VersionFromMarketWorker;

import org.json.JSONException;

import java.io.File;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static String supportEmail;

    @SuppressLint("StaticFieldLeak")
    private static AppUpdater appUpdater;

    public static String order_id;
    public static String invoiceId;

    public static final String DB_NAME = "data_09022025_9";

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
    public static String uid;
    public static String uid_Double;

    public static String paySystemStatus = "nal_payment";
    private AppBarConfiguration mAppBarConfiguration;

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


    VisicomFragment visicomFragment;

    public static Map<String, String> costMap;

    public static final String PERMISSION_REQUEST_COUNT_KEY = "PermissionRequestCount";
    public static boolean location_update;


    @SuppressLint("StaticFieldLeak")
    public static NavController navController;

    private String city;
    private String newTitle;
    public static List<Call<?>> activeCalls = new ArrayList<>();
    NavigationView navigationView;

    String baseUrl;
    @SuppressLint("StaticFieldLeak")
    public static PusherManager pusherManager;
    public ExecutionStatusViewModel viewModel;
    public static OrderResponse orderResponse;

    public static int currentNavDestination = -1; // ID текущего экрана
    ActivityMainBinding binding;
    static AppUpdateManager appUpdateManager;
    public ActivityResultLauncher<Intent> exactAlarmLauncher;
    public ActivityResultLauncher<Intent> batteryOptimizationLauncher;
    private NetworkMonitor networkMonitor;

//    ExecutorService executor;
    Constraints constraints;
    public static ImageButton button1;
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Установка локали перед вызовом super.onCreate()
        String localeCode = (String) MyApplication.sharedPreferencesHelperMain.getValue("locale", Locale.getDefault().getLanguage());
        applyLocale(localeCode);
        super.onCreate(savedInstanceState);

        // Инициализация View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(binding.getRoot());

        // Установка Toolbar как ActionBar
        setSupportActionBar(binding.appBarMain.toolbar);

        // Настройка Navigation
        DrawerLayout drawer = binding.drawerLayout;
        navigationView = binding.navView;
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        // Добавляем слушатель изменения направления
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            currentNavDestination = destination.getId(); // Обновляем текущий экран
        });

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_visicom,
                R.id.nav_home,
                R.id.nav_cancel,
                R.id.nav_about,
                R.id.nav_uid,
                R.id.nav_bonus,
                R.id.nav_card,
                R.id.nav_account,
                R.id.nav_author,
                R.id.nav_finish_separate,
                R.id.nav_restart,
                R.id.nav_search,
                R.id.nav_cacheOrder,
                R.id.nav_map,
                R.id.nav_city,
                R.id.nav_settings,
                R.id.nav_visicom_options,
                R.id.nav_anr
        ).setOpenableLayout(drawer).build();

        // Связывание Navigation с UI
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Инициализация меню и элементов
        navMenu = navigationView.getMenu();
        navVisicomMenuItem = navMenu.findItem(R.id.nav_visicom);

        // Явная обработка нажатий в NavigationView
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            NavDestination currentDestination = navController.getCurrentDestination();
            if (currentDestination != null && currentDestination.getId() != itemId) {
                navController.popBackStack(); // Очистить стек
                navController.navigate(itemId);
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });

        // Инициализация ViewModel
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this).get(ExecutionStatusViewModel.class);
            sharedPreferencesHelperMain.saveValue("setStatusX", true);
        }

        // Инициализация ActivityResultLauncher
        exactAlarmLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Logger.d(this, "MainActivity", "Exact Alarm permission result received");
                });

        batteryOptimizationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Logger.d(this, "MainActivity", "Battery optimization ignore permission result received");
                });

        // Логирование и очистка старых логов
        Logger.i(this, TAG, "*******************************************************************************");
        Logger.i(this, TAG, "MainActivity started");
        Logger.i(this, TAG, getString(R.string.application));
        Logger.i(this, TAG, getString(R.string.version));

        // Логирование информации об устройстве
        String model = Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;
        int sdkVersion = Build.VERSION.SDK_INT;
        Logger.i(this, TAG, "device: " + model);
        Logger.i(this, TAG, "android_version: " + androidVersion);
        Logger.i(this, TAG, "Build.VERSION.SDK_INT: " + sdkVersion);

        // Инициализация ресивера для отслеживания сети

        networkMonitor = new NetworkMonitor(this);
        networkMonitor.startMonitoring(this);
        constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        try {
            initDB();
        } catch (MalformedURLException | JSONException | InterruptedException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        // Настройка Action Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setCustomView(R.layout.custom_action_bar_title);

            // Доступ к элементам кастомного Action Bar
            View customView = getSupportActionBar().getCustomView();
            TextView titleTextView = customView.findViewById(R.id.action_bar_title);
            button1 = customView.findViewById(R.id.button1);

            setCityAppbar(); // Предполагается, что метод существует и корректен
            titleTextView.setText(newTitle);


            // Установка обработчиков нажатий
            View.OnClickListener clickListener = v -> {
                Logger.d(this, TAG, "Обработчик нажатия, сеть доступна: " + NetworkUtils.isNetworkAvailable(this));
                if (NetworkUtils.isNetworkAvailable(this)) {
                    Logger.d(this, "CityCheckFrgment", "Navigating to nav_city");
                    if (navController.getCurrentDestination().getId() != R.id.nav_finish_separate) {
                        Logger.d(this, "CityCheckFrgment", "Navigating to nav_city");
                        navController.navigate(R.id.nav_city, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_city, true)
                                .build());
                    }
                } else if (navController != null) {
                    currentNavDestination = R.id.nav_restart;
                    navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());
                } else {
                    Logger.e(this, TAG, "NavController равен null, навигация невозможна!");
                }
            };
            if (Objects.requireNonNull(navController.getCurrentDestination()).getId() != R.id.nav_finish_separate) {
                Logger.d(this, "CityCheckFrgment", "Navigating to nav_city");
                titleTextView.setOnClickListener(clickListener);
                button1.setOnClickListener(clickListener);
            }


        }

        // Проверка разрешений на доступ к местоположению
        MainActivity.location_update = checkLocationPermission();

        sharedPreferencesHelperMain.saveValue("time", "no_time");
        sharedPreferencesHelperMain.saveValue("date", "no_date");
        sharedPreferencesHelperMain.saveValue("comment", "no_comment");


    }

    // Вспомогательный метод для проверки разрешений
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void setCityAppbar()
    {
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        city = stringList.get(1);
        String cityMenu;
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
            case "Chernivtsi":
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
//            visicomFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            visicomFragment.requestPermissions();
        }
    }




    @Override
    protected void onResume() {
        super.onResume();

        costMap = null;

        appUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);

        appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate();
            }
        }).addOnFailureListener(e -> {
            Logger.e(this, TAG, "Ошибка проверки обновлений: " + e.getMessage());
        });


        sharedPreferencesHelperMain.saveValue("pay_error", "**");



        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        boolean gps_upd;
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            gps_upd = getIntent().getBooleanExtra("gps_upd", true);
        } else {
            gps_upd = false;
        }
        sharedPreferencesHelperMain.saveValue("gps_upd", gps_upd);


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
            settings.add("Kyiv City"); //1
            settings.add(api); //2
            settings.add(Kyiv_City_phone); //3
            settings.add("5000"); //4
            settings.add("500000"); //5
            settings.add(""); //6
            settings.add(""); //7
            insertCity(settings);

            cityMaxPay();
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

        if (NetworkUtils.isNetworkAvailable(this)) {
            // Действия при наличии интернета
            newUser();
        }

    }

    private void cityMaxPay() {

        String BASE_URL =sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

        CityApiClient cityApiClient = new CityApiClient(BASE_URL);
        CityService cityService = cityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponse> call = cityService.getMaxPayValues("Kyiv City", getString(R.string.application));

        call.enqueue(new Callback<CityResponse>() {
            @Override
            public void onResponse(@NonNull Call<CityResponse> call, @NonNull Response<CityResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CityResponse cityResponse = response.body();
                    int cardMaxPay = cityResponse.getCardMaxPay();
                    int bonusMaxPay = cityResponse.getBonusMaxPay();
                    String black_list = cityResponse.getBlack_list();

                    ContentValues cv = new ContentValues();
                    cv.put("card_max_pay", cardMaxPay);
                    cv.put("bonus_max_pay", bonusMaxPay);
                    sharedPreferencesHelperMain.saveValue("black_list", black_list);
                    Logger.d(getApplication(), TAG, "black_list 2" + black_list);
                    SQLiteDatabase database = getApplication().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    database.update(MainActivity.CITY_INFO, cv, "id = ?",
                            new String[]{"1"});

                    database.close();


                    // Добавьте здесь код для обработки полученных значений
                } else {
                    Logger.d(getApplication(), TAG, "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(getApplication(), TAG, "Failed. Error message: " + t.getMessage());
            }
        });
    }



    public void updatePushDate(Context context) {
        SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
                new Handler(Looper.getMainLooper()).post(() -> {
                    FirebaseCrashlytics.getInstance().recordException(e);
                });
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
            statement.bindString(3, "+38");
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
    public ActivityResultLauncher<Intent> getExactAlarmLauncher() {
        return exactAlarmLauncher;
    }

    public ActivityResultLauncher<Intent> getBatteryOptimizationLauncher() {
        return batteryOptimizationLauncher;
    }

    @SuppressLint("IntentReset")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_exit) {
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(MyApplication.getContext());
            firebaseAnalytics.setAnalyticsCollectionEnabled(false);

            deleteOldLogFile();
//            System.gc();

            finishAffinity(); // Закрывает все активити
            System.exit(0);
        }


        if (item.getItemId() == R.id.clearApp) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.clearAppMess) // Например: "Подтверждение"
                    .setMessage(R.string.clearAppMess) // Текст сообщения, например: "Вы уверены, что хотите очистить приложение?"
                    .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                        clearApplication(this);
                    })
                    .setNegativeButton(R.string.cancel_button, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
            return true;
        }

        if (item.getItemId() == R.id.gps) {
            eventGps();
        }

        if (item.getItemId() == R.id.settings) {

            navController.navigate(R.id.nav_settings, null, new NavOptions.Builder()
                    .build());

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

//            AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);
            Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
            appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                    String message = getString(R.string.update_ok);
                    MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                } else {
                    if (NetworkUtils.isNetworkAvailable(this)) {
                        appUpdater = new AppUpdater(
                                this,
                                this.getExactAlarmLauncher(),
                                this.getBatteryOptimizationLauncher()
                        );
                        appUpdater.startUpdate();
                    }
                }
            });



        }
        if (item.getItemId() == R.id.nav_driver) {
            if (NetworkUtils.isNetworkAvailable(this)) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxieasyua.job"));
                startActivity(browserIntent);
            }
        }


        if (item.getItemId() == R.id.send_like) {
            if (NetworkUtils.isNetworkAvailable(this)) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxi.easy.ua"));
                startActivity(browserIntent);
            }

        }
        if (item.getItemId() == R.id.uninstal_app) {
            AppDataUtils.delApp(this);

        }
        return false;
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
        String[] TO = {supportEmail};

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
        } catch(Exception ignored) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ignored) {}

        Logger.d(this, TAG, "onOptionsItemSelected gps_enabled: " + gps_enabled);
        Logger.d(this, TAG, "onOptionsItemSelected network_enabled: " + network_enabled);
        if(!gps_enabled) {
            MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        } else {
            Toast.makeText(this, getString(R.string.gps_ok), Toast.LENGTH_SHORT).show();
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);

        }
    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        Logger.d(this, TAG, "checkPermission: " + permission);
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, LOCATION_PERMISSION_REQUEST_CODE);
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
        super.onStart();
    }

    @Override
    protected void onStop() {
        WorkManager.getInstance(this).cancelAllWork(); // Отмена всех задач WorkManager
        Logger.d(this, TAG, "Все задачи WorkManager отменены в onStop");
        super.onStop();

        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
        }
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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


    private void insertPushDateWorkerTask() {
        OneTimeWorkRequest insertPushDateRequest = new OneTimeWorkRequest.Builder(InsertPushDateWorker.class)
                .setConstraints(constraints)
                .build();

        // Запуск задачи через WorkManager
        WorkManager.getInstance(this).enqueue(insertPushDateRequest);

        // Отслеживание завершения задачи
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(insertPushDateRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        Logger.d(this, TAG, "Задача insertPushDate завершена, состояние: " + workInfo.getState());
                    }
                });
    }


    public void newUser() {

        insertPushDateWorkerTask();

        String userEmail = logCursor(TABLE_USER_INFO).get(3);
        Logger.d(this, TAG, "newUser: " + userEmail);

        findUserFromServer(userEmail, findUser -> {
            // Use the boolean result here
            Log.d(TAG, "User exists: " + findUser);
            Logger.d(MainActivity.this, TAG, "CityCheckActivity: " + sharedPreferencesHelperMain.getValue("CityCheckActivity", "**"));

            if(userEmail.equals("email") || !findUser) {
                firstStart = true;

                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
                Toast.makeText(MainActivity.this, R.string.checking, Toast.LENGTH_SHORT).show();
                startFireBase();
            } else {
                String sityCheckActivity = (String) sharedPreferencesHelperMain.getValue("CityCheckActivity", "**");
                Logger.d(this, TAG, "CityCheckActivity: " + sityCheckActivity);

                if (sityCheckActivity.equals("**")) {
                    // Запускаем CityCheckActivity, если состояние страны не задано
                    Intent intent = new Intent(this, CityCheckActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                firstStart = false;

                // Инициализация и подключение к Pusher
                pusherManager = new PusherManager(
                        getString(R.string.application),
                        userEmail,
                        MainActivity.this,
                        viewModel
                );
                pusherManager.connect();
                pusherManager.subscribeToChannel();

                OneTimeWorkRequest versionFromMarketRequest = new OneTimeWorkRequest.Builder(VersionFromMarketWorker.class)
                        .setConstraints(constraints)
                        .build();

                OneTimeWorkRequest userPhoneFromFbRequest = new OneTimeWorkRequest.Builder(UserPhoneFromFbWorker.class)
                        .setConstraints(constraints)
                        .build();

                OneTimeWorkRequest updatePushDateRequest = new OneTimeWorkRequest.Builder(UpdatePushDateWorker.class)
                        .setConstraints(constraints)
                        .build();

                OneTimeWorkRequest getCardTokenWfpRequest = new OneTimeWorkRequest.Builder(GetCardTokenWfpWorker.class)
                        .setConstraints(constraints)
                        .setInputData(new Data.Builder()
                                .putString("city", city)
                                .build())
                        .build();

                OneTimeWorkRequest sendTokenRequest = new OneTimeWorkRequest.Builder(SendTokenWorker.class)
                        .setConstraints(constraints)
                        .setInputData(new Data.Builder()
                                .putString("userEmail", userEmail)
                                .build())
                        .build();

                // Запуск задач через WorkManager
                WorkManager.getInstance(this)
                        .beginWith(versionFromMarketRequest)
                        .then(userPhoneFromFbRequest)
                        .then(updatePushDateRequest)
                        .then(getCardTokenWfpRequest)
                        .then(sendTokenRequest)
                        .enqueue();

                // Отслеживание завершения задач
                WorkManager.getInstance(this).getWorkInfoByIdLiveData(sendTokenRequest.getId())
                        .observe(this, workInfo -> {
                            if (workInfo != null && workInfo.getState().isFinished()) {
                                Log.d(TAG, "Все задачи завершены, состояние: " + workInfo.getState());
                            }
                        });

                UserPermissions.getPermissions(userEmail, getApplicationContext());
            }
        });



    }

    public void findUserFromServer(String userEmail, CallbackUser<Boolean> resultCallback) {
        ApiUserService apiService = RetrofitClient.getRetrofitInstance().create(ApiUserService.class);

        Call<UserFindResponse> call = apiService.findUser(userEmail);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<UserFindResponse> call, @NonNull Response<UserFindResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserFindResponse serverResponse = response.body();
                    if (serverResponse.getCheckUser() != null) {
                        // Возвращаем значение checkUser через callback
                        resultCallback.onResult(serverResponse.getCheckUser());
                        Log.d(TAG, "Успех: Пользователь найден = " + serverResponse.getCheckUser());
                    } else {
                        // Обработка пустого или некорректного ответа
                        resultCallback.onResult(false);
                        Log.d(TAG, "Ошибка: Некорректный ответ от сервера");
                    }
                } else {
                    // Обработка HTTP-ошибки
                    resultCallback.onResult(false);
                    Log.d(TAG, "Запрос не удался, код: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserFindResponse> call, @NonNull Throwable t) {
                resultCallback.onResult(false);
                FirebaseCrashlytics.getInstance().recordException(t);
                Log.d(TAG, "Ошибка: " + t.getMessage());
            }
        });
    }


    public void getCardTokenWfp(String city) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor()) // 3 попытки
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
        Logger.d(getApplicationContext(), TAG, "getCardTokenWfp: ");
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO).get(3);

        Call<CallbackResponseWfp> call = service.handleCallbackWfpCardsId(
                getApplicationContext().getString(R.string.application),
                city,
                userEmail,
                "wfp"
        );

        try {
            Response<CallbackResponseWfp> response = call.execute();
            Logger.d(getApplicationContext(), TAG, "onResponse: " + response.body());
            if (response.isSuccessful() && response.body() != null) {
                CallbackResponseWfp callbackResponse = response.body();
                List<CardInfo> cards = callbackResponse.getCards();
                Logger.d(getApplicationContext(), TAG, "onResponse: cards" + cards);

                SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
                database.execSQL("DELETE FROM " + MainActivity.TABLE_WFP_CARDS + ";");

                if (cards != null && !cards.isEmpty()) {
                    for (CardInfo cardInfo : cards) {
                        String masked_card = cardInfo.getMasked_card();
                        String card_type = cardInfo.getCard_type();
                        String bank_name = cardInfo.getBank_name();
                        String rectoken = cardInfo.getRectoken();
                        String merchant = cardInfo.getMerchant();
                        String active = cardInfo.getActive();

                        Logger.d(getApplicationContext(), TAG, "onResponse: card_token: " + rectoken);
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
                Logger.d(getApplicationContext(), TAG, "onResponse: not successful, code: " + response.code());
            }
        } catch (Exception e) {
            Logger.d(getApplicationContext(), TAG, "onResponse: failure " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
    }
}

    private void startFireBase() {
        Toast.makeText(this, R.string.account_verify, Toast.LENGTH_SHORT).show();
        startSignIn();
    }

    private void startSignIn() {
        try {
            Logger.d(getApplicationContext(), TAG, "run: ");
            List<AuthUI.IdpConfig> providers = Collections.singletonList(
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build();

            signInLauncher.launch(signInIntent);
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Exception during sign-in launch " + e);
            FirebaseCrashlytics.getInstance().recordException(e);
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }
    }


    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(

            new FirebaseAuthUIActivityResultContract(),
            result -> {
                onSignInResult(result, getSupportFragmentManager());
            }
    );

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result, FragmentManager fm) {
        ContentValues cv = new ContentValues();
        Logger.d(this, TAG, "onSignInResult: ");

        // Попробуем выполнить вход
        try {
            int resultCode = result.getResultCode();
            Logger.d(this, TAG, "onSignInResult: result.getResultCode() " + resultCode);

            if (resultCode == RESULT_OK) {
                // Успешный вход
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                if (user != null) {
                    settingsNewUser(user.getEmail());

                    String sityCheckActivity = (String) sharedPreferencesHelperMain.getValue("CityCheckActivity", "**");
                    Logger.d(this, TAG, "CityCheckActivity: " + sityCheckActivity);

                    if (sityCheckActivity.equals("**")) {
                        // Запускаем CityCheckActivity, если состояние страны не задано
                        Intent intent = new Intent(this, CityCheckActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    pusherManager = new PusherManager(
                            getString(R.string.application),
                            user.getEmail(),
                            this,
                            viewModel
                    );
                    pusherManager.connect();
                    pusherManager.subscribeToChannel();
                }
            } else {
                handleSignInFailure(result);
            }
        } catch (Exception e) {
            handleException(e, cv);
        } finally {
            // Убедимся, что индикатор прогресса скрыт и данные обновлены в базе
//            hideProgressBarAndUpdateDatabase(cv);
        }
    }

    // Метод обработки ошибок при входе
    private void handleSignInFailure(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (response == null) {
            Logger.d(this, TAG, "Sign-in canceled by user.");
        } else {
            Logger.d(this, TAG, "Sign-in error: " + response.getError().getMessage());
            FirebaseCrashlytics.getInstance().recordException(response.getError());
        }
    }

    // Метод для обработки исключений
    private void handleException(Exception e, ContentValues cv) {
        FirebaseCrashlytics.getInstance().recordException(e);
        Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
        hideProgressBarAndUpdateDatabase(cv);
    }

    // Метод для скрытия индикатора прогресса и обновления базы данных
    private void hideProgressBarAndUpdateDatabase(ContentValues cv) {
        VisicomFragment.progressBar.setVisibility(GONE);
        cv.put("verifyOrder", "0");
        SQLiteDatabase database = null;
        try {
            database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
        } catch (Exception e) {
            Logger.e(this, TAG, "Database update error");
        } finally {
            if (database != null) {
                database.close();
            }
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }
    }

//    private void settingsNewUser (String emailUser) {
//        // Assuming this code is inside a method or a runnable block
//
//// Task 1: Update user info in a separate thread
//        Thread updateUserInfoThread = new Thread(() -> {
//            ContentValues cv = new ContentValues();
//            updateRecordsUserInfo("email", emailUser, getApplicationContext());
//            cv.put("verifyOrder", "1");
//            SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
//            database.close();
//            Logger.d(this, TAG, "settingsNewUser" + emailUser);
//        });
//        updateUserInfoThread.start();
//
//        Thread sendTokenThread = new Thread(() -> {
//            sendToken(emailUser);
//        });
//        sendTokenThread.start();
//
//// Task 2: Add user with no name in a separate thread
//        Thread addUserNoNameThread = new Thread(() -> {
//            addUserNoName(emailUser, getApplicationContext());
//        });
//        addUserNoNameThread.start();
//
//// Task 3: Fetch user phone information from the server in a separate thread
//        //            userPhoneFromServer(emailUser);
//        new Thread(this::userPhoneFromFb).start();
//
//
//// Wait for all threads to finish (optional)
//        try {
//            updateUserInfoThread.join();
//            addUserNoNameThread.join();
////            fondyCardThread.join();
////            monoCardThread.join();
//        } catch (InterruptedException e) {
//            FirebaseCrashlytics.getInstance().recordException(e);
//        }
//
//    }
    private void settingsNewUser(String emailUser) {

           // Создание запросов для задач settingsNewUser
        OneTimeWorkRequest updateUserInfoRequest = new OneTimeWorkRequest.Builder(UpdateUserInfoWorker.class)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putString("emailUser", emailUser)
                        .build())
                .build();

        OneTimeWorkRequest sendTokenRequest = new OneTimeWorkRequest.Builder(SendTokenWorker.class)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putString("userEmail", emailUser)
                        .build())
                .build();

        OneTimeWorkRequest addUserNoNameRequest = new OneTimeWorkRequest.Builder(AddUserNoNameWorker.class)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putString("emailUser", emailUser)
                        .build())
                .build();

        OneTimeWorkRequest userPhoneFromFbRequest = new OneTimeWorkRequest.Builder(UserPhoneFromFbWorker.class)
                .setConstraints(constraints)
                .build();

        // Запуск задач через WorkManager
        WorkManager.getInstance(this)
                .beginWith(updateUserInfoRequest)
                .then(sendTokenRequest)
                .then(addUserNoNameRequest)
                .then(userPhoneFromFbRequest)
                .enqueue();

        // Отслеживание завершения задач
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(userPhoneFromFbRequest.getId())
                .observe(this, workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        Log.d(TAG, "Задачи settingsNewUser завершены, состояние: " + workInfo.getState());
                    }
                });
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
            statement.bindDouble(2, 50.451107);
            statement.bindDouble(3, 30.524907);
            statement.bindDouble(4, 50.451107);
            statement.bindDouble(5, 30.524907);
            statement.bindString(6, getString(R.string.pos_k));
            statement.bindString(7, getString(R.string.pos_k));


            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
        database.close();
    }



//    public void userPhoneFromFb()
//    {
//        FirebaseUserManager userManager = new FirebaseUserManager();
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//
//        if (currentUser != null) {
//            String userId = currentUser.getUid();
//            userManager.getUserPhoneById(userId, phone -> {
//                if (phone != null) {
//                    // Используйте phone по своему усмотрению
//                    Logger.d(getApplicationContext(), TAG, "User phone: " + phone);
//                    String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
//                    boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
//
//                    if (val) {
//                        updateRecordsUser("phone_number", phone);
//                    } else {
//                        // Handle case where phone doesn't match the pattern
//                        Logger.d(getApplicationContext(), TAG, "Phone does not match pattern");
//                    }
//                } else {
//                    Logger.d(getApplicationContext(), TAG, "Phone is null");
//                }
//            });
//        }
//    }

    public static void checkForUpdateForPush(
            SharedPreferences SharedPreferences,
            long currentTime,
            String LAST_NOTIFICATION_TIME_KEY

    ) {
        // Обновляем время последней отправки уведомления
        SharedPreferences.Editor editor = SharedPreferences.edit();
        editor.putLong(LAST_NOTIFICATION_TIME_KEY, currentTime);
        editor.apply();


        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Доступны обновления
                Logger.d(MyApplication.getContext(), TAG, "Available updates found");
                String title = MyApplication.getContext().getString(R.string.new_version);
                String messageNotif = MyApplication.getContext().getString(R.string.news_of_version);

                String urlStr = "https://play.google.com/store/apps/details?id=com.taxi.easy.ua";
                NotificationHelper.showNotification(MyApplication.getContext(), title, messageNotif, urlStr);
            }
        });
    }
    @Override
    protected void onPause() {
        super.onPause();


    }

    private void applyLocale(String localeCode) {
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);

        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    void clearApplication(Context context) {
        Logger.d(context, TAG, "Starting clearApplication");
        clearAllSharedPreferences(context);
        clearAllDatabases(context);
        clearAllCache(context);
        clearAllExternalCache(context);

        // Restart the application
        try {
            Logger.d(context, TAG, "Initiating application restart");
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                System.exit(0); // Terminate the current process
            } else {
                Logger.d(context, TAG, "Could not find launch intent for package: " + context.getPackageName());
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Error during application restart: " + e.toString());
        }
        Logger.d(context, TAG, "Completed clearApplication");
    }

    // Clears all SharedPreferences files for the app
    void clearAllSharedPreferences(Context context) {
        Logger.d(context, TAG, "Starting clearAllSharedPreferences");
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        String prefsDir = context.getApplicationInfo().dataDir + "/shared_prefs";
        File dir = new File(prefsDir);

        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String file : files) {
                    if (file.endsWith(".xml")) {
                        String prefName = file.substring(0, file.length() - 4);
                        Logger.d(context, TAG, "Clearing SharedPreferences: " + prefName);
                        try {
                            SharedPreferences prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear();
                            editor.apply();
                            Logger.d(context, TAG, "Cleared SharedPreferences: " + prefName);
                        } catch (Exception e) {
                            Logger.e(context, TAG, "Error clearing SharedPreferences " + prefName + ": " + e.toString());
                        }
                    }
                }
            } else {
                Logger.d(context, TAG, "No SharedPreferences files found or unable to list files in: " + prefsDir);
            }
        } else {
            Logger.d(context, TAG, "SharedPreferences directory does not exist or is not a directory: " + prefsDir);
        }
        Logger.d(context, TAG, "Completed clearAllSharedPreferences");
    }

    // Clears all database files for the app
    void clearAllDatabases(Context context) {
        context.deleteDatabase(DB_NAME);
        Logger.d(context, TAG, "Starting clearAllDatabases");

        // Close any open database connections (example for SQLiteOpenHelper)
        // Replace with your actual database helper class
        if (database != null && database.isOpen()) {
            database.close();
        }

        Logger.d(context, TAG, "Closed database helper");


        String dbDir = context.getApplicationInfo().dataDir + "/databases";
        File dir = new File(dbDir);

        if (dir.exists() && dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String file : files) {
                    // Include additional database-related extensions
                    if (file.endsWith(".db") || file.endsWith(".sqlite") ||
                            file.endsWith(".db-journal") || file.endsWith(".db-wal") || file.endsWith(".db-shm")) {
                        Logger.d(context, TAG, "Deleting database: " + file);
                        try {
                            if (context.deleteDatabase(file)) {
                                Logger.d(context, TAG, "Deleted database: " + file);
                            } else {
                                Logger.d(context, TAG, "Failed to delete database: " + file);
                            }
                        } catch (Exception e) {
                            Logger.e(context, TAG, "Error deleting database " + file + ": " + e.toString());
                        }
                    }
                }
            } else {
                Logger.d(context, TAG, "No database files found or unable to list files in: " + dbDir);
            }
        } else {
            Logger.d(context, TAG, "Database directory does not exist or is not a directory: " + dbDir);
        }
        Logger.d(context, TAG, "Completed clearAllDatabases");
    }

    // Clears all cache files for the app
    void clearAllCache(Context context) {
        if (context == null) {
            Logger.e(context, TAG, "Context is null in clearAllCache");
            throw new IllegalArgumentException("Context cannot be null");
        }
        Logger.d(context, TAG, "Starting clearAllCache");
        File cacheDir = context.getCacheDir();
        if (cacheDir != null && cacheDir.isDirectory()) {
            if (deleteRecursive(cacheDir, context)) {
                Logger.d(context, TAG, "Cleared internal cache directory: " + cacheDir.getAbsolutePath());
            } else {
                Logger.d(context, TAG, "Failed to clear internal cache directory: " + cacheDir.getAbsolutePath());
            }
        } else {
            Logger.d(context, TAG, "Internal cache directory is null or not a directory");
        }
        Logger.d(context, TAG, "Completed clearAllCache");
    }

    // Clears all external cache files for the app
    void clearAllExternalCache(Context context) {
        if (context == null) {
            Logger.e(context, TAG, "Context is null in clearAllExternalCache");
            throw new IllegalArgumentException("Context cannot be null");
        }
        Logger.d(context, TAG, "Starting clearAllExternalCache");
        File externalCacheDir = context.getExternalCacheDir();
        if (externalCacheDir != null && externalCacheDir.isDirectory()) {
            if (deleteRecursive(externalCacheDir, context)) {
                Logger.d(context, TAG, "Cleared external cache directory: " + externalCacheDir.getAbsolutePath());
            } else {
                Logger.d(context, TAG, "Failed to clear external cache directory: " + externalCacheDir.getAbsolutePath());
            }
        } else {
            Logger.d(context, TAG, "External cache directory is null or not a directory");
        }
        Logger.d(context, TAG, "Completed clearAllExternalCache");
    }

    // Recursively deletes files and directories, returns true if successful
    boolean deleteRecursive(File fileOrDir, Context context) {
        if (fileOrDir == null) {
            Logger.d(context, TAG, "File or directory is null in deleteRecursive");
            return false;
        }
        boolean success = true;
        try {
            if (fileOrDir.isDirectory()) {
                File[] files = fileOrDir.listFiles();
                if (files != null) {
                    for (File child : files) {
                        Logger.d(context, TAG, "Attempting to delete: " + child.getAbsolutePath());
                        success &= deleteRecursive(child, context);
                    }
                } else {
                    Logger.d(context, TAG, "Unable to list files in directory: " + fileOrDir.getAbsolutePath());
                }
            }
            if (fileOrDir.delete()) {
                Logger.d(context, TAG, "Deleted: " + fileOrDir.getAbsolutePath());
            } else {
                Logger.d(context, TAG, "Failed to delete: " + fileOrDir.getAbsolutePath());
                success = false;
            }
        } catch (SecurityException e) {
            Logger.e(context, TAG, "SecurityException while deleting: " + fileOrDir.getAbsolutePath() + " " + e.toString());
            success = false;
        } catch (Exception e) {
            Logger.e(context, TAG, "Unexpected error while deleting: " + fileOrDir.getAbsolutePath() + " " + e.toString());
            success = false;
        }
        return success;
    }


}