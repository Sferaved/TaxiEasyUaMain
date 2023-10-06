package com.taxi.easy.ua.ui.open_map;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
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
import android.widget.CompoundButton;
import android.widget.EditText;
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
import com.taxi.easy.ua.ServerConnection;
import com.taxi.easy.ua.cities.Cherkasy.Cherkasy;
import com.taxi.easy.ua.cities.Dnipro.Dnipro;
import com.taxi.easy.ua.cities.Kyiv.KyivCity;
import com.taxi.easy.ua.cities.Odessa.Odessa;
import com.taxi.easy.ua.cities.Odessa.OdessaTest;
import com.taxi.easy.ua.cities.Zaporizhzhia.Zaporizhzhia;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.home.MyGeoDialogFragment;
import com.taxi.easy.ua.ui.home.MyGeoMarkerDialogFragment;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.maps.ToJSONParser;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;


public class OpenStreetMapActivity extends AppCompatActivity {
    private final String TAG = "TAG";
    private LocationManager locationManager;

    private static IMapController mapController;
    EditText to_number;
    private String to, messageResult, from_geo;
    public String[] arrayStreet;
    public static FloatingActionButton fab, fab_call, fab_open_map, fab_open_marker;

    public static double startLat, startLan, finishLat, finishLan;
    public static MapView map = null;
    public static String api;
    public static GeoPoint startPoint;
    public static GeoPoint endPoint;
    static Switch gpsSwitch;

    private static String[] array;

    ArrayList<Map> adressArr;

    public static Polyline roadOverlay;
    public static Marker m, marker;
    public static String FromAdressString, ToAdressString;
    public static String cm, UAH, em, co, fb, vi, fp, ord, onc, tm, tom, ntr, hlp,
            tra, plm, epm, tlm, sbt, cbt, vph, coo;
    LayoutInflater inflater;
    static View view;
    public static long addCost;
    public static long cost;
    int selectedItem;
    public static FragmentManager fragmentManager;
    public static ProgressBar progressBar;
    public static MyGeoDialogFragment bottomSheetDialogFragment;
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
    Dialog alertDialog;
    public static String phone;
    MarkerOverlay markerOverlay;
    @SuppressLint("MissingInflatedId")
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
        map.setTileSource(TileSourceFactory.MAPNIK);
        mapController = map.getController();

        FromAdressString = getString(R.string.startPoint);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        mapController.setZoom(19);
        map.setClickable(true);

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

        List<String> stringList = logCursor(MainActivity.CITY_INFO, this);
        switch (stringList.get(1)){
            case "Dnipropetrovsk Oblast":
                arrayStreet = Dnipro.arrayStreet();
                api = MainActivity.apiDnipro;
                phone = "tel:0667257070";
                break;
            case "Odessa":
                arrayStreet = Odessa.arrayStreet();
                api = MainActivity.apiOdessa;
                phone = "tel:0737257070";
                break;
            case "Zaporizhzhia":
                arrayStreet = Zaporizhzhia.arrayStreet();
                api = MainActivity.apiZaporizhzhia;
                phone = "tel:0687257070";
                break;
            case "Cherkasy Oblast":
                arrayStreet = Cherkasy.arrayStreet();
                api = MainActivity.apiCherkasy;
                phone = "tel:0962294243";
                break;
            case "OdessaTest":
                arrayStreet = OdessaTest.arrayStreet();
                api = MainActivity.apiTest;
                phone = "tel:0674443804";
                break;
            default:
                arrayStreet = KyivCity.arrayStreet();
                api = MainActivity.apiKyiv;
                phone = "tel:0674443804";
                break;
        }



        progressBar = findViewById(R.id.progressBar);

        if (!routMaps().isEmpty()) {
            adressArr = new ArrayList<>(routMaps().size());
        }

        fab = findViewById(R.id.fab);
        fab_call = findViewById(R.id.fab_call);
        fab_open_map = findViewById(R.id.fab_open_map);
        fab_open_marker = findViewById(R.id.fab_open_marker);
        fab_open_marker.setVisibility(View.INVISIBLE);

        gpsSwitch = findViewById(R.id.gpsSwitch);

