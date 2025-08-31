package com.taxi.easy.ua.ui.visicom;


import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.CITY_INFO;
import static com.taxi.easy.ua.MainActivity.activeCalls;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.MainActivity.navController;
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
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.FragmentVisicomBinding;
import com.taxi.easy.ua.service.OrderServiceResponse;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;
import com.taxi.easy.ua.ui.finish.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.utils.animation.car.CarProgressBar;
import com.taxi.easy.ua.utils.auth.FirebaseConsentManager;
import com.taxi.easy.ua.utils.blacklist.BlacklistManager;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetBonusFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyPhoneDialogFragment;
import com.taxi.easy.ua.utils.city.CityFinder;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.download.AppUpdater;
import com.taxi.easy.ua.utils.from_json_parser.FromJSONParserRetrofit;
import com.taxi.easy.ua.utils.ip.RetrofitClient;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.retrofit.cost_json_parser.CostJSONParserRetrofit;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VisicomFragment extends Fragment {


    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;

    private FragmentVisicomBinding binding;
    private static final String TAG = "VisicomFragment";
 

    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btn_minus, btn_plus, btnOrder, buttonBonus, gpsBtn, btnCallAdmin, btnCallAdminFin;
    @SuppressLint("StaticFieldLeak")
    public static TextView geoText;
    static String api;

    public static long firstCost;

    @SuppressLint("StaticFieldLeak")
    public static TextView text_view_cost;
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
    private boolean location_update;
    LocationManager locationManager;
    public static int currentNavDestination = -1; // ID текущего экрана



    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVisicomBinding.inflate(inflater, container, false);
        setupImages();
        UXCam.tagScreenName(TAG);

        View root = binding.getRoot();
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // result — Map<String, Boolean>, где ключ — имя разрешения, значение — результат (granted or not)
                    for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                        String permission = entry.getKey();
                        boolean granted = entry.getValue();

                        // Сохраняем результат
                        sharedPreferencesHelperMain.saveValue(permission, granted ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED);
                    }

                    // Можно обновить счетчик запросов разрешений, если нужно
                    int permissionRequestCount = loadPermissionRequestCount();
                    permissionRequestCount++;
                    savePermissionRequestCount(permissionRequestCount);
                    Log.d("loadPermission", "permissionRequestCount: " + permissionRequestCount);
                }
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

        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        svButton = root.findViewById(R.id.sv_button);

// Устанавливаем слушатель для распознавания жеста свайпа вниз
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Скрываем TextView (⬇️) сразу после появления индикатора свайпа
            clearTABLE_SERVICE_INFO();
            sharedPreferencesHelperMain.saveValue("time", "no_time");
            sharedPreferencesHelperMain.saveValue("date", "no_date");
            sharedPreferencesHelperMain.saveValue("comment", "no_comment");
            sharedPreferencesHelperMain.saveValue("tarif", " ");
            sharedPreferencesHelperMain.saveValue("setStatusX", true);
            svButton.setVisibility(GONE);

            // Выполняем необходимое действие (например, запуск новой активности)
            startActivity(new Intent(context, MainActivity.class));

            // Эмулируем окончание обновления с задержкой
            swipeRefreshLayout.postDelayed(() -> {
                // Отключаем индикатор загрузки
                swipeRefreshLayout.setRefreshing(false);

                // Показываем TextView (⬇️) снова после завершения обновления
                svButton.setVisibility(VISIBLE);
            }, 500); // Задержка 500 мс
        });


        fragmentManager = getParentFragmentManager();
        progressBar = binding.progressBar;
        linearLayout = binding.linearLayoutButtons;


        btnCallAdmin = binding.btnCallAdmin;
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
            String phone = stringList.get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });
        btnCallAdminFin = binding.btnCallAdminFin;
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
            String phone = stringList.get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
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

        constr2.setVisibility(View.INVISIBLE);

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

        return root;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("LifecycleCheck 1", "Current lifecycle state: " + getViewLifecycleOwner().getLifecycle().getCurrentState());
        setupActionBar();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        viewModel = new ViewModelProvider(requireActivity()).get(ExecutionStatusViewModel.class);

        if(button1 != null) {
          button1.setVisibility(View.VISIBLE);
        }



        viewModel.getStatusX().observe(getViewLifecycleOwner(), aBoolean -> {
            Logger.d(context, TAG,"StatusXUpdate changed: " + aBoolean);
            updateGpsButtonDrawable(aBoolean);
        });

        viewModel.getStatusGpsUpdate().observe(getViewLifecycleOwner(), aBoolean -> {
            Logger.d(context, TAG,"StatusGpsUpdate changed: " + aBoolean);
            if (aBoolean) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    binding.textfrom.setVisibility(VISIBLE);
                    num1.setVisibility(VISIBLE);
                    binding.textGeo.setVisibility(VISIBLE);
                    binding.textwhere.setVisibility(VISIBLE);
                    binding.num2.setVisibility(VISIBLE);
                    binding.textTo.setVisibility(VISIBLE);
                    Logger.d(context, TAG, "onResume: 3");
                    firstLocation();
                } else {
                    String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                    if (!userEmail.equals("email")) {
                        try {
                            visicomCost();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                        readTariffInfo();
                    }

                }
            }  else {
                String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                if (!userEmail.equals("email")) {
                    try {
                        visicomCost();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                    readTariffInfo();
                }

            }

        });
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
                        currentNavDestination = R.id.nav_restart;
                        navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_restart, true)
                                .build());
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
        schedule.setText(R.string.on_now);
        if (!MainActivity.firstStart) {
            ContentValues cv = new ContentValues();
            cv.put("time", "no_time");
            cv.put("date", "no_date");

            // обновляем по id
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[]{"1"});
            database.close();
        }

        schedule.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            sharedPreferencesHelperMain.saveValue("initial_page", "visicom");
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.nav_options);
        });


        shed_down.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            sharedPreferencesHelperMain.saveValue("initial_page", "visicom");
            NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_options, null, new NavOptions.Builder()
                    .build());
