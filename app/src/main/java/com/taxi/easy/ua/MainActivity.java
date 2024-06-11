package com.taxi.easy.ua;

import static com.taxi.easy.ua.R.string.cancel_button;
import static com.taxi.easy.ua.R.string.format_phone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.WorkManager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taxi.easy.ua.cities.api.CityApiClient;
import com.taxi.easy.ua.cities.api.CityResponse;
import com.taxi.easy.ua.cities.api.CityResponseMerchantFondy;
import com.taxi.easy.ua.cities.api.CityService;
import com.taxi.easy.ua.databinding.ActivityMainBinding;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.ApiService;
import com.taxi.easy.ua.ui.finish.City;
import com.taxi.easy.ua.ui.finish.RouteResponse;
import com.taxi.easy.ua.ui.fondy.callback.CallbackResponse;
import com.taxi.easy.ua.ui.fondy.callback.CallbackService;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetCityFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.VerifyUserTask;
import com.taxi.easy.ua.utils.activ_push.MyService;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.download.AppUpdater;
import com.taxi.easy.ua.utils.ip.IPUtil;
import com.taxi.easy.ua.utils.messages.UsersMessages;
import com.taxi.easy.ua.utils.notify.NotificationHelper;
import com.taxi.easy.ua.utils.permissions.UserPermissions;
import com.taxi.easy.ua.utils.phone.ApiClientPhone;
import com.taxi.easy.ua.utils.phone_state.DeviceUtils;
import com.taxi.easy.ua.utils.phone_state.MyBottomSheetPhoneStateFragment;
import com.taxi.easy.ua.utils.user.ApiServiceUser;
import com.taxi.easy.ua.utils.user.UserResponse;

import org.json.JSONException;

import java.io.File;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements VisicomFragment.AutoClickListener{
    private static final String TAG = "TAG_MAIN";
    public static String order_id;
    public static String invoiceId;

    public static final String DB_NAME = "data_11032024_0";

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
    public static String countryState;
    public static String apiKeyMapBox;
    public static String apiKey;

    private static String verifyInternet;
    public static final long MAX_TASK_EXECUTION_TIME_SECONDS = 3;
    public static String versionServer;

    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;
    String baseUrl = "https://m.easy-order-taxi.site";
    private List<RouteResponse> routeList;
    String[] array;
    public static boolean gps_upd;
    VisicomFragment visicomFragment;
    public static SharedPreferences sharedPreferences;
    public static SharedPreferences sharedPreferencesCount;
    public static final String PERMISSIONS_PREF_NAME = "Permissions";
    public static final String PERMISSION_REQUEST_COUNT_KEY = "PermissionRequestCount";
    public static boolean location_update;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
              R.id.nav_visicom, R.id.nav_home, R.id.nav_gallery, R.id.nav_about, R.id.nav_uid, R.id.nav_bonus, R.id.nav_card, R.id.nav_author)
             .setOpenableLayout(drawer)
             .build();
        navMenu = navigationView.getMenu();
        navVisicomMenuItem = navMenu.findItem(R.id.nav_visicom);


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        networkChangeReceiver = new NetworkChangeReceiver();
        verifyInternet = getString(R.string.verify_internet);

