package com.taxi.easy.ua.ui.finish.fragm;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.TABLE_USER_INFO;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.MainActivity.paySystemStatus;
import static com.taxi.easy.ua.MainActivity.uid;
import static com.taxi.easy.ua.MainActivity.uid_Double;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
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
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.FragmentFinishSeparateBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.BonusResponse;
import com.taxi.easy.ua.ui.finish.FinishCostResponse;
import com.taxi.easy.ua.ui.finish.OrderResponse;
import com.taxi.easy.ua.ui.finish.Status;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusResponse;
import com.taxi.easy.ua.ui.wfp.checkStatus.StatusService;
import com.taxi.easy.ua.ui.weather.finish.PassengerNotifier;
import com.taxi.easy.ua.utils.animation.car.CarProgressBar;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetAddCostFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorPaymentFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetFinishOptionFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetMessageFragment;
import com.taxi.easy.ua.utils.dialog.UklonAlertDialog;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.hold.APIHoldService;
import com.taxi.easy.ua.utils.hold.HoldResponse;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.notify.NotificationHelper;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.utils.order.EarlyOrderNavigationHelper;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.phone_state.PhoneCallHelper;
import com.taxi.easy.ua.utils.pusher.events.AddCostUpdateEvent;
import com.taxi.easy.ua.utils.pusher.events.CanceledStatusEvent;
import com.taxi.easy.ua.utils.payment.PaymentDeclinedNotifier;
import com.taxi.easy.ua.utils.payment.PaymentTypeHelper;
import com.taxi.easy.ua.utils.payment.PaymentErrorSheetHelper;
import com.taxi.easy.ua.utils.payment.PaymentSessionHelper;
import com.taxi.easy.ua.utils.payment.PendingTransactionHelper;
import com.taxi.easy.ua.utils.review.AppReviewManager;
import com.taxi.easy.ua.utils.time_ut.TimeUtils;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;
import com.uxcam.UXCam;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import com.taxi.easy.ua.utils.db.CursorReadHelper;

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
    public static Handler handler, handlerBonusBtn;
    public static Handler handlerStatus;
    public static Runnable myTaskStatus;

    String email;

    public static String phoneNumber;
    boolean cancel_btn_click = false;
    private boolean cancelRequestInFlight = false;
    @Nullable
    private Call<Status> activeCancelCall;
    long delayMillisStatus;
    boolean no_pay;
    boolean canceled = false;
    private int paymentCheckGeneration;
    private boolean paymentCheckInProgress;
    private Runnable pendingPaymentErrorSheetShow;
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
    private boolean statusPollPaused = false;
    /** Опрос статуса после сбоя cancel HTTP — не возвращать UI «ищем авто» сразу. */
    private int cancelFailureWatchRemaining = 0;
    private Runnable cancelWatchPoll;
    /** UID заказа, выбранного при открытии экрана (список «В работе» или новый заказ). */
    @Nullable
    private String navigationOrderUid;

    TimeUtils timeUtils;
    private Observer<Boolean> observer;
    private WeakReference<Activity> activityRef;
    private Call<OrderResponse> retrofitCall;
    private Call<StatusResponse> wfpStatusCheckCall;
    private Call<HoldResponse> holdVerifyCall;
    private int holdCheckGeneration;
    private boolean holdVerifiedOnEnter;
    @Nullable
    private String lastKnownPaymentStatus;
    @Nullable
    private String lastExecutionStatus;

    private ExecutionStatusViewModel viewModel;
    private String action;
    long delayMillis = 5 * 60 * 1000;
//    long delayMillis = 30 * 1000;
    private String pendingAddCost = "0";
    private boolean isTaskScheduled = false; // Флаг для отслеживания
    private static final String TAG_ADD_COST_SHEET = "add_cost_sheet";
    private AlertDialog addCostDialog;
    private boolean addCostSheetShowing = false;
    private boolean activeOrderCloseMode = false;
    private PassengerNotifier notifier;
    private Handler checkHandler = new Handler();
    private Runnable checkRunnable;
    private static final long ADD_COST_SLOW_NOTICE_MS = 20_000L;
    private static final long ADD_COST_TIMEOUT_MS = 120_000L;
    private final Handler addCostNoticeHandler = new Handler(Looper.getMainLooper());
    private final Runnable addCostSlowNoticeRunnable = this::showAddCostSlowNotice;
    private final Runnable addCostTimeoutRunnable = this::handleAddCostTimeout;
    @Nullable
    private Call<StatusResponse> addCostStatusCheckCall;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Инициализация ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(ExecutionStatusViewModel.class);
    }


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

        if(button1 != null) {
            button1.setVisibility(GONE);
        }


        fragmentManager = getParentFragmentManager();

        action = null;
      

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

        Bundle arguments = getArguments();
        assert arguments != null;


        String no_pay_key = arguments.getString("card_payment_key");
        receivedMap = (HashMap<String, String>) arguments.getSerializable("sendUrlMap");


        assert receivedMap != null;

        flexible_tariff_name = Objects.requireNonNull(receivedMap).get("flexible_tariff_name");

        if (receivedMap.get("pay_method") != null) {
            pay_method = receivedMap.get("pay_method");
        } else {
            List<String> cursorResult = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
            if (cursorResult.size() > 4) {
                pay_method = cursorResult.get(4);
            } else {
                // Установите значение по умолчанию или обработайте ошибку
                pay_method = "default_payment_method"; // Например
            }
        }

        assert pay_method != null;



        if(pay_method.equals("nal_payment")) {
            timeCheckOutAddCost = 60*1000;
        } else  {
            timeCheckOutAddCost =  75*1000;
        }

        Logger.d(context, TAG, "pay_method " + pay_method);

        AppCompatButton btnCallAdmin = root.findViewById(R.id.btnCallAdmin);
        btnCallAdmin.setOnClickListener(v -> {
            PhoneCallHelper.callWithFallback(() -> {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, MyApplication.getContext());
                return stringList.size() > 3 ? stringList.get(3) : "";
            });
//            Intent intent = new Intent(Intent.ACTION_DIAL);
//            List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
//            String phone = stringList.get(3);
//            intent.setData(Uri.parse(phone));
//            startActivity(intent);
        });
        messageFondy =  context.getString(R.string.fondy_message);
        email = logCursor(TABLE_USER_INFO, context).get(3);
        phoneNumber = logCursor(TABLE_USER_INFO, context).get(2);



        Logger.d(context, TAG, "no_pay: key " + no_pay_key);

        no_pay = no_pay_key != null && no_pay_key.equals("no");
        Logger.d(context, TAG, "no_pay: " + no_pay);




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
        text_full_message.setOnClickListener(view -> startPointShow());

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
        textCarMessage.setOnClickListener(v -> {
            TextView textView = (TextView) v;
            String textToCopy = textView.getText().toString();

            if (!textToCopy.isEmpty()) {
                // Анимация нажатия
                v.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                v.animate()
                                        .scaleX(1f)
                                        .scaleY(1f)
                                        .setDuration(100)
                                        .start();
                            }
                        }).start();

                // Копирование в буфер обмена
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("CarStatus", textToCopy);
                clipboard.setPrimaryClip(clip);

                // Показываем Snackbar (если используете)
                if (root != null) {
                    Snackbar.make(root, R.string.save_text, Snackbar.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), R.string.save_text, Toast.LENGTH_SHORT).show();
                }

                // Временно меняем цвет текста для обратной связи
                int originalColor = textView.getCurrentTextColor();
                textView.setTextColor(Color.parseColor("#4CAF50")); // Зеленый цвет
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        textView.setTextColor(originalColor);
                    }
                }, 200);

            } else {
                Toast.makeText(requireContext(), R.string.no_text_to_save, Toast.LENGTH_SHORT).show();
            }
        });
        textCarMessage.setVisibility(GONE);

        uid = arguments.getString("UID_key");
        navigationOrderUid = uid;

        Logger.d(context, TAG, "MainActivity.uid: " + uid);

        if (uid != null && MainActivity.order_id != null && !MainActivity.order_id.isEmpty()) {
            String savedRef = PaymentSessionHelper.getWfpOrderRef(uid);
            if (savedRef == null) {
                PaymentSessionHelper.saveWfpOrderRef(uid, MainActivity.order_id);
            } else {
                MainActivity.order_id = savedRef;
            }
        }

        uid_Double = receivedMap.get("dispatching_order_uid_Double");

        reconcileOrderIdentityFromPersistedState();
        if (uid != null && !uid.isEmpty()) {
            ExecutionStatusViewModel.resetNewOrderSession(uid);
        }

        text_status = root.findViewById(R.id.text_status);
        if (isViewingCompletedOrder()) {
            text_status.setText(context.getString(R.string.ex_st_finished));
        } else {
            text_status.setText(context.getString(R.string.ex_st_0));
            text_status.startAnimation(blinkAnimation);
        }


        btn_add_cost = root.findViewById(R.id.btn_add_cost);


        btn_cancel_order = root.findViewById(R.id.btn_cancel_order);





        if (PaymentTypeHelper.usesWalletHold(pay_method)) {
            amount = receivedMap.get("order_cost");
        }
        if (pay_method.equals("fondy_payment")) {
            amount = receivedMap.get("order_cost") + "00";
        }

        if (pay_method.equals("bonus_payment") && !no_pay) {
            handlerBonusBtn = new Handler(Looper.getMainLooper());
            fetchBonus();
        }

        handler = new Handler(Looper.getMainLooper());

//        if (pay_method.equals("bonus_payment") || pay_method.equals("wfp_payment") || pay_method.equals("fondy_payment") || pay_method.equals("mono_payment") ) {
//            handlerBonusBtn = new Handler(Looper.getMainLooper());
//
//            runnableBonusBtn = () -> {
//                MainActivity.order_id = null;
//                String newStatus = text_status.getText().toString();
//                if(!newStatus.contains( context.getString(R.string.time_out_text))
//                        || !newStatus.contains( context.getString(R.string.error_payment_card))
//                        || !newStatus.contains( context.getString(R.string.double_order_error))
//                        || !newStatus.contains( context.getString(R.string.call_btn_cancel))
//                        || !newStatus.contains( context.getString(R.string.ex_st_canceled))
//                ) {
//                    String cancelText = context.getString(R.string.status_checkout_message);
//                    text_status.setText(cancelText);
//
//                } else {
//                    text_status.setText(newStatus);
//                }
//
//            };
//            handlerBonusBtn.postDelayed(runnableBonusBtn, delayMillis);
//        }

