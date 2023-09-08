package com.taxi.easy.ua.ui.home;


import static android.content.Context.MODE_PRIVATE;
import static android.graphics.Color.RED;
import static com.taxi.easy.ua.R.string.address_error_message;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ServerConnection;
import com.taxi.easy.ua.cities.Cherkasy.Cherkasy;
import com.taxi.easy.ua.cities.Dnipro.Dnipro;
import com.taxi.easy.ua.cities.Kyiv.KyivCity;
import com.taxi.easy.ua.cities.Odessa.Odessa;
import com.taxi.easy.ua.cities.Odessa.OdessaTest;
import com.taxi.easy.ua.cities.Zaporizhzhia.Zaporizhzhia;
import com.taxi.easy.ua.databinding.FragmentHomeBinding;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.maps.ToJSONParser;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
import com.taxi.easy.ua.ui.start.ResultSONParser;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    public static String from, to;
    public static EditText from_number, to_number;
    String messageResult;


    public  static String api;

    FloatingActionButton fab_call, fab_map;
    private final String TAG = "TAG";
    private static final int CM_DELETE_ID = 1;

    private int selectesposition = -1;
    Button gpsbut;
    AppCompatButton btn_order, buttonAddServices, btn_minus, btn_plus, btnGeo, on_map;
    public String FromAddressString, ToAddressString;
    Integer selectedItem;
    private long firstCost;
    public static long addCost, cost;
    private static String[] arrayStreet;
    private String numberFlagFrom = "1", numberFlagTo = "1";

    public static String  from_numberCost, toCost, to_numberCost;
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

    long MIN_COST_VALUE, MAX_COST_VALUE;
    AutoCompleteTextView textViewFrom, textViewTo;
    ArrayAdapter<String> adapter;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        List<String> stringList = logCursor(MainActivity.CITY_INFO, getActivity());
        Log.d("TAG", "onViewCreated: " + stringList);
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
            adapter = new ArrayAdapter<>(getActivity(),android.R.layout.simple_dropdown_item_1line, arrayStreet);
        }


        text_view_cost = binding.textViewCost;
        btnGeo = binding.btnGeo;
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            btnGeo.setVisibility(View.VISIBLE);
        }  else {
            btnGeo.setVisibility(View.INVISIBLE);
        }
        btnGeo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
            }
        });
        btn_minus = binding.btnMinus;
        btn_plus= binding.btnPlus;
        btn_minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MIN_COST_VALUE = (long) (cost * 0.1);
                MAX_COST_VALUE = cost * 3;

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
                MIN_COST_VALUE = (long) (cost * 0.1);
                MAX_COST_VALUE = cost * 3;

                cost += 5;
                addCost += 5;
                if (cost <= MIN_COST_VALUE) {
                    cost = MIN_COST_VALUE;
                    addCost = MIN_COST_VALUE - cost;
                }
                text_view_cost.setText(String.valueOf(cost));
            }
        });




        textViewFrom =binding.textFrom;
        textViewFrom.setAdapter(adapter);
        textViewTo =binding.textTo;
        textViewTo.setAdapter(adapter);

        from_number = binding.fromNumber;
