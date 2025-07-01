package com.taxi.easy.ua.ui.finish.fragm;


import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.MainActivity.activeCalls;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.FragmentCacheOrderBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.Status;
import com.taxi.easy.ua.ui.finish.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.payment_system.PayApi;
import com.taxi.easy.ua.ui.payment_system.ResponsePaySystem;
import com.taxi.easy.ua.utils.animation.car.CarProgressBar;
import com.taxi.easy.ua.utils.auth.FirebaseConsentManager;
import com.taxi.easy.ua.utils.blacklist.BlacklistManager;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.ip.RetrofitClient;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;
import com.taxi.easy.ua.utils.user.user_verify.VerifyUserTask;
import com.uxcam.UXCam;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CacheOrderFragment extends Fragment {

    @SuppressLint("StaticFieldLeak")

    public static Map<String, String> costMap;
    private FragmentCacheOrderBinding binding;
    private static final String TAG = "CacheOrderFragment";
 

    @SuppressLint("StaticFieldLeak")
    public static AppCompatButton btnOrder, btnCallAdmin, btnCallAdminFin;
    @SuppressLint("StaticFieldLeak")
    public static TextView geoText;



    @SuppressLint("StaticFieldLeak")
    public static TextView text_view_cost;
    @SuppressLint("StaticFieldLeak")
    public static TextView textViewTo;
    @SuppressLint("StaticFieldLeak")
    public static EditText to_number;


    public static long cost;
    public static long addCost;
    public static String to;

    static String pay_method;
    private String urlOrder;
    private String text_full_message_string;

    private AlertDialog alertDialog;

    Activity context;
    static FragmentManager fragmentManager;


    public static Map<String, String> sendUrlMap;

    @SuppressLint("StaticFieldLeak")
    public static TextView text_full_message, textCostMessage, textStatusCar;

    private Animation blinkAnimation;
 
    private static String baseUrl;
 
    private CarProgressBar carProgressBar;


    public static  long startCost;
    public static  long finalCost;
    Bundle arguments;

    String uid, uid_Double;
    private ExecutionStatusViewModel viewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);
        button1.setVisibility(View.VISIBLE);
        binding = FragmentCacheOrderBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context = requireActivity();
        // Включаем блокировку кнопки "Назад" Применяем блокировку кнопки "Назад"
        BackPressBlocker backPressBlocker = new BackPressBlocker();
        backPressBlocker.setBackButtonBlocked(true);
        backPressBlocker.blockBackButtonWithCallback(this);


        // Получение ViewModel из области видимости Activity
        viewModel = new ViewModelProvider(requireActivity()).get(ExecutionStatusViewModel.class);

        text_full_message = root.findViewById(R.id.text_full_message);
        textCostMessage = root.findViewById(R.id.text_cost_message);
        textCostMessage.setText(context.getString(R.string.recounting_order));

        textStatusCar = root.findViewById(R.id.text_status);

        carProgressBar = root.findViewById(R.id.carProgressBar);



         

        fragmentManager = getParentFragmentManager();

        btnCallAdminFin = binding.btnCallAdminFin;
        btnCallAdminFin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
            String phone = stringList.get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });
 

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Действия при нажатии кнопки "Назад"
                sharedPreferencesHelperMain.saveValue("CachOrderBackPressed", true);
                cancelAllRequests(); // Отменяем запросы
            }
        });
        sharedPreferencesHelperMain.saveValue("carFound", false);

        arguments = getArguments();
        assert arguments != null;

        text_full_message_string = arguments.getString("text_full_message");

        String messageResult = text_full_message_string;

        text_full_message.setText(messageResult);

        messageResult = context.getString(R.string.check_cost_message);
        textCostMessage.setText(messageResult);

        textStatusCar.setText(R.string.check_order_mes);

        blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink_animation);
        textStatusCar.startAnimation(blinkAnimation);


        carProgressBar.resumeAnimation();
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




    @Override
    public void onPause() {
        super.onPause();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
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

    @SuppressLint("Range")
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
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
        if (urlAPI.equals("orderCacheReorder")) {

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
            String clientCost = "0";

            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + clientCost  + "/" + paramsUserArr + "/" + addCost + "/"
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
                + parameters + "/" + result + "/" + city + "/" + context.getString(R.string.application)+ "/" + uid ;

        database.close();

        return url;
    }

    private String addCostBlackList(String addcost) {

        int cost = Integer.parseInt(addcost); // Преобразуем строку в целое число
        cost += 45; // Увеличиваем на 45
        addcost = String.valueOf(cost); // Преобразуем обратно в строку

// Теперь addCost содержит новое значение
        return  addcost; // Вывод: "145"

    }
    @SuppressLint("Range")
    private String getCheckRectoken(String table, Context context) {
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
    private boolean verifyOrder(Context context) {

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
    private boolean orderRout() {
//        urlOrder = getTaxiUrlSearchMarkers("orderClientCost", MyApplication.getContext());
        urlOrder = getTaxiUrlSearchMarkers("orderCacheReorder", MyApplication.getContext());
        Logger.d(MyApplication.getContext(), TAG, "order:  urlOrder " + urlOrder);
        return true;
    }

    public void orderFinished() throws MalformedURLException {




                ToJSONParserRetrofit parser = new ToJSONParserRetrofit();
                baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
                Logger.d(context, TAG, "orderFinished: " + baseUrl + urlOrder);

                parser.sendURLChannel(urlOrder, new Callback<>() {

                    @Override
                    public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Map<String, String> sendUrlMap = response.body();
                            pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
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

    private void handleOrderFinished(Map<String, String> sendUrlMap, String pay_method, Context context) {
        assert sendUrlMap != null;
        String orderWeb = sendUrlMap.get("order_cost");
        Logger.d(context, TAG, "orderFinished: message " + orderWeb);
        String message = sendUrlMap.get("message");
        Logger.d(context, TAG, "orderFinished: message " + message);
        assert orderWeb != null;

        boolean CachOrderBackPressed = (boolean) sharedPreferencesHelperMain.getValue("CachOrderBackPressed", false);

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

            Bundle bundle = new Bundle();
            bundle.putString("messageResult_key", messageResult);
            bundle.putString("messagePay_key", messagePayment);
            bundle.putString("messageFondy_key", messageFondy);
            bundle.putString("messageCost_key", orderWeb);
            bundle.putSerializable("sendUrlMap", new HashMap<>(sendUrlMap));
            bundle.putString("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));

            viewModel.setStatusNalUpdate(true); //наюлюдение за опросом статусом нала
            new Handler(Looper.getMainLooper()).post (() -> {
                MainActivity.navController.navigate(
                        R.id.nav_finish_separate,
                        bundle,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_visicom, true)
                                .build()
                );
            } ); // 5000 миллисекунд = 5 секунд


        } else {
            sharedPreferencesHelperMain.saveValue("CachOrderBackPressed", false);
            new Handler(Looper.getMainLooper()).post (this::showAddCostDoubleDialog);
        }

//        else if (!CachOrderBackPressed) {
//            sharedPreferencesHelperMain.saveValue("CachOrderBackPressed", false);
//
////            assert message != null;
////
////            Logger.d(context, TAG, "2 orderFinished: message " + message);
////            String addType = "60";
////            if (message.contains("Дублирование")) {
////                sharedPreferencesHelperMain.saveValue("doubleOrderPref", true);
////                showAddCostDoubleDialog(addType);
////            } else if (message.equals("cash") || message.equals("cards only")) {
////                if (message.equals("cards only")) {
////                    addType = "45";
////                    showAddCostDoubleDialog(addType);
////                } else {
////                    message = context.getString(R.string.black_list_message);
////                    if (!isStateSaved() && isAdded()) {
////                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
////                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
////                    }
////                }
////            } else if (message.equals("ErrorMessage")) {
////                message = getResources().getString(R.string.server_error_connected);
////                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
////                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
////            } else {
////                switch (pay_method) {
////                    case "bonus_payment":
////                    case "card_payment":
////                    case "fondy_payment":
////                    case "mono_payment":
////                    case "wfp_payment":
////                        changePayMethodToNal(context.getString(R.string.to_nal_payment));
////                        break;
////                    default:
////                        message = getResources().getString(R.string.error_message);
////                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
////                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
////                }
////            }
//
//        } else {
//            sharedPreferencesHelperMain.saveValue("CachOrderBackPressed", false);
//
//            assert message != null;
//
//        }
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

            googleVerifyAccount();

            alertDialog.dismiss();
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(v -> {

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

            paymentType(context);

            googleVerifyAccount();


            alertDialog.dismiss();
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(v -> {

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

    }


    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onResume() {
        super.onResume();
        Logger.d(context, TAG, "onResume 1" );
        new VerifyUserTask(context).execute();

        sendUrlMap = null;
        MainActivity.uid = null;
//        MainActivity.action = null;

        MainActivity.orderResponse = null;
        viewModel.updateOrderResponse(null);
        viewModel.setTransactionStatus(null);
        viewModel.setCanceledStatus("no_canceled");

        String cityCheckActivity = (String) sharedPreferencesHelperMain.getValue("CityCheckActivity", "**");
        Logger.d(context, TAG, "CityCheckActivity: " + cityCheckActivity);


        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        }
//            if (!NetworkUtils.isNetworkAvailable(context)) {
//                // Ваш код при нажатии на заголовок
//                MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                        .setPopUpTo(R.id.nav_restart, true)
//                        .build());
//
//            }


        Logger.d(context, TAG, "onResume 5" );

        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");


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
            case "Chernivtsi":
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

        cancelOrderDoubleForNal();
    }


    private void blockUserBlackList() {
        // Log the start of the block process
        Logger.d(context, TAG, "Starting the block process for user.");
         // Retrieve email from the database
        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        Logger.d(context, TAG, "Retrieved email from database: " + email);

        // Add email to the blacklist
        BlacklistManager blacklistManager = new BlacklistManager();
        blacklistManager.addToBlacklist(email);
        Logger.d(context, TAG, "Request to add email to blacklist sent: " + email);

        // Update database entry to set "verifyOrder" to "0"
        ContentValues cv = new ContentValues();
        cv.put("verifyOrder", "0");
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        int rowsUpdated = database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
        Logger.d(context, TAG, "Updated 'verifyOrder' in database. Rows affected: " + rowsUpdated);

        // Close the database
        database.close();
        Logger.d(context, TAG, "Database connection closed.");
        MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_visicom, true)
                .build());
    }



//    private void showAddCostDoubleDialog(String addType) {
//        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
//        LayoutInflater inflater = requireActivity().getLayoutInflater();
//        int dialogViewInt = R.layout.dialog_add_cost;
//
//        if ("60".equals(addType)) {
//            dialogViewInt = R.layout.dialog_add_60_cost;
//        }
//
//        View dialogView = inflater.inflate(dialogViewInt, null);
//
//        // Заголовок и сообщение
//        String title = context.getString(R.string.double_order);
//        String message = context.getString(R.string.add_cost_fin_60);
//        String numberIndexString = "";
//
//        // Инициализация элементов для типа "60"
//        if ("60".equals(addType)) {
//            title = context.getString(R.string.double_order);
//
//            AppCompatButton minus = dialogView.findViewById(R.id.btn_minus);
//            AppCompatButton plus = dialogView.findViewById(R.id.btn_plus);
//            EditText discinp = dialogView.findViewById(R.id.discinp);
//
//            minus.setOnClickListener(v -> {
//                String addCost = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(5);
//                int addCostInt = Integer.parseInt(addCost);
//                if (addCostInt >= 5) {
//                    addCostInt -= 5;
//                    updateAddCost(String.valueOf(addCostInt), context);
//                    discinp.setText(String.valueOf(addCostInt + 60));
//                }
//            });
//
//            plus.setOnClickListener(v -> {
//                String addCost = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(5);
//                int addCostInt = Integer.parseInt(addCost);
//                addCostInt += 5;
//                updateAddCost(String.valueOf(addCostInt), context);
//                discinp.setText(String.valueOf(addCostInt + 60));
//            });
//
//            message = context.getString(R.string.add_cost_fin_60);
//            numberIndexString = message;
//        } else if ("45".equals(addType)) {
//            title = context.getString(R.string.black_list);
//            message = context.getString(R.string.add_cost_fin_45);
//            numberIndexString = "45";
//            blockUserBlackList();
//        }
//
//        // Установка заголовка и сообщения
//        TextView titleView = dialogView.findViewById(R.id.dialogTitle);
//        titleView.setText(title);
//
//        TextView messageView = dialogView.findViewById(R.id.dialogMessage);
//        SpannableStringBuilder spannable = new SpannableStringBuilder(message);
//        int numberIndex = message.indexOf(numberIndexString);
//        if (numberIndex != -1) {
//            spannable.setSpan(new StyleSpan(Typeface.BOLD), numberIndex, numberIndex + numberIndexString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        }
//        messageView.setText(spannable);
//
//        // Обработка кнопок
//        builder.setView(dialogView)
//                .setCancelable(false)
//                .setPositiveButton(R.string.ok_button, (dialog, which) -> {
//                    dialog.dismiss();
//                    if ("60".equals(addType)) {
//                        createDoubleOrder();
//                    } else if ("45".equals(addType)) {
//                        createBlackList();
//                    }
//                })
//                .setNegativeButton(R.string.cancel_button, (dialog, which) -> {
//                    if ("60".equals(addType)) {
//                        sharedPreferencesHelperMain.saveValue("doubleOrderPref", false);
//                    }
//                    MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
//                            .setPopUpTo(R.id.nav_visicom, true)
//                            .build());
//                });
//
//        AlertDialog dialog = builder.create();
//
//
//        dialog.show();
//
//        // Настройка цветов кнопок
//        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
//        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
//
//        if (positiveButton != null) {
//            positiveButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
//            positiveButton.setTextColor(ContextCompat.getColor(context, android.R.color.white));
//            ViewParent buttonPanel = positiveButton.getParent();
//            if (buttonPanel instanceof ViewGroup) {
//                ((ViewGroup) buttonPanel).setBackgroundColor(ContextCompat.getColor(context, R.color.background_color_new));
//            }
//
//        }
//        if (negativeButton != null) {
//            negativeButton.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_text_color_2));
//            negativeButton.setTextColor(ContextCompat.getColor(context, android.R.color.white));
//
//        }
//    }
    private void showAddCostDoubleDialog() {
    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
    LayoutInflater inflater = requireActivity().getLayoutInflater();
    int dialogViewInt = R.layout.dialog_add_cost;


    View dialogView = inflater.inflate(dialogViewInt, null);

    // Заголовок и сообщение
    String title = getString(R.string.err_nal_reoder_mes);

    // Установка заголовка и сообщения
    TextView titleView = dialogView.findViewById(R.id.dialogTitle);
    titleView.setText(title);

    // Обработка кнопок
    builder.setView(dialogView)
            .setCancelable(false)
            .setPositiveButton(R.string.ok_error, (dialog, which) -> {
                MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
            });

    AlertDialog dialog = builder.create();


    dialog.show();

    // Настройка цветов кнопок
    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

    if (positiveButton != null) {
        positiveButton.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAccent));
        positiveButton.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        ViewParent buttonPanel = positiveButton.getParent();
        if (buttonPanel instanceof ViewGroup) {
            ((ViewGroup) buttonPanel).setBackgroundColor(ContextCompat.getColor(context, R.color.background_color_new));
        }

    }

}
    private void createBlackList() {
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

                        cv = new ContentValues();
                        cv.put("verifyOrder", "0");

                        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                        database.close();


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
        int addCostInt = Integer.parseInt(addCost);


        addCostInt += 60;
        addCost = String.valueOf(addCostInt);
        updateAddCost(addCost, context);

        googleVerifyAccount();

    }

    private void googleVerifyAccount() {
        if(orderRout()) {
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

    private void cancelOrderDoubleForNal() {

        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);
        pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);

        baseUrl = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        uid = arguments.getString("uid");
        uid_Double = arguments.getString("uid_Double");

        String url = baseUrl + api + "/android/webordersCancelDouble/" + uid + "/" + uid_Double + "/" + pay_method + "/" + city + "/" + context.getString(R.string.application);

        Call<Status> call = ApiClient.getApiService().cancelOrderDouble(url);
        Logger.d(context, TAG, "cancelOrderDouble: " + url);


        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Status> call, @NonNull Response<Status> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Logger.d(context, TAG, "cancelOrderDouble response: " + response.toString());

                    new Handler(Looper.getMainLooper()).post(() -> {
                        googleVerifyAccount();
                    });



                }
            }

            @Override
            public void onFailure(@NonNull Call<Status> call, @NonNull Throwable t) {
                if (t instanceof SocketTimeoutException) {
                    Logger.d(context, TAG, "onFailure: Тайм-аут соединения");
                } else if (t instanceof IOException) {
                    Logger.d(context, TAG, "onFailure: Ошибка сети или соединения");
                } else {
                    Logger.d(context, TAG, "onFailure: Непредвиденная ошибка");
                }

                // Логируем исключение
                FirebaseCrashlytics.getInstance().recordException(t);

                // Выводим сообщение пользователю
                String errorMessage = t.getMessage();
                Logger.d(context, TAG, "onFailure: Ошибка: " + errorMessage);

            }

        });


    }
}