//            MyBottomSheetGeoFragment bottomSheetDialogFragment = new MyBottomSheetGeoFragment(text_view_cost);
//            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
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
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.upd_available)
                .setMessage(R.string.upd_available_promt)
                .setCancelable(false)
                .setPositiveButton(R.string.upd_available_ok, (dialog, which) -> {
                    MainActivity mainActivity = (MainActivity) requireActivity();
                    AppUpdater appUpdater = new AppUpdater(
                            mainActivity,
                            mainActivity.getExactAlarmLauncher(),
                            mainActivity.getBatteryOptimizationLauncher()
                    );
                    appUpdater.startUpdate();
                })
                .setNegativeButton(R.string.upd_available_cancel, (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    public static void addCheck(Context context) {

        String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");
        int newCheck = 0;
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        for (int i = 0; i < DataArr.arrayServiceCode().length; i++) {
            if (services.get(i + 1).equals("1")) {
                newCheck++;
            }
        }
        if (!tarif.equals(" ")) {
            newCheck++;
        }
//        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
//        String comment = stringList.get(2);
        String comment = sharedPreferencesHelperMain.getValue("comment", "no_comment").toString();
        Logger.d(context, TAG, "comment" + comment);

        if (!comment.equals("no_comment")) {
            newCheck++;
        }
        String mes = context.getString(R.string.add_services);
        if (newCheck != 0) {
            mes = context.getString(R.string.add_services) + " (" + newCheck + ")";
        }


        btnAdd.setText(mes);

    }


    public void btnVisible(int visible) {
        if (text_view_cost != null) {
            if (visible == View.INVISIBLE) {
                binding.progressBar.setVisibility(VISIBLE);
            } else {
                binding.progressBar.setVisibility(GONE);
            }


            binding.linearLayoutButtons.setVisibility(visible);

            binding.btnAdd.setVisibility(visible);

            binding.btnBonus.setVisibility(visible);
            binding.btnMinus.setVisibility(visible);
            binding.textViewCost.setVisibility(visible);
            binding.btnPlus.setVisibility(visible);
            binding.btnOrder.setVisibility(visible);

            binding.schedule.setVisibility(visible);

            binding.shedDown.setVisibility(visible);
        }
    }

    public static void btnStaticVisible(int visible) {
        if (text_view_cost != null) {
            if (visible == View.INVISIBLE) {
                progressBar.setVisibility(VISIBLE);
            } else {
                progressBar.setVisibility(GONE);
            }


            linearLayout.setVisibility(visible);

            btnAdd.setVisibility(visible);

            buttonBonus.setVisibility(visible);
            btn_minus.setVisibility(visible);
            text_view_cost.setVisibility(visible);
            btn_plus.setVisibility(visible);
            btnOrder.setVisibility(visible);


            schedule.setVisibility(visible);

            shed_down.setVisibility(visible);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
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
        super.onDestroyView();

        RetrofitClient.getInstance().cancelAllRequests();

        binding = null;
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
                    str = str.concat(cn + " = " + c.getString(c.getColumnIndex(cn)) + "; ");
                    list.add(c.getString(c.getColumnIndex(cn)));

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


    @SuppressLint({"Range", "ResourceAsColor"})
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: " + urlAPI);
        startTilePreloadWorker();

        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery(query, null);
        cursor.moveToFirst();

        double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
        double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
        double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
        double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
        String start = cursor.getString(cursor.getColumnIndex("start"));
        String finish = cursor.getString(cursor.getColumnIndex("finish"));

        if (start.trim().isEmpty() || geoText.getText().toString().trim().isEmpty()) {
            start = context.getString(R.string.startPoint);
            geoText.setText(start);
        }
        if (originLatitude == 0.0 || originLatitude == 0.0) {
            geoText.setText("");
            geoText.setBackgroundColor(R.color.selected_text_color);
            return "error";
        }
        if (finish.equals(context.getString(R.string.on_city_tv)) || finish.trim().isEmpty()) {
            finish = start;
            toLatitude = originLatitude;
            toLongitude = originLongitude;
        }
        if (finish.trim().equals(start.trim())) {
            textViewTo.setText("");
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

        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);

        if (urlAPI.equals("costSearchMarkersTime")) {
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

            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") *" + userEmail + "*" + payment_type + "/"
                    + time + "/" + date;
        }

        if (urlAPI.equals("orderClientCost")) {
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
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    wfpInvoice = MainActivity.order_id;
                }
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
            if((boolean)sharedPreferencesHelperMain.getValue("black_list_45", false)) {
                long cost = Long.parseLong(clientCost); // Convert string to double
                cost += 45; // Add 45
                clientCost = String.valueOf(cost); // Convert back to string
            }
            sharedPreferencesHelperMain.saveValue("black_list_45", false);

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
                result = cursor.getString(cursor.getColumnIndex("rectoken"));
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
        urlOrder = getTaxiUrlSearchMarkers("orderClientCost", context );
        Logger.d(context, TAG, "order: urlOrder " + urlOrder);
        if(urlOrder.equals("error")) {
            Toast.makeText(context, R.string.no_start_point_message, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    public void orderFinished() throws MalformedURLException {
        if (!verifyPhone()) {
            MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment(context, "visicom");
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            progressBar.setVisibility(GONE);
        } else {

                constraintLayoutVisicomMain.setVisibility(GONE);
                if (textViewTo.getText().equals("")) {
                    textViewTo.setText(context.getString(R.string.on_city_tv));
                }
                String messageResult =
                        geoText.getText().toString() + " " + getString(R.string.to_message) +
                                textViewTo.getText() + ".";

                text_full_message.setText(messageResult);

                messageResult = context.getString(R.string.check_cost_message);
                textCostMessage.setText(messageResult);

                textStatusCar.setText(R.string.ex_st_0);

            Animation blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink_animation);
                textStatusCar.startAnimation(blinkAnimation);
                String pay_method_message = "";
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
                    default:
                        pay_method_message += " " + context.getString(R.string.pay_method_message_nal);
                }


//                String messagePayment = text_view_cost.getText().toString() + " " + context.getString(R.string.UAH) + " " + pay_method_message;
//
//                textCostMessage.setText(messagePayment);
                carProgressBar.resumeAnimation();
                constraintLayoutVisicomFinish.setVisibility(VISIBLE);


                ToJSONParserRetrofit parser = new ToJSONParserRetrofit();
                baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                Logger.d(context, TAG, "orderFinished: " + baseUrl + urlOrder);

                parser.sendURLChannel(urlOrder, new Callback<>() {

                    @Override
                    public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, String> sendUrlMap = response.body();

                            handleOrderFinished(sendUrlMap, pay_method, context);
                        } else {
                            btnVisible(VISIBLE);
                            String messageErr = getString(R.string.cost_error);
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(messageErr);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                        FirebaseCrashlytics.getInstance().recordException(t);
                        navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_restart, true)
                                .build());
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

        boolean VisicomBackPressed = (boolean) sharedPreferencesHelperMain.getValue("VisicomBackPressed", false);

        if (!"0".equals(orderWeb)) {
            String to_name;
//            orderWeb = text_view_cost.getText().toString();
            if (Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
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
            String comment = sharedPreferencesHelperMain.getValue("comment", "no_comment").toString();
            sendUrlMap.put("comment_info", comment);

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
            Logger.d(context, TAG, "sendUrlMap: comment_info " + sendUrlMap.get("comment_info"));
            Logger.d(context, TAG, "sendUrlMap: extra_charge_codes " + sendUrlMap.get("extra_charge_codes"));

            MainActivity.uid = sendUrlMap.get("dispatching_order_uid");
            Logger.d(context, "MainActivity.uid", "MainActivity.uid 1 " + MainActivity.uid);

            Bundle bundle = new Bundle();
            bundle.putString("messageResult_key", messageResult);
            bundle.putString("messagePay_key", messagePayment);
            bundle.putString("messageFondy_key", messageFondy);
            bundle.putString("messageCost_key", orderWeb);
            bundle.putSerializable("sendUrlMap", new HashMap<>(sendUrlMap));
            bundle.putString("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
            viewModel.setStatusNalUpdate(true); //наюлюдение за опросом статусом нала
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                navController.navigate(
                        R.id.nav_finish_separate,
                        bundle,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_visicom, true)
                                .build()
                );
            }, 1000); // 5000 миллисекунд = 5 секунд


        } else if (!VisicomBackPressed) {
            sharedPreferencesHelperMain.saveValue("VisicomBackPressed", false);
            btnVisible(VISIBLE);
            assert message != null;
            constraintLayoutVisicomFinish.setVisibility(GONE);
            constraintLayoutVisicomMain.setVisibility(VISIBLE);
            Logger.d(context, TAG, "2 orderFinished: message " + message);
            String addType = "60";
            if (message.contains("Дублирование")) {
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
            } else {
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
        if (!isAdded() || getActivity() == null) {
            Logger.d(null, TAG, "Fragment not attached, skipping phone verification");
            return false; // Or handle appropriately
        }
        List<String> stringList = logCursor(MainActivity.TABLE_USER_INFO, context);
        if (stringList.size() < 3) {
            Logger.d(requireActivity(), TAG, "Invalid or empty stringList");
            return false;
        }
        String phone = stringList.get(2);
        if (phone == null || phone.isEmpty()) {
            Logger.d(requireActivity(), TAG, "Phone number is null or empty");
            return false;
        }
        Logger.d(requireActivity(), TAG, "onClick befor validate: ");
        String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
        Logger.d(requireActivity(), TAG, "onClick No validate: " + val);
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
            }

            if (orderRout()) {
                googleVerifyAccount();
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

        if (!NetworkUtils.isNetworkAvailable(requireActivity())) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        }

        

        VisicomFragment.sendUrlMap = null;
        MainActivity.uid = null;
        Logger.d(context, "MainActivity.uid", "MainActivity.uid 2 " + MainActivity.uid);

        MainActivity.orderResponse = null;
        viewModel.updateOrderResponse(null);
        viewModel.setTransactionStatus(null);
        viewModel.setCanceledStatus("no_canceled");

        textfrom = binding.textfrom;

        constraintLayoutVisicomMain.setVisibility(GONE);
 
        binding.svButton.setVisibility(GONE);
        binding.btnCallAdmin.setVisibility(GONE);

        String cityCheckActivity = (String) sharedPreferencesHelperMain.getValue("CityCheckActivity", "**");
        Logger.d(context, TAG, "CityCheckActivity: " + cityCheckActivity);
        progressBar.setVisibility(View.INVISIBLE);
        if (cityCheckActivity.equals("run")) {
            binding.textfrom.setVisibility(VISIBLE);
            binding.num1.setVisibility(VISIBLE);
            binding.textGeo.setVisibility(VISIBLE);
            binding.clearButtonFrom.setVisibility(VISIBLE);
            binding.num2.setVisibility(VISIBLE);
            binding.textTo.setVisibility(VISIBLE);
            binding.clearButtonTo.setVisibility(VISIBLE);
        }
        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        }


        String visible_shed = (String) sharedPreferencesHelperMain.getValue("visible_shed", "no");
        if(visible_shed.equals("no")) {
            Logger.d(context, TAG, "onResume 2" );
            schedule.setVisibility(View.INVISIBLE);
            shed_down.setVisibility(View.INVISIBLE);

            gpsBtn.setVisibility(View.INVISIBLE);
            binding.num1.setVisibility(View.INVISIBLE);
            binding.textfrom.setVisibility(View.INVISIBLE);

            binding.textwhere.setVisibility(View.INVISIBLE);
 
            binding.svButton.setVisibility(View.INVISIBLE);
            binding.btnCallAdmin.setVisibility(View.INVISIBLE);

        } else  {
            if (NetworkUtils.isNetworkAvailable(context)) {
                Logger.d(context, TAG, "onResume 3" );

                binding.textfrom.setVisibility(VISIBLE);
                binding.num1.setVisibility(VISIBLE);
                binding.clearButtonFrom.setVisibility(VISIBLE);


                binding.clearButtonTo.setVisibility(VISIBLE);

                binding.textGeo.setVisibility(VISIBLE);
                binding.clearButtonFrom.setVisibility(VISIBLE);

                binding.num2.setVisibility(VISIBLE);
                binding.textTo.setVisibility(VISIBLE);
                binding.clearButtonTo.setVisibility(VISIBLE);

                schedule.setVisibility(VISIBLE);
                shed_down.setVisibility(VISIBLE);


                binding.svButton.setVisibility(View.VISIBLE);
                binding.btnCallAdmin.setVisibility(View.VISIBLE);

            } else {
                Logger.d(context, TAG, "onResume 4" );
                schedule.setVisibility(View.INVISIBLE);
                shed_down.setVisibility(View.INVISIBLE);
                gpsBtn.setVisibility(View.INVISIBLE);
                binding.num1.setVisibility(View.INVISIBLE);
                binding.textfrom.setVisibility(View.INVISIBLE);

                binding.textwhere.setVisibility(VISIBLE);
                progressBar.setVisibility(VISIBLE);

              
       
                binding.svButton.setVisibility(GONE);
                binding.btnCallAdmin.setVisibility(GONE);

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
            navController.navigate(R.id.nav_search, bundle, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_search, true)
                    .build());

        });

        binding.clearButtonFrom.setOnClickListener(v -> {

            gpsBtn.setText(R.string.change);

            Bundle bundle = new Bundle();
            bundle.putString("start", "ok");
            bundle.putString("end", "no");
            navController.navigate(R.id.nav_search, bundle, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_search, true)
                    .build());

        });

        text_view_cost = binding.textViewCost;

        geo_marker = "visicom";

        Logger.d(context, TAG, "onCreateView: geo_marker " + geo_marker);

        buttonBonus.setOnClickListener(v -> {
            boolean black_list_yes = verifyOrder();
            Logger.d(context, TAG, "buttonBonus 2 " + black_list_yes);
            btnVisible(View.INVISIBLE);
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
            textViewTo.setText("");

            Bundle bundle = new Bundle();
            bundle.putString("start", "no");
            bundle.putString("end", "ok");
            navController.navigate(R.id.nav_search, bundle, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_search, true)
                    .build());

        });

        binding.clearButtonTo.setOnClickListener(v -> {
            updateRouteSettings();
            try {
                visicomCost();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            textViewTo.setText("");
        });

        btn_minus = binding.btnMinus;
        btn_plus = binding.btnPlus;
        btnOrder = binding.btnOrder;


        btnAdd.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            sharedPreferencesHelperMain.saveValue("initial_page", "visicom");
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.nav_options);
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
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
            }

            linearLayout.setVisibility(GONE);
            btnVisible(View.INVISIBLE);
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
                        }
                    }
                    break;
                default:
                    if (orderRout()) {
                        googleVerifyAccount();
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
            viewModel.setStatusX(true);
            Boolean statusX = viewModel.getStatusX().getValue(); // Get the boolean value
            Log.e("setStatusX 45", "setStatusXUpdate: " + (statusX != null ? statusX.toString() : "null"));
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.getContext().startActivity(intent);
//            Toast.makeText(v.getContext(), "Откройте настройки и отключите GPS вручную", Toast.LENGTH_LONG).show();
            return true; // сигнализирует, что обработка завершена
        });

        gpsBtn.setOnClickListener(v -> {
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

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                gpsBtn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.buttons_yellow));

                gpsBtn.setTextColor(Color.BLACK);
                viewModel.setStatusX(false);
            } else {
                gpsBtn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.buttons_green));
                gpsBtn.setTextColor(Color.WHITE);

                viewModel.setStatusX((boolean) sharedPreferencesHelperMain.getValue("setStatusX", true));
                Boolean statusX = viewModel.getStatusX().getValue(); // Get the boolean value
                Log.e("setStatusX 4", "setStatusXUpdate: " + (statusX != null ? statusX.toString() : "null"));
            }
        } else {
            gpsBtn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.btn_red));
            gpsBtn.setTextColor(Color.WHITE);
            viewModel.setStatusX(false);
            viewModel.setStatusGpsUpdate(false);
        }


        if (NetworkUtils.isNetworkAvailable(context)) {
            if (geoText.getText().toString().isEmpty()) {

                binding.textfrom.setVisibility(View.INVISIBLE);
                num1.setVisibility(View.INVISIBLE);
                binding.textwhere.setVisibility(View.INVISIBLE);
            }
            String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
            if (!userEmail.equals("email")) {
                try {
                    visicomCost();
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                readTariffInfo();
            }

        } else {
            binding.textwhere.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(GONE);
        }

        scheduleUpdate();
        addCheck(context);


        updateApp();
    }

    private void gpsButSetOnClickListener (LocationManager locationManager) {
//        viewModel.setStatusX(false);
//        sharedPreferencesHelperMain.saveValue("setStatusX", false);
        if (locationManager != null) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

                if(loadPermissionRequestCount() >= 3  && !location_update) {
                    sharedPreferencesHelperMain.saveValue("setStatusX", true);
                    viewModel.setStatusX(true);
                    Boolean statusX = viewModel.getStatusX().getValue(); // Get the boolean value
                    Log.e("setStatusX 451", "setStatusXUpdate: " + (statusX != null ? statusX.toString() : "null"));
                    MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment(getString(R.string.location_on));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                } else {
                    sharedPreferencesHelperMain.saveValue("setStatusX", true);
                    viewModel.setStatusX(true);
                    Boolean statusX = viewModel.getStatusX().getValue(); // Get the boolean value
                    Log.e("setStatusX 452", "setStatusXUpdate: " + (statusX != null ? statusX.toString() : "null"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // Обработка отсутствия необходимых разрешений
                            checkPermission( );
                        }
                    } else {
                        // Для версий Android ниже 10
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // Обработка отсутствия необходимых разрешений
                            checkPermission();
                            checkPermission();
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        // Обработка отсутствия необходимых разрешений
                        location_update = true;
                    }
                } else location_update = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;


                Logger.d(context, TAG, "locationManager: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
                // GPS включен, выполните ваш код здесь
                if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                    navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());
                }

                else  if(location_update) {
                    String searchText = getString(R.string.search_text) + "...";

                    progressBar.setVisibility(View.VISIBLE);
                    Toast.makeText(context, searchText, Toast.LENGTH_SHORT).show();
                    firstLocation();
                }
            } else {
                sharedPreferencesHelperMain.saveValue("setStatusX", true);
                viewModel.setStatusX(true);
                Boolean statusX = viewModel.getStatusX().getValue(); // Get the boolean value
                Log.e("setStatusX 453", "setStatusXUpdate: " + (statusX != null ? statusX.toString() : "null"));
                MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }

        } else {
            // GPS выключен, выполните необходимые действия
            // Например, показать диалоговое окно с предупреждением о включении GPS
            MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                gpsBtn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.buttons_yellow));

                gpsBtn.setTextColor(Color.BLACK);
                viewModel.setStatusX(false);
            } else {
                gpsBtn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.buttons_green));
                gpsBtn.setTextColor(Color.WHITE);

                viewModel.setStatusX((boolean) sharedPreferencesHelperMain.getValue("setStatusX", true));
                Boolean statusX = viewModel.getStatusX().getValue(); // Get the boolean value
                Log.e("setStatusX 4", "setStatusXUpdate: " + (statusX != null ? statusX.toString() : "null"));
            }
        } else {
            gpsBtn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.btn_red));
            gpsBtn.setTextColor(Color.WHITE);
            viewModel.setStatusX(false);
            viewModel.setStatusGpsUpdate(false);
        }
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
            default:
                btnBonusName = context.getString(R.string.btn_cache);
        }
        buttonBonus.setText(btnBonusName);
    }


    private void firstLocation() {
        progressBar.setVisibility(View.VISIBLE);
        schedule.setVisibility(View.VISIBLE);
        shed_down.setVisibility(View.VISIBLE);

        Toast.makeText(context, context.getString(R.string.search), Toast.LENGTH_SHORT).show();

        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        gpsBtn.setText(R.string.change);
        gpsBtn.setOnClickListener(v1 -> {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            gpsButSetOnClickListener(locationManager);
        });

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Logger.d(context, TAG, "Нет разрешения на геолокацию");
            return;
        }

        viewModel.setStatusX(false);

        // ✅ Одноразовое получение текущей локации
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        Logger.d(context, TAG, "getCurrentLocation: " + latitude + ", " + longitude);

                        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                        String api = stringList.get(2);
                        String language = Locale.getDefault().getLanguage();

                        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                        String urlFrom = baseUrl + "/" + api + "/android/fromSearchGeoLocal/" + latitude + "/" + longitude + "/" + language;

                        FromJSONParserRetrofit.sendURL(urlFrom, result -> {
                            if (result != null) {
                                String FromAdressString = result.get("route_address_from");
                                if (FromAdressString != null && FromAdressString.contains("Точка на карте")) {
                                    FromAdressString = context.getString(R.string.startPoint);
                                }

                                new CityFinder(
                                        context,
                                        latitude,
                                        longitude,
                                        FromAdressString,
                                        context
                                ).findCity(latitude, longitude);

                            } else {
                                Logger.d(context, TAG, "Ошибка при получении адреса");
                            }
                        });

                    } else {
                        Logger.d(context, TAG, "Локация = null");
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }


    private void updateRoutMarker(List<String> settings) {
        Logger.d(context, TAG, "updateRoutMarker: " + settings.toString());
        ContentValues cv = new ContentValues();

        cv.put("startLat", Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));
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

    private void visicomCost() throws MalformedURLException {
        Logger.d(context, TAG, "=== visicomCost() started ===");

        constr2.setVisibility(View.INVISIBLE);


        MainActivity.costMap = null;

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery("SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);

        if (!cursor.moveToFirst()) {
            Logger.w(context, TAG, "Маршрут не найден — cursor пуст");
            cursor.close();
            database.close();
            return;
        }

        double originLatitude = cursor.getDouble(cursor.getColumnIndexOrThrow("startLat"));
        double toLat = cursor.getDouble(cursor.getColumnIndexOrThrow("to_lat"));
        String start = cursor.getString(cursor.getColumnIndexOrThrow("start"));
        String finish = cursor.getString(cursor.getColumnIndexOrThrow("finish"));

        cursor.close();
        database.close();

        String cityCheckActivity = String.valueOf(sharedPreferencesHelperMain.getValue("CityCheckActivity", "**"));
        Logger.d(context, TAG, "cityCheckActivity = " + cityCheckActivity);
        Logger.d(context, TAG, "originLatitude = " + originLatitude + ", toLat = " + toLat);

        if ("run".equals(cityCheckActivity) && originLatitude != 0.0 && toLat != 0.0) {
            progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(context, context.getString(R.string.check_cost_message), Toast.LENGTH_SHORT).show();

            gpsBtn.setVisibility(View.VISIBLE);
            svButton.setVisibility(View.VISIBLE);
            btnCallAdmin.setVisibility(View.VISIBLE);
            textfrom.setVisibility(View.VISIBLE);
            num1.setVisibility(View.VISIBLE);
            textwhere.setVisibility(View.VISIBLE);
            num2.setVisibility(View.VISIBLE);
            textViewTo.setVisibility(View.VISIBLE);
            btnVisible(View.INVISIBLE);

            requestCostFromServer(start, finish);
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

    private void requestCostFromServer(String start, String finish) throws MalformedURLException {
        String urlCost = getTaxiUrlSearchMarkers("costSearchMarkersTime", context);
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

                    geoText.setText(start);
                    binding.textTo.setText(finish.trim().equals(start.trim()) ? "" : finish);

                    Map<String, String> map = response.body();
                    String cost;
                    if (map != null && !"0".equals(map.get("order_cost"))) {
                        cost = map.get("order_cost");
                        applyDiscountAndUpdateUI(cost, context);
                    } else {

                        if (map != null && Objects.equals(map.get("Message"), "Повторный запрос")) {
                            String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");
                            cost = (String) sharedPreferencesHelperMain.getValue(tarif, "100");
                            applyDiscountAndUpdateUI(cost, context);
                        } else {
                            cost = "100";
                            applyDiscountAndUpdateUI(cost, context);
                        }

                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.e(context, TAG, "Ошибка подключения к серверу: " + t.getMessage());
                applyDiscountAndUpdateUI("0", context);
            }
        });
    }

//    private long lastRequestTime = 0;
//    private static final long MIN_REQUEST_INTERVAL_MS = 2000; // минимальный интервал между запросами (2 секунды)
//
//    private void requestCostFromServer(String start, String finish) throws MalformedURLException {
//        long now = System.currentTimeMillis();
//        if (now - lastRequestTime < MIN_REQUEST_INTERVAL_MS) {
//            Logger.w(context, TAG, "Пропускаем повторный вызов requestCostFromServer (слишком часто)");
//            return;
//        }
//        lastRequestTime = now;
//
//        String urlCost = getTaxiUrlSearchMarkers("costSearchMarkersTime", context);
//        Logger.d(context, TAG, "Попытка #1, URL: " + urlCost);
//
//        // Создаем и запускаем WorkRequest
//        OneTimeWorkRequest request = WorkManagerHelper.scheduleCostRequest(context, urlCost);
//        WorkManager.getInstance(context).enqueue(request);
//
//        // Наблюдаем за результатом
//        WorkManager.getInstance(context)
//                .getWorkInfoByIdLiveData(request.getId())
//                .observe(getViewLifecycleOwner(), workInfo -> {
//                    if (workInfo != null && workInfo.getState().isFinished()) {
//                        new Handler(Looper.getMainLooper()).post(() -> {
//                            if (!isAdded() || binding == null) {
//                                Logger.w(context, TAG, "Фрагмент отсоединён или binding null — выходим");
//                                return;
//                            }
//
//                            geoText.setText(start);
//                            binding.textTo.setText(finish.trim().equals(start.trim()) ? "" : finish);
//
//                            Data outputData = workInfo.getOutputData();
//
//                            if (outputData.getString("error") != null) {
//                                FirebaseCrashlytics.getInstance().log("Ошибка costRequest: " + outputData.getString("error"));
//                                Logger.e(context, TAG, "Ошибка costRequest: " + outputData.getString("error"));
//                                applyDiscountAndUpdateUI("0", context);
//                                return;
//                            }
//
//                            String orderCost = outputData.getString("order_cost");
//                            String message = outputData.getString("Message");
//
//                            if (orderCost != null && !"0".equals(orderCost)) {
//                                applyDiscountAndUpdateUI(orderCost, context);
//                            } else if ("Повторный запрос".equals(message)) {
//                                String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");
//                                String cost = (String) sharedPreferencesHelperMain.getValue(tarif, "0");
//                                applyDiscountAndUpdateUI(cost, context);
//                            } else {
//                                applyDiscountAndUpdateUI("0", context);
//                            }
//                        });
//                    }
//                });
//    }
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
        Logger.d(context, TAG, "applyDiscountAndUpdateUI() start — orderCost = " + orderCost);

        if (orderCost == null || !orderCost.matches("\\d+")) {
            Logger.e(context, TAG, "Invalid orderCost: " + orderCost);
            return;
        }

        // Проверяем, не совпадает ли стоимость с предыдущей
//        if (orderCost.equals(lastAppliedCost)) {
//            Logger.d(context, TAG, "Стоимость не изменилась, обновление UI пропущено: " + orderCost);
//            return;
//        }
//        lastAppliedCost = orderCost;

        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
        Logger.d(context, TAG, "Retrieved discountText = " + discountText);

        if (discountText == null || !(discountText.matches("[+-]?\\d+") || discountText.equals("0"))) {
            Logger.w(context, TAG, "Invalid or missing discountText: " + discountText);
            return;
        }

        try {
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

            startCost = firstCost;
            finalCost = firstCost;
            MIN_COST_VALUE = (long) (firstCost * 0.6);
            firstCostForMin = firstCost;

            Logger.d(context, TAG, "Setting UI visibility and values");

            geoText.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);

            textfrom.setVisibility(View.VISIBLE);
            num1.setVisibility(View.VISIBLE);
            textwhere.setVisibility(View.VISIBLE);
            num2.setVisibility(View.VISIBLE);
            textViewTo.setVisibility(View.VISIBLE);

            btnAdd.setVisibility(View.VISIBLE);
            buttonBonus.setVisibility(View.VISIBLE);
            btn_minus.setVisibility(View.VISIBLE);
            text_view_cost.setVisibility(View.VISIBLE);
            btn_plus.setVisibility(View.VISIBLE);
            btnOrder.setVisibility(View.VISIBLE);
            constr2.setVisibility(View.VISIBLE);
            schedule.setVisibility(View.VISIBLE);
            shed_down.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Logger.e(context, TAG, "NumberFormatException в applyDiscountAndUpdateUI: " + e.getMessage());
        }

        btnVisible(View.VISIBLE);
    }


    private void blockUserBlackList() {
        // Log the start of the block process
        Log.d("blockUserBlackList", "Starting the block process for user.");

        // Update button text and make it non-clickable
        buttonBonus.setText(context.getString(R.string.card_payment));
//        buttonBonus.setClickable(false);
        buttonBonus.setOnClickListener(v -> {
            String message = context.getString(R.string.black_list_message_err);
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

        });

        Log.d("blockUserBlackList", "Button text set and made non-clickable.");

        // Retrieve email from the database
        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        Log.d("blockUserBlackList", "Retrieved email from database: " + email);

        // Add email to the blacklist
        BlacklistManager blacklistManager = new BlacklistManager();
        blacklistManager.addToBlacklist(email);
        Log.d("blockUserBlackList", "Request to add email to blacklist sent: " + email);


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

        String closeReasonText = context.getString(R.string.close_resone_def);

        for (int i = 0; i < routeListCancel.size(); i++) {
            RouteResponseCancel route = routeListCancel.get(i);
            String uid = route.getUid();
            String routeFrom = route.getRouteFrom();
            String routefromnumber = route.getRouteFromNumber();
            String routeTo = route.getRouteTo();
            String routeTonumber = route.getRouteToNumber();
            String webCost = route.getWebCost();
            String createdAt = route.getCreatedAt();
            String closeReason = route.getCloseReason();
            String auto = route.getAuto();
            String dispatchingOrderUidDouble = route.getDispatchingOrderUidDouble();
            String pay_method = route.getPay_method();
            String required_time = route.getRequired_time();
            String flexible_tariff_name = route.getFlexible_tariff_name();
            String comment_info = route.getComment_info();
            String extra_charge_codes = route.getExtra_charge_codes();

            switch (closeReason) {
                case "101":
                case "-1":
                    closeReasonText = context.getString(R.string.close_resone_in_work);
                    break;
                case "102":
                    closeReasonText = context.getString(R.string.close_resone_in_start_point);
                    break;
                case "103":
                    closeReasonText = context.getString(R.string.close_resone_in_rout);
                    break;
                case "104":
                case "8":
                    closeReasonText = context.getString(R.string.close_resone_8);
                    break;
                case "0":
                    closeReasonText = context.getString(R.string.close_resone_0);
                    break;
                case "1":
                    closeReasonText = context.getString(R.string.close_resone_1);
                    break;
                case "2":
                    closeReasonText = context.getString(R.string.close_resone_2);
                    break;
                case "3":
                    closeReasonText = context.getString(R.string.close_resone_3);
                    break;
                case "4":
                    closeReasonText = context.getString(R.string.close_resone_4);
                    break;
                case "5":
                    closeReasonText = context.getString(R.string.close_resone_5);
                    break;
                case "6":
                    closeReasonText = context.getString(R.string.close_resone_6);
                    break;
                case "7":
                    closeReasonText = context.getString(R.string.close_resone_7);
                    break;
                case "9":
                    closeReasonText = context.getString(R.string.close_resone_9);
                    break;
                default:
                    // оставляем прежний текст, если не совпало
                    break;
            }


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
            if (required_time != null && !required_time.contains("1970-01-01")) {
                required_time = " " + context.getString(R.string.time_order) + required_time;
            } else {
                required_time = "";
            }
            if (routeFrom.equals(routeTo)) {
                routeInfo = routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to)
                        + context.getString(R.string.on_city)
                        + required_time + "#"
                        + context.getString(R.string.close_resone_cost) + webCost + " " + context.getString(R.string.UAH) + "#"
                        + context.getString(R.string.auto_info) + " " + auto + "#"
                        + context.getString(R.string.close_resone_time)
                        + createdAt + "#"
                        + context.getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to) + routeTo + " " + routeTonumber + "."
                        + required_time + "#"
                        + context.getString(R.string.close_resone_cost) + webCost + " " + context.getString(R.string.UAH) + "#"
                        + context.getString(R.string.auto_info) + " " + auto + "#"
                        + context.getString(R.string.close_resone_time) + createdAt + "#"
                        + context.getString(R.string.close_resone_text) + closeReasonText;
            }

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
            settings.add(required_time);
            settings.add(flexible_tariff_name);
            settings.add(comment_info);
            settings.add(extra_charge_codes);

            Logger.d(context, TAG, settings.toString());
            databaseHelperUid.addCancelInfoUid(settings);
        }

        String[] array = databaseHelper.readRouteCancel();
        Logger.d(context, TAG, "processRouteList: array " + Arrays.toString(array));
        if (array != null) {
            String message = context.getString(R.string.order_to_cancel_true);
            NavController navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
            int currentDestination = navController.getCurrentDestination().getId();

            if (currentDestination == R.id.nav_visicom) {
                MyBottomSheetErrorFragment myBottomSheetMessageFragment = new MyBottomSheetErrorFragment(message);
                fragmentManager.beginTransaction()
                        .add(myBottomSheetMessageFragment, myBottomSheetMessageFragment.getTag())
                        .commitAllowingStateLoss();
            }

        } else {
            databaseHelper.clearTableCancel();
            databaseHelperUid.clearTableCancel();
        }
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
                        createBlackList();
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

