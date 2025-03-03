package com.taxi.easy.ua.utils.bottom_sheet;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponse;
import com.taxi.easy.ua.ui.fondy.callback.CallbackResponse;
import com.taxi.easy.ua.ui.fondy.callback.CallbackService;
import com.taxi.easy.ua.ui.home.cities.api.CityApiClient;
import com.taxi.easy.ua.ui.home.cities.api.CityLastAddressResponse;
import com.taxi.easy.ua.ui.home.cities.api.CityResponse;
import com.taxi.easy.ua.ui.home.cities.api.CityResponseMerchantFondy;
import com.taxi.easy.ua.ui.home.cities.api.CityService;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.ip.ApiServiceCountry;
import com.taxi.easy.ua.utils.ip.CountryResponse;
import com.taxi.easy.ua.utils.ip.RetrofitClient;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyBottomSheetCityFragment extends BottomSheetDialogFragment {

    private static final String TAG = "MyBottomSheetCityFragment";
    private static final String BASE_URL = "https://api64.ipify.org";

    ListView listView;
    String city;
    private String cityMenu;
    private String message;
    String pay_method;
    FragmentManager fragmentManager;

    private List<RouteResponse> routeList;
    String[] array;
    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;

    public MyBottomSheetCityFragment() {
        // Пустой конструктор без аргументов
    }

    public MyBottomSheetCityFragment(String city, Context context) {

        this.city = city;
        this.context = context;
    }
    private final String[] cityCode = new String[]{
            "Kyiv City",
            "Dnipropetrovsk Oblast",
            "Odessa",
            "Zaporizhzhia",
            "Cherkasy Oblast",
            "Lviv",
            "Ivano_frankivsk",
            "Vinnytsia",
            "Poltava",
            "Sumy",
            "Kharkiv",
            "Chernihiv",
            "Rivne",
            "Ternopil",
            "Khmelnytskyi",
            "Zakarpattya",
            "Zhytomyr",
            "Kropyvnytskyi",
            "Mykolaiv",
            "Сhernivtsi",
            "Lutsk",
            "OdessaTest",
            "foreign countries"
    };

    int positionFirst;
    /**
     * Phone section
     */
    public static final String Kyiv_City_phone = "tel:0674443804";
    public static final String Dnipropetrovsk_Oblast_phone = "tel:0667257070";
    public static final String Odessa_phone = "tel:0737257070";
    public static final String Zaporizhzhia_phone = "tel:0687257070";
    public static final String Cherkasy_Oblast_phone = "tel:0962294243";
    String phoneNumber;
    String baseUrl;
    Context context;
    String countryState;
    String newTitle;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Nullable
    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cities_list_layout, container, false);

        fragmentManager = getParentFragmentManager();
        listView = view.findViewById(R.id.listViewBonus);

        String[] cityList = new String[]{
                context.getString(R.string.city_kyiv),
                context.getString(R.string.city_dnipro),
                context.getString(R.string.city_odessa),
                context.getString(R.string.city_zaporizhzhia),
                context.getString(R.string.city_cherkassy),
                context.getString(R.string.city_lviv),
                context.getString(R.string.city_ivano_frankivsk),
                context.getString(R.string.city_vinnytsia),
                context.getString(R.string.city_poltava),
                context.getString(R.string.city_sumy),
                context.getString(R.string.city_kharkiv),
                context.getString(R.string.city_chernihiv),
                context.getString(R.string.city_rivne),
                context.getString(R.string.city_ternopil),
                context.getString(R.string.city_khmelnytskyi),
                context.getString(R.string.city_zakarpattya),
                context.getString(R.string.city_zhytomyr),
                context.getString(R.string.city_kropyvnytskyi),
                context.getString(R.string.city_mykolaiv),
                context.getString(R.string.city_chernivtsi),
                context.getString(R.string.city_lutsk),
                context.getString(R.string.test_city),
                context.getString(R.string.foreign_countries)
        };



        databaseHelper = new DatabaseHelper(context);
        databaseHelperUid = new DatabaseHelperUid(context);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.services_adapter_layout, cityList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if(city == null) {
            city = "Kyiv City";
        }

        switch (city){
            case "Kyiv City":
                positionFirst = 0;
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_kyiv);
                break;
            case "Dnipropetrovsk Oblast":
                positionFirst = 1;
                phoneNumber = Dnipropetrovsk_Oblast_phone;
                cityMenu = context.getString(R.string.city_dnipro);
                break;
            case "Odessa":
                positionFirst = 2;
                phoneNumber = Odessa_phone;
                cityMenu = context.getString(R.string.city_odessa);
                break;
            case "Zaporizhzhia":
                positionFirst = 3;
                phoneNumber = Zaporizhzhia_phone;
                cityMenu = context.getString(R.string.city_zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                positionFirst = 4;
                phoneNumber = Cherkasy_Oblast_phone;
                cityMenu = context.getString(R.string.city_cherkassy);
                break;
            case "Lviv":
                positionFirst = 5;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_lviv);
                break;
            case "Ivano_frankivsk":
                positionFirst = 6;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_ivano_frankivsk);
                break;
            case "Vinnytsia":
                positionFirst = 7;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_vinnytsia);
                break;
            case "Poltava":
                positionFirst = 8;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_poltava);
                break;
            case "Sumy":
                positionFirst = 9;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_sumy);
                break;
            case "Kharkiv":
                positionFirst = 10;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_kharkiv);
                break;
            case "Chernihiv":
                positionFirst = 11;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_chernihiv);
                break;
            case "Rivne":
                positionFirst = 12;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_rivne);
                break;
            case "Ternopil":
                positionFirst = 13;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_ternopil);
                break;
            case "Khmelnytskyi":
                positionFirst = 14;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_khmelnytskyi);
                break;
            case "Zakarpattya":
                positionFirst = 15;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_zakarpattya);
                break;
            case "Zhytomyr":
                positionFirst = 16;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_zhytomyr);
                break;
            case "Kropyvnytskyi":
                positionFirst = 17;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_kropyvnytskyi);
                break;
            case "Mykolaiv":
                positionFirst = 18;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_mykolaiv);
                break;
            case "Сhernivtsi":
                positionFirst = 19;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_chernivtsi);
                break;
            case "Lutsk":
                positionFirst = 20;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_lutsk);
                break;
            case "OdessaTest":
                positionFirst = 21;
                phoneNumber = Kyiv_City_phone;
                cityMenu = "Test";
                break;
            default:
                positionFirst = 22;
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.foreign_countries);
        }
        newTitle =  context.getString(R.string.menu_city) + " " + cityMenu;
        sharedPreferencesHelperMain.saveValue("newTitle", newTitle);

        Logger.d(context, TAG, "onCreateView: city" + city);