//        from_number.addTextChangedListener(new TextWatcher() {
//            private String previousText = " ";
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                // Вызывается перед изменением текста
//                previousText = charSequence.toString();
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                // Вызывается после изменения текста
//                String newText = editable.toString();
//                // Проверяем, что новая длина текста больше предыдущей длины
//                if (newText.length() > previousText.length())  {
//                    Log.d("TAG", "onTextChanged: newText" + "/" + newText + "/");
//
//                    Log.d("TAG", "onTextChanged: numberFlagTo" + "/" + numberFlagTo + "/");
//                    Log.d("TAG", "onTextChanged: charSequence.length()222" + "/" + newText + "/");
//                    if (newText.equals(" ")) {
//                        // CharSequene пустая
//                        from_numberCost = "1";
//                    } else if (newText.equals("XXX")) {
//
//                        from_numberCost = " ";
//                    } else {
//                        // CharSequene не пустая
//                        from_numberCost = newText;
//                    }
//                    // Вызывается во время изменения текста
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                        from_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.edit)));
//                        from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
//                    } else {
//                        ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(getResources().getColor(R.color.edit)));
//                    }
//                    Log.d("TAG", "onTextChanged: newText " + newText);
//                    if (to == null) {
//                        toCost = from;
//                        to_numberCost = from_numberCost;
//                    } else {
//                        toCost = to;
//                    }
//                    if(!textViewFrom.getText().toString().isEmpty()) {
//                        try {
//                            String urlCost = null;
//                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                                urlCost = getTaxiUrlSearch(from, from_numberCost, toCost, to_numberCost, "costSearch", getActivity());
//                            }
//
//                            Map sendUrlMapCost = CostJSONParser.sendURL(urlCost);
//                            String orderCost = (String) sendUrlMapCost.get("order_cost");
//                            String message = (String) sendUrlMapCost.get("message");
//
//                            if (orderCost.equals("0")) {
//                                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
//                                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
//
//                                text_view_cost.setVisibility(View.INVISIBLE);
//                                btn_minus.setVisibility(View.INVISIBLE);
//                                btn_plus.setVisibility(View.INVISIBLE);
//                                buttonAddServices.setVisibility(View.INVISIBLE);
//                            }
//                            if (!orderCost.equals("0")) {
//                                text_view_cost.setVisibility(View.VISIBLE);
//                                btn_minus.setVisibility(View.VISIBLE);
//                                btn_plus.setVisibility(View.VISIBLE);
//                                buttonAddServices.setVisibility(View.VISIBLE);
//                                btn_order.setVisibility(View.VISIBLE);
//
//                                String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, getContext()).get(3);
//                                long discountInt = Integer.parseInt(discountText);
//                                long discount;
//
//                                cost = Long.parseLong(orderCost);
//
//                                MIN_COST_VALUE = (long) ((long) cost * 0.1);
//                                MAX_COST_VALUE = cost * 3;
//                                firstCost = cost;
//
//                                discount = firstCost * discountInt / 100;
//                                cost = firstCost + discount;
//                                addCost = discount;
//                                text_view_cost.setText(Long.toString(cost));
//
//                            } else {
//                                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
//                                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
//                            }
//
//                        } catch (MalformedURLException e) {
//                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
//                            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
//                        }
//                    }
//                }
//                // Обновляем предыдущий текст
//                previousText = newText;
//            }
//        });

        to_number = binding.toNumber;
