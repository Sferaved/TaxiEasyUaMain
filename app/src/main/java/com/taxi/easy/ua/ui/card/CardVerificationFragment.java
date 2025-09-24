package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavOptions;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusResponse;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusService;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceResponse;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceService;
import com.taxi.easy.ua.ui.wfp.revers.ReversResponse;
import com.taxi.easy.ua.ui.wfp.revers.ReversService;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.helpers.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.uxcam.UXCam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class CardVerificationFragment extends Fragment {

    private final String TAG = "CardVerificationFragment";

    private WebView webView;
    private String order_id;
    private String amount;
    String email;
    String baseUrl;

    private FragmentManager fragmentManager;
    private Context context;
    private String messageFondy;

    @SuppressLint({"MissingInflatedId", "SetJavaScriptEnabled"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        UXCam.tagScreenName(TAG);
        if(button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }
        View view = inflater.inflate(R.layout.activity_fondy_payment, container, false);
        context = requireActivity();
        webView = view.findViewById(R.id.webView);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        amount = "1";
        messageFondy =  context.getString(R.string.fondy_message);
        order_id = UniqueNumberGenerator.generateUniqueNumber(context);
        
        fragmentManager = getParentFragmentManager();

         // Настройка WebView


        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Включает DOM-хранилище
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); // Разрешает открытие новых окон
        webSettings.setSupportMultipleWindows(true);

//        webView.getSettings().setJavaScriptEnabled(true);
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


        getUrlToPaymentWfp();

        return view;
    }


    private void getUrlToPaymentWfp() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor()) // 3 попытки
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

        Call<InvoiceResponse> call = service.createInvoice(
                context.getString(R.string.application),
                city,
                order_id,
                Integer.parseInt(amount),
                LocaleHelper.getLocale(),
                messageFondy,
                userEmail,
                phone_number
        );

        call.enqueue(new Callback<InvoiceResponse>() {
            @Override
            public void onResponse(@NonNull Call<InvoiceResponse> call, @NonNull Response<InvoiceResponse> response) {
                Logger.d(context, TAG, "onResponse: 1111" + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    InvoiceResponse invoiceResponse = response.body();

                    String checkoutUrl = invoiceResponse.getInvoiceUrl();
                    Logger.d(context, TAG, "onResponse: Invoice URL: " + checkoutUrl);
                    if(checkoutUrl != null) {
                        payWfp(checkoutUrl);
                    } else {
                        Logger.d(context, TAG,"Response body is null");
                    }
                } else {
                    Logger.d(context, TAG, "Request failed:3 " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<InvoiceResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(context, TAG, "Request failed:4 " + t.getMessage());
            }
        });

    }
    
    private void payWfp (String checkoutUrl)
    {
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Logger.d(context, TAG, "Загружен URL: " + url);

                if (url.contains("https://secure.wayforpay.com/invoice")) {
                    return false; // Разрешаем загрузку
                }
                if (url.contains("https://secure.wayforpay.com/closing")) {
                    getStatusWfp();
                    return false; // Разрешаем загрузку
                }
                return false; // По умолчанию разрешаем загрузку
            }

            // Для совместимости, если старый метод всё ещё вызовется, можно его перенаправить на новый
            @Override
            @Deprecated
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return shouldOverrideUrlLoading(view, new WebResourceRequest() {
                    @Override public Uri getUrl() { return Uri.parse(url); }
                    @Override public boolean isForMainFrame() { return true; }
                    @Override public boolean isRedirect() { return false; }
                    @Override public boolean hasGesture() { return false; }
                    @Override public String getMethod() { return "GET"; }
                    @Override public Map<String, String> getRequestHeaders() { return Collections.emptyMap(); }
                });
            }
        });

        // Ensure checkoutUrl is not null and valid before loading it
        if (checkoutUrl != null && URLUtil.isValidUrl(checkoutUrl)) {
            webView.loadUrl(checkoutUrl);
        } else {
            Logger.e(context,"MyBottomSheetCardVerification", "Checkout URL is null or invalid");
            // Handle the error appropriately, e.g., show an error message to the user
        }
    }

    private void getStatusWfp() {
        Logger.d(context, TAG, "getStatusWfp: ");

        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String city = stringList.get(1);

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor()) // 3 попытки
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
                .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
                .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
                .build();


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl  + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        StatusService service = retrofit.create(StatusService.class);

        Call<StatusResponse> call = service.checkStatus(
                context.getString(R.string.application),
                city,
                order_id
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<StatusResponse> call, @NonNull Response<StatusResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    StatusResponse statusResponse = response.body();
                    if (statusResponse != null) {
                        String orderStatus = statusResponse.getTransactionStatus();
                        Logger.d(context, TAG, "Transaction Status: " + orderStatus);

                        switch (orderStatus) {
                            case "Approved":
                            case "WaitingAuthComplete":
                                sharedPreferencesHelperMain.saveValue("pay_error", "**");
                                getReversWfp(city);
//                                dismiss();
                                getCardTokenWfp();
//                                MainActivity.navController.navigate(R.id.nav_card, null, new NavOptions.Builder()
//                                        .setPopUpTo(R.id.nav_card, true)
//                                        .build());
                                break;
                            default:
                                sharedPreferencesHelperMain.saveValue("pay_error", "pay_error");
                        }
                        // Другие данные можно также получить из statusResponse
                    } else {
                        Logger.d(context, TAG, "Response body is null");
                    }
                } else {
                    Logger.d(context, TAG, "Request failed:5");
                }
            }

            @Override
            public void onFailure(@NonNull Call<StatusResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Request failed:6" + t.getMessage());
                FirebaseCrashlytics.getInstance().recordException(t);
                MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
            }
        });

    }
    private  void getCardTokenWfp() {
        String city = logCursor(MainActivity.CITY_INFO, context).get(1);
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor()) // 3 попытки
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
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
                Logger.d(context, TAG, "onResponse: " + response.body());
                if (response.isSuccessful() && response.body() != null) {
                    CallbackResponseWfp callbackResponse = response.body();
                    if (callbackResponse != null) {
                        List<CardInfo> cards = callbackResponse.getCards();
                        Logger.d(context, TAG, "onResponse: cards" + cards);
                        String tableName = MainActivity.TABLE_WFP_CARDS; // Например, "wfp_cards"


                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);


                        database.execSQL("DELETE FROM " + tableName + ";");

                        if (cards != null && !cards.isEmpty()) {
                            for (CardInfo cardInfo : cards) {
                                String masked_card = cardInfo.getMasked_card(); // Маска карты
                                String card_type = cardInfo.getCard_type(); // Тип карты
                                String bank_name = cardInfo.getBank_name(); // Название банка
                                String rectoken = cardInfo.getRectoken(); // Токен карты
                                String merchant = cardInfo.getMerchant(); //
                                String active = cardInfo.getActive();

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
                        MainActivity.navController.navigate(R.id.nav_card, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_card, true)
                            .build());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(context, TAG, "onResponse: failure " + t);
            }
        });

        // Выполняем необходимое действие (например, запуск новой активности)
