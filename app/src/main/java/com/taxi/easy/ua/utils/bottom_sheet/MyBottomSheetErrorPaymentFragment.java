package com.taxi.easy.ua.utils.bottom_sheet;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.MainActivity.uid;
import static com.taxi.easy.ua.MainActivity.uid_Double;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavOptions;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.CustomCardAdapter;
import com.taxi.easy.ua.ui.card.MyBottomSheetCardPayment;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.ApiService;
import com.taxi.easy.ua.ui.finish.Status;
import com.taxi.easy.ua.ui.finish.fragm.FinishSeparateFragment;
import com.taxi.easy.ua.ui.fondy.gen_signatur.SignatureClient;
import com.taxi.easy.ua.ui.fondy.gen_signatur.SignatureResponse;
import com.taxi.easy.ua.ui.fondy.payment.ApiResponsePay;
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
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceResponse;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceService;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseResponse;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseService;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;
import com.uxcam.UXCam;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyBottomSheetErrorPaymentFragment extends BottomSheetDialogFragment {
    TextView textViewInfo;
    AppCompatButton btn_help, btn_ok, btn_card, btn_add_card, btn_cancel;

    public static ListView listView;
    String pay_method;
    String page = "orderSearchMarkersVisicom";
    private final String TAG = "MyBottomSheetErrorPaymentFragment";
    private static String messageFondy;
    String amount;
    List<String> arrayList;
    String MERCHANT_ID;
    String rectoken;
    Context context;
    FragmentManager fragmentManager;
    private TextView text_card;
    private String email;
    //    private final String baseUrl = "https://m.easy-order-taxi.site/";
    String baseUrl;
    String message;

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

    public MyBottomSheetErrorPaymentFragment(
            String pay_method,
            String messageFondy,
            String amount,
            Context context,
            String message
    ) {
        this.pay_method = pay_method;
        MyBottomSheetErrorPaymentFragment.messageFondy = messageFondy;
        this.amount = amount;
        this.context = context;
        this.message = message;
    }

    @SuppressLint("MissingInflatedId")

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        View view = inflater.inflate(R.layout.error_payment_layout, container, false);
        arrayList = logCursor(MainActivity.CITY_INFO, requireContext());
        MERCHANT_ID = arrayList.get(6);
        fragmentManager = getParentFragmentManager();

        email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

        btn_help = view.findViewById(R.id.btn_help);
        btn_help.setOnClickListener(v -> {
            List<String> stringList = logCursor(MainActivity.CITY_INFO, requireContext());
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = stringList.get(3);

            intent.setData(Uri.parse(phone));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Добавляем флаг FLAG_ACTIVITY_NEW_TASK
            startActivity(intent);
        });

        btn_ok = view.findViewById(R.id.btn_ok);
        boolean black_list_yes = verifyOrder();
        if (black_list_yes) {
            btn_ok.setVisibility(View.GONE);
        }
        btn_ok.setOnClickListener(v -> {
            btn_ok.setVisibility(View.INVISIBLE);
            view.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);

            FinishSeparateFragment.handlerStatus.removeCallbacks(FinishSeparateFragment.myTaskStatus);
            FinishSeparateFragment.text_status.setText(context.getString(R.string.recounting_order));
            cancelOrderDoubleForNal();
            sharedPreferencesHelperMain.saveValue("pay_error", "**");





        });
        textViewInfo = view.findViewById(R.id.textViewInfo);
        text_card = view.findViewById(R.id.text_card);

        if (message != null) {
            textViewInfo.setText(message);
        }
        btn_card = view.findViewById(R.id.btn_card);
        btn_card.setOnClickListener(v -> {
            switch (pay_method) {
                case "fondy_payment":
                    rectoken = getCheckRectoken(MainActivity.TABLE_FONDY_CARDS);
                    try {
                        paymentByTokenFondy(messageFondy, amount, rectoken);
                    } catch (UnsupportedEncodingException e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        throw new RuntimeException(e);
                    }
                    sharedPreferencesHelperMain.saveValue("pay_error", "**");
                    break;
                case "wfp_payment":
                    paymentByTokenWfp(messageFondy, amount);
                    sharedPreferencesHelperMain.saveValue("pay_error", "**");
                    break;
            }

            dismiss();
        });
        btn_add_card = view.findViewById(R.id.btn_add_card);
        btn_add_card.setOnClickListener(v -> {
            switch (pay_method) {
                case "fondy_payment":
                    getUrlToPaymentFondy(messageFondy, amount, getParentFragmentManager());
                    break;
                case "wfp_payment":
                    getUrlToPaymentWfp();
                    break;
            }

            dismiss();
        });

        btn_cancel = view.findViewById(R.id.btn_cancel_order);
        btn_cancel.setOnClickListener(v -> {
            cancelOrderDouble();
            sharedPreferencesHelperMain.saveValue("pay_error", "**");
            dismiss();
        });
        listView = view.findViewById(R.id.listView);
        return view;
    }

    private boolean verifyOrder() {
        return (boolean) sharedPreferencesHelperMain.getValue("verifyUserOrder", false);
    }

    private void getUrlToPaymentWfp() {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
                .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
                .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        InvoiceService service = retrofit.create(InvoiceService.class);
        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String city = stringList.get(1);

        stringList = logCursor(MainActivity.TABLE_USER_INFO, context);
        String userEmail = stringList.get(3);
        String phone_number = stringList.get(2);

        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
        callOrderIdMemory(MainActivity.order_id, MainActivity.uid, pay_method);

        Call<InvoiceResponse> call = service.createInvoice(
                getString(R.string.application),
                city,
                MainActivity.order_id,
                Integer.parseInt(amount),
                LocaleHelper.getLocale(),
                messageFondy,
                userEmail,
                phone_number
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<InvoiceResponse> call, @NonNull Response<InvoiceResponse> response) {
                Logger.d(context, TAG, "onResponse: 1111" + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    InvoiceResponse invoiceResponse = response.body();

                    String checkoutUrl = invoiceResponse.getInvoiceUrl();
                    Logger.d(context, TAG, "onResponse: Invoice URL: " + checkoutUrl);
                    if (checkoutUrl != null) {
                        MyBottomSheetCardPayment bottomSheetDialogFragment = new MyBottomSheetCardPayment(
                                checkoutUrl,
                                amount,
                                MainActivity.uid,
                                uid_Double,
                                context,
                                MainActivity.order_id
                        );
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                    } else {
                        Logger.d(context, TAG, "Response body is null");
                        cancelOrderDouble();
                    }
                } else {
                    Logger.d(context, TAG, "Request failed: " + response.code());
                    cancelOrderDouble();
                }
            }

            @Override
            public void onFailure(@NonNull Call<InvoiceResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Request failed: " + t.getMessage());
                cancelOrderDouble();
            }
        });
    }

    private void paymentByTokenWfp(
            String orderDescription,
            String amount
    ) {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
                .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
                .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        PurchaseService service = retrofit.create(PurchaseService.class);
        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
        String city = stringList.get(1);

        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
        callOrderIdMemory(MainActivity.order_id, MainActivity.uid, pay_method);


        Call<PurchaseResponse> call = service.purchaseWithChangeToken(
                getString(R.string.application),
                city,
                MainActivity.order_id,
                MainActivity.uid,
                orderDescription,
                email,
                FinishSeparateFragment.phoneNumber
        );
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<PurchaseResponse> call, @NonNull Response<PurchaseResponse> response) {

            }

            @Override
            public void onFailure(@NonNull Call<PurchaseResponse> call, @NonNull Throwable t) {
                // Логируем текст ошибки
                Logger.d(context, TAG, "Ошибка при выполнении запроса: " + t.getMessage());
            }

        });

    }

    void callOrderIdMemory(String orderId, String uid, String paySystem) {
        String baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<Void> call = apiService.orderIdMemory(orderId, uid, paySystem);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Обработка ошибки
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }


    public void orderFinished(String page) throws MalformedURLException {
        String urlOrder = getTaxiUrlSearchMarkers(page, context);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        ToJSONParserRetrofit parser = new ToJSONParserRetrofit();

//            // Пример строки URL с параметрами
        Logger.d(context, TAG, "orderFinished: " + baseUrl + urlOrder);
        parser.sendURL(urlOrder, new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                Map<String, String> sendUrlMap = response.body();

                assert sendUrlMap != null;
                String orderWeb = sendUrlMap.get("order_cost");
                String message = sendUrlMap.get("message");
                assert orderWeb != null;
                if (!orderWeb.equals("0")) {

                    String pay_method_message = context.getString(R.string.pay_method_message_main);
                    pay_method_message += " " + context.getString(R.string.pay_method_message_nal);

                    String messagePayment = orderWeb + context.getString(R.string.UAH) + " " + pay_method_message;

                    FinishSeparateFragment.textCost.setVisibility(View.VISIBLE);
                    FinishSeparateFragment.textCostMessage.setVisibility(View.VISIBLE);
                    FinishSeparateFragment.carProgressBar.setVisibility(View.VISIBLE);
//                    FinishSeparateFragment.progressBar.setVisibility(View.VISIBLE);
                    FinishSeparateFragment.progressSteps.setVisibility(View.VISIBLE);

                    FinishSeparateFragment.btn_options.setVisibility(View.VISIBLE);
                    FinishSeparateFragment.btn_open.setVisibility(View.VISIBLE);


                    FinishSeparateFragment.textCostMessage.setText(messagePayment);
                    MainActivity.uid = sendUrlMap.get("dispatching_order_uid");
                    uid_Double = " ";

                    pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
//                    FinishSeparateFragment.handlerStatus.post(FinishSeparateFragment.myTaskStatus);
                    if (pay_method.equals("wfp_payment")) {
                        FinishSeparateFragment.handlerAddcost.postDelayed(FinishSeparateFragment.showDialogAddcost, FinishSeparateFragment.timeCheckOutAddCost);
                    }
                    FinishSeparateFragment.pay_method = "nal_payment";
                } else {
                    assert message != null;
                    if (message.contains("Дублирование")) {
                        message = context.getResources().getString(R.string.double_order_error);
                        if(isAdded()) {
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }

                    } else if (message.equals("ErrorMessage")) {
                        message = context.getResources().getString(R.string.server_error_connected);
                        if(isAdded()) {
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (isAdded()) {
                            message = context.getResources().getString(R.string.error_message);
                            if(isAdded()) {
                                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
//        FinishSeparateFragment.progressBar.setVisibility(View.GONE);
        dismiss();
    }


    @SuppressLint("Range")
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: " + urlAPI);

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
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: start " + start);
        // Заменяем символ '/' в строках
        if (start != null) {
            start = start.replace("/", "|");
        }
        if (finish != null) {
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
        String tarif = stringListInfo.get(2);
        String payment_type = stringListInfo.get(4);
        String addCost = stringListInfo.get(5);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);

        if (urlAPI.equals("costSearchMarkersTime")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail + "*" + payment_type + "/"
                    + time + "/" + date;
        }
        if (urlAPI.equals("orderSearchMarkersVisicom")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);


            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail + "*" + payment_type + "/" + addCost + "/"
                    + time + "/" + comment + "/" + date + "/" + start + "/" + finish;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
//            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[]{"1"});

        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i < services.size() - 1; i++) {
            if (services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if (servicesVer) {
            for (int i = 0; i < DataArr.arrayServiceCode().length; i++) {
                if (services.get(i + 1).equals("1")) {
                    servicesChecked.add(DataArr.arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if (servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Logger.d(context, TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city + "/" + context.getString(R.string.application);

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
        callOrderIdMemory(MainActivity.order_id, MainActivity.uid, pay_method);

        PaymentApiToken paymentApi = retrofit.create(PaymentApiToken.class);

//        String merchantPassword = arrayList.get(7);
        List<String> stringList = logCursor(MainActivity.TABLE_USER_INFO, requireActivity());
        String email = stringList.get(3);

        String order_id = MainActivity.order_id;

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

        Logger.d(context, TAG, "paymentByTokenFondy: " + rectoken);

        Logger.d(context, TAG, "getStatusFondy: " + params);
        SignatureClient signatureClient = new SignatureClient();
// Передаем экземпляр SignatureCallback в метод generateSignature
        signatureClient.generateSignature(queryString, new SignatureClient.SignatureCallback() {
            @Override
            public void onSuccess(SignatureResponse response) {
                // Обработка успешного ответа
                String digest = response.getDigest();
                Logger.d(context, TAG, "Received signature digest: " + digest);

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
                Logger.d(context, TAG, "getUrlToPayment: " + statusRequest);

                Call<ApiResponseToken<SuccessResponseDataToken>> call = paymentApi.makePayment(statusRequest);


                call.enqueue(new Callback<ApiResponseToken<SuccessResponseDataToken>>() {

                    @Override
                    public void onResponse(@NonNull Call<ApiResponseToken<SuccessResponseDataToken>> call, Response<ApiResponseToken<SuccessResponseDataToken>> response) {
                        Logger.d(context, TAG, "onResponse: 1111" + response.code());
                        if (response.isSuccessful() && response.body() != null) {
                            ApiResponseToken<SuccessResponseDataToken> apiResponse = response.body();

                            Logger.d(context, TAG, "onResponse: " + new Gson().toJson(apiResponse));
                            try {
                                SuccessResponseDataToken responseBody = response.body().getResponse();

                                // Теперь у вас есть объект ResponseBodyRev для обработки
                                if (responseBody != null) {
                                    Logger.d(context, TAG, "JSON Response: " + new Gson().toJson(apiResponse));
                                    String orderStatus = responseBody.getOrderStatus();
                                    if (!"approved".equals(orderStatus)) {
                                        // Обработка ответа об ошибке
                                        String errorResponseMessage = responseBody.getErrorMessage();
                                        String errorResponseCode = responseBody.getErrorCode();
                                        Logger.d(context, TAG, "onResponse: errorResponseMessage " + errorResponseMessage);
                                        Logger.d(context, TAG, "onResponse: errorResponseCode" + errorResponseCode);
                                        cancelOrderDouble();

                                    }
                                } else {
                                    cancelOrderDouble();

//                            getUrlToPaymentFondy(messageFondy, amount);
                                }
                            } catch (JsonSyntaxException e) {
                                FirebaseCrashlytics.getInstance().recordException(e);
                                // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                                Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
                                cancelOrderDouble();
                            }
                        } else {
                            // Обработка ошибки
                            Logger.d(context, TAG, "onFailure: " + response.code());
                            cancelOrderDouble();
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponseToken<SuccessResponseDataToken>> call, @NonNull Throwable t) {
                        Logger.d(context, TAG, "onFailure1111: " + t);
                        cancelOrderDouble();
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Обработка ошибки
                Logger.d(context, TAG, "Received signature error: " + error);
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
        Logger.d(context, TAG, "getUrlToPayment: " + statusRequest);

        Call<ApiResponsePay<SuccessResponseDataPay>> call = paymentApi.makePayment(statusRequest);

        call.enqueue(new Callback<ApiResponsePay<SuccessResponseDataPay>>() {

            @Override
            public void onResponse(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, Response<ApiResponsePay<SuccessResponseDataPay>> response) {
                Logger.d(context, TAG, "onResponse: 1111" + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponsePay<SuccessResponseDataPay> apiResponse = response.body();

                    Logger.d(context, TAG, "onResponse: " + new Gson().toJson(apiResponse));
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
                                        MainActivity.uid,
                                        uid_Double,
                                        context,
                                        MainActivity.order_id
                                );
                                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                            } else if ("failure".equals(responseStatus)) {
                                // Обработка ответа об ошибке
                                String errorResponseMessage = responseBody.getErrorMessage();
                                String errorResponseCode = responseBody.getErrorCode();
                                Logger.d(context, TAG, "onResponse: errorResponseMessage " + errorResponseMessage);
                                Logger.d(context, TAG, "onResponse: errorResponseCode" + errorResponseCode);
                                cancelOrderDouble();
                                // Отобразить сообщение об ошибке пользователю
                            } else {
                                // Обработка других возможных статусов ответа
                                cancelOrderDouble();
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
                    Logger.d(context, TAG, "onFailure: " + response.code());
                    cancelOrderDouble();
                }

            }

            @Override
            public void onFailure(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, Throwable t) {
                Logger.d(context, TAG, "onFailure1111: " + t);
                cancelOrderDouble();
            }


        });
    }

    private void cancelOrderDouble() {
        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        ;
        String url = baseUrl + api + "/android/webordersCancelDouble/" + MainActivity.uid + "/" + uid_Double + "/" + pay_method + "/" + city + "/" + context.getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrderDouble(url);
        Logger.d(context, TAG, "cancelOrderDouble: " + url);

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Logger.d(context, TAG, "cancelOrderDouble response: " + response.toString());


                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    Logger.d(context, TAG, "onFailure: Тайм-аут соединения");
                } else if (t instanceof IOException) {
                    Logger.d(context, TAG, "onFailure: Ошибка сети или соединения");
                } else {
                    Logger.d(context, TAG, "onFailure: Непредвиденная ошибка");
                }

                // Логируем исключение
                FirebaseCrashlytics.getInstance().recordException(t);

                // Выводим сообщение пользователю
                String errorMessage = t.getMessage();
                Logger.d(context, TAG, "onFailure: Ошибка: " + errorMessage);

            }

        });
        dismiss();
    }

    private void cancelOrderDoubleForNal() {
        try {
            VisicomFragment.sendUrlMap = null;
            paymentType();
            Bundle bundle = new Bundle();

            bundle.putString("text_full_message", FinishSeparateFragment.text_full_message.getText().toString());
            bundle.putString("uid", uid);
            bundle.putString("uid_Double", uid_Double);
            Logger.d(context, TAG, "uid " + uid);
            Logger.d(context, TAG, "uid_Double " + uid_Double);

            MainActivity.navController.navigate(
                    R.id.nav_cacheOrder,
                    bundle,
                    new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_cacheOrder, true)
                            .build()
            );
            dismiss();
        } catch (IllegalArgumentException e) {
            Logger.e(context, TAG, "Ошибка навигации: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        MainActivity.uid= "";
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
        ArrayList<Map<String, String>> cardMaps = getCardMapsFromDatabase(table);
        if (!cardMaps.isEmpty()) {
            CustomCardAdapter listAdapter = new CustomCardAdapter(context, cardMaps, table, pay_method);
            listView.setAdapter(listAdapter);
        } else {
            listView.setVisibility(View.GONE);
            btn_card.setVisibility(View.GONE);
            text_card.setVisibility(View.GONE);
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

        if (cursor.moveToFirst()) {
            do {
                result = cursor.getString(cursor.getColumnIndex("rectoken"));
                Logger.d(context, TAG, "Found rectoken with rectoken_check = 1 " + ": " + result);
                return result;
            } while (cursor.moveToNext());
        }
        cursor.close();

        database.close();

        return result;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context; // Инициализация контекста
    }

    private void paymentType() {
        MainActivity.paySystemStatus = "nal_payment";
        ContentValues cv = new ContentValues();
        cv.put("payment_type", "nal_payment");
        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[]{"1"});
        database.close();
        Logger.d(context, TAG, "paymentType: nal_payment ");
    }

    @SuppressLint("Range")
    private ArrayList<Map<String, String>> getCardMapsFromDatabase(String table) {
        ArrayList<Map<String, String>> cardMaps = new ArrayList<>();
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        // Выполните запрос к таблице TABLE_FONDY_CARDS и получите данные
        Cursor cursor = database.query(table, null, null, null, null, null, null);
        Logger.d(context, TAG, "getCardMapsFromDatabase: card count: " + cursor.getCount());

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

    private DialogStateListener dialogStateListener;

    public void setDialogStateListener(DialogStateListener listener) {
        this.dialogStateListener = listener;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        // Уведомляем слушателя, что диалог закрылся
        if (dialogStateListener != null) {
            dialogStateListener.onDialogClosed();
        }
    }

}
