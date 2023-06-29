package com.taxi.easy.ua.ui.open_map;


import static com.taxi.easy.ua.ui.start.StartActivity.READ_CALL_PHONE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.maps.OrderJSONParser;
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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class OpenStreetMapActivity extends AppCompatActivity {
    private final String TAG = "TAG";
    private LocationManager locationManager;
    private Location locationStart;
    private IMapController mapController;
    EditText from_number, to_number;
    private String from, to, messageResult, from_geo;
    public String[] arrayStreet = StartActivity.arrayStreet;

    static FloatingActionButton fab, fab_call, fab_open_map;
    private TextView textViewFrom;
    private static double startLat, startLan, finishLat, finishLan;
    static MapView map = null;
    static GeoPoint startPoint, endPoint;
    static Switch gpsSwitch;
    private static String[] array;
    int on_city = 0;
    String to_str;
    String to_numb_str;

    ArrayList<Map> adressArr;
    AlertDialog progressDialog, coastDialog, adressDialog;
    static Polyline roadOverlay;
    static Marker m;
    private static String FromAdressString;
    static String cm, UAH, em, co, fb, vi, fp, ord, onc, tm, tom, ntr, hlp, tra, plm;
    LayoutInflater inflater;
    static View view;
    static ProgressBar progressBar;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.open_street_map_layout);

        cm =  getString(R.string.coastMarkersMessage);
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
        ntr =  getString(R.string.next_try);
        hlp =  getString(R.string.help);
        tra =  getString(R.string.try_again);
        plm = getString(R.string.please_phone_message);

        inflater = getLayoutInflater();
        view = inflater.inflate(R.layout.phone_verify_layout, null);
        Log.d(TAG, "onCreate: StartActivity.routMaps().size()" + StartActivity.routMaps().size());
        if(StartActivity.routMaps().size() != 0) {
            adressArr = new ArrayList<>(StartActivity.routMaps().size());
        };

        progressBar = findViewById(R.id.progressBar);

        fab = findViewById(R.id.fab);
        fab_call = findViewById(R.id.fab_call);
        fab_open_map = findViewById(R.id.fab_open_map);
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
                    try {
                        dialogFromToGeo();
                    } catch (MalformedURLException | InterruptedException | JSONException e) {
                        throw new RuntimeException(e);
                    }
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
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

/*        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission( Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
//            return;
        }*/
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        mapController = map.getController();
        mapController.setZoom(12);
        map.setClickable(true);

        GeoPoint initialGeoPoint = StartActivity.initialGeoPoint;
        map.getController().setCenter(initialGeoPoint);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.check_out_layout, null);
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(OpenStreetMapActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        progressDialog = builder.create();
        progressDialog.show();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        array = arrayAdressAdapter();



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

//        Toast.makeText(this, "Позначте на карті місця посадки та призначення", Toast.LENGTH_SHORT).show();
//
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(this, "Дозвольте доступ до розташування та спробуйте ще раз", Toast.LENGTH_SHORT).show();
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
            return;
        }

