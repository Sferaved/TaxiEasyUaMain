package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.finish.ApiService;
import com.taxi.easy.ua.ui.fondy.gen_signatur.SignatureClient;
import com.taxi.easy.ua.ui.fondy.gen_signatur.SignatureResponse;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusResponse;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusService;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorPaymentFragment;
import com.taxi.easy.ua.utils.log.Logger;
import com.uxcam.UXCam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyBottomSheetCardPayment extends BottomSheetDialogFragment {
    private WebView webView;
    private final String TAG = "MyBottomSheetCardPayment";
    private final String checkoutUrl;
    private final String amount;
    String email, uid, uid_Double;
    private String pay_method;
//    private final String baseUrl = "https://m.easy-order-taxi.site";
    private String baseUrl;
    private boolean hold;
    private static String timeoutText;
    private static String messageWaitingCarSearch;
    private static String messageSearchesForCar;
    private static String messageCanceled;
    private static String messageCarFoundOrderCarInfo;
    private static String messageCarFoundOrderdriverPhone;
    private static String messageCarFoundRequiredTime;
    private static String def_status;
    private static String messageFondy;
    private String order_id;
    private String invoiceId;
    private static String city;
    private static String api;
    private static String application;
    private static String comment;
    private static String MERCHANT_ID;
    private static String merchantPassword;

    private final Context context;
    private static final int TIMEOUT_SECONDS = 60;
    private CountDownTimer paymentTimer;
    private FragmentManager fragmentManager;

    public MyBottomSheetCardPayment(
            String checkoutUrl,
            String amount,
            String uid,
            String uid_Double,
            Context context,
            String order_id
            ) {
        this.checkoutUrl = checkoutUrl;
        this.amount = amount;
        this.uid = uid;
        this.uid_Double = uid_Double;
        this.hold = false;
        this.context = context;
        this.order_id = order_id;
    }



    @SuppressLint({"MissingInflatedId", "SetJavaScriptEnabled"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fondy_payment, container, false);

        UXCam.tagScreenName(TAG);

        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        webView = view.findViewById(R.id.webView);
        email = logCursor(MainActivity.TABLE_USER_INFO, requireActivity()).get(3);
        fragmentManager = getParentFragmentManager();
        timeoutText = context.getString(R.string.time_out_text);
        messageWaitingCarSearch = context.getString(R.string.ex_st_1);
        messageSearchesForCar = context.getString(R.string.ex_st_0);
        messageCanceled = context.getString(R.string.ex_st_canceled);
        messageCarFoundOrderCarInfo = context.getString(R.string.ex_st_3);
        messageCarFoundOrderdriverPhone = context.getString(R.string.ex_st_4);
        messageCarFoundRequiredTime = context.getString(R.string.ex_st_5);
        def_status = context.getString(R.string.def_status);
        application = context.getString(R.string.application);
        comment = context.getString(R.string.fondy_revers_message) + context.getString(R.string.fondy_message);
        messageFondy =  context.getString(R.string.fondy_message);
        List<String> listCity = logCursor(MainActivity.CITY_INFO, requireActivity());
        city = listCity.get(1);
        api = listCity.get(2);

      
        MERCHANT_ID = listCity.get(6);
        merchantPassword = listCity.get(7);
        

        invoiceId = MainActivity.invoiceId;

        Logger.d(getActivity(), TAG, "onCreateView:timeoutText " + timeoutText);
        Logger.d(context, TAG, "onCreateView: MainActivity.order_id " + MainActivity.order_id);
        Logger.d(context, TAG, "onCreateView:  order_id " +  order_id);
        // Настройка WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Включает DOM-хранилище
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); // Разрешает открытие новых окон
        webSettings.setSupportMultipleWindows(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                Logger.d(view.getContext(), "WebChromeClient", "onCreateWindow triggered");
                try {
                    WebView newWebView = new WebView(view.getContext());
                    WebSettings webSettings = newWebView.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webSettings.setDomStorageEnabled(true);
                    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                    webSettings.setSupportMultipleWindows(true);

                    newWebView.setWebViewClient(new WebViewClient());
                    newWebView.setWebChromeClient(this);

                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setView(newWebView);
                    builder.setPositiveButton(R.string.close, (dialog, which) -> newWebView.destroy());
                    builder.show();

                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(newWebView);
                    resultMsg.sendToTarget();
                    return true;
                } catch (Exception e) {
                    Logger.e(view.getContext(), "WebChromeClient", "Ошибка в onCreateWindow: " + e.getMessage());
                    return false;
                }
            }

        });
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                view.getWindowVisibleDisplayFrame(r);
                int screenHeight = view.getRootView().getHeight();

                // Вычисляем размер видимой области экрана
                int heightDifference = screenHeight - (r.bottom - r.top);

                // Если высота разницы больше 200dp (можете подстроить под свои нужды)
                if (heightDifference > dpToPx(200)) {
                    // Поднимаем WebView
                    view.setTranslationY(-heightDifference);
                } else {
                    // Сбрасываем перевод, если клавиатура закрыта
                    view.setTranslationY(0);
                }
            }
        });
