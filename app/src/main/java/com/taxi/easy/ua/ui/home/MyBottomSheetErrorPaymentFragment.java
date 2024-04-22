package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.CustomCardAdapter;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.finish.Status;
import com.taxi.easy.ua.ui.fondy.payment.ApiResponsePay;
import com.taxi.easy.ua.ui.fondy.payment.MyBottomSheetCardPayment;
import com.taxi.easy.ua.ui.fondy.payment.PaymentApi;
import com.taxi.easy.ua.ui.fondy.payment.RequestData;
import com.taxi.easy.ua.ui.fondy.payment.StatusRequestPay;
import com.taxi.easy.ua.ui.fondy.payment.SuccessResponseDataPay;
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
import com.taxi.easy.ua.ui.maps.ToJSONParser;
import com.taxi.easy.ua.ui.mono.MonoApi;
import com.taxi.easy.ua.ui.mono.cancel.RequestCancelMono;
import com.taxi.easy.ua.ui.mono.cancel.ResponseCancelMono;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;

import java.io.IOException;
import java.net.MalformedURLException;
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


public class MyBottomSheetErrorPaymentFragment extends BottomSheetDialogFragment {
    TextView textViewInfo;
    AppCompatButton btn_help, btn_ok, btn_card,btn_add_card;

    public static ListView listView;
    String pay_method;
    String page ="orderSearchMarkersVisicom";
    private String TAG = "MyBottomSheetErrorPaymentFragment";
    private static String messageFondy;
    String amount;
    List<String>  arrayList;
    String MERCHANT_ID;
    String rectoken;
    Context context;

