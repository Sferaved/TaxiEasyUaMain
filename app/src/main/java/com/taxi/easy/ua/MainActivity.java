package com.taxi.easy.ua;

import static android.view.View.GONE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.getContext;
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
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.InstallErrorCode;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.ActivityMainBinding;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.cities.api.CityApiClient;
import com.taxi.easy.ua.ui.cities.api.CityResponse;
import com.taxi.easy.ua.ui.cities.api.CityService;
import com.taxi.easy.ua.ui.cities.check.CityCheckActivity;
import com.taxi.easy.ua.ui.clear.AppDataUtils;
import com.taxi.easy.ua.ui.finish.OrderResponse;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.ui.weather.WeatherApiHelper;
import com.taxi.easy.ua.ui.weather.WeatherResponse;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.utils.bugreport.BugReportHelper;
import com.taxi.easy.ua.utils.centrifugo.CentrifugoManager;
import com.taxi.easy.ua.utils.connect.NetworkMonitor;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.download.AppUpdater;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.utils.model.OrderViewModel;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.notify.NotificationHelper;
import com.taxi.easy.ua.utils.permissions.UserPermissions;
import com.taxi.easy.ua.utils.phone_state.PhoneCallHelper;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;
import com.taxi.easy.ua.utils.pusher.PusherManager;
import com.taxi.easy.ua.utils.review.AppReviewManager;
import com.taxi.easy.ua.utils.user.del_server.ApiUserService;
import com.taxi.easy.ua.utils.user.del_server.CallbackUser;
import com.taxi.easy.ua.utils.user.del_server.RetrofitClient;
import com.taxi.easy.ua.utils.user.del_server.UserFindResponse;
import com.taxi.easy.ua.utils.user.user_verify.VerifyUserTask;
import com.taxi.easy.ua.utils.worker.AddUserNoNameWorker;
import com.taxi.easy.ua.utils.worker.CheckPushPermissionWorker;
import com.taxi.easy.ua.utils.worker.GetCardTokenWfpWorker;
import com.taxi.easy.ua.utils.worker.InclusiveTransportPreferenceWorker;
import com.taxi.easy.ua.utils.worker.InsertPushDateWorker;
import com.taxi.easy.ua.utils.worker.UpdatePushDateWorker;
import com.taxi.easy.ua.utils.worker.UpdateUserInfoWorker;
import com.taxi.easy.ua.utils.worker.UserPhoneFromFbWorker;
import com.taxi.easy.ua.utils.worker.VersionFromMarketWorker;
import com.taxi.easy.ua.utils.worker.utils.SaveIPWithEmailUtils;
import com.taxi.easy.ua.utils.worker.utils.TokenUtils;
import com.taxi.easy.ua.widget.WeatherNotificationHelper;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


// OkHttp