//        pay_system();
        paymentView(order_id);

        return view;
    }

    private void paymentView(String orderReference) {
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        switch (pay_method) {
            case "wfp_payment":
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {

                        Logger.d(context, TAG, "Загружен URL: " + url);
                        if(url.contains("https://secure.wayforpay.com/invoice")){
                            return false;
                        }
                        if(url.contains("https://secure.wayforpay.com/closing")
                        ) {
                            getStatusWfp(orderReference);
                            return false;
                        }
                        return false;
                        // Возвращаем false, чтобы разрешить WebView загрузить страницу.
                    }
                });
                // Ensure checkoutUrl is not null and valid before loading it
                if (checkoutUrl != null && URLUtil.isValidUrl(checkoutUrl)) {
                    webView.loadUrl(checkoutUrl);
                } else {
                    Log.e("MyBottomSheetCardVerification", "Checkout URL is null or invalid");
                    // Handle the error appropriately, e.g., show an error message to the user
                }
                break;
            case "fondy_payment":

                List<String>  arrayList = logCursor(MainActivity.CITY_INFO, requireActivity());
                String MERCHANT_ID = arrayList.get(6);


                Map<String, String> params = new TreeMap<>();
                params.put("order_id", MainActivity.order_id);
                params.put("merchant_id", MERCHANT_ID);
                SignatureClient signatureClient = new SignatureClient();
                signatureClient.generateSignature(params.toString(), new SignatureClient.SignatureCallback() {
                    @Override
                    public void onSuccess(SignatureResponse response) {
                        // Обработка успешного ответа
                        String digest = response.getDigest();
                        Logger.d(getActivity(), TAG, "Received signature digest: " + digest);
                        webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                Logger.d(getActivity(), TAG, "Загружен URL: " + url);

                                if(url.equals(baseUrl +"/mono/redirectUrl")) {
                                    Logger.d(getActivity(), TAG, "shouldOverrideUrlLoading: " + pay_method);
//                                                getStatusFondy(digest);
                                    return true;
                                } else {
                                    // Возвращаем false, чтобы разрешить WebView загрузить страницу.
                                    return false;
                                }
                            }
                        });
                        // Ensure checkoutUrl is not null and valid before loading it
                        if (checkoutUrl != null && URLUtil.isValidUrl(checkoutUrl)) {
                            webView.loadUrl(checkoutUrl);
                        } else {
                            Log.e("MyBottomSheetCardVerification", "Checkout URL is null or invalid");
                            // Handle the error appropriately, e.g., show an error message to the user
                        }
                    }
                    @Override
                    public void onError(String error) {
                        // Обработка ошибки

                        Logger.d(getActivity(), TAG, "Received signature error: " + error);
                    }
                });
                break;
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        assert dialog != null;
        FrameLayout bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        assert bottomSheet != null;
        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
        ViewGroup.LayoutParams layoutParams = bottomSheet.getLayoutParams();
        layoutParams.height = (int) (Resources.getSystem().getDisplayMetrics().heightPixels * 0.9);
        bottomSheet.setLayoutParams(layoutParams);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