// Initialize VisicomFragment and set AutoClickListener
        visicomFragment = new VisicomFragment();
        visicomFragment.setAutoClickListener(this); // "this" refers to the MainActivity

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

        try {
            initDB();
        } catch (MalformedURLException | JSONException | InterruptedException ignored) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Передаем результаты обратно вашему фрагменту для обработки
        if (visicomFragment != null) {
            visicomFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @SuppressLint("NewApi")
    private void isServiceRunning() {

        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        int serviceCount = 0;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.d(TAG, "isServiceRunning: " + service.service.getClassName());
            serviceCount++;
            if (MyService.class.getName().equals(service.service.getClassName())) {
                Intent intent = new Intent(this, MyService.class);
                stopService(intent);
                Log.d(TAG, "isServiceRunning: " + "stopService");
            }
        }
        Log.d(TAG, "Total running services: " + serviceCount);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, MyService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_MUTABLE);
        alarmManager.cancel(pendingIntent);

        WorkManager.getInstance(this).cancelAllWork();
    }


    @Override
    protected void onResume() {
        super.onResume();
        databaseHelper = new DatabaseHelper(getApplicationContext());
        databaseHelper.clearTable();

        databaseHelperUid = new DatabaseHelperUid(getApplicationContext());
        databaseHelperUid.clearTableUid();

        insertOrUpdatePushDate();
        Log.d(TAG, "onResume: isServiceRunning())  " );
        isServiceRunning();
        startService(new Intent(this, MyService.class));
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            gps_upd = getIntent().getBooleanExtra("gps_upd", true);
        } else {
            gps_upd = false;
        };
    }

    void checkNotificationPermissionAndRequestIfNeeded() {
        // Проверяем разрешение на отправку уведомлений
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.areNotificationsEnabled()) {
            // Разрешение на отправку уведомлений отключено, показываем диалоговое окно или системный экран для запроса разрешения
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
            // После отображения системного экрана для настроек уведомлений, можно предположить, что пользователь примет необходимые действия и вернется в приложение.
            // Здесь вы можете использовать метод onActivityResult() для обработки результата запроса разрешения.
        }

    }


    public void insertOrUpdatePushDate() {

        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        if (database != null) {
            try {
                // Получаем текущее время и дату
                @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentDateandTime = sdf.format(new Date());
                Log.d(TAG, "Current date and time: " + currentDateandTime);

                // Создаем объект ContentValues для передачи данных в базу данных
                ContentValues values = new ContentValues();
                values.put("push_date", currentDateandTime);

                // Пытаемся вставить новую запись. Если запись уже существует, выполняется обновление.
                int rowsAffected = database.update(MainActivity.TABLE_LAST_PUSH, values, "ROWID=1", null);
                if (rowsAffected > 0) {
                    Log.d(TAG, "Update successful");
                } else {
                    Log.d(TAG, "Error updating");
                }


            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                database.close();
            }
        }
        assert database != null;
        database.close();
    }
    private static final String PREFS_NAME = "UserActivityPrefs";
    private static final String LAST_ACTIVITY_KEY = "lastActivityTimestamp";
    private void updateLastActivityTimestamp() {

        // Обновление времени последней активности в SharedPreferences
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        long lastActivityTimestamp = prefs.getLong(LAST_ACTIVITY_KEY, 0);
        long currentTime = System.currentTimeMillis();
        Log.d(TAG, "lastActivity: Main " + timeFormatter(lastActivityTimestamp));
        Log.d(TAG, "currentTime:  Main " + timeFormatter(currentTime));
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_ACTIVITY_KEY, currentTime);
        editor.apply();
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
    private String timeFormatter(long timeMsec) {
        Date formattedTime = new Date(timeMsec);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(formattedTime);
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

    public void insertPushDate(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        if (database != null) {
            try {
                // Получаем текущее время и дату
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String currentDateandTime = sdf.format(new Date());
                Log.d("InsertOrUpdate", "Current date and time: " + currentDateandTime);

                // Создаем объект ContentValues для передачи данных в базу данных
                ContentValues values = new ContentValues();
                values.put("push_date", currentDateandTime);

                // Пытаемся вставить новую запись. Если запись уже существует, выполняется обновление.
                long rowId = database.insertWithOnConflict(MainActivity.TABLE_LAST_PUSH, null, values, SQLiteDatabase.CONFLICT_REPLACE);

                if (rowId != -1) {
                    Log.d("InsertOrUpdate", "Insert or update successful");
                } else {
                    Log.d("InsertOrUpdate", "Error inserting or updating");
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                Log.d(TAG, "Current date and time: " + currentDateandTime);

                // Создаем объект ContentValues для передачи данных в базу данных
                ContentValues values = new ContentValues();
                values.put("push_date", currentDateandTime);

                // Пытаемся вставить новую запись. Если запись уже существует, выполняется обновление.
                int rowsAffected = database.update(MainActivity.TABLE_LAST_PUSH, values, "ROWID=1", null);
                if (rowsAffected > 0) {
                    Log.d(TAG, "Update successful");
                } else {
                    Log.d(TAG, "Error updating");
                }


            } catch (Exception e) {
                e.printStackTrace();
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
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @SuppressLint("IntentReset")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_exit) {
            System.gc();
            finishAffinity();
        }

//        if (item.getItemId() == R.id.action_state_phone) {
//            checkPermission();
//        }

        if (item.getItemId() == R.id.gps) {
            eventGps();
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
            } catch (android.content.ActivityNotFoundException ignored) {

        }

            if (item.getItemId() == R.id.phone_settings) {
                if (NetworkUtils.isNetworkAvailable(this)) {
                    phoneNumberChange();
                } else {
                    Toast.makeText(this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
                }

            }
        }
        if (item.getItemId() == R.id.update) {
            Log.d(TAG, "onOptionsItemSelected: " +versionServer);
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
        if (item.getItemId() == R.id.phone_settings) {
            if (NetworkUtils.isNetworkAvailable(this)) {
                phoneNumberChange();
            } else {
                Toast.makeText(this, R.string.verify_internet, Toast.LENGTH_SHORT).show();
            }
        }
        if (item.getItemId() == R.id.nav_city) {
            if (NetworkUtils.isNetworkAvailable(this)) {
                List<String> listCity = logCursor(MainActivity.CITY_INFO);
                String city = listCity.get(1);
                MyBottomSheetCityFragment bottomSheetDialogFragment = new MyBottomSheetCityFragment(city, getApplicationContext());
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
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
        return false;
    }


    private AppUpdater appUpdater;
    private static final int REQUEST_INSTALL_PACKAGES = 123;
    private void updateApp() {

        // Создание экземпляра AppUpdater
        appUpdater = new AppUpdater(this);
        Log.d("UpdateApp", "Starting app update process");

        // Установка слушателя для обновления состояния установки
        appUpdater.setOnUpdateListener(new AppUpdater.OnUpdateListener() {
            @Override
            public void onUpdateCompleted() {
                // Показать пользователю сообщение о завершении обновления
                Toast.makeText(getApplicationContext(), "Обновление завершено. Приложение будет перезапущено.", Toast.LENGTH_SHORT).show();

                // Перезапуск приложения для применения обновлений
                restartApplication();
            }
        });

        // Регистрация слушателя
        appUpdater.registerListener();

        // Проверка наличия обновлений
        checkForUpdate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_INSTALL_PACKAGES) {
            if (resultCode == RESULT_OK) {
                // Пользователь разрешил установку пакетов, продолжаем установку
                // Ваш код для установки пакета

            } else {
                // Пользователь отказал в установке пакетов или отменил действие
                // Обработайте это событие соответствующим образом
            }
            Log.d(TAG, "onActivityResult:resultCode " + resultCode);
        }
    }

    // Добавляем проверку наличия обновлений
    private static final int MY_REQUEST_CODE = 1001;

    private void checkForUpdate() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    // Доступны обновления
                    Log.d("UpdateApp", "Available updates found");

                    // Запускаем процесс обновления
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE, // или AppUpdateType.FLEXIBLE
                                MainActivity.this, // Используем ссылку на активность
                                MY_REQUEST_CODE); // Код запроса для обновления
                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                } else {
                    String message = getString(R.string.update_ok);
                    MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }
        });
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
        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    // Доступны обновления
                    Log.d("UpdateApp", "Available updates found");
                    String title = getString(R.string.new_version);
                    String messageNotif = getString(R.string.news_of_version);

                    String urlStr = "https://play.google.com/store/apps/details?id=com.taxi.easy.ua";
                    NotificationHelper.showNotification(MainActivity.this, title, messageNotif, urlStr);
                }
            }
        });
    }






    private void restartApplication() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Отмена регистрации слушателя при уничтожении активности
        if (appUpdater != null) {
            appUpdater.unregisterListener();
        }
    }
    private static final int REQUEST_CODE_UNKNOWN_APP_SOURCES = 1234; // Произвольный код запроса разрешения
    @SuppressLint("ObsoleteSdkInt")