//        handlerStatus = new Handler(Looper.getMainLooper());
        handlerStatus = HandlerCompat.createAsync(Looper.getMainLooper());
        delayMillisStatus = 5 * 1000;
        myTaskStatus = new Runnable() {
            @Override
            public void run() {
                // Ваш код
                isTaskRunning = true;
                try {
                    statusOrder();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                } finally {
                    isTaskRunning = false; // Сбрасываем флаг после выполнения
                    // Планируем следующий запуск, если не отменено
//                    if (!isTaskCancelled && isAdded()) {
//                        handlerStatus.postDelayed(this, delayMillisStatus);
//                    }
//                    HandlerCompat.postDelayed(handlerAddcost, showDialogAddcost, null, timeCheckOutAddCost);
//                    startCycle();
                    if (!isTaskCancelled && !statusPollPaused && isAdded()) {
                        // Планируем следующий запуск с задержкой
                        HandlerCompat.postDelayed(handlerStatus, this, null, delayMillisStatus);
                    }
                }
            }
        };

        cancelWatchPoll = () -> {
            if (!isAdded() || canceled) {
                return;
            }
            if (!statusPollPaused) {
                return;
            }
            try {
                statusOrder();
            } catch (ParseException e) {
                Logger.e(context, TAG, "cancelWatchPoll: " + e.getMessage());
            }
            if (canceled || !isAdded() || handlerStatus == null) {
                return;
            }
            if (cancelRequestInFlight) {
                scheduleCancelWatchPoll();
                return;
            }
            if (cancelFailureWatchRemaining > 0) {
                cancelFailureWatchRemaining--;
                scheduleCancelWatchPoll();
                return;
            }
            resumeStatusPollingAfterCancelFailure();
        };

        btn_again = root.findViewById(R.id.btn_again);
        btn_again.setOnClickListener(v -> navigateToNewOrder());


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

        notifier = new PassengerNotifier(context);

        // Когда начинаете поиск машины:
        notifier.onSearchStarted();
        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);

        Logger.d(context, "PassengerNotifier", "city " + city);
        // Проверка через 1 секунду
        checkHandler.postDelayed(() -> {
            if (!isAdded() || notifier == null) {
                return;
            }
            String orderUid = resolveActiveOrderUid();
            if (PassengerNotifier.isWeatherAlreadyShownForOrder(orderUid)) {
                return;
            }
            notifier.checkAndNotify(context, city, orderUid);
        }, 1000);


        return root;
    }



    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activityRef = new WeakReference<>((Activity) context);
    }

     void updateUICardPayStatus(
            OrderResponse orderResponse
    ) {
        assert orderResponse != null;
        if (shouldIgnoreStatusPollingUi()) {
            Logger.d(context, TAG, "updateUICardPayStatus ignored: terminal/cancel UI");
            return;
        }

        String orderCarInfo = orderResponse.getOrderCarInfo();
        String driverPhone = orderResponse.getDriverPhone();

        String time_to_start_point = orderResponse.getTimeToStartPoint();

       action = resolveActionFromOrderResponse(orderResponse);
        lastExecutionStatus = orderResponse.getExecutionStatus();
        if (isOrderDispatched()) {
            clearDeclinedPaymentUi();
        }

        int closeReason = orderResponse.getCloseReason();

        Logger.d(context, TAG, "OrderResponse: action " +action + ", closeReason " + closeReason);
        if (action == null && isExternalApiCompletedCloseReason(closeReason)) {
            if (isExecutedExecutionStatus(orderResponse.getExecutionStatus())) {
                action = "Заказ выполнен";
            } else {
                action = "Поиск авто";
            }
        }
        if(action != null) {
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
                    action
            );
        }

    }

    @Nullable
    private String resolveActionFromOrderResponse(@NonNull OrderResponse orderResponse) {
        String resolvedAction = orderResponse.getAction();
        if (resolvedAction != null && !resolvedAction.isEmpty()) {
            return resolvedAction;
        }
        String executionStatus = orderResponse.getExecutionStatus();
        if (executionStatus == null) {
            return "Поиск авто";
        }
        switch (executionStatus) {
            case "CarFound":
                return "Авто найдено";
            case "WaitingAtAddress":
            case "AtAddress":
                return "На месте";
            case "Running":
            case "InRoute":
                return "В пути";
            case "Executed":
                return "Заказ выполнен";
            case "Canceled":
                return "Заказ снят";
            case "SearchesForCar":
            case "WaitingCarSearch":
            default:
                return "Поиск авто";
        }
    }

    public void handleTransactionStatusDeclined(String status, Context context) {
        Logger.d(context, TAG, "Transaction Status: " + status);
        if ("Declined".equals(status)) {
            confirmDeclinedWithServerCheck();
        }
    }

    private boolean isCardPayMethod() {
        return "wfp_payment".equals(pay_method)
                || "fondy_payment".equals(pay_method)
                || "mono_payment".equals(pay_method)
                || "card_payment".equals(pay_method);
    }

    private static boolean isApprovedPaymentStatus(@Nullable String status) {
        return "Approved".equals(status) || "WaitingAuthComplete".equals(status);
    }

    /** Hold на сервере или Approved/WaitingAuthComplete от WFP. */
    private boolean isPaymentVerified() {
        if (holdVerifiedOnEnter) {
            return true;
        }
        if (isApprovedPaymentStatus(lastKnownPaymentStatus)) {
            return true;
        }
        if (viewModel != null && isApprovedPaymentStatus(viewModel.getTransactionStatus().getValue())) {
            return true;
        }
        return false;
    }

    /** Заказ уже в работе — оплата не может считаться «зависшей». */
    private boolean isOrderDispatched() {
        if ((boolean) sharedPreferencesHelperMain.getValue("carFound", false)) {
            return true;
        }
        if (lastExecutionStatus != null) {
            switch (lastExecutionStatus) {
                case "CarFound":
                case "Running":
                case "Executed":
                    return true;
            }
        }
        if (action != null) {
            switch (action) {
                case "Авто найдено":
                case "На месте":
                case "В пути":
                case "Заказ выполнен":
                    return true;
            }
        }
        return false;
    }

    private static boolean isTerminalOrderAction(@Nullable String orderAction) {
        if (orderAction == null) {
            return false;
        }
        switch (orderAction) {
            case "Авто найдено":
            case "На месте":
            case "В пути":
            case "Заказ выполнен":
            case "Заказ снят":
                return true;
            default:
                return false;
        }
    }

    /** Не откатывать UI с «авто найдено» / «в пути» обратно на «поиск авто». */
    private boolean shouldIgnoreStatusDowngrade(@Nullable String incomingAction) {
        return isTerminalOrderAction(action)
                && "Поиск авто".equals(incomingAction);
    }

    /** Тост «Доплата не прошла» при неудачной доплате картой. */
    private void notifyAddCostPaymentFailed() {
        if (!ExecutionStatusViewModel.isAddCostInFlightPref()
                && ExecutionStatusViewModel.getPendingAddCostAmountPref() == null) {
            return;
        }
        showAddCostPaymentFailedUi();
    }

    private void scheduleAddCostProcessingNotices() {
        cancelAddCostProcessingNotices();
        if (!ExecutionStatusViewModel.isAddCostInFlightPref()) {
            return;
        }
        addCostNoticeHandler.postDelayed(addCostSlowNoticeRunnable, ADD_COST_SLOW_NOTICE_MS);
        addCostNoticeHandler.postDelayed(addCostTimeoutRunnable, ADD_COST_TIMEOUT_MS);
    }

    private void cancelAddCostProcessingNotices() {
        addCostNoticeHandler.removeCallbacks(addCostSlowNoticeRunnable);
        addCostNoticeHandler.removeCallbacks(addCostTimeoutRunnable);
        if (addCostStatusCheckCall != null && !addCostStatusCheckCall.isCanceled()) {
            addCostStatusCheckCall.cancel();
        }
    }

    private void showAddCostProcessingNotice() {
        if (!isAdded() || context == null || !ExecutionStatusViewModel.isAddCostInFlightPref()) {
            return;
        }
        String message = context.getString(R.string.add_cost_processing);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        if (text_status != null) {
            text_status.setText(message + ". " + context.getString(R.string.cancel_btn_enable));
        }
    }

    private void showAddCostSlowNotice() {
        if (!isAdded() || context == null || !ExecutionStatusViewModel.isAddCostInFlightPref()) {
            return;
        }
        String message = context.getString(R.string.add_cost_processing_slow);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        if (text_status != null) {
            text_status.setText(message);
        }
    }

    private void handleAddCostTimeout() {
        if (!isAdded() || !ExecutionStatusViewModel.isAddCostInFlightPref()) {
            return;
        }
        Logger.w(context, TAG, "Add-cost watchdog timeout — checking payment status");
        checkPendingAddCostPaymentStatus();
    }

    private void checkPendingAddCostPaymentStatus() {
        if (!isAdded() || context == null) {
            return;
        }
        String orderRef = ExecutionStatusViewModel.getPendingAddCostOrderRefPref();
        if (orderRef == null || orderRef.isEmpty()) {
            orderRef = MainActivity.order_id;
        }
        if (orderRef == null || orderRef.isEmpty()) {
            orderRef = resolveWfpOrderReference();
        }
        if (orderRef == null || orderRef.isEmpty()) {
            showAddCostPaymentFailedUi();
            return;
        }
        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        if (listCity.size() < 2) {
            showAddCostPaymentFailedUi();
            return;
        }
        String city = listCity.get(1);
        String baseUrlValue = (String) sharedPreferencesHelperMain.getValue(
                "baseUrl", "https://m.easy-order-taxi.site");
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrlValue + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        StatusService service = retrofit.create(StatusService.class);
        if (addCostStatusCheckCall != null && !addCostStatusCheckCall.isCanceled()) {
            addCostStatusCheckCall.cancel();
        }
        addCostStatusCheckCall = service.checkStatus(
                context.getString(R.string.application),
                city,
                orderRef
        );
        addCostStatusCheckCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<StatusResponse> call,
                                   @NonNull Response<StatusResponse> response) {
                if (!isAdded() || !ExecutionStatusViewModel.isAddCostInFlightPref()) {
                    return;
                }
                String orderStatus = response.body() != null
                        ? response.body().getTransactionStatus()
                        : null;
                Logger.d(context, TAG, "Add-cost checkStatus: " + orderStatus);
                if (isApprovedPaymentStatus(orderStatus)) {
                    cancelAddCostProcessingNotices();
                    String pendingAmount = ExecutionStatusViewModel.getPendingAddCostAmountPref();
                    ExecutionStatusViewModel.setAddCostInFlightPref(false);
                    ExecutionStatusViewModel.clearPendingAddCostAmountPref();
                    if (pendingAmount != null && !pendingAmount.equals("0")
                            && !PaymentTypeHelper.usesWalletHold(pay_method)) {
                        viewModel.setAddCostViewUpdate(pendingAmount);
                    }
                    viewModel.setCancelStatus(true);
                    return;
                }
                if ("InProcessing".equals(orderStatus) || "Pending".equals(orderStatus)) {
                    showAddCostSlowNotice();
                    addCostNoticeHandler.postDelayed(addCostTimeoutRunnable, 60_000L);
                    return;
                }
                showAddCostPaymentFailedUi();
            }

            @Override
            public void onFailure(@NonNull Call<StatusResponse> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) {
                    return;
                }
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.w(context, TAG, "Add-cost checkStatus failed: " + t.getMessage());
                showAddCostPaymentFailedUi();
            }
        });
    }

    private void showAddCostPaymentFailedUi() {
        cancelAddCostProcessingNotices();
        ExecutionStatusViewModel.setAddCostInFlightPref(false);
        ExecutionStatusViewModel.clearPendingAddCostAmountPref();
        if (viewModel != null) {
            viewModel.setCancelStatus(true);
        }
        if (!isAdded() || context == null) {
            return;
        }
        String message = context.getString(R.string.add_cost_payment_failed);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        if (text_status != null) {
            text_status.setText(message);
        }
        if (btn_cancel_order != null) {
            btn_cancel_order.setEnabled(true);
            btn_cancel_order.setClickable(true);
        }
    }

    private void onAddCostProcessingFinished() {
        cancelAddCostProcessingNotices();
    }

    /** Declined из push/Centrifugo — показываем шторку только после checkStatus WFP. */
    private void confirmDeclinedWithServerCheck() {
        if (!isAdded() || canceled || isViewingCompletedOrder()) {
            return;
        }
        if (isPaymentVerified() || isOrderDispatched()) {
            notifyAddCostPaymentFailed();
            PendingTransactionHelper.clear();
            clearDeclinedPaymentUi();
            return;
        }
        if (isCardPayMethod()) {
            requestWfpPaymentStatusCheck();
        }
    }

    /** Шторка смены способа оплаты после подтверждённого Declined. */
    public void presentDeclinedUiOnFinish() {
        if (!isAdded() || canceled || isViewingCompletedOrder()) {
            return;
        }
        if (isPaymentVerified()) {
            Logger.d(context, TAG, "presentDeclinedUiOnFinish: payment verified — skip");
            return;
        }
        if (isOrderDispatched()) {
            Logger.d(context, TAG, "presentDeclinedUiOnFinish: order dispatched — skip");
            clearDeclinedPaymentUi();
            return;
        }
        FragmentManager fm = getParentFragmentManager();
        if (PaymentErrorSheetHelper.isShowing(fm)) {
            Logger.d(context, TAG, "presentDeclinedUiOnFinish: sheet already showing");
            return;
        }
        if (!PaymentErrorSheetHelper.beginShowAttempt()) {
            Logger.d(context, TAG, "presentDeclinedUiOnFinish: show in flight");
            return;
        }
        if (!PaymentDeclinedNotifier.shouldShowSheetNow()) {
            PaymentErrorSheetHelper.releaseShowLock();
            Logger.d(context, TAG, "presentDeclinedUiOnFinish: debounce skip");
            return;
        }
        String activeUid = resolveActiveOrderUid();
        PaymentSessionHelper.markPaymentFailedForOrder(activeUid);
        PaymentSessionHelper.saveCashReorderContext(activeUid, uid_Double);
        Logger.w(context, TAG,
                "[cashReorder] presentDeclinedUiOnFinish"
                        + " uid=" + activeUid
                        + " uid_Double=" + uid_Double
                        + " amount=" + amount);
        PaymentDeclinedNotifier.prepareDeclinedOrderState();
        PaymentDeclinedNotifier.markSheetShown();
        PendingTransactionHelper.clear();
        showPaymentErrorBottomSheet();
    }

    private void clearDeclinedPaymentUi() {
        PaymentErrorSheetHelper.dismiss(getParentFragmentManager());
        PaymentSessionHelper.clearPaymentFailedForOrder(resolveActiveOrderUid());
        PendingTransactionHelper.clear();
        sharedPreferencesHelperMain.saveValue("add_show_flag", true);
        if (viewModel != null) {
            String current = viewModel.getTransactionStatus().getValue();
            if ("Declined".equals(current)) {
                viewModel.setTransactionStatus(null);
            }
        }
    }

    private boolean hasKnownPaymentFailure() {
        String orderUid = resolveActiveOrderUid();
        if (PaymentSessionHelper.hasPaymentFailedForOrder(orderUid)) {
            return true;
        }
        if (viewModel != null && "Declined".equals(viewModel.getTransactionStatus().getValue())) {
            return true;
        }
        if (PendingTransactionHelper.hasPendingDeclinedForActiveOrder()) {
            return true;
        }
        // prepareDeclinedOrderState() сбрасывает флаг — признак незавершённой оплаты
        return !(boolean) sharedPreferencesHelperMain.getValue("add_show_flag", true);
    }

    private void applyPaymentStatusFromServer(
            @Nullable String transactionStatus,
            boolean pendingDeclined,
            boolean knownFailureAtCheck
    ) {
        if (!isAdded() || canceled || isViewingCompletedOrder()) {
            return;
        }
        if (transactionStatus != null) {
            lastKnownPaymentStatus = transactionStatus;
        }
        Logger.d(context, TAG, "applyPaymentStatusFromServer: status=" + transactionStatus
                + " pendingDeclined=" + pendingDeclined
                + " knownFailureAtCheck=" + knownFailureAtCheck
                + " holdVerified=" + holdVerifiedOnEnter);

        if (isPaymentVerified() || isApprovedPaymentStatus(transactionStatus)) {
            PendingTransactionHelper.clear();
            clearDeclinedPaymentUi();
            return;
        }

        if (isOrderDispatched()) {
            Logger.d(context, TAG, "applyPaymentStatusFromServer: order dispatched — ignore");
            PendingTransactionHelper.clear();
            clearDeclinedPaymentUi();
            return;
        }

        if ("Declined".equals(transactionStatus)) {
            if (isPaymentVerified() || isOrderDispatched()) {
                notifyAddCostPaymentFailed();
                clearDeclinedPaymentUi();
                return;
            }
            presentDeclinedUiOnFinish();
        }
    }

    private void showPaymentErrorBottomSheet() {
        if (pendingPaymentErrorSheetShow != null) {
            View root = getView();
            if (root != null) {
                root.removeCallbacks(pendingPaymentErrorSheetShow);
            } else {
                new Handler(Looper.getMainLooper()).removeCallbacks(pendingPaymentErrorSheetShow);
            }
        }
        pendingPaymentErrorSheetShow = () -> {
            pendingPaymentErrorSheetShow = null;
            if (!isAdded() || canceled || getActivity() == null) {
                PaymentErrorSheetHelper.releaseShowLock();
                return;
            }
            if (isPaymentVerified() || isOrderDispatched()) {
                PaymentErrorSheetHelper.releaseShowLock();
                clearDeclinedPaymentUi();
                return;
            }
            FragmentManager fm = getParentFragmentManager();
            if (PaymentErrorSheetHelper.isShowing(fm)) {
                Logger.w(context, TAG,
                        "[cashReorder] showPaymentErrorBottomSheet SKIPPED: already showing");
                PaymentErrorSheetHelper.releaseShowLock();
                return;
            }
            String sheetPayMethod = isCardPayMethod() ? pay_method : "wfp_payment";
            Logger.d(context, TAG,
                    "[cashReorder] showPaymentErrorBottomSheet"
                            + " pay_method=" + sheetPayMethod
                            + " uid=" + uid);
            MyBottomSheetErrorPaymentFragment sheet =
                    new MyBottomSheetErrorPaymentFragment(sheetPayMethod, messageFondy, amount, context);
            sheet.show(fm, PaymentErrorSheetHelper.SHEET_TAG);
        };
        View root = getView();
        if (root != null) {
            root.post(pendingPaymentErrorSheetShow);
        } else {
            new Handler(Looper.getMainLooper()).post(pendingPaymentErrorSheetShow);
        }
    }


    private void btnOptions() {
        // Создаем Bundle для передачи данных
        Bundle bundle = new Bundle();

        Log.d("btnOptions", "flexible_tariff_name " + flexible_tariff_name);
        Log.d("btnOptions", "comment_info " + comment_info);
        Log.d("btnOptions", "extra_charge_codes " + extra_charge_codes);
        
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

            if (!activeOrderCloseMode) {
                btn_again.animate().alpha(0f).setDuration(300).withEndAction(() -> btn_again.setVisibility(GONE));
            }
        } else {
            // Анимация появления кнопок

            btn_open.setTextColor(colorDefault);
            btn_add_cost.setVisibility(View.VISIBLE);
            btn_add_cost.setAlpha(0f);
            btn_add_cost.setEnabled(true);
            btn_add_cost.animate().alpha(1f).setDuration(300);

            if (!activeOrderCloseMode) {
                if (btn_again.getVisibility() != View.VISIBLE || btn_again.getVisibility() != GONE) {
                    btn_again.setVisibility(View.VISIBLE);
                }
                btn_again.setAlpha(0f);
                btn_again.animate().alpha(1f).setDuration(300);
            }
        }
    }

    private void navigateToNewOrder() {
        MainActivity.order_id = null;
        sharedPreferencesHelperMain.saveValue("carFound", false);
        EarlyOrderNavigationHelper.clearSubmitState();
        sharedPreferencesHelperMain.saveValue("cost_recalc_from_finish", true);
        updateAddCost(String.valueOf(0));
        if (handlerStatus != null) {
            handlerStatus.removeCallbacks(myTaskStatus);
        }
        MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_visicom, true)
                .build());
    }

    /** Во время опроса статуса: «Заказать еще» → «Закрыть», возврат на главный экран. */
    private void applyActiveOrderCloseMode() {
        if (btn_again == null || !isAdded()) {
            return;
        }
        activeOrderCloseMode = true;
        btn_again.setText(R.string.close);
        btn_again.setVisibility(View.VISIBLE);
        btn_again.setOnClickListener(v -> closeActiveOrderScreen());
    }

    /** После отмены/выполнения — вернуть «Заказать еще». */
    private void restoreOrderAgainButton() {
        if (btn_again == null) {
            return;
        }
        activeOrderCloseMode = false;
        btn_again.setText(R.string.btn_again_text);
        btn_again.setOnClickListener(v -> navigateToNewOrder());
    }

    private void closeActiveOrderScreen() {
        if (context == null) {
            return;
        }
        stopCycle();
        if (canceled || ExecutionStatusViewModel.isUserCanceledPref()) {
            ExecutionStatusViewModel.clearActiveOrderNoticeSuppress();
            clearActiveOrderUidsAfterCancel();
        } else if (activeOrderCloseMode) {
            sharedPreferencesHelperMain.saveValue("carFound", false);
        }
        EarlyOrderNavigationHelper.clearSubmitState();
        sharedPreferencesHelperMain.saveValue("cost_recalc_from_finish", true);
        startActivity(new Intent(context, MainActivity.class));
    }

    private void stopCancelWatchPoll() {
        if (handlerStatus != null && cancelWatchPoll != null) {
            handlerStatus.removeCallbacks(cancelWatchPoll);
        }
    }

    private void scheduleCancelWatchPoll() {
        if (handlerStatus == null || cancelWatchPoll == null || canceled || !statusPollPaused) {
            return;
        }
        stopCancelWatchPoll();
        HandlerCompat.postDelayed(handlerStatus, cancelWatchPoll, null, 4000);
    }

    private void stopCycle() {
        isTaskCancelled = true;
        statusPollPaused = false;
        if (handlerStatus != null) {
            handlerStatus.removeCallbacks(myTaskStatus);
        }
        stopCancelWatchPoll();
        isTaskRunning = false;
        Log.d("FinishSeparateFragment", "Cycle stopped");
    }

    private void pauseStatusPolling() {
        statusPollPaused = true;
        if (handlerStatus != null) {
            handlerStatus.removeCallbacks(myTaskStatus);
        }
        isTaskRunning = false;
        scheduleCancelWatchPoll();
        Log.d(TAG, "Status polling paused");
    }

    private void resumeStatusPolling() {
        statusPollPaused = false;
        stopCancelWatchPoll();
        if (!canceled && isAdded()) {
            startCycle();
        }
    }

    private void startCycle() {
        Log.d("HandlerDebug startCycle", "startCycle called from: " + new Exception().getStackTrace()[1]);
        if (shouldIgnoreStatusPollingUi()) {
            Log.d(TAG, "startCycle skipped: cancel UI active");
            return;
        }
        if (isAdded() && handlerStatus != null && myTaskStatus != null && !isTaskRunning
                && !isTaskCancelled && !statusPollPaused) {
            handlerStatus.removeCallbacks(myTaskStatus);
            long delay = delayMillisStatus > 0 ? delayMillisStatus : 5000;
            Log.d("FinishSeparateFragment", "Starting cycle with delay: " + delay);
            HandlerCompat.postDelayed(handlerStatus, myTaskStatus, null, delay);
        } else {
            Log.e("FinishSeparateFragment", "Cannot start cycle: " +
                    "isAdded=" + isAdded() +
                    ", handlerStatus=" + (handlerStatus != null) +
                    ", myTaskStatus=" + (myTaskStatus != null) +
                    ", isTaskRunning=" + isTaskRunning +
                    ", isTaskCancelled=" + isTaskCancelled +
                    ", statusPollPaused=" + statusPollPaused);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        if (timeUtils != null) {
            timeUtils.stopTimer();
        }
        stopCycle();
//        if (handlerAddcost != null) {
//            handlerAddcost.removeCallbacks(showDialogAddcost);
//        }
//        if (handlerAddcost != null ) {
//            handlerAddcost.removeCallbacks(showDialogAddcost);
//        }
        cancelShowDialogAddCost();
        if (handlerCheckTask != null) {
            handlerCheckTask.removeCallbacks(checkTask);
        }
        if (checkHandler != null) {
            checkHandler.removeCallbacksAndMessages(null);
        }
        cancelAddCostProcessingNotices();
        if (notifier != null) {
            notifier.cancelPendingWeatherRequests();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activityRef.clear();
    }




    @Override
    public void onPause() {
        super.onPause();
        // Отменяем выполнение Runnable, если активити остановлена

        stopCycle();
//        if (handlerAddcost != null ) {
//            handlerAddcost.removeCallbacks(showDialogAddcost);
//        }
        cancelShowDialogAddCost();

        if (handlerCheckTask != null) {
            handlerCheckTask.removeCallbacks(checkTask);
        }
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
                    Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
                    Logger.w(context, TAG, "NO INTERNET - Showing toast message");

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
                    str = str.concat(cn + " = " + CursorReadHelper.getString(c, cn) + "; ");
                    list.add(CursorReadHelper.getString(c, cn));

                }
            } while (c.moveToNext());
        }
        database.close();
        c.close();
        return list;
    }
    @Nullable
    private String buildCancelRequestUrl() {
        if (context == null) {
            return null;
        }
        cancelShowDialogAddCost();
        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);
        baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        boolean orderInMyVod = (boolean) sharedPreferencesHelperMain.getValue("order_in_my_vod", false);
        String activeUid = resolveActiveOrderUid();
        if (activeUid == null || activeUid.trim().isEmpty()) {
            return null;
        }
        if (hasLinkedDoubleOrder()) {
            pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
            String doubleUid = uid_Double != null ? uid_Double.trim() : "";
            if (doubleUid.isEmpty()) {
                return null;
            }
            return baseUrl + api + "/android/webordersCancelDouble/" + activeUid + "/" + doubleUid
                    + "/" + pay_method + "/" + city + "/" + context.getString(R.string.application);
        }
        if (orderInMyVod) {
            return baseUrl + api + "/android/webordersCancelVod/" + activeUid;
        }
        return baseUrl + api + "/android/webordersCancel/" + activeUid + "/" + city
                + "/" + context.getString(R.string.application);
    }

    private void setCancelButtonBusy(boolean busy) {
        if (btn_cancel_order != null) {
            btn_cancel_order.setEnabled(!busy);
            btn_cancel_order.setClickable(!busy);
        }
    }

    private void clearActiveOrderUidsAfterCancel() {
        if (viewModel != null) {
            viewModel.clearOrderUid();
        } else {
            MainActivity.uid = null;
            MainActivity.uid_Double = null;
        }
    }

    private void handleCancelRequestFailed() {
        cancelRequestInFlight = false;
        ExecutionStatusViewModel.setCancelInFlightPref(false);
        activeCancelCall = null;
        cancel_btn_click = false;
        if (context == null || !isAdded()) {
            return;
        }
        setCancelButtonBusy(false);
        Toast.makeText(context, R.string.error_cancelling_order, Toast.LENGTH_LONG).show();
        if (text_status != null) {
            text_status.setText(R.string.verify_internet);
        }
        if (viewModel != null) {
            viewModel.setStatusNalUpdate(false);
        }
        cancelFailureWatchRemaining = 8;
        statusPollPaused = true;
        scheduleCancelWatchPoll();
        Logger.d(context, TAG, "handleCancelRequestFailed: watching status before resume, polls=" + cancelFailureWatchRemaining);
    }

    private void resumeStatusPollingAfterCancelFailure() {
        cancelFailureWatchRemaining = 0;
        stopCancelWatchPoll();
        statusPollPaused = false;
        canceled = false;
        if (context == null || !isAdded()) {
            return;
        }
        Logger.d(context, TAG, "resumeStatusPollingAfterCancelFailure: order still active on server");
        try {
            statusOrder();
        } catch (ParseException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            startCycle();
        }
    }

    /**
     * Отмена на сервере; UI «заказ снят» только после успешного ответа.
     */
    private void submitOrderCancelRequest(@NonNull String successMessage) {
        if (context == null || !isAdded()) {
            return;
        }
        if (cancelRequestInFlight) {
            Toast.makeText(context, R.string.sent_cancel_message, Toast.LENGTH_SHORT).show();
            return;
        }
        String url = buildCancelRequestUrl();
        if (url == null || url.contains("/null")) {
            Toast.makeText(context, R.string.error_cancelling_order, Toast.LENGTH_LONG).show();
            return;
        }
        final String uidToCancel = resolveActiveOrderUid();
        cancelFailureWatchRemaining = 0;
        cancelRequestInFlight = true;
        ExecutionStatusViewModel.setCancelInFlightPref(true);
        cancelShowDialogAddCost();
        setCancelButtonBusy(true);
        statusPollPaused = true;
        if (handlerStatus != null && myTaskStatus != null) {
            handlerStatus.removeCallbacks(myTaskStatus);
        }
        isTaskRunning = false;
        scheduleCancelWatchPoll();
        if (text_status != null) {
            text_status.clearAnimation();
            text_status.setText(R.string.sent_cancel_message);
        }
        Logger.d(context, TAG, "submitOrderCancelRequest: " + url + " uid=" + uidToCancel);
        final boolean useDoubleEndpoint = url.contains("webordersCancelDouble")
                || url.contains("webordersCancelVod");
        activeCancelCall = useDoubleEndpoint
                ? ApiClient.getCancelApiService().cancelOrderDouble(url)
                : ApiClient.getCancelApiService().cancelOrder(url);
        activeCancelCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                cancelRequestInFlight = false;
                ExecutionStatusViewModel.setCancelInFlightPref(false);
                activeCancelCall = null;
                if (context == null || !isAdded()) {
                    return;
                }
                setCancelButtonBusy(false);
                if (response.isSuccessful() && response.body() != null) {
                    Logger.d(context, TAG, "submitOrderCancelRequest OK: " + response.body());
                    completeOrderCancelSuccess(successMessage, uidToCancel);
                } else {
                    Logger.d(context, TAG, "submitOrderCancelRequest HTTP " + response.code());
                    handleCancelRequestFailed();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    cancelRequestInFlight = false;
                    ExecutionStatusViewModel.setCancelInFlightPref(false);
                    activeCancelCall = null;
                    return;
                }
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(context, TAG, "submitOrderCancelRequest onFailure: " + t.getMessage());
                handleCancelRequestFailed();
            }
        });
    }

    /**
     * Полная отмена заказа из шторки смены/ошибки оплаты — сначала сервер, потом UI.
     */
    public void cancelOrderFromPaymentErrorSheet() {
        if (context == null) {
            return;
        }
        cancelShowDialogAddCost();
        PaymentSessionHelper.clearPaymentFailedForOrder(resolveActiveOrderUid());
        submitOrderCancelRequest(context.getString(R.string.ex_st_canceled_no_pay));
    }

    /** Единая точка Declined (карта, Centrifugo, FCM) — без второй шторки. */
    public static void notifyPaymentDeclinedIfNeeded(@NonNull Context context) {
        FragmentActivity activity = null;
        if (context instanceof FragmentActivity) {
            activity = (FragmentActivity) context;
        } else if (context instanceof android.content.ContextWrapper) {
            Context base = ((android.content.ContextWrapper) context).getBaseContext();
            if (base instanceof FragmentActivity) {
                activity = (FragmentActivity) base;
            }
        }
        if (activity != null) {
            FinishSeparateFragment finish = findFinishFragment(activity.getSupportFragmentManager());
            if (finish != null && finish.isAdded()) {
                finish.confirmDeclinedWithServerCheck();
                return;
            }
        }
        PaymentDeclinedNotifier.maybeSendPaymentErrorPush(context, MainActivity.uid);
    }

    public static boolean cancelFromPaymentBottomSheet(@NonNull Context context) {
        FragmentActivity activity = null;
        if (context instanceof FragmentActivity) {
            activity = (FragmentActivity) context;
        } else if (context instanceof android.content.ContextWrapper) {
            Context base = ((android.content.ContextWrapper) context).getBaseContext();
            if (base instanceof FragmentActivity) {
                activity = (FragmentActivity) base;
            }
        }
        if (activity == null) {
            return false;
        }
        FinishSeparateFragment finish = findFinishFragment(activity.getSupportFragmentManager());
        if (finish != null && finish.isAdded()) {
            finish.cancelOrderFromPaymentErrorSheet();
            return true;
        }
        return false;
    }

    @Nullable
    private static FinishSeparateFragment findFinishFragment(@Nullable FragmentManager fragmentManager) {
        if (fragmentManager == null) {
            return null;
        }
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment instanceof FinishSeparateFragment finish && fragment.isAdded()) {
                return finish;
            }
            FinishSeparateFragment nested = findFinishFragment(fragment.getChildFragmentManager());
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    public void statusOrder() throws ParseException {

        btn_cancel_order.setEnabled(true);
        btn_cancel_order.setClickable(true);
        Logger.d(context, "RetrofitCall", "statusOrder");
        Logger.d(context, "RetrofitCall", "pay_method " + pay_method);
        if(pay_method.equals("nal_payment")) {


            String value = resolveActiveOrderUid();
            if (value == null || value.isEmpty()) {
                Logger.w(context, TAG, "statusOrder: active uid missing");
                return;
            }
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
                        Logger.d(context, TAG, "OrderResponse: orderCarInfo " + orderCarInfo);
                        Logger.d(context, TAG, "OrderResponse: driverPhone " + time_to_start_point);
                        Logger.d(context, TAG, "OrderResponse: time_to_start_point " + time_to_start_point);

                        if (isOrderCanceledOnServer(orderResponse)) {
                            if (isAdded() && context != null) {
                                context.runOnUiThread(
                                        () -> completeOrderCancelSuccess(
                                                context.getString(R.string.ex_st_canceled),
                                                orderResponse.getDispatchingOrderUid()
                                        ));
                            }
                            return;
                        }

                        if (shouldIgnoreStatusPollingUi()) {
                            Logger.d(context, TAG, "statusOrder onResponse ignored: cancel in progress");
                            return;
                        }

                        lastExecutionStatus = executionStatus;
                        if (isOrderDispatched()) {
                            clearDeclinedPaymentUi();
                        }

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

                        if (isAdded() && context != null) {
                            context.runOnUiThread(() -> {
                                refreshFinishCostFromOrder(orderResponse);
                                closeReasonReactNal(
                                        closeReason,
                                        executionStatus,
                                        driverPhone,
                                        time_to_start_point,
                                        orderCarInfo
                                );
                            });
                        }
                    } else {
                        int closeReason = -1;
                        String executionStatus = "*";
                        String driverPhone = "*";
                        String time_to_start_point = "*";
                        String orderCarInfo = "*";
                        if (isAdded() && context != null) {
                            context.runOnUiThread(() -> closeReasonReactNal(
                                    closeReason,
                                    executionStatus,
                                    driverPhone,
                                    time_to_start_point,
                                    orderCarInfo
                            ));
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                    if (!isAdded() || context == null || shouldIgnoreStatusPollingUi()) {
                        return;
                    }
                    context.runOnUiThread(() -> closeReasonReactNal(
                            -1, "*", "*", "*", "*"
                    ));
                }
            });
        } else {
            baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
            String url = baseUrl  + "getOrderStatusMessageResultPush/" + uid;

//            Call<OrderResponse> call = ApiClient.getApiService().getOrderStatusMessageResultPush(url);
            retrofitCall = ApiClient.getApiService().getOrderStatusMessageResultPush(url);
            Logger.d(context, "RetrofitCall", "/getOrderStatusMessageResultPush/: " + url);

            // Выполняем запрос асинхронно
            retrofitCall.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<OrderResponse> call, @NonNull Response<OrderResponse> response) {
                    Log.d("RetrofitCall", "Request URL: " + call.request().url());
                    if (response.isSuccessful()) {
                        delayMillisStatus = 5 * 1000;
                        if (response.body() != null) {
                            OrderResponse orderResponse = response.body();
                            Log.i("RetrofitCall", "Response successful. OrderResponse: " + orderResponse.toString());
                            Activity activity = activityRef.get();
                            if (activity != null && isAdded()) {
                                Log.d("RetrofitCall", "Updating UI for orderResponse: ");
                                activity.runOnUiThread(() -> updateUICardPayStatus(orderResponse));
                            } else {
                                Log.w("RetrofitCall", "Activity is null or fragment not added. Cannot update UI.");
                            }
                        } else {
                            Log.w("RetrofitCall", "Response successful but body is null or empty.");
                            // Handle empty response case (e.g., update UI to show "no data" or retry)
                        }
                    } else {
                        Log.w("RetrofitCall", "Response unsuccessful. Code: " + response.code() + ", Message: " + response.message());
                        if (response.errorBody() != null) {
                            try {
                                Log.e("RetrofitCall", "Error body: " + response.errorBody().string());
                            } catch (IOException e) {
                                Log.e("RetrofitCall", "Failed to parse error body", e);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<OrderResponse> call, @NonNull Throwable t) {
                    if (!call.isCanceled()) {
                        Log.e("RetrofitCall", "Request failed: " + t.getMessage(), t);
                        FirebaseCrashlytics.getInstance().recordException(t);
                        delayMillisStatus = Math.min(delayMillisStatus + 5000, 30_000);
                        Logger.w(context, TAG, "status poll backoff ms=" + delayMillisStatus);
                    } else {
                        Log.d("RetrofitCall", "Request was canceled.");
                    }
                }
            });
        }

    }

    private void orderComplete() {
        restoreOrderAgainButton();
        sharedPreferencesHelperMain.saveValue("carFound", true);
//        new Handler(Looper.getMainLooper()).post(() -> {
            // Выполнено
        // Збільшуємо лічильник завершених поїздок
        int currentCount = (int) sharedPreferencesHelperMain.getValue("completed_orders_count", 0);
        sharedPreferencesHelperMain.saveValue("completed_orders_count", currentCount + 1);
        // ✅ Додаємо виклик оцінювання після завершення поїздки
        showReviewDialogIfNeeded();

        stopCycle();
        updateProgress(4);

        String message = context.getString(R.string.ex_st_finished);
        text_status.setText(message);
        text_status.clearAnimation();

            // Скрываем элементы
        if (btn_cancel_order.getVisibility() != GONE) {
//            btn_cancel_order.setVisibility(GONE);
            viewModel.hideCancelButton();
        }
        // Показываем кнопку "Повторить"
        if (btn_again.getVisibility() != View.VISIBLE || btn_again.getVisibility() != GONE) {
            btn_again.setVisibility(View.VISIBLE);
        }
        setVisibility(GONE, btn_add_cost, btn_open, btn_options, btn_cancel_order,
                textStatusCar, textCarMessage, textCost, countdownTextView,
                textCostMessage, carProgressBar, progressSteps);


        // Отменяем все обработчики
        canceled = true;

        cancelAllHandlers(context);

        Logger.d(context, TAG, "orderComplete " + canceled);

        showFinishCost(context);
    }

    /**
     * Показує діалог оцінювання після успішного завершення поїздки
     */
    private void showReviewDialogIfNeeded() {
        // Отримуємо MainActivity
        Activity activity = getActivity();
        if (!(activity instanceof MainActivity)) {
            Logger.d(context, TAG, "Cannot show review dialog: activity is not MainActivity");
            return;
        }

        MainActivity mainActivity = (MainActivity) activity;
        AppReviewManager reviewManager = mainActivity.getAppReviewManager();

        if (reviewManager == null) {
            Logger.d(context, TAG, "AppReviewManager is null");
            return;
        }

        // Перевіряємо, чи можна показувати діалог
        if (reviewManager.hasUserReviewed()) {
            Logger.d(context, TAG, "User already reviewed the app");
            return;
        }

        // Затримка перед показом (щоб не заважати основному потоку)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && getActivity() != null) {
                reviewManager.requestReview(requireActivity(), new AppReviewManager.ReviewCallback() {
                    @Override
                    public void onReviewCompleted() {
                        Logger.d(context, TAG, "Review dialog completed after order");
                    }

                    @Override
                    public void onReviewFailed(Exception e) {
                        Logger.d(context, TAG, "Review failed after order: " + e.getMessage());
                    }

                    @Override
                    public void onReviewNotAvailable(String reason) {
                        Logger.d(context, TAG, "Review not available after order: " + reason);
                    }
                });
            }
        }, 2000); // Затримка 2 секунди
    }
    private void orderInRout(
            String driverPhone,
            String orderCarInfo
    ) {
        Logger.d(context, TAG, "orderInRout ");
        sharedPreferencesHelperMain.saveValue("carFound", true);
        updateProgress(3);
        text_status.setText(context.getString(R.string.ex_st_in_rout));
        text_status.clearAnimation();

        if (btn_cancel_order.getVisibility() == View.VISIBLE) {
            viewModel.hideCancelButton();
        }

        setVisibility(GONE, btn_open, btn_options, btn_cancel_order,
                countdownTextView, carProgressBar, progressSteps);
        setVisibility(View.VISIBLE, textCost, textCostMessage);

        if (!TextUtils.isEmpty(driverPhone)) {
            btn_add_cost.setText(context.getString(R.string.phone_driver));
            btn_add_cost.setOnClickListener(v -> PhoneCallHelper.callDriver(driverPhone, context));
            btn_add_cost.setVisibility(View.VISIBLE);
            btn_add_cost.setEnabled(true);
            btn_add_cost.setAlpha(1f);
        } else {
            btn_add_cost.setVisibility(View.VISIBLE);
            btn_add_cost.setEnabled(true);
            btn_add_cost.setAlpha(1f);
            btnAddCost(timeCheckOutAddCost);
        }

        if (!TextUtils.isEmpty(orderCarInfo)) {
            setVisibility(View.VISIBLE, textStatusCar, textCarMessage);
            textCarMessage.setText(orderCarInfo);
        } else {
            setVisibility(GONE, textStatusCar, textCarMessage);
        }

        applyActiveOrderCloseMode();
        viewModel.hideCancelButton();

        if (handler != null) {
            Logger.d(context, TAG, "Removing myRunnable handler");
            handler.removeCallbacks(myRunnable);
        }
        if (handlerBonusBtn != null) {
            Logger.d(context, TAG, "Removing runnableBonusBtn handler");
            handlerBonusBtn.removeCallbacks(runnableBonusBtn);
        }
//        if (handlerAddcost != null) {
//            Logger.d(context, TAG, "Removing showDialogAddcost handler");
//            handlerAddcost.removeCallbacks(showDialogAddcost);
//        }
        cancelShowDialogAddCost();
//        stopCycle();


    }

    private void carSearch() {
        if (shouldIgnoreStatusPollingUi()) {
            Logger.d(context, TAG, "carSearch ignored: cancel UI active");
            return;
        }
        if (isOrderDispatched() || isTerminalOrderAction(action)) {
            Logger.d(context, TAG, "carSearch ignored: order already dispatched, action=" + action);
            return;
        }
        sharedPreferencesHelperMain.saveValue("carFound", false);
        viewModel.showCancelButton();
        sharedPreferencesHelperMain.saveValue("bonusExecuted", false);
//        new Handler(Looper.getMainLooper()).post(() -> {
            Logger.d(context, TAG, "carSearch() started");
            if (btn_cancel_order.getVisibility() != View.VISIBLE) {
//                btn_cancel_order.setVisibility(VISIBLE);
                viewModel.showCancelButton();
            }

            btnAddCost (timeCheckOutAddCost);

        btn_add_cost.setVisibility(View.VISIBLE);
        btn_add_cost.setEnabled(true);
        btn_add_cost.setAlpha(1f);
            if (cancel_btn_click) {
                Logger.d(context, TAG, "Order cancellation detected, stopping search...");
                if (handler != null) {
                    Logger.d(context, TAG, "Removing myRunnable handler");
                    handler.removeCallbacks(myRunnable);
                }
                if (handlerBonusBtn != null) {
                    Logger.d(context, TAG, "Removing runnableBonusBtn handler");
                    handlerBonusBtn.removeCallbacks(runnableBonusBtn);
                }
//                if (handlerAddcost != null) {
//                    Logger.d(context, TAG, "Removing showDialogAddcost handler");
//                    handlerAddcost.removeCallbacks(showDialogAddcost);
//                }
                cancelShowDialogAddCost();
                setVisibility(GONE, btn_add_cost, carProgressBar);
                text_status.setText(context.getString(R.string.checkout_status));

                return;
            }

        if (need_20_add && handlerAddcost != null && showDialogAddcost != null) {
            Logger.d(context, TAG, "Triggering add cost delay: " + timeCheckOutAddCost);
//            handlerAddcost.postDelayed(showDialogAddcost, timeCheckOutAddCost);

            setShowDialogAddCost();

            setVisibility(View.VISIBLE, textCost, textCostMessage, carProgressBar, progressSteps, btn_options, btn_open);
            applyActiveOrderCloseMode();
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
//        });
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
//        if (handlerAddcost != null) {
//            Logger.d(context, TAG, "Removing showDialogAddcost handler");
//            handlerAddcost.removeCallbacks(showDialogAddcost);
//        }
        cancelShowDialogAddCost();
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
        boolean wasCarFound = (boolean) sharedPreferencesHelperMain.getValue("carFound", false);

        sharedPreferencesHelperMain.saveValue("carFound", true);
        clearDeclinedPaymentUi();

        if (!wasCarFound && !com.taxi.easy.ua.androidx.startup.MyApplication.isInForeground()
                && uid != null && !uid.isEmpty()) {
            NotificationHelper.showNotificationFindAutoMessage(
                    context, context.getString(R.string.ex_st_2), uid);
        }
//        if (handlerAddcost != null) {
//            handlerAddcost.removeCallbacks(showDialogAddcost);
//        }
        cancelShowDialogAddCost();

        text_status.clearAnimation();

        boolean bonusAlreadyCalled = (boolean) sharedPreferencesHelperMain.getValue("bonusExecuted", false);

        if (!bonusAlreadyCalled && (
                pay_method.equals("bonus_payment") ||
                        pay_method.equals("wfp_payment") ||
                        pay_method.equals("fondy_payment") ||
                        pay_method.equals("mono_payment")
        )) {
            sharedPreferencesHelperMain.saveValue("bonusExecuted", true);
            handlerBonusBtn = new Handler(Looper.getMainLooper());
            runnableBonusBtn = () -> viewModel.hideCancelButton();
            handlerBonusBtn.postDelayed(runnableBonusBtn, delayMillis);
        }


        setVisibility(View.VISIBLE, textCost, textCostMessage);

        applyActiveOrderCloseMode();


            List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
            String city = listCity.get(1);
            String api = listCity.get(2);

            if (closeReason == -1) {
                // Геокоординаты водителя по API
                drivercarposition(uid, city, api, context);
            } else {
                calculateTimeToStart(uid, api, context);
            }

            updateProgress(3);

            if (!cancel_btn_click) {
                delayMillisStatus = 5000;
                StringBuilder messageBuilder = new StringBuilder(context.getString(R.string.ex_st_2));

                if (!TextUtils.isEmpty(driverPhone)) {
                    Logger.d(context, TAG, "onResponse: driverPhone " + driverPhone);
                    btn_add_cost.setText(context.getString(R.string.phone_driver));
                    btn_add_cost.setOnClickListener(v -> {
                        PhoneCallHelper.callDriver(driverPhone, context);
//                        Intent intent = new Intent(Intent.ACTION_DIAL);
//                        intent.setData(Uri.parse("tel:" + driverPhone));
//                        context.startActivity(intent);
                    });
                    btn_add_cost.setVisibility(View.VISIBLE);
                    btn_add_cost.setEnabled(true);
                    btn_add_cost.setAlpha(1f);
                } else {
                    btn_add_cost.setVisibility(View.VISIBLE);
                    btn_add_cost.setEnabled(true);
                    btn_add_cost.setAlpha(1f);
                    btnAddCost(timeCheckOutAddCost);
                }

                if (!TextUtils.isEmpty(time_to_start_point)) {
                    messageBuilder.append(context.getString(R.string.ex_st_5))
                            .append(formatDate2(time_to_start_point));
                    countdownTextView.setVisibility(View.VISIBLE);
                    text_status.setText(messageBuilder.toString());
                } else {
                    text_status.setText(context.getString(R.string.ex_st_2));
                    countdownTextView.setVisibility(GONE);
                }

                if (!TextUtils.isEmpty(orderCarInfo)) {
                    setVisibility(View.VISIBLE, textStatusCar, textCarMessage);
                    textCarMessage.setText(orderCarInfo);
                } else {
                    setVisibility(GONE, textStatusCar, textCarMessage);
                }

            } else {
                text_status.setText(context.getString(R.string.ex_st_canceled));
            }
//        });
    }

    private void orderInStartPoint(
            String driverPhone,
            String orderCarInfo
    ) {
        Logger.d(context, TAG, "orderInStartPoint ");
        sharedPreferencesHelperMain.saveValue("carFound", true);

        if (btn_cancel_order.getVisibility() != GONE) {
//            btn_cancel_order.setVisibility(GONE);
            viewModel.hideCancelButton();
        }

        if (handler != null) {
            Logger.d(context, TAG, "Removing myRunnable handler");
            handler.removeCallbacks(myRunnable);
        }
        if (handlerBonusBtn != null) {
            Logger.d(context, TAG, "Removing runnableBonusBtn handler");
            handlerBonusBtn.removeCallbacks(runnableBonusBtn);
        }
//        if (handlerAddcost != null) {
//            Logger.d(context, TAG, "Removing showDialogAddcost handler");
//            handlerAddcost.removeCallbacks(showDialogAddcost);
//        }
        cancelShowDialogAddCost();

        text_status.clearAnimation();

        text_status.setText(context.getString(R.string.ex_st_in_start_point));

        setVisibility(VISIBLE, textCost, textCostMessage);
        setVisibility(GONE, btn_cancel_order, countdownTextView);

        updateProgress(3);


        delayMillisStatus = 5000;

        if (!TextUtils.isEmpty(driverPhone)) {
            Logger.d(context, TAG, "onResponse: driverPhone " + driverPhone);
            btn_add_cost.setText(context.getString(R.string.phone_driver));
            btn_add_cost.setOnClickListener(v -> {
                PhoneCallHelper.callWithFallback(() -> {
                    List<String> stringList = logCursor(MainActivity.CITY_INFO, MyApplication.getContext());
                    return stringList.size() > 3 ? stringList.get(3) : "";
                });
//                Intent intent = new Intent(Intent.ACTION_DIAL);
//                intent.setData(Uri.parse("tel:" + driverPhone));
//                context.startActivity(intent);
            });
            btn_add_cost.setVisibility(View.VISIBLE);
            btn_add_cost.setEnabled(true);
            btn_add_cost.setAlpha(1f);
        } else {
            btn_add_cost.setVisibility(View.VISIBLE);
            btn_add_cost.setEnabled(true);
            btn_add_cost.setAlpha(1f);
            btnAddCost(timeCheckOutAddCost);
        }


        if (!TextUtils.isEmpty(orderCarInfo)) {
            setVisibility(View.VISIBLE, textStatusCar, textCarMessage);
            textCarMessage.setText(orderCarInfo);
        } else {
            setVisibility(GONE, textStatusCar, textCarMessage);
        }
    }
    private void resumePendingAddCostIfInFlight() {
        if (!ExecutionStatusViewModel.isAddCostInFlightPref()) {
            return;
        }
        Logger.d(context, TAG, "[addCost] pending add-cost in flight — polling status");
        showAddCostProcessingNotice();
        scheduleAddCostProcessingNotices();
        checkPendingAddCostPaymentStatus();
    }

     private void btnAddCost (int timeCheckout) {
         btn_add_cost.setText(context.getString(R.string.add_cost_btn));
         btn_add_cost.setOnClickListener( view -> {
             Logger.d(context, TAG,
                     "[addCost] btn_add_cost click"
                             + " uid=" + uid
                             + " pay_method=" + pay_method
                             + " canceled=" + canceled
                             + " statusPollPaused=" + statusPollPaused
                             + " addCostSheetShowing=" + addCostSheetShowing
                             + " add_show_flag=" + sharedPreferencesHelperMain.getValue("add_show_flag", true)
             );

             if ("nal_payment".equals(pay_method)) {

                 String text = textCostMessage.getText().toString();
                 Logger.d(getActivity(), TAG, "textCostMessage.getText().toString() " + text);

                 Pattern pattern = Pattern.compile("(\\d+)");
                 Matcher matcher = pattern.matcher(text);

                 if (matcher.find()) {
                     Logger.d(context, TAG, "amount_to_add: " + matcher.group(1));
                     pauseStatusPolling();
                     ExecutionStatusViewModel.resetNewOrderSession(resolveActiveOrderUid());
                     MyBottomSheetAddCostFragment bottomSheetDialogFragment = new MyBottomSheetAddCostFragment(
                             matcher.group(1),
                             uid,
                             uid_Double,
                             pay_method,
                             viewModel
                     );
    // Устанавливаем слушатель для обработки закрытия
                     bottomSheetDialogFragment.setOnDismissListener(this::onAddCostSheetDismissed);
                     addCostSheetShowing = true;
                     bottomSheetDialogFragment.show(fragmentManager, TAG_ADD_COST_SHEET);
                 } else {
                     Logger.d(context, TAG, "No numeric value found in the text.");
                 }
             }  else if (PaymentTypeHelper.usesWalletHold(pay_method)) {

                 if (ExecutionStatusViewModel.isAddCostInFlightPref()) {
                     Logger.d(context, TAG, "[addCost] btn_add_cost blocked: add-cost in flight");
                     resumePendingAddCostIfInFlight();
                     return;
                 }
                 Logger.d(context, TAG, "[addCost] wfp_payment: verifyOldHold()");
                 verifyOldHold();


             } else if ("bonus_payment".equals(pay_method)) {

                 String message = context.getString(R.string.addCostBonusMessage);
                 MyBottomSheetErrorPaymentFragment bottomSheetDialogFragment = new MyBottomSheetErrorPaymentFragment("wfp_payment", messageFondy, amount, context, message);
                 bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());


             }
         });
     }
    private void orderCanceled(String message) {
            restoreOrderAgainButton();
            PaymentErrorSheetHelper.dismiss(getParentFragmentManager());
            sharedPreferencesHelperMain.saveValue("carFound", false);
            text_status.setText(R.string.recounting_order);

            text_status.clearAnimation();
            canceled = true;
            String activeUid = resolveActiveOrderUid();
            if (activeUid == null || activeUid.isEmpty()) {
                activeUid = uid;
            }
            if (!ExecutionStatusViewModel.isUserCanceledPref()) {
                ExecutionStatusViewModel.markUserCanceledOrder(activeUid);
            }
           action = null;

            // Скрываем ненужные элементы
            setVisibility(GONE, btn_add_cost, btn_open, btn_options, btn_cancel_order,
                    textStatusCar, textCarMessage, textCost, countdownTextView,
                    textCostMessage, carProgressBar, progressSteps);

            // Останавливаем все обработчики
            if (handler != null) {
                handler.removeCallbacks(myRunnable);
            }
            if (handlerBonusBtn != null) {
                handlerBonusBtn.removeCallbacks(runnableBonusBtn);
            }
//            if (handlerAddcost != null) {
//                handlerAddcost.removeCallbacks(showDialogAddcost);
//            }
            cancelShowDialogAddCost();

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



    /** close_reason 0/8/9 в weborders — завершение внешним API (см. UIDController, close_resone_8). */
    private static boolean isExternalApiCompletedCloseReason(int closeReason) {
        return closeReason == 0 || closeReason == 8 || closeReason == 9;
    }

    private static boolean isCanceledExecutionStatus(@Nullable String executionStatus) {
        if (executionStatus == null) {
            return false;
        }
        return "Canceled".equalsIgnoreCase(executionStatus)
                || "Cancelled".equalsIgnoreCase(executionStatus);
    }

    private static boolean isExecutedExecutionStatus(@Nullable String executionStatus) {
        return "Executed".equals(executionStatus);
    }

    private static boolean isOrderCanceledOnServer(@Nullable OrderResponse orderResponse) {
        if (orderResponse == null) {
            return false;
        }
        String executionStatus = orderResponse.getExecutionStatus();
        int closeReason = orderResponse.getCloseReason();
        if (orderResponse.isOrderIsArchive() && isCanceledExecutionStatus(executionStatus)) {
            return true;
        }
        if (closeReason >= 1 && closeReason <= 9 && closeReason != 8 && executionStatus != null) {
            return true;
        }
        return closeReason == -1 && isCanceledExecutionStatus(executionStatus);
    }

    /** Экран уже в финальном состоянии «заказ отменён» (не путать с кнопкой «Закрыть» при активном заказе). */
    private boolean isCancelUiShown() {
        return canceled;
    }

    /** Архивный просмотр выполненного заказа — только явный флаг, не epoch в required_time. */
    private boolean isViewingCompletedOrder() {
        return false;
    }

    /** Не перезаписывать UI отмены ответом опроса «ищем авто». */
    private boolean shouldIgnoreStatusPollingUi() {
        return canceled
                || cancelRequestInFlight
                || cancelFailureWatchRemaining > 0
                || isViewingCompletedOrder();
    }

    /** Просмотр выполненного заказа из списка — только данные из архива. */
    private void applyArchivedCompletedOrderUi() {
        if (!isAdded() || context == null) {
            return;
        }
        stopCycle();
        cancelShowDialogAddCost();
        updateProgress(4);
        text_status.setText(context.getString(R.string.ex_st_finished));
        text_status.clearAnimation();
        viewModel.hideCancelButton();
        restoreOrderAgainButton();
        setVisibility(GONE, btn_add_cost, btn_open, btn_options, btn_cancel_order,
                textStatusCar, textCarMessage, countdownTextView, carProgressBar, progressSteps);
        setVisibility(VISIBLE, text_full_message, textCost, textCostMessage);
    }

    private void finishCancelInFlightState() {
        cancelRequestInFlight = false;
        ExecutionStatusViewModel.setCancelInFlightPref(false);
        cancelFailureWatchRemaining = 0;
        stopCancelWatchPoll();
        statusPollPaused = false;
        if (activeCancelCall != null && !activeCancelCall.isCanceled()) {
            activeCancelCall.cancel();
            activeCancelCall = null;
        }
        setCancelButtonBusy(false);
    }

    /**
     * Заказ отменён (ответ webordersCancel* или опрос historyUIDStatusNew с Canceled).
     */
    private void completeOrderCancelSuccess(@NonNull String successMessage, @Nullable String uidToCancel) {
        if (!isAdded() || context == null) {
            return;
        }
        if (isCancelUiShown()) {
            finishCancelInFlightState();
            return;
        }
        Logger.d(context, TAG, "completeOrderCancelSuccess uid=" + uidToCancel);
        finishCancelInFlightState();
        if (uidToCancel == null || uidToCancel.isEmpty()) {
            uidToCancel = resolveActiveOrderUid();
        }
        if (uidToCancel == null || uidToCancel.isEmpty()) {
            uidToCancel = uid;
        }
        if (uidToCancel != null && !uidToCancel.isEmpty()) {
            ExecutionStatusViewModel.markUserCanceledOrderPair(uidToCancel, uid_Double);
        }
        cancel_btn_click = true;
        cancelShowDialogAddCost();
        orderCanceled(successMessage);
        clearActiveOrderUidsAfterCancel();
    }

    private void showOrderCanceledFromServer() {
        if (!isAdded() || context == null || isCancelUiShown()) {
            return;
        }
        Logger.d(context, TAG, "showOrderCanceledFromServer");
        cancelShowDialogAddCost();
        orderCanceled(context.getString(R.string.ex_st_canceled));
    }

    private void closeReasonReactNal(
            int closeReason,
            String executionStatus,
            String driverPhone,
            String time_to_start_point,
            String orderCarInfo
    ) {
        if (shouldIgnoreStatusPollingUi()) {
            return;
        }
        if (closeReason >= 1 && closeReason <= 9 && closeReason != 8) {
            if (executionStatus != null) {
                showOrderCanceledFromServer();
            } else {
                action = "Поиск авто";
                carSearch();
            }
            return;
        }
        if (closeReason == -1 && isCanceledExecutionStatus(executionStatus)) {
            showOrderCanceledFromServer();
            return;
        }
        if (closeReason == 8) {
            action = "Заказ выполнен";
            orderComplete();
            return;
        }

        switch (closeReason) {
            case -1:
                switch (executionStatus) {
                    case "CarFound": //Найдено авто
                    case "Running": //Найдено авто
                       action = "Авто найдено";
                        carFound (
                            closeReason,
                            driverPhone,
                            time_to_start_point,
                            orderCarInfo
                        );
                        break;
                    case "Executed": //Выполнено
                       action = "Заказ выполнен";
                        orderComplete();
                        break;
                    default: //Поиск авто
                       action = "Поиск авто";
                        carSearch();
                }
                break;
            case 101:
               action = "Авто найдено";
                carFound (
                        closeReason,
                        driverPhone,
                        time_to_start_point,
                        orderCarInfo
                );
                break;
            case 102:
               action = "На месте";
                orderInStartPoint(
                        driverPhone,
                        orderCarInfo
                );
                break;
            case 103:
               action = "В пути";
                orderInRout(driverPhone, orderCarInfo);
                break;
            case 104:
               action = "Заказ выполнен";
                orderComplete();
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
                   action = "Заказ снят";
                    String message = context.getString(R.string.ex_st_canceled);
                    orderCanceled(message);
                } else {
                    // Поиск авто
                   action = "Поиск авто";
                    carSearch();
                }
                break;
            case 0:
                if (isExecutedExecutionStatus(executionStatus)) {
                    action = "Заказ выполнен";
                    orderComplete();
                } else if (isCanceledExecutionStatus(executionStatus)) {
                    showOrderCanceledFromServer();
                } else {
                    action = "Поиск авто";
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
        Logger.d(context, TAG, "closeReasonReactCard: " + action);
        Logger.d(context, TAG, "closeReasonReactCard: " + closeReason);
        if (closeReason == 8) {
            orderComplete();
            return;
        }
        if (closeReason == 0 || closeReason == 9) {
            if (isExecutedExecutionStatus(lastExecutionStatus)) {
                orderComplete();
                return;
            }
            if (isCanceledExecutionStatus(lastExecutionStatus)) {
                showOrderCanceledFromServer();
                return;
            }
        }
        if (shouldIgnoreStatusDowngrade(action)) {
            Logger.d(context, TAG, "closeReasonReactCard: ignore downgrade to search");
            return;
        }
        if ("Заказ выполнен".equals(action)) {
            orderComplete();
            return;
        }
        String message;

        if(closeReason == 100 ||closeReason ==101 || closeReason ==102 || closeReason ==103 || closeReason ==104) {
            sharedPreferencesHelperMain.saveValue("order_in_my_vod", true);
            switch (closeReason) {
                case 101:
                    action = "Авто найдено";
                    carFound (
                            closeReason,
                            driverPhone,
                            time_to_start_point,
                            orderCarInfo
                    );
                    break;
                case 102:
                    action = "На месте";
                    orderInStartPoint(
                            driverPhone,
                            orderCarInfo
                    );
                    break;
                case 103:
                    action = "В пути";
                    orderInRout(driverPhone, orderCarInfo);
                    break;
                case 104:
                    action = "Заказ выполнен";
                    orderComplete();
                    break;
                default:
                    carSearch();
                    break;
            }
        } else {
            sharedPreferencesHelperMain.saveValue("order_in_my_vod", false);
            switch (action) {
                case "Авто найдено":
                    carFound(closeReason, driverPhone, time_to_start_point, orderCarInfo);
                    break;
                case "На месте":
                    orderInStartPoint(driverPhone, orderCarInfo);
                    break;
                case "В пути":
                    orderInRout(driverPhone, orderCarInfo);
                    break;
                case "Заказ выполнен":
                    orderComplete();
                    break;
                case "Заказ снят":

                    message = context.getString(R.string.ex_st_canceled);
                    orderCanceled(message);
                    break;
                default:
                    carSearch();
                    break;
            }
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
        if (canceledStatus != null && "canceled".equals(canceledStatus)) {
            viewModel.getOrderResponse().removeObservers(getViewLifecycleOwner());
            showOrderCanceledFromServer();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("LifecycleCheck 1", "Current lifecycle state: " + getViewLifecycleOwner().getLifecycle().getCurrentState());
        textCostMessage = view.findViewById(R.id.text_cost_message);
        textCost = view.findViewById(R.id.textCost);
        text_status = view.findViewById(R.id.text_status);
        textStatusCar = view.findViewById(R.id.textStatusCar);
        textCarMessage = view.findViewById(R.id.text_status_car);
        countdownTextView = view.findViewById(R.id.countdownTextView);
        carProgressBar = view.findViewById(R.id.carProgressBar);
        progressSteps = view.findViewById(R.id.progressSteps);

        btn_cancel_order = view.findViewById(R.id.btn_cancel_order);
        btn_again = view.findViewById(R.id.btn_again);
        btn_add_cost = view.findViewById(R.id.btn_add_cost);
        viewModelReviewer();
    }
    public void viewModelReviewer() {
        Log.d("DEBUG", "=== FRAGMENT VIEWMODEL CHECK ===");
        Log.d("DEBUG", "ViewModel hash code: " + viewModel.hashCode());
        Log.d("DEBUG", "ViewModel class: " + viewModel.getClass().getName());

        if (viewModel == null) {
            Log.e("ViewModelError", "viewModel is null in viewModelReviewer");
            return;
        }
        cancel_btn_click = false;

        viewModel.getAddCostViewUpdate().observe(getViewLifecycleOwner(), addCost -> {
            if (addCost != null && !addCost.equals("0")) {
                Logger.d(context, TAG, "addCostViewUpdate observe: " + addCost);
                if (PaymentTypeHelper.usesWalletHold(pay_method)) {
                    Logger.d(context, TAG, "addCostViewUpdate skipped for wfp (absolute cost from server)");
                    pendingAddCost = "0";
                    sharedPreferencesHelperMain.saveValue("pendingAddCost", "0");
                    viewModel.setAddCostViewUpdate("0");
                    onAddCostProcessingFinished();
                    return;
                }
                pendingAddCost = addCost;
                sharedPreferencesHelperMain.saveValue("pendingAddCost", addCost);
                addCostView(addCost);
                onAddCostProcessingFinished();
            }
        });

        viewModel.getFinishAbsoluteCostGrivna().observe(getViewLifecycleOwner(), absoluteCost -> {
            if (absoluteCost == null || absoluteCost.isEmpty() || !isAdded()) {
                return;
            }
            Logger.d(context, TAG, "finishAbsoluteCost observe: " + absoluteCost);
            applyDisplayCostToFinishUi(absoluteCost);
            ExecutionStatusViewModel.setAddCostInFlightPref(false);
            ExecutionStatusViewModel.clearPendingAddCostAmountPref();
            pendingAddCost = "0";
            sharedPreferencesHelperMain.saveValue("pendingAddCost", "0");
            viewModel.setCancelStatus(true);
            onAddCostProcessingFinished();
        });

        viewModel.getCancelStatus().observe(getViewLifecycleOwner(), status -> {
            Logger.d(context,"Pusher getCancelStatus", "Finish getCancelStatus status set: " + status);
            if (status == null || canceled || isCancelUiShown()) {
                return;
            }
            btn_cancel_order.setEnabled(status);
            if (!status) {
                if (ExecutionStatusViewModel.isAddCostInFlightPref()) {
                    showAddCostProcessingNotice();
                    scheduleAddCostProcessingNotices();
                } else {
                    String message = context.getString(R.string.recounting_order) + ". "
                            + context.getString(R.string.cancel_btn_enable);
                    text_status.setText(message);
                }
            } else {
                onAddCostProcessingFinished();
                resumeStatusPolling();
            }
        });

        // Centrifugo/FCM во время экрана (не при входе — там refreshPaymentStatusOnEnter)
        viewModel.getTransactionStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == null) {
                return;
            }
            Logger.d(context, "getTransactionStatus", "Finish transaction status: " + status);
            if (paymentCheckInProgress) {
                return;
            }
            if (PaymentErrorSheetHelper.isShowing(getParentFragmentManager())) {
                return;
            }
            if (isViewingCompletedOrder()) {
                return;
            }
            if ("Declined".equals(status)) {
                if (isPaymentVerified() || isOrderDispatched()) {
                    notifyAddCostPaymentFailed();
                    clearDeclinedPaymentUi();
                } else {
                    confirmDeclinedWithServerCheck();
                }
            } else if (isApprovedPaymentStatus(status)) {
                String pendingAmount = ExecutionStatusViewModel.getPendingAddCostAmountPref();
                boolean addCostPending = ExecutionStatusViewModel.isAddCostInFlightPref()
                        || (pendingAmount != null && !pendingAmount.equals("0"));
                ExecutionStatusViewModel.setAddCostInFlightPref(false);
                clearDeclinedPaymentUi();
                if (pendingAmount != null && !pendingAmount.equals("0")
                        && !PaymentTypeHelper.usesWalletHold(pay_method)) {
                    viewModel.setAddCostViewUpdate(pendingAmount);
                }
                ExecutionStatusViewModel.clearPendingAddCostAmountPref();
                if (addCostPending) {
                    onAddCostProcessingFinished();
                }
            }
        });

        viewModel.getCanceledStatus().removeObservers(getViewLifecycleOwner());
        viewModel.getCanceledStatus().observe(getViewLifecycleOwner(), status -> {
            Logger.d(context, "Pusher eventCanceled", "Finish eventCanceled status set: " + status);
            if (status != null && "canceled".equals(status)) {
                viewModel.getOrderResponse().removeObservers(getViewLifecycleOwner());
                showOrderCanceledFromServer();
            }
        });

        if (!paySystemStatus.equals("nal_payment")) {
            viewModel.getStatusNalUpdate().observe(getViewLifecycleOwner(), aBoolean -> {
                Logger.d(context, "startFinishPage","StatusNalUpdate changed: " + aBoolean);

                if (!aBoolean) {
//                    handlerStatus.removeCallbacks(myTaskStatus);
//                    try {
//                        statusOrder();
//                    } catch (ParseException e) {
//                        throw new RuntimeException(e);
//                    }
                    stopCycle();
                } else {
                    startCycle();
                    try {
                        statusOrder();
                    } catch (ParseException e) {
                        Logger.e(context, TAG, "statusOrder after orderAuto: " + e.getMessage());
                    }
                }
            });

            Log.d("LifecycleCheck", "Current lifecycle state: " + getViewLifecycleOwner().getLifecycle().getCurrentState());
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

        viewModel.getUid().observe(getViewLifecycleOwner(), newUid -> {
            if (newUid == null || newUid.isEmpty()) {
                return;
            }
            String previous = uid;
            boolean uidChanged = previous != null && !previous.isEmpty() && !previous.equals(newUid);
            if (uidChanged) {
                navigationOrderUid = newUid;
                PassengerNotifier.linkFinishOrderUidsAfterUidChange(previous, newUid);
                String persistedDouble = ExecutionStatusViewModel.getPersistedDoubleUid();
                if (persistedDouble != null && !persistedDouble.trim().isEmpty()) {
                    uid_Double = persistedDouble;
                    MainActivity.uid_Double = persistedDouble;
                }
            }
            uid = newUid;
            MainActivity.uid = newUid;
            sharedPreferencesHelperMain.saveValue(ExecutionStatusViewModel.PREF_UID_FCM, newUid);
            Logger.d(context, TAG, "order uid updated: active=" + newUid + " double=" + uid_Double);
            if (uidChanged) {
                String canceledUid = ExecutionStatusViewModel.getCanceledOrderUid();
                boolean lateRecreateAfterCancel = canceledUid != null && canceledUid.equals(previous);
                if (lateRecreateAfterCancel || (canceled && ExecutionStatusViewModel.isUserCanceledPref())) {
                    Logger.d(context, TAG, "late uid after cancel — auto-cancel recreated order");
                    canceled = false;
                    ExecutionStatusViewModel.resetNewOrderSession(null);
                    submitOrderCancelRequest(context.getString(R.string.ex_st_canceled));
                    return;
                }
            }
            if (uidChanged && isAdded()) {
                try {
                    statusOrder();
                } catch (ParseException e) {
                    Logger.e(context, TAG, "statusOrder after uid change: " + e.getMessage());
                }
            }
        });

        // Observe paySystemStatus changes
        viewModel.getPaySystemStatus().observe(requireActivity(), newPaySystemStatus -> {
            Log.d("UID 11123", "PaySystemStatus updated: " + newPaySystemStatus);
            // Update UI or perform other actions
            MainActivity.paySystemStatus = newPaySystemStatus;
        });
//наблюдение на кнопкой отмены
        viewModel.getCancelButtonVisible().observe(getViewLifecycleOwner(), isVisible -> {
            btn_cancel_order.setVisibility(View.VISIBLE); // всегда видима (если хочешь)
            if (isVisible != null) {
//                btn_cancel_order.setVisibility(isVisible ? View.VISIBLE : View.GONE);
                btn_cancel_order.setOnClickListener(v -> {
                    if (isVisible) {
                        // Действие 1
                        showCancelDialog();
                    } else {
                        // Действие 2
                        showCancelErrorDialog();
                    }
                });
            }
        });

    }

    // Добавьте этот метод в класс FinishSeparateFragment
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddCostUpdateEvent(AddCostUpdateEvent event) {
        Logger.d(context, "EventBus", "AddCostUpdateEvent ignored (LiveData path): " + event.getAddCost());
    }

    // Добавьте этот метод для восстановления после возврата из фона
    private void applyPendingAddCostIfNeeded() {
        // Проверяем сохраненное значение из SharedPreferences
        String savedAddCost = (String) sharedPreferencesHelperMain.getValue("pendingAddCost", "0");

        String costToApply = "0";

        if (!pendingAddCost.equals("0")) {
            costToApply = pendingAddCost;
        } else if (!savedAddCost.equals("0")) {
            costToApply = savedAddCost;
            pendingAddCost = savedAddCost;
        }

        // Если есть необработанное обновление - применяем его
        if (!costToApply.equals("0") && !PaymentTypeHelper.usesWalletHold(pay_method)) {
            final String finalCostToApply = costToApply; // ✅ Создаем final переменную

            Logger.d(context, TAG, "applyPendingAddCostIfNeeded: Applying pending cost update: " + finalCostToApply);

            // Небольшая задержка, чтобы UI успел полностью восстановиться
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && getActivity() != null) {
                    addCostView(finalCostToApply); // ✅ Используем final переменную
                }
            }, 200);
        }
    }
    private void showCancelErrorDialog() {
        if (isAdded()) {
            String message = getString(R.string.error_5_min_cancel_card_order);
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
        }
    }

    private void addCostView(String addCost) {
        Pattern pattern = Pattern.compile("(\\d+)");
        Matcher matcher = pattern.matcher(textCostMessage.getText().toString());

        if (matcher.find()) {
            btn_cancel_order.setEnabled(false);
            btn_cancel_order.setClickable(false);

            int originalNumber = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
            int updatedNumber = originalNumber + Integer.parseInt(addCost);
            applyDisplayCostToFinishUi(String.valueOf(updatedNumber));

            textCost.setVisibility(View.VISIBLE);
            textCostMessage.setVisibility(View.VISIBLE);
            carProgressBar.setVisibility(View.VISIBLE);
            progressSteps.setVisibility(View.VISIBLE);
            btn_options.setVisibility(View.VISIBLE);
            btn_open.setVisibility(View.VISIBLE);
            text_status.setText(context.getString(R.string.ex_st_0));
            viewModel.setCancelStatus(true);
            viewModel.setAddCostViewUpdate("0");
            pendingAddCost = "0";
            sharedPreferencesHelperMain.saveValue("pendingAddCost", "0");
        } else {
            Logger.d(context, TAG, "Число не найдено в строке.");
        }
    }

    private void refreshFinishCostFromOrder(@Nullable OrderResponse orderResponse) {
        if (orderResponse == null || textCostMessage == null) {
            return;
        }
        String activeUid = resolveActiveOrderUid();
        String responseUid = orderResponse.getDispatchingOrderUid();
        if (activeUid != null && responseUid != null && !responseUid.isEmpty()
                && !activeUid.equals(responseUid)) {
            return;
        }
        String baseCost = orderResponse.getOrderCost();
        if (baseCost == null || baseCost.isEmpty()) {
            return;
        }
        try {
            int serverTotal = (int) Math.round(Double.parseDouble(baseCost.replace(',', '.').trim()));
            int displayed = parseDisplayedCostGrivna();
            if (serverTotal <= 0) {
                return;
            }
            // order_cost from status API is the billable total; add_cost is metadata only.
            // Summing them duplicated +5 UAH after add-cost (60+5 showed as 70).
            if (displayed > 0 && serverTotal < displayed) {
                if (ExecutionStatusViewModel.isAddCostInFlightPref() || addCostSheetShowing) {
                    Logger.d(context, TAG, "refreshFinishCostFromOrder keep client cost="
                            + displayed + " server=" + serverTotal
                            + " add_cost(meta)=" + orderResponse.getAddCost());
                    return;
                }
                applyDisplayCostToFinishUi(String.valueOf(serverTotal));
                Logger.d(context, TAG, "refreshFinishCostFromOrder corrected stale client="
                        + displayed + " server=" + serverTotal);
                return;
            }
            if (serverTotal != displayed) {
                applyDisplayCostToFinishUi(String.valueOf(serverTotal));
                Logger.d(context, TAG, "refreshFinishCostFromOrder server=" + serverTotal
                        + " add_cost(meta)=" + orderResponse.getAddCost()
                        + " displayed=" + displayed);
            }
        } catch (NumberFormatException e) {
            Logger.w(context, TAG, "refreshFinishCostFromOrder parse failed: " + baseCost);
        }
    }

    private int parseDisplayedCostGrivna() {
        if (textCostMessage == null) {
            return 0;
        }
        Matcher matcher = Pattern.compile("(\\d+)").matcher(textCostMessage.getText().toString());
        if (matcher.find()) {
            return Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
        }
        return 0;
    }

    private String getPayMethodMessageSuffix() {
        if ("bonus_payment".equals(pay_method)) {
            return context.getString(R.string.pay_method_message_bonus);
        }
        if (isCardPayMethod()) {
            return context.getString(R.string.pay_method_message_card);
        }
        if ("google_pay_payment".equals(pay_method)) {
            return context.getString(R.string.pay_method_message_google);
        }
        return context.getString(R.string.pay_method_message_nal);
    }

    private void applyDisplayCostToFinishUi(String costGrivna) {
        if (textCostMessage == null) {
            return;
        }
        String message = costGrivna + " " + context.getString(R.string.UAH) + "  "
                + getPayMethodMessageSuffix();
        textCostMessage.setText(message);
        amount = costGrivna;
        if (viewModel != null) {
            viewModel.persistDisplayCostGrivna(costGrivna);
        }
        Logger.d(context, TAG, "applyDisplayCostToFinishUi: " + message);
    }

    @Nullable
    private String resolveReconcileDisplayCost(boolean explicitSelection,
            @Nullable String persistedCost,
            @Nullable Map<String, String> bundle) {
        String bundleCost = null;
        if (bundle != null) {
            bundleCost = bundle.get("order_cost");
            if (bundleCost == null || bundleCost.trim().isEmpty()) {
                bundleCost = bundle.get("orderWeb");
            }
        }
        if (bundleCost != null) {
            bundleCost = bundleCost.trim();
        }
        if (bundleCost == null || bundleCost.isEmpty()) {
            return persistedCost;
        }
        if (persistedCost == null || persistedCost.isEmpty() || explicitSelection) {
            return bundleCost;
        }
        try {
            int fromBundle = (int) Math.round(Double.parseDouble(bundleCost.replace(',', '.')));
            int fromPersist = (int) Math.round(Double.parseDouble(persistedCost.replace(',', '.')));
            return fromBundle >= fromPersist ? bundleCost : persistedCost;
        } catch (NumberFormatException e) {
            return bundleCost;
        }
    }

    /**
     * После доплаты / возврата из фона: uid и сумма из prefs/ViewModel.
     * При открытии из списка «В работе» приоритет у выбранного заказа, а не у сохранённого uid.
     */
    private void reconcileOrderIdentityFromPersistedState() {
        String persistedActive = ExecutionStatusViewModel.getPersistedActiveUid();
        String liveUid = viewModel != null ? viewModel.getUid().getValue() : null;
        if (navigationOrderUid != null && persistedActive != null
                && !navigationOrderUid.equals(persistedActive)
                && liveUid != null && liveUid.equals(persistedActive)) {
            navigationOrderUid = persistedActive;
        }
        boolean explicitSelection = navigationOrderUid != null && !navigationOrderUid.isEmpty();
        boolean orderSwitch = explicitSelection && persistedActive != null
                && !navigationOrderUid.equals(persistedActive);

        if (ExecutionStatusViewModel.isCancelInFlightPref() && !orderSwitch) {
            cancelRequestInFlight = true;
            setCancelButtonBusy(true);
            if (text_status != null) {
                text_status.clearAnimation();
                text_status.setText(R.string.sent_cancel_message);
            }
        }

        if (orderSwitch) {
            stopCycle();
            isTaskCancelled = false;
            statusPollPaused = false;
            canceled = false;
            cancel_btn_click = false;
            cancelRequestInFlight = false;
            setCancelButtonBusy(false);
            uid = navigationOrderUid;
            String bundleDouble = receivedMap != null
                    ? receivedMap.get("dispatching_order_uid_Double") : null;
            if (bundleDouble != null && !bundleDouble.trim().isEmpty()) {
                uid_Double = bundleDouble;
            } else {
                uid_Double = "";
            }
            ExecutionStatusViewModel.switchActiveOrderSession(navigationOrderUid, uid_Double);
            if (viewModel != null) {
                viewModel.restoreUidFromPersisted(navigationOrderUid, uid_Double);
            }
            if (receivedMap != null) {
                receivedMap.put("dispatching_order_uid", navigationOrderUid);
                receivedMap.put("dispatching_order_uid_Double", uid_Double);
            }
            Logger.d(context, TAG, "reconcile: switched to selected order " + navigationOrderUid);
        } else if (explicitSelection) {
            uid = navigationOrderUid;
            MainActivity.uid = navigationOrderUid;
            if (uid_Double == null && receivedMap != null) {
                uid_Double = receivedMap.get("dispatching_order_uid_Double");
            }
            if (navigationOrderUid.equals(persistedActive)) {
                String persistedDouble = ExecutionStatusViewModel.getPersistedDoubleUid();
                if (persistedDouble != null && !persistedDouble.trim().isEmpty()) {
                    uid_Double = persistedDouble;
                }
            }
            if (uid_Double != null) {
                MainActivity.uid_Double = uid_Double;
            }
            if (viewModel != null) {
                viewModel.restoreUidFromPersisted(navigationOrderUid, uid_Double);
            }
            if (receivedMap != null) {
                receivedMap.put("dispatching_order_uid", navigationOrderUid);
                if (uid_Double != null) {
                    receivedMap.put("dispatching_order_uid_Double", uid_Double);
                }
            }
        } else if (persistedActive != null) {
            uid = persistedActive;
            MainActivity.uid = persistedActive;
            String persistedDouble = ExecutionStatusViewModel.getPersistedDoubleUid();
            if (persistedDouble != null && !persistedDouble.trim().isEmpty()) {
                uid_Double = persistedDouble;
                MainActivity.uid_Double = persistedDouble;
            }
            if (viewModel != null) {
                viewModel.restoreUidFromPersisted(persistedActive, uid_Double);
            }
            if (receivedMap != null) {
                receivedMap.put("dispatching_order_uid", persistedActive);
                if (uid_Double != null) {
                    receivedMap.put("dispatching_order_uid_Double", uid_Double);
                }
            }
        }

        String displayCost = resolveReconcileDisplayCost(
                explicitSelection,
                ExecutionStatusViewModel.getPersistedDisplayCost(),
                receivedMap);
        if (displayCost != null && textCostMessage != null
                && !orderSwitch && uid != null
                && (persistedActive == null || uid.equals(persistedActive))) {
            applyDisplayCostToFinishUi(displayCost);
        }
        String syncedDouble = ExecutionStatusViewModel.getPersistedDoubleUid();
        if (syncedDouble != null && !syncedDouble.trim().isEmpty()) {
            uid_Double = syncedDouble;
            MainActivity.uid_Double = syncedDouble;
            if (receivedMap != null) {
                receivedMap.put("dispatching_order_uid_Double", syncedDouble);
            }
        }
        PassengerNotifier.syncWeatherNoticeWithFinishUids(uid, uid_Double);
    }

    @Nullable
    private String resolveActiveOrderUid() {
        if (uid != null && !uid.isEmpty()) {
            return uid;
        }
        if (navigationOrderUid != null && !navigationOrderUid.isEmpty()) {
            return navigationOrderUid;
        }
        if (viewModel != null) {
            String vmUid = viewModel.getUid().getValue();
            if (vmUid != null && !vmUid.isEmpty()) {
                return vmUid;
            }
        }
        if (MainActivity.uid != null && !MainActivity.uid.isEmpty()) {
            return MainActivity.uid;
        }
        String persisted = ExecutionStatusViewModel.getPersistedActiveUid();
        if (persisted != null) {
            return persisted;
        }
        return null;
    }

    /** После доплаты наличными — два uid, отменять нужно парой (webordersCancelDouble). */
    private boolean hasLinkedDoubleOrder() {
        if (uid_Double == null) {
            return false;
        }
        String trimmed = uid_Double.trim();
        return !trimmed.isEmpty();
    }

    @Nullable
    private String resolveWfpOrderReference() {
        String activeUid = resolveActiveOrderUid();
        String saved = PaymentSessionHelper.getWfpOrderRef(activeUid);
        if (saved != null) {
            MainActivity.order_id = saved;
            return saved;
        }
        if (MainActivity.order_id != null && !MainActivity.order_id.isEmpty()) {
            return MainActivity.order_id;
        }
        if (MainActivity.invoiceId != null && !MainActivity.invoiceId.isEmpty()) {
            return MainActivity.invoiceId;
        }
        Logger.w(context, TAG, "resolveWfpOrderReference: empty for uid=" + activeUid);
        return null;
    }

    /**
     * WFP: нет hold на сервере = оплата не прошла — шторка сразу, без ожидания push/checkStatus.
     */
    private void verifyPaymentHoldOnEnter(int holdGen) {
        if (!isAdded() || canceled || uid == null || uid.isEmpty()) {
            return;
        }
        if (!PaymentTypeHelper.usesWalletHold(pay_method)) {
            return;
        }

        String baseUrlValue = (String) sharedPreferencesHelperMain.getValue(
                "baseUrl", "https://m.easy-order-taxi.site");
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrlValue)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        if (holdVerifyCall != null && !holdVerifyCall.isCanceled()) {
            holdVerifyCall.cancel();
        }
        holdVerifyCall = retrofit.create(APIHoldService.class).verifyHold(uid);
        holdVerifyCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<HoldResponse> call, @NonNull Response<HoldResponse> response) {
                if (holdGen != holdCheckGeneration || !isAdded() || canceled) {
                    return;
                }
                String result = response.isSuccessful() && response.body() != null
                        ? response.body().getResult()
                        : null;
                Logger.d(context, TAG, "verifyHold on enter: " + result);
                if ("hold".equals(result)) {
                    holdVerifiedOnEnter = true;
                    clearDeclinedPaymentUi();
                } else {
                    Logger.d(context, TAG,
                            "verifyHold on enter: no active hold — wait for checkStatus (capture is OK)");
                }
            }

            @Override
            public void onFailure(@NonNull Call<HoldResponse> call, @NonNull Throwable t) {
                if (holdGen != holdCheckGeneration || !isAdded() || call.isCanceled()) {
                    return;
                }
                Logger.w(context, TAG, "verifyHold on enter failed: " + t.getMessage());
            }
        });
    }

    /**
     * При заходе на финиш: verifyHold + checkStatus для карты, затем шторка или снятие Declined.
     */
    private void refreshPaymentStatusOnEnter() {
        if (!isAdded() || canceled || isViewingCompletedOrder()) {
            return;
        }

        if (isOrderDispatched()) {
            PendingTransactionHelper.clear();
            clearDeclinedPaymentUi();
            if (uid != null && !uid.isEmpty()) {
                try {
                    statusOrder();
                } catch (ParseException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
            }
            return;
        }

        paymentCheckGeneration++;
        holdCheckGeneration++;
        holdVerifiedOnEnter = false;
        final int checkGen = paymentCheckGeneration;
        final int holdGen = holdCheckGeneration;
        final boolean pendingDeclined = PendingTransactionHelper.hasPendingDeclinedForActiveOrder();
        final boolean knownFailure = pendingDeclined || hasKnownPaymentFailure();

        String orderUid = resolveActiveOrderUid();
        if (orderUid != null) {
            uid = orderUid;
        }

        if (retrofitCall != null && !retrofitCall.isCanceled()) {
            retrofitCall.cancel();
        }

        verifyPaymentHoldOnEnter(holdGen);

        if (isCardPayMethod()) {
            paymentCheckInProgress = true;
            requestWfpPaymentStatusCheck(true, checkGen, pendingDeclined, knownFailure);
        }

        if (uid != null && !uid.isEmpty()) {
            try {
                Logger.d(context, TAG, "refreshPaymentStatusOnEnter: statusOrder");
                statusOrder();
            } catch (ParseException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        } else {
            Logger.w(context, TAG, "refreshPaymentStatusOnEnter: uid empty");
        }
    }

    private void requestWfpPaymentStatusCheck() {
        requestWfpPaymentStatusCheck(false, paymentCheckGeneration, false, false);
    }

    private void requestWfpPaymentStatusCheck(
            boolean fastOnResume,
            int checkGen,
            boolean pendingDeclined,
            boolean knownFailure
    ) {
        String orderRef = resolveWfpOrderReference();
        if (!isAdded() || orderRef == null) {
            paymentCheckInProgress = false;
            Logger.w(context, TAG, "checkStatus skipped: no orderReference");
            return;
        }
        Logger.d(context, TAG, "checkStatus orderRef=" + orderRef);
        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        if (listCity.size() < 2) {
            paymentCheckInProgress = false;
            return;
        }
        String city = listCity.get(1);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        if (fastOnResume) {
            clientBuilder
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(8, TimeUnit.SECONDS)
                    .writeTimeout(8, TimeUnit.SECONDS);
        } else {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            clientBuilder
                    .addInterceptor(new RetryInterceptor())
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);
        }
        OkHttpClient client = clientBuilder.build();

        String baseUrlValue = (String) sharedPreferencesHelperMain.getValue(
                "baseUrl", "https://m.easy-order-taxi.site");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrlValue + "/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        StatusService service = retrofit.create(StatusService.class);
        if (wfpStatusCheckCall != null && !wfpStatusCheckCall.isCanceled()) {
            wfpStatusCheckCall.cancel();
        }
        wfpStatusCheckCall = service.checkStatus(
                context.getString(R.string.application),
                city,
                orderRef
        );
        wfpStatusCheckCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<StatusResponse> call,
                                   @NonNull Response<StatusResponse> response) {
                if (checkGen != paymentCheckGeneration || !isAdded()) {
                    return;
                }
                paymentCheckInProgress = false;
                String orderStatus = response.body() != null
                        ? response.body().getTransactionStatus()
                        : null;
                Logger.d(context, TAG, "WFP checkStatus on enter: " + orderStatus);
                applyPaymentStatusFromServer(orderStatus, pendingDeclined, knownFailure);
            }

            @Override
            public void onFailure(@NonNull Call<StatusResponse> call, @NonNull Throwable t) {
                if (checkGen != paymentCheckGeneration || !isAdded()) {
                    return;
                }
                paymentCheckInProgress = false;
                if (!call.isCanceled()) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                    Logger.w(context, TAG, "WFP checkStatus failed, pendingDeclined=" + pendingDeclined);
                }
                if (holdVerifiedOnEnter || isPaymentVerified()) {
                    Logger.d(context, TAG, "checkStatus failed but payment verified — ignore");
                    return;
                }
                Logger.w(context, TAG, "checkStatus failed — skip declined UI without confirmed status");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        timeUtils = new TimeUtils(required_time, viewModel);
        timeUtils.startTimer();

//        btn_cancel_order.setOnClickListener(v ->
//                showCancelDialog());

        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
        if(pay_method.equals("nal_payment")) {
            timeCheckOutAddCost = 60*1000;
        } else  {
            timeCheckOutAddCost =  75*1000;
        }

        Logger.d(context, TAG, "pay_method " + pay_method);

        addCheck(context);
        reconcileOrderIdentityFromPersistedState();
        if (shouldIgnoreStatusPollingUi()) {
            if (isViewingCompletedOrder()) {
                Logger.d(context, TAG, "onResume: archived completed order — read-only");
                applyArchivedCompletedOrderUi();
            } else {
                Logger.d(context, TAG, "onResume: skip polling — cancel UI active");
                restoreOrderAgainButton();
            }
            btn_open.setOnClickListener(v -> btnOpen());
            return;
        }
        isTaskRunning = false;
        statusPollPaused = false;
        delayMillisStatus = 5 * 1000;
        if (handlerStatus != null) {
            handlerStatus.removeCallbacks(myTaskStatus);
        }
        refreshPaymentStatusOnEnter();
        if (!canceled && !isOrderDispatched()) {
            isTaskCancelled = false;
            startCycle();
        } else {
            Logger.d(context, TAG, "onResume: skip status poll restart, canceled="
                    + canceled + " dispatched=" + isOrderDispatched());
        }

        btn_open.setOnClickListener(v -> btnOpen());
        applyActiveOrderCloseMode();
        startAddCostDialog (timeCheckOutAddCost);

        applyPendingAddCostIfNeeded();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Отменяем выполнение Runnable, если фрагмент уходит в фон
//        EventBus.getDefault().unregister(this); // Удаление подписчика
        stopCycle();

//        if (handlerAddcost != null) {
//            handlerAddcost.removeCallbacks(showDialogAddcost);
//        }
        cancelShowDialogAddCost();

        if (retrofitCall != null && !retrofitCall.isCanceled()) {
            retrofitCall.cancel(); // Отмена вызова Retrofit
        }
        if (wfpStatusCheckCall != null && !wfpStatusCheckCall.isCanceled()) {
            wfpStatusCheckCall.cancel();
        }
        if (holdVerifyCall != null && !holdVerifyCall.isCanceled()) {
            holdVerifyCall.cancel();
        }
        paymentCheckGeneration++;
        holdCheckGeneration++;
        paymentCheckInProgress = false;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isResumed() && isAdded() && !shouldIgnoreStatusPollingUi()) {
            pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
            isTaskRunning = false;
            delayMillisStatus = 5 * 1000;
            if (handlerStatus != null) {
                handlerStatus.removeCallbacks(myTaskStatus);
            }
            refreshPaymentStatusOnEnter();
            if (!canceled && !isOrderDispatched()) {
                isTaskCancelled = false;
                startCycle();
            } else {
                Logger.d(context, TAG, "onHiddenChanged: skip status poll restart, canceled="
                        + canceled + " dispatched=" + isOrderDispatched());
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("LifecycleCheck", "Current lifecycle state: " + getViewLifecycleOwner().getLifecycle().getCurrentState());

        // ✅ ПРОВЕРЯЕМ, ЗАРЕГИСТРИРОВАН ЛИ УЖЕ
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
            Log.d(TAG, "EventBus registered");
        } else {
            Log.d(TAG, "EventBus already registered, skipping");
        }

        // Повторный запуск Runnable при возвращении активности
        if (action != null && !shouldIgnoreStatusPollingUi() && !isOrderDispatched()) {
            if(action.equals("Поиск авто")) {
                if (handler != null && myRunnable != null) {
                    handler.postDelayed(myRunnable, 10000);
                }

                isTaskRunning = false;
                isTaskCancelled = false;
                startCycle();

                if (handlerBonusBtn != null && runnableBonusBtn != null) {
                    handlerBonusBtn.postDelayed(runnableBonusBtn, 10000);
                }
            }
        }
    }
    private void startAddCostDialog (int timeCheckout) {
        if (isViewingCompletedOrder()) {
            return;
        }
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
        Logger.d(context, TAG, "payMetod startAddCostDialog " + pay_method);

        showDialogAddcost = () -> {
            // Вызов метода для отображения диалога
            showAddCostDialog(timeCheckout);
        };

        boolean carfound = (boolean) sharedPreferencesHelperMain.getValue("carFound", false);
        if(!carfound) {
            Logger.e(context, TAG, "required_time +++" + required_time);
            if(required_time == null  || required_time.contains("01.01.1970") || required_time.contains("1970-01-01") || required_time.isEmpty()) {
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
//            handlerAddcost = new Handler(Looper.getMainLooper());
            handlerAddcost = HandlerCompat.createAsync(Looper.getMainLooper());
            if (need_20_add) {
                if ("nal_payment".equals(pay_method) || PaymentTypeHelper.usesWalletHold(pay_method)) {
                    Logger.e(context, TAG, "status pay_method" + pay_method);
                    Logger.e(context, TAG, "status need_20_add" + need_20_add);
        
                    // Запускаем выполнение через 1 минуты (60 000 миллисекунд)
//                    handlerAddcost.postDelayed(showDialogAddcost, timeCheckout);
                    setShowDialogAddCost();
                }
            }
        }

        btnAddCost (timeCheckOutAddCost);
    }

    private void setShowDialogAddCost() {

        if (handlerAddcost != null && showDialogAddcost != null) {
            if (isTaskScheduled) {
                Log.d("HandlerDebug", "Task is already scheduled, skipping");
                return; // Не добавляем, если задача уже в очереди
            }
            Log.d("HandlerDebug", "Scheduling Runnable with delay: " + timeCheckOutAddCost);
            HandlerCompat.postDelayed(handlerAddcost, showDialogAddcost, null, timeCheckOutAddCost);
            isTaskScheduled = true; // Устанавливаем флаг
        } else {
            Log.e("HandlerDebug", "Handler or Runnable is null");
        }
    }
    private void cancelShowDialogAddCost() {
        if (handlerAddcost != null && showDialogAddcost != null) {
            Log.d("HandlerDebug", "Removing Runnable");
            handlerAddcost.removeCallbacks(showDialogAddcost);
            isTaskScheduled = false; // Сбрасываем флаг
        } else {
            Log.e("HandlerDebug", "Handler or Runnable is null");
        }
    }

    // Проверяет, открыта ли сейчас шторка добавления стоимости
    private boolean isAddCostSheetShown() {
        return addCostSheetShowing;
    }

    // Закрытие шторки добавления стоимости
    private void onAddCostSheetDismissed() {
        addCostSheetShowing = false;
        resumeStatusPolling();
    }

    private void verifyOldHold() {
        if (ExecutionStatusViewModel.isAddCostInFlightPref()) {
            Logger.d(context, TAG, "[addCost] verifyOldHold skipped: add-cost in flight");
            resumePendingAddCostIfInFlight();
            return;
        }
        Logger.d(context, TAG,
                "[addCost] verifyOldHold start"
                        + " uid=" + uid
                        + " pay_method=" + pay_method
                        + " canceled=" + canceled
                        + " statusPollPaused=" + statusPollPaused
                        + " addCostSheetShowing=" + addCostSheetShowing);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Создание клиента OkHttpClient с подключенным логгером
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.addInterceptor(new RetryInterceptor());
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
            Call<HoldResponse> call = apiService.verifyHold(uid);

            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<HoldResponse> call, @NonNull Response<HoldResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        HoldResponse holdResponse = response.body();
                        String result = holdResponse.getResult();
                        Logger.d(context, TAG, "verifyOldHold  result: " + result);
                        if ("pending_add_cost".equals(result)) {
                            if (ExecutionStatusViewModel.isAddCostInFlightPref()) {
                                new Handler(Looper.getMainLooper()).post(
                                        FinishSeparateFragment.this::resumePendingAddCostIfInFlight);
                                return;
                            }
                            Logger.w(context, TAG,
                                    "[addCost] verifyOldHold: pending_add_cost but not in flight"
                                            + " — show add-cost sheet (previous add-cost already applied)");
                            result = "hold";
                        }
                        if (result.equals("hold")) {
                            // Обработка неуспешного ответа
                            new Handler(Looper.getMainLooper()).post(() -> {
                                // Запускаем выполнение через 1 минуты (60 000 миллисекунд)
//                                if (handlerAddcost != null) {
//                                    handlerAddcost.postDelayed(showDialogAddcost, timeCheckOutAddCost);
//                                }
                                setShowDialogAddCost();

                                String text = textCostMessage.getText().toString();
                                Logger.d(getActivity(), TAG, "textCostMessage.getText().toString() " + text);

                                Pattern pattern = Pattern.compile("(\\d+)");
                                Matcher matcher = pattern.matcher(text);

                                if (matcher.find()) {
                                    Logger.d(context, TAG, "amount_to_add: " + matcher.group(1));
                                    pauseStatusPolling();
                                    MyBottomSheetAddCostFragment bottomSheetDialogFragment = new MyBottomSheetAddCostFragment(
                                            matcher.group(1),
                                            uid,
                                            uid_Double,
                                            pay_method,
                                            viewModel
                                    );
                                    bottomSheetDialogFragment.setOnDismissListener(
                                            FinishSeparateFragment.this::onAddCostSheetDismissed);
                                    addCostSheetShowing = true;
                                    bottomSheetDialogFragment.show(fragmentManager, TAG_ADD_COST_SHEET);
                                } else {
                                    Logger.d(context, TAG, "No numeric value found in the text.");
                                }
                            });

                        } else {
                            Logger.w(context, TAG,
                                    "[addCost] verifyOldHold: not hold (" + result + ")");
                            if (ExecutionStatusViewModel.isAddCostInFlightPref()) {
                                new Handler(Looper.getMainLooper()).post(FinishSeparateFragment.this::resumePendingAddCostIfInFlight);
                                return;
                            }
                            new Handler(Looper.getMainLooper()).post(() -> {
                                String text = textCostMessage.getText().toString();
                                Logger.d(getActivity(), TAG, "textCostMessage.getText().toString() " + text);
                                Pattern pattern = Pattern.compile("(\\d+)");
                                Matcher matcher = pattern.matcher(text);
                                if (matcher.find()) {
                                    Logger.d(context, TAG, "amount_to_add: " + matcher.group(1));
                                    pauseStatusPolling();
                                    MyBottomSheetAddCostFragment bottomSheetDialogFragment = new MyBottomSheetAddCostFragment(
                                            matcher.group(1),
                                            uid,
                                            uid_Double,
                                            pay_method,
                                            viewModel
                                    );
                                    bottomSheetDialogFragment.setOnDismissListener(
                                            FinishSeparateFragment.this::onAddCostSheetDismissed);
                                    addCostSheetShowing = true;
                                    bottomSheetDialogFragment.show(fragmentManager, TAG_ADD_COST_SHEET);
                                } else {
                                    Logger.d(context, TAG, "No numeric value found in the text.");
                                    resumeStatusPolling();
                                }
                            });

                        }

                    } else {
                        // Обработка неуспешного ответа
                        new Handler(Looper.getMainLooper()).post(() -> text_status.setText(R.string.recounting_order));
                        resumeStatusPolling();
                        setShowDialogAddCost();
                        Logger.e(context, TAG, "[addCost] verifyOldHold: unsuccessful response code=" + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<HoldResponse> call, @NonNull Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                    resumeStatusPolling();
                    setShowDialogAddCost();
                    Logger.e(context, TAG, "[addCost] verifyOldHold failed: " + t.getMessage());
                }
            });



    }

    private void showAddCostDialog(int timeCheckout) {
        // Убедитесь, что handlerAddcost не null и очищаем предыдущие задачи
//        if (handlerAddcost != null) {
//            handlerAddcost.removeCallbacks(showDialogAddcost);
//        }
        boolean add_show_flag = (boolean) sharedPreferencesHelperMain.getValue("add_show_flag", true);
        Log.d("add_show_flag", String.valueOf(add_show_flag));

        if (!add_show_flag) {
            Logger.w(context, TAG, "[addCost] showAddCostDialog skipped: add_show_flag=false");
            return;
        }
        // Не показываем диалог повторно, если он уже открыт
        if (addCostDialog != null && addCostDialog.isShowing()) {
            Log.d(TAG, "showAddCostDialog skipped: dialog already showing");
            Logger.w(context, TAG, "[addCost] showAddCostDialog skipped: dialog already showing");
            return;
        }
        // Не показываем диалог, если пользователь уже поднял шторку добавления стоимости
        if (isAddCostSheetShown()) {
            Log.d(TAG, "showAddCostDialog skipped: add cost bottom sheet already shown");
            Logger.w(context, TAG, "[addCost] showAddCostDialog skipped: addCostSheetShowing=true");
            return;
        }
        cancelShowDialogAddCost();
        pauseStatusPolling();
        // Убедитесь, что фрагмент добавлен

        if (!isAdded() || getActivity() == null) {
            Logger.w(context, TAG, "[addCost] showAddCostDialog aborted: fragment not added/activity null");
            return;
        }

        AlertDialog dialog = new UklonAlertDialog(context)
                .setIcon(R.drawable.ic_info)
                .setTitle(R.string.add_cost_fin)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_button, d -> {
                    if (PaymentTypeHelper.usesWalletHold(pay_method)) {
                        verifyOldHold();
                        return;
                    }
                    if ("nal_payment".equals(pay_method)) {
                        viewModel.setCancelStatus(false);
                        setShowDialogAddCost();
                        String text = textCostMessage.getText().toString();
                        Logger.d(getActivity(), TAG, "textCostMessage.getText().toString() " + text);

                        Pattern pattern = Pattern.compile("(\\d+)");
                        Matcher matcher = pattern.matcher(text);

                        if (matcher.find()) {
                            Logger.d(context, TAG, "amount_to_add: " + matcher.group(1));
                            pauseStatusPolling();
                            MyBottomSheetAddCostFragment bottomSheetDialogFragment = new MyBottomSheetAddCostFragment(
                                    matcher.group(1),
                                    uid,
                                    uid_Double,
                                    pay_method,
                                    viewModel
                            );
                            bottomSheetDialogFragment.setOnDismissListener(this::onAddCostSheetDismissed);
                            addCostSheetShowing = true;
                            bottomSheetDialogFragment.show(fragmentManager, TAG_ADD_COST_SHEET);
                        } else {
                            Logger.d(context, TAG, "No numeric value found in the text.");
                        }
                    }
                })
                .setNegativeButton(R.string.cancel_button, d -> {
                    setShowDialogAddCost();
                    resumeStatusPolling();
                })
                .create();
        addCostDialog = dialog;
        dialog.show();
    }

    private void showCancelDialog() {
        Logger.d(context, TAG, "btn_cancel_order.isEnabled(): " + btn_cancel_order.isEnabled());
        if (!btn_cancel_order.isEnabled()) {
            Toast.makeText(context, R.string.cancel_btn_enable, Toast.LENGTH_LONG).show();
        } else {
            viewModel.setStatusNalUpdate(true);
//            if (handlerAddcost != null) {
//                handlerAddcost.removeCallbacks(showDialogAddcost);
//            }
            cancelShowDialogAddCost();

            new UklonAlertDialog(context)
                    .setIcon(R.drawable.ic_baseline_delete_24)
                    .setIconTint(R.color.error_red)
                    .setTitle(R.string.add_cost_cancel)
                    .setCancelable(false)
                    .setPositiveDestructive(true)
                    .setPositiveButton(R.string.ok_button, d ->
                            submitOrderCancelRequest(context.getString(R.string.ex_st_canceled)))
                    .setNegativeButton(R.string.cancel_button, null)
                    .show();
        }
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
                    submitOrderCancelRequest(context.getString(R.string.ex_st_canceled));
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
        // Опции берём из данных заказа (extra_charge_codes), а не из текущего выбора в таблице
        if (extra_charge_codes != null && !extra_charge_codes.trim().isEmpty()) {
            for (String code : extra_charge_codes.split(",")) {
                String c = code.trim();
                if (!c.isEmpty()
                        && !c.equals("no_extra_charge_codes")
                        && !c.equals("*")) {
                    newCheck++;
                }
            }
        }
        Log.d("addCheck", "newCheck 1: " + newCheck);

        // Тариф заказа (без перезаписи текущим выбором из настроек)
        String tarif = (flexible_tariff_name != null) ? flexible_tariff_name : " ";
        Log.d("addCheck", "tarif " + tarif);
        if (!tarif.equals(" ")
                && !tarif.trim().isEmpty()
                && !tarif.equals(context.getResources().getString(R.string.start_t))) {
            newCheck++;
        }
        Log.d("addCheck", "newCheck 2: " + newCheck);
        Log.d("addCheck", "comment_info " + "/" + comment_info + "/");

        if (!comment_info.equals("no_comment")
                && !comment_info.isEmpty()
                && !comment_info.equals(" ")
                && !comment_info.equals("no_name ")
        ) {
            newCheck++;
        }
        Log.d("addCheck", "newCheck 3: " + newCheck);

        String mes = context.getString(R.string.add_services);
        if (newCheck != 0) {
            mes = context.getString(R.string.add_services) + " (" + newCheck + ")";
        }
        btn_options.setText(mes);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPreferencesHelperMain.saveValue("carFound", false);
    }


    private void startPointShow () {
        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor cursor = database.rawQuery(query, null);

        cursor.moveToFirst();

        // Получите значения полей из первой записи

        @SuppressLint("Range") double startLat = CursorReadHelper.getDouble(cursor, "startLat");
        @SuppressLint("Range") double startLan = CursorReadHelper.getDouble(cursor, "startLan");
        @SuppressLint("Range") String start = CursorReadHelper.getString(cursor, "start");

        cursor.close();
        database.close();
        Logger.d(context, TAG, "startPointShow:settings finish " + start);

        // Формируем URI с координатами и названием точки
        String uri = "geo:" + startLat + "," + startLan + "?q=" + startLat + "," + startLan + "(" + Uri.encode(start) + ")"+ "?z=21";

// Создаем Intent
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");

// Проверка, установлен ли Google Maps
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, R.string.google_maps, Toast.LENGTH_SHORT).show();
        }

    }
    private void showFinishCost(Context context) {
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);

        String api = logCursor(MainActivity.CITY_INFO, context).get(2);

        baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") +"/";

        String url = baseUrl + api + "/android/showFinishCost/" + uid;

        Call<FinishCostResponse> call = ApiClient.getApiService().showFinishCost(url);
        Logger.d(context, TAG, "showFinishCost: " + url);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<FinishCostResponse> call, @NonNull Response<FinishCostResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    FinishCostResponse finishCostResponse = response.body();
                    Logger.d(context, TAG, "showFinishCost finishCostResponse: " + finishCostResponse);
                    if(finishCostResponse.getFinishCost() != 0) {
                        String finishCost = String.valueOf((int) finishCostResponse.getFinishCost());
                        String messageResultCost = getString(R.string.lbl_amount_finish) + " " + finishCost + getString(R.string.UAH);
                        if(pay_method.equals("nal_payment")) {
                            messageResultCost += " " + getString(R.string.pay_method_message_nal);
                        }
                        if (PaymentTypeHelper.usesWalletHold(pay_method)) {
                            messageResultCost += " " + getString(
                                    PaymentTypeHelper.isGooglePay(pay_method)
                                            ? R.string.pay_method_message_google
                                            : R.string.pay_method_message_card);
                        }
                        if(pay_method.equals("bonus_payment")) {
                            messageResultCost += " " + getString(R.string.pay_method_message_bonus);
                        }
                        textCostMessage.setText(messageResultCost);
                        textCostMessage.setVisibility(VISIBLE);
                    }
                } else {
                    Logger.d(context, TAG, "Ошибка ответа: " + response.code());
                }
            }


            @Override
            public void onFailure(@NonNull Call<FinishCostResponse> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.d(context, TAG, "onFailure: " + errorMessage);
            }
        });

        }

}