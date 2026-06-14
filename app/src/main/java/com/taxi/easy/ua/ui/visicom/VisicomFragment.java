package com.taxi.easy.ua.ui.visicom;


import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.CITY_INFO;
import static com.taxi.easy.ua.MainActivity.activeCalls;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.MainActivity.firstStart;
import static com.taxi.easy.ua.MainActivity.navController;
import static com.taxi.easy.ua.MainActivity.orderViewModel;
import static com.taxi.easy.ua.androidx.startup.MyApplication.getCurrentActivity;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.taxi.easy.ua.utils.ui.CostCalculationProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.gms.wallet.PaymentsClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.dialog.UklonAlertDialog;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.FragmentVisicomBinding;
import com.taxi.easy.ua.service.OrderServiceResponse;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.utils.animation.car.CarProgressBar;
import com.taxi.easy.ua.utils.auth.FirebaseConsentManager;
import com.taxi.easy.ua.utils.blacklist.BlacklistManager;
import com.taxi.easy.ua.ui.home.ButtonVisibilityCallback;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetBonusFragment;
import com.taxi.easy.ua.utils.payment.GooglePayOrderHelper;
import com.taxi.easy.ua.utils.payment.PaymentSessionHelper;
import com.taxi.easy.ua.utils.helpers.WfpGooglePayHelper;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyPhoneDialogFragment;
import com.taxi.easy.ua.utils.bugreport.BugReportHelper;
import com.taxi.easy.ua.utils.city.CityFinder;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.cost.CostParseHelper;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.download.AppUpdater;
import com.taxi.easy.ua.utils.from_json_parser.FromJSONParserRetrofit;
import com.taxi.easy.ua.utils.ip.RetrofitClient;
import com.taxi.easy.ua.utils.kafka.KafkaRequest;
import com.taxi.easy.ua.utils.order.EarlyOrderNavigationHelper;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.location.AutoLocationAfterCityHelper;
import com.taxi.easy.ua.utils.location.TaxiLocationValidator;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.orders.OrderCreatedAtDisplayHelper;
import com.taxi.easy.ua.utils.orders.OrderHistoryStatusHelper;
import com.taxi.easy.ua.utils.orders.RequiredTimeParseHelper;
import com.taxi.easy.ua.utils.route.RoutePlaceMatcher;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.utils.phone_state.PhoneCallHelper;
import com.taxi.easy.ua.utils.retrofit.cost_json_parser.CostJSONParserRetrofit;
import com.taxi.easy.ua.utils.sanitizer.InputSanitizerHelper;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;
import com.taxi.easy.ua.utils.worker.InclusiveTransportPreferenceWorker;
import com.taxi.easy.ua.utils.worker.TilePreloadWorker;
import com.uxcam.UXCam;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.taxi.easy.ua.utils.db.CursorReadHelper;

public class VisicomFragment extends Fragment implements ButtonVisibilityCallback {


    @SuppressLint("StaticFieldLeak")
    public static CostCalculationProgressBar progressBar;

    private FragmentVisicomBinding binding;
    private static final String TAG = "VisicomFragment";
    private static final String ADDR_GUARD = "AddrGuard";


    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btn_minus, btn_plus, btnOrder, buttonBonus, gpsBtn, btnCallAdmin, btnCallAdminFin;
    @SuppressLint("StaticFieldLeak")
    public static TextView geoText;
    static String api;

    public static long firstCost;

    @SuppressLint("StaticFieldLeak")
    public static TextView text_view_cost;
    private static final String PREF_COST_RECALC_FROM_HISTORY = "cost_recalc_from_history";
    private static final String PREF_COST_RECALC_FROM_FINISH = "cost_recalc_from_finish";
    private static final String PREF_COST_PREVIEW_DISPLAY = "cost_preview_display";
    private TextView textCostRecalcStatus;
    @SuppressLint("StaticFieldLeak")
    public static TextView textViewTo;
    @SuppressLint("StaticFieldLeak")
    public static EditText to_number;
    public static String numberFlagTo;

    public static long cost;
    public static long addCost;
    public static String to;
    public static String geo_marker;
    static String pay_method;
    public static String urlOrder;
    public static long MIN_COST_VALUE;
    public static long firstCostForMin;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btnAdd;
    @SuppressLint("StaticFieldLeak")
    static ImageButton btn1;
    @SuppressLint("StaticFieldLeak")
    static ImageButton btn2;
    @SuppressLint("StaticFieldLeak")
    static ImageButton btn3;

    @SuppressLint("StaticFieldLeak")
    public static TextView textwhere, num2;
    private AlertDialog alertDialog;
    @SuppressLint("StaticFieldLeak")
    public static TextView textfrom;
    @SuppressLint("StaticFieldLeak")
    public static TextView num1;

    @SuppressLint("StaticFieldLeak")
    static ConstraintLayout linearLayout;
    Activity context;
    static FragmentManager fragmentManager;

    @SuppressLint("StaticFieldLeak")
    static FrameLayout frame_1;
    @SuppressLint("StaticFieldLeak")
    static FrameLayout frame_2;
    @SuppressLint("StaticFieldLeak")
    static FrameLayout frame_3;
    @SuppressLint("StaticFieldLeak")
    public static TextView schedule;
    @SuppressLint("StaticFieldLeak")
    static ImageButton shed_down;
    @SuppressLint("StaticFieldLeak")
    static ConstraintLayout constr2;
    static ConstraintLayout linear_layout_buttons;
    private List<RouteResponseCancel> routeListCancel;
    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;
    public static Map<String, String> sendUrlMap;
    public static ConstraintLayout constraintLayoutVisicomMain, constraintLayoutVisicomFinish;
    @SuppressLint("StaticFieldLeak")
    public static TextView text_full_message, textCostMessage, textStatusCar;

    private static String baseUrl;

    private CarProgressBar carProgressBar;
    @SuppressLint("StaticFieldLeak")
    static TextView svButton;

    public static  long startCost;
    public static  long finalCost;
    private ExecutionStatusViewModel viewModel;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<IntentSenderRequest> googlePayLauncher;
    private PaymentsClient googlePayPaymentsClient;
    private boolean googlePayOrderHoldInProgress;
    private String pendingGooglePayMerchant;
    private String pendingGooglePayAmount;
    private String pendingGooglePayOrderReference;
    /** Стоимость на момент orderRout(); не сбрасывается при visicomCost после GPay. */
    private String pendingOrderDisplayCost;
    private boolean location_update;
    LocationManager locationManager;

    private Handler costHandler;
    private Runnable reserveRunnable;
    private String lastCost = null;
    static SwipeRefreshLayout swipeRefreshLayout;
    private LifecycleObserver lifecycleObserver;
    private boolean appProcessWasInBackground = false;
    private static final long VISICOM_COST_DEBOUNCE_MS = 1200;
    private static final long VISICOM_COST_LAST_ADDRESS_COOLDOWN_MS = 30_000;
    private static final long NETWORK_RESTORE_RELOAD_COOLDOWN_MS = 12_000;
    private long lastVisicomCostRequestMs = 0;
    private long lastCostCalculationFailureMs = 0;

    private static final double DEFAULT_LAT = 50.4501; // Киев по умолчанию
    private static final double DEFAULT_LON = 30.5234;
    private boolean isUpdatingFromGPS = false;
    /** Авто-геолокация после выбора города (для состояния кнопки GPS). */
    private boolean autoLocationFromCityLoad = false;
    private long lastSuccessfulLocationTime = 0;
    private String lastProcessedAddress = "";
    private String pendingAddressRequest = null;
    /**
     * Пользователь нажал GPS, пока авто-GPS (после города) ещё определяет адрес.
     * В этом случае применяем найденные координаты сразу по завершению авто-определения,
     * чтобы не брать кэшированную локацию.
     */
    private boolean gpsClickAwaitingAutoDetected = false;
    private static boolean isFragmentVisible = false;

    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btnReset;
    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btnReport;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVisicomBinding.inflate(inflater, container, false);
//        setupImages();
        UXCam.tagScreenName(TAG);

        View root = binding.getRoot();
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                    boolean locationGranted = fineGranted && coarseGranted;

                    for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                        String permission = entry.getKey();
                        boolean granted = entry.getValue();
                        sharedPreferencesHelperMain.saveValue(permission, granted ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED);
                    }

                    int permissionRequestCount = loadPermissionRequestCount();
                    permissionRequestCount++;
                    savePermissionRequestCount(permissionRequestCount);
                    Logger.d(context, "loadPermission", "permissionRequestCount: " + permissionRequestCount);

                    if (locationGranted) {
                        location_update = true;
                        AutoLocationAfterCityHelper.markEverGranted();
                        if (AutoLocationAfterCityHelper.isPending()) {
                            AutoLocationAfterCityHelper.clearPending();
                            startAutoLocationAfterCityIfPossible();
                        }
                    } else if (AutoLocationAfterCityHelper.isPending()) {
                        AutoLocationAfterCityHelper.clearPending();
                        applyLastOrderAddressFromRouteMarker();
                    }
                }
        );
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
                                submitGooglePayHoldCharge(paymentDataJson);
                            }

                            @Override
                            public void onCancelled() {
                                onGooglePayOrderHoldCancelled();
                            }

                            @Override
                            public void onError(@NonNull String message) {
                                onGooglePayOrderHoldFailed(message);
                            }
                        }
                )
        );
        // Включаем блокировку кнопки "Назад" Применяем блокировку кнопки "Назад"
        BackPressBlocker backPressBlocker = new BackPressBlocker();
        backPressBlocker.setBackButtonBlocked(true);
        backPressBlocker.blockBackButtonWithCallback(this);

        constraintLayoutVisicomMain = root.findViewById(R.id.visicomMain);
        constraintLayoutVisicomFinish = root.findViewById(R.id.visicomFinish);

        constraintLayoutVisicomFinish.setVisibility(GONE);


        text_full_message = root.findViewById(R.id.text_full_message);
        textCostMessage = root.findViewById(R.id.text_cost_message);
        textStatusCar = root.findViewById(R.id.text_status);

        carProgressBar = root.findViewById(R.id.carProgressBar);


        context = requireActivity();
        binding.textwhere.setVisibility(VISIBLE);

        swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        svButton = root.findViewById(R.id.sv_button);
        text_view_cost = binding.textViewCost;
        textCostRecalcStatus = binding.textCostRecalcStatus;
// Устанавливаем слушатель для распознавания жеста свайпа вниз
        swipeRefreshLayout.setOnRefreshListener(() -> {
            clearTABLE_SERVICE_INFO();
            sharedPreferencesHelperMain.saveValue("time", "no_time");
            sharedPreferencesHelperMain.saveValue("date", "no_date");
            sharedPreferencesHelperMain.saveValue("comment", "no_comment");
            sharedPreferencesHelperMain.saveValue("tarif", " ");
            sharedPreferencesHelperMain.saveValue("setStatusX", true);
            svButton.setVisibility(GONE);
            sharedPreferencesHelperMain.saveValue("old_cost", "0");

            if (NetworkUtils.isNetworkAvailable(context)) {
                reloadOrderAfterNetworkRestored();
                swipeRefreshLayout.postDelayed(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    svButton.setVisibility(VISIBLE);
                }, 500);
            } else {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).syncNetworkBanner();
                }
                startActivity(new Intent(context, MainActivity.class));
                swipeRefreshLayout.postDelayed(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    svButton.setVisibility(VISIBLE);
                }, 500);
            }
        });


        fragmentManager = getParentFragmentManager();
        progressBar = binding.progressBar;
        linearLayout = binding.linearLayoutButtons;


        btnCallAdmin = binding.btnCallAdmin;

        btnCallAdminFin = binding.btnCallAdminFin;
        btnCallAdmin.setOnClickListener(v -> {
            PhoneCallHelper.callWithFallback(() -> {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, MyApplication.getContext());
                return stringList.size() > 3 ? stringList.get(3) : "";
            });
//            Intent intent = new Intent(Intent.ACTION_DIAL);
//            List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
//            String phone = stringList.get(3);
//            intent.setData(Uri.parse(phone));
//            startActivity(intent);
        });
        btn1 = binding.button1;
        btn2 = binding.button2;
        btn3 = binding.button3;

        frame_1 = binding.frame1;
        frame_2 = binding.frame2;
        frame_3 = binding.frame3;

        linear_layout_buttons = binding.linearLayoutButtons;

        gpsBtn = binding.gpsbut;

        schedule = binding.schedule;

        shed_down = binding.shedDown;
        Logger.d(requireActivity(), TAG, "MainActivity.firstStart" + MainActivity.firstStart);



        btnAdd = binding.btnAdd;
        constr2 = binding.constr2;

        constr2.setVisibility(GONE);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Действия при нажатии кнопки "Назад"
                sharedPreferencesHelperMain.saveValue("VisicomBackPressed", true);
                cancelAllRequests(); // Отменяем запросы
                constraintLayoutVisicomFinish.setVisibility(GONE);
                constraintLayoutVisicomMain.setVisibility(VISIBLE);
//                requireActivity().onBackPressed(); // Возвращаемся назад
            }
        });
        sharedPreferencesHelperMain.saveValue("carFound", false);
        btnReset = binding.btnReset;
        btnReport = binding.btnReport;
        return root;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.d(context,"LifecycleCheck 1", "Current lifecycle state: " + getViewLifecycleOwner().getLifecycle().getCurrentState());

        // Инициализация базовых компонентов
        setupActionBar();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Инициализация существующего ExecutionStatusViewModel (из MainActivity)
        viewModel = new ViewModelProvider(requireActivity()).get(ExecutionStatusViewModel.class);

        // Инициализация нового VisicomViewModel для этого фрагмента
        VisicomViewModel visicomViewModel = new ViewModelProvider(this).get(VisicomViewModel.class);

        // ========== РЕШЕНИЕ ДЛЯ ОТСЛЕЖИВАНИЯ ВОЗВРАТА ИЗ ФОНА ==========
        // Наблюдаем за сигналом перезагрузки стоимости
        visicomViewModel.getShouldReloadCost().observe(getViewLifecycleOwner(), shouldReload -> {
            if (shouldReload != null && shouldReload && isAdded()) {
                Logger.d(context,TAG, "Получен сигнал на перезагрузку стоимости после возврата из фона");

                // Проверяем, авторизован ли пользователь
                List<String> userInfo = logCursor(MainActivity.TABLE_USER_INFO, context);
                if (userInfo.size() > 3 && !userInfo.get(3).equals("email")) {
                    // Проверяем наличие интернета
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        // Небольшая задержка для восстановления всех сервисов
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (isAdded()) {
                                requestVisicomCost("foreground");
                                visicomViewModel.costReloaded();
                            }
                        }, 500); // Задержка 500 мс
                    } else {
                        Logger.d(context,TAG, "Нет интернета при возврате из фона");
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity()).syncNetworkBanner();
                        }
                        visicomViewModel.costReloaded();
                    }
                } else {
                    Logger.d(context,TAG, "Пользователь не авторизован, пропускаем перезагрузку");
                    visicomViewModel.costReloaded();
                }
            }
        });

        // Создаем observer для жизненного цикла приложения
        lifecycleObserver = new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onAppBackgrounded() {
                appProcessWasInBackground = true;
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onAppForegrounded() {
                if (!appProcessWasInBackground) {
                    Logger.d(context, TAG, "Process ON_START без ON_STOP — перезагрузку стоимости не ставим");
                    return;
                }
                appProcessWasInBackground = false;
                Logger.d(context, TAG, "Приложение вернулось из фона - устанавливаем сигнал перезагрузки");
                if (visicomViewModel != null) {
                    visicomViewModel.onAppForegrounded();
                }
            }
        };

        // Добавляем observer к жизненному циклу приложения
        ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);
        // ========== КОНЕЦ РЕШЕНИЯ ==========

        // Обработка кнопки button1
        if (button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }


        // Наблюдение за обновлением GPS
        viewModel.getStatusGpsUpdate().observe(getViewLifecycleOwner(), aBoolean -> {
            Logger.d(context, TAG, "StatusGpsUpdate changed: " + aBoolean);

            if (aBoolean) {
                // Если GPS обновление активно
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    // GPS включен - показываем интерфейс и получаем первую локацию
                    btnVisible(VISIBLE);
                    Logger.d(context, TAG, "onResume: 3");
                    firstLocation();
                } else {
                    // GPS выключен - пытаемся получить стоимость без GPS
                    String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                    if (!userEmail.equals("email")) {
                        try {
                            visicomCost();
                        } catch (MalformedURLException e) {
                            Logger.e(context,TAG, "Ошибка в visicomCost: " + e.getMessage());
                            throw new RuntimeException(e);
                        }
                        readTariffInfo();
                    }
                }
            }
            // Если aBoolean == false - ничего не делаем
        });
