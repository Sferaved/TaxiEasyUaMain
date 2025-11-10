package com.taxi.easy.ua.ui.home;


import static android.content.Context.MODE_PRIVATE;
import static android.graphics.Color.RED;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.R.string.address_error_message;
import static com.taxi.easy.ua.androidx.startup.MyApplication.getCurrentActivity;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BlendMode;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.room.Room;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.FragmentHomeBinding;
import com.taxi.easy.ua.ui.cities.Cherkasy.Cherkasy;
import com.taxi.easy.ua.ui.cities.Dnipro.DniproCity;
import com.taxi.easy.ua.ui.cities.Kyiv.KyivCity;
import com.taxi.easy.ua.ui.cities.Odessa.Odessa;
import com.taxi.easy.ua.ui.cities.Odessa.OdessaTest;
import com.taxi.easy.ua.ui.cities.Zaporizhzhia.Zaporizhzhia;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.home.room.AppDatabase;
import com.taxi.easy.ua.ui.home.room.RouteCost;
import com.taxi.easy.ua.ui.home.room.RouteCostDao;
import com.taxi.easy.ua.ui.payment_system.PayApi;
import com.taxi.easy.ua.ui.payment_system.ResponsePaySystem;
import com.taxi.easy.ua.ui.start.ResultSONParser;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.animation.car.CarProgressBar;
import com.taxi.easy.ua.utils.auth.FirebaseConsentManager;
import com.taxi.easy.ua.utils.blacklist.BlacklistManager;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetBonusFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyPhoneDialogFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.retrofit.cost_json_parser.CostJSONParserRetrofit;
import com.taxi.easy.ua.utils.retrofit.worker.RetrofitWorker;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;
import com.uxcam.UXCam;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    public static String from, to;
    @SuppressLint("StaticFieldLeak")
    public static EditText from_number, to_number;
    String messageResult;

    public static AppCompatButton btn_order;
    public static AppCompatButton buttonAddServices;
    public static AppCompatButton buttonBonus;
    public static AppCompatButton btn_minus;
    public static AppCompatButton btn_plus;
    public static AppCompatButton btnGeo;
    public static AppCompatButton btn_clear;
    public static AppCompatButton btnCallAdmin;
    public static AppCompatButton btnCallAdminFin;

    public static long addCost, cost, costFirst;
    private static String[] arrayStreet;
    private String numberFlagFrom = "1", numberFlagTo = "1";

    public static String  from_numberCost, toCost, to_numberCost;
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    String pay_method;
    public static long costFirstForMin;
    public static String urlOrder;
    public static long discount;
    public static int routeIdToCheck = 123;
    private AlertDialog alertDialog;
    private CarProgressBar carProgressBar;

    private final Executor executor = Executors.newSingleThreadExecutor();

    private ActivityResultLauncher<String[]> permissionLauncher;



    public static String[] arrayServiceCode() {
        return new String[]{
                "BAGGAGE",
                "ANIMAL",
                "CONDIT",
                "MEET",
                "COURIER",
                "CHECK_OUT",
                "BABY_SEAT",
                "DRIVER",
                "NO_SMOKE",
                "ENGLISH",
                "CABLE",
                "FUEL",
                "WIRES",
                "SMOKE",
        };
    }
    @SuppressLint("StaticFieldLeak")
    public static TextView text_view_cost;

    long MIN_COST_VALUE;
    AutoCompleteTextView textViewFrom, textViewTo;
    ArrayAdapter<String> adapter;

    String city;

    Activity context;
    FragmentManager fragmentManager;
    @SuppressLint("StaticFieldLeak")
    public static TextView schedule;
    static ImageButton shed_down;

    @SuppressLint("StaticFieldLeak")
    static ConstraintLayout constr2;

    private List<RouteResponseCancel> routeListCancel;
    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;

    ConstraintLayout constraintLayoutHomeMain, constraintLayoutHomeFinish;
    public static TextView text_full_message, textCostMessage, textStatusCar;
    private Animation blinkAnimation;