    public MyBottomSheetErrorPaymentFragment(
            String pay_method,
            String messageFondy,
            String amount,
            Context context
    ) {
        this.pay_method = pay_method;
        this.messageFondy = messageFondy;
        this.amount = amount;
        this.context = context;
    }

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.error_payment_layout, container, false);
        arrayList = logCursor(MainActivity.CITY_INFO, requireContext());
        MERCHANT_ID = arrayList.get(6);



        btn_help = view.findViewById(R.id.btn_help);
        btn_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, requireContext());
                Intent intent = new Intent(Intent.ACTION_DIAL);
                String phone = stringList.get(3);

                intent.setData(Uri.parse(phone));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Добавляем флаг FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent);
            }
        });

        btn_ok = view.findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelOrderDouble();

                paymentType("nal_payment");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                     try {
                          orderFinished(page);
                        } catch (MalformedURLException ignored) {}
                    }
                }, 5000);

                dismiss();
            }
        });
        textViewInfo = view.findViewById(R.id.textViewInfo);

        btn_card = view.findViewById(R.id.btn_card);
        btn_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rectoken = getCheckRectoken(MainActivity.TABLE_FONDY_CARDS, MERCHANT_ID);
                paymentByTokenFondy(messageFondy, amount, rectoken);
                dismiss();
            }
        });
        btn_add_card = view.findViewById(R.id.btn_add_card);
        btn_add_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUrlToPaymentFondy(messageFondy, amount, getParentFragmentManager());;
                dismiss();
            }
        });
        listView = view.findViewById(R.id.listView);
        return view;
    }

    public void orderFinished(String page) throws MalformedURLException {
        String urlOrder = getTaxiUrlSearchMarkers( page, context);
        Map<String, String> sendUrlMap = ToJSONParser.sendURL(urlOrder);
        Log.d(TAG, "Map sendUrlMap = ToJSONParser.sendURL(urlOrder); " + sendUrlMap);

        String orderWeb = sendUrlMap.get("order_cost");
        String message = sendUrlMap.get("message");

        assert orderWeb != null;
        if (!orderWeb.equals("0")) {
            String to_name;
            if (Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
                to_name = context.getString(R.string.on_city_tv);

            } else {
                if(sendUrlMap.get("routeto").equals("Точка на карте")) {
                    to_name = context.getString(R.string.end_point_marker);
                } else {
                    to_name = sendUrlMap.get("routeto") + " " + sendUrlMap.get("to_number");
                }
            }
            String messageResult = context.getString(R.string.thanks_message) +
                    sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
                    to_name + "." +
                    context.getString(R.string.call_of_order) + orderWeb + context.getString(R.string.UAH);
            String messageFondy = context.getString(R.string.fondy_message) + " " +
                    sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
                    to_name + ".";

            Intent intent = new Intent(context, FinishActivity.class);
            intent.putExtra("messageResult_key", messageResult);
            intent.putExtra("messageFondy_key", messageFondy);
            intent.putExtra("messageCost_key", orderWeb);
            intent.putExtra("sendUrlMap", new HashMap<>(sendUrlMap));
            intent.putExtra("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Добавляем флаг FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent);
        } else {
            Log.d(TAG, "orderFinished:message " + message);
            assert message != null;
            if(message != null && message.equals("Дублирование заказа. Вы не можете создавать подобный заказ, пока не нашлась машина на предыдущий заказ.")) {
                message = context.getString(R.string.double_order_error);
            }
            FinishActivity.text_status.setText(message);
//            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
//            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("Range")
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Log.d(TAG, "getTaxiUrlSearchMarkers: " + urlAPI);

        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery(query, null);

        cursor.moveToFirst();

        // Получите значения полей из первой записи

        double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
        double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
        double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
        double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
        String start = cursor.getString(cursor.getColumnIndex("start"));
        String finish = cursor.getString(cursor.getColumnIndex("finish"));
        Log.d(TAG, "getTaxiUrlSearchMarkers: start " + start);
        // Заменяем символ '/' в строках
        if(start != null) {
            start = start.replace("/", "|");
        }
        if(finish != null) {
            finish = finish.replace("/", "|");
        }
        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;

        cursor.close();


        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
        String comment = stringList.get(2);
        String date = stringList.get(3);

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif =  stringListInfo.get(2);
        String payment_type = stringListInfo.get(4);
        String addCost = stringListInfo.get(5);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);

        if(urlAPI.equals("costSearchMarkers")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type;
        }
        if(urlAPI.equals("orderSearchMarkersVisicom")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);


            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type + "/" + addCost + "/"
                    + time + "/" + comment + "/" + date+ "/" + start + "/" + finish;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });

        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
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

        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "https://m.easy-order-taxi.site/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);

        database.close();

        return url;
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
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO, context);
        String MERCHANT_ID = arrayList.get(6);
        String merchantPassword = arrayList.get(7);

        List<String> stringList = logCursor(MainActivity.TABLE_USER_INFO, context);
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

                                FinishActivity.text_status.setText(context.getString(R.string.error_payment_card));
                                cancelOrderDouble();
                            }
                        } else {
                            FinishActivity.text_status.setText(context.getString(R.string.error_payment_card));
                            cancelOrderDouble();
                        }
                    } catch (JsonSyntaxException e) {
                        // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                        FinishActivity.text_status.setText(context.getString(R.string.error_payment_card));
                        cancelOrderDouble();
                    }
                } else {
                    // Обработка ошибки
                    Log.d(TAG, "onFailure: " + response.code());
                    FinishActivity.text_status.setText(context.getString(R.string.error_payment_card));
                    cancelOrderDouble();
                }

            }

            @Override
            public void onFailure(@NonNull Call<ApiResponseToken<SuccessResponseDataToken>> call, @NonNull Throwable t) {
                Log.d(TAG, "onFailure1111: " + t.toString());
                FinishActivity.text_status.setText(context.getString(R.string.error_payment_card));
                cancelOrderDouble();
            }
        });
    }

    private void getUrlToPaymentFondy(String orderDescription, String amount, FragmentManager fragmentManager) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PaymentApi paymentApi = retrofit.create(PaymentApi.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO, context);
        String MERCHANT_ID = arrayList.get(6);
        String merchantPassword = arrayList.get(7);
        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

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
                                        FinishActivity.uid,
                                        FinishActivity.uid_Double,
                                        context
                                );
                                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                            } else if ("failure".equals(responseStatus)) {
                                // Обработка ответа об ошибке
                                String errorResponseMessage = responseBody.getErrorMessage();
                                String errorResponseCode = responseBody.getErrorCode();
                                Log.d(TAG, "onResponse: errorResponseMessage " + errorResponseMessage);
                                Log.d(TAG, "onResponse: errorResponseCode" + errorResponseCode);
                                cancelOrderDouble();
                                // Отобразить сообщение об ошибке пользователю
                            } else {
                                // Обработка других возможных статусов ответа
                            }
                        } else {
                            // Обработка пустого тела ответа
                            cancelOrderDouble();
                        }
                    } catch (JsonSyntaxException e) {
                        // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                        cancelOrderDouble();
                    }
                } else {
                    // Обработка ошибки
                    Log.d(TAG, "onFailure: " + response.code());
                    cancelOrderDouble();
                }

            }

            @Override
            public void onFailure(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, Throwable t) {
                Log.d(TAG, "onFailure1111: " + t.toString());

                cancelOrderDouble();
            }


        });
    }

    private void cancelOrderDouble() {
        List<String> listCity = logCursor(MainActivity.CITY_INFO,context);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = FinishActivity.baseUrl + "/" + api + "/android/webordersCancelDouble/" + FinishActivity.uid+ "/" + FinishActivity.uid_Double + "/" + pay_method + "/" + city  + "/" + context.getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrderDouble(url);
        Log.d(TAG, "cancelOrderDouble: " + url);

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful()) {
                    Status status = response.body();
                    if (status != null) {
                        String result =  String.valueOf(status.getResponse());
                        Log.d(TAG, "onResponse: result" + result);
                        String newStatus = FinishActivity.text_status.getText().toString();
                        Log.d(TAG, "onResponse:newStatus " + newStatus);

                        FinishActivity.text_status.setText(newStatus);

                        FinishActivity.progressBar.setVisibility(View.INVISIBLE);
                        FinishActivity.btn_cancel_order.setVisibility(View.GONE);
                    }
                } else {
                    // Обработка неуспешного ответа
                    if (pay_method.equals("nal_payment")) {
                        FinishActivity.text_status.setText(R.string.verify_internet);
                    }
                }
                FinishActivity.progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                Log.d(TAG, "onFailure: " + errorMessage);
                FinishActivity.text_status.setText(R.string.verify_internet);
                FinishActivity.progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    void getRevers(String orderId, String comment, String amount) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ReversApi apiService = retrofit.create(ReversApi.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO, context);
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
    void getReversMono(
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
    @Override
    public void onResume() {
        super.onResume();

        String table;
        switch (pay_method) {
            case "mono_payment":
                table = MainActivity.TABLE_MONO_CARDS;
                break;
            default:
                table = MainActivity.TABLE_FONDY_CARDS;
        }
        ArrayList<Map<String, String>> cardMaps  = getCardMapsFromDatabase(table);
        if (cardMaps != null && !cardMaps.isEmpty()) {
            CustomCardAdapter listAdapter = new CustomCardAdapter(context, cardMaps, table);
            listView.setAdapter(listAdapter);
        }

    }

    @SuppressLint("Range")
    private String getCheckRectoken(String table, String merchantId) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String[] columns = {"rectoken"}; // Указываем нужное поле
        String selection = "rectoken_check = ? AND merchant = ?";
        String[] selectionArgs = {"1", merchantId};
        String result = "";

        Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    result = cursor.getString(cursor.getColumnIndex("rectoken"));
                    Log.d(TAG, "Found rectoken with rectoken_check = 1 and merchant = " + merchantId + ": " + result);
                    return result;
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        database.close();

//        logTableContent(table);

        return result;
    }

    private void paymentType(String paymentCode) {
        ContentValues cv = new ContentValues();
        cv.put("payment_type", paymentCode);
        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }
    @SuppressLint("Range")
    private ArrayList<Map<String, String>> getCardMapsFromDatabase(String table) {
        ArrayList<Map<String, String>> cardMaps = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        // Выполните запрос к таблице TABLE_FONDY_CARDS и получите данные
        Cursor cursor = database.query(table, null, null, null, null, null, null);
        Log.d(TAG, "getCardMapsFromDatabase: card count: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> cardMap = new HashMap<>();
                cardMap.put("card_type", cursor.getString(cursor.getColumnIndex("card_type")));
                cardMap.put("bank_name", cursor.getString(cursor.getColumnIndex("bank_name")));
                cardMap.put("masked_card", cursor.getString(cursor.getColumnIndex("masked_card")));
                cardMap.put("rectoken", cursor.getString(cursor.getColumnIndex("rectoken")));
                cardMap.put("rectoken_check", cursor.getString(cursor.getColumnIndex("rectoken_check")));

                cardMaps.add(cardMap);
            } while (cursor.moveToNext());
        }
        cursor.close();

        database.close();

        return cardMaps;
    }
    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
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
        assert c != null;
        c.close();
        return list;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}