//    private void updateApp() throws InterruptedException {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            if (!getPackageManager().canRequestPackageInstalls()) {
//                // Пользователь еще не предоставил разрешение на установку пакетов из неизвестных источников.
//                // Здесь можно открыть системное окно настроек для запроса этого разрешения.
//                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
//                intent.setData(Uri.parse("package:" + getPackageName()));
//                startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP_SOURCES);
//            }
//        }
//
//        String fileName = "app-debug.apk";
//        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
//        String saveFilePath = file.getAbsolutePath();
//        String updateUrl = "https://m.easy-order-taxi.site/last_versions/" + getString(R.string.application);
//        AlertDialog alertDialog = progressBarUpload ();
//        alertDialog.show();
//        update_cancel = false;
//        FileDownloader.downloadFile(updateUrl, saveFilePath, new FileDownloader.DownloadCallback() {
//            @Override
//            public void onDownloadComplete(String filePath) {
//                File downloadedFile = new File(filePath);
//                long fileSizeInBytes = downloadedFile.length();
//                alertDialog.dismiss();
//                if(!update_cancel) {
//                    Log.d(TAG, "File size: " + fileSizeInBytes + " B");
//                    // Загрузка завершена, вызываем установку файла
//                    Log.d(TAG, "onDownloadComplete: " + filePath);
//                    installFile(filePath);
//                }
//
//            }
//
//            @Override
//            public void onDownloadFailed(Exception e) {
//                // Обработка ошибки загрузки файла
//                Log.d("TAG", "onDownloadFailed: " +  e.toString());
//                e.printStackTrace();
//            }
//        });
//    }

    private static final int INSTALL_REQUEST_CODE = 1;
    private boolean update_cancel;
    private void installFile(String filePath) {

        File file = new File(filePath);
        Log.d(TAG, "installFile: " + isApkFileValid(filePath));
        if (file.exists() && file.isFile()) {

            try {
                // Код установки приложения
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", file);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, INSTALL_REQUEST_CODE);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Installation failed: " + e.getMessage());
            }


        } else {
            Log.e(TAG, "File does not exist or is not a regular file");
            // Дополнительная обработка в случае отсутствия файла
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    public AlertDialog progressBarUpload () throws InterruptedException {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_layout, null);
//
// / Создание диалога с кастомным макетом
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(dialogView);
        alertDialogBuilder.setPositiveButton(getString(cancel_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                update_cancel = true;
            }
        });

        return alertDialogBuilder.create();

    }


    private boolean isApkFileValid(String filePath) {
        PackageManager pm = getPackageManager();
        PackageInfo packageInfo = pm.getPackageArchiveInfo(filePath, 0);
        if (packageInfo != null) {
            // Проверяем, что пакет содержит версию и название
            Log.d(TAG, "isApkFileValid: " + packageInfo.packageName);
            Log.d(TAG, "isApkFileValid: " + packageInfo.versionCode);
            return packageInfo.packageName != null && packageInfo.versionCode != 0;
        }
        return false;
    }


    private static final int PERMISSION_REQUEST_READ_PHONE_STATE = 1;
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            performPhoneStateOperation();
        } else {
            MyBottomSheetPhoneStateFragment bottomSheetDialogFragment = new MyBottomSheetPhoneStateFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }
    @SuppressLint({"HardwareIds", "ObsoleteSdkInt"})
    private void performPhoneStateOperation() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Начиная с Android 10, IMEI может быть недоступен без разрешения READ_PHONE_STATE
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "IMEI недоступен без разрешения", Toast.LENGTH_SHORT).show();
                    // Здесь вы можете запросить разрешение у пользователя
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_REQUEST_READ_PHONE_STATE);
                    return;
                }
            }
            String imei = null;
            String toastMessage = "";
            try {

                Log.d(TAG, "performPhoneStateOperation: Build.VERSION.SDK_INT" + Build.VERSION.SDK_INT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Для Android 8.0 (API уровня 26) и выше
                    imei = telephonyManager.getImei(); // Получение IMEI первой SIM-карты
                } else {
                    // Для Android ниже 8.0 (API уровня меньше 26)
                    imei = telephonyManager.getDeviceId(); // Получение IMEI устройства
                }
                if (imei != null) {
                    // Делаем что-то с IMEI
                    Log.d(TAG, "performPhoneStateOperation: IMEI: " + imei);
                    toastMessage = "IMEI: " + imei;
                } else {
                    // IMEI недоступен
                    Log.d(TAG, "performPhoneStateOperation: IMEI недоступен");
                    Toast.makeText(this, "IMEI недоступен", Toast.LENGTH_SHORT).show();
                }
            } catch (SecurityException e) {
                Log.d(TAG, "performPhoneStateOperation: IMEI недоступен");
                Toast.makeText(this, "IMEI недоступен", Toast.LENGTH_SHORT).show();

            }
            String deviceId = DeviceUtils.getDeviceId(getApplicationContext());
            toastMessage += " " + "Android ID устройства " + deviceId;
            try {
                String deviceIdSerial = DeviceUtils.getDeviceSerialNumber();
                toastMessage += " " + "Serial ID устройства " + deviceIdSerial;
                Toast.makeText(this, "Serial ID устройства " + deviceIdSerial, Toast.LENGTH_SHORT).show();
            }  catch (SecurityException e) {
               Log.d(TAG, "performPhoneStateOperation: IMEI недоступен");
               Toast.makeText(this, "Serial ID устройства недоступен", Toast.LENGTH_SHORT).show();

            }


            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();



        } else {
            // Устройство не поддерживает функции телефона
            Toast.makeText(this, "Функции телефона недоступны", Toast.LENGTH_SHORT).show();
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
            MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
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
        EditText userName = view.findViewById(R.id.userName);

        List<String> stringList =  logCursor(MainActivity.TABLE_USER_INFO);

        if(stringList.size() != 0) {
            phoneNumber.setText(stringList.get(2));
            userName.setText(stringList.get(4));


//        String result = phoneNumber.getText().toString();
        builder
                .setPositiveButton(R.string.cheng, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        if(connected()) {
                            Log.d("TAG", "onClick befor validate: ");
                            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                            boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                            Log.d("TAG", "onClick No validate: " + val);
                            if (!val) {
                                Toast.makeText(MainActivity.this, getString(format_phone) , Toast.LENGTH_SHORT).show();
                                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());

                            } else {
                               updateRecordsUser("phone_number", phoneNumber.getText().toString());
                               String newName = userName.getText().toString();
                               if (newName.trim().isEmpty()) {
                                   newName = "No_name";
                               }
                               updateRecordsUser("username", newName);
                            }
//                        }
                    }
                }).setNegativeButton(cancel_button, null)
                .show();
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

