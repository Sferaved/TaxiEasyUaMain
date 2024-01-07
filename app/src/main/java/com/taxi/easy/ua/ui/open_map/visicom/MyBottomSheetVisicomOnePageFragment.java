package com.taxi.easy.ua.ui.open_map.visicom;

import static android.content.Context.MODE_PRIVATE;

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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.cities.Kyiv.KyivRegion;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapVisicomActivity;
import com.taxi.easy.ua.ui.open_map.visicom.key_visicom.ApiCallback;
import com.taxi.easy.ua.ui.open_map.visicom.key_visicom.ApiClient;
import com.taxi.easy.ua.ui.open_map.visicom.key_visicom.ApiResponse;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.KeyboardUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MyBottomSheetVisicomOnePageFragment extends BottomSheetDialogFragment implements ApiCallback {

    private static final String TAG = "TAG_VIS_ADDR";

    AppCompatButton btn_ok, btn_no, btn_change;
    EditText fromEditAddress, toEditAddress;
    private ImageButton btn_clear_from, btn_clear_to;

    private final String apiUrl = "https://api.visicom.ua/data-api/5.0/uk/geocode.json";
    private String apiKey;
    private static List<double[]> coordinatesList;
    private static List<String[]> addresses;
    private final OkHttpClient client = new OkHttpClient();
    private String startPoint, finishPoint;
    ListView addressListView;

    private boolean verifyBuildingStart;
    private boolean verifyBuildingFinish;
    private TextView textGeoError, text_toError;
    private String fragmentInput;
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

    public MyBottomSheetVisicomOnePageFragment(String fragmentInput) {
        this.fragmentInput = fragmentInput;
    }

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.visicom_address_layout, container, false);
        setCancelable(false);
        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
        switch (stringList.get(1)) {
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
                citySearch = "Київ";
                kyivRegionArr = KyivRegion.city();
                break;
        }
        textGeoError = view.findViewById(R.id.textGeoError);
        text_toError = view.findViewById(R.id.text_toError);

        visicomKey(this);
        addressListView = view.findViewById(R.id.listAddress);

        btn_ok = view.findViewById(R.id.btn_ok);


        btn_no = view.findViewById(R.id.btn_no);
        btn_ok.setVisibility(View.INVISIBLE);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (verifyBuildingStart) {
                    textGeoError.setVisibility(View.VISIBLE);
                    textGeoError.setText(R.string.house_vis_mes);

                    fromEditAddress.requestFocus();
                    fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
                    KeyboardUtils.showKeyboard(getContext(), fromEditAddress);
                } else if (!verifyRoutStart) {
                    textGeoError.setVisibility(View.VISIBLE);
                    textGeoError.setText(R.string.rout_fin);

                    fromEditAddress.requestFocus();
                    fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
                    KeyboardUtils.showKeyboard(getContext(), fromEditAddress);
                }
                if (toEditAddress.getText().toString().equals(getString(R.string.on_city_tv))) {
                    verifyBuildingFinish = false;
                    verifyRoutFinish = true;
                }

                if (verifyBuildingFinish) {
                    text_toError.setVisibility(View.VISIBLE);
                    text_toError.setText(R.string.house_vis_mes);

                    toEditAddress.requestFocus();
                    toEditAddress.setSelection(toEditAddress.getText().toString().length());
                    KeyboardUtils.showKeyboard(getContext(), toEditAddress);
                } else if (!verifyRoutFinish) {
                    text_toError.setVisibility(View.VISIBLE);
                    text_toError.setText(R.string.rout_fin);

                    toEditAddress.requestFocus();
                    toEditAddress.setSelection(toEditAddress.getText().toString().length());
                    KeyboardUtils.showKeyboard(getContext(), toEditAddress);
                }