//        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//
//        OkHttpClient client = new OkHttpClient.Builder()
//                .addInterceptor(new RetryInterceptor())
//                .addInterceptor(interceptor)
//                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
//                .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
//                .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
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
//                    String paymentCode = responsePaySystem.getPay_system();
//                    switch (paymentCode) {
//                        case "wfp":
//                            pay_method = "wfp_payment";
//                            break;
//                        case "fondy":
//                            pay_method = "fondy_payment";
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
//
//                        cv = new ContentValues();
//                        cv.put("verifyOrder", "0");
//
//                        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
//                        database.close();
//
//                        orderRout();
//
//                        googleVerifyAccount();
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
//                FirebaseCrashlytics.getInstance().recordException(t);
//                if (isAdded()) { //
//                    navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                            .setPopUpTo(R.id.nav_restart, true)
//                            .build());
//                }
//            }
//        });
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

    private void googleVerifyAccount() {

        FirebaseConsentManager consentManager = new FirebaseConsentManager(context);

        consentManager.checkUserConsent(new FirebaseConsentManager.ConsentCallback() {
            @Override
            public void onConsentValid() {
                Logger.d(context, TAG, "Согласие пользователя действительное.");
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        if(!verifyOrder()) {
                            orderFinished();
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


            // Обновление таблицы
            ContentValues values = new ContentValues();
            values.put("startLat", Double.parseDouble(settings.get(0)));
            values.put("startLan", Double.parseDouble(settings.get(1)));
            values.put("to_lat", Double.parseDouble(settings.get(2)));
            values.put("to_lng", Double.parseDouble(settings.get(3)));
            values.put("start", settings.get(4));
            values.put("finish", settings.get(5));

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

    private void updateGpsButtonDrawable(boolean xShow) {
        if (xShow) {
            // Показываем крест
//            gpsBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.x_image, 0);
            gpsBtn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.buttons_green_cross));

        } else {
            // Убираем крест
//            gpsBtn.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
//            gpsBtn.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.buttons_green));
        }
    }
    private void statusOrder() throws ParseException {
        String uid =  (String) sharedPreferencesHelperMain.getValue("uid_fcm", "");
        Logger.d(context, TAG, "statusOrder: " + uid);
        new Thread(this::fetchRoutesCancel).start();
//        if(uid.isEmpty()) {
//
//            return;
//        }
        Logger.d(context, "Pusher", "statusCacheOrder: " + uid);

        List<String> listCity = logCursor(CITY_INFO, context);
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
                        if (!message.equals("Заказ снят") && !message.equals("Заказ не найден") && !message.equals("Автоматический заказ не найден")) {
                            sharedPreferencesHelperMain.saveValue("uid_fcm", "");
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
