package com.taxi.easy.ua.ui.fondy.payment;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.CardInfo;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.finish.Status;
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
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyBottomSheetCardPayment extends BottomSheetDialogFragment {
    private WebView webView;
    private String TAG = "MyBottomSheetCardPayment";
    private String checkoutUrl;
    private String amount;
    private AppCompatButton btnOk;
    String email, uid, uid_Double;
    private String pay_method;
    private final String baseUrl = "https://m.easy-order-taxi.site";
    private boolean hold;

    public MyBottomSheetCardPayment(
            String checkoutUrl,
            String amount,
            String uid,
            String uid_Double) {
        this.checkoutUrl = checkoutUrl;
        this.amount = amount;
        this.uid = uid;
        this.uid_Double = uid_Double;
        this.hold = false;
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fondy_payment, container, false);
        webView = view.findViewById(R.id.webView);
        email = logCursor(MainActivity.TABLE_USER_INFO, requireActivity()).get(3);
        pay_method =  pay_system();

        // Настройка WebView
        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Этот метод вызывается при каждой попытке загрузки новой страницы
                // внутри WebView. В параметре 'url' будет содержаться URL страницы.
                // Здесь вы можете сохранить URL или выполнить другие действия.

                // Пример сохранения URL в переменной
                String loadedUrl = url;
                Log.d("WebView", "Загружен URL: " + loadedUrl);
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

    private void getStatusFondy() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FondyApiService apiService = retrofit.create(FondyApiService.class);

        List<String>  arrayList = logCursor(MainActivity.CITY_INFO, requireActivity());
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

                        String orderStatus = responseData.getOrderStatus();
                        if(orderStatus.equals("approved")){
                            getCardToken("fondy");
                            hold = true;
                        } else {
                            hold = false;
                            dismiss();
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
            public void onFailure(Call<ApiResponse<SuccessfulResponseData>> call, Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d("TAG", "onFailure: Ошибка сети: " + t.getMessage());

                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });


    }

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

                        if(!status.equals("hold")){
                            hold = false;
                            cancelOrder(uid);
                        } else {
                            hold = true;
                            dismiss();
//                            getCardToken();
                        }
                    }
                } else {
                    // Обработка ошибки запроса
                    Log.d("TAG", "onResponse: Ошибка запроса, код " + response.code());
                    cancelOrder(uid);
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseStatusMono> call, @NonNull Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d("TAG", "onFailure: Ошибка сети: " + t.getMessage());
                cancelOrder(uid);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });

    }

    private void cancelOrder(String value) {
        String url = baseUrl + "/" + FinishActivity.api + "/android/webordersCancel/" + value;
        Call<Status> call = ApiClient.getApiService().cancelOrder(url);
        Log.d("TAG", "cancelOrderWithDifferentValue cancelOrderUrl: " + url);



        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                if (response.isSuccessful()) {
                    Status status = response.body();
                    if (status != null) {

                        String result =  String.valueOf(status.getResponse());
                        Log.d("TAG", "onResponse: result" + result);
                        FinishActivity.text_status.setText(result);

                        String comment = getString(R.string.fondy_revers_message) + getString(R.string.fondy_message);;
                        switch (pay_method) {
                            case "fondy_payment":
                                getReversFondy(MainActivity.order_id, comment, amount);
                                break;
                            case "mono_payment":
                                getReversMono(MainActivity.invoiceId, comment, Integer.parseInt(amount));
                                break;
                        }
                        dismiss();
                    }
                } else {
                    // Обработка неуспешного ответа
                    if (pay_method.equals("nal_payment")) {
                        FinishActivity.text_status.setText(R.string.verify_internet);
                    }
                }
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d("TAG", "onFailure: " + errorMessage);
                FinishActivity.text_status.setText(R.string.verify_internet);
            }
        });
    }
    private void cancelOrderDismiss(String value) {
        String url = baseUrl + "/" + FinishActivity.api + "/android/webordersCancel/" + value;
        Call<Status> call = ApiClient.getApiService().cancelOrder(url);
        Log.d("TAG", "cancelOrderWithDifferentValue cancelOrderUrl: " + url);

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                Status status = response.body();
                if (status != null) {

                    String result =  String.valueOf(status.getResponse());
                    Log.d("TAG", "onResponse: result" + result);
                    FinishActivity.text_status.setText(result);

                } else {
                    FinishActivity.text_status.setText(R.string.verify_internet);
                }
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d("TAG", "onFailure: " + errorMessage);

            }
        });
    }
    private void getReversFondy(String orderId, String comment, String amount) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ReversApi apiService = retrofit.create(ReversApi.class);

        List<String>  arrayList = logCursor(MainActivity.CITY_INFO, requireActivity());
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

    private String pay_system() {
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
                            paymentCodeNew = "fondy_payment";
                            break;
                        case "mono":
                            paymentCodeNew = "mono_payment";
                            break;
                    }
                    if(isAdded()){
                        ContentValues cv = new ContentValues();
                        cv.put("payment_type", paymentCodeNew);
                        // обновляем по id
                        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
            public void onFailure(Call<ResponsePaySystem> call, Throwable t) {
                if (isAdded()) {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }
        });
        return logCursor(MainActivity.TABLE_SETTINGS_INFO, requireActivity()).get(4);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(!hold) {
            FinishActivity.btn_cancel_order.setVisibility(View.GONE);
            FinishActivity.btn_reset_status.setVisibility(View.GONE);
            cancelOrderDismiss(uid);
            cancelOrderDismiss(uid_Double);
        }
    }

    private void getCardToken(String pay_system) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site") // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Создайте сервис
        CallbackService service = retrofit.create(CallbackService.class);

        // Выполните запрос
        Call<CallbackResponse> call = service.handleCallback(email, pay_system);

        String tableCard = new String();
        switch (pay_system) {
            case "fondy":
                tableCard = MainActivity.TABLE_FONDY_CARDS;
                break;
            case "mono":
                tableCard = MainActivity.TABLE_MONO_CARDS;
                break;
        }
        String finalTableCard = tableCard;
        call.enqueue(new Callback<CallbackResponse>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponse> call, @NonNull Response<CallbackResponse> response) {
                if (response.isSuccessful()) {
                    CallbackResponse callbackResponse = response.body();
                    if (callbackResponse != null) {
                        List<CardInfo> cards = callbackResponse.getCards();

                        if (cards != null && !cards.isEmpty()) {
                            SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты

                                Log.d(TAG, "onResponse: card_token 11111: " + rectoken);

                                if (isAdded()) {
                                    // Проверяем, есть ли запись с таким rectoken в таблице
                                    Cursor cursor = database.query(
                                            finalTableCard,
                                            new String[]{"rectoken"},
                                            "rectoken = ?",
                                            new String[]{rectoken},
                                            null,
                                            null,
                                            null
                                    );

                                    if (cursor.getCount() == 0) {
                                        // Если нет записи с таким rectoken, добавляем новую запись
                                        ContentValues cv = new ContentValues();
                                        cv.put("masked_card", masked_card);
                                        cv.put("card_type", card_type);
                                        cv.put("bank_name", bank_name);
                                        cv.put("rectoken", rectoken);
                                        cv.put("rectoken_check", "1");
                                        database.insert(finalTableCard, null, cv);
                                    }

                                    cursor.close();
                                }
                            }

                            database.close();
                        }
                    }

                } else {
                    // Обработка случаев, когда ответ не 200 OK
                }
                dismiss();
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponse> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
            }
        });
    }
    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
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
        return list;
    }
}

