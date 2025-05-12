package com.taxi.easy.ua.ui.finish.fragm;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.TABLE_USER_INFO;
import static com.taxi.easy.ua.MainActivity.paySystemStatus;
import static com.taxi.easy.ua.MainActivity.uid_Double;
import static com.taxi.easy.ua.MainActivity.viewModel;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.navigation.NavOptions;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentFinishSeparateBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.BonusResponse;
import com.taxi.easy.ua.ui.finish.OrderResponse;
import com.taxi.easy.ua.ui.finish.Status;
import com.taxi.easy.ua.utils.animation.car.CarProgressBar;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetAddCostFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorPaymentFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetFinishOptionFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.hold.APIHoldService;
import com.taxi.easy.ua.utils.hold.HoldResponse;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.pusher.events.CanceledStatusEvent;
import com.taxi.easy.ua.utils.time_ut.TimeUtils;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;
import com.uxcam.UXCam;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

    private  final String TAG = "FinishSeparateFragment";

    Activity context;
    FragmentManager fragmentManager;
    View root;
    @SuppressLint("StaticFieldLeak")
    public static TextView text_status;


    public static String baseUrl;
    Map<String, String> receivedMap;


    public static String pay_method;

    public static String amount;
    @SuppressLint("StaticFieldLeak")
    public static TextView text_full_message, textCost, textCostMessage, textCarMessage, textStatus, textStatusCar;
    String messageResult;
    String messageResultCost;
    String messageFondy;

    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btn_add_cost;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btn_cancel_order;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btn_again;

    public Runnable myRunnable;
    public Runnable runnableBonusBtn;
    public static Handler handler, handlerBonusBtn,  handlerStatus;
    public static Runnable myTaskStatus;

    String email;

    public static String phoneNumber;
    boolean cancel_btn_click = false;
    long delayMillisStatus;
    boolean no_pay;
    boolean canceled = false;
    @SuppressLint("StaticFieldLeak")
    public static  CarProgressBar carProgressBar;
    // Получаем доступ к кружочкам
    View step1;
    View step2;
    View step3;
    View step4;
    TextView countdownTextView;
    private long timeToStartMillis;
    @SuppressLint("StaticFieldLeak")
    public static LinearLayout progressSteps;
    Animation blinkAnimation;
    public static AppCompatButton btn_open;
    public static AppCompatButton btn_options;

    String flexible_tariff_name;
    String comment_info;
    String extra_charge_codes;

    public static Handler handlerAddcost;
    public static Runnable showDialogAddcost;

    public static int timeCheckOutAddCost;

    boolean need_20_add;
    String required_time;

    Handler handlerCheckTask;
    Runnable checkTask;
    private boolean isTaskRunning = false;
    private  boolean isTaskCancelled = false;

    TimeUtils timeUtils;
    private Observer<Boolean> observer;

    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        UXCam.tagScreenName(TAG);

        FragmentFinishSeparateBinding binding = FragmentFinishSeparateBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        context = requireActivity();

        // Включаем блокировку кнопки "Назад" Применяем блокировку кнопки "Назад"
        BackPressBlocker backPressBlocker = new BackPressBlocker();
        backPressBlocker.setBackButtonBlocked(true);
        backPressBlocker.blockBackButtonWithCallback(this);

        fragmentManager = getParentFragmentManager();
        MainActivity.action = null;
      

        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
        if(pay_method.equals("nal_payment")) {
            timeCheckOutAddCost = 60*1000;
        } else  {
            timeCheckOutAddCost =  75*1000;
        }

        Logger.d(context, TAG, "pay_method " + pay_method);

        AppCompatButton btnCallAdmin = root.findViewById(R.id.btnCallAdmin);
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
            String phone = stringList.get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });
        messageFondy =  context.getString(R.string.fondy_message);
        email = logCursor(TABLE_USER_INFO, context).get(3);
        phoneNumber = logCursor(TABLE_USER_INFO, context).get(2);

        Bundle arguments = getArguments();
        assert arguments != null;


        String no_pay_key = arguments.getString("card_payment_key");

        Logger.d(context, TAG, "no_pay: key " + no_pay_key);

        no_pay = no_pay_key != null && no_pay_key.equals("no");
        Logger.d(context, TAG, "no_pay: " + no_pay);


        receivedMap = (HashMap<String, String>) arguments.getSerializable("sendUrlMap");

        assert receivedMap != null;

        flexible_tariff_name = receivedMap.get("flexible_tariff_name");

        required_time = receivedMap.get("required_time");
        if (required_time == null || required_time.isEmpty()) {
            need_20_add = true;
        }
        Logger.d(context, TAG, "required_time: " + required_time);
        Logger.d(context, TAG, "need_20_add: " + need_20_add);

        comment_info = receivedMap.get("comment_info");
        Logger.d(context, TAG, "comment_info: " + comment_info);

        extra_charge_codes = receivedMap.get("extra_charge_codes");
        Logger.d(context, TAG, "extra_charge_codes: " + extra_charge_codes);

        Logger.d(context, TAG, "onCreate: receivedMap" + receivedMap.toString());
        text_full_message = root.findViewById(R.id.text_full_message);
        messageResult = arguments.getString("messageResult_key");

        assert messageResult != null;
        text_full_message.setText(messageResult.replace("null", ""));



        messageResultCost = arguments.getString("messagePay_key");
        textCost = root.findViewById(R.id.textCost);
        textCostMessage = root.findViewById(R.id.text_cost_message);
        Logger.d(context, TAG, "onCreate: textCostMessage" + messageResultCost);
        textCostMessage.setText(messageResultCost);

        textStatus = root.findViewById(R.id.textStatus);
        textStatusCar = root.findViewById(R.id.textStatusCar);
        textStatusCar.setVisibility(GONE);

        textCarMessage = root.findViewById(R.id.text_status_car);
        textCarMessage.setVisibility(GONE);

        MainActivity.uid = arguments.getString("UID_key");


        Logger.d(context, TAG, "MainActivity.uid: " + MainActivity.uid);

        uid_Double = receivedMap.get("dispatching_order_uid_Double");



        text_status = root.findViewById(R.id.text_status);
        text_status.setText( context.getString(R.string.ex_st_0));
        text_status.startAnimation(blinkAnimation);


        btn_add_cost = root.findViewById(R.id.btn_add_cost);


        btn_cancel_order = root.findViewById(R.id.btn_cancel_order);



        long delayMillis = 5 * 60 * 1000;

        if (pay_method.equals("wfp_payment")) {
            amount = receivedMap.get("order_cost");
        }
        if (pay_method.equals("fondy_payment")) {
            amount = receivedMap.get("order_cost") + "00";
        }

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
                        || !newStatus.contains( context.getString(R.string.call_btn_cancel))
                        || !newStatus.contains( context.getString(R.string.ex_st_canceled))
                ) {
                    String cancelText = context.getString(R.string.status_checkout_message);
                    text_status.setText(cancelText);

                } else {
                    text_status.setText(newStatus);
                }

            };
            handlerBonusBtn.postDelayed(runnableBonusBtn, delayMillis);
        }

        handlerStatus = new Handler();
        delayMillisStatus = 10 * 1000;
        myTaskStatus = new Runnable() {
            @Override
            public void run() {
                // Ваш код
                isTaskRunning = true;
                try {
                    statusOrder();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }

                // Запланировать повторное выполнение
                handlerStatus.postDelayed(this, delayMillisStatus);
                isTaskRunning = false; // Сброс состояния
            }
        };



        btn_again = root.findViewById(R.id.btn_again);
        btn_again.setOnClickListener(v -> {
            MainActivity.order_id = null;
            sharedPreferencesHelperMain.saveValue("carFound", false);
            updateAddCost(String.valueOf(0));
            if (handlerStatus != null) {
                handlerStatus.removeCallbacks(myTaskStatus);
            }
            MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());
        });


        Logger.d(context, TAG, "no_pay: 2 " + no_pay);

        ImageButton btn_no = root.findViewById(R.id.btn_no);
        btn_no.setOnClickListener(view -> startActivity(new Intent(context, MainActivity.class)));

        carProgressBar = root.findViewById(R.id.carProgressBar);

        // Запустить анимацию
        carProgressBar.resumeAnimation();

        btn_open = binding.btnOpen;

        btn_options = binding.btnOptions;

        int colorPressed = ContextCompat.getColor(context, R.color.colorDefault); // Цвет текста при нажатии
        int colorDefault = ContextCompat.getColor(context, R.color.colorAccent); // Исходный цвет текста
        btn_options.setOnClickListener(v -> {

            // Получаем текущий цвет текста кнопки
            int currentColor = btn_options.getCurrentTextColor();

            if (currentColor == colorDefault) {
                btn_options.setTextColor(colorPressed);
            } else {
                btn_options.setTextColor(colorDefault);
            }

            btnOptions();
        });
        try {
            statusOrder();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return root;
    }



     void updateUICardPayStatus(
            OrderResponse orderResponse
    ) {
        assert orderResponse != null;

        String orderCarInfo = orderResponse.getOrderCarInfo();
        String driverPhone = orderResponse.getDriverPhone();

        String time_to_start_point = orderResponse.getTimeToStartPoint();

        MainActivity.action = orderResponse.getAction();


        int closeReason = orderResponse.getCloseReason();

        Logger.d(context, TAG, "OrderResponse: action " + MainActivity.action);
        if(MainActivity.action != null) {
            if (time_to_start_point != null && !time_to_start_point.isEmpty()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                try {
                    // Преобразуем строку в объект Date
                    Date date = dateFormat.parse(time_to_start_point);
                    assert date != null;
                    timeToStartMillis = date.getTime(); // Получаем время в миллисекундах
                    startCountdown(context);
                } catch (ParseException e) {
                    return; // Выйти, если произошла ошибка парсинга
                }
            }

            closeReasonReactCard(
                    closeReason,
                    driverPhone,
                    time_to_start_point,
                    orderCarInfo,
                    MainActivity.action
            );
        }

    }

    public  void handleTransactionStatusDeclined(
            String status,
            Context context
    ) {
        Logger.d(context, TAG, "Transaction Status: " + status);

        if ("Declined".equals(status)) {

            MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment =
                    new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
        }
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
        Log.d("btnOpen", "pay_method " + pay_method);
        Log.d("btnOpen", "canceled " + canceled);
//        int currentColor = btn_open.getCurrentTextColor();
        int colorPressed = ContextCompat.getColor(context, R.color.colorDefault); // Цвет текста при нажатии
        int colorDefault = ContextCompat.getColor(context, R.color.colorAccent); // Исходный цвет текста

        if (btn_add_cost.getVisibility() == View.VISIBLE) {
            // Анимация исчезновения кнопок
            btn_open.setTextColor(colorPressed);
            btn_add_cost.animate().alpha(0f).setDuration(300).withEndAction(() -> btn_add_cost.setVisibility(GONE));

            btn_again.animate().alpha(0f).setDuration(300).withEndAction(() -> btn_again.setVisibility(GONE));
        } else {
            // Анимация появления кнопок

            btn_open.setTextColor(colorDefault);
            btn_add_cost.setVisibility(View.VISIBLE);
            btn_add_cost.setAlpha(0f);
            btn_add_cost.animate().alpha(1f).setDuration(300);

            if (btn_again.getVisibility() != View.VISIBLE || btn_again.getVisibility() != GONE) {
                btn_again.setVisibility(View.VISIBLE);
            }
            btn_again.setAlpha(0f);
            btn_again.animate().alpha(1f).setDuration(300);
        }
    }

    private void stopCycle() {
        isTaskCancelled = true; // Устанавливаем флаг
        if (handlerStatus != null) {
            handlerStatus.removeCallbacks(myTaskStatus);
        }
    }

    private void startCycle() {
        if (!isTaskRunning && !isTaskCancelled) {
            handlerStatus.postDelayed(myTaskStatus, delayMillisStatus);
            delayMillisStatus = 10 * 1000;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timeUtils.stopTimer();
        stopCycle();
        if (handlerAddcost != null ) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }
        if (handlerCheckTask != null) {
            handlerCheckTask.removeCallbacks(checkTask);
        }
//        viewModel.clearOrderResponse();
//        viewModel.getTransactionStatus().removeObservers(getViewLifecycleOwner());
//        viewModel.getOrderResponse().removeObservers(getViewLifecycleOwner());
//        viewModel.isTenMinutesRemaining.removeObservers(getViewLifecycleOwner());
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }




    @Override
    public void onPause() {
        super.onPause();
        // Отменяем выполнение Runnable, если активити остановлена

        stopCycle();
        if (handlerAddcost != null ) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }
        if (handlerCheckTask != null) {
            handlerCheckTask.removeCallbacks(checkTask);
        }
