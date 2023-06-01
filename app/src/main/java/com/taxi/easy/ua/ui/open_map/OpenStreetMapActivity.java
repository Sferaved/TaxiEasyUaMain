package com.taxi.easy.ua.ui.open_map;


import static com.taxi.easy.ua.ui.start.StartActivity.READ_CALL_PHONE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.maps.OrderJSONParser;
import com.taxi.easy.ua.ui.start.ResultSONParser;
import com.taxi.easy.ua.ui.start.StartActivity;

import org.json.JSONException;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.Map;
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
    private static double startLat;
    private static double startLan;


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



            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (connected()) {
                        try {
                            dialogFromTo();

                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });

            fab_call.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:0934066749"));
                    if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);
                    }
                    else startActivity(intent);
                }
            });
            fab_open_map.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        try {
                            dialogFromTo();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            dialogFromToGeo();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }
            });


            MapView map = (MapView) findViewById(R.id.map);
            map.setTileSource(TileSourceFactory.MAPNIK);

            map.setBuiltInZoomControls(true);
            map.setMultiTouchControls(true);

            mapController = map.getController();
            mapController.setZoom(14);
            map.setClickable(true);


            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

//            try {
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    dialogFromTo();
//                } else {
//                    dialogFromToGeo();
//                }
//
//            } catch (MalformedURLException e) {
//                throw new RuntimeException(e);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }

    }

    public void onResume() {
        super.onResume();
        Toast.makeText(this, "Позначте на карті місця посадки та призначення", Toast.LENGTH_SHORT).show();
//
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

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                1000 * 10, 10, locationListener);
        checkEnabled();
    }


    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            try {
                showLocation(location);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            locationStart = location;
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
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                Log.d("TAG", "onStatusChanged GPS_PROVIDER: Status: " + status);

            } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                Log.d("TAG", "onStatusChanged NETWORK_PROVIDER: Status: " + status);

            }
        }
    };

    private void showLocation(Location location) throws MalformedURLException, InterruptedException, JSONException {
        if (location == null)
            return;
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {

//            from_geo = formatLocation(location);
            startLat = location.getLatitude();
            startLan = location.getLongitude();
            dialogFromToGeo();

//            Toast.makeText(this, "showLocation GPS_PROVIDER: "  + from_geo, Toast.LENGTH_LONG).show();
        } else if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            Log.d("TAG", "showLocation NETWORK_PROVIDER: " + formatLocation(location));

            startLat = location.getLatitude();
            startLan = location.getLongitude();
            dialogFromToGeo();
//            from_geo = formatLocation(location);
//            Toast.makeText(this, "showLocation NETWORK_PROVIDER: "  + from_geo, Toast.LENGTH_LONG).show();

        }
    }

    @SuppressLint("DefaultLocale")
    private String formatLocation(Location location) {
        if (location == null)
            return "";

        return String.format("Coordinates: lat = %1$.4f, lon = %2$.4f, time = %3$tF %3$tT",
                location.getLatitude(), location.getLongitude(), new Date(location.getTime()));
    }

    @SuppressLint("SetTextI18n")
    private void checkEnabled() {
        Log.d("TAG", "checkEnabled: Enabled: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        Log.d("TAG", "checkEnabled: Enabled: " + locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

    }

    public void onClickLocationSettings(View view) {
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    };

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            Log.d("TAG", "checkPermission: " + new String[]{permission});
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
            Toast.makeText(this, "Перевірте інтернет-підключення або зателефонуйте оператору.", Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, "connected: " + hasConnect);
        return hasConnect;
    }
    private void dialogFromTo() throws MalformedURLException, InterruptedException {

        if(connected()) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.from_to_layout, null);
            builder.setView(view);



            from_number = view.findViewById(R.id.from_number);
            to_number = view.findViewById(R.id.to_number);


            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, arrayStreet);
            AutoCompleteTextView textViewFrom = view.findViewById(R.id.text_from);
            textViewFrom.setAdapter(adapter);

            textViewFrom.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(connected()) {
                        from = String.valueOf(adapter.getItem(position));
                        if (from.indexOf("/") != -1) {
                            from = from.substring(0,  from.indexOf("/"));
                        };
                        String url = "https://m.easy-order-taxi.site/api/android/autocompleteSearchComboHid/" + from;


                        Log.d("TAG", "onClick urlCost: " + url);
                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        String orderCost = (String) sendUrlMapCost.get("message");
                        Log.d("TAG", "onClick orderCost : " + orderCost);

                        if (orderCost.equals("1")) {
                            from_number.setVisibility(View.VISIBLE);
                            from_number.requestFocus();
                        }
                    }

                }
            });

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
                        String url = "https://m.easy-order-taxi.site/api/android/autocompleteSearchComboHid/" + to;


                        Log.d("TAG", "onClick urlCost: " + url);
                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        String orderCost = (String) sendUrlMapCost.get("message");
                        Log.d("TAG", "onClick orderCost : " + orderCost);

                        if (orderCost.equals("1")) {
                            to_number.setVisibility(View.VISIBLE);
                            to_number.requestFocus();
                        }
                    }

                }
            });


            builder.setMessage("Сформуйте маршрут")
                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(connected()) {
                                if (from != null) {
                                    if (to == null) {
                                        to = from;
                                        to_number.setText(from_number.getText());
                                    }
                                    try {
                                        String urlCost = getTaxiUrlSearch(from, from_number.getText().toString(), to, to_number.getText().toString(), "costSearch");

                                        Log.d("TAG", "onClick urlCost: " + urlCost);
                                        Map sendUrlMapCost = CostJSONParser.sendURL(urlCost);

                                        String orderCost = (String) sendUrlMapCost.get("order_cost");
                                        Log.d("TAG", "onClick orderCost : " + orderCost);

                                        if (!orderCost.equals("0")) {
                                            if(!MainActivity.verifyOrder) {
                                                Log.d(TAG, "dialogFromToOneRout FirebaseSignIn.verifyOrder: " + MainActivity.verifyOrder);
                                                Toast.makeText(OpenStreetMapActivity.this, "Вартість поїздки: " + orderCost + "грн. Вибачте, без перевірки Google-акаунту замовлення не можливе. Спробуйте знову.", Toast.LENGTH_SHORT).show();
                                            } else {

                                                new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                        .setMessage("Вартість поїздки: " + orderCost + "грн")
                                                        .setPositiveButton("Замовити", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {

                                                                if(connected()) {

                                                                    Cursor cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                                                    if (cursor.getCount() == 0) {

                                                                        getPhoneNumber();
                                                                        cursor.close();
                                                                    } else {
                                                                        String urlOrder = getTaxiUrlSearch(from, from_number.getText().toString(), to, to_number.getText().toString(), "orderSearch");
                                                                        if (cursor != null && !cursor.isClosed())
                                                                            cursor.close();
                                                                        try {
                                                                            Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);

                                                                            String orderWeb = (String) sendUrlMap.get("order_cost");
                                                                            if (!orderWeb.equals("0")) {

                                                                                String from_name = (String) sendUrlMap.get("from_name");
                                                                                String to_name = (String) sendUrlMap.get("to_name");
                                                                                if (from_name.equals(to_name)) {
                                                                                    messageResult = "Дякуемо за замовлення зі " +
                                                                                            from_name + " " + from_number.getText() + " " + " по місту." +
                                                                                            " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";


                                                                                } else {
                                                                                    messageResult = "Дякуемо за замовлення зі " +
                                                                                            from_name + " " + from_number.getText() + " " + " до " +
                                                                                            to_name + " " + to_number.getText() + "." +
                                                                                            " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";
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
                                                                                        .setMessage(message +
                                                                                                " Спробуйте ще або зателефонуйте оператору.")
                                                                                        .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                                                            @Override
                                                                                            public void onClick(DialogInterface dialog, int which) {
                                                                                                Intent intent = new Intent(Intent.ACTION_CALL);
                                                                                                intent.setData(Uri.parse("tel:0934066749"));
                                                                                                if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                                                                                                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                                                    checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);

                                                                                                }
                                                                                                else startActivity(intent);
                                                                                            }
                                                                                        })
                                                                                        .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                                                            @Override
                                                                                            public void onClick(DialogInterface dialog, int which) {

                                                                                            }
                                                                                        })
                                                                                        .show();
                                                                            }


                                                                        } catch (MalformedURLException e) {
                                                                            throw new RuntimeException(e);
                                                                        } catch (InterruptedException e) {
                                                                            throw new RuntimeException(e);
                                                                        } catch (JSONException e) {
                                                                            throw new RuntimeException(e);
                                                                        }
                                                                    }
                                                                    cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                                                    if (cursor.getCount() == 0) {
                                                                        Toast.makeText(OpenStreetMapActivity.this, "Формат вводу номера телефону: +380936665544. Спробуйте ще", Toast.LENGTH_SHORT).show();
                                                                        phoneNumber();
                                                                        cursor.close();
                                                                    }


                                                                }
                                                            }})
                                                        .setNegativeButton("Відміна", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
//
                                                        }
                                                        })
                                                        .show();
                                            }
                                        } else {

                                            String message = (String) sendUrlMapCost.get("message");
                                            new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                    .setMessage(message +
                                                            " Спробуйте ще або зателефонуйте оператору.")
                                                    .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            Intent intent = new Intent(Intent.ACTION_CALL);
                                                            intent.setData(Uri.parse("tel:0934066749"));
                                                            if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                                                                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);
                                                            }
                                                            else  startActivity(intent);
                                                        }
                                                    })
                                                    .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                        }
                                                    })
                                                    .show();
                                        }

                                    } catch (MalformedURLException e) {
                                        throw new RuntimeException(e);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else {
                                    Toast.makeText(OpenStreetMapActivity.this, "Вкажить місце відправлення", Toast.LENGTH_SHORT).show();

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
                    .show();
        }
    }
    private void dialogFromToGeo() throws MalformedURLException, InterruptedException, JSONException {

        if(connected()) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
            LayoutInflater inflater = this.getLayoutInflater();
            View view = inflater.inflate(R.layout.from_to_geo_layout, null);
            builder.setView(view);
            textViewFrom = view.findViewById(R.id.text_from);
            from_geo = startLat + " - " + startLan;


            String urlFrom = "https://m.easy-order-taxi.site/api/android/fromSearchGeo/" + startLat + "/" + startLan;
            Map sendUrlMap = FromJSONParser.sendURL(urlFrom);
            Log.d(TAG, "onClick sendUrlMap: " + sendUrlMap);
            String orderWeb = (String) sendUrlMap.get("order_cost");
            if (orderWeb.equals("100")) {
                from_geo = "Ви зараз тут: " + (String) sendUrlMap.get("route_address_from");
                textViewFrom.setText(from_geo);
            } else {
                Toast.makeText(this, (String) sendUrlMap.get("message"), Toast.LENGTH_SHORT).show();
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
                        String url = "https://m.easy-order-taxi.site/api/android/autocompleteSearchComboHid/" + to;


                        Log.d("TAG", "onClick urlCost: " + url);
                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
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


            builder.setMessage("Сформуйте маршрут")
                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(connected()) {
                                   try {
                                       if (to != null) {

                                           String urlCost = getTaxiUrlSearchGeo(locationStart.getLatitude(), locationStart.getLongitude(), to, to_number.getText().toString(), "costSearchGeo");

                                           Log.d("TAG", "onClick urlCost: " + urlCost);
                                           Map sendUrlMapCost = CostJSONParser.sendURL(urlCost);

                                           String orderCost = (String) sendUrlMapCost.get("order_cost");
                                           Log.d("TAG", "onClick orderCost : " + orderCost);

                                           if (!orderCost.equals("0")) {
                                               if (!MainActivity.verifyOrder) {
                                                   Log.d(TAG, "dialogFromToOneRout FirebaseSignIn.verifyOrder: " + MainActivity.verifyOrder);
                                                   Toast.makeText(OpenStreetMapActivity.this, "Вартість поїздки: " + orderCost + "грн. Вибачте, без перевірки Google-акаунту замовлення не можливе. Спробуйте знову.", Toast.LENGTH_SHORT).show();
                                               } else {
                                                   new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                           .setMessage("Вартість поїздки: " + orderCost + "грн")
                                                           .setPositiveButton("Замовити", new DialogInterface.OnClickListener() {
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
                                                                               Log.d(TAG, "onClick sendUrlMap: " + sendUrlMap);
                                                                               String orderWeb = (String) sendUrlMap.get("order_cost");
                                                                               if (!orderWeb.equals("0")) {
                                                                                   String from_name = (String) sendUrlMap.get("routefrom") + " " + (String) sendUrlMap.get("routefromnumber");
                                                                                   String to_name = (String) sendUrlMap.get("to_name");
                                                                                   messageResult = "Дякуемо за замовлення зі " +
                                                                                           from_name + " " + " до " +
                                                                                           to_name + " " + to_number.getText() + "." +
                                                                                           " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";

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
                                                                                           .setMessage(message +
                                                                                                   " Спробуйте ще або зателефонуйте оператору.")
                                                                                           .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                                                               @SuppressLint("SuspiciousIndentation")
                                                                                               @Override
                                                                                               public void onClick(DialogInterface dialog, int which) {
                                                                                                   Intent intent = new Intent(Intent.ACTION_CALL);
                                                                                                   intent.setData(Uri.parse("tel:0934066749"));
                                                                                                   if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                                                                                                           Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                                                       checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);

                                                                                                   } else
                                                                                                   startActivity(intent);
                                                                                               }
                                                                                           })
                                                                                           .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                                                               @Override
                                                                                               public void onClick(DialogInterface dialog, int which) {

                                                                                               }
                                                                                           })
                                                                                           .show();
                                                                               }


                                                                           } catch (
                                                                                   MalformedURLException e) {
                                                                               throw new RuntimeException(e);
                                                                           } catch (
                                                                                   InterruptedException e) {
                                                                               throw new RuntimeException(e);
                                                                           } catch (
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
                                           } else {

                                               String message = (String) sendUrlMapCost.get("message");
                                               new MaterialAlertDialogBuilder(OpenStreetMapActivity.this, R.style.AlertDialogTheme)
                                                       .setMessage(message +
                                                               " Спробуйте ще або зателефонуйте оператору.")
                                                       .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                           @SuppressLint("SuspiciousIndentation")
                                                           @Override
                                                           public void onClick(DialogInterface dialog, int which) {
                                                               Intent intent = new Intent(Intent.ACTION_CALL);
                                                               intent.setData(Uri.parse("tel:0934066749"));
                                                               if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                                                                       Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                   checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);
                                                               } else
                                                               startActivity(intent);
                                                           }
                                                       })
                                                       .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                           @Override
                                                           public void onClick(DialogInterface dialog, int which) {

                                                           }
                                                       })
                                                       .show();
                                           }
                                       }
                                       else {

                                           Toast.makeText(OpenStreetMapActivity.this, "Вкажіть місце призначення", Toast.LENGTH_SHORT).show();
                                       }
                                    } catch (MalformedURLException e) {
                                        throw new RuntimeException(e);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    } catch (JSONException e) {
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
                    .setNeutralButton("ЗМІНИТИ", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                                    try {
                                        dialogFromTo();
                                    } catch (MalformedURLException e) {
                                        throw new RuntimeException(e);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }

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
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber;
        }

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/api/android/" + urlAPI + "/" + parameters;
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

        String parameters = str_origin + "/" + str_dest + "/" + tarif;

        if(urlAPI.equals("orderSearchGeo")) {
            String phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber;
        }

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/api/android/" + urlAPI + "/" + parameters;
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
                Toast.makeText(this, "Формат вводу номера телефону: +380936665544. Спробуйте ще" , Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(OpenStreetMapActivity.this, "Формат вводу номера телефону: +380936665544. Спробуйте ще" , Toast.LENGTH_SHORT).show();
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
                                            messageResult = "Дякуемо за замовлення зі " +
                                                    from_name + " " + from_number.getText() + " " + " по місту." +
                                                    " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";

                                        } else {
                                            messageResult = "Дякуемо за замовлення зі " +
                                                    from_name + " " + from_number.getText() + " " + " до " +
                                                    to_name + " " + to_number.getText() + "." +
                                                    " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";
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
                                                .setMessage(message +
                                                        " Спробуйте ще або зателефонуйте оператору.")
                                                .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                    @SuppressLint("SuspiciousIndentation")
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(Intent.ACTION_CALL);
                                                        intent.setData(Uri.parse("tel:0934066749"));
                                                        if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                                                                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                            checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);

                                                        } else
                                                        startActivity(intent);
                                                    }
                                                })
                                                .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                    }
                                                })
                                                .show();
                                    }


                                } catch (MalformedURLException e) {
                                    throw new RuntimeException(e);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                } catch (JSONException e) {
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
                                Toast.makeText(OpenStreetMapActivity.this, "Формат вводу номера телефону: +380936665544. Спробуйте ще" , Toast.LENGTH_SHORT).show();
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
                                            messageResult = "Дякуемо за замовлення зі " +
                                                    from_name + " " + " до " +
                                                    to_name + " " + to_number.getText() + "." +
                                                    " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";

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
                                                    .setMessage(message +
                                                            " Спробуйте ще або зателефонуйте оператору.")
                                                    .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            Intent intent = new Intent(Intent.ACTION_CALL);
                                                            intent.setData(Uri.parse("tel:0934066749"));
                                                            if (ActivityCompat.checkSelfPermission(OpenStreetMapActivity.this,
                                                                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                checkPermission(Manifest.permission.CALL_PHONE, READ_CALL_PHONE);

                                                            } else
                                                                startActivity(intent);
                                                        }
                                                    })
                                                    .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                        }
                                                    })
                                                    .show();
                                        }


                                    } catch (
                                            MalformedURLException e) {
                                        throw new RuntimeException(e);
                                    } catch (
                                            InterruptedException e) {
                                        throw new RuntimeException(e);
                                    } catch (
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