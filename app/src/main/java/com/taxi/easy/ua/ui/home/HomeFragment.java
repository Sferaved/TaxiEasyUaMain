package com.taxi.easy.ua.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentHomeBinding;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.maps.Odessa;
import com.taxi.easy.ua.ui.maps.OrderJSONParser;
import com.taxi.easy.ua.ui.start.ResultSONParser;
import com.taxi.easy.ua.ui.start.StartActivity;

import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private String from, to;
    EditText from_number, to_number;
    String messageResult;
    private ListView listView;
    Button button;
    private String[] array;
    private String[] arrayStreet = Odessa.street();
    static FloatingActionButton fab, fab_call;
    private final String TAG = "TAG";


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        listView = binding.list;
        fab = binding.fab;
        fab_call = binding.fabCall;
        button = binding.btnRouts;

        if(connected()) {
            array = arrayToRoutsAdapter();
        }
        else {
            getActivity().finish();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connected()) {
                    try {
                        if (connected()) {
                            dialogFromTo();
                        }
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        fab_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:0934066749"));
                if (ActivityCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    checkPermission(Manifest.permission.CALL_PHONE, StartActivity.READ_CALL_PHONE);
                }
                startActivity(intent);
            }
        });

        if(array != null)  {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_single_choice, array);
            listView.setAdapter(adapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemChecked(0, true);

        } else  button.setVisibility(View.INVISIBLE);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (connected()) {
                    try {
                        dialogFromToOneRout(StartActivity.routChoice(listView.getCheckedItemPosition() + 1));
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        try {
            dialogFromTo();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private String[] arrayToRoutsAdapter () {
        ArrayList<Map>  routMaps = StartActivity.routMaps();
        String[] arrayRouts;
        if(routMaps.size() != 0) {
            arrayRouts = new String[routMaps.size()];
            for (int i = 0; i < routMaps.size(); i++) {
                arrayRouts[i] = "Звідки: " + routMaps.get(i).get("from_street").toString() + " " +
                        routMaps.get(i).get("from_number").toString() + "\nКуди: " +
                        routMaps.get(i).get("to_street").toString() + " " +
                        routMaps.get(i).get("to_number").toString();

            }
        } else {
            arrayRouts = null;
        }
        return arrayRouts;
    }
    private void sentPhone () throws MalformedURLException {
        String phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
            String urlErrorString = "https://m.easy-order-taxi.site/api/android/sentPhone/" + phoneNumber;
            Log.d("TAG", "dialogFromToOneRout urlErrorString: " + urlErrorString);
            URL urlError = new URL(urlErrorString);

            AsyncTask.execute(() -> {
                HttpsURLConnection urlConnection = null;

                try {
                    urlConnection = (HttpsURLConnection) urlError.openConnection();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                urlConnection.setDoInput(true);
                try {
                    urlConnection.getResponseCode();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                urlConnection.disconnect();
            });
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

        if (!hasConnect) {
            Toast.makeText(getActivity(), "Перевірте інтернет-підключення або зателефонуйте оператору.", Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, "connected: " + hasConnect);
        return hasConnect;
    }
    private void dialogFromToOneRout(Map <String, String> rout) throws MalformedURLException, InterruptedException {
        if(connected()) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.from_to_layout, null);
            builder.setView(view);

            String from_street_rout = rout.get("from_street");
            String from_number_rout = rout.get("from_number");
            String to_street_rout = rout.get("to_street");
            String to_number_rout = rout.get("to_number");
            Log.d("TAG", "dialogFromToOneRout: " + from_street_rout + to_street_rout);

            try {
                String urlCost = getTaxiUrlSearch(from_street_rout, from_number_rout, to_street_rout, to_number_rout, "costSearch");

                Log.d("TAG", "onClick urlCost: " + urlCost);
                Map sendUrlMapCost = CostJSONParser.sendURL(urlCost);

                String orderCost = (String) sendUrlMapCost.get("order_cost");
                Log.d("TAG", "onClick orderCost : " + orderCost);

                if (!orderCost.equals("0")) {

                    // Start downloading json data from Google Directions API

                    new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                            .setMessage("Вартість поїздки: " + orderCost + "грн")
                            .setPositiveButton("Замовити", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (connected()) {
                                        String urlOrder = getTaxiUrlSearch(from_street_rout, from_number_rout, to_street_rout, to_number_rout, "orderSearch");

                                        try {
                                            Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);

                                            String orderWeb = (String) sendUrlMap.get("order_cost");
                                            if (!orderWeb.equals("0")) {

                                                String from_name = (String) sendUrlMap.get("from_name");
                                                String to_name = (String) sendUrlMap.get("to_name");
                                                if (from_name.equals(to_name)) {
                                                    messageResult = "Дякуемо за замовлення зі " +
                                                            from_name + " " + from_number.getText() + " " + " по місту." +
                                                            " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";

                                                } else {
                                                    messageResult = "Дякуемо за замовлення зі " +
                                                            from_name + " " + from_number_rout + " " + " до " +
                                                            to_name + " " + to_number_rout + "." +
                                                            " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";
                                                }
                                                button.setVisibility(View.INVISIBLE);

                                                StartActivity.insertRecordsOrders(sendUrlMap,
                                                        from_number.getText().toString(), to_number.getText().toString());

                                                new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                        .setMessage(messageResult)
                                                        .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                if(connected()) {
                                                                    Log.d("TAG", "onClick ");
                                                                    if (array.length != 0) {
                                                                        button.setVisibility(View.VISIBLE);

                                                                    } else
                                                                        button.setVisibility(View.INVISIBLE);

                                                                    getActivity().finish();
                                                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                                                    startActivity(intent);
                                                                }
                                                            }
                                                        })
                                                        .show();
                                            } else {
                                                String message = (String) sendUrlMap.get("message");
                                                new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                        .setMessage(message +
                                                                " Спробуйте ще або зателефонуйте оператору.")
                                                        .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(Intent.ACTION_CALL);
                                                                intent.setData(Uri.parse("tel:0934066749"));
                                                                if (ActivityCompat.checkSelfPermission(getActivity(),
                                                                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                    checkPermission(Manifest.permission.CALL_PHONE, StartActivity.READ_CALL_PHONE);

                                                                }
                                                                if (array.length != 0) {
                                                                    button.setVisibility(View.VISIBLE);

                                                                } else
                                                                    button.setVisibility(View.INVISIBLE);

                                                                startActivity(intent);
                                                            }
                                                        })
                                                        .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                if (connected()) {
                                                                    if (array.length != 0) {
                                                                        button.setVisibility(View.VISIBLE);

                                                                    } else
                                                                        button.setVisibility(View.INVISIBLE);
                                                                    getActivity().finish();
                                                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                                                    startActivity(intent);
                                                                }
                                                            }
                                                        })
                                                        .show();
                                            }


                                        } catch (MalformedURLException e) {
                                            throw new RuntimeException(e);
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }

                            })
                            .setNegativeButton("Відміна", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (connected()) {
                                        Log.d("TAG", "onClick: " + "Відміна");
                                        if (array.length != 0) {
                                            button.setVisibility(View.VISIBLE);
                                        } else
                                            button.setVisibility(View.INVISIBLE);
                                    } else {
                                        getActivity().finish();
                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                        startActivity(intent);
                                    }
                                }
                            })
                            .show();
                } else {

                    sentPhone();
                    String message = (String) sendUrlMapCost.get("message");
                    new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                            .setMessage(message +
                                    " Спробуйте ще або зателефонуйте оператору.")
                            .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                    intent.setData(Uri.parse("tel:0934066749"));
                                    if (ActivityCompat.checkSelfPermission(getActivity(),
                                            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                        checkPermission(Manifest.permission.CALL_PHONE, StartActivity.READ_CALL_PHONE);
                                    }
                                    if (array.length != 0) {
                                        button.setVisibility(View.VISIBLE);

                                    } else button.setVisibility(View.INVISIBLE);

                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (connected()) {
                                        if (array.length != 0) {
                                            button.setVisibility(View.VISIBLE);

                                        } else button.setVisibility(View.INVISIBLE);
                                    } else {
                                        getActivity().finish();
                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                        startActivity(intent);
                                    }
                                }
                            })
                            .show();
                }

            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void dialogFromTo() throws MalformedURLException, InterruptedException {

        if(connected()) {

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme);
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.from_to_layout, null);
            builder.setView(view);

            from_number = view.findViewById(R.id.from_number);
            to_number = view.findViewById(R.id.to_number);
            if(array != null)  {
                button.setVisibility(View.VISIBLE);

            } else  button.setVisibility(View.INVISIBLE);


            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_dropdown_item_1line, arrayStreet);
            AutoCompleteTextView textViewFrom = view.findViewById(R.id.text_from);
            textViewFrom.setAdapter(adapter);

            textViewFrom.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(connected()) {
                        from = String.valueOf(adapter.getItem(position));
                        String url = "https://m.easy-order-taxi.site/api/android/autocompleteSearchComboHid/" + from;


                        Log.d("TAG", "onClick urlCost: " + url);
                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        String orderCost = (String) sendUrlMapCost.get("message");
                        Log.d("TAG", "onClick orderCost : " + orderCost);

                        if (orderCost.equals("1")) {
                            from_number.setVisibility(View.VISIBLE);
                            from_number.requestFocus();
                        }
                    }
                    else {
                        getActivity().finish();
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        startActivity(intent);
                    }
                }
            });

            AutoCompleteTextView textViewTo = view.findViewById(R.id.text_to);
            textViewTo.setAdapter(adapter);
            textViewTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(connected()) {
                        to = String.valueOf(adapter.getItem(position));
                        String url = "https://m.easy-order-taxi.site/api/android/autocompleteSearchComboHid/" + to;


                        Log.d("TAG", "onClick urlCost: " + url);
                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        String orderCost = (String) sendUrlMapCost.get("message");
                        Log.d("TAG", "onClick orderCost : " + orderCost);

                        if (orderCost.equals("1")) {
                            to_number.setVisibility(View.VISIBLE);
                            to_number.requestFocus();
                        }
                    }
                    else {
                        getActivity().finish();
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        startActivity(intent);
                    }
                }
            });


            builder.setMessage("Сформуйте маршрут")
                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(connected()) {
                                if (from != null) {
                                    if (to == null) {
                                        to = from;
                                        to_number.setText(from_number.getText());
                                    }
                                    try {
                                        String urlCost = getTaxiUrlSearch(from, from_number.getText().toString(), to, to_number.getText().toString(), "costSearch");

                                        Log.d("TAG", "onClick urlCost: " + urlCost);
                                        Map sendUrlMapCost = CostJSONParser.sendURL(urlCost);

                                        String orderCost = (String) sendUrlMapCost.get("order_cost");
                                        Log.d("TAG", "onClick orderCost : " + orderCost);

                                        if (!orderCost.equals("0")) {

                                            new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                    .setMessage("Вартість поїздки: " + orderCost + "грн")
                                                    .setPositiveButton("Замовити", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            if(connected()) {
                                                            Cursor cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                                            if (cursor.getCount() == 0) {
                                                                getPhoneNumber();
                                                                cursor.close();
                                                            } else {
                                                                String urlOrder = getTaxiUrlSearch(from, from_number.getText().toString(), to, to_number.getText().toString(), "orderSearch");
                                                                if (cursor != null && !cursor.isClosed())
                                                                    cursor.close();
                                                                try {
                                                                    Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);

                                                                    String orderWeb = (String) sendUrlMap.get("order_cost");
                                                                    if (!orderWeb.equals("0")) {

                                                                        String from_name = (String) sendUrlMap.get("from_name");
                                                                        String to_name = (String) sendUrlMap.get("to_name");
                                                                        if (from_name.equals(to_name)) {
                                                                            messageResult = "Дякуемо за замовлення зі " +
                                                                                    from_name + " " + from_number.getText() + " " + " по місту." +
                                                                                    " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";


                                                                        } else {
                                                                            messageResult = "Дякуемо за замовлення зі " +
                                                                                    from_name + " " + from_number.getText() + " " + " до " +
                                                                                    to_name + " " + to_number.getText() + "." +
                                                                                    " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";
                                                                        }

                                                                        StartActivity.insertRecordsOrders(sendUrlMap,
                                                                                from_number.getText().toString(), to_number.getText().toString());

                                                                        new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                                                .setMessage(messageResult)
                                                                                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                                                    @Override
                                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                                        if(connected()) {
                                                                                            getActivity().finish();
                                                                                            Intent intent = new Intent(getActivity(), MainActivity.class);
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    }
                                                                                })
                                                                                .show();
                                                                    } else {
                                                                        String message = (String) sendUrlMap.get("message");
                                                                        new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                                                .setMessage(message +
                                                                                        " Спробуйте ще або зателефонуйте оператору.")
                                                                                .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                                                    @Override
                                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                                        Intent intent = new Intent(Intent.ACTION_CALL);
                                                                                        intent.setData(Uri.parse("tel:0934066749"));
                                                                                        if (ActivityCompat.checkSelfPermission(getActivity(),
                                                                                                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                                            checkPermission(Manifest.permission.CALL_PHONE, StartActivity.READ_CALL_PHONE);

                                                                                        }
                                                                                        button.setVisibility(View.VISIBLE);

                                                                                        startActivity(intent);
                                                                                    }
                                                                                })
                                                                                .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                                                    @Override
                                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                                        if(connected()) {
                                                                                            button.setVisibility(View.VISIBLE);
                                                                                            getActivity().finish();
                                                                                            Intent intent = new Intent(getActivity(), MainActivity.class);
                                                                                            startActivity(intent);
                                                                                        }
                                                                                    }
                                                                                })
                                                                                .show();
                                                                    }


                                                                } catch (MalformedURLException e) {
                                                                    throw new RuntimeException(e);
                                                                } catch (InterruptedException e) {
                                                                    throw new RuntimeException(e);
                                                                } catch (JSONException e) {
                                                                    throw new RuntimeException(e);
                                                                }
                                                            }
                                                            cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                                            if (cursor.getCount() == 0) {
                                                                phoneNumber();
                                                                cursor.close();
                                                            }


                                                        }
                                                    }})
                                                    .setNegativeButton("Відміна", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            if(connected()) {
                                                                getActivity().finish();
                                                                Intent intent = new Intent(getActivity(), MainActivity.class);
                                                                startActivity(intent);
                                                            }
                                                        }
                                                    })
                                                    .show();
                                        } else {

                                            String message = (String) sendUrlMapCost.get("message");
                                            new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                    .setMessage(message +
                                                            " Спробуйте ще або зателефонуйте оператору.")
                                                    .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            Intent intent = new Intent(Intent.ACTION_CALL);
                                                            intent.setData(Uri.parse("tel:0934066749"));
                                                            if (ActivityCompat.checkSelfPermission(getActivity(),
                                                                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                checkPermission(Manifest.permission.CALL_PHONE, StartActivity.READ_CALL_PHONE);
                                                            }
                                                            button.setVisibility(View.VISIBLE);

                                                            startActivity(intent);
                                                        }
                                                    })
                                                    .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            if(connected()) {
                                                                button.setVisibility(View.VISIBLE);
                                                                getActivity().finish();
                                                                Intent intent = new Intent(getActivity(), MainActivity.class);
                                                                startActivity(intent);
                                                            }
                                                        }
                                                    })
                                                    .show();
                                        }

                                    } catch (MalformedURLException e) {
                                        throw new RuntimeException(e);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else {
                                    Toast.makeText(getActivity(), "Вкажить місце відправлення", Toast.LENGTH_SHORT).show();
                                    getActivity().finish();
                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                    startActivity(intent);
                                }
                            }
                        }
                    })
                    .setNegativeButton("Маршрути", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(connected()) {
                                if (array != null) {
                                    Log.d("TAG", "onClick: btnRouts " + array.length);
                                    listView.setItemChecked(0, true);
                                    button.setVisibility(View.VISIBLE);
                                    Toast.makeText(getActivity(), "Обирайте зі списку попередніх поїздок", Toast.LENGTH_SHORT).show();
                                } else {
                                    getActivity().finish();
                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                    startActivity(intent);
                                }
                            } else {
                                getActivity().finish();
                                Intent intent = new Intent(getActivity(), MainActivity.class);
                                startActivity(intent);
                            }
                        }
                    })
                    .show();
        }
    }

    private String getTaxiUrlSearch(String from, String from_number, String to, String to_number, String urlAPI) {

        // Origin of route
        String str_origin = from + "/" + from_number;

        // Destination of route
        String str_dest = to + "/" + to_number;

        StartActivity.cursorDb = StartActivity.database.query(StartActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        String tarif =  StartActivity.logCursor(StartActivity.TABLE_SETTINGS_INFO).get(2);


        // Building the parameters to the web service

        String parameters = str_origin + "/" + str_dest + "/" + tarif;

        if(urlAPI.equals("orderSearch")) {
            String phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber;
        }

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/api/android/" + urlAPI + "/" + parameters;
        Log.d("TAG", "getTaxiUrlSearch: " + url);



        return url;
    }
    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(getActivity(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);
        }
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
                Toast.makeText(getActivity(), "Формат вводу номера телефону: +380936665544" , Toast.LENGTH_SHORT).show();
                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + mPhoneNumber);
