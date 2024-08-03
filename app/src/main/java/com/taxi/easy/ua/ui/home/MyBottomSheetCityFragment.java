package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

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
import android.widget.AdapterView;
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
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.cities.api.CityApiClient;
import com.taxi.easy.ua.cities.api.CityResponse;
import com.taxi.easy.ua.cities.api.CityResponseMerchantFondy;
import com.taxi.easy.ua.cities.api.CityService;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponse;
import com.taxi.easy.ua.ui.fondy.callback.CallbackResponse;
import com.taxi.easy.ua.ui.fondy.callback.CallbackService;
import com.taxi.easy.ua.ui.payment_system.PayApi;
import com.taxi.easy.ua.ui.payment_system.ResponsePaySystem;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.ip.ApiServiceCountry;
import com.taxi.easy.ua.utils.ip.CountryResponse;
import com.taxi.easy.ua.utils.ip.RetrofitClient;
import com.taxi.easy.ua.utils.ip.ip_util_retrofit.IpResponse;
import com.taxi.easy.ua.utils.ip.ip_util_retrofit.IpifyService;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

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

    private static final String TAG = "TAG_CITY";
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
    private final String baseUrl = "https://m.easy-order-taxi.site";
    Context context;
    String countryState;
    SharedPreferencesHelper sharedPreferencesHelper;


    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Nullable
    @Override

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cities_list_layout, container, false);

        fragmentManager = getParentFragmentManager();
        listView = view.findViewById(R.id.listViewBonus);
        VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        if(context == null) {
            context = requireActivity();
        }
        String[] cityList = new String[]{
                context.getString(R.string.Kyiv_city),
                context.getString(R.string.Dnipro_city),
                context.getString(R.string.Odessa),
                context.getString(R.string.Zaporizhzhia),
                context.getString(R.string.Cherkasy),
                context.getString(R.string.test_city),
                context.getString(R.string.foreign_countries),
        };

        databaseHelper = new DatabaseHelper(context);
        databaseHelperUid = new DatabaseHelperUid(context);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.services_adapter_layout, cityList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        if(city == null) {
            city = "Kyiv City";
        }
        sharedPreferencesHelper = new SharedPreferencesHelper(context);
        switch (city){
            case "Kyiv City":positionFirst = 0;
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
                cityMenu = context.getString(R.string.city_cherkasy);
                break;
            case "OdessaTest":
                positionFirst = 5;
                phoneNumber = Kyiv_City_phone;
                cityMenu = "Test";
                break;
            default:
                positionFirst = 6;
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.foreign_countries);
        }

        Logger.d(context, TAG, "onCreateView: city" + city);
        updateMyPosition(city);
        listView.setItemChecked(positionFirst, true);

        int positionFirstOld = positionFirst;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
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
                        cityMenu = context.getString(R.string.city_cherkasy);
                        countryState = "UA";
                        break;
                    case "OdessaTest":
                        positionFirst = 5;
                        phoneNumber = Kyiv_City_phone;
                        cityMenu = "Test";
                        countryState = "UA";
                        break;
                    case "foreign countries":
                        positionFirst = 6;
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

                String cityCodeNew;
                    if (positionFirst == 6) {
                        getPublicIPAddress();
                        cityCodeNew = cityCode[0];
                    } else {
                        cityCodeNew = cityCode[positionFirst];
                        sharedPreferencesHelper.saveValue("countryState", countryState);
                    }
                Logger.d(context, TAG, "onItemClick: pay_method" + pay_method);

                pay_system(cityCodeNew);

                resetRoutHome();
                resetRoutMarker();
                updateMyPosition(cityCode[positionFirst]);

                cityMaxPay(cityCodeNew);
                Logger.d(context, TAG, "1");
                getCardTokenWfp(cityCode[positionFirst]);
                dismiss();
            }
        });
        databaseHelper = new DatabaseHelper(context);
        databaseHelperUid = new DatabaseHelperUid(context);
        return view;
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
        Logger.d(context, TAG, "getCardTokenWfp: ");
        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        // Выполните запрос
        Call<CallbackResponseWfp> call = service.handleCallbackWfp(
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
                        database.delete(MainActivity.TABLE_WFP_CARDS, "1", null);
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
                Logger.d(context, TAG, "Failed. Error message: " + t.getMessage());

            }
        });
    }

    private void pay_system(String cityCodeNew) {
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PayApi apiService = retrofit.create(PayApi.class);
        Call<ResponsePaySystem> call = apiService.getPaySystem();
        call.enqueue(new Callback<ResponsePaySystem>() {
            @Override
            public void onResponse(@NonNull Call<ResponsePaySystem> call, @NonNull Response<ResponsePaySystem> response) {
                if (response.isSuccessful()) {
                    // Обработка успешного ответа
                    ResponsePaySystem responsePaySystem = response.body();
                    assert responsePaySystem != null;
                    String paymentCode = responsePaySystem.getPay_system();

                    switch (paymentCode) {
                        case "wfp":
                            pay_method = "wfp_payment";
                            cityMaxPay(cityCodeNew);
                            Logger.d(context, TAG, "2");
                            getCardTokenWfp(city);
                            break;
                        case "fondy":
                            pay_method = "fondy_payment";
                            cityMaxPay(cityCodeNew);
                            Logger.d(context, TAG, "3");
                            merchantFondy(cityCodeNew, context);
                            break;
                        case "mono":
                            pay_method = "mono_payment";
                            break;
                    }
                    if(isAdded()){
                        ContentValues cv = new ContentValues();
                        cv.put("payment_type", pay_method);
                        // обновляем по id
                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                new String[] { "1" });
                        database.close();

                    }


                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponsePaySystem> call, @NonNull Throwable t) {

                    Logger.d(context, TAG, "Failed. Error message: " + t.getMessage());

                if (isAdded()) {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            }
        });
    }

    private void updateMyPosition(String city) {

        double startLat;
        double startLan;
        String position;
        Logger.d(context, TAG, "updateMyPosition:city "+ city);
        switch (city){
            case "Dnipropetrovsk Oblast":
            case "Odessa":
            case "Zaporizhzhia":
            case "Cherkasy Oblast":
            case "OdessaTest":
            case "Kyiv City":
                break;
            default:
                city = "foreign countries";
        }

        switch (city){
            case "Kyiv City":positionFirst = 0;
                position = context.getString(R.string.pos_k);
                startLat = 50.451107;
                startLan = 30.524907;
                break;
            case "Dnipropetrovsk Oblast":
                // Днепр
                position = context.getString(R.string.pos_d);
                startLat = 48.4647;
                startLan = 35.0462;
                break;
            case "Odessa":
                phoneNumber = Odessa_phone;
                position = context.getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                break;
            case "Zaporizhzhia":
                phoneNumber = Zaporizhzhia_phone;
                position = context.getString(R.string.pos_z);
                startLat = 47.84015;
                startLan = 35.13634;
                break;
            case "Cherkasy Oblast":
                phoneNumber = Cherkasy_Oblast_phone;
                position = context.getString(R.string.pos_c);
                startLat = 49.44469;
                startLan = 32.05728;
                break;
            case "OdessaTest":
                phoneNumber = Kyiv_City_phone;
                position = context.getString(R.string.pos_o);
                startLat = 46.4694;
                startLan = 30.7404;
                break;
            default:
                phoneNumber = Kyiv_City_phone;
                position = context.getString(R.string.pos_f);
                startLat = 52.13472;
                startLan = 21.00424;
                break;
        }
        pay_system(city);

        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

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
//        }


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
    }
    private void fetchRoutes(String value) {

        databaseHelper.clearTable();
        databaseHelperUid.clearTableUid();

        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String city = stringList.get(1);
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
                routeInfo =context.getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        +context.getString(R.string.close_resone_to)
                        +context.getString(R.string.on_city)
                        +context.getString(R.string.close_resone_cost) + webCost + " " +context.getString(R.string.UAH)
                        +context.getString(R.string.auto_info) + " " + auto + " "
                        +context.getString(R.string.close_resone_time)
                        + createdAt +context.getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo =context.getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        +context.getString(R.string.close_resone_to) + routeTo + " " + routeTonumber
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
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[]{"1"});
        database.close();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {

        super.onDismiss(dialog);
        MainActivity.navController.popBackStack();
        MainActivity.navController.navigate(R.id.nav_visicom);
        MainActivity.firstStart = false;
        if (positionFirst != 6) {
            message = context.getString(R.string.change_message) + context.getString(R.string.hi_mes) + " " + context.getString(R.string.order_in) + cityMenu + ".";
        } else {
            message = context.getString(R.string.change_message);
        }
        if (MainActivity.navVisicomMenuItem != null) {
            // Новый текст элемента меню
            String newTitle =  context.getString(R.string.menu_city) + " " + cityMenu;
            // Изменяем текст элемента меню
            MainActivity.navVisicomMenuItem.setTitle(newTitle);

            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

            VisicomFragment.textfrom.setVisibility(View.VISIBLE);
            VisicomFragment.num1.setVisibility(View.VISIBLE);
        }

        checkNotificationPermissionAndRequestIfNeeded();
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

                        ContentValues cv = new ContentValues();
                        cv.put("card_max_pay", cardMaxPay);
                        cv.put("bonus_max_pay", bonusMaxPay);
                        if(isAdded()) {
                            SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                    new String[]{"1"});

                            database.close();
                        }



                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                        Logger.d(getActivity(), TAG, "Failed. Error code: " + response.code());
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
                    Logger.d(getActivity(), TAG, "Failed. Error code: " + response.code());
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
        SQLiteDatabase database = requireContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
                sharedPreferencesHelper.saveValue("countryState", countryState);
            }

            @Override
            public void onFailure(@NonNull Call<CountryResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Error: " + t.getMessage());
                VisicomFragment.progressBar.setVisibility(View.GONE);;
                sharedPreferencesHelper.saveValue("countryState", "UA");
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
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
                boolean areNotificationsEnabled = notificationManager.areNotificationsEnabled();

                // Если уведомления не разрешены и разрешение еще не запрашивалось
                if (!areNotificationsEnabled && !isNotificationPermissionRequested) {
                    openNotificationSettings(requireContext());
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
}