//         Отключаем GNSS проверку на эмуляторе
        if (Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("Emulator")) {
            TaxiLocationValidator.setGnssCheckDisabled(true);
            Logger.d(context,TAG, "Emulator detected! GNSS check DISABLED");
        } else {
            TaxiLocationValidator.setGnssCheckDisabled(false);
            Logger.d(context,TAG, "Real device - GNSS check ENABLED");
        }
    }
    public static void updateGpsButtonCross(boolean show) {
        if (gpsBtn != null && getCurrentActivity() != null) {
            if (show) {
                // При show=true ВСЕГДА показываем крестик
                gpsBtn.setBackground(ContextCompat.getDrawable(getCurrentActivity(), R.drawable.buttons_green_cross));
                gpsBtn.setTextColor(Color.WHITE);
                Logger.d(null, TAG, "updateGpsButtonCross: показан КРЕСТИК (show=true)");
            } else {
                // При show=false проверяем статус GPS и разрешения
                Context context = MyApplication.getContext();
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                boolean hasFineLocationPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                boolean gpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                Logger.d(null, TAG, "updateGpsButtonCross: show=false, gpsEnabled=" + gpsEnabled + ", hasPermission=" + hasFineLocationPermission);

                if (gpsEnabled && hasFineLocationPermission) {
                    // GPS включен И есть разрешение - зеленая кнопка
                    gpsBtn.setBackground(ContextCompat.getDrawable(getCurrentActivity(), R.drawable.buttons_green));
                    gpsBtn.setTextColor(Color.WHITE);
                    Logger.d(null, TAG, "updateGpsButtonCross: ЗЕЛЕНАЯ кнопка");
                } else if (gpsEnabled && !hasFineLocationPermission) {
                    // GPS включен, НЕТ разрешения - желтая кнопка
                    gpsBtn.setBackground(ContextCompat.getDrawable(getCurrentActivity(), R.drawable.buttons_yellow));
                    gpsBtn.setTextColor(Color.BLACK);
                    Logger.d(null, TAG, "updateGpsButtonCross: ЖЕЛТАЯ кнопка");
                } else {
                    // GPS выключен - красная кнопка
                    gpsBtn.setBackground(ContextCompat.getDrawable(getCurrentActivity(), R.drawable.btn_red));
                    gpsBtn.setTextColor(Color.WHITE);
                    Logger.d(null, TAG, "updateGpsButtonCross: КРАСНАЯ кнопка");
                }
            }
            gpsBtn.invalidate();
        }
    }

    private void setupActionBar() {
        // Проверяем, что активность поддерживает ActionBar
        if (requireActivity() instanceof AppCompatActivity) {
            androidx.appcompat.app.ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowCustomEnabled(true);
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setCustomView(R.layout.custom_action_bar_title);

                // Доступ к элементам кастомного ActionBar
                View customView = actionBar.getCustomView();
                TextView titleTextView = customView.findViewById(R.id.action_bar_title);
                ImageButton button1 = customView.findViewById(R.id.button1);

                // Устанавливаем заголовок (замените newTitle на нужное значение)
                setCityAppbar(); // Убедитесь, что метод существует
                String newTitle = (String) sharedPreferencesHelperMain.getValue("newTitle", ""); // Или получите из аргументов/ресурсов
                titleTextView.setText(newTitle);
                if (Objects.requireNonNull(navController.getCurrentDestination()).getId() != R.id.nav_finish_separate) {
                    button1.setVisibility(VISIBLE);
                }
                // Установка обработчиков нажатий
                View.OnClickListener clickListener = v -> {
                    Logger.d(context, TAG, "Обработчик нажатия, сеть доступна: " + NetworkUtils.isNetworkAvailable(context));
                    if (NetworkUtils.isNetworkAvailable(context)) {
                        Logger.d(context, "CityCheckFrgment", "Navigating to nav_city");
                        if (navController.getCurrentDestination().getId() != R.id.nav_finish_separate) {
                            Logger.d(context, "CityCheckFrgment", "Navigating to nav_city");
                            navController.navigate(R.id.nav_city, null, new NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_city, true)
                                    .build());
                        }
                    } else if (navController != null) {
                        Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
                        Logger.w(context, TAG, "NO INTERNET - Showing toast message");
                    } else {
                        Logger.e(context, TAG, "NavController равен null, навигация невозможна!");
                    }
                };
                if (Objects.requireNonNull(navController.getCurrentDestination()).getId() != R.id.nav_finish_separate) {
                    Logger.d(context, "CityCheckFrgment", "Navigating to nav_city");
                    titleTextView.setOnClickListener(clickListener);
                    button1.setOnClickListener(clickListener);
                }
            } else {
                Logger.e(requireActivity(), TAG, "ActionBar равен null!");
            }
        } else {
            Logger.e(requireActivity(), TAG, "Активность не является AppCompatActivity!");
        }
    }

    private void setCityAppbar()
    {
        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireContext());
        String city = stringList.get(1);
        String cityMenu;
        switch (city){
            case "Kyiv City":
                cityMenu = getString(R.string.city_kyiv);
                break;
            case "Dnipropetrovsk Oblast":
                cityMenu = getString(R.string.city_dnipro);
                break;
            case "Odessa":
                cityMenu = getString(R.string.city_odessa);
                break;
            case "Zaporizhzhia":
                cityMenu = getString(R.string.city_zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                cityMenu = getString(R.string.city_cherkassy);
                break;
            case "Lviv":
                cityMenu = getString(R.string.city_lviv);
                break;
            case "Ivano_frankivsk":
                cityMenu = getString(R.string.city_ivano_frankivsk);
                break;
            case "Vinnytsia":
                cityMenu = getString(R.string.city_vinnytsia);
                break;
            case "Poltava":
                cityMenu = getString(R.string.city_poltava);
                break;
            case "Sumy":
                cityMenu = getString(R.string.city_sumy);
                break;
            case "Kharkiv":
                cityMenu = getString(R.string.city_kharkiv);
                break;
            case "Chernihiv":
                cityMenu = getString(R.string.city_chernihiv);
                break;
            case "Rivne":
                cityMenu = getString(R.string.city_rivne);
                break;
            case "Ternopil":
                cityMenu = getString(R.string.city_ternopil);
                break;
            case "Khmelnytskyi":
                cityMenu = getString(R.string.city_khmelnytskyi);
                break;
            case "Zakarpattya":
                cityMenu = getString(R.string.city_zakarpattya);
                break;
            case "Zhytomyr":
                cityMenu = getString(R.string.city_zhytomyr);
                break;
            case "Kropyvnytskyi":
                cityMenu = getString(R.string.city_kropyvnytskyi);
                break;
            case "Mykolaiv":
                cityMenu = getString(R.string.city_mykolaiv);
                break;
            case "Chernivtsi":
                cityMenu = getString(R.string.city_chernivtsi);
                break;
            case "Lutsk":
                cityMenu = getString(R.string.city_lutsk);
                break;
            case "OdessaTest":
                cityMenu = "Test";
                break;
            default:
                cityMenu = getString(R.string.foreign_countries);
        }
        String newTitle =  getString(R.string.menu_city) + " " + cityMenu;
        sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
    }


    private void setupImages() {
        Activity activity = requireActivity();
        if (activity == null || activity.isFinishing()) {
            Logger.e(context, "NetworkMonitor", "Activity is null or finishing, skipping navigation");
            return;
        }
        // Загрузка изображений с помощью Glide с отложенным выполнением
        binding.getRoot().post(() -> {
            Glide.with(this)
                    .load(R.drawable.button_image_button1_sm)
                    .into(binding.button1);

            Glide.with(this)
                    .load(R.drawable.button_image_button2_sm)
                    .into(binding.button2);

            Glide.with(this)
                    .load(R.drawable.button_image_button3_sm)
                    .into(binding.button3);

            Glide.with(this)
                    .load(R.drawable.down_arrow_white)
                    .override(32, 32)
                    .into(binding.shedDown);
        });
    }

    private void cancelAllRequests() {
        for (Call<?> call : activeCalls) {
            if (!call.isCanceled()) {
                call.cancel();
            }
        }
        activeCalls.clear(); // Очищаем список после отмены
    }

    private void scheduleUpdate() {
        // Читаем сохранённые значения
        String savedTime = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String savedDate = (String) sharedPreferencesHelperMain.getValue("date", "no_date");

        // Если время и дата установлены, отображаем их
        if (!"no_time".equals(savedTime) && !"no_date".equals(savedDate)) {
            schedule.setText(savedDate + " " + savedTime);  // Формат: "dd.MM.yyyy HH:mm"
        } else {
            schedule.setText(R.string.on_now);  // Дефолт: "Сейчас"
        }

        // С existing код (сброс в БД, если нужно)
        if (!MainActivity.firstStart) {
            ContentValues cv = new ContentValues();
            cv.put("time", "no_time");
            cv.put("date", "no_date");
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
        }

        // С existing слушатели кликов
        schedule.setOnClickListener(v -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }
            btnVisible(GONE);
            sharedPreferencesHelperMain.saveValue("initial_page", "visicom");
            NavController navController = NavHostFragment.findNavController(VisicomFragment.this);
            navController.navigate(R.id.nav_options, null, new NavOptions.Builder().build());
        });

        shed_down.setOnClickListener(v -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }
            btnVisible(GONE);
            sharedPreferencesHelperMain.saveValue("initial_page", "visicom");
            NavController navController = NavHostFragment.findNavController(VisicomFragment.this);
            navController.navigate(R.id.nav_options, null, new NavOptions.Builder().build());
        });
    }

    public void updateApp() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                int installStatus = appUpdateInfo.installStatus();

                if (installStatus == InstallStatus.PENDING || installStatus == InstallStatus.UNKNOWN
                        || installStatus == InstallStatus.INSTALLED || installStatus == InstallStatus.FAILED
                        || installStatus == InstallStatus.CANCELED || installStatus == InstallStatus.DOWNLOADED) {

                    // Обновление доступно и можно начинать
                    showUpdateDialog(); // Показываем диалог или запускаем update flow

                    if (isAdded()) {
                        Logger.d(MyApplication.getContext(), TAG, "Available updates found and ready to start");
                    }

                } else {
                    // Установка уже в процессе (DOWNLOADING или INSTALLING)
                    Logger.d(MyApplication.getContext(), TAG, "Update already in progress. Skipping start. Status: " + installStatus);
                }

            } else {
                Logger.d(MyApplication.getContext(), TAG, "No updates available or type not allowed.");
            }
        });




    }


    public void showUpdateDialog() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (!isAdded()) {
            return; // Фрагмент не прикреплен
        }
        new UklonAlertDialog(requireContext())
                .setIcon(R.drawable.ic_info)
                .setTitle(R.string.upd_available)
                .setMessage(R.string.upd_available_promt)
                .setCancelable(false)
                .setPositiveButton(R.string.upd_available_ok, dialog -> {
                    MainActivity mainActivity = (MainActivity) requireActivity();
                    AppUpdater appUpdater = new AppUpdater(
                            mainActivity,
                            mainActivity.getExactAlarmLauncher(),
                            mainActivity.getBatteryOptimizationLauncher()
                    );
                    appUpdater.startUpdate();
                })
                .setNegativeButton(R.string.upd_available_cancel, dialog -> dialog.dismiss())
                .show();
    }

    public static void addCheck(Context context) {


        int newCheck = 0;
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        for (int i = 0; i < DataArr.arrayServiceCode().length; i++) {
            if (services.get(i + 1).equals("1")) {
                newCheck++;
            }
        }

        String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");
        if (!tarif.equals(" ")) {
            newCheck++;
        }

        String comment = sharedPreferencesHelperMain.getValue("comment", "no_comment").toString();
        Logger.d(context, TAG, "comment" + comment);

        if (!comment.equals("no_comment")
                && !comment.isEmpty()
                && !comment.equals(" ")
                && !comment.equals("no_name ")
        ) {
            newCheck++;
        }
        String discount= sharedPreferencesHelperMain.getValue("discount", "0").toString();
        if(!discount.equals("0")) {
            newCheck++;
        }

        String mes = context.getString(R.string.add_services);
        if (newCheck != 0) {
            mes = context.getString(R.string.add_services) + " (" + newCheck + ")";
        }

        btnAdd.setText(mes);

    }

    public void btnVisible(int visible) {
        if (!isAdded() || binding == null) {
            Logger.d(context, "BTN_VISIBLE", "Skipped: fragment detached or binding null");
            return;
        }
        Logger.d(context,"BTN_VISIBLE", "Метод вызван с параметром: " + getVisibilityString(visible));

        if (btnCallAdmin != null) {
            btnCallAdmin.setVisibility(View.VISIBLE);
        }
        if (gpsBtn != null) {
            gpsBtn.setVisibility(View.VISIBLE);
        }

        if (visible == GONE || visible == INVISIBLE) {
            if (!CostCalculationProgressBar.isCalculationInProgress()) {
                showRetryMode();
            }
        } else if (visible == VISIBLE) {
            showNormalMode();
        }

        if (!CostCalculationProgressBar.isCalculationInProgress()) {
            binding.progressBar.setVisibility(visible == GONE ? View.VISIBLE : View.GONE);
        }

        if (CostCalculationProgressBar.isCalculationInProgress()) {
            binding.textViewCost.setVisibility(VISIBLE);
            progressBar.forceShow();
            setViewsVisibility(visible,
                    binding.linearLayoutButtons,
                    binding.btnAdd, binding.btnBonus, binding.btnMinus,
                    binding.btnPlus, binding.btnOrder,
                    binding.schedule, binding.shedDown
            );
        } else {
            setViewsVisibility(visible,
                    binding.linearLayoutButtons,
                    binding.btnAdd, binding.btnBonus, binding.btnMinus,
                    binding.textViewCost, binding.btnPlus, binding.btnOrder,
                    binding.schedule, binding.shedDown
            );
        }

        Logger.d(context,"BTN_VISIBLE", "Метод завершен. Текущий режим: " + getVisibilityString(visible));
    }

    private void showRetryMode() {
        if (binding == null) {
            return;
        }
        binding.btnReset.setVisibility(VISIBLE);
        binding.btnReport.setVisibility(VISIBLE);
        binding.btnReport.setOnClickListener(v -> {
            BugReportHelper bugReportHelper = new BugReportHelper((MainActivity) getCurrentActivity());
            bugReportHelper.showBugReportManager();
            progressBar.setVisibility(INVISIBLE);
        });

        binding.btnReset.setOnClickListener(v -> {
            Logger.d(context,"BTN_VISIBLE", "Клик: Попробовать снова");
            clearTABLE_SERVICE_INFO();
            sharedPreferencesHelperMain.saveValue("time", "no_time");
            sharedPreferencesHelperMain.saveValue("date", "no_date");
            sharedPreferencesHelperMain.saveValue("comment", "no_comment");
            sharedPreferencesHelperMain.saveValue("tarif", " ");
            sharedPreferencesHelperMain.saveValue("setStatusX", true);
            svButton.setVisibility(GONE);
            sharedPreferencesHelperMain.saveValue("old_cost", "0");
            if (NetworkUtils.isNetworkAvailable(context)) {
                reloadOrderAfterNetworkRestored();
            } else if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).syncNetworkBanner();
                startActivity(new Intent(context, MainActivity.class));
            } else {
                startActivity(new Intent(context, MainActivity.class));
            }
        });


    }

    private void showNormalMode() {
        if (binding == null) {
            return;
        }
        binding.btnReset.setVisibility(GONE);
        binding.btnReport.setVisibility(GONE);
        setViewsVisibility(VISIBLE,
                binding.textfrom, binding.num1, binding.clearButtonFrom,
                binding.textGeo, binding.textwhere, binding.num2,
                binding.textTo, binding.clearButtonTo
                // binding.gpsbut - удаляем отсюда
        );


    }

    private void setViewsVisibility(int visibility, View... views) {
        for (View view : views) {
            if (view != null) view.setVisibility(visibility);
        }
    }



    private static final class CostPreviewHint {
        final String value;
        final boolean finalDisplay;

        CostPreviewHint(String value, boolean finalDisplay) {
            this.value = value;
            this.finalDisplay = finalDisplay;
        }
    }

    private CostPreviewHint resolveCostPreviewForRecalc() {
        if (!isAdded() || context == null) {
            return null;
        }
        if (Boolean.TRUE.equals(sharedPreferencesHelperMain.getValue(PREF_COST_RECALC_FROM_FINISH, false))
                || Boolean.TRUE.equals(sharedPreferencesHelperMain.getValue(PREF_COST_RECALC_FROM_HISTORY, false))) {
            String cached = String.valueOf(sharedPreferencesHelperMain.getValue("old_cost", ""));
            if (hasDisplayableCost(cached)) {
                return new CostPreviewHint(cached, false);
            }
        }
        String savedPreview = String.valueOf(sharedPreferencesHelperMain.getValue(PREF_COST_PREVIEW_DISPLAY, ""));
        if (hasDisplayableCost(savedPreview)) {
            return new CostPreviewHint(savedPreview.trim(), true);
        }
        if (text_view_cost != null) {
            CharSequence shown = text_view_cost.getText();
            if (shown != null && hasDisplayableCost(shown.toString())) {
                return new CostPreviewHint(shown.toString().trim(), true);
            }
        }
        String cached = String.valueOf(sharedPreferencesHelperMain.getValue("old_cost", ""));
        if (hasDisplayableCost(cached) && !"0".equals(cached.trim())) {
            return new CostPreviewHint(cached, false);
        }
        return null;
    }

    private void clearCostRecalcFromHistoryFlag() {
        sharedPreferencesHelperMain.saveValue(PREF_COST_RECALC_FROM_HISTORY, false);
        sharedPreferencesHelperMain.saveValue(PREF_COST_RECALC_FROM_FINISH, false);
    }

    private void showCostRecalcStatusMessage() {
        if (textCostRecalcStatus != null) {
            textCostRecalcStatus.setText(R.string.check_cost_message);
            textCostRecalcStatus.setVisibility(VISIBLE);
        }
    }

    private void hideCostRecalculatingStatus() {
        clearCostRecalcFromHistoryFlag();
        sharedPreferencesHelperMain.saveValue(PREF_COST_PREVIEW_DISPLAY, "");
        if (text_view_cost != null) {
            text_view_cost.setAlpha(1f);
        }
        if (textCostRecalcStatus != null) {
            textCostRecalcStatus.setVisibility(GONE);
        }
    }

    private long computeDisplayCostFromRaw(String orderCost) throws NumberFormatException {
        double costValue = Double.parseDouble(orderCost);
        long base = Math.round(costValue);
        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
        if (discountText == null || !(discountText.matches("[+-]?\\d+") || discountText.equals("0"))) {
            return base;
        }
        if (base != 0 && (boolean) sharedPreferencesHelperMain.getValue("verifyUserOrder", false)) {
            base = base + 45;
        }
        long discountInt = Long.parseLong(discountText);
        long discount = base * discountInt / 100;
        return base + discount;
    }

    private void updateCostPreviewOnUi(CostPreviewHint preview) {
        if (preview == null || text_view_cost == null) {
            return;
        }
        if (preview.finalDisplay) {
            text_view_cost.setText(preview.value);
        } else {
            text_view_cost.setText(String.valueOf(computeDisplayCostFromRaw(preview.value)));
        }
        text_view_cost.setAlpha(0.45f);
        text_view_cost.setVisibility(VISIBLE);
    }

    private void showCostRecalculatingWithPreview(CostPreviewHint preview) {
        if (!isAdded() || binding == null) {
            return;
        }
        CostCalculationProgressBar.setCalculationInProgress(true);
        hideOrderControlsDuringCostCalculation();
        binding.btnReset.setVisibility(GONE);
        binding.btnReport.setVisibility(GONE);
        binding.textViewCost.setVisibility(VISIBLE);
        if (preview != null) {
            updateCostPreviewOnUi(preview);
        } else {
            text_view_cost.setText("");
            text_view_cost.setAlpha(1f);
        }
        progressBar.forceShow();
        progressBar.bringToFront();
        showCostRecalcStatusMessage();
        binding.btnAdd.setVisibility(INVISIBLE);
        binding.btnBonus.setVisibility(INVISIBLE);
        binding.schedule.setVisibility(INVISIBLE);
        binding.shedDown.setVisibility(INVISIBLE);
        binding.getRoot().post(this::restoreCostCalculationProgressIfNeeded);
    }

    private void showCostCalculationProgress() {
        showCostRecalculatingWithPreview(null);
    }

    private void hideCostCalculationProgress() {
        hideCostRecalculatingStatus();
        CostCalculationProgressBar.setCalculationInProgress(false);
        if (progressBar != null) {
            progressBar.forceHide();
        }
    }

    private void restoreCostCalculationProgressIfNeeded() {
        if (!CostCalculationProgressBar.isCalculationInProgress() || binding == null) {
            return;
        }
        CharSequence priceText = text_view_cost != null ? text_view_cost.getText() : null;
        if (priceText != null && !priceText.toString().trim().isEmpty()) {
            return;
        }
        binding.textViewCost.setVisibility(VISIBLE);
        progressBar.forceShow();
        progressBar.bringToFront();
    }

    private void hideOrderControlsDuringCostCalculation() {
        if (binding == null) {
            return;
        }
        binding.linearLayoutButtons.setVisibility(GONE);
        binding.btnAdd.setVisibility(INVISIBLE);
        binding.btnBonus.setVisibility(INVISIBLE);
        binding.btnMinus.setVisibility(GONE);
        binding.btnPlus.setVisibility(GONE);
        binding.btnOrder.setVisibility(GONE);
        binding.schedule.setVisibility(INVISIBLE);
        binding.shedDown.setVisibility(INVISIBLE);
    }

    private void finishCostCalculationWithPrice() {
        if (text_view_cost == null) {
            return;
        }
        CharSequence priceText = text_view_cost.getText();
        if (priceText == null || priceText.toString().trim().isEmpty()) {
            showCostCalculationProgress();
            return;
        }
        hideCostCalculationProgress();
        sharedPreferencesHelperMain.saveValue("old_cost", priceText.toString().trim());
        btnVisible(VISIBLE);
        btnAdd.setVisibility(View.VISIBLE);
        buttonBonus.setVisibility(View.VISIBLE);
        btn_minus.setVisibility(View.VISIBLE);
        text_view_cost.setVisibility(View.VISIBLE);
        btn_plus.setVisibility(View.VISIBLE);
        btnOrder.setVisibility(View.VISIBLE);
        constr2.setVisibility(View.VISIBLE);
        schedule.setVisibility(View.VISIBLE);
        shed_down.setVisibility(View.VISIBLE);
    }

    private static boolean hasDisplayableCost(String cost) {
        if (cost == null || cost.trim().isEmpty()) {
            return false;
        }
        try {
            return Long.parseLong(cost.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isTerminalCostMessage(String message) {
        if (message == null) {
            return false;
        }
        return "ErrorMessage".equals(message)
                || "ErrorCardPayment".equals(message)
                || message.startsWith("Ошибка от сервера")
                || message.startsWith("Ошибка подключения");
    }

    private void cancelPendingReserveCost() {
        if (costHandler != null && reserveRunnable != null) {
            costHandler.removeCallbacks(reserveRunnable);
        }
    }

    /** Кэшированная цена «по городу», если сервер ещё не вернул маршрут/стоимость. */
    private boolean tryApplyCachedAroundCityCost() {
        if (!isAdded() || context == null) {
            return false;
        }
        CostPreviewHint preview = resolveCostPreviewForRecalc();
        if (preview == null) {
            return false;
        }
        Logger.d(context, TAG, "tryApplyCachedAroundCityCost: " + preview.value);
        if (preview.finalDisplay) {
            text_view_cost.setText(preview.value);
            text_view_cost.setAlpha(1f);
            finishCostCalculationWithPrice();
        } else {
            applyDiscountAndUpdateUI(preview.value, context);
        }
        return true;
    }

    private void showCostCalculationError(String serverMessage) {
        cancelPendingReserveCost();
        hideCostCalculationProgress();
        long now = System.currentTimeMillis();
        lastVisicomCostRequestMs = now;
        if (isNetworkRelatedCostError(serverMessage)) {
            lastCostCalculationFailureMs = 0;
            btnVisible(GONE);
            Logger.w(context, TAG, "Ошибка сети при расчёте стоимости: " + serverMessage);
            return;
        }
        lastCostCalculationFailureMs = now;
        btnVisible(VISIBLE);
        if (!isAdded() || isStateSaved()) {
            return;
        }
        MyBottomSheetErrorFragment sheet = new MyBottomSheetErrorFragment(resolveCostErrorMessage(serverMessage));
        sheet.show(getChildFragmentManager(), sheet.getTag());
    }

    private static boolean isNetworkRelatedCostError(String serverMessage) {
        if (serverMessage == null || serverMessage.trim().isEmpty()) {
            return true;
        }
        String lower = serverMessage.toLowerCase(Locale.ROOT);
        return lower.contains("подключ")
                || lower.contains("connect")
                || lower.contains("timeout")
                || lower.contains("unable to resolve")
                || lower.contains("network")
                || lower.contains("hostname")
                || lower.contains("internet");
    }

    private static boolean isForceRetrySource(String source) {
        return "networkRestored".equals(source)
                || "swipeRefresh".equals(source)
                || "manualRetry".equals(source)
                || "manualGps".equals(source)
                || "autoGps".equals(source)
                || "addressChanged".equals(source)
                || "fromHistory".equals(source)
                || "fromFinish".equals(source);
    }

    private void snapshotCostPreviewBeforeRouteChange() {
        if (text_view_cost != null) {
            CharSequence shown = text_view_cost.getText();
            if (shown != null && hasDisplayableCost(shown.toString())) {
                sharedPreferencesHelperMain.saveValue(PREF_COST_PREVIEW_DISPLAY, shown.toString().trim());
                return;
            }
        }
        String oldCost = String.valueOf(sharedPreferencesHelperMain.getValue("old_cost", ""));
        if (hasDisplayableCost(oldCost) && !"0".equals(oldCost.trim())) {
            sharedPreferencesHelperMain.saveValue(PREF_COST_PREVIEW_DISPLAY, oldCost.trim());
        }
    }

    private void openAddressSearch(Bundle bundle) {
        snapshotCostPreviewBeforeRouteChange();
        navController.navigate(R.id.nav_search, bundle, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_search, true)
                .build());
    }

    private void requestVisicomCostAfterRouteChange() {
        snapshotCostPreviewBeforeRouteChange();
        requestVisicomCost("addressChanged");
    }

    private String resolveVisicomCostSourceOnResume() {
        if (Boolean.TRUE.equals(sharedPreferencesHelperMain.getValue(PREF_COST_RECALC_FROM_FINISH, false))) {
            return "fromFinish";
        }
        if (Boolean.TRUE.equals(sharedPreferencesHelperMain.getValue(PREF_COST_RECALC_FROM_HISTORY, false))) {
            return "fromHistory";
        }
        String preview = String.valueOf(sharedPreferencesHelperMain.getValue(PREF_COST_PREVIEW_DISPLAY, ""));
        if (hasDisplayableCost(preview)) {
            return "addressChanged";
        }
        return "onResume";
    }

    public void reloadOrderAfterNetworkRestored() {
        if (!isAdded() || context == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (CostCalculationProgressBar.isCalculationInProgress()) {
            Logger.d(context, TAG, "reloadOrderAfterNetworkRestored: пропуск — расчёт уже идёт");
            return;
        }
        if (lastVisicomCostRequestMs > 0
                && now - lastVisicomCostRequestMs < NETWORK_RESTORE_RELOAD_COOLDOWN_MS) {
            Logger.d(context, TAG, "reloadOrderAfterNetworkRestored: пропуск — недавний расчёт (onResume)");
            return;
        }
        resetCostCalculationState("networkRestored");
        requestVisicomCost("networkRestored");
    }

    private void resetCostCalculationState(String reason) {
        Logger.d(context, TAG, "resetCostCalculationState: " + reason);
        cancelPendingReserveCost();
        hideCostCalculationProgress();
        lastCostCalculationFailureMs = 0;
        lastVisicomCostRequestMs = 0;
        lastCost = null;
        resetRealtimeOrderCostDedup();
    }

    private void resetRealtimeOrderCostDedup() {
        if (!isAdded()) {
            return;
        }
        Activity activity = getActivity();
        if (activity instanceof MainActivity mainActivity) {
            mainActivity.resetCentrifugoOrderCostDedup();
        }
    }

    private String resolveCostErrorMessage(String serverMessage) {
        if (serverMessage == null || serverMessage.trim().isEmpty()) {
            return getString(R.string.server_error_connected);
        }
        if ("ErrorMessage".equals(serverMessage)) {
            return getString(R.string.server_error_connected);
        }
        if ("ErrorCardPayment".equals(serverMessage)) {
            return getString(R.string.server_error_card_payment);
        }
        if (serverMessage.startsWith("Ошибка подключения")
                || serverMessage.startsWith("Ошибка от сервера")
                || serverMessage.toLowerCase().contains("timeout")) {
            return getString(R.string.server_error_connected);
        }
        return serverMessage;
    }

    // Вспомогательный метод для логирования (добавьте в класс)
    private String getVisibilityString(int visibility) {
        switch (visibility) {
            case View.VISIBLE:
                return "VISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return "UNKNOWN (" + visibility + ")";
        }
    }

    public static void btnStaticVisible(int visible) {
        Activity activity = MyApplication.getCurrentActivity();

        if (activity == null || text_view_cost == null) {
            Logger.d(null, TAG, "btnStaticVisible: activity or views are null");
            return;
        }

        // Управление ProgressBar
        if (visible == GONE) {
            progressBar.setVisibility(VISIBLE);
        } else {
            progressBar.setVisibility(GONE);
        }

        // Управление режимами отображения
        if (visible == GONE) {
            // Режим ошибки/загрузки (как showRetryMode)
            showRetryModeStatic();
        } else if (visible == VISIBLE) {
            // Нормальный режим (как showNormalMode)
            showNormalModeStatic();
        }

        // Групповая установка видимости для основных элементов
        linearLayout.setVisibility(visible);
        btnAdd.setVisibility(visible);
        buttonBonus.setVisibility(visible);
        btn_minus.setVisibility(visible);
        text_view_cost.setVisibility(visible);
        btn_plus.setVisibility(visible);
        btnOrder.setVisibility(visible);
        schedule.setVisibility(visible);
        shed_down.setVisibility(visible);

        // GPS кнопка всегда видна
        if (gpsBtn != null) {
            gpsBtn.setVisibility(VISIBLE);
        }

        // Кнопка администратора всегда видна
        if (btnCallAdmin != null) {
            btnCallAdmin.setVisibility(VISIBLE);
        }
    }

    private static void showRetryModeStatic() {
        Activity activity = MyApplication.getCurrentActivity();
        if (activity == null) return;

        if (btnReset != null) btnReset.setVisibility(VISIBLE);
        if (btnReport != null) btnReport.setVisibility(VISIBLE);

        if (btnReport != null) {
            btnReport.setOnClickListener(v -> {
                BugReportHelper bugReportHelper = new BugReportHelper((MainActivity) activity);
                bugReportHelper.showBugReportManager();
                if (progressBar != null) progressBar.setVisibility(INVISIBLE);
            });
        }

        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                Logger.d(null, "btnStaticVisible", "Клик: Попробовать снова");
                clearTABLE_SERVICE_INFOStatic();
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                sharedPreferencesHelperMain.saveValue("comment", "no_comment");
                sharedPreferencesHelperMain.saveValue("tarif", " ");
                sharedPreferencesHelperMain.saveValue("setStatusX", true);
                if (svButton != null) svButton.setVisibility(GONE);
                sharedPreferencesHelperMain.saveValue("old_cost", "0");
                activity.startActivity(new Intent(activity, MainActivity.class));
            });
        }
    }

    private static void showNormalModeStatic() {
        if (btnReset != null) btnReset.setVisibility(GONE);
        if (btnReport != null) btnReport.setVisibility(GONE);

        // Показываем текстовые поля
        if (geoText != null) geoText.setVisibility(VISIBLE);
        if (textfrom != null) textfrom.setVisibility(VISIBLE);
        if (num1 != null) num1.setVisibility(VISIBLE);
        if (textwhere != null) textwhere.setVisibility(VISIBLE);
        if (num2 != null) num2.setVisibility(VISIBLE);
        if (textViewTo != null) textViewTo.setVisibility(VISIBLE);
    }

    private static void clearTABLE_SERVICE_INFOStatic() {
        String[] arrayServiceCode = DataArr.arrayServiceCode();
        SQLiteDatabase database = MyApplication.getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            for (String code : arrayServiceCode) {
                ContentValues cv = new ContentValues();
                cv.put(code, "0");
                database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?", new String[]{"1"});
            }
        } catch (Exception e) {
            Logger.e(null, "clearTABLE_SERVICE_INFO", "Ошибка: " + e.getMessage());
        } finally {
            database.close();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        isFragmentVisible = false;
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    public static boolean isFragmentVisible() {
        return isFragmentVisible;
    }
    /**
     * Статический метод для обновления позиции из CityFinder
     */
    public static void updateMyPositionStatic(double lat, double lng, String address) {
        Activity activity = MyApplication.getCurrentActivity();
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            // Обновляем TextView
            if (geoText != null) {
                geoText.setText(address);
            }

            // Обновляем базу данных
            SQLiteDatabase database = activity.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            ContentValues cv = new ContentValues();
            cv.put("startLat", lat);
            cv.put("startLan", lng);
            cv.put("start", address);
            database.update(MainActivity.ROUT_MARKER, cv, "id = ?", new String[]{"1"});
            database.close();
            btnStaticVisible(VISIBLE);
        });
    }

    /**
     * Пересчёт стоимости после восстановления локации
     */
    public static void recalculateCost() {
        Activity activity = MyApplication.getCurrentActivity();
        if (activity == null) return;

        activity.runOnUiThread(() -> {
            try {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                // Вызываем метод через рефлексию или создайте публичный метод
                btnStaticVisible(VISIBLE);
//                 visicomCost();
            } catch (Exception e) {
                Logger.e(activity, TAG, "Error recalculating cost: " + e.getMessage());
            }
        });
    }

    public void requestPermissions() {
        String[] permissions = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
                // добавьте нужные разрешения
        };
        permissionLauncher.launch(permissions);
    }
    // Метод для сохранения количества запросов разрешений в SharedPreferences
    private void savePermissionRequestCount(int count) {
        sharedPreferencesHelperMain.saveValue(MainActivity.PERMISSION_REQUEST_COUNT_KEY, count);

    }

    // Метод для загрузки количества запросов разрешений из SharedPreferences
    private int loadPermissionRequestCount() {
        return (int) sharedPreferencesHelperMain.getValue(MainActivity.PERMISSION_REQUEST_COUNT_KEY, 0);
//        return MainActivity.sharedPreferencesCount.getInt(MainActivity.PERMISSION_REQUEST_COUNT_KEY, 0);
    }

    @Override
    public void onDestroyView() {
        // ✅ Отменяем активные запросы
        pendingAddressRequest = null;
        isUpdatingFromGPS = false;
        CityFinder.resetFlags();
        // Удаляем observer жизненного цикла
        if (lifecycleObserver != null) {
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(lifecycleObserver);
        }

        // Отмена всех запросов
        RetrofitClient.getInstance().cancelAllRequests();

        // Очистка binding
        binding = null;

        super.onDestroyView();
    }

    @SuppressLint("Range")
    private static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            String str;
            do {
                str = "";
                for (String cn : c.getColumnNames()) {
                    String value = CursorReadHelper.getString(c, cn);
                    str = str.concat(cn + " = " + value + "; ");
                    list.add(value);
                }

            } while (c.moveToNext());
        }
        database.close();
        c.close();
        return list;
    }

    private static void updateAddCost(String addCost, Context context) {
        ContentValues cv = new ContentValues();
        Logger.d(context, TAG, "updateAddCost: addCost" + addCost);
        cv.put("addCost", addCost);
        finalCost= startCost + Long.parseLong( addCost);
        if(text_view_cost != null) {
            text_view_cost.setText(String.valueOf( finalCost));
        }

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[]{"1"});
        database.close();
    }

    private String cleanString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ").replaceAll("\\s{2,}$", " ");
    }

    private boolean isCityOnlyFinishInDatabase(String finish) {
        if (finish == null) {
            return true;
        }
        String trimmed = finish.trim();
        return trimmed.isEmpty()
                || trimmed.equals(getString(R.string.on_city_tv))
                || trimmed.equals(getString(R.string.on_city))
                || trimmed.contains("по місту")
                || trimmed.contains("по городу")
                || trimmed.contains("around the city");
    }

    private boolean isValidStartAddressForFromField(String start) {
        return start != null
                && !start.trim().isEmpty()
                && !isCityOnlyFinishInDatabase(start);
    }

    public static void clearFromAddressUiForCityChange() {
        if (geoText != null) {
            geoText.post(() -> geoText.setText(""));
        }
    }

    private void clearStaleFromAddressUiIfDatabaseHasNoStart() {
        if (!isAdded() || binding == null || context == null) {
            return;
        }
        List<String> route = logCursor(MainActivity.ROUT_MARKER, context);
        if (route.size() <= 5) {
            return;
        }
        if (isValidStartAddressForFromField(route.get(5))) {
            return;
        }
        String uiBefore = binding.textGeo.getText() != null
                ? binding.textGeo.getText().toString()
                : "";
        if (!uiBefore.trim().isEmpty()) {
            logAddrGuardOverwrite("clearStaleFromUi", uiBefore, "", "no start in ROUT_MARKER");
            binding.textGeo.setText("");
        }
    }

    private void clearInvalidStartInRouteMarker() {
        if (context == null) {
            return;
        }
        try {
            List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);
            String currentCity = cityInfo.size() > 1 ? cityInfo.get(1) : "";
            double[] center = com.taxi.easy.ua.utils.city.CityLastAddressHelper.getCityCenter(currentCity);
            ContentValues cv = new ContentValues();
            cv.put("start", "");
            if (center != null) {
                cv.put("startLat", center[0]);
                cv.put("startLan", center[1]);
            }
            AutoLocationAfterCityHelper.clearStartAddressSource();
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.ROUT_MARKER, cv, "id = ?", new String[]{"1"});
            database.close();
        } catch (Exception e) {
            Logger.e(context, TAG, "clearInvalidStartInRouteMarker: " + e.getMessage());
        }
    }

    private static final double MIN_DESTINATION_COORD_DELTA = 2e-4;

    /** Точка «Куда» выбрана на карте или введена вручную — не сбрасывать при onResume. */
    private boolean isValidSavedDestination(double startLat, double startLon,
                                            double toLat, double toLng, String finish) {
        if (finish != null && !finish.trim().isEmpty() && !isCityOnlyFinishInDatabase(finish)) {
            return true;
        }
        if (toLat == 0.0 && toLng == 0.0) {
            return false;
        }
        if (startLat == 0.0) {
            return true;
        }
        return Math.abs(startLat - toLat) > MIN_DESTINATION_COORD_DELTA
                || Math.abs(startLon - toLng) > MIN_DESTINATION_COORD_DELTA;
    }

    private void restoreDestinationUiFromDatabaseIfNeeded(String finish) {
        if (!isAdded() || textViewTo == null || finish == null) {
            return;
        }
        if (!textViewTo.getText().toString().trim().isEmpty()) {
            return;
        }
        if (isCityOnlyFinishInDatabase(finish)) {
            applyAroundCityDestinationToUiIfNeeded();
        } else {
            textViewTo.setText(finish);
        }
    }

    /**
     * Без активного заказа не подставляем старую точку назначения из прошлой поездки —
     * оставляем «по місту» для фиксированного тарифа.
     */
    private boolean normalizeRouteMarkerToAroundCityWhenNoActiveOrder() {
        if (!isAdded() || context == null || hasActiveOrderSession()) {
            return false;
        }
        if (textViewTo != null) {
            String uiFinish = textViewTo.getText().toString().trim();
            if (!uiFinish.isEmpty() && !isCityOnlyFinishInDatabase(uiFinish)) {
                return false;
            }
        }
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            cursor = database.rawQuery(
                    "SELECT startLat, startLan, to_lat, to_lng, start, finish FROM "
                            + MainActivity.ROUT_MARKER + " LIMIT 1",
                    null);
            if (!cursor.moveToFirst()) {
                return false;
            }
            double startLat = cursor.getDouble(0);
            double startLon = cursor.getDouble(1);
            double toLat = cursor.getDouble(2);
            double toLng = cursor.getDouble(3);
            String start = cursor.getString(4);
            String finish = cursor.getString(5);
            if (start == null) {
                start = "";
            }
            if (finish == null) {
                finish = "";
            }
            if (startLat == 0.0) {
                return false;
            }
            if (isValidSavedDestination(startLat, startLon, toLat, toLng, finish)) {
                restoreDestinationUiFromDatabaseIfNeeded(finish);
                return false;
            }
            if (isCityOnlyFinishInDatabase(finish) && isAroundCityRoute(start, finish)) {
                applyAroundCityDestinationToUiIfNeeded();
                return false;
            }
            ContentValues values = new ContentValues();
            values.put("to_lat", startLat);
            values.put("to_lng", startLon);
            values.put("finish", "");
            int updated = database.update(MainActivity.ROUT_MARKER, values, "id = ?", new String[]{"1"});
            if (updated == 0) {
                values.put("startLat", startLat);
                values.put("startLan", startLon);
                values.put("start", start);
                database.insert(MainActivity.ROUT_MARKER, null, values);
            }
            applyAroundCityDestinationToUiIfNeeded();
            Logger.d(context, TAG, "normalizeRouteMarkerToAroundCity: сброшен устаревший маршрут");
            return true;
        } catch (Exception e) {
            Logger.e(context, TAG, "normalizeRouteMarkerToAroundCity: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    private void applyAroundCityDestinationToUiIfNeeded() {
        if (!isAdded()) {
            return;
        }
        String onCity = getString(R.string.on_city_tv);
        if (textViewTo != null) {
            textViewTo.setText(onCity);
        }
        if (binding != null && binding.textTo != null) {
            binding.textTo.setText(onCity);
        }
    }

    /** Поездка «по городу»: пустое/«по місту» назначение или те же адреса откуда и куда. */
    private boolean isAroundCityRoute(String start, String finish) {
        if (isCityOnlyFinishInDatabase(finish)) {
            return true;
        }
        if (start == null || finish == null) {
            return false;
        }
        String from = start.trim();
        String to = finish.trim();
        return !from.isEmpty() && from.equals(to);
    }

    private boolean isRouteReadyForCost(double originLatitude, double toLat, String start, String finish) {
        if (start == null) {
            start = "";
        }
        if (finish == null) {
            finish = "";
        }
        if (originLatitude == 0.0) {
            return false;
        }
        if (isAroundCityRoute(start, finish)) {
            return true;
        }
        return !start.trim().isEmpty() && (toLat != 0.0 || !finish.trim().isEmpty());
    }

    private void syncAroundCityRouteToUi(String finish) {
        if (!isAdded() || textViewTo == null || !isCityOnlyFinishInDatabase(finish)) {
            return;
        }
        if (textViewTo.getText().toString().trim().isEmpty()) {
            textViewTo.setText(getString(R.string.on_city_tv));
        }
    }

    /** Восстанавливает «Куда» из БД после возврата с экрана поиска (view мог пересоздаться). */
    private void restoreDestinationFieldFromDatabase() {
        if (!isAdded() || textViewTo == null || context == null) {
            return;
        }
        if (!textViewTo.getText().toString().trim().isEmpty()) {
            return;
        }
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            cursor = database.rawQuery(
                    "SELECT startLat, startLan, to_lat, to_lng, finish FROM "
                            + MainActivity.ROUT_MARKER + " LIMIT 1",
                    null);
            if (!cursor.moveToFirst()) {
                return;
            }
            double startLat = cursor.getDouble(0);
            double startLon = cursor.getDouble(1);
            double toLat = cursor.getDouble(2);
            double toLng = cursor.getDouble(3);
            String finish = cursor.getString(4);
            if (finish == null) {
                finish = "";
            }
            if (isValidSavedDestination(startLat, startLon, toLat, toLng, finish)) {
                restoreDestinationUiFromDatabaseIfNeeded(finish);
                return;
            }
            if (!hasActiveOrderSession()) {
                normalizeRouteMarkerToAroundCityWhenNoActiveOrder();
            } else {
                restoreDestinationUiFromDatabaseIfNeeded(finish);
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "restoreDestinationFieldFromDatabase: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }


    @SuppressLint({"Range", "ResourceAsColor"})
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {

        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: " + urlAPI);
        startTilePreloadWorker();
        restoreDestinationFieldFromDatabase();

        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery(query, null);
        cursor.moveToFirst();

        double originLatitude = CursorReadHelper.getDouble(cursor, "startLat");
        double originLongitude = CursorReadHelper.getDouble(cursor, "startLan");
        double toLatitude = CursorReadHelper.getDouble(cursor, "to_lat");
        double toLongitude = CursorReadHelper.getDouble(cursor, "to_lng");
        String start = CursorReadHelper.getString(cursor, "start");
        String finish = CursorReadHelper.getString(cursor, "finish");
        if (start == null) {
            start = "";
        }
        if (finish == null) {
            finish = "";
        }

//        if (start.trim().isEmpty() || geoText.getText().toString().trim().isEmpty()) {
//            start = context.getString(R.string.startPoint);
//            geoText.setText(start);
//        }
        if (originLatitude == 0.0) {
            geoText.setText("");
            geoText.setBackgroundColor(R.color.selected_text_color);
            return "error";
        }
        if (isAroundCityRoute(start, finish)) {
            finish = context.getString(R.string.on_city_tv);
            toLatitude = originLatitude;
            toLongitude = originLongitude;
        }

        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: start " + start);
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: finish " + finish);

        start = start.replace("/", "|");
        finish = finish.replace("/", "|");

        String str_origin = originLatitude + "/" + originLongitude;
        String str_dest = toLatitude + "/" + toLongitude;
        cursor.close();

        String time = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String comment = (String) sharedPreferencesHelperMain.getValue("comment", "no_comment");
        String date = (String) sharedPreferencesHelperMain.getValue("date", "no_date");

        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: time " + time);
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: comment " + comment);
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: date " + date);

        String mes = context.getString(R.string.on) + " " + time + " " + date;
        if (time.equals("no_time") && date.equals("no_date")) {
            mes = context.getString(R.string.on_now);
        }
        schedule.setText(mes);

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String payment_type = stringListInfo.get(4);
        String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");

        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: tarif " + tarif);
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: payment_type " + payment_type);

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);
        displayName = InputSanitizerHelper.sanitize(displayName, InputSanitizerHelper.InputType.USERNAME);

        if (displayName.trim().isEmpty()) {
            displayName = "No_name";
        }
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);

        if (urlAPI.equals("costSearchMarkersTimeMyApi")) {
            boolean black_list_yes = verifyOrder();
            Logger.d(context, TAG, "getTaxiUrlSearchMarkers: black_list_yes " + black_list_yes);

            if (black_list_yes) {


                if (!pay_method.equals("wfp_payment")) {
                    payment_type = "wfp_payment";
                    ContentValues cv = new ContentValues();
                    cv.put("payment_type", payment_type);

                    database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?", new String[]{"1"});

                    buttonBonus.setText(context.getString(R.string.card_payment));

                    String message = context.getString(R.string.black_list_message_err);

                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

                }
            }

            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
            }
            c.close();
            // Проверяем, что комментарий не является "мусорным"
            boolean isInclusive = InclusiveTransportPreferenceWorker.needsInclusiveTransport();
            if (isInclusive) {


                boolean isDummyComment = (comment == null ||
                        comment.equals("no_comment") ||
                        comment.equals("no_name") ||
                        comment.equals("none") ||
                        comment.trim().isEmpty());

                if (isDummyComment) {
                    // Если комментарий мусорный - просто устанавливаем сообщение про инклюзив
                    comment = context.getString(R.string.inclusive_transport_message_yes);
                    Logger.d(context, "comment", "Был мусорный комментарий, устанавливаем только инклюзив: " + comment);
                } else {
                    // Проверяем, содержит ли уже комментарий эту фразу
                    if (!comment.contains(context.getString(R.string.inclusive_transport_message_yes))) {
                        comment += " " + context.getString(R.string.inclusive_transport_message_yes);
                        Logger.d(context, "comment", "Добавляем инклюзив к существующему комментарию: " + comment);
                    }
                }
            }
            sharedPreferencesHelperMain.saveValue("comment", comment );
            Logger.d(context, "comment", comment);
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") *" + userEmail + "*" + payment_type + "/"
                    + time + "/" + date;
        }

        if (urlAPI.equals("orderClientCostMyApi")) {
            boolean black_list_yes = verifyOrder();

            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: startCost " + startCost);
            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: finalCost " + finalCost);

            long addCostInt = finalCost - startCost;
            String addCost = String.valueOf(addCostInt);
            Logger.d(context, TAG, "getTaxiUrlSearchMarkers: addCost " + addCost);

            String wfpInvoice = "*";
            if (payment_type.equals("wfp_payment")) {
                String rectoken = getCheckRectoken(context);
                Logger.d(context, TAG, "payWfp: rectoken " + rectoken);
                if (!rectoken.isEmpty()) {
                    String activeUid = MainActivity.uid;
                    String savedRef = activeUid != null && !activeUid.isEmpty()
                            ? PaymentSessionHelper.getWfpOrderRef(activeUid)
                            : null;
                    if (savedRef != null) {
                        MainActivity.order_id = savedRef;
                        wfpInvoice = savedRef;
                    } else {
                        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                        wfpInvoice = MainActivity.order_id;
                    }
                }
            } else if (payment_type.equals("google_pay_payment")) {
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                wfpInvoice = MainActivity.order_id;
            }

            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
            String paramsUserArr = displayName + " (" + context.getString(R.string.version_code) + ") *" + userEmail + "*" + payment_type;

            if (black_list_yes) {
                String lastCharacter = phoneNumber.substring(phoneNumber.length() - 1);
                phoneNumber = phoneNumber.substring(0, phoneNumber.length() - 1).replace(" ", "");
                comment = "цифра номера " + lastCharacter + ", Оплатили службе 45грн. " + comment;

//                addCost = addCostBlackList(addCost);
            }

            boolean doubleOrder = (boolean) sharedPreferencesHelperMain.getValue("doubleOrderPref", false);
            if (doubleOrder) {
                paramsUserArr += "*doubleOrder";
                sharedPreferencesHelperMain.saveValue("doubleOrderPref", false);
            }

            String clientCost = text_view_cost.getText().toString();
            if (clientCost == null || clientCost.trim().isEmpty()) {
                clientCost = " ";
            }
            if((boolean)sharedPreferencesHelperMain.getValue("black_list_45", false)) {
                long cost = Long.parseLong(clientCost); // Convert string to double
                cost += 45; // Add 45
                clientCost = String.valueOf(cost); // Convert back to string
            }
            sharedPreferencesHelperMain.saveValue("black_list_45", false);
            sharedPreferencesHelperMain.saveValue("old_cost", clientCost);
            boolean isInclusive = InclusiveTransportPreferenceWorker.needsInclusiveTransport();
            if (isInclusive) {


                boolean isDummyComment = (comment == null ||
                        comment.equals("no_comment") ||
                        comment.equals("no_name") ||
                        comment.equals("none") ||
                        comment.trim().isEmpty());

                if (isDummyComment) {
                    // Если комментарий мусорный - просто устанавливаем сообщение про инклюзив
                    comment = context.getString(R.string.inclusive_transport_message_yes);
                    Logger.d(context, "comment", "Был мусорный комментарий, устанавливаем только инклюзив: " + comment);
                } else {
                    // Проверяем, содержит ли уже комментарий эту фразу
                    if (!comment.contains(context.getString(R.string.inclusive_transport_message_yes))) {
                        comment += " " + context.getString(R.string.inclusive_transport_message_yes);
                        Logger.d(context, "comment", "Добавляем инклюзив к существующему комментарию: " + comment);
                    }
                }
            }
            sharedPreferencesHelperMain.saveValue("comment", comment );
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + clientCost + "/" + paramsUserArr + "/" + addCost + "/"
                    + time + "/" + comment + "/" + date + "/" + start + "/" + finish + "/" + wfpInvoice;

            sharedPreferencesHelperMain.saveValue("time", "no_time");
            sharedPreferencesHelperMain.saveValue("date", "no_date");
//            sharedPreferencesHelperMain.saveValue("comment", "no_comment");
        }

        // Сервисы
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        List<String> servicesChecked = new ArrayList<>();
        boolean servicesVer = false;

        for (int i = 1; i < services.size() - 1; i++) {
            if (services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }

        String result;
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
        if (urlAPI.equals("costSearchMarkersTimeMyApi")) {
            String urlKafka = "/" + parameters + "/" + result + "/" + city + "/" + context.getString(R.string.application);

            Logger.e(context,"KafkaRequest", "urlKafka: " + urlKafka);
            KafkaRequest costRequest = new KafkaRequest();
            costRequest.sendCostMessage(urlKafka);
        }
        btnVisible(GONE);

        database.close();
        return url;
    }

    @SuppressLint("Range")
    private static String getCheckRectoken(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String[] columns = {"rectoken"}; // Указываем нужное поле
        String selection = "rectoken_check = ?";
        String[] selectionArgs = {"1"};
        String result = "";

        Cursor cursor = database.query(MainActivity.TABLE_WFP_CARDS, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                result = CursorReadHelper.getString(cursor, "rectoken");
                Logger.d(context, TAG, "Found rectoken with rectoken_check = 1" + ": " + result);
                return result;
            } while (cursor.moveToNext());
        }
        cursor.close();

        database.close();


        return result;
    }

    private static String addCostBlackList(String addcost) {

        int cost = Integer.parseInt(addcost); // Преобразуем строку в целое число
        cost += 45; // Увеличиваем на 45
        addcost = String.valueOf(cost); // Преобразуем обратно в строку

// Теперь addCost содержит новое значение
        return  addcost; // Вывод: "145"

    }

    @SuppressLint("SetTextI18n")
    public void readTariffInfo() {
        // Создаем экземпляр класса для работы с базой данных

        tariffBtnColor();

        btn1.setOnClickListener(v -> {
            progressBar.setVisibility(VISIBLE);

            ContentValues cv = new ContentValues();
            cv.put("tarif", "Базовый");
            sharedPreferencesHelperMain.saveValue("tarif", "Базовый");

            String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
            if (!userEmail.equals("email")) {
                try {
                    visicomCost();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                readTariffInfo();
            }

            frame_1.setBackgroundResource(R.drawable.input);
            frame_2.setBackgroundResource(R.drawable.buttons);
            frame_3.setBackgroundResource(R.drawable.buttons);

            addCheck(context);

        });

        btn2.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(VISIBLE);
                ContentValues cv = new ContentValues();
                cv.put("tarif", "Универсал");
                sharedPreferencesHelperMain.saveValue("tarif", "Универсал");
                // обновляем по id
                String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                if (!userEmail.equals("email")) {
                    try {
                        visicomCost();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                    readTariffInfo();
                }
                frame_1.setBackgroundResource(R.drawable.buttons);
                frame_2.setBackgroundResource(R.drawable.input);
                frame_3.setBackgroundResource(R.drawable.buttons);
                addCheck(context);
            }
        });

        btn3.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(VISIBLE);

                ContentValues cv = new ContentValues();
                cv.put("tarif", "Микроавтобус");
                sharedPreferencesHelperMain.saveValue("tarif", "Микроавтобус");
                // обновляем по id

                String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                if (!userEmail.equals("email")) {
                    try {
                        visicomCost();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                    readTariffInfo();
                }
                frame_1.setBackgroundResource(R.drawable.buttons);
                frame_2.setBackgroundResource(R.drawable.buttons);
                frame_3.setBackgroundResource(R.drawable.input);

                addCheck(context);
            }
        });

    }
    public static void tariffBtnColor() {
        if (frame_1 == null || frame_2 == null || frame_3 == null) {
            Logger.e(MyApplication.getContext(),TAG, "tariffBtnColor: frames are null, skipping update");
            return;
        }
        String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");
        if (tarif.equals("Базовый")) {
            frame_1.setBackgroundResource(R.drawable.input);
            frame_2.setBackgroundResource(R.drawable.buttons);
            frame_3.setBackgroundResource(R.drawable.buttons);
        } else if (tarif.equals("Универсал")) {
            frame_1.setBackgroundResource(R.drawable.buttons);
            frame_2.setBackgroundResource(R.drawable.input);
            frame_3.setBackgroundResource(R.drawable.buttons);
        } else if (tarif.equals("Микроавтобус")) {
            frame_1.setBackgroundResource(R.drawable.buttons);
            frame_2.setBackgroundResource(R.drawable.buttons);
            frame_3.setBackgroundResource(R.drawable.input);
        } else {
            frame_1.setBackgroundResource(R.drawable.buttons);
            frame_2.setBackgroundResource(R.drawable.buttons);
            frame_3.setBackgroundResource(R.drawable.buttons);
        }
    }

    @SuppressLint("ResourceAsColor")
    public boolean orderRout() {
        if (geoText.getText().toString().trim().isEmpty()) {
            Toast.makeText(context, R.string.no_start_point_message, Toast.LENGTH_SHORT).show();
            return false;
        }
        restoreDestinationFieldFromDatabase();
        urlOrder = getTaxiUrlSearchMarkers("orderClientCostMyApi", context );
        Logger.d(context, TAG, "order: urlOrder " + urlOrder);
        if(urlOrder.equals("error")) {
            Toast.makeText(context, R.string.no_start_point_message, Toast.LENGTH_SHORT).show();
            return false;
        }
        pendingOrderDisplayCost = resolveOrderDisplayCostForSubmit();
        Logger.d(context, TAG, "order: pendingOrderDisplayCost " + pendingOrderDisplayCost);
        return true;
    }

    /** Сумма для finish/early-nav: не text_view_cost после пересчёта тарифа. */
    @Nullable
    private String resolveOrderDisplayCostForSubmit() {
        if (CostParseHelper.hasDisplayableCost(pendingOrderDisplayCost)) {
            return CostParseHelper.normalizeCostString(pendingOrderDisplayCost);
        }
        if (finalCost > 0) {
            return String.valueOf(finalCost);
        }
        String fromUrl = CostParseHelper.extractClientCostFromOrderUrl(urlOrder);
        if (fromUrl != null) {
            return fromUrl;
        }
        if (CostParseHelper.hasDisplayableCost(pendingGooglePayAmount)) {
            return CostParseHelper.normalizeCostString(pendingGooglePayAmount);
        }
        if (text_view_cost != null && text_view_cost.getText() != null) {
            return CostParseHelper.normalizeCostString(text_view_cost.getText().toString().trim());
        }
        return null;
    }


    public void orderFinished() throws MalformedURLException {
        if (!isAdded() || getActivity() == null || getContext() == null) {
            Logger.d(null, TAG, "Fragment not attached, cannot order");
            return;
        }

        Context ctx = requireContext(); // ← получаем актуальный контекст

        if (!verifyPhone()) {
            MyPhoneDialogFragment bottomSheetDialogFragment = MyPhoneDialogFragment.newInstance("visicom");
            bottomSheetDialogFragment.show(fragmentManager, "MyPhoneDialogFragment");
            progressBar.setVisibility(GONE);
        } else {

            constraintLayoutVisicomMain.setVisibility(GONE);

            if (textViewTo.getText().equals("")) {
                textViewTo.setText(ctx.getString(R.string.on_city_tv)); // ← ctx
            }
            String messageResult =
                    geoText.getText().toString() + " " + getString(R.string.to_message) +
                            textViewTo.getText() + ".";

            text_full_message.setText(messageResult);

            messageResult = ctx.getString(R.string.check_cost_message); // ← ctx
            textCostMessage.setText(messageResult);

            textStatusCar.setText(R.string.ex_st_0);

            Animation blinkAnimation = AnimationUtils.loadAnimation(ctx, R.anim.blink_animation); // ← ctx
            textStatusCar.startAnimation(blinkAnimation);

            String pay_method_message = "";
            switch (pay_method) {
                case "bonus_payment":
                    pay_method_message += " " + ctx.getString(R.string.pay_method_message_bonus); // ← ctx
                    break;
                case "card_payment":
                case "fondy_payment":
                case "mono_payment":
                case "wfp_payment":
                    pay_method_message += " " + ctx.getString(R.string.pay_method_message_card); // ← ctx
                    break;
                case "google_pay_payment":
                    pay_method_message += " " + ctx.getString(R.string.pay_method_message_google); // ← ctx
                    break;
                default:
                    pay_method_message += " " + ctx.getString(R.string.pay_method_message_nal); // ← ctx
            }

            carProgressBar.resumeAnimation();
            constraintLayoutVisicomFinish.setVisibility(VISIBLE);

            String displayCost = resolveOrderDisplayCostForSubmit();
            if (displayCost == null) {
                displayCost = text_view_cost != null && text_view_cost.getText() != null
                        ? text_view_cost.getText().toString().trim() : "";
            }
            EarlyOrderNavigationHelper.markSubmitStarted(ctx, pay_method, displayCost);

            ToJSONParserRetrofit parser = new ToJSONParserRetrofit();
            baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
            Logger.d(ctx, TAG, "orderFinished: " + baseUrl + urlOrder); // ← ctx

            parser.sendURLChannel(urlOrder, new Callback<>() {

                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, String> sendUrlMap = response.body();
                        if (!isAdded() || getActivity() == null) {
                            Logger.d(ctx, TAG, "Fragment detached — enrich after early nav");
                            EarlyOrderNavigationHelper.applyHttpEnrichment(ctx, sendUrlMap, pay_method);
                            EarlyOrderNavigationHelper.clearSubmitState();
                            return;
                        }
                        handleOrderFinished(sendUrlMap, pay_method, ctx);
                    } else {
                        EarlyOrderNavigationHelper.clearSubmitState();
                        btnVisible(VISIBLE);
                        String messageErr = getString(R.string.cost_error);
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(messageErr);
                        if (fragmentManager != null) {
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    EarlyOrderNavigationHelper.clearSubmitState();
                    if (!isAdded() || getActivity() == null) {
                        Logger.d(null, TAG, "Fragment detached during onFailure");
                        return;
                    }

                    FirebaseCrashlytics.getInstance().recordException(t);
                    Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
                    Logger.w(ctx, TAG, "NO INTERNET - Showing toast message"); // ← ctx
                }
            });
        }
    }

    private void handleOrderFinished(Map<String, String> sendUrlMap, String pay_method, Context context) {
        assert sendUrlMap != null;
        String orderWeb = sendUrlMap.get("order_cost");
        String message = sendUrlMap.get("message");
        Logger.d(context, TAG, "orderFinished: message " + message);
        assert orderWeb != null;

        if (isDuplicateOrderMessage(message)) {
            if (EarlyOrderNavigationHelper.isEarlyNavigationDone() || hasActiveOrderSession()) {
                EarlyOrderNavigationHelper.applyHttpEnrichment(context, sendUrlMap, pay_method);
                EarlyOrderNavigationHelper.clearSubmitState();
                Logger.d(context, TAG, "handleOrderFinished: duplicate with active session — ignore");
                return;
            }
        }
        if ("DuplicateActiveOrder".equals(message)) {
            message = "";
            sendUrlMap.put("message", "");
        }

        if (EarlyOrderNavigationHelper.isEarlyNavigationDone()) {
            EarlyOrderNavigationHelper.applyHttpEnrichment(context, sendUrlMap, pay_method);
            EarlyOrderNavigationHelper.clearSubmitState();
            Logger.d(context, TAG, "handleOrderFinished: early nav already done, skip navigate");
            return;
        }

        String dispatchUid = sendUrlMap.get("dispatching_order_uid");
        if (MainActivity.uid != null && !MainActivity.uid.isEmpty()
                && dispatchUid != null && MainActivity.uid.equals(dispatchUid)) {
            int dest = MainActivity.currentNavDestination;
            if (dest == R.id.nav_finish_separate) {
                Logger.d(context, TAG, "handleOrderFinished: повторный ответ для активного заказа, пропуск");
                return;
            }
        }

        if (!RoutePlaceMatcher.isCityRideOrder(sendUrlMap)
                && Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))
                && Objects.equals(sendUrlMap.get("lat"), sendUrlMap.get("from_lat"))
                && Objects.equals(sendUrlMap.get("lng"), sendUrlMap.get("from_lng"))
                && (message == null || message.isEmpty())) {
            Logger.w(context, TAG, "handleOrderFinished: некорректный маршрут (from==to), пропуск");
            if (isAdded()) {
                btnVisible(VISIBLE);
                constraintLayoutVisicomFinish.setVisibility(GONE);
                constraintLayoutVisicomMain.setVisibility(VISIBLE);
            }
            return;
        }

        boolean VisicomBackPressed = (boolean) sharedPreferencesHelperMain.getValue("VisicomBackPressed", false);

        if (!"0".equals(orderWeb)) {
            String to_name;
//            orderWeb = text_view_cost.getText().toString();
            if (RoutePlaceMatcher.isCityRideOrder(sendUrlMap)) {
                to_name = context.getString(R.string.on_city_tv);
                Logger.d(context, TAG, "orderFinished: to_name 1 " + to_name);
                if (!Objects.equals(sendUrlMap.get("lat"), "0")) {
                    insertRecordsOrders(
                            sendUrlMap.get("routefrom"), sendUrlMap.get("routefrom"),
                            sendUrlMap.get("routefromnumber"), sendUrlMap.get("routefromnumber"),
                            sendUrlMap.get("from_lat"), sendUrlMap.get("from_lng"),
                            sendUrlMap.get("from_lat"), sendUrlMap.get("from_lng"),
                            context
                    );
                }
            } else {
                if (Objects.equals(sendUrlMap.get("routeto"), "Точка на карте")) {
                    to_name = context.getString(R.string.end_point_marker);
                } else {
                    to_name = sendUrlMap.get("routeto") + " " + sendUrlMap.get("to_number");
                }
                Logger.d(context, TAG, "orderFinished: to_name 2 " + to_name);
                if (!Objects.equals(sendUrlMap.get("lat"), "0")) {
                    insertRecordsOrders(
                            sendUrlMap.get("routefrom"), to_name,
                            sendUrlMap.get("routefromnumber"), sendUrlMap.get("to_number"),
                            sendUrlMap.get("from_lat"), sendUrlMap.get("from_lng"),
                            sendUrlMap.get("lat"), sendUrlMap.get("lng"),
                            context
                    );
                }
            }
            Logger.d(context, TAG, "orderFinished: to_name 3" + to_name);
            String to_name_local = to_name;
            if (to_name.contains("по місту")
                    || to_name.contains("по городу")
                    || to_name.contains("around the city")) {
                to_name_local = context.getString(R.string.on_city_tv);
            }
            Logger.d(context, TAG, "orderFinished: to_name 4" + to_name_local);

            String required_time = sendUrlMap.get("required_time");
            Logger.d(context, TAG, "orderFinished: required_time " + required_time);
            if (required_time != null && !required_time.contains("1970")) {
                try {
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                    // Список возможных форматов
                    String[] formats = {
                            "dd.MM.yyyy HH:mm",
                            "yyyy-MM-dd'T'HH:mm",
                            "yyyy-MM-dd'T'HH:mm:ss"
                    };

                    Date date = null;
                    for (String format : formats) {
                        try {
                            @SuppressLint("SimpleDateFormat") SimpleDateFormat inputFormat = new SimpleDateFormat(format);
                            inputFormat.setLenient(false);
                            date = inputFormat.parse(required_time);
                            if (date != null) break;
                        } catch (ParseException ignored) {
                        }
                    }

                    if (date != null) {
                        required_time = " " + context.getString(R.string.time_order) + " " + outputFormat.format(date) + ".";
                    } else {
                        required_time = "";
                    }

                } catch (Exception e) {
                    required_time = "";
                }

            } else {
                required_time = "";
            }

            String pay_method_message = context.getString(R.string.pay_method_message_main);
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
                case "google_pay_payment":
                    pay_method_message += " " + context.getString(R.string.pay_method_message_google);
                    break;
                default:
                    pay_method_message += " " + context.getString(R.string.pay_method_message_nal);
            }
            String messageResult =
                    sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
                            to_name_local + "." +
                            required_time;

            if (required_time.isEmpty()) {
                messageResult =
                        sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
                                to_name_local + ".";
            }

            messageResult = cleanString(messageResult);

            String messagePayment = orderWeb + " " + context.getString(R.string.UAH) + " " + pay_method_message;

            String messageFondy = context.getString(R.string.fondy_message) + " " +
                    sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
                    to_name_local + ".";

            Logger.d(context, TAG, "orderFinished: messageResult " + messageResult);
            Logger.d(context, TAG, "orderFinished: to_name " + to_name);