//        to_number.addTextChangedListener(new TextWatcher() {
//            private String previousText = " ";
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//                // Вызывается перед изменением текста
//                previousText = charSequence.toString();
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                String newText = editable.toString();
//                // Проверяем, что новая длина текста больше предыдущей длины
//                if (newText.length() > previousText.length())  {
//                    String to_numberCost;
//                    Log.d("TAG", "onTextChanged: charSequence.toString()2222222" + "/" + newText + "/");
//                    Log.d("TAG", "onTextChanged: numberFlagTo2222222" + "/" + numberFlagTo + "/");
//                    if (newText.equals(" ")) {
//                        // CharSequene пустая
//                        to_numberCost = "1";
//                    } else if (newText.equals("XXX")) {
//
//                        to_numberCost = " ";
//                    } else {
//                        // CharSequene не пустая
//                        to_numberCost = newText;
//                    }
//                    // Вызывается во время изменения текста
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                        to_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.edit)));
//                        to_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
//                    } else {
//                        ViewCompat.setBackgroundTintList(to_number, ColorStateList.valueOf(getResources().getColor(R.color.edit)));
//                    }
//                    Log.d("TAG", "onTextChanged: newText " + newText);
//                    try {
//                        String urlCost = null;
//                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                            urlCost = getTaxiUrlSearch(from, from_numberCost, toCost, to_numberCost, "costSearch", getActivity());
//                        }
//
//                        Map sendUrlMapCost = CostJSONParser.sendURL(urlCost);
//                        String orderCost = (String) sendUrlMapCost.get("order_cost");
//                        String message = (String) sendUrlMapCost.get("message");
//
//                        if (orderCost.equals("0")) {
//                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
//                            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
//
//                            text_view_cost.setVisibility(View.INVISIBLE);
//                            btn_minus.setVisibility(View.INVISIBLE);
//                            btn_plus.setVisibility(View.INVISIBLE);
//                            buttonAddServices.setVisibility(View.INVISIBLE);
//                        }
//                        if (!orderCost.equals("0")) {
//                            text_view_cost.setVisibility(View.VISIBLE);
//                            btn_minus.setVisibility(View.VISIBLE);
//                            btn_plus.setVisibility(View.VISIBLE);
//                            buttonAddServices.setVisibility(View.VISIBLE);
//                            btn_order.setVisibility(View.VISIBLE);
//
//                            String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, getContext()).get(3);
//                            long discountInt = Integer.parseInt(discountText);
//                            long discount;
//
//                            cost = Long.parseLong(orderCost);
//
//                            MIN_COST_VALUE = (long) ((long) cost * 0.1);
//                            MAX_COST_VALUE = cost * 3;
//                            firstCost = cost;
//
//                            discount = firstCost * discountInt / 100;
//                            cost = firstCost + discount;
//                            addCost = discount;
//                            text_view_cost.setText(Long.toString(cost));
//
//                        } else {
//                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
//                            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
//                        }
//
//                    } catch (MalformedURLException e) {
//                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
//                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
//                    }
//                }
//                // Обновляем предыдущий текст
//                previousText = newText;
//            }
//        });

        btn_order = binding.btnOrder;
        btn_order.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if(connected()) {
                   order();
                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }
        });

        gpsbut = binding.gpsbut;
        gpsbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });

        fab_call = binding.fabCall;
        fab_map = binding.fabOpenMap;
        fab_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!verifyOrder(getContext())) {

                    MyBottomSheetBlackListFragment bottomSheetDialogFragment = new MyBottomSheetBlackListFragment("orderCost");
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                } else {
                    LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                    boolean gps_enabled = false;
                    boolean network_enabled = false;

                    try {
                        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    } catch(Exception ex) {
                    }

                    try {
                        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    } catch(Exception ex) {
                    }

                    if(!gps_enabled || !network_enabled) {
                        // notify user
                        MaterialAlertDialogBuilder builder =  new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme);
                        LayoutInflater inflater = getActivity().getLayoutInflater();

                        View view_cost = inflater.inflate(R.layout.message_layout, null);
                        builder.setView(view_cost);
                        TextView message = view_cost.findViewById(R.id.textMessage);
                        message.setText(R.string.gps_info);
                        builder.setPositiveButton(R.string.gps_on, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                        getActivity().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                    }
                                })
                                .setNegativeButton(R.string.cancel_button, null)
                                .show();
                    }  else  {
                        // Разрешения уже предоставлены, выполнить ваш код
                        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.on_geo_loc_mes));
                            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                            btnGeo.setVisibility(View.VISIBLE);
                        }  else {
                            btnGeo.setVisibility(View.INVISIBLE);
                            Intent intent = new Intent(getActivity(), OpenStreetMapActivity.class);
                            startActivity(intent);
                        }

                    }
                }
            }
        });

        on_map = binding.btnMap;
        on_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!verifyOrder(getContext())) {

                    MyBottomSheetBlackListFragment bottomSheetDialogFragment = new MyBottomSheetBlackListFragment("orderCost");
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                } else {
                    LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                    boolean gps_enabled = false;
                    boolean network_enabled = false;

                    try {
                        gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                    } catch(Exception ex) {
                    }

                    try {
                        network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                    } catch(Exception ex) {
                    }

                    if(!gps_enabled || !network_enabled) {
                        // notify user
                        MaterialAlertDialogBuilder builder =  new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme);
                        LayoutInflater inflater = getActivity().getLayoutInflater();

                        View view_cost = inflater.inflate(R.layout.message_layout, null);
                        builder.setView(view_cost);
                        TextView message = view_cost.findViewById(R.id.textMessage);
                        message.setText(R.string.gps_info);
                        builder.setPositiveButton(R.string.gps_on, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                                        getActivity().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                    }
                                })
                                .setNegativeButton(R.string.cancel_button, null)
                                .show();
                    }  else  {
                        // Разрешения уже предоставлены, выполнить ваш код
                        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.on_geo_loc_mes));
                            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                            btnGeo.setVisibility(View.VISIBLE);
                        }  else {
                            btnGeo.setVisibility(View.INVISIBLE);
                            Intent intent = new Intent(getActivity(), OpenStreetMapActivity.class);
                            startActivity(intent);
                        }

                    }
                }
            }
        });
        fab_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                String phone;
                List<String> stringList = logCursor(MainActivity.CITY_INFO, getActivity());
                switch (stringList.get(1)){
                    case "Kyiv City":
                        phone = "tel:0674443804";
                        break;
                    case "Dnipropetrovsk Oblast":
                        phone = "tel:0667257070";
                        break;
                    case "Odessa":
                        phone = "tel:0737257070";
                        break;
                    case "Zaporizhzhia":
                        phone = "tel:0687257070";
                        break;
                    case "Cherkasy Oblast":
                        phone = "tel:0962294243";
                        break;
                    default:
                        phone = "tel:0674443804";
                        break;
                }
                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });

        buttonAddServices = binding.btnAdd;
        buttonAddServices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyBottomSheetDialogFragment bottomSheetDialogFragment = new MyBottomSheetDialogFragment();
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        });

        return root;
    }
    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(getActivity(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);

        }
    }
    @Override
    public void onResume() {
        super.onResume();
        List<String> stringList = logCursor(MainActivity.CITY_INFO, getActivity());
        Log.d("TAG", "onViewCreated: " + stringList);
        if (stringList.size() != 0) {
            rout();

            text_view_cost.setVisibility(View.INVISIBLE);
            btn_minus.setVisibility(View.INVISIBLE);
            btn_plus.setVisibility(View.INVISIBLE);
            buttonAddServices.setVisibility(View.INVISIBLE);
            btn_order.setVisibility(View.INVISIBLE);
            cost = 0;
            addCost = 0;
            from = null;
            to = null;
            text_view_cost.setText("");
            textViewFrom.setText("");
            from_number.setText("");
            textViewTo.setText("");
            to_number.setText("");
        }


    }

    @SuppressLint("Range")
    private void rout() {
        textViewFrom.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                selectesposition = position; // Обновляем выбранную позицию
//                adapter.notifyDataSetChanged(); // Обновляем вид списка
                Log.d(TAG, "onItemClick: textViewTo.getText().toString()" + textViewTo.getText().toString());

                if (textViewTo.getText().toString().isEmpty()) {
                        to = null;
                } else {
                    to = textViewTo.getText().toString();
                }
                if (numberFlagTo.equals("0")) {
                    to_number.setText(" ");
                }
                Log.d(TAG, "onItemClick: to" + to);
                if(connected()) {
                    from = String.valueOf(adapter.getItem(position));
                    if (from.indexOf("/") != -1) {
                        from = from.substring(0,  from.indexOf("/"));
                    };


                    String url = "https://m.easy-order-taxi.site/" + api + "/android/autocompleteSearchComboHid/" + from;

                    Map sendUrlMapCost = null;
                    try {
                        sendUrlMapCost = ResultSONParser.sendURL(url);
                    } catch (MalformedURLException | InterruptedException | JSONException e) {
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.error_firebase_start));
                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                    }
                    assert sendUrlMapCost != null;
                    String orderCost = (String) sendUrlMapCost.get("message");
                    switch (Objects.requireNonNull(orderCost)) {
                        case "200": {
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.error_firebase_start));
                            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                            break;
                        }
                        case "400": {
                            textViewFrom.setTextColor(RED);
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(address_error_message));
                            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
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
                            Log.d(TAG, "onItemClick: " + new IllegalStateException("Unexpected value: " + Objects.requireNonNull(orderCost)));
                    }

                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
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
                        to = String.valueOf(adapter.getItem(position));
                        if (to.indexOf("/") != -1) {
                            to = to.substring(0, to.indexOf("/"));
                        }
                         String url = "https://m.easy-order-taxi.site/" + api + "/android/autocompleteSearchComboHid/" + to;

                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException | InterruptedException | JSONException e) {
                            Toast.makeText(getActivity(), R.string.error_firebase_start, Toast.LENGTH_SHORT).show();
                        }

                        String orderCost = (String) sendUrlMapCost.get("message");
                        Log.d(TAG, "onItemClick: orderCost" + orderCost);
                        switch (Objects.requireNonNull(orderCost)) {
                            case "200":
                                bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.error_firebase_start));
                                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                                break;
                            case "400":
                                textViewTo.setTextColor(RED);
                                bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.address_error_message));
                                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
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
                                Log.d(TAG, "onItemClick: " + new IllegalStateException("Unexpected value: " + Objects.requireNonNull(orderCost)));
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
                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                    }
            };

        });

    }
    @SuppressLint("ResourceAsColor")
    private void cost(){
        textViewTo.setVisibility(View.VISIBLE);
        binding.textwhere.setVisibility(View.VISIBLE);
        binding.num2.setVisibility(View.VISIBLE);


        if (numberFlagFrom.equals("1") && from_number.getText().toString().equals(" ")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                from_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
                from_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                from_number.requestFocus();
            } else {
                ViewCompat.setBackgroundTintList(from_number, ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
                from_number.requestFocus();
            }
            from_numberCost = "1";
        } else {

            if (numberFlagFrom.equals("0")) {
                from_numberCost = " ";
            } else{
                from_numberCost = from_number.getText().toString();
            }
        }

        if (numberFlagTo.equals("1") && to_number.getText().toString().equals(" ")) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                to_number.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
                to_number.setBackgroundTintBlendMode(BlendMode.SRC_IN); // Устанавливаем режим смешивания цветов
                to_number.requestFocus();
            } else {
                ViewCompat.setBackgroundTintList(to_number, ColorStateList.valueOf(getResources().getColor(R.color.selected_text_color)));
            }
            to_numberCost = "1";
        } else {
            if (numberFlagTo.equals("0")) {
                to_numberCost = " ";
            } else{
                to_numberCost = to_number.getText().toString();
            }

        }
        Log.d(TAG, "cost: numberFlagTo " + numberFlagTo);

        if (to == null) {
            toCost = from;
            to_numberCost = from_numberCost;
        } else {
            toCost = to;
        }


        try {
            String urlCost = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                urlCost = getTaxiUrlSearch(from, from_numberCost, toCost, to_numberCost, "costSearch", getActivity());
            }

            Map sendUrlMapCost = CostJSONParser.sendURL(urlCost);
            String orderCost = (String) sendUrlMapCost.get("order_cost");
            String message = (String) sendUrlMapCost.get("message");

            if (orderCost.equals("0")) {
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());

                text_view_cost.setVisibility(View.INVISIBLE);
                btn_minus.setVisibility(View.INVISIBLE);
                btn_plus.setVisibility(View.INVISIBLE);
                buttonAddServices.setVisibility(View.INVISIBLE);
            }
            if (!orderCost.equals("0")) {
                text_view_cost.setVisibility(View.VISIBLE);
                btn_minus.setVisibility(View.VISIBLE);
                btn_plus.setVisibility(View.VISIBLE);
                buttonAddServices.setVisibility(View.VISIBLE);
                btn_order.setVisibility(View.VISIBLE);

                String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO, getContext()).get(3);
                long discountInt = Integer.parseInt(discountText);
                long discount;

                cost = Long.parseLong(orderCost);

                MIN_COST_VALUE = (long) ((long) cost * 0.1);
                MAX_COST_VALUE = cost * 3;
                firstCost = cost;

                discount = firstCost * discountInt / 100;
                addCost = discount;
                cost = firstCost + discount;
                text_view_cost.setText(Long.toString(cost));

            } else {
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            }

        } catch (MalformedURLException e) {
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }
    @SuppressLint("ResourceAsColor")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void order() {
        if(!verifyOrder(getContext())) {
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.black_list_message));
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
            return;
        }
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
        if(stop) {return;}

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
        if(from_number.getText().toString().equals(" ")) {
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

        if (!verifyPhone(getContext())) {
            getPhoneNumber();
        }
        if (!verifyPhone(getContext())) {
            MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment();
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        }
        if(connected()) {
            if (verifyPhone(getContext())) {
                try {
                    String urlOrder = getTaxiUrlSearch(from, from_numberCost, toCost, to_numberCost, "orderSearch", getActivity());
                    Map<String, String> sendUrlMap = ToJSONParser.sendURL(urlOrder);

                    String orderWeb = (String) sendUrlMap.get("order_cost");
                    if (!orderWeb.equals("0")) {

                        String from_name = (String) sendUrlMap.get("routefrom");
                        String to_name = (String) sendUrlMap.get("routeto");
                        if (from_name.equals(to_name)) {
                            messageResult = getString(R.string.thanks_message) +
                                    from_name + " " + from_number.getText() + " " +  getString(R.string.on_city) +
                                    getString(R.string.cost_of_order) + orderWeb + getString(R.string.UAH);


                        } else {
                            messageResult =  getString(R.string.thanks_message) +
                                    from_name + " " + from_number.getText() + " " + getString(R.string.to_message) +
                                    to_name + " " + to_number.getText() + "." +
                                    getString(R.string.cost_of_order) + orderWeb + getString(R.string.UAH);
                        }
                        Log.d(TAG, "order: sendUrlMap.get(\"from_lat\")" + sendUrlMap.get("from_lat"));
                        Log.d(TAG, "order: sendUrlMap.get(\"lat\")" + sendUrlMap.get("lat"));
                        if(!sendUrlMap.get("from_lat").equals("0") && !sendUrlMap.get("lat").equals("0")) {
                            if(from_name.equals(to_name)) {
                                insertRecordsOrders(
                                        from_name, from_name,
                                        from_number.getText().toString(), from_number.getText().toString(),
                                        (String) sendUrlMap.get("from_lat"), (String) sendUrlMap.get("from_lng"),
                                        (String) sendUrlMap.get("from_lat"), (String) sendUrlMap.get("from_lng"),
                                        getContext()
                                );
                            } else {
                                insertRecordsOrders(
                                        from_name, to_name,
                                        from_number.getText().toString(), to_number.getText().toString(),
                                        (String) sendUrlMap.get("from_lat"), (String) sendUrlMap.get("from_lng"),
                                        (String) sendUrlMap.get("lat"), (String) sendUrlMap.get("lng"), getContext()
                                );

                            }
                        }

                        Intent intent = new Intent(getActivity(), FinishActivity.class);
                        intent.putExtra("messageResult_key", messageResult);
                        intent.putExtra("UID_key", String.valueOf(sendUrlMap.get("dispatching_order_uid")));
                        startActivity(intent);

                    } else {
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                    }


                } catch (MalformedURLException e) {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
            }
        } else {
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
            bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
        }






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

    private boolean verifyPhone(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(MainActivity.TABLE_USER_INFO, context).get(2).equals("+380")) {
                verify = false;
            }
            cursor.close();
        }

        return verify;
    }
    private void updateRecordsUser(String result, Context context) {
        ContentValues cv = new ContentValues();

        cv.put("phone_number", result);

        // обновляем по id
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        int updCount = database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        Log.d("TAG", "updated rows count = " + updCount);


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
        Log.d("TAG", "routMaps: " + routsArr);
        return routsArr;
    }
    private String[] arrayToRoutsAdapter () {
        ArrayList<Map>  routMaps = routMaps(getContext());
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
                               OpenStreetMapActivity.tom +
                               routMaps.get(i).get("to_street").toString() + " " +
                               routMaps.get(i).get("to_number").toString();
                   } else {
                       arrayRouts[i] = routMaps.get(i).get("from_street").toString()  +
                               OpenStreetMapActivity.tom +
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
    public CompletableFuture<Boolean> checkConnectionAsync() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        ServerConnection.checkConnection("https://m.easy-order-taxi.site/", new ServerConnection.ConnectionCallback() {
            @Override
            public void onConnectionResult(boolean isConnected) {
                future.complete(isConnected);
            }
        });

        return future;
    }
    private boolean hasServer() {
        CompletableFuture<Boolean> connectionFuture = checkConnectionAsync();
        boolean isConnected = false;
        try {
            isConnected = connectionFuture.get();
        } catch (Exception e) {

        }
        return  isConnected;
    };
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getTaxiUrlSearch(String from, String from_number, String to, String to_number, String urlAPI, Context context) {

            //  Проверка даты и времени

            List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO, context);
            String time = stringList.get(1);
            String comment = stringList.get(2);
            String date = stringList.get(3);

        Log.d(TAG, "getTaxiUrlSearch: addCost" + addCost);

        // Origin of route
        String str_origin = from + "/" + from_number;

        // Destination of route
        String str_dest = to + "/" + to_number;

        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String tarif =  logCursor(MainActivity.TABLE_SETTINGS_INFO, context).get(2);

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
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + displayName + "(" + userEmail + ")";
        }

        if(urlAPI.equals("orderSearch")) {
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



        return url;
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
    private void getPhoneNumber () {
        String mPhoneNumber;
        TelephonyManager tMgr = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "Manifest.permission.READ_PHONE_NUMBERS: " + ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_NUMBERS));
            Log.d("TAG", "Manifest.permission.READ_PHONE_STATE: " + ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_PHONE_STATE));
            return;
        }
        mPhoneNumber = tMgr.getLine1Number();
//        mPhoneNumber = null;
        if(mPhoneNumber != null) {
            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
            boolean val = Pattern.compile(PHONE_PATTERN).matcher(mPhoneNumber).matches();
            Log.d("TAG", "onClick No validate: " + val);
            if (val == false) {
                Toast.makeText(getActivity(), getString(R.string.format_phone) , Toast.LENGTH_SHORT).show();
                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + mPhoneNumber);
//                getActivity().finish();

            } else {
                updateRecordsUser(mPhoneNumber, getContext());
            }
        }

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
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

        if(urlAPI.equals("orderSearch")) {
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

    private static void insertRecordsOrders( String from, String to,
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

        cursor_from.close();
        cursor_to.close();

    }

}