//        Log.d(TAG, "onResume PASSIVE_PROVIDER: " + locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER));

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    1000*5, 10, locationListener);
        } else if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000*5, 10, locationListener);
        }
        checkEnabled();
        map.onResume();



    }


    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
        map.onPause();
    }

    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            try {
                showLocation(location);
            } catch (MalformedURLException | InterruptedException | JSONException e) {
                throw new RuntimeException(e);
            }
            locationStart = location;
            // Отключение слушателя после получения первых координат
            locationManager.removeUpdates(this);
            GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapController.setCenter(startPoint);
        }

        @Override
        public void onProviderDisabled(String provider) {
            checkEnabled();
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onProviderEnabled(String provider) {
            checkEnabled();
            try {
                showLocation(locationManager.getLastKnownLocation(provider));
            } catch (MalformedURLException | InterruptedException | JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
//            if (provider.equals(LocationManager.GPS_PROVIDER)) {
//                Log.d("TAG", "onStatusChanged GPS_PROVIDER: Status: " + status);
//
//            } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
//                Log.d("TAG", "onStatusChanged NETWORK_PROVIDER: Status: " + status);
//
//            }
        }
    };

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

        String urlFrom =  "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/fromSearchGeo/" + startLat + "/" + startLan;
        Map sendUrlFrom = FromJSONParser.sendURL(urlFrom);

        FromAdressString =  (String) sendUrlFrom.get("route_address_from");
        dialogFromToGeo();

//        Toast.makeText(this, location.getProvider() + startLat + " - " + startLan, Toast.LENGTH_SHORT).show();
    }

    private static void setMarker(double Lat, double Lan, String title) {
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
        progressBar.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void checkEnabled() {
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  != true && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) != true) {
            finish();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
//        Log.d("TAG", "checkEnabled: Enabled GPS_PROVIDER: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
//        Log.d("TAG", "checkEnabled: Enabled NETWORK_PROVIDER: " + locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

    }

    public void onClickLocationSettings(View view) {
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    };

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
//            Log.d("TAG", "checkPermission: " + new String[]{permission});
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

        if (!hasConnect) {
            Toast.makeText(map.getContext(), vi, Toast.LENGTH_LONG).show();
        }
        Log.d("TAG", "connected: " + hasConnect);
        return hasConnect;
    }

    public static void dialogMarkers() throws MalformedURLException, JSONException, InterruptedException {
        if(endPoint != null) {
            Log.d("TAG", "onResume: endPoint" +  endPoint.getLatitude());
            map.getOverlays().remove(OpenStreetMapActivity.m);
            map.getOverlays().removeAll(Collections.singleton(roadOverlay));

            String urlCost = getTaxiUrlSearchMarkers(startPoint.getLatitude(), startPoint.getLongitude(),
                    endPoint.getLatitude(), endPoint.getLongitude(), "costSearchMarkers");
            Log.d("TAG", "dialogMarkers: " + urlCost);

            Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

            String message = (String) sendUrlMapCost.get("message");
            String orderCost = (String) sendUrlMapCost.get("order_cost");
            Log.d("TAG", "onClick orderCost : " + orderCost);

            if (orderCost.equals("0")) {
                coastOfRoad(startPoint, message);
//                Toast.makeText(map.getContext(), em + message, Toast.LENGTH_LONG).show();
//                Intent intent = new Intent(map.getContext(), OpenStreetMapActivity.class);
//                map.getContext().startActivity(intent);
            }
            if (!orderCost.equals("0")) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme);
                String message_coats_markers = cm + sendUrlMapCost.get("routeto") +  " "  + orderCost + UAH;
                builder.setMessage(message_coats_markers)
                        .setPositiveButton(ord, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if(connected()) {
                                    Cursor cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                    if (cursor.getCount() == 0) {
                                        Toast.makeText(map.getContext(),  R.string.please_phone_message, Toast.LENGTH_SHORT).show();
                                        getPhoneNumber();
                                        cursor.close();
                                        cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                        if (cursor.getCount() == 0) {
                                            phoneNumberMarkers();
                                            cursor.close();
                                        }
                                    } else {
                                        try {

                                            String urlCost = getTaxiUrlSearchMarkers(startPoint.getLatitude(), startPoint.getLongitude(),
                                                    endPoint.getLatitude(), endPoint.getLongitude(), "orderSearchMarkers");

                                            Log.d("TAG", "onClick urlCost: " + urlCost);

                                            Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

                                            String message = (String) sendUrlMapCost.get("message");
                                            String orderCost = (String) sendUrlMapCost.get("order_cost");
                                            Log.d("TAG", "onClick orderCost : " + orderCost);

                                            if (orderCost.equals("0")) {

                                                Toast.makeText(map.getContext(), em + message, Toast.LENGTH_LONG).show();
                                                Intent intent = new Intent(map.getContext(), OpenStreetMapActivity.class);
                                                map.getContext().startActivity(intent);
                                            }
                                            if (!orderCost.equals("0")) {


                                                if (!MainActivity.verifyOrder) {
                                                    Toast.makeText(map.getContext(), co + orderCost + fb, Toast.LENGTH_SHORT).show();
                                                } else {
                                                    String orderWeb = (String) sendUrlMapCost.get("order_cost");

                                                    if (!orderWeb.equals("0")) {

                                                        String to_name;
                                                        if (Objects.equals(sendUrlMapCost.get("routefrom"), sendUrlMapCost.get("routeto"))) {
                                                            to_name = onc;
                                                            StartActivity.insertRecordsOrders((String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routefrom"),
                                                                    (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("routefromnumber"));
                                                        } else {
                                                            to_name = (String) sendUrlMapCost.get("routeto") + " " + (String) sendUrlMapCost.get("to_number");
                                                            StartActivity.insertRecordsOrders((String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routeto"),
                                                                    (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("to_number"));
                                                        }
                                                        String messageResult = tm +
                                                                FromAdressString + tom +
                                                                to_name + "." +
                                                                co + orderWeb + UAH;
                                                        Log.d("TAG", "onClick messageResult: " + messageResult);

                                                        new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                                                                .setMessage(messageResult)
                                                                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        Intent intent = new Intent(map.getContext(), StartActivity.class);
                                                                        map.getContext().startActivity(intent);
                                                                    }
                                                                })
                                                                .show();
                                                    } else {
                                                        message = (String) sendUrlMapCost.get("message");
                                                        new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                                                                .setMessage(message + ntr)
                                                                .setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                                    @SuppressLint("SuspiciousIndentation")
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                        intent.setData(Uri.parse("tel:0674443804"));
                                                                        map.getContext().startActivity(intent);
                                                                    }
                                                                })
                                                                .setNegativeButton(tra, new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        Intent intent = new Intent(map.getContext(), StartActivity.class);
                                                                        map.getContext().startActivity(intent);
                                                                    }
                                                                })
                                                                .show();
                                                    }
                                                }

                                            }

                                        } catch (MalformedURLException | InterruptedException |
                                                 JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }

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
    }

    private static void coastOfRoad(GeoPoint startPoint, String messageResult) {

        //                Toast.makeText(map.getContext(), em + message, Toast.LENGTH_LONG).show();
//                Intent intent = new Intent(map.getContext(), OpenStreetMapActivity.class);
//                map.getContext().startActivity(intent);
        new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                .setMessage(messageResult)
                .setPositiveButton( onc, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                            Cursor cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                            if (cursor.getCount() == 0) {
                                Toast.makeText(map.getContext(),  R.string.please_phone_message, Toast.LENGTH_SHORT).show();
                                getPhoneNumber();
                                cursor.close();
                                cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                if (cursor.getCount() == 0) {
                                    phoneNumberMarkersOfRoads();
                                    cursor.close();
                                }
                            } else {
                                try {

                                    String urlCost = getTaxiUrlSearchGeo(startPoint.getLatitude(), startPoint.getLongitude(),
                                            Double.toString(startLat),
                                            " ", "costSearchGeo");

                                    Log.d("TAG", "onClick urlCost 1111: " + urlCost);

                                    Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

                                    String message = (String) sendUrlMapCost.get("message");
                                    String orderCost = (String) sendUrlMapCost.get("order_cost");
                                    Log.d("TAG", "onClick orderCost : " + orderCost);

                                    if (orderCost.equals("0")) {

                                        Toast.makeText(map.getContext(), em + message, Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(map.getContext(), OpenStreetMapActivity.class);
                                        map.getContext().startActivity(intent);
                                    }
                                    if (!orderCost.equals("0")) {

                                        if (!MainActivity.verifyOrder) {
                                            Toast.makeText(map.getContext(), co + orderCost + fb, Toast.LENGTH_SHORT).show();
                                        } else {
                                            String orderWeb = (String) sendUrlMapCost.get("order_cost");

                                            if (!orderWeb.equals("0")) {

                                                String to_name = onc;
                                                StartActivity.insertRecordsOrders((String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routefrom"),
                                                        (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("routefromnumber"));

                                                String messageResult = tm +
                                                        FromAdressString + tom +
                                                        to_name + "." +
                                                        co + orderWeb + UAH;
                                                Log.d("TAG", "onClick messageResult: " + messageResult);

                                                new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                                                        .setMessage(messageResult)
                                                        .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(map.getContext(), StartActivity.class);
                                                                map.getContext().startActivity(intent);
                                                            }
                                                        })
                                                        .show();
                                            } else {
                                                message = (String) sendUrlMapCost.get("message");
                                                new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                                                        .setMessage(message + ntr)
                                                        .setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                            @SuppressLint("SuspiciousIndentation")
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                intent.setData(Uri.parse("tel:0674443804"));
                                                                map.getContext().startActivity(intent);
                                                            }
                                                        })
                                                        .setNegativeButton(tra, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(map.getContext(), StartActivity.class);
                                                                map.getContext().startActivity(intent);
                                                            }
                                                        })
                                                        .show();
                                            }
                                        }

                                    }

                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }

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

    private void dialogFromToGeo() throws MalformedURLException, InterruptedException, JSONException {

        if(connected()) {
            map.getOverlays().remove(m);
            map.getOverlays().removeAll(Collections.singleton(roadOverlay));
            MarkerOverlay markerOverlay = new MarkerOverlay(this);
            map.getOverlays().add(markerOverlay);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.from_to_geo_layout, null);
            builder.setView(view);
            coastDialog = builder.create();

            textViewFrom = view.findViewById(R.id.text_from);
            to_number = view.findViewById(R.id.to_number);

            from_geo = startLat + " - " + startLan;


            String urlFrom = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/fromSearchGeo/" + startLat + "/" + startLan;
            Map sendUrlMap = FromJSONParser.sendURL(urlFrom);
            Log.d(TAG, "onClick sendUrlMap: " + sendUrlMap);
            String orderWeb = (String) sendUrlMap.get("order_cost");
            if (orderWeb.equals("100")) {

                from_geo = getString(R.string.you_this) + (String) sendUrlMap.get("route_address_from");
                textViewFrom.setText(from_geo);

                startPoint = new GeoPoint(startLat, startLan);
                setMarker(startLat,startLan, from_geo);

            } else {
                Toast.makeText(this, (String) sendUrlMap.get("message"), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }

            Log.d(TAG, "dialogFromToGeo: " + from_geo);
            AutoCompleteTextView text_to = view.findViewById(R.id.text_to);

            CheckBox checkBox = view.findViewById(R.id.on_city);
            checkBox.setChecked(false);
            on_city = 0;
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(checkBox.isChecked()) {
                        text_to.setVisibility(View.INVISIBLE);
                        on_city = 1;
                        to = Double.toString(startLat);
                    } else {
                        text_to.setVisibility(View.VISIBLE);
                        on_city = 0;
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(OpenStreetMapActivity.this,
                                android.R.layout.simple_dropdown_item_1line, arrayStreet);

                        AutoCompleteTextView textViewTo = view.findViewById(R.id.text_to);
                        textViewTo.setAdapter(adapter);

                        textViewTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                if(connected()) {
                                    to = String.valueOf(adapter.getItem(position));
                                    if (to.indexOf("/") != -1) {
                                        to = to.substring(0,  to.indexOf("/"));
                                    };
                                    String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/autocompleteSearchComboHid/" + to;


                                    Log.d("TAG", "onClick urlCost: " + url);
                                    Map sendUrlMapCost = null;
                                    try {
                                        sendUrlMapCost = ResultSONParser.sendURL(url);
                                    } catch (MalformedURLException | InterruptedException | JSONException e) {
                                        throw new RuntimeException(e);
                                    }

                                    String orderCost = (String) sendUrlMapCost.get("message");
                                    Log.d("TAG", "onClick Hid : " + orderCost);

                                    if (orderCost.equals("1")) {
                                        to_number.setVisibility(View.VISIBLE);
                                        to_number.requestFocus();
                                    }
                                }

                            }
                        });
                    }
                }
            });
            Log.d(TAG, "dialogFromToGeo: on_city" + on_city);
            if(on_city == 0) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, arrayStreet);

                AutoCompleteTextView textViewTo = view.findViewById(R.id.text_to);
                textViewTo.setAdapter(adapter);

                textViewTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if(connected()) {
                            to = String.valueOf(adapter.getItem(position));
                            if (to.indexOf("/") != -1) {
                                to = to.substring(0,  to.indexOf("/"));
                            };
                            String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/autocompleteSearchComboHid/" + to;


                            Log.d("TAG", "onClick urlCost: " + url);
                            Map sendUrlMapCost = null;
                            try {
                                sendUrlMapCost = ResultSONParser.sendURL(url);
                            } catch (MalformedURLException | InterruptedException | JSONException e) {
                                throw new RuntimeException(e);
                            }

                            String orderCost = (String) sendUrlMapCost.get("message");
                            Log.d("TAG", "onClick Hid : " + orderCost);

                            if (orderCost.equals("1")) {
                                to_number.setVisibility(View.VISIBLE);
                                to_number.requestFocus();
                            }
                        }

                    }
                });
            } else {
                to = Double.toString(startLat);
                to_number.setText(" ");
            }
            if(to == null) {
                to = Double.toString(startLat);
                to_number.setText(" ");
            }

            progressDialog.dismiss();
            builder.setMessage( R.string.make_rout_message)
                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if(connected()) {
                                try {

                                    String urlCost = getTaxiUrlSearchGeo(locationStart.getLatitude(), locationStart.getLongitude(), to, to_number.getText().toString(), "costSearchGeo");

                                    Log.d("TAG", "onClick urlCost: " + urlCost);

                                    Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

                                    String message = (String) sendUrlMapCost.get("message");
                                    String orderCost = (String) sendUrlMapCost.get("order_cost");
                                    Log.d("TAG", "onClick orderCost : " + orderCost);

                                    if (orderCost.equals("0")) {

                                        Toast.makeText(OpenStreetMapActivity.this, getString(R.string.error_message) + message, Toast.LENGTH_LONG).show();
                                        finish();
                                        Intent intent = new Intent(OpenStreetMapActivity.this, OpenStreetMapActivity.class);
                                        startActivity(intent);
                                    }
                                    if (!orderCost.equals("0")) {
                                        Log.d(TAG, "onClick 3333: " + sendUrlMapCost.get("lat") + " " + sendUrlMapCost.get("lng"));

                                        finishLat = Double.parseDouble(sendUrlMapCost.get("lat").toString());
                                        finishLan = Double.parseDouble(sendUrlMapCost.get("lng").toString());
                                        if(finishLan != 0) {
                                            String target = getString(R.string.to_point) + to + " " + to_number.getText().toString();
                                            setMarker(finishLat, finishLan, target);
                                            GeoPoint endPoint = new GeoPoint(finishLat, finishLan);
                                            showRout(startPoint, endPoint);
                                        }

                                        if (!MainActivity.verifyOrder) {
                                            Log.d(TAG, "dialogFromToOneRout FirebaseSignIn.verifyOrder: " + MainActivity.verifyOrder);
                                            Toast.makeText(OpenStreetMapActivity.this, getString(R.string.call_of_order) + orderCost + getString(R.string.firebase_false_message), Toast.LENGTH_SHORT).show();
                                        } else {
                                            new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                    .setMessage(getString(R.string.cost_of_order) + orderCost + getString(R.string.UAH))
                                                    .setPositiveButton(getString(R.string.order), new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                            if (connected()) {

                                                                Cursor cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                                                if (cursor.getCount() == 0) {

                                                                    getPhoneNumber();
                                                                    cursor.close();
                                                                    cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                                                    if (cursor.getCount() == 0) {
                                                                        phoneNumberGeo();
                                                                        cursor.close();
                                                                    }
                                                                } else {
                                                                    String urlOrder = getTaxiUrlSearchGeo(locationStart.getLatitude(), locationStart.getLongitude(), to, to_number.getText().toString(), "orderSearchGeo");


                                                                    try {
                                                                        Map sendUrlMap = ToJSONParser.sendURL(urlOrder);
                                                                        Log.d(TAG, "Map sendUrlMap = ToJSONParser.sendURL(urlOrder); " + sendUrlMap);

                                                                        String orderWeb = (String) sendUrlMap.get("order_cost");

                                                                        if (!orderWeb.equals("0")) {
                                                                            String to_name;
                                                                            if(Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
                                                                                to_name = getString(R.string.on_city_tv);
                                                                                StartActivity.insertRecordsOrders((String) sendUrlMap.get("routefrom"), (String) sendUrlMap.get("routefrom"),
                                                                                        (String) sendUrlMap.get("routefromnumber"), (String) sendUrlMap.get("routefromnumber"));
                                                                            } else {
                                                                                to_name = (String) sendUrlMap.get("routeto") + " " + (String) sendUrlMap.get("to_number");
                                                                                StartActivity.insertRecordsOrders((String) sendUrlMap.get("routefrom"), (String) sendUrlMap.get("routeto"),
                                                                                        (String) sendUrlMap.get("routefromnumber"), (String) sendUrlMap.get("to_number"));
                                                                            }
                                                                            messageResult = getString(R.string.thanks_message) +
                                                                                    FromAdressString + " " + getString(R.string.to_message) +
                                                                                    to_name + "." +
                                                                                    getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);


                                                                            new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                                                    .setMessage(messageResult)
                                                                                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                                                        @Override
                                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                                            Intent intent = new Intent(OpenStreetMapActivity.this, StartActivity.class);
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    })
                                                                                    .show();
                                                                        } else {
                                                                            String message = (String) sendUrlMap.get("message");
                                                                            new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                                                    .setMessage(message + getString(R.string.next_try))
                                                                                    .setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                                                        @SuppressLint("SuspiciousIndentation")
                                                                                        @Override
                                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                                            Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                                            intent.setData(Uri.parse("tel:0674443804"));
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    })
                                                                                    .setNegativeButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                                                                                        @Override
                                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                                            Intent intent = new Intent(OpenStreetMapActivity.this, StartActivity.class);
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    })
                                                                                    .show();
                                                                        }


                                                                    } catch (
                                                                            MalformedURLException |
                                                                            InterruptedException |
                                                                            JSONException e) {
                                                                        throw new RuntimeException(e);
                                                                    }
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

                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.change), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            coastDialog.dismiss();
                            Intent intent = new Intent(OpenStreetMapActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    })
                    .setNeutralButton(R.string.my_adresses, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            coastDialog.dismiss();
                            if(array.length == 0) {
                                Toast.makeText(OpenStreetMapActivity.this, R.string.make_order_message, Toast.LENGTH_SHORT).show();
                                try {
                                    dialogFromToGeo();
                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {

                                try {
                                    dialogFromToGeoAdress(array);
                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                        }
                    }).show();
            Toast.makeText(this, R.string.find_of_map, Toast.LENGTH_SHORT).show();
        }
    }
    private void dialogFromToGeoAdress(String[] array) throws MalformedURLException, InterruptedException, JSONException {

        if(connected()) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.from_to_geo_adress_layout, null);
            builder.setView(view);
            adressDialog = builder.create();

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, array);
            ListView listView = view.findViewById(R.id.list);
            listView.setAdapter(adapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemChecked(0, true);
            registerForContextMenu(listView);

            Log.d(TAG, "onClick: startLat adress" +
                    startLat);
            Log.d(TAG, "onClick: startLan adress" +
                    startLan);
            Log.d(TAG, "onClick:  to adress" +
                    to);
            Log.d(TAG, "onClick: to_number.getText().toString() adress " +
                    "." + to_number.getText().toString() + ".");


            builder.setMessage( getString(R.string.to_adress_message))
                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, "onClick: " + adressArr.get(listView.getCheckedItemPosition()));
                            to_str = (String) adressArr.get(listView.getCheckedItemPosition()).get("street");
                            to_numb_str = (String) adressArr.get(listView.getCheckedItemPosition()).get("number");

                            if(connected()) {
                                try {

                                    String urlCost = getTaxiUrlSearchGeo(locationStart.getLatitude(), locationStart.getLongitude(), to_str, to_numb_str, "costSearchGeo");

                                    Log.d("TAG", "onClick urlCost: " + urlCost);

                                    Map sendUrlMapCost = ToJSONParser.sendURL(urlCost);

                                    String message = (String) sendUrlMapCost.get("message");
                                    String orderCost = (String) sendUrlMapCost.get("order_cost");
                                    Log.d("TAG", "onClick orderCost : " + orderCost);

                                    if (orderCost.equals("0")) {

                                        Toast.makeText(OpenStreetMapActivity.this, getString(R.string.error_message) + message, Toast.LENGTH_LONG).show();
                                        finish();
                                        Intent intent = new Intent(OpenStreetMapActivity.this, OpenStreetMapActivity.class);
                                        startActivity(intent);
                                    }
                                    if (!orderCost.equals("0")) {
                                        Log.d(TAG, "onClick 3333: " + sendUrlMapCost.get("lat") + " " + sendUrlMapCost.get("lng"));

                                        finishLat = Double.parseDouble(sendUrlMapCost.get("lat").toString());
                                        finishLan = Double.parseDouble(sendUrlMapCost.get("lng").toString());
                                        if(finishLan != 0) {
                                            String target = getString(R.string.to_point) + to + " " + to_number.getText().toString();
                                            setMarker(finishLat, finishLan, target);
                                            GeoPoint endPoint = new GeoPoint(finishLat, finishLan);
                                            showRout(startPoint, endPoint);
                                        }

                                        if (!MainActivity.verifyOrder) {
                                            Log.d(TAG, "dialogFromToOneRout FirebaseSignIn.verifyOrder: " + MainActivity.verifyOrder);
                                            Toast.makeText(OpenStreetMapActivity.this, getString(R.string.call_of_order) + orderCost + getString(R.string.firebase_false_message), Toast.LENGTH_SHORT).show();
                                        } else {
                                            new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                    .setMessage(getString(R.string.cost_of_order) + orderCost + getString(R.string.UAH))
                                                    .setPositiveButton(getString(R.string.order), new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                            if (connected()) {

                                                                Cursor cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                                                if (cursor.getCount() == 0) {

                                                                    getPhoneNumber();
                                                                    cursor.close();
                                                                    cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                                                    if (cursor.getCount() == 0) {
                                                                        phoneNumberGeo();
                                                                        cursor.close();
                                                                    }
                                                                } else {
                                                                    String urlOrder = getTaxiUrlSearchGeo(locationStart.getLatitude(), locationStart.getLongitude(), to_str, to_numb_str, "orderSearchGeo");


                                                                    try {
                                                                        Map sendUrlMap = ToJSONParser.sendURL(urlOrder);
                                                                        Log.d(TAG, "Map sendUrlMap = ToJSONParser.sendURL(urlOrder); " + sendUrlMap);

                                                                        String orderWeb = (String) sendUrlMap.get("order_cost");

                                                                        if (!orderWeb.equals("0")) {
                                                                            String to_name;
                                                                            if(Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
                                                                                to_name = getString(R.string.on_city_tv);
                                                                                StartActivity.insertRecordsOrders((String) sendUrlMap.get("routefrom"), (String) sendUrlMap.get("routefrom"),
                                                                                        (String) sendUrlMap.get("routefromnumber"), (String) sendUrlMap.get("routefromnumber"));
                                                                            } else {
                                                                                to_name = (String) sendUrlMap.get("routeto") + " " + (String) sendUrlMap.get("to_number");
                                                                                StartActivity.insertRecordsOrders((String) sendUrlMap.get("routefrom"), (String) sendUrlMap.get("routeto"),
                                                                                        (String) sendUrlMap.get("routefromnumber"), (String) sendUrlMap.get("to_number"));
                                                                            }
                                                                            messageResult = getString(R.string.thanks_message) +
                                                                                    FromAdressString + getString(R.string.to_message) +
                                                                                    to_name + "." +
                                                                                    getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);


                                                                            new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                                                    .setMessage(messageResult)
                                                                                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                                                        @Override
                                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                                            Intent intent = new Intent(OpenStreetMapActivity.this, StartActivity.class);
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    })
                                                                                    .show();
                                                                        } else {
                                                                            String message = (String) sendUrlMap.get("message");
                                                                            new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                                                    .setMessage(message + getString(R.string.next_try))
                                                                                    .setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                                                        @SuppressLint("SuspiciousIndentation")
                                                                                        @Override
                                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                                            Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                                            intent.setData(Uri.parse("tel:0674443804"));
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    })
                                                                                    .setNegativeButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                                                                                        @Override
                                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                                            Intent intent = new Intent(OpenStreetMapActivity.this, StartActivity.class);
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    })
                                                                                    .show();
                                                                        }


                                                                    } catch (
                                                                            MalformedURLException |
                                                                            InterruptedException |
                                                                            JSONException e) {
                                                                        throw new RuntimeException(e);
                                                                    }
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
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    })

                    .setNegativeButton(getString(R.string.change), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adressDialog.dismiss();
                            try {
                                dialogFromToGeo();
                            } catch (MalformedURLException | InterruptedException | JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    })
                    .show();
        }
    }

    private String[] arrayAdressAdapter() {
        ArrayList<Map>  routMaps = StartActivity.routMaps();
        HashMap<String, String> adressMap;
        String[] arrayRouts;
        ArrayList<Map> adressArrLoc = new ArrayList<>(StartActivity.routMaps().size());

        int i = 0, k = 0;
        boolean flag;
        if(routMaps.size() != 0) {

            for (int j = 0; j < routMaps.size(); j++) {
                if(!routMaps.get(j).get("from_street").toString().equals(routMaps.get(j).get("to_street").toString())) {

                    adressMap = new HashMap<>();
                    adressMap.put("street", routMaps.get(j).get("from_street").toString());
                    adressMap.put("number",  routMaps.get(j).get("from_number").toString());
                    adressArrLoc.add(k++, adressMap);

                    adressMap = new HashMap<>();
                    adressMap.put("street", routMaps.get(j).get("to_street").toString());
                    adressMap.put("number",  routMaps.get(j).get("to_number").toString());
                    adressArrLoc.add(k++, adressMap);


                }
            };
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
                adressArr.add(i++, adressMap);
            };


        }
        arrayRouts = new String[arrayList.size()];
        for (int l = 0; l < arrayList.size(); l++) {
            arrayRouts[l] = arrayList.get(l);
        }

        return arrayRouts;
    }
    private String getTaxiUrlSearch(String from, String from_number, String to, String to_number, String urlAPI) {

        // Origin of route
        String str_origin = from + "/" + from_number;

        // Destination of route
        String str_dest = to + "/" + to_number;

        StartActivity.cursorDb = StartActivity.database.query(StartActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        String tarif =  StartActivity.logCursor(StartActivity.TABLE_SETTINGS_INFO).get(2);


        // Building the parameters to the web service

        String parameters = str_origin + "/" + str_dest + "/" + tarif;

        if(urlAPI.equals("orderSearch")) {
            String phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + StartActivity.displayName + " (" + StartActivity.userEmail + ")";
        }

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/" + urlAPI + "/" + parameters;
        Log.d("TAG", "getTaxiUrlSearch: " + url);



        return url;
    }
    private static String getTaxiUrlSearchGeo(double originLatitude, double originLongitude, String to, String to_number, String urlAPI) {

        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = to + "/" + to_number;

        StartActivity.cursorDb = StartActivity.database.query(StartActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        String tarif =  StartActivity.logCursor(StartActivity.TABLE_SETTINGS_INFO).get(2);


        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        if(urlAPI.equals("costSearchGeo")) {
            Cursor c = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + StartActivity.displayName + "(" + StartActivity.userEmail + ")";
        }

        if(urlAPI.equals("orderSearchGeo")) {
            phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + StartActivity.displayName ;
        }

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/" + urlAPI + "/" + parameters;
        Log.d("TAG", "getTaxiUrlSearch: " + url);



        return url;
    }
    private static String getTaxiUrlSearchMarkers(double originLatitude, double originLongitude,
                                                  double toLatitude, double toLongitude,
                                                  String urlAPI) {

        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;

        StartActivity.cursorDb = StartActivity.database.query(StartActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        String tarif =  StartActivity.logCursor(StartActivity.TABLE_SETTINGS_INFO).get(2);


        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        if(urlAPI.equals("costSearchMarkers")) {
            Cursor c = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + StartActivity.displayName + "(" + StartActivity.userEmail + ")";
        }

        if(urlAPI.equals("orderSearchMarkers")) {
            phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + StartActivity.displayName ;
        }

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/" + urlAPI + "/" + parameters;
        Log.d("TAG", "getTaxiUrlSearch: " + url);



        return url;
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
                StartActivity.insertRecordsUser(mPhoneNumber);
            }
        }

    }
    private void phoneNumber() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);

        LayoutInflater inflater = this.getLayoutInflater();

        View view = inflater.inflate(R.layout.phone_verify_layout, null);

        builder.setView(view);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText phoneNumber = view.findViewById(R.id.phoneNumber);