        gpsSwitch.setChecked(switchState());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });

        fab_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });


        gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                gpsSwitch.setChecked(switchState());
            }


        });

        fab_open_marker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyGeoMarkerDialogFragment bottomSheet = new MyGeoMarkerDialogFragment();
                bottomSheet.show(fragmentManager, bottomSheet.getTag());
            }
        });
        array = arrayAdressAdapter();

    }
    private void updateMyPosition(Double startLat, Double startLan, String position) {
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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

   ArrayList<Map> routMaps() {
        Map <String, String> routs;
        ArrayList<Map> routsArr = new ArrayList<>();
        SQLiteDatabase database = this.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(MainActivity.TABLE_ORDERS_INFO, null, null, null, null, null, null);
        int i = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    routs = new HashMap<>();
                    routs.put("id", c.getString(c.getColumnIndexOrThrow ("id")));
                    routs.put("from_street", c.getString(c.getColumnIndexOrThrow ("from_street")));
                    routs.put("from_number", c.getString(c.getColumnIndexOrThrow ("from_number")));
                    routs.put("to_street", c.getString(c.getColumnIndexOrThrow ("to_street")));
                    routs.put("to_number", c.getString(c.getColumnIndexOrThrow ("to_number")));

                    routs.put("from_lat", c.getString(c.getColumnIndexOrThrow ("from_lat")));
                    routs.put("from_lng", c.getString(c.getColumnIndexOrThrow ("from_lng")));

                    routs.put("to_lat", c.getString(c.getColumnIndexOrThrow ("to_lat")));
                    routs.put("to_lng", c.getString(c.getColumnIndexOrThrow ("to_lng")));
                    routsArr.add(i++, routs);
                } while (c.moveToNext());
            }
        }
        database.close();

        return routsArr;
    }
   private boolean  switchState() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled || !network_enabled) {
            return false;
        } else

            return true;
    };
   public void onResume() {
        super.onResume();
        gpsSwitch.setChecked(switchState());
        markerOverlay = new MarkerOverlay(OpenStreetMapActivity.this);
        map.getOverlays().add(markerOverlay);
        fab_open_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                startActivity(new Intent(OpenStreetMapActivity.this, OpenStreetMapActivity.class));
            }
        });
        List<String> startList = logCursor(MainActivity.TABLE_POSITION_INFO, this);