//    private void pay_system() {
//        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//
//        OkHttpClient client = new OkHttpClient.Builder()
//                .addInterceptor(interceptor)
//                .build();
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(baseUrl)
//                .addConverterFactory(GsonConverterFactory.create())
//                .client(client)
//                .build();
//
//        PayApi apiService = retrofit.create(PayApi.class);
//        Call<ResponsePaySystem> call = apiService.getPaySystem();
//        call.enqueue(new Callback<ResponsePaySystem>() {
//            @Override
//            public void onResponse(@NonNull Call<ResponsePaySystem> call, @NonNull Response<ResponsePaySystem> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    // Обработка успешного ответа
//                    ResponsePaySystem responsePaySystem = response.body();
//                    assert responsePaySystem != null;
//                    String paymentCode = responsePaySystem.getPay_system();
//                    Logger.d(getActivity(), TAG, "Загружен URL: " + checkoutUrl);
//                    switch (paymentCode) {
//                        case "wfp":
//                            pay_method = "wfp_payment";
//                            webView.setWebViewClient(new WebViewClient() {
//                                @Override
//                                public boolean shouldOverrideUrlLoading(WebView view, String url) {
//
//                                    Logger.d(context, TAG, "Загружен URL: " + url);
//                                    if(url.contains("https://secure.wayforpay.com/invoice")){
//                                        return false;
//                                    }
//                                    if(url.contains("https://secure.wayforpay.com/closing")
//                                    ) {
//                                        getStatusWfp(order_id);
//                                        return false;
//                                    }
//                                    return false;
//                                    // Возвращаем false, чтобы разрешить WebView загрузить страницу.
//                                }
//                            });
//                            // Ensure checkoutUrl is not null and valid before loading it
//                            if (checkoutUrl != null && URLUtil.isValidUrl(checkoutUrl)) {
//                                webView.loadUrl(checkoutUrl);
//                            } else {
//                                Log.e("MyBottomSheetCardVerification", "Checkout URL is null or invalid");
//                                // Handle the error appropriately, e.g., show an error message to the user
//                            }
//                            break;
//                        case "fondy":
//                            pay_method = "fondy_payment";
//                            List<String>  arrayList = logCursor(MainActivity.CITY_INFO, requireActivity());
//                            String MERCHANT_ID = arrayList.get(6);
//
//
//                            Map<String, String> params = new TreeMap<>();
//                            params.put("order_id", MainActivity.order_id);
//                            params.put("merchant_id", MERCHANT_ID);
//                            SignatureClient signatureClient = new SignatureClient();
//                            signatureClient.generateSignature(params.toString(), new SignatureClient.SignatureCallback() {
//                                @Override
//                                public void onSuccess(SignatureResponse response) {
//                                    // Обработка успешного ответа
//                                    String digest = response.getDigest();
//                                    Logger.d(getActivity(), TAG, "Received signature digest: " + digest);
//                                    webView.setWebViewClient(new WebViewClient() {
//                                        @Override
//                                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                                            Logger.d(getActivity(), TAG, "Загружен URL: " + url);
//
//                                            if(url.equals(baseUrl +"/mono/redirectUrl")) {
//                                                Logger.d(getActivity(), TAG, "shouldOverrideUrlLoading: " + pay_method);
////                                                getStatusFondy(digest);
//                                                return true;
//                                            } else {
//                                                // Возвращаем false, чтобы разрешить WebView загрузить страницу.
//                                                return false;
//                                            }
//                                        }
//                                    });
//                                    // Ensure checkoutUrl is not null and valid before loading it
//                                    if (checkoutUrl != null && URLUtil.isValidUrl(checkoutUrl)) {
//                                        webView.loadUrl(checkoutUrl);
//                                    } else {
//                                        Log.e("MyBottomSheetCardVerification", "Checkout URL is null or invalid");
//                                        // Handle the error appropriately, e.g., show an error message to the user
//                                    }
//                                }
//                                @Override
//                                public void onError(String error) {
//                                    // Обработка ошибки
//
//                                    Logger.d(getActivity(), TAG, "Received signature error: " + error);
//                                }
//                            });
//                            break;
//                        case "mono":
//                            pay_method = "mono_payment";
//                            break;
//                    }
//                    if(isAdded()){
//                        ContentValues cv = new ContentValues();
//                        cv.put("payment_type", pay_method);
//                        // обновляем по id
//                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//                        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
//                                new String[] { "1" });
//                        database.close();
//
//                    }
//
//
//                } else {
//                    if (isAdded()) { //
//                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
//                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//                    }
//
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<ResponsePaySystem> call, @NonNull Throwable t) {
//                if (isAdded()) { //
//                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
//                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//                }
//            }
//        });
//    }

    private void getStatusWfp(String order_id) {
        Logger.d(getActivity(), TAG, "getStatusWfp: ");
        Logger.d(context, TAG, "getStatusWfp: MainActivity.order_id " +  order_id);
        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String city = stringList.get(1);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request();
                    // Логируем URL запроса
                    Log.d("HTTP Request", "Request URL: " + request.url());
                    return chain.proceed(request);
                })
                .addInterceptor(interceptor) // Ваш существующий перехватчик
                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
                .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
                .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
                .build();

        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl +"/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        StatusService service = retrofit.create(StatusService.class);

        Call<StatusResponse> call = service.checkStatus(
                context.getString(R.string.application),
                city,
                order_id
        );
