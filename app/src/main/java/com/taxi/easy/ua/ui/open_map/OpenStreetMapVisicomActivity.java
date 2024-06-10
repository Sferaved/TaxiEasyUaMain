package com.taxi.easy.ua.ui.open_map;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.NetworkChangeReceiver;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.open_map.api.ApiResponse;
import com.taxi.easy.ua.ui.open_map.api.ApiService;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;

import org.json.JSONException;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class OpenStreetMapVisicomActivity extends AppCompatActivity {
    private static final String TAG = "TAG_OPENMAP";
    public static IMapController mapController;
    private static final String BASE_URL = "https://m.easy-order-taxi.site/";
    private static ApiService apiService;

    public String[] arrayStreet;
    public static FloatingActionButton  fab_call, fab_open_map, fab_open_marker;
    public static ImageButton fab;

    public static double startLat, startLan, finishLat, finishLan;
    public static MapView map;
    public static String api;
    public static GeoPoint startPoint;
    public static GeoPoint endPoint;
    @SuppressLint({"StaticFieldLeak", "UseSwitchCompatOrMaterialCode"})
    static Switch gpsSwitch;
    public static Polyline roadOverlay;
    public static Marker m, marker;
    public static String FromAdressString, ToAdressString;
    LayoutInflater inflater;
    @SuppressLint("StaticFieldLeak")
    static View view;
    public static long addCost;

    public static FragmentManager fragmentManager;
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    @SuppressLint("StaticFieldLeak")

    private String city;

    private static String  startMarker;
    private static String finishMarker;
    private static Drawable originalDrawable;
    private static Drawable scaledDrawable;
    private static String startPointNoText;
    private static String endPointNoText;

    public static String[] arrayServiceCode() {
        return new String[]{
                "BAGGAGE",
                "ANIMAL",
                "CONDIT",
                "MEET",
                "COURIER",
                "CHECK_OUT",
                "BABY_SEAT",
                "DRIVER",
                "NO_SMOKE",
                "ENGLISH",
                "CABLE",
                "FUEL",
                "WIRES",
                "SMOKE",
        };
    }

    NetworkChangeReceiver networkChangeReceiver;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    public static MarkerOverlayVisicom markerOverlay;



    @SuppressLint({"MissingInflatedId", "InflateParams"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_street_map_layout);


        startPointNoText = getString(R.string.startPoint);
        endPointNoText = getString(R.string.end_point_marker);

        startMarker = getIntent().getStringExtra("startMarker");
        finishMarker = getIntent().getStringExtra("finishMarker");

        originalDrawable = getResources().getDrawable(R.drawable.marker_green);
        int width = 48;
        int height = 48;
        Bitmap bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) originalDrawable).getBitmap(), width, height, false);

        // Создайте новый Drawable из уменьшенного изображения
        scaledDrawable = new BitmapDrawable(getResources(), bitmap);

        new  VerifyUserTask(getApplicationContext()).execute();

        networkChangeReceiver = new NetworkChangeReceiver();

        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        fragmentManager = getSupportFragmentManager();

        inflater = getLayoutInflater();
        view = inflater.inflate(R.layout.phone_verify_layout, null);




        FromAdressString = getString(R.string.startPoint);
        ToAdressString = getString(R.string.end_point_marker);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.INVISIBLE);

        fab = findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);
        fab_call = findViewById(R.id.fab_call);
        fab_open_map = findViewById(R.id.fab_open_map);
        fab_open_map.setOnClickListener(v -> {
            Intent intent = new Intent(OpenStreetMapVisicomActivity.this, MainActivity.class);
            intent.putExtra("gps_upd", false);
            startActivity(intent);
        });
        fab_open_marker = findViewById(R.id.fab_open_marker);
        fab_open_marker.setVisibility(View.INVISIBLE);

        gpsSwitch = findViewById(R.id.gpsSwitch);

        gpsSwitch.setChecked(switchState());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gpsSwitch.setVisibility(View.INVISIBLE);
        }
        fab_call.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            List<String> stringList = logCursor(MainActivity.CITY_INFO, getApplicationContext());
            String phone = stringList.get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });


        gpsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            gpsSwitch.setChecked(switchState());
        });

    }

    private void switchToRegion() {

        List<String> stringList = logCursor(MainActivity.CITY_INFO, this);
        city = stringList.get(1);
        api =  stringList.get(2);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            Cursor cursor = database.rawQuery(query, null);

            cursor.moveToFirst();

            // Получите значения полей из первой записи

            @SuppressLint("Range") double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
            @SuppressLint("Range") double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
            @SuppressLint("Range") String start = cursor.getString(cursor.getColumnIndex("start"));

            cursor.close();
            database.close();
            FromAdressString = start;
            startPoint = new GeoPoint(originLatitude,originLongitude);

        }


        mapController = map.getController();
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        double newZoomLevel = Double.parseDouble(logCursor(MainActivity.TABLE_POSITION_INFO, getApplicationContext()).get(4));
        mapController.setZoom(newZoomLevel);
        map.setClickable(true);
        map.setTileSource(TileSourceFactory.MAPNIK);
