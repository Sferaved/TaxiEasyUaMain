package com.taxi.easy.ua.cities.check;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

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
import com.taxi.easy.ua.ui.home.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.ip.ApiServiceCountry;
import com.taxi.easy.ua.utils.ip.CountryResponse;
import com.taxi.easy.ua.utils.ip.RetrofitClient;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
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
    AppCompatButton btn_exit;

    ListView listView;
    String city;
    private String cityMenu;
    private String message;
    String pay_method;


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
        btn_exit = findViewById(R.id.btn_exit);


        btn_city_1.setText(R.string.Kyiv_city);
        btn_city_2.setText(R.string.Dnipro_city);
        btn_city_3.setText(R.string.Odessa);
        btn_city_4.setText(R.string.Zaporizhzhia);
        btn_city_5.setText(R.string.Cherkasy);
        btn_city_6.setText(R.string.test_city);
        btn_city_7.setText(R.string.foreign_countries);
        btn_exit.setText(R.string.action_exit);

        btn_city_1.setOnClickListener(view1 -> {
            sharedPreferencesHelper.saveValue("countryState", "UA");
            city = "Kyiv City";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.city_kyiv);
            updateMyPosition();
        });
        btn_city_2.setOnClickListener(view12 -> {
            sharedPreferencesHelper.saveValue("countryState", "UA");
            city = "Dnipropetrovsk Oblast";
            phoneNumber = Dnipropetrovsk_Oblast_phone;
            cityMenu = getString(R.string.city_dnipro);
            updateMyPosition();
        });
        btn_city_3.setOnClickListener(view13 -> {
            sharedPreferencesHelper.saveValue("countryState", "UA");
            city = "Odessa";
            phoneNumber = Odessa_phone;
            cityMenu = getString(R.string.city_odessa);
            updateMyPosition();
        });
        btn_city_4.setOnClickListener(view14 -> {
            sharedPreferencesHelper.saveValue("countryState", "UA");
            city = "Zaporizhzhia";
            phoneNumber = Zaporizhzhia_phone;
            cityMenu = getString(R.string.city_zaporizhzhia);
            updateMyPosition();
        });
        btn_city_5.setOnClickListener(view -> {
            sharedPreferencesHelper.saveValue("countryState", "UA");
            city = "Cherkasy Oblast";
            phoneNumber = Cherkasy_Oblast_phone;
            cityMenu = getString(R.string.city_cherkasy);
            updateMyPosition();
        });
        btn_city_6.setOnClickListener(view15 -> {
            sharedPreferencesHelper.saveValue("countryState", "UA");
            city = "OdessaTest";
            phoneNumber = Kyiv_City_phone;
            cityMenu = "Test";
            updateMyPosition();
        });
        btn_city_7.setOnClickListener(view16 -> {
            new Thread(this::getCountryByIP).start();
            city = "foreign countries";
            phoneNumber = Kyiv_City_phone;
            cityMenu = getString(R.string.foreign_countries);
            updateMyPosition();
        });
        btn_exit.setOnClickListener(view16 -> {
            closeApplication();
        });
    }

    private void getCardTokenWfp(String city) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site") // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        // Создайте сервис
        CallbackServiceWfp service = retrofit.create(CallbackServiceWfp.class);
        Logger.d(getApplicationContext(), TAG, "getCardTokenWfp: ");
        String email = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        // Выполните запрос
        Call<CallbackResponseWfp> call = service.handleCallbackWfp(
                getString(R.string.application),
                city,
                email,
                "wfp"
        );
        call.enqueue(new Callback<CallbackResponseWfp>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
                Logger.d(getApplicationContext(), TAG, "onResponse: " + response.body());
                if (response.isSuccessful()) {
                    CallbackResponseWfp callbackResponse = response.body();
                    if (callbackResponse != null) {
                        List<CardInfo> cards = callbackResponse.getCards();
                        Logger.d(getApplicationContext(), TAG, "onResponse: cards" + cards);
                        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.delete(MainActivity.TABLE_WFP_CARDS, "1", null);
                        if (cards != null && !cards.isEmpty()) {
                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты
                                String merchant = cardInfo.getMerchant(); // Токен карты

                                Logger.d(getApplicationContext(), TAG, "onResponse: card_token: " + rectoken);
                                ContentValues cv = new ContentValues();
                                cv.put("masked_card", masked_card);
                                cv.put("card_type", card_type);
                                cv.put("bank_name", bank_name);
                                cv.put("rectoken", rectoken);
                                cv.put("merchant", merchant);
                                cv.put("rectoken_check", "0");
                                database.insert(MainActivity.TABLE_WFP_CARDS, null, cv);
                            }
                            Cursor cursor = database.rawQuery("SELECT * FROM " + MainActivity.TABLE_WFP_CARDS + " ORDER BY id DESC LIMIT 1", null);
                            if (cursor != null && cursor.moveToFirst()) {
                                // Получаем значение ID последней записи
                                @SuppressLint("Range") int lastId = cursor.getInt(cursor.getColumnIndex("id"));
                                cursor.close();

                                // Обновляем строку с найденным ID
                                ContentValues cv = new ContentValues();
                                cv.put("rectoken_check", "1");
                                database.update(MainActivity.TABLE_WFP_CARDS, cv, "id = ?", new String[] { String.valueOf(lastId) });
                            }

                            database.close();
                        }
                    }

                } else {
                    // Обработка случаев, когда ответ не 200 OK
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
                 Logger.d(getApplicationContext(), TAG, "Failed. Error message: " + t.getMessage());                
            }
        });
    }



    private void updateMyPosition() {

        double startLat;
        double startLan;
        String position;
        Logger.d(getApplicationContext(), TAG, "updateMyPosition:city "+ city);

        switch (city){
            case "Kyiv City":
                position = getString(R.string.pos_k);
                startLat = 50.451107;
                startLan = 30.524907;
                break;
            case "Dnipropetrovsk Oblast":
                // Днепр
                position = getString(R.string.pos_d);
                startLat = 48.4647;
                startLan = 35.0462;
                break;
            case "Odessa":
                phoneNumber = Odessa_phone;
                position = getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                break;
            case "Zaporizhzhia":
                phoneNumber = Zaporizhzhia_phone;
                position = getString(R.string.pos_z);
                startLat = 47.84015;
                startLan = 35.13634;
                break;
            case "Cherkasy Oblast":
                phoneNumber = Cherkasy_Oblast_phone;
                position = getString(R.string.pos_c);
                startLat = 49.44469;
                startLan = 32.05728;
                break;
            case "OdessaTest":
                phoneNumber = Kyiv_City_phone;
                position = getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                break;
            default:
                phoneNumber = Kyiv_City_phone;
                position = getString(R.string.pos_f);
                startLat = 52.13472;
                startLan = 21.00424;
                break;
        }
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


    private void cityMaxPay(String city, Context context) {
        CityService cityService = CityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponse> call = cityService.getMaxPayValues(city);

        call.enqueue(new Callback<CityResponse>() {
            @Override
            public void onResponse(@NonNull Call<CityResponse> call, @NonNull Response<CityResponse> response) {
                if (response.isSuccessful()) {
                    CityResponse cityResponse = response.body();
                    if (cityResponse != null) {
                        int cardMaxPay = cityResponse.getCardMaxPay();
                        int bonusMaxPay = cityResponse.getBonusMaxPay();

                        ContentValues cv = new ContentValues();
                        cv.put("card_max_pay", cardMaxPay);
                        cv.put("bonus_max_pay", bonusMaxPay);

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
                sharedPreferencesHelper.saveValue("countryState", countryState);
            }

            @Override
            public void onFailure(@NonNull Call<CountryResponse> call, @NonNull Throwable t) {
                Logger.d(getApplicationContext(), TAG, "Error: " + t.getMessage());
                VisicomFragment.progressBar.setVisibility(View.GONE);;
                sharedPreferencesHelper.saveValue("countryState", "UA");
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