//                getActivity().finish();

            } else {
                StartActivity.insertRecordsUser(mPhoneNumber);
            }
        }

    }

    private void phoneNumber() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme);

        LayoutInflater inflater = this.getLayoutInflater();

        View view = inflater.inflate(R.layout.phone_verify_layout, null);

        builder.setView(view);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText phoneNumber = view.findViewById(R.id.phoneNumber);
        phoneNumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                phoneNumber.setHint("");


            }
        });


//        String result = phoneNumber.getText().toString();
        builder.setTitle("Перевірка телефону")
                .setPositiveButton("Відправити", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                        Log.d("TAG", "onClick befor validate: ");
                        String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                        Log.d("TAG", "onClick No validate: " + val);
                        if (val == false) {
                            Toast.makeText(getActivity(), "Формат вводу номера телефону: +380936665544" , Toast.LENGTH_SHORT).show();
                            Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());
                            getActivity().finish();

                        } else {
                            StartActivity.insertRecordsUser(phoneNumber.getText().toString());
                            String urlOrder = getTaxiUrlSearch(from, from_number.getText().toString(), to, to_number.getText().toString(), "orderSearch");

                            try {
                                Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);

                                String orderWeb = (String) sendUrlMap.get("order_cost");
                                if (!orderWeb.equals("0")) {

                                    String from_name = (String) sendUrlMap.get("from_name");
                                    String to_name = (String) sendUrlMap.get("to_name");
                                    if (from_name.equals(to_name)) {
                                        messageResult = "Дякуемо за замовлення зі " +
                                                from_name + " " + from_number.getText() + " " + " по місту." +
                                                " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";

                                    } else {
                                        messageResult = "Дякуемо за замовлення зі " +
                                                from_name + " " + from_number.getText() + " " + " до " +
                                                to_name + " " + to_number.getText() + "." +
                                                " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";
                                    }

                                    StartActivity.insertRecordsOrders(sendUrlMap,
                                            from_number.getText().toString(), to_number.getText().toString());

                                    new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                            .setMessage(messageResult)
                                            .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    if(connected()) {
                                                        getActivity().finish();
                                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                                        startActivity(intent);
                                                    }

                                                }
                                            })
                                            .show();
                                } else {
                                    String message = (String) sendUrlMap.get("message");
                                    new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                            .setMessage(message +
                                                    " Спробуйте ще або зателефонуйте оператору.")
                                            .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                                    intent.setData(Uri.parse("tel:0934066749"));
                                                    if (ActivityCompat.checkSelfPermission(getActivity(),
                                                            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                        checkPermission(Manifest.permission.CALL_PHONE, StartActivity.READ_CALL_PHONE);

                                                    }
                                                    button.setVisibility(View.VISIBLE);

                                                    startActivity(intent);
                                                }
                                            })
                                            .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    if(connected()) {
                                                        if(array != null)  {
                                                            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_single_choice, array);
                                                            listView.setAdapter(adapter);
                                                            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                                                            listView.setItemChecked(0, true);

                                                        } else  button.setVisibility(View.INVISIBLE);
                                                    }

                                                }
                                            })
                                            .show();
                                }


                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        }
                    }
                })
                .show();

    }
}