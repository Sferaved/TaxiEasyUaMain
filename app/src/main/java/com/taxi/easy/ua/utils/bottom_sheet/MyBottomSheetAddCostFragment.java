package com.taxi.easy.ua.utils.bottom_sheet;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.finish.ApiService;
import com.taxi.easy.ua.ui.finish.Status;
import com.taxi.easy.ua.ui.finish.fragm.FinishSeparateFragment;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseResponse;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseService;
import com.taxi.easy.ua.utils.helpers.WfpGooglePayHelper;
import com.taxi.easy.ua.utils.hold.APIHoldService;
import com.taxi.easy.ua.utils.hold.HoldResponse;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.utils.payment.GooglePayOrderHelper;
import com.taxi.easy.ua.utils.payment.PaymentTypeHelper;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.uxcam.UXCam;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.taxi.easy.ua.utils.db.CursorReadHelper;


public class MyBottomSheetAddCostFragment extends BottomSheetDialogFragment {

    private static final String TAG = "MyBottomSheetAddCostFragment";
    private static final int ADD_COST_WFP_TIMEOUT_SEC = 90;
    TextView textViewCost;
    AppCompatButton btn_ok, btn_minus, btn_plus;
    String cost, uid, uid_Double, pay_method;
    Context context;

    private ExecutionStatusViewModel viewModel;

    private PaymentsClient googlePayPaymentsClient;
    private ActivityResultLauncher<IntentSenderRequest> googlePayLauncher;
    @Nullable
    private String pendingGooglePayAddCostOrderRef;