//    private String baseUrl = "https://m.easy-order-taxi.site";
    private String baseUrl;

    private ExecutionStatusViewModel viewModel;

    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);
        if(button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");


        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Logger.d(context, TAG, "Permissions result: " + result.toString());

                    // Сохраняем результаты разрешений в SharedPreferences
                    for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                        sharedPreferencesHelperMain.saveValue(entry.getKey(), entry.getValue() ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED);
                    }

                    // Обновляем счетчик запросов разрешений
                    int permissionRequestCount = loadPermissionRequestCount();
                    permissionRequestCount++;
                    savePermissionRequestCount(permissionRequestCount);
                    Logger.d(context, TAG, "permissionRequestCount: " + permissionRequestCount);
                }
        );


        // Включаем блокировку кнопки "Назад" Применяем блокировку кнопки "Назад"
        BackPressBlocker backPressBlocker = new BackPressBlocker();
        backPressBlocker.setBackButtonBlocked(true);
        backPressBlocker.blockBackButtonWithCallback(this);

        constraintLayoutHomeMain = root.findViewById(R.id.homeMain);
        constraintLayoutHomeFinish = root.findViewById(R.id.homeFinish);
        constraintLayoutHomeFinish.setVisibility(GONE);

        text_full_message = root.findViewById(R.id.text_full_message);
        textCostMessage = root.findViewById(R.id.text_cost_message);
        textStatusCar = root.findViewById(R.id.text_status);
        carProgressBar = root.findViewById(R.id.carProgressBar);

        context = requireActivity();
        fragmentManager = getParentFragmentManager();

        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);

        city = stringList.get(1);
        switch (city){
            case "Ivano_frankivsk":
            case "Vinnytsia":
            case "Poltava":
            case "Sumy":
            case "Kharkiv":
            case "Chernihiv":
            case "Rivne":
            case "Ternopil":
            case "Khmelnytskyi":
            case "Zakarpattya":
            case "Zhytomyr":
            case "Kropyvnytskyi":
            case "Mykolaiv":
            case "Chernivtsi":
            case "Lutsk":
            case "foreign countries":
                MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
                break;
        }
        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        }


        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        progressBar = binding.progressBar;
        buttonBonus = binding.btnBonus;

        addCost = 0;
        updateAddCost(String.valueOf(addCost));

        if(!stringList.isEmpty()) {
            switch (city){
                case "Dnipropetrovsk Oblast":
                    arrayStreet = DniproCity.arrayStreet();
                    paymentType();
                    break;
                case "Zaporizhzhia":
                    arrayStreet = Zaporizhzhia.arrayStreet();
                    paymentType();
                    break;
                case "Cherkasy Oblast":
                    arrayStreet = Cherkasy.arrayStreet();
                    paymentType();
                    break;
                case "Odessa":
                    arrayStreet = Odessa.arrayStreet();
                    paymentType();
                    break;
                case "OdessaTest":
                    arrayStreet = OdessaTest.arrayStreet();
                    break;
                default:
                    arrayStreet = KyivCity.arrayStreet();
                    break;
            }
            adapter = new ArrayAdapter<>(context,R.layout.drop_down_layout_home, arrayStreet);
        }

        text_view_cost = binding.textViewCost;
        btnGeo = binding.btnGeo;

        btnGeo.setOnClickListener(v -> {
            if(loadPermissionRequestCount() >= 3 && !MainActivity.location_update) {
                MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment(getString(R.string.location_on));
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // Обработка отсутствия необходимых разрешений
                        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                    }
                } else {
                    // Для версий Android ниже 10
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // Обработка отсутствия необходимых разрешений
                        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                    }
                }
            }

            // Обработка отсутствия необходимых разрешений
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Обработка отсутствия необходимых разрешений
                    MainActivity.location_update = true;
                }
            } else MainActivity.location_update = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        });
        if(!text_view_cost.getText().toString().isEmpty()) {
            costFirst = Long.parseLong(text_view_cost.getText().toString());
            cost = Long.parseLong(text_view_cost.getText().toString());
        }

        MIN_COST_VALUE = (long) (cost*0.6);

        btn_minus = binding.btnMinus;
        btn_plus= binding.btnPlus;

        btn_minus.setOnClickListener(v -> {
            Logger.d(context, TAG, "onCreateView: cost " +cost);
            Logger.d(context, TAG, "onCreateView: MIN_COST_VALUE " +MIN_COST_VALUE);

                List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
                addCost = Long.parseLong(stringListInfo.get(5));
                cost = Long.parseLong(text_view_cost.getText().toString());
                cost -= 5;
                addCost -= 5;
            if (cost <= MIN_COST_VALUE) {
                addCost += 5;
                cost += 5;
            }
            updateAddCost(String.valueOf(addCost));
            text_view_cost.setText(String.valueOf(cost));
        });

        btn_plus.setOnClickListener(v -> {
            List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
            addCost = Long.parseLong(stringListInfo.get(5));
            cost = Long.parseLong(text_view_cost.getText().toString());
            cost += 5;
            addCost += 5;
            updateAddCost(String.valueOf(addCost));
            text_view_cost.setText(String.valueOf(cost));
        });

        textViewFrom =binding.textFrom;
        int inputType = textViewFrom.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        textViewFrom.setInputType(inputType);

        textViewFrom.setAdapter(adapter);

        textViewTo =binding.textTo;
        textViewTo.setAdapter(adapter);
        int inputTypeTo = textViewTo.getInputType() | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        textViewTo.setInputType(inputTypeTo);
        from_number = binding.fromNumber;
        to_number = binding.toNumber;

        btn_order = binding.btnOrder;

        btn_order.setOnClickListener(new View.OnClickListener() {

            @SuppressLint("UseRequireInsteadOfGet")
            @Override
            public void onClick(View v) {
                if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                    navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());
                }

                btnVisible(VISIBLE);
                if(connected()) {
                    List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                    List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
                    pay_method =  stringListInfo.get(4);
                    switch (stringList.get(1)) {
                        case "Kyiv City":
                        case "Dnipropetrovsk Oblast":
                        case "Odessa":
                        case "Zaporizhzhia":
                        case "Cherkasy Oblast":
                            break;
                        case "OdessaTest":
                            if(pay_method.equals("bonus_payment")) {
                                String bonus = logCursor(MainActivity.TABLE_USER_INFO, context).get(5);
                                if(Long.parseLong(bonus) < cost * 100 ) {
                                    paymentType();
                                }
                            }
                            break;
                    }
                    progressBar.setVisibility(VISIBLE);


                        Logger.d(context, TAG, "onClick: pay_method" + pay_method);
                        List<String> stringListCity = logCursor(MainActivity.CITY_INFO, context);
                        String card_max_pay = stringListCity.get(4);

                        String bonus_max_pay = stringListCity.get(5);
                        switch (pay_method) {
                            case "bonus_payment":
                                if (Long.parseLong(bonus_max_pay) <= Long.parseLong(text_view_cost.getText().toString()) * 100) {
                                    changePayMethodMax(text_view_cost.getText().toString(), pay_method);
                                } else {
                                    try {
                                        if (orderRout()) {
                                            googleVerifyAccount();
                                        }
                                    } catch (UnsupportedEncodingException e) {
                                        FirebaseCrashlytics.getInstance().recordException(e);
                                        throw new RuntimeException(e);
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
                                    try {
                                        if (orderRout()) {
                                            googleVerifyAccount();
                                        }
                                    } catch (UnsupportedEncodingException e) {
                                        FirebaseCrashlytics.getInstance().recordException(e);
                                        throw new RuntimeException(e);
                                    }
                                }
                                break;
                            default:
                                try {
                                    if (orderRout()) {
                                        googleVerifyAccount();
                                    }
                                } catch (UnsupportedEncodingException e) {
                                    FirebaseCrashlytics.getInstance().recordException(e);
                                    throw new RuntimeException(e);
                                }
                        }

                } else {
                    MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());
                }
            }
        });


        buttonAddServices = binding.btnAdd;
        buttonAddServices.setOnClickListener(v -> {
            sharedPreferencesHelperMain.saveValue("initial_page", "home");
            NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_options, null, new NavOptions.Builder().build());
//            MyBottomSheetDialogFragment bottomSheetDialogFragment = new MyBottomSheetDialogFragment();
//            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
        });
        buttonBonus.setOnClickListener(v -> {
            HomeFragment.btnVisible(INVISIBLE);
            List<String> stringList1 = logCursor(MainActivity.CITY_INFO, context);
            String api =  stringList1.get(2);
            updateAddCost("0");
            // Внутри метода onCreateView или обработчика onClick
            String costText = text_view_cost.getText().toString().trim(); // Убираем лишние пробелы
            long costValue = 0; // Значение по умолчанию, если текст пустой или некорректный

            if (!costText.isEmpty()) {
                try {
                    costValue = Long.parseLong(costText);
                } catch (NumberFormatException e) {
                    // Логируем ошибку или показываем сообщение пользователю
                    Log.e("HomeFragment", "Некорректное значение в text_view_cost: " + costText, e);
                    // Можно оставить costValue = 0 или задать другое значение по умолчанию
                }
            }

            MyBottomSheetBonusFragment bottomSheetDialogFragment = new MyBottomSheetBonusFragment(
                    costValue,
                    "home",
                    api,
                    text_view_cost
            );
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
        });
        btnCallAdmin = binding.btnCallAdmin;
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, requireActivity()).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

        btnCallAdminFin = binding.btnCallAdminFin;
        btnCallAdminFin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, requireActivity()).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });


        schedule = binding.schedule;
        shed_down = binding.shedDown;

        constr2 = binding.constr2;

        constr2.setVisibility(INVISIBLE);
        setBtnBonusName(context);
        sharedPreferencesHelperMain.saveValue("carFound", false);
        return root;
    }
    public static void setBtnBonusName(Context context) {
        String btnBonusName;
        String pay_method =  logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);

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
                break;
        }

        buttonBonus.setText(btnBonusName);
    }
    private void scheduleUpdate() {
        // Читаем сохранённые значения из SharedPreferences
        String savedTime = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String savedDate = (String) sharedPreferencesHelperMain.getValue("date", "no_date");

        // Если время и дата установлены, отображаем их
        if (!"no_time".equals(savedTime) && !"no_date".equals(savedDate)) {
            schedule.setText(savedDate + " " + savedTime);  // Формат: "dd.MM.yyyy HH:mm"
        } else {
            schedule.setText(R.string.on_now);  // Дефолт: "Сейчас"
        }

        // Существующий код сброса в БД (если нужно; иначе удалите)
        if (!MainActivity.firstStart) {
            ContentValues cv = new ContentValues();
            cv.put("time", "no_time");
            cv.put("date", "no_date");
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
        }

        // Существующие слушатели кликов (на опции)
        schedule.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            sharedPreferencesHelperMain.saveValue("initial_page", "home");  // Адаптировано для HomeFragment
            NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_options, null, new NavOptions.Builder().build());
        });

        shed_down.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            sharedPreferencesHelperMain.saveValue("initial_page", "home");  // Адаптировано
            NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_options, null, new NavOptions.Builder().build());
        });
    }
    private void orderFinished() {

        if (!verifyPhone()){
            MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment(context, "home");
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            progressBar.setVisibility(GONE);
        } else {
            constraintLayoutHomeMain.setVisibility(GONE);

            String toRout = textViewTo.getText().toString();
            Logger.d(context, TAG, "orderFinished toRout: "  + toRout);
            if(toRout.isEmpty()) {
                toRout =  getString(R.string.on_city);
            } else {
                toRout = textViewTo.getText().toString() + ", " + to_number.getText().toString();
            }
            String messageResultFin =
                    textViewFrom.getText().toString() + ", " + from_number.getText().toString() + " " + getString(R.string.to_message) + toRout;
            text_full_message.setText(messageResultFin);

            messageResult = getString(R.string.check_cost_message);
            textCostMessage.setText(messageResult);

            textStatusCar.setText(R.string.ex_st_0);

            blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink_animation);
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


            String messagePayment = text_view_cost.getText().toString() + " " + context.getString(R.string.UAH) + " " + pay_method_message;

            textCostMessage.setText(messagePayment);
            carProgressBar.resumeAnimation();
            constraintLayoutHomeFinish.setVisibility(VISIBLE);


            ToJSONParserRetrofit parser = new ToJSONParserRetrofit();

//            // Пример строки URL с параметрами
            Logger.d(context, TAG, "orderFinished: "  + baseUrl + urlOrder);
            parser.sendURL(urlOrder, new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    Map<String, String> sendUrlMap = response.body();

                    assert sendUrlMap != null;
                    String orderWeb = sendUrlMap.get("order_cost");
                    String message = sendUrlMap.get("message");

                    assert orderWeb != null;

                    if (!orderWeb.equals("0")) {

                        String from_name = sendUrlMap.get("routefrom");
                        String to_name = sendUrlMap.get("routeto");
//                        orderWeb = text_view_cost.getText().toString();

                        assert from_name != null;

                        String required_time = sendUrlMap.get("required_time");
                        Logger.d(context, TAG, "orderFinished: required_time " + required_time);
                        if (required_time != null && !required_time.contains("1970-01-01") && !required_time.contains("01.01.1970")) {
                            required_time = " " + context.getString(R.string.time_order) + " " + required_time + ".";
                        } else {
                            required_time = "";
                        }

                        if (from_name.equals(to_name)) {
                            messageResult =
                                    from_name + ", " + from_number.getText() + " " + getString(R.string.to_message) + getString(R.string.on_city) +
                                            required_time;
                        } else {
                            messageResult =
                                    from_name + ", " + from_number.getText() + " " + getString(R.string.to_message) +
                                            to_name + ", " + to_number.getText() + "." +
                                            required_time;
                        }
                        Logger.d(context, TAG, "order: sendUrlMap.get(\"from_lat\")" + sendUrlMap.get("from_lat"));
                        Logger.d(context, TAG, "order: sendUrlMap.get(\"lat\")" + sendUrlMap.get("lat"));


                        String pay_method_message = getString(R.string.pay_method_message_main);
                        switch (pay_method) {
                            case "bonus_payment":
                                pay_method_message += " " + getString(R.string.pay_method_message_bonus);
                                break;
                            case "card_payment":
                            case "fondy_payment":
                            case "mono_payment":
                            case "wfp_payment":
                                pay_method_message += " " + getString(R.string.pay_method_message_card);
                                break;
                            default:
                                pay_method_message += " " + getString(R.string.pay_method_message_nal);
                        }

                        String messagePayment = orderWeb + " " + getString(R.string.UAH) + " " + pay_method_message;

//                        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
//                        String comment = stringList.get(2);
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


                        Bundle bundle = new Bundle();
                        bundle.putString("messageResult_key", messageResult);
                        bundle.putString("messagePay_key", messagePayment);
                        bundle.putString("messageCost_key", orderWeb);
                        bundle.putSerializable("sendUrlMap", new HashMap<>(sendUrlMap));
                        bundle.putString("UID_key", String.valueOf(sendUrlMap.get("dispatching_order_uid")));

// Установите Bundle как аргументы фрагмента
                        MainActivity.navController.navigate(R.id.nav_finish_separate, bundle, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_visicom, true)
                                .build());


                    } else {
                        constraintLayoutHomeFinish.setVisibility(GONE);
                        constraintLayoutHomeMain.setVisibility(VISIBLE);
                        assert message != null;
                        String addType = "60";
                        if (message.equals("ErrorMessage")) {
                            message = getString(R.string.server_error_connected);
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        } else if (message.contains("Дублирование")) {
                            sharedPreferencesHelperMain.saveValue("doubleOrderPrefHome", true);
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
                        } else {
                            switch (pay_method) {
                                case "bonus_payment":
                                case "card_payment":
                                case "fondy_payment":
                                case "mono_payment":
                                case "wfp_payment":
                                    changePayMethodToNal();
                                    break;
                                default:
                                    message = getResources().getString(R.string.error_message);
                                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                            }
                        }
                        btnVisible(VISIBLE);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    btnVisible(VISIBLE);
                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });
        }
    }

    private void changePayMethodToNal() {
        // Инфлейтим макет для кастомного диалога
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);

        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setView(dialogView);
        alertDialog.setCancelable(false);
        // Настраиваем элементы макета


        TextView messageTextView = dialogView.findViewById(R.id.dialog_message);
        String messagePaymentType = getString(R.string.to_nal_payment);
        messageTextView.setText(messagePaymentType);

        Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
        okButton.setOnClickListener(v -> {
            paymentType();

            try {
                if(orderRout()){
                    googleVerifyAccount();
                }
            } catch (UnsupportedEncodingException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                throw new RuntimeException(e);
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

    @SuppressLint("ResourceAsColor")
    private boolean orderRout() throws UnsupportedEncodingException {
        List<String> stringListRoutHome = logCursor(MainActivity.ROUT_HOME, context);
        progressBar.setVisibility(GONE);
        if (stringListRoutHome.get(1).equals(" ") && !textViewTo.getText().equals("")) {
            boolean stop = false;
            if (numberFlagFrom.equals("1") && from_number.getText().toString().equals(" ")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    from_number.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.selected_text_color)));
                    from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                    from_number.requestFocus();
                } else {
                    ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.selected_text_color)));
                    from_number.requestFocus();
                }
                stop = true;
            }
            if (numberFlagTo.equals("1") && to_number.getText().toString().equals(" ")) {
                to_number.setBackgroundTintList(ColorStateList.valueOf(R.color.selected_text_color));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    to_number.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.selected_text_color)));
                    to_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                    to_number.requestFocus();
                } else {
                    ViewCompat.setBackgroundTintList(to_number, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.selected_text_color)));
                    to_number.requestFocus();
                }
                stop = true;

            }
            if (stop) {
                return false;
            }

            if (numberFlagFrom.equals("1") && !from_number.getText().toString().equals(" ")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    from_number.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.edit)));
                    from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                    from_number.requestFocus();
                } else {
                    ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.edit)));
                    from_number.requestFocus();
                }

            }
            if (numberFlagTo.equals("1") && !to_number.getText().toString().equals(" ")) {
                to_number.setBackgroundTintList(ColorStateList.valueOf(R.color.selected_text_color));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    to_number.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.edit)));
                    to_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                    to_number.requestFocus();
                } else {
                    ViewCompat.setBackgroundTintList(to_number, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.edit)));
                    to_number.requestFocus();
                }


            }

            String from_numberCost;
            if (from_number.getText().toString().equals(" ")) {
                from_numberCost = " ";
            } else {

                from_numberCost = from_number.getText().toString();
            }
            String toCost, to_numberCost;
            if (to == null) {
                toCost = from;
                to_numberCost = from_number.getText().toString();
            } else {
                toCost = to;
                to_numberCost = to_number.getText().toString();
            }
            List<String> settings = new ArrayList<>();
            settings.add(from);
            settings.add(from_numberCost);
            settings.add(toCost);
            settings.add(to_numberCost);
            Logger.d(context, TAG, "order: settings" + settings);
            updateRoutHome(settings);
        }
        urlOrder = getTaxiUrlSearch( "orderOldClientCost", context);
        return true;
    }
    private void updateAddCost(String addCost) {
        ContentValues cv = new ContentValues();
        Logger.d(context, TAG, "updateAddCost: addCost" + addCost);
        cv.put("addCost", addCost);
        if(text_view_cost!= null && !text_view_cost.getText().toString().isEmpty()) {
            String startCost = text_view_cost.getText().toString();
            long finalCost= Long.parseLong(startCost) + Long.parseLong( addCost);
            text_view_cost.setText(String.valueOf( finalCost));
        }
        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }
    // Метод для запуска запроса разрешений
    private void requestPermissions() {
        String[] permissions = new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
                // другие нужные разрешения
        };
        permissionLauncher.launch(permissions);
    }

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        Logger.d(context, TAG, "checkPermission: " + permission);

        requestPermissions();
    }

    public static void btnVisible(int visible) {

        (MyApplication.getCurrentActivity()).runOnUiThread(() -> {
            if (text_view_cost != null) {
                schedule.setVisibility(visible);
                shed_down.setVisibility(visible);
                text_view_cost.setVisibility(visible);
                btn_clear.setVisibility(visible);
                btn_minus.setVisibility(visible);
                btn_plus.setVisibility(visible);
                buttonAddServices.setVisibility(visible);
                buttonBonus.setVisibility(visible);
                btn_clear.setVisibility(visible); // Note: btn_clear is set twice, consider removing duplicate
                btn_order.setVisibility(visible);
                if (visible == View.INVISIBLE) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    // Метод для сохранения количества запросов разрешений в SharedPreferences
    private void savePermissionRequestCount(int count) {
        sharedPreferencesHelperMain.saveValue(MainActivity.PERMISSION_REQUEST_COUNT_KEY, count);
    }

    // Метод для загрузки количества запросов разрешений из SharedPreferences
    private int loadPermissionRequestCount() {
        return (int) sharedPreferencesHelperMain.getValue(MainActivity.PERMISSION_REQUEST_COUNT_KEY, 0);
    }
    @Override
    public void onPause() {
        super.onPause();
        if(alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }
    private boolean verifyPhone() {

        List<String> stringList =  logCursor(MainActivity.TABLE_USER_INFO, requireActivity());

        String phone = stringList.get(2);

        Logger.d(requireActivity(), TAG, "onClick befor validate: ");
        String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
        Logger.d(requireActivity(), TAG, "onClick No validate: " + val);
        return val;
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onResume() {
        super.onResume();

        VisicomFragment.sendUrlMap = null;
        MainActivity.uid = null;

        Logger.d(context, "MainActivity.uid", "MainActivity.uid 6 " + MainActivity.uid);

        constraintLayoutHomeMain.setVisibility(VISIBLE);
        constraintLayoutHomeFinish.setVisibility(GONE);

        databaseHelper = new DatabaseHelper(context);
        databaseHelperUid = new DatabaseHelperUid(context);
        viewModel.setTransactionStatus(null);



        

        progressBar.setVisibility(GONE);
        pay_method =  logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);



        addCost = 0;
        updateAddCost(String.valueOf(addCost));
        btn_clear = binding.btnClear;


        SwipeRefreshLayout swipeRefreshLayout =binding.swipeRefreshLayout;
        TextView svButton = binding.svButton;

// Устанавливаем слушатель для распознавания жеста свайпа вниз
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Скрываем TextView (⬇️) сразу после появления индикатора свайпа
            svButton.setVisibility(GONE);

            // Выполняем необходимое действие (например, запуск новой активности)
            btn_clear.performClick();

            // После завершения обновления, уберите индикатор загрузки
            swipeRefreshLayout.setRefreshing(false);
            

            // Эмулируем окончание обновления с задержкой
            swipeRefreshLayout.postDelayed(() -> {
                // Отключаем индикатор загрузки
                swipeRefreshLayout.setRefreshing(false);

                // Показываем TextView (⬇️) снова после завершения обновления
                svButton.setVisibility(VISIBLE);
            }, 500); // Задержка 500 мс
        });


        textViewTo.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Фокус установлен на TextView, очищаем его
                textViewTo.setText("");
                to_number.setText("");
                to_number.setVisibility(INVISIBLE);
            }
        });
        List<String> stringListRoutHome = logCursor(MainActivity.ROUT_HOME, context);
        String valueAtIndex1 = stringListRoutHome.get(1);

        rout();


        if (valueAtIndex1 != null && !valueAtIndex1.equals(" ")) {
            textViewFrom.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // Фокус установлен на TextView, очищаем его
                    resetRoutHome();

                    MainActivity.navController.navigate(R.id.nav_home, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_visicom, true)
                            .build());
                }
            });
            costRoutHome(stringListRoutHome);
        } else {
            resetRoutHome();
            updateAddCost("0");
        }


        btn_clear.setOnClickListener(v -> {
            resetRoutHome();

            MainActivity.navController.navigate(R.id.nav_home, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());

        });

    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("LifecycleCheck 1", "Current lifecycle state: " + getViewLifecycleOwner().getLifecycle().getCurrentState());
        viewModel = new ViewModelProvider(requireActivity()).get(ExecutionStatusViewModel.class);

    }
    @SuppressLint("Range")
    private void rout() {

        textViewFrom.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateAddCost("0");

                if (textViewTo.getText().toString().isEmpty()) {
                        to = null;
                } else {
                    to = textViewTo.getText().toString();
                }
                if (numberFlagTo.equals("0")) {
                    to_number.setText(" ");
                }
                Logger.d(context, TAG, "onItemClick: to" + to);
                if(connected()) {
                    from = String.valueOf(adapter.getItem(position));
                    if (from.indexOf("/") != -1) {
                        from = from.substring(0,  from.indexOf("/"));
                    }
                    List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                    String city = stringList.get(1);
                    String api =  stringList.get(2);
                    baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                    String url = baseUrl + "/" + api + "/android/autocompleteSearchComboHid/" + from + "/" + city;

                    Map sendUrlMapCost = null;
                    try {
                        sendUrlMapCost = ResultSONParser.sendURL(url);
                    } catch (MalformedURLException | InterruptedException | JSONException e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        Logger.d(context, TAG, "AnrActivity.class 2" );
                        NavController navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
                        navController.navigate(R.id.nav_anr, null, new NavOptions.Builder()
                                .build());
//                        Intent intent = new Intent(context, AnrActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        context.startActivity(intent);
                    }
                    assert sendUrlMapCost != null;
                    String orderCost = (String) sendUrlMapCost.get("message");
                    switch (Objects.requireNonNull(orderCost)) {
                        case "200": {
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.error_firebase_start));
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                            break;
                        }
                        case "400": {
                            textViewFrom.setTextColor(RED);
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(address_error_message));
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                            break;
                        }
                        case "1":
                            from_number.setVisibility(VISIBLE);
                            from_number.requestFocus();
                            numberFlagFrom = "1";
                            from_number.setText("1");
                            cost();
                            from_number.setText(" ");
                            break;
                        case "0":
                            from_number.setText(" ");
                            from_number.setVisibility(INVISIBLE);
                            numberFlagFrom = "0";
                            cost();
                            break;
                        default:
                            Logger.d(context, TAG, "AnrActivity.class 3" );
                            NavController navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
                            navController.navigate(R.id.nav_anr, null, new NavOptions.Builder()
                                    .build());