//            List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
//            String comment = stringList.get(2);
//            String comment = sharedPreferencesHelperMain.getValue("comment", "no_comment").toString();
//            sendUrlMap.put("comment_info", comment);

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
                result = String.join(",", servicesChecked);
                Logger.d(context, TAG, "getTaxiUrlSearchGeo result:" + result + "/");
                sendUrlMap.put("extra_charge_codes", result);
            }
            String comment =  sendUrlMap.get("comment_info");
            Logger.d(context, TAG, "sendUrlMap: comment_info " + comment);
            String savedComment = sharedPreferencesHelperMain.getValue("comment", "").toString();
            if (!savedComment.trim().isEmpty() && !savedComment.equals("no_name") && !savedComment.equals("no_comment")) {
                comment = savedComment;
            } else {
                comment = "";
            }
            // Перезаписываем значение по ключу
            sendUrlMap.put("comment_info", comment);


            Logger.d(context, TAG, "sendUrlMap: comment_info " + sendUrlMap.get("comment_info"));
            Logger.d(context, TAG, "sendUrlMap: extra_charge_codes " + sendUrlMap.get("extra_charge_codes"));

            String orderUid = EarlyOrderNavigationHelper.resolveOrderUid(sendUrlMap);
            if (orderUid == null || orderUid.isEmpty()) {
                Logger.w(context, TAG, "handleOrderFinished: UID нет в HTTP — ждём Centrifugo");
                EarlyOrderNavigationHelper.applyHttpEnrichment(context, sendUrlMap, pay_method);
                if (EarlyOrderNavigationHelper.isSubmitInProgress()) {
                    return;
                }
                EarlyOrderNavigationHelper.clearSubmitState();
                if (isAdded()) {
                    btnVisible(VISIBLE);
                    constraintLayoutVisicomFinish.setVisibility(GONE);
                    constraintLayoutVisicomMain.setVisibility(VISIBLE);
                }
                return;
            }

            MainActivity.uid = orderUid;
            sharedPreferencesHelperMain.saveValue("uid_fcm", orderUid);
            sharedPreferencesHelperMain.saveValue("last_car_found_notify_uid", "");
            if (MainActivity.order_id != null && !MainActivity.order_id.isEmpty()) {
                PaymentSessionHelper.saveWfpOrderRef(orderUid, MainActivity.order_id);
            }
            ExecutionStatusViewModel.resetNewOrderSession(orderUid);
            Logger.d(context, "MainActivity.uid", "MainActivity.uid 1 " + orderUid);

            Bundle bundle = new Bundle();
            bundle.putString("messageResult_key", messageResult);
            bundle.putString("messagePay_key", messagePayment);
            bundle.putString("messageFondy_key", messageFondy);
            bundle.putString("messageCost_key", orderWeb);
            bundle.putSerializable("sendUrlMap", new HashMap<>(sendUrlMap));
            bundle.putString("UID_key", orderUid);
            viewModel.setStatusNalUpdate(true); //наюлюдение за опросом статусом нала
            navController.navigate(
                    R.id.nav_finish_separate,
                    bundle,
                    new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_visicom, true)
                            .build()
            );
            EarlyOrderNavigationHelper.clearSubmitState();


        } else if (!VisicomBackPressed) {
            EarlyOrderNavigationHelper.clearSubmitState();
            sharedPreferencesHelperMain.saveValue("VisicomBackPressed", false);
            btnVisible(VISIBLE);
            assert message != null;
            constraintLayoutVisicomFinish.setVisibility(GONE);
            constraintLayoutVisicomMain.setVisibility(VISIBLE);
            Logger.d(context, TAG, "2 orderFinished: message " + message);
            String addType = "60";
            if (message.contains("Дублирование") || "DuplicateActiveOrder".equals(message)) {
                if (hasActiveOrderSession()) {
                    Logger.d(context, TAG, "handleOrderFinished: duplicate dialog skipped — active order");
                    return;
                }
                sharedPreferencesHelperMain.saveValue("doubleOrderPref", true);
                showAddCostDoubleDialog(addType);
            } else if (message.equals("cash") || message.equals("cards only")) {
                if (message.equals("cards only")) {
                    addType = "45";
                    showAddCostDoubleDialog(addType);
                } else {
                    message = context.getString(R.string.black_list_message);
                    if (!isStateSaved() && isAdded()) {
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
                }
            } else if (message.equals("ErrorMessage")) {
                message = getResources().getString(R.string.server_error_connected);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            } else if (message.equals("ErrorCardPayment")) {
                message = getResources().getString(R.string.server_error_card_payment);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            } else {
                Logger.d(context, TAG, "pay_method " + pay_method);
                switch (pay_method) {
                    case "bonus_payment":
                    case "card_payment":
                    case "fondy_payment":
                    case "mono_payment":
                    case "wfp_payment":
                        changePayMethodToNal(context.getString(R.string.to_nal_payment));
                        break;
                    default:
                        message = getResources().getString(R.string.error_message);
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            }

            btnVisible(VISIBLE);
        } else {
            sharedPreferencesHelperMain.saveValue("VisicomBackPressed", false);
            btnVisible(VISIBLE);
            assert message != null;
            constraintLayoutVisicomFinish.setVisibility(GONE);
            constraintLayoutVisicomMain.setVisibility(VISIBLE);
        }
    }
    private static void insertRecordsOrders( String from, String to,
                                             String from_number, String to_number,
                                             String from_lat, String from_lng,
                                             String to_lat, String to_lng, Context context) {
        Logger.d(context, TAG, "insertRecordsOrders: from_lat" + from_lat);
        Logger.d(context, TAG, "insertRecordsOrders: from_lng" + from_lng);
        Logger.d(context, TAG, "insertRecordsOrders: to_lat" + to_lat);
        Logger.d(context, TAG, "insertRecordsOrders: to_lng" + to_lng);

        String selection = "from_street = ?";
        String[] selectionArgs = new String[] {from};
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor_from = database.query(MainActivity.TABLE_ORDERS_INFO,
                null, selection, selectionArgs, null, null, null);

        selection = "to_street = ?";
        selectionArgs = new String[] {to};

        Cursor cursor_to = database.query(MainActivity.TABLE_ORDERS_INFO,
                null, selection, selectionArgs, null, null, null);



        if (cursor_from.getCount() == 0 || cursor_to.getCount() == 0) {

            String sql = "INSERT INTO " + MainActivity.TABLE_ORDERS_INFO + " VALUES(?,?,?,?,?,?,?,?,?);";
            SQLiteStatement statement = database.compileStatement(sql);
            database.beginTransaction();
            try {
                statement.clearBindings();
                statement.bindString(2, from);
                statement.bindString(3, from_number);
                statement.bindString(4, from_lat);
                statement.bindString(5, from_lng);
                statement.bindString(6, to);
                statement.bindString(7, to_number);
                statement.bindString(8, to_lat);
                statement.bindString(9, to_lng);

                statement.execute();
                database.setTransactionSuccessful();

            } finally {
                database.endTransaction();
            }

        }

        cursor_from.close();
        cursor_to.close();

    }

    private boolean verifyPhone() {
        // 1. Проверка, что фрагмент прикреплён к активности
        if (!isAdded() || getActivity() == null || getContext() == null) {
            Logger.d(null, TAG, "Fragment not attached, skipping phone verification");
            return false;
        }

        // 2. Получаем актуальный Context
        Context ctx = requireContext(); // или getContext()

        List<String> stringList = logCursor(MainActivity.TABLE_USER_INFO, ctx);
        if (stringList.size() < 3) {
            Logger.d(ctx, TAG, "Invalid or empty stringList");
            return false;
        }

        String phone = stringList.get(2);
        if (phone == null || phone.isEmpty()) {
            Logger.d(ctx, TAG, "Phone number is null or empty");
            return false;
        }

        Logger.d(ctx, TAG, "onClick before validate: ");
        String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
        Logger.d(ctx, TAG, "onClick No validate: " + val);
        return val;
    }

    private static boolean verifyOrder() {
        Object value = sharedPreferencesHelperMain.getValue("verifyUserOrder", false);
        return Boolean.parseBoolean(String.valueOf(value));
    }



    private void changePayMethodMax(String textCost, String paymentType) {
        List<String> stringListCity = logCursor(MainActivity.CITY_INFO, context);

        String card_max_pay = stringListCity.get(4);
        String bonus_max_pay = stringListCity.get(5);
        // Инфлейтим макет для кастомного диалога
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);

        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setView(dialogView);
        alertDialog.setCancelable(false);
        // Настраиваем элементы макета


        TextView messageTextView = dialogView.findViewById(R.id.dialog_message);
        messageTextView.setText(R.string.max_limit_message);

        Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
        okButton.setOnClickListener(v -> {
            progressBar.setVisibility(VISIBLE);
            switch (paymentType) {
                case "bonus_payment":
                    if (Long.parseLong(bonus_max_pay) <= Long.parseLong(textCost) * 100) {
                        paymentType(context);
                    }
                    break;
                case "card_payment":
                case "fondy_payment":
                case "mono_payment":
                case "wfp_payment":
                    if (Long.parseLong(card_max_pay) <= Long.parseLong(textCost)) {
                        paymentType(context);
                    }
                    break;
                case "google_pay_payment":
                    if (Long.parseLong(card_max_pay) <= Long.parseLong(textCost)) {
                        paymentType(context);
                    }
                    break;
            }

            if (orderRout()) {
                googleVerifyAccount();
            } else {
                btnVisible(VISIBLE);
            }

            progressBar.setVisibility(GONE);
            alertDialog.dismiss();
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(v -> {
            btnVisible(VISIBLE);
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private void changePayMethodToNal(String message) {
        // Инфлейтим макет для кастомного диалога
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);

        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setView(dialogView);
        alertDialog.setCancelable(false);
        // Настраиваем элементы макета


        TextView messageTextView = dialogView.findViewById(R.id.dialog_message);
        messageTextView.setText(message);

        Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
        okButton.setOnClickListener(v -> {
            progressBar.setVisibility(VISIBLE);
            paymentType(context);

            if (orderRout()) {
                googleVerifyAccount();
            } else {
                btnVisible(VISIBLE);
            }

            progressBar.setVisibility(GONE);
            alertDialog.dismiss();
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(v -> {
            progressBar.setVisibility(GONE);
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private static void paymentType(Context context) {
        ContentValues cv = new ContentValues();
        cv.put("payment_type", "nal_payment");
        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[]{"1"});
        database.close();
        pay_method = "nal_payment";
        buttonBonus.setText(context.getString(R.string.nal_payment));
    }


    @SuppressLint({"UseCompatLoadingForDrawables", "ResourceAsColor"})
    @Override
    public void onResume() {
        super.onResume();
        Logger.d(context, TAG, "onResume 1" );


        isFragmentVisible = true;

        logAddrGuardState("onResume:before");
        clearStaleFromAddressUiIfDatabaseHasNoStart();
        // ✅ Если есть активный запрос, не восстанавливаем из БД
        if (!firstStart && !isUpdatingFromGPS && pendingAddressRequest == null
                && !AutoLocationAfterCityHelper.isGpsStartApplied()) {
            if (!hasActiveOrderSession()) {
                normalizeRouteMarkerToAroundCityWhenNoActiveOrder();
            }
            // Восстанавливаем адрес из БД только если нет активных обновлений
            List<String> startList = logCursor(MainActivity.ROUT_MARKER, context);
            if (startList.size() > 5) {
                String fromAddressString = startList.get(5);
                String uiBefore = binding.textGeo.getText().toString();
                List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);
                String currentCity = cityInfo.size() > 1 ? cityInfo.get(1) : "";
                boolean canRestore = isValidStartAddressForFromField(fromAddressString)
                        && com.taxi.easy.ua.utils.city.CityLastAddressHelper.shouldApplyLastAddress(
                        currentCity, startList.get(1), startList.get(2), fromAddressString);
                if (canRestore) {
                    if (!uiBefore.equals(fromAddressString)) {
                        logAddrGuardOverwrite("onResume:restoreFromDb", uiBefore, fromAddressString, "ROUT_MARKER sync");
                        binding.textGeo.setText(fromAddressString);
                    }
                } else {
                    clearInvalidStartInRouteMarker();
                    if (!uiBefore.trim().isEmpty()) {
                        logAddrGuardOverwrite("onResume:clearStaleUi", uiBefore, "", "ROUT_MARKER start invalid");
                        binding.textGeo.setText("");
                    }
                }
            }
            if (startList.size() > 6) {
                restoreDestinationFieldFromDatabase();
            }
        } else {
            Logger.d(context, ADDR_GUARD, String.format(Locale.US,
                    "[onResume:restore] SKIP | firstStart=%s isUpdatingFromGPS=%s pendingAddressRequest=%s gpsApplied=%s",
                    firstStart, isUpdatingFromGPS, pendingAddressRequest, AutoLocationAfterCityHelper.isGpsStartApplied()));
        }
        logAddrGuardState("onResume:after");


        if (!NetworkUtils.isNetworkAvailable(requireActivity())) {
            Logger.w(context, TAG, "NO INTERNET - sync network banner");
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).syncNetworkBanner();
            }
        }



        VisicomFragment.sendUrlMap = null;
        MainActivity.uid = null;
        EarlyOrderNavigationHelper.clearSubmitState();
        Logger.d(context, "MainActivity.uid", "MainActivity.uid 2 " + MainActivity.uid);

        MainActivity.orderResponse = null;
        viewModel.updateOrderResponse(null);
        viewModel.setTransactionStatus(null);
        viewModel.setCanceledStatus("no_canceled");

        textfrom = binding.textfrom;

        constraintLayoutVisicomMain.setVisibility(GONE);

        String cityCheckActivity = (String) sharedPreferencesHelperMain.getValue("CityCheckActivity", "**");
        Logger.d(context, TAG, "CityCheckActivity: " + cityCheckActivity);
        progressBar.setVisibility(GONE);
        if (cityCheckActivity.equals("run")) {
            btnVisible(VISIBLE);
        }

        String visible_shed = (String) sharedPreferencesHelperMain.getValue("visible_shed", "no");
        if(visible_shed.equals("no")) {
            Logger.d(context, TAG, "onResume 2" );
            btnVisible(GONE);
        } else  {
            if (NetworkUtils.isNetworkAvailable(context)) {
                Logger.d(context, TAG, "onResume 3" );
                btnVisible(VISIBLE);
            } else {
                Logger.d(context, TAG, "onResume 4" );
                btnVisible(GONE);
            }
        }
        Logger.d(context, TAG, "onResume 5" );
        constraintLayoutVisicomMain.setVisibility(VISIBLE);
        constraintLayoutVisicomFinish.setVisibility(GONE);

        databaseHelper = new DatabaseHelper(context);
        databaseHelperUid = new DatabaseHelperUid(context);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
//        new Thread(this::fetchRoutesCancel).start();
        try {
            statusOrder();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }


        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String cityMenu;
        switch (city) {
            case "Kyiv City":
                cityMenu = context.getString(R.string.city_kyiv);
                break;
            case "Dnipropetrovsk Oblast":
                cityMenu = context.getString(R.string.city_dnipro);
                break;
            case "Odessa":
                cityMenu = context.getString(R.string.city_odessa);
                break;
            case "Zaporizhzhia":
                cityMenu = context.getString(R.string.city_zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                cityMenu = context.getString(R.string.city_cherkassy);
                break;
            case "Lviv":
                cityMenu = context.getString(R.string.city_lviv);
                break;
            case "Ivano_frankivsk":
                cityMenu = context.getString(R.string.city_ivano_frankivsk);
                break;
            case "Vinnytsia":
                cityMenu = context.getString(R.string.city_vinnytsia);
                break;
            case "Poltava":
                cityMenu = context.getString(R.string.city_poltava);
                break;
            case "Sumy":
                cityMenu = context.getString(R.string.city_sumy);
                break;
            case "Kharkiv":
                cityMenu = context.getString(R.string.city_kharkiv);
                break;
            case "Chernihiv":
                cityMenu = context.getString(R.string.city_chernihiv);
                break;
            case "Rivne":
                cityMenu = context.getString(R.string.city_rivne);
                break;
            case "Ternopil":
                cityMenu = context.getString(R.string.city_ternopil);
                break;
            case "Khmelnytskyi":
                cityMenu = context.getString(R.string.city_khmelnytskyi);
                break;
            case "Zakarpattya":
                cityMenu = context.getString(R.string.city_zakarpattya);
                break;
            case "Zhytomyr":
                cityMenu = context.getString(R.string.city_zhytomyr);
                break;
            case "Kropyvnytskyi":
                cityMenu = context.getString(R.string.city_kropyvnytskyi);
                break;
            case "Mykolaiv":
                cityMenu = context.getString(R.string.city_mykolaiv);
                break;
            case "Chernivtsi":  // Обрати внимание, тут "С" кириллицей
                cityMenu = context.getString(R.string.city_chernivtsi);
                break;
            case "Lutsk":
                cityMenu = context.getString(R.string.city_lutsk);
                break;
            case "OdessaTest":
                cityMenu = "Test";
                break;
            default:
                cityMenu = context.getString(R.string.foreign_countries);
                break;
        }



        String newTitle = context.getString(R.string.menu_city) + " " + cityMenu;
        // Изменяем текст элемента меню
        MainActivity.navVisicomMenuItem.setTitle(newTitle);
        AppCompatActivity activity = (AppCompatActivity) context;
        Objects.requireNonNull(activity.getSupportActionBar()).setTitle(newTitle);

        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        api = stringList.get(2);

        buttonBonus = binding.btnBonus;

        setBtnBonusName(context);


        num1 = binding.num1;

        addCost = 0;
        updateAddCost(String.valueOf(addCost), context);

        numberFlagTo = "2";

        geoText = binding.textGeo;
        geoText.setOnClickListener(v -> {
            gpsBtn.setText(R.string.change);
            geoText.setBackgroundColor(689194);

            Bundle bundle = new Bundle();
            bundle.putString("start", "ok");
            bundle.putString("end", "no");
            openAddressSearch(bundle);

        });

        binding.clearButtonFrom.setOnClickListener(v -> {

            gpsBtn.setText(R.string.change);

            Bundle bundle = new Bundle();
            bundle.putString("start", "ok");
            bundle.putString("end", "no");
            openAddressSearch(bundle);

        });



        geo_marker = "visicom";

        Logger.d(context, TAG, "onCreateView: geo_marker " + geo_marker);

        buttonBonus.setOnClickListener(v -> {
            boolean black_list_yes = verifyOrder();
            Logger.d(context, TAG, "buttonBonus 2 " + black_list_yes);
            btnVisible(GONE);
            String costText = text_view_cost.getText().toString().trim();

            text_view_cost.setText("***");
            if (!costText.isEmpty() && costText.matches("\\d+")) {
                updateAddCost("0", context);
                MyBottomSheetBonusFragment bottomSheetDialogFragment = new MyBottomSheetBonusFragment(Long.parseLong(costText), geo_marker, api, text_view_cost);
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });

        textViewTo = binding.textTo;
        textViewTo.setOnClickListener(v -> {
            // Не сбрасываем ROUT_MARKER: на экране поиска подставится сохранённый адрес «куда».
            Bundle bundle = new Bundle();
            bundle.putString("start", "no");
            bundle.putString("end", "ok");
            openAddressSearch(bundle);

        });

        binding.clearButtonTo.setOnClickListener(v -> {
            textViewTo.setText("");
            updateRouteSettings();
            requestVisicomCostAfterRouteChange();
        });

        btn_minus = binding.btnMinus;
        btn_plus = binding.btnPlus;
        btnOrder = binding.btnOrder;


        btnAdd.setOnClickListener(v -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }
            btnVisible(GONE);
            sharedPreferencesHelperMain.saveValue("initial_page", "visicom");
            sharedPreferencesHelperMain.saveValue("old_cost", "0");
            NavController navController = NavHostFragment.findNavController(VisicomFragment.this);
            navController.navigate(R.id.nav_options, null, new NavOptions.Builder().build());
        });


        btn_minus.setOnClickListener(v -> {

            List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);

            String costString = text_view_cost.getText().toString();
            if (!costString.isEmpty()) {
                cost = Long.parseLong(costString);
            }

            String addCostString = stringListInfo.get(5);
            if (!addCostString.isEmpty()) {
                addCost = Long.parseLong(addCostString);
            }

            cost -= 5;
            addCost -= 5;
            if (cost < MIN_COST_VALUE) {
                addCost += 5;
                cost += 5;
            }
            updateAddCost(String.valueOf(addCost), context);
            text_view_cost.setText(String.valueOf(cost));
            finalCost = cost;
            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: btn_minus MIN_COST_VALUE " + MIN_COST_VALUE);

            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: btn_minus " + startCost);
            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: btn_minus " + finalCost);

        });

        btn_plus.setOnClickListener(v -> {
            List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);

            String costString = text_view_cost.getText().toString();
            if (!costString.isEmpty()) {
                cost = Long.parseLong(costString);
            }

            String addCostString = stringListInfo.get(5);
            if (!addCostString.isEmpty()) {
                addCost = Long.parseLong(addCostString);
            }

            cost += 5;
            addCost += 5;
            updateAddCost(String.valueOf(addCost), context);
            text_view_cost.setText(String.valueOf(cost));

            finalCost = cost;

            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost:  btn_plus MIN_COST_VALUE " + MIN_COST_VALUE);

            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: btn_plus " + startCost);
            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: btn_plus " + finalCost);

        });
        btnOrder.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
                Logger.w(context, TAG, "NO INTERNET - Showing toast message");
            }

            linearLayout.setVisibility(GONE);
            btnVisible(GONE);
            List<String> stringList1 = logCursor(MainActivity.CITY_INFO, context);

            pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);

            switch (stringList1.get(1)) {
                case "Kyiv City":
                case "Dnipropetrovsk Oblast":
                case "Odessa":
                case "Zaporizhzhia":
                case "Cherkasy Oblast":
                    break;
                case "OdessaTest":
                    if (pay_method.equals("bonus_payment")) {
                        String bonus = logCursor(MainActivity.TABLE_USER_INFO, context).get(5);
                        if (Long.parseLong(bonus) < cost * 100) {
                            paymentType(context);
                        }
                    }
                    break;
            }

            Logger.d(context, TAG, "onClick: pay_method " + pay_method);


            List<String> stringListCity = logCursor(MainActivity.CITY_INFO, context);
            String card_max_pay = stringListCity.get(4);
            Logger.d(context, TAG, "onClick:card_max_pay " + card_max_pay);

            String bonus_max_pay = stringListCity.get(5);
            Logger.d(context, TAG, "onClick:bonus_max_pay " + card_max_pay);
            switch (pay_method) {
                case "bonus_payment":
                    if (Long.parseLong(bonus_max_pay) <= Long.parseLong(text_view_cost.getText().toString()) * 100) {
                        changePayMethodMax(text_view_cost.getText().toString(), pay_method);
                    } else {
                        if (orderRout()) {
                            googleVerifyAccount();
                        }else {
                            btnVisible(VISIBLE);
                        }
                    }
                    break;
                case "card_payment":
                case "fondy_payment":
                case "mono_payment":
                case "wfp_payment":
                    if (Long.parseLong(card_max_pay) <= Long.parseLong(text_view_cost.getText().toString())) {
                        changePayMethodMax(text_view_cost.getText().toString(), pay_method);
                    } else {
                        if (orderRout()) {
                            googleVerifyAccount();
                        }else {
                            btnVisible(VISIBLE);
                        }
                    }
                    break;
                default:
                    if (orderRout()) {
                        googleVerifyAccount();
                    }else {
                        btnVisible(VISIBLE);
                    }

            }

        });

        textwhere = binding.textwhere;
        num2 = binding.num2;

        location_update = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Обработка отсутствия необходимых разрешений
                location_update = true;
            }
        } else location_update = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        gpsBtn.setOnLongClickListener(v -> {
            sharedPreferencesHelperMain.saveValue("setStatusX", true);
            sharedPreferencesHelperMain.saveValue("old_cost", "0");
            viewModel.setStatusX(true);
            Boolean statusX = viewModel.getStatusX().getValue(); // Get the boolean value
            Logger.e(context,"setStatusX 45", "setStatusXUpdate: " + (statusX != null ? statusX.toString() : "null"));
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.getContext().startActivity(intent);
//            Toast.makeText(v.getContext(), "Откройте настройки и отключите GPS вручную", Toast.LENGTH_LONG).show();
            return true; // сигнализирует, что обработка завершена
        });

        gpsBtn.setOnClickListener(v -> {
            sharedPreferencesHelperMain.saveValue("old_cost", "0");
            v.animate()
                    .scaleX(0.9f) // Уменьшить до 90% по X
                    .scaleY(0.9f) // Уменьшить до 90% по Y
                    .setDuration(100) // Длительность анимации
                    .withEndAction(() -> {
                        // Возврат к исходному размеру
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                        // Логика нажатия
                        gpsButSetOnClickListener (locationManager);
                        schedule.setVisibility(VISIBLE);
                        shed_down.setVisibility(VISIBLE);
                    })
                    .start();

        });

        // Получаем текущее состояние
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean hasPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean xStatus = (boolean) sharedPreferencesHelperMain.getValue("setStatusX", true);

        if (gpsEnabled) {
            if (hasPermission) {
                // Есть разрешение - зеленая, если не в режиме X
                // НЕ вызываем setStatusX здесь, так как это сделает observer
                updateGpsButtonCross(xStatus);
            } else {
                // Нет разрешения - желтая (show=false покажет желтую)
                updateGpsButtonCross(false);
            }
        } else {
            // GPS выключен - красная (show=false покажет красную)
            updateGpsButtonCross(false);
            viewModel.setStatusGpsUpdate(false);
        }