//        if (!hasConnect) {
//            Toast.makeText(this, verify_internet, Toast.LENGTH_LONG).show();
//        }
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
        Log.d(TAG, "newUser: " + userEmail);

        if(userEmail.equals("email")) {
//            checkNotificationPermissionAndRequestIfNeeded();
            new Thread(() -> insertPushDate(getApplicationContext())).start();

            try {
                FirebaseApp.initializeApp(MainActivity.this);
            } catch (Exception e) {
                Log.e(TAG, "Exception during authentication", e);
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);

            }
            Toast.makeText(this, R.string.checking, Toast.LENGTH_SHORT).show();
            startFireBase();

        } else {
            new Thread(() -> fetchRoutes(userEmail)).start();
            new Thread(() -> updatePushDate(getApplicationContext())).start();

            String application =  getString(R.string.application);
            new VerifyUserTask(userEmail, application, getApplicationContext()).execute();

            UserPermissions.getPermissions(userEmail, getApplicationContext());
            new UsersMessages(userEmail, getApplicationContext());

            // Проверка новой версии в маркете
            new Thread(this::versionFromMarket).start();

            Thread wfpCardThread = new Thread(() -> {
                List<String> stringList = logCursor(MainActivity.CITY_INFO);
                String city = stringList.get(1);
                getCardTokenWfp(city,"wfp", userEmail);

            });
            wfpCardThread.start();
        }


    }

    private void getCardTokenWfp(String city, String pay_system, String email) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site") // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        // Создайте сервис
        CallbackServiceWfp service = retrofit.create(CallbackServiceWfp.class);
        Log.d(TAG, "getCardTokenWfp: ");
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
                Log.d(TAG, "onResponse: " + response.body());
                if (response.isSuccessful()) {
                    CallbackResponseWfp callbackResponse = response.body();
                    if (callbackResponse != null) {
                        List<CardInfo> cards = callbackResponse.getCards();
                        Log.d(TAG, "onResponse: cards" + cards);
                        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.delete(MainActivity.TABLE_WFP_CARDS, "1", null);
                        if (cards != null && !cards.isEmpty()) {
                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты
                                String merchant = cardInfo.getMerchant(); // Токен карты

                                Log.d(TAG, "onResponse: card_token: " + rectoken);
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
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
                Log.d(TAG, "onResponse: failure " + t.toString());
            }
        });
    }
    private void startFireBase() {
        Toast.makeText(this, R.string.account_verify, Toast.LENGTH_SHORT).show();
        startSignInInBackground();
    }
    private void startSignInInBackground() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                Log.d(TAG, "run: ");
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
                    Log.e(TAG, "Exception during sign-in launch", e);
                    e.printStackTrace();