//                            Intent intent = new Intent(context, AnrActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                            context.startActivity(intent);
                            Logger.d(context, TAG, "onItemClick: " + new IllegalStateException("Unexpected value: " + Objects.requireNonNull(orderCost)));
                    }
                    progressBar.setVisibility(GONE);

                } else {
                    progressBar.setVisibility(GONE);
                    MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());
                }

            }
        });

        from_number.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                if (!from_number.getText().toString().equals(" ")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        from_number.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.edit)));
                        from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                    } else {
                        ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.edit)));
                    }
               }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    from_number.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.edit)));
                    from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                } else {
                    ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.edit)));
                }
            }
        });
        textViewTo.setOnItemClickListener((parent, view, position, id) -> {

            MyBottomSheetErrorFragment bottomSheetDialogFragment;

            if (connected()) {
                updateAddCost("0");
                to = String.valueOf(adapter.getItem(position));
                if (to.indexOf("/") != -1) {
                    to = to.substring(0, to.indexOf("/"));
                }
                List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                String city = stringList.get(1);
                String api =  stringList.get(2);
                baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                String url = baseUrl + "/" + api + "/android/autocompleteSearchComboHid/" + to + "/" + city;

                Map sendUrlMapCost = null;
                try {
                    sendUrlMapCost = ResultSONParser.sendURL(url);
                } catch (MalformedURLException | InterruptedException | JSONException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    Toast.makeText(context, R.string.error_firebase_start, Toast.LENGTH_SHORT).show();
                }

                String orderCost = (String) sendUrlMapCost.get("message");
                Logger.d(context, TAG, "onItemClick: orderCost" + orderCost);
                switch (Objects.requireNonNull(orderCost)) {
                    case "200":
                        bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.error_firebase_start));
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        break;
                    case "400":
                        textViewTo.setTextColor(RED);
                        bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.address_error_message));
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        break;
                    case "1":
                        to_number.setVisibility(VISIBLE);
                        to_number.requestFocus();
                        numberFlagTo = "1";
                        to_number.setText("1");
                        cost();
                        to_number.setText(" ");
                        break;
                    case "0":
                        to_number.setText(" ");
                        to_number.setVisibility(INVISIBLE);
                        numberFlagTo = "0";
                        cost();
                        break;
                    default:
                        Logger.d(context, TAG, "onItemClick: " + new IllegalStateException("Unexpected value: " + Objects.requireNonNull(orderCost)));
                }
                if (textViewFrom.getText().toString().isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        textViewFrom.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.selected_text_color)));
                        textViewFrom.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                    } else {
                        ViewCompat.setBackgroundTintList(textViewFrom, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.selected_text_color)));
                    }

                }

        }
            else {
                MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
            }
    });

    }