//        map.setTileSource(TileSourceFactory.WIKIMEDIA);
//        map.setTileSource(TileSourceFactory.PUBLIC_TRANSPORT);
//        map.setTileSource(TileSourceFactory.CLOUDMADESTANDARDTILES);
//        map.setTileSource(TileSourceFactory.CLOUDMADESMALLTILES);
//        map.setTileSource(TileSourceFactory.FIETS_OVERLAY_NL);
//        map.setTileSource(TileSourceFactory.BASE_OVERLAY_NL);
//        map.setTileSource(TileSourceFactory.ROADS_OVERLAY_NL);
//        map.setTileSource(TileSourceFactory.HIKEBIKEMAP);
// *       map.setTileSource(TileSourceFactory.OPEN_SEAMAP);
//        map.setTileSource(TileSourceFactory.USGS_SAT);
//        map.setTileSource(TileSourceFactory.ChartbundleWAC);
//        map.setTileSource(TileSourceFactory.ChartbundleENRH);
//        map.setTileSource(TileSourceFactory.ChartbundleENRL);
//        map.setTileSource(TileSourceFactory.OpenTopo);
        map.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    // Обработка изменения масштаба
                    double newZoomLevel = map.getZoomLevelDouble();
                    Log.d(TAG, "Zoom level: " + newZoomLevel);
                    // Добавьте свой код обработки здесь
                    SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    ContentValues cv = new ContentValues();

                    cv.put("newZoomLevel", newZoomLevel);
                    database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                            new String[] { "1" });
                    database.close();
                }
                return false;
            }
        });

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }
    @Override
    protected void onRestart() {
        super.onRestart();
        finish();
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
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }
   private boolean  switchState() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ignored) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ignored) {}

       return gps_enabled && network_enabled;
    }

    public static void dialogMarkerStartPoint() throws MalformedURLException {
        Log.d(TAG, "dialogMarkerStartPoint: " + startPoint.toString());
        if(startPoint != null) {

            startLat = startPoint.getLatitude();
            startLan = startPoint.getLongitude();
            if(m != null) {
                map.getOverlays().remove(m);
                map.invalidate();
                m = null;
            }


            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Log.d(TAG, "Request URL: " + retrofit.baseUrl().toString());

            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    // Log the URL
                    Log.d(TAG, message);
                }
            });

            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)  // Set the client with logging interceptor
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);

            makeApiCall(startLat, startLan);
        }
    }

    public static void dialogMarkersEndPoint() throws MalformedURLException, JSONException, InterruptedException {  {
            if(endPoint != null) {


                finishLat = endPoint.getLatitude();
                finishLan = endPoint.getLongitude();
                if(marker != null) {
                    map.getOverlays().remove(marker);
                    map.invalidate();
                    marker = null;
                }



                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                Log.d(TAG, "Request URL: " + retrofit.baseUrl().toString());

                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(String message) {
                        // Log the URL
                        Log.d(TAG, message);
                    }
                });

                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient client = new OkHttpClient.Builder()
                        .addInterceptor(loggingInterceptor)
                        .build();

                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)  // Set the client with logging interceptor
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();

                apiService = retrofit.create(ApiService.class);

                makeApiCall(finishLat, finishLan);



            }
        }
    }

    public static void makeApiCall(double latitude, double longitude) {
        Locale locale = Locale.getDefault();
        String language = locale.getLanguage(); // Получаем язык устройства
        Call<ApiResponse> call = apiService.reverseAddressLocal(latitude, longitude, language);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse != null) {
                        String result = apiResponse.getResult();
                        if (map != null && map.getRepository() != null) {
                            if (startMarker.equals("ok")) {
                                if (!result.equals("404")) {
                                    FromAdressString = result;
                                } else {
                                    FromAdressString = startPointNoText;
                                }

                                if (map != null && map.getRepository() != null) {
                                    m = new Marker(map);
                                    m.setPosition(startPoint);
                                    m.setTextLabelBackgroundColor(
                                            Color.TRANSPARENT
                                    );
                                    m.setTextLabelForegroundColor(
                                            Color.RED
                                    );
                                    m.setTextLabelFontSize(40);
                                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                                    String unuString = new String(Character.toChars(0x1F449));

                                    m.setTitle("1." + unuString + FromAdressString);
                                    m.setIcon(scaledDrawable);
                                    m.showInfoWindow();

                                    map.getOverlays().add(m);

                                    map.getController().setCenter(startPoint);
                                    double newZoomLevel = Double.parseDouble(logCursor(MainActivity.TABLE_POSITION_INFO, fab.getContext()).get(4));
                                    mapController.setZoom(newZoomLevel);

                                    map.invalidate();
                                    List<String> settings = new ArrayList<>();

                                    if (VisicomFragment.textViewTo.getText().toString().equals(map.getContext().getString(R.string.on_city_tv))) {

                                        settings.add(String.valueOf(startLat));
                                        settings.add(String.valueOf(startLan));
                                        settings.add(String.valueOf(startLat));
                                        settings.add(String.valueOf(startLan));
                                        settings.add(FromAdressString);
                                        settings.add(map.getContext().getString(R.string.on_city_tv));
                                    } else {
                                        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                                        SQLiteDatabase database = map.getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                                        Cursor cursor = database.rawQuery(query, null);

                                        cursor.moveToFirst();
                                        // Получите значения полей из первой записи

                                        @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                                        @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                                        cursor.close();
                                        database.close();


                                        settings.add(String.valueOf(startLat));
                                        settings.add(String.valueOf(startLan));
                                        settings.add(String.valueOf(toLatitude));
                                        settings.add(String.valueOf(toLongitude));

                                        settings.add(FromAdressString);
                                        settings.add(VisicomFragment.textViewTo.getText().toString());
                                    }
                                    updateRoutMarker(settings, map.getContext());
                                    updateMyPosition(startLat, startLan, FromAdressString, map.getContext());
                                }

                            }
                            if (finishMarker.equals("ok")) {
                                if(!result.equals("Точка на карте")) {
                                    ToAdressString = result;
                                }


                                assert map != null;
                                marker = new Marker(map);
                                marker.setPosition(new GeoPoint(endPoint.getLatitude(), endPoint.getLongitude()));
                                marker.setTextLabelBackgroundColor(
                                        Color.TRANSPARENT
                                );
                                marker.setTextLabelForegroundColor(
                                        Color.RED
                                );
                                marker.setTextLabelFontSize(40);
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
                                String unuString = new String(Character.toChars(0x1F449));

                                marker.setTitle("2." + unuString + ToAdressString);
                                marker.setIcon(scaledDrawable);
                                marker.showInfoWindow();

                                map.getOverlays().add(marker);

                                GeoPoint initialGeoPoint = new GeoPoint(endPoint.getLatitude(), endPoint.getLongitude());
                                map.getController().setCenter(initialGeoPoint);
//                            double newZoomLevel = Double.parseDouble(logCursor(MainActivity.TABLE_POSITION_INFO, fab.getContext()).get(4));
//                            mapController.setZoom(newZoomLevel);

                                map.invalidate();
                                showRout(startPoint, endPoint);
                                List<String> settings = new ArrayList<>();

                                settings.add(String.valueOf(startLat));
                                settings.add(String.valueOf(startLan));
                                settings.add(String.valueOf(endPoint.getLatitude()));
                                settings.add(String.valueOf(endPoint.getLongitude()));
                                settings.add(FromAdressString);
                                settings.add(ToAdressString);

                                updateRoutMarker(settings, map.getContext());
                            }
                        }
                      }
                } else {
                    // Обработка неуспешного запроса
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                // Обработка ошибок
            }
        });
    }
    public void onResume() {
        super.onResume();
        map = findViewById(R.id.map);
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, getApplicationContext()).get(3);

        String application =  getString(R.string.application);
        new com.taxi.easy.ua.utils.VerifyUserTask(userEmail, application, getApplicationContext()).execute();

        switchToRegion();

        gpsSwitch.setChecked(switchState());
        Log.d(TAG, "onResume: startMarker" + startMarker);
        Log.d(TAG, "onResume: finishMarker" + finishMarker);
        Log.d(TAG, "onResume: getFromTablePositionInfo(this, \"startLat\" )" + getFromTablePositionInfo(this, "startLat" ));

        if (startMarker.equals("ok")) {
            markerOverlay = new MarkerOverlayVisicom(OpenStreetMapVisicomActivity.this, "startMarker");
        }
        if (finishMarker.equals("ok")) {
            markerOverlay = new MarkerOverlayVisicom(OpenStreetMapVisicomActivity.this, "finishMarker");
        }
        map.getOverlays().add(markerOverlay);

        if(getFromTablePositionInfo(this, "startLat" ) != 0 ){
            startLat = getFromTablePositionInfo(this, "startLat" );
            startLan = getFromTablePositionInfo(this, "startLan" );

            List<String> startList = logCursor(MainActivity.TABLE_POSITION_INFO, this);
            FromAdressString = startList.get(3);
            startPoint = new GeoPoint(startLat, startLan);
            map.getController().setCenter(startPoint);
            setMarker(startLat, startLan, FromAdressString, getApplicationContext());

            map.invalidate();

            List<String> settings = new ArrayList<>();
            // Проверяем, не являются ли textViewTo и map пустыми
            if (VisicomFragment.textViewTo != null && map != null && map.getContext() != null) {
                // Продолжаем выполнение кода
                if (VisicomFragment.textViewTo.getText().toString().equals(map.getContext().getString(R.string.on_city_tv))) {
                    settings.add(String.valueOf(startLat));
                    settings.add(String.valueOf(startLan));
                    settings.add(String.valueOf(startLat));
                    settings.add(String.valueOf(startLan));
                    settings.add(FromAdressString);
                    settings.add(map.getContext().getString(R.string.on_city_tv));
                } else  {
                    String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                    SQLiteDatabase database = map.getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    Cursor cursor = database.rawQuery(query, null);

                    cursor.moveToFirst();
                    // Получите значения полей из первой записи

                    @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                    @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                    cursor.close();
                    database.close();


                    settings.add(String.valueOf(startLat));
                    settings.add(String.valueOf(startLan));
                    settings.add(String.valueOf(toLatitude));
                    settings.add(String.valueOf(toLongitude));

                    settings.add(FromAdressString);
                    settings.add(VisicomFragment.textViewTo.getText().toString());
                }
                updateRoutMarker(settings, map.getContext());
                updateMyPosition(startLat, startLan, FromAdressString, map.getContext());
            } else {
                // Обработка ситуации, когда textViewTo или map не были инициализированы
                // Можете добавить здесь логирование или другие действия по вашему усмотрению
                String message = getString(R.string.error_message);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            }

       } else {
            Log.d(TAG, "onResume: " + ContextCompat.checkSelfPermission(OpenStreetMapVisicomActivity.this, Manifest.permission.ACCESS_FINE_LOCATION));
            if(ContextCompat.checkSelfPermission(OpenStreetMapVisicomActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                startLat = startPoint.getLatitude();
                startLan = startPoint.getLongitude();
                startPoint = new GeoPoint(startLat, startLan);
                map.getController().setCenter(startPoint);
                setMarker(startLat, startLan, FromAdressString, getApplicationContext());

                map.invalidate();

               List<String> settings = new ArrayList<>();

               if (VisicomFragment.textViewTo.getText().toString().equals(map.getContext().getString(R.string.on_city_tv))) {

                   settings.add(String.valueOf(startLat));
                   settings.add(String.valueOf(startLan));
                   settings.add(String.valueOf(startLat));
                   settings.add(String.valueOf(startLan));
                   settings.add(FromAdressString);
                   settings.add(map.getContext().getString(R.string.on_city_tv));
               } else  {
                   String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                   SQLiteDatabase database = map.getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                   Cursor cursor = database.rawQuery(query, null);

                   cursor.moveToFirst();
                   // Получите значения полей из первой записи

                   @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                   @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                   cursor.close();
                   database.close();


                   settings.add(String.valueOf(startLat));
                   settings.add(String.valueOf(startLan));
                   settings.add(String.valueOf(toLatitude));
                   settings.add(String.valueOf(toLongitude));

                   settings.add(FromAdressString);
                   settings.add(VisicomFragment.textViewTo.getText().toString());
               }
               updateRoutMarker(settings, map.getContext());
               updateMyPosition(startLat, startLan, FromAdressString, map.getContext());

            } else {

                Toast.makeText(this, R.string.check_position, Toast.LENGTH_SHORT).show();
                Configuration.getInstance().load(OpenStreetMapVisicomActivity.this, PreferenceManager.getDefaultSharedPreferences(OpenStreetMapVisicomActivity.this));

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

                locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    // Обработка полученных местоположений
                    stopLocationUpdates();

                    // Обработка полученных местоположений
                    List<Location> locations = locationResult.getLocations();
                    Log.d(TAG, "onLocationResult: locations 222222" + locations);

                    if (!locations.isEmpty()) {
                        Location firstLocation = locations.get(0);
                        if (startLat != firstLocation.getLatitude() && startLan != firstLocation.getLongitude()) {

                            double latitude = firstLocation.getLatitude();
                            double longitude = firstLocation.getLongitude();
                            startLat = latitude;
                            startLan = longitude;

                        }
                    }
                    Locale locale = Locale.getDefault();
                    String language = locale.getLanguage(); // Получаем язык устройства

                    String urlFrom = "https://m.easy-order-taxi.site/" + api + "/android/fromSearchGeoLocal/"  + startLat + "/" + startLan + "/" + language;

                    try {
                        FromJSONParser parser = new FromJSONParser(urlFrom);
                        Map<String, String> sendUrlFrom = parser.sendURL(urlFrom);
                        assert sendUrlFrom != null;
                        FromAdressString = (String) sendUrlFrom.get("route_address_from");
                        if (FromAdressString != null) {
                            if (FromAdressString.equals("Точка на карте")) {
                                FromAdressString = getString(R.string.startPoint);
                            }
                        }
                        updateMyPosition(startLat, startLan, FromAdressString, getApplicationContext());


                        map.getOverlays().add(markerOverlay);

                        startPoint = new GeoPoint(startLat, startLan);
                        map.getController().setCenter(startPoint);

                        setMarker(startLat, startLan, FromAdressString, getApplicationContext());
                        map.invalidate();
                    } catch (MalformedURLException | InterruptedException |
                             JSONException e) {
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    }

                }
            };

                    startLocationUpdates();

        }

            }
        map.onResume();
    }

   @Override
   protected void onPause() {
       super.onPause();
       map.onPause();
   }

    public static void setMarker(double Lat, double Lan, String title, Context context) {
        m = new Marker(map);
        m.setPosition(new GeoPoint(Lat, Lan));

        // Установите название маркера
        String unuString = new String(Character.toChars(0x1F449));
        m.setTitle("1." + unuString + title);

        m.setTextLabelBackgroundColor(Color.TRANSPARENT);
        m.setTextLabelForegroundColor(Color.RED);
        m.setTextLabelFontSize(40);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

        @SuppressLint("UseCompatLoadingForDrawables") Drawable originalDrawable = context.getResources().getDrawable(R.drawable.marker_green);

        // Уменьшите размер до 48 пикселей
        int width = 48;
        int height = 48;
        Bitmap bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) originalDrawable).getBitmap(), width, height, false);

        // Создайте новый Drawable из уменьшенного изображения
        Drawable scaledDrawable = new BitmapDrawable(context.getResources(), bitmap);
        m.setIcon(scaledDrawable);

        m.showInfoWindow();
        map.getOverlays().add(m);
        map.invalidate();
    }
    public static void showRout(GeoPoint startP, GeoPoint endP) {
        map.getOverlays().removeAll(Collections.singleton(roadOverlay));

        AsyncTask.execute(() -> {
            RoadManager roadManager = new OSRMRoadManager(map.getContext(),  System.getProperty("http.agent"));
            ArrayList<GeoPoint> waypoints = new ArrayList<>();

            waypoints.add(startP);

            waypoints.add(endP);
            Road road = roadManager.getRoad(waypoints);
            roadOverlay = RoadManager.buildRoadOverlay(road);
            roadOverlay.setWidth(10); // Измените это значение на желаемую толщину

            map.getOverlays().add(roadOverlay);
            // Вычисляем BoundingBox
            map.invalidate();
        });
    }

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

    private static void updateRoutMarker(List<String> settings, Context context) {
        ContentValues cv = new ContentValues();

        cv.put("startLat",  Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

    @SuppressLint("Range")
    public static String getTaxiUrlSearchMarkers(String urlAPI, Context context) {

        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery(query, null);

        cursor.moveToFirst();

        // Получите значения полей из первой записи

        double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
        double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
        double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
        double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;

        cursor.close();
        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
        String comment = stringList.get(2);
        String date = stringList.get(3);

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif =  stringListInfo.get(2);
        String payment_type =  stringListInfo.get(4);
        String addCost = stringListInfo.get(5);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);
        if(urlAPI.equals("costSearchMarkers")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type;
        }

        if(urlAPI.equals("orderSearchMarkers")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);


            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "/" + addCost + "/" + time + "/" + comment + "/" + date;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });

        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
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
            for (int i = 0; i < OpenStreetMapVisicomActivity.arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(OpenStreetMapVisicomActivity.arrayServiceCode()[i]);
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
        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "https://m.easy-order-taxi.site/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);
        database.close();
        Log.d(TAG, "getTaxiUrlSearchMarkers: " + url);
        return url;
    }

    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        assert c != null;
        c.close();
        database.close();
        return list;
    }

    public static class VerifyUserTask extends AsyncTask<Void, Void, Map<String, String>> {
        @SuppressLint("StaticFieldLeak")
        private final Context context;
        SQLiteDatabase database;

        public VerifyUserTask(Context context) {
            this.context = context;
            this.database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        }
        @Override
        protected Map<String, String> doInBackground(Void... voids) {
            String userEmail = logCursor(MainActivity.TABLE_USER_INFO, this.context).get(3);

            String url = "https://m.easy-order-taxi.site/android/verifyBlackListUser/" + userEmail + "/" + "com.taxi.easy.ua";
            try {
                return CostJSONParser.sendURL(url);
            } catch (Exception e) {
                return null;
            }

        }

        @Override
        protected void onPostExecute(Map<String, String> sendUrlMap) {
            String message = sendUrlMap.get("Message");
            ContentValues cv = new ContentValues();

            if (message != null) {
                if (message.equals("В черном списке")) {
                    cv.put("verifyOrder", "0");
                    database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                }
            }
            database.close();
        }
    }

    @SuppressLint("Range")
    private double getFromTablePositionInfo(Context context, String columnName) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery("SELECT "+ columnName + " FROM " + MainActivity.TABLE_POSITION_INFO + " WHERE id = ?", new String[]{"1"});

        double result = 0.0; // Значение по умолчанию или обработка, если запись не найдена.

        if (cursor != null && cursor.moveToFirst()) {
            result = cursor.getDouble(cursor.getColumnIndex(columnName));
            cursor.close();
        }

        database.close();

        return result;
    }



}