//                    Toast.makeText(getApplicationContext(), getApplicationContext().getString(verify_internet), Toast.LENGTH_SHORT).show();
                    VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
                }
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
                    Log.d(TAG, "onCreate:" + new RuntimeException(e));
                }
            }
    );


    private void onSignInResult(FirebaseAuthUIAuthenticationResult result, FragmentManager fm) throws MalformedURLException, JSONException, InterruptedException {
        ContentValues cv = new ContentValues();
        Log.d(TAG, "onSignInResult: ");
        try {
            Log.d(TAG, "onSignInResult: result.getResultCode() " + result.getResultCode());
            if (result.getResultCode() == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                assert user != null;
                settingsNewUser(user.getEmail());
                Toast.makeText(this, R.string.city_search, Toast.LENGTH_SHORT).show();
                startGetPublicIPAddressTask(fm, getApplicationContext());

                new Thread(() -> fetchRoutes(user.getEmail())).start();
            } else {
                Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
//                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.firebase_error));
//                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                cv.put("verifyOrder", "0");
                SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                database.close();
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
//            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.firebase_error));
//            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            cv.put("verifyOrder", "0");
            SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }
    }
    private void fetchRoutes(String value) {

        String url = baseUrl + "/android/UIDStatusShowEmail/" + value;
        Call<List<RouteResponse>> call = ApiClient.getApiService().getRoutes(url);
        routeList = new ArrayList<>();
        Log.d("TAG", "fetchRoutes: " + url);
        call.enqueue(new Callback<List<RouteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RouteResponse>> call, @NonNull Response<List<RouteResponse>> response) {
                if (response.isSuccessful()) {
                    List<RouteResponse> routes = response.body();
                    Log.d("TAG", "onResponse: " + routes);
                    if (routes != null && !routes.isEmpty()) {
                        boolean hasRouteWithAsterisk = false;
                        for (RouteResponse route : routes) {
                            if ("*".equals(route.getRouteFrom())) {
                                // Найден объект с routefrom = "*"
                                hasRouteWithAsterisk = true;
                                break;  // Выход из цикла, так как условие уже выполнено
                            }
                        }
                        if (!hasRouteWithAsterisk) {
                            routeList.addAll(routes);
                            processRouteList();
                        }

                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<RouteResponse>> call, @NonNull Throwable t) {

            }
        });
    }

    private void processRouteList() {
        // В этом методе вы можете использовать routeList для выполнения дополнительных действий с данными.

        // Создайте массив строк

        array = new String[routeList.size()];


        String closeReasonText = getString(R.string.close_resone_def);

        for (int i = 0; i < routeList.size(); i++) {
            RouteResponse route = routeList.get(i);

            String routeFrom = route.getRouteFrom();
            String routefromnumber = route.getRouteFromNumber();
            String startLat = route.getStartLat();
            String startLan = route.getStartLan();

            String routeTo = route.getRouteTo();
            String routeTonumber = route.getRouteToNumber();
            String to_lat = route.getTo_lat();
            String to_lng = route.getTo_lng();

            String webCost = route.getWebCost();
            String createdAt = route.getCreatedAt();
            String closeReason = route.getCloseReason();
            String auto = route.getAuto();

            switch (closeReason){
                case "-1":
                    closeReasonText = getString(R.string.close_resone_in_work);
                    break;
                case "0":
                    closeReasonText = getString(R.string.close_resone_0);
                    break;
                case "1":
                    closeReasonText = getString(R.string.close_resone_1);
                    break;
                case "2":
                    closeReasonText = getString(R.string.close_resone_2);
                    break;
                case "3":
                    closeReasonText = getString(R.string.close_resone_3);
                    break;
                case "4":
                    closeReasonText = getString(R.string.close_resone_4);
                    break;
                case "5":
                    closeReasonText = getString(R.string.close_resone_5);
                    break;
                case "6":
                    closeReasonText = getString(R.string.close_resone_6);
                    break;
                case "7":
                    closeReasonText = getString(R.string.close_resone_7);
                    break;
                case "8":
                    closeReasonText = getString(R.string.close_resone_8);
                    break;
                case "9":
                    closeReasonText = getString(R.string.close_resone_9);
                    break;

            }

            if(routeFrom.equals("Місце відправлення")) {
                routeFrom = getString(R.string.start_point_text);
            }


            if(routeTo.equals("Точка на карте")) {
                routeTo = getString(R.string.end_point_marker);
            }
            if(routeTo.contains("по городу")) {
                routeTo = getString(R.string.on_city);
            }
            if(routeTo.contains("по місту")) {
                routeTo = getString(R.string.on_city);
            }
            String routeInfo = "";

            if(auto == null) {
                auto = "??";
            }

            if(routeFrom.equals(routeTo)) {
                routeInfo = getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        + getString(R.string.close_resone_to)
                        + getString(R.string.on_city)
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)
                        + getString(R.string.auto_info) + " " + auto + " "
                        + getString(R.string.close_resone_time)
                        + createdAt + getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        + getString(R.string.close_resone_to) + routeTo + " " + routeTonumber
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)
                        + getString(R.string.auto_info) + " " + auto + " "
                        + getString(R.string.close_resone_time)
                        + createdAt + getString(R.string.close_resone_text) + closeReasonText;
            }

//                array[i] = routeInfo;
            databaseHelper.addRouteInfo(routeInfo);

            List<String> settings = new ArrayList<>();

            settings.add(startLat);
            settings.add(startLan);
            settings.add(to_lat);
            settings.add(to_lng);
            settings.add(routeFrom + " " + routefromnumber);
            settings.add(routeTo + " " + routeTonumber);
            Log.d(TAG, settings.toString());
            databaseHelperUid.addRouteInfoUid(settings);


        }
        array = databaseHelper.readRouteInfo();
        Log.d("TAG", "processRouteList: array 1211" + Arrays.toString(array));
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
        });
        updateUserInfoThread.start();

