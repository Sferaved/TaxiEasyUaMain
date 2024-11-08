package com.taxi.easy.ua.cities.check;

import static com.taxi.easy.ua.MainActivity.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.cities.api.CityApiClient;
import com.taxi.easy.ua.cities.api.CityResponse;
import com.taxi.easy.ua.cities.api.CityResponseMerchantFondy;
import com.taxi.easy.ua.cities.api.CityService;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.fondy.callback.CallbackResponse;
import com.taxi.easy.ua.ui.fondy.callback.CallbackService;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.utils.ip.ApiServiceCountry;
import com.taxi.easy.ua.utils.ip.CountryResponse;
import com.taxi.easy.ua.utils.ip.RetrofitClient;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class CityCheckActivity extends AppCompatActivity {

    private static final String TAG = "CityCheckActivity";
    AppCompatButton btn_city_1;
    AppCompatButton btn_city_2;
    AppCompatButton btn_city_3;
    AppCompatButton btn_city_4;
    AppCompatButton btn_city_5;
    AppCompatButton btn_city_6;
    AppCompatButton btn_city_7;
    AppCompatButton btn_city_8;
    AppCompatButton btn_city_9;
    AppCompatButton btn_city_10;
    AppCompatButton btn_city_11;
    AppCompatButton btn_city_12;
    AppCompatButton btn_city_13;
    AppCompatButton btn_city_14;
    AppCompatButton btn_city_15;
    AppCompatButton btn_city_16;
    AppCompatButton btn_city_17;
    AppCompatButton btn_city_18;
    AppCompatButton btn_city_19;
    AppCompatButton btn_city_20;
    AppCompatButton btn_city_21;
    AppCompatButton btn_city_22;
    AppCompatButton btn_city_23;
    AppCompatButton btn_exit;

    String city;
    private String cityMenu;


    /**
     * Phone section
     */
    public static final String Kyiv_City_phone = "tel:0674443804";
    public static final String Dnipropetrovsk_Oblast_phone = "tel:0667257070";
    public static final String Odessa_phone = "tel:0737257070";
    public static final String Zaporizhzhia_phone = "tel:0687257070";
    public static final String Cherkasy_Oblast_phone = "tel:0962294243";
    String phoneNumber;

    String countryState;
    SharedPreferencesHelper sharedPreferencesHelper;


    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.city_activity_layout);
        sharedPreferencesHelper = new SharedPreferencesHelper(this);


        String message = getString(R.string.check_city);
        MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

        btn_city_1 = findViewById(R.id.btn_city_1);
        btn_city_2 = findViewById(R.id.btn_city_2);
        btn_city_3 = findViewById(R.id.btn_city_3);
        btn_city_4 = findViewById(R.id.btn_city_4);
        btn_city_5 = findViewById(R.id.btn_city_5);
        btn_city_6 = findViewById(R.id.btn_city_6);
        btn_city_7 = findViewById(R.id.btn_city_7);
        btn_city_8 = findViewById(R.id.btn_city_8);
        btn_city_9 = findViewById(R.id.btn_city_9);
        btn_city_10 = findViewById(R.id.btn_city_10);
        btn_city_11 = findViewById(R.id.btn_city_11);
        btn_city_12 = findViewById(R.id.btn_city_12);
        btn_city_13 = findViewById(R.id.btn_city_13);
        btn_city_14 = findViewById(R.id.btn_city_14);
        btn_city_15 = findViewById(R.id.btn_city_15);
        btn_city_16 = findViewById(R.id.btn_city_16);
        btn_city_17 = findViewById(R.id.btn_city_17);
        btn_city_18 = findViewById(R.id.btn_city_18);
        btn_city_19 = findViewById(R.id.btn_city_19);
        btn_city_20 = findViewById(R.id.btn_city_20);
        btn_city_21 = findViewById(R.id.btn_city_21);
        btn_city_22 = findViewById(R.id.btn_city_22);
        btn_city_23 = findViewById(R.id.btn_city_23);
        btn_exit = findViewById(R.id.btn_exit);

        btn_city_1.setText(R.string.city_kyiv); //
        btn_city_2.setText(R.string.city_dnipro); //
        btn_city_3.setText(R.string.city_odessa); //
        btn_city_4.setText(R.string.city_zaporizhzhia); //
        btn_city_5.setText(R.string.city_cherkassy); //
        btn_city_6.setText(R.string.city_lviv); //
        btn_city_7.setText(R.string.city_ivano_frankivsk); //
        btn_city_8.setText(R.string.city_vinnytsia); //
        btn_city_9.setText(R.string.city_poltava); //
        btn_city_10.setText(R.string.city_sumy); //
        btn_city_11.setText(R.string.city_kharkiv); //
        btn_city_12.setText(R.string.city_chernihiv); //
        btn_city_13.setText(R.string.city_rivne);//
        btn_city_14.setText(R.string.city_ternopil); //
        btn_city_15.setText(R.string.city_khmelnytskyi); //
        btn_city_16.setText(R.string.city_zakarpattya); //
        btn_city_17.setText(R.string.city_zhytomyr);//
        btn_city_18.setText(R.string.city_kropyvnytskyi);//
        btn_city_19.setText(R.string.city_mykolaiv); //
        btn_city_20.setText(R.string.city_chernivtsi); //
        btn_city_21.setText(R.string.city_lutsk); //
        btn_city_22.setText(R.string.test_city); //
        btn_city_23.setText(R.string.foreign_countries); //
        btn_exit.setText(R.string.action_exit);


        btn_city_1.setOnClickListener(view1 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Kyiv City";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_kyiv);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_2.setOnClickListener(view2 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Dnipropetrovsk Oblast";
            phoneNumber = Dnipropetrovsk_Oblast_phone;
            cityMenu = getString(R.string.city_dnipro);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_3.setOnClickListener(view3 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Odessa";
            phoneNumber = Odessa_phone;
            cityMenu = getString(R.string.city_odessa);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_4.setOnClickListener(view4 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Zaporizhzhia";
            phoneNumber = Zaporizhzhia_phone;
            cityMenu = getString(R.string.city_zaporizhzhia);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_5.setOnClickListener(view5 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Cherkasy Oblast";
            phoneNumber = Cherkasy_Oblast_phone;
            cityMenu = getString(R.string.city_cherkassy);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_6.setOnClickListener(view6 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Lviv";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_lviv);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
       btn_city_7.setOnClickListener(view7 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Ivano_frankivsk";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_ivano_frankivsk);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_8.setOnClickListener(view8 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Vinnytsia";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_vinnytsia);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });

        btn_city_9.setOnClickListener(view9 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Poltava";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_poltava);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_10.setOnClickListener(view10 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Sumy";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_sumy);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_11.setOnClickListener(view11 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Kharkiv";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_kharkiv);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_12.setOnClickListener(view12 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Chernihiv";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_chernihiv);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });

        btn_city_13.setOnClickListener(view13 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Rivne";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_rivne);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_14.setOnClickListener(view14 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Ternopil";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_ternopil);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_15.setOnClickListener(view15 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Khmelnytskyi";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_khmelnytskyi);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_16.setOnClickListener(view16 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Zakarpattya";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_zakarpattya);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_17.setOnClickListener(view17 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Zhytomyr";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_zhytomyr);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_18.setOnClickListener(view18 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Kropyvnytskyi";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_kropyvnytskyi);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_19.setOnClickListener(view19 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Mykolaiv";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_mykolaiv);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_20.setOnClickListener(view20 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Сhernivtsi";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_chernivtsi);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_21.setOnClickListener(view21 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "Lutsk";
            phoneNumber = Kyiv_City_phone;
            cityMenu = "Lutsk";
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_22.setOnClickListener(view21 -> {
            sharedPreferencesHelperMain.saveValue("countryState", "UA");
            city = "OdessaTest";
            phoneNumber = Kyiv_City_phone;
            cityMenu = "Test";
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_city_23.setOnClickListener(view22 -> {
            new Thread(this::getCountryByIP).start();
            city = "foreign countries";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.foreign_countries);
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            updateMyPosition();
        });
        btn_exit.setOnClickListener(view16 -> {
            closeApplication();
        });
    }


    private void updateMyPosition() {

        double startLat;
        double startLan;
        String position;
        Logger.d(getApplicationContext(), TAG, "updateMyPosition:city "+ city);

        switch (city) {
            case "Kyiv City":
                position = getString(R.string.pos_k);
                startLat = 50.451107;
                startLan = 30.524907;
                phoneNumber = Kyiv_City_phone; // Здесь также добавляем номер телефона
                break;
            case "Dnipropetrovsk Oblast":
                // Днепр
                position = getString(R.string.pos_d);
                startLat = 48.4647;
                startLan = 35.0462;
                phoneNumber = Dnipropetrovsk_Oblast_phone; // Укажите соответствующий номер телефона
                break;
            case "Odessa":
                position = getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                phoneNumber = Odessa_phone;
                break;
            case "Zaporizhzhia":
                position = getString(R.string.pos_z);
                startLat = 47.84015;
                startLan = 35.13634;
                phoneNumber = Zaporizhzhia_phone;
                break;
            case "Cherkasy Oblast":
                position = getString(R.string.pos_c);
                startLat = 49.44469;
                startLan = 32.05728;
                phoneNumber = Cherkasy_Oblast_phone;
                break;
            case "Lviv":
                position = getString(R.string.pos_l);
                startLat = 49.83993;
                startLan = 24.02973;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Ivano_frankivsk":
                position = getString(R.string.pos_if);
                startLat = 48.92005;
                startLan = 24.71067;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Vinnytsia":
                position = getString(R.string.pos_v);
                startLat = 49.23325;
                startLan = 28.46865;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Poltava":
                position = getString(R.string.pos_p);
                startLat = 49.59325;
                startLan = 34.54938;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Sumy":
                position = getString(R.string.pos_s);
                startLat = 50.90775;
                startLan = 34.79865;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Kharkiv":
                position = getString(R.string.pos_h);
                startLat = 49.99358;
                startLan = 36.23191;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Chernihiv":
                position = getString(R.string.pos_ch);
                startLat = 51.4933;
                startLan = 31.2972;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Rivne":
                position = getString(R.string.pos_r);
                startLat = 50.6198;
                startLan = 26.2406;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Ternopil":
                position = getString(R.string.pos_t);
                startLat = 49.54479;
                startLan = 25.5990;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Khmelnytskyi":
                position = getString(R.string.pos_kh);
                startLat = 49.41548;
                startLan = 27.00674;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;

            case "Zakarpattya":
                position = getString(R.string.pos_uz);
                startLat = 48.61913;
                startLan = 22.29475;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Zhytomyr":
                position = getString(R.string.pos_zt);
                startLat = 50.26801;
                startLan = 28.68026;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Kropyvnytskyi":
                position = getString(R.string.pos_kr);
                startLat = 48.51159;
                startLan = 32.26982;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Mykolaiv":
                position = getString(R.string.pos_m);
                startLat = 46.97498;
                startLan = 31.99378;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Сhernivtsi":
                position = getString(R.string.pos_chr);
                startLat = 48.29306;
                startLan = 25.93484;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Lutsk":
                position = getString(R.string.pos_ltk);
                startLat = 50.73968;
                startLan = 25.32400;
            phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
            break;
            case "OdessaTest":
                position = getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                phoneNumber = Kyiv_City_phone;
                break;

            default:
                position = getString(R.string.pos_f);
                startLat = 52.13472;
                startLan = 21.00424;
                phoneNumber = Kyiv_City_phone; // Номер телефона по умолчанию
                break;
        }

        cityMaxPay(city);
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        ContentValues cv = new ContentValues();

        cv.put("city", city);
        cv.put("phone", phoneNumber);
        database.update(MainActivity.CITY_INFO, cv, "id = ?", new String[]{"1"});

        cv = new ContentValues();
        cv.put("startLat", startLat);
        cv.put("startLan", startLan);
        cv.put("position", position);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                new String[] { "1" });

        cv = new ContentValues();
        cv.put("tarif", " ");
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });

        cv = new ContentValues();
        cv.put("payment_type", "nal_payment");

        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();

        List<String> settings = new ArrayList<>();

        settings.add(Double.toString(startLat));
        settings.add(Double.toString(startLan));
        settings.add(Double.toString(startLat));
        settings.add(Double.toString(startLan));
        settings.add(position);
        settings.add(position);

        updateRoutMarker(settings);

        String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
        sharedPreferencesHelperMain.saveValue("newTitle", newTitle);

    }

   
    private void updateRoutMarker(List<String> settings) {
        Logger.d(getApplicationContext(), TAG, "updateRoutMarker: " + settings.toString());
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
        sharedPreferencesHelper.saveValue("CityCheckActivity", "run");
        startActivity(new Intent(this, MainActivity.class));
    }


    private void cityMaxPay(String city) {
        CityService cityService = CityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponse> call = cityService.getMaxPayValues(city, getString(R.string.application));

        call.enqueue(new Callback<CityResponse>() {
            @Override
            public void onResponse(@NonNull Call<CityResponse> call, @NonNull Response<CityResponse> response) {
                if (response.isSuccessful()) {
                    CityResponse cityResponse = response.body();
                    if (cityResponse != null) {
                        int cardMaxPay = cityResponse.getCardMaxPay();
                        int bonusMaxPay = cityResponse.getBonusMaxPay();
                        String black_list = cityResponse.getBlack_list();

                        ContentValues cv = new ContentValues();
                        cv.put("card_max_pay", cardMaxPay);
                        cv.put("bonus_max_pay", bonusMaxPay);
                        sharedPreferencesHelperMain.saveValue("black_list", black_list);

                        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                new String[]{"1"});

                        database.close();




                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                    Logger.d(getApplicationContext(), TAG, "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                Logger.d(getApplicationContext(), TAG, "Failed. Error message: " + t.getMessage());
            }
        });
    }

    private void merchantFondy(String city, Context context) {
        CityService cityService = CityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponseMerchantFondy> call = cityService.getMerchantFondy(city);

        call.enqueue(new Callback<CityResponseMerchantFondy>() {
            @Override
            public void onResponse(@NonNull Call<CityResponseMerchantFondy> call, @NonNull Response<CityResponseMerchantFondy> response) {
                if (response.isSuccessful()) {
                    CityResponseMerchantFondy cityResponse = response.body();
                    Logger.d(getApplicationContext(), TAG, "onResponse: cityResponse" + cityResponse);
                    if (cityResponse != null) {
                        String merchant_fondy = cityResponse.getMerchantFondy();
                        String fondy_key_storage = cityResponse.getFondyKeyStorage();

                        ContentValues cv = new ContentValues();
                        cv.put("merchant_fondy", merchant_fondy);
                        cv.put("fondy_key_storage", fondy_key_storage);


                            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                    new String[]{"1"});

                            database.close();



                        Logger.d(getApplicationContext(), TAG, "onResponse: merchant_fondy" + merchant_fondy);
                        Logger.d(getApplicationContext(), TAG, "onResponse: fondy_key_storage" + fondy_key_storage);

                        if(merchant_fondy != null) {
                            getCardToken(getApplicationContext(), merchant_fondy);
                        }


                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                    Logger.d(getApplicationContext(), TAG, "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponseMerchantFondy> call, @NonNull Throwable t) {
                Logger.d(getApplicationContext(), TAG, "Failed. Error message: " + t.getMessage());
            }
        });
    }
    private void getCardToken(Context context, String merchant_fondy) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site") // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String baseUrl = retrofit.baseUrl().toString();

        Logger.d(getApplicationContext(), TAG, "Base URL: " + baseUrl);
        // Создайте сервис
        CallbackService service = retrofit.create(CallbackService.class);

        Logger.d(getApplicationContext(), TAG, "getCardTokenFondy: ");

        List<String> arrayList = logCursor(MainActivity.TABLE_USER_INFO);
        String email = arrayList.get(3);

            // Выполните запрос
            Call<CallbackResponse> call = service.handleCallback(email, "fondy", merchant_fondy);
            String requestUrl = call.request().toString();
            Logger.d(getApplicationContext(), TAG, "Request URL: " + requestUrl);

            call.enqueue(new Callback<CallbackResponse>() {
                @Override
                public void onResponse(@NonNull Call<CallbackResponse> call, @NonNull Response<CallbackResponse> response) {
                    Logger.d(getApplicationContext(), TAG, "onResponse: " + response.body());
                    if (response.isSuccessful()) {
                        CallbackResponse callbackResponse = response.body();
                        if (callbackResponse != null) {
                            List<CardInfo> cards = callbackResponse.getCards();
                            Logger.d(getApplicationContext(), TAG, "onResponse: cards" + cards);
                            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            // Очистка таблицы
                            database.delete(MainActivity.TABLE_FONDY_CARDS, "1", null);
                            if (cards != null && !cards.isEmpty()) {
                                for (CardInfo cardInfo : cards) {
                                    ContentValues cv = new ContentValues();
                                    String masked_card = cardInfo.getMasked_card(); // Маска карты
                                    String card_type = cardInfo.getCard_type(); // Тип карты
                                    String bank_name = cardInfo.getBank_name(); // Название банка
                                    String rectoken = cardInfo.getRectoken(); // Токен карты
                                    String merchantId = cardInfo.getMerchant(); // Токен карты

                                    Logger.d(getApplicationContext(), TAG, "onResponse: card_token: " + rectoken);

                                    cv.put("masked_card", masked_card);
                                    cv.put("card_type", card_type);
                                    cv.put("bank_name", bank_name);
                                    cv.put("rectoken", rectoken);
                                    cv.put("merchant", merchantId);
                                    cv.put("rectoken_check", "-1");
                                    database.insert(MainActivity.TABLE_FONDY_CARDS, null, cv);
                                }
                                // Выбираем минимальное значение ID из таблицы
                                Cursor cursor = database.rawQuery("SELECT MIN(id) FROM " + MainActivity.TABLE_FONDY_CARDS, null);
                                if (cursor != null && cursor.moveToFirst()) {
                                    // Получаем минимальное значение ID
                                    int minId = cursor.getInt(0);
                                    cursor.close();

                                    // Обновляем строку с минимальным ID
                                    ContentValues cv = new ContentValues();
                                    cv.put("rectoken_check", "1");
                                    database.update(MainActivity.TABLE_FONDY_CARDS, cv, "id = ?", new String[] { String.valueOf(minId) });
                                }
                                database.close();

                            }
                        }

                    } else {
                        // Обработка случаев, когда ответ не 200 OK
                    }
                }

                @Override
                public void onFailure(@NonNull Call<CallbackResponse> call, @NonNull Throwable t) {
                    // Обработка ошибки запроса
                    Logger.d(getApplicationContext(), TAG, "Failed. Error message: " + t.getMessage());
                    VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
                }
            });



    }
    public void resetRoutHome() {
        ContentValues cv = new ContentValues();

        cv.put("from_street", " ");
        cv.put("from_number", " ");
        cv.put("to_street", " ");
        cv.put("to_number", " ");

        // обновляем по id
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_HOME, cv, "id = ?",
                new String[]{"1"});
        database.close();
    }
    private void resetRoutMarker() {
        List<String> settings = new ArrayList<>();

            settings.add("0");
            settings.add("0");
            settings.add("0");
            settings.add("0");
            settings.add("");
            settings.add("");

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
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = db.query(table, null, null, null, null, null, null);
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
        db.close();
        return list;
    }

    private void getCountryByIP() {
        ApiServiceCountry apiService = RetrofitClient.getClient().create(ApiServiceCountry.class);
        Call<CountryResponse> call = apiService.getCountryByIP("ipAddress");
        call.enqueue(new Callback<CountryResponse>() {
            @Override
            public void onResponse(@NonNull Call<CountryResponse> call, @NonNull Response<CountryResponse> response) {
                if (response.isSuccessful()) {
                    CountryResponse countryResponse = response.body();
                    assert countryResponse != null;
                    Logger.d(getApplicationContext(), TAG, "onResponse:countryResponse.getCountry(); " + countryResponse.getCountry());
                    countryState = countryResponse.getCountry();
                } else {
                    countryState = "UA";
                }
                sharedPreferencesHelperMain.saveValue("countryState", countryState);
            }

            @Override
            public void onFailure(@NonNull Call<CountryResponse> call, @NonNull Throwable t) {
                Logger.d(getApplicationContext(), TAG, "Error: " + t.getMessage());
                VisicomFragment.progressBar.setVisibility(View.GONE);
                sharedPreferencesHelperMain.saveValue("countryState", "UA");
            }
        });
    }




    @Override
    protected void onPause() {
        super.onPause();
        // Проверяем, идет ли приложение в фон
        if (isFinishing()) {
            // Закрываем приложение полностью
            closeApplication();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Проверяем, идет ли приложение в фон
        if (isFinishing()) {
            // Закрываем приложение полностью
            closeApplication();
        }
    }

    @Override
    public void onBackPressed() {
        // Ничего не делать, блокируя действие кнопки "назад"
        super.onBackPressed();
        closeApplication();
    }

    private void closeApplication() {
        // Полный выход из приложения
        sharedPreferencesHelper.saveValue("CityCheckActivity", "**");
        finishAffinity();
        System.exit(0);
    }
}