//        startLat = Double.parseDouble(startList.get(1));
//        startLan = Double.parseDouble(startList.get(2));

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
                setMarker(startLat, startLan, FromAdressString);

                bottomSheetDialogFragment = MyGeoDialogFragment.newInstance(FromAdressString);
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                map.invalidate();
            } else {
                Toast.makeText(this, R.string.check_position, Toast.LENGTH_SHORT).show();
                Configuration.getInstance().load(OpenStreetMapActivity.this, PreferenceManager.getDefaultSharedPreferences(OpenStreetMapActivity.this));

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

                locationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
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
                        FromAdressString = (String) sendUrlFrom.get("route_address_from");
                        if(FromAdressString != null) {
                            if (FromAdressString.equals("Точка на карте")) {
                                FromAdressString = getString(R.string.startPoint);
                            }
                        }
                        updateMyPosition(startLat, startLan, FromAdressString);
                        bottomSheetDialogFragment = MyGeoDialogFragment.newInstance(FromAdressString);
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                        map.getOverlays().add(markerOverlay);
                        setMarker(startLat, startLan, FromAdressString);
                        GeoPoint initialGeoPoint = new GeoPoint(startLat-0.0009, startLan);
                        map.getController().setCenter(initialGeoPoint);

                        setMarker(startLat, startLan, FromAdressString);
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
                public void onLocationResult(LocationResult locationResult) {
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
                    FromAdressString = (String) sendUrlFrom.get("route_address_from");
                    if(FromAdressString != null) {
                        if (FromAdressString.equals("Точка на карте")) {
                            FromAdressString = getString(R.string.startPoint);
                        }
                    }
                    updateMyPosition(startLat, startLan, FromAdressString);
                    bottomSheetDialogFragment = MyGeoDialogFragment.newInstance(FromAdressString);
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                    map.getOverlays().add(markerOverlay);
                    setMarker(startLat, startLan, FromAdressString);
                    GeoPoint initialGeoPoint = new GeoPoint(startLat-0.0009, startLan);
                    map.getController().setCenter(initialGeoPoint);

                    setMarker(startLat, startLan, FromAdressString);
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

    public void setMarker(double Lat, double Lan, String title) {
        m = new Marker(map);
        m.setPosition(new GeoPoint(Lat, Lan));

        // Установите название маркера
        String unuString = new String(Character.toChars(0x1F449));
        m.setTitle("1." + unuString + title);

        m.setTextLabelBackgroundColor(Color.TRANSPARENT);
        m.setTextLabelForegroundColor(Color.RED);
        m.setTextLabelFontSize(40);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

        Drawable originalDrawable = getResources().getDrawable(R.drawable.marker_green);

        // Уменьшите размер до 48 пикселей
        int width = 48;
        int height = 48;
        Bitmap bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) originalDrawable).getBitmap(), width, height, false);

        // Создайте новый Drawable из уменьшенного изображения
        Drawable scaledDrawable = new BitmapDrawable(getResources(), bitmap);
        m.setIcon(scaledDrawable);


        m.showInfoWindow();
        map.getOverlays().add(m);
        map.invalidate();
    }


    public static void showRout(GeoPoint startP, GeoPoint endP) {
        map.getOverlays().removeAll(Collections.singleton(roadOverlay));

        AsyncTask.execute(() -> {
            RoadManager roadManager = new OSRMRoadManager(map.getContext(),  System.getProperty("http.agent"));
            ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();

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

    public static CompletableFuture<Boolean> checkConnectionAsync() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        ServerConnection.checkConnection("https://m.easy-order-taxi.site/", new ServerConnection.ConnectionCallback() {
            @Override
            public void onConnectionResult(boolean isConnected) {
                future.complete(isConnected);
            }
        });

        return future;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void dialogMarkers(FragmentManager fragmentManager, Context context) throws MalformedURLException, JSONException, InterruptedException {
        if(endPoint != null) {
            GeoPoint startPoint = new GeoPoint(startLat, startLan);
            showRout(startPoint, endPoint);

            Log.d("TAG", "onResume: endPoint" +  endPoint.getLatitude());

            String urlCost = getTaxiUrlSearchMarkers(startLat, startLan,
                    endPoint.getLatitude(), endPoint.getLongitude(), "costSearchMarkers", map.getContext());

            Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

            String message = sendUrlMapCost.get("message");
            String orderCost = sendUrlMapCost.get("order_cost");

            if (orderCost != null) {
                    if (orderCost.equals("0")) {
                        Toast.makeText(map.getContext(), message, Toast.LENGTH_SHORT).show();

                    } else {
                        Log.d("TAG", "11111 dialogMarkers: sendUrlMapCost.get(\"routeto\")" + sendUrlMapCost.get("routeto"));
                        if(sendUrlMapCost.get("routeto").equals("Точка на карте")) {
                            ToAdressString = context.getString(R.string.end_point_marker);
                        } else {
                            ToAdressString = (String) sendUrlMapCost.get("routeto") + " " + (String) sendUrlMapCost.get("to_number");
                        }

                        Log.d("TAG", "dialogMarkers: ToAdressString " + ToAdressString);
                        Log.d("TAG", "dialogMarkers: endPoint " + endPoint.toString());
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

                        Drawable originalDrawable = context.getResources().getDrawable(R.drawable.marker_green);
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
                        mapController.setZoom(16);

                        map.invalidate();

                        MyGeoMarkerDialogFragment bottomSheet = new MyGeoMarkerDialogFragment();

                        bottomSheet.show(fragmentManager, bottomSheet.getTag());
                    }
                }


        };

    }





    private String[] arrayAdressAdapter() {
        ArrayList<Map>  routMaps = routMaps();

        HashMap<String, String> adressMap;
        String[] arrayRouts;
        ArrayList<Map> adressArrLoc = new ArrayList<>(routMaps().size());

        int i = 0, k = 0;
        boolean flag;
        if(routMaps.size() != 0) {

            for (int j = 0; j < routMaps.size(); j++) {
                Object toLatObject = routMaps.get(j).get("to_lat");
                Object fromLatObject = routMaps.get(j).get("from_lat");

                if (toLatObject != null && fromLatObject != null) {
                    String toLat = toLatObject.toString();
                    String fromLat = fromLatObject.toString();

                    if (!toLat.equals(fromLat)) {
                        if(!Objects.requireNonNull(routMaps.get(j).get("to_lat")).toString().equals(Objects.requireNonNull(routMaps.get(j).get("from_lat")).toString())) {
                            adressMap = new HashMap<>();
                            adressMap.put("street", routMaps.get(j).get("from_street").toString());
                            adressMap.put("number", routMaps.get(j).get("from_number").toString());
                            adressMap.put("to_lat", routMaps.get(j).get("from_lat").toString());
                            adressMap.put("to_lng", routMaps.get(j).get("from_lng").toString());
                            adressArrLoc.add(k++, adressMap);
                        }
                        if(!routMaps.get(j).get("to_street").toString().equals("Місце призначення")&&
                                !routMaps.get(j).get("to_street").toString().equals(routMaps.get(j).get("to_lat").toString()) &&
                                !routMaps.get(j).get("to_street").toString().equals(routMaps.get(j).get("to_number").toString())) {
                            adressMap = new HashMap<>();
                            adressMap.put("street", routMaps.get(j).get("to_street").toString());
                            adressMap.put("number", routMaps.get(j).get("to_number").toString());
                            adressMap.put("to_lat", routMaps.get(j).get("to_lat").toString());
                            adressMap.put("to_lng", routMaps.get(j).get("to_lng").toString());
                            adressArrLoc.add(k++, adressMap);
                        }
                    }
                }

            };
            Log.d("TAG", "arrayAdressAdapter: adressArrLoc " + adressArrLoc.toString());
        } else {
            arrayRouts = null;
        }
        i=0;
        ArrayList<String> arrayList = new ArrayList<>();
        for (int j = 0; j <  adressArrLoc.size(); j++) {

            flag = true;
            for (int l = 0; l <  adressArr.size(); l++) {

                if ( adressArrLoc.get(j).get("street").equals(adressArr.get(l).get("street"))) {
                    flag = false;
                    break;
                }
            }

            if(adressArrLoc.get(j) != null && flag) {
                arrayList.add(adressArrLoc.get(j).get("street") + " " +
                        adressArrLoc.get(j).get("number"));
                adressMap = new HashMap<>();
                adressMap.put("street", (String) adressArrLoc.get(j).get("street"));
                adressMap.put("number", (String) adressArrLoc.get(j).get("number"));

                adressMap.put("to_lat", (String) adressArrLoc.get(j).get("to_lat"));
                adressMap.put("to_lng", (String) adressArrLoc.get(j).get("to_lng"));
                adressArr.add(i++, adressMap);
            };


        }
        arrayRouts = new String[arrayList.size()];
        for (int l = 0; l < arrayList.size(); l++) {
            arrayRouts[l] = arrayList.get(l);
        }

        return arrayRouts;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getTaxiUrlSearchMarkers(double originLatitude, double originLongitude,
                                                 double toLatitude, double toLongitude,
                                                 String urlAPI, Context context) {
        //  Проверка даты и времени
//        if(hasServer()) {

            List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
            String time = stringList.get(1);
            String comment = stringList.get(2);
            String date = stringList.get(3);

            // Origin of route
            String str_origin = String.valueOf(originLatitude) + "/" + String.valueOf(originLongitude);

            // Destination of route
            String str_dest = String.valueOf(toLatitude) + "/" + String.valueOf(toLongitude);

    //        Cursor cursorDb = MainActivity.database.query(MainActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

            List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
            String tarif =  stringListInfo.get(2);
            String bonusPayment =  stringListInfo.get(4);

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
                        + displayName + "*" + userEmail  + "*" + bonusPayment;
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
                Log.d("TAG", "getTaxiUrlSearchGeo result:" + result + "/");
            } else {
                result = "no_extra_charge_codes";
            }

            String url = "https://m.easy-order-taxi.site/" + api + "/android/" + urlAPI + "/" + parameters + "/" + result;


            database.close();


            return url;
//        } else  {
//            Toast.makeText(context, context.getString(R.string.server_error_connected), Toast.LENGTH_LONG).show();
//            return null;
//        }
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
        database.close();
        return list;
    }

    public static class VerifyUserTask extends AsyncTask<Void, Void, Map<String, String>> {
        private Exception exception;
        private Context context;
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
                exception = e;
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