//        MainActivity.navController.navigate(R.id.nav_card, null, new NavOptions.Builder()
//                .setPopUpTo(R.id.nav_card, true)
//                .build());

    }
    private void getReversWfp(String city) {
        // Log method entry with input parameter
        Logger.i(context,"ReversWfp", "Starting getReversWfp with city: " + city);

        // Set up HTTP logging interceptor
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        Log.d("ReversWfp", "HttpLoggingInterceptor configured with level: BODY");

        // Retrieve base URL from SharedPreferences
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        Log.d("ReversWfp", "Base URL retrieved: " + baseUrl);

        // Build OkHttpClient with interceptors and timeouts
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor()) // 3 retries
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Connection timeout
                .readTimeout(30, TimeUnit.SECONDS)    // Read timeout
                .writeTimeout(30, TimeUnit.SECONDS)   // Write timeout
                .build();
        Logger.d(context,"ReversWfp", "OkHttpClient built with retry interceptor and 30s timeouts");

        // Build Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        Logger.d(context,"ReversWfp", "Retrofit instance created with base URL: " + baseUrl + "/");

        // Create ReversService
        ReversService service = retrofit.create(ReversService.class);
        Logger.d(context,"ReversWfp", "ReversService created");

        // Prepare API call parameters
        String application = context.getString(R.string.application);
        Logger.i(context,"ReversWfp", "Preparing refundVerifyCards API call with parameters: " +
                "application=" + application + ", city=" + city + ", order_id=" + order_id + ", amount=" + amount);

        // Make asynchronous API call
        Call<ReversResponse> call = service.refundVerifyCards(application, city, order_id, amount);
        Logger.d(context,"ReversWfp", "API call initiated for refundVerifyCards");

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ReversResponse> call, @NonNull Response<ReversResponse> response) {
                if (response.isSuccessful()) {
                    ReversResponse reversResponse = response.body();
                    if (reversResponse != null) {
                        Logger.i(context, "ReversWfp", "API call successful. Response: " + reversResponse.toString());
                    } else {
                        Logger.w(context, "ReversWfp", "API call successful but response body is null");
                    }
                } else {
                    Logger.w(context, "ReversWfp", "API call failed with HTTP code: " + response.code() +
                            ", message: " + response.message());
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        Logger.d(context, "ReversWfp", "Error body: " + errorBody);
                    } catch (IOException e) {
                        Logger.e(context, "ReversWfp", "Failed to read error body: " + e.getMessage() + e);
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReversResponse> call, @NonNull Throwable t) {
                Logger.e(context, "ReversWfp", "API call failed: " + t.getMessage() + t);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });

        // Log dismissal
        Logger.d(context,"ReversWfp", "Dismissing (likely UI dialog)");
        dismiss();
    }

    public void dismiss() {
        Logger.d(context, TAG, "onDismiss:");
        MainActivity.navController.navigate(R.id.nav_card, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_card, true)
                .build());
    }

    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = database.query(table, null, null, null, null, null, null);
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
        database.close();
        return list;
    }
}

