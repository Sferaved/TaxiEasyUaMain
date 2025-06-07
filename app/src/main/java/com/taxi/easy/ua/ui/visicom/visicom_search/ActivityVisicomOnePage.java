package com.taxi.easy.ua.ui.visicom.visicom_search;


import static com.taxi.easy.ua.androidx.startup.MyApplication.getContext;
import static com.taxi.easy.ua.androidx.startup.MyApplication.getCurrentActivity;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.cities.Kyiv.KyivRegion;
import com.taxi.easy.ua.ui.cities.Kyiv.KyivRegionRu;
import com.taxi.easy.ua.ui.keyboard.KeyboardUtils;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapVisicomActivity;
import com.taxi.easy.ua.ui.open_map.mapbox.Feature;
import com.taxi.easy.ua.ui.open_map.mapbox.Geometry;
import com.taxi.easy.ua.ui.open_map.mapbox.MapboxApiClient;
import com.taxi.easy.ua.ui.open_map.mapbox.MapboxResponse;
import com.taxi.easy.ua.ui.open_map.mapbox.MapboxService;
import com.taxi.easy.ua.ui.open_map.mapbox.key_mapbox.ApiClientMapbox;
import com.taxi.easy.ua.ui.open_map.mapbox.key_mapbox.ApiResponseMapbox;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.ui.visicom.visicom_search.key_visicom.ApiResponse;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.utils.city.CityFinder;
import com.taxi.easy.ua.utils.connect.ConnectionSpeedTester;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ActivityVisicomOnePage extends AppCompatActivity {

    private static final String TAG = "ActivityVisicomOnePage";

    AppCompatButton btn_change, btnOnMap;

    ProgressBar progressBar;
    EditText fromEditAddress, toEditAddress;
    private ImageButton btn_clear_from, btn_clear_to, btn_ok, btn_no;
    private static List<double[]> coordinatesList;
    private static List<String[]> addresses;
    private OkHttpClient client;
    private String startPoint, finishPoint;
    ListView addressListView;

    private boolean verifyBuildingStart;
    private boolean verifyBuildingFinish;
    private TextView textGeoError, text_toError;
    private String citySearch;
    private String[] kyivRegionArr;
    private int positionChecked;
    private String zone;
    private boolean verifyRoutStart;
    private boolean verifyRoutFinish;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private String messageInfo;
    private String startMarker;
    private String finishMarker;
    private String start;
    private String end;
    ArrayAdapter<String> addressAdapter;
    private boolean extraExit;
    private final long timeout = 50;
    LocationManager locationManager;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    private boolean location_update;
    private ImageButton scrollButtonDown, scrollButtonUp;
    private final int desiredHeight = 630;
    private final int max_length_string_size = 4;
    List<String> addressesList;
    AppCompatButton btnCallAdmin;
    ViewGroup.LayoutParams layoutParams;
    String countryState;
    private SharedPreferencesHelper sharedPreferencesHelper;

    @SuppressLint({"MissingInflatedId", "UseCompatLoadingForDrawables"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        setContentView(R.layout.activity_visicom_address_layout);


    }

    private void scrollSetVisibility() {
        if (addressesList != null) {
            addressListView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                int totalHeight = 0;
                int desiredWidth = View.MeasureSpec.makeMeasureSpec(addressListView.getWidth(), View.MeasureSpec.AT_MOST);

                for (int i = 0; i < addressAdapter.getCount(); i++) {
                    View listItem = addressAdapter.getView(i, null, addressListView);

                    // Замер высоты элемента
                    listItem.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
                    totalHeight += listItem.getMeasuredHeight();
                }
                Log.d("TotalHeight", "Total height of all items: " + totalHeight);
                if (totalHeight > 300) {
                    scrollButtonUp.setVisibility(View.VISIBLE);
                    scrollButtonDown.setVisibility(View.VISIBLE);
                } else {
                    scrollButtonUp.setVisibility(View.GONE);
                    scrollButtonDown.setVisibility(View.GONE);
                }
            });



        }
    }

    private void firstLocation() {
        // Получить менеджер ввода
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        inputMethodManager.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), 0);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        Logger.d(getApplicationContext(), TAG, "firstLocation: ");
        btn_change.setText(R.string.cancel_gps);

        btn_change.setOnClickListener(v -> {
            progressBar.setVisibility(View.INVISIBLE);
//                btn_clear_from.performClick();
            if (fusedLocationProviderClient != null && locationCallback != null) {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                Logger.d(getApplicationContext(), TAG, "Location updates cancelled");
                // Дополнительные действия, которые вы хотите выполнить при отмене геопоиска
                // Например, изменение текста на кнопке или другие обновления интерфейса
                btn_change.setText(R.string.change); // Предположим, что текст на кнопке изменяется на "Start GPS"
            }

            btn_change.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    if (locationManager != null) {
                        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            Logger.d(getApplicationContext(), TAG, "locationManager: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
                            if(loadPermissionRequestCount() >= 3  && !location_update) {
                                MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment(getString(R.string.location_on));
                                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        // Обработка отсутствия необходимых разрешений
                                        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                                    }
                                } else {
                                    // Для версий Android ниже 10
                                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                            || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        // Обработка отсутствия необходимых разрешений
                                        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                                        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                                    }
                                }
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    // Обработка отсутствия необходимых разрешений
                                    location_update = true;
                                }
                            } else location_update = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;


                            // GPS включен, выполните ваш код здесь
                            if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                                MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                        .setPopUpTo(R.id.nav_restart, true)
                                        .build());
                            }

                            if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                                NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
                                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                        .setPopUpTo(R.id.nav_restart, true)
                                        .build());
                            }

                            else {

                                if (location_update) {
                                    String searchText = getString(R.string.search_text) + "...";
                                    Toast.makeText(ActivityVisicomOnePage.this, searchText, Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.VISIBLE);
                                    firstLocation();

                                }
                            }

                        } else {
                            // GPS выключен, выполните необходимые действия
                            // Например, показать диалоговое окно с предупреждением о включении GPS
                            MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                        }
                    }
                }
            });
        });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Обработка полученных местоположений
                stopLocationUpdates();

                // Обработка полученных местоположений
                List<Location> locations = locationResult.getLocations();
                Logger.d(getApplicationContext(), TAG, "onLocationResult: locations 222222" + locations);

                if (!locations.isEmpty()) {
                    Location firstLocation = locations.get(0);

                    double latitude = firstLocation.getLatitude();
                    double longitude = firstLocation.getLongitude();

                    CityFinder cityFinder = new CityFinder(getContext());
                    cityFinder.findCity(latitude, longitude);

                    List<String> stringList = logCursor(MainActivity.CITY_INFO);
                    String api =  stringList.get(2);

                    Locale locale = Locale.getDefault();
                    String language = locale.getLanguage(); // Получаем язык устройства
                    String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                    String urlFrom = baseUrl + "/" + api + "/android/fromSearchGeoLocal/"  + latitude + "/" + longitude + "/" + language;

                    try {
                        FromJSONParser parser = new FromJSONParser(urlFrom);
                        Map<String, String> sendUrlFrom = parser.sendURL(urlFrom);
                        assert sendUrlFrom != null;
                        String FromAdressString = sendUrlFrom.get("route_address_from");
                        if (FromAdressString != null) {
                            if (FromAdressString.contains("Точка на карте")) {
                                FromAdressString = getString(R.string.startPoint);
                            }
                        }
                        updateMyPosition(latitude, longitude, FromAdressString, getApplicationContext());
                        fromEditAddress.setText(FromAdressString);
                        progressBar.setVisibility(View.INVISIBLE);
                        assert FromAdressString != null;
                        fromEditAddress.setSelection(FromAdressString.length());

                        btn_clear_from.setVisibility(View.VISIBLE);
                        VisicomFragment.geoText.setText(FromAdressString);

                        List<String> settings = new ArrayList<>();
                        String ToAdressString = toEditAddress.getText().toString();
                        if(ToAdressString.equals(getString(R.string.on_city_tv)) ||
                                ToAdressString.isEmpty()) {
                            settings.add(Double.toString(latitude));
                            settings.add(Double.toString(longitude));
                            settings.add(Double.toString(latitude));
                            settings.add(Double.toString(longitude));
                            settings.add(FromAdressString);
                            settings.add(getString(R.string.on_city_tv));
                        } else {

                            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            Cursor cursor = database.rawQuery(query, null);

                            cursor.moveToFirst();

                            // Получите значения полей из первой записи


                            @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                            @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                            cursor.close();
                            database.close();

                            settings.add(Double.toString(latitude));
                            settings.add(Double.toString(longitude));
                            settings.add(Double.toString(toLatitude));
                            settings.add(Double.toString(toLongitude));
                            settings.add(FromAdressString);
                            settings.add(ToAdressString);
                        }
                        updateRoutMarker(settings);
                        btn_ok.performClick();

                    } catch (MalformedURLException | InterruptedException |
                             JSONException ignored) {

                    }
                }
            }

        };

        startLocationUpdates();

    }

    private static void updateMyPosition(Double startLat, Double startLan, String position, Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        ContentValues cv = new ContentValues();

        cv.put("startLat", startLat);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                new String[] { "1" });
        cv.put("startLan", startLan);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                new String[] { "1" });
        cv.put("position", position);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();

    }
    private void startLocationUpdates() {
        LocationRequest locationRequest = createLocationRequest();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private LocationRequest createLocationRequest() {
        return new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY)
                .setIntervalMillis(1000)          // Интервал обновления местоположения в миллисекундах
                .setMinUpdateIntervalMillis(100)  // Самый быстрый интервал обновления
                .build();
    }


    private void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, LOCATION_PERMISSION_REQUEST_CODE);

        }
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Logger.d(getApplicationContext(), TAG, "onRequestPermissionsResult: " + requestCode);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (permissions.length > 0) {
//                SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();
                for (int i = 0; i < permissions.length; i++) {
                    sharedPreferencesHelperMain.saveValue(permissions[i], grantResults[i]);
//                    editor.putInt(permissions[i], grantResults[i]);

                }
//                editor.apply();

                int permissionRequestCount = loadPermissionRequestCount();

                // Увеличение счетчика запросов разрешений при необходимости
                permissionRequestCount++;

                // Сохранение обновленного значения счетчика
                savePermissionRequestCount(permissionRequestCount);
                Logger.d(getApplicationContext(), TAG, "permissionRequestCount: " + permissionRequestCount);
                // Далее вы можете загрузить сохраненные разрешения и их результаты в любом месте вашего приложения,
                // используя тот же самый объект SharedPreferences
            }
        }
    }


    // Метод для сохранения количества запросов разрешений в SharedPreferences
    private void savePermissionRequestCount(int count) {
        sharedPreferencesHelperMain.saveValue(MainActivity.PERMISSION_REQUEST_COUNT_KEY, count);
//        SharedPreferences.Editor editor = MainActivity.sharedPreferencesCount.edit();
//        editor.putInt(MainActivity.PERMISSION_REQUEST_COUNT_KEY, count);
//        editor.apply();
    }

    // Метод для загрузки количества запросов разрешений из SharedPreferences
    private int loadPermissionRequestCount() {
//        return MainActivity.sharedPreferencesCount.getInt(MainActivity.PERMISSION_REQUEST_COUNT_KEY, 0);
        return (int) sharedPreferencesHelperMain.getValue(MainActivity.PERMISSION_REQUEST_COUNT_KEY, 0);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onResume() {
        super.onResume();
        if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
            NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        }

//        if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
//            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                    .setPopUpTo(R.id.nav_restart, true)
//                    .build());
//        }
        start = getIntent().getStringExtra("start");
        end = getIntent().getStringExtra("end");
        client = new OkHttpClient();
        List<String> stringList = logCursor(MainActivity.CITY_INFO);

        if(MainActivity.apiKey == null) {
            visicomKey();
        }
        if(MainActivity.apiKeyMapBox == null) {
            mapboxKey();
        }
        sharedPreferencesHelper = new SharedPreferencesHelper(this);
        countryState = (String) sharedPreferencesHelperMain.getValue("countryState", "**");

        switch (LocaleHelper.getLocale()) {
            case "ru":
                switch (stringList.get(1)) {
                    case "Kyiv City":
                        citySearch = "Киев";
                        kyivRegionArr = KyivRegionRu.city();
                        break;
                    case "Dnipropetrovsk Oblast":
                        citySearch = "Днепр";
                        break;
                    case "Odessa":
                    case "OdessaTest":
                        citySearch = "Одесса";
                        break;
                    case "Zaporizhzhia":
                        citySearch = "Запорожье";
                        break;
                    case "Cherkasy Oblast":
                        citySearch = "Черкассы";
                        break;
                    case "Ivano_frankivsk":
                        citySearch = "Ивано-Франковск";
                        break;
                    case "Vinnytsia":
                        citySearch = "Винница";
                        break;
                    case "Poltava":
                        citySearch = "Полтава";
                        break;
                    case "Sumy":
                        citySearch = "Сумы";
                        break;
                    case "Kharkiv":
                        citySearch = "Харьков";
                        break;
                    case "Chernihiv":
                        citySearch = "Чернигов";
                        break;
                    case "Rivne":
                        citySearch = "Ровно";
                        break;
                    case "Ternopil":
                        citySearch = "Тернополь";
                        break;
                    case "Khmelnytskyi":
                        citySearch = "Хмельницкий";
                        break;
                    case "Zakarpattya":
                        citySearch = "Ужгород";
                        break;
                    case "Zhytomyr":
                        citySearch = "Житомир";
                        break;
                    case "Kropyvnytskyi":
                        citySearch = "Кропивницкий";
                        break;
                    case "Mykolaiv":
                        citySearch = "Николаев";
                        break;
                    case "Chernivtsi":
                        citySearch = "Черновцы";
                        break;
                    case "Lutsk":
                        citySearch = "Луцк";
                        break;
                    default:
                        citySearch = "FC";
                        break;
                }
                break;
            case "en":
                switch (stringList.get(1)) {
                    case "Kyiv City":
                        citySearch = "Kyiv";
                        kyivRegionArr = KyivRegionRu.city();
                        break;
                    case "Dnipropetrovsk Oblast":
                        citySearch = "Dnipro";
                        break;
                    case "Odessa":
                    case "OdessaTest":
                        citySearch = "Odessa";
                        break;
                    case "Zaporizhzhia":
                        citySearch = "Zaporizhzhia";
                        break;
                    case "Cherkasy Oblast":
                        citySearch = "Cherkasy";
                        break;
                    case "Ivano_frankivsk":
                        citySearch = "Ivano-";
                        break;
                    case "Vinnytsia":
                        citySearch = "Vinnytsia";
                        break;
                    case "Poltava":
                        citySearch = "Poltava";
                        break;
                    case "Sumy":
                        citySearch = "Sumy";
                        break;
                    case "Kharkiv":
                        citySearch = "Kharkiv";
                        break;
                    case "Chernihiv":
                        citySearch = "Chernihiv";
                        break;
                    case "Rivne":
                        citySearch = "Rivne";
                        break;
                    case "Ternopil":
                        citySearch = "Ternopil";
                        break;
                    case "Khmelnytskyi":
                        citySearch = "Khmelnytskyi";
                        break;
                    case "Zakarpattya":
                        citySearch = "Zakarpattya";
                        break;
                    case "Zhytomyr":
                        citySearch = "Zhytomyr";
                        break;
                    case "Kropyvnytskyi":
                        citySearch = "Kropyvnytskyi";
                        break;
                    case "Mykolaiv":
                        citySearch = "Mykolaiv";
                        break;
                    case "Chernivtsi":
                        citySearch = "Сhernivtsi";
                        break;
                    case "Lutsk":
                        citySearch = "Lutsk";
                        break;
                    default:
                        citySearch = "FC";
                        break;
                }
                break;
            default:
                switch (stringList.get(1)) {
                    case "Kyiv City":
                        citySearch = "Київ";
                        kyivRegionArr = KyivRegion.city();
                        break;
                    case "Dnipropetrovsk Oblast":
                        citySearch = "Дніпро";
                        break;
                    case "Odessa":
                    case "OdessaTest":
                        citySearch = "Одеса";
                        break;
                    case "Zaporizhzhia":
                        citySearch = "Запоріжжя";
                        break;
                    case "Cherkasy Oblast":
                        citySearch = "Черкаси";
                        break;
                    case "Ivano_frankivsk":
                        citySearch = "Івано-Франківськ";
                        break;
                    case "Vinnytsia":
                        citySearch = "Вінниця";
                        break;
                    case "Poltava":
                        citySearch = "Полтава";
                        break;
                    case "Sumy":
                        citySearch = "Суми";
                        break;
                    case "Kharkiv":
                        citySearch = "Харків";
                        break;
                    case "Chernihiv":
                        citySearch = "Чернігів";
                        break;
                    case "Rivne":
                        citySearch = "Рівне";
                        break;
                    case "Ternopil":
                        citySearch = "Тернопіль";
                        break;
                    case "Khmelnytskyi":
                        citySearch = "Хмельницький";
                        break;
                    case "Zakarpattya":
                        citySearch = "Ужгород";
                        break;
                    case "Zhytomyr":
                        citySearch = "Житомир";
                        break;
                    case "Kropyvnytskyi":
                        citySearch = "Кропивницький";
                        break;
                    case "Mykolaiv":
                        citySearch = "Миколаїв";
                        break;
                    case "Chernivtsi":
                        citySearch = "Чернівці";
                        break;
                    case "Lutsk":
                        citySearch = "Луцьк";
                        break;
                    default:
                        citySearch = "FC";
                        break;
                }
        }

        textGeoError = findViewById(R.id.textGeoError);
        text_toError = findViewById(R.id.text_toError);


        addressListView = findViewById(R.id.listAddress);
        progressBar = findViewById(R.id.progress_bar_visicom);

        btn_no = findViewById(R.id.btn_no);

        btn_ok = findViewById(R.id.btn_ok);

        btn_ok.setVisibility(View.INVISIBLE);

        btn_no.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            // Получаем доступ к InputMethodManager
            InputMethodManager immHide = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            // Пытаемся скрыть клавиатуру
            immHide.hideSoftInputFromWindow(v.getWindowToken(), 0);
            if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
            }
