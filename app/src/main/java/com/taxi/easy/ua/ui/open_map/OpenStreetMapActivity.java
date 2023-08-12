package com.taxi.easy.ua.ui.open_map;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.NetworkChangeReceiver;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ServerConnection;
import com.taxi.easy.ua.cities.Kyiv.KyivCity;
import com.taxi.easy.ua.cities.OdessaTest.Odessa;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.home.MyBottomSheetDialogFragment;
import com.taxi.easy.ua.ui.home.MyGeoDialogFragment;
import com.taxi.easy.ua.ui.home.MyPhoneDialogFragment;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.maps.ToJSONParser;
import com.taxi.easy.ua.ui.start.ResultSONParser;
import com.taxi.easy.ua.ui.start.StartActivity;

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
import java.util.regex.Pattern;


public class OpenStreetMapActivity extends AppCompatActivity {
    private final String TAG = "TAG";
    private LocationManager locationManager;

    private IMapController mapController;
    EditText to_number;
    private String to, messageResult, from_geo;
    public String[] arrayStreet;
    static FloatingActionButton fab, fab_call, fab_open_map, fab_add;

    private static double startLat, startLan, finishLat, finishLan;
    static MapView map = null;
    private static String api;
    public static GeoPoint startPoint;
    public static GeoPoint endPoint;
    static Switch gpsSwitch;
    private static String[] array;

    ArrayList<Map> adressArr;
    AlertDialog  coastDialog;
    static Polyline roadOverlay;
    static Marker m;
    private static String FromAdressString;
    public static String cm, UAH, em, co, fb, vi, fp, ord, onc, tm, tom, ntr, hlp,
            tra, plm, epm, tlm, sbt, cbt, vph, coo;
    LayoutInflater inflater;
    static View view;

    int selectedItem;
    public static FragmentManager fragmentManager;
    ProgressBar progressBar;
    public static String[] arrayServiceCode() {
        return new String[]{
                "BAGGAGE",
                "ANIMAL",
                "CONDIT",
                "MEET",
                "COURIER",
                "TERMINAL",
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
    public static String from_name, from_house;

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    Dialog alertDialog;

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, getString(R.string.check_position), Toast.LENGTH_SHORT).show();
        networkChangeReceiver = new NetworkChangeReceiver();
        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.open_street_map_layout);
        fragmentManager = getSupportFragmentManager();

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

        inflater = getLayoutInflater();
        view = inflater.inflate(R.layout.phone_verify_layout, null);
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        mapController = map.getController();
        startLat = 50.27;
        startLan = 30.31;
        GeoPoint initialGeoPoint = new GeoPoint(startLat, startLan);
        map.getController().setCenter(initialGeoPoint);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        mapController.setZoom(16);
        map.setClickable(true);

        List<String> stringList = logCursor(StartActivity.CITY_INFO, this);
        switch (stringList.get(1)){
            case "Kyiv City":
                arrayStreet = KyivCity.arrayStreet();
                api = StartActivity.apiKyiv;
                break;
            case "Odessa":
                arrayStreet = Odessa.arrayStreet();
                api = StartActivity.apiTest;
                break;
            default:
                arrayStreet = Odessa.arrayStreet();
                break;
        }


        if (!routMaps().isEmpty()) {
            adressArr = new ArrayList<>(routMaps().size());
        }

        fab = findViewById(R.id.fab);
        fab_call = findViewById(R.id.fab_call);
        fab_open_map = findViewById(R.id.fab_open_map);
        fab_add = findViewById(R.id.fab_add);
        gpsSwitch = findViewById(R.id.gpsSwitch);

        gpsSwitch.setChecked(switchState());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connected()) {
                    Intent intent = new Intent(OpenStreetMapActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        });

        fab_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:0674443804"));
                startActivity(intent);
            }
        });
        fab_open_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(OpenStreetMapActivity.this, OpenStreetMapActivity.class));
                } else {
                    Intent intent = new Intent(OpenStreetMapActivity.this, MainActivity.class);
                    startActivity(intent);
                }
            }
        });

        gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                gpsSwitch.setChecked(switchState());
            }


        });

        fab_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyBottomSheetDialogFragment bottomSheetDialogFragment = new MyBottomSheetDialogFragment();
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });



        progressBar = findViewById(R.id.progressBar);