//        String result = phoneNumber.getText().toString();
        builder.setTitle("Перевірка телефону")
                .setPositiveButton("Відправити", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                            Log.d("TAG", "onClick befor validate: ");
                            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                            boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                            Log.d("TAG", "onClick No validate: " + val);
                            if (val == false) {
                                Toast.makeText(OpenStreetMapActivity.this, getString(R.string.format_phone) , Toast.LENGTH_SHORT).show();
                                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());
                                OpenStreetMapActivity.this.finish();

                            } else {
                                StartActivity.insertRecordsUser(phoneNumber.getText().toString());
                                String urlOrder = getTaxiUrlSearch(from, from_number.getText().toString(), to, to_number.getText().toString(), "orderSearch");

                                try {
                                    Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);

                                    String orderWeb = (String) sendUrlMap.get("order_cost");
                                    if (!orderWeb.equals("0")) {

                                        String from_name = (String) sendUrlMap.get("from_name");
                                        String to_name = (String) sendUrlMap.get("to_name");
                                        if (from_name.equals(to_name)) {
                                            messageResult = getString(R.string.thanks_message) +
                                                    FromAdressString + getString(R.string.on_city) +
                                                    getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);

                                        } else {
                                            messageResult = getString(R.string.thanks_message) +
                                                    FromAdressString + getString(R.string.to_message) +
                                                    to_name + " " + to_number.getText() + "." +
                                                    getString(R.string.cost_of_order) + orderWeb + getString(R.string.UAH);
                                        }

                                        StartActivity.insertRecordsOrders(from_name, to_name,
                                                from_number.getText().toString(), to_number.getText().toString());

                                        new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                .setMessage(messageResult)
                                                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(OpenStreetMapActivity.this, StartActivity.class);
                                                        startActivity(intent);
                                                    }
                                                })
                                                .show();
                                    } else {
                                        String message = (String) sendUrlMap.get("message");
                                        new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                .setMessage(message + getString(R.string.next_try))
                                                .setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                    @SuppressLint("SuspiciousIndentation")
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(Intent.ACTION_DIAL);
                                                        intent.setData(Uri.parse("tel:0674443804"));
                                                        startActivity(intent);
                                                    }
                                                })
                                                .setNegativeButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                    }
                                                })
                                                .show();
                                    }


                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                })
                .show();

    }
    private void phoneNumberGeo() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);

        LayoutInflater inflater = this.getLayoutInflater();

        View view = inflater.inflate(R.layout.phone_verify_layout, null);

        builder.setView(view);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText phoneNumber = view.findViewById(R.id.phoneNumber);

