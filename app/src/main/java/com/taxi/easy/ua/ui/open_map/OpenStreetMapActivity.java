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
import android.widget.CompoundButton;
import android.widget.EditText;
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
    MapView map = null;
    GeoPoint startPoint;
    static Switch gpsSwitch;
    AlertDialog progressDialog;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.open_street_map_layout);

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
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:0674443804"));
                if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);
                }
                if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                        Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    startActivity(intent);
                }

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
                    1000*10, 10, locationListener);
        } else if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    1000*10, 10, locationListener);
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
            progressDialog.dismiss();
            dialogFromToGeo();
        }
        else
        if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            progressDialog.dismiss();
            startLat = location.getLatitude();
            startLan = location.getLongitude();
            dialogFromToGeo();
        }

//        Toast.makeText(this, location.getProvider() + startLat + " - " + startLan, Toast.LENGTH_SHORT).show();
    }

    private void setMarker(double Lat, double Lan, String title) {
        Marker m = new Marker(map);
        m.setPosition(new GeoPoint(Lat, Lan));
        m.setTextLabelBackgroundColor(
                Color.TRANSPARENT
        );
        m.setTextLabelForegroundColor(
                Color.RED
        );
        m.setTextLabelFontSize(40);

        m.setAnchor(Marker.ANCHOR_TOP, Marker.ANCHOR_TOP);
        m.setTitle(title);
        map.getOverlays().add(m);
        map.invalidate();
    }

    private void showRout(GeoPoint startPoint, GeoPoint endPoint) {
        AsyncTask.execute(() -> {
            RoadManager roadManager = new OSRMRoadManager(this, System.getProperty("http.agent"));
            ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();

            waypoints.add(startPoint);

            waypoints.add(endPoint);
            Road road = roadManager.getRoad(waypoints);
            Polyline roadOverlay = RoadManager.buildRoadOverlay(road);
            map.getOverlays().add(roadOverlay);
            map.invalidate();
        });
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
    private boolean connected() {

        Boolean hasConnect = false;

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(
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
            Toast.makeText(this, getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, "connected: " + hasConnect);
        return hasConnect;
    }

    private void dialogFromToGeo() throws MalformedURLException, InterruptedException, JSONException {

        if(connected()) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.from_to_geo_layout, null);
            builder.setView(view);
            textViewFrom = view.findViewById(R.id.text_from);
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


            to_number = view.findViewById(R.id.to_number);
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


            builder.setMessage( getString(R.string.make_rout_message))
                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            if(connected()) {
                                try {
                                    if (to != null) {

                                        String urlCost = getTaxiUrlSearchGeo(locationStart.getLatitude(), locationStart.getLongitude(), to, to_number.getText().toString(), "costSearchGeo");

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
                                                                        String urlOrder = getTaxiUrlSearchGeo(locationStart.getLatitude(), locationStart.getLongitude(), to, to_number.getText().toString(), "orderSearchGeo");


                                                                        try {
                                                                            Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);
                                                                            Log.d(TAG, "(String) sendUrlMap.get(\"routefromnumber\") " + (String) sendUrlMap.get("routefromnumber"));

                                                                            String orderWeb = (String) sendUrlMap.get("order_cost");

                                                                            if (!orderWeb.equals("0")) {
                                                                                String from_name;
                                                                                String routefromnumber = " ";
                                                                                if(Objects.equals((String) sendUrlMap.get("routefromnumber"), null)) {
                                                                                    from_name = (String) sendUrlMap.get("routefrom") + " " + (String) sendUrlMap.get("routefromnumber");
                                                                                    routefromnumber = (String) sendUrlMap.get("routefromnumber");
                                                                                } else {
                                                                                    from_name = (String) sendUrlMap.get("routefrom");
                                                                                }
                                                                                String to_name = (String) sendUrlMap.get("to_name");
                                                                                messageResult = getString(R.string.thanks_message) +
                                                                                        from_name + " " + getString(R.string.to_message) +
                                                                                        to_name + " " + to_number.getText() + "." +
                                                                                        getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);

                                                                                StartActivity.insertRecordsOrders((String) sendUrlMap.get("routefrom"), to_name,
                                                                                        routefromnumber, to_number.getText().toString());

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
                                                                                        .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                                                            @SuppressLint("SuspiciousIndentation")
                                                                                            @Override
                                                                                            public void onClick(DialogInterface dialog, int which) {
                                                                                                Intent intent = new Intent(Intent.ACTION_CALL);
                                                                                                intent.setData(Uri.parse("tel:0674443804"));
                                                                                                if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                                                                                                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                                                    checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);

                                                                                                } else
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
                                    }
                                    else {

                                        Toast.makeText(OpenStreetMapActivity.this, getString(R.string.show_to_point), Toast.LENGTH_SHORT).show();
                                    }
                                } catch (MalformedURLException | InterruptedException |
                                         JSONException e) {
                                    throw new RuntimeException(e);
                                }

                            }
                        }
                    })
//                    .setNegativeButton("Мапа", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//
//                        }
//                    })
                    .setNeutralButton(getString(R.string.change), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(OpenStreetMapActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    })
                    .show();
        }
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
    private String getTaxiUrlSearchGeo(double originLatitude, double originLongitude, String to, String to_number, String urlAPI) {

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

    private void getPhoneNumber () {
        String mPhoneNumber;
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mPhoneNumber = tMgr.getLine1Number();

        if(mPhoneNumber != null) {
            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
            boolean val = Pattern.compile(PHONE_PATTERN).matcher(mPhoneNumber).matches();
            Log.d("TAG", "onClick No validate: " + val);
            if (val == false) {
                Toast.makeText(this, getString(R.string.format_phone) , Toast.LENGTH_SHORT).show();
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
                                                    from_name + " " + from_number.getText() + " " + getString(R.string.on_city) +
                                                    getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);

                                        } else {
                                            messageResult = getString(R.string.thanks_message) +
                                                    from_name + " " + from_number.getText() + " " + getString(R.string.to_message) +
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
                                                .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                    @SuppressLint("SuspiciousIndentation")
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(Intent.ACTION_CALL);
                                                        intent.setData(Uri.parse("tel:0674443804"));
                                                        if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                                                                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                            checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);

                                                        } else
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

                                String urlOrder = getTaxiUrlSearchGeo(locationStart.getLatitude(), locationStart.getLongitude(), to, to_number.getText().toString(), "orderSearchGeo");


                                try {
                                    Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);
                                    Log.d(TAG, "onClick sendUrlMap: " + sendUrlMap);
                                    String orderWeb = (String) sendUrlMap.get("order_cost");
                                    if (!orderWeb.equals("0")) {
                                        String from_name = (String) sendUrlMap.get("routefrom") + " " + (String) sendUrlMap.get("routefromnumber");
                                        String to_name = (String) sendUrlMap.get("to_name");
                                        messageResult = getString(R.string.thanks_message) +
                                                from_name + " " + getString(R.string.to_message) +
                                                to_name + " " + to_number.getText() + "." +
                                                getString(R.string.cost_of_order) + orderWeb + getString(R.string.UAH);

                                        StartActivity.insertRecordsOrders((String) sendUrlMap.get("routefrom"), to_name,
                                                (String) sendUrlMap.get("routefromnumber"), to_number.getText().toString());

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
                                                .setPositiveButton(getString(R.string.help_button), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(Intent.ACTION_CALL);
                                                        intent.setData(Uri.parse("tel:0674443804"));
                                                        if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                                                                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                            checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);

                                                        } else
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

}