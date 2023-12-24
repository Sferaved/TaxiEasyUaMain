package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.cities.api.CityApiClient;
import com.taxi.easy.ua.cities.api.CityResponse;
import com.taxi.easy.ua.cities.api.CityResponseMerchantFondy;
import com.taxi.easy.ua.cities.api.CityService;
import com.taxi.easy.ua.ui.card.CardFragment;
import com.taxi.easy.ua.ui.gallery.GalleryFragment;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import com.taxi.easy.ua.ui.payment_system.PayApi;
import com.taxi.easy.ua.ui.payment_system.ResponsePaySystem;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyBottomSheetBonusFragment extends BottomSheetDialogFragment {

    private static final String TAG = "TAG_BON";
    long cost;
    String rout;
    String api;
    TextView textView;

    ListView listView;
    String[] array, arrayCode;
    AppCompatButton btn_ok;
    int pos;
    ProgressBar progressBar;
    CustomArrayAdapter adapter;
    private static SQLiteDatabase database;
    private String baseUrl = "https://m.easy-order-taxi.site";

    public MyBottomSheetBonusFragment(long cost, String rout, String api, TextView textView) {
        this.cost = cost;
        this.rout = rout;
        this.api = api;
        this.textView = textView;
    }

    @SuppressLint({"MissingInflatedId", "Range"})
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bonus_list_layout, container, false);
        progressBar = view.findViewById(R.id.progress);
        database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        listView = view.findViewById(R.id.listViewBonus);
        array = new  String[]{
                getString(R.string.nal_payment),
                getString(R.string.bonus_payment),
                getString(R.string.card_payment),
        };
        arrayCode = new  String[]{
                "nal_payment",
                "bonus_payment",
                "card_payment",
        };

        adapter = new CustomArrayAdapter(requireActivity(), R.layout.services_adapter_layout, Arrays.asList(array));
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        btn_ok = view.findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        fistItem();

        String bonus = logCursor(MainActivity.TABLE_USER_INFO).get(5);

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city = stringList.get(1);
        //

        switch (city) {
            case "Kyiv City":
            case "Dnipropetrovsk Oblast":
            case "Odessa":
            case "Zaporizhzhia":
            case "Cherkasy Oblast":
                adapter.setItemEnabled(1, false);
            case "OdessaTest":
                if(Long.parseLong(bonus) <= cost * 100 ) {
                    adapter.setItemEnabled(1, false);
                    break;
                }
        }

        merchantFondy(city, getContext());
//        switch (city) {
//            case "Kyiv City":
//            case "Dnipropetrovsk Oblast":
//            case "Odessa":
//            case "Zaporizhzhia":
//            case "Cherkasy Oblast":
//                ContentValues cv = new ContentValues();
//                cv.put("merchant_fondy", "");
//                cv.put("fondy_key_storage", "");
//
//                SQLiteDatabase database = requireContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//                database.update(MainActivity.CITY_INFO, cv, "id = ?",
//                        new String[]{"1"});
//                database.close();
//                adapter.setItemEnabled(2, false);
//                break;
//            case "OdessaTest":
//                merchantFondy(city, getContext());
//                break;
//        }


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                progressBar.setVisibility(View.VISIBLE);
                btn_ok.setVisibility(View.GONE);
                setCancelable(false);
                pos = position;
                Log.d(TAG, "onItemClick: pos " + pos);
                if (pos == 2) {
                    paySystem(new CardFragment.PaySystemCallback() {
                        @Override
                        public void onPaySystemResult(String paymentCode) {
                            Log.d(TAG, "onPaySystemResult: paymentCode" + paymentCode);
                            // Здесь вы можете использовать полученное значение paymentCode
                             paymentType(paymentCode, requireContext());
                        }

                        @Override
                        public void onPaySystemFailure(String errorMessage) {
                        }
                    });
                } else {
                    paymentType(arrayCode [pos], requireContext());
                }

            }

        });
        return view;
    }

    private void cityMaxPay(String $city, Context context) {
        CityService cityService = CityApiClient.getClient().create(CityService.class);

        // Замените "your_city" на фактическое название города
        Call<CityResponse> call = cityService.getMaxPayValues($city);

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

                        if(merchant_fondy == null) {
                            adapter.setItemEnabled(2, false);
                            listView.setItemChecked(0, true);
                            paymentType(arrayCode [0], context);
                        } else {
                            adapter.setItemEnabled(2, true);
                            cityMaxPay(city, context);
                        }

                        // Добавьте здесь код для обработки полученных значений
                    }
                } else {
                    Log.e("Request", "Failed. Error code: " + response.code());
                    adapter.setItemEnabled(2, false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponseMerchantFondy> call, @NonNull Throwable t) {
                Log.e("Request", "Failed. Error message: " + t.getMessage());
                adapter.setItemEnabled(2, false);
            }
        });
    }

    private void paymentType(String paymentCode, Context context) {
        ContentValues cv = new ContentValues();
        Log.d(TAG, "paymentType: paymentCode 1111" + paymentCode);

        cv.put("payment_type", paymentCode);
        // обновляем по id
//        if(isAdded()){
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                    new String[] { "1" });
            database.close();
