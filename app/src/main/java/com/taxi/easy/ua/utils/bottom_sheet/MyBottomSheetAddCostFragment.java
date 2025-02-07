package com.taxi.easy.ua.utils.bottom_sheet;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.MyBottomSheetCardPayment;
import com.taxi.easy.ua.ui.finish.ApiService;
import com.taxi.easy.ua.ui.finish.Status;
import com.taxi.easy.ua.ui.finish.fragm.FinishSeparateFragment;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusResponse;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusService;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceResponse;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceService;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseResponse;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseService;
import com.taxi.easy.ua.utils.LocaleHelper;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MyBottomSheetAddCostFragment extends BottomSheetDialogFragment {

    private static final String TAG = "MyBottomSheetAddCostFragment";
    TextView textViewCost;
    AppCompatButton btn_ok, btn_minus, btn_plus;
    String cost, uid, uid_Double, pay_method;
    Context context;
    FragmentManager fragmentManager;


    public MyBottomSheetAddCostFragment(
            String cost,
            String uid,
            String uid_Double,
            String pay_method,
            Context context,
            FragmentManager fragmentManager
    ) {
        this.cost = cost;
        this.uid = uid;
        this.uid_Double = uid_Double;
        this.pay_method = pay_method;
        this.context = context;
        this.fragmentManager = fragmentManager;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.add_cost_bottom_layout, container, false);

        btn_ok = view.findViewById(R.id.btn_ok);

        btn_minus = view.findViewById(R.id.btn_minus);
        btn_plus = view.findViewById(R.id.btn_plus);
        textViewCost = view.findViewById(R.id.text_view_cost);


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Начальная стоимость
        int initialCost = Integer.parseInt(cost);
        int initialAddCost = 0;
// Текущее значение стоимости
        int[] currentCost = {initialCost};
        int[] currentAddCost = {initialAddCost};

        textViewCost.setText(String.valueOf(initialCost));

        btn_minus.setOnClickListener(v -> {
            if (currentCost[0] > initialCost) {
                currentCost[0] -= 5;
                currentAddCost[0] -= 5;
                textViewCost.setText(String.valueOf(currentCost[0]));
            }
        });

        btn_plus.setOnClickListener(v -> {
            currentCost[0] += 5;
            currentAddCost[0] += 5;
            textViewCost.setText(String.valueOf(currentCost[0]));
        });
        btn_ok.setOnClickListener(v -> {
            Logger.d(getActivity(), TAG, "btn_ok: " + currentAddCost[0]);
            if(currentAddCost[0] > 0) {
                startAddCostUpdate(
                        uid,
                        String.valueOf(currentAddCost[0]),
                        cost
                );
            }

            dismiss();
        });

    }
    private void startAddCostUpdate(
            String uid,
            String addCost,
            String cost
    ) {

        String  baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

        if ("nal_payment".equals(pay_method)) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl) // Замените BASE_URL на ваш базовый URL сервера
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);

            Call<Status> call = apiService.startAddCostBottomUpdate(
                    uid,
                    addCost
            );
            String url = call.request().url().toString();
            Logger.d(context, TAG, "URL запроса nal_payment: " + url);
            FinishSeparateFragment.text_status.setText(R.string.recounting_order);

            call.enqueue(new Callback<Status>() {
                @Override
                public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                    if (response.isSuccessful()) {
                        Status status = response.body();
                        assert status != null;
                        String responseStatus = status.getResponse();
                        Logger.d(context, TAG, "startAddCostUpdate  nal_payment: " + responseStatus);
                        if(responseStatus.equals("200")) {

                            Pattern pattern = Pattern.compile("(\\d+)");
                            Matcher matcher = pattern.matcher(FinishSeparateFragment.textCostMessage.getText().toString());

                            if (matcher.find()) {
                                // Преобразуем найденное число в целое, добавляем 20
                                int originalNumber = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
                                int updatedNumber = originalNumber + Integer.parseInt(addCost);

                                // Заменяем старое значение на новое
                                String updatedCost = matcher.replaceFirst(String.valueOf(updatedNumber));
                                FinishSeparateFragment.textCost.setVisibility(View.VISIBLE);
                                FinishSeparateFragment.textCostMessage.setVisibility(View.VISIBLE);
                                FinishSeparateFragment.carProgressBar.setVisibility(View.VISIBLE);
//                                FinishSeparateFragment.progressBar.setVisibility(View.VISIBLE);
                                FinishSeparateFragment.progressSteps.setVisibility(View.VISIBLE);
//                                FinishSeparateFragment.progressBar.setVisibility(View.GONE);

                                FinishSeparateFragment.btn_options.setVisibility(View.VISIBLE);
                                FinishSeparateFragment.btn_open.setVisibility(View.VISIBLE);

                                FinishSeparateFragment.textCostMessage.setText(updatedCost);
                                String message =  context.getString(R.string.ex_st_0);
                                FinishSeparateFragment.text_status.setText(message);
                                Log.d("UpdatedCost", "Обновленная строка: " + updatedCost);
                            } else {
                                Log.e("UpdatedCost", "Число не найдено в строке: " + cost);
                            }
                        } else {
                            // Обработка неуспешного ответа
                            FinishSeparateFragment.text_status.setText(R.string.verify_internet);
                        }

                    } else {
                        // Обработка неуспешного ответа
                        FinishSeparateFragment.text_status.setText(R.string.verify_internet);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                    // Обработайте ошибку при выполнении запроса

                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });
        }
        if ("wfp_payment".equals(pay_method)) {
            startAddCostCardUpdate(addCost);
        }
    }

    private void startAddCostCardUpdate(String addCost) {
        Logger.d(context, TAG, "startAddCostCardUpdate: ");
        String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS);
        Logger.d(context, TAG, "payWfp: rectoken " + rectoken);

        FinishSeparateFragment.text_status.setText(R.string.recounting_order);

        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);

        wfpInvoice(MainActivity.order_id , addCost, uid);

        if (rectoken.isEmpty()) {
            getUrlToPaymentWfp(addCost, MainActivity.order_id );
            getStatusWfp(MainActivity.order_id, addCost);
        } else {
            paymentByTokenWfp(FinishSeparateFragment.messageFondy, addCost, MainActivity.order_id );
        }

    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        FinishSeparateFragment.handlerStatus.postDelayed(FinishSeparateFragment.myTaskStatus, 20000);
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
                Logger.d(context, TAG, "Found rectoken with rectoken_check = 1" + ": " + result);
                return result;
            } while (cursor.moveToNext());
        }
        cursor.close();

        database.close();

        logTableContent(table);

        return result;
    }
    private void logTableContent(String table) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String[] columns = {"rectoken_check", "merchant", "rectoken"}; // Укажите все необходимые поля
        String selection = null;
        String[] selectionArgs = null;

        Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String rectokenCheck = cursor.getString(cursor.getColumnIndex("rectoken_check"));
                @SuppressLint("Range") String merchant = cursor.getString(cursor.getColumnIndex("merchant"));
                @SuppressLint("Range") String rectoken = cursor.getString(cursor.getColumnIndex("rectoken"));

                Logger.d(context, TAG, "rectoken_check: " + rectokenCheck + ", merchant: " + merchant + ", rectoken: " + rectoken);
            } while (cursor.moveToNext());
        }
        cursor.close();

        database.close();
    }


    private void wfpInvoice(String orderId, String amount, String uid) {
        String  baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);
        Call<Void> call = apiService.wfpInvoice(orderId, amount, uid);

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
    private void newOrderCardPayAdd20 (
            String addCost
    ) {

        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher( FinishSeparateFragment.textCostMessage.getText().toString());

        if (matcher.find()) {
            // Преобразуем найденное число в целое, добавляем 20
            int originalNumber = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
            int updatedNumber = originalNumber + Integer.parseInt(addCost);

            // Заменяем старое значение на новое
            String updatedCost = matcher.replaceFirst(String.valueOf(updatedNumber));
            FinishSeparateFragment.textCost.setVisibility(View.VISIBLE);
            FinishSeparateFragment.textCostMessage.setVisibility(View.VISIBLE);
            FinishSeparateFragment.carProgressBar.setVisibility(View.VISIBLE);

            FinishSeparateFragment.progressSteps.setVisibility(View.VISIBLE);


            FinishSeparateFragment.btn_options.setVisibility(View.VISIBLE);
            FinishSeparateFragment.btn_open.setVisibility(View.VISIBLE);



            FinishSeparateFragment.textCostMessage.setText(updatedCost);
            String message =  context.getString(R.string.ex_st_0);
            FinishSeparateFragment.text_status.setText(message);
            Log.d("UpdatedCost", "Обновленная строка: " + updatedCost);

        } else {
            Log.e("UpdatedCost", "Число не найдено в строке: " + cost);
        }

        FinishSeparateFragment.handlerStatus.removeCallbacks(FinishSeparateFragment.myTaskStatus);
        String  baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city = stringList.get(1);
        if( MainActivity.order_id == null) {
            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
        }
        Call<Status> call = apiService.startAddCostCardBottomUpdate(
                uid,
                uid_Double,
                pay_method,
                MainActivity.order_id,
                city,
                addCost

        );
        String url = call.request().url().toString();
        Logger.d(context, TAG, "URL запроса wfp_payment: " + url);


        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful()) {
                    Status status = response.body();
                    assert status != null;
                    String responseStatus = status.getResponse();
                    Logger.d(context, TAG, "startAddCostUpdate wfp_payment status: " + responseStatus);
                    if(!responseStatus.equals("200")) {
                        // Обработка неуспешного ответа
                        FinishSeparateFragment.text_status.setText(R.string.verify_internet);
                    }
                } else {
                    // Обработка неуспешного ответа
                    FinishSeparateFragment.text_status.setText(R.string.verify_internet);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
        FinishSeparateFragment.handlerStatus.post(FinishSeparateFragment.myTaskStatus);

    }

    private void getUrlToPaymentWfp(String amount, String order_id) {
        String  baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

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

        InvoiceService service = retrofit.create(InvoiceService.class);
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city = stringList.get(1);

        stringList = logCursor(MainActivity.TABLE_USER_INFO);
        String userEmail = stringList.get(3);
        String phone_number = stringList.get(2);

        Call<InvoiceResponse> call = service.createInvoice(
                context.getString(R.string.application),
                city,
                order_id,
                Integer.parseInt(amount),
                LocaleHelper.getLocale(),
                FinishSeparateFragment.messageFondy,
                userEmail,
                phone_number
        );

        call.enqueue(new Callback<InvoiceResponse>() {
            @Override
            public void onResponse(@NonNull Call<InvoiceResponse> call, @NonNull Response<InvoiceResponse> response) {
                Logger.d(context, TAG, "onResponse: 1111" + response.code());

                if (response.isSuccessful()) {
                    InvoiceResponse invoiceResponse = response.body();

                    if (invoiceResponse != null) {
                        String checkoutUrl = invoiceResponse.getInvoiceUrl();
                        Logger.d(context, TAG, "onResponse: Invoice URL: " + checkoutUrl);
                        if(checkoutUrl != null) {

                            MyBottomSheetCardPayment bottomSheetDialogFragment = new MyBottomSheetCardPayment(
                                    checkoutUrl,
                                    amount,
                                    uid,
                                    uid_Double,
                                    context,
                                    order_id
                            );
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                        } else {
                            Logger.d(context, TAG,"Response body is null");
                            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                            callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                            MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        }
                    } else {
                        Logger.d(context, TAG,"Response body is null");
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
                } else {
                    Logger.d(context, TAG, "Request failed: " + response.code());
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<InvoiceResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Request failed: " + t.getMessage());
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });


    }

    public static void callOrderIdMemory(String orderId, String uid, String paySystem) {
        String  baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
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

    private void paymentByTokenWfp(
            String orderDescription,
            String amount,
            String order_id
    ) {
        String  baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

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

        PurchaseService service = retrofit.create(PurchaseService.class);
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city = stringList.get(1);

        stringList = logCursor(MainActivity.TABLE_USER_INFO);
        String email = stringList.get(3);
        String phoneNumber = stringList.get(2);

        Call<PurchaseResponse> call = service.purchase(
                context.getString(R.string.application),
                city,
                order_id,
                amount,
                orderDescription,
                email,
                phoneNumber
        );
        call.enqueue(new Callback<PurchaseResponse>() {
            @Override
            public void onResponse(@NonNull Call<PurchaseResponse> call, @NonNull Response<PurchaseResponse> response) {
                if (response.isSuccessful()) {
                    PurchaseResponse purchaseResponse = response.body();
                    if (purchaseResponse != null) {
                        // Обработка ответа
                        Logger.d(context, TAG, "onResponse:purchaseResponse " + purchaseResponse.toString());

                        String orderStatus = purchaseResponse.getTransactionStatus();

                        Logger.d(context, TAG, "Transaction Status: " + orderStatus);

                        switch (orderStatus) {
                            case "Approved":
                            case "WaitingAuthComplete":
                                sharedPreferencesHelperMain.saveValue("pay_error", "**");
                                newOrderCardPayAdd20(amount);
                                break;
                            default:
                                sharedPreferencesHelperMain.saveValue("pay_error", "pay_error");
                                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                                callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                                MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                        }
//                        getStatusWfp(order_id, amount);
                    } else {
                        // Ошибка при парсинге ответа
                        Logger.d(context, TAG, "Ошибка при парсинге ответа");
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        callOrderIdMemory(order_id, uid, pay_method);
                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
                } else {
                    // Ошибка запроса
                    Logger.d(context, TAG, "Ошибка запроса");
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PurchaseResponse> call, @NonNull Throwable t) {
                // Ошибка при выполнении запроса
                Logger.d(context, TAG, "Ошибка при выполнении запроса");
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });

    }

    private void getStatusWfp(
            String orderReferens,
            String amount
    ) {
        Logger.d(context, TAG, "getStatusWfp: ");

        String  baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";


        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city = stringList.get(1);

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

        StatusService service = retrofit.create(StatusService.class);

        Call<StatusResponse> call = service.checkStatus(
                context.getString(R.string.application),
                city,
                orderReferens
        );

        call.enqueue(new Callback<StatusResponse>() {
            @Override
            public void onResponse(@NonNull Call<StatusResponse> call, @NonNull Response<StatusResponse> response) {

                if (response.isSuccessful()) {
                    StatusResponse statusResponse = response.body();
                    assert statusResponse != null;
                    String orderStatus = statusResponse.getTransactionStatus();
                    Logger.d(context, TAG, "Transaction Status: " + orderStatus);

                    switch (orderStatus) {
                        case "Approved":
                        case "WaitingAuthComplete":
                            sharedPreferencesHelperMain.saveValue("pay_error", "**");
                            newOrderCardPayAdd20(amount);
                            break;
                        default:
                            sharedPreferencesHelperMain.saveValue("pay_error", "pay_error");
                            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                            callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                            MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<StatusResponse> call, @NonNull Throwable t) {
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });

    }

    @SuppressLint("Range")
    private List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
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
        assert c != null;
        c.close();
        return list;
    }
}