//        String result = phoneNumber.getText().toString();
        builder.setTitle("Перевірка телефону")
                .setPositiveButton("Відправити", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                            Log.d("TAG", "onClick befor validate: ");
                            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                            boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                            Log.d("TAG", "onClick No validate: " + val);
                            if (val == false) {
                                Toast.makeText(OpenStreetMapActivity.this, getString(R.string.format_phone) , Toast.LENGTH_SHORT).show();
                                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());
                                OpenStreetMapActivity.this.finish();

                            } else {
                                StartActivity.insertRecordsUser(phoneNumber.getText().toString());

                                try {
                                    String urlCost = getTaxiUrlSearchGeo(locationStart.getLatitude(),
                                            locationStart.getLongitude(),
                                            to,
                                            to_number.getText().toString(), "orderSearchGeo");
                                    Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

                                    String message = (String) sendUrlMapCost.get("message");
                                    String orderCost = (String) sendUrlMapCost.get("order_cost");
                                    Log.d("TAG", "onClick orderCost : " + orderCost);

                                    if (orderCost.equals("0")) {

                                        Toast.makeText(map.getContext(), em + message, Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(map.getContext(), OpenStreetMapActivity.class);
                                        map.getContext().startActivity(intent);
                                    }
                                    if (!orderCost.equals("0")) {

                                        if (!MainActivity.verifyOrder) {
                                            Toast.makeText(map.getContext(), co + orderCost + fb, Toast.LENGTH_SHORT).show();
                                        } else {
                                            String orderWeb = (String) sendUrlMapCost.get("order_cost");

                                            if (!orderWeb.equals("0")) {

                                                String to_name;
                                                if (Objects.equals(sendUrlMapCost.get("routefrom"), sendUrlMapCost.get("routeto"))) {
                                                    to_name = onc;
                                                    StartActivity.insertRecordsOrders((String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routefrom"),
                                                            (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("routefromnumber"));
                                                } else {
                                                    to_name = (String) sendUrlMapCost.get("routeto") + " " + (String) sendUrlMapCost.get("to_number");
                                                    StartActivity.insertRecordsOrders((String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routeto"),
                                                            (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("to_number"));
                                                }
                                                String messageResult = tm +
                                                        FromAdressString  + tom +
                                                        to_name + "." +
                                                        co + orderWeb + UAH;
                                                Log.d("TAG", "onClick messageResult: " + messageResult);

                                                new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                                                        .setMessage(messageResult)
                                                        .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(map.getContext(), StartActivity.class);
                                                                map.getContext().startActivity(intent);
                                                            }
                                                        })
                                                        .show();
                                            } else {
                                                message = (String) sendUrlMapCost.get("message");
                                                new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                                                        .setMessage(message + ntr)
                                                        .setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                            @SuppressLint("SuspiciousIndentation")
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                intent.setData(Uri.parse("tel:0674443804"));
                                                                map.getContext().startActivity(intent);
                                                            }
                                                        })
                                                        .setNegativeButton(tra, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(map.getContext(), StartActivity.class);
                                                                map.getContext().startActivity(intent);
                                                            }
                                                        })
                                                        .show();
                                            }
                                        }

                                    }


                                } catch (
                                        MalformedURLException | InterruptedException |
                                        JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                        }
                    }
                })
                .show();

    }
    private static void phoneNumberMarkers() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme);

        builder.setView(view);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText phoneNumber = view.findViewById(R.id.phoneNumber);
        Log.d("TAG", "phoneNumber " + phoneNumber.getText());