//        viewModel.clearOrderResponse();
//        viewModel.getTransactionStatus().removeObservers(getViewLifecycleOwner());
//        viewModel.getOrderResponse().removeObservers(getViewLifecycleOwner());
//        viewModel.isTenMinutesRemaining.removeObservers(getViewLifecycleOwner());
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
        String url = baseUrl + "bonusBalance/recordsBloke/" + MainActivity.uid + "/" +  context.getString(R.string.application);
        Call<BonusResponse> call = ApiClient.getApiService().getBonus(url);
        Logger.d(context, TAG, "fetchBonus: " + url);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<BonusResponse> call, @NonNull Response<BonusResponse> response) {
                BonusResponse bonusResponse = response.body();
                if (response.isSuccessful() && response.body() != null) {

                    assert bonusResponse != null;
                    String bonus = String.valueOf(bonusResponse.getBonus());
                    String message = context.getString(R.string.block_mes) + " " + bonus + " " + context.getString(R.string.bon);

                    MyBottomSheetMessageFragment bottomSheetDialogFragment = new MyBottomSheetMessageFragment(message);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                } else {
                    MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());
                }
            }

            @Override
            public void onFailure(@NonNull Call<BonusResponse> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }

    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
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
        c.close();
        return list;
    }
    private void cancelOrder(String value, Context context) throws ParseException {

        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = baseUrl  + api + "/android/webordersCancel/" + value + "/" + city  + "/" +  context.getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrder(url);
        Logger.d(context, TAG, "cancelOrderWithDifferentValue cancelOrderUrl: " + url);
        text_status.setText(R.string.sent_cancel_message);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Status status = response.body();
                    Logger.d(context, TAG, "cancelOrderDouble status: " + status.toString());

                    // Проверка, содержит ли ответ фразу "не вдалося скасувати"
                    if (status.getResponse() != null && status.getResponse().contains("не вдалося скасувати")) {
                        Logger.d(context, TAG, "Ошибка: Не удалось отменить заказ");
                        // Здесь можно добавить дополнительную логику, например, показать сообщение пользователю
                        try {
                            cancelOrder(MainActivity.uid, context);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        Logger.d(context, TAG, "Заказ успешно обработан или другой ответ: " + status.getResponse());
                        // Логика для успешного ответа
                        String message = context.getString(R.string.ex_st_canceled);
                        orderCanceled(message);
                    }
                } else {
                    Logger.d(context, TAG, "Ошибка ответа: " + response.code());
                    // Обработка неуспешного ответа (например, HTTP-ошибки)
                    cancelOrderDouble(context);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(context, TAG, "onFailure: " + errorMessage);
                text_status.setText(R.string.verify_internet);
            }
        });

    }
    private void cancelOrderDouble(Context context) {
        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);

        baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") +"/";
        String url = baseUrl + api + "/android/webordersCancelDouble/" + MainActivity.uid + "/" + uid_Double + "/" + pay_method + "/" + city  + "/" +  context.getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrderDouble(url);
        Logger.d(context, TAG, "cancelOrderDouble: " + url);
        text_status.setText(R.string.sent_cancel_message);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Status status = response.body();
                    Logger.d(context, TAG, "cancelOrderDouble status: " + status.toString());

                    // Проверка, содержит ли ответ фразу "не вдалося скасувати"
                    if (status.getResponse() != null && status.getResponse().contains("не вдалося скасувати")) {
                        Logger.d(context, TAG, "Ошибка: Не удалось отменить заказ");
                        // Здесь можно добавить дополнительную логику, например, показать сообщение пользователю
                        cancelOrderDouble(context);
                    } else {
                        Logger.d(context, TAG, "Заказ успешно обработан или другой ответ: " + status.getResponse());
                        // Логика для успешного ответа
                        String message = context.getString(R.string.ex_st_canceled);
                        orderCanceled(message);
                    }
                } else {
                    Logger.d(context, TAG, "Ошибка ответа: " + response.code());
                    // Обработка неуспешного ответа (например, HTTP-ошибки)
                    cancelOrderDouble(context);
                }
            }


            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(context, TAG, "onFailure: " + errorMessage);
            }
        });

    }

    public void statusOrder() throws ParseException {

        btn_cancel_order.setVisibility(VISIBLE);
        btn_cancel_order.setEnabled(true);
        btn_cancel_order.setClickable(true);

        if(pay_method.equals("nal_payment")) {


            String value = MainActivity.uid;
            Logger.d(context, "Pusher", "statusCacheOrder: " + value);


            List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
            String city = listCity.get(1);
            String api = listCity.get(2);
            baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
            String url = baseUrl  + api + "/android/historyUIDStatusNew/" + value + "/" + city  + "/" +  context.getString(R.string.application);

            Call<OrderResponse> call = ApiClient.getApiService().statusOrder(url);
            Logger.d(context, TAG, "/android/historyUIDStatusNew/: " + url);

            // Выполняем запрос асинхронно
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Получаем объект OrderResponse из успешного ответа
                        OrderResponse orderResponse = response.body();

                        // Далее вы можете использовать полученные данные из orderResponse
                        // например:
                        String executionStatus = orderResponse.getExecutionStatus();

                        String orderCarInfo = orderResponse.getOrderCarInfo();
                        String driverPhone = orderResponse.getDriverPhone();

                        String time_to_start_point = orderResponse.getTimeToStartPoint();

                        int closeReason = orderResponse.getCloseReason();

                        Logger.d(context, TAG, "OrderResponse: closeReason " + closeReason);
                        Logger.d(context, TAG, "OrderResponse: executionStatus " + executionStatus);


                        if (time_to_start_point != null && !time_to_start_point.isEmpty()) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            try {
                                // Преобразуем строку в объект Date
                                Date date = dateFormat.parse(time_to_start_point);
                                assert date != null;
                                timeToStartMillis = date.getTime(); // Получаем время в миллисекундах
                                startCountdown(context);
                            } catch (ParseException e) {
                                return; // Выйти, если произошла ошибка парсинга
                            }
                        }

                        requireActivity().runOnUiThread(() -> closeReasonReactNal(
                                closeReason,
                                executionStatus,
                                driverPhone,
                                time_to_start_point,
                                orderCarInfo
                        ));
                    } else {
                        int closeReason = -1;
                        String executionStatus = "*";
                        String driverPhone = "*";
                        String time_to_start_point = "*";
                        String orderCarInfo = "*";
                        requireActivity().runOnUiThread(() -> closeReasonReactNal(
                                closeReason,
                                executionStatus,
                                driverPhone,
                                time_to_start_point,
                                orderCarInfo
                        ));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                    int closeReason = -1;
                    String executionStatus = "*";
                    String driverPhone = "*";
                    String time_to_start_point = "*";
                    String orderCarInfo = "*";
                    closeReasonReactNal(
                            closeReason,
                            executionStatus,
                            driverPhone,
                            time_to_start_point,
                            orderCarInfo
                    );
                }
            });
        } else {
            baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
            String url = baseUrl  + "getOrderStatusMessageResultPush/" + MainActivity.uid;

            Call<OrderResponse> call = ApiClient.getApiService().getOrderStatusMessageResultPush(url);
            Logger.d(context, TAG, "/getOrderStatusMessageResultPush/: " + url);

            // Выполняем запрос асинхронно
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Получаем объект OrderResponse из успешного ответа
                        OrderResponse orderResponse = response.body();
                        updateUICardPayStatus(orderResponse);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });
        }

    }


    private void orderComplete() {
        sharedPreferencesHelperMain.saveValue("carFound", true);
        new Handler(Looper.getMainLooper()).post(() -> {
            // Выполнено
            stopCycle();

            String message = context.getString(R.string.ex_st_finished);
            text_status.setText(message);
            text_status.clearAnimation();

            // Скрываем элементы

            setVisibility(
                    GONE,
                    btn_add_cost,
                    btn_open,
                    btn_options,
                    textCost,
                    textCostMessage,
                    carProgressBar,
                    progressSteps,
                    textStatusCar,
                    textCarMessage
            );

            // Показываем кнопку "Повторить"
            if (btn_again.getVisibility() != View.VISIBLE || btn_again.getVisibility() != GONE) {
                btn_again.setVisibility(View.VISIBLE);
            }

            if (btn_cancel_order.getVisibility() == View.VISIBLE) {
                btn_cancel_order.setVisibility(GONE);
            }
            // Отменяем все обработчики
            canceled = true;

            cancelAllHandlers(context);

            Logger.d(context, TAG, "orderComplete " + canceled);

            stopCycle();
        });
    }


    private void carSearch() {
        sharedPreferencesHelperMain.saveValue("carFound", false);
        new Handler(Looper.getMainLooper()).post(() -> {
            Logger.d(context, TAG, "carSearch() started");
            if (btn_cancel_order.getVisibility() != View.VISIBLE) {
                btn_cancel_order.setVisibility(VISIBLE);
            }

            if (cancel_btn_click) {
                Logger.d(context, TAG, "Order cancellation detected, stopping search...");
                cancelAllHandlers(context);
                setVisibility(GONE, btn_add_cost, carProgressBar);
                text_status.setText(context.getString(R.string.checkout_status));
                startCycle();
                return;
            }

        if (need_20_add && handlerAddcost != null && showDialogAddcost != null) {
            Logger.d(context, TAG, "Triggering add cost delay: " + timeCheckOutAddCost);
            handlerAddcost.postDelayed(showDialogAddcost, timeCheckOutAddCost);
            setVisibility(View.VISIBLE, textCost, textCostMessage, carProgressBar, progressSteps, btn_options, btn_open);
        }

            Logger.d(context, TAG, "Updating status and UI for car search");
            text_status.setText(context.getString(R.string.ex_st_0));
            carProgressBar.setVisibility(View.VISIBLE);
            text_status.startAnimation(blinkAnimation);
            updateProgress(2);
            countdownTextView.setVisibility(GONE);
            delayMillisStatus = 5 * 1000;

            setVisibility(GONE, textStatusCar, textCarMessage);
            setVisibility(VISIBLE, carProgressBar);
            Logger.d(context, TAG, "carSearch() completed");
        });
    }

    // Вспомогательный метод для отмены всех обработчиков
    private  void cancelAllHandlers(Context context) {
        if (handler != null) {
            Logger.d(context, TAG, "Removing myRunnable handler");
            handler.removeCallbacks(myRunnable);
        }
        if (handlerBonusBtn != null) {
            Logger.d(context, TAG, "Removing runnableBonusBtn handler");
            handlerBonusBtn.removeCallbacks(runnableBonusBtn);
        }
        if (handlerAddcost != null) {
            Logger.d(context, TAG, "Removing showDialogAddcost handler");
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }
        if (handlerCheckTask != null) {
            Logger.d(context, TAG, "Removing checkTask handler");
            handlerCheckTask.removeCallbacks(checkTask);
        }
    }



    private void setVisibility(int visibility, View... views) {
        for (View view : views) {
            view.setVisibility(visibility);
        }
    }


    private void carFound(
            int closeReason,
            String driverPhone,
            String time_to_start_point,
            String orderCarInfo
    ) {

        sharedPreferencesHelperMain.saveValue("carFound", true);
        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            text_status.clearAnimation();
            setVisibility(View.VISIBLE, textCost, textCostMessage);


//            if (btn_cancel_order.getVisibility() == View.VISIBLE) {
//                btn_cancel_order.setVisibility(GONE);
//            }

            List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
            String city = listCity.get(1);
            String api = listCity.get(2);

            if (closeReason == -1) {
                // Геокоординаты водителя по API
                drivercarposition(MainActivity.uid, city, api, context);
            } else {
                calculateTimeToStart(MainActivity.uid, api, context);
            }

            updateProgress(3);

            if (!cancel_btn_click) {
                delayMillisStatus = 5000;
                StringBuilder messageBuilder = new StringBuilder(context.getString(R.string.ex_st_2));

                if (!TextUtils.isEmpty(driverPhone)) {
                    Logger.d(context, TAG, "onResponse: driverPhone " + driverPhone);
                    btn_add_cost.setText(context.getString(R.string.phone_driver));
                    btn_add_cost.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + driverPhone));
                        context.startActivity(intent);
                    });
                    btn_add_cost.setVisibility(View.VISIBLE);
                } else {
                    btn_add_cost.setVisibility(GONE);
                }

                if (!TextUtils.isEmpty(time_to_start_point)) {
                    messageBuilder.append(context.getString(R.string.ex_st_5))
                            .append(formatDate2(time_to_start_point));
                }

                if (!TextUtils.isEmpty(orderCarInfo)) {
                    setVisibility(View.VISIBLE, textStatusCar, textCarMessage);
                    textCarMessage.setText(orderCarInfo);
                } else {
                    setVisibility(GONE, textStatusCar, textCarMessage);
                }

                countdownTextView.setVisibility(View.VISIBLE);
                text_status.setText(messageBuilder.toString());

            } else {
                text_status.setText(context.getString(R.string.ex_st_canceled));
            }
        });
    }

    private void orderCanceled(String message) {
//        new Handler(Looper.getMainLooper()).post(() -> {
            sharedPreferencesHelperMain.saveValue("carFound", false);
            text_status.setText(R.string.recounting_order);

            text_status.clearAnimation();
            canceled = true;
            MainActivity.action = null;

            // Скрываем ненужные элементы
            setVisibility(GONE, btn_add_cost, btn_open, btn_options, btn_cancel_order,
                    textStatusCar, textCarMessage, textCost,
                    textCostMessage, carProgressBar, progressSteps);

            // Останавливаем все обработчики
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


            Logger.d(context, TAG, "orderCanceled !!!! orderCanceled " + canceled);


            stopCycle();

            text_status.setText(message);
            btn_again.setVisibility(View.VISIBLE);
            if (btn_again.getVisibility() != View.VISIBLE || btn_again.getVisibility() != GONE) {
                btn_again.setVisibility(View.VISIBLE);
            }
//        });
    }

    private void orderCanceledError() {
        if(!text_status.getText().equals(context.getString(R.string.ex_st_canceled))) {

            cancel_btn_click = true;
            if(!uid_Double.equals(" ")) {
                cancelOrderDouble(context);

            } else{
                try {
                    cancelOrder(MainActivity.uid, context);
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }

            }
        }
        // Создание Handler
        Handler handlerCancelErr = new Handler();

        // Создание Runnable, который будет содержать метод
        Runnable runnable = () -> {
            // Запускаем метод
            if(!text_status.getText().equals(context.getString(R.string.ex_st_canceled))) {
                text_status.setText(R.string.cancel_error);

                text_status.clearAnimation();
                canceled = true;
                MainActivity.action = null;

                // Скрываем ненужные элементы
                setVisibility(GONE, btn_add_cost, btn_open, btn_options, btn_cancel_order,
                        textStatusCar, textCarMessage, textCost,
                        textCostMessage, carProgressBar, progressSteps);

                // Останавливаем все обработчики
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


                Logger.d(context, TAG, "orderCanceledError orderCanceled " + canceled);


                stopCycle();

                if (btn_again.getVisibility() != View.VISIBLE || btn_again.getVisibility() != GONE) {
                    btn_again.setVisibility(View.VISIBLE);
                }
            }
        };

        // Запускаем runnable через 60 секунд (60 000 миллисекунд)
        handlerCancelErr.postDelayed(runnable, 60000);


    }

    private void closeReasonReactNal(
            int closeReason,
            String executionStatus,
            String driverPhone,
            String time_to_start_point,
            String orderCarInfo
    ) {
        
        switch (closeReason) {
            case -1:
                switch (executionStatus) {
                    case "CarFound": //Найдено авто
                    case "Running": //Найдено авто
                        MainActivity.action = "Авто найдено";
                        carFound (
                            closeReason,
                            driverPhone,
                            time_to_start_point,
                            orderCarInfo
                        );
                        break;
                    case "Executed": //Выполнено
                        MainActivity.action = "Заказ выполнен";
                        orderComplete();
                        break;
                    default: //Поиск авто
                        MainActivity.action = "Поиск авто";
                        carSearch();
                }
                break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 9:
                if(executionStatus != null) {
                    // все статусы Отказ клиента
                    MainActivity.action = "Заказ снят";
                    String message = context.getString(R.string.ex_st_canceled);
                    orderCanceled(message);
                } else {
                    // Поиск авто
                    MainActivity.action = "Поиск авто";
                    carSearch();
                }
                break;
            case 0:
            case 8:
                if(executionStatus != null) {
                    // все статусы Выполнено
                    MainActivity.action = "Заказ выполнен";
                    orderComplete();
                } else {
                    // Поиск авто
                    MainActivity.action = "Поиск авто";
                    carSearch();
                }
                break;
        }
    }

    private void closeReasonReactCard(
            int closeReason,
            String driverPhone,
            String time_to_start_point,
            String orderCarInfo,
            String action
    ) {

        String message;
        switch (action) {
            case "Авто найдено":
                carFound(closeReason, driverPhone, time_to_start_point, orderCarInfo);
                break;
            case "Заказ выполнен":
                orderComplete();
                break;
            case "Заказ снят":

                message = context.getString(R.string.ex_st_canceled);
                orderCanceled(message);
                break;
            default:
                MainActivity.action = "Поиск авто";
                carSearch();
                break;
        }
    }

    void drivercarposition(String value, String city, String api,  Context context) {

        String url = baseUrl  + api + "/android/drivercarposition/" + value + "/" + city  + "/" +  context.getString(R.string.application);

        Call<Void> call = ApiClient.getApiService().drivercarposition(url);

        Logger.d(context, TAG, "/android/drivercarposition/: " + url);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }

    void calculateTimeToStart(String value, String api, Context context) {

        String url = baseUrl  + api + "/android/calculateTimeToStart/" + value;

        Call<Void> call = ApiClient.getApiService().calculateTimeToStart(url);

        Logger.d(context, TAG, "calculateTimeToStart: " + url);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }


    private void startCountdown(Context context) {
        long currentTimeMillis = System.currentTimeMillis();
        long remainingTime = timeToStartMillis - currentTimeMillis; // Вычисляем оставшееся время
        Logger.d(context, TAG, "remainingTime: " + remainingTime);
        if (remainingTime > 60000) {
            updateCountdownText(remainingTime, context);
        } else {
            updateCountdownText(60000, context);
        }
    }

    private void updateCountdownText(long millisUntilFinished, Context context) {

        int minutes = (int) ((millisUntilFinished / (1000 * 60)));
        Logger.d(context, TAG, "minutes " + minutes);
        // Обновляем текст с минутами и секундами
        String countdownText = context.getString(R.string.time_info_to_start_point) +  " " +  minutes +  context.getString(R.string.minutes);
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
        }

        assert date != null;
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


    @Subscribe(threadMode = ThreadMode.MAIN) // Обработка события в UI-потоке
    public void onCanceledStatusEvent(CanceledStatusEvent event) {
        String canceledStatus = event.getCanceledStatus();
        Log.d("EventBus", "Received canceled status: " + canceledStatus);
        // Обновление UI или выполнение действий
        Logger.d(context,"Pusher eventCanceled", "Finish eventCanceled status set: " + canceledStatus);
        if (canceledStatus != null) {
            if ("canceled".equals(canceledStatus)) {
                viewModel.getOrderResponse().removeObservers(getViewLifecycleOwner());
                String message = context.getString(R.string.ex_st_canceled);
                orderCanceled(message);
            }
        }
    }


