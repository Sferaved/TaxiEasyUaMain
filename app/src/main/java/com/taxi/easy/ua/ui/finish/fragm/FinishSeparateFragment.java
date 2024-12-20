package com.taxi.easy.ua.ui.finish.fragm;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavOptions;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentFinishSeparateBinding;
import com.taxi.easy.ua.ui.card.MyBottomSheetCardPayment;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.ApiService;
import com.taxi.easy.ua.ui.finish.BonusResponse;
import com.taxi.easy.ua.ui.finish.OrderResponse;
import com.taxi.easy.ua.ui.finish.Status;
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
import com.taxi.easy.ua.ui.mono.MonoApi;
import com.taxi.easy.ua.ui.mono.payment.RequestPayMono;
import com.taxi.easy.ua.ui.mono.payment.ResponsePayMono;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusResponse;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusService;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceResponse;
import com.taxi.easy.ua.ui.wfp.invoice.InvoiceService;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseResponse;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseService;
import com.taxi.easy.ua.ui.wfp.revers.ReversResponse;
import com.taxi.easy.ua.ui.wfp.revers.ReversService;
import com.taxi.easy.ua.utils.LocaleHelper;
import com.taxi.easy.ua.utils.animation.car.CarProgressBar;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetAddCostFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorPaymentFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetFinishOptionFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.log.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class FinishSeparateFragment extends Fragment {

    private static final String TAG = "FinishSeparateFragment";

    private FragmentFinishSeparateBinding binding;
    Activity context;
    FragmentManager fragmentManager;
    View root;
    @SuppressLint("StaticFieldLeak")
    public static TextView text_status;


    public static String baseUrl;
    Map<String, String> receivedMap;
    public static String uid;
    Thread thread;
    static String pay_method;

    public static String amount;
    @SuppressLint("StaticFieldLeak")
    public static TextView text_full_message, textCost, textCostMessage, textCarMessage, textStatus, textStatusCar;
    String messageResult;
    String messageResultCost;
    public static String messageFondy;
    public static String uid_Double;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btn_reset_status;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btn_cancel_order;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btn_again;

    public static Runnable myRunnable;
    public static Runnable runnableBonusBtn;
    public static Handler handler, handlerBonusBtn,  handlerStatus;
    public static Runnable myTaskStatus;

    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    @SuppressLint("StaticFieldLeak")
    public static  String email;
    @SuppressLint("StaticFieldLeak")
    public static  String phoneNumber;
    private boolean cancel_btn_click = false;
    long delayMillisStatus;
    private static boolean no_pay;
    private static boolean canceled;
    public static  CarProgressBar carProgressBar;
    // Получаем доступ к кружочкам
    View step1;
    View step2;
    View step3;
    View step4;
    private TextView countdownTextView;
    private long timeToStartMillis;
    public static LinearLayout progressSteps;
    private Animation blinkAnimation;
    public static AppCompatButton btn_open;
    public static AppCompatButton btn_options;

    public static String flexible_tariff_name;
    public static String comment_info;
    public static  String extra_charge_codes;

    private Handler handlerAddcost;
    private Runnable showDialogAddcost;

    private  int timeCheckOutAddCost;
    boolean need_20_add;
    String required_time;
    boolean isTenMinutesRemaining;
    boolean isTenMinutesRemainingBlock;
    Handler handlerCheckTask;
    Runnable checkTask;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentFinishSeparateBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        context = requireActivity();

        fragmentManager = getParentFragmentManager();

        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        progressBar = root.findViewById(R.id.progress_bar);

        baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        // Получаем доступ к кружочкам
        progressSteps = root.findViewById(R.id.progressSteps);
        step1 = root.findViewById(R.id.step1);
        step2 = root.findViewById(R.id.step2);
        step3 = root.findViewById(R.id.step3);
        step4 = root.findViewById(R.id.step4);
        updateProgress(2);
        blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink_animation);

        countdownTextView = root.findViewById(R.id.countdownTextView);
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(4);
        if(pay_method.equals("nal_payment")) {
            timeCheckOutAddCost = 60*1000;
        } else  {
            timeCheckOutAddCost =  75*1000;
        }

        Logger.d(context, TAG, "pay_method " + pay_method);

        AppCompatButton btnCallAdmin = root.findViewById(R.id.btnCallAdmin);
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            List<String> stringList = logCursor(MainActivity.CITY_INFO);
            String phone = stringList.get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });
        messageFondy =  context.getString(R.string.fondy_message);
        email = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        phoneNumber = logCursor(MainActivity.TABLE_USER_INFO).get(2);

        Bundle arguments = getArguments();
        assert arguments != null;
        messageResult = arguments.getString("messageResult_key");


        String no_pay_key = arguments.getString("card_payment_key");
        no_pay = no_pay_key != null && no_pay_key.equals("no");

        receivedMap = (HashMap<String, String>) arguments.getSerializable("sendUrlMap");

        assert receivedMap != null;



        flexible_tariff_name = receivedMap.get("flexible_tariff_name");

        required_time = receivedMap.get("required_time");
        Logger.d(context, TAG, "required_time: " + required_time);

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");



        if (required_time == null || required_time.isEmpty()) {
            need_20_add = true;
        } else {
            // Регулярное выражение для проверки формата даты "dd.MM.yyyy HH:mm"
            String dateTimePattern = "\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}";
            Pattern pattern = Pattern.compile(dateTimePattern);
            Matcher matcher = pattern.matcher(required_time);

            // Извлекаем только дату и время, если они найдены
            if (matcher.find()) {
                required_time = matcher.group(); // Оставляем только дату и время
            } else {
                required_time = ""; // Если формат не найден, установим пустую строку
            }

            handlerCheckTask = new Handler(Looper.getMainLooper());
            checkTask = new Runnable() {
                @Override
                public void run() {
                    try {
                        Logger.e(context, TAG, "required_time " + required_time);
                        if (!required_time.isEmpty()) {
                            Date requiredDate = dateFormat.parse(required_time);
                            Date currentDate = new Date();

                            if (requiredDate != null) {
                                long timeDifferenceMillis = requiredDate.getTime() - currentDate.getTime();
                                long timeDifferenceMinutes = timeDifferenceMillis / (60 * 1000);

                                Logger.e(context, TAG, "timeDifferenceMinutes 22 " + timeDifferenceMinutes);
                                // Проверяем, если разница в пределах 30 минут
                                if (timeDifferenceMinutes > 0 && timeDifferenceMinutes <= 30) {
                                    need_20_add = true; // Время ещё не наступило и в пределах 30 минут
                                } else if (timeDifferenceMinutes > 30){
                                    need_20_add = false; // Время либо наступило, либо за пределами 30 минут
                                }
                            } else {
                                need_20_add = true; // requiredDate null, устанавливаем true
                            }
                        } else {
                            need_20_add = true; // Формат даты некорректен
                        }
                        if(need_20_add) {
                            if (handlerStatus == null) {
                                handlerAddcost.postDelayed(showDialogAddcost, timeCheckOutAddCost);
                            }
                        }
                                                Logger.e(context, TAG, "need_20_add 22 " + need_20_add);
                    } catch (ParseException e) {
                        need_20_add = true; // Если произошла ошибка разбора, установить need_20_add в true
                        Logger.e(context, TAG, "Ошибка разбора даты: " + e.getMessage());
                    }

                    // Повторяем задачу через минуту, если окно активно

                    handlerCheckTask.postDelayed(this, 60000); // 60000 миллисекунд = 1 минута

                }
            };

            // Запускаем задачу проверки
            handlerCheckTask.post(checkTask);


        }


        Logger.d(context, TAG, "need_20_add: " + need_20_add);


        comment_info = receivedMap.get("comment_info");
        extra_charge_codes = receivedMap.get("extra_charge_codes");

        Logger.d(context, TAG, "onCreate: receivedMap" + receivedMap.toString());
        text_full_message = root.findViewById(R.id.text_full_message);
        text_full_message.setText(messageResult);

        messageResultCost = arguments.getString("messagePay_key");
        textCost = root.findViewById(R.id.textCost);
        textCostMessage = root.findViewById(R.id.text_cost_message);
        Logger.d(context, TAG, "onCreate: textCostMessage" + messageResultCost);
        textCostMessage.setText(messageResultCost);

        textStatus = root.findViewById(R.id.textStatus);
        textStatusCar = root.findViewById(R.id.textStatusCar);
        textStatusCar.setVisibility(View.GONE);

        textCarMessage = root.findViewById(R.id.text_status_car);
        textCarMessage.setVisibility(View.GONE);

        uid = arguments.getString("UID_key");
        uid_Double = receivedMap.get("dispatching_order_uid_Double");



        text_status = root.findViewById(R.id.text_status);

        text_status.setText( context.getString(R.string.status_checkout_message));
        btn_reset_status = root.findViewById(R.id.btn_reset_status);

        try {
            statusOrderWithDifferentValue(uid);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        btn_cancel_order = root.findViewById(R.id.btn_cancel_order);
        long delayMillis = 5 * 60 * 1000;
        boolean black_list_yes = verifyOrder(requireContext());
        if (pay_method.equals("wfp_payment")) {
            amount = receivedMap.get("order_cost");
        }
        if (pay_method.equals("fondy_payment")) {
            amount = receivedMap.get("order_cost") + "00";
        }




//        btn_reset_status.setOnClickListener(v -> {
//
//
////            if(connected()){
////                try {
////                    statusOrderWithDifferentValue(uid);
////                } catch (ParseException e) {
////                    throw new RuntimeException(e);
////                }
////            } else {
////                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment( context.getString(R.string.verify_internet));
////                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
////            }
//
//        });

        if (pay_method.equals("bonus_payment") && !no_pay) {
            handlerBonusBtn = new Handler();
            fetchBonus();
        }

        handler = new Handler();

        if (pay_method.equals("bonus_payment") || pay_method.equals("wfp_payment") || pay_method.equals("fondy_payment") || pay_method.equals("mono_payment") ) {
            handlerBonusBtn = new Handler();

            runnableBonusBtn = () -> {
                MainActivity.order_id = null;
                String newStatus = text_status.getText().toString();
                if(!newStatus.contains( context.getString(R.string.time_out_text))
                        || !newStatus.contains( context.getString(R.string.error_payment_card))
                        || !newStatus.contains( context.getString(R.string.double_order_error))
                        || !newStatus.contains( context.getString(R.string.call_btn_cancel)) ) {
                    String cancelText = context.getString(R.string.status_checkout_message);
                    text_status.setText(cancelText);

                } else {
                    text_status.setText(newStatus);
                }

                carProgressBar.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);

            };
            handlerBonusBtn.postDelayed(runnableBonusBtn, delayMillis);
        }

        handlerStatus = new Handler();
        delayMillisStatus = 5 * 1000;
        myTaskStatus = new Runnable() {
            @Override
            public void run() {
                // Ваша логика
                try {
                    statusOrderWithDifferentValue(uid);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                // Запланировать повторное выполнение
                handlerStatus.postDelayed(this, delayMillisStatus);
            }
        };

        // Запускаем цикл
        startCycle();

        // Запланируйте выполнение задачи

        if (pay_method.equals("fondy_payment") || pay_method.equals("mono_payment")|| pay_method.equals("wfp_payment")) {

            myRunnable = () -> {
                MainActivity.order_id = null;
                String newStatus = text_status.getText().toString();
                if(!newStatus.contains( context.getString(R.string.time_out_text))
                        || !newStatus.contains( context.getString(R.string.error_payment_card))
                        || !newStatus.contains( context.getString(R.string.double_order_error))
                        || !newStatus.contains( context.getString(R.string.call_btn_cancel)) ) {
                    String cancelText = context.getString(R.string.status_checkout_message);
                    text_status.setText(cancelText);

                } else {
                    text_status.setText(newStatus);
                }
                
//                btn_cancel_order.setText( context.getString(R.string.help_button));
                progressBar.setVisibility(View.GONE);
                carProgressBar.setVisibility(View.GONE);

            };
            handler.postDelayed(myRunnable, delayMillis);
        }
        btn_cancel_order.setOnClickListener(v -> {
//            handler.removeCallbacks(myRunnable);

            if (handlerAddcost != null) {
                handlerAddcost.removeCallbacks(showDialogAddcost);
            }

            carProgressBar.setVisibility(View.GONE);
            cancel_btn_click = true;
            canceled = false;
            textCost.setVisibility(View.GONE);
            textCostMessage.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            progressSteps.setVisibility(View.GONE);

//            String message =  context.getString(R.string.ex_st_canceled);
//            text_status.setText(message);

            btn_options.setVisibility(View.GONE);
            btn_open.setVisibility(View.GONE);
            btn_reset_status.setVisibility(View.GONE);
            btn_cancel_order.setVisibility(View.GONE);
            btn_again.setVisibility(View.VISIBLE);

            String messageInfo = context.getString(R.string.finish_info);
            MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(messageInfo);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());


            if(connected()){
                if(!uid_Double.equals(" ")) {
                    cancelOrderDouble();

                } else{
                    try {
                        cancelOrder(uid);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }

                }
                if (thread != null && thread.isAlive()) {
                    thread.interrupt();
                }
            } else {
                progressBar.setVisibility(View.INVISIBLE);
                text_status.setText(R.string.verify_internet);

            }
        });

        btn_again = root.findViewById(R.id.btn_again);
        btn_again.setOnClickListener(v -> {
            MainActivity.order_id = null;
            updateAddCost(String.valueOf(0));
            if (handlerStatus != null) {
                handlerStatus.removeCallbacks(myTaskStatus);
            }
            if(connected()){
                MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
            } else {
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment( context.getString(R.string.verify_internet));
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });

        


        if(!no_pay) {
            switch (pay_method) {
                case "wfp_payment":
                    try {
                        payWfp();
                    } catch (UnsupportedEncodingException e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        throw new RuntimeException(e);
                    }
                    break;
                case "fondy_payment":
                    try {
                        payFondy();
                    } catch (UnsupportedEncodingException e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        throw new RuntimeException(e);
                    }
                    break;
                case "mono_payment":
                    String reference = MainActivity.order_id;
                    String comment =  context.getString(R.string.fondy_message);

                    getUrlToPaymentMono(amount, reference, comment);
                    break;

            }
        }
        ImageButton btn_no = root.findViewById(R.id.btn_no);
        btn_no.setOnClickListener(view -> startActivity(new Intent(context, MainActivity.class)));

        carProgressBar = root.findViewById(R.id.carProgressBar);

        // Запустить анимацию
        carProgressBar.resumeAnimation();

        btn_open = binding.btnOpen;

        btn_options = binding.btnOptions;

        btn_options.setOnClickListener(v -> {
            btnOptions();
        });

        return root;
    }

    private boolean verifyOrder(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(MainActivity.TABLE_USER_INFO).get(1).equals("0")) {
                verify = false;
                Log.d("TAG", "verifyOrder:verify " + verify);
            }
            cursor.close();
        }
        database.close();
        return verify;
    }

    private void btnOptions() {
        // Создаем Bundle для передачи данных
        Bundle bundle = new Bundle();
        bundle.putString("flexible_tariff_name", flexible_tariff_name);
        bundle.putString("comment_info", comment_info);
        bundle.putString("extra_charge_codes", extra_charge_codes);

// Создаем экземпляр MyBottomSheetFinishOptionFragment
        MyBottomSheetFinishOptionFragment bottomSheetDialogFragment = new MyBottomSheetFinishOptionFragment();

// Устанавливаем аргументы в фрагмент
        bottomSheetDialogFragment.setArguments(bundle);

// Показываем BottomSheet
        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

    }

    private void btnOpen() {

        if (btn_reset_status.getVisibility() == View.VISIBLE) {
            // Анимация исчезновения кнопок
            btn_reset_status.animate().alpha(0f).setDuration(300).withEndAction(() -> btn_reset_status.setVisibility(View.GONE));
            btn_cancel_order.animate().alpha(0f).setDuration(300).withEndAction(() -> btn_cancel_order.setVisibility(View.GONE));
            btn_again.animate().alpha(0f).setDuration(300).withEndAction(() -> btn_again.setVisibility(View.GONE));
        } else {
            // Анимация появления кнопок
            btn_reset_status.setVisibility(View.VISIBLE);
            btn_reset_status.setAlpha(0f);
            btn_reset_status.animate().alpha(1f).setDuration(300);

            btn_cancel_order.setVisibility(View.VISIBLE);
            btn_cancel_order.setAlpha(0f);
            btn_cancel_order.animate().alpha(1f).setDuration(300);

            btn_again.setVisibility(View.VISIBLE);
            btn_again.setAlpha(0f);
            btn_again.animate().alpha(1f).setDuration(300);
        }
    }

    private void startCycle() {
        handlerStatus.post(myTaskStatus);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Отменяем выполнение Runnable, если активити остановлена
//        if (handlerStatus != null) {
//            handlerStatus.removeCallbacks(myTaskStatus);
//        }
        if (handlerAddcost != null ) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }
        if (handlerCheckTask != null) {
            handlerCheckTask.removeCallbacks(checkTask);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        if (handlerStatus != null) {
//            handlerStatus.removeCallbacks(myTaskStatus);
//        }
        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }
        if (handlerCheckTask != null) {
            handlerCheckTask.removeCallbacks(checkTask);
        }
    }

    /**
     * Wfp
     */
    @SuppressLint("Range")
    private void payWfp() throws UnsupportedEncodingException {
        String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS);
        Logger.d(context, TAG, "payWfp: rectoken " + rectoken);
        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
        Logger.d(context, TAG, "payWfp: MainActivity.order_id " + MainActivity.order_id);
        callOrderIdMemory(MainActivity.order_id, uid, pay_method);
        if (rectoken.isEmpty()) {
            getUrlToPaymentWfp(amount, MainActivity.order_id);
            getStatusWfp(MainActivity.order_id);
        } else {
            paymentByTokenWfp(messageFondy, amount, rectoken, MainActivity.order_id);
        }

    }


    @Override
    public void onPause() {
        super.onPause();
        // Отменяем выполнение Runnable, если активити остановлена
//        if (handlerStatus != null) {
//            handlerStatus.removeCallbacks(myTaskStatus);
//        }
        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }
    }

    //"transactionStatus":"InProcessing"
    private void getUrlToPaymentWfp(String amount, String order_id) {
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
                messageFondy,
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
                            MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        }
                    } else {
                        Logger.d(context, TAG,"Response body is null");
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
                } else {
                    Logger.d(context, TAG, "Request failed: " + response.code());
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<InvoiceResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Request failed: " + t.getMessage());
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });
        
        
        
        
        progressBar.setVisibility(View.GONE);
    }

    private void paymentByTokenWfp(
            String orderDescription,
            String amount,
            String rectoken,
            String order_id
    ) {
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

        Call<PurchaseResponse> call = service.purchase(
                context.getString(R.string.application),
                city,
                order_id,
                amount,
                orderDescription,
                email,
                phoneNumber,
                rectoken
        );
        call.enqueue(new Callback<PurchaseResponse>() {
            @Override
            public void onResponse(@NonNull Call<PurchaseResponse> call, @NonNull Response<PurchaseResponse> response) {
                if (response.isSuccessful()) {
                    PurchaseResponse purchaseResponse = response.body();
                    if (purchaseResponse != null) {
                        // Обработка ответа
                        Logger.d(context, TAG, "onResponse:purchaseResponse " + purchaseResponse);
                        getStatusWfp(order_id);
                    } else {
                        // Ошибка при парсинге ответа
                        Logger.d(context, TAG, "Ошибка при парсинге ответа");
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        callOrderIdMemory(order_id, uid, pay_method);
                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
                } else {
                    // Ошибка запроса
                    Logger.d(context, TAG, "Ошибка запроса");
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PurchaseResponse> call, @NonNull Throwable t) {
                // Ошибка при выполнении запроса
                Logger.d(context, TAG, "Ошибка при выполнении запроса");
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });

    }

    private void getStatusWfp(String orderReferens) {
        Logger.d(context, TAG, "getStatusWfp: ");
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
        String order_id= MainActivity.order_id;
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
                            newOrderCardPayAdd20(order_id);
                            break;
                        default:
                            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                            callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                            MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", FinishSeparateFragment.messageFondy, amount, context);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                    }
                } else {
                    getReversWfp(city);
                }
            }

            @Override
            public void onFailure(@NonNull Call<StatusResponse> call, @NonNull Throwable t) {
                getReversWfp(city);
            }
        });

    }

    private void getReversWfp(String city) {
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

        ReversService service = retrofit.create(ReversService.class);

        Call<ReversResponse> call = service.checkStatus(
                 context.getString(R.string.application),
                city,
                MainActivity.order_id,
                amount
        );
        call.enqueue(new Callback<ReversResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReversResponse> call, @NonNull Response<ReversResponse> response) {
                if (response.isSuccessful()) {
                    ReversResponse statusResponse = response.body();
                    assert statusResponse != null;
                    if (statusResponse.getReasonCode() == 1100) {
                        Logger.d(context, TAG, "Transaction Status: " + statusResponse.getTransactionStatus());
                        // Другие данные можно также получить из statusResponse
                    } else {
                        Logger.d(context, TAG, "Response body is null");
                        Logger.d(context, TAG,"Response body is null");
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());


                    }
                } else {
                    Logger.d(context, TAG, "Request failed: " + response.code());
                    Logger.d(context, TAG,"Response body is null");
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                }

            }

            @Override
            public void onFailure(@NonNull Call<ReversResponse> call, @NonNull Throwable t) {
//                dismiss();
                Logger.d(context, TAG, "Request failed: " + t.getMessage());
            }
        });

    }
    /**
     * payFondy
     * @throws UnsupportedEncodingException
     */

    @SuppressLint("Range")
    private void payFondy() throws UnsupportedEncodingException {


        String rectoken = getCheckRectoken(MainActivity.TABLE_FONDY_CARDS);
        Logger.d(context, TAG, "payFondy: rectoken " + rectoken);
        if (rectoken.isEmpty()) {
            getUrlToPaymentFondy(messageFondy, amount);
        } else {
            paymentByTokenFondy(messageFondy, amount, rectoken);
        }

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
        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
        callOrderIdMemory(MainActivity.order_id, uid, pay_method);
        PaymentApiToken paymentApi = retrofit.create(PaymentApiToken.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO);
        String MERCHANT_ID = arrayList.get(6);

//        String merchantPassword = arrayList.get(7);
        List<String> stringList = logCursor(MainActivity.TABLE_USER_INFO);
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
                        if (response.isSuccessful()) {
                            ApiResponseToken<SuccessResponseDataToken> apiResponse = response.body();

                            Logger.d(context, TAG, "onResponse: " +  new Gson().toJson(apiResponse));
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

//                                Toast.makeText(context, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                                        callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                                    }
                                } else {
//                            Toast.makeText(context, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                                    callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

//                            getUrlToPaymentFondy(messageFondy, amount);
                                }
                            } catch (JsonSyntaxException e) {
                                // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                                FirebaseCrashlytics.getInstance().recordException(e);
                                Logger.d(context, TAG, "Error parsing JSON response: " + e.getMessage());
//                        Toast.makeText(context, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                                callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                                MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//                        getUrlToPaymentFondy(messageFondy, amount);
                            }
                        } else {
                            // Обработка ошибки
                            Logger.d(context, TAG, "onFailure: " + response.code());
//                    Toast.makeText(context, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();
                            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                            callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                            MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//                    getUrlToPaymentFondy(messageFondy, amount);
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponseToken<SuccessResponseDataToken>> call, @NonNull Throwable t) {
                        Logger.d(context, TAG, "onFailure1111: " + t);
//                Toast.makeText(context, R.string.pay_failure_mes, Toast.LENGTH_SHORT).show();

                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//                getUrlToPaymentFondy(messageFondy, amount);
                    }
                });
            }


            @Override
            public void onError(String error) {
                // Обработка ошибки

                Logger.d(context, TAG, "Received signature error: " + error);
            }
        });





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


    private void getUrlToPaymentFondy(String orderDescription, String amount) throws UnsupportedEncodingException {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://pay.fondy.eu/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        PaymentApi paymentApi = retrofit.create(PaymentApi.class);
        List<String>  arrayList = logCursor(MainActivity.CITY_INFO);
        String MERCHANT_ID = arrayList.get(6);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        String email = logCursor(MainActivity.TABLE_USER_INFO).get(3);

        String order_id = MainActivity.order_id;

        Map<String, String> params = new TreeMap<>();
        params.put("order_id", order_id);
        params.put("order_desc", orderDescription);
        params.put("currency", "UAH");
        params.put("amount", amount);
        params.put("preauth", "Y");
        params.put("required_rectoken", "Y");
        params.put("merchant_id", MERCHANT_ID);
        params.put("sender_email", email);
        params.put("server_callback_url", baseUrl + "server-callback");

        Logger.d(context, TAG, "getStatusFondy: " + params);
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
                Logger.d(context, TAG, "Received signature digest: " + digest);

                RequestData paymentRequest = new RequestData(
                        order_id,
                        orderDescription,
                        amount,
                        MERCHANT_ID,
                        digest,
                        email
                );


                StatusRequestPay statusRequest = new StatusRequestPay(paymentRequest);
                Logger.d(context, TAG, "getUrlToPayment: " + statusRequest);

                Call<ApiResponsePay<SuccessResponseDataPay>> call = paymentApi.makePayment(statusRequest);

                call.enqueue(new Callback<ApiResponsePay<SuccessResponseDataPay>>() {

                    @Override
                    public void onResponse(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, Response<ApiResponsePay<SuccessResponseDataPay>> response) {
                        Logger.d(context, TAG, "onResponse: 1111" + response.code());

                        if (response.isSuccessful()) {
                            ApiResponsePay<SuccessResponseDataPay> apiResponse = response.body();

                            Logger.d(context, TAG, "onResponse: " +  new Gson().toJson(apiResponse));
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
                                                uid,
                                                uid_Double,
                                                context,
                                                MainActivity.order_id
                                        );
                                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                                    } else {
                                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                                        callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                                    }
                                } else {
                                    // Обработка пустого тела ответа

                                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                                    callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                                }
                            } catch (JsonSyntaxException e) {
                                // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                                Logger.d(context, TAG, "Error parsing JSON response: " + e.getMessage());
                                FirebaseCrashlytics.getInstance().recordException(e);

                                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                                callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                                MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                            }
                        } else {
                            // Обработка ошибки
                            Logger.d(context, TAG, "onFailure: " + response.code());

                            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                            callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                            MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponsePay<SuccessResponseDataPay>> call, Throwable t) {
                        Logger.d(context, TAG, "onFailure1111: " + t);
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        callOrderIdMemory(MainActivity.order_id, uid, pay_method);
                        MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("fondy_payment", messageFondy, amount, context);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
                });
            }
            @Override
            public void onError(String error) {
                // Обработка ошибки
                Logger.d(context, TAG, "Received signature error: " + error);
            }
        });
         progressBar.setVisibility(View.GONE);
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

        Logger.d(context, TAG, "getUrlToPayment: " + paymentRequest);

        String token = context.getString(R.string.mono_key_storage); // Получение токена из ресурсов
        Call<ResponsePayMono> call = monoApi.invoiceCreate(token, paymentRequest);

        call.enqueue(new Callback<ResponsePayMono>() {

            @Override
            public void onResponse(@NonNull Call<ResponsePayMono> call, Response<ResponsePayMono> response) {
                Logger.d(context, TAG, "onResponse: 1111" + response.code());
                if (response.isSuccessful()) {
                    ResponsePayMono apiResponse = response.body();

                    Logger.d(context, TAG, "onResponse: " +  new Gson().toJson(apiResponse));
                    try {
                        assert response.body() != null;
                        String pageUrl = response.body().getPageUrl();
                        MainActivity.invoiceId = response.body().getInvoiceId();


                        MyBottomSheetCardPayment bottomSheetDialogFragment = new MyBottomSheetCardPayment(
                                pageUrl,
                                amount,
                                uid,
                                uid_Double,
                                context,
                                MainActivity.order_id
                        );
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                    } catch (JsonSyntaxException e) {
                        // Возникла ошибка при разборе JSON, возможно, сервер вернул неправильный формат ответа
                        Logger.d(context, TAG, "Error parsing JSON response: " + e.getMessage());
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
            public void onFailure(@NonNull Call<ResponsePayMono> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "onFailure1111: " + t);
                cancelOrderDouble();
            }


        });
    }
    private void updateAddCost(String addCost) {
        ContentValues cv = new ContentValues();
        Logger.d(context, TAG, "updateAddCost: addCost" + addCost);
        cv.put("addCost", addCost);

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

    private void fetchBonus() {
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        String url = baseUrl + "bonusBalance/recordsBloke/" + uid + "/" +  context.getString(R.string.application);
        Call<BonusResponse> call = ApiClient.getApiService().getBonus(url);
        Logger.d(context, TAG, "fetchBonus: " + url);
        call.enqueue(new Callback<BonusResponse>() {
            @Override
            public void onResponse(@NonNull Call<BonusResponse> call, @NonNull Response<BonusResponse> response) {
                BonusResponse bonusResponse = response.body();
                if (response.isSuccessful()) {

                    assert bonusResponse != null;
                    String bonus = String.valueOf(bonusResponse.getBonus());
                    String message =  context.getString(R.string.block_mes) + " " + bonus + " " +  context.getString(R.string.bon);

                    MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment( context.getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            }

            @Override
            public void onFailure(@NonNull Call<BonusResponse> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }

    public static void callOrderIdMemory(String orderId, String uid, String paySystem) {
        if(!no_pay) {
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

    }

    private void wfpInvoice(String orderId, String amount, String uid) {
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

    private boolean connected() {

        boolean hasConnect = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
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
    private void cancelOrder(String value) throws ParseException {
        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }
        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = baseUrl  + api + "/android/webordersCancel/" + value + "/" + city  + "/" +  context.getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrder(url);
        Logger.d(context, TAG, "cancelOrderWithDifferentValue cancelOrderUrl: " + url);
        text_status.setText(R.string.sent_cancel_message);
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful()) {
                    Status status = response.body();
                    assert status != null;
                    Logger.d(context, TAG, "cancelOrder status: " + status.toString());
                } else {
                    // Обработка неуспешного ответа
                    text_status.setText(R.string.verify_internet);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                Logger.d(context, TAG, "onFailure: " + errorMessage);
                text_status.setText(R.string.verify_internet);
            }
        });
        progressBar.setVisibility(View.GONE);
        statusOrderWithDifferentValue(uid);
    }
    private void cancelOrderDouble() {
        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") +"/";
        String url = baseUrl + api + "/android/webordersCancelDouble/" + uid+ "/" + uid_Double + "/" + pay_method + "/" + city  + "/" +  context.getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrderDouble(url);
        Logger.d(context, TAG, "cancelOrderDouble: " + url);
        text_status.setText(R.string.sent_cancel_message);
        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
//                if (response.isSuccessful()) {
//                    Status status = response.body();
//                    assert status != null;
//                    Logger.d(context, TAG, "cancelOrderDouble status: " + status);
//                } else {
//                    // Обработка неуспешного ответа
//                    text_status.setText(R.string.verify_internet);
//                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                Logger.d(context, TAG, "onFailure: " + errorMessage);
//                text_status.setText(R.string.verify_internet);
            }
        });
        progressBar.setVisibility(View.GONE);
    }

    public void statusOrderWithDifferentValue(String value) throws ParseException {

        isTenMinutesRemainingFunction();

        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);
        baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        String url = baseUrl  + api + "/android/historyUIDStatus/" + value + "/" + city  + "/" +  context.getString(R.string.application);

        Call<OrderResponse> call = ApiClient.getApiService().statusOrder(url);
        Logger.d(context, TAG, "/android/historyUIDStatus/: " + url);

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
                    String time_to_start_point = orderResponse.getTimeToStartPoint();

                    int closeReason = orderResponse.getCloseReason();
//                    if (requiredTime != null && !requiredTime.isEmpty()) {
//                        requiredTime = formatDate (orderResponse.getRequiredTime());
//                    }

                    if (time_to_start_point != null && !time_to_start_point.isEmpty()) {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        try {
                            // Преобразуем строку в объект Date
                            Date date = dateFormat.parse(time_to_start_point);
                            timeToStartMillis = date.getTime(); // Получаем время в миллисекундах
                            startCountdown();
                        } catch (ParseException e) {
                            e.printStackTrace();
                            return; // Выйти, если произошла ошибка парсинга
                        }
                    }

                    String message;
                    // Обработка различных вариантов executionStatus
                    switch (executionStatus) {
                        case "WaitingCarSearch":
                        case "SearchesForCar":
                            // 10 минут до предварительного заказа
                            if (!isTenMinutesRemainingBlock && isTenMinutesRemaining) {
                                isTenMinutesRemainingAction();
                            }

                            if(cancel_btn_click) {
                                if (handler != null) {
                                    handler.removeCallbacks(myRunnable);
                                }
                                if (handlerBonusBtn != null) {
                                    handlerBonusBtn.removeCallbacks(runnableBonusBtn);
                                }

                                if (handlerAddcost != null) {
                                    handlerAddcost.removeCallbacks(showDialogAddcost);
                                }
                                if (handlerCheckTask != null) {
                                    handlerCheckTask.removeCallbacks(checkTask);
                                }
                                btn_reset_status.setVisibility(View.GONE);
                                btn_cancel_order.setVisibility(View.GONE);
                                carProgressBar.setVisibility(View.GONE);
                                message =  context.getString(R.string.checkout_status);
                                text_status.setText(message);
                            } else {
                                if(need_20_add) {
                                    if (handlerAddcost != null && showDialogAddcost != null) {
                                        handlerAddcost.postDelayed(showDialogAddcost, timeCheckOutAddCost); // Устанавливаем нужную задержку
                                    }
                                }

                                message =  context.getString(R.string.ex_st_0);
                                carProgressBar.setVisibility(View.VISIBLE);
                                text_status.setText(message);
                                text_status.startAnimation(blinkAnimation);
                                updateProgress(2);
                                countdownTextView.setVisibility(View.GONE);
                                delayMillisStatus = 5 * 1000;


                                textStatusCar.setVisibility(View.GONE);
                                textCarMessage.setVisibility(View.GONE);

//                                btn_reset_status.setText(context.getString(R.string.textStatus));
//                                btn_reset_status.setOnClickListener(v -> {
//                                    if(connected()){
//                                        try {
//                                            statusOrderWithDifferentValue(uid);
//                                        } catch (ParseException e) {
//                                            throw new RuntimeException(e);
//                                        }
//                                    } else {
//                                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment( context.getString(R.string.verify_internet));
//                                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//                                    }
//                                });

                            }

                            break;
                        case "Canceled":
                            text_status.clearAnimation();
                            canceled = false;
                            if (handler != null) {
                                handler.removeCallbacks(myRunnable);
                            }
                            if (handlerBonusBtn != null) {
                                handlerBonusBtn.removeCallbacks(runnableBonusBtn);
                            }
                            if (handlerStatus != null) {
                                handlerStatus.removeCallbacks(myTaskStatus);
                            }
                            if (handlerAddcost != null) {
                                handlerAddcost.removeCallbacks(showDialogAddcost);
                            }
                            if (handlerCheckTask != null) {
                                handlerCheckTask.removeCallbacks(checkTask);
                            }
                            Logger.d(context, TAG, "statusOrderWithDifferentValue canceled: " + canceled);
                            if(cancel_btn_click) {
                                message = context.getString(R.string.ex_st_canceled);


                                message =  context.getString(R.string.ex_st_canceled);
                            } else {
                                if (pay_method.equals("fondy_payment") || pay_method.equals("mono_payment")|| pay_method.equals("wfp_payment")) {

                                    message =  context.getString(R.string.pay_cancel);
                                } else {
                                    message =  context.getString(R.string.ex_st_canceled);
                                }
                            }

                            if(canceled) {
                                MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);

                                fragmentManager.beginTransaction()
                                        .add(bottomSheetDialogFragment, bottomSheetDialogFragment.getTag())
                                        .commitAllowingStateLoss();
                            }
                            textCost.setVisibility(View.GONE);
                            textCostMessage.setVisibility(View.GONE);
                            carProgressBar.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            progressSteps.setVisibility(View.GONE);


                            btn_options.setVisibility(View.GONE);
                            btn_open.setVisibility(View.GONE);
                            btn_reset_status.setVisibility(View.GONE);
                            btn_cancel_order.setVisibility(View.GONE);
                            btn_again.setVisibility(View.VISIBLE);

                            text_status.setText(message);
                            break;
                        case "CarFound":
                            text_status.clearAnimation();
                            textCost.setVisibility(View.VISIBLE);
                            textCostMessage.setVisibility(View.VISIBLE);
                            if (handlerAddcost != null) {
                                handlerAddcost.removeCallbacks(showDialogAddcost);
                            }
                            text_status.clearAnimation();
                            btn_cancel_order.setVisibility(View.GONE);
                            if(closeReason == -1) {
                               // Гекоординаты водителя по АПИ
                                drivercarposition(value, city, api);
                            } else {
                                calculateTimeToStart(value, api);
                            }
                            updateProgress(3);

                            if(!cancel_btn_click) {
                                delayMillisStatus = 5 * 1000;
                                // Формируем сообщение с учетом возможных пустых значений переменных
                                StringBuilder messageBuilder = new StringBuilder( context.getString(R.string.ex_st_2));


                                if (driverPhone != null && !driverPhone.isEmpty()) {
                                    Logger.d(context, TAG, "onResponse:driverPhone " + driverPhone);
                                    btn_reset_status.setText( context.getString(R.string.phone_driver));
                                    btn_reset_status.setOnClickListener(v -> {
                                        Intent intent = new Intent(Intent.ACTION_DIAL);
                                        intent.setData(Uri.parse("tel:" + driverPhone));
                                        startActivity(intent);
                                    });
                                }


                                if (time_to_start_point != null && !time_to_start_point.isEmpty()) {
                                    messageBuilder.append( context.getString(R.string.ex_st_5)).append(formatDate2(time_to_start_point));
                                }

                                if (orderCarInfo != null && !orderCarInfo.isEmpty()) {
                                    textStatusCar.setVisibility(View.VISIBLE);
                                    textCarMessage.setVisibility(View.VISIBLE);
                                    textCarMessage.setText(orderCarInfo);
                                }

                                countdownTextView.setVisibility(View.VISIBLE);

                                progressBar.setVisibility(View.GONE);
                                message = messageBuilder.toString();
                            } else {
                                message =  context.getString(R.string.ex_st_canceled);
                            }
                            text_status.setText(message);
                            break;
                        case "CarInStartPoint":
                            textCost.setVisibility(View.VISIBLE);
                            textCostMessage.setVisibility(View.VISIBLE);
                            if (handlerAddcost != null) {
                                handlerAddcost.removeCallbacks(showDialogAddcost);
                            }
                            if (handlerCheckTask != null) {
                                handlerCheckTask.removeCallbacks(checkTask);
                            }
                            text_status.clearAnimation();
                            btn_cancel_order.setVisibility(View.GONE);
                            carProgressBar.setVisibility(View.GONE);
                            countdownTextView.setVisibility(View.GONE);
                            updateProgress(4);
                            if(!cancel_btn_click) {
                                delayMillisStatus = 5 * 1000;
                                // Формируем сообщение с учетом возможных пустых значений переменных
                                StringBuilder messageBuilder = new StringBuilder( context.getString(R.string.ex_st_2_1));

                                if (driverPhone != null && !driverPhone.isEmpty()) {
                                    Logger.d(context, TAG, "onResponse:driverPhone " + driverPhone);
                                    btn_reset_status.setText( context.getString(R.string.phone_driver));
                                    btn_reset_status.setOnClickListener(v -> {
                                        Intent intent = new Intent(Intent.ACTION_DIAL);
                                        intent.setData(Uri.parse("tel:" + driverPhone));
                                        startActivity(intent);
                                    });
                                }

                                if (orderCarInfo != null && !orderCarInfo.isEmpty()) {
                                    textStatusCar.setVisibility(View.VISIBLE);
                                    textCarMessage.setVisibility(View.VISIBLE);
                                    textCarMessage.setText(orderCarInfo);
                                }

                                progressBar.setVisibility(View.GONE);
                                message = messageBuilder.toString();
                            } else {
                                message =  context.getString(R.string.ex_st_canceled);
                            }
                            text_status.setText(message);
                            break;
                        default:
                            if (handlerAddcost != null) {
                                handlerAddcost.removeCallbacks(showDialogAddcost);
                            }
                            textCost.setVisibility(View.VISIBLE);
                            textCostMessage.setVisibility(View.VISIBLE);
                            btn_cancel_order.setVisibility(View.VISIBLE);
                            carProgressBar.setVisibility(View.VISIBLE);
                            delayMillisStatus = 30 * 1000;
                            message =  context.getString(R.string.status_checkout_message);
                            text_status.setText(message);
                            break;
                    }
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
            }
        });
    }

    void drivercarposition (String value, String city, String api) {

        String url = baseUrl  + api + "/android/drivercarposition/" + value + "/" + city  + "/" +  context.getString(R.string.application);

        Call<Void> call = ApiClient.getApiService().drivercarposition(url);

        Logger.d(context, TAG, "/android/drivercarposition/: " + url);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }

    void calculateTimeToStart (String value, String api) {

        String url = baseUrl  + api + "/android/calculateTimeToStart/" + value;

        Call<Void> call = ApiClient.getApiService().calculateTimeToStart(url);

        Logger.d(context, TAG, "calculateTimeToStart: " + url);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }


    private void startCountdown() {
        long currentTimeMillis = System.currentTimeMillis();
        long remainingTime = timeToStartMillis - currentTimeMillis; // Вычисляем оставшееся время
        Logger.d(context, TAG, "remainingTime: " + remainingTime);
        if (remainingTime > 60000) {
            updateCountdownText(remainingTime);
        } else {
            updateCountdownText(60000);
        }
    }

    private void updateCountdownText(long millisUntilFinished) {

        int minutes = (int) ((millisUntilFinished / (1000 * 60)));
        Logger.d(context, TAG, "minutes " + minutes);
        // Обновляем текст с минутами и секундами
        String countdownText = getString(R.string.time_info_to_start_point) +  " " +  minutes +  getString(R.string.minutes);
        countdownTextView.setText(countdownText);
    }



    private String formatDate2(String requiredTime) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("uk", "UA"));
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("uk", "UA"));
        Date date = null;
        try {
            date = inputFormat.parse(requiredTime);
        } catch (ParseException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Logger.d(context, TAG, "onCreate:" + new RuntimeException(e));
        }

        return outputFormat.format(date);
    }

    private void updateProgress(int step) {
        // Сбрасываем все шаги на неактивные
        step1.setBackgroundResource(R.drawable.circle_step_inactive);
        step2.setBackgroundResource(R.drawable.circle_step_inactive);
        step3.setBackgroundResource(R.drawable.circle_step_inactive);
        step4.setBackgroundResource(R.drawable.circle_step_inactive);

        // Активируем шаги вплоть до текущего
        if (step >= 1) {
            step1.setBackgroundResource(R.drawable.circle_step_active);
        }
        if (step >= 2) {
            step2.setBackgroundResource(R.drawable.circle_step_active);
        }
        if (step >= 3) {
            step3.setBackgroundResource(R.drawable.circle_step_active);
        }
        if (step >= 4) {
            step4.setBackgroundResource(R.drawable.circle_step_active);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        addCheck(context);
        btn_open.setOnClickListener(v -> {
            btnOpen();
        });
        startAddCostDialog (
                pay_method,
                uid,
                timeCheckOutAddCost);
    }


    @Override
    public void onStop() {
        super.onStop();
        // Отменяем выполнение Runnable, если фрагмент уходит в фон
//        if (handlerStatus != null) {
//            handlerStatus.removeCallbacks(myTaskStatus);
//        }
        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        // Повторный запуск Runnable при возвращении активности
        if (handler != null && myRunnable != null) {
            handler.postDelayed(myRunnable, 1000); // Устанавливаем нужную задержку
        }
        if (handlerBonusBtn != null && runnableBonusBtn != null) {
            handlerBonusBtn.postDelayed(runnableBonusBtn, 2000); // Устанавливаем нужную задержку
        }
        if (handlerStatus != null && myTaskStatus != null) {
            handlerStatus.postDelayed(myTaskStatus, 3000); // Устанавливаем нужную задержку
        }

    }
    private void startAddCostDialog (
            String pay_method,
            String uid_old,
            int timeCheckout
    ) {



        Logger.d(context, TAG, "payMetod " + pay_method);

        if ("nal_payment".equals(pay_method) || "wfp_payment".equals(pay_method)) {
            handlerAddcost = new Handler();
            showDialogAddcost = () -> {
                // Вызов метода для отображения диалога
//                if (handlerStatus != null) {
//                    handlerStatus.removeCallbacks(myTaskStatus);
//                }
                showAddCostDialog(uid_old, timeCheckout);
            };
            // Запускаем выполнение через 1 минуты (60 000 миллисекунд)
            handlerAddcost.postDelayed(showDialogAddcost, timeCheckout);
        }

        btn_reset_status.setOnClickListener(view -> {
            String text = textCostMessage.getText().toString();
            Logger.d(getActivity(), TAG, "textCostMessage.getText().toString() " + text);

            Pattern pattern = Pattern.compile("(\\d+)");
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                Logger.d(context, TAG, "amount_to_add: " + matcher.group(1));
                MyBottomSheetAddCostFragment bottomSheetDialogFragment = new MyBottomSheetAddCostFragment(
                        matcher.group(1),
                        uid,
                        uid_Double,
                        pay_method,
                        context,
                        fragmentManager
                );
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            } else {
                Logger.d(context, TAG, "No numeric value found in the text.");
            }
        });
    }
    private void showAddCostDialog(String uid, int timeCheckout) {
        // Убедитесь, что handlerAddcost не null и очищаем предыдущие задачи
        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }

        // Убедитесь, что фрагмент добавлен
        if (!isAdded() || getActivity() == null) {
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_cost, null);

        // Настройка текста с выделенным числом
        TextView messageView = dialogView.findViewById(R.id.dialogMessage);
        String messageText = getString(R.string.add_cost_fin_20); // "Вам нужно добавить 20 единиц"
        SpannableStringBuilder spannable = new SpannableStringBuilder(messageText);
        int numberIndex = messageText.indexOf("20");
        spannable.setSpan(new StyleSpan(Typeface.BOLD), numberIndex, numberIndex + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        messageView.setText(spannable);


        builder.setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                    // Действие для кнопки "OK"
                    dialog.dismiss();
                    startAddCostUpdate(uid);

                    // Перезапускаем задачу
                    if (handlerAddcost != null) {
                        handlerAddcost.postDelayed(showDialogAddcost, timeCheckout);
                    }
                })
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> {
                    // Действие для кнопки "Отмена"
                    if (handlerAddcost != null) {
                        handlerAddcost.postDelayed(showDialogAddcost, timeCheckout);
                    }

//                    if (handlerStatus != null) {
//                        handlerStatus.post(myTaskStatus);
//                    }
                    dialog.dismiss();
                })
                .show();
    }



    private void startAddCostUpdate(
            String uid
    ) {

        String cost = textCostMessage.getText().toString();
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(4);
        if ("nal_payment".equals(pay_method)) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl) // Замените BASE_URL на ваш базовый URL сервера
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            ApiService apiService = retrofit.create(ApiService.class);

            Pattern pattern = Pattern.compile("(\\d+)");
            Matcher matcher = pattern.matcher(textCostMessage.getText().toString());
            if (matcher.find()) {
                Logger.d(context, TAG, "amount_to_add: " + matcher.group(1));
            } else {
                Logger.d(context, TAG, "No numeric value found in the text.");
            }
            Call<Status> call = apiService.startAddCostWithAddBottomUpdate(
                    uid,
                    matcher.group(1)
            );

            String url = call.request().url().toString();
            Logger.d(context, TAG, "URL запроса nal_payment: " + url);
            call.enqueue(new Callback<Status>() {
                @Override
                public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                    if (response.isSuccessful()) {
                        Status status = response.body();
                        assert status != null;
                        String responseStatus = status.getResponse();
                        Logger.d(context, TAG, "startAddCostUpdate  nal_payment: " + responseStatus);
                        if(responseStatus.equals("200")) {
                            handlerStatus.post(myTaskStatus);

                            Pattern pattern = Pattern.compile("(\\d+)");
                            Matcher matcher = pattern.matcher(textCostMessage.getText().toString());

                            if (matcher.find()) {
                                // Преобразуем найденное число в целое, добавляем 20
                                int originalNumber = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
                                int updatedNumber = originalNumber + 20;

                                // Заменяем старое значение на новое
                                String updatedCost = matcher.replaceFirst(String.valueOf(updatedNumber));
                                textCost.setVisibility(View.VISIBLE);
                                textCostMessage.setVisibility(View.VISIBLE);
                                carProgressBar.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.VISIBLE);
                                progressSteps.setVisibility(View.VISIBLE);

                                btn_options.setVisibility(View.VISIBLE);
                                btn_open.setVisibility(View.VISIBLE);


                                textCostMessage.setText(updatedCost);
                                Log.d("UpdatedCost", "Обновленная строка: " + updatedCost);
                            } else {
                                Log.e("UpdatedCost", "Число не найдено в строке: " + cost);
                            }
                        } else {
                            // Обработка неуспешного ответа
                            text_status.setText(R.string.verify_internet);
                        }

                    } else {
                        // Обработка неуспешного ответа
                        text_status.setText(R.string.verify_internet);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                    // Обработайте ошибку при выполнении запроса
                    handlerStatus.post(myTaskStatus);
                    textCostMessage.setText(cost);
                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });
        }
        if ("wfp_payment".equals(pay_method)) {
            startAddCostCardUpdate();
        }
    }

    private void startAddCostCardUpdate() {
        Logger.d(context, TAG, "startAddCostCardUpdate: ");
        String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS);
        Logger.d(context, TAG, "payWfp: rectoken " + rectoken);
        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);

        wfpInvoice(MainActivity.order_id , "20", uid);

        if (rectoken.isEmpty()) {
            getUrlToPaymentWfp("20", MainActivity.order_id );
        } else {
            paymentByTokenWfp(messageFondy, "20", rectoken, MainActivity.order_id );
        }

    }

    private void newOrderCardPayAdd20 (String order_id) {
        String baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";

        Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        ApiService apiService = retrofit.create(ApiService.class);
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city = stringList.get(1);
        Call<Status> call = apiService.startAddCostCardBottomUpdate(
                uid,
                uid_Double,
                pay_method,
                order_id,
                city,
                "20"
        );
        String url = call.request().url().toString();
        Logger.d(context, TAG, "URL запроса wfp_payment: " + url);

        String cost = textCostMessage.getText().toString();

        call.enqueue(new Callback<Status>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful()) {
                    Status status = response.body();
                    assert status != null;
                    String responseStatus = status.getResponse();
                    Logger.d(context, TAG, "startAddCostUpdate wfp_payment status: " + responseStatus);
                    if(responseStatus.equals("200")) {
                        handlerStatus.post(myTaskStatus);

                        Pattern pattern = Pattern.compile("(\\d+)");
                        Matcher matcher = pattern.matcher(cost);

                        if (matcher.find()) {
                            // Преобразуем найденное число в целое, добавляем 20
                            int originalNumber = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
                            int updatedNumber = originalNumber + 20;

                            // Заменяем старое значение на новое
                            String updatedCost = matcher.replaceFirst(String.valueOf(updatedNumber));
                            textCost.setVisibility(View.VISIBLE);
                            textCostMessage.setVisibility(View.VISIBLE);
                            carProgressBar.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.VISIBLE);
                            progressSteps.setVisibility(View.VISIBLE);

                            btn_options.setVisibility(View.VISIBLE);
                            btn_open.setVisibility(View.VISIBLE);
                            

                            textCostMessage.setText(updatedCost);
                            Log.d("UpdatedCost", "Обновленная строка: " + updatedCost);

                        } else {
                            Log.e("UpdatedCost", "Число не найдено в строке: " + cost);
                        }

                    } else {
                        // Обработка неуспешного ответа
                        text_status.setText(R.string.verify_internet);
                    }

                } else {
                    // Обработка неуспешного ответа
                    text_status.setText(R.string.verify_internet);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                // Обработайте ошибку при выполнении запроса
                handlerStatus.post(myTaskStatus);
                textCostMessage.setText(cost);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });


    }

    private void isTenMinutesRemainingFunction() throws ParseException {

        if (required_time != null && !required_time.isEmpty()) {
            Date currentDate = new Date();
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

            Date requiredDate = dateFormat.parse(required_time);
            assert requiredDate != null;
            long differenceInMillis = requiredDate.getTime() - currentDate.getTime(); // Разница в миллисекундах
            Log.d("isTenMinutesRemainingFunction", " 1 required_time " + required_time);
            Log.d("isTenMinutesRemainingFunction", " 1 differenceInMillis " + differenceInMillis);
            if(differenceInMillis <=0 && !required_time.equals("01.01.1970 00:00")) {
                isTenMinutesRemaining = true;
            } else {
                long tenMinutesInMillis = 10 * 60 * 1000; // 10 минут в миллисекундах
                // Проверка на оставшиеся 10 минут
                Log.d("isTenMinutesRemainingFunction", "tenMinutesInMillis " + tenMinutesInMillis);
                Log.d("isTenMinutesRemainingFunction", "differenceInMillis " + differenceInMillis);
                Log.d("isTenMinutesRemainingFunction", "isTenMinutesRemaining " + isTenMinutesRemaining);
                isTenMinutesRemaining = differenceInMillis <= tenMinutesInMillis && differenceInMillis > 0;
            }


        }

    }
    private void isTenMinutesRemainingAction() {
        isTenMinutesRemainingBlock = true;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_cost, null);

        // Настройка текста с выделенным числом
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText(R.string.time_car_found);
        TextView messageView = dialogView.findViewById(R.id.dialogMessage);
        String messageText = getString(R.string.cancel_car_found_time);
        messageView.setText(messageText);

        builder.setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                    dialog.dismiss();
                    if(!uid_Double.equals(" ")) {
                        cancelOrderDouble();
                    } else{
                        try {
                            cancelOrder(uid);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_visicom, true)
                            .build());
                })
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
    private void addCheck(Context context) {

        int newCheck = 0;
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO);
        for (int i = 0; i < DataArr.arrayServiceCode().length; i++) {
            if (services.get(i + 1).equals("1")) {
                newCheck++;
            }
        }
        String mes = context.getString(R.string.add_services);
        if (newCheck != 0) {
            mes = context.getString(R.string.add_services) + " (" + newCheck + ")";
        }
        btn_options.setText(mes);

    }
 }