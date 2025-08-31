package com.taxi.easy.ua.ui.gallery;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentGalleryBinding;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetBonusFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetDialogFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetGalleryFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.to_json_parser.ToJSONParserRetrofit;
import com.uxcam.UXCam;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GalleryFragment extends Fragment {

    private static final String TAG = "GalleryFragment";
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressbar;
    private FragmentGalleryBinding binding;
    private ListView listView;
    private String[] array;
    @SuppressLint("StaticFieldLeak")
    public static TextView textView, text_view_cost;
    String from_mes, to_mes;
    public static AppCompatButton del_but, btnRouts, btn_minus, btn_plus, btnAdd, buttonBonus, btnCallAdmin;
    int selectedItem;
    String FromAddressString, ToAddressString;
    public static long  addCost, cost;
    public static Double from_lat;
    public static Double from_lng;
    public static Double to_lat;
    public static Double to_lng;
    long MIN_COST_VALUE;
    private String pay_method;
    public static long costFirstForMin;
    private ArrayAdapter<String> listAdapter;
    private String urlOrder;
    private long discount;
    private ImageButton scrollButtonDown, scrollButtonUp;
    private AlertDialog alertDialog;
    FragmentManager fragmentManager;

    private int desiredHeight;
    @SuppressLint("StaticFieldLeak")
    public static TextView schedule;
    ImageButton shed_down;
    @SuppressLint("StaticFieldLeak")
    static ConstraintLayout constr2;
    Context context;
    String baseUrl;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);
        if(button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context = requireActivity();
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        }

//        if (!NetworkUtils.isNetworkAvailable(context)) {
//
//            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                    .setPopUpTo(R.id.nav_restart, true)
//                    .build());
//        }
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        scrollButtonUp = binding.scrollButtonUp;
        scrollButtonDown = binding.scrollButtonDown;

        addCost = 0;
        updateAddCost(String.valueOf(addCost));

        progressbar = binding.progressBar;

        textView = binding.textGallery;
        textView.setText(R.string.my_routs);

        listView = binding.listView;

        btnCallAdmin = binding.btnCallAdmin;
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
            String phone = stringList.get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });
        del_but = binding.delBut;
        del_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteRouts();
            }
        });
        btnRouts = binding.btnRouts;
        text_view_cost = binding.textViewCost;
        btn_minus = binding.btnMinus;
        btn_plus = binding.btnPlus;

        btn_minus.setOnClickListener(v -> {
            List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
            addCost = Long.parseLong(stringListInfo.get(5));
            cost = Long.parseLong(text_view_cost.getText().toString());
            cost -= 5;
            addCost -= 5;
            if (cost <= MIN_COST_VALUE) {
                cost = MIN_COST_VALUE;
                addCost = MIN_COST_VALUE - costFirstForMin;
            }
            updateAddCost(String.valueOf(addCost));
            text_view_cost.setText(String.valueOf(cost));
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
        btnAdd = binding.btnAdd;
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyBottomSheetGalleryFragment bottomSheetDialogFragment = new MyBottomSheetGalleryFragment();
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });

        btnRouts.setVisibility(View.INVISIBLE);

        array = arrayToRoutsAdapter ();

        if(array != null) {
            scrollButtonDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Определяем следующую позицию для прокрутки
                    int nextVisiblePosition = listView.getLastVisiblePosition() + 1;

                    // Проверяем, чтобы не прокручивать за пределы списка
                    if (nextVisiblePosition < array.length) {
                        // Плавно прокручиваем к следующей позиции
                        listView.smoothScrollToPosition(nextVisiblePosition);
                    }
                }
            });

            scrollButtonUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int offset = -1; // или другое значение, чтобы указать направление прокрутки
                    listView.smoothScrollByOffset(offset);
                }
            });

            listAdapter = new ArrayAdapter<>(context, R.layout.services_adapter_layout, array);
            listView.setAdapter(listAdapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            registerForContextMenu(listView);


            listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    root.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Теперь мы можем получить высоту фрагмента
                    desiredHeight = root.getHeight() - 100;
                    ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
                    layoutParams.height = desiredHeight;
                    listView.setLayoutParams(layoutParams);

                    int totalItemHeight = 0;
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        totalItemHeight += listView.getChildAt(i).getHeight();
                    }

                    if (totalItemHeight > desiredHeight) {
                        scrollButtonUp.setVisibility(View.VISIBLE);
                        scrollButtonDown.setVisibility(View.VISIBLE);
                    } else {
                        scrollButtonUp.setVisibility(View.GONE);
                        scrollButtonDown.setVisibility(View.GONE);
                    }

                    // Убираем слушатель, чтобы он не срабатывал многократно
