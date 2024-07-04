package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.CustomCardAdapter;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.finish.Status;
import com.taxi.easy.ua.ui.fondy.gen_signatur.SignatureClient;
import com.taxi.easy.ua.ui.fondy.gen_signatur.SignatureResponse;
import com.taxi.easy.ua.ui.fondy.payment.ApiResponsePay;
import com.taxi.easy.ua.ui.fondy.payment.MyBottomSheetCardPayment;
import com.taxi.easy.ua.ui.fondy.payment.PaymentApi;
import com.taxi.easy.ua.ui.fondy.payment.RequestData;
import com.taxi.easy.ua.ui.fondy.payment.StatusRequestPay;
import com.taxi.easy.ua.ui.fondy.payment.SuccessResponseDataPay;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.fondy.token_pay.ApiResponseToken;
import com.taxi.easy.ua.ui.fondy.token_pay.PaymentApiToken;
import com.taxi.easy.ua.ui.fondy.token_pay.RequestDataToken;
import com.taxi.easy.ua.ui.fondy.token_pay.StatusRequestToken;
import com.taxi.easy.ua.ui.fondy.token_pay.SuccessResponseDataToken;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceResponse;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceService;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseResponse;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseService;
import com.taxi.easy.ua.utils.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
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
    private final String TAG = "MyBottomSheetErrorPaymentFragment";
    private static String messageFondy;
    String amount;
    List<String>  arrayList;
    String MERCHANT_ID;
    String rectoken;
    Context context;
    FragmentManager fragmentManager;
    public MyBottomSheetErrorPaymentFragment(
            String pay_method,
            String messageFondy,
            String amount,
            Context context
    ) {
        this.pay_method = pay_method;
        MyBottomSheetErrorPaymentFragment.messageFondy = messageFondy;
        this.amount = amount;
        this.context = context;
    }

    @SuppressLint("MissingInflatedId")
     
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.error_payment_layout, container, false);
        arrayList = logCursor(MainActivity.CITY_INFO, requireContext());
        MERCHANT_ID = arrayList.get(6);
        fragmentManager = getParentFragmentManager();
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
        btn_ok.setOnClickListener(v -> {
            cancelOrderDouble();

            paymentType();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                 try {
                      orderFinished(page);
                    } catch (MalformedURLException e) {
                     FirebaseCrashlytics.getInstance().recordException(e);
                 }
                }
            }, 5000);

            dismiss();
        });
        textViewInfo = view.findViewById(R.id.textViewInfo);

        btn_card = view.findViewById(R.id.btn_card);
        btn_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (pay_method) {
                    case "fondy_payment":
                        rectoken = getCheckRectoken(MainActivity.TABLE_FONDY_CARDS);
                        try {
                            paymentByTokenFondy(messageFondy, amount, rectoken);
                        } catch (UnsupportedEncodingException e) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                            throw new RuntimeException(e);
                        }
                        break;
                    case "wfp_payment":
                        rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS);
                        paymentByTokenWfp(messageFondy, amount, rectoken);
                        break;
                }

                dismiss();
            }
        });
        btn_add_card = view.findViewById(R.id.btn_add_card);
        btn_add_card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (pay_method) {
                    case "fondy_payment":
                        getUrlToPaymentFondy(messageFondy, amount, getParentFragmentManager());
                        break;
                    case "wfp_payment":
                        getUrlToPaymentWfp();
                        break;
                }

                dismiss();
            }
        });
        listView = view.findViewById(R.id.listView);
        return view;
    }

    private void getUrlToPaymentWfp() {
        
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        InvoiceService service = retrofit.create(InvoiceService.class);
        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String city = stringList.get(1);

        stringList = logCursor(MainActivity.TABLE_USER_INFO, context);
        String userEmail = stringList.get(3);
        String phone_number = stringList.get(2);

        Call<InvoiceResponse> call = service.createInvoice(
                getString(R.string.application),
                city,
                MainActivity.order_id,
                Integer.parseInt(amount),
                LocaleHelper.getLocale(),
                FinishActivity.messageFondy,
                userEmail,
                phone_number
        );

        call.enqueue(new Callback<InvoiceResponse>() {
            @Override
            public void onResponse(@NonNull Call<InvoiceResponse> call, @NonNull Response<InvoiceResponse> response) {
                Logger.d(getActivity(), TAG, "onResponse: 1111" + response.code());
                if (response.isSuccessful()) {
                    InvoiceResponse invoiceResponse = response.body();

                    if (invoiceResponse != null) {
                        String checkoutUrl = invoiceResponse.getInvoiceUrl();
                        Logger.d(getActivity(), TAG, "onResponse: Invoice URL: " + checkoutUrl);
                        if(checkoutUrl != null) {
                            MyBottomSheetCardPayment bottomSheetDialogFragment = new MyBottomSheetCardPayment(
                                    checkoutUrl,
                                    amount,
                                    FinishActivity.uid,
                                    FinishActivity.uid_Double,
                                    context
                            );
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                        } else {
                            Logger.d(getActivity(), TAG,"Response body is null");
                            cancelOrderDouble();
                        }
                    } else {
                        Logger.d(getActivity(), TAG,"Response body is null");
                        cancelOrderDouble();
                    }
                } else {
                    Logger.d(getActivity(), TAG, "Request failed: " + response.code());
                    cancelOrderDouble();
                }
            }

            @Override
            public void onFailure(@NonNull Call<InvoiceResponse> call, @NonNull Throwable t) {
                Logger.d(getActivity(), TAG, "Request failed: " + t.getMessage());
                cancelOrderDouble();
            }
        });
    }

    private void paymentByTokenWfp(
            String orderDescription,
            String amount,
            String rectoken
    ) {
        
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(FinishActivity.baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        PurchaseService service = retrofit.create(PurchaseService.class);
        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
        String city = stringList.get(1);

        Call<PurchaseResponse> call = service.purchase(
                getString(R.string.application),
                city,
                MainActivity.order_id,
                amount,
                orderDescription,
                FinishActivity.email,
                FinishActivity.phoneNumber,
                rectoken
        );
        call.enqueue(new Callback<PurchaseResponse>() {
            @Override
            public void onResponse(@NonNull Call<PurchaseResponse> call, @NonNull Response<PurchaseResponse> response) {
                if (response.isSuccessful()) {
                    PurchaseResponse purchaseResponse = response.body();
                    if (purchaseResponse != null) {
                        // Обработка ответа
                        Logger.d(getActivity(), TAG, "onResponse:purchaseResponse " + purchaseResponse);
                    } else {
                        // Ошибка при парсинге ответа
                        Logger.d(getActivity(), TAG, "Ошибка при парсинге ответа");
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        FinishActivity.callOrderIdMemory(MainActivity.order_id, FinishActivity.uid, pay_method);
                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, getActivity());
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
                } else {
                    // Ошибка запроса
                    Logger.d(getActivity(), TAG, "Ошибка запроса");
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    FinishActivity.callOrderIdMemory(MainActivity.order_id, FinishActivity.uid, pay_method);
                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, getActivity());
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PurchaseResponse> call, @NonNull Throwable t) {
                // Ошибка при выполнении запроса
                Logger.d(getActivity(), TAG, "Ошибка при выполнении запроса");
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                FinishActivity.callOrderIdMemory(MainActivity.order_id, FinishActivity.uid, pay_method);
                MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, getActivity());
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });

    }

    public void orderFinished(String page) throws MalformedURLException {
        String urlOrder = getTaxiUrlSearchMarkers(page, context);

        ToJSONParserRetrofit parser = new ToJSONParserRetrofit();

//            // Пример строки URL с параметрами
        Logger.d(getActivity(), TAG, "orderFinished: "  + "https://m.easy-order-taxi.site"+ urlOrder);
        parser.sendURL(urlOrder, new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                Map<String, String> sendUrlMap = response.body();

                assert sendUrlMap != null;
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


                    String pay_method_message = context.getString(R.string.pay_method_message_main);
                    pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
                    switch (pay_method) {
                        case "bonus_payment":
                            pay_method_message += " " + context.getString(R.string.pay_method_message_bonus);
                            break;
                        case "card_payment":
                        case "fondy_payment":
                        case "mono_payment":
                        case "wfp_payment":
                            pay_method_message += " " + context.getString(R.string.pay_method_message_card);
                            break;
                        default:
                            pay_method_message += " " + context.getString(R.string.pay_method_message_nal);
                    }
                    String to_name_local = to_name;
                    if(to_name.contains("по місту")
                            ||to_name.contains("по городу")
                            || to_name.contains("around the city")
                    ) {
                        to_name_local = context.getString(R.string.on_city_tv);
                    }
                    String messageResult = context.getString(R.string.thanks_message) +
                            sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
                            to_name_local + "." +
                            context.getString(R.string.call_of_order) + orderWeb + context.getString(R.string.UAH) + " " + pay_method_message;
                    String messageFondy = context.getString(R.string.fondy_message) + " " +
                            sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
                            to_name_local + ".";

                    FinishActivity.text_status.setText(messageResult);
                    Intent intent = new Intent(context, FinishActivity.class);
                    intent.putExtra("messageResult_key", messageResult);
                    intent.putExtra("messageFondy_key", messageFondy);
                    intent.putExtra("messageCost_key", orderWeb);
                    intent.putExtra("sendUrlMap", new HashMap<>(sendUrlMap));
                    intent.putExtra("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Добавляем флаг FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent);
                } else {
                    Logger.d(getActivity(), TAG, "orderFinished:message " + message);
                    assert message != null;
                    if(message != null && message.equals("Дублирование заказа. Вы не можете создавать подобный заказ, пока не нашлась машина на предыдущий заказ.")) {
                        message = context.getString(R.string.double_order_error);
                    }
                    FinishActivity.text_status.setText(message);
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });

    }

     
    @SuppressLint("Range")
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Logger.d(getActivity(), TAG, "getTaxiUrlSearchMarkers: " + urlAPI);

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
        Logger.d(getActivity(), TAG, "getTaxiUrlSearchMarkers: start " + start);
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
            Logger.d(getActivity(), TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);

        database.close();

        return url;
    }
    private void paymentByTokenFondy(
            String orderDescription,
            String amount,
            String rectoken
    ) throws UnsupportedEncodingException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(requireActivity());
        FinishActivity.callOrderIdMemory(MainActivity.order_id, FinishActivity.uid, pay_method);

        PaymentApiToken paymentApi = retrofit.create(PaymentApiToken.class);