//        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
//        LayoutInflater inflater = this.getLayoutInflater();
//        View view = inflater.inflate(R.layout.check_out_layout, null);
//        builder.setView(view);
//
//        // Устанавливаем отрицательную кнопку (Отмена)
//        builder.setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                progressBar.setVisibility(View.INVISIBLE);
//                alertDialog.dismiss();
//                 Intent intent = new Intent(OpenStreetMapActivity.this, MainActivity.class);
//                startActivity(intent);
//            }
//        });
//
//        // Создаем и показываем диалог
//        alertDialog = builder.show();

        array = arrayAdressAdapter();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // Обработка полученных местоположений
                stopLocationUpdates();

                // Обработка полученных местоположений
                List<Location> locations = locationResult.getLocations();
                if (!locations.isEmpty()) {
                    Location firstLocation = locations.get(0);
                    double latitude = firstLocation.getLatitude();
                    double longitude = firstLocation.getLongitude();
                    startLat = latitude;
                    startLan = longitude;

                    String urlFrom =  "https://m.easy-order-taxi.site/" + api + "/android/fromSearchGeo/" + startLat + "/" + startLan;
                    Map sendUrlFrom = null;
                    try {
                        sendUrlFrom = FromJSONParser.sendURL(urlFrom);

                    } catch (MalformedURLException | InterruptedException | JSONException e) {
                        Toast.makeText(getApplicationContext(), getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                        finish();
                    }
                    FromAdressString =  (String) sendUrlFrom.get("route_address_from");
                    progressBar.setVisibility(View.INVISIBLE);
                    MarkerOverlay markerOverlay = new MarkerOverlay(OpenStreetMapActivity.this);
                    map.getOverlays().add(markerOverlay);
                    setMarker(startLat, startLan, FromAdressString);
                    GeoPoint initialGeoPoint = new GeoPoint(startLat, startLan);
                    map.getController().setCenter(initialGeoPoint);

                    map.invalidate();
                    progressBar.setVisibility(View.INVISIBLE);

                }


                try {
                    dialogFromToGeo();
                } catch (MalformedURLException | InterruptedException | JSONException e) {
                    Toast.makeText(getApplicationContext(), getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                }

            }
        };

        List<String> stringListArr = logCursor(StartActivity.CITY_INFO, this);
        switch (stringListArr.get(1)){
            case "Kyiv City":
                arrayStreet = KyivCity.arrayStreet();
                api = StartActivity.apiKyiv;
                break;
            case "Odessa":
                arrayStreet = Odessa.arrayStreet();
                api = StartActivity.apiTest;
                break;
            default:
                arrayStreet = Odessa.arrayStreet();
                api = StartActivity.apiTest;
                break;
        }
        
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = createLocationRequest();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

   ArrayList<Map> routMaps() {
        Map <String, String> routs;
        ArrayList<Map> routsArr = new ArrayList<>();
        SQLiteDatabase database = this.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(StartActivity.TABLE_ORDERS_INFO, null, null, null, null, null, null);
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

        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
        map.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }


    private void showLocation(Location location) throws MalformedURLException, InterruptedException, JSONException {
        if (location == null)
            return;

        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            startLat = location.getLatitude();
            startLan = location.getLongitude();
        }
        else {
            if (location.getProvider().equals(
                    LocationManager.NETWORK_PROVIDER)) {
                startLat = location.getLatitude();
                startLan = location.getLongitude();
            }
        }

        String urlFrom =  "https://m.easy-order-taxi.site/" + api + "/android/fromSearchGeo/" + startLat + "/" + startLan;
        Map<String, String> sendUrlFrom = FromJSONParser.sendURL(urlFrom);

        FromAdressString =  (String) sendUrlFrom.get("route_address_from");
        MarkerOverlay markerOverlay = new MarkerOverlay(this);
        map.getOverlays().add(markerOverlay);

        dialogFromToGeo();

    }

    public static void setMarker(double Lat, double Lan, String title) {
        m = new Marker(map);
        m.setPosition(new GeoPoint(Lat, Lan));
        m.setTextLabelBackgroundColor(
                Color.TRANSPARENT
        );
        m.setTextLabelForegroundColor(
                Color.RED
        );
        m.setTextLabelFontSize(40);
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        m.setTitle(title);
        map.getOverlays().add(m);
        map.invalidate();
    }

    private static void showRout(GeoPoint startPoint, GeoPoint endPoint) {
        map.getOverlays().removeAll(Collections.singleton(roadOverlay));
        AsyncTask.execute(() -> {
            RoadManager roadManager = new OSRMRoadManager(map.getContext(),  System.getProperty("http.agent"));
            ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();

            waypoints.add(startPoint);

            waypoints.add(endPoint);
            Road road = roadManager.getRoad(waypoints);
            roadOverlay = RoadManager.buildRoadOverlay(road);

            map.getOverlays().add(roadOverlay);
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
    private static boolean hasServer() {
        CompletableFuture<Boolean> connectionFuture = checkConnectionAsync();
        boolean isConnected = false;
        try {
            isConnected = connectionFuture.get();
        } catch (Exception e) {

        }
        return  isConnected;
    };
    private static boolean connected() {

        Boolean hasConnect = false;

        ConnectivityManager cm = (ConnectivityManager) map.getContext().getSystemService(
                Context.CONNECTIVITY_SERVICE);
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

        return hasConnect;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void dialogMarkers(FragmentManager fragmentManager) throws MalformedURLException, JSONException, InterruptedException {
        if(hasServer()){
            if(endPoint != null) {

            Log.d("TAG", "onResume: endPoint" +  endPoint.getLatitude());
            map.getOverlays().remove(OpenStreetMapActivity.m);
            map.getOverlays().removeAll(Collections.singleton(roadOverlay));

            String urlCost = getTaxiUrlSearchMarkers(startPoint.getLatitude(), startPoint.getLongitude(),
                    endPoint.getLatitude(), endPoint.getLongitude(), "costSearchMarkers", map.getContext());

            Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

            String message = (String) sendUrlMapCost.get("message");
            String orderCost = (String) sendUrlMapCost.get("order_cost");


            if (orderCost.equals("0")) {
                Toast.makeText(map.getContext(), message, Toast.LENGTH_SHORT).show();
                map.getContext().startActivity(new Intent(map.getContext(), MainActivity.class));
            }
            if (!orderCost.equals("0")) {
                MaterialAlertDialogBuilder builderAddCost =  new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme);
                LayoutInflater inflater =  LayoutInflater.from(map.getContext());

                View view_cost = inflater.inflate(R.layout.add_cost_layout, null);
                builderAddCost.setView(view_cost);
                TextView costView = view_cost.findViewById(R.id.cost);
                costView.setText(orderCost);
                StartActivity.cost = Long.parseLong(orderCost);
                StartActivity.addCost = 0;
                Button btn_minus = view_cost.findViewById(R.id.btn_minus);
                Button btn_plus = view_cost.findViewById(R.id.btn_plus);

                btn_minus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(StartActivity.addCost != 0) {
                            StartActivity.addCost -= 5;
                            StartActivity.cost -= 5;
                            costView.setText(String.valueOf(StartActivity.cost));
                        }
                    }
                });
                btn_plus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        StartActivity.addCost += 5;
                        StartActivity.cost += 5;

                        costView.setText(String.valueOf(StartActivity.cost));
                    }
                });
                if (!verifyPhone(map.getContext())) {
                    getPhoneNumber();
                }
                if (!verifyPhone(map.getContext())) {

                    MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment();
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }

                builderAddCost
                        .setPositiveButton(ord, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(connected()) {
                                    if (verifyPhone(map.getContext())) {
                                        try {
                                            String urlCost = getTaxiUrlSearchMarkers(startPoint.getLatitude(), startPoint.getLongitude(),
                                                    endPoint.getLatitude(), endPoint.getLongitude(), "orderSearchMarkers", map.getContext());

                                            Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

                                            String message = (String) sendUrlMapCost.get("message");
                                            String orderCost = (String) sendUrlMapCost.get("order_cost");

                                            if (orderCost.equals("0")) {
                                                Toast.makeText(map.getContext(), em + message, Toast.LENGTH_LONG).show();
                                            }
                                            if (!orderCost.equals("0")) {

                                                if (!verifyOrder(map.getContext())) {

                                                    Toast.makeText(map.getContext(), co + orderCost + fb, Toast.LENGTH_SHORT).show();
                                                } else {
                                                    String orderWeb = (String) sendUrlMapCost.get("order_cost");

                                                    if (!orderWeb.equals("0")) {

                                                        String to_name;
                                                        if(Objects.equals(sendUrlMapCost.get("routefrom"), sendUrlMapCost.get("routeto"))) {
                                                            to_name = onc;
                                                            if(!sendUrlMapCost.get("lat").equals("0")) {
                                                                insertRecordsOrders(
                                                                        (String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routefrom"),
                                                                        (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("routefromnumber"),
                                                                        Double.toString(startPoint.getLatitude()), Double.toString(startPoint.getLongitude()),
                                                                        Double.toString(startPoint.getLatitude()), Double.toString(startPoint.getLongitude()), map.getContext()
                                                                );
                                                            }
                                                        } else {
                                                            to_name = (String) sendUrlMapCost.get("routeto") + " " + (String) sendUrlMapCost.get("to_number");
                                                            if(!sendUrlMapCost.get("lat").equals("0")) {
                                                                insertRecordsOrders(
                                                                        (String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routeto"),
                                                                        (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("to_number"),
                                                                        Double.toString(startPoint.getLatitude()), Double.toString(startPoint.getLongitude()),
                                                                        (String) sendUrlMapCost.get("lat"), (String) sendUrlMapCost.get("lng"), map.getContext()
                                                                );
                                                            }
                                                        }

                                                        String messageResult = tm +
                                                                FromAdressString + tom +
                                                                to_name + "." +
                                                                co + orderWeb + UAH;
//                                                        Log.d("TAG", "onClick messageResult: " + messageResult);
                                                        finishLat = Double.parseDouble(sendUrlMapCost.get("lat").toString());
                                                        finishLan = Double.parseDouble(sendUrlMapCost.get("lng").toString());
                                                        if(finishLan != 0) {

                                                            setMarker(finishLat, finishLan, to_name);
                                                            GeoPoint endPoint = new GeoPoint(finishLat, finishLan);
                                                            showRout(startPoint, endPoint);
                                                        }
//                                                        Toast.makeText(map.getContext(), messageResult, Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent(map.getContext(), FinishActivity.class);
                                                        intent.putExtra("messageResult_key", messageResult);
                                                        intent.putExtra("UID_key", Objects.requireNonNull(sendUrlMapCost.get("dispatching_order_uid")));
                                                        map.getContext().startActivity(intent);
                                                    } else {
                                                        message = (String) sendUrlMapCost.get("message");
                                                        MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme);
                                                        LayoutInflater inflater = LayoutInflater.from(map.getContext());
                                                        View view = inflater.inflate(R.layout.free_message_layout, null);
                                                        TextView alertMessage = view.findViewById(R.id.text_message);
                                                        alertMessage.setText(message + ntr);
                                                        alertDialogBuilder.setView(view);

                                                        alertDialogBuilder.setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                                    @SuppressLint("SuspiciousIndentation")
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                        intent.setData(Uri.parse("tel:0674443804"));
                                                                        map.getContext().startActivity(intent);
                                                                    }
                                                                })
                                                                .setNegativeButton(tra, null)
                                                                .show();
                                                    }
                                                }

                                            }

                                        } catch (MalformedURLException | InterruptedException |
                                                 JSONException e) {
                                             
                                        }
                                    }
                                    else {
                                        Toast.makeText(map.getContext(), plm, Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(map.getContext(), OpenStreetMapActivity.class);
                                        map.getContext().startActivity(intent);
                                    }

                                } else {
                                    Toast.makeText(map.getContext(), vi, Toast.LENGTH_LONG).show();
                                }
                            }
                        })
                        .setNegativeButton(R.string.try_again, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(map.getContext(), OpenStreetMapActivity.class);
                                map.getContext().startActivity(intent);
                            }
                        })
                        .show();
            }

            String target =  OpenStreetMapActivity.FromAdressString;

            OpenStreetMapActivity.setMarker(startPoint.getLatitude(), startPoint.getLongitude(), target);

            target = sendUrlMapCost.get("routeto");
            OpenStreetMapActivity.setMarker(endPoint.getLatitude(), endPoint.getLongitude(), target);

            OpenStreetMapActivity.showRout(startPoint, endPoint);
        };
        } else {
            Toast.makeText(map.getContext(), R.string.server_error_connected, Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean verifyOrder(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);

        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(StartActivity.TABLE_USER_INFO, context).get(1).equals("0")) {
                verify = false;
            }
            cursor.close();
        }

        return verify;

    }

    private static boolean verifyPhone(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(StartActivity.TABLE_USER_INFO, context).get(2).equals("+380")) {
                verify = false;
            }
            cursor.close();
        }

        return verify;
    }
    private void dialogFromToGeo() throws MalformedURLException, InterruptedException, JSONException {
//        alertDialog.dismiss();
        if(hasServer()) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.from_to_geo_layout, null);
            builder.setView(view);
            coastDialog = builder.create();

            to_number = view.findViewById(R.id.to_number);

            Button buttonAddServicesView =  view.findViewById(R.id.btnAdd);
            buttonAddServicesView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MyBottomSheetDialogFragment bottomSheetDialogFragment = new MyBottomSheetDialogFragment();
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            });
            from_geo = startLat + " - " + startLan;


            String urlFrom = "https://m.easy-order-taxi.site/" + api + "/android/fromSearchGeo/" + startLat + "/" + startLan;
            Map sendUrlMap = FromJSONParser.sendURL(urlFrom);

            String orderWeb = (String) sendUrlMap.get("order_cost");
            if (orderWeb.equals("100")) {

                from_geo = getString(R.string.you_this) + (String) sendUrlMap.get("route_address_from");
//                textViewFrom.setText(from_geo);
//                Toast.makeText(this, from_geo, Toast.LENGTH_SHORT).show();
                from_name = (String) sendUrlMap.get("name");
                from_house = (String) sendUrlMap.get("house");

                MyGeoDialogFragment bottomSheetDialogFragment = MyGeoDialogFragment.newInstance(from_geo);
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                startPoint = new GeoPoint(startLat, startLan);
                setMarker(startLat,startLan, from_geo);
                to = Double.toString(startLat);
                to_number.setText(" ");
            } else {
                Toast.makeText(this, (String) sendUrlMap.get("message"), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
//            Toast.makeText(this, R.string.find_of_map, Toast.LENGTH_SHORT).show();

            AutoCompleteTextView text_to = view.findViewById(R.id.text_to);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(OpenStreetMapActivity.this,
                    android.R.layout.simple_dropdown_item_1line, arrayStreet);

            AutoCompleteTextView textViewTo = view.findViewById(R.id.text_to);

            textViewTo.setAdapter(adapter);
            Log.d("TAG", "dialogFromToGeo textViewTo: " + textViewTo.getText());
            textViewTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(connected()) {

                        to = String.valueOf(adapter.getItem(position));
                        if (to.indexOf("/") != -1) {
                            to = to.substring(0,  to.indexOf("/"));
                        };
                        String url = "https://m.easy-order-taxi.site/" + api + "/android/autocompleteSearchComboHid/" + to;


                        Log.d("TAG", "onClick urlCost: " + url);
                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException | InterruptedException | JSONException e) {
                             
                        }

                        String orderCost = (String) sendUrlMapCost.get("message");
                        Log.d("TAG", "onClick Hid : " + orderCost);

                        if (orderCost.equals("1")) {
                            to_number.setVisibility(View.VISIBLE);
                            to_number.setText(" ");
                            to_number.requestFocus();
                        }  else if (orderCost.equals("0")) {
                            to_number.setText(" ");
                            to_number.setVisibility(View.INVISIBLE);
                        }
                    }

                }
            });

        builder.setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            coastDialog.dismiss();

                            if(connected()) {
                                String urlCost = getTaxiUrlSearchGeo(startPoint.getLatitude(), startPoint.getLongitude(),
                                        to, to_number.getText().toString(), "costSearchGeo", OpenStreetMapActivity.this);

                                Log.d("TAG", "onClick urlCost: " + urlCost);

                                Map<String, String> sendUrlMapCost = null;
                                try {
                                    sendUrlMapCost = ToJSONParser.sendURL(urlCost);
                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    Toast.makeText(OpenStreetMapActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                                }

                                String message = (String) sendUrlMapCost.get("message");
                                String orderCost = (String) sendUrlMapCost.get("order_cost");
                                Log.d("TAG", "onClick orderCost : " + orderCost);

                                if (orderCost.equals("0")) {
                                    Log.d("TAG", "onClick: 6579465465465465465456");
                                    Toast.makeText(OpenStreetMapActivity.this, getString(R.string.error_message) + message, Toast.LENGTH_LONG).show();
//                                        finish();
//                                        Intent intent = new Intent(OpenStreetMapActivity.this, OpenStreetMapActivity.class);
//                                        startActivity(intent);
                                }
                                if (!orderCost.equals("0")) {
                                        Log.d(TAG, "onClick 3333: " + sendUrlMapCost.get("lat") + " " + sendUrlMapCost.get("lng"));

                                        finishLat = Double.parseDouble(sendUrlMapCost.get("lat").toString());
                                        finishLan = Double.parseDouble(sendUrlMapCost.get("lng").toString());
                                        if(finishLan != 0) {
                                            String target = to + " " + to_number.getText().toString();
                                            setMarker(finishLat, finishLan, target);
                                            GeoPoint endPoint = new GeoPoint(finishLat, finishLan);
                                            showRout(startPoint, endPoint);
                                        }

                                        if (!verifyOrder(getApplicationContext())) {
                                            Toast.makeText(OpenStreetMapActivity.this, getString(R.string.call_of_order) + orderCost + getString(R.string.firebase_false_message), Toast.LENGTH_SHORT).show();
                                        } else {


                                            MaterialAlertDialogBuilder builderAddCost =  new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme);
                                            LayoutInflater inflater = OpenStreetMapActivity.this.getLayoutInflater();

                                            View view_cost = inflater.inflate(R.layout.add_cost_layout, null);
                                            builderAddCost.setView(view_cost);
                                            TextView costView = view_cost.findViewById(R.id.cost);
                                            costView.setText(orderCost);
                                            StartActivity.cost = Long.parseLong(orderCost);
                                            StartActivity.addCost = 0;
                                            Button btn_minus = view_cost.findViewById(R.id.btn_minus);
                                            Button btn_plus = view_cost.findViewById(R.id.btn_plus);

                                            btn_minus.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {

                                                    if(StartActivity.addCost != 0) {
                                                        StartActivity.addCost -= 5;
                                                        StartActivity.cost -= 5;
                                                        costView.setText(String.valueOf(StartActivity.cost));
                                                    }
                                                }
                                            });
                                            btn_plus.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    StartActivity.addCost += 5;
                                                    StartActivity.cost += 5;
                                                    Log.d(TAG, "onClick StartActivity.addCost " + StartActivity.addCost);
                                                    costView.setText(String.valueOf(StartActivity.cost));
                                                }
                                            });
                                            if (!verifyPhone(getApplicationContext())) {
                                                getPhoneNumber();
                                            }
                                            if (!verifyPhone(getApplicationContext())) {
                                                MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment();
                                                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                                            }
                                            builderAddCost
                                                    .setPositiveButton(getString(R.string.order), new DialogInterface.OnClickListener() {
                                                        @RequiresApi(api = Build.VERSION_CODES.O_MR1)
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                            if (connected()) {

                                                                if (verifyPhone(getApplicationContext())) {
                                                                    try {
                                                                        String urlOrder = getTaxiUrlSearchGeo(startPoint.getLatitude(), startPoint.getLongitude(),
                                                                                to, to_number.getText().toString(), "orderSearchGeo", OpenStreetMapActivity.this);
                                                                        Map<String, String> sendUrlMap = ToJSONParser.sendURL(urlOrder);
                                                                        Log.d(TAG, "Map sendUrlMap = ToJSONParser.sendURL(urlOrder); " + sendUrlMap);

                                                                        String orderWeb = (String) sendUrlMap.get("order_cost");

                                                                        if (!orderWeb.equals("0")) {
                                                                            String to_name;
                                                                            if (Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
                                                                                to_name = getString(R.string.on_city_tv);
                                                                                if (!sendUrlMap.get("lat").equals("0")) {
                                                                                    insertRecordsOrders(
                                                                                            (String) sendUrlMap.get("routefrom"), (String) sendUrlMap.get("routefrom"),
                                                                                            (String) sendUrlMap.get("routefromnumber"), (String) sendUrlMap.get("routefromnumber"),
                                                                                            Double.toString(startPoint.getLatitude()), Double.toString(startPoint.getLongitude()),
                                                                                            Double.toString(startPoint.getLatitude()), Double.toString(startPoint.getLongitude()),
                                                                                            getApplicationContext()
                                                                                    );
                                                                                }
                                                                            } else {
                                                                                to_name = (String) sendUrlMap.get("routeto") + " " + (String) sendUrlMap.get("to_number");
                                                                                if (!sendUrlMap.get("lat").equals("0")) {
                                                                                    insertRecordsOrders(
                                                                                            (String) sendUrlMap.get("routefrom"), (String) sendUrlMap.get("routeto"),
                                                                                            (String) sendUrlMap.get("routefromnumber"), (String) sendUrlMap.get("to_number"),
                                                                                            Double.toString(startPoint.getLatitude()), Double.toString(startPoint.getLongitude()),
                                                                                            (String) sendUrlMap.get("lat"), (String) sendUrlMap.get("lng"), getApplicationContext()
                                                                                    );
                                                                                }
                                                                            }
                                                                            messageResult = getString(R.string.thanks_message) +
                                                                                    FromAdressString + " " + getString(R.string.to_message) +
                                                                                    to_name + "." +
                                                                                    getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);

                                                                            finishLat = Double.parseDouble((String) sendUrlMap.get("lat"));
                                                                            finishLan = Double.parseDouble((String) sendUrlMap.get("lng"));
                                                                            if(finishLan != 0) {

                                                                                setMarker(finishLat, finishLan, to_name);
                                                                                GeoPoint endPoint = new GeoPoint(finishLat, finishLan);
                                                                                showRout(startPoint, endPoint);
                                                                            }
//                                                                            Toast.makeText(OpenStreetMapActivity.this, messageResult, Toast.LENGTH_SHORT).show();
                                                                            Intent intent = new Intent(OpenStreetMapActivity.this, FinishActivity.class);
                                                                            intent.putExtra("messageResult_key", messageResult);
                                                                            intent.putExtra("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
                                                                            startActivity(intent);
                                                                        } else {

                                                                            String message = (String) sendUrlMap.get("message");
                                                                            MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme);
                                                                            LayoutInflater inflater = getLayoutInflater();
                                                                            View view = inflater.inflate(R.layout.free_message_layout, null);
                                                                            TextView alertMessage = view.findViewById(R.id.text_message);
                                                                            alertMessage.setText(getString(R.string.error_message) + message );
                                                                            alertDialogBuilder.setView(view);

                                                                            alertDialogBuilder.setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                                                        @SuppressLint("SuspiciousIndentation")
                                                                                        @Override
                                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                                            Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                                            intent.setData(Uri.parse("tel:0674443804"));
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    })
                                                                                    .setNegativeButton(getString(R.string.try_again), null)
                                                                                    .show();
                                                                        }


                                                                    } catch (
                                                                            MalformedURLException |
                                                                            InterruptedException |
                                                                            JSONException e) {
                                                                        
                                                                    }
                                                                }
                                                                else {
                                                                    Toast.makeText(OpenStreetMapActivity.this, getString(R.string.please_phone_message), Toast.LENGTH_SHORT).show();
                                                                    Intent intent = new Intent(OpenStreetMapActivity.this, OpenStreetMapActivity.class);
                                                                    startActivity(intent);
                                                                }
                                                            }
                                                        }
                                                    })

                                                    .setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            //
                                                        }
                                                    })
                                                    .show();



                                        }
                                    }
                            } else {
                                Toast.makeText(OpenStreetMapActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .setNeutralButton(R.string.my_adresses, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            coastDialog.dismiss();
                            if(array.length == 0) {
                                Toast.makeText(OpenStreetMapActivity.this, R.string.make_order_message, Toast.LENGTH_SHORT).show();
                            } else {
                                try {
                                    dialogFromToGeoAdress(array);
                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    Toast.makeText(OpenStreetMapActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                                }
                            }

                        }
                    }).show();

        }  else {
            Toast.makeText(OpenStreetMapActivity.this, getString(R.string.server_error_connected), Toast.LENGTH_LONG).show();
        }
    }
    private void dialogFromToGeoAdress(String[] array) throws MalformedURLException, InterruptedException, JSONException {

        if(hasServer()) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.from_to_geo_adress_layout, null);
            builder.setView(view);


            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.custom_list_item, array);
            ListView listView = view.findViewById(R.id.listAddress);
            listView.setAdapter(adapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemChecked(0, true);
           listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Обработка выбора элемента
                    selectedItem = position;
                    // Дополнительный код по обработке выбора элемента
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Код, если ни один элемент не выбран
                }
            });
            builder
                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Double to_lat =   Double.valueOf ((String) adressArr.get(listView.getCheckedItemPosition()).get("to_lat"));
                            Double to_lng = Double.valueOf ((String) adressArr.get(listView.getCheckedItemPosition()).get("to_lng"));
                            Log.d(TAG, "onClick  to_lat, to_lng: " +  to_lat + " " + to_lng);
                            if(connected()) {
                                try {

                                    String urlCost = OpenStreetMapActivity.getTaxiUrlSearchMarkers(startPoint.getLatitude(), startPoint.getLongitude(),
                                            to_lat, to_lng, "costSearchMarkers", OpenStreetMapActivity.this);

                                    Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

                                    String message = sendUrlMapCost.get("message");
                                    String orderCost = sendUrlMapCost.get("order_cost");

                                    Log.d("TAG", "onClick urlCost: " + urlCost);

                                    if (orderCost.equals("0")) {

                                        Toast.makeText(OpenStreetMapActivity.this, getString(R.string.error_message) + message, Toast.LENGTH_LONG).show();
//                                        finish();
//                                        Intent intent = new Intent(OpenStreetMapActivity.this, OpenStreetMapActivity.class);
//                                        startActivity(intent);
                                    }
                                    if (!orderCost.equals("0")) {
                                        if (!verifyOrder(getApplicationContext())) {
                                             Toast.makeText(OpenStreetMapActivity.this, getString(R.string.call_of_order) + orderCost + getString(R.string.firebase_false_message), Toast.LENGTH_SHORT).show();
                                        } else {

                                            MaterialAlertDialogBuilder builderAddCost =  new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme);
                                            LayoutInflater inflater = OpenStreetMapActivity.this.getLayoutInflater();

                                            View view_cost = inflater.inflate(R.layout.add_cost_layout, null);
                                            builderAddCost.setView(view_cost);
                                            TextView costView = view_cost.findViewById(R.id.cost);
                                            costView.setText(orderCost);
                                            StartActivity.cost = Long.parseLong(orderCost);
                                            StartActivity.addCost = 0;
                                            Button btn_minus = view_cost.findViewById(R.id.btn_minus);
                                            Button btn_plus = view_cost.findViewById(R.id.btn_plus);

                                            btn_minus.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {

                                                    if(StartActivity.addCost != 0) {
                                                        StartActivity.addCost -= 5;
                                                        StartActivity.cost -= 5;
                                                        costView.setText(String.valueOf(StartActivity.cost));
                                                    }
                                                }
                                            });
                                            btn_plus.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    StartActivity.addCost += 5;
                                                    StartActivity.cost += 5;
                                                    Log.d(TAG, "onClick StartActivity.addCost " + StartActivity.addCost);
                                                    costView.setText(String.valueOf(StartActivity.cost));
                                                }
                                            });
                                            if (!verifyPhone(getApplicationContext())) {
                                                getPhoneNumber();
                                            }
                                            if (!StartActivity.verifyPhone) {
                                                MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment();
                                                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                                            }
                                            builderAddCost
                                                    .setPositiveButton(getString(R.string.order), new DialogInterface.OnClickListener() {
                                                        @SuppressLint("NewApi")
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                            if (connected()) {

                                                                if (verifyPhone(getApplicationContext())) {
                                                                    try {
                                                                        String urlOrder = OpenStreetMapActivity.getTaxiUrlSearchMarkers(startPoint.getLatitude(), startPoint.getLongitude(),
                                                                                to_lat, to_lng, "orderSearchMarkers", OpenStreetMapActivity.this);

                                                                        Map<String, String> sendUrlMap = ToJSONParser.sendURL(urlOrder);
                                                                        Log.d(TAG, "Map sendUrlMap = ToJSONParser.sendURL(urlOrder); " + urlOrder);

                                                                        String orderWeb = (String) sendUrlMap.get("order_cost");
                                                                        Log.d("TAG", "onClick: orderWeb.equals(\"0\")" + orderWeb.equals("0"));
                                                                        if (!orderWeb.equals("0")) {
                                                                            String to_name;
                                                                            if(Objects.equals(sendUrlMapCost.get("routefrom"), sendUrlMapCost.get("routeto"))) {
                                                                                to_name = onc;
                                                                                if(!sendUrlMapCost.get("lat").equals("0")) {
                                                                                    insertRecordsOrders(
                                                                                            (String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routefrom"),
                                                                                            (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("routefromnumber"),
                                                                                            Double.toString(startPoint.getLatitude()), Double.toString(startPoint.getLongitude()),
                                                                                            Double.toString(startPoint.getLatitude()), Double.toString(startPoint.getLongitude()),
                                                                                            getApplicationContext()
                                                                                    );
                                                                                }
                                                                            } else {
                                                                                to_name = (String) sendUrlMapCost.get("routeto") + " " + (String) sendUrlMapCost.get("to_number");
                                                                                if(!sendUrlMapCost.get("lat").equals("0")) {
                                                                                   insertRecordsOrders(
                                                                                            (String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routeto"),
                                                                                            (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("to_number"),
                                                                                            Double.toString(startPoint.getLatitude()), Double.toString(startPoint.getLongitude()),
                                                                                            (String) sendUrlMapCost.get("lat"), (String) sendUrlMapCost.get("lng"), getApplicationContext()
                                                                                    );
                                                                                }
                                                                            }
                                                                            messageResult = getString(R.string.thanks_message) +
                                                                                    FromAdressString + getString(R.string.to_message) +
                                                                                    to_name + "." +
                                                                                    getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);
                                                                            finishLat = Double.parseDouble(sendUrlMapCost.get("lat"));
                                                                            finishLan = Double.parseDouble(sendUrlMapCost.get("lng"));
                                                                            if(finishLan != 0) {

                                                                                setMarker(finishLat, finishLan, to_name);
                                                                                GeoPoint endPoint = new GeoPoint(finishLat, finishLan);
                                                                                showRout(startPoint, endPoint);
                                                                            }
//                                                                            Toast.makeText(OpenStreetMapActivity.this, messageResult, Toast.LENGTH_LONG).show();
                                                                            Intent intent = new Intent(OpenStreetMapActivity.this, FinishActivity.class);
                                                                            intent.putExtra("messageResult_key", messageResult);
                                                                            intent.putExtra("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
                                                                            startActivity(intent);
                                                                        } else {
                                                                            String message = (String) sendUrlMap.get("message");
                                                                            MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme);
                                                                            LayoutInflater inflater = getLayoutInflater();
                                                                            View view = inflater.inflate(R.layout.free_message_layout, null);
                                                                            TextView alertMessage = view.findViewById(R.id.text_message);
                                                                            alertMessage.setText(getString(R.string.error_message) + message);
                                                                            alertDialogBuilder.setView(view);

                                                                            alertDialogBuilder.setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                                                        @SuppressLint("SuspiciousIndentation")
                                                                                        @Override
                                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                                            Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                                            intent.setData(Uri.parse("tel:0674443804"));
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    })
                                                                                    .setNegativeButton(getString(R.string.try_again),null)
                                                                                    .show();
                                                                        }


                                                                    } catch (
                                                                            MalformedURLException |
                                                                            InterruptedException |
                                                                            JSONException e) {
                                                                        
                                                                    }
                                                                }
                                                                else {
                                                                    Toast.makeText(OpenStreetMapActivity.this, getString(R.string.please_phone_message), Toast.LENGTH_SHORT).show();
                                                                    Intent intent = new Intent(OpenStreetMapActivity.this, OpenStreetMapActivity.class);
                                                                    startActivity(intent);
                                                                }
                                                            }
                                                        }
                                                    })
                                                    .setNegativeButton("Відміна", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            //
                                                        }
                                                    })
                                                    .show();
                                        }
                                    }

                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    Toast.makeText(OpenStreetMapActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                                }
                            }  else {
                                Toast.makeText(OpenStreetMapActivity.this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                            }
                        }
                    })

                    .setNegativeButton(getString(R.string.change), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                           startActivity(new Intent(OpenStreetMapActivity.this, MainActivity.class));
                        }
                    })
                    .show();
        } else {
            Toast.makeText(OpenStreetMapActivity.this, getString(R.string.server_error_connected), Toast.LENGTH_LONG).show();
        }
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
//                Log.d("TAG", "arrayAdressAdapter routMaps.get(j).get(\"from_lat\"): " + routMaps.get(j).get("from_street"));
//                Log.d("TAG", "arrayAdressAdapter routMaps.get(j).get(\"to_lat\"): " + routMaps.get(j).get("to_street"));

                if(!Objects.requireNonNull(routMaps.get(j).get("to_lat")).toString().equals(Objects.requireNonNull(routMaps.get(j).get("from_lat")).toString())) {
//                    if (!Objects.requireNonNull(routMaps.get(j).get("from_street")).toString().equals(Objects.requireNonNull(routMaps.get(j).get("from_lat")).toString()))
//                        if (!Objects.requireNonNull(routMaps.get(j).get("from_street")).toString().equals("Місце призначення") && !Objects.requireNonNull(routMaps.get(j).get("from_street")).toString().equals(Objects.requireNonNull(routMaps.get(j).get("from_number")).toString())) {
                            adressMap = new HashMap<>();
                            adressMap.put("street", routMaps.get(j).get("from_street").toString());
                            adressMap.put("number", routMaps.get(j).get("from_number").toString());
                            adressMap.put("to_lat", routMaps.get(j).get("from_lat").toString());
                            adressMap.put("to_lng", routMaps.get(j).get("from_lng").toString());
                            adressArrLoc.add(k++, adressMap);
                        }
                    if(!routMaps.get(j).get("to_street").toString().equals("Місце призначення")&&
                            !routMaps.get(j).get("to_street").toString().equals(routMaps.get(j).get("to_lat").toString()) &&
                            !routMaps.get(j).get("to_street").toString().equals(routMaps.get(j).get("to_number").toString()))
                        {
                        adressMap = new HashMap<>();
                        adressMap.put("street", routMaps.get(j).get("to_street").toString());
                        adressMap.put("number", routMaps.get(j).get("to_number").toString());
                        adressMap.put("to_lat", routMaps.get(j).get("to_lat").toString());
                        adressMap.put("to_lng", routMaps.get(j).get("to_lng").toString());
                        adressArrLoc.add(k++, adressMap);
                    }