//    @SuppressLint("ResourceAsColor")
//    private void cost() {
//
//        constr2.setVisibility(INVISIBLE);
//        textViewTo.setVisibility(VISIBLE);
//        binding.textwhere.setVisibility(VISIBLE);
//        binding.num2.setVisibility(VISIBLE);
//        btn_clear.setVisibility(VISIBLE);
//
//
//
//        from = textViewFrom.getText().toString();
//
//        if (numberFlagFrom.equals("1") && from_number.getText().toString().equals(" ")) {
//            setEditTextBackgroundTint(from_number, R.color.selected_text_color);
//            from_numberCost = "1";
//        } else {
//            if (numberFlagFrom.equals("0")) {
//                from_numberCost = " ";
//            } else {
//                from_numberCost = from_number.getText().toString();
//            }
//        }
//
//        if (numberFlagTo.equals("1") && to_number.getText().toString().equals(" ")) {
//            setEditTextBackgroundTint(to_number, R.color.selected_text_color);
//            to_numberCost = "1";
//        } else {
//            if (numberFlagTo.equals("0")) {
//                to_numberCost = " ";
//            } else {
//                to_numberCost = to_number.getText().toString();
//            }
//        }
//
//        Logger.d(context, TAG, "cost: numberFlagTo " + numberFlagTo);
//
//        if (to == null) {
//            toCost = from;
//            to_numberCost = from_numberCost;
//        } else {
//            toCost = to;
//        }
//        List<String> settings = new ArrayList<>();
//        String urlCost;
//        try {
//
//            settings.add(from);
//            settings.add(from_numberCost);
//            settings.add(toCost);
//            settings.add(to_numberCost);
//            updateRoutHome(settings);
//            urlCost = getTaxiUrlSearch("costSearchTime", context);
//
//            CostJSONParserRetrofit parser = new CostJSONParserRetrofit();
//            parser.sendURL(urlCost, new Callback<>() {
//                @Override
//                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
//                    Map<String, String> sendUrlMapCost = response.body();
//                    assert sendUrlMapCost != null;
//                    handleCostResponse(sendUrlMapCost);
//                }
//
//                @Override
//                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
//                    FirebaseCrashlytics.getInstance().recordException(t);
//                }
//            });
//
//        } catch (MalformedURLException | UnsupportedEncodingException e) {
//            resetRoutHome();
//            FirebaseCrashlytics.getInstance().recordException(e);
//            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                    .setPopUpTo(R.id.nav_restart, true)
//                    .build());
//        }
//    }
@SuppressLint("SetTextI18n")
private void cost() {

    constr2.setVisibility(INVISIBLE);
    textViewTo.setVisibility(VISIBLE);
    binding.textwhere.setVisibility(VISIBLE);
    binding.num2.setVisibility(VISIBLE);
    btn_clear.setVisibility(VISIBLE);

    from = textViewFrom.getText().toString();

    // FROM номер
    if (numberFlagFrom.equals("1") && from_number.getText().toString().equals(" ")) {
        setEditTextBackgroundTint(from_number, R.color.selected_text_color);
        from_numberCost = "1";
    } else {
        from_numberCost = numberFlagFrom.equals("0") ? " " : from_number.getText().toString();
    }

    // TO номер
    if (numberFlagTo.equals("1") && to_number.getText().toString().equals(" ")) {
        setEditTextBackgroundTint(to_number, R.color.selected_text_color);
        to_numberCost = "1";
    } else {
        to_numberCost = numberFlagTo.equals("0") ? " " : to_number.getText().toString();
    }

    Logger.d(context, TAG, "cost: numberFlagTo " + numberFlagTo);

    if (to == null) {
        toCost = from;
        to_numberCost = from_numberCost;
    } else {
        toCost = to;
    }

    try {
        List<String> settings = new ArrayList<>();
        settings.add(from);
        settings.add(from_numberCost);
        settings.add(toCost);
        settings.add(to_numberCost);
        updateRoutHome(settings);

        String urlCost = getTaxiUrlSearch("costSearchTime", context);

        // === WorkManager Start ===
        Data inputData = new Data.Builder()
                .putString("taskType", "costRequest")
                .putString("url", urlCost)
                .build();

        OneTimeWorkRequest costRequest = new OneTimeWorkRequest.Builder(RetrofitWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(costRequest);

        WorkManager.getInstance(requireContext())
                .getWorkInfoByIdLiveData(costRequest.getId())
                .observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        Data output = workInfo.getOutputData();

                        if (output.getString("error") != null) {
                            String error = output.getString("error");
                            FirebaseCrashlytics.getInstance().log("Ошибка costRequest: " + error);
                            resetRoutHome();
                            showErrorSheet(getString(R.string.server_error_connected));
                            return;
                        }

                        Map<String, String> resultMap = new HashMap<>();
                        resultMap.put("order_cost", output.getString("order_cost"));
                        resultMap.put("Message", output.getString("Message"));

                        handleCostResponse(resultMap);
                    }
                });
        // === WorkManager End ===

    } catch (UnsupportedEncodingException e) {
        resetRoutHome();
        FirebaseCrashlytics.getInstance().recordException(e);
        MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_restart, true)
                .build());
    }
}

    private void showErrorSheet(String message) {
        MyBottomSheetErrorFragment bottomSheet = new MyBottomSheetErrorFragment(message);
        bottomSheet.show(fragmentManager, bottomSheet.getTag());
    }

    @SuppressLint("SetTextI18n")
    private void handleCostResponse(Map<String, String> response) {
        progressBar.setVisibility(INVISIBLE);
        String message = response.get("Message");
        String orderCostStr = response.get("order_cost");

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, requireContext());
        long addCost = Long.parseLong(stringListInfo.get(5));

        assert orderCostStr != null;
        long orderCostLong = Long.parseLong(orderCostStr);

        boolean black_list_yes = verifyOrder();

        String orderCost = String.valueOf(orderCostLong + addCost);
        if(black_list_yes) {
            String black_list_city = sharedPreferencesHelperMain.getValue("black_list", "cache").toString();
            if(black_list_city.equals("cards only")) {
                orderCost = addCostBlackList(orderCost);
            }
        }
        if (!orderCost.equals("0")) {
            scheduleUpdate();


            String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
            long discountInt = Integer.parseInt(discountText);
            Logger.d(context, TAG, "discountInt: " + discountInt);
            cost = Long.parseLong(orderCost);
            Logger.d(context, TAG, "cost: " + cost);
            discount = cost * discountInt / 100;
            Logger.d(context, TAG, "discount: " + discount);
            cost += discount;

            updateAddCost(String.valueOf(discount));
            text_view_cost.setText(Long.toString(cost));

            costFirstForMin = cost;
            MIN_COST_VALUE = (long) (cost * 0.6);
            Logger.d(context, TAG, "cost: MIN_COST_VALUE "  + MIN_COST_VALUE);

            insertRouteCostToDatabase();
            btnVisible(VISIBLE);

        } else {
            resetRoutHome();

            assert message != null;
            if (message.equals("ErrorMessage")) {
                message = getString(R.string.server_error_connected);
            } else if (message.contains("Дублирование")) {
                message = getResources().getString(R.string.double_order_error);
            } else {
                switch (pay_method) {
                    case "bonus_payment":
                    case "card_payment":
                    case "fondy_payment":
                    case "mono_payment":
                    case "wfp_payment":
                        changePayMethodToNal();
                        break;
                    default:
                        message = getResources().getString(R.string.error_message);

                }
            }
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());



        }
    }

    private String addCostBlackList (String addcost) {

        int cost = Integer.parseInt(addcost); // Преобразуем строку в целое число
        cost += 45; // Увеличиваем на 45
        addcost = String.valueOf(cost); // Преобразуем обратно в строку

// Теперь addCost содержит новое значение
        return  addcost; // Вывод: "145"

    }



    private void insertRouteCostToDatabase() {
        AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, "app-database")
                .addMigrations(AppDatabase.MIGRATION_1_3)
                .build();
        RouteCostDao routeCostDao = db.routeCostDao();

        int routeId = routeIdToCheck;
        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif = stringListInfo.get(2);
        String payment_type = stringListInfo.get(4);
        String addCost = stringListInfo.get(5);

        executor.execute(() -> {
            RouteCost existingRouteCost = routeCostDao.getRouteCost(routeId);
            if (existingRouteCost == null) {
                RouteCost routeCost = new RouteCost();
                routeCost.routeId = routeId;
                routeCost.from = textViewFrom.getText().toString();
                routeCost.fromNumber = from_number.getText().toString();
                routeCost.to = textViewTo.getText().toString();
                routeCost.toNumber = to_number.getText().toString();
                routeCost.text_view_cost = text_view_cost.getText().toString();
                routeCost.tarif = tarif;
                routeCost.payment_type = payment_type;
                routeCost.addCost = addCost;
                routeCostDao.insert(routeCost);
            } else {
                existingRouteCost.from = textViewFrom.getText().toString();
                existingRouteCost.fromNumber = from_number.getText().toString();
                existingRouteCost.to = textViewTo.getText().toString();
                existingRouteCost.toNumber = to_number.getText().toString();
                existingRouteCost.text_view_cost = text_view_cost.getText().toString();
                existingRouteCost.tarif = tarif;
                existingRouteCost.payment_type = payment_type;
                existingRouteCost.addCost = addCost;
                routeCostDao.update(existingRouteCost);
            }
        });
    }





    private void setEditTextBackgroundTint(EditText editText, @ColorRes int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            editText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),colorResId)));
            editText.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
            editText.requestFocus();
        } else {
            ViewCompat.setBackgroundTintList(editText, ColorStateList.valueOf(ContextCompat.getColor(requireContext(),colorResId)));
            editText.requestFocus();
        }
    }


    @SuppressLint({"SetTextI18n", "StaticFieldLeak"})
    private void costRoutHome(final List<String> stringListRoutHome) {
       btnVisible(INVISIBLE);

        loadRouteCost(routeIdToCheck, stringListRoutHome);

    }


    private void loadRouteCost(int routeIdToCheck, final List<String> stringListRoutHome) {
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        progressBar.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            // Работа в фоновом потоке (аналог doInBackground)
            AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, "app-database")
                    .addMigrations(AppDatabase.MIGRATION_1_3)
                    .build();
            RouteCostDao routeCostDao = db.routeCostDao();
            RouteCost retrievedRouteCost = routeCostDao.getRouteCost(routeIdToCheck);

            List<RouteCost> allRouteCosts = routeCostDao.getAllRouteCosts();
            Logger.d(context, TAG, "All RouteCosts: " + allRouteCosts);

            Logger.d(context, TAG, "retrievedRouteCost" + retrievedRouteCost);
            // Возврат в главный поток (аналог onPostExecute)
            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                if (retrievedRouteCost != null) {
                    textViewFrom.setText(retrievedRouteCost.from);
                    from_number.setText(retrievedRouteCost.fromNumber);

                    textViewTo.setText(retrievedRouteCost.to);
                    to_number.setText(retrievedRouteCost.toNumber);

                    Logger.d(context, TAG, "onPostExecute: from_number.getText().toString()" + from_number.getText().toString());

                    if (!from_number.getText().toString().equals(" ")) {
                        from_number.setVisibility(View.VISIBLE);
                    }

                    Logger.d(context, TAG, "onPostExecute: retrievedRouteCost.toNumber/" + retrievedRouteCost.toNumber + "/");

                    if (!retrievedRouteCost.from.equals(retrievedRouteCost.to)) {
                        textViewTo.setVisibility(View.VISIBLE);
                        binding.textwhere.setVisibility(View.VISIBLE);
                        binding.num2.setVisibility(View.VISIBLE);
                        to_number.setText(" ");
                    }
                    if (!to_number.getText().toString().equals(" ")) {
                        to_number.setVisibility(View.VISIBLE);
                    } else {
                        to_number.setVisibility(View.INVISIBLE);
                    }

                    List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
                    long addCostforMin = Long.parseLong(stringListInfo.get(5));
                    Logger.d(context, TAG, "onPostExecute: addCostforMin" + addCostforMin);
                    MIN_COST_VALUE = (long) ((Long.parseLong(retrievedRouteCost.text_view_cost) - addCostforMin) * 0.6);
                    Logger.d(context, TAG, "onPostExecute: MIN_COST_VALUE" + MIN_COST_VALUE);
                    btnVisible(View.VISIBLE);
                } else {
                    Logger.d(context, TAG, "AnrActivity.class 1" );
                    NavController navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
                    navController.navigate(R.id.nav_anr, null, new NavOptions.Builder()
                            .build());
//                    Intent intent = new Intent(context, AnrActivity.class);
//                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    context.startActivity(intent);
                }

                updateAddCost("0");
                updateUIFromList(stringListRoutHome);
                scheduleUpdate();
            });
        });
    }

    private void updateUIFromList(List<String> stringListRoutHome) {
        textViewFrom.setText(stringListRoutHome.get(1));

        if (!stringListRoutHome.get(2).equals(" ")) {
            from_number.setVisibility(VISIBLE);
            from_number.setText(stringListRoutHome.get(2));
        }
        if (!stringListRoutHome.get(4).equals(" ")) {
            to_number.setText(stringListRoutHome.get(4));
            to_number.setVisibility(VISIBLE);
        }
        if (!stringListRoutHome.get(1).equals(stringListRoutHome.get(3))) {
            textViewTo.setText(stringListRoutHome.get(3));
            textViewTo.setVisibility(VISIBLE);
            binding.textwhere.setVisibility(VISIBLE);
            binding.num2.setVisibility(VISIBLE);
        } else {
            to_number.setVisibility(INVISIBLE);
        }

        textViewTo.setVisibility(VISIBLE);
        binding.textwhere.setVisibility(VISIBLE);
        binding.num2.setVisibility(VISIBLE);

        text_view_cost.setVisibility(VISIBLE);
        btn_minus.setVisibility(VISIBLE);
        btn_plus.setVisibility(VISIBLE);
        buttonAddServices.setVisibility(VISIBLE);
        buttonBonus.setVisibility(VISIBLE);
        btn_clear.setVisibility(VISIBLE);

        btn_order.setVisibility(VISIBLE);

        String urlCost;
        String message;
        try {

            urlCost = getTaxiUrlSearch("costSearchTime", context);
            List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, requireContext());
            long addCost = Long.parseLong(stringListInfo.get(5));
            String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
            
            CostJSONParserRetrofit parser = new CostJSONParserRetrofit();
            parser.sendURL(urlCost, new Callback<Map<String, String>>() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    Map<String, String> sendUrlMapCost = response.body();
                    assert sendUrlMapCost != null;
                    String orderCost = sendUrlMapCost.get("order_cost");
                    Logger.d(context, TAG, "costRoutHome:orderCost " + orderCost);
//
//
//                    assert orderCostStr != null;
//                    long orderCostLong = Long.parseLong(orderCostStr);
//                    String orderCost = String.valueOf(orderCostLong);

                    String message = sendUrlMapCost.get("Message");
                    String addType ="60";
                    assert orderCost != null;
                    if (orderCost.equals("0")) {
                        constraintLayoutHomeFinish.setVisibility(GONE);
                        constraintLayoutHomeMain.setVisibility(VISIBLE);
                        assert message != null;
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
                        }else if (message.equals("ErrorMessage")) {
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
                                    changePayMethodToNal();
                                    break;
                                default:
                                    resetRoutHome();
                                    message = getResources().getString(R.string.error_message);
                                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                            }

                        }
                    } else  {
                        scheduleUpdate();
//                        text_view_cost.setVisibility(VISIBLE);
//                        btn_minus.setVisibility(VISIBLE);
//                        btn_plus.setVisibility(VISIBLE);
//                        buttonAddServices.setVisibility(VISIBLE);
//                        buttonBonus.setVisibility(VISIBLE);
//                        btn_order.setVisibility(VISIBLE);
//                        btn_clear.setVisibility(VISIBLE);


                        long discountInt = Integer.parseInt(discountText);
                        Logger.d(context, TAG, "costRoutHome:discountInt " + discountInt);

                        cost = Long.parseLong(orderCost);
                        discount = cost * discountInt / 100;
                        cost = cost + discount;
                        updateAddCost(String.valueOf(discount));
                        text_view_cost.setText(Long.toString(cost));
                        Logger.d(context, TAG, "costRoutHome:Long.toString(cost) " + cost);
                        costFirstForMin = cost;
                        MIN_COST_VALUE = (long) (cost * 0.6);
                        btnVisible(VISIBLE);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });

        } catch (MalformedURLException | UnsupportedEncodingException e) {
            resetRoutHome();
            FirebaseCrashlytics.getInstance().recordException(e);
            message = getString(R.string.error_message);
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
        }
    }



    private boolean verifyOrder() {
        return (boolean) sharedPreferencesHelperMain.getValue("verifyUserOrder", false);
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    public static ArrayList<Map> routMaps(Context context) {
        Map <String, String> routs;
        ArrayList<Map> routsArr = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(MainActivity.TABLE_ORDERS_INFO, null, null, null, null, null, null);
        int i = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    routs = new HashMap<>();
                    routs.put("id", c.getString(c.getColumnIndexOrThrow ("id")));
                    routs.put("from_street", c.getString(c.getColumnIndexOrThrow ("from_street")));
                    routs.put("from_number", c.getString(c.getColumnIndexOrThrow ("from_number")));
                    routs.put("to_street", c.getString(c.getColumnIndexOrThrow ("to_street")));
                    routs.put("to_number", c.getString(c.getColumnIndexOrThrow ("to_number")));
                    routsArr.add(i++, routs);
                } while (c.moveToNext());
            }
        }
        database.close();
        Logger.d(context, TAG, "routMaps: " + routsArr);
        return routsArr;
    }

    private boolean connected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        if (capabilities == null) return false;

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH);
    }


    public void updateRoutHome(List<String> settings) {
        ContentValues cv = new ContentValues();

        cv.put("from_street",  settings.get(0));
        cv.put("from_number", settings.get(1));
        cv.put("to_street", settings.get(2));
        cv.put("to_number", settings.get(3));

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_HOME, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }
    public static void  addCheck(Context context) {

        int newCheck = 0;
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        for (int i = 0; i < DataArr.arrayServiceCode().length; i++) {
            if(services.get(i+1).equals("1")) {
                newCheck++;
            }
        }

//        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
//        String comment = stringList.get(2);
        String comment = sharedPreferencesHelperMain.getValue("comment", "no_comment").toString();
        Logger.d(context, TAG, "comment" + comment);



        if (!comment.equals("no_comment")) {
            newCheck++;
        }

        String mes = context.getString(R.string.add_services);
        if(newCheck != 0) {
            mes = context.getString(R.string.add_services) + " (" + newCheck + ")";
        }
        buttonAddServices.setText(mes);

    }
    public void resetRoutHome() {
        ContentValues cv = new ContentValues();

        cv.put("from_street", " ");
        cv.put("from_number", " ");
        cv.put("to_street", " ");
        cv.put("to_number", " ");

        // обновляем по id

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_HOME, cv, "id = ?",
                new String[] { "1" });


        database.close();

        updateAddCost("0");

        paymentType();
        text_view_cost.setVisibility(INVISIBLE);
        btn_minus.setVisibility(INVISIBLE);
        btn_plus.setVisibility(INVISIBLE);
        buttonAddServices.setVisibility(INVISIBLE);
        buttonBonus.setVisibility(INVISIBLE);
        textViewFrom.setText("");
        from_number.setText("");
        from_number.setVisibility(INVISIBLE);
        textViewTo.setText("");
        textViewTo.setVisibility(INVISIBLE);
        btn_clear.setVisibility(INVISIBLE);
        binding.textTo.setVisibility(INVISIBLE);
        binding.num2.setVisibility(INVISIBLE);
        binding.textwhere.setVisibility(INVISIBLE);
        to_number.setVisibility(INVISIBLE);
    }


    private String getTaxiUrlSearch(String urlAPI, Context context) throws UnsupportedEncodingException {
        Logger.d(context, TAG, "startCost: discountText" + logCursor(MainActivity.TABLE_SETTINGS_INFO, context));

        List<String> stringListRout = logCursor(MainActivity.ROUT_HOME, context);
        Logger.d(context, TAG, "getTaxiUrlSearch: stringListRout" + stringListRout);

        String originalString = stringListRout.get(1);
        int indexOfSlash = originalString.indexOf("/");
        String from = (indexOfSlash != -1) ? originalString.substring(0, indexOfSlash) : originalString;

        String from_number = stringListRout.get(2);

        originalString = stringListRout.get(3);
        indexOfSlash = originalString.indexOf("/");
        String to = (indexOfSlash != -1) ? originalString.substring(0, indexOfSlash) : originalString;

        String to_number = stringListRout.get(4);


        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
//        String comment = stringList.get(2);
        String date = stringList.get(3);
        String comment = sharedPreferencesHelperMain.getValue("comment", "no_comment").toString();

        // Origin of route
        String str_origin = from + "/" + from_number;

        // Destination of route
        String str_dest = to + "/" + to_number;

        //City Table
        List<String> stringListCity = logCursor(MainActivity.CITY_INFO, context);
        String city = stringListCity.get(1);
        String api =  stringListCity.get(2);

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);


        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, requireContext());

        String tarif =  stringListInfo.get(2);
        String payment_type =  stringListInfo.get(4);
        String addCost = stringListInfo.get(5);

        Logger.d(context, TAG, "startCost: discountText" + discount);

        Logger.d(context, TAG, "getTaxiUrlSearch: addCost11111" + addCost);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);
        boolean black_list_yes = verifyOrder();

        if(black_list_yes) {
            payment_type = "wfp_payment";
            ContentValues cv = new ContentValues();
            cv.put("payment_type", payment_type);
            // обновляем по id
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                    new String[] { "1" });

            buttonBonus.setText(context.getString(R.string.card_payment));
            buttonBonus.setOnClickListener(v -> {
                String message = context.getString(R.string.black_list_message_err);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            });
        }
        if(urlAPI.equals("costSearchTime")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }

            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail  + "*" + payment_type+ "/"
                    + time + "/" + date ;
        }


        if(urlAPI.equals("orderOldClientCost")) {
            String wfpInvoice = "*";
            if(payment_type.equals("wfp_payment")) {
                String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS);
                Logger.d(context, TAG, "payWfp: rectoken " + rectoken);

                if (!rectoken.isEmpty()) {
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    wfpInvoice = MainActivity.order_id;
                }
            }

            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);


            String paramsUserArr = displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail + "*" + payment_type;

            if(black_list_yes) {
                String lastCharacter = phoneNumber.substring(phoneNumber.length() - 1); // Получаем последний знак
                phoneNumber = phoneNumber.substring(0, phoneNumber.length() - 1); // Часть без последнего знака
                phoneNumber = phoneNumber.replace(" ", ""); // удаляем пробелы
                comment = "цифра номера" + " " +  lastCharacter + ". Оплатили службе 45грн. " + comment;
                addCost = addCostBlackList(addCost);
            }
            boolean doubleOrder = (boolean) sharedPreferencesHelperMain.getValue("doubleOrderPrefHome", false);
            if(doubleOrder) {
                paramsUserArr = displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail + "*" + payment_type + "*" + "doubleOrder";
                sharedPreferencesHelperMain.saveValue("doubleOrderPrefHome", false);
            }
            String clientCost = text_view_cost.getText().toString();
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + clientCost  + "/" + paramsUserArr + "/" + addCost + "/" + time + "/" + comment + "/" + date + "/" + wfpInvoice;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
//            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });

        }

        // Building the url to the web service
// Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i <= 14 ; i++) {
            if(services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if(servicesVer) {
            for (int i = 0; i < arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if(servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Logger.d(context, TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }


        String url = "/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);

        Logger.d(context, TAG, "getTaxiUrlSearch: " + url);

        database.close();

        return url;
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
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (paymentType) {
                    case "bonus_payment":
                        if (Long.parseLong(bonus_max_pay) <= Long.parseLong(textCost) * 100) {
                            paymentType();
                        }
                        break;
                    case "card_payment":
                    case "fondy_payment":
                    case "mono_payment":
                    case "wfp_payment":
                        if (Long.parseLong(card_max_pay) <= Long.parseLong(textCost)) {
                            paymentType();
                        }
                        break;
                }

                try {
                    if (orderRout()) {
                        googleVerifyAccount();
                    }
                } catch (UnsupportedEncodingException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    throw new RuntimeException(e);
                }

                progressBar.setVisibility(GONE);
                alertDialog.dismiss();
            }
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeFragment.progressBar.setVisibility(GONE);
                progressBar.setVisibility(GONE);
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    @SuppressLint("Range")
    private static List<String> logCursor(String table, Context context) {
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


    private void paymentType() {
        ContentValues cv = new ContentValues();
        cv.put("payment_type", "nal_payment");
        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
        pay_method = "nal_payment";
    }
    private void fetchRoutesCancel() {
        Logger.d(context, TAG, "fetchRoutesCancel: ");
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        if (!userEmail.equals("email"))
        {
            databaseHelper.clearTable();

            databaseHelperUid.clearTableUid();

            routeListCancel = new ArrayList<>();

//            String baseUrl = "https://m.easy-order-taxi.site";
            String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

            List<String> stringList = logCursor(MainActivity.CITY_INFO,context);
            String city = stringList.get(1);
            baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
            String url = baseUrl + "/android/UIDStatusShowEmailCancelApp/" + userEmail + "/" + city + "/" +  context.getString(R.string.application);

            Call<List<RouteResponseCancel>> call = ApiClient.getApiService().getRoutesCancel(url);
            Logger.d(context, TAG, "fetchRoutesCancel: " + url);
            call.enqueue(new Callback<List<RouteResponseCancel>>() {
                @Override
                public void onResponse(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Response<List<RouteResponseCancel>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<RouteResponseCancel> routes = response.body();
                        assert routes != null;
                        Logger.d(context, TAG, "onResponse: " + routes.toString());
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

        String closeReasonText = getString(R.string.close_resone_def);

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
                    // оставляем closeReasonText как есть
                    break;
            }


            if (routeFrom.equals("Місце відправлення")) {
                routeFrom = getString(R.string.start_point_text);
            }

            if (routeTo.equals("Точка на карте")) {
                routeTo = getString(R.string.end_point_marker);
            }
            if (routeTo.contains("по городу")) {
                routeTo = getString(R.string.on_city);
            }
            if (routeTo.contains("по місту")) {
                routeTo = getString(R.string.on_city);
            }
            String routeInfo = "";

            if (auto == null) {
                auto = "??";
            }

            String required_time_text = "";
            if (required_time != null && !required_time.contains("1970-01-01")) {
                try {

                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

                    // Преобразуем строку required_time в Date
                    Date date = outputFormat.parse(required_time);

                    // Преобразуем Date в строку нужного формата
                    assert date != null;
                    required_time_text = " " + context.getString(R.string.time_order) + " " + outputFormat.format(date) + ".";

                } catch (ParseException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    required_time_text = ""; // Если ошибка парсинга, задаём пустое значение
                }
            }

            if (routeFrom.equals(routeTo)) {
                routeInfo = routeFrom + " " + routefromnumber
                        + getString(R.string.close_resone_to)
                        + getString(R.string.on_city)
                        + required_time_text  + "#"
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)  + "#"
                        + getString(R.string.auto_info) + " " + auto + "#"
                        + getString(R.string.close_resone_time)
                        + createdAt  + "#"
                        + getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = routeFrom + " " + routefromnumber
                        + getString(R.string.close_resone_to) + routeTo + " " + routeTonumber + "."
                        + required_time_text + "#"
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)  + "#"
                        + getString(R.string.auto_info) + " " + auto + "#"
                        + getString(R.string.close_resone_time) + createdAt  + "#"
                        + getString(R.string.close_resone_text) + closeReasonText;
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
            String message = getString(R.string.order_to_cancel_true);
            MyBottomSheetErrorFragment myBottomSheetMessageFragment = new MyBottomSheetErrorFragment(message);
            myBottomSheetMessageFragment.show(fragmentManager, myBottomSheetMessageFragment.getTag());
        } else {
            databaseHelper.clearTableCancel();
            databaseHelperUid.clearTableCancel();
        }
    }

    private void showAddCostDoubleDialog(String addType) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        int  dialogViewInt = R.layout.dialog_add_cost;
        switch(addType) {
            case "60":
                dialogViewInt = R.layout.dialog_add_60_cost;
                break;
            case "45":
                break;
        }
        View dialogView = inflater.inflate(dialogViewInt, null);


        String title = getString(R.string.double_order);
        String message = getString(R.string.add_cost_fin_60);
        String numberIndexString = "";

        switch(addType) {
            case "60":
                title = getString(R.string.double_order);

                Button minus = dialogView.findViewById(R.id.btn_minus);
                Button plus = dialogView.findViewById(R.id.btn_plus);
                EditText discinp = dialogView.findViewById(R.id.discinp);

                minus.setOnClickListener(v -> {
                    String addCost = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(5);
                    int addCostInt = Integer.parseInt(addCost);
                    if(addCostInt >= 5) {
                        addCostInt -=5;
                        updateAddCost(String.valueOf(addCostInt));
                        discinp.setText(String.valueOf(addCostInt + 60));
                    }

                });

                plus.setOnClickListener(v -> {
                    String addCost = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(5);
                    int addCostInt = Integer.parseInt(addCost);
                    addCostInt +=5;
                    updateAddCost(String.valueOf(addCostInt));
                    discinp.setText(String.valueOf(addCostInt + 60));
                });

                message = getString(R.string.add_cost_fin_60);
                numberIndexString = message;
                break;
            case "45":
                title = getString(R.string.black_list);
                message = getString(R.string.add_cost_fin_45);
                numberIndexString = "45";
                blockUserBlackList();
                break;
        }
        TextView titleView = dialogView.findViewById(R.id.dialogTitle);
        titleView.setText(title);

        TextView messageView = dialogView.findViewById(R.id.dialogMessage);

        SpannableStringBuilder spannable = new SpannableStringBuilder(message);

        int numberIndex = message.indexOf(numberIndexString);
        int length = numberIndexString.length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), numberIndex, numberIndex + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        messageView.setText(spannable);

        builder.setView(dialogView)
                .setCancelable(false)
                .setPositiveButton(R.string.ok_button, (dialog, which) -> {
                    dialog.dismiss();

                    switch (addType) {
                        case "60":
                            createDoubleOrder();
                            break;
                        case "45":
                            createBlackList();
                            break;
                    }

                })
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.show();

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

    private void createBlackList() {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
                .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
                .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        PayApi apiService = retrofit.create(PayApi.class);
        Call<ResponsePaySystem> call = apiService.getPaySystem();
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponsePaySystem> call, @NonNull Response<ResponsePaySystem> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Обработка успешного ответа
                    ResponsePaySystem responsePaySystem = response.body();
                    assert responsePaySystem != null;
                    String paymentCode = responsePaySystem.getPay_system();
                    switch (paymentCode) {
                        case "wfp":
                            pay_method = "wfp_payment";
                            break;
                        case "fondy":
                            pay_method = "fondy_payment";
                            break;
                        case "mono":
                            pay_method = "mono_payment";
                            break;
                    }
                    if (isAdded()) {
                        ContentValues cv = new ContentValues();
                        cv.put("payment_type", pay_method);
                        // обновляем по id
                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                new String[]{"1"});
                        database.close();

                        try {
                            orderRout();
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                        googleVerifyAccount();
                    }


                } else {
                    if (isAdded()) { //
                        MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_restart, true)
                                .build());
                    }

                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponsePaySystem> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                if (isAdded()) { //
                    MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());
                }
            }
        });





    }
    public void createDoubleOrder() {

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);

        String addCost = stringListInfo.get(5);
        int cost = Integer.parseInt(addCost);
        cost += 60;
        addCost = String.valueOf(cost);
        updateAddCost(addCost);

        try {
            orderRout();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        googleVerifyAccount();
    }

    private void googleVerifyAccount() {
        FirebaseConsentManager consentManager = new FirebaseConsentManager(context);

        consentManager.checkUserConsent(new FirebaseConsentManager.ConsentCallback() {
            @Override
            public void onConsentValid() {
                Logger.d(context, TAG, "Согласие пользователя действительное.");
                new Handler(Looper.getMainLooper()).post(() -> {
                    orderFinished();
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

}