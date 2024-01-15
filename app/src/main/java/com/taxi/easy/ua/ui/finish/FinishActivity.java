package com.taxi.easy.ua.ui.finish;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.fondy.payment.ApiResponsePay;
import com.taxi.easy.ua.ui.fondy.payment.MyBottomSheetCardPayment;
import com.taxi.easy.ua.ui.fondy.payment.PaymentApi;
import com.taxi.easy.ua.ui.fondy.payment.RequestData;
import com.taxi.easy.ua.ui.fondy.payment.StatusRequestPay;
import com.taxi.easy.ua.ui.fondy.payment.SuccessResponseDataPay;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.fondy.revers.ApiResponseRev;
import com.taxi.easy.ua.ui.fondy.revers.ReversApi;
import com.taxi.easy.ua.ui.fondy.revers.ReversRequestData;
import com.taxi.easy.ua.ui.fondy.revers.ReversRequestSent;
import com.taxi.easy.ua.ui.fondy.revers.SuccessResponseDataRevers;
import com.taxi.easy.ua.ui.fondy.token_pay.ApiResponseToken;
import com.taxi.easy.ua.ui.fondy.token_pay.PaymentApiToken;
import com.taxi.easy.ua.ui.fondy.token_pay.RequestDataToken;
import com.taxi.easy.ua.ui.fondy.token_pay.StatusRequestToken;
import com.taxi.easy.ua.ui.fondy.token_pay.SuccessResponseDataToken;
import com.taxi.easy.ua.ui.home.MyBottomSheetBlackListFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.mono.MonoApi;
import com.taxi.easy.ua.ui.mono.cancel.RequestCancelMono;
import com.taxi.easy.ua.ui.mono.cancel.ResponseCancelMono;
import com.taxi.easy.ua.ui.mono.payment.RequestPayMono;
import com.taxi.easy.ua.ui.mono.payment.ResponsePayMono;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FinishActivity extends AppCompatActivity {
    private static final String TAG = "TAG_FINISH";
    public static TextView text_status;

    public static String baseUrl = "https://m.easy-order-taxi.site";
    Map<String, String> receivedMap;
    public static String uid;
    Thread thread;
    String pay_method;

    public static String amount;
    public static TextView text_full_message;
    String messageResult;
    public static String messageFondy;
    public static String uid_Double;
    public static Button btn_reset_status;
    public static Button btn_cancel_order;
    private long delayMillis;
    public static Runnable myRunnable;
    public static Handler handler;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finish);
        new VerifyUserTask().execute();
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(4);
        Log.d(TAG, "onCreate: " + pay_method);
        messageFondy = getString(R.string.fondy_message);

        messageResult = getIntent().getStringExtra("messageResult_key");

        receivedMap = (HashMap<String, String>) getIntent().getSerializableExtra("sendUrlMap");
        amount = receivedMap.get("order_cost") + "00";

        Log.d(TAG, "onCreate: receivedMap" + receivedMap.toString());
        text_full_message = findViewById(R.id.text_full_message);
        text_full_message.setText(messageResult);

        uid = getIntent().getStringExtra("UID_key");
        uid_Double = receivedMap.get("dispatching_order_uid_Double");

        text_status = findViewById(R.id.text_status);
        statusOrderWithDifferentValue(uid);


        btn_reset_status = findViewById(R.id.btn_reset_status);
        btn_reset_status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connected()){
                    statusOrderWithDifferentValue(uid);
                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }
        });

        btn_cancel_order = findViewById(R.id.btn_cancel_order);
        delayMillis = 5 * 60 * 1000;

        handler = new Handler();

        if (pay_method.equals("bonus_payment")) {

             String url = baseUrl + "/bonusBalance/recordsBloke/" + uid;

             fetchBonus(url);
             handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.order_id = null;
                        String cancelText = getApplicationContext().getString(R.string.call_btn_cancel);
                        text_status.setText(cancelText);
                        btn_cancel_order.setText(getString(R.string.help_button));
                        btn_cancel_order.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);

                                List<String> stringList = logCursor(MainActivity.CITY_INFO);
                                String phone = stringList.get(3);
                                intent.setData(Uri.parse(phone));
                                startActivity(intent);
                            }
                        });
                    }
             }, delayMillis);
         }

        if (pay_method.equals("fondy_payment") || pay_method.equals("mono_payment")) {
            /**
             * Записываем номер заказа
             */
            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(FinishActivity.this);
            callOrderIdMemory(MainActivity.order_id, uid, pay_method);
            myRunnable = new Runnable() {
                @Override
                public void run() {
                    MainActivity.order_id = null;
                    String cancelText = getApplicationContext().getString(R.string.call_btn_cancel);
                    text_status.setText(cancelText);
                    btn_cancel_order.setText(getString(R.string.help_button));
                    btn_cancel_order.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_DIAL);

                            List<String> stringList = logCursor(MainActivity.CITY_INFO);
                            String phone = stringList.get(3);
                            intent.setData(Uri.parse(phone));
                            startActivity(intent);
                        }
                    });
                }
            };
            handler.postDelayed(myRunnable, delayMillis);
        }


        btn_cancel_order.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connected()){
                    cancelOrder(uid);
                    if(!uid_Double.equals(" ")) {
                        cancelOrder(uid_Double);
                    }
                    if (thread != null && thread.isAlive()) {
                        thread.interrupt();
                    }
                } else {
                    text_status.setText(R.string.verify_internet);
                }
                btn_reset_status.setVisibility(View.GONE);
                btn_cancel_order.setVisibility(View.GONE);
                handler.removeCallbacks(myRunnable);
            }
        });

        Button btn_again = findViewById(R.id.btn_again);
        btn_again.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.order_id = null;
                updateAddCost(String.valueOf(0));
                if(!verifyOrder()) {
                    MyBottomSheetBlackListFragment bottomSheetDialogFragment = new MyBottomSheetBlackListFragment("orderCost");
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                } else {
                    if(connected()){
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    } else {
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                    }
                }
            }
        });

        Button btn_cancel = findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.order_id = null;
                finishAffinity();
            }
        });
        FloatingActionButton fab_cal = findViewById(R.id.fab_call);
        fab_cal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);

                List<String> stringList = logCursor(MainActivity.CITY_INFO);
                String phone = stringList.get(3);
                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });
        infoPaymentType();
        switch (pay_method) {
            case "fondy_payment":
                payFondy();
                break;
            case "mono_payment":
                String reference = MainActivity.order_id;
                String comment = getString(R.string.fondy_message);

                getUrlToPaymentMono(amount, reference, comment);
                break;

        }

    }
    @SuppressLint("Range")
    private void payFondy() {


        String rectoken = getCheckRectoken(MainActivity.TABLE_FONDY_CARDS);
        Log.d(TAG, "payFondy: rectoken " + rectoken);
        if (rectoken.equals("")) {
            getUrlToPaymentFondy(messageFondy, amount);
        } else {
            paymentByTokenFondy(messageFondy, amount, rectoken);
        }

    }
    private void paymentByTokenFondy(
            String orderDescription,
            String amount,
            String rectoken
    ) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PaymentApiToken paymentApi = retrofit.create(PaymentApiToken.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO);
        String MERCHANT_ID = arrayList.get(6);
        String merchantPassword = arrayList.get(7);

        List<String> stringList = logCursor(MainActivity.TABLE_USER_INFO);
        String email = stringList.get(3);

        String order_id =  MainActivity.order_id;

        Log.d(TAG, "paymentByTokenFondy: " + rectoken);

        RequestDataToken paymentRequest = new RequestDataToken(
                order_id,
                orderDescription,
                amount,
                MERCHANT_ID,
                merchantPassword,
                rectoken,
                email
        );


        StatusRequestToken statusRequest = new StatusRequestToken(paymentRequest);
        Log.d(TAG, "getUrlToPayment: " + statusRequest);

        Call<ApiResponseToken<SuccessResponseDataToken>> call = paymentApi.makePayment(statusRequest);


        call.enqueue(new Callback<ApiResponseToken<SuccessResponseDataToken>>() {

            @Override
            public void onResponse(@NonNull Call<ApiResponseToken<SuccessResponseDataToken>> call, Response<ApiResponseToken<SuccessResponseDataToken>> response) {
                Log.d(TAG, "onResponse: 1111" + response.code());
                if (response.isSuccessful()) {
                    ApiResponseToken<SuccessResponseDataToken> apiResponse = response.body();

                    Log.d(TAG, "onResponse: " +  new Gson().toJson(apiResponse));
                    try {
                        SuccessResponseDataToken responseBody = response.body().getResponse();;

                        // Теперь у вас есть объект ResponseBodyRev для обработки
                        if (responseBody != null) {
                            Log.d(TAG, "JSON Response: " + new Gson().toJson(apiResponse));
                            String orderStatus = responseBody.getOrderStatus();
                            if (!"approved".equals(orderStatus)) {
                                // Обработка ответа об ошибке
                                String errorResponseMessage = responseBody.getErrorMessage();
                                String errorResponseCode = responseBody.getErrorCode();
                                Log.d(TAG, "onResponse: errorResponseMessage " + errorResponseMessage);
                                Log.d(TAG, "onResponse: errorResponseCode" + errorResponseCode);

                                Toast.makeText(FinishActivity.this, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                                getUrlToPaymentFondy(messageFondy, amount);
                            }
                        } else {
                            Toast.makeText(FinishActivity.this, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(FinishActivity.this);
                            getUrlToPaymentFondy(messageFondy, amount);
                        }
                    } catch (JsonSyntaxException e) {
                        // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                        Toast.makeText(FinishActivity.this, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(FinishActivity.this);
                        getUrlToPaymentFondy(messageFondy, amount);
                    }
                } else {
                    // Обработка ошибки
                    Log.d(TAG, "onFailure: " + response.code());
                    Toast.makeText(FinishActivity.this, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                    
                    getUrlToPaymentFondy(messageFondy, amount);
                }

            }

            @Override
            public void onFailure(Call<ApiResponseToken<SuccessResponseDataToken>> call, Throwable t) {
                Log.d(TAG, "onFailure1111: " + t.toString());
                Toast.makeText(FinishActivity.this, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(FinishActivity.this);
                getUrlToPaymentFondy(messageFondy, amount);
            }
        });
    }
    @SuppressLint("Range")
    private String getCheckRectoken(String table) {
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String[] columns = {"rectoken"}; // Указываем нужное поле
        String selection = "rectoken_check = ?";
        String[] selectionArgs = {"1"};
        String result = "";

        Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    result = cursor.getString(cursor.getColumnIndex("rectoken"));
                    Log.d(TAG, "Found rectoken with rectoken_check = 1: " + result);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        database.close();
        return result;
    }

    private void getUrlToPaymentFondy(String orderDescription, String amount) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PaymentApi paymentApi = retrofit.create(PaymentApi.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO);
        String MERCHANT_ID = arrayList.get(6);
        String merchantPassword = arrayList.get(7);
        String email = logCursor(MainActivity.TABLE_USER_INFO).get(3);

        String order_id = MainActivity.order_id;
        RequestData paymentRequest = new RequestData(
                order_id,
                orderDescription,
                amount,
                MERCHANT_ID,
                merchantPassword,
                email
        );


        StatusRequestPay statusRequest = new StatusRequestPay(paymentRequest);
        Log.d(TAG, "getUrlToPayment: " + statusRequest.toString());

        Call<ApiResponsePay<SuccessResponseDataPay>> call = paymentApi.makePayment(statusRequest);

        call.enqueue(new Callback<ApiResponsePay<SuccessResponseDataPay>>() {

            @Override
            public void onResponse(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, Response<ApiResponsePay<SuccessResponseDataPay>> response) {
                Log.d(TAG, "onResponse: 1111" + response.code());
                if (response.isSuccessful()) {
                    ApiResponsePay<SuccessResponseDataPay> apiResponse = response.body();

                    Log.d(TAG, "onResponse: " +  new Gson().toJson(apiResponse));
                    try {
                        SuccessResponseDataPay responseBody = response.body().getResponse();;

                        // Теперь у вас есть объект ResponseBodyRev для обработки
                        if (responseBody != null) {
                            String responseStatus = responseBody.getResponseStatus();
                            String checkoutUrl = responseBody.getCheckoutUrl();
                            if ("success".equals(responseStatus)) {
                                // Обработка успешного ответа

                                MyBottomSheetCardPayment bottomSheetDialogFragment = new MyBottomSheetCardPayment(
                                        checkoutUrl,
                                        amount,
                                        uid,
                                        uid_Double
                                );
                                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                            } else if ("failure".equals(responseStatus)) {
                                // Обработка ответа об ошибке
                                String errorResponseMessage = responseBody.getErrorMessage();
                                String errorResponseCode = responseBody.getErrorCode();
                                Log.d(TAG, "onResponse: errorResponseMessage " + errorResponseMessage);
                                Log.d(TAG, "onResponse: errorResponseCode" + errorResponseCode);
                                cancelOrderDismiss(uid);
                                cancelOrderDismiss(uid_Double);
                                // Отобразить сообщение об ошибке пользователю
                            } else {
                                // Обработка других возможных статусов ответа
                            }
                        } else {
                            // Обработка пустого тела ответа
                            cancelOrderDismiss(uid);
                            cancelOrderDismiss(uid_Double);
                        }
                    } catch (JsonSyntaxException e) {
                        // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                        cancelOrderDismiss(uid);
                        cancelOrderDismiss(uid_Double);
                    }
                } else {
                    // Обработка ошибки
                    Log.d(TAG, "onFailure: " + response.code());
                    cancelOrderDismiss(uid);
                    cancelOrderDismiss(uid_Double);
                }

            }

            @Override
            public void onFailure(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, Throwable t) {
                Log.d(TAG, "onFailure1111: " + t.toString());

                cancelOrderDismiss(uid);
                cancelOrderDismiss(uid_Double);
            }


        });
    }

    private void cancelOrderDismiss(String value) {
        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);


        String url = baseUrl + "/" + api + "/android/webordersCancel/" + value + "/" + city  + "/" + getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrder(url);
        Log.d(TAG, "cancelOrderWithDifferentValue cancelOrderUrl: " + url);

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                Status status = response.body();
                if (status != null) {

                    String result =  String.valueOf(status.getResponse());
                    Log.d(TAG, "onResponse: result" + result);
                    FinishActivity.text_status.setText(result + getString(R.string.pay_failure));

                } else {
                    FinishActivity.text_status.setText(R.string.verify_internet);
                }
            }

            @Override
            public void onFailure(Call<Status> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d(TAG, "onFailure: " + errorMessage);

            }
        });
    }

    private void getUrlToPaymentMono(String amount, String reference, String comment) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.monobank.ua/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        MonoApi monoApi = retrofit.create(MonoApi.class);
        int amountMono = Integer.parseInt(amount);
        RequestPayMono paymentRequest = new RequestPayMono(
                amountMono,
                reference,
                comment
        );

        Log.d(TAG, "getUrlToPayment: " + paymentRequest.toString());

        String token = getResources().getString(R.string.mono_key_storage); // Получение токена из ресурсов
        Call<ResponsePayMono> call = monoApi.invoiceCreate(token, paymentRequest);

        call.enqueue(new Callback<ResponsePayMono>() {

            @Override
            public void onResponse(@NonNull Call<ResponsePayMono> call, Response<ResponsePayMono> response) {
                Log.d(TAG, "onResponse: 1111" + response.code());
                if (response.isSuccessful()) {
                    ResponsePayMono apiResponse = response.body();

                    Log.d(TAG, "onResponse: " +  new Gson().toJson(apiResponse));
                    try {
                        String pageUrl = response.body().getPageUrl();;
                        MainActivity.invoiceId = response.body().getInvoiceId();;

                        // Теперь у вас есть объект ResponseBodyRev для обработки
                        if (pageUrl != null) {

                            // Обработка успешного ответа

                            MyBottomSheetCardPayment bottomSheetDialogFragment = new MyBottomSheetCardPayment(
                                    pageUrl,
                                    amount,
                                    uid,
                                    uid_Double
                            );
                            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                        } else {
                            cancelOrderDismiss(uid);
                            cancelOrderDismiss(uid_Double);
                        }

                    } catch (JsonSyntaxException e) {
                        // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                        cancelOrderDismiss(uid);
                        cancelOrderDismiss(uid_Double);
                    }
                } else {
                    // Обработка ошибки
                    Log.d(TAG, "onFailure: " + response.code());
                    cancelOrderDismiss(uid);
                    cancelOrderDismiss(uid_Double);
                }
            }

            @Override
            public void onFailure(Call<ResponsePayMono> call, Throwable t) {
                Log.d(TAG, "onFailure1111: " + t.toString());
                cancelOrderDismiss(uid);
                cancelOrderDismiss(uid_Double);
            }


        });
    }
    private void updateAddCost(String addCost) {
        ContentValues cv = new ContentValues();
        Log.d(TAG, "updateAddCost: addCost" + addCost);
        cv.put("addCost", addCost);

        // обновляем по id
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    private void infoPaymentType() {
        if (pay_method.equals("bonus_payment")
                || pay_method.equals("card_payment")
                || pay_method.equals("fondy_payment")
                || pay_method.equals("mono_payment")) {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // Здесь вызывайте вашу функцию fetchCarFound()
                    fetchCarFound();
                }
            });
            thread.start();
        } else {
            String message = getString(R.string.nal_pay_message);

            MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }
    private void fetchBonus(String url) {

        Call<BonusResponse> call = ApiClient.getApiService().getBonus(url);
        Log.d(TAG, "fetchBonus: " + url);
        call.enqueue(new Callback<BonusResponse>() {
            @Override
            public void onResponse(Call<BonusResponse> call, Response<BonusResponse> response) {
                BonusResponse bonusResponse = response.body();
                if (response.isSuccessful()) {

                    String bonus = String.valueOf(bonusResponse.getBonus());
                    String message = getString(R.string.block_mes) + " " + bonus + " " + getString(R.string.bon);

                    MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(Call<BonusResponse> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                // Дополнительная обработка ошибки
            }
        });
    }
    private void fetchCarFound() {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl) // Замените BASE_URL на ваш базовый URL сервера
                .addConverterFactory(GsonConverterFactory.create())
                .build();

// Создайте экземпляр ApiServiceMapbox
        ApiService apiService = retrofit.create(ApiService.class);

// Вызов метода startNewProcessExecutionStatus с передачей параметров
        Call<Void> call = apiService.startNewProcessExecutionStatus(
                receivedMap.get("doubleOrder")
        );
        String url = call.request().url().toString();
        Log.d(TAG, "URL запроса: " + url);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Обработайте ошибку при выполнении запроса
            }
        });

    }

    public void callOrderIdMemory(String orderId, String uid, String paySystem) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<Void> call = apiService.orderIdMemory(orderId, uid, paySystem);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Обработка успешного ответа
                } else {
                    // Обработка неуспешного ответа
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Обработка ошибки
            }
        });
    }
    private boolean verifyOrder() {
        SQLiteDatabase database = this.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(MainActivity.TABLE_USER_INFO).get(1).equals("0")) {
                verify = false;
            }
            cursor.close();
        }
        database.close();
        return verify;
    }
    private boolean connected() {

        boolean hasConnect = false;

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            hasConnect = true;
        }
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            hasConnect = true;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            hasConnect = true;
        }

        return hasConnect;
    }
    @SuppressLint("Range")
    private List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = this.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        assert c != null;
        c.close();
        return list;
    }
    private void cancelOrder(String value) {
        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = baseUrl + "/" + api + "/android/webordersCancel/" + value + "/" + city  + "/" + getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrder(url);
        Log.d(TAG, "cancelOrderWithDifferentValue cancelOrderUrl: " + url);

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(Call<Status> call, Response<Status> response) {
                if (response.isSuccessful()) {
                    Status status = response.body();
                    if (status != null) {
                        String result =  String.valueOf(status.getResponse());
                        Log.d(TAG, "onResponse: result" + result);
                        text_status.setText(result);
                        String comment = getString(R.string.fondy_revers_message) + getString(R.string.fondy_message);;

                        switch (pay_method) {
                            case "fondy_payment":
                                getRevers(MainActivity.order_id, comment, amount);
                                break;
                            case "mono_payment":
                                getReversMono(MainActivity.invoiceId, comment, Integer.parseInt(amount));
                                break;
                        }
                    }
                } else {
                    // Обработка неуспешного ответа
                    if (pay_method.equals("nal_payment")) {
                        text_status.setText(R.string.verify_internet);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d(TAG, "onFailure: " + errorMessage);
                text_status.setText(R.string.verify_internet);
            }
        });
    }
    private void getRevers(String orderId, String comment, String amount) {

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
            public void onFailure(Call<ApiResponseRev<SuccessResponseDataRevers>> call, Throwable t) {
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

        String token = getResources().getString(R.string.mono_key_storage); // Получение токена из ресурсов
        Call<ResponseCancelMono> call = monoApi.invoiceCancel(token, paymentRequest);

        call.enqueue(new Callback<ResponseCancelMono>() {
            @Override
            public void onResponse(@NonNull Call<ResponseCancelMono> call, @NonNull Response<ResponseCancelMono> response) {

                if (response.isSuccessful()) {
                    ResponseCancelMono apiResponse = response.body();
                    Log.d(TAG, "JSON Response: " + new Gson().toJson(apiResponse));
                    if (apiResponse != null) {
                        String responseData = apiResponse.getStatus();
                        Log.d(TAG, "onResponse: " + responseData.toString());
                        // Обработка успешного ответа

                        switch (responseData) {
                            case "processing":
                                Log.d(TAG, "onResponse: " + "заява на скасування знаходиться в обробці");
                                break;
                            case "success":
                                Log.d(TAG, "onResponse: " + "заяву на скасування виконано успішно");
                                break;
                            case "failure":
                                Log.d(TAG, "onResponse: " + "неуспішне скасування");
                                Log.d(TAG, "onResponse: ErrCode: " + apiResponse.getErrCode());
                                Log.d(TAG, "onResponse: ErrText: " + apiResponse.getErrText());
                                break;
                        }

                    }
                } else {
                    // Обработка ошибки запроса
                    Log.d(TAG, "onResponse: Ошибка запроса, код " + response.code());
                    try {
                        assert response.errorBody() != null;
                        String errorBody = response.errorBody().string();
                        Log.d(TAG, "onResponse: Тело ошибки: " + errorBody);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseCancelMono> call, Throwable t) {
                // Обработка ошибки сети или другие ошибки
                Log.d(TAG, "onFailure: Ошибка сети: " + t.getMessage());
            }
        });

    }

    private void statusOrderWithDifferentValue(String value) {

        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = baseUrl + "/" + api + "/android/historyUIDStatus/" + value + "/" + city  + "/" + getString(R.string.application);

        Call<OrderResponse> call = ApiClient.getApiService().statusOrder(url);
        Log.d(TAG, "/android/historyUIDStatus/: " + url);

        // Выполняем запрос асинхронно
        call.enqueue(new Callback<OrderResponse>() {
            @Override
            public void onResponse(Call<OrderResponse> call, Response<OrderResponse> response) {
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
                            message = getString(R.string.ex_st_1);
                            break;
                        case "SearchesForCar":
                            message = getString(R.string.ex_st_0);
                            break;
                        case "Canceled":
                            message = getString(R.string.ex_st_canceled);
                            break;
                        case "CarFound":
                            // Формируем сообщение с учетом возможных пустых значений переменных
                            StringBuilder messageBuilder = new StringBuilder(getString(R.string.ex_st_2));

                            if (orderCarInfo != null && !orderCarInfo.isEmpty()) {
                                messageBuilder.append(getString(R.string.ex_st_3)).append(orderCarInfo);
                            }

                            if (driverPhone != null && !driverPhone.isEmpty()) {
                                messageBuilder.append(getString(R.string.ex_st_4)).append(driverPhone);
                            }

                            if (requiredTime != null && !requiredTime.isEmpty()) {
                                messageBuilder.append(getString(R.string.ex_st_5)).append(requiredTime);
                            }

                            message = messageBuilder.toString();
                            break;
                        default:
                            message = getString(R.string.def_status);
                            break;
                    }

                    text_status.setText(message);

                } else {
                    text_status.setText(getString(R.string.def_status));
                }
            }

            @Override
            public void onFailure(Call<OrderResponse> call, Throwable t) {
                text_status.setText(getString(R.string.def_status));
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

    public class VerifyUserTask extends AsyncTask<Void, Void, Map<String, String>> {
        private Exception exception;
        @Override
        protected Map<String, String> doInBackground(Void... voids) {
            String userEmail = logCursor(MainActivity.TABLE_USER_INFO).get(3);

            String url = "https://m.easy-order-taxi.site/android/verifyBlackListUser/" + userEmail + "/" + "com.taxi.easy.ua";
            try {
                return CostJSONParser.sendURL(url);
            } catch (Exception e) {
                exception = e;
                return null;
            }

        }

        @Override
        protected void onPostExecute(Map<String, String> sendUrlMap) {
            String message = sendUrlMap.get("message");
            ContentValues cv = new ContentValues();
            SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            if (message != null) {

                if (message.equals("В черном списке")) {

                    cv.put("verifyOrder", "0");
                    database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                }
            }
            database.close();
        }
    }


}