//        String result = phoneNumber.getText().toString();
        builder.setTitle("Перевірка телефону")
                .setPositiveButton("Відправити", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                            Log.d("TAG", "onClick befor validate: ");
                            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                            boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                            Log.d("TAG", "onClick No validate: " + val);
                            if (val == false) {
                                Toast.makeText(map.getContext(), fp , Toast.LENGTH_SHORT).show();
                                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());
                            } else {
                                StartActivity.insertRecordsUser(phoneNumber.getText().toString());
                                try {

                                    String urlCost = getTaxiUrlSearchMarkers(startPoint.getLatitude(), startPoint.getLongitude(),
                                            endPoint.getLatitude(), endPoint.getLongitude(), "orderSearchMarkers");

                                    Log.d("TAG", "onClick urlCost: " + urlCost);

                                    Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

                                    String message = (String) sendUrlMapCost.get("message");
                                    String orderCost = (String) sendUrlMapCost.get("order_cost");
                                    Log.d("TAG", "onClick orderCost : " + orderCost);

                                    if (orderCost.equals("0")) {

                                        Toast.makeText(map.getContext(), em + message, Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(map.getContext(), OpenStreetMapActivity.class);
                                        map.getContext().startActivity(intent);
                                    }
                                    if (!orderCost.equals("0")) {

                                        if (!MainActivity.verifyOrder) {
                                            Toast.makeText(map.getContext(), co + orderCost + fb, Toast.LENGTH_SHORT).show();
                                        } else {
                                            String orderWeb = (String) sendUrlMapCost.get("order_cost");

                                            if (!orderWeb.equals("0")) {

                                                String to_name;
                                                if (Objects.equals(sendUrlMapCost.get("routefrom"), sendUrlMapCost.get("routeto"))) {
                                                    to_name = onc;
                                                    StartActivity.insertRecordsOrders((String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routefrom"),
                                                            (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("routefromnumber"));
                                                } else {
                                                    to_name = (String) sendUrlMapCost.get("routeto") + " " + (String) sendUrlMapCost.get("to_number");
                                                    StartActivity.insertRecordsOrders((String) sendUrlMapCost.get("routefrom"), (String) sendUrlMapCost.get("routeto"),
                                                            (String) sendUrlMapCost.get("routefromnumber"), (String) sendUrlMapCost.get("to_number"));
                                                }
                                                String messageResult = tm +
                                                        FromAdressString + " " + tom +
                                                        to_name + "." +
                                                        co + orderWeb + UAH;
                                                Log.d("TAG", "onClick messageResult: " + messageResult);

                                                new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                                                        .setMessage(messageResult)
                                                        .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(map.getContext(), StartActivity.class);
                                                                map.getContext().startActivity(intent);
                                                            }
                                                        })
                                                        .show();
                                            } else {
                                                message = (String) sendUrlMapCost.get("message");
                                                new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                                                        .setMessage(message + ntr)
                                                        .setPositiveButton(hlp, new DialogInterface.OnClickListener() {
                                                            @SuppressLint("SuspiciousIndentation")
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                intent.setData(Uri.parse("tel:0674443804"));
                                                                map.getContext().startActivity(intent);
                                                            }
                                                        })
                                                        .setNegativeButton(tra, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(map.getContext(), StartActivity.class);
                                                                map.getContext().startActivity(intent);
                                                            }
                                                        })
                                                        .show();
                                            }
                                        }

                                    }

                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }

                        }
                    }
                })
                .show();

    }
    private static void phoneNumberMarkersOfRoads() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme);

        builder.setView(view);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText phoneNumber = view.findViewById(R.id.phoneNumber);
        Log.d("TAG", "phoneNumber " + phoneNumber.getText());

