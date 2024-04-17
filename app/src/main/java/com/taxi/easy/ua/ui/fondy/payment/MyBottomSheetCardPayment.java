package com.taxi.easy.ua.ui.fondy.payment;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.taxi.easy.ua.ui.finish.OrderResponse;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
    private boolean timeout;
    private static String timeoutText;
    private static String messageWaitingCarSearch;
    private static String messageSearchesForCar;
    private static String messageCanceled;
    private static String messageCarFoundOrderCarInfo;
    private static String messageCarFoundOrderdriverPhone;
    private static String messageCarFoundRequiredTime;
    private static String def_status;
    private String order_id;
    private String invoiceId;
    private static String city;
    private static String api;
    private static String application;
    private static String comment;
    private static String MERCHANT_ID;
    private static String merchantPassword;
    private Context context;

    public MyBottomSheetCardPayment(
            String checkoutUrl,
            String amount,
            String uid,
            String uid_Double,
            Context context
    ) {
        this.checkoutUrl = checkoutUrl;
        this.amount = amount;
        this.uid = uid;
        this.uid_Double = uid_Double;
        this.hold = false;
        this.context = context;
    }



    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fondy_payment, container, false);
        webView = view.findViewById(R.id.webView);
        email = logCursor(MainActivity.TABLE_USER_INFO, requireActivity()).get(3);
        pay_method =  pay_system(getContext());
  
        timeoutText = context.getString(R.string.time_out_text);
        messageWaitingCarSearch = context.getString(R.string.ex_st_1);
        messageSearchesForCar = context.getString(R.string.ex_st_0);
        messageCanceled = context.getString(R.string.ex_st_canceled);
        messageCarFoundOrderCarInfo = context.getString(R.string.ex_st_3);
        messageCarFoundOrderdriverPhone = context.getString(R.string.ex_st_4);
        messageCarFoundRequiredTime = context.getString(R.string.ex_st_5);
        def_status = context.getString(R.string.def_status);
        application = context.getString(R.string.application);
        comment = context.getString(R.string.fondy_revers_message) + context.getString(R.string.fondy_message);;
        
        List<String> listCity = logCursor(MainActivity.CITY_INFO, requireActivity());
        city = listCity.get(1);
        api = listCity.get(2);

      
        MERCHANT_ID = listCity.get(6);
        merchantPassword = listCity.get(7);
        
        order_id = MainActivity.order_id;
        invoiceId = MainActivity.invoiceId;

        Log.d(TAG, "onCreateView:timeoutText " + timeoutText);
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
        // Таймер оплаты
        startPaymentTimer();
        return view;
    }

    private void getStatusFondy() {
        hold = false;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FondyApiService apiService = retrofit.create(FondyApiService.class);

        List<String>  arrayList = logCursor(MainActivity.CITY_INFO, requireActivity());
        String MERCHANT_ID = arrayList.get(6);
        String merchantPassword = arrayList.get(7);

        StatusRequestBody requestBody = new StatusRequestBody(
                order_id,
                MERCHANT_ID,
                merchantPassword
        );
        StatusRequest statusRequest = new StatusRequest(requestBody);
        Log.d(TAG, "getUrlToPayment: " + statusRequest.toString());

        Call<ApiResponse<SuccessfulResponseData>> call = apiService.checkOrderStatus(statusRequest);

        call.enqueue(new Callback<ApiResponse<SuccessfulResponseData>>() {
            @SuppressLint("NewApi")
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SuccessfulResponseData>> call, Response<ApiResponse<SuccessfulResponseData>> response) {
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
                    Log.d(TAG, "onResponse: Ошибка запроса, код " + response.code());

                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SuccessfulResponseData>> call, @NonNull Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d(TAG, "onFailure: Ошибка сети: " + t.getMessage());

                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });


    }

    private void getStatusMono() {
        hold = false;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.monobank.ua/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MonoApi monoApi = retrofit.create(MonoApi.class);

        String token = context.getString(R.string.mono_key_storage); // Получение токена из ресурсов
        Call<ResponseStatusMono> call = monoApi.getInvoiceStatus(token, invoiceId);

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
                            cancelOrderRevers(uid);
                        } else {
                            hold = true;

//                            getCardToken();
                        }
                        dismiss();
                    }
                } else {
                    // Обработка ошибки запроса
                    Log.d(TAG, "onResponse: Ошибка запроса, код " + response.code());
                    cancelOrderRevers(uid);
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseStatusMono> call, @NonNull Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d(TAG, "onFailure: Ошибка сети: " + t.getMessage());
                cancelOrderRevers(uid);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });

    }

    private void cancelOrderRevers(String value) {

        String url = baseUrl + "/" + api + "/android/webordersCancel/" + value + "/" + city  + "/" + application;

        Call<Status> call = ApiClient.getApiService().cancelOrder(url);
        Log.d(TAG, "cancelOrderWithDifferentValue cancelOrderUrl: " + url);

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful()) {
                    Status status = response.body();
                    if (status != null) {

                        String result =  String.valueOf(status.getResponse());
                        Log.d(TAG, "onResponse: result" + result);
                        timeoutText = result;

                        String comment = context.getString(R.string.fondy_revers_message) + context.getString(R.string.fondy_message);;
                        switch (pay_method) {
                            case "fondy_payment":
                                getReversFondy(order_id, comment, amount);
                                break;
                            case "mono_payment":
                                getReversMono(invoiceId, comment, Integer.parseInt(amount));
                                break;
                        }
                        dismiss();
                    }
                } else {
                    // Обработка неуспешного ответа
                    if (pay_method.equals("nal_payment")) {
                        timeoutText = context.getString(R.string.verify_internet);
                    }
                }
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d(TAG, "onFailure: " + errorMessage);
                timeoutText = context.getString(R.string.verify_internet);
            }
        });
    }
    private void cancelOrderDismiss() {

        String url = baseUrl + "/" + api + "/android/webordersCancelDouble/" + uid+ "/" + uid_Double + "/" + pay_method + "/" + city  + "/" + context.getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrderDouble(url);
        Log.d(TAG, "cancelOrderDouble: " + url);
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                Status status = response.body();
                if (status != null) {
                    FinishActivity.text_status.setText(timeoutText);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d(TAG, "onFailure: " + errorMessage);
            }
        });
    }

    private void cancelOrderDouble() {
        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = baseUrl + "/" + api + "/android/webordersCancelDouble/" + uid+ "/" + uid_Double + "/" + pay_method + "/" + city  + "/" + context.getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrderDouble(url);
        Log.d(TAG, "cancelOrderDouble: " + url);

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful()) {
                    String comment = context.getString(R.string.fondy_revers_message) + context.getString(R.string.fondy_message);;
                    switch (pay_method) {
                        case "fondy_payment":
                            getReversFondy(order_id, comment, amount);
                            break;
                        case "mono_payment":
                            getReversMono(invoiceId, comment, Integer.parseInt(amount));
                            break;
                    }
                    FinishActivity.btn_cancel_order.setVisibility(View.GONE);
                    FinishActivity.btn_reset_status.setVisibility(View.GONE);

                    if(!timeout) {
                        FinishActivity.text_status.setText(timeoutText);
                    } else {
                        FinishActivity.text_status.setText(context.getString(R.string.ex_st_canceled));
                    }
                } else {
                   FinishActivity.text_status.setText(R.string.verify_internet);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d(TAG, "onFailure: " + errorMessage);
                FinishActivity.text_status.setText(R.string.verify_internet);
            }
        });
    }
    private void getReversFondy(String orderId, String comment, String amount) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ReversApi apiService = retrofit.create(ReversApi.class);

        ReversRequestData reversRequestData = new ReversRequestData(
                orderId,
                comment,
                amount,
                MERCHANT_ID,
                merchantPassword
        );
        Log.d(TAG, "getRevers: " + reversRequestData.toString());
        ReversRequestSent reversRequestSent = new ReversRequestSent(reversRequestData);


        Call<ApiResponseRev<SuccessResponseDataRevers>> call = apiService.makeRevers(reversRequestSent);

        call.enqueue(new Callback<ApiResponseRev<SuccessResponseDataRevers>>() {
            @Override
            public void onResponse(Call<ApiResponseRev<SuccessResponseDataRevers>> call, Response<ApiResponseRev<SuccessResponseDataRevers>> response) {

                if (response.isSuccessful()) {
                    ApiResponseRev<SuccessResponseDataRevers> apiResponse = response.body();
                    Log.d(TAG, "JSON Response: " + new Gson().toJson(apiResponse));
                    if (apiResponse != null) {
                        SuccessResponseDataRevers responseData = apiResponse.getResponse();
                        Log.d(TAG, "onResponse: " + responseData.toString());
                        if (responseData != null) {
                            // Обработка успешного ответа
                            Log.d(TAG, "onResponse: " + responseData.toString());

                        }
                    }
                } else {
                    // Обработка ошибки запроса
                    Log.d(TAG, "onResponse: Ошибка запроса, код " + response.code());
                    try {
                        String errorBody = response.errorBody().string();
                        Log.d(TAG, "onResponse: Тело ошибки: " + errorBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void onFailure(@NonNull Call<ApiResponseRev<SuccessResponseDataRevers>> call, @NonNull Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d(TAG, "onFailure: Ошибка сети: " + t.getMessage());
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
        Log.d(TAG, "getRevers: " + paymentRequest.toString());

        String token = context.getString(R.string.mono_key_storage); // Получение токена из ресурсов
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

    private String pay_system(Context context) {
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
//                    if(isAdded()){
                        ContentValues cv = new ContentValues();
                        cv.put("payment_type", paymentCodeNew);
                        // обновляем по id
                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                new String[] { "1" });
                        database.close();
//                    }


                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());

                }
            }

            @Override
            public void onFailure(Call<ResponsePaySystem> call, Throwable t) {
                if (isAdded()) {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }
        });
        return logCursor(MainActivity.TABLE_SETTINGS_INFO, requireActivity()).get(4);
    }
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d(TAG, "onDismiss: timeout " + timeout);
        Log.d(TAG, "onDismiss: hold " + hold);


        statusOrderWithDifferentValue(uid, true);
        if(timeout) {
            stopPaymentTimer();
        }

//        statusOrderWithDifferentValue(uid_Double, false);
    }

    private void statusOrderWithDifferentValue(String value, boolean info) {

        String url = baseUrl + "/" + api + "/android/historyUIDStatus/" + value + "/" + city  + "/" + application;

        Call<OrderResponse> call = ApiClient.getApiService().statusOrder(url);
        Log.d(TAG, "/android/historyUIDStatus/: " + url);

        // Выполняем запрос асинхронно
        call.enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                if (response.isSuccessful()) {
                    // Получаем объект OrderResponse из успешного ответа
                    OrderResponse orderResponse = response.body();

                    // Далее вы можете использовать полученные данные из orderResponse
                    // например:
                    assert orderResponse != null;
                    String executionStatus = orderResponse.getExecutionStatus();
                    String orderCarInfo = orderResponse.getOrderCarInfo();
                    String driverPhone = orderResponse.getDriverPhone();
                    String requiredTime = orderResponse.getRequiredTime();
                    if (requiredTime != null && !requiredTime.isEmpty()) {
                        requiredTime = formatDate (orderResponse.getRequiredTime());
                    }

                    String message;
                    // Обработка различных вариантов executionStatus
                    switch (executionStatus) {
                        case "WaitingCarSearch":
                            message = messageWaitingCarSearch;
                            if(!hold && info) {
                                cancelOrderDouble();
                            }
                            break;
                        case "SearchesForCar":
                            message = messageSearchesForCar;
                            if(!hold && info) {
                                cancelOrderDouble();
                            }
                            break;
                        case "Canceled":
                            message = messageCanceled;
                            FinishActivity.btn_cancel_order.setVisibility(View.GONE);
                            FinishActivity.btn_reset_status.setVisibility(View.GONE);

                            switch (pay_method) {
                                case "fondy_payment":
                                    getReversFondy(order_id, comment, amount);
                                    break;
                                case "mono_payment":
                                    getReversMono(invoiceId, comment, Integer.parseInt(amount));
                                    break;
                            }
                            break;
                        case "CarFound":
                            // Формируем сообщение с учетом возможных пустых значений переменных
                            StringBuilder messageBuilder = new StringBuilder(context.getString(R.string.ex_st_2));

                            if (orderCarInfo != null && !orderCarInfo.isEmpty()) {
                                messageBuilder.append(messageCarFoundOrderCarInfo).append(orderCarInfo);
                            }

                            if (driverPhone != null && !driverPhone.isEmpty()) {
                                messageBuilder.append(messageCarFoundOrderdriverPhone).append(driverPhone);
                            }

                            if (requiredTime != null && !requiredTime.isEmpty()) {
                                messageBuilder.append(messageCarFoundRequiredTime).append(requiredTime);
                            }

                            message = messageBuilder.toString();
                            if(!hold && info) {

                                FinishActivity.handler.removeCallbacks(FinishActivity.myRunnable);
                                cancelOrderDouble();
                            }
                            break;
                        default:
                            message = def_status;
                            cancelOrderDouble();
                            break;
                    }

                    if(!timeout) {
                        message = timeoutText;
                    }

                    FinishActivity.text_status.setText(message);

                } else {
                    FinishActivity.text_status.setText(def_status);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                FinishActivity.text_status.setText(def_status);
            }
        });
    }

    private String formatDate (String requiredTime) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        // Формат для вывода в украинской локализации
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("uk", "UA"));
        // Преобразуем строку в объект Date
        Date date = null;
        try {
            date = inputFormat.parse(requiredTime);
        } catch (ParseException e) {
            Log.d(TAG, "onCreate:" + new RuntimeException(e));
        }

        // Форматируем дату и время в украинском формате
        return outputFormat.format(date);

    }
    private void getCardToken(String pay_system) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site") // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Создайте сервис
        CallbackService service = retrofit.create(CallbackService.class);

        // Выполните запрос
        Call<CallbackResponse> call = service.handleCallback(email, pay_system, MERCHANT_ID);

        String tableCard = "";
        switch (pay_system) {
            case "mono":
                tableCard = MainActivity.TABLE_MONO_CARDS;
                break;
            default:
                tableCard = MainActivity.TABLE_FONDY_CARDS;
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
                            database.delete(finalTableCard, "1", null);
                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты

                                Log.d(TAG, "onResponse: card_token 11111: " + rectoken);

                                if (isAdded()) {
                                    // Если нет записи с таким rectoken, добавляем новую запись
                                    ContentValues cv = new ContentValues();
                                    cv.put("masked_card", masked_card);
                                    cv.put("card_type", card_type);
                                    cv.put("bank_name", bank_name);
                                    cv.put("rectoken", rectoken);
                                    cv.put("merchant", MERCHANT_ID);
                                    cv.put("rectoken_check", "-1");
                                    database.insert(finalTableCard, null, cv);
                                }
                            }
                            // Выбираем минимальное значение ID из таблицы
                            Cursor cursor = database.query(finalTableCard, new String[]{"MIN(id)"}, null, null, null, null, null);
                            if (cursor != null && cursor.moveToFirst()) {
                                int minId = cursor.getInt(0);
                                cursor.close();

                                ContentValues cv = new ContentValues();
                                cv.put("rectoken_check", "1");
                                database.update(finalTableCard, cv, "id = ?", new String[] { String.valueOf(minId) });
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

    private static final int TIMEOUT_SECONDS = 60;
    private CountDownTimer paymentTimer;


    private void startPaymentTimer() {
        paymentTimer = new CountDownTimer(TIMEOUT_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Таймер идет, ничего не делаем
                timeout = true;
            }

            @Override
            public void onFinish() {
                // Таймер завершился, обрабатываем таймаут
                timeout = false;
                FinishActivity.text_status.setText(getResources().getString(R.string.time_out_text));
                cancelOrderDismiss();
            }
        }.start();
    }

    private void stopPaymentTimer() {
        if (paymentTimer != null) {
            paymentTimer.cancel();
        }
    }

}