//                    listView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    progressbar.setVisibility(View.VISIBLE);
                    desiredHeight = 500; // Ваше желаемое значение высоты в пикселях
                    ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
                    layoutParams.height = desiredHeight;
                    int totalItemHeight = 0;
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        totalItemHeight += listView.getChildAt(i).getHeight();
                    }

                    if (totalItemHeight > desiredHeight) {
                        scrollButtonUp.setVisibility(View.VISIBLE);
                        scrollButtonDown.setVisibility(View.VISIBLE);
                    } else {
                        scrollButtonUp.setVisibility(View.GONE);
                        scrollButtonDown.setVisibility(View.GONE);
                    }

                    selectedItem = position + 1;
                    Logger.d(getActivity(), TAG, "onItemClick: selectedItem " + selectedItem);
                    try {
                        dialogFromToOneRout(routChoice(selectedItem));
                    } catch (MalformedURLException | InterruptedException | JSONException e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        Logger.d(getActivity(), TAG, "onItemClick: " + e);
                    }


                }
            });
        } else {
            textView.setText(R.string.no_routs);

        }
        btnRouts.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                progressbar.setVisibility(View.VISIBLE);
                List<String> stringList = logCursor(MainActivity.CITY_INFO, context);

                pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);

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
                            if(Long.parseLong(bonus) < Long.parseLong(text_view_cost.getText().toString()) * 100 ) {
                                paymentType();
                            }
                        }
                        break;
                }

                List<String> stringListCity = logCursor(MainActivity.CITY_INFO, context);
                String card_max_pay = stringListCity.get(4);
                String bonus_max_pay = stringListCity.get(5);
                switch (pay_method) {
                    case "bonus_payment":
                        if (Long.parseLong(bonus_max_pay) <= Long.parseLong(text_view_cost.getText().toString()) * 100) {
                            changePayMethodMax(text_view_cost.getText().toString(), pay_method);
                        } else {
                            if(orderRout()) {
                                orderFinished();
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
                            if(orderRout()) {
                                orderFinished();
                            }
                        }
                        break;
                    default:
                        if(orderRout()) {
                            orderFinished();
                        }
                }
            }
        });

        buttonBonus = binding.btnBonus;

        buttonBonus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
                String api =  stringList.get(2);
                updateAddCost("0");
                if (!text_view_cost.getText().toString().isEmpty()) {
                    MyBottomSheetBonusFragment bottomSheetDialogFragment = new MyBottomSheetBonusFragment(Long.parseLong(text_view_cost.getText().toString()), "marker", api, text_view_cost) ;
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }

            }
        });


        schedule = binding.schedule;
        shed_down = binding.shedDown;

        constr2 = binding.constr2;
        btnVisible(View.INVISIBLE);
        constr2.setVisibility(View.INVISIBLE);
        return root;
    }
    private void scheduleUpdate() {
        schedule.setText(R.string.on_now);
        if(!MainActivity.firstStart) {
            ContentValues cv = new ContentValues();
            cv.put("time", "no_time");
            cv.put("date", "no_date");

            // обновляем по id
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });
            database.close();
        }

        schedule.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            MyBottomSheetGalleryFragment bottomSheetDialogFragment = new MyBottomSheetGalleryFragment();
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        });

        shed_down.setOnClickListener(v -> {
            btnVisible(View.INVISIBLE);
            MyBottomSheetDialogFragment bottomSheetDialogFragment = new MyBottomSheetDialogFragment();
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
        });
        constr2.setVisibility(View.VISIBLE);
        addCheck(context);
    }
    private String cleanString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ").replaceAll("\\s{2,}$", " ");
    }
    public static void  addCheck(Context context) {

        int newCheck = 0;
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        for (int i = 0; i < DataArr.arrayServiceCode().length; i++) {
            if(services.get(i+1).equals("1")) {
                newCheck++;
            }
        }
        String mes = context.getString(R.string.add_services);
        if(newCheck != 0) {
            mes = context.getString(R.string.add_services) + " (" + newCheck + ")";
        }
        btnAdd.setText(mes);

    }
    public static void btnVisible(int visible) {
        constr2.setVisibility(visible);
        del_but.setVisibility(visible);
        text_view_cost.setVisibility(visible);
        btnRouts.setVisibility(visible);
        btn_minus.setVisibility(visible);
        btn_plus.setVisibility(visible);
        btnAdd.setVisibility(visible);
        buttonBonus.setVisibility(visible);


        if (visible == View.INVISIBLE) {
            progressbar.setVisibility(View.GONE);
        } else {
            progressbar.setVisibility(View.GONE);
        }
    }
    @SuppressLint("ResourceAsColor")
    private boolean orderRout() {
        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
            return false;
        }

        else {
            if (verifyOrder()) {
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.black_list_message));
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                progressbar.setVisibility(View.INVISIBLE);
                return false;
            } else {
                urlOrder = getTaxiUrlSearchMarkers("orderSearchMarkersVisicom", context);
                progressbar.setVisibility(View.INVISIBLE);
                return true;
            }

        }
    }

    private boolean verifyOrder() {
        return (boolean) sharedPreferencesHelperMain.getValue("verifyUserOrder", false);
    }

    private void orderFinished() {
        btnVisible(View.INVISIBLE);
        Toast.makeText(context, R.string.check_order_mes, Toast.LENGTH_SHORT).show();
        ToJSONParserRetrofit parser = new ToJSONParserRetrofit();

//            // Пример строки URL с параметрами
        Logger.d(getActivity(), TAG, "orderFinished: "  + baseUrl+ urlOrder);
        parser.sendURL(urlOrder, new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {

                Map<String, String> sendUrlMap = response.body();

                assert sendUrlMap != null;
                String orderWeb = sendUrlMap.get("order_cost");
                String message = sendUrlMap.get("message");

                assert orderWeb != null;
                if (!orderWeb.equals("0")) {
                    String to_name;
                    if (Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
                        to_name = getString(R.string.on_city_tv);
                    } else {
                        if(Objects.equals(sendUrlMap.get("routeto"), "Точка на карте")) {
                            to_name = context.getString(R.string.end_point_marker);
                        } else {
                            to_name = sendUrlMap.get("routeto") + " " + sendUrlMap.get("to_number");
                        }
                    }
                    String pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(4);
                    String pay_method_message = getString(R.string.pay_method_message_main);
                    String required_time = sendUrlMap.get("required_time");
                    if(required_time != null && !required_time.contains("1970-01-01")) {
                        required_time = " " + context.getString(R.string.time_order) + required_time + ". ";
                    } else {
                        required_time = "";
                    }
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
                    String to_name_local = to_name;
                    if(to_name.contains("по місту")
                            ||to_name.contains("по городу")
                            || to_name.contains("around the city")
                    ) {
                        to_name_local = getString(R.string.on_city_tv);
                    }

                    String messageResult =
                            sendUrlMap.get("routefrom")
                            + sendUrlMap.get("routefromnumber") + " " + getString(R.string.to_message) +
                            to_name_local + "." +
                            required_time;
                    String messagePayment = orderWeb + " " + getString(R.string.UAH) + " " + pay_method_message;
                    messageResult = cleanString(messageResult);

                    String messageFondy = getString(R.string.fondy_message) + " " +
                            sendUrlMap.get("routefrom") + sendUrlMap.get("routefromnumber") + " " + getString(R.string.to_message) +
                            to_name_local + ".";

                    Bundle bundle = new Bundle();
                    bundle.putString("messageResult_key", messageResult);
                    bundle.putString("messagePay_key", messagePayment);
                    bundle.putString("messageFondy_key", messageFondy);
                    bundle.putString("messageCost_key", orderWeb);
                    bundle.putSerializable("sendUrlMap", new HashMap<>(sendUrlMap));
                    bundle.putString("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));

// Установите Bundle как аргументы фрагмента
                    MainActivity.navController.navigate(R.id.nav_finish_separate, bundle, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_visicom, true)
                            .build());
                } else {
                    assert message != null;
                    if (message.contains("Дублирование")) {
                        message = getResources().getString(R.string.double_order_error);
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                    } else if (message.equals("ErrorMessage")) {
                        message = getResources().getString(R.string.server_error_connected);
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                    }else {
                        switch (pay_method) {
                            case "bonus_payment":
                            case "card_payment":
                            case "fondy_payment":
                            case "mono_payment":
                            case "wfp_payment":
                                changePayMethodToNal();
                                break;
                            default:
                                btnVisible(View.VISIBLE);
                                message = getResources().getString(R.string.error_message);
                                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        }
                    }
                    btnVisible(View.VISIBLE);

                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                btnVisible(View.VISIBLE);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });

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

            if(orderRout()){
                orderFinished();
            }
            progressbar.setVisibility(View.GONE);
            alertDialog.dismiss();
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(v -> {
            progressbar.setVisibility(View.GONE);
            alertDialog.dismiss();
        });

        alertDialog.show();
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

     private void updateRoutMarker(List<String> settings) {

        Logger.d(getActivity(), TAG, "updateRoutMarker: settings - " + settings);

        ContentValues cv = new ContentValues();

        cv.put("startLat",  Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
         cv.put("start", settings.get(4));
         cv.put("finish", settings.get(5));

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }


    private void dialogFromToOneRout(Map <String, String> rout) throws MalformedURLException, InterruptedException, JSONException {
        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        }

//        if (!NetworkUtils.isNetworkAvailable(context)) {
//
//            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                    .setPopUpTo(R.id.nav_restart, true)
//                    .build());
//        }

        else  {
            Logger.d(getActivity(), TAG, "dialogFromToOneRout: " + rout.toString());
            from_lat =  Double.valueOf(rout.get("from_lat"));
            from_lng = Double.valueOf(rout.get("from_lng"));
            to_lat = Double.valueOf(rout.get("to_lat"));
            to_lng = Double.valueOf(rout.get("to_lng"));

            Logger.d(getActivity(), TAG, "dialogFromToOneRout: from_lat - " + from_lat);
            Logger.d(getActivity(), TAG, "dialogFromToOneRout: from_lng - " + from_lng);
            Logger.d(getActivity(), TAG, "dialogFromToOneRout: to_lat - " + to_lat);
            Logger.d(getActivity(), TAG, "dialogFromToOneRout: to_lng - " + to_lng);

            FromAddressString = rout.get("from_street") + rout.get("from_number") ;
            Logger.d(getActivity(), TAG, "dialogFromToOneRout: FromAddressString" + FromAddressString);
            ToAddressString = rout.get("to_street") + rout.get("to_number");
            if(rout.get("from_street").equals(rout.get("to_street"))) {
                ToAddressString =  getString(R.string.on_city_tv);
            }
            Logger.d(getActivity(), TAG, "dialogFromToOneRout: ToAddressString" + ToAddressString);
            List<String> settings = new ArrayList<>();

            settings.add(rout.get("from_lat"));
            settings.add(rout.get("from_lng"));
            settings.add(rout.get("to_lat"));
            settings.add(rout.get("to_lng"));

            settings.add(FromAddressString);
            settings.add(ToAddressString);

            updateRoutMarker(settings);
            String urlCost = getTaxiUrlSearchMarkers("costSearchMarkersTime", context);

            ToJSONParserRetrofit parser = new ToJSONParserRetrofit();
            baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
//            // Пример строки URL с параметрами
            Logger.d(getActivity(), TAG, "orderFinished: "  + baseUrl + urlCost);
            parser.sendURL(urlCost, new Callback<Map<String, String>>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    progressbar.setVisibility(View.GONE);
                    Map<String, String> sendUrlMapCost = response.body();

                    String message = context.getString(R.string.error_message);
                    String orderCost = sendUrlMapCost.get("order_cost");
                    Logger.d(getActivity(), TAG, "dialogFromToOneRout:orderCost " + orderCost);
                    assert orderCost != null;
                    if (!orderCost.equals("0")) {
                        scheduleUpdate();
                        String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(3);
                        long discountInt = Integer.parseInt(discountText);
                        cost = Long.parseLong(orderCost);
                        discount = cost * discountInt / 100;

                        cost += discount;
                        updateAddCost(String.valueOf(discount));
                        text_view_cost.setText(String.valueOf(cost));

                        costFirstForMin = cost;
                        MIN_COST_VALUE = (long) (cost*0.6);
                        btnVisible(View.VISIBLE);
                      } else {
                        message = getString(R.string.error_message);
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        btnVisible(View.INVISIBLE);                   }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                }
            });

        }
        progressbar.setVisibility(View.GONE);
    }
    private Map <String, String> routChoice(int i) {
        Map <String, String> rout = new HashMap<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = database.query(MainActivity.TABLE_ORDERS_INFO, null, null, null, null, null, null);
        c.move(i);
        rout.put("id", c.getString(c.getColumnIndexOrThrow ("id")));
        rout.put("from_lat", c.getString(c.getColumnIndexOrThrow ("from_lat")));
        rout.put("from_lng", c.getString(c.getColumnIndexOrThrow ("from_lng")));
        rout.put("to_lat", c.getString(c.getColumnIndexOrThrow ("to_lat")));
        rout.put("to_lng", c.getString(c.getColumnIndexOrThrow ("to_lng")));
        rout.put("from_street", c.getString(c.getColumnIndexOrThrow ("from_street")));
        rout.put("from_number", c.getString(c.getColumnIndexOrThrow ("from_number")));
        rout.put("to_street", c.getString(c.getColumnIndexOrThrow ("to_street")));
        rout.put("to_number", c.getString(c.getColumnIndexOrThrow ("to_number")));

        Logger.d(getActivity(), TAG, "routMaps: " + rout);
        return rout;
    }

    @SuppressLint("Range")
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Logger.d(getActivity(), TAG, "getTaxiUrlSearchMarkers: " + urlAPI);

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

        // Заменяем символ '/' в строках
        start = start.replace("/", "|");
        finish = finish.replace("/", "|");

        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;

        cursor.close();

        List<String> listCity = logCursor(MainActivity.CITY_INFO, context);
        String city = listCity.get(1);
        String api = listCity.get(2);

        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
