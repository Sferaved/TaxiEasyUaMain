package com.taxi.easy.ua.ui.gallery;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentGalleryBinding;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.home.MyBottomSheetBonusFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetGalleryFragment;
import com.taxi.easy.ua.ui.maps.ToJSONParser;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import com.taxi.easy.ua.utils.connect.NetworkUtils;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GalleryFragment extends Fragment {

    private static final String TAG = "TAG_GEL";
    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressbar;
    private FragmentGalleryBinding binding;
    private ListView listView;
    private String[] array;
    @SuppressLint("StaticFieldLeak")
    public static TextView textView, text_view_cost;
    String from_mes, to_mes;
    public static AppCompatButton del_but, btnRouts, btn_minus, btn_plus, btnAdd, buttonBonus;
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
    NavController navController;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            navController.navigate(R.id.nav_visicom);
        }

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        scrollButtonUp = binding.scrollButtonUp;
        scrollButtonDown = binding.scrollButtonDown;

        addCost = 0;
        updateAddCost(String.valueOf(addCost));

        progressbar = binding.progressBar;

        textView = binding.textGallery;
        textView.setText(R.string.my_routs);

        listView = binding.listView;

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
            List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, requireActivity());
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
                List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO, requireActivity());
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

            listAdapter = new ArrayAdapter<>(requireActivity(), R.layout.services_adapter_layout, array);
            listView.setAdapter(listAdapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            registerForContextMenu(listView);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    int desiredHeight = 500; // Ваше желаемое значение высоты в пикселях
                    ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
                    layoutParams.height = desiredHeight;
                    listView.setLayoutParams(layoutParams);
                    scrollButtonDown.setVisibility(View.VISIBLE);
                    scrollButtonUp.setVisibility(View.VISIBLE);

                    del_but.setVisibility(View.VISIBLE);
                    btnRouts.setVisibility(View.VISIBLE);
                    text_view_cost.setVisibility(View.VISIBLE);
                    buttonBonus.setVisibility(View.VISIBLE);
                    btn_minus.setVisibility(View.VISIBLE);
                    btn_plus.setVisibility(View.VISIBLE);
                    btnAdd.setVisibility(View.VISIBLE);
                    selectedItem = position + 1;
                    Log.d(TAG, "onItemClick: selectedItem " + selectedItem);
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            dialogFromToOneRout(routChoice(selectedItem));
                        }
                    } catch (MalformedURLException | InterruptedException | JSONException e) {
                        Log.d(TAG, "onItemClick: " + e.toString());
                    }


                }
            });
        } else {
            textView.setText(R.string.no_routs);

        }
        btnRouts.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());

                pay_method = logCursor(MainActivity.TABLE_SETTINGS_INFO, requireActivity()).get(4);

                switch (stringList.get(1)) {
                    case "Kyiv City":
                    case "Dnipropetrovsk Oblast":
                    case "Odessa":
                    case "Zaporizhzhia":
                    case "Cherkasy Oblast":
                        break;
                    case "OdessaTest":
                         if(pay_method.equals("bonus_payment")) {
                            String bonus = logCursor(MainActivity.TABLE_USER_INFO, requireActivity()).get(5);
                            if(Long.parseLong(bonus) < Long.parseLong(text_view_cost.getText().toString()) * 100 ) {
                                paymentType("nal_payment");
                            }
                        }
                        break;
                }
                progressbar.setVisibility(View.VISIBLE);
                List<String> stringListCity = logCursor(MainActivity.CITY_INFO, requireActivity());
                String card_max_pay = stringListCity.get(4);
                String bonus_max_pay = stringListCity.get(5);
                switch (pay_method) {
                    case "bonus_payment":
                        if (Long.parseLong(bonus_max_pay) <= Long.parseLong(text_view_cost.getText().toString()) * 100) {
                            changePayMethodMax(text_view_cost.getText().toString(), pay_method);
                        } else {
                            orderRout();
                            orderFinished();
                        }
                        break;
                    case "card_payment":
                    case "fondy_payment":
                    case "mono_payment":
                        if (Long.parseLong(card_max_pay) <= Long.parseLong(text_view_cost.getText().toString())) {
                            changePayMethodMax(text_view_cost.getText().toString(), pay_method);
                        } else {
                            orderRout();
                            orderFinished();
                        }
                        break;
                    default:
                        orderRout();
                        orderFinished();
                        break;
                }
            }
        });

        buttonBonus = binding.btnBonus;

        buttonBonus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
                String api =  stringList.get(2);
                updateAddCost("0");
                MyBottomSheetBonusFragment bottomSheetDialogFragment = new MyBottomSheetBonusFragment(Long.parseLong(text_view_cost.getText().toString()), "marker", api, text_view_cost) ;
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });
        del_but.setVisibility(View.INVISIBLE);
        text_view_cost.setVisibility(View.INVISIBLE);
        btnRouts.setVisibility(View.INVISIBLE);
        btn_minus.setVisibility(View.INVISIBLE);
        btn_plus.setVisibility(View.INVISIBLE);
        btnAdd.setVisibility(View.INVISIBLE);
        buttonBonus.setVisibility(View.INVISIBLE);

        return root;
    }

    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void orderRout() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            navController.navigate(R.id.nav_visicom);
        } else {
            urlOrder = getTaxiUrlSearchMarkers("orderSearchMarkersVisicom", requireActivity());
        }
    }
    private void orderFinished() {
        try {
            Map<String, String> sendUrlMap = ToJSONParser.sendURL(urlOrder);
            Log.d(TAG, "Map sendUrlMap = ToJSONParser.sendURL(urlOrder); " + sendUrlMap);

            String orderWeb = sendUrlMap.get("order_cost");

            assert orderWeb != null;
            if (!orderWeb.equals("0")) {
                String to_name;
                if (Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
                    to_name = getString(R.string.on_city_tv);
                } else {
                    if(Objects.equals(sendUrlMap.get("routeto"), "Точка на карте")) {
                        to_name = requireActivity().getString(R.string.end_point_marker);
                    } else {
                        to_name = sendUrlMap.get("routeto") + " " + sendUrlMap.get("to_number");
                    }
                }
                String messageResult = getString(R.string.thanks_message) +
                        sendUrlMap.get("routefrom") + sendUrlMap.get("routefromnumber") + " " + getString(R.string.to_message) +
                        to_name + "." +
                        getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);
                String messageFondy = getString(R.string.fondy_message) + " " +
                        sendUrlMap.get("routefrom") + sendUrlMap.get("routefromnumber") + " " + getString(R.string.to_message) +
                        to_name + ".";

                Intent intent = new Intent(requireActivity(), FinishActivity.class);
                intent.putExtra("messageResult_key", messageResult);
                intent.putExtra("messageFondy_key", messageFondy);
                intent.putExtra("messageCost_key", orderWeb);
                intent.putExtra("sendUrlMap", new HashMap<>(sendUrlMap));
                intent.putExtra("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
                startActivity(intent);
            } else {
                String message = requireActivity().getString(R.string.error_message);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                OpenStreetMapActivity.progressBar.setVisibility(View.INVISIBLE);
            }


        } catch (MalformedURLException ignored) {

        }
    }

    private void paymentType(String paymentCode) {
        ContentValues cv = new ContentValues();
        cv.put("payment_type", paymentCode);
        // обновляем по id
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

     private void updateRoutMarker(List<String> settings) {

        Log.d(TAG, "updateRoutMarker: settings - " + settings);

        ContentValues cv = new ContentValues();

        cv.put("startLat",  Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
         cv.put("start", settings.get(4));
         cv.put("finish", settings.get(5));

        // обновляем по id
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void dialogFromToOneRout(Map <String, String> rout) throws MalformedURLException, InterruptedException, JSONException {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            navController.navigate(R.id.nav_visicom);
        } else  {
            Log.d(TAG, "dialogFromToOneRout: " + rout.toString());
            from_lat =  Double.valueOf(rout.get("from_lat"));
            from_lng = Double.valueOf(rout.get("from_lng"));
            to_lat = Double.valueOf(rout.get("to_lat"));
            to_lng = Double.valueOf(rout.get("to_lng"));

            Log.d(TAG, "dialogFromToOneRout: from_lat - " + from_lat);
            Log.d(TAG, "dialogFromToOneRout: from_lng - " + from_lng);
            Log.d(TAG, "dialogFromToOneRout: to_lat - " + to_lat);
            Log.d(TAG, "dialogFromToOneRout: to_lng - " + to_lng);

            FromAddressString = rout.get("from_street") + rout.get("from_number") ;
            Log.d(TAG, "dialogFromToOneRout: FromAddressString" + FromAddressString);
            ToAddressString = rout.get("to_street") + rout.get("to_number");
            if(rout.get("from_street").equals(rout.get("to_street"))) {
                ToAddressString =  getString(R.string.on_city_tv);;
            }
            Log.d(TAG, "dialogFromToOneRout: ToAddressString" + ToAddressString);
            List<String> settings = new ArrayList<>();

            settings.add(rout.get("from_lat"));
            settings.add(rout.get("from_lng"));
            settings.add(rout.get("to_lat"));
            settings.add(rout.get("to_lng"));

            settings.add(FromAddressString);
            settings.add(ToAddressString);

            updateRoutMarker(settings);
            String urlCost = getTaxiUrlSearchMarkers("costSearchMarkers", requireActivity());

            Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

            String message = requireActivity().getString(R.string.error_message);
            String orderCost = sendUrlMapCost.get("order_cost");
            Log.d(TAG, "dialogFromToOneRout:orderCost " + orderCost);


            if (!orderCost.equals("0")) {
                String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, requireActivity()).get(3);
                long discountInt = Integer.parseInt(discountText);
                cost = Long.parseLong(orderCost);
                discount = cost * discountInt / 100;

                cost += discount;
                updateAddCost(String.valueOf(discount));
                text_view_cost.setText(String.valueOf(cost));

                costFirstForMin = cost;
                MIN_COST_VALUE = (long) (cost*0.6);
            } else {
                message = getString(R.string.error_message);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());

                text_view_cost.setVisibility(View.INVISIBLE);
                btnRouts.setVisibility(View.INVISIBLE);
                btn_minus.setVisibility(View.INVISIBLE);
                btn_plus.setVisibility(View.INVISIBLE);
                btnAdd.setVisibility(View.INVISIBLE);
                buttonBonus.setVisibility(View.INVISIBLE);
            }
        }
    }
    private Map <String, String> routChoice(int i) {
        Map <String, String> rout = new HashMap<>();
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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

        Log.d(TAG, "routMaps: " + rout);
        return rout;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("Range")
    public String getTaxiUrlSearchMarkers(String urlAPI, Context context) {
        Log.d(TAG, "getTaxiUrlSearchMarkers: " + urlAPI);

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

        List<String> listCity = logCursor(MainActivity.CITY_INFO, requireActivity());
        String city = listCity.get(1);
        String api = listCity.get(2);

        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
        String comment = stringList.get(2);
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

        if(urlAPI.equals("costSearchMarkers")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type;
        }
        if(urlAPI.equals("orderSearchMarkersVisicom")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);


            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + "*" + userEmail  + "*" + payment_type + "/" + addCost + "/"
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
            for (int i = 0; i < OpenStreetMapActivity.arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(OpenStreetMapActivity.arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if(servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Log.d(TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        String url = "https://m.easy-order-taxi.site/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);
        Log.d(TAG, "getTaxiUrlSearchMarkers: " + url);

        database.close();

        return url;
    }

    private void changePayMethodMax(String textCost, String paymentType) {
        List<String> stringListCity = logCursor(MainActivity.CITY_INFO, requireActivity());
        String card_max_pay =  stringListCity.get(4);
        String bonus_max_pay =  stringListCity.get(5);

        // Инфлейтим макет для кастомного диалога
        LayoutInflater inflater = LayoutInflater.from(requireActivity());
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);

        AlertDialog alertDialog = new AlertDialog.Builder(requireActivity()).create();
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
                        if (Long.parseLong(card_max_pay) <= Long.parseLong(textCost)) {
                            paymentType("nal_payment");
                        }
                        break;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    orderRout();
                }
                orderFinished();
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
    public List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
            SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

            database.execSQL(deleteQuery);
            database.close();
        }
        reIndexOrders();
        array = arrayToRoutsAdapter();
        if (array != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.services_adapter_layout, array);
            listView.setAdapter(adapter);
        } else {
            // Если массив пустой, отобразите текст "no_routs" вместо списка
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.services_adapter_layout, new String[]{});
            listView.setAdapter(adapter);
            textView.setText(R.string.no_routs);

            del_but.setVisibility(View.INVISIBLE);
            text_view_cost.setVisibility(View.INVISIBLE);
            btnRouts.setVisibility(View.INVISIBLE);
            btn_minus.setVisibility(View.INVISIBLE);
            btn_plus.setVisibility(View.INVISIBLE);
            btnAdd.setVisibility(View.INVISIBLE);
            buttonBonus.setVisibility(View.INVISIBLE);
            scrollButtonDown.setVisibility(View.INVISIBLE);
            scrollButtonUp.setVisibility(View.INVISIBLE);

        }
        del_but.setVisibility(View.INVISIBLE);
        text_view_cost.setVisibility(View.INVISIBLE);
        btnRouts.setVisibility(View.INVISIBLE);
        btn_minus.setVisibility(View.INVISIBLE);
        btn_plus.setVisibility(View.INVISIBLE);
        btnAdd.setVisibility(View.INVISIBLE);
        buttonBonus.setVisibility(View.INVISIBLE);


    }
    private String[] arrayToRoutsAdapter() {
        ArrayList<Map> routMaps = routMaps(requireActivity());
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


                        Log.d(TAG, "arrayToRoutsAdapter:   routMaps.get(i).get(\"from_street\").toString()" +  routMaps.get(i).get("from_street").toString());

                        arrayRouts[i] = from_mes + " " +
                                routMaps.get(i).get("from_number").toString() + " -> " +
                                to_mes + " " +
                                routMaps.get(i).get("to_number").toString();
                    } else if(!routMaps.get(i).get("to_street").toString().equals(routMaps.get(i).get("to_number").toString())) {
                        Log.d(TAG, "arrayToRoutsAdapter:   routMaps.get(i).get(\"from_street\").toString()" +  routMaps.get(i).get("from_street").toString());

                        arrayRouts[i] = routMaps.get(i).get("from_street").toString() +
                                getString(R.string.to_message) +
                                routMaps.get(i).get("to_street").toString() + " " +
                                routMaps.get(i).get("to_number").toString();
                    } else {

                        Log.d(TAG, "arrayToRoutsAdapter:   routMaps.get(i).get(\"from_street\").toString()" +  routMaps.get(i).get("from_street").toString());

                        arrayRouts[i] = from_mes + " " +
                                getString(R.string.to_message) +
                                to_mes;

                    }

                } else {

                    Log.d(TAG, "arrayToRoutsAdapter:   routMaps.get(i).get(\"from_street\").toString()" +  routMaps.get(i).get("from_street").toString());

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
        Log.d(TAG, "routMaps: 1111 " + routsArr);
        return routsArr;
    }

    private void updateAddCost(String addCost) {
        ContentValues cv = new ContentValues();
        Log.d(TAG, "updateAddCost: addCost" + addCost);
        cv.put("addCost", addCost);

        // обновляем по id
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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

        Log.d(TAG, "onResume: selectedItem " + selectedItem);
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