//        String result = phoneNumber.getText().toString();
        builder.setTitle("Перевірка телефону")
                .setPositiveButton("Відправити", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                            Log.d("TAG", "onClick befor validate: ");
                            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                            boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                            Log.d("TAG", "onClick No validate: " + val);
                            if (val == false) {
                                Toast.makeText(map.getContext(), fp , Toast.LENGTH_SHORT).show();
                                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());
                            } else {
                                StartActivity.insertRecordsUser(phoneNumber.getText().toString());

                                try {

                                    String urlCost = getTaxiUrlSearchGeo(startPoint.getLatitude(), startPoint.getLongitude(),
                                            Double.toString(startLat),
                                            " ", "costSearchGeo");

                                    Log.d("TAG", "onClick urlCost 1111: " + urlCost);

                                    Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);


                                    String orderCost = (String) sendUrlMapCost.get("order_cost");

                                    if (!MainActivity.verifyOrder) {
                                        Toast.makeText(map.getContext(), co + orderCost + fb, Toast.LENGTH_SHORT).show();
                                    } else {
                                        String orderWeb = sendUrlMapCost.get("order_cost");


                                        String to_name = onc;
                                        StartActivity.insertRecordsOrders(sendUrlMapCost.get("routefrom"), sendUrlMapCost.get("routefrom"),
                                                sendUrlMapCost.get("routefromnumber"), sendUrlMapCost.get("routefromnumber"));

                                        String messageResult = tm +
                                                FromAdressString + tom +
                                                to_name + "." +
                                                co + orderWeb + UAH;
                                        Log.d("TAG", "onClick messageResult: " + messageResult);

                                        new MaterialAlertDialogBuilder(map.getContext(), R.style.AlertDialogTheme)
                                                .setMessage(messageResult)
                                                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(map.getContext(), OpenStreetMapActivity.class);
                                                        map.getContext().startActivity(intent);
                                                    }
                                                })
                                                .show();

                                    }


                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                })
                .show();

    }

}