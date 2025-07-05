package com.taxi.easy.ua.utils.city;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.getCurrentActivity;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.ip.ApiServiceCountry;
import com.taxi.easy.ua.utils.ip.CountryResponse;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CityFinder {
    private static final String TAG = "CityFinder";
    Context context;
    String countryState;
    String baseUrl;
    String pay_method;
    String phoneNumber;

    double startLat;
    double startLan;
    String position;

    public CityFinder() {
        // Пустой конструктор без аргументов
    }

    public CityFinder (
            Context context
    ) {
        this.context = context;
    }

    public CityFinder (
            Context context,
            double startLat,
            double startLan,
            String position
    ) {
        this.context = context;
        this.startLat = startLat;
        this.startLan = startLan;
        this.position = position;
    }

    public void findCity(double latitude, double longitude) {
        CityApiService apiService = RetrofitClient.getClient().create(CityApiService.class);
        Call<CityResponse> call = apiService.findCity(latitude, longitude);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<CityResponse> call, @NonNull Response<CityResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String city = response.body().getCity();
                    Log.d(TAG, "City: " + city);

                    cityVerify(city);
                } else {
                    Log.e(TAG, "Request failed: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                Log.e(TAG, "Error: " + t.getMessage(), t);
            }
        });
    }
    
    private void cityVerify (String city) {
        String cityResult;
        switch (city) {
            case "city_kiev":
                cityResult = "Kyiv City";
                break;
            case "city_cherkassy":
                cityResult = "Cherkasy Oblast";
                break;
            case "city_odessa":
                cityResult = "Odessa";
                break;
            case "city_zaporizhzhia":
                cityResult = "Zaporizhzhia";
                break;
            case "city_dnipro":
                cityResult = "Dnipropetrovsk Oblast";
                break;
            case "city_lviv":
                cityResult = "Lviv";
                break;
            case "city_ivano_frankivsk":
                cityResult = "Ivano_frankivsk";
                break;
            case "city_vinnytsia":
                cityResult = "Vinnytsia";
                break;
            case "city_poltava":
                cityResult = "Poltava";
                break;
            case "city_sumy":
                cityResult = "Sumy";
                break;
            case "city_kharkiv":
                cityResult = "Kharkiv";
                break;
            case "city_chernihiv":
                cityResult = "Chernihiv";
                break;
            case "city_rivne":
                cityResult = "Rivne";
                break;
            case "city_ternopil":
                cityResult = "Ternopil";
                break;
            case "city_khmelnytskyi":
                cityResult = "Khmelnytskyi";
                break;
            case "city_zakarpattya":
                cityResult = "Zakarpattya";
                break;
            case "city_zhytomyr":
                cityResult = "Zhytomyr";
                break;
            case "city_kropyvnytskyi":
                cityResult = "Kropyvnytskyi";
                break;
            case "city_mykolaiv":
                cityResult = "Mykolaiv";
                break;
            case "city_chernivtsi":
                cityResult = "Сhernivtsi";
                break;
            case "city_lutsk":
                cityResult = "Lutsk";
                break;
            default:
                cityResult = "all";
        }
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        city = stringList.get(1);

//        if(!city.equals(cityResult)) {
            sharedPreferencesHelperMain.saveValue("setStatusX", false);
            Log.d(TAG, "City: " + city);
            Log.d(TAG, "cityResult: " + cityResult);

            String newTitle;


            String Kyiv_City_phone = "tel:0674443804";
            String Dnipropetrovsk_Oblast_phone = "tel:0667257070";
            String Odessa_phone = "tel:0737257070";
            String Zaporizhzhia_phone = "tel:0687257070";
            String Cherkasy_Oblast_phone = "tel:0962294243";

            String cityMenu;

            switch (cityResult){
                case "Kyiv City":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_kyiv);
                    countryState = "UA";
                    break;
                case "Dnipropetrovsk Oblast":

                    phoneNumber = Dnipropetrovsk_Oblast_phone;
                    cityMenu = context.getString(R.string.city_dnipro);
                    countryState = "UA";
                    break;
                case "Odessa":

                    phoneNumber = Odessa_phone;
                    cityMenu = context.getString(R.string.city_odessa);
                    countryState = "UA";
                    break;
                case "Zaporizhzhia":

                    phoneNumber = Zaporizhzhia_phone;
                    cityMenu = context.getString(R.string.city_zaporizhzhia);
                    countryState = "UA";
                    break;
                case "Cherkasy Oblast":

                    phoneNumber = Cherkasy_Oblast_phone;
                    cityMenu = context.getString(R.string.city_cherkassy);
                    countryState = "UA";
                    break;
                case "Lviv":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_lviv);
                    countryState = "UA";
                    break;
                case "Ivano_frankivsk":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_ivano_frankivsk);
                    countryState = "UA";
                    break;
                case "Vinnytsia":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_vinnytsia);
                    countryState = "UA";
                    break;
                case "Poltava":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_poltava);
                    countryState = "UA";
                    break;
                case "Sumy":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_sumy);
                    countryState = "UA";
                    break;
                case "Kharkiv":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_kharkiv);
                    countryState = "UA";
                    break;
                case "Chernihiv":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_chernihiv);
                    countryState = "UA";
                    break;
                case "Rivne":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_rivne);
                    countryState = "UA";
                    break;
                case "Ternopil":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_ternopil);
                    countryState = "UA";
                    break;
                case "Khmelnytskyi":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_khmelnytskyi);
                    countryState = "UA";
                    break;
                case "Zakarpattya":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_zakarpattya);
                    countryState = "UA";
                    break;
                case "Zhytomyr":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_zhytomyr);
                    countryState = "UA";
                    break;
                case "Kropyvnytskyi":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_kropyvnytskyi);
                    countryState = "UA";
                    break;
                case "Mykolaiv":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_mykolaiv);
                    countryState = "UA";
                    break;
                case "Chernivtsi":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_chernivtsi);
                    countryState = "UA";
                    break;
                case "Lutsk":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_chernivtsi);
                    countryState = "UA";
                    break;
                case "OdessaTest":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = "Test";
                    countryState = "UA";
                    break;
                case "foreign countries":

                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.foreign_countries);
                    break;
                default:
                    phoneNumber = Kyiv_City_phone;
                    getPublicIPAddress();
                    cityMenu = context.getString(R.string.city_kyiv);
                    countryState = "UA";
                    break;
            }

            newTitle =  context.getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);

            sharedPreferencesHelperMain.saveValue("countryState", countryState);

            Logger.d(context, TAG, "onItemClick: pay_method" + pay_method);

            updateMyPosition(
                    cityResult,
                    startLat,
                    startLan,
                    position
            );