// Task 2: Add user with no name in a separate thread
        Thread addUserNoNameThread = new Thread(() -> {
            addUserNoName(emailUser, getApplicationContext());
        });
        addUserNoNameThread.start();

// Task 3: Fetch user phone information from the server in a separate thread
        Thread userPhoneThread = new Thread(() -> {
            userPhoneFromServer(emailUser);
        });
        userPhoneThread.start();

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
            userPhoneThread.join();
//            fondyCardThread.join();
//            monoCardThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    // Ограничение времени в секундах

    private void startGetPublicIPAddressTask(FragmentManager fm, Context context) {
        AsyncTask<Void, Void, String> getPublicIPAddressTask = new GetPublicIPAddressTask(fm, context);

        try {

            getPublicIPAddressTask.execute().get(MAX_TASK_EXECUTION_TIME_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Обработка исключения, возникающего при превышении времени выполнения задачи
            e.printStackTrace();
            // Дополнительные действия...
            getCityByIP("31.202.139.47", fm, context);
//            Toast.makeText(getApplicationContext(), getApplicationContext().getString(verify_internet), Toast.LENGTH_SHORT).show();
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }
    }


    public static void addUserNoName(String email, Context context) {
        // Создание объекта Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Создание экземпляра ApiService
        ApiServiceUser apiService = retrofit.create(ApiServiceUser.class);

        // Вызов метода addUserNoName
        Call<UserResponse> call = apiService.addUserNoName(email);

        // Асинхронный вызов
        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
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
//                Toast.makeText(context, context.getString(verify_internet), Toast.LENGTH_SHORT).show();
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);

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
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO);
        String MERCHANT_ID = arrayList.get(6);
        Log.d(TAG, "getCardToken:MERCHANT_ID " + MERCHANT_ID);
        if(MERCHANT_ID != null) {
    // Выполните запрос
    Call<CallbackResponse> call = service.handleCallback(email, pay_system, MERCHANT_ID);
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
                        // Очистка таблицы
                        database.delete(table, "1", null);

                        for (CardInfo cardInfo : cards) {
                            ContentValues cv = new ContentValues();
                            String masked_card = cardInfo.getMasked_card(); // Маска карты
                            String card_type = cardInfo.getCard_type(); // Тип карты
                            String bank_name = cardInfo.getBank_name(); // Название банка
                            String rectoken = cardInfo.getRectoken(); // Токен карты
                            String merchantId = cardInfo.getMerchant(); // Токен карты

                            Log.d(TAG, "onResponse: card_token: " + rectoken);

                            cv.put("masked_card", masked_card);
                            cv.put("card_type", card_type);
                            cv.put("bank_name", bank_name);
                            cv.put("rectoken", rectoken);
                            cv.put("merchant", merchantId);
                            cv.put("rectoken_check", "-1");
                            database.insert(table, null, cv);
                        }
                        // Выбираем минимальное значение ID из таблицы
                        Cursor cursor = database.rawQuery("SELECT MIN(id) FROM " + table, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            // Получаем минимальное значение ID
                            int minId = cursor.getInt(0);
                            cursor.close();

                            // Обновляем строку с минимальным ID
                            ContentValues cv = new ContentValues();
                            cv.put("rectoken_check", "1");
                            database.update(table, cv, "id = ?", new String[] { String.valueOf(minId) });
                        }
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
//                Toast.makeText(getApplicationContext(), getApplicationContext().getString(verify_internet), Toast.LENGTH_SHORT).show();
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }
    });
}


    }
    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    @Override
    public void onAutoClick() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        FragmentManager fragmentManager = getSupportFragmentManager();
        VisicomFragment visicomFragment = (VisicomFragment) fragmentManager.findFragmentByTag("VisicomFragment");

        if (visicomFragment != null) {
            // Если фрагмент существует, просто перейдите к нему
            navController.navigate(R.id.nav_visicom);
            visicomFragment.autoClickButton();
        } else {
            // Фрагмент не существует, создаем и добавляем его
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            VisicomFragment newFragment = new VisicomFragment();
            transaction.replace(R.id.nav_host_fragment_content_main, newFragment, "VisicomFragment");
            transaction.commit();

            // Ждем, чтобы убедиться, что фрагмент добавлен перед вызовом autoClickButton
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    VisicomFragment addedFragment = (VisicomFragment) getSupportFragmentManager().findFragmentByTag("VisicomFragment");
                    if (addedFragment != null) {
                        navController.navigate(R.id.nav_visicom);
                        addedFragment.autoClickButton();
                    }
                }
            }, 100);
        }
    }



