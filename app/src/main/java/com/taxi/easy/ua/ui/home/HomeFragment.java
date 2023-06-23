package com.taxi.easy.ua.ui.home;

import static com.taxi.easy.ua.ui.start.StartActivity.READ_CALL_PHONE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
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
import com.taxi.easy.ua.ui.maps.OrderJSONParser;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;
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
    public String[] arrayStreet = StartActivity.arrayStreet;
    static FloatingActionButton fab, fab_call, fab_open_map, fab_gps;
    private final String TAG = "TAG";
    static Switch gpsSwitch;
    private static final int CM_DELETE_ID = 1;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {



        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        listView = binding.list;
        fab = binding.fab;
        fab_call = binding.fabCall;
        fab_open_map = binding.fabOpenMap;

        gpsSwitch = binding.gpsSwitch;
        gpsSwitch.setChecked(switchState());


        button = binding.btnRouts;

        if(connected()) {
            array = arrayToRoutsAdapter();
            try {
                dialogFromTo();

            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connected()) {
                    try {
                        dialogFromTo();

                    } catch (MalformedURLException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        fab_call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:0674443804"));
                startActivity(intent);
            }
        });
        fab_open_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(connected()){
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                        checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                    }
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Intent intent = new Intent(getActivity(), OpenStreetMapActivity.class);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        startActivity(intent);
                    }

                }
            }
        });
        gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                gpsSwitch.setChecked(switchState());
            }


        });

        if(array != null)  {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_single_choice, array);
            listView.setAdapter(adapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setItemChecked(0, true);
            registerForContextMenu(listView);

        } else  button.setVisibility(View.INVISIBLE);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    dialogFromToOneRout(StartActivity.routChoice(listView.getCheckedItemPosition() + 1));
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        });

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        gpsSwitch.setChecked(switchState());
    }

    private boolean  switchState() {
        LocationManager lm = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        if(!gps_enabled || !network_enabled) {
            return false;
        } else

            return true;
    };
    public static String[] join(String[] a, String [] b)
    {
        String [] c = new String[a.length + b.length];

        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
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
                if(!routMaps.get(i).get("from_street").toString().equals(routMaps.get(i).get("to_street").toString())) {
                    arrayRouts[i] = routMaps.get(i).get("from_street").toString() + " " +
                            routMaps.get(i).get("from_number").toString() + " -> " +
                            routMaps.get(i).get("to_street").toString() + " " +
                            routMaps.get(i).get("to_number").toString();
                } else {
                    arrayRouts[i] = routMaps.get(i).get("from_street").toString() + " " +
                            routMaps.get(i).get("from_number").toString() + " -> " +
                            getString(R.string.on_city);
                }

            }
        } else {
            arrayRouts = null;
        }
        return arrayRouts;
    }
    private void sentPhone () throws MalformedURLException {
        String phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
        String urlErrorString = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/sentPhone/" + phoneNumber;
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
            Toast.makeText(getActivity(), getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
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

                    if(!MainActivity.verifyOrder) {
                        Log.d(TAG, "dialogFromToOneRout FirebaseSignIn.verifyOrder: " + MainActivity.verifyOrder);
                        Toast.makeText(getActivity(), getString(R.string.cost_of_order) + orderCost + getString(R.string.firebase_false_message), Toast.LENGTH_SHORT).show();
                    } else {

                        new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                .setMessage(getString(R.string.cost_of_order) + orderCost + getString(R.string.UAH))
                                .setPositiveButton(getString(R.string.order), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (connected()) {
//                                        Intent intent = new Intent(getActivity(), FirebaseSignIn.class);
//                                        startActivity(intent);

                                            String urlOrder = getTaxiUrlSearch(from_street_rout, from_number_rout, to_street_rout, to_number_rout, "orderSearch");

                                            try {
                                                Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);

                                                String orderWeb = (String) sendUrlMap.get("order_cost");
                                                if (!orderWeb.equals("0")) {

                                                    String from_name = (String) sendUrlMap.get("from_name");
                                                    String to_name = (String) sendUrlMap.get("to_name");
                                                    if (from_name.equals(to_name)) {
                                                        messageResult = getString(R.string.thanks_message) +
                                                                from_name + " " + from_number_rout + " " + getString(R.string.on_city) +
                                                                getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);

                                                    } else {
                                                        messageResult = getString(R.string.thanks_message) +
                                                                from_name + " " + from_number_rout + " " + getString(R.string.to_message) +
                                                                to_name + " " + to_number_rout + "." +
                                                                getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);
                                                    }
                                                    button.setVisibility(View.INVISIBLE);

                                                    StartActivity.insertRecordsOrders(from_name, to_name,
                                                            from_number.getText().toString(), to_number.getText().toString());

                                                    new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                            .setMessage(messageResult)
                                                            .setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    if (connected()) {
                                                                        Log.d("TAG", "onClick ");
                                                                        if (array.length != 0) {
                                                                            button.setVisibility(View.VISIBLE);

                                                                        } else
                                                                            button.setVisibility(View.INVISIBLE);

//                                                                        getActivity().finish();
//                                                                    Intent intent = new Intent(getActivity(), MainActivity.class);
//                                                                    startActivity(intent);
                                                                    }
                                                                }
                                                            })
                                                            .show();
                                                } else {
                                                    String message = (String) sendUrlMap.get("message");
                                                    new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                            .setMessage(message)
                                                            .setPositiveButton(getString(R.string.help), new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                    intent.setData(Uri.parse("tel:0674443804"));
                                                                    startActivity(intent);
                                                                }
                                                            })
                                                            .setNegativeButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    if (connected()) {
                                                                        if (array.length != 0) {
                                                                            button.setVisibility(View.VISIBLE);

                                                                        } else
                                                                            button.setVisibility(View.INVISIBLE);
                                                                        getActivity().finish();
//                                                                    Intent intent = new Intent(getActivity(), MainActivity.class);
//                                                                    startActivity(intent);
                                                                    }
                                                                }
                                                            })
                                                            .show();
                                                }


                                            } catch (MalformedURLException | InterruptedException |
                                                     JSONException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    }

                                })
                                .setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
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
//                                        Intent intent = new Intent(getActivity(), MainActivity.class);
//                                        startActivity(intent);
                                        }
                                    }
                                })
                                .show();
                    }
                } else {

                    sentPhone();
                    String message = (String) sendUrlMapCost.get("message");
                    new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                            .setMessage(message + getString(R.string.next_try))
                            .setPositiveButton(getString(R.string.help), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Intent.ACTION_DIAL);
                                    intent.setData(Uri.parse("tel:0674443804"));
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (connected()) {
                                        if (array.length != 0) {
                                            button.setVisibility(View.VISIBLE);

                                        } else button.setVisibility(View.INVISIBLE);
                                    } else {
                                        getActivity().finish();
//                                        Intent intent = new Intent(getActivity(), MainActivity.class);
//                                        startActivity(intent);
                                    }
                                }
                            })
                            .show();
                }

            } catch (MalformedURLException | InterruptedException | JSONException e) {
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

            AutoCompleteTextView text_to = view.findViewById(R.id.text_to);

            CheckBox checkBox = view.findViewById(R.id.on_city);
            checkBox.setChecked(false);
            checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(checkBox.isChecked()) {
                        text_to.setVisibility(View.INVISIBLE);
                    } else {
                        text_to.setVisibility(View.VISIBLE);
                    }
                }
            });
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
                        if (from.indexOf("/") != -1) {
                            from = from.substring(0,  from.indexOf("/"));
                        };
                        String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/autocompleteSearchComboHid/" + from;


                        Log.d("TAG", "onClick urlCost: " + url);
                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException | InterruptedException | JSONException e) {
                            throw new RuntimeException(e);
                        }

                        String orderCost = (String) sendUrlMapCost.get("message");
                        Log.d("TAG", "onClick orderCost : " + orderCost);

                        if (orderCost.equals("1")) {
                            from_number.setVisibility(View.VISIBLE);
                            from_number.requestFocus();
                        }
                    }