//        String merchantPassword = arrayList.get(7);
        List<String> stringList = logCursor(MainActivity.TABLE_USER_INFO, requireActivity());
        String email = stringList.get(3);

        String order_id =  MainActivity.order_id;

        Map<String, String> params = new TreeMap<>();
        params.put("order_id", order_id);
        params.put("order_desc", orderDescription);
        params.put("currency", "UAH");
        params.put("amount", amount);
        params.put("rectoken", rectoken);
        params.put("merchant_id", MERCHANT_ID);
        params.put("preauth", "Y");
        params.put("sender_email", email);

        StringBuilder paramsBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (paramsBuilder.length() > 0) {
                paramsBuilder.append("&");
            }
            paramsBuilder.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        String queryString = paramsBuilder.toString();




        Logger.d(getActivity(), TAG, "paymentByTokenFondy: " + rectoken);

        Logger.d(getActivity(), TAG, "getStatusFondy: " + params);
        SignatureClient signatureClient = new SignatureClient();
// Передаем экземпляр SignatureCallback в метод generateSignature
        signatureClient.generateSignature(queryString, new SignatureClient.SignatureCallback() {
            @Override
            public void onSuccess(SignatureResponse response) {
                // Обработка успешного ответа
                String digest = response.getDigest();
                Logger.d(getActivity(), TAG, "Received signature digest: " + digest);

                RequestDataToken paymentRequest = new RequestDataToken(
                        order_id,
                        orderDescription,
                        amount,
                        MERCHANT_ID,
                        digest,
                        rectoken,
                        email
                );


                StatusRequestToken statusRequest = new StatusRequestToken(paymentRequest);
                Logger.d(getActivity(), TAG, "getUrlToPayment: " + statusRequest);

                Call<ApiResponseToken<SuccessResponseDataToken>> call = paymentApi.makePayment(statusRequest);


                call.enqueue(new Callback<ApiResponseToken<SuccessResponseDataToken>>() {

                    @Override
                    public void onResponse(@NonNull Call<ApiResponseToken<SuccessResponseDataToken>> call, Response<ApiResponseToken<SuccessResponseDataToken>> response) {
                        Logger.d(getActivity(), TAG, "onResponse: 1111" + response.code());
                        if (response.isSuccessful()) {
                            ApiResponseToken<SuccessResponseDataToken> apiResponse = response.body();

                            Logger.d(getActivity(), TAG, "onResponse: " +  new Gson().toJson(apiResponse));
                            try {
                                SuccessResponseDataToken responseBody = response.body().getResponse();

                                // Теперь у вас есть объект ResponseBodyRev для обработки
                                if (responseBody != null) {
                                    Logger.d(getActivity(), TAG, "JSON Response: " + new Gson().toJson(apiResponse));
                                    String orderStatus = responseBody.getOrderStatus();
                                    if (!"approved".equals(orderStatus)) {
                                        // Обработка ответа об ошибке
                                        String errorResponseMessage = responseBody.getErrorMessage();
                                        String errorResponseCode = responseBody.getErrorCode();
                                        Logger.d(getActivity(), TAG, "onResponse: errorResponseMessage " + errorResponseMessage);
                                        Logger.d(getActivity(), TAG, "onResponse: errorResponseCode" + errorResponseCode);

//                                Toast.makeText(FinishActivity.this, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                                        FinishActivity.btn_again.setVisibility(View.VISIBLE);
                                        FinishActivity.btn_cancel.setVisibility(View.VISIBLE);
                                        FinishActivity.progressBar.setVisibility(View.INVISIBLE);
                                        cancelOrderDouble();

                                    }
                                } else {
//                            Toast.makeText(FinishActivity.this, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                                    FinishActivity.btn_again.setVisibility(View.VISIBLE);
                                    FinishActivity.btn_cancel.setVisibility(View.VISIBLE);
                                    FinishActivity.progressBar.setVisibility(View.INVISIBLE);
                                    cancelOrderDouble();

//                            getUrlToPaymentFondy(messageFondy, amount);
                                }
                            } catch (JsonSyntaxException e) {
                                FirebaseCrashlytics.getInstance().recordException(e);
                                // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                                Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                                FirebaseCrashlytics.getInstance().recordException(e);
                                FinishActivity.btn_again.setVisibility(View.VISIBLE);
                                FinishActivity.btn_cancel.setVisibility(View.VISIBLE);
                                FinishActivity.progressBar.setVisibility(View.INVISIBLE);
                                cancelOrderDouble();
                            }
                        } else {
                            // Обработка ошибки
                            Logger.d(getActivity(), TAG, "onFailure: " + response.code());
                            FinishActivity.btn_again.setVisibility(View.VISIBLE);
                            FinishActivity.btn_cancel.setVisibility(View.VISIBLE);
                            FinishActivity.progressBar.setVisibility(View.INVISIBLE);
                            cancelOrderDouble();
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponseToken<SuccessResponseDataToken>> call, @NonNull Throwable t) {
                        Logger.d(getActivity(), TAG, "onFailure1111: " + t);

                        FinishActivity.btn_again.setVisibility(View.VISIBLE);
                        FinishActivity.btn_cancel.setVisibility(View.VISIBLE);
                        FinishActivity.progressBar.setVisibility(View.INVISIBLE);
                        cancelOrderDouble();
                    }
                });
            }


            @Override
            public void onError(String error) {
                // Обработка ошибки
                Logger.d(getActivity(), TAG, "Received signature error: " + error);

                FinishActivity.btn_again.setVisibility(View.VISIBLE);
                FinishActivity.btn_cancel.setVisibility(View.VISIBLE);
                FinishActivity.progressBar.setVisibility(View.INVISIBLE);
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
        Logger.d(getActivity(), TAG, "getUrlToPayment: " + statusRequest);

        Call<ApiResponsePay<SuccessResponseDataPay>> call = paymentApi.makePayment(statusRequest);

        call.enqueue(new Callback<ApiResponsePay<SuccessResponseDataPay>>() {

            @Override
            public void onResponse(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, Response<ApiResponsePay<SuccessResponseDataPay>> response) {
                Logger.d(getActivity(), TAG, "onResponse: 1111" + response.code());
                if (response.isSuccessful()) {
                    ApiResponsePay<SuccessResponseDataPay> apiResponse = response.body();

                    Logger.d(getActivity(), TAG, "onResponse: " +  new Gson().toJson(apiResponse));
                    try {
                        SuccessResponseDataPay responseBody = response.body().getResponse();

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
                                Logger.d(getActivity(), TAG, "onResponse: errorResponseMessage " + errorResponseMessage);
                                Logger.d(getActivity(), TAG, "onResponse: errorResponseCode" + errorResponseCode);
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
                        FirebaseCrashlytics.getInstance().recordException(e);
                        // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                        Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                        FirebaseCrashlytics.getInstance().recordException(e);
                        cancelOrderDouble();
                    }
                } else {
                    // Обработка ошибки
                    Logger.d(getActivity(), TAG, "onFailure: " + response.code());
                    cancelOrderDouble();
                }

            }

            @Override
            public void onFailure(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, Throwable t) {
                Logger.d(getActivity(), TAG, "onFailure1111: " + t);

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
        Logger.d(getActivity(), TAG, "cancelOrderDouble: " + url);

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful()) {
                    Status status = response.body();
                    if (status != null) {
                        String result =  String.valueOf(status.getResponse());
                        Logger.d(getActivity(), TAG, "onResponse: result" + result);
                        String newStatus = FinishActivity.text_status.getText().toString();
                        Logger.d(getActivity(), TAG, "onResponse:newStatus " + newStatus);

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
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(getActivity(), TAG, "onFailure: " + errorMessage);
                FinishActivity.text_status.setText(R.string.verify_internet);
                FinishActivity.progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        String table;
        switch (pay_method) {
            case "wfp_payment":
                table = MainActivity.TABLE_WFP_CARDS;
                break;
            case "mono_payment":
                table = MainActivity.TABLE_MONO_CARDS;
                break;
            default:
                table = MainActivity.TABLE_FONDY_CARDS;
        }
        ArrayList<Map<String, String>> cardMaps  = getCardMapsFromDatabase(table);
        if (!cardMaps.isEmpty()) {
            CustomCardAdapter listAdapter = new CustomCardAdapter(context, cardMaps, table, pay_method);
            listView.setAdapter(listAdapter);
        }

    }

    @SuppressLint("Range")
    private String getCheckRectoken(String table) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String[] columns = {"rectoken"}; // Указываем нужное поле
        String selection = "rectoken_check = ?";
        String[] selectionArgs = {"1"};
        String result = "";

        Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    result = cursor.getString(cursor.getColumnIndex("rectoken"));
                    Logger.d(getActivity(), TAG, "Found rectoken with rectoken_check = 1 " + ": " + result);
                    return result;
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        database.close();

//        logTableContent(table);

        return result;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context; // Инициализация контекста
    }
    private void paymentType() {
        ContentValues cv = new ContentValues();
        cv.put("payment_type", "nal_payment");
        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }
    @SuppressLint("Range")
    private ArrayList<Map<String, String>> getCardMapsFromDatabase(String table) {
        ArrayList<Map<String, String>> cardMaps = new ArrayList<>();
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        // Выполните запрос к таблице TABLE_FONDY_CARDS и получите данные
        Cursor cursor = database.query(table, null, null, null, null, null, null);
        Logger.d(getActivity(), TAG, "getCardMapsFromDatabase: card count: " + cursor.getCount());

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