//    @SuppressLint("StaticFieldLeak")
//    public class VerifyUserTask extends AsyncTask<Void, Void, Map<String, String>> {
//        private Exception exception;
//        @Override
//        protected Map<String, String> doInBackground(Void... voids) {
//            String userEmail = logCursor(TABLE_USER_INFO).get(3);
//
//            String url = "https://m.easy-order-taxi.site/android/verifyBlackListUser/" + userEmail + "/" + getString(R.string.application);
//            try {
//                return CostJSONParser.sendURL(url);
//            } catch (Exception e) {
//                exception = e;
////                Toast.makeText(getApplicationContext(), getApplicationContext().getString(verify_internet), Toast.LENGTH_SHORT).show();
//                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
//                return null;
//            }
//
//        }
//
//        @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
//        @Override
//        protected void onPostExecute(Map<String, String> sendUrlMap) {
//            String message = sendUrlMap.get("message");
//            ContentValues cv = new ContentValues();
//            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//            if (message != null) {
//
//                if (message.equals("В черном списке")) {
//
//                    cv.put("verifyOrder", "0");
//                    database.update(TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
//                } else {
//                    versionServer = message;
//                    //                        version(message);
//
//                    cv.put("verifyOrder", "1");
//                    database.update(TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
//
//                }
//            }
//            database.close();
//        }
//    }

    private static final String PREFS_NAME_VERSION = "MyPrefsFileNew";
    private static final String LAST_NOTIFICATION_TIME_KEY = "lastNotificationTimeNew";