//        String comment = stringList.get(2);
        String comment = sharedPreferencesHelperMain.getValue("comment", "no_comment").toString();
        String date = stringList.get(3);



        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, context);
        String tarif =  stringListInfo.get(2);
        String payment_type = stringListInfo.get(4);
        String addCost = stringListInfo.get(5);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO, context).get(4);

        if(urlAPI.equals("costSearchMarkersTime")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail  + "*" + payment_type+ "/"
                    + time + "/" + date ;
        }
        if(urlAPI.equals("orderSearchMarkersVisicom")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);


            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail  + "*" + payment_type + "/" + addCost + "/"
                    + time + "/" + comment + "/" + date+ "/" + start + "/" + finish;

            ContentValues cv = new ContentValues();

            cv.put("time", "no_time");
            cv.put("comment", "no_comment");
            cv.put("date", "no_date");

            // обновляем по id
            database.update(MainActivity.TABLE_ADD_SERVICE_INFO, cv, "id = ?",
                    new String[] { "1" });

        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO, context);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i < services.size()-1 ; i++) {
            if(services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if(servicesVer) {
            for (int i = 0; i < DataArr.arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(DataArr.arrayServiceCode()[i]);
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
        Logger.d(getActivity(), TAG, "getTaxiUrlSearchMarkers: " + url);

        database.close();

        return url;
    }

    private void changePayMethodMax(String textCost, String paymentType) {
        List<String> stringListCity = logCursor(MainActivity.CITY_INFO, context);
        String card_max_pay =  stringListCity.get(4);
        String bonus_max_pay =  stringListCity.get(5);

        // Инфлейтим макет для кастомного диалога
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);

        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
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
                if(orderRout()) {
                    orderFinished();
                }
                progressbar.setVisibility(View.GONE);
                alertDialog.dismiss();
            }
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressbar.setVisibility(View.GONE);
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }


    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
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

        return list;
    }
    private void reIndexOrders() {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE  temp_table" + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " from_lat text," +
                " from_lng text," +
                " to_street text," +
                " to_number text," +
                " to_lat text," +
                " to_lng text);");
        // Копирование данных из старой таблицы во временную
        database.execSQL("INSERT INTO temp_table SELECT * FROM " + MainActivity.TABLE_ORDERS_INFO);

        // Удаление старой таблицы
        database.execSQL("DROP TABLE " + MainActivity.TABLE_ORDERS_INFO);

        // Создание новой таблицы
        database.execSQL("CREATE TABLE " + MainActivity.TABLE_ORDERS_INFO + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " from_lat text," +
                " from_lng text," +
                " to_street text," +
                " to_number text," +
                " to_lat text," +
                " to_lng text);");

        String query = "INSERT INTO " + MainActivity.TABLE_ORDERS_INFO + " (from_street, from_number, from_lat, from_lng, to_street, to_number, to_lat, to_lng) " +
                "SELECT from_street, from_number, from_lat, from_lng, to_street, to_number, to_lat, to_lng FROM temp_table";

        // Копирование данных из временной таблицы в новую
        database.execSQL(query);

        // Удаление временной таблицы
        database.execSQL("DROP TABLE temp_table");
        database.close();
    }
    private void deleteRouts () {
        SparseBooleanArray checkespositions = listView.getCheckedItemPositions();
        ArrayList<Integer> selectespositions = new ArrayList<>();

        for (int i = 0; i < checkespositions.size(); i++) {
            int pos = checkespositions.keyAt(i);
            if (checkespositions.get(pos)) {
                selectespositions.add(pos);
            }
        }

        for (int position : selectespositions) {
            int i = position + 1;

            String deleteQuery = "DELETE FROM " + MainActivity.TABLE_ORDERS_INFO + " WHERE id = " + i  + ";";
            SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

            database.execSQL(deleteQuery);
            database.close();
        }
        reIndexOrders();
        array = arrayToRoutsAdapter();
        if (array != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.services_adapter_layout, array);
            listView.setAdapter(adapter);
        } else {
            // Если массив пустой, отобразите текст "no_routs" вместо списка
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.services_adapter_layout, new String[]{});
            listView.setAdapter(adapter);
            textView.setText(R.string.no_routs);
            btnVisible(View.INVISIBLE);

        }
        btnVisible(View.INVISIBLE);

    }
    private String[] arrayToRoutsAdapter() {
        ArrayList<Map> routMaps = routMaps(context);
        String[] arrayRouts;
        if(routMaps.size() != 0) {
            arrayRouts = new String[routMaps.size()];
            for (int i = 0; i < routMaps.size(); i++) {
                if(routMaps.get(i).get("from_street").toString().equals("Місце відправлення")) {
                    from_mes = getString(R.string.start_point_text);
                }
                else {
                    from_mes = routMaps.get(i).get("from_street").toString();
                }

                if(routMaps.get(i).get("to_street").toString().equals("Місце призначення")) {
                    to_mes = getString(R.string.end_point_marker);
                }
                else {
                    to_mes = routMaps.get(i).get("to_street").toString();
                }


                if(!routMaps.get(i).get("from_street").toString().equals(routMaps.get(i).get("to_street").toString())) {
                    if (!routMaps.get(i).get("from_street").toString().equals(routMaps.get(i).get("from_number").toString())) {


                        Logger.d(getActivity(), TAG, "arrayToRoutsAdapter:   routMaps.get(i).get(\"from_street\").toString()" +  routMaps.get(i).get("from_street").toString());

                        arrayRouts[i] = from_mes + " " +
                                routMaps.get(i).get("from_number").toString() + " -> " +
                                to_mes + " " +
                                routMaps.get(i).get("to_number").toString();
                    } else if(!routMaps.get(i).get("to_street").toString().equals(routMaps.get(i).get("to_number").toString())) {
                        Logger.d(getActivity(), TAG, "arrayToRoutsAdapter:   routMaps.get(i).get(\"from_street\").toString()" +  routMaps.get(i).get("from_street").toString());

                        arrayRouts[i] = routMaps.get(i).get("from_street").toString() +
                                getString(R.string.to_message) +
                                routMaps.get(i).get("to_street").toString() + " " +
                                routMaps.get(i).get("to_number").toString();
                    } else {

                        Logger.d(getActivity(), TAG, "arrayToRoutsAdapter:   routMaps.get(i).get(\"from_street\").toString()" +  routMaps.get(i).get("from_street").toString());

                        arrayRouts[i] = from_mes + " " +
                                getString(R.string.to_message) +
                                to_mes;

                    }

                } else {

                    Logger.d(getActivity(), TAG, "arrayToRoutsAdapter:   routMaps.get(i).get(\"from_street\").toString()" +  routMaps.get(i).get("from_street").toString());

                    arrayRouts[i] = from_mes + " " +
                            routMaps.get(i).get("from_number").toString() + " -> " +
                            getString(R.string.on_city_tv);
                }

            }
        } else {
            arrayRouts = null;
        }
        return arrayRouts;
    }
    private ArrayList<Map> routMaps(Context context) {
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
        Logger.d(getActivity(), TAG, "routMaps: 1111 " + routsArr);
        return routsArr;
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
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        

        Logger.d(getActivity(), TAG, "onResume: selectedItem " + selectedItem);
        listView.clearChoices();
        listView.requestLayout(); // Обновляем визуальное состояние списка
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged(); // Обновляем адаптер
        }
        del_but.setVisibility(View.INVISIBLE);
        text_view_cost.setVisibility(View.INVISIBLE);
        btnRouts.setVisibility(View.INVISIBLE);
        btn_minus.setVisibility(View.INVISIBLE);
        btn_plus.setVisibility(View.INVISIBLE);
        btnAdd.setVisibility(View.INVISIBLE);
        buttonBonus.setVisibility(View.INVISIBLE);
    }
}