//                    else {
//                        getActivity().finish();
//                        Intent intent = new Intent(getActivity(), MainActivity.class);
//                        startActivity(intent);
//                    }
                }
            });

            AutoCompleteTextView textViewTo = view.findViewById(R.id.text_to);
            textViewTo.setAdapter(adapter);
            textViewTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(connected()) {
                        to = String.valueOf(adapter.getItem(position));
                        if (to.indexOf("/") != -1) {
                            to = to.substring(0,  to.indexOf("/"));
                        };
                        String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/autocompleteSearchComboHid/" + to;


                        Log.d("TAG", "onClick urlCost: " + url);
                        Map sendUrlMapCost = null;
                        try {
                            sendUrlMapCost = ResultSONParser.sendURL(url);
                        } catch (MalformedURLException | InterruptedException | JSONException e) {
                            throw new RuntimeException(e);
                        }

                        String orderCost = (String) sendUrlMapCost.get("message");
                        Log.d("TAG", "onClick orderCost : " + orderCost);

                        if (orderCost.equals("1")) {
                            to_number.setVisibility(View.VISIBLE);
                            to_number.requestFocus();
                        }
                    }
//                    else {
//                        getActivity().finish();
//                        Intent intent = new Intent(getActivity(), MainActivity.class);
//                        startActivity(intent);
//                    }
                }
            });


            builder.setMessage(getString(R.string.make_rout))
                    .setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
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
                                        String message = (String) sendUrlMapCost.get("message");


                                        if (orderCost.equals("0")) {

                                            Toast.makeText(getActivity(), getString(R.string.error_message) + message, Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(getActivity(), MainActivity.class);
                                            startActivity(intent);
                                        }
                                        if (!orderCost.equals("0")) {
                                            if(!MainActivity.verifyOrder) {
                                                Log.d(TAG, "dialogFromToOneRout FirebaseSignIn.verifyOrder: " + MainActivity.verifyOrder);
                                                Toast.makeText(getActivity(), getString(R.string.cost_of_order) + orderCost + getString(R.string.firebase_false_message), Toast.LENGTH_SHORT).show();
                                            } else {

                                                new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                        .setMessage(getString(R.string.cost_of_order) + orderCost + getString(R.string.UAH))
                                                        .setPositiveButton(getString(R.string.order), new DialogInterface.OnClickListener() {
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
                                                                                    messageResult = getString(R.string.thanks_message) +
                                                                                            from_name + " " + from_number.getText() + " " +  getString(R.string.on_city) +
                                                                                            getString(R.string.cost_of_order) + orderWeb + getString(R.string.UAH);


                                                                                } else {
                                                                                    messageResult =  getString(R.string.thanks_message) +
                                                                                            from_name + " " + from_number.getText() + " " + getString(R.string.to_message) +
                                                                                            to_name + " " + to_number.getText() + "." +
                                                                                            getString(R.string.cost_of_order) + orderWeb + getString(R.string.UAH);
                                                                                }

                                                                                StartActivity.insertRecordsOrders(from_name, to_name,
                                                                                        from_number.getText().toString(), to_number.getText().toString());

                                                                                new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                                                        .setMessage(messageResult)
                                                                                        .setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
                                                                                            @Override
                                                                                            public void onClick(DialogInterface dialog, int which) {
//                                                                                        if(connected()) {
                                                                                                getActivity().finish();
////                                                                                            Intent intent = new Intent(getActivity(), MainActivity.class);
////                                                                                            startActivity(intent);
//                                                                                        }
                                                                                            }
                                                                                        })
                                                                                        .show();
                                                                            } else {
                                                                                String message = (String) sendUrlMap.get("message");
                                                                                new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                                                        .setMessage(message + getString(R.string.try_again))
                                                                                        .setPositiveButton(getString(R.string.help), new DialogInterface.OnClickListener() {
                                                                                            @Override
                                                                                            public void onClick(DialogInterface dialog, int which) {
                                                                                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                                                                                intent.setData(Uri.parse("tel:0674443804"));
                                                                                                startActivity(intent);

                                                                                            }
                                                                                        })
                                                                                        .setNegativeButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
                                                                                            @Override
                                                                                            public void onClick(DialogInterface dialog, int which) {
                                                                                                if(connected()) {
                                                                                                    button.setVisibility(View.VISIBLE);
                                                                                                    getActivity().finish();
//                                                                                            Intent intent = new Intent(getActivity(), MainActivity.class);
//                                                                                            startActivity(intent);
                                                                                                }
                                                                                            }
                                                                                        })
                                                                                        .show();
                                                                            }


                                                                        } catch (MalformedURLException |
                                                                                 InterruptedException |
                                                                                 JSONException e) {
                                                                            throw new RuntimeException(e);
                                                                        }
                                                                    }
                                                                    cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
                                                                    if (cursor.getCount() == 0) {
                                                                        Toast.makeText(getActivity(), getString(R.string.format_phone), Toast.LENGTH_SHORT).show();
                                                                        phoneNumber();
                                                                        cursor.close();
                                                                    }


                                                                }
                                                            }})
                                                        .setNegativeButton(getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
//                                                            if(connected()) {
                                                                getActivity().finish();
//                                                                Intent intent = new Intent(getActivity(), MainActivity.class);
//                                                                startActivity(intent);
                                                            }
