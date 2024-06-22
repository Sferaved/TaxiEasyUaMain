package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.NetworkChangeReceiver;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentCardBinding;
import com.taxi.easy.ua.ui.fondy.gen_signatur.SignatureClient;
import com.taxi.easy.ua.ui.fondy.gen_signatur.SignatureResponse;
import com.taxi.easy.ua.ui.fondy.payment.ApiResponsePay;
import com.taxi.easy.ua.ui.fondy.payment.PaymentApi;
import com.taxi.easy.ua.ui.fondy.payment.RequestData;
import com.taxi.easy.ua.ui.fondy.payment.StatusRequestPay;
import com.taxi.easy.ua.ui.fondy.payment.SuccessResponseDataPay;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.mono.MonoApi;
import com.taxi.easy.ua.ui.mono.cancel.RequestCancelMono;
import com.taxi.easy.ua.ui.mono.cancel.ResponseCancelMono;
import com.taxi.easy.ua.ui.payment_system.PayApi;
import com.taxi.easy.ua.ui.payment_system.ResponsePaySystem;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.ui.wfp.verify.VerifyService;
import com.taxi.easy.ua.utils.LocaleHelper;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.web.MyWebViewClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class CardFragment extends Fragment {

    private @NonNull FragmentCardBinding binding;
    public static AppCompatButton btnCardLink;

    private NetworkChangeReceiver networkChangeReceiver;
    private String baseUrl = "https://m.easy-order-taxi.site";
    private String messageFondy;
    public static ProgressBar progressBar;
    private String TAG = "TAG_CARD";
    String email;
    String amount = "100";
    public static TextView textCard;

    public static ListView listView;
    public static String table;
    String pay_method;
    NavController navController;
    private boolean show_cards;
    Activity context;
    WebView webView;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        webView = binding.webView;
        context = requireActivity();
        navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            navController.navigate(R.id.nav_visicom);
        }
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        btnCardLink  = binding.btnCardLink;

        return root;
    }
    private void pay_system() {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle")
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        String city = logCursor(MainActivity.CITY_INFO, context).get(1);
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        Log.d(TAG, "newUser: " + userEmail);

        PayApi apiService = retrofit.create(PayApi.class);
        Call<ResponsePaySystem> call = apiService.getPaySystem();
        call.enqueue(new Callback<ResponsePaySystem>() {
            @SuppressLint("Recycle")
            @Override
            public void onResponse(@NonNull Call<ResponsePaySystem> call, @NonNull Response<ResponsePaySystem> response) {
                if (response.isSuccessful()) {
                    // Обработка успешного ответа
                    ResponsePaySystem responsePaySystem = response.body();
                    assert responsePaySystem != null;
                    String paymentCode = responsePaySystem.getPay_system();
                    Cursor cursor;
                    switch (paymentCode) {
                        case "wfp":
                            pay_method = "wfp_payment";
                            getCardTokenWfp(city);
                            cursor = database.rawQuery("SELECT * FROM " + MainActivity.TABLE_WFP_CARDS + " ORDER BY id DESC LIMIT 1", null);
                            show_cards = cursor != null;
                            break;
                        case "fondy":
                            pay_method = "fondy_payment";
                            cursor = database.rawQuery("SELECT * FROM " + MainActivity.TABLE_FONDY_CARDS + " ORDER BY id DESC LIMIT 1", null);
                            show_cards = cursor != null;
                            break;
                        case "mono":
                            pay_method = "mono_payment";
                            cursor = database.rawQuery("SELECT * FROM " + MainActivity.TABLE_MONO_CARDS + " ORDER BY id DESC LIMIT 1", null);
                            show_cards = cursor != null;
                            break;
                    }
                    Log.d(TAG, "onResponse:show_cards " + show_cards);
                    if (show_cards) {
                        textCard.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        networkChangeReceiver = new NetworkChangeReceiver();
                        email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                        btnCardLink.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                            progressBar.setVisibility(View.VISIBLE);

                                Log.d(TAG, "onClick: " + pay_method);
                                NavController navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
                                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                                    navController.navigate(R.id.nav_visicom);
                                } else {
                                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(getActivity());
                                    messageFondy = getString(R.string.fondy_message);

                                    switch (pay_method) {
                                        case "wfp_payment":
                                            try {
                                                getUrlToPaymentWfp(MainActivity.order_id, messageFondy);
                                            } catch (UnsupportedEncodingException e) {
                                                throw new RuntimeException(e);
                                            }
                                            break;
                                        case "fondy_payment":
                                            try {
                                                getUrlToPaymentFondy(MainActivity.order_id, messageFondy);
                                            } catch (UnsupportedEncodingException e) {
                                                throw new RuntimeException(e);
                                            }
                                            break;
                                        case "mono_payment":
                                            getUrlToPaymentMono(MainActivity.order_id, messageFondy);
                                            break;
                                    }
                                    progressBar.setVisibility(View.GONE);

                                }
                            }
                        });
                        Log.d(TAG, "onResponse:pay_method "+pay_method);
                        ArrayList<Map<String, String>> cardMaps = new ArrayList<>();
                        switch (pay_method) {
                            case "wfp_payment":
                                cardMaps = getCardMapsFromDatabase(MainActivity.TABLE_WFP_CARDS);
                                table = MainActivity.TABLE_WFP_CARDS;
                                break;
                            case "fondy_payment":
                                cardMaps = getCardMapsFromDatabase(MainActivity.TABLE_FONDY_CARDS);
                                table = MainActivity.TABLE_FONDY_CARDS;
                                break;
                            case "mono_payment":
                                cardMaps = getCardMapsFromDatabase(MainActivity.TABLE_MONO_CARDS);
                                table = MainActivity.TABLE_MONO_CARDS;
                                break;
                        }
                        Log.d(TAG, "onResponse:cardMaps " + cardMaps);
                        if (!cardMaps.isEmpty()) {
                            CustomCardAdapter listAdapter = new CustomCardAdapter(context, cardMaps, table, pay_method);
                            listView.setAdapter(listAdapter);
                            progressBar.setVisibility(View.GONE);
                        } else {
                            textCard.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                            textCard.setText(R.string.no_cards);
                            progressBar.setVisibility(View.GONE);
                        }



                    } else {
                        textCard.setVisibility(View.GONE);
//            btnCardLink.setVisibility(View.GONE);
                        MyBottomSheetErrorCardFragment bottomSheetDialogFragment = new MyBottomSheetErrorCardFragment(getString(R.string.city_no_cards));
                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());

                    }
                } else {
                    if (isAdded()) { //
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                    }

                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponsePaySystem> call, @NonNull Throwable t) {
                if (isAdded()) { //
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }
        });
    }

    private  void getCardTokenWfp(String city) {
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
        Log.d(TAG, "getCardTokenWfp: ");
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

        // Выполните запрос
        Call<CallbackResponseWfp> call = service.handleCallbackWfp(
                context.getString(R.string.application),
                city,
                userEmail,
                "wfp"
        );
        call.enqueue(new Callback<CallbackResponseWfp>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
                Log.d(TAG, "onResponse: " + response.body());
                if (response.isSuccessful()) {
                    CallbackResponseWfp callbackResponse = response.body();
                    if (callbackResponse != null) {
                        List<CardInfo> cards = callbackResponse.getCards();
                        Log.d(TAG, "onResponse: cards" + cards);
                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.delete(MainActivity.TABLE_WFP_CARDS, "1", null);
                        if (cards != null && !cards.isEmpty()) {
                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты
                                String merchant = cardInfo.getMerchant(); // Токен карты

                                Log.d(TAG, "onResponse: card_token: " + rectoken);
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
                        database.close();
                    }

                } else {
                    // Обработка случаев, когда ответ не 200 OK
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
                Log.d(TAG, "onResponse: failure " + t.toString());
            }
        });
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        progressBar = binding.progressBar;
        progressBar.setVisibility(View.VISIBLE);
        pay_system();
        textCard = binding.textCard;
        listView = binding.listView;
        if(textCard.getVisibility() == View.VISIBLE) {
            btnCardLink.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        }




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


    private void getUrlToPaymentMono(String orderId, String messageFondy) {
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
                } else {
                    // Обработка ошибки запроса
                    Log.d("TAG2", "onResponse: Ошибка запроса, код " + response.code());
                    try {
                        assert response.errorBody() != null;
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
    private void paySystem(final PaySystemCallback callback) {
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

                    String paymentCodeNew = "wfp_payment"; // Изначально устанавливаем значение

                    switch (paymentCode) {
                        case "wfp":
                            paymentCodeNew = "wfp_payment";
                            break;
                        case "fondy":
                            paymentCodeNew = "fondy_payment";
                            break;
                        case "mono":
                            paymentCodeNew = "mono_payment";
                            break;
                    }

                    // Вызываем обработчик, передавая полученное значение
                    if (getActivity() != null) {
                        // Fragment is attached to an activity, it's safe to call onPaySystemResult
                        callback.onPaySystemResult(paymentCodeNew);
                    }
                } else {
                    // Обработка ошибки
                    callback.onPaySystemFailure(getString(R.string.verify_internet));
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(@NonNull Call<ResponsePaySystem> call, @NonNull Throwable t) {
                // Проверяем, что фрагмент присоединен к контексту
                if (isAdded()) {
                    // Обработка ошибки
                    callback.onPaySystemFailure(getString(R.string.verify_internet));
                }
                progressBar.setVisibility(View.GONE);
            }

        });
    }

    // Интерфейс для обработки результата и ошибки
    public interface PaySystemCallback {
        void onPaySystemResult(String paymentCode);
        void onPaySystemFailure(String errorMessage);
    }



    @SuppressLint("SetJavaScriptEnabled")
    private void getUrlToPaymentWfp(String order_id, String orderDescription) throws UnsupportedEncodingException {
// Настраиваем Retrofit
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(logging);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://m.easy-order-taxi.site/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(httpClient.build())
                .build();

        VerifyService service = retrofit.create(VerifyService.class);
        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String city = stringList.get(1);

        stringList = logCursor(MainActivity.TABLE_USER_INFO, context);
        String userEmail = stringList.get(3);
        String phone_number = stringList.get(2);

           Call<String> call = service.verify(
                getString(R.string.application),
                city,
                order_id,
                userEmail,
                phone_number,
                LocaleHelper.getLocale()
        );

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Включаем поддержку JavaScript
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Скрыть прогресс бар или что-то подобное
            }
        });
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Отобразить HTML в WebView
                    displayHtmlContent(response.body());
                } else {
                    Log.e(TAG, "Response was not successful or body was null");
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e(TAG, "IOException"  + t);
            }
        });

        progressBar.setVisibility(View.INVISIBLE);
    }

    private void displayHtmlContent(String htmlContent) {
        String baseUrl = "https://secure.wayforpay.com/"; // Замените на ваш базовый URL
        btnCardLink.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);

        // Присваиваем WebViewClient для отслеживания URL
        webView.setWebViewClient(new MyWebViewClient(context, navController));

        // Загружаем HTML-контент в WebView
        webView.loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null);
    }

    private void getUrlToPaymentFondy(String order_id, String orderDescription) throws UnsupportedEncodingException {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PaymentApi paymentApi = retrofit.create(PaymentApi.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO, context);
        String MERCHANT_ID = arrayList.get(6);

        Map<String, String> params = new TreeMap<>();
        params.put("order_id", order_id);
        params.put("order_desc", orderDescription);
        params.put("currency", "UAH");
        params.put("amount", amount);
        params.put("preauth", "Y");
        params.put("required_rectoken", "Y");
        params.put("merchant_id", MERCHANT_ID);
        params.put("sender_email", email);
        params.put("server_callback_url", "https://m.easy-order-taxi.site/server-callback");

        Log.d(TAG, "getStatusFondy: " + params);
        SignatureClient signatureClient = new SignatureClient();
// Передаем экземпляр SignatureCallback в метод generateSignature

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


        signatureClient.generateSignature(queryString, new SignatureClient.SignatureCallback() {
            @Override
            public void onSuccess(SignatureResponse response) {
                // Обработка успешного ответа
                String digest = response.getDigest();
                Log.d(TAG, "Received signature digest: " + digest);

                RequestData paymentRequest = new RequestData(
                        order_id,
                        orderDescription,
                        amount,
                        MERCHANT_ID,
                        digest,
                        email
                );


                StatusRequestPay statusRequest = new StatusRequestPay(paymentRequest);
                Log.d(TAG, "getUrlToPaymentFondy: " + statusRequest.toString());

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

                                        MyBottomSheetCardVerification bottomSheetDialogFragment = new MyBottomSheetCardVerification(checkoutUrl, amount);
                                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());


                                    } else if ("failure".equals(responseStatus)) {
                                        // Обработка ответа об ошибке
                                        String errorResponseMessage = responseBody.getErrorMessage();
                                        String errorResponseCode = responseBody.getErrorCode();
                                        Log.d("TAG1", "onResponse: errorResponseMessage " + errorResponseMessage);
                                        Log.d("TAG1", "onResponse: errorResponseCode" + errorResponseCode);
                                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.pay_failure));
                                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                                        // Отобразить сообщение об ошибке пользователю
                                    } else {
                                        // Обработка других возможных статусов ответа
                                    }
                                } else {
                                    // Обработка пустого тела ответа
                                }
                            } catch (JsonSyntaxException e) {
                                // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа

                            }
                        } else {
                            // Обработка ошибки
                            Log.d("TAG1", "onFailure: " + response.code());
                        }
                        progressBar.setVisibility(View.GONE);
//                navController.navigate(R.id.nav_visicom);

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, @NonNull Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Log.d("TAG1", "onFailure1111: " + t.toString());
                    }


                });
            }


            @Override
            public void onError(String error) {
                // Обработка ошибки

                Log.d(TAG, "Received signature error: " + error);
            }
        });

        progressBar.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}