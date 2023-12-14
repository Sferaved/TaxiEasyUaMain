package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.fondy.callback.CallbackResponse;
import com.taxi.easy.ua.ui.fondy.callback.CallbackService;
import com.taxi.easy.ua.ui.fondy.revers.ApiResponseRev;
import com.taxi.easy.ua.ui.fondy.revers.ReversApi;
import com.taxi.easy.ua.ui.fondy.revers.ReversRequestData;
import com.taxi.easy.ua.ui.fondy.revers.ReversRequestSent;
import com.taxi.easy.ua.ui.fondy.revers.SuccessResponseDataRevers;
import com.taxi.easy.ua.ui.fondy.status.ApiResponse;
import com.taxi.easy.ua.ui.fondy.status.FondyApiService;
import com.taxi.easy.ua.ui.fondy.status.StatusRequest;
import com.taxi.easy.ua.ui.fondy.status.StatusRequestBody;
import com.taxi.easy.ua.ui.fondy.status.SuccessfulResponseData;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.mono.MonoApi;
import com.taxi.easy.ua.ui.mono.cancel.RequestCancelMono;
import com.taxi.easy.ua.ui.mono.cancel.ResponseCancelMono;
import com.taxi.easy.ua.ui.mono.status.ResponseStatusMono;
import com.taxi.easy.ua.ui.payment_system.PayApi;
import com.taxi.easy.ua.ui.payment_system.ResponsePaySystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyBottomSheetCardVerification extends BottomSheetDialogFragment {
    private WebView webView;
    private String TAG = "TAG3";
    private String checkoutUrl;
    private String amount;
    private AppCompatButton btnOk;
    String email;
    String pay_method;
    static SQLiteDatabase database;
    private final String baseUrl = "https://m.easy-order-taxi.site";
    Context context;

    public MyBottomSheetCardVerification(String checkoutUrl, String amount) {
        this.checkoutUrl = checkoutUrl;
        this.amount = amount;
    }

    @SuppressLint({"MissingInflatedId", "SetJavaScriptEnabled"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fondy_payment, container, false);
        webView = view.findViewById(R.id.webView);
        email = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        Log.d(TAG, "onCreateView: "  );
        context = getActivity();
        database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//        getCardTokenFondy();
        pay_system();
        // Настройка WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                Log.d("WebView", "Загружен URL: " + url);
                if(url.equals("https://m.easy-order-taxi.site/mono/redirectUrl")) {
                    switch (pay_method) {
                        case "fondy_payment":
                            getStatusFondy();
                            break;
                        case "mono_payment":
                            getStatusMono();
                            break;
                    }
                    return true;
                } else {
                    // Возвращаем false, чтобы разрешить WebView загрузить страницу.
                    return false;
                }
            }
        });
        webView.loadUrl(Objects.requireNonNull(checkoutUrl));
        return view;
    }



    /**
     * Fondy section
     */
    private void getStatusFondy() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FondyApiService apiService = retrofit.create(FondyApiService.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO);
        String MERCHANT_ID = arrayList.get(6);
        String merchantPassword = arrayList.get(7);

        StatusRequestBody requestBody = new StatusRequestBody(
                MainActivity.order_id,
                MERCHANT_ID,
                merchantPassword
        );
        StatusRequest statusRequest = new StatusRequest(requestBody);
        Log.d("TAG1", "getUrlToPayment: " + statusRequest.toString());

        Call<ApiResponse<SuccessfulResponseData>> call = apiService.checkOrderStatus(statusRequest);

        call.enqueue(new Callback<ApiResponse<SuccessfulResponseData>>() {
            @SuppressLint("NewApi")
            @Override
            public void onResponse(Call<ApiResponse<SuccessfulResponseData>> call, Response<ApiResponse<SuccessfulResponseData>> response) {
                if (response.isSuccessful()) {
                    ApiResponse<SuccessfulResponseData> apiResponse = response.body();
                    Log.d(TAG, "JSON Response: " + new Gson().toJson(apiResponse));
                    if (apiResponse != null) {
                        SuccessfulResponseData responseData = apiResponse.getResponse();
                        Log.d(TAG, "onResponse: " + responseData.toString());
                        // Обработка успешного ответа
                        Log.d("TAG", "getMerchantId: " + responseData.getMerchantId());
                        Log.d("TAG", "getOrderStatus: " + responseData.getOrderStatus());
                        Log.d("TAG", "getResponse_description: " + responseData.getResponseDescription());
                        String orderStatus = responseData.getOrderStatus();
                        if(orderStatus.equals("approved")){
                            getCardTokenFondy();
                            getReversFondy(MainActivity.order_id,getString(R.string.return_pay), amount);
                        };
                    }
                } else {
                    // Обработка ошибки запроса
                    Log.d("TAG", "onResponse: Ошибка запроса, код " + response.code());

                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }

            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SuccessfulResponseData>> call, @NonNull Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d("TAG", "onFailure: Ошибка сети: " + t.getMessage());

                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });

    }
    private void getCardTokenFondy() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site") // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Создайте сервис
        CallbackService service = retrofit.create(CallbackService.class);
        Log.d(TAG, "getCardTokenFondy: ");
        // Выполните запрос
        Call<CallbackResponse> call = service.handleCallback(email, "fondy");
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
                            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты

                                Log.d(TAG, "onResponse: card_token: " + rectoken);


                                    // Проверяем, есть ли запись с таким rectoken в таблице
                                    Cursor cursor = database.query(
                                            MainActivity.TABLE_FONDY_CARDS,
                                            new String[]{"rectoken"},
                                            "rectoken = ?",
                                            new String[]{rectoken},
                                            null,
                                            null,
                                            null
                                    );