//        lastAddressUser(city);

        listView.setItemChecked(positionFirst, true);

        int positionFirstOld = positionFirst;

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            positionFirst = position;
            switch (cityCode[positionFirst]){
                case "Kyiv City":positionFirst = 0;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_kyiv);
                    countryState = "UA";
                    break;
                case "Dnipropetrovsk Oblast":
                    positionFirst = 1;
                    phoneNumber = Dnipropetrovsk_Oblast_phone;
                    cityMenu = context.getString(R.string.city_dnipro);
                    countryState = "UA";
                    break;
                case "Odessa":
                    positionFirst = 2;
                    phoneNumber = Odessa_phone;
                    cityMenu = context.getString(R.string.city_odessa);
                    countryState = "UA";
                    break;
                case "Zaporizhzhia":
                    positionFirst = 3;
                    phoneNumber = Zaporizhzhia_phone;
                    cityMenu = context.getString(R.string.city_zaporizhzhia);
                    countryState = "UA";
                    break;
                case "Cherkasy Oblast":
                    positionFirst = 4;
                    phoneNumber = Cherkasy_Oblast_phone;
                    cityMenu = context.getString(R.string.city_cherkassy);
                    countryState = "UA";
                    break;
                case "Lviv":
                    positionFirst = 5;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_lviv);
                    countryState = "UA";
                    break;
                case "Ivano_frankivsk":
                    positionFirst = 6;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_ivano_frankivsk);
                    countryState = "UA";
                    break;
                case "Vinnytsia":
                    positionFirst = 7;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_vinnytsia);
                    countryState = "UA";
                    break;
                case "Poltava":
                    positionFirst = 8;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_poltava);
                    countryState = "UA";
                    break;
                case "Sumy":
                    positionFirst = 9;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_sumy);
                    countryState = "UA";
                    break;
                case "Kharkiv":
                    positionFirst = 10;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_kharkiv);
                    countryState = "UA";
                    break;
                case "Chernihiv":
                    positionFirst = 11;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_chernihiv);
                    countryState = "UA";
                    break;
                case "Rivne":
                    positionFirst = 12;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_rivne);
                    countryState = "UA";
                    break;
                case "Ternopil":
                    positionFirst = 13;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_ternopil);
                    countryState = "UA";
                    break;
                case "Khmelnytskyi":
                    positionFirst = 14;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_khmelnytskyi);
                    countryState = "UA";
                    break;
                case "Zakarpattya":
                    positionFirst = 15;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_zakarpattya);
                    countryState = "UA";
                    break;
                case "Zhytomyr":
                    positionFirst = 16;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_zhytomyr);
                    countryState = "UA";
                    break;
                case "Kropyvnytskyi":
                    positionFirst = 17;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_kropyvnytskyi);
                    countryState = "UA";
                    break;
                case "Mykolaiv":
                    positionFirst = 18;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_mykolaiv);
                    countryState = "UA";
                    break;
                case "Сhernivtsi":
                    positionFirst = 19;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_chernivtsi);
                    countryState = "UA";
                    break;
                case "Lutsk":
                    positionFirst = 20;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = getString(R.string.city_chernivtsi);
                    countryState = "UA";
                    break;
                case "OdessaTest":
                    positionFirst = 21;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = "Test";
                    countryState = "UA";
                    break;
                case "foreign countries":
                    positionFirst = 22;
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.foreign_countries);
                    break;
                default:
                    phoneNumber = Kyiv_City_phone;
                    positionFirst = 0;
                    cityMenu = context.getString(R.string.city_kyiv);
                    countryState = "UA";
                    break;
            }

            newTitle =  context.getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
             String cityCodeNew;
                if (positionFirst == 22) {
                    getPublicIPAddress();
                    cityCodeNew = cityCode[0];
                } else {
                    cityCodeNew = cityCode[positionFirst];
                    sharedPreferencesHelperMain.saveValue("countryState", countryState);
                }
            Logger.d(context, TAG, "onItemClick: pay_method" + pay_method);

            pay_system(cityCodeNew);

            lastAddressUser(cityCode[positionFirst]);

        });
        databaseHelper = new DatabaseHelper(context);
        databaseHelperUid = new DatabaseHelperUid(context);


        return view;
    }

    private void getCardTokenWfp(String city) {

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.delete(MainActivity.TABLE_WFP_CARDS, "1", null);
        database.close();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl) // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        // Создайте сервис
        CallbackServiceWfp service = retrofit.create(CallbackServiceWfp.class);
        Logger.d(context, TAG, "getCardTokenWfp: ");
        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        // Выполните запрос
        Call<CallbackResponseWfp> call = service.handleCallbackWfpCardsId(
                context.getString(R.string.application),
                city,
                email,
                "wfp"
        );
        call.enqueue(new Callback<CallbackResponseWfp>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
                Logger.d(context, TAG, "onResponse: " + response.body());
                if (response.isSuccessful()) {
                    CallbackResponseWfp callbackResponse = response.body();
                    if (callbackResponse != null) {
                        List<CardInfo> cards = callbackResponse.getCards();
                        Logger.d(context, TAG, "onResponse: cards" + cards);
                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        if (cards != null && !cards.isEmpty()) {
                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты
                                String merchant = cardInfo.getMerchant(); // Токен карты

                                Logger.d(context, TAG, "onResponse: card_token: " + rectoken);
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
                            if (cursor.moveToFirst()) {
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
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
                Logger.d(context, TAG, "Failed. Error message: " + t.getMessage());

            }
        });
    }

    private void pay_system(String city) {

        cityMaxPay(city);
        getCardTokenWfp(city);

//        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(baseUrl)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//
//        PayApi apiService = retrofit.create(PayApi.class);
//        Call<ResponsePaySystem> call = apiService.getPaySystem();
//        call.enqueue(new Callback<ResponsePaySystem>() {
//            @Override
//            public void onResponse(@NonNull Call<ResponsePaySystem> call, @NonNull Response<ResponsePaySystem> response) {
//                if (response.isSuccessful()) {
//                    // Обработка успешного ответа
//                    ResponsePaySystem responsePaySystem = response.body();
//                    assert responsePaySystem != null;
//                    String paymentCode = responsePaySystem.getPay_system();
//
//                    switch (paymentCode) {
//                        case "wfp":
//                            pay_method = "wfp_payment";
//                            cityMaxPay(cityCodeNew);
//                            Logger.d(context, TAG, "2");
//                            getCardTokenWfp(city);
//                            break;
//                        case "fondy":
//                            pay_method = "fondy_payment";
//                            cityMaxPay(cityCodeNew);
//                            Logger.d(context, TAG, "3");
//                            merchantFondy(cityCodeNew, context);
//                            break;
//                        case "mono":
//                            pay_method = "mono_payment";
//                            break;
//                    }
//                    if(isAdded()){
//                        ContentValues cv = new ContentValues();
//                        cv.put("payment_type", pay_method);
//                        // обновляем по id
//                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//                        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
//                                new String[] { "1" });
//                        database.close();
//
//                    }
//
//
//                } else {
//                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
//                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<ResponsePaySystem> call, @NonNull Throwable t) {
//
//                    Logger.d(context, TAG, "Failed. Error message: " + t.getMessage());
//
//                if (isAdded()) {
//                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
//                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//                }
//            }
//        });
    }

    private void updateMyPosition(String city) {

        double startLat;
        double startLan;
        String position;
        Logger.d(context, TAG, "updateMyPosition:city "+ city);
//        ActionBarUtil.setupCustomActionBar(this, R.layout.custom_action_bar_title, R.id.action_bar_title, newTitle);

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
            case "Сhernivtsi":
            case "Lutsk":
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://m.easy-order-taxi.site");
                break;
            case "OdessaTest":
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://test-taxi.kyiv.ua");
                break;
            default:
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://m.easy-order-taxi.site");
                city = "foreign countries";
        }

        switch (city) {
            case "Kyiv City":
                position = context.getString(R.string.pos_k);
                startLat = 50.451107;
                startLan = 30.524907;
                phoneNumber = Kyiv_City_phone; // Здесь также добавляем номер телефона
                break;
            case "Dnipropetrovsk Oblast":
                // Днепр
                position = context.getString(R.string.pos_d);
                startLat = 48.4647;
                startLan = 35.0462;
                phoneNumber = Dnipropetrovsk_Oblast_phone; // Укажите соответствующий номер телефона
                break;
            case "Odessa":
                position = context.getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                phoneNumber = Odessa_phone;
                break;
            case "Zaporizhzhia":
                position = context.getString(R.string.pos_z);
                startLat = 47.84015;
                startLan = 35.13634;
                phoneNumber = Zaporizhzhia_phone;
                break;
            case "Cherkasy Oblast":
                position = context.getString(R.string.pos_c);
                startLat = 49.44469;
                startLan = 32.05728;
                phoneNumber = Cherkasy_Oblast_phone;
                break;
            case "Lviv":
                position = context.getString(R.string.pos_l);
                startLat = 49.83993;
                startLan = 24.02973;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Ivano_frankivsk":
                position = context.getString(R.string.pos_if);
                startLat = 48.92005;
                startLan = 24.71067;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Vinnytsia":
                position = context.getString(R.string.pos_v);
                startLat = 49.23325;
                startLan = 28.46865;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Poltava":
                position = context.getString(R.string.pos_p);
                startLat = 49.59325;
                startLan = 34.54938;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Sumy":
                position = context.getString(R.string.pos_s);
                startLat = 50.90775;
                startLan = 34.79865;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Kharkiv":
                position = context.getString(R.string.pos_h);
                startLat = 49.99358;
                startLan = 36.23191;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Chernihiv":
                position = context.getString(R.string.pos_ch);
                startLat = 51.4933;
                startLan = 31.2972;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Rivne":
                position = context.getString(R.string.pos_r);
                startLat = 50.6198;
                startLan = 26.2406;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Ternopil":
                position = context.getString(R.string.pos_t);
                startLat = 49.54479;
                startLan = 25.5990;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Khmelnytskyi":
                position = context.getString(R.string.pos_kh);
                startLat = 49.41548;
                startLan = 27.00674;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;

            case "Zakarpattya":
                position = context.getString(R.string.pos_uz);
                startLat = 48.61913;
                startLan = 22.29475;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Zhytomyr":
                position = context.getString(R.string.pos_zt);
                startLat = 50.26801;
                startLan = 28.68026;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Kropyvnytskyi":
                position = context.getString(R.string.pos_kr);
                startLat = 48.51159;
                startLan = 32.26982;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Mykolaiv":
                position = context.getString(R.string.pos_m);
                startLat = 46.97498;
                startLan = 31.99378;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Сhernivtsi":
                position = context.getString(R.string.pos_chr);
                startLat = 48.29306;
                startLan = 25.93484;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Lutsk":
                position = context.getString(R.string.pos_ltk);
                startLat = 50.73968;
                startLan = 25.32400;
                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;

            case "OdessaTest":
                position = context.getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                phoneNumber = Kyiv_City_phone;
                break;

            default:
                position = context.getString(R.string.pos_f);
                startLat = 52.13472;
                startLan = 21.00424;
                phoneNumber = Kyiv_City_phone; // Номер телефона по умолчанию
                break;
        }
        pay_system(city);

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

        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        Logger.d(context, TAG, "newUser: " + userEmail);

        new Thread(() -> fetchRoutes(userEmail)).start();
        cityMaxPay(city);
        Logger.d(context, TAG, "1");
        getCardTokenWfp(cityCode[positionFirst]);
        dismiss();
    }

    private void updateMyLatsPosition(String routefrom, String startLatString, String startLanString, String city) {

        double startLat = Double.parseDouble(startLatString);;
        double startLan = Double.parseDouble(startLanString);
        String position = routefrom;
        Logger.d(context, TAG, "updateMyPosition:city "+ city);
//        ActionBarUtil.setupCustomActionBar(this, R.layout.custom_action_bar_title, R.id.action_bar_title, newTitle);

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
            case "Сhernivtsi":
            case "Lutsk":
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://m.easy-order-taxi.site");
                break;
            case "OdessaTest":
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://test-taxi.kyiv.ua");
                break;
            default:
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://m.easy-order-taxi.site");
                city = "foreign countries";
        }

        switch (city) {
            case "Kyiv City":

                phoneNumber = Kyiv_City_phone; // Здесь также добавляем номер телефона
                break;
            case "Dnipropetrovsk Oblast":
                // Днепр

                phoneNumber = Dnipropetrovsk_Oblast_phone; // Укажите соответствующий номер телефона
                break;
            case "Odessa":

                phoneNumber = Odessa_phone;
                break;
            case "Zaporizhzhia":

                phoneNumber = Zaporizhzhia_phone;
                break;
            case "Cherkasy Oblast":

                phoneNumber = Cherkasy_Oblast_phone;
                break;
            case "Lviv":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Ivano_frankivsk":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Vinnytsia":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Poltava":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Sumy":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Kharkiv":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Chernihiv":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Rivne":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Ternopil":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Khmelnytskyi":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;

            case "Zakarpattya":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Zhytomyr":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Kropyvnytskyi":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Mykolaiv":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Сhernivtsi":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;
            case "Lutsk":

                phoneNumber = Kyiv_City_phone; // Укажите соответствующий номер телефона
                break;

            case "OdessaTest":

                phoneNumber = Kyiv_City_phone;
                break;

            default:

                phoneNumber = Kyiv_City_phone; // Номер телефона по умолчанию
                break;
        }
        pay_system(city);

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

        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        Logger.d(context, TAG, "newUser: " + userEmail);

        new Thread(() -> fetchRoutes(userEmail)).start();

        cityMaxPay(city);
        Logger.d(context, TAG, "1");
        getCardTokenWfp(cityCode[positionFirst]);
        dismiss();
    }
    private void fetchRoutes(String value) {

        databaseHelper.clearTable();
        databaseHelperUid.clearTableUid();

        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String city = stringList.get(1);

        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        String url = baseUrl + "/android/UIDStatusShowEmailCityApp/" + value + "/" + city + "/" + context.getString(R.string.application);

        Call<List<RouteResponse>> call = ApiClient.getApiService().getRoutes(url);
        routeList = new ArrayList<>();
        Logger.d(context, TAG, "fetchRoutes: " + url);
        call.enqueue(new Callback<List<RouteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RouteResponse>> call, @NonNull Response<List<RouteResponse>> response) {
                if (response.isSuccessful()) {
                    List<RouteResponse> routes = response.body();
                    Logger.d (context, TAG, "onResponse: " + routes);
                    if (routes != null && !routes.isEmpty()) {
                        boolean hasRouteWithAsterisk = false;
                        for (RouteResponse route : routes) {
                            if ("*".equals(route.getRouteFrom())) {
                                // Найден объект с routefrom = "*"
                                hasRouteWithAsterisk = true;
                                break;  // Выход из цикла, так как условие уже выполнено
                            }
                        }
                        if (!hasRouteWithAsterisk) {
                            routeList.addAll(routes);
                            processRouteList();
                        }

                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<RouteResponse>> call, @NonNull Throwable t) {

            }
        });
    }


    private void processRouteList() {
        // В этом методе вы можете использовать routeList для выполнения дополнительных действий с данными.

        // Создайте массив строк
        databaseHelper = new DatabaseHelper(context);
        databaseHelper.clearTable();

        databaseHelperUid = new DatabaseHelperUid(context);
        databaseHelperUid.clearTableUid();

        array = new String[routeList.size()];


        String closeReasonText = context.getString(R.string.close_resone_def);

        for (int i = 0; i < routeList.size(); i++) {
            RouteResponse route = routeList.get(i);

            String routeFrom = route.getRouteFrom();
            String routefromnumber = route.getRouteFromNumber();
            String startLat = route.getStartLat();
            String startLan = route.getStartLan();

            String routeTo = route.getRouteTo();
            String routeTonumber = route.getRouteToNumber();
            String to_lat = route.getTo_lat();
            String to_lng = route.getTo_lng();

            String webCost = route.getWebCost();
            String createdAt = route.getCreatedAt();
            String closeReason = route.getCloseReason();
            String auto = route.getAuto();

            switch (closeReason){
                case "-1":
                    closeReasonText =context.getString(R.string.close_resone_in_work);
                    break;
                case "0":
                    closeReasonText =context.getString(R.string.close_resone_0);
                    break;
                case "1":
                    closeReasonText =context.getString(R.string.close_resone_1);
                    break;
                case "2":
                    closeReasonText =context.getString(R.string.close_resone_2);
                    break;
                case "3":
                    closeReasonText =context.getString(R.string.close_resone_3);
                    break;
                case "4":
                    closeReasonText =context.getString(R.string.close_resone_4);
                    break;
                case "5":
                    closeReasonText =context.getString(R.string.close_resone_5);
                    break;
                case "6":
                    closeReasonText =context.getString(R.string.close_resone_6);
                    break;
                case "7":
                    closeReasonText =context.getString(R.string.close_resone_7);
                    break;
                case "8":
                    closeReasonText =context.getString(R.string.close_resone_8);
                    break;
                case "9":
                    closeReasonText =context.getString(R.string.close_resone_9);
                    break;

            }

            if(routeFrom.equals("Місце відправлення")) {
                routeFrom =context.getString(R.string.start_point_text);
            }


            if(routeTo.equals("Точка на карте")) {
                routeTo =context.getString(R.string.end_point_marker);
            }
            if(routeTo.contains("по городу")) {
                routeTo =context.getString(R.string.on_city);
            }
            if(routeTo.contains("по місту")) {
                routeTo =context.getString(R.string.on_city);
            }
            String routeInfo = "";

            if(auto == null) {
                auto = "??";
            }

            if(routeFrom.equals(routeTo)) {
                routeInfo = routeFrom + " " + routefromnumber
                        +context.getString(R.string.close_resone_to)
                        +context.getString(R.string.on_city)
                        +context.getString(R.string.close_resone_cost) + webCost + " " +context.getString(R.string.UAH)
                        +context.getString(R.string.auto_info) + " " + auto + " "
                        +context.getString(R.string.close_resone_time)
                        + createdAt +context.getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = routeFrom + " " + routefromnumber
                        +context.getString(R.string.close_resone_to) + routeTo + " " + routeTonumber + "."
                        +context.getString(R.string.close_resone_cost) + webCost + " " +context.getString(R.string.UAH)
                        +context.getString(R.string.auto_info) + " " + auto + " "
                        +context.getString(R.string.close_resone_time)
                        + createdAt +context.getString(R.string.close_resone_text) + closeReasonText;
            }

            databaseHelper.addRouteInfo(routeInfo);

            List<String> settings = new ArrayList<>();

            settings.add(startLat);
            settings.add(startLan);
            settings.add(to_lat);
            settings.add(to_lng);
            settings.add(routeFrom + " " + routefromnumber);
            settings.add(routeTo + " " + routeTonumber);
            Logger.d(context, TAG, settings.toString());
            databaseHelperUid.addRouteInfoUid(settings);


        }
        array = databaseHelper.readRouteInfo();
        Logger.d(context, TAG, "processRouteList: array 1211" + Arrays.toString(array));
    }
    private void updateRoutMarker(List<String> settings) {
        Logger.d(context, TAG, "updateRoutMarker: " + settings.toString());
        ContentValues cv = new ContentValues();

        cv.put("startLat", Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[]{"1"});
        database.close();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {

        super.onDismiss(dialog);

        MainActivity.firstStart = false;
        if (positionFirst != 6) {
            message = context.getString(R.string.change_message) + context.getString(R.string.hi_mes) + " " + context.getString(R.string.order_in) + cityMenu + ".";
        } else {
            message = context.getString(R.string.change_message);
        }
        if (MainActivity.navVisicomMenuItem != null) {
            // Новый текст элемента меню
            newTitle =  context.getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
//            ActionBarUtil.setupCustomActionBar(this, R.layout.custom_action_bar_title, R.id.action_bar_title, newTitle);

            // Изменяем текст элемента меню
            MainActivity.navVisicomMenuItem.setTitle(newTitle);

            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

            VisicomFragment.textfrom.setVisibility(View.VISIBLE);
            VisicomFragment.num1.setVisibility(View.VISIBLE);
        }

        checkNotificationPermissionAndRequestIfNeeded();
        startActivity(new Intent(context, MainActivity.class));
    }

    private void cityMaxPay(String city) {


        CityService cityService = CityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponse> call = cityService.getMaxPayValues(city, context.getString(R.string.application));

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
                        Logger.d(context, TAG, "black_list 2" + black_list);
                        if(isAdded()) {
                            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                    new String[]{"1"});

                            database.close();
                        }



                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                        Logger.d(context, TAG, "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Failed. Error message: " + t.getMessage());
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
                    Logger.d(context, TAG, "onResponse: cityResponse" + cityResponse);
                    if (cityResponse != null) {
                        String merchant_fondy = cityResponse.getMerchantFondy();
                        String fondy_key_storage = cityResponse.getFondyKeyStorage();

                        ContentValues cv = new ContentValues();
                        cv.put("merchant_fondy", merchant_fondy);
                        cv.put("fondy_key_storage", fondy_key_storage);


                            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                    new String[]{"1"});

                            database.close();



                        Logger.d(context, TAG, "onResponse: merchant_fondy" + merchant_fondy);
                        Logger.d(context, TAG, "onResponse: fondy_key_storage" + fondy_key_storage);

                        if(merchant_fondy != null) {
                            getCardToken(context, merchant_fondy);
                        }


                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                    Logger.d(context, TAG, "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponseMerchantFondy> call, @NonNull Throwable t) {
                 Logger.d(context, TAG, "Failed. Error message: " + t.getMessage());
            }
        });
    }
    private void getCardToken(Context context, String merchant_fondy) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site") // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String baseUrl = retrofit.baseUrl().toString();

        Logger.d(context, TAG, "Base URL: " + baseUrl);
        // Создайте сервис
        CallbackService service = retrofit.create(CallbackService.class);

        Logger.d(context, TAG, "getCardTokenFondy: ");

        List<String> arrayList = logCursor(MainActivity.TABLE_USER_INFO, context);
        String email = arrayList.get(3);

            // Выполните запрос
            Call<CallbackResponse> call = service.handleCallback(email, "fondy", merchant_fondy);
            String requestUrl = call.request().toString();
            Logger.d(context, TAG, "Request URL: " + requestUrl);

            call.enqueue(new Callback<CallbackResponse>() {
                @Override
                public void onResponse(@NonNull Call<CallbackResponse> call, @NonNull Response<CallbackResponse> response) {
                    Logger.d(context, TAG, "onResponse: " + response.body());
                    if (response.isSuccessful()) {
                        CallbackResponse callbackResponse = response.body();
                        if (callbackResponse != null) {
                            List<CardInfo> cards = callbackResponse.getCards();
                            Logger.d(context, TAG, "onResponse: cards" + cards);
                            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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

                                    Logger.d(context, TAG, "onResponse: card_token: " + rectoken);

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
                    Logger.d(context, TAG, "Failed. Error message: " + t.getMessage());
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
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[]{"1"});
        database.close();

    }

    @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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

    public void getPublicIPAddress() {
        getCountryByIP();
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
                VisicomFragment.progressBar.setVisibility(View.GONE);;
                sharedPreferencesHelperMain.saveValue("countryState", "UA");
            }
        });
    }

    void checkNotificationPermissionAndRequestIfNeeded() {
        if (isAdded()) {
            // Получаем доступ к настройкам приложения
            SharedPreferences sharedPreferences = requireActivity().getPreferences(MODE_PRIVATE);

            // Проверяем, было ли уже запрошено разрешение
            boolean isNotificationPermissionRequested = sharedPreferences.getBoolean("notification_permission_requested", false);

            // Проверяем версию Android
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 и выше
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                boolean areNotificationsEnabled = notificationManager.areNotificationsEnabled();

                // Если уведомления не разрешены и разрешение еще не запрашивалось
                if (!areNotificationsEnabled && !isNotificationPermissionRequested) {
                    openNotificationSettings(context);
                    // Сохраняем информацию о том, что разрешение было запрошено
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("notification_permission_requested", true);
                    editor.apply();
                }
            }
        }
    }

    public void openNotificationSettings(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        context.startActivity(intent);
    }

    private void lastAddressUser(String cityString) {

        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);


        Logger.d(context, TAG, "lastAddressUser: cityString" + cityString);
        if (cityString.equals("OdessaTest")) {
            sharedPreferencesHelperMain.saveValue("baseUrl", "https://test-taxi.kyiv.ua");
        } else {
            sharedPreferencesHelperMain.saveValue("baseUrl", "https://m.easy-order-taxi.site");
        }
        Logger.d(context, TAG, "lastAddressUser: baseUrl" + sharedPreferencesHelperMain.getValue("baseUrl", ""));
        CityService cityService= CityApiClient.getClient().create(CityService.class);

        Call<CityLastAddressResponse> call = cityService.lastAddressUser(email, cityString, context.getString(R.string.application));
        resetRoutHome();
        resetRoutMarker();
        Logger.d(context, TAG, "Запрос: " + call.request().url());

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<CityLastAddressResponse> call, @NonNull Response<CityLastAddressResponse> response) {
                if (response.isSuccessful()) {
                    CityLastAddressResponse cityResponse = response.body();
                    assert cityResponse != null;
                    Logger.d(context, TAG, "onResponse: cityResponse" + cityResponse.toString());
                    String routefrom = cityResponse.getRoutefrom();
                    String startLat = cityResponse.getStartLat();
                    String startLan = cityResponse.getStartLan();


                    Logger.d(context, TAG, "lastAddressUser: routefrom" + routefrom);
                    Logger.d(context, TAG, "lastAddressUser: startLat" + startLat);
                    Logger.d(context, TAG, "lastAddressUser: startLan" + startLan);
                    if (startLat.equals("0.0") || startLat.equals("0")) {
                        updateMyPosition(cityString);
                    } else {
                        updateMyLatsPosition(routefrom, startLat, startLan, cityString);
                    }

                } else {
                    Logger.d(context, TAG, "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityLastAddressResponse> call, Throwable t) {
                Logger.d(getContext(), TAG, "Failed. Error message: " + t.getMessage());
                VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
            }
        });
        VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
    }
}