//                }
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
    private static String getTaxiUrlSearchGeo(double originLatitude, double originLongitude, String to, String to_number, String urlAPI, Context context) {
    if(hasServer()) {
        //  Проверка даты и времени

        List<String> stringList = logCursor(StartActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
        String comment = stringList.get(2);
        String date = stringList.get(3);

        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = to + "/" + to_number;

        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);

        String tarif = logCursor(StartActivity.TABLE_SETTINGS_INFO, context).get(2);


        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        if(urlAPI.equals("costSearchGeo")) {
            Cursor c = database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(StartActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + StartActivity.displayName + "(" + StartActivity.userEmail + ")";
        }

        if(urlAPI.equals("orderSearchGeo")) {
            phoneNumber = logCursor(StartActivity.TABLE_USER_INFO, context).get(2);

            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + StartActivity.displayName  + "/" + StartActivity.addCost + "/" + time + "/" + comment + "/" + date;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(StartActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });
        }

        // Building the url to the web service
        List<String> services = logCursor(StartActivity.TABLE_SERVICE_INFO, context);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i <= 15 ; i++) {
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
        Log.d("TAG", "getTaxiUrlSearch services: " + url);

        return url;
    } else  {
        Toast.makeText(context, context.getString(R.string.server_error_connected), Toast.LENGTH_LONG).show();
        return null;
    }

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getTaxiUrlSearchMarkers(double originLatitude, double originLongitude,
                                                 double toLatitude, double toLongitude,
                                                 String urlAPI, Context context) {
        //  Проверка даты и времени
        if(hasServer()) {
            List<String> stringList = logCursor(StartActivity.TABLE_ADD_SERVICE_INFO, context);
            String time = stringList.get(1);
            String comment = stringList.get(2);
            String date = stringList.get(3);

            // Origin of route
            String str_origin = originLatitude + "/" + originLongitude;

            // Destination of route
            String str_dest = toLatitude + "/" + toLongitude;

    //        Cursor cursorDb = StartActivity.database.query(StartActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
            SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
            String tarif = logCursor(StartActivity.TABLE_SETTINGS_INFO, context).get(2);


            // Building the parameters to the web service

            String parameters = null;
            String phoneNumber = "no phone";
            if(urlAPI.equals("costSearchMarkers")) {
                Cursor c = database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);

                if (c.getCount() == 1) {
                    phoneNumber = logCursor(StartActivity.TABLE_USER_INFO, context).get(2);
                    c.close();
                }
                parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + StartActivity.displayName + "(" + StartActivity.userEmail + ")";
            }

            if(urlAPI.equals("orderSearchMarkers")) {
                phoneNumber = logCursor(StartActivity.TABLE_USER_INFO, context).get(2);


                parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                        + StartActivity.displayName  + "/" + StartActivity.addCost + "/" + time + "/" + comment + "/" + date;

                ContentValues cv = new ContentValues();

                cv.put("time", "no_time");
                cv.put("comment", "no_comment");
                cv.put("date", "no_date");

                // обновляем по id
                database.update(StartActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                        new String[] { "1" });

            }

            // Building the url to the web service
            List<String> services = logCursor(StartActivity.TABLE_SERVICE_INFO, context);
            List<String> servicesChecked = new ArrayList<>();
            String result;
            boolean servicesVer = false;
            for (int i = 1; i <= 15 ; i++) {
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
        } else  {
            Toast.makeText(context, context.getString(R.string.server_error_connected), Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
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

    private static void getPhoneNumber() {
        String mPhoneNumber;
        TelephonyManager tMgr = (TelephonyManager) map.getContext().getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(map.getContext(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(map.getContext(), Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(map.getContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mPhoneNumber = tMgr.getLine1Number();

        if(mPhoneNumber != null) {
            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
            boolean val = Pattern.compile(PHONE_PATTERN).matcher(mPhoneNumber).matches();
            Log.d("TAG", "onClick No validate: " + val);
            if (val == false) {
                Toast.makeText(map.getContext(), fp , Toast.LENGTH_SHORT).show();
                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + mPhoneNumber);

            } else {
                updateRecordsUser(mPhoneNumber, map.getContext());
            }
        }

    }
    public static void updateRecordsUser(String result, Context context) {
        ContentValues cv = new ContentValues();

        cv.put("phone_number", result);

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
        int updCount = database.update(StartActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        Log.d("TAG", "updated rows count = " + updCount);


    }
    private static void insertRecordsOrders( String from, String to,
                                             String from_number, String to_number,
                                             String from_lat, String from_lng,
                                             String to_lat, String to_lng, Context context) {

        String selection = "from_street = ?";
        String[] selectionArgs = new String[] {from};
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor_from = database.query(StartActivity.TABLE_ORDERS_INFO,
                null, selection, selectionArgs, null, null, null);

        selection = "to_street = ?";
        selectionArgs = new String[] {to};

        Cursor cursor_to = database.query(StartActivity.TABLE_ORDERS_INFO,
                null, selection, selectionArgs, null, null, null);



        if (cursor_from.getCount() == 0 || cursor_to.getCount() == 0) {

            String sql = "INSERT INTO " + StartActivity.TABLE_ORDERS_INFO + " VALUES(?,?,?,?,?,?,?,?,?);";
            SQLiteStatement statement = database.compileStatement(sql);
            database.beginTransaction();
            try {
                statement.clearBindings();
                statement.bindString(2, from);
                statement.bindString(3, from_number);
                statement.bindString(4, from_lat);
                statement.bindString(5, from_lng);
                statement.bindString(6, to);
                statement.bindString(7, to_number);
                statement.bindString(8, to_lat);
                statement.bindString(9, to_lng);

                statement.execute();
                database.setTransactionSuccessful();

            } finally {
                database.endTransaction();
            }

        }

        cursor_from.close();
        cursor_to.close();

    }
}