//                                                        }
                                                        })
                                                        .show();
                                            }
                                        }

                                    } catch (MalformedURLException | InterruptedException |
                                             JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else {
                                    Toast.makeText(getActivity(), getString(R.string.rout_from_message), Toast.LENGTH_SHORT).show();
                                    getActivity().finish();
//                                    Intent intent = new Intent(getActivity(), MainActivity.class);
//                                    startActivity(intent);
                                }
                            }
                        }
                    })
                    .setNegativeButton( getString(R.string.routs_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if(connected()) {
                                if (array != null) {
                                    Log.d("TAG", "onClick: btnRouts " + array.length);
                                    listView.setItemChecked(0, true);
                                    button.setVisibility(View.VISIBLE);
                                    Toast.makeText(getActivity(), getString(R.string.old_routs_message), Toast.LENGTH_SHORT).show();
                                } else {
                                    getActivity().finish();
//                                    Intent intent = new Intent(getActivity(), MainActivity.class);
//                                    startActivity(intent);
                                }
                            } else {
                                getActivity().finish();
//                                Intent intent = new Intent(getActivity(), MainActivity.class);
//                                startActivity(intent);
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

        String parameters = null;
        String phoneNumber = "no phone";

        if(urlAPI.equals("costSearch")) {
            Cursor c = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
            if (c.getCount() == 1) {
                phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + StartActivity.displayName + "(" + StartActivity.userEmail + ")";
        }

        if(urlAPI.equals("orderSearch")) {
            phoneNumber = StartActivity.logCursor(StartActivity.TABLE_USER_INFO).get(1);
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/" + StartActivity.displayName ;
        }

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/" + StartActivity.api + "/android/" + urlAPI + "/" + parameters;
        Log.d("TAG", "getTaxiUrlSearch: " + url);



        return url;
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
        builder.setTitle(getString(R.string.verify_phone))
                .setPositiveButton(getString(R.string.sent_button), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                            Log.d("TAG", "onClick befor validate: ");
                            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                            boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                            Log.d("TAG", "onClick No validate: " + val);
                            if (val == false) {
                                Toast.makeText(getActivity(), getString(R.string.format_phone) , Toast.LENGTH_SHORT).show();
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
                                            messageResult = getString(R.string.thanks_message) +
                                                    from_name + " " + from_number.getText() + " " + " по місту." +
                                                    getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);

                                        } else {
                                            messageResult = getString(R.string.thanks_message) +
                                                    from_name + " " + from_number.getText() + " " + getString(R.string.on_city) +
                                                    to_name + " " + to_number.getText() + "." +
                                                    getString(R.string.call_of_order) + orderWeb + getString(R.string.UAH);
                                        }

                                        StartActivity.insertRecordsOrders(from_name, to_name,
                                                from_number.getText().toString(), to_number.getText().toString());

                                        new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                .setMessage(messageResult)
                                                .setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
//                                                    if(connected()) {
                                                        getActivity().finish();
////                                                        Intent intent = new Intent(getActivity(), MainActivity.class);
////                                                        startActivity(intent);
//                                                    }

                                                    }
                                                })
                                                .show();
                                    } else {
                                        String message = (String) sendUrlMap.get("message");
                                        new MaterialAlertDialogBuilder(getActivity(), R.style.AlertDialogTheme)
                                                .setMessage(message + getString(R.string.next_try))
                                                .setPositiveButton(getString(R.string.help_button), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Intent intent = new Intent(Intent.ACTION_DIAL);
                                                        intent.setData(Uri.parse("tel:0674443804"));
                                                        startActivity(intent);


                                                    }
                                                })
                                                .setNegativeButton(getString(R.string.try_again), new DialogInterface.OnClickListener() {
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


                                } catch (MalformedURLException | InterruptedException | JSONException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    }
                })
                .show();

    }
    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(getActivity(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);
        }
        Log.d(TAG, "checkPermission: +++ " +  ContextCompat.checkSelfPermission(getActivity(), permission));
    }
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CM_DELETE_ID, 0, R.string.delete_record);
    }

    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == CM_DELETE_ID) {
            // получаем из пункта контекстного меню данные по пункту списка
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item
                    .getMenuInfo();
            StartActivity.reIndexOrders();
            // извлекаем id записи и удаляем соответствующую запись в БД
            long del_id = acmi.id+1;
            int i_del =  StartActivity.database.delete(StartActivity.TABLE_ORDERS_INFO, "id = " + del_id, null);
            StartActivity.reIndexOrders();
            getActivity().finish();

            return true;
        }
        return super.onContextItemSelected(item);
    }
}