// Логируем вызов для подтверждения
//        Log.d("HTTP Request", "Call prepared: " + call.request().url());

        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<StatusResponse> call, @NonNull Response<StatusResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    StatusResponse statusResponse = response.body();
                    if (statusResponse != null) {
                        String orderStatus = statusResponse.getTransactionStatus();
                        if (orderStatus != null) {
                            Logger.d(context, TAG, "Transaction Status: " + orderStatus);
                            hold = false;
                            switch (orderStatus) {
                                case "Approved":
                                case "WaitingAuthComplete":
                                    sharedPreferencesHelperMain.saveValue("pay_error", "**");
                                    getCardTokenWfp(city);
                                    hold = true;
                                    break;
                                default:
                                    sharedPreferencesHelperMain.saveValue("pay_error", "pay_error");
                                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                                    callOrderIdMemory(MainActivity.order_id, MainActivity.uid, pay_method);

                                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                                    dismiss();
                            }
                        }
                    } else {
                        Logger.d(getActivity(), TAG, "Response body is null");
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        callOrderIdMemory(MainActivity.order_id, MainActivity.uid, pay_method);

                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
                } else {
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    callOrderIdMemory(MainActivity.order_id, MainActivity.uid, pay_method);

                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    Logger.d(getActivity(), TAG, "Request failed:");

                }
            }

            @Override
            public void onFailure(@NonNull Call<StatusResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Request failed:"+ t.getMessage());
            }
        });

    }

    private void callOrderIdMemory(String orderId, String uid, String paySystem) {
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

    private  void getCardTokenWfp(String city) {
        String tableName = MainActivity.TABLE_WFP_CARDS; // Например, "wfp_cards"
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.execSQL("DELETE FROM " + tableName + ";");
        database.close();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
                .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
                .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl) // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        // Создайте сервис
        CallbackServiceWfp service = retrofit.create(CallbackServiceWfp.class);
        Logger.d(context, TAG, "getCardTokenWfp: ");
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

        // Выполните запрос
        Call<CallbackResponseWfp> call = service.handleCallbackWfpCardsId(
                context.getString(R.string.application),
                city,
                userEmail,
                "wfp"
        );
        call.enqueue(new Callback<CallbackResponseWfp>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
                Logger.d(context, TAG, "onResponse: " + response.body());
                if (response.isSuccessful() && response.body() != null) {
                    CallbackResponseWfp callbackResponse = response.body();
                    if (callbackResponse != null) {
                        List<CardInfo> cards = callbackResponse.getCards();
                        Logger.d(context, TAG, "onResponse: cards" + cards);
                        String tableName = MainActivity.TABLE_WFP_CARDS; // Например, "wfp_cards"

// Открываем или создаем базу данных
                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

                         if (cards != null && !cards.isEmpty()) {
                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты
                                String merchant = cardInfo.getMerchant(); //
                                String  active = cardInfo.getActive();

                                Logger.d(context, TAG, "onResponse: card_token: " + rectoken);
                                ContentValues cv = new ContentValues();
                                cv.put("masked_card", masked_card);
                                cv.put("card_type", card_type);
                                cv.put("bank_name", bank_name);
                                cv.put("rectoken", rectoken);
                                cv.put("merchant", merchant);
                                cv.put("rectoken_check", active);
                                database.insert(MainActivity.TABLE_WFP_CARDS, null, cv);
                            }
                        }
                        database.close();
                    }

                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
                Logger.d(context, TAG, "onResponse: failure " + t);
            }
        });

    }