// Java / Android


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static String supportEmail;

    @SuppressLint("StaticFieldLeak")
    private static AppUpdater appUpdater;

    public static String order_id;
    public static String invoiceId;

    public static final String DB_NAME = "data_21042026_0";

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

    public static final String api = "apiTest";

    /**
     * Phone section
     */
    public static final String Kyiv_City_phone = "tel:0674443804";

    public static SQLiteDatabase database;
    public static Menu navMenu;
    public static MenuItem navVisicomMenuItem;

    public static String apiKeyMapBox;
    public static String apiKey;
    public static String weatherKey;

    VisicomFragment visicomFragment;

    public static Map<String, String> costMap;


    public static final String PERMISSION_REQUEST_COUNT_KEY = "PermissionRequestCount";
    public static boolean location_update;


    @SuppressLint("StaticFieldLeak")
    public static NavController navController;

    String city;
    public String newTitle;
    public static List<Call<?>> activeCalls = new ArrayList<>();
    NavigationView navigationView;

    String baseUrl;
    @SuppressLint("StaticFieldLeak")
    public static PusherManager pusherManager;
    public static ExecutionStatusViewModel viewModel;
    public static OrderResponse orderResponse;

    public static int currentNavDestination = -1; // ID текущего экрана
    ActivityMainBinding binding;
    static AppUpdateManager appUpdateManager;
    public ActivityResultLauncher<Intent> exactAlarmLauncher;
    public ActivityResultLauncher<Intent> batteryOptimizationLauncher;
    private NetworkMonitor networkMonitor;
    public static OrderViewModel orderViewModel;
    //    ExecutorService executor;
    Constraints constraints;
    @SuppressLint("StaticFieldLeak")
    public static ImageButton button1;
    private CentrifugoManager centrifugoManager;
    private Snackbar noInternetSnackbar;
    private boolean isSnackbarShowing = false;
    private AppReviewManager appReviewManager;
    public AppReviewManager getAppReviewManager() {
        return appReviewManager;
    }
    private BugReportHelper bugReportHelper;
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Установка локали перед вызовом super.onCreate()
        String localeCode = (String) MyApplication.sharedPreferencesHelperMain.getValue("locale", Locale.getDefault().getLanguage());
        applyLocale(localeCode);
        super.onCreate(savedInstanceState);
        bugReportHelper = new BugReportHelper(this);
        PhoneCallHelper.initWithActivity(this);
        PhoneCallHelper.ensureCallPermission();
        if (getIntent() != null && getIntent().hasExtra("shortcut_action")) {
            String action = getIntent().getStringExtra("shortcut_action");

            switch (action) {
                case "order":
                    openOrderScreen();
                    break;
                case "driver":
                    openDriverScreen();
                    break;
                case "weather_notification":  // ДОБАВЬТЕ ЭТОТ КЕЙС 👇
                    sendWeatherNotificationFromShortcut();
                    break;

            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
// Ініціалізація менеджера оцінювання
        appReviewManager = new AppReviewManager(this);

        orderViewModel = new ViewModelProvider(this).get(OrderViewModel.class);


        // Проверка, есть ли сохранённый "pending" orderCost


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
                R.id.nav_search,
                R.id.nav_cacheOrder,
                R.id.nav_map,
                R.id.nav_city,
                R.id.nav_settings,
                R.id.nav_options,
                R.id.nav_anr,
                R.id.nav_weather
        ).setOpenableLayout(drawer).build();

        // Связывание Navigation с UI
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        if (getIntent() != null && getIntent().getBooleanExtra("open_weather", false)) {
            navController.navigate(R.id.nav_visicom);
        }
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

        viewModel = new ViewModelProvider(this).get(ExecutionStatusViewModel.class);
        sharedPreferencesHelperMain.saveValue("setStatusX", true);


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
        networkMonitor.setListener(isConnected -> {
            runOnUiThread(() -> {
                if (isConnected) {
                    hideNoInternetSnackbar();
                } else {
                    showNoInternetSnackbar();
                }
            });
        });
        networkMonitor.startMonitoring();
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
                } else {
                    Toast.makeText(this, R.string.network_no_internet, Toast.LENGTH_LONG).show();
                    Logger.w(this, TAG, "NO INTERNET - Showing toast message");
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

    /**
     * Отправка уведомления о погоде из контекстного меню (App Shortcut)
     */
    private void sendWeatherNotificationFromShortcut() {
        Logger.d(this, TAG, "Weather notification requested from shortcut");

        // Проверяем разрешение на уведомления для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Запрашиваем разрешение
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
                Toast.makeText(this, "Будь ласка, надайте дозвіл на сповіщення", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Получаем кэшированную погоду
        WeatherResponse weather = WeatherApiHelper.getCachedWeather(this);

        if (weather != null && weather.getMain() != null) {
            // Получаем город
            String cityName = getCurrentCityName();

            // Отправляем уведомление
            WeatherNotificationHelper.showWeatherNotification(this, weather, cityName);

            Toast.makeText(this, "☁️ Сповіщення про погоду надіслано", Toast.LENGTH_SHORT).show();
            Logger.d(this, TAG, "Weather notification sent from shortcut");
        } else {
            // Нет кэша - пробуем загрузить
            Toast.makeText(this, "⏳ Завантаження погоди...", Toast.LENGTH_SHORT).show();

            String apiKey = WeatherApiHelper.getApiKey(this);
            if (apiKey != null && !apiKey.isEmpty()) {
                String city = getCurrentCityName();
                WeatherApiHelper.fetchWeatherAsync(this, city, apiKey, new WeatherApiHelper.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherResponse w) {
                        // Сохраняем в кэш
                        WeatherApiHelper.cacheWeather(MainActivity.this, w);

                        String cityName = getCurrentCityName();
                        WeatherNotificationHelper.showWeatherNotification(MainActivity.this, w, cityName);

                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "☁️ Сповіщення надіслано", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onFailure(String error) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, "❌ Помилка завантаження погоди", Toast.LENGTH_SHORT).show());
                        Logger.e(MainActivity.this, TAG, "Weather fetch failed: " + error);
                    }
                });
            } else {
                Toast.makeText(this, "❌ Ключ погоди не знайдено", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Получает название текущего города для отображения
     */
    private String getCurrentCityName() {

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city = stringList.get(1);
        String cityMenu;
        switch (city){
            case "Kyiv City":
                cityMenu =  getString(R.string.Kyiv_city);
                break;
            case "Dnipropetrovsk Oblast":
                cityMenu = getString(R.string.Dnipro_city);
                break;
            case "Odessa":
            case "OdessaTest":
                cityMenu = getString(R.string.city_odessa);
                break;
            case "Zaporizhzhia":
                cityMenu = getString(R.string.city_zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                cityMenu = getString(R.string.Cherkasy);
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
            default:
                cityMenu = getString(R.string.Kyiv_city);
        }


//        return getCityName(city);
        return  cityMenu;
    }

    /**
     * Получает название текущего города для API
     */
    private String getCurrentCityForApi() {
        List<String> stringList = logCursor(CITY_INFO);
        if (stringList == null || stringList.size() < 2) {
            return "Kyiv";
        }

        String city = stringList.get(1);
        switch (city) {
            case "Kyiv City": return "Kyiv";
            case "Dnipropetrovsk Oblast": return "Dnipro";
            case "Odessa": return "Odessa";
            case "Lviv": return "Lviv";
            case "Kharkiv": return "Kharkiv";
            default: return "Kyiv";
        }
    }

    private void showNoInternetSnackbar() {
        // Не показываем, если уже показываем или Activity уничтожена
        if (isSnackbarShowing || isFinishing() || isDestroyed()) {
            return;
        }

        // Находим корневое View
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) {
            return;
        }

        try {
            // Сначала скрываем старый, если есть
            if (noInternetSnackbar != null && noInternetSnackbar.isShown()) {
                noInternetSnackbar.dismiss();
            }

            noInternetSnackbar = Snackbar.make(rootView, R.string.network_no_internet, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.retry, v -> {
                        if (networkMonitor != null) {
                            networkMonitor.forceCheck();
                        }
                        if (NetworkUtils.isNetworkAvailable(MainActivity.this)) {
                            Toast.makeText(MainActivity.this, R.string.network_available_check, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.network_still_down, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setActionTextColor(ContextCompat.getColor(this, R.color.white))
                    .setTextColor(ContextCompat.getColor(this, R.color.white));

            View snackbarView = noInternetSnackbar.getView();
            if (snackbarView != null) {
                snackbarView.setBackgroundColor(ContextCompat.getColor(this, R.color.error_red));
            }

            noInternetSnackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    isSnackbarShowing = false;
                    noInternetSnackbar = null;  // ← важно: обнуляем ссылку
                }

                @Override
                public void onShown(Snackbar transientBottomBar) {
                    isSnackbarShowing = true;
                }
            });

            noInternetSnackbar.show();
            Logger.d(this, TAG, "No internet Snackbar shown");

        } catch (Exception e) {
            Logger.e(this, TAG, "Error showing snackbar: " + e.getMessage());
        }
    }

    private void hideNoInternetSnackbar() {
        // Проверяем через флаг и прямо через snackbar
        if (noInternetSnackbar != null && noInternetSnackbar.isShown()) {
            try {
                noInternetSnackbar.dismiss();
                // isSnackbarShowing сбросится в onDismissed
                Logger.d(this, TAG, "No internet Snackbar hidden");
            } catch (Exception e) {
                Logger.e(this, TAG, "Error hiding snackbar: " + e.getMessage());
            }
        } else {
            // Флаг мог остаться true, а snackbar уже нет - сбрасываем
            if (isSnackbarShowing) {
                isSnackbarShowing = false;
                Logger.d(this, TAG, "Snackbar flag reset");
            }
        }
    }

    /**
     * Обновляет Snackbar при смене фрагмента (чтобы привязать к новому корневому View)
     */
    private void refreshSnackbarIfNeeded() {
        if (isSnackbarShowing && !NetworkUtils.isNetworkAvailable(this)) {
            // Скрываем текущий и показываем новый
            hideNoInternetSnackbar();
            showNoInternetSnackbar();
        }
    }

    private void openOrderScreen() {
        // Открыть экран заказа такси
    }

    private void openDriverScreen() {
        // Открыть экран для водителей
        if (NetworkUtils.isNetworkAvailable(this)) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxieasyua.job"));
            startActivity(browserIntent);
        }
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

    public void setCityAppbar() {
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        city = stringList.get(1);
        String cityMenu;
        switch (city) {
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
        newTitle = getString(R.string.menu_city) + " " + cityMenu;
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
    private void showFirstStartToasts() {
        // Проверяем, показывали ли уже
        boolean hasShown = (boolean) sharedPreferencesHelperMain.getValue("hasShownFirstStartToasts", false);
        if (hasShown) {
            return; // Уже показывали, выходим
        }

        // Массив сообщений
        String[] messages = {
                getString(R.string.first_start_reading),
                getString(R.string.first_start_checking),
                getString(R.string.first_start_setting)
        };

        for (int i = 0; i < messages.length; i++) {
            final int index = i;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Toast.makeText(MainActivity.this, messages[index], Toast.LENGTH_SHORT).show();
            }, i * 1500); // Каждое сообщение через 1.5 секунды
        }

        // Сохраняем флаг
        sharedPreferencesHelperMain.saveValue("hasShownFirstStartToasts", true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (NetworkUtils.isNetworkAvailable(this)) {
            hideNoInternetSnackbar();
        }
        if (!InclusiveTransportPreferenceWorker.hasBeenAsked() && !firstStart) {
            runOnUiThread(this::showInclusiveTransportDialog);
        }
        // ✅ ИСПРАВЛЕННЫЙ БЛОК - Toast показываются ТОЛЬКО ПРИ ПЕРВОМ ЗАПУСКЕ
        if (firstStart) {
            showFirstStartToasts();
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            sendCurrentFcmToken();
        }
        costMap = null;


        new Thread(() -> {
            appUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);

            appUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    // Возвращаемся на главный поток для завершения
                    runOnUiThread(() -> appUpdateManager.completeUpdate());
                }
            }).addOnFailureListener(e -> {
                Logger.e(MainActivity.this, TAG, "Ошибка проверки обновлений: " + e.getMessage());
            });
        }).start();

        sharedPreferencesHelperMain.saveValue("pay_error", "**");


        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        boolean gps_upd;
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
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
        if (HomeFragment.progressBar != null) {
            HomeFragment.progressBar.setVisibility(View.INVISIBLE);
        }
        if (VisicomFragment.progressBar != null) {
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
                String newTitle = getString(R.string.menu_city) + " " + cityMenu;
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

        String BASE_URL = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

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
            statement.bindString(10, "0");
            statement.bindString(11, "0");
            statement.bindString(12, "0");
            statement.bindString(13, "0");
            statement.bindString(14, "0");
            statement.bindString(15, "0");

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
                new String[]{"1"});
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
            statement.bindDouble(3, 0);
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
            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getContext());
            firebaseAnalytics.setAnalyticsCollectionEnabled(false);
            MyApplication.sharedPreferencesHelperMain.removeValue("userEmail");
            MyApplication.sharedPreferencesHelperMain.removeValue("last_fcm_token");
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
        if (item.getItemId() == R.id.inclusiveTransport) {
            showInclusiveTransportDialog();
        }
        if (item.getItemId() == R.id.settings) {

            navController.navigate(R.id.nav_settings, null, new NavOptions.Builder()
                    .build());

        }
        if (item.getItemId() == R.id.weather) {

            navController.navigate(R.id.nav_weather, null, new NavOptions.Builder()
                    .build());

        }
        if (item.getItemId() == R.id.send_email_admin) {
            bugReportHelper.showBugReportManager();
            return true;
        }