    public MyBottomSheetAddCostFragment(
            String cost,
            String uid,
            String uid_Double,
            String pay_method,
            ExecutionStatusViewModel viewModel
    ) {
        this.cost = cost;
        this.uid = uid;
        this.uid_Double = uid_Double;
        this.pay_method = pay_method;
        this.viewModel = viewModel;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        googlePayPaymentsClient = WfpGooglePayHelper.createPaymentsClient(this);
        googlePayLauncher = registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(),
                result -> WfpGooglePayHelper.handlePaymentResult(
                        result.getResultCode(),
                        result.getData(),
                        requireContext(),
                        new WfpGooglePayHelper.PaymentResultCallback() {
                            @Override
                            public void onSuccess(@NonNull String paymentDataJson) {
                                submitGooglePayAddCostCharge(paymentDataJson);
                            }

                            @Override
                            public void onCancelled() {
                                onGooglePayAddCostCancelled();
                            }

                            @Override
                            public void onError(@NonNull String message) {
                                onGooglePayAddCostFailed(message);
                            }
                        }
                )
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        View view = inflater.inflate(R.layout.add_cost_bottom_layout, container, false);

        context = requireContext();

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
            if (ExecutionStatusViewModel.shouldBlockAddCost(uid)) {
                Logger.d(getActivity(), TAG, "btn_ok blocked for uid=" + uid);
                Toast.makeText(context, R.string.error_cancelling_order, Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }
            if (PaymentTypeHelper.usesWalletHold(pay_method)
                    && ExecutionStatusViewModel.isAddCostInFlightPref()) {
                Logger.d(getActivity(), TAG, "btn_ok blocked: add-cost in flight");
                Toast.makeText(context, R.string.recounting_order, Toast.LENGTH_LONG).show();
                dismiss();
                return;
            }
            if (currentAddCost[0] > 0) {
                // Проверка на null
                if (viewModel == null) {
                    Logger.e(getActivity(), TAG, "viewModel is null in btn_ok");
                    return;
                }
                viewModel.setCancelStatus(false);
                startAddCostUpdate(
                        uid,
                        String.valueOf(currentAddCost[0])
                );
            }
            dismiss();
        });

    }
    private void startAddCostUpdate(
            String uid,
            String addCost
    ) {

        String  baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

        if ("nal_payment".equals(pay_method)) {
            viewModel.setCancelStatus(false);
            startAddCostWithUpdate(uid, addCost, baseUrl);
        }
        if ("wfp_payment".equals(pay_method)) {
            viewModel.setCancelStatus(false);
            startAddCostCardUpdate(addCost);
        }
        if (PaymentTypeHelper.isGooglePay(pay_method)) {
            viewModel.setCancelStatus(false);
            startAddCostGooglePayUpdate(addCost);
        }
    }
    public void startAddCostWithUpdate(String uid, String addCost, String baseUrl) {
            if (ExecutionStatusViewModel.shouldBlockAddCost(uid)) {
                Logger.d(context, TAG, "startAddCostWithUpdate skipped: cancel for uid=" + uid);
                return;
            }

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl) // Замените BASE_URL на ваш базовый URL сервера
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);

            Call<Status> call = apiService.startAddCostWithAddBottomUpdate(uid, addCost);
            String url = call.request().url().toString();
            Logger.d(context, TAG, "URL запроса nal_payment: " + url);

            // Выполняем асинхронный запрос
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                    if (ExecutionStatusViewModel.shouldBlockAddCost(uid)) {
                        Logger.d(context, TAG, "startAddCost response ignored: order canceled uid=" + uid);
                        return;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        String responseStatus = response.body().getResponse();
                        Logger.d(context, TAG, "startAddCostUpdate nal_payment: " + responseStatus);
                        if (isNalAddCostError(responseStatus)) {
                            if (isAdded()) {
                                showNalAddCostError(responseStatus);
                            }
                            viewModel.setCancelStatus(true);
                            return;
                        }
                        viewModel.setAddCostViewUpdate(addCost);
                        viewModel.setCancelStatus(true);
                        return;
                    }
                    if (isAdded()) {
                        showNalAddCostError(null);
                    }
                    viewModel.setCancelStatus(true);
                }

                @Override
                public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                    Logger.e(context, TAG, "startAddCostWithUpdate failed: " + t.getMessage());
                    if (isAdded()) {
                        Toast.makeText(context, R.string.network_no_internet, Toast.LENGTH_LONG).show();
                    }
                    viewModel.setCancelStatus(true);
                }
            });

    }

    private static boolean isNalAddCostError(@Nullable String response) {
        if (response == null || response.trim().isEmpty()) {
            return false;
        }
        String lower = response.toLowerCase();
        return lower.contains("дублир")
                || lower.contains("дублюван")
                || lower.contains("не можете")
                || lower.contains("не можете")
                || lower.contains("cannot");
    }

    private void showNalAddCostError(@Nullable String serverMessage) {
        if (!isAdded()) {
            return;
        }
        String text = serverMessage != null && !serverMessage.trim().isEmpty()
                ? serverMessage
                : getString(R.string.double_order_error);
        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
    }

    private void startAddCostCardUpdate(String addCost) {
        if (ExecutionStatusViewModel.isAddCostInFlightPref()) {
            Logger.d(context, TAG, "startAddCostCardUpdate skipped: add-cost in flight");
            Toast.makeText(context, R.string.recounting_order, Toast.LENGTH_LONG).show();
            return;
        }
        Logger.d(context, TAG, "startAddCostCardUpdate: ");
        String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS);
        Logger.d(context, TAG, "payWfp: rectoken " + rectoken);

        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);

        wfpInvoice(MainActivity.order_id , addCost, uid);
        String messageFondy = context.getString(R.string.fondy_message);
        if (!rectoken.isEmpty()) {
            paymentByTokenWfp(messageFondy, addCost, MainActivity.order_id );
        }

    }

    private void startAddCostGooglePayUpdate(String addCost) {
        if (ExecutionStatusViewModel.isAddCostInFlightPref()) {
            Logger.d(context, TAG, "startAddCostGooglePayUpdate skipped: add-cost in flight");
            Toast.makeText(context, R.string.recounting_order, Toast.LENGTH_LONG).show();
            return;
        }
        int amountUah = GooglePayOrderHelper.parseAmountUah(addCost);
        if (amountUah <= 0) {
            Toast.makeText(context, R.string.cost_error, Toast.LENGTH_SHORT).show();
            return;
        }

        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
        pendingGooglePayAddCostOrderRef = MainActivity.order_id;
        wfpInvoice(MainActivity.order_id, addCost, uid);

        ExecutionStatusViewModel.setPendingAddCostAmountPref(addCost);
        ExecutionStatusViewModel.setPendingAddCostOrderRefPref(MainActivity.order_id);
        ExecutionStatusViewModel.setAddCostInFlightPref(true);
        viewModel.setCancelStatus(false);

        WfpGooglePayHelper.checkReady(googlePayPaymentsClient, ready -> {
            if (!isAdded()) {
                return;
            }
            if (!ready) {
                onGooglePayAddCostFailed(getString(R.string.google_pay_unavailable));
                return;
            }
            List<String> cityInfo = logCursor(MainActivity.CITY_INFO);
            String city = cityInfo.size() > 1 ? cityInfo.get(1) : "";
            String baseUrl = (String) sharedPreferencesHelperMain.getValue(
                    "baseUrl", "https://m.easy-order-taxi.site");
            GooglePayOrderHelper.fetchMerchantConfig(
                    baseUrl,
                    context.getString(R.string.application),
                    city,
                    new GooglePayOrderHelper.ConfigCallback() {
                        @Override
                        public void onSuccess(@NonNull String merchantAccount) {
                            if (!isAdded()) {
                                return;
                            }
                            WfpGooglePayHelper.requestPayment(
                                    MyBottomSheetAddCostFragment.this,
                                    googlePayPaymentsClient,
                                    merchantAccount,
                                    String.valueOf(amountUah),
                                    googlePayLauncher,
                                    new WfpGooglePayHelper.PaymentResultCallback() {
                                        @Override
                                        public void onSuccess(@NonNull String paymentDataJson) {
                                            submitGooglePayAddCostCharge(paymentDataJson);
                                        }

                                        @Override
                                        public void onCancelled() {
                                            onGooglePayAddCostCancelled();
                                        }

                                        @Override
                                        public void onError(@NonNull String message) {
                                            onGooglePayAddCostFailed(message);
                                        }
                                    }
                            );
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            onGooglePayAddCostFailed(message);
                        }
                    }
            );
        });
    }

    private void submitGooglePayAddCostCharge(@NonNull String paymentDataJson) {
        if (!isAdded() || pendingGooglePayAddCostOrderRef == null) {
            return;
        }
        String pendingAmount = ExecutionStatusViewModel.getPendingAddCostAmountPref();
        int amountUah = GooglePayOrderHelper.parseAmountUah(
                pendingAmount != null ? pendingAmount : "0");
        if (amountUah <= 0) {
            onGooglePayAddCostFailed("invalid_amount");
            return;
        }
        List<String> cityInfo = logCursor(MainActivity.CITY_INFO);
        String city = cityInfo.size() > 1 ? cityInfo.get(1) : "";
        List<String> userInfo = logCursor(MainActivity.TABLE_USER_INFO);
        String email = userInfo.size() > 3 ? userInfo.get(3) : "";
        String phone = userInfo.size() > 2 ? userInfo.get(2) : "";
        String baseUrl = (String) sharedPreferencesHelperMain.getValue(
                "baseUrl", "https://m.easy-order-taxi.site");

        GooglePayOrderHelper.submitHoldCharge(
                context,
                baseUrl,
                context.getString(R.string.application),
                city,
                pendingGooglePayAddCostOrderRef,
                amountUah,
                email,
                phone,
                paymentDataJson,
                new GooglePayOrderHelper.ChargeCallback() {
                    @Override
                    public void onHoldSuccess(@NonNull String orderReference) {
                        if (!isAdded()) {
                            return;
                        }
                        MainActivity.order_id = orderReference;
                        Logger.d(context, TAG, "Google Pay add-cost hold ok: " + orderReference);
                        viewModel.setCancelStatus(false);
                        Toast.makeText(context, R.string.add_cost_processing, Toast.LENGTH_LONG).show();
                        enableCancelButtonIfAddCostNotInFlight();
                    }

                    @Override
                    public void onHoldFailed(@NonNull String message) {
                        onGooglePayAddCostFailed(message);
                    }
                }
        );
    }

    private void onGooglePayAddCostCancelled() {
        Logger.d(context, TAG, "Google Pay add-cost cancelled");
        clearGooglePayAddCostInFlight(true);
        Toast.makeText(context, R.string.e_google_pay_canceled, Toast.LENGTH_SHORT).show();
    }

    private void onGooglePayAddCostFailed(@Nullable String message) {
        Logger.w(context, TAG, "Google Pay add-cost failed: " + message);
        String orderRef = pendingGooglePayAddCostOrderRef;
        clearGooglePayAddCostInFlight(true);
        if (orderRef != null) {
            deleteInvoice(orderRef);
        }
        if (isAdded()) {
            Toast.makeText(context, R.string.add_cost_payment_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void clearGooglePayAddCostInFlight(boolean enableCancel) {
        ExecutionStatusViewModel.setAddCostInFlightPref(false);
        ExecutionStatusViewModel.clearPendingAddCostAmountPref();
        pendingGooglePayAddCostOrderRef = null;
        if (viewModel != null && enableCancel) {
            viewModel.setCancelStatus(true);
        }
        enableCancelButtonIfAddCostNotInFlight();
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
                result = CursorReadHelper.getString(cursor, "rectoken");
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
                @SuppressLint("Range") String rectokenCheck = CursorReadHelper.getString(cursor, "rectoken_check");
                @SuppressLint("Range") String merchant = CursorReadHelper.getString(cursor, "merchant");
                @SuppressLint("Range") String rectoken = CursorReadHelper.getString(cursor, "rectoken");

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

        call.enqueue(new Callback<>() {
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
                // Доплата — одна попытка без RetryInterceptor, чтобы не дублировать CHARGE на сервере.
                .addInterceptor(interceptor)
                .connectTimeout(ADD_COST_WFP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .readTimeout(ADD_COST_WFP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .writeTimeout(ADD_COST_WFP_TIMEOUT_SEC, TimeUnit.SECONDS)
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

        ExecutionStatusViewModel.setPendingAddCostAmountPref(amount);
        ExecutionStatusViewModel.setPendingAddCostOrderRefPref(order_id);
        ExecutionStatusViewModel.setAddCostInFlightPref(true);

        Call<PurchaseResponse> call = service.chargeActiveTokenAddCost(
                context.getString(R.string.application),
                city,
                order_id,
                amount,
                orderDescription,
                email,
                phoneNumber
        );
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<PurchaseResponse> call, @NonNull Response<PurchaseResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PurchaseResponse statusResponse = response.body();

                    String orderStatus = statusResponse.getTransactionStatus();
                    Logger.d(context, TAG, "1 Transaction Status: " + orderStatus);


                    switch (orderStatus) {
                        case "Approved":
                        case "WaitingAuthComplete":
                            // WFP: держим in-flight до finishAbsoluteCost / order_uid_new
                            if (!PaymentTypeHelper.usesWalletHold(pay_method)) {
                                ExecutionStatusViewModel.setAddCostInFlightPref(false);
                                ExecutionStatusViewModel.clearPendingAddCostAmountPref();
                                viewModel.setAddCostViewUpdate(amount);
                            }
                            viewModel.setCancelStatus(true);
                            break;
                        case "InProcessing":
                        case "Pending":
                            Logger.d(context, TAG, "Add-cost in progress: " + orderStatus);
                            viewModel.setCancelStatus(false);
                            Toast.makeText(context, R.string.add_cost_processing, Toast.LENGTH_LONG).show();
                            break;
                        default:
                            ExecutionStatusViewModel.setAddCostInFlightPref(false);
                            ExecutionStatusViewModel.clearPendingAddCostAmountPref();
                            deleteInvoice(order_id);
                            Toast.makeText(context, context.getString(R.string.pay_failure_mes), Toast.LENGTH_SHORT).show();
                            Logger.d(context, TAG, "onResponse: Unexpected status: " + orderStatus);
                            viewModel.setCancelStatus(true);
                    }


                } else {
                    Logger.w(context, TAG, "onResponse unsuccessful code="
                            + response.code() + ", awaiting push/status");
                    viewModel.setCancelStatus(false);
                    Toast.makeText(context, R.string.add_cost_processing, Toast.LENGTH_LONG).show();
                }
                enableCancelButtonIfAddCostNotInFlight();
            }

            @Override
            public void onFailure(@NonNull Call<PurchaseResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.w(context, TAG, "Add-cost HTTP failed, awaiting push/status: " + t.getMessage());
                viewModel.setCancelStatus(false);
                Toast.makeText(context, R.string.add_cost_processing_slow, Toast.LENGTH_LONG).show();
            }
        });

    }
    private void enableCancelButtonIfAddCostNotInFlight() {
        if (ExecutionStatusViewModel.isAddCostInFlightPref()) {
            return;
        }
        if (FinishSeparateFragment.btn_cancel_order != null) {
            FinishSeparateFragment.btn_cancel_order.setVisibility(VISIBLE);
            FinishSeparateFragment.btn_cancel_order.setEnabled(true);
            FinishSeparateFragment.btn_cancel_order.setClickable(true);
            Logger.d(context, "Pusher eventTransactionStatus", "Cancel button enabled successfully");
        }
    }

    private void deleteInvoice(String orderReference) {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Создание клиента OkHttpClient с подключенным логгером
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
                .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
                .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
                .build();
        String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient) // Подключение клиента OkHttpClient с логгером
                .build();


        APIHoldService apiService = retrofit.create(APIHoldService.class);
        Call<HoldResponse> call = apiService.deleteInvoice(orderReference);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<HoldResponse> call, @NonNull Response<HoldResponse> response) {

            }

            @Override
            public void onFailure(@NonNull Call<HoldResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
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
                    str = str.concat(cn + " = " + CursorReadHelper.getString(c, cn) + "; ");
                    list.add(CursorReadHelper.getString(c, cn));

                }

            } while (c.moveToNext());
        }
        database.close();
        c.close();
        return list;
    }

    private OnDismissListener dismissListener;

    public interface OnDismissListener {
        void onDismiss();
    }

    public void setOnDismissListener(OnDismissListener listener) {
        this.dismissListener = listener;
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null) {
            dismissListener.onDismiss(); // Уведомляем о закрытии
        }
    }
}
