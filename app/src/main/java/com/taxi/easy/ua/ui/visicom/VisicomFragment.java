package com.taxi.easy.ua.ui.visicom;


import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.activeCalls;
import static com.taxi.easy.ua.MainActivity.viewModel;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavOptions;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.FragmentVisicomBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import com.taxi.easy.ua.ui.payment_system.PayApi;
import com.taxi.easy.ua.ui.payment_system.ResponsePaySystem;
import com.taxi.easy.ua.ui.visicom.visicom_search.ActivityVisicomOnePage;
import com.taxi.easy.ua.utils.animation.car.CarProgressBar;
import com.taxi.easy.ua.utils.auth.FirebaseConsentManager;
import com.taxi.easy.ua.utils.blacklist.BlacklistManager;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetBonusFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGPSFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGeoFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyPhoneDialogFragment;
import com.taxi.easy.ua.utils.city.CityFinder;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.cost_json_parser.CostJSONParserRetrofit;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.download.AppUpdater;
import com.taxi.easy.ua.utils.from_json_parser.FromJSONParserRetrofit;
import com.taxi.easy.ua.utils.ip.RetrofitClient;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;
import com.taxi.easy.ua.utils.user.user_verify.VerifyUserTask;
import com.uxcam.UXCam;

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
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class VisicomFragment extends Fragment {

    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    private FragmentVisicomBinding binding;
    private static final String TAG = "VisicomFragment";
 

    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btn_minus, btn_plus, btnOrder, buttonBonus, gpsbut, btnCallAdmin, btnCallAdminFin;
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
    public static AppCompatButton btnAdd, ubt_btn;
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
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 123;

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

    private Animation blinkAnimation;
//    private final String baseUrl = "https://m.easy-order-taxi.site";
    private static String baseUrl;
    private final int MY_REQUEST_CODE = 1234;
    private CarProgressBar carProgressBar;
    @SuppressLint("StaticFieldLeak")
    static TextView svButton;

    public static  long startCost;
    public static  long finalCost;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        binding = FragmentVisicomBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Включаем блокировку кнопки "Назад" Применяем блокировку кнопки "Назад"
        BackPressBlocker backPressBlocker = new BackPressBlocker();
        backPressBlocker.setBackButtonBlocked(true);
        backPressBlocker.blockBackButtonWithCallback(this);

        constraintLayoutVisicomMain = root.findViewById(R.id.visicomMain);
        constraintLayoutVisicomFinish = root.findViewById(R.id.visicomFinish);

        constraintLayoutVisicomFinish.setVisibility(View.GONE);


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
            svButton.setVisibility(View.GONE);

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

        gpsbut = binding.gpsbut;

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
                constraintLayoutVisicomFinish.setVisibility(View.GONE);
                constraintLayoutVisicomMain.setVisibility(VISIBLE);
//                requireActivity().onBackPressed(); // Возвращаемся назад
            }
        });

        return root;
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
            MyBottomSheetGeoFragment bottomSheetDialogFragment = new MyBottomSheetGeoFragment(text_view_cost);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
        });

        shed_down.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            MyBottomSheetGeoFragment bottomSheetDialogFragment = new MyBottomSheetGeoFragment(text_view_cost);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
        });
    }

    public void updateApp() {
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(context);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Доступны обновления
                showUpdateDialog();
                Logger.d(requireActivity(), TAG, "Available updates found");
            }
        });


    }


    public void showUpdateDialog() {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.upd_available)
                .setMessage(R.string.upd_available_promt)
                .setCancelable(false) // Запрет закрытия диалога при нажатии вне его
                .setPositiveButton(R.string.upd_available_ok, (dialog, which) -> {
                    // Код для начала обновления
                    AppUpdater appUpdater = new AppUpdater(requireActivity());
                    appUpdater.startUpdate();
                })
                .setNegativeButton(R.string.upd_available_cancel, (dialog, which) -> {
                    // Просто закрываем диалог
                    dialog.dismiss();
                })
                .show();
    }

    public static void addCheck(Context context) {
        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif = stringListInfo.get(2);
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
        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String comment = stringList.get(2);
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


    public static void btnVisible(int visible) {
        if (text_view_cost != null) {
            if (visible == View.INVISIBLE) {
                progressBar.setVisibility(VISIBLE);
            } else {
                progressBar.setVisibility(View.GONE);
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

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        Logger.d(context, TAG, "checkPermission: " + permission);
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(context, new String[]{permission}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Logger.d(context, TAG, "onRequestPermissionsResult: " + requestCode);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (permissions.length > 0) {
                SharedPreferences.Editor editor = MainActivity.sharedPreferences.edit();
                for (int i = 0; i < permissions.length; i++) {
                    editor.putInt(permissions[i], grantResults[i]);

                }
                editor.apply();

                int permissionRequestCount = loadPermissionRequestCount();

                // Увеличение счетчика запросов разрешений при необходимости
                permissionRequestCount++;

                // Сохранение обновленного значения счетчика
                savePermissionRequestCount(permissionRequestCount);
                Log.d("loadPermission", "permissionRequestCount: " + permissionRequestCount);
                // Далее вы можете загрузить сохраненные разрешения и их результаты в любом месте вашего приложения,
                // используя тот же самый объект SharedPreferences
            }
        }
    }


    // Метод для сохранения количества запросов разрешений в SharedPreferences
    private void savePermissionRequestCount(int count) {
        SharedPreferences.Editor editor = MainActivity.sharedPreferencesCount.edit();
        editor.putInt(MainActivity.PERMISSION_REQUEST_COUNT_KEY, count);
        editor.apply();
    }

    // Метод для загрузки количества запросов разрешений из SharedPreferences
    private int loadPermissionRequestCount() {
        return MainActivity.sharedPreferencesCount.getInt(MainActivity.PERMISSION_REQUEST_COUNT_KEY, 0);
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

    @SuppressLint("Range")
    public static String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: " + urlAPI);

        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery(query, null);

        cursor.moveToFirst();

        // Получите значения полей из первой записи

        double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
        double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
        double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
        double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
        String start = cursor.getString(cursor.getColumnIndex("start"));
        String finish = cursor.getString(cursor.getColumnIndex("finish"));
        if (finish.equals(context.getString(R.string.on_city_tv))) {
            finish = start;
        }
        if (originLatitude == toLatitude) {
            textViewTo.setText(context.getString(R.string.on_city_tv));
        }
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: start " + start);
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: finish " + finish);

        // Заменяем символ '/' в строках
        if (start != null) {
            start = start.replace("/", "|");
        }
        if (finish != null) {
            finish = finish.replace("/", "|");
        }
        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;

        cursor.close();


        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
        String comment = stringList.get(2);
        String date = stringList.get(3);

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif = stringListInfo.get(2);
        String payment_type = stringListInfo.get(4);


        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);

        boolean black_list_yes = verifyOrder(context);

        Logger.d(context, TAG, "black_list_yes 1 " + black_list_yes);
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


        if (urlAPI.equals("costSearchMarkersTime")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }

            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail + "*" + payment_type + "/"
                    + time + "/" + date;
        }
        if (urlAPI.equals("orderSearchMarkersVisicomWfpInvoiceChannel")) {

            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: startCost " + startCost);
            Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: finalCost " + finalCost);

            long addCostInt = finalCost - startCost;
            String addCost = String.valueOf(addCostInt);
            Logger.d(context, TAG, "getTaxiUrlSearchMarkers: addCost " + addCost);

            String wfpInvoice = "*";
            if(payment_type.equals("wfp_payment")) {
                String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS, context);
                Logger.d(context, TAG, "payWfp: rectoken " + rectoken);

                if (!rectoken.isEmpty()) {
                    MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(context);
                    wfpInvoice = MainActivity.order_id;
                }
            }
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
            String lastCharacter;

            String paramsUserArr = displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail + "*" + payment_type;

            if(black_list_yes) {
                lastCharacter = phoneNumber.substring(phoneNumber.length() - 1); // Получаем последний знак
                phoneNumber = phoneNumber.substring(0, phoneNumber.length() - 1); // Часть без последнего знака
                phoneNumber = phoneNumber.replace(" ", ""); // удаляем пробелы
                comment = "цифра номера" + " " + lastCharacter + ", Оплатили службе 45грн. " + comment;
                addCost = addCostBlackList(addCost);

            }

            boolean doubleOrder = (boolean) sharedPreferencesHelperMain.getValue("doubleOrderPref", false);
            if(doubleOrder) {
                paramsUserArr = displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail + "*" + payment_type + "*" + "doubleOrder";
                sharedPreferencesHelperMain.saveValue("doubleOrderPref", false);
            }

            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + paramsUserArr + "/" + addCost + "/"
                    + time + "/" + comment + "/" + date + "/" + start + "/" + finish + "/" + wfpInvoice;


            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
//            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[]{"1"});

        }

        // Building the url to the web service
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
            for (int i = 0; i < OpenStreetMapActivity.arrayServiceCode().length; i++) {
                if (services.get(i + 1).equals("1")) {
                    servicesChecked.add(OpenStreetMapActivity.arrayServiceCode()[i]);
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
    private static String getCheckRectoken(String table, Context context) {
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
    public static void readTariffInfo(Context context) {
        // Создаем экземпляр класса для работы с базой данных

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif = stringListInfo.get(2);
        switch (tarif) {
            case "Базовый":
                frame_1.setBackgroundResource(R.drawable.input);
                frame_2.setBackgroundResource(R.drawable.buttons);
                frame_3.setBackgroundResource(R.drawable.buttons);
                break;
            case "Универсал":
                    frame_1.setBackgroundResource(R.drawable.buttons);
                    frame_2.setBackgroundResource(R.drawable.input);
                    frame_3.setBackgroundResource(R.drawable.buttons);
                    break;
            case "Микроавтобус":
                    frame_1.setBackgroundResource(R.drawable.buttons);
                    frame_2.setBackgroundResource(R.drawable.buttons);
                    frame_3.setBackgroundResource(R.drawable.input);
                    break;
            default:
                frame_1.setBackgroundResource(R.drawable.buttons);
                frame_2.setBackgroundResource(R.drawable.buttons);
                frame_3.setBackgroundResource(R.drawable.buttons);
        }

        btn1.setOnClickListener(v -> {
            progressBar.setVisibility(VISIBLE);

            ContentValues cv = new ContentValues();
            cv.put("tarif", "Базовый");

            // обновляем по id
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                    new String[]{"1"});
            database.close();

            try {
                String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                if (!userEmail.equals("email")) {
                    visicomCost(context);
                    readTariffInfo(context);
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
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

                // обновляем по id
                SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                        new String[]{"1"});
                database.close();

                try {
                    String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                    if (!userEmail.equals("email")) {
                        visicomCost(context);
                        readTariffInfo(context);
                    }
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
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

                // обновляем по id
                SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                        new String[]{"1"});
                database.close();

                try {
                    String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                    if (!userEmail.equals("email")) {
                        visicomCost(context);
                        readTariffInfo(context);
                    }
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                frame_1.setBackgroundResource(R.drawable.buttons);
                frame_2.setBackgroundResource(R.drawable.buttons);
                frame_3.setBackgroundResource(R.drawable.input);

                addCheck(context);
            }
        });

    }

    @SuppressLint("ResourceAsColor")
    public static boolean orderRout() {
        urlOrder = getTaxiUrlSearchMarkers("orderSearchMarkersVisicomWfpInvoiceChannel", MyApplication.getContext());
        Logger.d(MyApplication.getContext(), TAG, "order:  urlOrder " + urlOrder);
        return true;
    }

    public void orderFinished() throws MalformedURLException {
        if (!verifyPhone()) {
            MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment(context, "visicom");
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            progressBar.setVisibility(View.GONE);
        } else {

                constraintLayoutVisicomMain.setVisibility(View.GONE);
                String messageResult =
                        geoText.getText().toString() + " " + getString(R.string.to_message) +
                                textViewTo.getText() + ".";

                text_full_message.setText(messageResult);

                messageResult = context.getString(R.string.check_cost_message);
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
                constraintLayoutVisicomFinish.setVisibility(VISIBLE);


                ToJSONParserRetrofit parser = new ToJSONParserRetrofit();
                baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                Logger.d(context, TAG, "orderFinished: " + baseUrl + urlOrder);

                parser.sendURLChannel(urlOrder, new Callback<>() {

                    @Override
                    public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, String> sendUrlMap = response.body();

                            assert sendUrlMap != null;
                            handleOrderFinished(sendUrlMap, pay_method, context);
                        } else {
                            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_restart, true)
                                    .build());
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                        FirebaseCrashlytics.getInstance().recordException(t);
                        MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
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

        boolean visicomBackPressed = (boolean) sharedPreferencesHelperMain.getValue("VisicomBackPressed", false);

        if (!"0".equals(orderWeb)) {
            String to_name;
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
                            SimpleDateFormat inputFormat = new SimpleDateFormat(format);
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
                    e.printStackTrace();
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

            List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
            String comment = stringList.get(2);
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
                for (int i = 0; i < OpenStreetMapActivity.arrayServiceCode().length; i++) {
                    if (services.get(i + 1).equals("1")) {
                        servicesChecked.add(OpenStreetMapActivity.arrayServiceCode()[i]);
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

            Bundle bundle = new Bundle();
            bundle.putString("messageResult_key", messageResult);
            bundle.putString("messagePay_key", messagePayment);
            bundle.putString("messageFondy_key", messageFondy);
            bundle.putString("messageCost_key", orderWeb);
            bundle.putSerializable("sendUrlMap", new HashMap<>(sendUrlMap));
            bundle.putString("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                MainActivity.navController.navigate(
                        R.id.nav_finish_separate,
                        bundle,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_visicom, true)
                                .build()
                );
            }, 1000); // 5000 миллисекунд = 5 секунд


        } else if (!visicomBackPressed) {
            sharedPreferencesHelperMain.saveValue("VisicomBackPressed", false);
            btnVisible(VISIBLE);
            assert message != null;
            constraintLayoutVisicomFinish.setVisibility(View.GONE);
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
            constraintLayoutVisicomFinish.setVisibility(View.GONE);
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

        List<String> stringList = logCursor(MainActivity.TABLE_USER_INFO, requireActivity());

        String phone = stringList.get(2);

        Logger.d(requireActivity(), TAG, "onClick befor validate: ");
        String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
        Logger.d(requireActivity(), TAG, "onClick No validate: " + val);
        return val;
    }

    private static boolean verifyOrder(Context context) {

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

        boolean verify = false;
        if (cursor.getCount() == 1) {

            if (logCursor(MainActivity.TABLE_USER_INFO, context).get(1).equals("0")) {
                verify = true;
                Log.d(TAG, "verifyOrder:verify " + verify);
            }
            cursor.close();
        }
        database.close();
        return verify;
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
                        paymentType(context);;
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

            progressBar.setVisibility(View.GONE);
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

            progressBar.setVisibility(View.GONE);
            alertDialog.dismiss();
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.GONE);
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


    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onResume() {
        super.onResume();
        Logger.d(context, TAG, "onResume 1" );
        new VerifyUserTask(context).execute();

        VisicomFragment.sendUrlMap = null;
        MainActivity.uid = null;
        MainActivity.action = null;

        MainActivity.orderResponse = null;
        viewModel.updateOrderResponse(null);
        viewModel.setTransactionStatus(null);
        viewModel.setCanceledStatus("no_canceled");

        textfrom = binding.textfrom;

        constraintLayoutVisicomMain.setVisibility(View.GONE);
 
        binding.svButton.setVisibility(View.GONE);
        binding.btnCallAdmin.setVisibility(View.GONE);

        String cityCheckActivity = (String) sharedPreferencesHelperMain.getValue("CityCheckActivity", "**");
        Logger.d(context, TAG, "CityCheckActivity: " + cityCheckActivity);
        progressBar.setVisibility(View.INVISIBLE);
        if (cityCheckActivity.equals("run")) {
            binding.textfrom.setVisibility(VISIBLE);
            binding.num1.setVisibility(VISIBLE);
            binding.textGeo.setVisibility(VISIBLE);
            binding.num2.setVisibility(VISIBLE);
            binding.textTo.setVisibility(VISIBLE);
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            // Ваш код при нажатии на заголовок
            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());

        }

        String visible_shed = (String) sharedPreferencesHelperMain.getValue("visible_shed", "no");
        if(visible_shed.equals("no")) {
            Logger.d(context, TAG, "onResume 2" );
            schedule.setVisibility(View.INVISIBLE);
            shed_down.setVisibility(View.INVISIBLE);

            gpsbut.setVisibility(View.INVISIBLE);
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

                binding.textTo.setVisibility(VISIBLE);
                binding.num2.setVisibility(VISIBLE);

                binding.textGeo.setVisibility(VISIBLE);
                binding.num2.setVisibility(VISIBLE);
                binding.textTo.setVisibility(VISIBLE);


                schedule.setVisibility(VISIBLE);
                shed_down.setVisibility(VISIBLE);

 
                binding.svButton.setVisibility(View.VISIBLE);
                binding.btnCallAdmin.setVisibility(View.VISIBLE);

            } else {
                Logger.d(context, TAG, "onResume 4" );
                schedule.setVisibility(View.INVISIBLE);
                shed_down.setVisibility(View.INVISIBLE);
                gpsbut.setVisibility(View.INVISIBLE);
                binding.num1.setVisibility(View.INVISIBLE);
                binding.textfrom.setVisibility(View.INVISIBLE);

                binding.textwhere.setVisibility(VISIBLE);
                progressBar.setVisibility(VISIBLE);

              
       
                binding.svButton.setVisibility(View.GONE);
                binding.btnCallAdmin.setVisibility(View.GONE);

            }
        }
        Logger.d(context, TAG, "onResume 5" );
        constraintLayoutVisicomMain.setVisibility(VISIBLE);
        constraintLayoutVisicomFinish.setVisibility(View.GONE);

        databaseHelper = new DatabaseHelper(context);
        databaseHelperUid = new DatabaseHelperUid(context);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        new Thread(this::fetchRoutesCancel).start();



        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String cityMenu = switch (city) {
            case "Kyiv City" -> context.getString(R.string.city_kyiv);
            case "Dnipropetrovsk Oblast" -> context.getString(R.string.city_dnipro);
            case "Odessa" -> context.getString(R.string.city_odessa);
            case "Zaporizhzhia" -> context.getString(R.string.city_zaporizhzhia);
            case "Cherkasy Oblast" -> context.getString(R.string.city_cherkassy);
            case "Lviv" -> context.getString(R.string.city_lviv);
            case "Ivano_frankivsk" -> context.getString(R.string.city_ivano_frankivsk);
            case "Vinnytsia" -> context.getString(R.string.city_vinnytsia);
            case "Poltava" -> context.getString(R.string.city_poltava);
            case "Sumy" -> context.getString(R.string.city_sumy);
            case "Kharkiv" -> context.getString(R.string.city_kharkiv);
            case "Chernihiv" -> context.getString(R.string.city_chernihiv);
            case "Rivne" -> context.getString(R.string.city_rivne);
            case "Ternopil" -> context.getString(R.string.city_ternopil);
            case "Khmelnytskyi" -> context.getString(R.string.city_khmelnytskyi);
            case "Zakarpattya" -> context.getString(R.string.city_zakarpattya);
            case "Zhytomyr" -> context.getString(R.string.city_zhytomyr);
            case "Kropyvnytskyi" -> context.getString(R.string.city_kropyvnytskyi);
            case "Mykolaiv" -> context.getString(R.string.city_mykolaiv);
            case "Сhernivtsi" -> context.getString(R.string.city_chernivtsi);
            case "Lutsk" -> context.getString(R.string.city_lutsk);
            case "OdessaTest" -> "Test";
            default -> context.getString(R.string.foreign_countries);
        };


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
            if (fusedLocationProviderClient != null && locationCallback != null) {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                gpsbut.setText(R.string.change);
            }
            sharedPreferencesHelperMain.saveValue("gps_upd", false);
            Intent intent = new Intent(getContext(), ActivityVisicomOnePage.class);
            intent.putExtra("start", "ok");
            intent.putExtra("end", "no");
            startActivity(intent);
        });

       

         

        text_view_cost = binding.textViewCost;

        geo_marker = "visicom";

        Logger.d(context, TAG, "onCreateView: geo_marker " + geo_marker);

        buttonBonus.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            String costText = text_view_cost.getText().toString().trim();
            if (!costText.isEmpty() && costText.matches("\\d+")) {
                updateAddCost("0", context);
                MyBottomSheetBonusFragment bottomSheetDialogFragment = new MyBottomSheetBonusFragment(Long.parseLong(costText), geo_marker, api, text_view_cost);
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });
//        setNalPaymentNoCards(context);
        textViewTo = binding.textTo;
        textViewTo.setOnClickListener(v -> {
            textViewTo.setText("");
            Intent intent = new Intent(getContext(), ActivityVisicomOnePage.class);
            intent.putExtra("start", "no");
            intent.putExtra("end", "ok");
            startActivity(intent);
        });


        btn_minus = binding.btnMinus;
        btn_plus = binding.btnPlus;
        btnOrder = binding.btnOrder;


        btnAdd.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            MyBottomSheetGeoFragment bottomSheetDialogFragment = new MyBottomSheetGeoFragment(text_view_cost);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
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
            if (cost >= MIN_COST_VALUE) {
                updateAddCost(String.valueOf(addCost), context);
                text_view_cost.setText(String.valueOf(cost));
            } else {
                addCost += 5;
                cost += 5;
                updateAddCost(String.valueOf(addCost), context);
                text_view_cost.setText(String.valueOf(cost));
            }
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
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {

                MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
            }
            linearLayout.setVisibility(View.GONE);
            btnVisible(View.INVISIBLE);
            List<String> stringList1 = logCursor(MainActivity.CITY_INFO, context);

            sharedPreferencesHelperMain.saveValue("gps_upd", true);
            sharedPreferencesHelperMain.saveValue("gps_upd_address", true);

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
                            paymentType(context);;
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


        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        gpsbut.setOnClickListener(v -> {
            if (locationManager != null) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    if (loadPermissionRequestCount() >= 3 && !MainActivity.location_update) {
                        MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment(context.getString(R.string.location_on));
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
                    } else
                        MainActivity.location_update = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

                    // GPS включен, выполните ваш код здесь
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_restart, true)
                                .build());
                    } else if (isAdded() && isVisible() && MainActivity.location_update) {
                        List<String> settings = new ArrayList<>();

                        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";

                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        Cursor cursor = database.rawQuery(query, null);

                        cursor.moveToFirst();

                        // Получите значения полей из первой записи

                        @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                        @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                        @SuppressLint("Range") String ToAdressString = cursor.getString(cursor.getColumnIndex("finish"));
                        Logger.d(context, TAG, "autoClickButton:ToAdressString " + ToAdressString);
                        cursor.close();
                        database.close();

                        settings.add(Double.toString(0));
                        settings.add(Double.toString(0));
                        settings.add(Double.toString(toLatitude));
                        settings.add(Double.toString(toLongitude));
                        settings.add(context.getString(R.string.search));
                        settings.add(ToAdressString);

                        updateRoutMarker(settings);

                        firstLocation();
                    }

                } else {
                    // GPS выключен, выполните необходимые действия

                    MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            } else {

                MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
            schedule.setVisibility(VISIBLE);
            shed_down.setVisibility(VISIBLE);

        });
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                gpsbut.setBackground(getResources().getDrawable(R.drawable.btn_yellow));
                gpsbut.setTextColor(Color.BLACK);
            } else {
                gpsbut.setBackground(getResources().getDrawable(R.drawable.btn_green));
                gpsbut.setTextColor(Color.WHITE);
            }
        } else {
            gpsbut.setBackground(getResources().getDrawable(R.drawable.btn_red));
            gpsbut.setTextColor(Color.WHITE);
        }


        if (NetworkUtils.isNetworkAvailable(context)) {
            if (geoText.getText().toString().isEmpty()) {
         
                binding.textfrom.setVisibility(View.INVISIBLE);
                num1.setVisibility(View.INVISIBLE);
                binding.textwhere.setVisibility(View.INVISIBLE);
            }

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    Logger.d(context, TAG, "onResume: 1");
                    progressBar.setVisibility(VISIBLE);
                    try {
                        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                        if (!userEmail.equals("email")) {
                            visicomCost(context);
                            readTariffInfo(context);
                        }

                    } catch (MalformedURLException e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        
                        textfrom.setVisibility(VISIBLE);
                        num1.setVisibility(VISIBLE);
                        geoText.setVisibility(VISIBLE);

                        binding.textwhere.setVisibility(VISIBLE);
                        num2.setVisibility(VISIBLE);
                        textViewTo.setVisibility(VISIBLE);
                    }

                } else {
                    boolean gps_upd = (boolean) sharedPreferencesHelperMain.getValue("gps_upd", false);
                    boolean gps_upd_address = (boolean) sharedPreferencesHelperMain.getValue("gps_upd_address", false);
                    Logger.d(context, TAG, "gps_upd" + sharedPreferencesHelperMain.getValue("gps_upd", false));
                    Logger.d(context, TAG, "gps_upd_address" + sharedPreferencesHelperMain.getValue("gps_upd_address", false));

                    if (gps_upd && gps_upd_address) {
                        textfrom.setVisibility(VISIBLE);
                        num1.setVisibility(VISIBLE);
                        geoText.setVisibility(VISIBLE);
                        binding.textwhere.setVisibility(VISIBLE);
                        num2.setVisibility(VISIBLE);
                        textViewTo.setVisibility(VISIBLE);
                        Logger.d(context, TAG, "onResume: 3");
                        firstLocation();
                    } else {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {
                            gpsbut.setBackground(getResources().getDrawable(R.drawable.btn_yellow));
                            gpsbut.setTextColor(Color.BLACK);
                        }

                        Logger.d(context, TAG, "onResume: 4");
                        progressBar.setVisibility(VISIBLE);
                        try {
                            String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                            if (!userEmail.equals("email")) {
                                visicomCost(context);
                                readTariffInfo(context);
                            }

                        } catch (MalformedURLException e) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                            
                            textfrom.setVisibility(VISIBLE);
                            num1.setVisibility(VISIBLE);
                            geoText.setVisibility(VISIBLE);

                            binding.textwhere.setVisibility(VISIBLE);
                            num2.setVisibility(VISIBLE);
                            textViewTo.setVisibility(VISIBLE);
                        }
                    }

                }
            } else {
                Logger.d(context, TAG, "onResume: 6");
                try {
                    String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                    if (!userEmail.equals("email")) {
                        visicomCost(context);
                        readTariffInfo(context);
                    }
                } catch (MalformedURLException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    Logger.d(context, TAG, "onResume: 7");

                    
                    textfrom.setVisibility(VISIBLE);
                    num1.setVisibility(VISIBLE);
                    geoText.setVisibility(VISIBLE);

                    binding.textwhere.setVisibility(VISIBLE);
                    num2.setVisibility(VISIBLE);
                    textViewTo.setVisibility(VISIBLE);

                    btnVisible(View.INVISIBLE);
                }

            }


        } else {

            binding.textwhere.setVisibility(View.INVISIBLE);

            progressBar.setVisibility(View.GONE);


        }

        scheduleUpdate();
        addCheck(context);


        updateApp();
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
        progressBar.setVisibility(VISIBLE);
        schedule.setVisibility(VISIBLE);
        shed_down.setVisibility(VISIBLE);

        Toast.makeText(context, context.getString(R.string.search), Toast.LENGTH_SHORT).show();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        btnVisible(View.INVISIBLE);
        gpsbut.setOnClickListener(v -> {

            if (fusedLocationProviderClient != null && locationCallback != null) {
                fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            }

            gpsbut.setText(R.string.change);
            gpsbut.setOnClickListener(v1 -> {
                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null) {
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        // GPS включен, выполните ваш код здесь
                        if (!NetworkUtils.isNetworkAvailable(context)) {
                            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_restart, true)
                                    .build());
                        } else if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                        } else if (isAdded() && isVisible()) {
                            List<String> settings = new ArrayList<>();

                            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";

                            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            Cursor cursor = database.rawQuery(query, null);

                            cursor.moveToFirst();

                            // Получите значения полей из первой записи

                            @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                            @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                            @SuppressLint("Range") String ToAdressString = cursor.getString(cursor.getColumnIndex("finish"));
                            Logger.d(context, TAG, "autoClickButton:ToAdressString " + ToAdressString);
                            cursor.close();
                            database.close();

                            settings.add(Double.toString(0));
                            settings.add(Double.toString(0));
                            settings.add(Double.toString(toLatitude));
                            settings.add(Double.toString(toLongitude));
                            settings.add(context.getString(R.string.search));
                            settings.add(ToAdressString);

                            updateRoutMarker(settings);
                            Toast.makeText(context, context.getString(R.string.search), Toast.LENGTH_SHORT).show();
                            firstLocation();
                        }

                    } else {
                        // GPS выключен, выполните необходимые действия
                        // Например, показать диалоговое окно с предупреждением о включении GPS
                        MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
                } else {

                    MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            });
            schedule.setVisibility(VISIBLE);
            shed_down.setVisibility(VISIBLE);
        });
        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Обработка полученных местоположений
                stopLocationUpdates();

                // Обработка полученных местоположений
                List<Location> locations = locationResult.getLocations();
                Logger.d(context, TAG, "onLocationResult: locations 222222" + locations);

                if (!locations.isEmpty()) {
                    Location firstLocation = locations.get(0);

                    double latitude = firstLocation.getLatitude();
                    double longitude = firstLocation.getLongitude();



                    List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                    String api = stringList.get(2);

                    Locale locale = Locale.getDefault();
                    String language = locale.getLanguage(); // Получаем язык устройства
                    baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                    String urlFrom = baseUrl + "/" + api + "/android/fromSearchGeoLocal/" + latitude + "/" + longitude + "/" + language;
                    String mes_city = context.getString(R.string.on_city_tv);
                    FromJSONParserRetrofit.sendURL(urlFrom, result -> {
                        // Обработка результата в основном потоке
                        if (result != null) {
                            Logger.d(context, TAG, "Результат: " + result);
                            String FromAdressString = result.get("route_address_from");

                            if (FromAdressString != null && FromAdressString.contains("Точка на карте")) {
                                List<String> stringListCity = logCursor(MainActivity.CITY_INFO, context);
                                String city = context.getString(R.string.foreign_countries);
                                switch (stringListCity.get(1)) {
                                    case "Kyiv City":
                                        city = context.getString(R.string.Kyiv_city);
                                        break;
                                    case "Dnipropetrovsk Oblast":
                                        break;
                                    case "Odessa":
                                    case "OdessaTest":
                                        city = context.getString(R.string.Odessa);
                                        break;
                                    case "Zaporizhzhia":
                                        city = context.getString(R.string.Zaporizhzhia);
                                        break;
                                    case "Cherkasy Oblast":
                                        city = context.getString(R.string.Cherkasy);
                                        break;
                                    default:
                                        city = context.getString(R.string.foreign_countries);
                                        break;
                                }
                                FromAdressString = context.getString(R.string.startPoint);
                            }

                            updateMyPosition(latitude, longitude, FromAdressString, context);

                            geoText.setText(FromAdressString);
                            progressBar.setVisibility(View.GONE);
                            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                            @SuppressLint("Recycle") Cursor cursor = database.rawQuery(query, null);

                            cursor.moveToFirst();

                            // Получите значения полей из первой записи
                            @SuppressLint("Range") double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
                            @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
                            @SuppressLint("Range") double toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));
                            @SuppressLint("Range") String ToAdressString = cursor.getString(cursor.getColumnIndex("finish"));


                            Logger.d(context, TAG, "onLocationResult:FromAdressString " + FromAdressString);
                            Logger.d(context, TAG, "onLocationResult:ToAdressString " + ToAdressString);


                            List<String> settings = new ArrayList<>();
                            if (FromAdressString != null && ToAdressString != null) {
                                boolean addressesEqual = FromAdressString.equals(ToAdressString);
                                Logger.d(context, TAG, "onLocationResult: FromAdressString.equals(ToAdressString): " + addressesEqual);
                            } else {
                                Log.w(TAG, "onLocationResult: One or both address strings are null");
                            }
                            // Пример кода для установки текста в TextView

                            if (originLatitude == toLatitude) {
                                textViewTo.setText(mes_city.isEmpty() ? "" : mes_city);
                            } else {
                                textViewTo.setText(ToAdressString);
                                Logger.d(context, TAG, "onLocationResult:ToAdressString " + ToAdressString);
                            }

                            assert ToAdressString != null;

                            if (ToAdressString.equals(mes_city) ||
                                    ToAdressString.isEmpty()) {
                                settings.add(Double.toString(latitude));
                                settings.add(Double.toString(longitude));
                                settings.add(Double.toString(latitude));
                                settings.add(Double.toString(longitude));
                                settings.add(FromAdressString);
                                settings.add(FromAdressString);
                                updateRoutMarker(settings);
                            } else {


                                if (isAdded()) {

                                    settings.add(Double.toString(latitude));
                                    settings.add(Double.toString(longitude));
                                    settings.add(Double.toString(toLatitude));
                                    settings.add(Double.toString(toLongitude));
                                    settings.add(FromAdressString);
                                    settings.add(ToAdressString);
                                    updateRoutMarker(settings);
                                }

                            }
                            gpsbut.setText(R.string.change);
                            gpsbut.setOnClickListener(v -> {
                                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                                if (locationManager != null) {
                                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                        // GPS включен, выполните ваш код здесь
                                        if (!NetworkUtils.isNetworkAvailable(context)) {
                                            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                                    .setPopUpTo(R.id.nav_restart, true)
                                                    .build());
                                        } else if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                                            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                                        } else if (isAdded() && isVisible()) {
                                            List<String> settings1 = new ArrayList<>();

                                            String query1 = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";

                                            SQLiteDatabase database1 = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                                            Cursor cursor1 = database1.rawQuery(query1, null);

                                            cursor1.moveToFirst();

                                            // Получите значения полей из первой записи

                                            @SuppressLint("Range") double toLatitude1 = cursor1.getDouble(cursor1.getColumnIndex("to_lat"));
                                            @SuppressLint("Range") double toLongitude1 = cursor1.getDouble(cursor1.getColumnIndex("to_lng"));
                                            @SuppressLint("Range") String ToAdressString1 = cursor1.getString(cursor1.getColumnIndex("finish"));
                                            Logger.d(context, TAG, "autoClickButton:ToAdressString " + ToAdressString1);
                                            cursor1.close();
                                            database1.close();

                                            settings1.add(Double.toString(0));
                                            settings1.add(Double.toString(0));
                                            settings1.add(Double.toString(toLatitude1));
                                            settings1.add(Double.toString(toLongitude1));
                                            settings1.add(context.getString(R.string.search));
                                            settings1.add(ToAdressString1);

                                            updateRoutMarker(settings1);
                                            Toast.makeText(context, context.getString(R.string.search), Toast.LENGTH_SHORT).show();
                                            firstLocation();
                                        }

                                    } else {
                                        // GPS выключен, выполните необходимые действия
                                        // Например, показать диалоговое окно с предупреждением о включении GPS

                                        MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                                    }
                                } else {
                                    MyBottomSheetGPSFragment bottomSheetDialogFragment = new MyBottomSheetGPSFragment("");
                                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                                }
                            });

                            CityFinder cityFinder = new CityFinder(context, latitude, longitude , FromAdressString);
                            cityFinder.findCity(latitude, longitude);

                            try {
                                String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
                                if (!userEmail.equals("email")) {
                                    visicomCost(context);
                                    readTariffInfo(context);
                                }
                            } catch (MalformedURLException e) {
                                FirebaseCrashlytics.getInstance().recordException(e);
                                throw new RuntimeException(e);
                            }
                        } else {
                            Logger.d(context, TAG, "Ошибка при выполнении запроса");
                        }
                    });


                }
            }

        };

        startLocationUpdates();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = createLocationRequest();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000); // Интервал обновления местоположения в миллисекундах
        locationRequest.setFastestInterval(100); // Самый быстрый интервал обновления местоположения в миллисекундах
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Приоритет точного местоположения
        return locationRequest;
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

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }


    private static void visicomCost(Context context) throws MalformedURLException {

        progressBar.setVisibility(VISIBLE);
        constr2.setVisibility(View.INVISIBLE);
        


        String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor cursor = database.rawQuery(query, null);

        cursor.moveToFirst();

        @SuppressLint("Range") double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
        @SuppressLint("Range") double toLan = cursor.getDouble(cursor.getColumnIndex("to_lat"));
        String cityCheckActivity = (String) sharedPreferencesHelperMain.getValue("CityCheckActivity", "**");
        Logger.d(context, TAG, "CityCheckActivity: " + cityCheckActivity);
        progressBar.setVisibility(View.INVISIBLE);
        if (cityCheckActivity.equals("run")) {
            if (originLatitude != 0.0 && toLan != 0.0) {

                gpsbut.setVisibility(View.VISIBLE);
                svButton.setVisibility(View.VISIBLE);
                btnCallAdmin.setVisibility(View.VISIBLE);

                textfrom.setVisibility(VISIBLE);
                num1.setVisibility(VISIBLE);
                textwhere.setVisibility(VISIBLE);
                num2.setVisibility(VISIBLE);
                textViewTo.setVisibility(VISIBLE);

                boolean black_list_yes = verifyOrder(context);

                String black_list_city = sharedPreferencesHelperMain.getValue("black_list", "cache").toString();
                Log.d("black_list_city", "black_list_city 1 " + black_list_city);

                Handler handler = new Handler(Looper.getMainLooper());
                new Thread(() -> {
                    @SuppressLint("Range") String start = cursor.getString(cursor.getColumnIndex("start"));
                    @SuppressLint("Range") double toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));

                    @SuppressLint("Range") String finish = cursor.getString(cursor.getColumnIndex("finish"));


                    String urlCost = getTaxiUrlSearchMarkers("costSearchMarkersTime", context);

                    Logger.d(context, TAG, "visicomCost: " + urlCost);


                    CostJSONParserRetrofit parser = new CostJSONParserRetrofit();
                    try {
                        parser.sendURL(urlCost, new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                                handler.post(() -> {
                                    geoText.setText(start);
                                    if (originLatitude == toLatitude) {
                                        textViewTo.setText(context.getString(R.string.on_city_tv));
                                    } else {
                                        textViewTo.setText(finish);
                                    }

                                    Map<String, String> sendUrlMapCost = response.body();
                                    if (sendUrlMapCost == null) {
                                        Toast.makeText(context, context.getString(R.string.server_error_connected), Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    String orderCost = sendUrlMapCost.get("order_cost");

                                    Log.d("black_list_city", "black_list_city." + black_list_city);

                                    if(black_list_yes) {
                                        if (black_list_city.equals("cards only")) {
                                            orderCost = addCostBlackList(sendUrlMapCost.get("order_cost"));
                                        }
                                    }


                                    String orderMessage = sendUrlMapCost.get("Message");

                                    assert orderCost != null;
                                    if (orderCost.equals("0")) {

                                        String message = context.getString(R.string.error_message);
                                        assert orderMessage != null;
                                        if (orderMessage.equals("ErrorMessage")) {
                                            message = context.getString(R.string.server_error_connected);
                                        }

                                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                                        if (fragmentManager != null && !fragmentManager.isStateSaved()) {
                                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                                        } else {
                                            assert fragmentManager != null;
                                            fragmentManager.beginTransaction()
                                                    .add(bottomSheetDialogFragment, bottomSheetDialogFragment.getTag())
                                                    .commitAllowingStateLoss();
                                        }


                                    } else {
                                        applyDiscountAndUpdateUI(orderCost, context);
                                    }
                                    linear_layout_buttons.setVisibility(VISIBLE);
                                });
                            }

                            @Override
                            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                                handler.post(() -> {
                                    FirebaseCrashlytics.getInstance().recordException(t);
                                    Toast.makeText(context, context.getString(R.string.server_error_connected), Toast.LENGTH_SHORT).show();
                                });

                            }
                        });
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            } else {
                String message = context.getString(R.string.verify_address);

                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                if (fragmentManager != null && !fragmentManager.isStateSaved()) {
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                } else {
                    assert fragmentManager != null;
                    fragmentManager.beginTransaction()
                            .add(bottomSheetDialogFragment, bottomSheetDialogFragment.getTag())
                            .commitAllowingStateLoss();
                }

            }
        }
    }

    private static void applyDiscountAndUpdateUI(String orderCost, Context context) {
        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
        if (discountText.matches("[+-]?\\d+") || discountText.equals("0")) {
            long discountInt = Integer.parseInt(discountText);
            long discount;

            firstCost = Long.parseLong(orderCost);
            discount = firstCost * discountInt / 100;
            firstCost = VisicomFragment.firstCost + discount;
            updateAddCost(String.valueOf(discount), context);
            text_view_cost.setText(String.valueOf(VisicomFragment.firstCost));

            startCost = firstCost;
            finalCost = firstCost;


            MIN_COST_VALUE = (long) (VisicomFragment.firstCost * 0.6);
            firstCostForMin = VisicomFragment.firstCost;


            geoText.setVisibility(VISIBLE);
            progressBar.setVisibility(View.GONE);

            textfrom.setVisibility(VISIBLE);
            num1.setVisibility(VISIBLE);
            textwhere.setVisibility(VISIBLE);
            num2.setVisibility(VISIBLE);
            textViewTo.setVisibility(VISIBLE);

            btnAdd.setVisibility(VISIBLE);

            buttonBonus.setVisibility(VISIBLE);
            btn_minus.setVisibility(VISIBLE);
            text_view_cost.setVisibility(VISIBLE);
            btn_plus.setVisibility(VISIBLE);
            btnOrder.setVisibility(VISIBLE);


            constr2.setVisibility(VISIBLE);

            schedule.setVisibility(VISIBLE);
            shed_down.setVisibility(VISIBLE);
            // Получение текущего времени
            String currentTime = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
//            Log.i("Pusher", "Время обновления visicom: " + currentTime + ", Скидка: " + discount + ", Итоговая стоимость: " + firstCost);

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

        // Update database entry to set "verifyOrder" to "0"
        ContentValues cv = new ContentValues();
        cv.put("verifyOrder", "0");
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        int rowsUpdated = database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
        Log.d("blockUserBlackList", "Updated 'verifyOrder' in database. Rows affected: " + rowsUpdated);

        // Close the database
        database.close();
        Log.d("blockUserBlackList", "Database connection closed.");
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
                case "-1":
                    closeReasonText = context.getString(R.string.close_resone_in_work);
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
                case "8":
                    closeReasonText = context.getString(R.string.close_resone_8);
                    break;
                case "9":
                    closeReasonText = context.getString(R.string.close_resone_9);
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
            String routeInfo = "";

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
            MyBottomSheetErrorFragment myBottomSheetMessageFragment = new MyBottomSheetErrorFragment(message);
            fragmentManager.beginTransaction()
                    .add(myBottomSheetMessageFragment, myBottomSheetMessageFragment.getTag())
                    .commitAllowingStateLoss();
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
        ViewGroup parent = (ViewGroup) dialog.findViewById(android.R.id.content);

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
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
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
        call.enqueue(new Callback<ResponsePaySystem>() {
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
                    if(isAdded()){
                        ContentValues cv = new ContentValues();
                        cv.put("payment_type", pay_method);
                        // обновляем по id
                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                new String[] { "1" });

                        cv = new ContentValues();
                        cv.put("verifyOrder", "0");

                        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                        database.close();

                        orderRout();

                        googleVerifyAccount();
                    }


                } else {
                    if (isAdded()) { //
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }

                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponsePaySystem> call, @NonNull Throwable t) {
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
                        orderFinished();
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
}
