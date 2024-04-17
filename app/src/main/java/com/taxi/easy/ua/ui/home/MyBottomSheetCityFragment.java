package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
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
import com.taxi.easy.ua.utils.ip.ApiServiceCountry;
import com.taxi.easy.ua.utils.ip.CountryResponse;
import com.taxi.easy.ua.utils.ip.IPUtil;
import com.taxi.easy.ua.utils.ip.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyBottomSheetCityFragment extends BottomSheetDialogFragment {

    private static final String TAG = "TAG_CITY";
    ListView listView;
    String city;
    private String cityMenu;
    private String message;

    public MyBottomSheetCityFragment() {
        // Пустой конструктор без аргументов
    }

    public MyBottomSheetCityFragment(String city) {
        this.city = city;
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

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cities_list_layout, container, false);
        listView = view.findViewById(R.id.listViewBonus);
        VisicomFragment.progressBar.setVisibility(View.INVISIBLE);

        String[] cityList = new String[]{
                getString(R.string.Kyiv_city),
                getString(R.string.Dnipro_city),
                getString(R.string.Odessa),
                getString(R.string.Zaporizhzhia),
                getString(R.string.Cherkasy),
                "Тест",
                getString(R.string.foreign_countries),
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.services_adapter_layout, cityList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        switch (city){
            case "Kyiv City":positionFirst = 0;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.city_kyiv);
                MainActivity.countryState = "UA";
                break;

            case "Dnipropetrovsk Oblast":
                positionFirst = 1;
                phoneNumber = Dnipropetrovsk_Oblast_phone;
                cityMenu = getString(R.string.city_dnipro);
                break;
            case "Odessa":
                positionFirst = 2;
                phoneNumber = Odessa_phone;
                cityMenu = getString(R.string.city_odessa);
                MainActivity.countryState = "UA";
                break;
            case "Zaporizhzhia":
                positionFirst = 3;
                phoneNumber = Zaporizhzhia_phone;
                cityMenu = getString(R.string.city_zaporizhzhia);
                MainActivity.countryState = "UA";
                break;
            case "Cherkasy Oblast":
                positionFirst = 4;
                phoneNumber = Cherkasy_Oblast_phone;
                cityMenu = getString(R.string.city_cherkasy);
                MainActivity.countryState = "UA";
                break;
            case "OdessaTest":
                positionFirst = 5;
                phoneNumber = Kyiv_City_phone;
                cityMenu = "Test";
                MainActivity.countryState = "UA";
                break;
            default:
                positionFirst = 6;
                phoneNumber = Kyiv_City_phone;
                cityMenu = getString(R.string.foreign_countries);
                new GetPublicIPAddressTask().execute();
                break;
        }
        Log.d(TAG, "onCreateView: city" + city);
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
                        cityMenu = getString(R.string.city_kyiv);
                        MainActivity.countryState = "UA";
                        break;
                    case "Dnipropetrovsk Oblast":
                        positionFirst = 1;
                        phoneNumber = Dnipropetrovsk_Oblast_phone;
                        cityMenu = getString(R.string.city_dnipro);
                        MainActivity.countryState = "UA";
                        break;
                    case "Odessa":
                        positionFirst = 2;
                        phoneNumber = Odessa_phone;
                        cityMenu = getString(R.string.city_odessa);
                        MainActivity.countryState = "UA";
                        break;
                    case "Zaporizhzhia":
                        positionFirst = 3;
                        phoneNumber = Zaporizhzhia_phone;
                        cityMenu = getString(R.string.city_zaporizhzhia);
                        MainActivity.countryState = "UA";
                        break;
                    case "Cherkasy Oblast":
                        positionFirst = 4;
                        phoneNumber = Cherkasy_Oblast_phone;
                        cityMenu = getString(R.string.city_cherkasy);
                        MainActivity.countryState = "UA";
                        break;
                    case "OdessaTest":
                        positionFirst = 5;
                        phoneNumber = Kyiv_City_phone;
                        cityMenu = "Test";
                        MainActivity.countryState = "UA";
                        break;
                    case "foreign countries":
                        positionFirst = 6;
                        phoneNumber = Kyiv_City_phone;
                        cityMenu = getString(R.string.foreign_countries);
                        break;
                    default:
                        phoneNumber = Kyiv_City_phone;
                        positionFirst = 0;
                        cityMenu = getString(R.string.city_kyiv);
                        MainActivity.countryState = "UA";
                        break;
                }
                    if (positionFirst == 6) {
                        new GetPublicIPAddressTask().execute();
                        cityMaxPay(cityCode[0], getContext());
                        merchantFondy(cityCode[0], getContext());
                    } else {
                        cityMaxPay(cityCode[positionFirst], getContext());
                        merchantFondy(cityCode[positionFirst], getContext());
                    }
                    resetRoutHome();
                    resetRoutMarker();
                    updateMyPosition(cityCode[positionFirst]);

                dismiss();
            }
        });

        return view;
    }
    private void updateMyPosition(String city) {

        double startLat;
        double startLan;
        String position;
        Log.d(TAG, "updateMyPosition:city "+ city);
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
                break;
        }

        switch (city){
            case "Kyiv City":positionFirst = 0;
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
                position = "DW631 47, 00-514 Warszawa, Poland\t";
                startLat = 52.13472;
                startLan = 21.00424;
                break;
        }

        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        ContentValues cv = new ContentValues();

        cv.put("city", city);
        cv.put("phone", phoneNumber);
        database.update(MainActivity.CITY_INFO, cv, "id = ?", new String[]{"1"});

        cv = new ContentValues();
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


        List<String> settings = new ArrayList<>();


        settings.add(Double.toString(startLat));
        settings.add(Double.toString(startLan));
        settings.add(Double.toString(startLat));
        settings.add(Double.toString(startLan));
        settings.add(position);
        settings.add(getString(R.string.on_city_tv));
        updateRoutMarker(settings);

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

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {

        super.onDismiss(dialog);


        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.nav_visicom);
        if (positionFirst != 6) {
            message = getString(R.string.change_message) + getString(R.string.hi_mes) + " " + getString(R.string.order_in) + cityMenu + ".";
        } else {
            message = getString(R.string.change_message);
        }
        if (MainActivity.navVisicomMenuItem != null) {
            // Новый текст элемента меню
            String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
            // Изменяем текст элемента меню
            MainActivity.navVisicomMenuItem.setTitle(newTitle);

            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();

            VisicomFragment.textfrom.setVisibility(View.VISIBLE);
            VisicomFragment.num1.setVisibility(View.VISIBLE);
        }
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

                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.CITY_INFO, cv, "id = ?",
                                new String[]{"1"});

                        database.close();


                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                    Log.e("Request", "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                Log.e("Request", "Failed. Error message: " + t.getMessage());
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
                    Log.d(TAG, "onResponse: cityResponse" + cityResponse);
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



                        Log.d(TAG, "onResponse: merchant_fondy" + merchant_fondy);
                        Log.d(TAG, "onResponse: fondy_key_storage" + fondy_key_storage);

                        if(merchant_fondy != null) {
                            getCardToken(context, merchant_fondy);
                        }


                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                    Log.e("Request", "Failed. Error code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponseMerchantFondy> call, @NonNull Throwable t) {
                Log.e("Request", "Failed. Error message: " + t.getMessage());
            }
        });
    }
    private void getCardToken(Context context, String merchant_fondy) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site") // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        String baseUrl = retrofit.baseUrl().toString();

        Log.d(TAG, "Base URL: " + baseUrl);
        // Создайте сервис
        CallbackService service = retrofit.create(CallbackService.class);

        Log.d(TAG, "getCardTokenFondy: ");

        List<String> arrayList = logCursor(MainActivity.TABLE_USER_INFO, context);
        String email = arrayList.get(3);

            // Выполните запрос
            Call<CallbackResponse> call = service.handleCallback(email, "fondy", merchant_fondy);
            String requestUrl = call.request().toString();
            Log.d(TAG, "Request URL: " + requestUrl);

            call.enqueue(new Callback<CallbackResponse>() {
                @Override
                public void onResponse(@NonNull Call<CallbackResponse> call, @NonNull Response<CallbackResponse> response) {
                    Log.d(TAG, "onResponse: " + response.body());
                    if (response.isSuccessful()) {
                        CallbackResponse callbackResponse = response.body();
                        if (callbackResponse != null) {
                            List<CardInfo> cards = callbackResponse.getCards();
                            Log.d(TAG, "onResponse: cards" + cards);
                            if (cards != null && !cards.isEmpty()) {
                                SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                                // Очистка таблицы
                                database.delete(MainActivity.TABLE_FONDY_CARDS, "1", null);
                                for (CardInfo cardInfo : cards) {
                                    ContentValues cv = new ContentValues();
                                    String masked_card = cardInfo.getMasked_card(); // Маска карты
                                    String card_type = cardInfo.getCard_type(); // Тип карты
                                    String bank_name = cardInfo.getBank_name(); // Название банка
                                    String rectoken = cardInfo.getRectoken(); // Токен карты
                                    String merchantId = cardInfo.getMerchant(); // Токен карты

                                    Log.d(TAG, "onResponse: card_token: " + rectoken);

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
                    Log.d(TAG, "onResponse: failure " + t.toString());
//                Toast.makeText(getApplicationContext(), getApplicationContext().getString(verify_internet), Toast.LENGTH_SHORT).show();
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

    private static class GetPublicIPAddressTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                return IPUtil.getPublicIPAddress();
            } catch (Exception e) {
                // Log the exception
                Log.e(TAG, "Exception in doInBackground: " + e.getMessage());
                // Return null or handle the exception as needed
                return null;
            }
        }

        @Override
        protected void onPostExecute(String ipAddress) {
            try {
                if (ipAddress != null) {
                    Log.d(TAG, "onCreate: Local IP Address: " + ipAddress);
                    getCountryByIP(ipAddress);
                } else {
                    MainActivity.countryState = "UA";
                }
            } catch (Exception e) {
                // Log the exception
                Log.e(TAG, "Exception in onPostExecute: " + e.getMessage());
                // Handle the exception as needed
            }

        }
    }
    private static void getCountryByIP(String ipAddress) {
        ApiServiceCountry apiService = RetrofitClient.getClient().create(ApiServiceCountry.class);
        Call<CountryResponse> call = apiService.getCountryByIP(ipAddress);

        call.enqueue(new Callback<CountryResponse>() {
            @Override
            public void onResponse(@NonNull Call<CountryResponse> call, @NonNull Response<CountryResponse> response) {
                if (response.isSuccessful()) {
                    CountryResponse countryResponse = response.body();
                    if (countryResponse != null) {
                        MainActivity.countryState = countryResponse.getCountry();
                    } else {
                        MainActivity.countryState = "UA";
                    }
                } else {
                    MainActivity.countryState = "UA";
                }
                Log.d(TAG, "countryState  " + MainActivity.countryState);
            }

            @Override
            public void onFailure(@NonNull Call<CountryResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error: " + t.getMessage());
            }
        });
    }
}