//                            Log.d(TAG, "onClick: verifyBuildingStart" + verifyBuildingStart);
//                            Log.d(TAG, "onClick: verifyBuildingFinish" + verifyBuildingFinish);
                if (!verifyBuildingStart && !verifyBuildingFinish && verifyRoutStart && verifyRoutFinish) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        visicomCost();
                        dismiss();
                    }
                }

            }
        });
        btn_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        fromEditAddress = view.findViewById(R.id.textGeo);
        switch (fragmentInput) {
            case "map":
                fromEditAddress.setText(GeoDialogVisicomFragment.geoText.getText().toString());
                break;
            case "home":
                fromEditAddress.setText(VisicomFragment.geoText.getText().toString());
                break;
        }


        fromEditAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String inputString = charSequence.toString();
                int charCount = inputString.length();

                if (charCount > 2) {
                    if (startPoint == null) {
                        performAddressSearch(inputString, "start");
                    } else if (!startPoint.equals(inputString)) {
                        performAddressSearch(inputString, "start");
                    }
                    textGeoError.setVisibility(View.GONE);
                }
                btn_clear_from.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        toEditAddress = view.findViewById(R.id.text_to);
        switch (fragmentInput) {
            case "map":
                toEditAddress.setText(GeoDialogVisicomFragment.textViewTo.getText().toString());

                break;
            case "home":
                toEditAddress.setText(VisicomFragment.textViewTo.getText().toString());
                break;
        }


        toEditAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Вызывается перед изменением текста
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Вызывается при изменении текста
                String inputString = charSequence.toString();
                int charCount = inputString.length();

                if (charCount > 2) {
                    if (finishPoint == null) {
                        performAddressSearch(inputString, "finish");
                    } else if (!finishPoint.equals(inputString)) {
                        performAddressSearch(inputString, "finish");
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
        btn_clear_from = view.findViewById(R.id.btn_clear_from);
        btn_clear_from.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fromEditAddress.setText("");
                btn_clear_from.setVisibility(View.INVISIBLE);
                textGeoError.setVisibility(View.GONE);
            }
        });
        btn_clear_to = view.findViewById(R.id.btn_clear_to);
        btn_clear_to.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toEditAddress.setText("");
                btn_clear_to.setVisibility(View.INVISIBLE);
                text_toError.setVisibility(View.GONE);
            }
        });

        verifyRoutStart = true;
        verifyRoutFinish = true;
        btn_change = view.findViewById(R.id.change);
        btn_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocationManager lm = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
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
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }  else  {
                    // Разрешения уже предоставлены, выполнить ваш код
                    if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                    }  else {
                        firstLocation();
                    }
                }
            }
        });
        return view;
    }

    private void firstLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
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

                    if (isAdded() && getActivity() != null) {
                        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
                        String api =  stringList.get(2);
                        String urlFrom = "https://m.easy-order-taxi.site/" + api + "/android/fromSearchGeo/" + latitude + "/" + longitude;
                        Map sendUrlFrom = null;
                        try {
                            sendUrlFrom = FromJSONParser.sendURL(urlFrom);

                        } catch (MalformedURLException | InterruptedException |
                                 JSONException e) {
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                            bottomSheetDialogFragment.show(getParentFragmentManager(), bottomSheetDialogFragment.getTag());
                        }
                        assert sendUrlFrom != null;
                        String FromAdressString = (String) sendUrlFrom.get("route_address_from");
                        if (FromAdressString != null) {
                            if (FromAdressString.equals("Точка на карте")) {
                                FromAdressString = getString(R.string.startPoint);
                            }
                        }
                        updateMyPosition(latitude, longitude, FromAdressString, requireActivity());
                        fromEditAddress.setText(FromAdressString);
                        assert FromAdressString != null;
                        fromEditAddress.setSelection(FromAdressString.length());
                        btn_clear_from.setVisibility(View.VISIBLE);
                        switch (fragmentInput) {
                            case "map":
                                GeoDialogVisicomFragment.geoText.setText(FromAdressString);
                                break;
                            case "home":
                                VisicomFragment.geoText.setText(FromAdressString);
                                break;
                        }

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
                            SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
            }
        };
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
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
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Показываем объяснение пользователю, почему мы запрашиваем разрешение
            // Можно использовать диалоговое окно или другой пользовательский интерфейс
            MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment();
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    private void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(requireActivity(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{permission}, requestCode);

        }
    }
    @Override
    public void onResume() {
        super.onResume();

        if (fromEditAddress.getText().toString().equals("")) {

            btn_clear_from.setVisibility(View.INVISIBLE);
            fromEditAddress.requestFocus();

            fromEditAddress.post(new Runnable() {
                @Override
                public void run() {
                    KeyboardUtils.showKeyboard(requireContext(), fromEditAddress);
                }
            });
        } else if (toEditAddress.getText().toString().equals("")) {
            toEditAddress.requestFocus();
            btn_clear_to.setVisibility(View.INVISIBLE);

            toEditAddress.post(new Runnable() {
                @Override
                public void run() {
                    KeyboardUtils.showKeyboard(requireContext(), toEditAddress);
                }
            });

        }
    }

    private void performAddressSearch(String inputText, String point) {
        try {
            String url = apiUrl;
            if (point.equals("start")) {
                verifyBuildingStart = false;
            } else {
                verifyBuildingFinish = false;
            }

            if (!inputText.substring(3).contains(", ")) {

                url = url + "?categories=adr_street&text=" + inputText + "&key=" + apiKey;
                if (point.equals("start")) {
                    verifyBuildingStart = true;
                } else {
                    verifyBuildingFinish = true;
                }
            } else {
                // Если ", " присутствует после первых четырех символов, то выполняем вторую строку
                Log.d(TAG, "performAddressSearch: inputText1111 " + inputText);
                String number = numbers(inputText);


                Log.d(TAG, "performAddressSearch: inputText" + inputTextBuild());
                inputText = inputTextBuild() + ", " + number;

                url = url + "?categories=adr_address&text=" + inputText + "&key=" + apiKey;

                Log.d(TAG, "performAddressSearch: 6666666666 " + url);
                if (point.equals("start")) {
                    verifyBuildingStart = false;
                } else {
                    verifyBuildingFinish = false;
                }
            }

            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Log.d(TAG, "performAddressSearch: " + url);
            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(okhttp3.Call call, okhttp3.Response response) {
                    try {
                        String responseData = response.body().string();
                        Log.d(TAG, "onResponse: " + responseData);
                        processAddressData(responseData, point);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            // Log the exception or display an error message
        }
    }

    private String inputTextBuild() {

        String[] selectedAddress = addresses.get(positionChecked);
        Log.d(TAG, "inputTextBuild: " + Arrays.toString(selectedAddress));
        // Получение элементов отдельно
        String name = selectedAddress[1];
        zone = selectedAddress[2];
        String settlement = selectedAddress[3];
        String result = settlement + ", " + name;

        return result;

    }

    @SuppressLint("ResourceType")
    private void processAddressData(String responseData, String point) {
        try {
            JSONObject jsonResponse = new JSONObject(responseData);
            JSONArray features = jsonResponse.getJSONArray("features");
            Log.d(TAG, "processAddressData: features" + features);
            addresses = new ArrayList<>();
            coordinatesList = new ArrayList<>(); // Список для хранения координат

            for (int i = 0; i < features.length(); i++) {
                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");

                JSONObject geoCentroid = features.getJSONObject(i).getJSONObject("geo_centroid");

                if (!properties.getString("country_code").equals("ua")) {
                    Log.d(TAG, "processAddressData: Skipped address - Country is not Україна");
                    continue;
                }

                if (properties.getString("categories").equals("adr_street")) {

                    String settlement = properties.optString("settlement", "").toLowerCase();
                    String city = citySearch.toLowerCase();
                    String address;

                    if (settlement.contains(city)) {
                        if (properties.has("zone")) {
                            address = String.format("%s %s (%s), ",
                                    properties.getString("type"),
                                    properties.getString("name"),
                                    properties.getString("zone"));
                            addresses.add(new String[]{
                                    address,
                                    properties.getString("name"),
                                    properties.getString("zone"),
                                    properties.getString("settlement"),
                            });
                        } else {
                            address = String.format("%s %s, ",
                                    properties.getString("type"),
                                    properties.getString("name"));
                            addresses.add(new String[]{
                                    address,
                                    properties.getString("name"),
                                    "",
                                    properties.getString("settlement"),
                            });
                        }

                        double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                        double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

                        coordinatesList.add(new double[]{longitude, latitude});
                    }

                    // Проверка по Киевской области

                    if (citySearch.equals("Київ")) {
                        if (checkWordInArray(properties.getString("settlement"), kyivRegionArr)) {
                            address = String.format("%s %s (%s), ",
                                    properties.getString("type"),
                                    properties.getString("name"),
                                    properties.getString("settlement"));


                            addresses.add(new String[]{
                                    address,
                                    properties.getString("name"),
                                    "",
                                    properties.getString("settlement"),
                            });
                            double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                            double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

                            coordinatesList.add(new double[]{longitude, latitude});
                        }
                    }

                }
                if (properties.getString("categories").equals("adr_address")) {
                    String settlement = properties.optString("settlement", "").toLowerCase();
                    String city = citySearch.toLowerCase();
                    String address;

                    if (settlement.contains(city)) {
                        Log.d(TAG, "processAddressData: properties ййй" + properties);
                        if (properties.has("zone")) {
                            // Получение элементов отдельно

                            Log.d(TAG, "processAddressData: zone" + zone);
                            if (properties.getString("zone").equals(zone)) {
                                address = String.format("%s %s %s, %s, %s %s",

                                        properties.getString("street_type"),
                                        properties.getString("street"),
                                        properties.getString("name"),
                                        properties.getString("zone"),
                                        properties.getString("settlement_type"),
                                        properties.getString("settlement"));
                                addresses.add(new String[]{
                                        address,
                                        "",
                                        "",
                                        "",
                                });

                            }

                        } else {
                            address = String.format("%s %s, %s, %s %s",

                                    properties.getString("street_type"),
                                    properties.getString("street"),
                                    properties.getString("name"),
                                    properties.getString("settlement_type"),
                                    properties.getString("settlement"));
                            addresses.add(new String[]{
                                    address,
                                    "",
                                    "",
                                    "",
                            });
                        }


                        double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                        double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);
                        Log.d(TAG, "processAddressData: latitude longitude" + latitude + " " + longitude);

                        coordinatesList.add(new double[]{longitude, latitude});
                    }
                    // Проверка по Киевской области

                    if (citySearch.equals("Київ")) {
                        Log.d(TAG, "processAddressData:citySearch " + citySearch);
                        if (checkWordInArray(properties.getString("settlement"), kyivRegionArr)) {
                            address = String.format("%s %s %s, %s %s ",
                                    properties.getString("street_type"),
                                    properties.getString("street"),
                                    properties.getString("name"),
                                    properties.getString("settlement_type"),
                                    properties.getString("settlement"));

                            Log.d(TAG, "processAddressData: address" + address);
                            addresses.add(new String[]{
                                    address,
                                    "",
                                    "",
                                    "",
                            });
                            double longitude = geoCentroid.getJSONArray("coordinates").getDouble(0);
                            double latitude = geoCentroid.getJSONArray("coordinates").getDouble(1);

                            coordinatesList.add(new double[]{longitude, latitude});
                        }
                    }
                }
            }

            addresses.add(new String[]{
                    getString(R.string.address_on_map),
                    "",
                    "",
                    "",
            });

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


                    ArrayAdapter<String> addressAdapter = new ArrayAdapter<>(requireActivity(), R.layout.custom_list_item, addressesList);

                    addressListView.setVisibility(View.VISIBLE);
                    addressListView.setAdapter(addressAdapter);
                    addressListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    addressListView.setItemChecked(0, true);
                    if (point.equals("start")) {
                        verifyRoutStart = false;
                    } else if (point.equals("finish")) {
                        verifyRoutFinish = false;
                    }
                    addressListView.setOnItemClickListener((parent, viewC, position, id) -> {
                        positionChecked = position;
                        startMarker = "ok";
                        finishMarker = "no";
                        if (point.equals("start")) {
                            fromEditAddress.requestFocus();
                            fromEditAddress.setSelection(fromEditAddress.getText().toString().length());
                            KeyboardUtils.showKeyboard(getContext(), fromEditAddress);
                            messageInfo = getString(R.string.drag_marker_bottom);


                        } else if (point.equals("finish")) {
                            toEditAddress.requestFocus();
                            toEditAddress.setSelection(toEditAddress.getText().toString().length());
                            KeyboardUtils.showKeyboard(getContext(), toEditAddress);
                            messageInfo = getString(R.string.two_point_mes);
                            startMarker = "no";
                            finishMarker = "ok";
                        }

                        if (position == addressesList.size() - 1) {
                            Intent intent = new Intent(requireActivity(), OpenStreetMapVisicomActivity.class);
                           
                            intent.putExtra("startMarker", startMarker);
                            intent.putExtra("finishMarker", finishMarker);

                            startActivity(intent);
                            dismiss();
                        } else {
                            double[] coordinates = coordinatesList.get(position);

                            if (point.equals("start")) {
                                startPoint = addressesList.get(position);
                                fromEditAddress.setText(startPoint);
                                fromEditAddress.setSelection(startPoint.length());

                                if (!verifyBuildingStart) {
                                    verifyRoutStart = true;
                                    List<String> settings = new ArrayList<>();


                                    switch (fragmentInput) {
                                        case "map":
                                            settings.add(Double.toString(coordinates[1]));
                                            settings.add(Double.toString(coordinates[0]));
                                            if (toEditAddress.getText().toString().equals(getString(R.string.on_city_tv))) {
                                                settings.add(Double.toString(coordinates[1]));
                                                settings.add(Double.toString(coordinates[0]));
                                                settings.add(addressesList.get(position));
                                                settings.add(getString(R.string.on_city_tv));
                                            } else {
                                                Log.d(TAG, "processAddressData: OpenStreetMapVisicomActivity.finishLan" + OpenStreetMapVisicomActivity.finishLan);
                                                if (OpenStreetMapVisicomActivity.finishLan != 0) {
                                                    settings.add(String.valueOf(OpenStreetMapVisicomActivity.finishLat));
                                                    settings.add(String.valueOf(OpenStreetMapVisicomActivity.finishLan));
                                                    settings.add(addressesList.get(position));
                                                    settings.add(toEditAddress.getText().toString());
                                                } else {
                                                    settings.add(Double.toString(coordinates[1]));
                                                    settings.add(Double.toString(coordinates[0]));
                                                    settings.add(addressesList.get(position));
                                                    settings.add(getString(R.string.on_city_tv));
                                                }

                                            }
                                            Log.d(TAG, "processAddressData:22222 " + settings);
                                            updateRoutMarker(settings);

                                            GeoDialogVisicomFragment.geoText.setText(startPoint);
                                            OpenStreetMapVisicomActivity.startLat = coordinates[1];
                                            OpenStreetMapVisicomActivity.startLan = coordinates[0];
                                            OpenStreetMapVisicomActivity.FromAdressString = addressesList.get(position);


                                            Log.d(TAG, "settings: " + settings);
                                            fromEditAddress.setSelection(addressesList.get(position).length());

                                            double startLat = coordinates[1];
                                            double startLan = coordinates[0];
                                            if (OpenStreetMapVisicomActivity.m != null) {
                                                OpenStreetMapVisicomActivity.map.getOverlays().remove(OpenStreetMapVisicomActivity.m);
                                                OpenStreetMapVisicomActivity.map.invalidate();
                                                OpenStreetMapVisicomActivity.m = null;
                                            }

                                            GeoPoint initialGeoPoint = new GeoPoint(startLat - 0.0009, startLan);
                                            OpenStreetMapVisicomActivity.map.getController().setCenter(initialGeoPoint);

                                            OpenStreetMapVisicomActivity.setMarker(startLat, startLan, startPoint, requireContext());
                                            OpenStreetMapVisicomActivity.map.invalidate();

                                            break;
                                        case "home":
                                            settings.add(Double.toString(coordinates[1]));
                                            settings.add(Double.toString(coordinates[0]));
                                            if (toEditAddress.getText().toString().equals(getString(R.string.on_city_tv))) {
                                                settings.add(Double.toString(coordinates[1]));
                                                settings.add(Double.toString(coordinates[0]));
                                                settings.add(addressesList.get(position));
                                                settings.add(getString(R.string.on_city_tv));
                                            } else {
                                                String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                                                SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
                                            VisicomFragment.geoText.setText(startPoint);

                                            break;
                                    }

                                }
                            } else if (point.equals("finish")) {
                                finishPoint = addressesList.get(position);
                                toEditAddress.setText(finishPoint);
                                toEditAddress.setSelection(finishPoint.length());
                                btn_clear_to.setVisibility(View.VISIBLE);
                                if (!verifyBuildingFinish) {
                                    verifyRoutFinish = true;
                                    List<String> settings = new ArrayList<>();
                                    switch (fragmentInput) {
                                        case "map":
                                            GeoDialogVisicomFragment.textViewTo.setText(addressesList.get(position));
                                            GeoDialogVisicomFragment.btn_clear_to.setVisibility(View.VISIBLE);
                                            if (!fromEditAddress.getText().toString().equals("")) {
                                                String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                                                SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                                                Cursor cursor = database.rawQuery(query, null);

                                                cursor.moveToFirst();

                                                // Получите значения полей из первой записи


                                                @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                                                @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                                                cursor.close();
                                                database.close();

                                                settings.add(Double.toString(coordinates[1]));
                                                settings.add(Double.toString(coordinates[0]));
                                                settings.add(String.valueOf(toLatitude));
                                                settings.add(String.valueOf(toLongitude));

                                                settings.add(fromEditAddress.getText().toString());
                                                settings.add(addressesList.get(position));

                                                updateRoutMarker(settings);
                                            }
                                            GeoPoint endPoint = new GeoPoint(coordinates[1], coordinates[0]);
                                            OpenStreetMapVisicomActivity.endPoint = endPoint;
                                            OpenStreetMapVisicomActivity.ToAdressString = finishPoint;
                                            showRoutMap(endPoint);

                                            break;
                                        case "home":
                                            VisicomFragment.textViewTo.setText(addressesList.get(position));
                                            VisicomFragment.btn_clear_to.setVisibility(View.VISIBLE);
                                            if (!fromEditAddress.getText().toString().equals("")) {
                                                String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                                                SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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

                                                settings.add(fromEditAddress.getText().toString());
                                                settings.add(addressesList.get(position));
                                                updateRoutMarker(settings);
                                            }
                                            break;
                                    }


                                    Log.d(TAG, "settings: " + settings);
                                    toEditAddress.setSelection(addressesList.get(position).length());

                                }
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


    private void showRoutMap(GeoPoint geoPoint) {
        if (OpenStreetMapVisicomActivity.marker != null) {
            OpenStreetMapVisicomActivity.map.getOverlays().remove(OpenStreetMapVisicomActivity.marker);
            OpenStreetMapVisicomActivity.map.invalidate();
            OpenStreetMapVisicomActivity.marker = null;
        }


        OpenStreetMapVisicomActivity.marker = new Marker(OpenStreetMapVisicomActivity.map);
        OpenStreetMapVisicomActivity.marker.setPosition(geoPoint);
        OpenStreetMapVisicomActivity.marker.setTextLabelBackgroundColor(
                Color.TRANSPARENT
        );
        OpenStreetMapVisicomActivity.marker.setTextLabelForegroundColor(
                Color.RED
        );
        OpenStreetMapVisicomActivity.marker.setTextLabelFontSize(40);
        OpenStreetMapVisicomActivity.marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        String unuString = new String(Character.toChars(0x1F449));

        OpenStreetMapVisicomActivity.marker.setTitle("2." + unuString + OpenStreetMapVisicomActivity.ToAdressString);

        @SuppressLint("UseCompatLoadingForDrawables") Drawable originalDrawable = requireActivity().getResources().getDrawable(R.drawable.marker_green);
        int width = 48;
        int height = 48;
        Bitmap bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) originalDrawable).getBitmap(), width, height, false);

        // Создайте новый Drawable из уменьшенного изображения
        Drawable scaledDrawable = new BitmapDrawable(requireActivity().getResources(), bitmap);
        OpenStreetMapVisicomActivity.marker.setIcon(scaledDrawable);

        OpenStreetMapVisicomActivity.marker.showInfoWindow();

        OpenStreetMapVisicomActivity.map.getOverlays().add(OpenStreetMapVisicomActivity.marker);

        GeoPoint initialGeoPoint = new GeoPoint(geoPoint.getLatitude() - 0.01, geoPoint.getLongitude());
        OpenStreetMapVisicomActivity.map.getController().setCenter(initialGeoPoint);
        double newZoomLevel = Double.parseDouble(logCursor(MainActivity.TABLE_POSITION_INFO, requireContext()).get(4));
        OpenStreetMapVisicomActivity.mapController.setZoom(newZoomLevel);

        OpenStreetMapVisicomActivity.map.invalidate();

        GeoPoint startPoint = new GeoPoint(OpenStreetMapVisicomActivity.startLat, OpenStreetMapVisicomActivity.startLan);

        OpenStreetMapVisicomActivity.showRout(startPoint, OpenStreetMapVisicomActivity.endPoint);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void visicomCost() {
        String urlCost = getTaxiUrlSearchMarkers("costSearchMarkers", requireActivity());
        Log.d(TAG, "visicomCost: " + urlCost);
        Map<String, String> sendUrlMapCost = null;
        try {
            sendUrlMapCost = CostJSONParser.sendURL(urlCost);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        String message = sendUrlMapCost.get("message");
        String orderCost = sendUrlMapCost.get("order_cost");
        Log.d(TAG, "startCost: orderCost " + orderCost);

        assert orderCost != null;
        if (orderCost.equals("0")) {
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        } else {
            String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, requireContext()).get(3);
            long discountInt = Integer.parseInt(discountText);
            long discount;
            switch (fragmentInput) {
                case "map":
                    GeoDialogVisicomFragment.geoText.setText(fromEditAddress.getText().toString());
                    GeoDialogVisicomFragment.firstCost = Long.parseLong(orderCost);
                    discount = GeoDialogVisicomFragment.firstCost * discountInt / 100;
                    GeoDialogVisicomFragment.firstCost = GeoDialogVisicomFragment.firstCost + discount;
                    updateAddCost(String.valueOf(discount));
                    GeoDialogVisicomFragment.text_view_cost.setText(String.valueOf(GeoDialogVisicomFragment.firstCost));
                    GeoDialogVisicomFragment.MIN_COST_VALUE = (long) (GeoDialogVisicomFragment.firstCost * 0.6);
                    GeoDialogVisicomFragment.firstCostForMin = GeoDialogVisicomFragment.firstCost;
                    break;
                case "home":
                    VisicomFragment.geoText.setText(fromEditAddress.getText().toString());
                    VisicomFragment.firstCost = Long.parseLong(orderCost);
                    discount = VisicomFragment.firstCost * discountInt / 100;
                    VisicomFragment.firstCost = VisicomFragment.firstCost + discount;
                    updateAddCost(String.valueOf(discount));
                    VisicomFragment.text_view_cost.setText(String.valueOf(VisicomFragment.firstCost));
                    VisicomFragment.MIN_COST_VALUE = (long) (VisicomFragment.firstCost * 0.6);
                    VisicomFragment.firstCostForMin = VisicomFragment.firstCost;


                    VisicomFragment.geoText.setVisibility(View.VISIBLE);
                    VisicomFragment.btn_clear_from.setVisibility(View.VISIBLE);
                    VisicomFragment.textwhere.setVisibility(View.VISIBLE);
                    VisicomFragment.num2.setVisibility(View.VISIBLE);
                    VisicomFragment.textViewTo.setVisibility(View.VISIBLE);
                    VisicomFragment.btn_clear_to.setVisibility(View.VISIBLE);
                    VisicomFragment.btnAdd.setVisibility(View.VISIBLE);
                    VisicomFragment.buttonBonus.setVisibility(View.VISIBLE);
                    VisicomFragment.btn_minus.setVisibility(View.VISIBLE);
                    VisicomFragment.text_view_cost.setVisibility(View.VISIBLE);
                    VisicomFragment.btn_plus.setVisibility(View.VISIBLE);
                    VisicomFragment.btnOrder.setVisibility(View.VISIBLE);

                    VisicomFragment.btn_clear_from_text.setVisibility(View.GONE);
                    break;
            }

        }



    }

    private void updateAddCost(String addCost) {
        ContentValues cv = new ContentValues();
        Log.d("TAG", "updateAddCost: addCost" + addCost);
        cv.put("addCost", addCost);

        // обновляем по id
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[]{"1"});
        database.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("Range")
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Log.d(TAG, "getTaxiUrlSearchMarkers: " + urlAPI);

//        List<String> stringList11 = logCursor(MainActivity.ROUT_MARKER, requireContext());
//        Log.d(TAG, "getTaxiUrlSearchMarkers:stringList " + stringList11.toString());
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

        Log.d(TAG, "getTaxiUrlSearchMarkers: originLatitude" + originLatitude);
        Log.d(TAG, "getTaxiUrlSearchMarkers: originLongitude" + originLongitude);
        Log.d(TAG, "getTaxiUrlSearchMarkers: toLatitude" + toLatitude);
        Log.d(TAG, "getTaxiUrlSearchMarkers: toLongitude" + toLongitude);

        // Заменяем символ '/' в строках
        start = start.replace("/", "|");
        finish = finish.replace("/", "|");

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
        String tarif = stringListInfo.get(2);
        String payment_type = stringListInfo.get(4);
        String addCost = stringListInfo.get(5);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);

        if (urlAPI.equals("costSearchMarkers")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail + "*" + payment_type;
        }
        if (urlAPI.equals("orderSearchMarkersVisicom")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);


            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail + "*" + payment_type + "/" + addCost + "/"
                    + time + "/" + comment + "/" + date + "/" + start + "/" + finish;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[]{"1"});

        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i < services.size() - 1; i++) {
            if (services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if (servicesVer) {
            for (int i = 0; i < OpenStreetMapVisicomActivity.arrayServiceCode().length; i++) {
                if (services.get(i + 1).equals("1")) {
                    servicesChecked.add(OpenStreetMapVisicomActivity.arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if (servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Log.d(TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }
        List<String> listCity = logCursor(MainActivity.CITY_INFO, requireActivity());
        String city = listCity.get(1);
        String api = listCity.get(2);
        String url = "https://m.easy-order-taxi.site/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city + "/" + context.getString(R.string.application);

        database.close();

        return url;
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
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[]{"1"});
        database.close();
    }

    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
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
        assert c != null;
        c.close();
        return list;
    }
    private void visicomKey(final ApiCallback callback) {
        ApiClient.getVisicomKeyInfo(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse != null) {
                        String keyVisicom = apiResponse.getKeyVisicom();
                        Log.d("ApiResponseMapbox", "keyVisicom: " + keyVisicom);

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
}