//    private static final long ONE_DAY_IN_MILLISECONDS = 0; // 24 часа в миллисекундах
    private static final long ONE_DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000; // 24 часа в миллисекундах

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void version(String versionApi) throws MalformedURLException {


        // Получаем SharedPreferences
        SharedPreferences SharedPreferences = getSharedPreferences(PREFS_NAME_VERSION, Context.MODE_PRIVATE);

        // Получаем время последней отправки уведомления
        long lastNotificationTime = SharedPreferences.getLong(LAST_NOTIFICATION_TIME_KEY, 0);

        // Получаем текущее время
        long currentTime = System.currentTimeMillis();

        // Проверяем, прошло ли уже 24 часа с момента последней отправки
        if (currentTime - lastNotificationTime >= ONE_DAY_IN_MILLISECONDS) {
            if (!versionApi.equals(getString(R.string.version_code))) {

                String title = getString(R.string.new_version);
                String messageNotif = getString(R.string.news_of_version);

                String urlStr = "https://play.google.com/store/apps/details?id=com.taxi.easy.ua";
                NotificationHelper.showNotification(this, title, messageNotif, urlStr);

                // Обновляем время последней отправки уведомления
                SharedPreferences.Editor editor = SharedPreferences.edit();
                editor.putLong(LAST_NOTIFICATION_TIME_KEY, currentTime);
                editor.apply();


            }
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

    private void cityMaxPay(String city) {
        CityService cityService = CityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponse> call = cityService.getMaxPayValues(city);

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
//                Toast.makeText(getApplicationContext(), getApplicationContext().getString(verify_internet), Toast.LENGTH_SHORT).show();
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
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

                    }
                } else {
                    Log.e("Request", "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CityResponseMerchantFondy> call, Throwable t) {
                Log.e("Request", "Failed. Error message: " + t.getMessage());
//                Toast.makeText(getApplicationContext(), getApplicationContext().getString(verify_internet), Toast.LENGTH_SHORT).show();
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void userPhoneFromServer (String email) {
        ApiClientPhone apiClient = new ApiClientPhone();
        MainActivity.verifyPhone = false;
        apiClient.getUserPhone(email, new ApiClientPhone.OnUserPhoneResponseListener() {
            @Override
            public void onSuccess(String phone) {
                // Обработка успешного ответа
                Log.d("UserPhone", "Phone: " + phone);

                // Check if phone is not null
                if (phone != null) {
                    String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                    boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();

                    if (val) {
                        updateRecordsUser("phone_number", phone);
                    } else {
                        // Handle case where phone doesn't match the pattern
                        Log.e("UserPhone", "Phone does not match pattern");
                    }
                } else {
                    // Handle case where phone is null
                    Log.e("UserPhone", "Phone is null");
                }
            }


            @Override
            public void onError(String error) {
                // Обработка ошибки
                Log.e("UserPhone", "Error: " + error);
            }
        });
    }
    private static class GetPublicIPAddressTask extends AsyncTask<Void, Void, String> {
        FragmentManager fragmentManager;
        Context context;
        public GetPublicIPAddressTask(FragmentManager fragmentManager, Context context) {
            this.fragmentManager = fragmentManager;
            this.context = context;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                return IPUtil.getPublicIPAddress();
            } catch (Exception e) {
                // Log the exception
                Log.e(TAG, "Exception in doInBackground: " + e.getMessage());
                // Return null or handle the exception as needed
                getCityByIP("31.202.139.47",fragmentManager, context);
//                Toast.makeText(context, context.getString(verify_internet), Toast.LENGTH_SHORT).show();
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String ipAddress) {
            try {
                if (ipAddress != null) {
                    Log.d(TAG, "onCreate: Local IP Address: " + ipAddress);
                    getCityByIP(ipAddress, fragmentManager, context);
                } else {
                    getCityByIP("31.202.139.47",fragmentManager, context);
                }
            } catch (Exception e) {
                // Log the exception
                Log.e(TAG, "Exception in onPostExecute: " + e.getMessage());
                // Handle the exception as needed
                getCityByIP("31.202.139.47",fragmentManager, context);
//                Toast.makeText(context, context.getString(verify_internet), Toast.LENGTH_SHORT).show();
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            }

        }
    }

    private static void getCityByIP(String ip, FragmentManager fm, Context context) {

        ApiService apiService = ApiClient.getApiService();

        Call<City> call = apiService.cityByIp(ip);

        call.enqueue(new Callback<City>() {
            @Override
            public void onResponse(@NonNull Call<City> call, @NonNull Response<City> response) {
                if (response.isSuccessful()) {
                    City status = response.body();
                    if (status != null) {
                        String result = status.getResponse();
                        Log.d("TAG", "onResponse:result " + result);

                        MyBottomSheetCityFragment bottomSheetDialogFragment = new MyBottomSheetCityFragment(result, context);

                        if (!fm.isStateSaved()) {
                            bottomSheetDialogFragment.show(fm, bottomSheetDialogFragment.getTag());
                        } else {
                            Log.w("TAG", "Fragment state is already saved. Cannot perform transaction.");
                        }
                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<City> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d("TAG", "onFailure: " + errorMessage);
//                Toast.makeText(context, context.getString(verify_internet), Toast.LENGTH_SHORT).show();
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            }
        });
//        }
    }


}