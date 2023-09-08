package com.taxi.easy.ua.ui.gallery;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.cities.Cherkasy.Cherkasy;
import com.taxi.easy.ua.cities.Dnipro.Dnipro;
import com.taxi.easy.ua.cities.Kyiv.KyivCity;
import com.taxi.easy.ua.cities.Odessa.Odessa;
import com.taxi.easy.ua.cities.Odessa.OdessaTest;
import com.taxi.easy.ua.cities.Zaporizhzhia.Zaporizhzhia;
import com.taxi.easy.ua.databinding.FragmentGalleryBinding;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.home.MyBottomSheetBlackListFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.home.MyBottomSheetGalleryFragment;
import com.taxi.easy.ua.ui.maps.ToJSONParser;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private ListView listView;
    private String[] array, arraySpinner;
    private static final int CM_DELETE_ID = 1;
    public static TextView textView, text_view_cost;
    AppCompatButton del_but, btnRouts, btn_minus, btn_plus, btnAdd;
    Spinner spinner;
    Integer selectedItem;
    String FromAddressString, ToAddressString;
    private long firstCost;
    public static long  addCost, cost;
    public static Double from_lat;
    public static Double from_lng;
    public static Double to_lat;
    public static Double to_lng;
    long MIN_COST_VALUE, MAX_COST_VALUE;
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
    private static String[] arrayStreet;
    public  static String api;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        List<String> stringList = logCursor(MainActivity.CITY_INFO, getActivity());

        if(stringList.size() !=0 ) {
            switch (stringList.get(1)){
                case "Dnipropetrovsk Oblast":
                    arrayStreet = Dnipro.arrayStreet();
                    api = MainActivity.apiDnipro;
                    break;
                case "Zaporizhzhia":
                    arrayStreet = Zaporizhzhia.arrayStreet();
                    api = MainActivity.apiZaporizhzhia;
                    break;
                case "Cherkasy Oblast":
                    arrayStreet = Cherkasy.arrayStreet();
                    api = MainActivity.apiCherkasy;
                    break;
                case "Odessa":
                    arrayStreet = Odessa.arrayStreet();
                    api = MainActivity.apiOdessa;
                    break;
                case "OdessaTest":
                    arrayStreet = OdessaTest.arrayStreet();
                    api = MainActivity.apiTest;
                    break;
                default:
                    arrayStreet = KyivCity.arrayStreet();
                    api = MainActivity.apiKyiv;
                    break;
            };
        }
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
        if(!text_view_cost.getText().toString().isEmpty()) {
            cost = Long.parseLong(text_view_cost.getText().toString());
            MIN_COST_VALUE = (long) (cost * 0.1);
            MAX_COST_VALUE = cost * 3;
        }

        btn_minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                cost -= 5;
                addCost -= 5;
                if (cost <= MIN_COST_VALUE) {
                    cost = MIN_COST_VALUE;
                    addCost = MIN_COST_VALUE - cost;
                }
                text_view_cost.setText(String.valueOf(cost));

            }
        });

        btn_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cost += 5;
                addCost += 5;
                if (cost <= MIN_COST_VALUE) {
                    cost = MIN_COST_VALUE;
                    addCost = MIN_COST_VALUE - cost;
                }
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
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.services_adapter_layout, array);
            listView.setAdapter(adapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            registerForContextMenu(listView);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    del_but.setVisibility(View.VISIBLE);
                    btnRouts.setVisibility(View.VISIBLE);
                    text_view_cost.setVisibility(View.VISIBLE);

                    btn_minus.setVisibility(View.VISIBLE);
                    btn_plus.setVisibility(View.VISIBLE);
                    btnAdd.setVisibility(View.VISIBLE);
                    SparseBooleanArray checkespositions = listView.getCheckedItemPositions();
                    ArrayList<Integer> selectespositions = new ArrayList<>();

                    for (int i = 0; i < checkespositions.size(); i++) {
                        int pos = checkespositions.keyAt(i);
                        if (checkespositions.get(pos)) {
                            selectespositions.add(pos);
                        }
                    }

                    for (int posit : selectespositions) {
                        selectedItem = posit + 1;
                    }



                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            dialogFromToOneRout(routChoice(selectedItem));
                        }
                    } catch (MalformedURLException | InterruptedException | JSONException e) {
                        Log.d("TAG", "onItemClick: " + e.toString());
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
                if (connected()) {
                    if(!verifyOrder(getContext())) {

                        MyBottomSheetBlackListFragment bottomSheetDialogFragment = new MyBottomSheetBlackListFragment("orderCost");
                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                    } else {
                        try {
                            String url = getTaxiUrlSearchMarkers(from_lat, from_lng,
                                    to_lat, to_lng, "orderSearchMarkers", getContext());
                            Log.d("TAG", "onClick 55555555585: " + url);
                            Map<String, String> sendUrl = ToJSONParser.sendURL(url);

                            String mes = (String) sendUrl.get("message");
                            String orderC = (String) sendUrl.get("order_cost");

                            if (orderC.equals("0")) {
                                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(mes);
                                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                            }
                            if (!orderC.equals("0")) {
                                String orderWeb = orderC;
                                String messageResult = getString(R.string.thanks_message) +
                                       FromAddressString + getString(R.string.to_message) + ToAddressString +
                                       getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);

                                Intent intent = new Intent(getActivity(), FinishActivity.class);
                                intent.putExtra("messageResult_key", messageResult);
                                intent.putExtra("UID_key", Objects.requireNonNull(sendUrl.get("dispatching_order_uid")));
                                 startActivity(intent);

                            }

                        } catch (MalformedURLException e) {
                            Toast.makeText(getActivity(), getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(getActivity(), getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                }
            }
        });
        del_but.setVisibility(View.INVISIBLE);
        text_view_cost.setVisibility(View.INVISIBLE);
        btnRouts.setVisibility(View.INVISIBLE);
        btn_minus.setVisibility(View.INVISIBLE);
        btn_plus.setVisibility(View.INVISIBLE);
        btnAdd.setVisibility(View.INVISIBLE);

        return root;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void dialogFromToOneRout(Map <String, String> rout) throws MalformedURLException, InterruptedException, JSONException {
        if(connected()) {
            Log.d("TAG", "dialogFromToOneRout: " + rout.toString());
            from_lat =  Double.valueOf(rout.get("from_lat"));
            from_lng = Double.valueOf(rout.get("from_lng"));
            to_lat = Double.valueOf(rout.get("to_lat"));
            to_lng = Double.valueOf(rout.get("to_lng"));
            FromAddressString = rout.get("from_street") + rout.get("from_number") ;
            Log.d("TAG1", "dialogFromToOneRout: FromAddressString" + FromAddressString);
            ToAddressString = rout.get("to_street") + rout.get("to_number");
            if(rout.get("from_street").equals(rout.get("to_street"))) {
                ToAddressString =  getString(R.string.on_city_tv);;
            }
            Log.d("TAG1", "dialogFromToOneRout: ToAddressString" + ToAddressString);

            String urlCost = getTaxiUrlSearchMarkers(from_lat, from_lng,
                    to_lat, to_lng, "costSearchMarkers", getContext());

            Map<String, String> sendUrlMapCost = ToJSONParser.sendURL(urlCost);

            String message = (String) sendUrlMapCost.get("message");
            String orderCost = (String) sendUrlMapCost.get("order_cost");


            if (orderCost.equals("0")) {
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());

                text_view_cost.setVisibility(View.INVISIBLE);
                btnRouts.setVisibility(View.INVISIBLE);
                btn_minus.setVisibility(View.INVISIBLE);
                btn_plus.setVisibility(View.INVISIBLE);
                btnAdd.setVisibility(View.INVISIBLE);
            }
            if (!orderCost.equals("0")) {


                String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, getContext()).get(3);
                long discountInt = Integer.parseInt(discountText);
                long discount;
                firstCost = Long.parseLong(orderCost);
                discount = firstCost * discountInt / 100;
                cost = firstCost + discount;
                addCost = discount;
                text_view_cost.setText(String.valueOf(cost));
//                    addCost = discount;
                    Log.d("TAG", "dialogFromToOneRout: cost " + cost);
                    Log.d("TAG", "dialogFromToOneRout: addCost " + addCost);
                    Log.d("TAG", "dialogFromToOneRout: cost " + cost);



                }
        } else {
            Toast.makeText(getActivity(), getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
        }
    }
    private Map <String, String> routChoice(int i) {
        Map <String, String> rout = new HashMap<>();
        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(MainActivity.TABLE_ORDERS_INFO, null, null, null, null, null, null);
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

        Log.d("TAG", "routMaps: " + rout);
        return rout;
    }
    private boolean connected() {

        Boolean hasConnect = false;

        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(
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
    private String getTaxiUrlSearchMarkers(double originLatitude, double originLongitude,
                                           double toLatitude, double toLongitude,
                                           String urlAPI, Context context) {
        //  Проверка даты и времени

        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
        String time = stringList.get(1);
        String comment = stringList.get(2);
        String date = stringList.get(3);

        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;

//        Cursor cursorDb = MainActivity.database.query(MainActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        String tarif = logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(2);


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
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + displayName + "(" + userEmail + ")";
        }

        if(urlAPI.equals("orderSearchMarkers")) {
            phoneNumber = logCursor(MainActivity.TABLE_USER_INFO, context).get(2);


            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName  + "/" + addCost + "/" + time + "/" + comment + "/" + date;

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
            Log.d("TAG", "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        String url = "https://m.easy-order-taxi.site/" + api + "/android/" + urlAPI + "/" + parameters + "/" + result;

        Log.d("TAG", "getTaxiUrlSearch: " + url);
        database.close();


        return url;
    }
    private boolean verifyOrder(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(MainActivity.TABLE_USER_INFO, context).get(1).equals("0")) {
                verify = false;Log.d("TAG", "verifyOrder:verify " +verify);
            }
            cursor.close();
        }
        database.close();
        return verify;
    }
    @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = getActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        SQLiteDatabase database = getActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
            SQLiteDatabase database = getActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

            database.execSQL(deleteQuery);
            database.close();
        }
        reIndexOrders();
        array = arrayToRoutsAdapter();
        if (array != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.services_adapter_layout, array);
            listView.setAdapter(adapter);
        } else {
            // Если массив пустой, отобразите текст "no_routs" вместо списка
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.services_adapter_layout, new String[]{});
            listView.setAdapter(adapter);
            textView.setText(R.string.no_routs);

            del_but.setVisibility(View.INVISIBLE);
            text_view_cost.setVisibility(View.INVISIBLE);
            btnRouts.setVisibility(View.INVISIBLE);
            btn_minus.setVisibility(View.INVISIBLE);
            btn_plus.setVisibility(View.INVISIBLE);
            btnAdd.setVisibility(View.INVISIBLE);

        }
        del_but.setVisibility(View.INVISIBLE);
        text_view_cost.setVisibility(View.INVISIBLE);
        btnRouts.setVisibility(View.INVISIBLE);
        btn_minus.setVisibility(View.INVISIBLE);
        btn_plus.setVisibility(View.INVISIBLE);
        btnAdd.setVisibility(View.INVISIBLE);


    }
    private String[] arrayToRoutsAdapter() {
        ArrayList<Map> routMaps = routMaps(getContext());
        String[] arrayRouts;
        if(routMaps.size() != 0) {
            arrayRouts = new String[routMaps.size()];
            for (int i = 0; i < routMaps.size(); i++) {
                if(!routMaps.get(i).get("from_street").toString().equals(routMaps.get(i).get("to_street").toString())) {
                    if (!routMaps.get(i).get("from_street").toString().equals(routMaps.get(i).get("from_number").toString())) {
                        arrayRouts[i] = routMaps.get(i).get("from_street").toString() + " " +
                                routMaps.get(i).get("from_number").toString() + " -> " +
                                routMaps.get(i).get("to_street").toString() + " " +
                                routMaps.get(i).get("to_number").toString();
                    } else if(!routMaps.get(i).get("to_street").toString().equals(routMaps.get(i).get("to_number").toString())) {
                        arrayRouts[i] = routMaps.get(i).get("from_street").toString() +
                                getString(R.string.to_message) +
                                routMaps.get(i).get("to_street").toString() + " " +
                                routMaps.get(i).get("to_number").toString();
                    } else {
                        arrayRouts[i] = routMaps.get(i).get("from_street").toString()  +
                                getString(R.string.to_message) +
                                routMaps.get(i).get("to_street").toString();

                    }

                } else {
                    arrayRouts[i] = routMaps.get(i).get("from_street").toString() + " " +
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
        Log.d("TAG", "routMaps: " + routsArr);
        return routsArr;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}