//    private void getCardTokenWfp(String city) {
//
//
//
//        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
//
//        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//
//        OkHttpClient client = new OkHttpClient.Builder()
//                .addInterceptor(interceptor)
//                .build();
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(baseUrl) // Замените на фактический URL вашего сервера
//                .addConverterFactory(GsonConverterFactory.create())
//                .client(client)
//                .build();
//
//        // Создайте сервис
//        CallbackServiceWfp service = retrofit.create(CallbackServiceWfp.class);
//
//        // Выполните запрос
//        Call<CallbackResponseWfp> call = service.handleCallbackWfp(
//                context.getString(R.string.application),
//                city,
//                email,
//                "wfp"
//        );
//        call.enqueue(new Callback<CallbackResponseWfp>() {
//            @Override
//            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
//
//                if (response.isSuccessful() && response.body() != null) {
//                    CallbackResponseWfp callbackResponse = response.body();
//                    if (callbackResponse != null) {
//                        List<CardInfo> cards = callbackResponse.getCards();
//
//                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
//                        database.delete(MainActivity.TABLE_WFP_CARDS, "1", null);
//                        if (cards != null && !cards.isEmpty()) {
//                            for (CardInfo cardInfo : cards) {
//                                String masked_card = cardInfo.getMasked_card(); // Маска карты
//                                String card_type = cardInfo.getCard_type(); // Тип карты
//                                String bank_name = cardInfo.getBank_name(); // Название банка
//                                String rectoken = cardInfo.getRectoken(); // Токен карты
//                                String merchant = cardInfo.getMerchant(); // Токен карты
//
//
//                                ContentValues cv = new ContentValues();
//                                cv.put("masked_card", masked_card);
//                                cv.put("card_type", card_type);
//                                cv.put("bank_name", bank_name);
//                                cv.put("rectoken", rectoken);
//                                cv.put("merchant", merchant);
//                                cv.put("rectoken_check", "0");
//                                database.insert(MainActivity.TABLE_WFP_CARDS, null, cv);
//                            }
//                            Cursor cursor = database.rawQuery("SELECT * FROM " + MainActivity.TABLE_WFP_CARDS + " ORDER BY id DESC LIMIT 1", null);
//                            if (cursor.moveToFirst()) {
//                                // Получаем значение ID последней записи
//                                @SuppressLint("Range") int lastId = cursor.getInt(cursor.getColumnIndex("id"));
//                                cursor.close();
//
//                                // Обновляем строку с найденным ID
//                                ContentValues cv = new ContentValues();
//                                cv.put("rectoken_check", "1");
//                                database.update(MainActivity.TABLE_WFP_CARDS, cv, "id = ?", new String[] { String.valueOf(lastId) });
//                            }
//
//                            database.close();
//                        }
//                    }
//
//                } else {
//                      MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
//                      bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
//                // Обработка ошибки запроса
//
//                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
//                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//            }
//        });
////        dismiss();
//    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        getStatusWfp(order_id);
        Logger.d(getActivity(), TAG, "onDismiss: hold " + hold);
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