//        }

    }
    private void updateMyPosition(
            String city,
            double startLat,
            double startLan,
            String position
    ) {

        Logger.d(context, TAG, "updateMyPosition:city "+ city);

        switch (city){
            case "Dnipropetrovsk Oblast":
            case "Odessa":
            case "Zaporizhzhia":
            case "Cherkasy Oblast":
            case "Kyiv City":
            case "Lviv":
            case "Ivano_frankivsk":
            case "Vinnytsia":
            case "Poltava":
            case "Sumy":
            case "Kharkiv":
            case "Chernihiv":
            case "Rivne":
            case "Ternopil":
            case "Khmelnytskyi":
            case "Zakarpattya":
            case "Zhytomyr":
            case "Kropyvnytskyi":
            case "Mykolaiv":
            case "Chernivtsi":
            case "Lutsk":
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://m.easy-order-taxi.site");
                break;
            case "OdessaTest":
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://t.easy-order-taxi.site");
                break;
            default:
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://m.easy-order-taxi.site");
                city = "foreign countries";
        }

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String cityOld = stringList.get(1);

        if(!city.equals(cityOld)) {

            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

            ContentValues cv = new ContentValues();

            cv.put("city", city);
            cv.put("phone", phoneNumber);
            database.update(MainActivity.CITY_INFO, cv, "id = ?", new String[]{"1"});

            cv = new ContentValues();
            cv.put("startLat", startLat);
            cv.put("startLan", startLan);
            cv.put("position", position);
            database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                    new String[]{"1"});

            cv = new ContentValues();
            cv.put("tarif", " ");
            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                    new String[]{"1"});

            cv = new ContentValues();
            cv.put("payment_type", "nal_payment");

            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                    new String[]{"1"});
            database.close();
            sharedPreferencesHelperMain.saveValue("time", "no_time");
            sharedPreferencesHelperMain.saveValue("date", "no_date");
            sharedPreferencesHelperMain.saveValue("comment", "no_comment");
            sharedPreferencesHelperMain.saveValue("tarif", " ");
        }
        List<String> settings = new ArrayList<>();

        settings.add(Double.toString(startLat));
        settings.add(Double.toString(startLan));
        settings.add(Double.toString(startLat));
        settings.add(Double.toString(startLan));
        settings.add(position);
        settings.add("");

        updateRoutMarker(settings);
        clearTABLE_SERVICE_INFO();

        sharedPreferencesHelperMain.saveValue("CityCheckActivity", "run");


        NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_visicom, true)
                .build());

    }
    private void clearTABLE_SERVICE_INFO () {
        String[] arrayServiceCode = DataArr.arrayServiceCode();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        for (int i = 0; i < arrayServiceCode.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put(arrayServiceCode[i], "0");
            database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });
        }
        database.close();
    }

    private void updateRoutMarker(List<String> settings) {
        Logger.d(context, TAG, "updateRoutMarker: " + settings.toString());
        ContentValues cv = new ContentValues();

        cv.put("startLat", Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(0)));
        cv.put("to_lng", Double.parseDouble(settings.get(1)));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[]{"1"});
        database.close();
    }



    public void getPublicIPAddress() {
        getCountryByIP();
    }

    private void getCountryByIP() {
        ApiServiceCountry apiService = com.taxi.easy.ua.utils.ip.RetrofitClient.getClient().create(ApiServiceCountry.class);
        Call<CountryResponse> call = apiService.getCountryByIP("ipAddress");
        call.enqueue(new Callback<CountryResponse>() {
            @Override
            public void onResponse(@NonNull Call<CountryResponse> call, @NonNull Response<CountryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CountryResponse countryResponse = response.body();
                    Logger.d(context, TAG, "onResponse:countryResponse.getCountry(); " + countryResponse.getCountry());
                    countryState = countryResponse.getCountry();
                } else {
                    countryState = "UA";
                }
                sharedPreferencesHelperMain.saveValue("countryState", countryState);
            }

            @Override
            public void onFailure(@NonNull Call<CountryResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Error: " + t.getMessage());
                FirebaseCrashlytics.getInstance().recordException(t);
                VisicomFragment.progressBar.setVisibility(View.GONE);;
                sharedPreferencesHelperMain.saveValue("countryState", "UA");
            }
        });
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = db.query(table, null, null, null, null, null, null);
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
        db.close();
        return list;
    }

}