// Обновляем ViewModel, но НЕ вызываем updateGpsButtonCross повторно
        viewModel.setStatusX(xStatus);

        if (NetworkUtils.isNetworkAvailable(context)) {
            if (geoText.getText().toString().isEmpty()) {

                binding.textfrom.setVisibility(GONE);
                num1.setVisibility(GONE);
                binding.textwhere.setVisibility(GONE);
            }
            String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
            if (!userEmail.equals("email")) {
                requestVisicomCost(resolveVisicomCostSourceOnResume());
                readTariffInfo();
            }

        } else {
            binding.textwhere.setVisibility(GONE);
            progressBar.setVisibility(GONE);
        }

        scheduleUpdate();
        addCheck(context);

        View rootView = getView();
        if (rootView != null) {
            rootView.postDelayed(this::updateApp, 8_000);
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(this::updateApp, 8_000);
        }
        maybeAutoApplyLocationAfterCity();
        restoreGpsCrossIfPendingUserApply();
        dismissGpsCrossAfterGeoCityChangeIfReady();
        syncGpsCrossAfterResume();

    }

    /** Финальное состояние крестика GPS после всех авто-обработчиков onResume. */
    private void syncGpsCrossAfterResume() {
        if (!isAdded() || binding == null || locationManager == null) {
            return;
        }
        if (AutoLocationAfterCityHelper.isGpsStartApplied()) {
            boolean xStatus = (boolean) sharedPreferencesHelperMain.getValue("setStatusX", false);
            viewModel.setStatusX(xStatus);
            updateGpsButtonCross(xStatus);
            return;
        }
        boolean xStatus = (boolean) sharedPreferencesHelperMain.getValue("setStatusX", false);
        String uiStart = geoText.getText() != null ? geoText.getText().toString() : "";
        if (AutoLocationAfterCityHelper.isGpsPendingUserApply() || xStatus
                || isValidStartAddressForFromField(uiStart)) {
            sharedPreferencesHelperMain.saveValue("setStatusX", true);
            viewModel.setStatusX(true);
            updateGpsButtonCross(true);
        }
    }

    /** После смены города по геопозиции — снять крестик, если авто-GPS уже не идёт. */
    private void dismissGpsCrossAfterGeoCityChangeIfReady() {
        if (!isAdded() || binding == null) {
            return;
        }
        if (!AutoLocationAfterCityHelper.isCityChangedViaGeo()) {
            return;
        }
        String uiStart = geoText.getText() != null ? geoText.getText().toString() : "";
        if (isValidStartAddressForFromField(uiStart) && !AutoLocationAfterCityHelper.isGpsStartApplied()) {
            return;
        }
        if (AutoLocationAfterCityHelper.isCityReady()
                && !AutoLocationAfterCityHelper.isPending()
                && !autoLocationFromCityLoad
                && !isUpdatingFromGPS) {
            AutoLocationAfterCityHelper.clearCityChangedViaGeo();
            finishAutoLocationGpsButtonState();
        }
    }

    private void restoreGpsCrossIfPendingUserApply() {
        if (!isAdded() || binding == null) {
            return;
        }
        if (AutoLocationAfterCityHelper.isCityChangedViaGeo()) {
            return;
        }
        if (AutoLocationAfterCityHelper.isGpsPendingUserApply()) {
            sharedPreferencesHelperMain.saveValue("setStatusX", true);
            viewModel.setStatusX(true);
            updateGpsButtonCross(true);
        }
    }

    /**
     * После загрузки города (флаг pending): один раз запросить геолокацию.
     * При отказе или без разрешения — адрес из последнего заказа в ROUT_MARKER.
     */
    private void maybeAutoApplyLocationAfterCity() {
        if (!isAdded() || binding == null || context == null) {
            return;
        }
        if (!AutoLocationAfterCityHelper.isCityReady() || isUpdatingFromGPS) {
            return;
        }

        clearStaleFromAddressUiIfDatabaseHasNoStart();

        AutoLocationAfterCityHelper.syncFromSystemPermission(context);

        boolean pending = AutoLocationAfterCityHelper.isPending();
        boolean hasPermission = AutoLocationAfterCityHelper.hasLocationPermission(context);

        if (!pending) {
            return;
        }

        if (!hasPermission) {
            if (!AutoLocationAfterCityHelper.wasPromptShown()) {
                AutoLocationAfterCityHelper.markPromptShown();
                requestLocationPermissions();
                return;
            }
            AutoLocationAfterCityHelper.clearPending();
            applyLastOrderAddressFromRouteMarker();
            return;
        }

        AutoLocationAfterCityHelper.markEverGranted();
        AutoLocationAfterCityHelper.clearPending();
        startAutoLocationAfterCityIfPossible();
    }

    private void startAutoLocationAfterCityIfPossible() {
        if (!isAdded() || locationManager == null) {
            return;
        }
        location_update = true;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Logger.d(context, TAG, "Авто-геолокация после выбора города");
            autoLocationFromCityLoad = true;
            if (!AutoLocationAfterCityHelper.isCityChangedViaGeo()) {
                sharedPreferencesHelperMain.saveValue("setStatusX", true);
                viewModel.setStatusX(true);
                updateGpsButtonCross(true);
            }
            detectAndStoreAutoLocationAfterCity();
        } else {
            applyLastOrderAddressFromRouteMarker();
        }
    }

    /**
     * После города: определить GPS. Если «Откуда» пустое — подставить GPS;
     * иначе только prefs + крестик на кнопке GPS.
     */
    private void detectAndStoreAutoLocationAfterCity() {
        if (!isAdded() || context == null || !autoLocationFromCityLoad) {
            return;
        }
        if (isUpdatingFromGPS) {
            Logger.d(context, TAG, "Авто-GPS: определение уже выполняется");
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            finishAutoLocationAfterCityWithLastOrderAddress();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        isUpdatingFromGPS = true;

        FusedLocationProviderClient fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context);

        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (!isAdded()) {
                        isUpdatingFromGPS = false;
                        return;
                    }
                    if (location == null) {
                        Logger.w(context, TAG, "Авто-GPS: локация null");
                        finishAutoLocationAfterCityWithLastOrderAddress();
                        return;
                    }

                    final double latitude = location.getLatitude();
                    final double longitude = location.getLongitude();
                    Logger.d(context, TAG, String.format(Locale.US,
                            "Авто-GPS: координаты определены lat=%.6f, lon=%.6f", latitude, longitude));

                    List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                    String api = stringList.get(2);
                    String language = Locale.getDefault().getLanguage();
                    baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                    String urlFrom = baseUrl + "/" + api + "/android/fromSearchGeoLocal/"
                            + latitude + "/" + longitude + "/" + language;
                    Logger.d(context, TAG, "Авто-GPS: запрос адреса по координатам: " + urlFrom);

                    FromJSONParserRetrofit.sendURL(urlFrom, result -> {
                        if (!isAdded()) {
                            isUpdatingFromGPS = false;
                            return;
                        }
                        String address = null;
                        if (result != null) {
                            address = result.get("route_address_from");
                            if (address != null && address.contains("Точка на карте")) {
                                address = context.getString(R.string.startPoint);
                            }
                        }
                        if (address != null && !address.trim().isEmpty()) {
                            Logger.d(context, TAG, "Авто-GPS: найденный адрес по GPS: \"" + address + "\"");
                        } else {
                            Logger.w(context, TAG, "Авто-GPS: адрес по GPS не получен (пустой ответ геокодера)");
                        }
                        AutoLocationAfterCityHelper.saveDetectedCoordinates(latitude, longitude, address);
                        logAutoDetectedRouteAndPath(latitude, longitude, address);
                        if (address != null && !address.trim().isEmpty() && isAdded() && binding != null) {
                            gpsClickAwaitingAutoDetected = false;
                            Logger.d(context, ADDR_GUARD,
                                    "autoGps: координаты сохранены в prefs — к заказу только по нажатию GPS");
                            finishAutoLocationDetectedWithoutOverwritingOrder();
                            logAddrGuardState("autoGps:storedOnly");
                            return;
                        }
                        Logger.w(context, ADDR_GUARD, "autoGps: геокод пуст — fallback на кэш lastOrder");
                        logAddrGuardState("autoGps:beforeFallbackCache");
                        finishAutoLocationAfterCityWithLastOrderAddress();
                        logAddrGuardState("autoGps:afterFallbackCache");
                    });
                })
                .addOnFailureListener(e -> {
                    Logger.e(context, TAG, "Авто-GPS: ошибка определения: " + e.getMessage());
                    finishAutoLocationAfterCityWithLastOrderAddress();
                });
    }

    /** Нажатие GPS: перезаписать «Откуда» и ROUT_MARKER по геолокации. */
    private void logAddrGuardState(String where) {
        if (context == null) {
            return;
        }
        String uiStart = "n/a";
        if (binding != null && geoText != null && geoText.getText() != null) {
            uiStart = geoText.getText().toString();
        }
        List<String> route = logCursor(MainActivity.ROUT_MARKER, context);
        String dbStart = route.size() > 5 ? String.valueOf(route.get(5)) : "n/a";
        String dbLat = route.size() > 1 ? route.get(1) : "n/a";
        String dbLon = route.size() > 2 ? route.get(2) : "n/a";
        Object source = sharedPreferencesHelperMain.getValue(AutoLocationAfterCityHelper.KEY_START_ADDRESS_SOURCE, "");
        Logger.d(context, ADDR_GUARD, String.format(Locale.US,
                "[%s] uiStart='%s' | dbStart='%s' | dbLat=%s dbLon=%s | gpsApplied=%s | isUpdatingFromGPS=%s | pendingAddressRequest=%s | autoCityLoad=%s | source=%s",
                where, uiStart, dbStart, dbLat, dbLon,
                AutoLocationAfterCityHelper.isGpsStartApplied(),
                isUpdatingFromGPS,
                pendingAddressRequest,
                autoLocationFromCityLoad,
                source));
    }

    private void logAddrGuardOverwrite(String where, String from, String to, String reason) {
        if (context == null) {
            return;
        }
        Logger.w(context, ADDR_GUARD, String.format(Locale.US,
                "[%s] OVERWRITE '%s' -> '%s' | reason=%s", where, from, to, reason));
    }

    private void applyGpsLocationToOrder(double latitude, double longitude, String address) {
        applyGpsLocationToOrder(latitude, longitude, address, false);
    }

    private void applyGpsLocationToOrder(double latitude, double longitude, String address, boolean fromAutoGps) {
        if (!isAdded() || binding == null) {
            isUpdatingFromGPS = false;
            return;
        }
        String uiBefore = geoText.getText() != null ? geoText.getText().toString() : "";
        logAddrGuardState("applyGps:before fromAuto=" + fromAutoGps);
        Logger.d(context, TAG, String.format(Locale.US,
                "GPS: применяем к заказу lat=%.6f, lon=%.6f, address='%s'", latitude, longitude, address));
        updateCoordinatesInDatabase(latitude, longitude, address);
        AutoLocationAfterCityHelper.markGpsStartApplied();
        if (!uiBefore.equals(address)) {
            logAddrGuardOverwrite("applyGps", uiBefore, address, fromAutoGps ? "autoGps geocode" : "manualGps");
        }
        geoText.setText(address);
        progressBar.setVisibility(View.GONE);
        isUpdatingFromGPS = false;
        finishAutoLocationGpsButtonState();
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        if (!"email".equals(userEmail) && NetworkUtils.isNetworkAvailable(context)) {
            requestVisicomCost(fromAutoGps ? "autoGps" : "manualGps");
            readTariffInfo();
        }
        logAddrGuardState("applyGps:after fromAuto=" + fromAutoGps);
    }

    private void finishAutoLocationAfterCityWithLastOrderAddress() {
        if (!isAdded()) {
            isUpdatingFromGPS = false;
            autoLocationFromCityLoad = false;
            return;
        }
        logAddrGuardState("finishAutoLocation:entry");
        progressBar.setVisibility(View.GONE);
        isUpdatingFromGPS = false;
        autoLocationFromCityLoad = false;

        if (AutoLocationAfterCityHelper.isCityChangedViaGeo()) {
            Logger.d(context, ADDR_GUARD, "finishAutoLocation: cityChangedViaGeo → lastOrder cache");
            AutoLocationAfterCityHelper.clearCityChangedViaGeo();
            finishAutoLocationGpsButtonState();
            applyLastOrderAddressFromRouteMarker(false);
            logAddrGuardState("finishAutoLocation:afterCityChangedViaGeo");
            return;
        }

        Logger.d(context, ADDR_GUARD, "finishAutoLocation: GPS не применён → lastOrder cache");
        finishAutoLocationGpsButtonState();
        applyLastOrderAddressFromRouteMarker(false);
        logAddrGuardState("finishAutoLocation:afterLastOrder");
    }

    private void logAutoDetectedRouteAndPath(double detectedLat, double detectedLon, String detectedAddress) {
        Logger.d(context, TAG, "═══════════════════════════════════════════");
        Logger.d(context, TAG, "Авто-GPS после города: применение к заказу при успешном геокоде");
        Logger.d(context, TAG, String.format(Locale.US,
                "GPS координаты: lat=%.6f, lon=%.6f", detectedLat, detectedLon));
        if (detectedAddress != null && !detectedAddress.trim().isEmpty()) {
            Logger.d(context, TAG, "GPS адрес (геокод): \"" + detectedAddress + "\"");
        } else {
            Logger.d(context, TAG, "GPS адрес (геокод): не определён");
        }
        Logger.d(context, TAG, String.format(Locale.US,
                "Сохранено в prefs: lat=%.6f, lon=%.6f, address='%s'",
                detectedLat, detectedLon, detectedAddress != null ? detectedAddress : ""));
        List<String> route = logCursor(MainActivity.ROUT_MARKER, context);
        if (route.size() > 6) {
            Logger.d(context, TAG, String.format(Locale.US,
                    "Маршрут ROUT_MARKER (активный в заказе): startLat=%s, startLan=%s, to_lat=%s, to_lng=%s, start='%s', finish='%s'",
                    route.get(1), route.get(2), route.get(3), route.get(4), route.get(5), route.get(6)));
        } else {
            Logger.d(context, TAG, "Маршрут ROUT_MARKER: запись пуста или неполная, size=" + route.size());
        }
        Logger.d(context, TAG, "═══════════════════════════════════════════");
    }

    private void finishAutoLocationGpsButtonState() {
        autoLocationFromCityLoad = false;
        AutoLocationAfterCityHelper.clearGpsPendingUserApply();
        sharedPreferencesHelperMain.saveValue("setStatusX", false);
        viewModel.setStatusX(false);
        updateGpsButtonCross(false);
    }

    /** Стартовая точка уже выбрана вручную или из кэша последнего заказа. */
    private boolean hasEstablishedStartAddressInRouteMarker() {
        if (context == null) {
            return false;
        }
        List<String> route = logCursor(MainActivity.ROUT_MARKER, context);
        if (route.size() <= 5) {
            return false;
        }
        String start = route.get(5);
        if (start == null || start.trim().isEmpty()) {
            return false;
        }
        try {
            return Double.parseDouble(route.get(1)) != 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** GPS определён, но «Откуда» не трогаем — пользователь может нажать кнопку GPS. */
    private void finishAutoLocationDetectedWithoutOverwritingOrder() {
        if (!isAdded()) {
            isUpdatingFromGPS = false;
            autoLocationFromCityLoad = false;
            return;
        }
        progressBar.setVisibility(View.GONE);
        isUpdatingFromGPS = false;
        autoLocationFromCityLoad = false;
        if (!AutoLocationAfterCityHelper.isCityChangedViaGeo()) {
            updateGpsButtonCross(true);
        }
    }

    private void applyLastOrderAddressFromRouteMarker() {
        applyLastOrderAddressFromRouteMarker(false);
    }

    private void applyLastOrderAddressFromRouteMarker(boolean keepGpsCross) {
        if (!isAdded() || binding == null) {
            return;
        }
        logAddrGuardState("lastOrderCache:entry keepGpsCross=" + keepGpsCross);
        if (AutoLocationAfterCityHelper.isGpsStartApplied() || isUpdatingFromGPS) {
            Logger.d(context, ADDR_GUARD, String.format(Locale.US,
                    "lastOrderCache: SKIP | gpsApplied=%s isUpdatingFromGPS=%s",
                    AutoLocationAfterCityHelper.isGpsStartApplied(), isUpdatingFromGPS));
            return;
        }
        Logger.d(context, TAG, "Подстановка адреса из последнего заказа (без GPS)");

        List<String> route = logCursor(MainActivity.ROUT_MARKER, context);
        if (route.size() <= 5) {
            return;
        }

        String startAddress;
        try {
            double startLat = Double.parseDouble(route.get(1));
            if (startLat == 0.0) {
                return;
            }
            startAddress = route.get(5);
        } catch (NumberFormatException e) {
            Logger.e(context, TAG, "Некорректные координаты в ROUT_MARKER: " + e.getMessage());
            return;
        }

        List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);
        String currentCity = cityInfo.size() > 1 ? cityInfo.get(1) : "";
        if (!com.taxi.easy.ua.utils.city.CityLastAddressHelper.shouldApplyLastAddress(
                currentCity, route.get(1), route.get(2), startAddress)
                || !isValidStartAddressForFromField(startAddress)) {
            Logger.d(context, ADDR_GUARD, "lastOrderCache: SKIP — адрес не для текущего города");
            clearInvalidStartInRouteMarker();
            clearStaleFromAddressUiIfDatabaseHasNoStart();
            return;
        }

        String uiBefore = geoText.getText() != null ? geoText.getText().toString() : "";
        if (!uiBefore.equals(startAddress)) {
            logAddrGuardOverwrite("lastOrderCache", uiBefore, startAddress, "ROUT_MARKER last order");
        }
        geoText.setText(startAddress);
        if (!keepGpsCross) {
            sharedPreferencesHelperMain.saveValue("setStatusX", false);
            viewModel.setStatusX(false);
            updateGpsButtonCross(false);
        }

        if (!hasActiveOrderSession()) {
            normalizeRouteMarkerToAroundCityWhenNoActiveOrder();
        }

        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        if (!"email".equals(userEmail) && NetworkUtils.isNetworkAvailable(context)) {
            requestVisicomCost("lastOrderAddress");
            readTariffInfo();
        }
        logAddrGuardState("lastOrderCache:after");
    }

    private void requestVisicomCost(String source) {
        if (!isAdded() || context == null) {
            return;
        }
        if (MainActivity.uid != null && !MainActivity.uid.isEmpty()) {
            Logger.d(context, TAG, "visicomCost пропущен (активный заказ), источник: " + source);
            return;
        }
        if (googlePayOrderHoldInProgress || EarlyOrderNavigationHelper.isSubmitInProgress()) {
            Logger.d(context, TAG, "visicomCost пропущен (оплата/отправка заказа), источник: " + source);
            return;
        }
        List<String> userInfo = logCursor(MainActivity.TABLE_USER_INFO, context);
        if (userInfo.size() <= 3 || "email".equals(userInfo.get(3))) {
            return;
        }
        if (!NetworkUtils.isNetworkAvailable(context)) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean forceRetry = isForceRetrySource(source);

        if (!forceRetry && ("lastOrderAddress".equals(source) || "foreground".equals(source))) {
            if (CostCalculationProgressBar.isCalculationInProgress()) {
                Logger.d(context, TAG, "visicomCost пропущен (расчёт идёт), источник: " + source);
                return;
            }
            if (lastCostCalculationFailureMs > 0
                    && now - lastCostCalculationFailureMs < VISICOM_COST_LAST_ADDRESS_COOLDOWN_MS) {
                Logger.d(context, TAG, "visicomCost пропущен (после ошибки расчёта), источник: " + source);
                return;
            }
            if (lastVisicomCostRequestMs > 0
                    && now - lastVisicomCostRequestMs < VISICOM_COST_LAST_ADDRESS_COOLDOWN_MS) {
                Logger.d(context, TAG, "visicomCost пропущен (cooldown "
                        + (VISICOM_COST_LAST_ADDRESS_COOLDOWN_MS / 1000) + "s), источник: " + source);
                return;
            }
        }

        if (!forceRetry && now - lastVisicomCostRequestMs < VISICOM_COST_DEBOUNCE_MS) {
            Logger.d(context, TAG, "visicomCost пропущен (debounce), источник: " + source);
            return;
        }
        if (!isRouteReadyForCostFromDatabase()) {
            if (!hasActiveOrderSession()) {
                normalizeRouteMarkerToAroundCityWhenNoActiveOrder();
            }
            if (!isRouteReadyForCostFromDatabase()) {
                if (tryApplyCachedAroundCityCost()) {
                    Logger.d(context, TAG, "visicomCost: кэш по городу, источник: " + source);
                    return;
                }
                Logger.d(context, TAG, "visicomCost отложен (маршрут не готов), источник: " + source);
                hideCostCalculationProgress();
                return;
            }
        }
        lastVisicomCostRequestMs = now;
        lastCost = null;
        resetRealtimeOrderCostDedup();
        CostPreviewHint preview = resolveCostPreviewForRecalc();
        if (preview != null) {
            showCostRecalculatingWithPreview(preview);
        } else {
            showCostCalculationProgress();
        }
        try {
            Logger.d(context, TAG, "visicomCost, источник: " + source);
            visicomCost();
        } catch (MalformedURLException e) {
            Logger.e(context, TAG, "visicomCost (" + source + "): " + e.getMessage());
            hideCostCalculationProgress();
            if (isAdded()) {
                btnVisible(VISIBLE);
            }
        }
    }

    private void gpsButSetOnClickListener(LocationManager locationManager) {
        if (locationManager != null) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                if (loadPermissionRequestCount() >= 3 && !location_update) {
                    sharedPreferencesHelperMain.saveValue("setStatusX", true);
                    viewModel.setStatusX(true);
                    MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment(getString(R.string.location_on));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                } else {
                    sharedPreferencesHelperMain.saveValue("setStatusX", true);
                    viewModel.setStatusX(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            checkPermission();
                        }
                    } else {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            checkPermission();
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        location_update = true;
                    }
                } else {
                    location_update = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                }

                Logger.d(context, TAG, "locationManager: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));

                if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                    Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
                    Logger.w(context, TAG, "NO INTERNET - Showing toast message");
                } else if (location_update) {
                    String searchText = getString(R.string.search_text) + "...";
                    progressBar.setVisibility(View.VISIBLE);
                    Toast.makeText(context, searchText, Toast.LENGTH_SHORT).show();
                    if (autoLocationFromCityLoad && isUpdatingFromGPS) {
                        gpsClickAwaitingAutoDetected = true;
                        Logger.d(context, TAG, "GPS: ждём завершения авто-GPS после города (применим свежую локацию)");
                        return;
                    }
                    firstLocation();
                }
            } else {
                sharedPreferencesHelperMain.saveValue("setStatusX", true);
                viewModel.setStatusX(true);
                MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        } else {
            MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        }

        // ✅ ЕДИНЫЙ ЦЕНТРАЛИЗОВАННЫЙ КОД ДЛЯ ОБНОВЛЕНИЯ ФОНА КНОПКИ
        updateGpsButtonCross(Boolean.TRUE.equals(viewModel.getStatusX().getValue()));

    }
    private void checkPermission() {

        requestLocationPermissions();
    }
    private void requestLocationPermissions() {
        permissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }
    public static void setBtnBonusName(Context context) {
        String btnBonusName;
        String pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
        switch (pay_method) {
            case "bonus_payment":
                btnBonusName = context.getString(R.string.btn_bon);
                break;
            case "card_payment":
            case "fondy_payment":
            case "mono_payment":
            case "wfp_payment":
                btnBonusName = context.getString(R.string.btn_card);
                break;
            case "google_pay_payment":
                btnBonusName = context.getString(R.string.btn_pay_google);
                break;
            default:
                btnBonusName = context.getString(R.string.btn_cache);
        }
        buttonBonus.setText(btnBonusName);
    }

    @Override
    public void onShowButtons(int visibility) {
        btnVisible(visibility);
    }

    private void firstLocation() {
        autoLocationFromCityLoad = false;

        if (AutoLocationAfterCityHelper.isGpsPendingUserApply()
                && AutoLocationAfterCityHelper.hasDetectedCoordinates()) {
            String pendingAddress = AutoLocationAfterCityHelper.getDetectedAddress();
            if (pendingAddress != null && !pendingAddress.trim().isEmpty()) {
                double pendingLat = AutoLocationAfterCityHelper.getDetectedLat();
                double pendingLon = AutoLocationAfterCityHelper.getDetectedLon();
                List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);
                String currentCity = cityInfo.size() > 1 ? cityInfo.get(1) : "";
                if (com.taxi.easy.ua.utils.city.CityLastAddressHelper.isNearSelectedCity(
                        currentCity, pendingLat, pendingLon)) {
                    applyGpsLocationToOrder(pendingLat, pendingLon, pendingAddress);
                    return;
                }
                Logger.d(context, ADDR_GUARD,
                        "GPS: кэш вне выбранного города — первое нажатие через CityFinder");
            }
        }

        // ✅ Защита от повторных вызовов
        if (isUpdatingFromGPS) {
            Logger.d(context, TAG, "GPS update already in progress, skipping");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        schedule.setVisibility(View.VISIBLE);
        shed_down.setVisibility(View.VISIBLE);

        Toast.makeText(context, context.getString(R.string.search), Toast.LENGTH_SHORT).show();

        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Logger.d(context, TAG, "Нет разрешения на геолокацию");
            progressBar.setVisibility(View.GONE);
            return;
        }

        // ✅ Устанавливаем флаг, что начали обновление
        isUpdatingFromGPS = true;
        viewModel.setStatusX(false);

        // ✅ Одноразовое получение текущей локации
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {

                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // ✅ Проверяем, изменились ли координаты
                        boolean coordinatesChanged = haveCoordinatesChanged(latitude, longitude);
                        Logger.d(context, TAG, "getCurrentLocation: координаты изменились = " + coordinatesChanged);

                        if (!coordinatesChanged && AutoLocationAfterCityHelper.isGpsStartApplied()) {
                            progressBar.setVisibility(View.GONE);
                            finishAutoLocationGpsButtonState();
                            isUpdatingFromGPS = false;
                            pendingAddressRequest = null;
                            Logger.d(context, TAG, "Пропускаем обновление - координаты не изменились");
                            return;
                        }




                        Logger.d(context, TAG, "getCurrentLocation: " + latitude + ", " + longitude);

                        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                        String api = stringList.get(2);
                        String language = Locale.getDefault().getLanguage();

                        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                        String urlFrom = baseUrl + "/" + api + "/android/fromSearchGeoLocal/" + latitude + "/" + longitude + "/" + language;

                        // ✅ Сохраняем уникальный ключ запроса
                        final String requestKey = latitude + "_" + longitude + "_";
                        pendingAddressRequest = requestKey;

                        FromJSONParserRetrofit.sendURL(urlFrom, result -> {
                            // ✅ Проверяем, что это последний запрос
                            if (!requestKey.equals(pendingAddressRequest)) {
                                Logger.d(context, TAG, "Ignoring stale address response");
                                progressBar.setVisibility(View.GONE);
                                isUpdatingFromGPS = false;
                                return;
                            }

                            if (!isAdded()) {
                                isUpdatingFromGPS = false;
                                pendingAddressRequest = null; // ✅ Сбрасываем запрос
                                return;
                            }

                            if (result != null) {
                                String FromAdressString = result.get("route_address_from");
                                Logger.d(context, TAG, "FromAdressString: " + FromAdressString);

                                if (FromAdressString != null && FromAdressString.contains("Точка на карте")) {
                                    FromAdressString = context.getString(R.string.startPoint);
                                }

                                // ✅ Проверяем, не тот же ли это адрес


                                // ✅ Обновляем UI
//                                geoText.setText(FromAdressString);
                                lastProcessedAddress = FromAdressString;


                                if (CityFinder.isCityFinderBusy()) {
                                    Logger.d(context, TAG, "CityFinder занят, пропускаем обновление города");
                                    progressBar.setVisibility(View.GONE);
                                    isUpdatingFromGPS = false;
                                    finishAutoLocationGpsButtonState();
                                    return;
                                }
                                // ✅ Обновляем город
                                CityFinder cityFinder = new CityFinder(
                                        context,
                                        latitude,
                                        longitude,
                                        FromAdressString,
                                        context
                                );

// ✅ Используем callback для определения, нужно ли обновлять позицию
                                // В методе firstLocation(), перед вызовом cityFinder.findCityWithCallback:

// ✅ Создаём финальную копию переменной
                                final String finalAddress = FromAdressString;
                                final double finalLatitude = latitude;
                                final double finalLongitude = longitude;

                                cityFinder.findCityWithCallback(latitude, longitude, (cityChanged, userConfirmed) -> {
                                    // ========== НАЧАЛО КОЛБЭКА ==========
                                    Logger.d(context, TAG, "═══════════════════════════════════════════");
                                    Logger.d(context, TAG, "🏁 CityCheckCallback ВЫЗВАН");
                                    Logger.d(context, TAG, "═══════════════════════════════════════════");
                                    Logger.d(context, TAG, "📊 Параметры колбэка:");
                                    Logger.d(context, TAG, "   ├─ cityChanged = " + cityChanged);
                                    Logger.d(context, TAG, "   ├─ userConfirmed = " + userConfirmed);
                                    Logger.d(context, TAG, "   ├─ finalLatitude = " + finalLatitude);
                                    Logger.d(context, TAG, "   ├─ finalLongitude = " + finalLongitude);
                                    Logger.d(context, TAG, "   └─ finalAddress = '" + finalAddress + "'");

                                    // ========== ЛОГИКА РЕШЕНИЯ ==========
                                    boolean shouldUpdatePosition = !cityChanged || (cityChanged && userConfirmed);

                                    Logger.d(context, TAG, "───────────────────────────────────────────");
                                    Logger.d(context, TAG, "🧠 Логика принятия решения:");
                                    Logger.d(context, TAG, "   ├─ Формула: shouldUpdatePosition = !cityChanged || (cityChanged && userConfirmed)");
                                    Logger.d(context, TAG, "   ├─ !cityChanged = " + (!cityChanged));
                                    Logger.d(context, TAG, "   ├─ (cityChanged && userConfirmed) = " + (cityChanged && userConfirmed));
                                    Logger.d(context, TAG, "   └─ РЕЗУЛЬТАТ: shouldUpdatePosition = " + shouldUpdatePosition);

                                    // ========== РАСШИФРОВКА РЕШЕНИЯ ==========
                                    if (!cityChanged) {
                                        sharedPreferencesHelperMain.saveValue("setStatusX", false);
                                        viewModel.setStatusX(false);
                                        Logger.d(context, TAG, "📝 Расшифровка: Город НЕ ИЗМЕНИЛСЯ → обновляем позицию");
                                    } else if (cityChanged && userConfirmed) {
                                        sharedPreferencesHelperMain.saveValue("setStatusX", false);
                                        viewModel.setStatusX(false);
                                        Logger.d(context, TAG, "📝 Расшифровка: Город ИЗМЕНИЛСЯ И пользователь СОГЛАСИЛСЯ → обновляем позицию");
                                    } else if (cityChanged && !userConfirmed) {
                                        sharedPreferencesHelperMain.saveValue("setStatusX", true);
                                        viewModel.setStatusX(true);
                                        updateGpsButtonCross(true);
                                        Logger.d(context, TAG, "📝 Расшифровка: Город ИЗМЕНИЛСЯ НО пользователь ОТКАЗАЛСЯ → НЕ обновляем позицию");
                                    }

                                    Logger.d(context, TAG, "───────────────────────────────────────────");

                                    // ========== ВЕТКА: ОБНОВЛЕНИЕ ПОЗИЦИИ ==========
                                    if (shouldUpdatePosition) {
                                        Logger.d(context, TAG, "✅ ПРИНЯТО РЕШЕНИЕ: Обновляем позицию и стоимость");
                                        Logger.d(context, TAG, "───────────────────────────────────────────");
                                        Logger.d(context, TAG, "🔄 НАЧАЛО обновления данных:");
                                        setCityAppbar();
                                        // Обновление координат в БД
                                        Logger.d(context, TAG, "   1️⃣ Обновление координат в БД...");
                                        Logger.d(context, TAG, "      ├─ Вызов updateCoordinatesInDatabase(" + finalLatitude + ", " + finalLongitude + ", '" + finalAddress + "')");

                                        long dbStartTime = System.currentTimeMillis();
                                        updateCoordinatesInDatabase(finalLatitude, finalLongitude, finalAddress);
                                        AutoLocationAfterCityHelper.markGpsStartApplied();
                                        long dbEndTime = System.currentTimeMillis();
                                        Logger.d(context, TAG, "      └─ ✅ БД обновлена за " + (dbEndTime - dbStartTime) + " мс");

                                        // Пересчёт стоимости
                                        Logger.d(context, TAG, "   2️⃣ Пересчёт стоимости...");
                                        Logger.d(context, TAG, "      ├─ Вызов visicomCost()");
                                        finishAutoLocationGpsButtonState();
                                        long costStartTime = System.currentTimeMillis();
                                        requestVisicomCostAfterRouteChange();
                                        long costEndTime = System.currentTimeMillis();
                                        Logger.d(context, TAG, "      └─ ✅ Запрос пересчёта стоимости за " + (costEndTime - costStartTime) + " мс");

                                        Logger.d(context, TAG, "🔄 ЗАВЕРШЕНО обновление данных");

                                    }
                                    // ========== ВЕТКА: ОТКАЗ ОТ ОБНОВЛЕНИЯ ==========
                                    else {
                                        sharedPreferencesHelperMain.saveValue("setStatusX", true);
                                        viewModel.setStatusX(true);
                                        Logger.d(context, TAG, "❌ ПРИНЯТО РЕШЕНИЕ: НЕ обновляем позицию и стоимость");
                                        Logger.d(context, TAG, "───────────────────────────────────────────");
                                        Logger.d(context, TAG, "🚫 Причина отказа:");

                                        if (cityChanged && !userConfirmed) {
                                            Logger.d(context, TAG, "   └─ Пользователь ОТКАЗАЛСЯ от смены города");
                                            Logger.d(context, TAG, "      └─ Старый город сохранён, новая позиция ИГНОРИРУЕТСЯ");
                                        } else {
                                            Logger.d(context, TAG, "   └─ Другая причина: cityChanged=" + cityChanged + ", userConfirmed=" + userConfirmed);
                                        }

                                        // Скрываем ProgressBar и показываем кнопки
                                        Logger.d(context, TAG, "───────────────────────────────────────────");
                                        Logger.d(context, TAG, "🎨 Восстановление UI:");


                                        geoText.setText("");
                                        textViewTo.setText("");
                                        btnVisible(GONE);
                                        if (progressBar != null) {
                                            progressBar.setVisibility(View.GONE);
                                            Logger.d(context, TAG, "   ├─ progressBar.setVisibility(GONE)");
                                        } else {
                                            Logger.w(context, TAG, "   ├─ ⚠️ progressBar = null, пропускаем");
                                        }
                                        Logger.d(context, TAG, "   └─ btnVisible(VISIBLE) - кнопки восстановлены");
                                    }

                                    // ========== ЗАВЕРШЕНИЕ КОЛБЭКА ==========
                                    Logger.d(context, TAG, "───────────────────────────────────────────");
                                    Logger.d(context, TAG, "🧹 Очистка флагов:");

                                    boolean wasUpdating = isUpdatingFromGPS;
                                    String wasPending = pendingAddressRequest;

                                    isUpdatingFromGPS = false;
                                    pendingAddressRequest = null;
                                    autoLocationFromCityLoad = false;

                                    Logger.d(context, TAG, "   ├─ isUpdatingFromGPS: " + wasUpdating + " → false");
                                    Logger.d(context, TAG, "   ├─ pendingAddressRequest: " + (wasPending != null ? wasPending : "null") + " → null");

                                    Logger.d(context, TAG, "═══════════════════════════════════════════");
                                    Logger.d(context, TAG, "🏁 CityCheckCallback ЗАВЕРШЁН");
                                    Logger.d(context, TAG, "═══════════════════════════════════════════\n");
                                });
                            } else {
                                Logger.d(context, TAG, "Ошибка при получении адреса");
                                progressBar.setVisibility(View.GONE);
                                isUpdatingFromGPS = false;
                                pendingAddressRequest = null;
                                if (autoLocationFromCityLoad) {
                                    finishAutoLocationGpsButtonState();
                                }
                            }
                        });
                    } else {
                        Logger.d(context, TAG, "Локация = null");
                        progressBar.setVisibility(View.GONE);
                        isUpdatingFromGPS = false;
                        pendingAddressRequest = null;
                        if (autoLocationFromCityLoad) {
                            finishAutoLocationGpsButtonState();
                        } else {
                            sharedPreferencesHelperMain.saveValue("setStatusX", true);
                            viewModel.setStatusX(true);
                            updateGpsButtonCross(true);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Logger.e(context, TAG, "Failed to get location: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    isUpdatingFromGPS = false;
                    pendingAddressRequest = null;
                    if (autoLocationFromCityLoad) {
                        finishAutoLocationGpsButtonState();
                    } else {
                        sharedPreferencesHelperMain.saveValue("setStatusX", true);
                        viewModel.setStatusX(true);
                        updateGpsButtonCross(true);
                    }
                    Toast.makeText(context, R.string.location_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void showLocationErrorDialog(Location location) {
        View rootView = requireView();
        String message = getString(R.string.location_blocked_message);
        message += buildWarningMessage(location);

        String actionText = getString(R.string.gnss_error_action);

        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snackbar.setAction(actionText, v -> {});
        snackbar.show();
    }
    private void showSuspiciousLocationWarning(Location location, TaxiLocationValidator.RiskLevel riskLevel) {
        String message = "";
        int duration = Toast.LENGTH_SHORT;

        // Логируем полученные параметры
        Logger.d(context, TAG, "showSuspiciousLocationWarning вызван");
        Logger.d(context, TAG, "RiskLevel: " + riskLevel);
        Logger.d(context, TAG, "Location details: " +
                "lat=" + location.getLatitude() +
                ", lon=" + location.getLongitude() +
                ", accuracy=" + location.getAccuracy() +
                ", speed=" + location.getSpeed() +
                ", isMock=" + location.isFromMockProvider());

        switch (riskLevel) {
            case BLOCK:
                message = getString(R.string.risk_level_block);
                duration = Toast.LENGTH_LONG;
                Logger.w(context, TAG, "ВЫСОКАЯ УГРОЗА: Локация заблокирована");
                break;

            case SUSPICIOUS:
                message = getString(R.string.risk_level_suspicious);
                duration = Toast.LENGTH_LONG;
                Logger.w(context, TAG, "СРЕДНЯЯ УГРОЗА: Подозрительная локация");
                break;

            case SAFE:
                message = getString(R.string.risk_level_safe);
                duration = Toast.LENGTH_SHORT;
                Logger.d(context, TAG, "Локация безопасна");
                break;

            default:
                Logger.e(context, TAG, "Неизвестный уровень риска: " + riskLevel);
                return;
        }

        // Логируем перед показом Toast
        Logger.d(context, TAG, "Показываем Toast: " + message + ", duration: " + duration);

        try {
            Toast.makeText(context, message, duration).show();
            Logger.d(context, TAG, "Toast показан успешно");
        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка при показе Toast: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        // Для блокировки еще показываем диалог
        if (riskLevel == TaxiLocationValidator.RiskLevel.BLOCK) {
            Logger.d(context, TAG, "Показываем диалог блокировки");
            showLocationErrorDialog(location);
        } else if (riskLevel == TaxiLocationValidator.RiskLevel.SUSPICIOUS) {
            Logger.d(context, TAG, "Подозрительная локация - можно показать дополнительный диалог");
            // Можно добавить дополнительную логику для SUSPICIOUS
        }
    }



    private String buildWarningMessage(Location location) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.suspicious_location_warning));

        if (location != null) {
            sb.append("\n\n");
            sb.append(getString(R.string.details)).append(":\n");

            if (location.isFromMockProvider()) {
                sb.append("✓ ").append(getString(R.string.location_mock_warning)).append("\n");
            }

            if (location.getAccuracy() > 50) {
                sb.append("✓ ").append(String.format(
                        getString(R.string.location_accuracy_warning),
                        location.getAccuracy()
                )).append("\n");
            }

            if (location.getSpeed() > 50) {
                float speedKmh = location.getSpeed() * 3.6f;
                sb.append("✓ ").append(String.format(
                        getString(R.string.location_speed_warning),
                        speedKmh
                )).append("\n");
            }
        }

        return sb.toString();
    }
    private boolean haveCoordinatesChanged(double newLat, double newLon) {
        double[] currentCoordinates = getCurrentCoordinatesFromDatabase();
        double currentLat = currentCoordinates[0];
        double currentLon = currentCoordinates[1];

        // ✅ Увеличил порог до 0.00005 (~5 метров) для стабильности
        double latDiff = Math.abs(newLat - currentLat);
        double lonDiff = Math.abs(newLon - currentLon);
        boolean changed = latDiff > 0.00005 || lonDiff > 0.00005;

        Logger.d(context, TAG, String.format(Locale.US,
                "haveCoordinatesChanged: changed=%b, " +
                        "old=(%.6f, %.6f), new=(%.6f, %.6f), " +
                        "diff(lat=%.6f, lon=%.6f), threshold=0.00005",
                changed, currentLat, currentLon, newLat, newLon, latDiff, lonDiff));

        return changed;
    }

    // Вспомогательный метод для получения координат из БД
    private double[] getCurrentCoordinatesFromDatabase() {
        double[] coordinates = {DEFAULT_LAT, DEFAULT_LON};

        try {
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            Cursor cursor = database.rawQuery("SELECT startLat, startLan FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);

            if (cursor.moveToFirst()) {
                coordinates[0] = cursor.getDouble(0);
                coordinates[1] = cursor.getDouble(1);
            }

            cursor.close();
            database.close();
        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка получения координат из БД: " + e.getMessage());
        }

        return coordinates;
    }
    private void updateCoordinatesInDatabase(double newLat, double newLon, String address) {
        String TAG = "updateCoordinatesInDatabase";
        Logger.d(context, TAG, "=== updateCoordinatesInDatabase START ===");
        Logger.d(context, TAG, "Входные параметры: newLat=" + newLat + ", newLon=" + newLon + ", address='" + address + "'");

        try {
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            Logger.d(context, TAG, "База данных открыта: " + MainActivity.DB_NAME);

            // Проверяем, существует ли таблица
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MainActivity.ROUT_MARKER + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "startLat REAL, startLan REAL, to_lat REAL, to_lng REAL, " +
                    "start TEXT, finish TEXT)");
            Logger.d(context, TAG, "Таблица " + MainActivity.ROUT_MARKER + " проверена/создана");

            // Сохраняем текущие значения конечной точки
            Logger.d(context, TAG, "Загружаем текущие значения конечной точки из БД...");
            Cursor cursor = database.rawQuery("SELECT to_lat, to_lng, finish FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);
            double existingToLat = 0.0;
            double existingToLng = 0.0;
            String existingFinish = "";

            if (cursor.moveToFirst()) {
                existingToLat = cursor.getDouble(0);
                existingToLng = cursor.getDouble(1);
                existingFinish = cursor.getString(2) != null ? cursor.getString(2) : "";
                Logger.d(context, TAG, String.format(Locale.US,
                        "Найдена существующая конечная точка: to_lat=%.6f, to_lng=%.6f, finish='%s'",
                        existingToLat, existingToLng, existingFinish));
            } else {
                Logger.d(context, TAG, "Запись в таблице не найдена, будет создана новая");
            }
            cursor.close();

            // Обновляем только стартовую точку, сохраняя существующую конечную
            if (address.equals(existingFinish)) {
                existingFinish = "";
            }
            if (isCityOnlyFinishInDatabase(existingFinish)) {
                existingFinish = "";
                existingToLat = newLat;
                existingToLng = newLon;
            }
            ContentValues values = new ContentValues();
            values.put("startLat", newLat);
            values.put("startLan", newLon);
            values.put("start", address);
            values.put("to_lat", existingToLat);  // Сохраняем старую конечную точку
            values.put("to_lng", existingToLng);  // Сохраняем старую конечную точку
            values.put("finish", existingFinish);  // Сохраняем старый адрес назначения

            Logger.d(context, TAG, "Подготовлены значения для обновления:");
            Logger.d(context, TAG, "  startLat=" + newLat);
            Logger.d(context, TAG, "  startLan=" + newLon);
            Logger.d(context, TAG, "  start='" + address + "'");
            Logger.d(context, TAG, "  to_lat=" + existingToLat + " (сохранено)");
            Logger.d(context, TAG, "  to_lng=" + existingToLng + " (сохранено)");
            Logger.d(context, TAG, "  finish='" + existingFinish + "' (сохранено)");

            int rowsUpdated = database.update(MainActivity.ROUT_MARKER, values, "id = ?", new String[]{"1"});
            Logger.d(context, TAG, "Результат обновления: rowsUpdated=" + rowsUpdated);

            if (rowsUpdated == 0) {
                // Если записи нет - создаем
                Logger.d(context, TAG, "Запись не найдена (rowsUpdated=0), создаем новую запись...");
                long newId = database.insert(MainActivity.ROUT_MARKER, null, values);
                Logger.d(context, TAG, "Создана новая запись с id=" + newId);
            } else {
                Logger.d(context, TAG, "Существующая запись успешно обновлена");
            }

            // Для верификации - читаем обновленные данные
            Logger.d(context, TAG, "Верификация: читаем обновленные данные из БД...");
            Cursor verifyCursor = database.rawQuery("SELECT startLat, startLan, start, to_lat, to_lng, finish FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);
            if (verifyCursor.moveToFirst()) {
                Logger.d(context, TAG, String.format(Locale.US,
                        "Проверка: startLat=%.6f, startLan=%.6f, start='%s', to_lat=%.6f, to_lng=%.6f, finish='%s'",
                        verifyCursor.getDouble(0), verifyCursor.getDouble(1), verifyCursor.getString(2),
                        verifyCursor.getDouble(3), verifyCursor.getDouble(4), verifyCursor.getString(5)));
            }
            verifyCursor.close();

            Logger.d(context, TAG, String.format(Locale.US,
                    "✅ Обновлена стартовая точка: (%.6f, %.6f) -> '%s'. Конечная точка сохранена: (%.6f, %.6f) -> '%s'",
                    newLat, newLon, address, existingToLat, existingToLng, existingFinish));

            database.close();
            Logger.d(context, TAG, "База данных закрыта");

        } catch (Exception e) {
            Logger.e(context, TAG, "❌ Ошибка обновления координат в БД: " + e.getMessage());
            Logger.e(context, TAG, "Stack trace: " + e.toString());
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        Logger.d(context, TAG, "=== updateCoordinatesInDatabase END ===");
    }

    private void updateRoutMarker(List<String> settings) {
        Logger.d(context, TAG, "updateRoutMarker: " + settings.toString());
        ContentValues cv = new ContentValues();
        String finish = settings.get(5);
        if(settings.get(4).equals(finish)) {
            finish = "";
        }
        cv.put("startLat", Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
        cv.put("start", settings.get(4));
        cv.put("finish", finish);
        if (isAdded()) {
            // обновляем по id
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                    new String[]{"1"});
            database.close();
        }
    }

    private static void updateMyPosition(Double startLat, Double startLan, String position, Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        ContentValues cv = new ContentValues();

        cv.put("startLat", startLat);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                new String[]{"1"});
        cv.put("startLan", startLan);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                new String[]{"1"});
        cv.put("position", position);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?",
                new String[]{"1"});
        database.close();

    }

    /** Есть старт и пункт назначения (координаты или текст), достаточные для расчёта. */
    private boolean isRouteReadyForCostFromDatabase() {
        if (context == null) {
            return false;
        }
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery("SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            database.close();
            return false;
        }
        double originLatitude = cursor.getDouble(cursor.getColumnIndexOrThrow("startLat"));
        double toLat = cursor.getDouble(cursor.getColumnIndexOrThrow("to_lat"));
        String start = cursor.getString(cursor.getColumnIndexOrThrow("start"));
        String finish = cursor.getString(cursor.getColumnIndexOrThrow("finish"));
        cursor.close();
        database.close();
        if (start == null) {
            start = "";
        }
        if (finish == null) {
            finish = "";
        }
        return isRouteReadyForCost(originLatitude, toLat, start, finish);
    }

    private void visicomCost() throws MalformedURLException {
        Logger.d(context, TAG, "=== visicomCost() started ===");
        restoreDestinationFieldFromDatabase();
        constr2.setVisibility(GONE);

        MainActivity.costMap = null;

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery("SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);

        if (!cursor.moveToFirst()) {
            Logger.w(context, TAG, "Маршрут не найден — cursor пуст");
            cursor.close();
            database.close();
            hideCostCalculationProgress();
            return;
        }

        double originLatitude = cursor.getDouble(cursor.getColumnIndexOrThrow("startLat"));
        double toLat = cursor.getDouble(cursor.getColumnIndexOrThrow("to_lat"));
        String start = cursor.getString(cursor.getColumnIndexOrThrow("start"));
        String finish = cursor.getString(cursor.getColumnIndexOrThrow("finish"));

        cursor.close();
        database.close();

        if (start == null) {
            start = "";
        }
        if (finish == null) {
            finish = "";
        }

        String cityCheckActivity = String.valueOf(sharedPreferencesHelperMain.getValue("CityCheckActivity", "**"));
        Logger.d(context, TAG, "cityCheckActivity = " + cityCheckActivity);
        Logger.d(context, TAG, "originLatitude = " + originLatitude + ", toLat = " + toLat + ", start = '" + start + "'");

        boolean routeReady = isRouteReadyForCost(originLatitude, toLat, start, finish);
        if (routeReady && isAroundCityRoute(start, finish)) {
            syncAroundCityRouteToUi(finish);
        }

        if ("run".equals(cityCheckActivity) && routeReady) {
            hideOrderControlsDuringCostCalculation();

            if (!CostCalculationProgressBar.isCalculationInProgress()) {
                CostPreviewHint preview = resolveCostPreviewForRecalc();
                if (preview != null) {
                    Logger.d(context, TAG, "visicomCost: превью цены перед перерасчётом");
                    showCostRecalculatingWithPreview(preview);
                } else {
                    showCostCalculationProgress();
                }
            }

            String urlCost = getTaxiUrlSearchMarkers("costSearchMarkersTimeMyApi", context);
            if ("error".equals(urlCost)) {
                Logger.w(context, TAG, "visicomCost: невалидный URL расчёта стоимости");
                hideCostCalculationProgress();
                btnVisible(VISIBLE);
                return;
            }
            requestCostFromServer(start, finish);
        } else if ("run".equals(cityCheckActivity) && originLatitude != 0.0) {
            if (!hasActiveOrderSession()) {
                normalizeRouteMarkerToAroundCityWhenNoActiveOrder();
            }
            if (isRouteReadyForCostFromDatabase()) {
                try {
                    visicomCost();
                } catch (MalformedURLException e) {
                    Logger.e(context, TAG, "visicomCost retry: " + e.getMessage());
                    hideCostCalculationProgress();
                    if (isAdded()) {
                        btnVisible(VISIBLE);
                    }
                }
                return;
            }
            if (tryApplyCachedAroundCityCost()) {
                return;
            }
            Logger.d(context, ADDR_GUARD, "visicomCost: city=run, маршрут не готов — finish='"
                    + finish + "' toLat=" + toLat);
            hideCostCalculationProgress();
            if (isAdded()) {
                btnVisible(VISIBLE);
            }
            return;
        } else {
            sharedPreferencesHelperMain.saveValue("CityCheckActivity", "**");

            try {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_anr);
            } catch (Exception e) {
                Logger.e(context, TAG, "Ошибка навигации: " + e.getMessage());
            }
        }

        Logger.d(context, TAG, "=== visicomCost() завершён ===");
    }

    private void syncAddressFieldsFromRouteMarker() {
        if (!isAdded() || binding == null || context == null) {
            return;
        }
        String uiBefore = geoText.getText() != null ? geoText.getText().toString() : "";
        List<String> route = logCursor(MainActivity.ROUT_MARKER, context);
        if (route.size() <= 5) {
            Logger.d(context, ADDR_GUARD, "syncFromDb: SKIP — ROUT_MARKER неполный, size=" + route.size());
            return;
        }
        String start = route.get(5);
        List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);
        String currentCity = cityInfo.size() > 1 ? cityInfo.get(1) : "";
        boolean canSyncStart = isValidStartAddressForFromField(start)
                && com.taxi.easy.ua.utils.city.CityLastAddressHelper.shouldApplyLastAddress(
                currentCity, route.get(1), route.get(2), start);
        if (canSyncStart) {
            if (!uiBefore.equals(start)) {
                logAddrGuardOverwrite("syncFromDb:costCallback", uiBefore, start, "актуальный ROUT_MARKER после расчёта");
            }
            geoText.setText(start);
        } else {
            clearInvalidStartInRouteMarker();
            if (!uiBefore.trim().isEmpty()) {
                logAddrGuardOverwrite("syncFromDb:clearStaleUi", uiBefore, "", "ROUT_MARKER start invalid");
                geoText.setText("");
            }
        }
        if (route.size() > 6) {
            String finish = route.get(6);
            if (finish != null && binding.textTo != null) {
                if (isCityOnlyFinishInDatabase(finish)) {
                    applyAroundCityDestinationToUiIfNeeded();
                } else {
                    binding.textTo.setText(finish);
                }
            }
        }
        Logger.d(context, ADDR_GUARD, String.format(Locale.US,
                "syncFromDb: uiStart='%s' dbStart='%s' gpsApplied=%s",
                geoText.getText(), start, AutoLocationAfterCityHelper.isGpsStartApplied()));
    }

    private void requestCostFromServer(String start, String finish) throws MalformedURLException {
        String urlCost = getTaxiUrlSearchMarkers("costSearchMarkersTimeMyApi", context);
        reserveCost(start, finish, urlCost);

        if (costHandler == null) costHandler = new Handler(Looper.getMainLooper());
        reserveRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Logger.d(context, TAG, "Таймер сработал: вызываем reserveCost");
                    reserveCost(start, finish, urlCost);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        };

        orderViewModel.getOrderCost().observe(getViewLifecycleOwner(), cost -> {
            if (cost != null && cost.equals(lastCost)) {
                return;
            }
            lastCost = cost;

            Logger.d(context, TAG, "Updated order_cost: " + cost);

            if (!hasDisplayableCost(cost)) {
                showCostCalculationProgress();
                if (costHandler != null && reserveRunnable != null) {
                    costHandler.postDelayed(reserveRunnable, 10_000);
                }
                return;
            }

            if (costHandler != null && reserveRunnable != null) {
                costHandler.removeCallbacks(reserveRunnable);
            }

            if (binding != null) {
                Logger.d(context, ADDR_GUARD, "costObserver: sync UI from ROUT_MARKER after cost=" + cost);
                syncAddressFieldsFromRouteMarker();
                applyDiscountAndUpdateUI(cost, context);
            }
        });

    }


    private void reserveCost(String start, String finish, String urlCost) throws MalformedURLException {
        if (costHandler != null && reserveRunnable != null) {
            costHandler.removeCallbacks(reserveRunnable);
        }

        // здесь твоя логика запроса
        Logger.d(context, TAG, "reserveCost вызван -> start: " + start + ", finish: " + finish);
        Logger.d(context, ADDR_GUARD, String.format(Locale.US,
                "reserveCost: capturedStart='%s' capturedFinish='%s' gpsApplied=%s",
                start, finish, AutoLocationAfterCityHelper.isGpsStartApplied()));

        Logger.d(context, TAG, "Попытка #" + ( 1) + ", URL: " + urlCost);

        CostJSONParserRetrofit parser = new CostJSONParserRetrofit();
        parser.sendURL(urlCost, new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded() || binding == null) {
                        Logger.w(context, TAG, "Фрагмент отсоединён или binding null — выходим");
                        return;
                    }

                    syncAddressFieldsFromRouteMarker();

                    Map<String, String> map = response.body();
                    String cost;
                    if (map != null && hasDisplayableCost(map.get("order_cost"))) {
                        cost = map.get("order_cost");
                        applyDiscountAndUpdateUI(cost, context);
                    } else if (map != null && isTerminalCostMessage(map.get("Message"))) {
                        Logger.d(context, TAG, "reserveCost: ошибка расчёта, Message=" + map.get("Message"));
                        if (!(isAroundCityRoute(start, finish) && tryApplyCachedAroundCityCost())) {
                            showCostCalculationError(map.get("Message"));
                        }
                    } else if (isAroundCityRoute(start, finish) && tryApplyCachedAroundCityCost()) {
                        Logger.d(context, TAG, "reserveCost: сервер без цены — кэш по городу");
                    } else {
                        showCostCalculationProgress();
                        if (map != null) {
                            Logger.d(context, TAG, "reserveCost: цена ещё не готова, Message="
                                    + map.get("Message") + ", order_cost=" + map.get("order_cost"));
                        }
                    }


                });
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.e(context, TAG, "Ошибка подключения к серверу: " + t.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!isAdded() || binding == null) {
                        return;
                    }
                    if (isAroundCityRoute(start, finish) && tryApplyCachedAroundCityCost()) {
                        Logger.d(context, TAG, "reserveCost: сеть — кэш по городу");
                        return;
                    }
                    showCostCalculationError("Ошибка подключения: " + t.getLocalizedMessage());
                });
            }
        });
    }
    private void clearTABLE_SERVICE_INFO () {
        String[] arrayServiceCode = DataArr.arrayServiceCode();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        for (int i = 0; i < arrayServiceCode.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put(arrayServiceCode[i], "0");
            database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });
        }
        database.close();
    }