//    @Subscribe(threadMode = ThreadMode.MAIN) // Выполнение в UI-потоке
//    public void onOrderResponseEvent(OrderResponseEvent event) {
//        OrderResponse response = event.getOrderResponse();
//        updateUICardPayStatus(response);
////        if (response != null) {
////            Logger.d(context, "Pusher 1111", "Finish Order updated: " + response.toString());
////            // Обновляем UI на основе новых данных
////            updateUICardPayStatus(response);
////        } else {
////            MainActivity.action = "Поиск авто";
////            int closeReason = 1;
////            String driverPhone = "*";
////            String time_to_start_point = "*";
////            String orderCarInfo = "*";
////
////            closeReasonReactCard(
////                    closeReason,
////                    driverPhone,
////                    time_to_start_point,
////                    orderCarInfo,
////                    MainActivity.action
////            );
////
////            Logger.d(context, TAG, "Received null OrderResponse in EventBus observer");
////        }
//    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModelReviewer();
    }
    public void viewModelReviewer() {
        // Инициализация ViewModel
        cancel_btn_click = false;
        // Наблюдение за статусом транзакции

        if(!paySystemStatus.equals("nal_payment")) {
            // Инициализация ViewModel

            viewModel.getTransactionStatus().removeObservers(getViewLifecycleOwner());
            viewModel.getTransactionStatus().observe(getViewLifecycleOwner(), status -> {
                Logger.d(context,"Pusher eventTransactionStatus", "Finish transaction status set: " + status);
                String declined_invoice = (String) sharedPreferencesHelperMain.getValue("declined_invoice", "**");
                Logger.d(context,"Pusher eventTransactionStatus", "Finish declined_invoice: " + declined_invoice);
                Logger.d(context,"Pusher eventTransactionStatus", "Finish MainActivity.order_id: " + MainActivity.order_id);
                if ("Declined".equals(status) && !declined_invoice.equals(MainActivity.order_id)) {
                    sharedPreferencesHelperMain.saveValue("declined_invoice", MainActivity.order_id);
                    handleTransactionStatusDeclined(status, context);
                    if (FinishSeparateFragment.btn_cancel_order != null) {
                        FinishSeparateFragment.btn_cancel_order.setVisibility(VISIBLE);
                        FinishSeparateFragment.btn_cancel_order.setEnabled(true);
                        FinishSeparateFragment.btn_cancel_order.setClickable(true);
                        Logger.d(context,"Pusher eventTransactionStatus", "Cancel button enabled successfully");
                    }

                }
            });

            viewModel.getCanceledStatus().removeObservers(getViewLifecycleOwner());
            viewModel.getCanceledStatus().observe(getViewLifecycleOwner(), status -> {
                Logger.d(context,"Pusher eventCanceled", "Finish eventCanceled status set: " + status);
                        if (status != null) {
                            if ("canceled".equals(status)) {

                                viewModel.getOrderResponse().removeObservers(getViewLifecycleOwner());
                                String message = context.getString(R.string.ex_st_canceled);
                                orderCanceled(message);
                            }
                        }
            });

            // Наблюдение за OrderResponse
//            viewModel.getOrderResponse().removeObservers(getViewLifecycleOwner());
//
//            viewModel.getOrderResponse().observe(getViewLifecycleOwner(), response -> {
//                if (response != null) {
//                    Logger.d(context, "Pusher 1111", "Finish Order updated: " + response.toString());
//                    // Обновляем UI на основе новых данных
//                    updateUICardPayStatus(response);
//                } else {
//                    MainActivity.action = "Поиск авто";
//                    int closeReason = 1;
//                    String driverPhone = "*";
//                    String time_to_start_point = "*";
//                    String orderCarInfo = "*";
//                    closeReasonReactCard(
//                            closeReason,
//                            driverPhone,
//                            time_to_start_point,
//                            orderCarInfo,
//                            MainActivity.action
//                    );
//
//                    Logger.d(context, TAG, "Received null OrderResponse in observer");
//                }
//            });
        }

        viewModel.setIsTenMinutesRemaining(false);

        observer = isTenMinutesRemaining -> {
            if (isTenMinutesRemaining != null) {
                if (isTenMinutesRemaining) {
                    isTenMinutesRemainingAction();
                }
            }
        };

        // Начинаем наблюдение
        viewModel.isTenMinutesRemaining.observe(getViewLifecycleOwner(), observer);


    }

    @Override
    public void onResume() {
        super.onResume();

        timeUtils = new TimeUtils(required_time);
        timeUtils.startTimer();

//        viewModelReviewer();
        btn_cancel_order.setOnClickListener(v -> showCancelDialog());

        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
        if(pay_method.equals("nal_payment")) {
            timeCheckOutAddCost = 60*1000;
        } else  {
            timeCheckOutAddCost =  75*1000;
        }

        Logger.d(context, TAG, "pay_method " + pay_method);

        addCheck(context);
        isTaskRunning = false;
        isTaskCancelled = false;
        startCycle();

        btn_open.setOnClickListener(v -> btnOpen());
        startAddCostDialog (timeCheckOutAddCost);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Отменяем выполнение Runnable, если фрагмент уходит в фон
        EventBus.getDefault().unregister(this); // Удаление подписчика
        stopCycle();

        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this); // Регистрация подписчика
        // Повторный запуск Runnable при возвращении активности
        if(MainActivity.action != null) {
            if( MainActivity.action.equals("Поиск авто")) {
                if (handler != null && myRunnable != null) {
                    handler.postDelayed(myRunnable, 10000); // Устанавливаем нужную задержку
                }

                isTaskRunning = false;
                isTaskCancelled = false;
                startCycle();

                if (handlerBonusBtn != null && runnableBonusBtn != null) {
                    handlerBonusBtn.postDelayed(runnableBonusBtn, 10000); // Устанавливаем нужную задержку
                }
            }
        }
    }
    private void startAddCostDialog (int timeCheckout) {
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
        Logger.d(context, TAG, "payMetod startAddCostDialog " + pay_method);

        showDialogAddcost = () -> {
            // Вызов метода для отображения диалога
            showAddCostDialog(timeCheckout);
        };

        boolean carfound = (boolean) sharedPreferencesHelperMain.getValue("carFound", false);
        if(!carfound) {
            Logger.e(context, TAG, "required_time +++" + required_time);
            if(required_time.contains("01.01.1970") || required_time.contains("1970-01-01") || required_time.isEmpty()) {
                need_20_add = true;
            } else {
                // Регулярное выражение для проверки формата даты "dd.MM.yyyy HH:mm"
        
                handlerCheckTask = new Handler(Looper.getMainLooper());
        
                checkTask = new Runnable() {
                    @Override
                    public void run() {
                        if (!required_time.isEmpty() ) {
                            if(!required_time.contains("01.01.1970") && !required_time.contains("1970-01-01")) {
                                required_time = TimeUtils.convertAndSubtractMinutes(required_time);
        
                                Logger.e(context, TAG, "required_time " + required_time);
        
                                @SuppressLint("SimpleDateFormat")
                                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        
                                try {
                                    Date requiredDate = inputFormat.parse(required_time);
                                    Date currentDate = new Date(); // Текущее время
        
                                    // Разница в миллисекундах
                                    assert requiredDate != null;
                                    long diffInMillis = requiredDate.getTime() - currentDate.getTime();
        
                                    // Конвертируем в минуты
                                    long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);
        
                                    if (diffInMinutes > 0 && diffInMinutes <= 30) {
                                        need_20_add = true; // Время ещё не наступило и в пределах 30 минут
                                    } else if (diffInMinutes > 30){
                                        need_20_add = false; // Время либо наступило, либо за пределами 30 минут
                                    }
        
                                } catch (ParseException e) {
                                    Logger.e(context, TAG, "requiredDate" +  e);
                                }
                            } else {
                                need_20_add = true; // Формат даты некорректен
                            }
        
                        } else {
                            need_20_add = true; // Формат даты некорректен
                        }
                        // Повторяем задачу через минуту, если окно активно
                        handlerCheckTask.postDelayed(this, 60000); // 60000 миллисекунд = 1 минута
                    }
                };
        
                // Запускаем задачу проверки
                handlerCheckTask.post(checkTask);
            }
            Logger.e(context, TAG, "status pay_method" + pay_method);
            Logger.e(context, TAG, "status need_20_add" + need_20_add);
            handlerAddcost = new Handler();
            if (need_20_add) {
                if ("nal_payment".equals(pay_method) || "wfp_payment".equals(pay_method)) {
                    Logger.e(context, TAG, "status pay_method" + pay_method);
                    Logger.e(context, TAG, "status need_20_add" + need_20_add);
        
                    // Запускаем выполнение через 1 минуты (60 000 миллисекунд)
                    handlerAddcost.postDelayed(showDialogAddcost, timeCheckout);
                }
            }
        }
       


        btn_add_cost.setOnClickListener( view -> {

                if ("nal_payment".equals(pay_method)) {

                    // Запускаем выполнение через 1 минуты (60 000 миллисекунд)
                    if (handlerAddcost != null) {
                        handlerAddcost.postDelayed(showDialogAddcost, timeCheckout);
                    }

                    String text = textCostMessage.getText().toString();
                    Logger.d(getActivity(), TAG, "textCostMessage.getText().toString() " + text);

                    Pattern pattern = Pattern.compile("(\\d+)");
                    Matcher matcher = pattern.matcher(text);

                    if (matcher.find()) {
                        Logger.d(context, TAG, "amount_to_add: " + matcher.group(1));
                        stopCycle();
                        MyBottomSheetAddCostFragment bottomSheetDialogFragment = new MyBottomSheetAddCostFragment(
                                matcher.group(1),
                                MainActivity.uid,
                                uid_Double,
                                pay_method,
                                context,
                                fragmentManager
                        );
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    } else {
                        Logger.d(context, TAG, "No numeric value found in the text.");
                    }
                }  else if ("wfp_payment".equals(pay_method)) {

                     verifyOldHold();


                } else if ("bonus_payment".equals(pay_method)) {

                    String message = context.getString(R.string.addCostBonusMessage);
                    MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context, message);
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());


                }
            });
    }

    private void verifyOldHold() {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Создание клиента OkHttpClient с подключенным логгером
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(loggingInterceptor);
        httpClient.connectTimeout(60, TimeUnit.SECONDS); // Тайм-аут для соединения
        httpClient.readTimeout(60, TimeUnit.SECONDS);    // Тайм-аут для чтения
        httpClient.writeTimeout(60, TimeUnit.SECONDS);   // Тайм-аут для записи

        String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build()) // Подключение клиента OkHttpClient с логгером
                .build();


            APIHoldService apiService = retrofit.create(APIHoldService.class);
            Call<HoldResponse> call = apiService.verifyHold(MainActivity.uid);

            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<HoldResponse> call, @NonNull Response<HoldResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        HoldResponse holdResponse = response.body();
                        String result = holdResponse.getResult();
                        Logger.d(context, TAG, "verifyOldHold  result: " + result);
                        if (result.equals("hold")) {
                            // Обработка неуспешного ответа
                            new Handler(Looper.getMainLooper()).post(() -> {
                                // Запускаем выполнение через 1 минуты (60 000 миллисекунд)
                                if (handlerAddcost != null) {
                                    handlerAddcost.postDelayed(showDialogAddcost, timeCheckOutAddCost);
                                }

                                String text = textCostMessage.getText().toString();
                                Logger.d(getActivity(), TAG, "textCostMessage.getText().toString() " + text);

                                Pattern pattern = Pattern.compile("(\\d+)");
                                Matcher matcher = pattern.matcher(text);

                                if (matcher.find()) {
                                    Logger.d(context, TAG, "amount_to_add: " + matcher.group(1));
                                    stopCycle();
                                    MyBottomSheetAddCostFragment bottomSheetDialogFragment = new MyBottomSheetAddCostFragment(
                                            matcher.group(1),
                                            MainActivity.uid,
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

                        } else {
                            new Handler(Looper.getMainLooper()).post(() -> text_status.setText(R.string.recounting_order));

                        }

                    } else {
                        // Обработка неуспешного ответа
                        new Handler(Looper.getMainLooper()).post(() -> text_status.setText(R.string.recounting_order));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<HoldResponse> call, @NonNull Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });



    }

    private void showAddCostDialog(int timeCheckout) {
        // Убедитесь, что handlerAddcost не null и очищаем предыдущие задачи
        if (handlerAddcost != null) {
            handlerAddcost.removeCallbacks(showDialogAddcost);
        }

        stopCycle();
        // Убедитесь, что фрагмент добавлен

        if (!isAdded() || getActivity() == null) {
            return;
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_cost, null);

        builder.setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                    // Действие для кнопки "OK"
                    if ("wfp_payment".equals(pay_method)) {
                        verifyOldHold();
                    }
                    if ("nal_payment".equals(pay_method)) {
                        if (FinishSeparateFragment.btn_cancel_order != null) {
                            FinishSeparateFragment.btn_cancel_order.setEnabled(false);
                            FinishSeparateFragment.btn_cancel_order.setClickable(false);
                        } else {
                            Log.e("Pusher", "btn_cancel_order is null!");
                        }
                        // Перезапускаем задачу
                        if (handlerAddcost != null) {
                            handlerAddcost.postDelayed(showDialogAddcost, timeCheckout);
                        }
                        dialog.dismiss();
                        String text = textCostMessage.getText().toString();
                        Logger.d(getActivity(), TAG, "textCostMessage.getText().toString() " + text);

                        Pattern pattern = Pattern.compile("(\\d+)");
                        Matcher matcher = pattern.matcher(text);

                        if (matcher.find()) {
                            Logger.d(context, TAG, "amount_to_add: " + matcher.group(1));
                            stopCycle();
                            MyBottomSheetAddCostFragment bottomSheetDialogFragment = new MyBottomSheetAddCostFragment(
                                    matcher.group(1),
                                    MainActivity.uid,
                                    uid_Double,
                                    pay_method,
                                    context,
                                    fragmentManager
                            );
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        } else {
                            Logger.d(context, TAG, "No numeric value found in the text.");
                        }
                    }
//                    startAddCostUpdate();

                })
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> {
                    // Действие для кнопки "Отмена"
                    if (handlerAddcost != null) {
                        handlerAddcost.postDelayed(showDialogAddcost, timeCheckout);
                    }
                    handlerStatus.post(myTaskStatus);
                     dialog.dismiss();
                });

        AlertDialog dialog = builder.create();

        dialog.show();

        // Настройка цветов кнопок
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
            positiveButton.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            ViewParent buttonPanel = positiveButton.getParent();
            if (buttonPanel instanceof ViewGroup) {
                ((ViewGroup) buttonPanel).setBackgroundColor(ContextCompat.getColor(context, R.color.background_color_new));
            }

        }
        if (negativeButton != null) {
            negativeButton.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_text_color_2));
            negativeButton.setTextColor(ContextCompat.getColor(context, android.R.color.white));

        }
    }

    private void showCancelDialog() {

        if (!btn_cancel_order.isEnabled()) {
            Toast.makeText(context, R.string.cancel_btn_enable, Toast.LENGTH_LONG).show();
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_add_cost, null);
            TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
            dialogTitle.setText(context.getString(R.string.add_cost_cancel));
            builder.setView(dialogView)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                        // Действие для кнопки "OK"
                        cancel_btn_click = true;
                        if(!uid_Double.equals(" ")) {
                            cancelOrderDouble(context);

                        } else{
                            try {
                                cancelOrder(MainActivity.uid, context);
                            } catch (ParseException e) {
                                throw new RuntimeException(e);
                            }

                        }
//                        cancel_30();
                        dialog.dismiss();


                    })
                    .setNegativeButton(R.string.cancel_button, (dialog, which) -> {
                        // Действие для кнопки "Отмена"
                        dialog.dismiss();
                    });

            AlertDialog dialog = builder.create();

            dialog.show();

            // Настройка цветов кнопок
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

            if (positiveButton != null) {
                positiveButton.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_text_color));
                positiveButton.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                ViewParent buttonPanel = positiveButton.getParent();
                if (buttonPanel instanceof ViewGroup) {
                    ((ViewGroup) buttonPanel).setBackgroundColor(ContextCompat.getColor(context, R.color.background_color_new));
                }

            }
            if (negativeButton != null) {
                negativeButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
                negativeButton.setTextColor(ContextCompat.getColor(context, android.R.color.white));

            }
        }
    }

    private void cancel_30() {
        // Создание Handler
        Handler handlerErr = new Handler();

        // Создание Runnable, который будет содержать метод
        // Запускаем метод
        Runnable runnable = this::orderCanceledError;

        // Запускаем runnable через 30 секунд (30 000 миллисекунд)
        handlerErr.postDelayed(runnable, 30000);

    }

    private  void isTenMinutesRemainingAction() {
        timeUtils.stopTimer();
        viewModel.isTenMinutesRemaining.removeObserver(observer);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);


        LayoutInflater inflater = LayoutInflater.from(context); // Fallback if null

        View dialogView = inflater.inflate(R.layout.dialog_add_cost, null);

        // Настройка текста с выделенным числом
        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText(R.string.time_car_found);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        TextView messageView = dialogView.findViewById(R.id.dialogMessage);
        String messageText = context.getString(R.string.cancel_car_found_time);
        messageView.setText(messageText);

        builder.setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                    dialog.dismiss();
                    if(!uid_Double.equals(" ")) {
                        cancelOrderDouble(context);
                    } else{
                        try {
                            cancelOrder(MainActivity.uid, context);
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }



                })
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.show();

        // Настройка цветов кнопок
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
            positiveButton.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            ViewParent buttonPanel = positiveButton.getParent();
            if (buttonPanel instanceof ViewGroup) {
                ((ViewGroup) buttonPanel).setBackgroundColor(ContextCompat.getColor(context, R.color.background_color_new));
            }

        }
        if (negativeButton != null) {
            negativeButton.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_text_color_2));
            negativeButton.setTextColor(ContextCompat.getColor(context, android.R.color.white));

        }
    }
    private void addCheck(Context context) {

        int newCheck = 0;
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        for (int i = 0; i < DataArr.arrayServiceCode().length; i++) {
            if (services.get(i + 1).equals("1")) {
                newCheck++;
            }
        }
        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif = stringListInfo.get(2);
        if (!tarif.equals(" ")) {
            newCheck++;
        }
        Log.d("comment_info", "comment_info " + comment_info);

        if (!comment_info.equals("no_comment") && !comment_info.isEmpty()) {
            newCheck++;
        }

        String mes = context.getString(R.string.add_services);
        if (newCheck != 0) {
            mes = context.getString(R.string.add_services) + " (" + newCheck + ")";
        }
        btn_options.setText(mes);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logMemoryState();
        sharedPreferencesHelperMain.saveValue("carFound", false);
    }

    private void logMemoryState() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memoryInfo);

        // Формирование отчета о состоянии памяти
        String memoryReport = context.getString(R.string.low_memory_2) +
                context.getString(R.string.low_memory_3) + memoryInfo.availMem + context.getString(R.string.low_memory_6) +
                context.getString(R.string.low_memory_4) + memoryInfo.threshold + context.getString(R.string.low_memory_6) +
                context.getString(R.string.low_memory_5) + memoryInfo.lowMemory;

        // Логирование отчета
        Logger.d(context, "MemoryReport", memoryReport);

        if (memoryInfo.lowMemory) {
                Toast.makeText(context, memoryReport, Toast.LENGTH_LONG).show();
        }

    }


}