//        if (item.getItemId() == R.id.send_email_admin) {
//            // Инфлейтим кастомный layout
//            View dialogView = getLayoutInflater().inflate(R.layout.dialog_send_report, null);
//
//            // Находим все элементы
//            TextInputEditText etMessage = dialogView.findViewById(R.id.discinp);
////            TextView tvLogInfo = dialogView.findViewById(R.id.dialogMessage);
//            ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);
//            TextView tvCharCounter = dialogView.findViewById(R.id.charCounter);
//            AppCompatButton negativeButton = dialogView.findViewById(R.id.negativeButton);
//            AppCompatButton positiveButton = dialogView.findViewById(R.id.positiveButton);
//
//
//
//            // Устанавливаем начальное значение счетчика
//            tvCharCounter.setText(String.format(getString(R.string.char_counter), 0));
//
//            // Счетчик символов
//            etMessage.addTextChangedListener(new TextWatcher() {
//                @Override
//                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//                @Override
//                public void onTextChanged(CharSequence s, int start, int before, int count) {
//                    int length = s.length();
//                    tvCharCounter.setText(String.format(getString(R.string.char_counter), length));
//                    progressBar.setProgress(Math.min(length, 500));
//
//                    if (length > 500) {
//                        tvCharCounter.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.error_red));
//                        progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.error_red)));
//                    } else if (length > 450) {
//                        tvCharCounter.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.warning_orange));
//                        progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.warning_orange)));
//                    } else {
//                        tvCharCounter.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.gray_500));
//                        progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.colorPrimaryDark)));
//                    }
//                }
//
//                @Override
//                public void afterTextChanged(Editable s) {}
//            });
//
//            // Создаем диалог
//            AlertDialog.Builder builder =
//                    new AlertDialog.Builder(this, R.style.CustomAlertDialogTheme);
//            builder.setView(dialogView);
//
//            AlertDialog dialog = builder.create();
//            dialog.setCancelable(true);
//
//            // Обработка кнопок
//            positiveButton.setOnClickListener(v -> {
//                String message = etMessage.getText().toString().trim();
//
//                if (message.length() > 500) {
//                    Toast.makeText(this, getString(R.string.error_message_too_long), Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                if (message.isEmpty()) {
//                    Toast.makeText(this, getString(R.string.error_message_empty), Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                String logFilePath = getExternalFilesDir(null) + "/app_log.txt";
//                TelegramUtils.sendErrorToTelegram(generateEmailBody(message), logFilePath);
//
//                Toast.makeText(this, getString(R.string.report_sent), Toast.LENGTH_SHORT).show();
//                dialog.dismiss();
//            });
//
//            negativeButton.setOnClickListener(v -> dialog.dismiss());
//
//            dialog.show();
//        }
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

            // Показываем индикатор загрузки
            Toast.makeText(this, getString(R.string.checking_updates), Toast.LENGTH_SHORT).show();

            // Запускаем проверку в фоновом потоке
            new Thread(() -> {
                try {
                    // Проверяем и инициализируем appUpdateManager если нужно
                    if (appUpdateManager == null) {
                        appUpdateManager = AppUpdateManagerFactory.create(MainActivity.this);
                    }

                    // Получаем информацию об обновлениях синхронно с таймаутом
                    Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
                    AppUpdateInfo appUpdateInfo = Tasks.await(appUpdateInfoTask, 5, TimeUnit.SECONDS);

                    // Возвращаемся на главный поток для обновления UI
                    runOnUiThread(() -> {
                        if (appUpdateInfo.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) {
                            String message = getString(R.string.update_ok);
                            MyBottomSheetMessageFragment bottomSheetDialogFragment =
                                    new MyBottomSheetMessageFragment(message);
                            bottomSheetDialogFragment.show(getSupportFragmentManager(),
                                    bottomSheetDialogFragment.getTag());
                        } else {
                            if (NetworkUtils.isNetworkAvailable(MainActivity.this)) {
                                int status = appUpdateInfo.installStatus();
                                if (status != InstallStatus.DOWNLOADING &&
                                        status != InstallStatus.INSTALLING) {

                                    // AppUpdater создаем в том же фоновом потоке
                                    new Thread(() -> {
                                        AppUpdater updater = new AppUpdater(
                                                MainActivity.this,
                                                getExactAlarmLauncher(),
                                                getBatteryOptimizationLauncher()
                                        );
                                        runOnUiThread(() -> {
                                            appUpdater = updater;
                                            appUpdater.startUpdate();
                                        });
                                    }).start();

                                } else {
                                    Logger.d(getContext(), TAG,
                                            "Update already in progress. Skipping restart.");
                                    Toast.makeText(MainActivity.this,
                                            getString(R.string.update_in_progress), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(MainActivity.this,
                                        getString(R.string.no_internet), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } catch (Exception e) {
                    Logger.e(MainActivity.this, TAG, "Ошибка проверки обновлений: " + e.getMessage());
                    FirebaseCrashlytics.getInstance().recordException(e);
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.update_check_failed), Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();

            return true;
        }
        if (item.getItemId() == R.id.nav_driver) {
            if (NetworkUtils.isNetworkAvailable(this)) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxieasyua.job"));
                startActivity(browserIntent);
            }
        }


        if (item.getItemId() == R.id.send_like) {
            // Перевіряємо, чи вже оцінював
            if (appReviewManager.hasUserReviewed()) {
                // Вже оцінював - показуємо повідомлення або одразу відкриваємо сторінку
                Toast.makeText(this, R.string.thanks_for_review, Toast.LENGTH_SHORT).show();

                // Або одразу відкриваємо сторінку в Google Play
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                } catch (Exception e) {
                    // Ігноруємо помилку
                }
            } else {
                // Запитуємо оцінку
                appReviewManager.requestReview(this, new AppReviewManager.ReviewCallback() {
                    @Override
                    public void onReviewCompleted() {
                        Logger.d(getContext(), TAG, "Review dialog completed");
                        // Можна показати подяку
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,  R.string.thanks_for_review, Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onReviewFailed(Exception e) {
                        Logger.e(getContext(), TAG, "Review failed: " + e.getMessage());
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, R.string.no_open_review, Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onReviewNotAvailable(String reason) {
                        Logger.d(getContext(),TAG, "Review not available: " + reason);
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this, R.string.open_review_later, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            }
            return true;
        }
        if (item.getItemId() == R.id.uninstal_app) {
            AppDataUtils.delApp(this);

        }
        return false;
    }

    public String generateEmailBody(String errorMessage) {

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        List<String> userList = logCursor(MainActivity.TABLE_USER_INFO);


        // Определение города

        String city;
        String input = stringList.get(1);

        switch (input) {
            case "Dnipropetrovsk Oblast":
                city = getString(R.string.Dnipro_city);
                break;
            case "OdessaTest":
                city = getString(R.string.OdessaTest);
                break;
            case "Odessa":
                city = getString(R.string.city_odessa);
                break;
            case "Zaporizhzhia":
                city = getString(R.string.city_zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                city = getString(R.string.city_cherkassy);
                break;
            case "Lviv":
                city = getString(R.string.city_lviv);
                break;
            case "Ivano_frankivsk":
                city = getString(R.string.city_ivano_frankivsk);
                break;
            case "Vinnytsia":
                city = getString(R.string.city_vinnytsia);
                break;
            case "Poltava":
                city = getString(R.string.city_poltava);
                break;
            case "Sumy":
                city = getString(R.string.city_sumy);
                break;
            case "Kharkiv":
                city = getString(R.string.city_kharkiv);
                break;
            case "Chernihiv":
                city = getString(R.string.city_chernihiv);
                break;
            case "Rivne":
                city = getString(R.string.city_rivne);
                break;
            case "Ternopil":
                city = getString(R.string.city_ternopil);
                break;
            case "Khmelnytskyi":
                city = getString(R.string.city_khmelnytskyi);
                break;
            case "Zakarpattya":
                city = getString(R.string.city_zakarpattya);
                break;
            case "Zhytomyr":
                city = getString(R.string.city_zhytomyr);
                break;
            case "Kropyvnytskyi":
                city = getString(R.string.city_kropyvnytskyi);
                break;
            case "Mykolaiv":
                city = getString(R.string.city_mykolaiv);
                break;
            case "Chernivtsi":
                city = getString(R.string.city_chernivtsi);
                break;
            case "Lutsk":
                city = getString(R.string.city_lutsk);
                break;
            default:
                city = getString(R.string.Kyiv_city);
                break;
        }

        // Формирование тела сообщения

        return errorMessage + "\n"+
                getString(R.string.SA_info_pas) + "\n" +
                getString(R.string.SA_info_city) + " " + city + "\n" +
                getString(R.string.SA_pas_text) + " " + getString(R.string.version) + "\n" +
                getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
                getString(R.string.SA_email) + " " + userList.get(3) + "\n";
//                + getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n" + "\n";
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideNoInternetSnackbar();
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
        }
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

//    @SuppressLint("IntentReset")
//    private void sendEmailAdminFS() {
//        List<String> stringList = logCursor(MainActivity.CITY_INFO);
//        String city = stringList.get(1);
//
//        List<String> userList = logCursor(MainActivity.TABLE_USER_INFO);
//
//        String subject = getString(R.string.SA_subject) + generateRandomString(10);
//
//        String body = getString(R.string.SA_message_start) + "\n\n\n" +
//                getString(R.string.SA_info_pas) + "\n" +
//                getString(R.string.SA_info_city) + " " + city + "\n" +
//                getString(R.string.SA_pas_text) + " " + getString(R.string.version) + "\n" +
//                getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
//                getString(R.string.SA_email) + " " + userList.get(3) + "\n" +
//                getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n\n";
//
//        String[] CC = {"cartaxi4@gmail.com"};
//        String[] TO = {supportEmail};
//
//        File logFile = new File(getExternalFilesDir(null), "app_log.txt");
//
//        if (!logFile.exists()) {
//            Logger.e(this, "MainActivity", "Log file does not exist");
//            return;
//        }
//
//        // Firebase Storage reference
//        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
//        StorageReference logRef = storageRef.child("logs/" + logFile.getName());
//
//        Uri fileUri = Uri.fromFile(logFile);
//        logRef.putFile(fileUri)
//                .addOnSuccessListener(taskSnapshot -> logRef.getDownloadUrl()
//                        .addOnSuccessListener(uri -> {
//                            // Добавляем ссылку на лог в тело письма
//                            String bodyWithLink = body + "\nDownload log: " + uri.toString();
//
//                            Intent emailIntent = new Intent(Intent.ACTION_SEND);
//                            emailIntent.setType("message/rfc822");
//                            emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
//                            emailIntent.putExtra(Intent.EXTRA_CC, CC);
//                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
//                            emailIntent.putExtra(Intent.EXTRA_TEXT, bodyWithLink);
//
//                            try {
//                                startActivity(Intent.createChooser(emailIntent, subject));
//                            } catch (android.content.ActivityNotFoundException e) {
//                                FirebaseCrashlytics.getInstance().recordException(e);
//                            }
//                        }))
//                .addOnFailureListener(e -> {
//                    Logger.e(this, "MainActivity", "Failed to upload log" + e);
//                    FirebaseCrashlytics.getInstance().recordException(e);
//                });
//    }

//    @SuppressLint("IntentReset")
//    private void sendEmailAdmin() {
//        List<String> stringList = logCursor(MainActivity.CITY_INFO);
//        String city = stringList.get(1);
//
//        List<String> userList = logCursor(MainActivity.TABLE_USER_INFO);
//
//        String subject = getString(R.string.SA_subject) + generateRandomString(10);
//
//        String body = getString(R.string.SA_message_start) + "\n\n" +
//                getString(R.string.SA_info_pas) + "\n" +
//                getString(R.string.SA_info_city) + " " + city + "\n" +
//                getString(R.string.SA_pas_text) + " " + getString(R.string.version) + "\n" +
//                getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
//                getString(R.string.SA_email) + " " + userList.get(3) + "\n" +
//                getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n\n";
//
//        File logFile = new File(getExternalFilesDir(null), "app_log.txt");
//
//        if (!logFile.exists()) {
//            Logger.e(this, "MainActivity", "Log file does not exist");
//            return;
//        }
//
//        // Создаем Retrofit
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("https://t.easy-order-taxi.site/") // ваш сервер
//                .addConverterFactory(ScalarsConverterFactory.create()) // для получения plain text
//                .build();
//
//        UploadService service = retrofit.create(UploadService.class);
//
//        RequestBody requestFile = RequestBody.create(logFile, MediaType.parse("text/plain"));
//        MultipartBody.Part bodyPart = MultipartBody.Part.createFormData("file", logFile.getName(), requestFile);
//
//        Call<String> call = service.uploadLog(bodyPart);
//
//        // Асинхронный вызов
//        call.enqueue(new retrofit2.Callback<>() {
//            @Override
//            public void onResponse(@NonNull Call<String> call, @NonNull retrofit2.Response<String> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    String fileUrl = response.body().trim();
//                    String bodyWithLink = body + "\nDownload log: " + fileUrl;
//
//                    String[] CC = {"cartaxi4@gmail.com"};
//                    String[] TO = {supportEmail};
//
//                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
//                    emailIntent.setType("message/rfc822");
//                    emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
//                    emailIntent.putExtra(Intent.EXTRA_CC, CC);
//                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
//                    emailIntent.putExtra(Intent.EXTRA_TEXT, bodyWithLink);
//
//                    try {
//                        startActivity(Intent.createChooser(emailIntent, subject));
//                    } catch (android.content.ActivityNotFoundException e) {
//                        FirebaseCrashlytics.getInstance().recordException(e);
//                    }
//                } else {
//                    Logger.e(getApplicationContext(), TAG, "Upload failed: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
//                Logger.e(getApplicationContext(), "MainActivity", "Upload exception" + t);
//            }
//        });
//    }

//    @SuppressLint("IntentReset")
//    private void sendEmailAdmin() {
//        // Получаем город напрямую из базы
//        List<String> stringList = logCursor(MainActivity.CITY_INFO);
//        String city = stringList.get(1);
//
//        // Получаем данные пользователя
//        List<String> userList = logCursor(MainActivity.TABLE_USER_INFO);
//
//        // Тема письма с случайной частью
//        String subject = getString(R.string.SA_subject) + generateRandomString(10);
//
//        // Тело письма
//        String body = getString(R.string.SA_message_start) + "\n\n" +
//                getString(R.string.SA_info_pas) + "\n" +
//                getString(R.string.SA_info_city) + " " + city + "\n" +
//                getString(R.string.SA_pas_text) + " " + getString(R.string.version) + "\n" +
//                getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
//                getString(R.string.SA_email) + " " + userList.get(3) + "\n" +
//                getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n\n";
//
//        // Файл логов
//        File logFile = new File(getExternalFilesDir(null), "app_log.txt");
//
//        if (!logFile.exists()) {
//            Logger.e(this, "MainActivity", "Log file does not exist");
//            return;
//        }
//
//        // Настройка Retrofit
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl("https://t.easy-order-taxi.site/") // твой сервер
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        UploadService service = retrofit.create(UploadService.class);
//
//        // Подготовка файла для отправки
//        RequestBody requestFile = RequestBody.create(logFile, MediaType.parse("text/plain"));
//        MultipartBody.Part bodyPart = MultipartBody.Part.createFormData("file", logFile.getName(), requestFile);
//
//        Call<UploadResponse> call = service.uploadLog(bodyPart);
//
//        // Асинхронный вызов
//        call.enqueue(new retrofit2.Callback<>() {
//            @Override
//            public void onResponse(@NonNull Call<UploadResponse> call, @NonNull retrofit2.Response<UploadResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    String fileUrl = response.body().getUrl();
//
//                    // Добавляем ссылку на лог в тело письма
//                    String bodyWithLink = body + "\nDownload log: " + fileUrl;
//
//                    String[] CC = {"cartaxi4@gmail.com"};
//                    String[] TO = {supportEmail};
//
//                    // Создаём Intent для отправки Email
//                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
//                    emailIntent.setType("message/rfc822");
//                    emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
//                    emailIntent.putExtra(Intent.EXTRA_CC, CC);
//                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
//                    emailIntent.putExtra(Intent.EXTRA_TEXT, bodyWithLink);
//
//                    try {
//                        startActivity(Intent.createChooser(emailIntent, subject));
//                    } catch (android.content.ActivityNotFoundException e) {
//                        FirebaseCrashlytics.getInstance().recordException(e);
//                    }
//
//                } else {
//                    Logger.e(getApplicationContext(), "MainActivity", "Upload failed: " + response.code());
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<UploadResponse> call, @NonNull Throwable t) {
//                Logger.e(getApplicationContext(), "MainActivity", "Upload exception: " + t);
//            }
//        });
//    }


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
        } catch (Exception ignored) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        Logger.d(this, TAG, "onOptionsItemSelected gps_enabled: " + gps_enabled);
        Logger.d(this, TAG, "onOptionsItemSelected network_enabled: " + network_enabled);
        if (!gps_enabled) {
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
                new String[]{"1"});
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

        if (userEmail.equals("email") || userEmail.equals("no_email") ||userEmail.isEmpty()) {
            firstStart = true;
            sharedPreferencesHelperMain.saveValue("CityCheckActivity", "**");
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            Toast.makeText(MainActivity.this, R.string.checking, Toast.LENGTH_SHORT).show();
            startFireBase();
        } else {

            findUserFromServer(userEmail, findUser -> {
                // Use the boolean result here
                Log.d(TAG, "User exists: " + findUser);
                Logger.d(MainActivity.this, TAG, "CityCheckActivity: " + sharedPreferencesHelperMain.getValue("CityCheckActivity", "**"));

                if (!findUser) {
                    firstStart = true;

                    VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(MainActivity.this, R.string.checking, Toast.LENGTH_SHORT).show();
                    startFireBase();
                } else {
                    // Инициализация и подключение к Pusher
//                    pusherManager = new PusherManager(
//                            getString(R.string.application),
//                            userEmail,
//                            MainActivity.this,
//                            viewModel
//                    );
//                    pusherManager.connect();
//                    pusherManager.subscribeToChannel();

                    centrifugoManager = new CentrifugoManager(
                            getString(R.string.application),
                            userEmail,
                            MainActivity.this,
                            viewModel
                    );

                    centrifugoManager.connect();
// subscribeToChannel() не обязателен, но можно вызвать для надежности
                    centrifugoManager.subscribeToChannel();

                    new VerifyUserTask(this).execute();
                    String sityCheckActivity = (String) sharedPreferencesHelperMain.getValue("CityCheckActivity", "**");
                    Logger.d(this, TAG, "CityCheckActivity: " + sityCheckActivity);

                    if (sityCheckActivity.equals("**")) {
                        // Запускаем CityCheckActivity, если состояние страны не задано
                        Intent intent = new Intent(this, CityCheckActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    firstStart = false;

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


                    OneTimeWorkRequest immediatePushCheck =
                            new OneTimeWorkRequest.Builder(CheckPushPermissionWorker.class)
                                    .build();

                    // Запуск задач через WorkManager
                    WorkManager.getInstance(this)
                            .beginWith(versionFromMarketRequest)
                            .then(userPhoneFromFbRequest)
                            .then(updatePushDateRequest)
                            .then(getCardTokenWfpRequest)
                            .then(immediatePushCheck)
                            .enqueue();


                    UserPermissions.getPermissions(userEmail, getApplicationContext());

                }
            });
        }



    }

    public void findUserFromServer(String userEmail, CallbackUser<Boolean> resultCallback) {
        new VerifyUserTask(this).checkUserBlacklist();
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

                    userEmailForTest = user.getEmail();
                    usernameForTest = "username";

                    settingsNewUser(user.getEmail());

                    String sityCheckActivity = (String) sharedPreferencesHelperMain.getValue("CityCheckActivity", "**");
                    Logger.d(this, TAG, "CityCheckActivity: " + sityCheckActivity);

                    if (sityCheckActivity.equals("**")) {
                        // Запускаем CityCheckActivity, если состояние страны не задано
                        Intent intent = new Intent(this, CityCheckActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
//                    pusherManager = new PusherManager(
//                            getString(R.string.application),
//                            user.getEmail(),
//                            this,
//                            viewModel
//                    );
//                    pusherManager.connect();
//                    pusherManager.subscribeToChannel();
//                    Log.d("DEBUG", "Creating PusherManager instance. Hash: " + pusherManager.hashCode());
//                    Log.d("DEBUG", "ViewModel passed to PusherManager hash: " + viewModel.hashCode());
                    centrifugoManager = new CentrifugoManager(
                            getString(R.string.application),
                            user.getEmail(),
                            MainActivity.this,
                            viewModel
                    );

                    centrifugoManager.connect();
// subscribeToChannel() не обязателен, но можно вызвать для надежности
                    centrifugoManager.subscribeToChannel();
                }
            } else {
                handleSignInFailure(result);
            }
        } catch (Exception e) {
            handleException(e, cv);
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


    }


    private void settingsNewUser(String emailUser) {
        // Пример 2: Запуск с указанием страницы информации об IP клиента
        String app = getString(R.string.application) + getString(R.string.version);
        SaveIPWithEmailUtils.startWorker(emailUser, app, getApplicationContext());
        // Сохраняем email (если ещё не сохранён)
        MyApplication.sharedPreferencesHelperMain.saveValue("userEmail", emailUser);

// Отправляем актуальный токен
        sendCurrentFcmToken();

        new VerifyUserTask(this).execute();
        // Создание запросов для задач settingsNewUser
        OneTimeWorkRequest updateUserInfoRequest = new OneTimeWorkRequest.Builder(UpdateUserInfoWorker.class)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putString("emailUser", emailUser)
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
//                .then(sendTokenRequest)
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

    @SuppressLint("StaticFieldLeak")
    public static void checkForUpdateForPush(
            SharedPreferences sharedPreferences,
            long currentTime,
            String LAST_NOTIFICATION_TIME_KEY
    ) {
        // Обновляем время последней отправки уведомления
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(LAST_NOTIFICATION_TIME_KEY, currentTime);
        editor.apply();

        // Используем AsyncTask для фоновой работы
        new AsyncTask<Void, Void, AppUpdateInfo>() {
            private Exception error = null;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Logger.d(MyApplication.getContext(), TAG,
                        "Начинаем проверку обновлений для push-уведомления");
            }

            @Override
            protected AppUpdateInfo doInBackground(Void... params) {
                try {
                    // Проверяем, инициализирован ли appUpdateManager
                    if (appUpdateManager == null) {
                        appUpdateManager = AppUpdateManagerFactory.create(MyApplication.getContext());
                    }

                    // Получаем информацию об обновлениях
                    Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

                    // Ждем результат не более 5 секунд
                    return Tasks.await(appUpdateInfoTask, 5, TimeUnit.SECONDS);

                } catch (TimeoutException e) {
                    error = e;
                    Logger.e(MyApplication.getContext(), TAG,
                            "Таймаут при проверке обновлений: " + e.getMessage());
                } catch (ExecutionException e) {
                    // Проверяем, является ли корневая причина InstallException
                    Throwable cause = e.getCause();
                    if (cause instanceof com.google.android.play.core.install.InstallException) {
                        com.google.android.play.core.install.InstallException installEx =
                                (com.google.android.play.core.install.InstallException) cause;
                        int errorCode = installEx.getErrorCode();

                        // Игнорируем ошибку -10 (APP_NOT_OWNED)
                        if (errorCode == -10) {
                            Logger.d(MyApplication.getContext(), TAG,
                                    "Приложение установлено не из Google Play Store, пропускаем проверку обновлений");
                            return null;
                        }

                        // Игнорируем другие ошибки, используя константы InstallErrorCode
                        if (errorCode == InstallErrorCode.ERROR_INSTALL_UNAVAILABLE) { // -2
                            Logger.d(MyApplication.getContext(), TAG,
                                    "Обновления временно недоступны (ERROR_INSTALL_UNAVAILABLE)");
                            return null;
                        }

                        if (errorCode == InstallErrorCode.ERROR_API_NOT_AVAILABLE) { // -3
                            Logger.d(MyApplication.getContext(), TAG,
                                    "API обновлений недоступен (ERROR_API_NOT_AVAILABLE)");
                            return null;
                        }

                        if (errorCode == InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED) { // -7
                            Logger.d(MyApplication.getContext(), TAG,
                                    "Установка обновлений не разрешена (ERROR_INSTALL_NOT_ALLOWED)");
                            return null;
                        }
                    }
                    error = e;
                    Logger.e(MyApplication.getContext(), TAG,
                            "Ошибка выполнения при проверке обновлений: " + e.getMessage());
                    FirebaseCrashlytics.getInstance().recordException(e);
                } catch (InterruptedException e) {
                    error = e;
                    Logger.e(MyApplication.getContext(), TAG,
                            "Проверка обновлений прервана: " + e.getMessage());
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    error = e;
                    Logger.e(MyApplication.getContext(), TAG,
                            "Неожиданная ошибка при проверке обновлений: " + e.getMessage());
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(AppUpdateInfo appUpdateInfo) {
                super.onPostExecute(appUpdateInfo);

                if (error != null) {
                    // Не показываем ошибку пользователю, просто логируем
                    Logger.d(MyApplication.getContext(), TAG,
                            "Проверка обновлений пропущена");
                    return;
                }

                if (appUpdateInfo == null) {
                    Logger.d(MyApplication.getContext(), TAG,
                            "Не удалось получить информацию об обновлениях");
                    return;
                }

                int updateAvailability = appUpdateInfo.updateAvailability();
                int installStatus = appUpdateInfo.installStatus();

                Logger.d(MyApplication.getContext(), TAG,
                        "checkForUpdateForPush - updateAvailability: " + updateAvailability +
                                ", installStatus: " + installStatus);

                // Проверяем, доступно ли обновление и не идет ли уже установка
                if (updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
                    if (installStatus != InstallStatus.DOWNLOADING &&
                            installStatus != InstallStatus.INSTALLING) {

                        Logger.d(MyApplication.getContext(), TAG,
                                "Доступно обновление, показываем уведомление");

                        try {
                            String title = MyApplication.getContext()
                                    .getString(R.string.new_version);
                            String messageNotif = MyApplication.getContext()
                                    .getString(R.string.news_of_version);
                            String urlStr = "https://play.google.com/store/apps/details?id=" +
                                    MyApplication.getContext().getPackageName();

                            NotificationHelper.showNotification(
                                    MyApplication.getContext(),
                                    title,
                                    messageNotif,
                                    urlStr
                            );
                        } catch (Exception e) {
                            Logger.e(MyApplication.getContext(), TAG,
                                    "Ошибка показа уведомления: " + e.getMessage());
                            FirebaseCrashlytics.getInstance().recordException(e);
                        }
                    } else {
                        Logger.d(MyApplication.getContext(), TAG,
                                "Обновление уже загружается или устанавливается");
                    }
                } else {
                    Logger.d(MyApplication.getContext(), TAG,
                            "Обновлений не найдено или недоступны");
                }
            }
        }.execute();
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
        // Скидаємо дані про оцінки
        if (appReviewManager != null) {
            appReviewManager.resetReviewData();
        }

        // 1. Разлогиниваем пользователя из Firebase Auth
        try {
            AuthUI.getInstance().signOut(context)
                    .addOnCompleteListener(task -> {
                        Logger.d(context, TAG, "Firebase Auth sign out completed: " + task.isSuccessful());
                    });
            // Также можно FirebaseAuth.getInstance().signOut(); если не используешь FirebaseUI
            FirebaseAuth.getInstance().signOut();
            Logger.d(context, TAG, "Пользователь разлогинен из Firebase Auth");
        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка при выходе из Firebase Auth: " + e.toString());
        }

        // 2. Очищаем все данные
        clearAllSharedPreferences(context);
        clearAllDatabases(context);
        clearAllCache(context);
        clearAllExternalCache(context);

        // 3. Рестарт приложения
        try {
            Logger.d(context, TAG, "Перезапускаем приложение как при первой установке");
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent != null) {
                intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK |  // Важно! Очищает весь back stack
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                );
                context.startActivity(intent);

                // Убиваем текущий процесс
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            } else {
                Logger.e(context, TAG, "Не удалось найти launch intent");
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка при рестарте приложения: " + e.toString());
        }

        Logger.d(context, TAG, "Completed clearApplication");
    }

    // Clears all SharedPreferences files for the app
    // Clears all SharedPreferences files for the app
    void clearAllSharedPreferences(Context context) {
        Logger.d(context, TAG, "Starting clearAllSharedPreferences");
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }

        // 1. Очищаем через существующий метод clear()
        if (sharedPreferencesHelperMain != null) {
            sharedPreferencesHelperMain.clear();
            Logger.d(context, TAG, "Cleared SharedPreferences via helper.clear()");
        }

        // 2. Физически удаляем все XML файлы (для надежности)
        String prefsDir = context.getApplicationInfo().dataDir + "/shared_prefs";
        File dir = new File(prefsDir);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".xml")) {
                        boolean deleted = file.delete();
                        Logger.d(context, TAG, "Deleted " + file.getName() + ": " + deleted);
                    }
                }
            }
        }

        // 3. Пересоздаем экземпляр helper с чистыми настройками
        sharedPreferencesHelperMain = new SharedPreferencesHelper(context);

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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        NotificationHelper.cancelNotificationFromIntent(this, intent);

    }

    //ТЕСТЫ
// Поля для теста
    private String userEmailForTest;
    private String usernameForTest;

    // Геттеры для теста
    public String getUserEmailForTest() {
        return userEmailForTest;
    }

    public String getUsernameForTest() {
        return usernameForTest;
    }

    // Метод для теста, чтобы подставить мок-пользователя
    public void setCurrentUserForTest(FirebaseUser user) {
        if (user != null) {
            this.userEmailForTest = user.getEmail();
            this.usernameForTest = "username"; // можно любое тестовое имя
        }
    }

    private void sendCurrentFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "Текущий FCM токен: " + token);
                   String userEmail = getCurrentUserEmail();

                    // Отправляем только если токен изменился ИЛИ если email есть
                    if (!userEmail.isEmpty() && !userEmail.equals("no_email")) {
                        Log.d(TAG, "Отправляем обновлённый токен на сервер");
                        TokenUtils.sendToken(this, userEmail, token);
                        MyApplication.sharedPreferencesHelperMain.saveValue("last_fcm_token", token);
                    } else {
                        Log.w(TAG, "Пользователь не залогинен — токен не отправляем");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Не удалось получить FCM токен", e);
                });
    }

    private String getCurrentUserEmail() {
        // 1. Основной источник — SharedPreferences (как у тебя везде используется)
        String emailFromPrefs = (String) sharedPreferencesHelperMain.getValue("userEmail", "no_email");
        if (!Objects.equals(emailFromPrefs, "no_email") && !emailFromPrefs.isEmpty()) {
            return emailFromPrefs.trim(); // на всякий случай убираем пробелы
        }

        // 2. Резервный источник — FirebaseAuth (если пользователь аутентифицирован через Firebase)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            String emailFromFirebase = currentUser.getEmail().trim();
            Log.d(TAG, "Email взят из FirebaseAuth: " + emailFromFirebase);

            // Опционально: синхронизируем с SharedPreferences, чтобы в дальнейшем брать оттуда
            sharedPreferencesHelperMain.saveValue("userEmail", emailFromFirebase);

            return emailFromFirebase;
        }

        // 3. Если ничего не нашли — возвращаем пустую строку
        Log.w(TAG, "Email пользователя не найден ни в SharedPreferences, ни в FirebaseAuth");
        return "";
    }



    private void showInclusiveTransportDialog() {
        Logger.d(this, TAG, "showInclusiveTransportDialog вызван");

        if (isFinishing() || isDestroyed()) {
            return;
        }

        runOnUiThread(() -> {
            try {
                // Создаем кастомный layout программно
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(16));
                layout.setBackgroundColor(getColor(android.R.color.white));

                // Заголовок
                TextView title = new TextView(this);
                title.setText("Інклюзивний транспорт");
                title.setTextSize(18);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setTextColor(getColor(R.color.colorPrimary));
                title.setGravity(Gravity.CENTER);
                title.setPadding(0, 0, 0, dpToPx(16));
                layout.addView(title);

                // Разделитель
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
                divider.setBackgroundColor(getColor(android.R.color.darker_gray));
                divider.setPadding(0, 0, 0, dpToPx(16));
                layout.addView(divider);

                // Текст вопроса
                TextView questionText = new TextView(this);
                questionText.setText("Чи потрібні вам автомобілі, адаптовані для людей з обмеженими можливостями?");
                questionText.setTextSize(16);
                questionText.setTextColor(getColor(android.R.color.black));
                questionText.setPadding(0, 0, 0, dpToPx(20));
                questionText.setLineSpacing(0, 1.3f);
                layout.addView(questionText);

                // Switch контейнер
                LinearLayout switchContainer = new LinearLayout(this);
                switchContainer.setOrientation(LinearLayout.HORIZONTAL);
                switchContainer.setGravity(Gravity.CENTER_VERTICAL);
                switchContainer.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
                switchContainer.setBackgroundColor(getColor(android.R.color.white));

                // Фон для контейнера
                GradientDrawable containerBg = new GradientDrawable();
                containerBg.setColor(getColor(android.R.color.white));
                containerBg.setCornerRadius(dpToPx(12));
                containerBg.setStroke(dpToPx(1), getColor(android.R.color.darker_gray));
                switchContainer.setBackground(containerBg);

                // Текст "Потрібен інклюзивний транспорт"
                TextView switchLabel = new TextView(this);
                switchLabel.setText("Потрібен інклюзивний транспорт");
                switchLabel.setTextSize(15);
                switchLabel.setTextColor(getColor(android.R.color.black));
                switchLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                switchContainer.addView(switchLabel);

                // Switch
                androidx.appcompat.widget.SwitchCompat switchBtn = new androidx.appcompat.widget.SwitchCompat(this);
                boolean needsInclusive = InclusiveTransportPreferenceWorker.needsInclusiveTransport();
                switchBtn.setChecked(needsInclusive);
                switchBtn.setTextOff(getString(R.string.inclusive_transport_no));
                switchBtn.setTextOn(getString(R.string.inclusive_transport_yes));
                switchBtn.setPadding(dpToPx(8), 0, 0, 0);

                switchContainer.addView(switchBtn);
                layout.addView(switchContainer);

                // Кнопка "Зберегти"
                Button saveButton = new Button(this);
                saveButton.setText("Зберегти");
                saveButton.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                saveButton.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));
                ((LinearLayout.LayoutParams) saveButton.getLayoutParams()).topMargin = dpToPx(24);

                GradientDrawable buttonBg = new GradientDrawable();
                buttonBg.setColor(getColor(R.color.colorPrimary));
                buttonBg.setCornerRadius(dpToPx(8));
                saveButton.setBackground(buttonBg);
                saveButton.setTextColor(getColor(android.R.color.white));
                saveButton.setTextSize(14);
                saveButton.setAllCaps(false);

                layout.addView(saveButton);

                // Создаем диалог
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(layout)
                        .setCancelable(true);

                AlertDialog dialog = builder.create();
                dialog.show();

                // Обработчик сохранения
                saveButton.setOnClickListener(v -> {
                    boolean newValue = switchBtn.isChecked();
                    InclusiveTransportPreferenceWorker.saveUserPreference(newValue);
                    dialog.dismiss();

                    // Показываем Toast с подтверждением
                    String message = newValue ?
                            getString(R.string.inclusiv_on) :
                            getString(R.string.inclusiv_off);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Logger.e(this, TAG, "Ошибка: " + e.getMessage());
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        });
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}