//    private String lastAppliedCost = null;

    private void applyDiscountAndUpdateUI(String orderCost, Context context) {
        applyDiscountAndUpdateUI(orderCost, context, true);
    }

    private void applyDiscountAndUpdateUI(String orderCost, Context context, boolean finalizeUi) {
        Logger.d(context, TAG, "applyDiscountAndUpdateUI() start — orderCost = " + orderCost);
        double costValue = Double.parseDouble(orderCost);
        orderCost = String.valueOf(Math.round(costValue));

        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
        Logger.d(context, TAG, "Retrieved discountText = " + discountText);

        if (discountText == null || !(discountText.matches("[+-]?\\d+") || discountText.equals("0"))) {
            Logger.w(context, TAG, "Invalid or missing discountText: " + discountText);
            return;
        }

        try {
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            Cursor cursor = database.rawQuery("SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);

            if (!cursor.moveToFirst()) {
                Logger.w(context, TAG, "Маршрут не найден — cursor пуст");
                cursor.close();
                database.close();
                return;
            }

            String finish = cursor.getString(cursor.getColumnIndexOrThrow("finish"));
            Logger.d(context, TAG, "Retrieved finish = " + finish);

            cursor.close();
            database.close();
            long discountInt = Long.parseLong(discountText);
            long discount;

            firstCost = Long.parseLong(orderCost);
            if (firstCost != 0) {
                if ((boolean) sharedPreferencesHelperMain.getValue("verifyUserOrder", false)) {
                    firstCost = firstCost + 45;
                }
            }
            discount = firstCost * discountInt / 100;
            firstCost = firstCost + discount;

            Logger.d(context, TAG, "Calculated firstCost = " + firstCost + ", discount = " + discount);

            updateAddCost(String.valueOf(discount), context);
            text_view_cost.setText(String.valueOf(firstCost));
            text_view_cost.setAlpha(1f);

            if (finalizeUi) {
                startCost = firstCost;
                finalCost = firstCost;
                MIN_COST_VALUE = (long) (firstCost * 0.6);
                firstCostForMin = firstCost;

                Logger.d(context, TAG, "Setting UI visibility and values");

                if (!isCityOnlyFinishInDatabase(finish)) {
                    textViewTo.setText(finish);
                }

                finishCostCalculationWithPrice();
            }

        } catch (NumberFormatException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Logger.e(context, TAG, "NumberFormatException в applyDiscountAndUpdateUI: " + e.getMessage());
            showCostCalculationProgress();
        }

        if (finalizeUi) {
            btnVisible(View.VISIBLE);
        }
    }


    private void blockUserBlackList() {
        // Log the start of the block process
        Logger.d(context,"blockUserBlackList", "Starting the block process for user.");

        // Update button text and make it non-clickable
//        buttonBonus.setText(context.getString(R.string.card_payment));
//        buttonBonus.setClickable(false);
//        buttonBonus.setOnClickListener(v -> {
//            String message = context.getString(R.string.black_list_message_err);
//            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
//            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
//
//        });

        Logger.d(context,"blockUserBlackList", "Button text set and made non-clickable.");

        // Retrieve email from the database
        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        Logger.d(context,"blockUserBlackList", "Retrieved email from database: " + email);

        // Add email to the blacklist
        BlacklistManager blacklistManager = new BlacklistManager();
        blacklistManager.addToBlacklist(email);
        Logger.d(context,"blockUserBlackList", "Request to add email to blacklist sent: " + email);


    }

    private void fetchRoutesCancel() {
        Logger.d(context, TAG, "fetchRoutesCancel: ");
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        if (!userEmail.equals("email")) {
            databaseHelper.clearTable();

            databaseHelperUid.clearTableUid();

            routeListCancel = new ArrayList<>();

//            String baseUrl = "https://m.easy-order-taxi.site";

            List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
            String city = stringList.get(1);
            baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
            String url = baseUrl + "/android/UIDStatusShowEmailCancelApp/" + userEmail + "/" + city + "/" + context.getString(R.string.application);

            Call<List<RouteResponseCancel>> call = ApiClient.getApiService().getRoutesCancel(url);
            Logger.d(context, TAG, "fetchRoutesCancel: " + url);
            call.enqueue(new Callback<List<RouteResponseCancel>>() {
                @Override
                public void onResponse(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Response<List<RouteResponseCancel>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<RouteResponseCancel> routes = response.body();
                        Logger.d(context, TAG, "onResponse: " + routes);
                        if (routes.size() == 1) {
                            RouteResponseCancel route = routes.get(0);
                            if ("*".equals(route.getRouteFrom()) && "*".equals(route.getRouteFromNumber()) &&
                                    "*".equals(route.getRouteTo()) && "*".equals(route.getRouteToNumber()) &&
                                    "*".equals(route.getWebCost()) && "*".equals(route.getCloseReason()) &&
                                    "*".equals(route.getAuto()) && "*".equals(route.getCreatedAt())) {
                                databaseHelper.clearTableCancel();
                                databaseHelperUid.clearTableCancel();
                                return;
                            }
                        }
                        if (!routes.isEmpty()) {
                            boolean hasRouteWithAsterisk = false;
                            for (RouteResponseCancel route : routes) {
                                if ("*".equals(route.getRouteFrom())) {
                                    // Найден объект с routefrom = "*"
                                    hasRouteWithAsterisk = true;
                                    break;  // Выход из цикла, так как условие уже выполнено
                                }
                            }
                            if (!hasRouteWithAsterisk) {
                                if (routeListCancel == null) {
                                    routeListCancel = new ArrayList<>();
                                }
                                routeListCancel.addAll(routes);
                                processCancelList();
                            }

                        }
                    }
                }

                public void onFailure(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Throwable t) {
                    // Обработка ошибок сети или других ошибок
                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });
        }

    }

    private void processCancelList() {
        if (routeListCancel == null || routeListCancel.isEmpty()) {
            Logger.d(context, TAG, "routeListCancel is null or empty");
            return;
        }

        // Создайте массив строк

        databaseHelper.clearTableCancel();
        databaseHelperUid.clearTableCancel();

        for (int i = 0; i < routeListCancel.size(); i++) {
            RouteResponseCancel route = routeListCancel.get(i);
            String uid = route.getUid();
            String routeFrom = route.getRouteFrom();
            String routefromnumber = route.getRouteFromNumber();
            String routeTo = route.getRouteTo();
            String routeTonumber = route.getRouteToNumber();
            String webCost = route.getWebCost();
            String createdAt = OrderCreatedAtDisplayHelper.formatForDisplay(route.getCreatedAt());
            String closeReason = route.getCloseReason();
            String auto = route.getAuto();
            String dispatchingOrderUidDouble = route.getDispatchingOrderUidDouble();
            String pay_method = route.getPay_method();
            String required_time = route.getRequired_time();
            String flexible_tariff_name = route.getFlexible_tariff_name();
            String comment_info = route.getComment_info();
            String extra_charge_codes = route.getExtra_charge_codes();

            String closeReasonText = OrderHistoryStatusHelper.resolveStatusText(
                    context,
                    closeReason,
                    route.getExecution_status(),
                    required_time,
                    uid
            );

            if (routeFrom.equals("Місце відправлення")) {
                routeFrom = context.getString(R.string.start_point_text);
            }

            if (routeTo.equals("Точка на карте")) {
                routeTo = context.getString(R.string.end_point_marker);
            }
            if (routeTo.contains("по городу")) {
                routeTo = context.getString(R.string.on_city);
            }
            if (routeTo.contains("по місту")) {
                routeTo = context.getString(R.string.on_city);
            }
            String routeInfo;

            if (auto == null) {
                auto = "??";
            }
            String routeHead;
            if (routeFrom.equals(routeTo)) {
                routeHead = routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to)
                        + context.getString(R.string.on_city);
            } else {
                routeHead = routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to) + routeTo + " " + routeTonumber + ".";
            }
            routeInfo = RequiredTimeParseHelper.buildCancelListRouteInfo(
                    context,
                    routeHead,
                    webCost,
                    auto,
                    createdAt,
                    route.getRequired_time(),
                    closeReasonText
            );

            databaseHelper.addRouteCancel(uid, routeInfo);
            List<String> settings = new ArrayList<>();

            settings.add(uid);
            settings.add(webCost);
            settings.add(routeFrom);
            settings.add(routefromnumber);
            settings.add(routeTo);
            settings.add(routeTonumber);
            settings.add(dispatchingOrderUidDouble);
            settings.add(pay_method);
            settings.add(RequiredTimeParseHelper.formatForStorage(required_time));
            settings.add(flexible_tariff_name);
            settings.add(comment_info);
            settings.add(extra_charge_codes);

            Logger.d(context, TAG, settings.toString());
            databaseHelperUid.addCancelInfoUid(settings);
        }

        String[] array = databaseHelper.readRouteCancel();
        Logger.d(context, TAG, "processRouteList: array " + Arrays.toString(array));
        if (array != null && array.length > 0) {
            NavController navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
            int currentDestination = navController.getCurrentDestination().getId();

            if (currentDestination == R.id.nav_visicom
                    && !ExecutionStatusViewModel.shouldSuppressActiveOrderNotice()) {
                MyBottomSheetErrorFragment.showScheduledTripsNotice(fragmentManager, context);
            }

        } else {
            databaseHelper.clearTableCancel();
            databaseHelperUid.clearTableCancel();
        }
    }

    private static boolean isDuplicateOrderMessage(@Nullable String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        return message.contains("Дублирование") || "DuplicateActiveOrder".equals(message);
    }

    private boolean hasActiveOrderSession() {
        if (MainActivity.uid != null && !MainActivity.uid.isEmpty()) {
            return true;
        }
        Object earlyUid = sharedPreferencesHelperMain.getValue("order_early_nav_uid", "");
        if (earlyUid instanceof String && !((String) earlyUid).isEmpty()) {
            return true;
        }
        String persisted = ExecutionStatusViewModel.getPersistedActiveUid();
        return persisted != null && !persisted.isEmpty();
    }

    private void showAddCostDoubleDialog(String addType) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        int dialogViewInt = R.layout.dialog_add_cost;

        if ("60".equals(addType)) {
            dialogViewInt = R.layout.dialog_add_60_cost;
        }

        View dialogView = inflater.inflate(dialogViewInt, null);

        // Заголовок и сообщение
        String title = context.getString(R.string.double_order);
        String message = context.getString(R.string.add_cost_fin_60);
        String numberIndexString = "";

        // Инициализация элементов для типа "60"
        if ("60".equals(addType)) {
            title = context.getString(R.string.double_order);

            AppCompatButton minus = dialogView.findViewById(R.id.btn_minus);
            AppCompatButton plus = dialogView.findViewById(R.id.btn_plus);
            EditText discinp = dialogView.findViewById(R.id.discinp);

            minus.setOnClickListener(v -> {
                String addCost = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(5);
                int addCostInt = Integer.parseInt(addCost);
                if (addCostInt >= 5) {
                    addCostInt -= 5;
                    updateAddCost(String.valueOf(addCostInt), context);
                    discinp.setText(String.valueOf(addCostInt + 60));
                }
            });

            plus.setOnClickListener(v -> {
                String addCost = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(5);
                int addCostInt = Integer.parseInt(addCost);
                addCostInt += 5;
                updateAddCost(String.valueOf(addCostInt), context);
                discinp.setText(String.valueOf(addCostInt + 60));
            });

            message = context.getString(R.string.add_cost_fin_60);
            numberIndexString = message;
        } else if ("45".equals(addType)) {
            title = context.getString(R.string.black_list);
            message = context.getString(R.string.add_cost_fin_45);
            numberIndexString = "45";
            blockUserBlackList();
        }

        // Установка заголовка и сообщения
        TextView titleView = dialogView.findViewById(R.id.dialogTitle);
        titleView.setText(title);

        TextView messageView = dialogView.findViewById(R.id.dialogMessage);
        SpannableStringBuilder spannable = new SpannableStringBuilder(message);
        int numberIndex = message.indexOf(numberIndexString);
        if (numberIndex != -1) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), numberIndex, numberIndex + numberIndexString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        messageView.setText(spannable);

        // Обработка кнопок
        builder.setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                    dialog.dismiss();

                    if ("60".equals(addType)) {
                        createDoubleOrder();

                    } else if ("45".equals(addType)) {
                        checkCardPaymentForCity();
//                        createBlackList();
                    }
                })
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> {
                    if ("60".equals(addType)) {
                        sharedPreferencesHelperMain.saveValue("doubleOrderPref", false);
                    }
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
    private void checkCardPaymentForCity() {
        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String cityName = stringList.get(1);
        FirestoreHelper firestoreHelper = new FirestoreHelper(context);
        firestoreHelper.getCardPaymentKeyForCity(
                new FirestoreHelper.OnCardPaymentKeyFetchedListener() {
                    @Override
                    public void onSuccess(Boolean cardPaymentEnabled) {
                        Logger.d(context, TAG, "Успешно получено значение: " + cardPaymentEnabled);

                        if (cardPaymentEnabled) {
                            Logger.d(context, TAG, "Оплата картой ДОСТУПНА для города " + cityName);
                            createBlackList();
                        } else {
                            Logger.d(context, TAG, "Оплата картой НЕДОСТУПНА для города " + cityName);
                            String message = context.getString(R.string.card_payment_false);
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Logger.e(context,TAG, "Ошибка получения настроек: " + e.getMessage());

                        // Показываем ошибку пользователю
                        Toast.makeText(context,
                                "Ошибка загрузки настроек: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                },
                cityName
        );
    }

    private void createBlackList() {
        sharedPreferencesHelperMain.saveValue("black_list_45", true);
        pay_method = "wfp_payment";
        ContentValues cv = new ContentValues();
        cv.put("payment_type", pay_method);
        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();

        orderRout();

        googleVerifyAccount();

    }

    public void createDoubleOrder() {

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);

        String addCost = stringListInfo.get(5);
        int addCostInt = Integer.parseInt(addCost);


        addCostInt += 60;
        addCost = String.valueOf(addCostInt);
        updateAddCost(addCost, context);

        orderRout();

        googleVerifyAccount();

    }

    private void startGooglePayHoldBeforeOrder() {
        if (!isAdded() || googlePayOrderHoldInProgress) {
            return;
        }
        if (MainActivity.order_id == null || MainActivity.order_id.isEmpty()) {
            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
            if (!orderRout()) {
                btnVisible(VISIBLE);
                return;
            }
        }
        String costText = resolveOrderDisplayCostForSubmit();
        if (costText == null && text_view_cost != null && text_view_cost.getText() != null) {
            costText = text_view_cost.getText().toString().trim();
        }
        int amountUah = GooglePayOrderHelper.parseAmountUah(costText != null ? costText : "");
        if (amountUah <= 0) {
            Toast.makeText(context, R.string.cost_error, Toast.LENGTH_SHORT).show();
            btnVisible(VISIBLE);
            return;
        }
        pendingGooglePayAmount = String.valueOf(amountUah);
        pendingGooglePayOrderReference = MainActivity.order_id;
        progressBar.setVisibility(VISIBLE);
        googlePayOrderHoldInProgress = true;

        WfpGooglePayHelper.checkReady(googlePayPaymentsClient, ready -> {
            if (!isAdded()) {
                return;
            }
            if (!ready) {
                onGooglePayOrderHoldFailed(context.getString(R.string.google_pay_unavailable));
                return;
            }
            List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);
            String city = cityInfo.size() > 1 ? cityInfo.get(1) : "";
            String appBaseUrl = (String) sharedPreferencesHelperMain.getValue(
                    "baseUrl", "https://m.easy-order-taxi.site");
            GooglePayOrderHelper.fetchMerchantConfig(
                    appBaseUrl,
                    context.getString(R.string.application),
                    city,
                    new GooglePayOrderHelper.ConfigCallback() {
                        @Override
                        public void onSuccess(@NonNull String merchantAccount) {
                            if (!isAdded()) {
                                return;
                            }
                            pendingGooglePayMerchant = merchantAccount;
                            WfpGooglePayHelper.requestPayment(
                                    VisicomFragment.this,
                                    googlePayPaymentsClient,
                                    merchantAccount,
                                    pendingGooglePayAmount,
                                    googlePayLauncher,
                                    new WfpGooglePayHelper.PaymentResultCallback() {
                                        @Override
                                        public void onSuccess(@NonNull String paymentDataJson) {
                                            submitGooglePayHoldCharge(paymentDataJson);
                                        }

                                        @Override
                                        public void onCancelled() {
                                            onGooglePayOrderHoldCancelled();
                                        }

                                        @Override
                                        public void onError(@NonNull String message) {
                                            onGooglePayOrderHoldFailed(message);
                                        }
                                    }
                            );
                        }

                        @Override
                        public void onError(@NonNull String message) {
                            onGooglePayOrderHoldFailed(message);
                        }
                    }
            );
        });
    }

    private void submitGooglePayHoldCharge(@NonNull String paymentDataJson) {
        if (!isAdded() || pendingGooglePayOrderReference == null) {
            return;
        }
        List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);
        String city = cityInfo.size() > 1 ? cityInfo.get(1) : "";
        List<String> userInfo = logCursor(MainActivity.TABLE_USER_INFO, context);
        String email = userInfo.size() > 3 ? userInfo.get(3) : "";
        String phone = userInfo.size() > 2 ? userInfo.get(2) : "";
        int amountUah = GooglePayOrderHelper.parseAmountUah(
                pendingGooglePayAmount != null ? pendingGooglePayAmount : "0");
        String appBaseUrl = (String) sharedPreferencesHelperMain.getValue(
                "baseUrl", "https://m.easy-order-taxi.site");

        GooglePayOrderHelper.submitHoldCharge(
                context,
                appBaseUrl,
                context.getString(R.string.application),
                city,
                pendingGooglePayOrderReference,
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
                        googlePayOrderHoldInProgress = false;
                        progressBar.setVisibility(GONE);
                        MainActivity.order_id = orderReference;
                        try {
                            orderFinished();
                        } catch (MalformedURLException e) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                            onGooglePayOrderHoldFailed(e.getMessage() != null
                                    ? e.getMessage() : "order_error");
                        }
                    }

                    @Override
                    public void onHoldFailed(@NonNull String message) {
                        onGooglePayOrderHoldFailed(message);
                    }
                }
        );
    }

    private void onGooglePayOrderHoldCancelled() {
        googlePayOrderHoldInProgress = false;
        pendingOrderDisplayCost = null;
        progressBar.setVisibility(GONE);
        btnVisible(VISIBLE);
        Toast.makeText(context, R.string.e_google_pay_canceled, Toast.LENGTH_SHORT).show();
    }

    private void onGooglePayOrderHoldFailed(@Nullable String message) {
        googlePayOrderHoldInProgress = false;
        pendingOrderDisplayCost = null;
        progressBar.setVisibility(GONE);
        btnVisible(VISIBLE);
        MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
        EarlyOrderNavigationHelper.clearSubmitState();
        Logger.w(context, TAG, "Google Pay hold failed: " + message);
        if (!isAdded() || fragmentManager == null) {
            return;
        }
        MyBottomSheetErrorFragment bottomSheet = new MyBottomSheetErrorFragment(
                getString(R.string.google_pay_hold_failed_message));
        bottomSheet.show(fragmentManager, "GooglePayHoldFailed");
    }

    private void googleVerifyAccount() {

        FirebaseConsentManager consentManager = new FirebaseConsentManager(context);

        consentManager.checkUserConsent(new FirebaseConsentManager.ConsentCallback() {
            @Override
            public void onConsentValid() {
                Logger.d(context, TAG, "Согласие пользователя действительное.");
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        if(!verifyOrder()) {
                            if ("google_pay_payment".equals(pay_method)) {
                                startGooglePayHoldBeforeOrder();
                            } else {
                                orderFinished();
                            }
                        } else {
                            if (pay_method.equals("wfp_payment")) {
                                String rectoken = getCheckRectoken(context);
                                Logger.d(context, TAG, "payWfp: rectoken " + rectoken);
                                if (rectoken.isEmpty()) {
                                    String message = context.getString(R.string.no_cards_info);
                                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                                    bottomSheetDialogFragment.show(getParentFragmentManager(), bottomSheetDialogFragment.getTag());

                                } else {
                                    orderFinished();
                                }

                            } else {
                                String message = context.getString(R.string.black_list_message_err);
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

                                btnVisible(VISIBLE);
                            }
                        }

                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            @Override
            public void onConsentInvalid() {
                Logger.d(context, TAG, "Согласие пользователя НЕ действительное.");
                String message = getString(R.string.google_verify_mes);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });
    }

    private void startTilePreloadWorker() {
        try {
            OneTimeWorkRequest tilePreloadRequest = new OneTimeWorkRequest.Builder(TilePreloadWorker.class)
                    .addTag("TilePreloadWork")
                    .build();
            WorkManager.getInstance(requireContext()).enqueue(tilePreloadRequest);
            Logger.d(requireContext(), TAG, "TilePreloadWorker enqueued");
        } catch (Exception e) {
            Logger.e(requireContext(), TAG, "Error enqueuing TilePreloadWorker: " + e.getMessage());
        }
    }
    private void updateRouteSettings() {

        List<String> settings = new ArrayList<>();
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database =context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            // Убедимся, что таблица существует с правильной схемой
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MainActivity.ROUT_MARKER + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "startLat REAL, startLan REAL, to_lat REAL, to_lng REAL, " +
                    "start TEXT, finish TEXT)");

            // Получение текущих данных
            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
            cursor = database.rawQuery(query, null);
            double currentFromLatitude = 0.0;
            double currentFromLongitude = 0.0;
            String currentStartAddress = "";

            double currentToLatitude = 0.0;
            double currentToLongitude = 0.0;
            String currentFinishAddress = "";

            if (cursor.moveToFirst()) {
                int fromLatIndex = cursor.getColumnIndex("startLat");
                int fromLngIndex = cursor.getColumnIndex("startLan");
                int startIndex = cursor.getColumnIndex("start");

                int toLatIndex = cursor.getColumnIndex("to_lat");
                int toLngIndex = cursor.getColumnIndex("to_lng");
                int finishIndex = cursor.getColumnIndex("finish");

                if (fromLatIndex != -1) {
                    currentFromLatitude = cursor.getDouble(fromLatIndex);
                } else {
                    Logger.i(context, TAG, "from_lat column not found in ROUT_MARKER");
                }
                if (fromLngIndex != -1) {
                    currentFromLongitude = cursor.getDouble(fromLngIndex);
                } else {
                    Logger.i(context, TAG, "from_lng column not found in ROUT_MARKER");
                }
                if (startIndex != -1) {
                    currentStartAddress = cursor.getString(startIndex) != null ? cursor.getString(startIndex) : "";
                } else {
                    Logger.i(context, TAG, "start column not found in ROUT_MARKER");
                }



                if (toLatIndex != -1) {
                    currentToLatitude = cursor.getDouble(toLatIndex);
                } else {
                    Logger.i(context, TAG, "to_lat column not found in ROUT_MARKER");
                }
                if (toLngIndex != -1) {
                    currentToLongitude = cursor.getDouble(toLngIndex);
                } else {
                    Logger.i(context, TAG, "to_lng column not found in ROUT_MARKER");
                }
                if (finishIndex != -1) {
                    currentFinishAddress = cursor.getString(finishIndex) != null ? cursor.getString(finishIndex) : "";
                } else {
                    Logger.i(context, TAG, "finish column not found in ROUT_MARKER");
                }
                Logger.d(context, TAG, "Current data: toLatitude=" + currentToLatitude + ", toLongitude=" + currentToLongitude + ", finishAddress=" + currentFinishAddress);
            }

            // Подготовка новых данных

            settings.add(String.valueOf(currentFromLatitude));
            settings.add(String.valueOf(currentFromLongitude));
            settings.add(String.valueOf(currentFromLatitude));
            settings.add(String.valueOf(currentFromLongitude));
            settings.add(currentStartAddress);
            settings.add("");
            Logger.d(context, TAG, "New settings for route finish point update: " + settings);

            String finish = settings.get(5);
            if(settings.get(4).equals(finish)) {
                finish = "";
            }
            // Обновление таблицы
            ContentValues values = new ContentValues();
            values.put("startLat", Double.parseDouble(settings.get(0)));
            values.put("startLan", Double.parseDouble(settings.get(1)));
            values.put("to_lat", Double.parseDouble(settings.get(2)));
            values.put("to_lng", Double.parseDouble(settings.get(3)));
            values.put("start", settings.get(4));
            values.put("finish", finish);

            int rowsUpdated = database.update(MainActivity.ROUT_MARKER, values, null, null);
            if (rowsUpdated == 0) {
                database.insert(MainActivity.ROUT_MARKER, null, values);
                Logger.d(context, TAG, "Inserted new route marker data");
            } else {
                Logger.d(context, TAG, "Updated route marker data, rows affected: " + rowsUpdated);
            }
            // Обновление маршрута и позиции

            updateRoutMarker(settings);

            updateMyPosition(currentFromLatitude, currentFromLongitude, currentStartAddress, context);
            Logger.d(context, TAG, "Updated route settings and position");

        } catch (Exception e) {
            Logger.e(context, TAG, "Failed to update ROUT_MARKER: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }



    }

    private void statusOrder() throws ParseException {
        String uid = (String) sharedPreferencesHelperMain.getValue("uid_fcm", "");
        uid = uid != null ? uid.trim() : "";
        if (uid.isEmpty()) {
            String persisted = ExecutionStatusViewModel.getPersistedActiveUid();
            if (persisted != null && !persisted.trim().isEmpty()) {
                uid = persisted.trim();
                sharedPreferencesHelperMain.saveValue("uid_fcm", uid);
                Logger.d(context, TAG, "statusOrder: restored uid_fcm from persisted active order");
            }
        }
        Logger.d(context, TAG, "statusOrder: " + uid);

        new Thread(this::fetchRoutesCancel).start();

        // Если uid нет, то и "основного заказа" нет — не шлем запрос с пробелом в URL.
        if (uid.isEmpty()) {
            Logger.d(context, TAG, "statusOrder: uid_fcm empty — skip request");
            if (isAdded()) {
                normalizeRouteMarkerToAroundCityWhenNoActiveOrder();
            }
            return;
        }

        Logger.d(context, "Pusher", "statusCacheOrder: " + uid);

        List<String> listCity = logCursor(CITY_INFO, context);
        if (listCity.size() < 3) {
            Logger.w(context, TAG, "statusOrder: CITY_INFO is empty — skip request");
            return;
        }
        String api = listCity.get(2);

        String baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        String url = baseUrl  + api + "/android/searchAutoOrderService/" + uid +"/no_mes";

        Call<OrderServiceResponse> call = ApiClient.getApiService().searchAutoOrderService(url);
        Logger.d(context, TAG, "statusOrder url: " + url);

        // Выполняем запрос асинхронно
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<OrderServiceResponse> call, @NonNull Response<OrderServiceResponse> response) {

                Logger.i(context, TAG, "Response received: HTTP " + response.code() + ", Success: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    OrderServiceResponse orderResponse = response.body();
                    Logger.d(context, TAG, "Response body: status=" + orderResponse.getStatus() + ", message=" + orderResponse.getMessage());

                    // Проверяем, что статус success и сообщение содержит информацию об автомобиле
                    if ("success".equals(orderResponse.getStatus()) && orderResponse.getMessage() != null) {
                        String message = orderResponse.getMessage();
                        // Проверяем, что сообщение не "Заказ снят" или "Заказ не найден"
                        if (message.equals("Заказ снят")
                                || message.equals("Заказ не найден")
                                || message.equals("Автоматический заказ не найден")) {
                            sharedPreferencesHelperMain.saveValue("uid_fcm", "");
                            if (isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        normalizeRouteMarkerToAroundCityWhenNoActiveOrder());
                            }
                        }
                    }
                } else {
                    Logger.w(context, TAG, "Unsuccessful response or no body: HTTP " + response.code());
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Logger.w(context, TAG, "Error body: " + errorBody);
                        } catch (IOException e) {
                            Logger.e(context, TAG, "Failed to read error body: " + e.getMessage()+ e);
                            FirebaseCrashlytics.getInstance().recordException(e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<OrderServiceResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }
}
