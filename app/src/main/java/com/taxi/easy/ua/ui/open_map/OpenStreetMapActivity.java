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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
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
import com.taxi.easy.ua.ui.maps.ToJSONParser;
import com.taxi.easy.ua.ui.open_map.visicom.GeoDialogVisicomFragment;

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
import java.util.Map;
import java.util.Objects;


public class OpenStreetMapActivity extends AppCompatActivity {
    private static final String TAG = "TAG_OPENMAP";
    public static IMapController mapController;
    public String[] arrayStreet;
    public static FloatingActionButton fab, fab_call, fab_open_map, fab_open_marker;

    public static double startLat, startLan, finishLat, finishLan;
    public static MapView map = null;
    public static String api;
    public static GeoPoint endPoint;
    @SuppressLint({"StaticFieldLeak", "UseSwitchCompatOrMaterialCode"})
    static Switch gpsSwitch;
    public static Polyline roadOverlay;
    public static Marker m, marker;
    public static String FromAdressString, ToAdressString;
    public static String cm, UAH, em, co, fb, vi, fp, ord, onc, tm, tom, ntr, hlp,
            tra, plm, epm, tlm, sbt, cbt, vph, coo;
    LayoutInflater inflater;
    @SuppressLint("StaticFieldLeak")
    static View view;
    public static long addCost;
    public static long cost;
    public static FragmentManager fragmentManager;
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    @SuppressLint("StaticFieldLeak")
    public static GeoDialogVisicomFragment bottomSheetDialogFragment;
    private String city;


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
    public static String phone;
    public static MarkerOverlay markerOverlay;
    @SuppressLint({"MissingInflatedId", "InflateParams"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_street_map_layout);

        new  VerifyUserTask(getApplicationContext()).execute();

        networkChangeReceiver = new NetworkChangeReceiver();

        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        fragmentManager = getSupportFragmentManager();

        inflater = getLayoutInflater();
        view = inflater.inflate(R.layout.phone_verify_layout, null);

        map = findViewById(R.id.map);
//        map.setTileSource(TileSourceFactory.MAPNIK);
        mapController = map.getController();

        switchToRegion();

//        map.setBuiltInZoomControls(true);
//        map.setMultiTouchControls(true);
//        mapController.setZoom(19);
//        map.setClickable(true);

        FromAdressString = getString(R.string.startPoint);
        cm = getString(R.string.coastMarkersMessage);
        UAH = getString(R.string.UAH);
        em = getString(R.string.error_message);
        co = getString(R.string.call_of_order);
        fb = getString(R.string.firebase_false_message);
        vi = getString(R.string.verify_internet);
        fp = getString(R.string.format_phone);
        ord = getString(R.string.order);
        onc = getString(R.string.on_city_tv);
        tm = getString(R.string.thanks_message);
        tom = getString(R.string.to_message);
        ntr = getString(R.string.next_try);
        hlp = getString(R.string.help);
        tra = getString(R.string.try_again);
        plm = getString(R.string.please_phone_message);
        epm = getString(R.string.end_point_marker);
        tlm = getString(R.string.time_limit);
        sbt = getString(R.string.sent_button);
        cbt = getString(R.string.cancel_button);
        vph = getString(R.string.verify_phone);
        coo = getString(R.string.cost_of_order);



        progressBar = findViewById(R.id.progressBar);


        fab = findViewById(R.id.fab);
        fab_call = findViewById(R.id.fab_call);
        fab_open_map = findViewById(R.id.fab_open_map);
        fab_open_marker = findViewById(R.id.fab_open_marker);
        fab_open_marker.setVisibility(View.INVISIBLE);

        gpsSwitch = findViewById(R.id.gpsSwitch);

        gpsSwitch.setChecked(switchState());
        fab.setOnClickListener(view -> {
            finish();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        });

        fab_call.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });


        gpsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            gpsSwitch.setChecked(switchState());
        });

        fab_open_marker.setOnClickListener(v -> {
            progressBar.setVisibility(View.INVISIBLE);
            GeoDialogVisicomFragment bottomSheet = new GeoDialogVisicomFragment();
            bottomSheet.show(fragmentManager, bottomSheet.getTag());
        });
    }

    private void switchToRegion() {

        List<String> stringList = logCursor(MainActivity.CITY_INFO, this);
        city = stringList.get(1);
        api =  stringList.get(2);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        double newZoomLevel = Double.parseDouble(logCursor(MainActivity.TABLE_POSITION_INFO, getApplicationContext()).get(4));
        mapController.setZoom(newZoomLevel);
        map.setClickable(true);
        map.setTileSource(TileSourceFactory.MAPNIK);
//        map.setTileSource(TileSourceFactory.OpenTopo);
        switch (city){
            case "Dnipropetrovsk Oblast":
                // Днепр
                phone = "tel:0667257070";
                break;
            case "Odessa":
                // Одесса
                phone = "tel:0737257070";
                break;
            case "Zaporizhzhia":
                // Запорожье
                phone = "tel:0687257070";
                break;
            case "Cherkasy Oblast":
                // Черкассы
                phone = "tel:0962294243";
                break;
            default:
                phone = "tel:0674443804";
                break;
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
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
   public void onResume() {
        super.onResume();
        gpsSwitch.setChecked(switchState());
        markerOverlay = new MarkerOverlay(OpenStreetMapActivity.this);
        map.getOverlays().add(markerOverlay);
        fab_open_map.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(OpenStreetMapActivity.this, OpenStreetMapActivity.class));
        });
        List<String> startList = logCursor(MainActivity.TABLE_POSITION_INFO, this);

       startLat = getFromTablePositionInfo(this, "startLat" );
       startLan = getFromTablePositionInfo(this, "startLan" );

       FromAdressString = startList.get(3);
       if(FromAdressString != null) {
           if (FromAdressString.equals("Точка на карте")) {
               FromAdressString = getString(R.string.startPoint);
           }
       }
        if (FromAdressString != null) {
            if (!FromAdressString.equals("Палац Спорту, м.Киів")) {
                GeoPoint initialGeoPoint = new GeoPoint(startLat-0.0009, startLan);
                map.getController().setCenter(initialGeoPoint);
                setMarker(startLat, startLan, FromAdressString, getApplicationContext());

                bottomSheetDialogFragment = GeoDialogVisicomFragment.newInstance();
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                map.invalidate();
            } else {
                Toast.makeText(this, R.string.check_position, Toast.LENGTH_SHORT).show();
                Configuration.getInstance().load(OpenStreetMapActivity.this, PreferenceManager.getDefaultSharedPreferences(OpenStreetMapActivity.this));

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
                        String urlFrom = "https://m.easy-order-taxi.site/" + api + "/android/fromSearchGeo/" + startLat + "/" + startLan;
                        Map sendUrlFrom = null;
                        try {
                            sendUrlFrom = FromJSONParser.sendURL(urlFrom);

                        } catch (MalformedURLException | InterruptedException |
                                 JSONException e) {
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                        }
                        assert sendUrlFrom != null;
                        FromAdressString = (String) sendUrlFrom.get("route_address_from");
                        if(FromAdressString != null) {
                            if (FromAdressString.equals("Точка на карте")) {
                                FromAdressString = getString(R.string.startPoint);
                            }
                        }
                        updateMyPosition(startLat, startLan, FromAdressString, getApplicationContext());
                        bottomSheetDialogFragment = GeoDialogVisicomFragment.newInstance();
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                        map.getOverlays().add(markerOverlay);
//                        setMarker(startLat, startLan, FromAdressString, getApplicationContext());
                        GeoPoint initialGeoPoint = new GeoPoint(startLat-0.0009, startLan);
                        map.getController().setCenter(initialGeoPoint);

                        setMarker(startLat, startLan, FromAdressString, getApplicationContext());
                        map.invalidate();
                    }
                };


                if (ContextCompat.checkSelfPermission(OpenStreetMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    requestLocationPermission();
                }
            }
        } else {
            Toast.makeText(this, R.string.check_position, Toast.LENGTH_SHORT).show();
            Configuration.getInstance().load(OpenStreetMapActivity.this, PreferenceManager.getDefaultSharedPreferences(OpenStreetMapActivity.this));

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    // Обработка полученных местоположений
                    stopLocationUpdates();

                    // Обработка полученных местоположений
                    List<Location> locations = locationResult.getLocations();


                    Log.d(TAG, "onLocationResult: locations 666666  " + locations);
                    if (!locations.isEmpty()) {
                        Location firstLocation = locations.get(0);
                        if (startLat != firstLocation.getLatitude() && startLan != firstLocation.getLongitude()) {

                            double latitude = firstLocation.getLatitude();
                            double longitude = firstLocation.getLongitude();
                            startLat = latitude;
                            startLan = longitude;

                        }
                    } else {
                        FromAdressString = getString(R.string.startPoint);
                    }
                    String urlFrom = "https://m.easy-order-taxi.site/" + api + "/android/fromSearchGeo/" + startLat + "/" + startLan;

                    Map sendUrlFrom = null;
                    try {
                        sendUrlFrom = FromJSONParser.sendURL(urlFrom);

                    } catch (MalformedURLException | InterruptedException |
                             JSONException e) {
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    }
                    assert sendUrlFrom != null;
                    FromAdressString = (String) sendUrlFrom.get("route_address_from");
                    if(FromAdressString != null) {
                        if (FromAdressString.equals("Точка на карте")) {
                            FromAdressString = getString(R.string.startPoint);
                        }
                    }
                    updateMyPosition(startLat, startLan, FromAdressString, getApplicationContext());
                    bottomSheetDialogFragment = GeoDialogVisicomFragment.newInstance();
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                    map.getOverlays().add(markerOverlay);
                    setMarker(startLat, startLan, FromAdressString, getApplicationContext());
                    GeoPoint initialGeoPoint = new GeoPoint(startLat-0.0009, startLan);
                    map.getController().setCenter(initialGeoPoint);

                    setMarker(startLat, startLan, FromAdressString, getApplicationContext());
                    map.invalidate();
                }
            };


            if (ContextCompat.checkSelfPermission(OpenStreetMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                requestLocationPermission();
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

        // Set the marker as draggable
        final GeoDialogVisicomFragment bottomSheetDialogFragment = GeoDialogVisicomFragment.newInstance();
        m.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                Toast.makeText(context, R.string.drag_marker, Toast.LENGTH_LONG).show();
                m.setDraggable(true);
                return true;
            }
        });

        // Set up the drag listener
        m.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                // Handle drag start event
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // Handle drag event
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // Получаем координаты маркера после завершения перетаскивания
                GeoPoint newPosition = marker.getPosition();

                // Получаем широту и долготу
                double newLatitude = newPosition.getLatitude();
                double newLongitude = newPosition.getLongitude();
                startLat = newLatitude;
                startLan = newLongitude;
                String urlFrom = "https://m.easy-order-taxi.site/" + api + "/android/fromSearchGeo/" + startLat + "/" + startLan;
                Map sendUrlFrom = null;
                try {
                    sendUrlFrom = FromJSONParser.sendURL(urlFrom);

                } catch (MalformedURLException | InterruptedException |
                         JSONException ignored) {
                }
                assert sendUrlFrom != null;
                FromAdressString = (String) sendUrlFrom.get("route_address_from");

                updateMyPosition(startLat, startLan, FromAdressString, context);

                if (!bottomSheetDialogFragment.isAdded()) {
                    // Если нет, используем getSupportFragmentManager()
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                } else {
                    // Если присоединен, используем getChildFragmentManager()
                    bottomSheetDialogFragment.show(bottomSheetDialogFragment.getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }

            }
        });


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
//            m.showInfoWindow();
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void dialogMarkers(FragmentManager fragmentManager, Context context) throws MalformedURLException, JSONException, InterruptedException {
        if(endPoint != null) {
            GeoPoint startPoint = new GeoPoint(startLat, startLan);
            showRout(startPoint, endPoint);

            Log.d(TAG, "onResume: endPoint" +  endPoint.getLatitude());

            List<String> settings = new ArrayList<>();
            settings.add(String.valueOf(startLat));
            settings.add(String.valueOf(startLan));
            settings.add(String.valueOf(endPoint.getLatitude()));
            settings.add(String.valueOf(endPoint.getLongitude()));

            updateRoutMarker(settings, context);

            String urlCost = getTaxiUrlSearchMarkers("costSearchMarkers", map.getContext());

            Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

            String message = map.getContext().getString(R.string.error_message);
            String orderCost = sendUrlMapCost.get("order_cost");

            if (orderCost != null) {
                    if (orderCost.equals("0")) {
                        Toast.makeText(map.getContext(), message, Toast.LENGTH_SHORT).show();

                    } else {
                        Log.d(TAG, "11111 dialogMarkers: sendUrlMapCost.get(\"routeto\")" + sendUrlMapCost.get("routeto"));
                        if(Objects.requireNonNull(sendUrlMapCost.get("routeto")).equals("Точка на карте")) {
                            ToAdressString = context.getString(R.string.end_point_marker);
                        } else {
                            ToAdressString = sendUrlMapCost.get("routeto") + " " + sendUrlMapCost.get("to_number");
                        }

                        Log.d(TAG, "dialogMarkers: ToAdressString " + ToAdressString);
                        Log.d(TAG, "dialogMarkers: endPoint " + endPoint.toString());
                        finishLat = endPoint.getLatitude();
                        finishLan = endPoint.getLongitude();
                        if(marker != null) {
                            map.getOverlays().remove(marker);
                            map.invalidate();
                            marker = null;
                        }


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

                        marker.setTitle("2."+ unuString + ToAdressString);

                        @SuppressLint("UseCompatLoadingForDrawables") Drawable originalDrawable = context.getResources().getDrawable(R.drawable.marker_green);
                        int width = 48;
                        int height = 48;
                        Bitmap bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) originalDrawable).getBitmap(), width, height, false);

                        // Создайте новый Drawable из уменьшенного изображения
                        Drawable scaledDrawable = new BitmapDrawable(context.getResources(), bitmap);
                        marker.setIcon(scaledDrawable);

                        marker.showInfoWindow();

                        map.getOverlays().add(marker);

                        GeoPoint initialGeoPoint = new GeoPoint(endPoint.getLatitude()-0.01, endPoint.getLongitude());
                        map.getController().setCenter(initialGeoPoint);
                        double newZoomLevel = Double.parseDouble(logCursor(MainActivity.TABLE_POSITION_INFO, context).get(4));
                        mapController.setZoom(newZoomLevel);

                        map.invalidate();

                        GeoDialogVisicomFragment bottomSheet = new GeoDialogVisicomFragment();
                        bottomSheet.show(fragmentManager, bottomSheet.getTag());
                    }
                }
        }
    }

    private static void updateRoutMarker(List<String> settings, Context context) {
        ContentValues cv = new ContentValues();

        cv.put("startLat",  Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
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
            String message = sendUrlMap.get("message");
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