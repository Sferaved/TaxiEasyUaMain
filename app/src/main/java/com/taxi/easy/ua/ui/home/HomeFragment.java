package com.taxi.easy.ua.ui.home;


import static android.content.Context.MODE_PRIVATE;
import static android.graphics.Color.RED;
import static com.taxi.easy.ua.R.string.address_error_message;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.BlendMode;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.room.Room;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.cities.Cherkasy.Cherkasy;
import com.taxi.easy.ua.cities.Dnipro.DniproCity;
import com.taxi.easy.ua.cities.Kyiv.KyivCity;
import com.taxi.easy.ua.cities.Odessa.Odessa;
import com.taxi.easy.ua.cities.Odessa.OdessaTest;
import com.taxi.easy.ua.cities.Zaporizhzhia.Zaporizhzhia;
import com.taxi.easy.ua.databinding.FragmentHomeBinding;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.home.room.AppDatabase;
import com.taxi.easy.ua.ui.home.room.RouteCost;
import com.taxi.easy.ua.ui.home.room.RouteCostDao;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import com.taxi.easy.ua.ui.start.ResultSONParser;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.cost_json_parser.CostJSONParserRetrofit;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;
import com.taxi.easy.ua.utils.user.user_verify.VerifyUserTask;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FragmentHomeBinding binding;
    public static String from, to;
    public static EditText from_number, to_number;
    String messageResult;

    Button gpsbut;
    public static AppCompatButton btn_order;
    public static AppCompatButton buttonAddServices;
    public static AppCompatButton buttonBonus;
    public static AppCompatButton btn_minus;
    public static AppCompatButton btn_plus;
    public static AppCompatButton btnGeo;
    public static AppCompatButton btn_clear;
    public static AppCompatButton btnCallAdmin;

    public static long addCost, cost, costFirst;
    private static String[] arrayStreet;
    private String numberFlagFrom = "1", numberFlagTo = "1";

    public static String  from_numberCost, toCost, to_numberCost;
    public static ProgressBar progressBar;
    String pay_method;
    public static long costFirstForMin;
    public static String urlOrder;
    public static long discount;
    private MyPhoneDialogFragment bottomSheetDialogFragment;

    public static int routeIdToCheck = 123;
    private boolean finiched;
    private AlertDialog alertDialog;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 123;
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
    public static TextView text_view_cost;

    long MIN_COST_VALUE;
    AutoCompleteTextView textViewFrom, textViewTo;
    ArrayAdapter<String> adapter;

    String city;
    LocationManager locationManager;
    Activity context;
    FragmentManager fragmentManager;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        context = requireActivity();
        fragmentManager = getParentFragmentManager();

        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);

        city = stringList.get(1);
        if (!NetworkUtils.isNetworkAvailable(requireContext()) || city.equals("foreign countries")) {
            MainActivity.navController.popBackStack();
            MainActivity.navController.navigate(R.id.nav_visicom);
        }

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        finiched = true;

        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        progressBar = binding.progressBar;
        buttonBonus = binding.btnBonus;

        addCost = 0;
        updateAddCost(String.valueOf(addCost));

        if(!stringList.isEmpty()) {
            switch (city){
                case "Dnipropetrovsk Oblast":
                    arrayStreet = DniproCity.arrayStreet();
                    paymentType("nal_payment");
                    break;
                case "Zaporizhzhia":
                    arrayStreet = Zaporizhzhia.arrayStreet();
                    paymentType("nal_payment");
                    break;
                case "Cherkasy Oblast":
                    arrayStreet = Cherkasy.arrayStreet();
                    paymentType("nal_payment");
                    break;
                case "Odessa":
                    arrayStreet = Odessa.arrayStreet();
                    paymentType("nal_payment");
                    break;
                case "OdessaTest":
                    arrayStreet = OdessaTest.arrayStreet();
                    break;
                default:
                    arrayStreet = KyivCity.arrayStreet();
                    break;
            }
            adapter = new ArrayAdapter<>(context,R.layout.drop_down_layout, arrayStreet);
        }

        text_view_cost = binding.textViewCost;
        btnGeo = binding.btnGeo;

        btnGeo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

            }
        });
        if(!text_view_cost.getText().toString().isEmpty()) {
            costFirst = Long.parseLong(text_view_cost.getText().toString());
            cost = Long.parseLong(text_view_cost.getText().toString());
        }

        MIN_COST_VALUE = (long) (cost*0.6);

        btn_minus = binding.btnMinus;
        btn_plus= binding.btnPlus;

        btn_minus.setOnClickListener(v -> {
            Logger.d(getActivity(), TAG, "onCreateView: cost " +cost);
            Logger.d(getActivity(), TAG, "onCreateView: MIN_COST_VALUE " +MIN_COST_VALUE);

                List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
                addCost = Long.parseLong(stringListInfo.get(5));
                cost = Long.parseLong(text_view_cost.getText().toString());
                cost -= 5;
                addCost -= 5;
            if (cost >= MIN_COST_VALUE) {
                updateAddCost(String.valueOf(addCost));
                text_view_cost.setText(String.valueOf(cost));
            }
        });

        btn_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
                addCost = Long.parseLong(stringListInfo.get(5));
                cost = Long.parseLong(text_view_cost.getText().toString());
                cost += 5;
                addCost += 5;
                updateAddCost(String.valueOf(addCost));
                text_view_cost.setText(String.valueOf(cost));
            }
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
                btnVisible(View.VISIBLE);
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
                                    paymentType("nal_payment");
                                }
                            }
                            break;
                    }
                    progressBar.setVisibility(View.VISIBLE);


                        Logger.d(getActivity(), TAG, "onClick: pay_method" + pay_method);
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
                                            orderFinished();
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
                                            orderFinished();
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
                                        orderFinished();
                                    }
                                } catch (UnsupportedEncodingException e) {
                                    FirebaseCrashlytics.getInstance().recordException(e);
                                    throw new RuntimeException(e);
                                }
                        }

                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }
            }
        });

        gpsbut = binding.gpsbut;
        gpsbut.setVisibility(View.INVISIBLE);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        gpsbut.setOnClickListener(v -> {
            if (locationManager != null) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
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

                    // GPS включен, выполните ваш код здесь
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        Toast.makeText(context, getString(R.string.verify_internet), Toast.LENGTH_SHORT).show();
                    }  else if (isAdded() && isVisible() && MainActivity.location_update)  {
                        startActivity(new Intent(requireContext(), OpenStreetMapActivity.class));
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
        });


        buttonAddServices = binding.btnAdd;
        buttonAddServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyBottomSheetDialogFragment bottomSheetDialogFragment = new MyBottomSheetDialogFragment();
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });
        buttonBonus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                String api =  stringList.get(2);
                updateAddCost("0");
                MyBottomSheetBonusFragment bottomSheetDialogFragment = new MyBottomSheetBonusFragment(
                        Long.parseLong(text_view_cost.getText().toString()),
                        "home",
                        api,
                        text_view_cost
                );
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            }
        });
        btnCallAdmin = binding.btnCallAdmin;
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, requireActivity()).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });
        return root;
    }

    private void orderFinished() {

        if (!verifyPhone()){
            MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment(context, "home");
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            progressBar.setVisibility(View.GONE);
        } else {
            Toast.makeText(context, R.string.check_order_mes, Toast.LENGTH_SHORT).show();
            ToJSONParserRetrofit parser = new ToJSONParserRetrofit();

//            // Пример строки URL с параметрами
            Logger.d(getActivity(), TAG, "orderFinished: "  + "https://m.easy-order-taxi.site"+ urlOrder);
            parser.sendURL(urlOrder, new Callback<Map<String, String>>() {
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

                        assert from_name != null;
                        if (from_name.equals(to_name)) {
                            messageResult = getString(R.string.thanks_message) +
                                    from_name + " " + from_number.getText() + getString(R.string.on_city) +
                                    getString(R.string.cost_of_order) + orderWeb + getString(R.string.UAH);
                        } else {
                            messageResult = getString(R.string.thanks_message) +
                                    from_name + " " + from_number.getText() + " " + getString(R.string.to_message) +
                                    to_name + " " + to_number.getText() + "." +
                                    getString(R.string.cost_of_order) + orderWeb + getString(R.string.UAH);
                        }
                        Logger.d(getActivity(), TAG, "order: sendUrlMap.get(\"from_lat\")" + sendUrlMap.get("from_lat"));
                        Logger.d(getActivity(), TAG, "order: sendUrlMap.get(\"lat\")" + sendUrlMap.get("lat"));
                        if (!Objects.equals(sendUrlMap.get("from_lat"), "0") && !Objects.equals(sendUrlMap.get("lat"), "0")) {
                            if (from_name.equals(to_name)) {
                                insertRecordsOrders(
                                        from_name, from_name,
                                        from_number.getText().toString(), from_number.getText().toString(),
                                        sendUrlMap.get("from_lat"), sendUrlMap.get("from_lng"),
                                        sendUrlMap.get("from_lat"), sendUrlMap.get("from_lng"),
                                        requireContext()
                                );
                            } else {
                                insertRecordsOrders(
                                        from_name, to_name,
                                        from_number.getText().toString(), to_number.getText().toString(),
                                        sendUrlMap.get("from_lat"), sendUrlMap.get("from_lng"),
                                        sendUrlMap.get("lat"), sendUrlMap.get("lng"),
                                        requireContext()
                                );
                            }
                        }
//                    insertRouteCostToDatabase();
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
                        messageResult += ". " + pay_method_message;
                        Intent intent = new Intent(context, FinishActivity.class);
                        intent.putExtra("messageResult_key", messageResult);
                        intent.putExtra("messageCost_key", orderWeb);
                        intent.putExtra("sendUrlMap", new HashMap<>(sendUrlMap));
                        intent.putExtra("UID_key", String.valueOf(sendUrlMap.get("dispatching_order_uid")));
                        startActivity(intent);
                        progressBar.setVisibility(View.GONE);

                    } else {
                        btnVisible(View.INVISIBLE);
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

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    btnVisible(View.VISIBLE);
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
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentType("nal_payment");

                try {
                    if(orderRout()){
                        orderFinished();
                    }
                } catch (UnsupportedEncodingException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    throw new RuntimeException(e);
                }
                progressBar.setVisibility(View.GONE);
                alertDialog.dismiss();
            }
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.GONE);
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    @SuppressLint("ResourceAsColor")
    private boolean orderRout() throws UnsupportedEncodingException {
        if(!verifyOrder(requireContext())) {
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.black_list_message));
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            progressBar.setVisibility(View.GONE);
            return false;
        } else {
            List<String> stringListRoutHome = logCursor(MainActivity.ROUT_HOME, context);
            progressBar.setVisibility(View.GONE);
            if (stringListRoutHome.get(1).equals(" ") && !textViewTo.getText().equals("")) {
                boolean stop = false;
                if (numberFlagFrom.equals("1") && from_number.getText().toString().equals(" ")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        from_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
                        from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                        from_number.requestFocus();
                    } else {
                        ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
                        from_number.requestFocus();
                    }
                    stop = true;
                }
                if (numberFlagTo.equals("1") && to_number.getText().toString().equals(" ")) {
                    to_number.setBackgroundTintList(ColorStateList.valueOf(R.color.selected_text_color));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        to_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
                        to_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                        to_number.requestFocus();
                    } else {
                        ViewCompat.setBackgroundTintList(to_number, ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
                        to_number.requestFocus();
                    }
                    stop = true;

                }
                if (stop) {
                    return false;
                }

                if (numberFlagFrom.equals("1") && !from_number.getText().toString().equals(" ")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        from_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.edit)));
                        from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                        from_number.requestFocus();
                    } else {
                        ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(getResources().getColor(R.color.edit)));
                        from_number.requestFocus();
                    }

                }
                if (numberFlagTo.equals("1") && !to_number.getText().toString().equals(" ")) {
                    to_number.setBackgroundTintList(ColorStateList.valueOf(R.color.selected_text_color));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        to_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.edit)));
                        to_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                        to_number.requestFocus();
                    } else {
                        ViewCompat.setBackgroundTintList(to_number, ColorStateList.valueOf(getResources().getColor(R.color.edit)));
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
                Logger.d(getActivity(), TAG, "order: settings" + settings);
                updateRoutHome(settings);
            }

            urlOrder = getTaxiUrlSearch( "orderSearch", context);
            return true;
        }

    }
    private void updateAddCost(String addCost) {
        ContentValues cv = new ContentValues();
        Logger.d(getActivity(), TAG, "updateAddCost: addCost" + addCost);
        cv.put("addCost", addCost);

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        Logger.d(getActivity(), TAG, "checkPermission: " + permission);
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(context, new String[]{permission}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Logger.d(getActivity(), TAG, "onRequestPermissionsResult: " + requestCode);
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
                Logger.d(getActivity(), TAG, "permissionRequestCount: " + permissionRequestCount);
                // Далее вы можете загрузить сохраненные разрешения и их результаты в любом месте вашего приложения,
                // используя тот же самый объект SharedPreferences
            }
        }
    }
    static void btnVisible(int visible) {
        text_view_cost.setVisibility(visible);
        btn_clear.setVisibility(visible);
        btn_minus.setVisibility(visible);
        btn_plus.setVisibility(visible);
        buttonAddServices.setVisibility(visible);
        buttonBonus.setVisibility(visible);
        btn_clear.setVisibility(visible);
        btn_order.setVisibility(visible);

        if (visible == View.INVISIBLE) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
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
        String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
        Logger.d(requireActivity(), TAG, "onClick No validate: " + val);
        return val;
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onResume() {
        super.onResume();

        new VerifyUserTask(context).execute();

        progressBar.setVisibility(View.GONE);
        pay_method =  logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);

        if(bottomSheetDialogFragment != null) {
            bottomSheetDialogFragment.dismiss();
        }

        addCost = 0;
        updateAddCost(String.valueOf(addCost));
        btn_clear = binding.btnClear;

        textViewTo.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    // Фокус установлен на TextView, очищаем его
                    textViewTo.setText("");
                    to_number.setText("");
                    to_number.setVisibility(View.INVISIBLE);
                }
            }
        });
        List<String> stringListRoutHome = logCursor(MainActivity.ROUT_HOME, context);
        String valueAtIndex1 = stringListRoutHome.get(1);
        rout();


        if (valueAtIndex1 != null && !valueAtIndex1.equals(" ")) {
            textViewFrom.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        // Фокус установлен на TextView, очищаем его
                        resetRoutHome();
                        MainActivity.navController.popBackStack();
                        MainActivity.navController.navigate(R.id.nav_home);
                    }
                }
            });
            costRoutHome(stringListRoutHome);
        } else {
            resetRoutHome();
            updateAddCost("0");
        }


        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetRoutHome();
                MainActivity.navController.popBackStack();
                MainActivity.navController.navigate(R.id.nav_home);

            }
        });

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
                Logger.d(getActivity(), TAG, "onItemClick: to" + to);
                if(connected()) {
                    from = String.valueOf(adapter.getItem(position));
                    if (from.indexOf("/") != -1) {
                        from = from.substring(0,  from.indexOf("/"));
                    }
                    List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                    String city = stringList.get(1);
                    String api =  stringList.get(2);

                    String url = "https://m.easy-order-taxi.site/" + api + "/android/autocompleteSearchComboHid/" + from + "/" + city;

                    Map sendUrlMapCost = null;
                    try {
                        sendUrlMapCost = ResultSONParser.sendURL(url);
                    } catch (MalformedURLException | InterruptedException | JSONException e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.error_firebase_start));
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
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
                            from_number.setVisibility(View.VISIBLE);
                            from_number.requestFocus();
                            numberFlagFrom = "1";
                            from_number.setText("1");
                            cost();
                            from_number.setText(" ");
                            break;
                        case "0":
                            from_number.setText(" ");
                            from_number.setVisibility(View.INVISIBLE);
                            numberFlagFrom = "0";
                            cost();
                            break;
                        default:
                            Logger.d(getActivity(), TAG, "onItemClick: " + new IllegalStateException("Unexpected value: " + Objects.requireNonNull(orderCost)));
                    }
                    progressBar.setVisibility(View.GONE);

                } else {
                    progressBar.setVisibility(View.GONE);
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                }

            }
        });

        from_number.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (!from_number.getText().toString().equals(" ")) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            from_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.edit)));
                            from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                        } else {
                            ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(getResources().getColor(R.color.edit)));
                        }
                   }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        from_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.edit)));
                        from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                    } else {
                        ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(getResources().getColor(R.color.edit)));
                    }
                }
            }
        });
        textViewTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    selectesposition = position; // Обновляем выбранную позицию