//        }
        reCount();
    }

    @SuppressLint("Range")
    private void fistItem() {

        String payment_type = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(4);

        Log.d(TAG, "fistItem: " + payment_type);
        switch (payment_type) {
            case "nal_payment":
                listView.setItemChecked(0, true);
                pos = 0;
                paymentType(arrayCode [pos], requireContext());
                break;
            case "bonus_payment":
                listView.setItemChecked(1, true);
                pos = 1;
                paymentType(arrayCode [pos], requireContext());
                break;
            case "card_payment":
            case "fondy_payment":
            case "mono_payment":
                listView.setItemChecked(2, true);
                pos = 2;
//                paySystem(new CardFragment.PaySystemCallback() {
//                    @Override
//                    public void onPaySystemResult(String paymentCode) {
//                        Log.d(TAG, "onPaySystemResult: paymentCode" + paymentCode);
//                        // Здесь вы можете использовать полученное значение paymentCode
//                        paymentType(paymentCode, requireContext());
//
//                    }
//
//                    @Override
//                    public void onPaySystemFailure(String errorMessage) {
//                    }
//                });

                break;
        }
   }
    private void paySystem(final CardFragment.PaySystemCallback callback) {
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

                    String paymentCodeNew = "fondy"; // Изначально устанавливаем значение

                    switch (paymentCode) {
                        case "fondy":
                            paymentCodeNew = "fondy_payment";
                            break;
                        case "mono":
                            paymentCodeNew = "mono_payment";
                            break;
                    }
                    reCount();
                    // Вызываем обработчик, передавая полученное значение
                    callback.onPaySystemResult(paymentCodeNew);
                } else {
                    // Обработка ошибки
                    callback.onPaySystemFailure(getString(R.string.verify_internet));
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponsePaySystem> call, @NonNull Throwable t) {
                // Обработка ошибки
                callback.onPaySystemFailure(getString(R.string.verify_internet));
            }
        });
    }

    public void reCount() {
        Log.d(TAG, "onDismiss: rout " + rout);
        if(rout.equals("home")) {
            String urlCost = null;
            Map<String, String> sendUrlMapCost = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    urlCost = getTaxiUrlSearch("costSearch", requireContext());
                }

                sendUrlMapCost = CostJSONParser.sendURL(urlCost);
            } catch (MalformedURLException | UnsupportedEncodingException ignored) {

            }
            assert sendUrlMapCost != null;
            String orderCost = (String) sendUrlMapCost.get("order_cost");

            assert orderCost != null;
            if (!orderCost.equals("0")) {
                String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(3);
                long discountInt = Integer.parseInt(discountText);
                long discount;
                long firstCost = Long.parseLong(orderCost);
                discount = firstCost * discountInt / 100;


                firstCost = firstCost + discount;
                updateAddCost(String.valueOf(discount));

                HomeFragment.costFirstForMin = firstCost;
                String costUpdate = String.valueOf(firstCost);
                textView.setText(costUpdate);

            }
        }
        if(rout.equals("visicom")) {
            String urlCost = null;
            Map<String, String> sendUrlMapCost = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (isAdded() && getActivity() != null) {
                        urlCost = getTaxiUrlSearchMarkers("costSearchMarkers", getActivity());
                        sendUrlMapCost = CostJSONParser.sendURL(urlCost);
                        assert sendUrlMapCost != null;
                        String orderCost = (String) sendUrlMapCost.get("order_cost");
                        Log.d(TAG, "onDismiss: orderCost " + orderCost);
                        assert orderCost != null;
                        if (!orderCost.equals("0")) {
                            String costUpdate;
                            String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(3);
                            long discountInt = Integer.parseInt(discountText);
                            long discount;
                            long firstCost = Long.parseLong(orderCost);
                            discount = firstCost * discountInt / 100;

                            firstCost = firstCost + discount;
                            updateAddCost(String.valueOf(discount));

                            VisicomFragment.firstCostForMin = firstCost;
                            costUpdate = String.valueOf(firstCost);
                            textView.setText(costUpdate);
                        }
                    }
                }


            } catch (MalformedURLException ignored) {

            }

        }
        if(rout.equals("marker")) {
            String urlCost = null;
            Map<String, String> sendUrlMapCost = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (isAdded() && getActivity() != null) {
                        urlCost = getTaxiUrlSearchMarkers("costSearchMarkers", getActivity());
                        sendUrlMapCost = CostJSONParser.sendURL(urlCost);
                        assert sendUrlMapCost != null;
                        String orderCost = (String) sendUrlMapCost.get("order_cost");
                        Log.d(TAG, "onDismiss: orderCost " + orderCost);
                        assert orderCost != null;
                        if (!orderCost.equals("0")) {
                            String costUpdate;
                            String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(3);
                            long discountInt = Long.parseLong(discountText);
                            long discount;
                            long firstCost = Long.parseLong(orderCost);
                            discount = firstCost * discountInt / 100;

                            firstCost = firstCost + discount;
                            updateAddCost(String.valueOf(discount));

                            GalleryFragment.costFirstForMin = firstCost;
                            costUpdate = String.valueOf(firstCost);
                            textView.setText(costUpdate);
                        }
                    }
                }


            } catch (MalformedURLException ignored) {

            }

        }
        progressBar.setVisibility(View.GONE);
        btn_ok.setVisibility(View.VISIBLE);
    }
    private void updateAddCost(String addCost) {
        ContentValues cv = new ContentValues();
        Log.d(TAG, "updateAddCost: addCost" + addCost);
        cv.put("addCost", addCost);

        // обновляем по id
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getTaxiUrlSearch(String urlAPI, Context context) throws UnsupportedEncodingException {

        List<String> stringListRout = logCursor(MainActivity.ROUT_HOME);

        String originalString = stringListRout.get(1);
        int indexOfSlash = originalString.indexOf("/");
        String from = (indexOfSlash != -1) ? originalString.substring(0, indexOfSlash) : originalString;

        String from_number = stringListRout.get(2);

        originalString = stringListRout.get(3);
        indexOfSlash = originalString.indexOf("/");
        String to = (indexOfSlash != -1) ? originalString.substring(0, indexOfSlash) : originalString;

        String to_number = stringListRout.get(4);

        // Origin of route
        String str_origin = from + "/" + from_number;

        // Destination of route
        String str_dest = to + "/" + to_number;



        List<String> stringList = logCursor(MainActivity.TABLE_SETTINGS_INFO);
        String tarif =  stringList.get(2);
        String payment_type = stringList.get(4);

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO).get(4);

        if(urlAPI.equals("costSearch")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type;
        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i <= 14 ; i++) {
            if(services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if(servicesVer) {
            for (int i = 0; i < arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if(servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Log.d(TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "https://m.easy-order-taxi.site/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);


        Log.d(TAG, "getTaxiUrlSearch: " + url);

        return url;
    }

    @SuppressLint("Range")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getTaxiUrlSearchMarkers(String urlAPI, Context context) {

//        List<String> stringListRout = logCursor(MainActivity.ROUT_MARKER);
//        Log.d(TAG, "getTaxiUrlSearch: stringListRout" + stringListRout);
//
//        double originLatitude = Double.parseDouble(stringListRout.get(1));
//        double originLongitude = Double.parseDouble(stringListRout.get(2));
//        double toLatitude = Double.parseDouble(stringListRout.get(3));
//        double toLongitude = Double.parseDouble(stringListRout.get(4));
        double originLatitude = 0;
        double originLongitude = 0;
        double toLatitude = 0;
        double toLongitude = 0;

        String[] projection = {
                "startLat",
                "startLan",
                "to_lat",
                "to_lng"
        };

        String selection = "id = ?";
        String[] selectionArgs = { "1" }; // предполагается, что вы хотите получить данные для записи с id=1
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(
                MainActivity.ROUT_MARKER, // имя таблицы
                projection,   // столбцы, которые вы хотите получить
                selection,    // условие выборки
                selectionArgs,// аргументы условия выборки
                null,         // группировка строк
                null,         // условие группировки строк
                null          // порядок сортировки
        );

        if (cursor != null && cursor.moveToFirst()) {
            originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
            originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
            toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
            toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));

            // Теперь у вас есть значения из базы данных
            Log.d(TAG, "StartLat: " + originLatitude + ", StartLan: " + originLongitude + ", ToLat: " + toLatitude + ", ToLng: " + toLongitude);

            cursor.close();
        } else {
            // Обработка случая, когда данных нет
            Log.e(TAG, "No data found in ROUT_MARKER table");
        }


        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;


        List<String> stringList = logCursor(MainActivity.TABLE_SETTINGS_INFO);
        String tarif =  stringList.get(2);
        String payment_type = stringList.get(4);

        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO).get(4);

        if(urlAPI.equals("costSearchMarkers")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type;
        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i < services.size()-1 ; i++) {
            if(services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if(servicesVer) {
            for (int i = 0; i < OpenStreetMapActivity.arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(OpenStreetMapActivity.arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if(servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Log.d(TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "https://m.easy-order-taxi.site/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);
        database.close();
        return url;
    }


    @Override
    public void onDetach() {
        super.onDetach();
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
    public static String[] arrayServiceCode() {
        return new String[]{
                "BAGGAGE",
                "ANIMAL",
                "CONDIT",
                "MEET",
                "COURIER",
                "CHECK_OUT",
                "BABY_SEAT",
                "DRIVER",
                "NO_SMOKE",
                "ENGLISH",
                "CABLE",
                "FUEL",
                "WIRES",
                "SMOKE",
        };
    }
    @SuppressLint("Range")
    private List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();

        SQLiteDatabase  database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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

        assert c != null;
        c.close();
        database.close();
        return list;
    }
   }

