package com.taxi.easy.ua.ui.open_map.visicom;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.cities.Kyiv.KyivRegion;
import com.taxi.easy.ua.cities.Kyiv.KyivRegionRu;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapVisicomActivity;
import com.taxi.easy.ua.ui.open_map.mapbox.Feature;
import com.taxi.easy.ua.ui.open_map.mapbox.Geometry;
import com.taxi.easy.ua.ui.open_map.mapbox.MapboxApiClient;
import com.taxi.easy.ua.ui.open_map.mapbox.MapboxResponse;
import com.taxi.easy.ua.ui.open_map.mapbox.MapboxService;
import com.taxi.easy.ua.ui.open_map.visicom.key_mapbox.ApiCallbackMapbox;
import com.taxi.easy.ua.ui.open_map.visicom.key_mapbox.ApiClientMapbox;
import com.taxi.easy.ua.ui.open_map.visicom.key_mapbox.ApiResponseMapbox;
import com.taxi.easy.ua.ui.open_map.visicom.key_visicom.ApiCallback;
import com.taxi.easy.ua.ui.open_map.visicom.key_visicom.ApiClient;
import com.taxi.easy.ua.ui.open_map.visicom.key_visicom.ApiResponse;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.KeyboardUtils;
import com.taxi.easy.ua.utils.LocaleHelper;
import com.taxi.easy.ua.utils.connect.ConnectionSpeedTester;
import com.taxi.easy.ua.utils.ip.OnIPAddressReceivedListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ActivityVisicomOnePage extends AppCompatActivity
        implements ApiCallback, ApiCallbackMapbox, OnIPAddressReceivedListener {

    private static final String TAG = "TAG_VIS_ADDR";

    AppCompatButton  btn_change, btnOnMap;

    ProgressBar progressBar;
    EditText fromEditAddress, toEditAddress;
    private ImageButton btn_clear_from, btn_clear_to, btn_ok, btn_no;
    private String apiKey;
    private static List<double[]> coordinatesList;
    private static List<String[]> addresses;
    private final OkHttpClient client = new OkHttpClient();
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
    private static String apiKeyMapBox;
    private boolean extraExit;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visicom_address_layout);

        start = getIntent().getStringExtra("start");
        end = getIntent().getStringExtra("end");


        List<String> stringList = logCursor(MainActivity.CITY_INFO);
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
                        citySearch = "Запорож";
                        break;
                    case "Cherkasy Oblast":
                        citySearch = "Черкассы";
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
                        citySearch = "Дніпр";
                        break;
                    case "Odessa":
                    case "OdessaTest":
                        citySearch = "Одеса";
                        break;
                    case "Zaporizhzhia":
                        citySearch = "Запорі";
                        break;
                    case "Cherkasy Oblast":
                        citySearch = "Черкас";
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

        btn_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               finish();
            }
        });

        fromEditAddress = findViewById(R.id.textGeo);

        int inputType = fromEditAddress.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        fromEditAddress.setInputType(inputType);


        toEditAddress = findViewById(R.id.text_to);
        toEditAddress.setText(VisicomFragment.textViewTo.getText().toString());
        inputType = toEditAddress.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        toEditAddress.setInputType(inputType);

        btn_clear_from = findViewById(R.id.btn_clear_from);
        btn_clear_from.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addresses = new ArrayList<>();
                coordinatesList = new ArrayList<>();

                List<String> addressesList = new ArrayList<>();
                addressAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_list_item, addressesList);
                addressListView.setAdapter(addressAdapter);

                fromEditAddress.setText("");
                btn_clear_from.setVisibility(View.INVISIBLE);
                textGeoError.setVisibility(View.GONE);
                Log.d(TAG, "onClick: wwww");
                fromEditAddress.requestFocus();
                KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
            }
        });
        btn_clear_to = findViewById(R.id.btn_clear_to);
        btn_clear_to.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addresses = new ArrayList<>();
                coordinatesList = new ArrayList<>();
                List<String> addressesList = new ArrayList<>();
                addressAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_list_item, addressesList);

                addressListView.setAdapter(addressAdapter);

                toEditAddress.setText("");
                btn_clear_to.setVisibility(View.INVISIBLE);
                text_toError.setVisibility(View.GONE);
                toEditAddress.requestFocus();
                KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);

            }
        });

        verifyRoutStart = true;
        verifyRoutFinish = true;
        btn_change = findViewById(R.id.change);
        btn_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                boolean gps_enabled = false;
                boolean network_enabled = false;

                try {
                    gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                } catch(Exception ignored) {
                }

                try {
                    network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                } catch(Exception ignored) {
                }

                if(!gps_enabled || !network_enabled) {
                    MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment();
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }  else  {
                    // Разрешения уже предоставлены, выполнить ваш код
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                    }  else {
                        firstLocation();
                    }
                }
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
            fromEditAddress.setVisibility(View.GONE);
            btn_clear_from.setVisibility(View.GONE);
            textGeoError.setVisibility(View.GONE);
            btn_change.setVisibility(View.GONE);
            findViewById(R.id.textfrom).setVisibility(View.GONE);
            findViewById(R.id.num1).setVisibility(View.GONE);
        }
        btnOnMap = findViewById(R.id.btn_on_map);
        btnOnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), OpenStreetMapVisicomActivity.class);
               
                intent.putExtra("startMarker", start);
                intent.putExtra("finishMarker", end);

                startActivity(intent);
                finish();
            }
        });
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(extraExit) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    }, 200);
                } else {
                    if(start.equals("ok")){
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(fromEditAddress.getWindowToken(), 0);

                    }
                    if(end.equals("ok")) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(toEditAddress.getWindowToken(), 0);
                    }
                    if(start.equals("ok")) {
                        verifyRoutStart = false;
                        verifyBuildingStart = false;

                        if (fromEditAddress.getText().toString().contains("\t")) {
                            verifyRoutStart = true;
                            verifyBuildingStart = true;
                        } else  {
                            verifyBuildingStart = false;
                        }
                        if (!verifyBuildingStart) {
                            textGeoError.setVisibility(View.VISIBLE);
                            textGeoError.setText(R.string.house_vis_mes);

                            fromEditAddress.requestFocus();
                            fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
                            KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
                        } else if (!verifyRoutStart) {
                            textGeoError.setVisibility(View.VISIBLE);
                            textGeoError.setText(R.string.rout_fin);

                            fromEditAddress.requestFocus();
                            fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
                            KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
                        }
                        if (verifyRoutStart && verifyBuildingStart) {
                            finish();
                        }
                    }
                    if(end.equals("ok")) {
                        verifyRoutFinish = false;
                        verifyRoutFinish = false;

                        if (toEditAddress.getText().toString().equals(getString(R.string.on_city_tv))) {
                            verifyRoutFinish = true;
                            verifyBuildingFinish = true;
                        } else {
                            if (toEditAddress.getText().toString().contains("\t")) {
                                verifyRoutFinish = true;
                                verifyBuildingFinish = true;
                            }
                            if (toEditAddress.getText().toString().contains("\f")) {
                                verifyBuildingFinish = false;
                            }
                        }
                        if (!verifyBuildingFinish) {
                            text_toError.setVisibility(View.VISIBLE);
                            text_toError.setText(R.string.house_vis_mes);

                            toEditAddress.requestFocus();
                            toEditAddress.setSelection(toEditAddress.getText().toString().length());
                            KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);
                        }else if (!verifyRoutFinish) {
                            text_toError.setVisibility(View.VISIBLE);
                            text_toError.setText(R.string.rout_fin);

                            toEditAddress.requestFocus();
                            toEditAddress.setSelection(toEditAddress.getText().toString().length());
                            KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);
                        }

                        if (verifyRoutFinish && verifyBuildingFinish) {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            }, 200);

                        }
                }



                }

            }
        });
        if(start.equals("ok")){
            fromEditAddress.requestFocus();
            fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
            KeyboardUtils.showKeyboard(getApplicationContext(), fromEditAddress);
        }
        if(end.equals("ok")) {
            toEditAddress.requestFocus();
            toEditAddress.setSelection(toEditAddress.getText().toString().length());
            KeyboardUtils.showKeyboard(getApplicationContext(), toEditAddress);
        }
    }




    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("Range")
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Log.d(TAG, "getTaxiUrlSearchMarkers: " + urlAPI);

        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery(query, null);

        cursor.moveToFirst();

        // Получите значения полей из первой записи

        double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
        double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
        double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
        double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
        String start = cursor.getString(cursor.getColumnIndex("start"));
        String finish = cursor.getString(cursor.getColumnIndex("finish"));
        Log.d(TAG, "getTaxiUrlSearchMarkers: start " + start);
        // Заменяем символ '/' в строках
        start = start.replace("/", "|");
        finish = finish.replace("/", "|");

        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;

        cursor.close();


        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO);
        String time = stringList.get(1);
        String comment = stringList.get(2);
        String date = stringList.get(3);

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO);
        String tarif =  stringListInfo.get(2);
        String payment_type = stringListInfo.get(4);
        String addCost = stringListInfo.get(5);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO).get(4);

        if(urlAPI.equals("costSearchMarkers")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type;
        }
        if(urlAPI.equals("orderSearchMarkersVisicom")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO).get(2);


            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type + "/" + addCost + "/"
                    + time + "/" + comment + "/" + date+ "/" + start + "/" + finish;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });

        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i < services.size()-1 ; i++) {
            if(services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if(servicesVer) {
            for (int i = 0; i < OpenStreetMapActivity.arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(OpenStreetMapActivity.arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if(servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Log.d(TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "https://m.easy-order-taxi.site/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);

        database.close();

        return url;
    }
    private void firstLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        locationCallback = new LocationCallback() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Обработка полученных местоположений
                stopLocationUpdates();

                // Обработка полученных местоположений
                List<Location> locations = locationResult.getLocations();
                Log.d(TAG, "onLocationResult: locations 222222" + locations);

                if (!locations.isEmpty()) {
                    Location firstLocation = locations.get(0);

                    double latitude = firstLocation.getLatitude();
                    double longitude = firstLocation.getLongitude();


                        List<String> stringList = logCursor(MainActivity.CITY_INFO);
                        String api =  stringList.get(2);
                        String urlFrom = "https://m.easy-order-taxi.site/" + api + "/android/fromSearchGeo/" + latitude + "/" + longitude;
                        Map sendUrlFrom = null;
                        try {
                            sendUrlFrom = FromJSONParser.sendURL(urlFrom);

                        } catch (MalformedURLException | InterruptedException |
                                 JSONException e) {
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                        }
                        assert sendUrlFrom != null;
                        String FromAdressString = (String) sendUrlFrom.get("route_address_from");
                        if (FromAdressString != null) {
                            if (FromAdressString.equals("Точка на карте")) {
                                FromAdressString = getString(R.string.startPoint);
                            }
                        }
                        updateMyPosition(latitude, longitude, FromAdressString, getApplicationContext());
                        fromEditAddress.setText(FromAdressString);
                    assert FromAdressString != null;
                    if (FromAdressString != null) {
                        fromEditAddress.setSelection(FromAdressString.length());
                    }

                    btn_clear_from.setVisibility(View.VISIBLE);
                        VisicomFragment.geoText.setText(FromAdressString);

                        List<String> settings = new ArrayList<>();
                        String ToAdressString = toEditAddress.getText().toString();
                        if(ToAdressString.equals(getString(R.string.on_city_tv)) ||
                                ToAdressString.equals("") ) {
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

                        btn_ok.setVisibility(View.VISIBLE);
                    }



            }
        };
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }

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
                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000); // Интервал обновления местоположения в миллисекундах
        locationRequest.setFastestInterval(100); // Самый быстрый интервал обновления местоположения в миллисекундах
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Приоритет точного местоположения
        return locationRequest;
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Показываем объяснение пользователю, почему мы запрашиваем разрешение
            // Можно использовать диалоговое окно или другой пользовательский интерфейс
            MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (MainActivity.countryState != null) {
            if (!MainActivity.countryState.equals("UA")) {
                mapboxKey(this);
            } else {
                visicomKey(this);
            }
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
                String inputString = charSequence.toString();
                int charCount = inputString.length();
                Log.d(TAG, "onTextChanged: " + inputString);
                if (charCount > 2) {
                    Log.d(TAG, "onTextChanged:startPoint " + startPoint);
                    Log.d(TAG, "onTextChanged:fromEditAddress.getText().toString() " + fromEditAddress.getText().toString());
                    Log.d(TAG, "onTextChanged:MainActivity.countryState " + MainActivity.countryState);
                    if (startPoint == null) {
                        if(MainActivity.countryState.equals("UA")) {
                            performAddressSearch(inputString, "start");
                        } else {
                            mapBoxSearch(inputString, "start");
                        }
                    } else if (!startPoint.equals(inputString)) {
                        if(MainActivity.countryState.equals("UA")) {
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
                // Вызывается при изменении текста
                String inputString = charSequence.toString();
                int charCount = inputString.length();

                if (charCount > 2) {

                    if (finishPoint == null) {
                        if(MainActivity.countryState.equals("UA")) {
                            performAddressSearch(inputString, "finish");
                        } else {
                            mapBoxSearch(inputString, "finish");
                        }
                    } else if (!finishPoint.equals(inputString)) {
                        if(MainActivity.countryState.equals("UA")) {
                            performAddressSearch(inputString, "finish");
                        } else {
                            mapBoxSearch(inputString, "finish");
                        }
                    }
                }

                btn_clear_to.setVisibility(View.VISIBLE);
                text_toError.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // Вызывается после изменения текста
            }
        });

        if (fromEditAddress.getText().toString().equals("")) {

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

//        testConnectionTime("https://api.visicom.ua/data-api/5.0/uk/", apiKey, 1000, new ConnectionSpeedTestCallback() {
//            @Override
//            public void onConnectionTestResult(boolean isConnectionFast, long duration) {
//                Log.d("SpeedTest", "connectionTime: " + duration);
//                Log.d("SpeedTest", "testConnectionTime: timeLimitMillis: 1000");
//                Log.d("SpeedTest", "testConnectionTime: res: " + isConnectionFast);
//                if (!isConnectionFast) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Log.d(TAG, "Скорость подключения превышает ограничение времени или произошла ошибка");
//                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
//                            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
//                        }
//                    });
//                }
//            }
//        });
        try {
            String apiUrl = "https://api.visicom.ua/data-api/5.0/";
            String url = apiUrl  + LocaleHelper.getLocale() + "/geocode.json";


            if (point.equals("start")) {
                verifyBuildingStart = false;
            } else {
                verifyBuildingFinish = false;
            }
            String modifiedText = "";
            Log.d(TAG, "performAddressSearch:modifiedText " + modifiedText);
            if (!inputText.substring(3).contains("\f")) {
                modifiedText = inputText.replaceAll("[\f\t]", " ");
                url = url
                        + "?"
                        + "categories=poi_railway_station"
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
                        + ",poi_underground_railway_station"
                        + ",adr_street"
                        + "&l=20"
                        + "&text=" + modifiedText + "&key=" + apiKey;

            } else {
                Log.d(TAG, "performAddressSearch:positionChecked  " + positionChecked);
                String number = numbers(modifiedText);

                if (positionChecked != 0) {
                    inputText = inputTextBuild() + ", " + number;
                }
                modifiedText = inputText.replaceAll("[\f\t]", " ");
                url = url + "?categories=adr_address&text=" + modifiedText
                        + "&l=20" + "&key=" + apiKey;

            }

            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Log.d(TAG, "performAddressSearch: " + url);
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) {
                    try {
                        String responseData = response.body().string();
                        Log.d(TAG, "onResponse: " + responseData);
                        processAddressData(responseData, point);
                    } catch (Exception e) {
                        e.printStackTrace();
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    }
                }

                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }



    }

    private String inputTextBuild() {
        Log.d(TAG, "inputTextBuild: " + positionChecked);
        String result = "";
        if(positionChecked != 0){
            String[] selectedAddress = addresses.get(positionChecked);
            Log.d(TAG, "inputTextBuild: " + Arrays.toString(selectedAddress));
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
            Log.d(TAG, "processAddressData:jsonResponse " + jsonResponse);

            if (jsonResponse.has("features")) {
                addresses = new ArrayList<>();
                coordinatesList = new ArrayList<>(); // Список для хранения координат
                JSONArray features = jsonResponse.getJSONArray("features");

                Log.d(TAG, "processAddressData: features" + features.length());

                // В массиве есть элементы, обрабатываем результат
                // Ваши дополнительные действия с features
                for (int i = 0; i < features.length(); i++) {
                    JSONObject properties = features.getJSONObject(i).getJSONObject("properties");
//                    Log.d(TAG, "processAddressData:properties " + i + " - " + properties);
                    JSONObject geoCentroid = features.getJSONObject(i).getJSONObject("geo_centroid");

                    if (properties.getString("country_code").equals("ua")) {
                        switch (properties.getString("categories")) {
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
                                    Log.d(TAG, "processAddressData: properties ййй" + properties);
                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    if (properties.has("zone")) {
                                        // Получение элементов отдельно

                                        Log.d(TAG, "processAddressData: zone" + zone);


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
                                    Log.d(TAG, "processAddressData: properties ййй 222" + properties);
                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    if (properties.has("zone")) {
                                        // Получение элементов отдельно

                                        Log.d(TAG, "processAddressData: zone" + zone);

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
                                    Log.d(TAG, "processAddressData:citySearch " + citySearch);
                                    if (checkWordInArray(properties.getString("settlement"), kyivRegionArr)) {
                                        address = String.format("%s %s %s %s %s\t",
                                                properties.getString("street_type"),
                                                properties.getString("street"),
                                                properties.getString("name"),
                                                properties.getString("settlement_type"),
                                                properties.getString("settlement"));

                                        Log.d(TAG, "processAddressData: address" + address);
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
                            case "poi_airport":
                                settlement = properties.optString("address", "").toLowerCase();
                                city = citySearch.toLowerCase();

                                if (settlement.contains(city)) {
                                    Log.d(TAG, "poi_railway_station" + properties);
                                    address = String.format("%s %s\t",
                                            properties.getString("vitrine"),
                                            properties.getString("address"));

                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    Log.d(TAG, "processAddressData: latitude longitude" + latitude + " " + longitude);

                                    addAddressOne(
                                            address,
                                            "",
                                            "",
                                            "",
                                            longitude,
                                            latitude);
                                } else if (citySearch.equals("FC")) {
                                    Log.d(TAG, "poi_railway_station" + properties);
                                    address = String.format("%s %s\t",
                                            properties.getString("vitrine"),
                                            properties.getString("address"));

                                    double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                                    double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                                    Log.d(TAG, "processAddressData: latitude longitude" + latitude + " " + longitude);

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

                    List<String> stringList = logCursor(MainActivity.CITY_INFO);
                    String city = getString(R.string.foreign_countries);
                    switch (stringList.get(1)) {
                        case "Kyiv City":
                            city = getString(R.string.Kyiv_city);
                            break;
                        case "Dnipropetrovsk Oblast":
                            break;
                        case "Odessa":
                        case "OdessaTest":
                            city = getString(R.string.Odessa);
                            break;
                        case "Zaporizhzhia":
                            city = getString(R.string.Zaporizhzhia);
                            break;
                        case "Cherkasy Oblast":
                            city = getString(R.string.Cherkasy);
                            break;
                        default:
                            city = getString(R.string.foreign_countries);
                            break;
                    }
                    String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                    SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    Cursor cursor = database.rawQuery(query, null);

                    cursor.moveToFirst();

                    // Получите значения полей из первой записи


                    @SuppressLint("Range") double startLat = cursor.getDouble(cursor.getColumnIndex("startLat"));
                    @SuppressLint("Range") double startLan = cursor.getDouble(cursor.getColumnIndex("startLan"));
                    @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                    @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                    @SuppressLint("Range") String start = cursor.getString(cursor.getColumnIndex("start"));
                    @SuppressLint("Range") String finish = cursor.getString(cursor.getColumnIndex("finish"));
                    cursor.close();
                    database.close();
                    List<String> settings = new ArrayList<>();
                    settings.add(String.valueOf(startLat));
                    settings.add(String.valueOf(startLat));
                    settings.add(String.valueOf(toLatitude));
                    settings.add(String.valueOf(toLongitude));

                    if (!point.equals("finish")) {
                        String startPoint = fromEditAddress.getText().toString().replaceAll("[\\d\\s]+$", "")+ ", " + getString(R.string.city_loc) + " " + city;
                        Log.d(TAG, "processAddressData:startPoint " + startPoint);
                        settings.add(startPoint);
                        settings.add(finish);
                        VisicomFragment.geoText.setText(startPoint);
                    } else  {
                        String toPoint = toEditAddress.getText().toString().replaceAll("[\\d\\s]+$", "") + ", " + getString(R.string.city_loc) + " " + city;
                        Log.d(TAG, "processAddressData:startPoint " + toPoint);
                        settings.add(start);
                        settings.add(toPoint);
                        VisicomFragment.geoText.setText(toPoint);
                    }

                    updateRoutMarker(settings);
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

                                    Log.d(TAG, "processAddressData: zone" + zone);
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


                                Log.d(TAG, "processAddressData: latitude longitude" + latitude + " " + longitude);
                            }
                            // Проверка по Киевской области

                            if (citySearch.equals("Київ") || citySearch.equals("Киев")) {
                                Log.d(TAG, "processAddressData:citySearch " + citySearch);
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

            if (addresses.size() != 0) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    List<String> addressesList = new ArrayList<>();
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


                    addressListView.setVisibility(View.VISIBLE);

                    addressListView.setAdapter(addressAdapter);
                    addressListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    addressListView.setItemChecked(0, true);

                    addressListView.setOnItemClickListener((parent, viewC, position, id) -> {
                        Log.d(TAG, "processAddressData:position3333 " + position);
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
                                Log.d(TAG, "processAddressData:coordinates " + coordinates.toString());
                                startPoint = addressesList.get(position);
                                fromEditAddress.setText(startPoint);
                                fromEditAddress.setSelection(startPoint.length());
//
                                    List<String> settings = new ArrayList<>();

                                    settings.add(Double.toString(coordinates[1]));
                                    settings.add(Double.toString(coordinates[0]));
                                     Log.d(TAG, "processAddressData:settings ddd " + settings.toString());
                                    if (toEditAddress.getText().toString().equals(getString(R.string.on_city_tv))) {
                                        settings.add(Double.toString(coordinates[1]));
                                        settings.add(Double.toString(coordinates[0]));
                                        settings.add(addressesList.get(position));
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

                                        settings.add(String.valueOf(toLatitude));
                                        settings.add(String.valueOf(toLongitude));
                                        settings.add(addressesList.get(position));
                                        settings.add(toEditAddress.getText().toString());
                                    }
                                        Log.d(TAG, "processAddressData:settings " + settings);
                                    updateRoutMarker(settings);
                                    updateMyPosition(coordinates[1], coordinates[0], startPoint, getApplicationContext());
                                    VisicomFragment.geoText.setText(startPoint);
                                    Log.d(TAG, "processAddressData: startPoint " + startPoint);

                                }
                             else if (point.equals("finish")) {
                                finishPoint = addressesList.get(position);
                                toEditAddress.setText(finishPoint);
                                toEditAddress.setSelection(finishPoint.length());
                                btn_clear_to.setVisibility(View.VISIBLE);

                                    verifyRoutFinish = true;
                                    List<String> settings = new ArrayList<>();

                                            VisicomFragment.textViewTo.setText(addressesList.get(position));
                                            VisicomFragment.btn_clear_to.setVisibility(View.VISIBLE);
                                Log.d(TAG, "processAddressData: ");
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
                                                Log.d(TAG, "processAddressData:fromEditAddress.getText().toString() " + fromEditAddress.getText().toString());

                                                settings.add(VisicomFragment.geoText.getText().toString());
                                                settings.add(addressesList.get(position));
                                                updateRoutMarker(settings);
//                                            }


                                    Log.d(TAG, "settings: " + settings);
                                    toEditAddress.setSelection(addressesList.get(position).length());


                            }
                        }

                        addressListView.setVisibility(View.INVISIBLE);
                    });
                    btn_ok.setVisibility(View.VISIBLE);

                });
            }



        } catch (JSONException e) {
            e.printStackTrace();
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
        Log.d(TAG, "checkWordInArray: result" + result);
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
            Log.d(TAG, "numbers: " + numbersAfterComma);

        }
        return numbersAfterComma;
    }
    private void updateRoutMarker(List<String> settings) {
        Log.d(TAG, "updateRoutMarker: " + settings.toString());
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
                    Log.d(TAG, "oldAddresses: toStreet " + toStreet);
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

        btn_ok.setVisibility(View.VISIBLE);
        db.close();
        assert c != null;
        c.close();
        addresses.add(new String[]{
                getString(R.string.address_on_map),
                "",
                "",
                "",
        });
        List<String> addressesList = new ArrayList<>();
        for (String[] addressArray : addresses) {
            // Выбираем значение 'address' из массива и добавляем его в addressesList
            addressesList.add(addressArray[0]);
        }
        Log.d(TAG, "onCreate: " + addressesList.toString());
        addressAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.custom_list_item, addressesList);
        addressListView.setVisibility(View.VISIBLE);
        addressListView.setAdapter(addressAdapter);
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
                        if (toEditAddress.getText().toString().equals(getString(R.string.on_city_tv))) {
                            settings.add(Double.toString(coordinates[1]));
                            settings.add(Double.toString(coordinates[0]));
                            settings.add(addressesList.get(position));
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

                            settings.add(String.valueOf(toLatitude));
                            settings.add(String.valueOf(toLongitude));
                            settings.add(addressesList.get(position));
                            settings.add(toEditAddress.getText().toString());
                        }
                        updateRoutMarker(settings);
                        updateMyPosition(coordinates[1], coordinates[0], startPoint, getApplicationContext());
                        VisicomFragment.geoText.setText(startPoint);
                        Log.d(TAG, "processAddressData: startPoint " + startPoint);
                    }
                } else if (point.equals("finish")) {
                    finishPoint = addressesList.get(position);
                    toEditAddress.setText(finishPoint);
                    toEditAddress.setSelection(finishPoint.length());
                    btn_clear_to.setVisibility(View.VISIBLE);
                    if (!verifyBuildingFinish) {
                        verifyRoutFinish = true;
                        List<String> settings = new ArrayList<>();

                        VisicomFragment.textViewTo.setText(addressesList.get(position));
                        Log.d(TAG, "oldAddresses: " + addressesList.get(position));
                        VisicomFragment.btn_clear_to.setVisibility(View.VISIBLE);
                        Log.d(TAG, "oldAddresses:2222 "+ VisicomFragment.geoText.getText().toString());
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


                        Log.d(TAG, "settings: " + settings);
                        toEditAddress.setSelection(addressesList.get(position).length());

                    }
                }
            }

            addressListView.setVisibility(View.INVISIBLE);
        });
    }
    private void mapboxKey(final ApiCallbackMapbox callback) {
        ApiClientMapbox.getMapboxKeyInfo(new Callback<ApiResponseMapbox>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponseMapbox> call, @NonNull Response<ApiResponseMapbox> response) {
                if (response.isSuccessful()) {
                    ApiResponseMapbox apiResponse = response.body();
                    if (apiResponse != null) {
                        String keyMaxbox = apiResponse.getKeyMapbox();
                        Log.d("ApiResponseMapbox", "keyMapbox: " + keyMaxbox);

                        // Теперь у вас есть ключ Visicom для дальнейшего использования
                        callback.onMapboxKeyReceived(keyMaxbox);
                    }
                } else {
                    // Обработка ошибки
                    Log.e("ApiResponseMapbox", "Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponseMapbox> call, @NonNull Throwable t) {
                // Обработка ошибки
                Log.e("ApiResponseMapbox", "Failed to make API call", t);
            }
        },
        getString(R.string.application)
        );
    }
    @Override
    public void onMapboxKeyReceived(String key) {
        Log.d(TAG, "onMapboxKeyReceived: " + key);
        apiKeyMapBox = key;
    }


    private void visicomKey(final ApiCallback callback) {
        ApiClient.getVisicomKeyInfo(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse != null) {
                        String keyVisicom = apiResponse.getKeyVisicom();
                        Log.d("ApiResponse", "keyVisicom: " + keyVisicom);

                        // Теперь у вас есть ключ Visicom для дальнейшего использования
                        callback.onVisicomKeyReceived(keyVisicom);
                    }
                } else {
                    // Обработка ошибки
                    Log.e("ApiResponseMapbox", "Error: " + response.code());
                    callback.onApiError(response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                // Обработка ошибки
                Log.e("ApiResponseMapbox", "Failed to make API call", t);
                callback.onApiFailure(t);
            }
        },
            getString(R.string.application)
        );
    }
    @Override
    public void onVisicomKeyReceived(String key) {
        Log.d(TAG, "onVisicomKeyReceived: " + key);
        apiKey = key;
    }

    @Override
    public void onApiError(int errorCode) {

    }

    @Override
    public void onApiFailure(Throwable t) {

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

    @Override
    public void onIPAddressReceived(String ipAddress) {
        if (ipAddress != null) {
            Log.d(TAG, "Global IP Address: " + ipAddress);
            // Вызываем AsyncTask для определения страны по IP-адресу

        } else {
            Log.e(TAG, "Failed to retrieve global IP Address");
        }
    }



    public interface ConnectionSpeedTestCallback {
        void onConnectionTestResult(boolean isConnectionFast, long duration);
    }

    public void testConnectionTime(String baseUrl, String apiKey, long timeLimitMillis, ConnectionSpeedTestCallback callback) {
        final long[] connectionTime = {0};  // Переменная для хранения времени подключения

        long startTime = System.currentTimeMillis();

        // Убедимся, что baseUrl заканчивается символом /
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        ConnectionSpeedTester.testConnectionSpeed(baseUrl, apiKey, new ConnectionSpeedTester.SpeedTestListener() {
            @Override
            public void onSpeedTestCompleted(double speed) {
                long endTime = System.currentTimeMillis();
                connectionTime[0] = endTime - startTime;

                Log.d("SpeedTest", "Скорость подключения: " + speed + " байт/мс");
                Log.d("SpeedTest", "Скорость подключения: " + connectionTime[0]);

                // Здесь вы можете обновить ваш интерфейс или выполнить другие действия

                // Передаем результаты обратно через callback
                boolean isConnectionFast = connectionTime[0] >= 0 && connectionTime[0] <= timeLimitMillis;
                callback.onConnectionTestResult(isConnectionFast, connectionTime[0]);
            }

            @Override
            public void onSpeedTestFailed(String errorMessage) {
                Log.e("SpeedTest", errorMessage);
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

        Call<MapboxResponse> call = mapboxService.getLocation(address, apiKeyMapBox);
        call.enqueue(new Callback<MapboxResponse>() {
            @Override
            public void onResponse(Call<MapboxResponse> call, Response<MapboxResponse> response) {
                if (response.isSuccessful()) {
                    // Обработка успешного ответа
                    MapboxResponse mapboxResponse = response.body();
                    processAddressDataMapBox(mapboxResponse, point);

                } else {
                    // Обработка ошибки
                    Log.d(TAG, "Error: " + response.code() + " " + response.message());
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MapboxResponse> call, @NonNull Throwable t) {
                // Обработка ошибки при выполнении запроса
                t.printStackTrace();
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
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
            Log.d(TAG, "No results found.");
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
                List<String> addressesList = new ArrayList<>();
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


                addressListView.setVisibility(View.VISIBLE);

                addressListView.setAdapter(addressAdapter);
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
                                if (toEditAddress.getText().toString().equals(getString(R.string.on_city_tv))) {
                                    settings.add(Double.toString(coordinates[1]));
                                    settings.add(Double.toString(coordinates[0]));
                                    settings.add(addressesList.get(position));
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

                                    settings.add(String.valueOf(toLatitude));
                                    settings.add(String.valueOf(toLongitude));
                                    settings.add(addressesList.get(position));
                                    settings.add(toEditAddress.getText().toString());
                                }
                                updateRoutMarker(settings);
                                updateMyPosition(coordinates[1], coordinates[0], startPoint, getApplicationContext());
                                VisicomFragment.geoText.setText(startPoint);
                                Log.d(TAG, "processAddressData: startPoint " + startPoint);

                            }
                        } else if (point.equals("finish")) {
                            finishPoint = addressesList.get(position);
                            toEditAddress.setText(finishPoint);
                            toEditAddress.setSelection(finishPoint.length());
                            btn_clear_to.setVisibility(View.VISIBLE);

                            verifyRoutFinish = true;
                            List<String> settings = new ArrayList<>();

                            VisicomFragment.textViewTo.setText(addressesList.get(position));
                            VisicomFragment.btn_clear_to.setVisibility(View.VISIBLE);
                            if (!toEditAddress.getText().toString().equals("")) {
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

                                settings.add(fromEditAddress.getText().toString());
                                settings.add(addressesList.get(position));
                                updateRoutMarker(settings);
                            }


                            Log.d(TAG, "settings: " + settings);
                            toEditAddress.setSelection(addressesList.get(position).length());


                        }
                    }

                    addressListView.setVisibility(View.INVISIBLE);
                });
                btn_ok.setVisibility(View.VISIBLE);

            });
        }
    }



}