//                    adapter.notifyDataSetChanged(); // Обновляем вид списка

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

                        String url = "https://m.easy-order-taxi.site/" + api + "/android/autocompleteSearchComboHid/" + to + "/" + city;

                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException | InterruptedException | JSONException e) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                            Toast.makeText(context, R.string.error_firebase_start, Toast.LENGTH_SHORT).show();
                        }

                        String orderCost = (String) sendUrlMapCost.get("message");
                        Logger.d(getActivity(), TAG, "onItemClick: orderCost" + orderCost);
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
                                to_number.setVisibility(View.VISIBLE);
                                to_number.requestFocus();
                                numberFlagTo = "1";
                                to_number.setText("1");
                                cost();
                                to_number.setText(" ");
                                break;
                            case "0":
                                to_number.setText(" ");
                                to_number.setVisibility(View.INVISIBLE);
                                numberFlagTo = "0";
                                cost();
                                break;
                            default:
                                Logger.d(getActivity(), TAG, "onItemClick: " + new IllegalStateException("Unexpected value: " + Objects.requireNonNull(orderCost)));
                        }
                        if (textViewFrom.getText().toString().isEmpty()) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                textViewFrom.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
                                textViewFrom.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                            } else {
                                ViewCompat.setBackgroundTintList(textViewFrom, ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
                            }

                        }

                }
                    else {
                        bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    }
            }

        });

    }
    @SuppressLint("ResourceAsColor")
    private void cost() {
        textViewTo.setVisibility(View.VISIBLE);
        binding.textwhere.setVisibility(View.VISIBLE);
        binding.num2.setVisibility(View.VISIBLE);
        btn_clear.setVisibility(View.VISIBLE);
        from = textViewFrom.getText().toString();

        if (numberFlagFrom.equals("1") && from_number.getText().toString().equals(" ")) {
            setEditTextBackgroundTint(from_number, R.color.selected_text_color);
            from_numberCost = "1";
        } else {
            if (numberFlagFrom.equals("0")) {
                from_numberCost = " ";
            } else {
                from_numberCost = from_number.getText().toString();
            }
        }

        if (numberFlagTo.equals("1") && to_number.getText().toString().equals(" ")) {
            setEditTextBackgroundTint(to_number, R.color.selected_text_color);
            to_numberCost = "1";
        } else {
            if (numberFlagTo.equals("0")) {
                to_numberCost = " ";
            } else {
                to_numberCost = to_number.getText().toString();
            }
        }

        Logger.d(getActivity(), TAG, "cost: numberFlagTo " + numberFlagTo);

        if (to == null) {
            toCost = from;
            to_numberCost = from_numberCost;
        } else {
            toCost = to;
        }
        List<String> settings = new ArrayList<>();
        String urlCost;
        try {

            settings.add(from);
            settings.add(from_numberCost);
            settings.add(toCost);
            settings.add(to_numberCost);
            updateRoutHome(settings);
            urlCost = getTaxiUrlSearch("costSearch", context);

            CostJSONParserRetrofit parser = new CostJSONParserRetrofit();
            parser.sendURL(urlCost, new Callback<Map<String, String>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    Map<String, String> sendUrlMapCost = response.body();
                    assert sendUrlMapCost != null;
                    handleCostResponse(sendUrlMapCost);
                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });

        } catch (MalformedURLException | UnsupportedEncodingException e) {
            resetRoutHome();
            FirebaseCrashlytics.getInstance().recordException(e);
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
        }
    }

    private void handleCostResponse(Map<String, String> response) {
        String message = response.get("Message");
        String orderCostStr = response.get("order_cost");

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, requireContext());
        long addCost = Long.parseLong(stringListInfo.get(5));

        assert orderCostStr != null;
        long orderCostLong = Long.parseLong(orderCostStr);

        String orderCost = String.valueOf(orderCostLong + addCost);

        if (!orderCost.equals("0")) {
            text_view_cost.setVisibility(View.VISIBLE);
            btn_minus.setVisibility(View.VISIBLE);
            btn_plus.setVisibility(View.VISIBLE);
            buttonAddServices.setVisibility(View.VISIBLE);
            buttonBonus.setVisibility(View.VISIBLE);
            btn_order.setVisibility(View.VISIBLE);

            String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
            long discountInt = Integer.parseInt(discountText);
            Logger.d(getActivity(), TAG, "discountInt: " + discountInt);
            cost = Long.parseLong(orderCost);
            Logger.d(getActivity(), TAG, "cost: " + cost);
            discount = cost * discountInt / 100;
            Logger.d(getActivity(), TAG, "discount: " + discount);
            cost += discount;

            updateAddCost(String.valueOf(discount));
            text_view_cost.setText(Long.toString(cost));

            costFirstForMin = cost;
            MIN_COST_VALUE = (long) (cost * 0.6);
            Logger.d(getActivity(), TAG, "cost: MIN_COST_VALUE "  + MIN_COST_VALUE);

            insertRouteCostToDatabase();

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

    private void insertRouteCostToDatabase() {
        AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, "app-database")
                .addMigrations(AppDatabase.MIGRATION_1_3) // Добавьте миграцию
                .build();
        RouteCostDao routeCostDao = db.routeCostDao();
        int routeId = routeIdToCheck; // Получите routeId
        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif =  stringListInfo.get(2);
        String payment_type =  stringListInfo.get(4);
        String addCost = stringListInfo.get(5);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
//                List<String> servicesChecked = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
//                Logger.d(getActivity(), TAG, "insertRouteCostToDatabase: " + servicesChecked.toString());
                RouteCost existingRouteCost = routeCostDao.getRouteCost(routeId);
                if (existingRouteCost == null) {
                    // Записи с таким routeId ещё нет, выполните вставку
                    RouteCost routeCost = new RouteCost();
                    routeCost.routeId = routeId; // установите уникальный идентификатор
                    routeCost.from = textViewFrom.getText().toString();
                    routeCost.fromNumber = from_number.getText().toString();
                    routeCost.to = textViewTo.getText().toString();
                    routeCost.toNumber = to_number.getText().toString();
                    routeCost.text_view_cost = text_view_cost.getText().toString();
                    routeCost.tarif = tarif;
                    routeCost.payment_type = payment_type;
                    routeCost.addCost = addCost;
//                    routeCost.servicesChecked = servicesChecked;

                    routeCostDao.insert(routeCost);
                } else {
                    // Запись с таким routeId уже существует, выполните обновление
                    existingRouteCost.from = textViewFrom.getText().toString();
                    existingRouteCost.fromNumber = from_number.getText().toString();
                    existingRouteCost.to = textViewTo.getText().toString();
                    existingRouteCost.toNumber = to_number.getText().toString();
                    existingRouteCost.text_view_cost = text_view_cost.getText().toString();
                    existingRouteCost.tarif = tarif;
                    existingRouteCost.payment_type = payment_type;
                    existingRouteCost.addCost = addCost;
//                    existingRouteCost.servicesChecked = servicesChecked;
                    routeCostDao.update(existingRouteCost); // Обновление существующей записи
                }
            }
        });
    }




    private void setEditTextBackgroundTint(EditText editText, @ColorRes int colorResId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            editText.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(colorResId)));
            editText.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
            editText.requestFocus();
        } else {
            ViewCompat.setBackgroundTintList(editText, ColorStateList.valueOf(getResources().getColor(colorResId)));
            editText.requestFocus();
        }
    }


    @SuppressLint({"SetTextI18n", "StaticFieldLeak"})
    private void costRoutHome(final List<String> stringListRoutHome) {
        progressBar.setVisibility(View.VISIBLE);

      new AsyncTask<Integer, Void, RouteCost>() {
            @Override
            protected RouteCost doInBackground(Integer... params) {
                int routeIdToCheck = params[0];
                AppDatabase db = Room.databaseBuilder(context, AppDatabase.class, "app-database")
                        .addMigrations(AppDatabase.MIGRATION_1_3) // Добавьте миграцию
                        .build();
                RouteCostDao routeCostDao = db.routeCostDao();
                return routeCostDao.getRouteCost(routeIdToCheck);
            }

            @SuppressLint("StaticFieldLeak")
            @Override
            protected void onPostExecute(RouteCost retrievedRouteCost) {
                progressBar.setVisibility(View.GONE);
                if (retrievedRouteCost != null) {
                    // Данные с указанным routeId существуют в базе данных
                    textViewFrom.setText(retrievedRouteCost.from);
                    from_number.setText(retrievedRouteCost.fromNumber);
                    textViewTo.setText(retrievedRouteCost.to);
                    to_number.setText(retrievedRouteCost.toNumber);
                    text_view_cost.setVisibility(View.INVISIBLE);
//                    text_view_cost.setText(retrievedRouteCost.text_view_cost);
//                    updateAddCost(retrievedRouteCost.addCost);

                    textViewTo.setVisibility(View.VISIBLE);
                    binding.textwhere.setVisibility(View.VISIBLE);
                    binding.num2.setVisibility(View.VISIBLE);

                    text_view_cost.setVisibility(View.VISIBLE);
                    btn_minus.setVisibility(View.VISIBLE);
                    btn_plus.setVisibility(View.VISIBLE);
                    buttonAddServices.setVisibility(View.VISIBLE);
                    buttonBonus.setVisibility(View.VISIBLE);
                    btn_clear.setVisibility(View.VISIBLE);
                    Logger.d(getActivity(), TAG, "onPostExecute: from_number.getText().toString()" + from_number.getText().toString());
                    if (!from_number.getText().toString().equals(" ")) {
                        from_number.setVisibility(View.VISIBLE);
                    }
                    Logger.d(getActivity(), TAG, "onPostExecute: retrievedRouteCost.toNumber/" + retrievedRouteCost.toNumber +"/");

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
                    Logger.d(getActivity(), TAG, "onPostExecute: addCostforMin" + addCostforMin);
                    MIN_COST_VALUE = (long) ((Long.parseLong(retrievedRouteCost.text_view_cost) - addCostforMin) * 0.6);
                    Logger.d(getActivity(), TAG, "onPostExecute: MIN_COST_VALUE" + MIN_COST_VALUE);
                    btn_order.setVisibility(View.VISIBLE);
                }
                updateAddCost("0");
                updateUIFromList(stringListRoutHome);
            }
        }.execute(routeIdToCheck);

    }

    private void updateUIFromList(List<String> stringListRoutHome) {
        textViewFrom.setText(stringListRoutHome.get(1));

        if (!stringListRoutHome.get(2).equals(" ")) {
            from_number.setVisibility(View.VISIBLE);
            from_number.setText(stringListRoutHome.get(2));
        }
        if (!stringListRoutHome.get(4).equals(" ")) {
            to_number.setText(stringListRoutHome.get(4));
            to_number.setVisibility(View.VISIBLE);
        }
        if (!stringListRoutHome.get(1).equals(stringListRoutHome.get(3))) {
            textViewTo.setText(stringListRoutHome.get(3));
            textViewTo.setVisibility(View.VISIBLE);
            binding.textwhere.setVisibility(View.VISIBLE);
            binding.num2.setVisibility(View.VISIBLE);
        } else {
            to_number.setVisibility(View.INVISIBLE);
        }

        textViewTo.setVisibility(View.VISIBLE);
        binding.textwhere.setVisibility(View.VISIBLE);
        binding.num2.setVisibility(View.VISIBLE);

        text_view_cost.setVisibility(View.VISIBLE);
        btn_minus.setVisibility(View.VISIBLE);
        btn_plus.setVisibility(View.VISIBLE);
        buttonAddServices.setVisibility(View.VISIBLE);
        buttonBonus.setVisibility(View.VISIBLE);
        btn_clear.setVisibility(View.VISIBLE);

        btn_order.setVisibility(View.VISIBLE);

        String urlCost;
        String message;
        try {

            urlCost = getTaxiUrlSearch("costSearch", context);
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
                    String orderCostStr = sendUrlMapCost.get("order_cost");



                    assert orderCostStr != null;
                    long orderCostLong = Long.parseLong(orderCostStr);
                    String orderCost = String.valueOf(orderCostLong + addCost);
                    String message = sendUrlMapCost.get("Message");

                    if (orderCost.equals("0")) {
                        assert message != null;
                        if (message.contains("Дублирование")) {
                            resetRoutHome();
                            message = getResources().getString(R.string.double_order_error);
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
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
                        text_view_cost.setVisibility(View.VISIBLE);
                        btn_minus.setVisibility(View.VISIBLE);
                        btn_plus.setVisibility(View.VISIBLE);
                        buttonAddServices.setVisibility(View.VISIBLE);
                        buttonBonus.setVisibility(View.VISIBLE);
                        btn_order.setVisibility(View.VISIBLE);
                        btn_clear.setVisibility(View.VISIBLE);


                        long discountInt = Integer.parseInt(discountText);
                        Logger.d(getActivity(), TAG, "costRoutHome:discountInt " + discountInt);

                        cost = Long.parseLong(orderCost);
                        discount = cost * discountInt / 100;
                        cost = cost + discount;
                        updateAddCost(String.valueOf(discount));
                        text_view_cost.setText(Long.toString(cost));

                        costFirstForMin = cost;
                        MIN_COST_VALUE = (long) (cost * 0.6);
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



    private boolean verifyOrder(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(MainActivity.TABLE_USER_INFO, context).get(1).equals("0")) {
                verify = false;Logger.d(getActivity(), TAG, "verifyOrder:verify " +verify);
            }
            cursor.close();
        }
        database.close();
        return verify;
    }

    private void updateRecordsUser(String result, Context context) {
        ContentValues cv = new ContentValues();

        cv.put("phone_number", result);

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        int updCount = database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        Logger.d(getActivity(), TAG, "updated rows count = " + updCount);
        database.close();

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
        paymentType("nal_payment");
        text_view_cost.setVisibility(View.INVISIBLE);
        btn_minus.setVisibility(View.INVISIBLE);
        btn_plus.setVisibility(View.INVISIBLE);
        buttonAddServices.setVisibility(View.INVISIBLE);
        buttonBonus.setVisibility(View.INVISIBLE);
        textViewFrom.setText("");
        from_number.setText("");
        from_number.setVisibility(View.INVISIBLE);
        textViewTo.setText("");
        textViewTo.setVisibility(View.INVISIBLE);
        btn_clear.setVisibility(View.INVISIBLE);
        binding.textTo.setVisibility(View.INVISIBLE);
        binding.num2.setVisibility(View.INVISIBLE);
        binding.textwhere.setVisibility(View.INVISIBLE);
        to_number.setVisibility(View.INVISIBLE);
    }


    private String getTaxiUrlSearch(String urlAPI, Context context) throws UnsupportedEncodingException {
        Logger.d(getActivity(), TAG, "startCost: discountText" + logCursor(MainActivity.TABLE_SETTINGS_INFO, context));

        List<String> stringListRout = logCursor(MainActivity.ROUT_HOME, context);
        Logger.d(getActivity(), TAG, "getTaxiUrlSearch: stringListRout" + stringListRout);

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
        String comment = stringList.get(2);
        String date = stringList.get(3);


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

        Logger.d(getActivity(), TAG, "startCost: discountText" + discount);

        Logger.d(getActivity(), TAG, "getTaxiUrlSearch: addCost11111" + addCost);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);

        if(urlAPI.equals("costSearch")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }

            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail  + "*" + payment_type;
        }


        if(urlAPI.equals("orderSearch")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);

            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail  + "*" + payment_type + "/" + addCost + "/" + time + "/" + comment + "/" + date;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("comment", "no_comment");
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
            Logger.d(getActivity(), TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }


        String url = "/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);

        Logger.d(getActivity(), TAG, "getTaxiUrlSearch: " + url);

        database.close();

        return url;
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
                            paymentType("nal_payment");
                        }
                        break;
                    case "card_payment":
                    case "fondy_payment":
                    case "mono_payment":
                    case "wfp_payment":
                        if (Long.parseLong(card_max_pay) <= Long.parseLong(textCost)) {
                            paymentType("nal_payment");
                        }
                        break;
                }

                try {
                    if (orderRout()) {
                        orderFinished();
                    }
                } catch (UnsupportedEncodingException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    throw new RuntimeException(e);
                }

                progressBar.setVisibility(View.GONE);
                alertDialog.dismiss();
            }
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeFragment.progressBar.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
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
    private void getPhoneNumber () {
        String mPhoneNumber;
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Logger.d(getActivity(), TAG, "Manifest.permission.READ_PHONE_NUMBERS: " + ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS));
            Logger.d(getActivity(), TAG, "Manifest.permission.READ_PHONE_STATE: " + ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE));
            return;
        }
        mPhoneNumber = tMgr.getLine1Number();
//        mPhoneNumber = null;
        if(mPhoneNumber != null) {
            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
            boolean val = Pattern.compile(PHONE_PATTERN).matcher(mPhoneNumber).matches();
            Logger.d(getActivity(), TAG, "onClick No validate: " + val);
            if (!val) {
                Toast.makeText(context, getString(R.string.format_phone) , Toast.LENGTH_SHORT).show();
                Logger.d(getActivity(), TAG, "onClick:phoneNumber.getText().toString() " + mPhoneNumber);
//                context.finish();

            } else {
                updateRecordsUser(mPhoneNumber, context);
            }
        }

    }

    public static void insertRecordsOrders(String from, String to,
                                    String from_number, String to_number,
                                    String from_lat, String from_lng,
                                    String to_lat, String to_lng, Context context) {

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
        database.close();
        cursor_from.close();
        cursor_to.close();

    }
    private void paymentType(String paymentCode) {
        ContentValues cv = new ContentValues();
        cv.put("payment_type", paymentCode);
        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

}