//                                    Log.d(TAG, "onResponse: cursor.getCount() " + cursor.getCount());
                                    if (cursor.getCount() == 0) {
                                        // Если нет записи с таким rectoken, добавляем новую запись
                                        ContentValues cv = new ContentValues();
                                        cv.put("masked_card", masked_card);
                                        cv.put("card_type", card_type);
                                        cv.put("bank_name", bank_name);
                                        cv.put("rectoken", rectoken);
                                        cv.put("rectoken_check", "1");
                                        database.insert(MainActivity.TABLE_FONDY_CARDS, null, cv);
                                    }

                                    cursor.close();


                            }


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
            }
        });
    }
    private void getReversFondy(String orderId, String comment, String amount) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ReversApi apiService = retrofit.create(ReversApi.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO);
        String MERCHANT_ID = arrayList.get(6);
        String merchantPassword = arrayList.get(7);

        ReversRequestData reversRequestData = new ReversRequestData(
                orderId,
                comment,
                amount,
                MERCHANT_ID,
                merchantPassword
        );
        Log.d("TAG1", "getRevers: " + reversRequestData.toString());
        ReversRequestSent reversRequestSent = new ReversRequestSent(reversRequestData);


        Call<ApiResponseRev<SuccessResponseDataRevers>> call = apiService.makeRevers(reversRequestSent);

        call.enqueue(new Callback<ApiResponseRev<SuccessResponseDataRevers>>() {
            @Override
            public void onResponse(Call<ApiResponseRev<SuccessResponseDataRevers>> call, Response<ApiResponseRev<SuccessResponseDataRevers>> response) {

                if (response.isSuccessful()) {
                    ApiResponseRev<SuccessResponseDataRevers> apiResponse = response.body();
                    Log.d("TAG1", "JSON Response: " + new Gson().toJson(apiResponse));
                    if (apiResponse != null) {
                        SuccessResponseDataRevers responseData = apiResponse.getResponse();
                        Log.d("TAG1", "onResponse: " + responseData.toString());
                        if (responseData != null) {
                            // Обработка успешного ответа
                            Log.d("TAG1", "onResponse: " + responseData.toString());
//                            if (isAdded()) { // Проверяем, что фрагмент присоединен к активности
                                if (response.isSuccessful()) {
                                    if(isAdded()) {
                                        Toast.makeText(context, getString(R.string.link_card_succesfuly), Toast.LENGTH_SHORT).show();
                                    }


                                    ArrayList<Map<String, String>> cardMaps = getCardMapsFromDatabase();
                                    Log.d("TAG", "onResume: cardMaps" + cardMaps);
                                    if (!cardMaps.isEmpty()) {
                                        // Если массив пустой, отобразите текст "no_routs" вместо списка
                                        CardFragment.textCard.setVisibility(View.GONE);

                                        CustomCardAdapter listAdapter = new CustomCardAdapter(context, getCardMapsFromDatabase(), CardFragment.table);
                                        CardFragment.listView.setAdapter(listAdapter);
                                        CardFragment.listView.setVisibility(View.VISIBLE);
                                        updateCardFragment();
                                    }
                                    dismiss();
//                                }
                            }
                        }
                    }
                } else {
                    // Обработка ошибки запроса
                    Log.d("TAG", "onResponse: Ошибка запроса, код " + response.code());

                    try {
                        String errorBody = response.errorBody().string();
                        Log.d("TAG", "onResponse: Тело ошибки: " + errorBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void onFailure(Call<ApiResponseRev<SuccessResponseDataRevers>> call, Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d("TAG", "onFailure: Ошибка сети: " + t.getMessage());

            }
        });

    }

    private void updateCardFragment() {
        // Создаем новый экземпляр фрагмента
        CardFragment newFragment = new CardFragment();

// Получаем менеджер фрагментов
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager(); // Если вы используете AppCompatActivity

// Начинаем транзакцию фрагментов
        FragmentTransaction transaction = fragmentManager.beginTransaction();

// Заменяем текущий фрагмент новым
        transaction.replace(R.id.fragment_card, newFragment); // R.id.fragment_container - это ID контейнера, в котором находится ваш фрагмент

// Добавляем транзакцию в стек возврата
        transaction.addToBackStack(null);

// Применяем транзакцию
        transaction.commit();

    }

    @SuppressLint("Range")
    private ArrayList<Map<String, String>> getCardMapsFromDatabase() {
        ArrayList<Map<String, String>> cardMaps = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        // Выполните запрос к таблице TABLE_FONDY_CARDS и получите данные
        Cursor cursor = database.query(MainActivity.TABLE_FONDY_CARDS, null, null, null, null, null, null);
        Log.d("TAG", "getCardMapsFromDatabase: card count: " + cursor.getCount());

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Map<String, String> cardMap = new HashMap<>();
                    cardMap.put("card_type", cursor.getString(cursor.getColumnIndex("card_type")));
                    cardMap.put("bank_name", cursor.getString(cursor.getColumnIndex("bank_name")));
                    cardMap.put("masked_card", cursor.getString(cursor.getColumnIndex("masked_card")));
                    cardMap.put("rectoken", cursor.getString(cursor.getColumnIndex("rectoken")));

                    cardMaps.add(cardMap);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        database.close();

        return cardMaps;
    }
    /**
     * Mono section
     */
    private void getStatusMono() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.monobank.ua/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MonoApi monoApi = retrofit.create(MonoApi.class);

        String token = getResources().getString(R.string.mono_key_storage); // Получение токена из ресурсов
        Call<ResponseStatusMono> call = monoApi.getInvoiceStatus(token, MainActivity.invoiceId);

        call.enqueue(new Callback<ResponseStatusMono>() {
            @SuppressLint("NewApi")
            @Override
            public void onResponse(@NonNull Call<ResponseStatusMono> call, @NonNull Response<ResponseStatusMono> response) {
                if (response.isSuccessful()) {
                    ResponseStatusMono apiResponse = response.body();
                    Log.d(TAG, "JSON Response: " + new Gson().toJson(apiResponse));
                    if (apiResponse != null) {
                        String status = apiResponse.getStatus();
                        Log.d(TAG, "onResponse: " + status);
                        // Обработка успешного ответа

//                            dismiss();
//                            getCardToken();

                    }
                } else {
                    // Обработка ошибки запроса
                    Log.d("TAG", "onResponse: Ошибка запроса, код " + response.code());

                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseStatusMono> call, @NonNull Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d("TAG", "onFailure: Ошибка сети: " + t.getMessage());

                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });

    }
    private void getReversMono(
            String invoiceId,
            String extRef,
            int amount
    ) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.monobank.ua/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MonoApi monoApi = retrofit.create(MonoApi.class);

        RequestCancelMono paymentRequest = new RequestCancelMono(
                invoiceId,
                extRef,
                amount
        );
        Log.d("TAG1", "getRevers: " + paymentRequest.toString());

        String token = getResources().getString(R.string.mono_key_storage); // Получение токена из ресурсов
        Call<ResponseCancelMono> call = monoApi.invoiceCancel(token, paymentRequest);

        call.enqueue(new Callback<ResponseCancelMono>() {
            @Override
            public void onResponse(@NonNull Call<ResponseCancelMono> call, @NonNull Response<ResponseCancelMono> response) {

                if (response.isSuccessful()) {
                    ResponseCancelMono apiResponse = response.body();
                    Log.d("TAG2", "JSON Response: " + new Gson().toJson(apiResponse));
                    if (apiResponse != null) {
                        String responseData = apiResponse.getStatus();
                        Log.d("TAG2", "onResponse: " + responseData.toString());
                        if (responseData != null) {
                            // Обработка успешного ответа

                            switch (responseData) {
                                case "processing":
                                    Log.d("TAG2", "onResponse: " + "заява на скасування знаходиться в обробці");
                                    break;
                                case "success":
                                    Log.d("TAG2", "onResponse: " + "заяву на скасування виконано успішно");
                                    break;
                                case "failure":
                                    Log.d("TAG2", "onResponse: " + "неуспішне скасування");
                                    Log.d("TAG2", "onResponse: ErrCode: " + apiResponse.getErrCode());
                                    Log.d("TAG2", "onResponse: ErrText: " + apiResponse.getErrText());
                                    break;
                            }

                        }
                    }
                } else {
                    // Обработка ошибки запроса
                    Log.d("TAG2", "onResponse: Ошибка запроса, код " + response.code());
                    try {
                        String errorBody = response.errorBody().string();
                        Log.d("TAG2", "onResponse: Тело ошибки: " + errorBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseCancelMono> call, Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d("TAG2", "onFailure: Ошибка сети: " + t.getMessage());
            }
        });

    }

    /**
     *
     * @param dialog the dialog that was dismissed will be passed into the
     *               method
     */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        CardFragment.progressBar.setVisibility(View.GONE);
        if(MainActivity.order_id !=null) {
            getReversFondy(MainActivity.order_id,getString(R.string.return_pay), amount);
        }
    }

    private void pay_system() {
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
                    String paymentCodeNew = "fondy";

                    switch (paymentCode) {
                        case "fondy":
                            pay_method = "fondy_payment";
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
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());

                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponsePaySystem> call, @NonNull Throwable t) {
//                if (isAdded()) {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
//                }
            }
        });
    }
    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        return list;
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
}