//            if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
//                MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                        .setPopUpTo(R.id.nav_restart, true)
//                        .build());
//            }
            VisicomFragment.btnVisible(View.INVISIBLE);
            finish();

        });

        fromEditAddress = findViewById(R.id.textGeo);

        int inputType = fromEditAddress.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        fromEditAddress.setInputType(inputType);


        toEditAddress = findViewById(R.id.text_to);
        inputType = toEditAddress.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        toEditAddress.setInputType(inputType);

        addressesList = new ArrayList<>();
        addressAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_list_item, addressesList);
        addressListView.setAdapter(addressAdapter);
        scrollButtonUp = findViewById(R.id.scrollButtonUp);
        scrollButtonDown = findViewById(R.id.scrollButtonDown);

        scrollSetVisibility();
        btn_clear_from = findViewById(R.id.btn_clear_from);
        btn_clear_from.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollButtonUp.setVisibility(View.GONE);
                scrollButtonDown.setVisibility(View.GONE);
                addresses = new ArrayList<>();
                coordinatesList = new ArrayList<>();

                addressesList = new ArrayList<>();
                addressAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_list_item, addressesList);

                addressListView.setAdapter(addressAdapter);


                fromEditAddress.setText("");
                btn_clear_from.setVisibility(View.INVISIBLE);
                textGeoError.setVisibility(View.GONE);

                fromEditAddress.requestFocus();
                KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
            }
        });
        btn_clear_to = findViewById(R.id.btn_clear_to);
        btn_clear_to.setOnClickListener(v -> {
            scrollButtonUp.setVisibility(View.GONE);
            scrollButtonDown.setVisibility(View.GONE);

            addresses = new ArrayList<>();
            coordinatesList = new ArrayList<>();
            addressesList = new ArrayList<>();
            addressAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_list_item, addressesList);


            addressListView.setAdapter(addressAdapter);

            toEditAddress.setText("");
            btn_clear_to.setVisibility(View.INVISIBLE);
            text_toError.setVisibility(View.GONE);
            toEditAddress.requestFocus();
            KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);

        });

        verifyRoutStart = true;
        verifyRoutFinish = true;
        btn_change = findViewById(R.id.change);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btn_change.setOnClickListener(v -> {
            if (locationManager != null) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                    if(loadPermissionRequestCount() >= 3  && !location_update) {
                        MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment(getString(R.string.location_on));
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                // Обработка отсутствия необходимых разрешений
                                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                            }
                        } else {
                            // Для версий Android ниже 10
                            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                    || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                // Обработка отсутствия необходимых разрешений
                                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                                checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            // Обработка отсутствия необходимых разрешений
                            location_update = true;
                        }
                    } else location_update = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;


                    Logger.d(getApplicationContext(), TAG, "locationManager: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
                    // GPS включен, выполните ваш код здесь
//                    if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
//                        MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                                .setPopUpTo(R.id.nav_restart, true)
//                                .build());
//                    }
                    if (!NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                        NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
                        navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_restart, true)
                                .build());
                    }

                    else  if(location_update) {
                        String searchText = getString(R.string.search_text) + "...";

                        progressBar.setVisibility(View.VISIBLE);
                        Toast.makeText(ActivityVisicomOnePage.this, searchText, Toast.LENGTH_SHORT).show();
                        firstLocation();
                    }
                } else {
                    MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }

            } else {
                // GPS выключен, выполните необходимые действия
                // Например, показать диалоговое окно с предупреждением о включении GPS
                MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });

        if(start.equals("ok")) {
            oldAddresses("start");
            toEditAddress.setVisibility(View.GONE);
            btn_clear_to.setVisibility(View.GONE);
            text_toError.setVisibility(View.GONE);
            findViewById(R.id.textwhere).setVisibility(View.GONE);
            findViewById(R.id.num2).setVisibility(View.GONE);
        }
        if(end.equals("ok")) {
            oldAddresses("finish");
            findViewById(R.id.textfrom).setVisibility(View.GONE);
            findViewById(R.id.num1).setVisibility(View.GONE);
            fromEditAddress.setVisibility(View.GONE);
            btn_clear_from.setVisibility(View.GONE);

            textGeoError.setVisibility(View.GONE);
            btn_change.setText(getString(R.string.on_city_tv));
            btn_change.setOnClickListener(v -> {

                String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                Cursor cursor = database.rawQuery(query, null);

                cursor.moveToFirst();

                // Получите значения полей из первой записи


                @SuppressLint("Range") double startLat = cursor.getDouble(cursor.getColumnIndex("startLat"));
                @SuppressLint("Range") double startLan = cursor.getDouble(cursor.getColumnIndex("startLan"));
                @SuppressLint("Range") String start = cursor.getString(cursor.getColumnIndex("start"));
                cursor.close();
                database.close();
                List<String> settings = new ArrayList<>();
                settings.add(String.valueOf(startLat));
                settings.add(String.valueOf(startLan));
                settings.add(String.valueOf(startLat));
                settings.add(String.valueOf(startLan));
                settings.add(start);
                settings.add(getString(R.string.on_city_tv));

                updateRoutMarker(settings);
                extraExit = true;
                btn_ok.performClick();
            });
        }
        btnOnMap = findViewById(R.id.btn_on_map);
        btnOnMap.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), OpenStreetMapVisicomActivity.class);

            intent.putExtra("startMarker", start);
            intent.putExtra("finishMarker", end);

            startActivity(intent);
            finish();
        });
        btn_ok.setOnClickListener(v -> {
            sharedPreferencesHelper.saveValue("gps_upd_address", false);
            Logger.d(this, TAG, "sharedPreferencesHelper.getValue(\"gps_upd\", false)" +sharedPreferencesHelper.getValue("gps_upd", false));

            startActivity(new Intent(this, MainActivity.class));
        });

        scrollButtonDown.setOnClickListener(v -> {
            int nextVisiblePosition = addressListView.getLastVisiblePosition() + 1;
            addressListView.smoothScrollToPosition(nextVisiblePosition);
        });

        scrollButtonUp.setOnClickListener(v -> {
            int offset = -1; // или другое значение, чтобы указать направление прокрутки
            addressListView.smoothScrollByOffset(offset);
        });

        layoutParams = addressListView.getLayoutParams();
        layoutParams.height = desiredHeight;
        addressListView.setLayoutParams(layoutParams);

        btnCallAdmin = findViewById(R.id.btnCallAdmin);
        btnCallAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                List<String> stringList = logCursor(MainActivity.CITY_INFO);
                String phone = stringList.get(3);
                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });



        location_update = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Обработка отсутствия необходимых разрешений
                location_update = true;
            }
        } else location_update = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if(start.equals("ok")){
            fromEditAddress.requestFocus();
            fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
            KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    btn_change.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_yellow));

                    btn_change.setTextColor(Color.BLACK);
                } else {
                    btn_change.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_green));
                    btn_change.setTextColor(Color.WHITE);
                }
            } else {
                btn_change.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_red));
                btn_change.setTextColor(Color.WHITE);

            }
        }
        if(end.equals("ok")) {
            toEditAddress.requestFocus();
            toEditAddress.setSelection(toEditAddress.getText().toString().length());
            KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);
        }


        fromEditAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int after) {
                // Пользователь удаляет символы, если before > 0
                String inputString = s.toString();

                int charCount = inputString.length();
                if (before > 0 && charCount > 2) {
                    positionChecked = 0;
                }

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {

                layoutParams.height = desiredHeight;
                addressListView.setLayoutParams(layoutParams);
                btnCallAdmin.setVisibility(View.GONE);

                String inputString = charSequence.toString();
                int charCount = inputString.length();
                Logger.d(getApplicationContext(), TAG, "onTextChanged: " + inputString);
                if (charCount > 2) {
                    Logger.d(getApplicationContext(), TAG, "onTextChanged:startPoint " + startPoint);
                    Logger.d(getApplicationContext(), TAG, "onTextChanged:fromEditAddress.getText().toString() " + fromEditAddress.getText().toString());

                    Logger.d(getApplicationContext(), TAG, "onTextChanged:countryState " + countryState);

                    if (startPoint == null) {
                        if(countryState.equals("UA")) {
                            performAddressSearch(inputString, "start");
                        } else {
                            mapBoxSearch(inputString, "start");
                        }
                    } else if (!startPoint.equals(inputString)) {
                        if(countryState.equals("UA")) {
                            performAddressSearch(inputString, "start");
                        } else {
                            mapBoxSearch(inputString, "start");
                        }
                    }
                    textGeoError.setVisibility(View.GONE);
                }
                btn_clear_from.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        toEditAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int after) {
                String inputString = s.toString();
                int charCount = inputString.length();
                if (before > 0 && charCount > 2) {
                    positionChecked = 0;
                }

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                layoutParams.height = desiredHeight;
                addressListView.setLayoutParams(layoutParams);
                btnCallAdmin.setVisibility(View.GONE);

                // Вызывается при изменении текста
                String inputString = charSequence.toString();
                int charCount = inputString.length();

                if (charCount > 2) {

                    if (finishPoint == null) {
                        if(countryState.equals("UA")) {
                            performAddressSearch(inputString, "finish");
                        } else {
                            mapBoxSearch(inputString, "finish");
                        }
                    } else if (!finishPoint.equals(inputString)) {
                        if(countryState.equals("UA")) {
                            performAddressSearch(inputString, "finish");
                        } else {
                            mapBoxSearch(inputString, "finish");
                        }
                    }
                }

                text_toError.setVisibility(View.GONE);
             }

            @Override
            public void afterTextChanged(Editable editable) {
                // Вызывается после изменения текста

            }
        });

        if (fromEditAddress.getText().toString().isEmpty()) {

            btn_clear_from.setVisibility(View.INVISIBLE);
            fromEditAddress.requestFocus();

            fromEditAddress.post(new Runnable() {
                @Override
                public void run() {
                    KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
                }
            });
        } else if (toEditAddress.getText().toString().equals("")) {
            toEditAddress.requestFocus();
            btn_clear_to.setVisibility(View.INVISIBLE);

            toEditAddress.post(new Runnable() {
                @Override
                public void run() {
                    KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);
                }
            });

        }
    }

    private void performAddressSearch(String inputText, String point) {

        try {
            String apiUrl = "https://api.visicom.ua/data-api/5.0/";
            String url = apiUrl  + LocaleHelper.getLocale() + "/geocode.json";


            if (point.equals("start")) {
                verifyBuildingStart = false;
            } else {
                verifyBuildingFinish = false;
            }
            String modifiedText = "";
            Logger.d(getApplicationContext(), TAG, "performAddressSearch:modifiedText " + modifiedText);
            if (!inputText.substring(3).contains("\f")) {
                modifiedText = inputText.replaceAll("[\f\t]", " ");
                url = url
                        + "?"
                        + "categories=poi_railway_station"
                        + ",adm_settlement"
                        + ",poi_bus_station"
                        + ",poi_airport_terminal"
                        + ",poi_airport"
                        + ",poi_shopping_centre"
                        + ",poi_night_club"
                        + ",poi_hotel_and_motel"
                        + ",poi_cafe_bar"
                        + ",poi_restaurant"
                        + ",poi_entertaining_complex"
                        + ",poi_supermarket"
                        + ",poi_grocery"
                        + ",poi_swimming_pool"
                        + ",poi_sports_complexe"
                        + ",poi_post_office"
                        + ",poi_underground_railway_station"
                        + ",poi_hospital"
                        + ",adr_street"
                        + "&l=20"
                        + "&text=" + modifiedText + "&key=" + MainActivity.apiKey;

            } else {
                Logger.d(getApplicationContext(), TAG, "performAddressSearch:positionChecked  " + positionChecked);
                String number = numbers(modifiedText);

                if (positionChecked != 0) {
                    inputText = inputTextBuild() + ", " + number;
                }
                modifiedText = inputText.replaceAll("[\f\t]", " ");
                url = url + "?categories=adr_address&text=" + modifiedText
                        + "&l=20" + "&key=" + MainActivity.apiKey;

            }

            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Logger.d(getApplicationContext(), TAG, "performAddressSearch: " + url);
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) {
                    try {
                        assert response.body() != null;
                        String responseData = response.body().string();
                        Logger.d(getApplicationContext(), TAG, "onResponse: " + responseData);
                        processAddressData(responseData, point);
                    } catch (Exception e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                }

                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            });
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }



    }


    private String inputTextBuild() {
        Logger.d(getApplicationContext(), TAG, "inputTextBuild: " + positionChecked);
        String result = "";
        if(positionChecked != 0){
            String[] selectedAddress = addresses.get(positionChecked);
            Logger.d(getApplicationContext(), TAG, "inputTextBuild: " + Arrays.toString(selectedAddress));
            // Получение элементов отдельно
            String name = selectedAddress[1];
            zone = selectedAddress[2];
            String settlement = selectedAddress[3];
            result = settlement + ", " + name;
        }


        return result;

    }

    @SuppressLint("ResourceType")
    private void processAddressData(String responseData, String point) {
        extraExit = false;
        try {
            JSONObject jsonResponse = new JSONObject(responseData);
            Logger.d(getApplicationContext(), TAG, "processAddressData:jsonResponse " + jsonResponse);

            if (jsonResponse.has("features")) {
                addresses = new ArrayList<>();
                coordinatesList = new ArrayList<>(); // Список для хранения координат
                JSONArray features = jsonResponse.getJSONArray("features");

                Logger.d(getApplicationContext(), TAG, "processAddressData: features" + features.length());

                // В массиве есть элементы, обрабатываем результат
                // Ваши дополнительные действия с features
                for (int i = 0; i < features.length(); i++) {
                    JSONObject properties = features.getJSONObject(i).getJSONObject("properties");
//                    Logger.d(getApplicationContext(), TAG, "processAddressData:properties " + i + " - " + properties);
                    JSONObject geoCentroid = features.getJSONObject(i).getJSONObject("geo_centroid");

                    if (properties.getString("country_code").equals("ua")) {
                        switch (properties.getString("categories")) {
                            case "adm_settlement":

                                // Проверка по Киевской области
                                if (citySearch.equals("Київ") || citySearch.equals("Киев")) {

                                        String addressAdm = String.format("%s %s\t",
                                                properties.getString("type"),
                                                properties.getString("name")
                                        );

                                        double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                        double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

                                        addAddressOne(
                                                addressAdm,
                                                "",
                                                "",
                                                "",
                                                longitude,
                                                latitude);

                                }
                                break;
                            case "adr_street":
                                String settlement = properties.optString("settlement", "").toLowerCase();
                                String city = citySearch.toLowerCase();
                                String address;
                                if (settlement.contains(city)) {
                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    if (properties.has("zone")) {
                                        address = String.format("%s %s (%s)\f",
                                                properties.getString("type"),
                                                properties.getString("name"),
                                                properties.getString("zone"));

                                        addAddressOne(
                                                address,
                                                properties.getString("name"),
                                                "",
                                                properties.getString("settlement"),
                                                longitude,
                                                latitude);
                                    } else {
                                        address = String.format("%s %s\f",
                                                properties.getString("type"),
                                                properties.getString("name"));
                                        addAddressOne(
                                                address,
                                                properties.getString("name"),
                                                "",
                                                properties.getString("settlement"),
                                                longitude,
                                                latitude);
                                    }
                                } else if (citySearch.equals("FC")) {
                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    if (properties.has("zone")) {
                                        address = String.format("%s %s (%s)\f",
                                                properties.getString("type"),
                                                properties.getString("name"),
                                                properties.getString("zone"));

                                        addAddressOne(
                                                address,
                                                properties.getString("name"),
                                                "",
                                                properties.getString("settlement"),
                                                longitude,
                                                latitude);
                                    } else {
                                        address = String.format("%s %s\f",
                                                properties.getString("type"),
                                                properties.getString("name"));
                                        addAddressOne(
                                                address,
                                                properties.getString("name"),
                                                "",
                                                properties.getString("settlement"),
                                                longitude,
                                                latitude);
                                    }
                                }
                                // Проверка по Киевской области
                                if (citySearch.equals("Київ") || citySearch.equals("Киев")) {
                                    if (checkWordInArray(properties.getString("settlement"), kyivRegionArr)) {
                                        address = String.format("%s %s (%s)\f",
                                                properties.getString("type"),
                                                properties.getString("name"),
                                                properties.getString("settlement"));

                                        double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                        double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

                                        addAddressOne(
                                                address,
                                                properties.getString("name"),
                                                "",
                                                properties.getString("settlement"),
                                                longitude,
                                                latitude);
                                    }
                                }
                                break;
                            case "adr_address":
                                settlement = properties.optString("settlement", "").toLowerCase();
                                city = citySearch.toLowerCase();

                                if (settlement.contains(city)) {
                                    Logger.d(getApplicationContext(), TAG, "processAddressData: properties ййй" + properties);
                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    if (properties.has("zone")) {
                                        // Получение элементов отдельно

                                        Logger.d(getApplicationContext(), TAG, "processAddressData: zone" + zone);


                                            address = String.format("%s %s %s %s %s %s\t",

                                                    properties.getString("street_type"),
                                                    properties.getString("street"),
                                                    properties.getString("name"),
                                                    properties.getString("zone"),
                                                    properties.getString("settlement_type"),
                                                    properties.getString("settlement"));
                                            addAddressOne(
                                                    address,
                                                    "",
                                                    "",
                                                    "",
                                                    longitude,
                                                    latitude);


                                    } else {
                                        address = String.format("%s %s %s %s %s\t",

                                                properties.getString("street_type"),
                                                properties.getString("street"),
                                                properties.getString("name"),
                                                properties.getString("settlement_type"),
                                                properties.getString("settlement"));
                                        addAddressOne(
                                                address,
                                                "",
                                                "",
                                                "",
                                                longitude,
                                                latitude);
                                    }

                                } else if (citySearch.equals("FC")) {
                                    Logger.d(getApplicationContext(), TAG, "processAddressData: properties ййй 222" + properties);
                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    if (properties.has("zone")) {
                                        // Получение элементов отдельно

                                        Logger.d(getApplicationContext(), TAG, "processAddressData: zone" + zone);

                                        address = String.format("%s %s %s %s %s %s\t",
                                                properties.getString("street_type"),
                                                properties.getString("street"),
                                                properties.getString("name"),
                                                properties.getString("zone"),
                                                properties.getString("settlement_type"),
                                                properties.getString("settlement"));
                                        addAddressOne(
                                                address,
                                                "",
                                                "",
                                                "",
                                                longitude,
                                                latitude);



                                    } else {
                                        address = String.format("%s %s %s %s %s\t",

                                                properties.getString("street_type"),
                                                properties.getString("street"),
                                                properties.getString("name"),
                                                properties.getString("settlement_type"),
                                                properties.getString("settlement"));
                                        addAddressOne(
                                                address,
                                                "",
                                                "",
                                                "",
                                                longitude,
                                                latitude);
                                    }

                                }
                                // Проверка по Киевской области

                                if (citySearch.equals("Київ") || citySearch.equals("Киев")) {
                                    Logger.d(getApplicationContext(), TAG, "processAddressData:citySearch " + citySearch);
                                    if (checkWordInArray(properties.getString("settlement"), kyivRegionArr)) {
                                        address = String.format("%s %s %s %s %s\t",
                                                properties.getString("street_type"),
                                                properties.getString("street"),
                                                properties.getString("name"),
                                                properties.getString("settlement_type"),
                                                properties.getString("settlement"));

                                        Logger.d(getApplicationContext(), TAG, "processAddressData: address" + address);
                                        double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                        double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

//                                        coordinatesList.add(new double[]{longitude, latitude});
                                        addAddressOne(
                                                address,
                                                "",
                                                "",
                                                "",
                                                longitude,
                                                latitude);
                                    }
                                }
                                break;
                            case "poi_railway_station":
                            case "poi_bus_station":
                            case "poi_airport_terminal":
                            case "poi_post_office":
                            case "poi_airport":
                                settlement = properties.optString("address", "").toLowerCase();
                                city = citySearch.toLowerCase();

                                if (settlement.contains(city)) {
                                    Logger.d(getApplicationContext(), TAG, "poi_railway_station" + properties);
                                    address = String.format("%s %s\t",
                                            properties.getString("vitrine"),
                                            properties.getString("address"));

                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    Logger.d(getApplicationContext(), TAG, "processAddressData: latitude longitude" + latitude + " " + longitude);

                                    addAddressOne(
                                            address,
                                            "",
                                            "",
                                            "",
                                            longitude,
                                            latitude);
                                } else if (citySearch.equals("FC")) {
                                    Logger.d(getApplicationContext(), TAG, "poi_railway_station" + properties);
                                    address = String.format("%s %s\t",
                                            properties.getString("vitrine"),
                                            properties.getString("address"));

                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    Logger.d(getApplicationContext(), TAG, "processAddressData: latitude longitude" + latitude + " " + longitude);

                                    addAddressOne(
                                            address,
                                            "",
                                            "",
                                            "",
                                            longitude,
                                            latitude);
                                }

                            default:
                                settlement = properties.optString("address", "").toLowerCase();
                                city = citySearch.toLowerCase();

                                if (settlement.contains(city)) {
                                    address = String.format("%s %s\t",
                                            properties.getString("vitrine"),
                                            properties.getString("address"));

                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    addAddressOne(
                                            address,
                                            "",
                                            "",
                                            "",
                                            longitude,
                                            latitude);
                                } else if (citySearch.equals("FC")) {
                                    address = String.format("%s %s\t",
                                            properties.getString("vitrine"),
                                            properties.getString("address"));

                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    addAddressOne(
                                            address,
                                            "",
                                            "",
                                            "",
                                            longitude,
                                            latitude);
                                }
                                break;
                        }
                    }
                }
            } else {
                addresses = new ArrayList<>();
                coordinatesList = new ArrayList<>();

                if(jsonResponse.length() == 0) {
                    extraExit = true;
                } else {
                    JSONObject properties = jsonResponse.getJSONObject("properties");
                    JSONObject geoCentroid = jsonResponse.getJSONObject("geo_centroid");

                    if (properties.getString("country_code").equals("ua")) {

                        if (properties.getString("categories").equals("adr_street")) {

                            String settlement = properties.optString("settlement", "").toLowerCase();
                            String city = citySearch.toLowerCase();
                            String address;

                            if (settlement.contains(city)) {
                                double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

                                if (properties.has("zone")) {
                                    address = String.format("%s %s (%s)\f",
                                            properties.getString("type"),
                                            properties.getString("name"),
                                            properties.getString("zone"));
                                    addAddressOne(
                                            address,
                                            properties.getString("name"),
                                            properties.getString("zone"),
                                            properties.getString("settlement"),
                                            longitude,
                                            latitude);
                                } else {
                                    address = String.format("%s %s\f",
                                            properties.getString("type"),
                                            properties.getString("name"));
                                    addAddressOne(
                                            address,
                                            properties.getString("name"),
                                            "",
                                            properties.getString("settlement"),
                                            longitude,
                                            latitude);
                                }
                            }

                            // Проверка по Киевской области

                            if (citySearch.equals("Київ") || citySearch.equals("Киев")) {
                                if (checkWordInArray(properties.getString("settlement"), kyivRegionArr)) {
                                    address = String.format("%s %s (%s)\f",
                                            properties.getString("type"),
                                            properties.getString("name"),
                                            properties.getString("settlement"));

                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

                                    addAddressOne(
                                            address,
                                            properties.getString("name"),
                                            "",
                                            properties.getString("settlement"),
                                            longitude,
                                            latitude);
                                }
                            }
                        }
                        if (properties.getString("categories").equals("adr_address")) {
                            String settlement = properties.optString("settlement", "").toLowerCase();
                            String city = citySearch.toLowerCase();
                            String address;

                            if (settlement.contains(city)) {

                                double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

                                if (properties.has("zone")) {
                                    // Получение элементов отдельно

                                    Logger.d(getApplicationContext(), TAG, "processAddressData: zone" + zone);
                                    if (properties.getString("zone").equals(zone)) {
                                        address = String.format("%s %s %s %s %s %s\t",

                                                properties.getString("street_type"),
                                                properties.getString("street"),
                                                properties.getString("name"),
                                                properties.getString("zone"),
                                                properties.getString("settlement_type"),
                                                properties.getString("settlement"));
                                        addAddressOne(
                                                address,
                                                "",
                                                "",
                                                "",
                                                longitude,
                                                latitude);
                                    }

                                } else {
                                    address = String.format("%s %s %s, %s %s\t",

                                            properties.getString("street_type"),
                                            properties.getString("street"),
                                            properties.getString("name"),
                                            properties.getString("settlement_type"),
                                            properties.getString("settlement"));
                                    addAddressOne(
                                            address,
                                            "",
                                            "",
                                            "",
                                            longitude,
                                            latitude);
                                }


                                Logger.d(getApplicationContext(), TAG, "processAddressData: latitude longitude" + latitude + " " + longitude);
                            }
                            // Проверка по Киевской области

                            if (citySearch.equals("Київ") || citySearch.equals("Киев")) {
                                Logger.d(getApplicationContext(), TAG, "processAddressData:citySearch " + citySearch);
                                if (checkWordInArray(properties.getString("settlement"), kyivRegionArr)) {
                                    address = String.format("%s %s %s %s %s\t",
                                            properties.getString("street_type"),
                                            properties.getString("street"),
                                            properties.getString("name"),
                                            properties.getString("settlement_type"),
                                            properties.getString("settlement"));

                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

                                    addAddressOne(
                                            address,
                                            "",
                                            "",
                                            "",
                                            longitude,
                                            latitude);
                                }
                            }
                        }
                    }
                }

            }
            String newAddress = getString(R.string.address_on_map);

            boolean isAddressExists = false;
            for (String[] address : addresses) {
                if (address.length > 0 && address[0].equals(newAddress)) {
                    isAddressExists = true;
                    break;
                }
            }

            if (!isAddressExists) {
                addresses.add(new String[]{newAddress, "", "", ""});
            }


        } catch (JSONException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        Logger.d(getApplicationContext(), TAG, "processAddressData: 44444444");
        if (!addresses.isEmpty()) {

            new Handler(Looper.getMainLooper()).post(() -> {
                addressesList = new ArrayList<>();
                List<String> nameList = new ArrayList<>();
                List<String> zoneList = new ArrayList<>();
                List<String> settlementList = new ArrayList<>();

                for (String[] addressArray : addresses) {
                    if (addressArray != null) {
                        // Выбираем значение 'address' из массива и добавляем его в addressesList
                        addressesList.add(addressArray[0]);
                        nameList.add(addressArray[1]);
                        zoneList.add(addressArray[2]);
                        settlementList.add(addressArray[3]);
                    }
                }

                addressAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_list_item, addressesList);


                if(addressesList.size() == 1) {
                    scrollButtonDown.setVisibility(View.GONE);
                    scrollButtonUp.setVisibility(View.GONE);
                    if (start.equals("ok")) {
                        String textEdit = fromEditAddress.getText().toString();
                        Logger.d(this, TAG, "textEdit" + textEdit);
                        if(textEdit.length() >= max_length_string_size) {
                            if (textEdit.contains("\f")) {
                                textGeoError.setVisibility(View.VISIBLE);
                                textGeoError.setText(R.string.no_house_vis_mes);
                                layoutParams.height = desiredHeight/3;
                                addressListView.setLayoutParams(layoutParams);
                                scrollButtonDown.setVisibility(View.GONE);
                                scrollButtonUp.setVisibility(View.GONE);

                                btnCallAdmin.setVisibility(View.VISIBLE);
                            } else {
                                textGeoError.setVisibility(View.VISIBLE);
                                textGeoError.setText(R.string.no_adrees_mes);
                                scrollButtonDown.setVisibility(View.GONE);
                                scrollButtonUp.setVisibility(View.GONE);
                                layoutParams.height = desiredHeight/3;
                                addressListView.setLayoutParams(layoutParams);
                                btnCallAdmin.setVisibility(View.VISIBLE);
                            }


                        }
                    } else {
                        String textEdit = toEditAddress.getText().toString();
                        if (textEdit.length() >= max_length_string_size) {
                            if (textEdit.contains("\f")) {
                                text_toError.setVisibility(View.VISIBLE);
                                text_toError.setText(R.string.no_house_vis_mes);
                                layoutParams.height = desiredHeight/3;
                                addressListView.setLayoutParams(layoutParams);
                                scrollButtonDown.setVisibility(View.GONE);
                                scrollButtonUp.setVisibility(View.GONE);

                                btnCallAdmin.setVisibility(View.VISIBLE);
                            } else {

                                scrollButtonDown.setVisibility(View.GONE);
                                scrollButtonUp.setVisibility(View.GONE);
                                text_toError.setVisibility(View.VISIBLE);
                                text_toError.setText(R.string.no_adrees_mes);
                                layoutParams.height = desiredHeight/3;
                                addressListView.setLayoutParams(layoutParams);
                                btnCallAdmin.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                }
                addressListView.setVisibility(View.VISIBLE);
                
                addressListView.setAdapter(addressAdapter);


                addressListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                addressListView.setItemChecked(0, true);

                addressListView.setOnItemClickListener((parent, viewC, position, id) -> {
                    Logger.d(getApplicationContext(), TAG, "processAddressData:position3333 " + position);
                    positionChecked = position;
                    startMarker = "ok";
                    finishMarker = "no";
                    if (point.equals("start")) {
                        fromEditAddress.requestFocus();
                        fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
                        KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
                        messageInfo = getString(R.string.drag_marker_bottom);


                    } else if (point.equals("finish")) {
                        toEditAddress.requestFocus();
                        toEditAddress.setSelection(toEditAddress.getText().toString().length());
                        KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);
                        messageInfo = getString(R.string.two_point_mes);
                        startMarker = "no";
                        finishMarker = "ok";
                    }

                    if (position == addressesList.size() - 1) {
                        Intent intent = new Intent(getApplicationContext(), OpenStreetMapVisicomActivity.class);

                        intent.putExtra("startMarker", startMarker);
                        intent.putExtra("finishMarker", finishMarker);

                        startActivity(intent);
                        finish();
                    } else {
                        double[] coordinates = coordinatesList.get(position);

                        if (point.equals("start")) {
                            Logger.d(getApplicationContext(), TAG, "processAddressData:coordinates " + coordinates.toString());
                            startPoint = addressesList.get(position);
                            fromEditAddress.setText(startPoint);
                            fromEditAddress.setSelection(startPoint.length());
//
                            List<String> settings = new ArrayList<>();

                            settings.add(Double.toString(coordinates[1]));
                            settings.add(Double.toString(coordinates[0]));
                            Logger.d(getApplicationContext(), TAG, "processAddressData:settings ddd " + settings);

                            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            @SuppressLint("Recycle") Cursor cursor = database.rawQuery(query, null);

                            cursor.moveToFirst();

                            // Получите значения полей из первой записи

                            @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                            @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                            String finish = cursor.getString(cursor.getColumnIndex("finish"));

                            cursor.close();
                            database.close();
                            Logger.d(getApplicationContext(), TAG, "processAddressData:settings finish " + finish);


                            if(finish.equals(getString(R.string.on_city_tv))) {
                                settings.add(Double.toString(coordinates[1]));
                                settings.add(Double.toString(coordinates[0]));
                                settings.add(addressesList.get(position));
                                settings.add(addressesList.get(position));
                            } else {


                                settings.add(String.valueOf(toLatitude));
                                settings.add(String.valueOf(toLongitude));
                                settings.add(addressesList.get(position));
                                settings.add(finish);
                            }
                            Logger.d(getApplicationContext(), TAG, "processAddressData:settings " + settings);
                            updateRoutMarker(settings);
                            updateMyPosition(coordinates[1], coordinates[0], startPoint, getApplicationContext());
                            VisicomFragment.geoText.setText(startPoint);
                            Logger.d(getApplicationContext(), TAG, "processAddressData: startPoint 1" + startPoint);
                            if(startPoint.contains("\t")) {
                                btn_ok.performClick();
                            } else {
                                textGeoError.setVisibility(View.VISIBLE);
                                textGeoError.setText(R.string.house_vis_mes);
                                scrollButtonDown.setVisibility(View.GONE);
                                scrollButtonUp.setVisibility(View.GONE);
                            }

                        }
                        else if (point.equals("finish")) {
                            finishPoint = addressesList.get(position);
                            toEditAddress.setText(finishPoint);
                            toEditAddress.setSelection(finishPoint.length());
                            btn_clear_to.setVisibility(View.INVISIBLE);

                            verifyRoutFinish = true;
                            List<String> settings = new ArrayList<>();

                            VisicomFragment.textViewTo.setText(addressesList.get(position));

                            Logger.d(getApplicationContext(), TAG, "processAddressData: ");
//                                            if (!toEditAddress.getText().toString().equals("")) {
                            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            Cursor cursor = database.rawQuery(query, null);

                            cursor.moveToFirst();

                            // Получите значения полей из первой записи

                            double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
                            double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));

                            cursor.close();
                            database.close();

                            settings.add(Double.toString(originLatitude));
                            settings.add(Double.toString(originLongitude));
                            settings.add(Double.toString(coordinates[1]));
                            settings.add(Double.toString(coordinates[0]));
                            Logger.d(getApplicationContext(), TAG, "processAddressData:fromEditAddress.getText().toString() " + fromEditAddress.getText().toString());

                            settings.add(VisicomFragment.geoText.getText().toString());
                            settings.add(addressesList.get(position));
                            updateRoutMarker(settings);
//                                            }


                            Logger.d(getApplicationContext(), TAG, "settings: " + settings);
                            toEditAddress.setSelection(addressesList.get(position).length());
                            if(addressesList.get(position).contains("\t")) {
                                btn_ok.performClick();
                            } else {
                                text_toError.setVisibility(View.VISIBLE);
                                text_toError.setText(R.string.house_vis_mes);
                                scrollButtonDown.setVisibility(View.GONE);
                                scrollButtonUp.setVisibility(View.GONE);
                            }

                        }
                    }

                    addressListView.setVisibility(View.INVISIBLE);
                    Logger.d(getApplicationContext(), TAG, "processAddressData:222222 " + addressesList.get(position));
                });

                scrollSetVisibility();
            });
        }

    }

    private boolean checkWordInArray(String wordToCheck, String[] searchArr) {

        boolean result = false;
        for (String word : searchArr) {
            if (word.equals(wordToCheck)) {
                // Слово найдено в массиве
                result = true;
                break;
            }
        }
        Logger.d(getApplicationContext(), TAG, "checkWordInArray: result" + result);
        return result;
    }

    private static String[] removeTextInParentheses(String inputText) {
        // Поиск индекса открывающей и закрывающей скобок
        int startIndex = inputText.indexOf('(');
        int endIndex = inputText.indexOf(')');

        // Если обе скобки найдены и закрывающая скобка идет после открывающей
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            // Получение текста в скобках
            String removedValueInParentheses = inputText.substring(startIndex + 1, endIndex);

            // Удаление текста в круглых скобках из исходной строки
            String result = inputText.substring(0, startIndex) + inputText.substring(endIndex + 1);

            // Возвращение результатов в виде массива
            return new String[]{result.trim(), removedValueInParentheses.trim()};
        } else {
            // Если скобки не найдены, вернуть исходную строку
            return new String[]{inputText.trim(), ""};
        }
    }

    private String numbers(String inputString) {


        // Регулярное выражение для поиска чисел после запятой и пробела
        String regex = ".*,\\s*([0-9]+).*";

        // Создание Pattern и Matcher
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputString);
        String numbersAfterComma = null;

        // Поиск соответствия
        if (matcher.matches()) {
            // Получение чисел после запятой и пробела
            numbersAfterComma = matcher.group(1);

            // Вывод чисел
            Logger.d(getApplicationContext(), TAG, "numbers: " + numbersAfterComma);

        }
        return numbersAfterComma;
    }
    private void updateRoutMarker(List<String> settings) {
        Logger.d(getApplicationContext(), TAG, "updateRoutMarker: " + settings.toString());
        ContentValues cv = new ContentValues();

        cv.put("startLat", Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));

        // обновляем по id
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[]{"1"});
        database.close();
    }

    @SuppressLint("Range")
    private List<String> logCursor(String table) {
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
        assert c != null;
        c.close();
        return list;
    }

    private void oldAddresses(String point) {
        addresses = new ArrayList<>();
        coordinatesList = new ArrayList<>(); // Список для хранения координат

        SQLiteDatabase db = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = db.query(MainActivity.TABLE_ORDERS_INFO, null, null, null, null, null, null);

        if (c != null) {
            if (c.moveToFirst()) {
                Set<String> uniqueAddressesSet = new HashSet<>();

                do {
                    // Получаем данные из курсора
                    String fromStreet = c.getString(c.getColumnIndexOrThrow("from_street"));
                    fromStreet = fromStreet.trim();
                    String toStreet = c.getString(c.getColumnIndexOrThrow("to_street"));
                    toStreet = toStreet.trim();
                    // Проверяем, есть ли уже такая запись в множестве
                    if (uniqueAddressesSet.add(fromStreet)) {
                        addresses.add(new String[]{fromStreet + "\t"});
                        double fromLongitude = Double.parseDouble(c.getString(c.getColumnIndexOrThrow("from_lng")));
                        double fromLatitude = Double.parseDouble(c.getString(c.getColumnIndexOrThrow("from_lat")));
                        coordinatesList.add(new double[]{fromLongitude, fromLatitude});
                    }
                    Logger.d(getApplicationContext(), TAG, "oldAddresses: toStreet " + toStreet);
                    // Проверяем, есть ли уже такая запись в множестве
                     if (!toStreet.equals("по місту") && !toStreet.equals("по городу")) {

                         if (uniqueAddressesSet.add(toStreet) && !toStreet.equals(fromStreet)) {
                             addresses.add(new String[]{toStreet + "\t"});
                             double toLongitude = Double.parseDouble(c.getString(c.getColumnIndexOrThrow("to_lng")));
                             double toLatitude = Double.parseDouble(c.getString(c.getColumnIndexOrThrow("to_lat")));
                             coordinatesList.add(new double[]{toLongitude, toLatitude});
                         }
                     }
                } while (c.moveToNext());
            }
        }

        //                    btn_ok.setVisibility(View.VISIBLE);
        db.close();
        assert c != null;
        c.close();
        addresses.add(new String[]{
                getString(R.string.address_on_map),
                "",
                "",
                "",
        });
         addressesList = new ArrayList<>();
        for (String[] addressArray : addresses) {
            // Выбираем значение 'address' из массива и добавляем его в addressesList
            addressesList.add(addressArray[0]);
        }
        Logger.d(getApplicationContext(), TAG, "onCreate: " + addressesList);
        addressAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_list_item, addressesList);
        
        addressListView.setAdapter(addressAdapter);

        addressListView.setVisibility(View.VISIBLE);


        addressListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        addressListView.setItemChecked(0, true);
        addressListView.setOnItemClickListener((parent, viewC, position, id) -> {
            positionChecked = position;
            startMarker = "ok";
            finishMarker = "no";
            if (point.equals("start")) {
                fromEditAddress.requestFocus();
                fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
                KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
                messageInfo = getString(R.string.drag_marker_bottom);


            } else if (point.equals("finish")) {
                toEditAddress.requestFocus();
                toEditAddress.setSelection(toEditAddress.getText().toString().length());
                KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);
                messageInfo = getString(R.string.two_point_mes);
                startMarker = "no";
                finishMarker = "ok";
            }

            if (position == addressesList.size() - 1) {
                Intent intent = new Intent(getApplicationContext(), OpenStreetMapVisicomActivity.class);

                intent.putExtra("startMarker", startMarker);
                intent.putExtra("finishMarker", finishMarker);

                startActivity(intent);
                finish();
            } else {
                double[] coordinates = coordinatesList.get(position);

                if (point.equals("start")) {
                    startPoint = addressesList.get(position);
                    fromEditAddress.setText(startPoint);
                    fromEditAddress.setSelection(startPoint.length());

                    if (!verifyBuildingStart) {
                        verifyRoutStart = true;
                        List<String> settings = new ArrayList<>();


                        settings.add(Double.toString(coordinates[1]));
                        settings.add(Double.toString(coordinates[0]));

                        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        @SuppressLint("Recycle") Cursor cursor = database.rawQuery(query, null);

                        cursor.moveToFirst();

                        // Получите значения полей из первой записи

                        @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                        @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                        @SuppressLint("Range") String finish = cursor.getString(cursor.getColumnIndex("finish"));

                        cursor.close();
                        database.close();
                        Logger.d(getApplicationContext(), TAG, "processAddressData:settings finish " + finish);

                        if(finish.equals(getString(R.string.on_city_tv))) {
                            settings.add(Double.toString(coordinates[1]));
                            settings.add(Double.toString(coordinates[0]));
                            settings.add(addressesList.get(position));
                            settings.add(addressesList.get(position));
                        } else {
                            settings.add(String.valueOf(toLatitude));
                            settings.add(String.valueOf(toLongitude));
                            settings.add(addressesList.get(position));
                            settings.add(finish);
                        }
                        updateRoutMarker(settings);
                        updateMyPosition(coordinates[1], coordinates[0], startPoint, getApplicationContext());
                        VisicomFragment.geoText.setText(startPoint);
                        Logger.d(getApplicationContext(), TAG, "processAddressData: startPoint 2" + startPoint);
                        if(startPoint.contains("\t")) {
                            btn_ok.performClick();
                        }
                    }
                } else if (point.equals("finish")) {
                    finishPoint = addressesList.get(position);
                    toEditAddress.setText(finishPoint);
                    toEditAddress.setSelection(finishPoint.length());
                    btn_clear_to.setVisibility(View.INVISIBLE);
                    if (!verifyBuildingFinish) {
                        verifyRoutFinish = true;
                        List<String> settings = new ArrayList<>();

                        VisicomFragment.textViewTo.setText(addressesList.get(position));
                        Logger.d(getApplicationContext(), TAG, "oldAddresses: " + addressesList.get(position));

                        Logger.d(getApplicationContext(), TAG, "oldAddresses:2222 "+ VisicomFragment.geoText.getText().toString());
                        if (!VisicomFragment.geoText.getText().toString().equals("")) {
                            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            Cursor cursor = database.rawQuery(query, null);

                            cursor.moveToFirst();

                            // Получите значения полей из первой записи

                            @SuppressLint("Range") double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
                            @SuppressLint("Range") double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));

                            cursor.close();
                            database.close();

                            settings.add(Double.toString(originLatitude));
                            settings.add(Double.toString(originLongitude));
                            settings.add(Double.toString(coordinates[1]));
                            settings.add(Double.toString(coordinates[0]));

                            settings.add(VisicomFragment.geoText.getText().toString());
                            settings.add(addressesList.get(position));
                            updateRoutMarker(settings);
                        }


                        Logger.d(getApplicationContext(), TAG, "settings: " + settings);
                        toEditAddress.setSelection(addressesList.get(position).length());
                        if(toEditAddress.getText().toString().contains("\t")) {
                            btn_ok.performClick();
                        }
                    }
                }
            }

            addressListView.setVisibility(View.INVISIBLE);
            scrollSetVisibility();
        });
    }

    private void addAddressOne (
            String newAddress1,
            String newAddress2,
            String newAddress3,
            String newAddress4,
            double longitude,
            double latitude
    ) {
        boolean isAddressExists = false;
        for (String[] address : addresses) {
            if (address.length > 0 && address[0].equals(newAddress1)) {
                isAddressExists = true;
                break;
            }
        }

        if (!isAddressExists) {
            addresses.add(new String[]{newAddress1, newAddress2, newAddress3, newAddress4});
            coordinatesList.add(new double[]{longitude, latitude});
        }
    }

    public interface ConnectionSpeedTestCallback {
        void onConnectionTestResult(boolean isConnectionFast, long duration);
    }

    public void testConnectionTime(String baseUrl, String apiKey, long timeLimitMillis, ConnectionSpeedTestCallback callback) {
        final long[] connectionTime = {0};  // Переменная для хранения времени подключения

        long startTime = System.currentTimeMillis();
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        // Убедимся, что baseUrl заканчивается символом /
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        ConnectionSpeedTester.testConnectionSpeed(baseUrl, apiKey, new ConnectionSpeedTester.SpeedTestListener() {
            @Override
            public void onSpeedTestCompleted(double speed) {
                long endTime = System.currentTimeMillis();
                connectionTime[0] = endTime - startTime;

                Logger.d(getApplicationContext(), TAG, "Скорость подключения: " + speed + " байт/мс");
                Logger.d(getApplicationContext(), TAG, "Скорость подключения: " + connectionTime[0]);

                // Здесь вы можете обновить ваш интерфейс или выполнить другие действия

                // Передаем результаты обратно через callback
                boolean isConnectionFast = connectionTime[0] >= 0 && connectionTime[0] <= timeLimitMillis;
                callback.onConnectionTestResult(isConnectionFast, connectionTime[0]);
            }

            @Override
            public void onSpeedTestFailed(String errorMessage) {
                Logger.d(getApplicationContext(), TAG, errorMessage);
                // Обработка ошибок, например, вывод сообщения пользователю
                connectionTime[0] = -1;  // Помечаем время подключения как ошибочное

                // Передаем результаты обратно через callback
                callback.onConnectionTestResult(false, connectionTime[0]);
            }
        });
    }

    private void mapBoxSearch(String address, String point) {
        // Создаем Retrofit-клиент
        MapboxService mapboxService = MapboxApiClient.create();

        Call<MapboxResponse> call = mapboxService.getLocation(address, MainActivity.apiKeyMapBox);
        call.enqueue(new Callback<MapboxResponse>() {
            @Override
            public void onResponse(Call<MapboxResponse> call, Response<MapboxResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Обработка успешного ответа
                    MapboxResponse mapboxResponse = response.body();
                    processAddressDataMapBox(mapboxResponse, point);

                } else {
                    // Обработка ошибки
                    Logger.d(getApplicationContext(), TAG, "Error: " + response.code() + " " + response.message());
//                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
//                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MapboxResponse> call, @NonNull Throwable t) {
                // Обработка ошибки при выполнении запроса
                FirebaseCrashlytics.getInstance().recordException(t);
//                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
//                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });

    }

    private void processAddressDataMapBox(MapboxResponse mapboxResponse, String point) {
        addresses = new ArrayList<>();
        coordinatesList = new ArrayList<>();

        if (mapboxResponse != null && mapboxResponse.getFeatures() != null
                && !mapboxResponse.getFeatures().isEmpty()) {
            for (Feature feature : mapboxResponse.getFeatures()) {
                Geometry geometry = feature.getGeometry();
                List<Double> coordinates = geometry.getCoordinates();
                double longitude = coordinates.get(0);
                double latitude = coordinates.get(1);

                addAddressOne(
                        feature.getPlaceName() + "\t",
                        "",
                        "",
                        "",
                        longitude,
                        latitude);
            }

        } else {
            Logger.d(getApplicationContext(), TAG, "No results found.");
        }
        String newAddress = getString(R.string.address_on_map);

        boolean isAddressExists = false;
        for (String[] address : addresses) {
            if (address.length > 0 && address[0].equals(newAddress)) {
                isAddressExists = true;
                break;
            }
        }
        if (!isAddressExists) {
            addresses.add(new String[]{newAddress, "", "", ""});
        }

        if (addresses.size() != 0) {
            new Handler(Looper.getMainLooper()).post(() -> {
                 addressesList = new ArrayList<>();
                List<String> nameList = new ArrayList<>();
                List<String> zoneList = new ArrayList<>();
                List<String> settlementList = new ArrayList<>();

                for (String[] addressArray : addresses) {
                    // Выбираем значение 'address' из массива и добавляем его в addressesList
                    addressesList.add(addressArray[0]);
                    nameList.add(addressArray[1]);
                    zoneList.add(addressArray[2]);
                    settlementList.add(addressArray[3]);
                }

                addressAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_list_item, addressesList);

                
                addressListView.setAdapter(addressAdapter);
                addressListView.setVisibility(View.VISIBLE);

                addressListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                addressListView.setItemChecked(0, true);

                addressListView.setOnItemClickListener((parent, viewC, position, id) -> {

                    positionChecked = position;
                    startMarker = "ok";
                    finishMarker = "no";
                    if (point.equals("start")) {
                        fromEditAddress.requestFocus();
                        fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
                        KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
                        messageInfo = getString(R.string.drag_marker_bottom);


                    } else if (point.equals("finish")) {
                        toEditAddress.requestFocus();
                        toEditAddress.setSelection(toEditAddress.getText().toString().length());
                        KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);
                        messageInfo = getString(R.string.two_point_mes);
                        startMarker = "no";
                        finishMarker = "ok";
                    }

                    if (position == addressesList.size() - 1) {
                        Intent intent = new Intent(getApplicationContext(), OpenStreetMapVisicomActivity.class);

                        intent.putExtra("startMarker", startMarker);
                        intent.putExtra("finishMarker", finishMarker);

                        startActivity(intent);
                        finish();
                    } else {
                        double[] coordinates = coordinatesList.get(position);

                        if (point.equals("start")) {
                            startPoint = addressesList.get(position);
                            fromEditAddress.setText(startPoint);
                            fromEditAddress.setSelection(startPoint.length());
                            if (fromEditAddress.getText().toString().contains("\t")) {
                                verifyRoutStart = true;
                                verifyBuildingStart = false;
                            }
                            if (fromEditAddress.getText().toString().contains("\f")) {
                                verifyRoutStart = false;
                                verifyBuildingStart = true;
                            }
                            if (!verifyBuildingStart) {
                                verifyRoutStart = true;
                                List<String> settings = new ArrayList<>();

                                settings.add(Double.toString(coordinates[1]));
                                settings.add(Double.toString(coordinates[0]));

                                String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                                SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                                @SuppressLint("Recycle") Cursor cursor = database.rawQuery(query, null);

                                cursor.moveToFirst();

                                // Получите значения полей из первой записи

                                @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                                @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                                @SuppressLint("Range") String finish = cursor.getString(cursor.getColumnIndex("finish"));

                                cursor.close();
                                database.close();
                                Logger.d(getApplicationContext(), TAG, "processAddressData:settings finish " + finish);


                                if(finish.equals(getString(R.string.on_city_tv))) {
                                    settings.add(Double.toString(coordinates[1]));
                                    settings.add(Double.toString(coordinates[0]));
                                    settings.add(addressesList.get(position));
                                    settings.add(addressesList.get(position));
                                } else {
                                    settings.add(String.valueOf(toLatitude));
                                    settings.add(String.valueOf(toLongitude));
                                    settings.add(addressesList.get(position));
                                    settings.add(finish);
                                }
                                updateRoutMarker(settings);
                                updateMyPosition(coordinates[1], coordinates[0], startPoint, getApplicationContext());
                                VisicomFragment.geoText.setText(startPoint);
                                Logger.d(getApplicationContext(), TAG, "processAddressData: startPoint 3" + startPoint);
                                if(startPoint.contains("\t")) {

                                        Intent intent = new Intent(getApplicationContext(), ActivityVisicomOnePage.class);
                                        intent.putExtra("start", "no");
                                        intent.putExtra("end", "ok");
                                        startActivity(intent);

                                }
                            }
                        } else if (point.equals("finish")) {
                            finishPoint = addressesList.get(position);
                            toEditAddress.setText(finishPoint);
                            toEditAddress.setSelection(finishPoint.length());
                            btn_clear_to.setVisibility(View.INVISIBLE);

                            verifyRoutFinish = true;
                            List<String> settings = new ArrayList<>();

                            VisicomFragment.textViewTo.setText(addressesList.get(position));

                            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            @SuppressLint("Recycle") Cursor cursor = database.rawQuery(query, null);

                            cursor.moveToFirst();

                            // Получите значения полей из первой записи

                            @SuppressLint("Range") double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
                            @SuppressLint("Range") double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
                            @SuppressLint("Range") String finish = cursor.getString(cursor.getColumnIndex("finish"));

                            cursor.close();
                            database.close();
                            Logger.d(getApplicationContext(), TAG, "processAddressData:settings finish " + finish);


                            if(!finish.equals(getString(R.string.on_city_tv))) {
                                settings.add(Double.toString(originLatitude));
                                settings.add(Double.toString(originLongitude));
                                settings.add(Double.toString(coordinates[1]));
                                settings.add(Double.toString(coordinates[0]));

                                settings.add(fromEditAddress.getText().toString());
                                settings.add(addressesList.get(position));
                                updateRoutMarker(settings);
                            }


                            Logger.d(getApplicationContext(), TAG, "settings: " + settings);
                            toEditAddress.setSelection(addressesList.get(position).length());
                            if(addressesList.get(position).contains("\t")) {
                                btn_ok.performClick();
                            }

                        }
                    }

                    addressListView.setVisibility(View.INVISIBLE);
                });
                //                    btn_ok.setVisibility(View.VISIBLE);

            });
        }
    }

    private void visicomKey() {
        com.taxi.easy.ua.ui.visicom.visicom_search.key_visicom.ApiClient.getVisicomKeyInfo(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
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
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(getApplicationContext(),"visicomKey", "Failed to make API call" + t);
            }
        },getString(R.string.application)
        );
    }
    private void mapboxKey() {
        ApiClientMapbox.getMapboxKeyInfo(new Callback<ApiResponseMapbox>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponseMapbox> call, @NonNull Response<ApiResponseMapbox> response) {
                if (response.isSuccessful() && response.body() != null) {
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
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(getApplicationContext(),"ApiResponseMapbox", "Failed to make API call" + t);
            }
        }, getString(R.string.application)
        );
    }
}


