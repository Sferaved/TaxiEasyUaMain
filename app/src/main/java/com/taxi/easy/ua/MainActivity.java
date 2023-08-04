package com.taxi.easy.ua;

import static com.taxi.easy.ua.R.string.cancel_button;
import static com.taxi.easy.ua.R.string.format_phone;
import static com.taxi.easy.ua.R.string.verify_internet;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.taxi.easy.ua.databinding.ActivityMainBinding;
import com.taxi.easy.ua.ui.home.MyPhoneDialogFragment;
import com.taxi.easy.ua.ui.start.StartActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    NetworkChangeReceiver networkChangeReceiver;
    String  cityNew;
    String  cityNewMes;
    private final String[] cityList = new String[]{
            "Киев",
            "Тест"
    };
   private final String[] cityCode = new String[]{
            "Kyiv City",
            "Odessa"
    };
    String message;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_about)
                .setOpenableLayout(drawer)
                .build();



        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        networkChangeReceiver = new NetworkChangeReceiver();

//        Toast.makeText(this, getString(R.string.wellcome), Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.phone_settings) {
                phoneNumberChange();
        }
        if (item.getItemId() == R.id.nav_driver) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxieasyua.job&pli=1"));
            startActivity(browserIntent);
        }
        if (item.getItemId() == R.id.action_exit) {
            finishAffinity();
        }
        if (item.getItemId() == R.id.gps) {
            eventGps(this);
        }

        if (item.getItemId() == R.id.nav_city) {
            cityChange();
        }
        if (item.getItemId() == R.id.send_email) {
            String subject = getString(R.string.android);
            String body = getString(R.string.good_day);

            String[] CC = {""};
            Intent emailIntent = new Intent(Intent.ACTION_SEND);

            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_CC, CC);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);

            try {
                startActivity(Intent.createChooser(emailIntent, getString(R.string.share)));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, getString(R.string.no_email_agent), Toast.LENGTH_SHORT).show();
            }

        }

        return false;
    }

    private void cityChange() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.city_change_layout, null);
        builder.setView(view);


        ArrayAdapter<String> adapterCity = new ArrayAdapter<String>(this, R.layout.my_simple_spinner_item, cityList);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Spinner spinner = view.findViewById(R.id.list_city);
        spinner.setAdapter(adapterCity);
        spinner.setPrompt("Выберите город");
        spinner.setBackgroundResource(R.drawable.spinner_border);

        String cityOld = logCursor(StartActivity.CITY_INFO, this).get(1);
        for (int i = 0; i < cityList.length; i++) {
            if (cityCode[i].equals(cityOld)) {
                spinner.setSelection(i);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cityNew =  cityCode[position];
                message = getString(R.string.your_city) + cityList[position];
                ContentValues cv = new ContentValues();
                SQLiteDatabase database = view.getContext().openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
                switch (cityNew ){
                    case "Kyiv City":
                        cv = new ContentValues();
                        cv.put("tarif", "Базовий онлайн");
                        database.update(StartActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                new String[] { "1" });
                        database.close();
                        break;
                    case "Odessa":
                        cv.put("tarif", "Базовый");
                        database.update(StartActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                new String[] { "1" });
                        database.close();
                        break;

                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        builder.setPositiveButton(R.string.cheng, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ContentValues cv = new ContentValues();

                        cv.put("city", cityNew);
                        // обновляем по id
                        SQLiteDatabase database = openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(StartActivity.CITY_INFO, cv, "id = ?",
                                new String[] { "1" });
                        startActivity(new Intent(MainActivity.this, MainActivity.class));
                        Toast.makeText(MainActivity.this, getString(R.string.change_message) + message   , Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton(cancel_button, null)
                .show();

    }

    public void eventGps(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {}

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {}

        Log.d("TAG", "onOptionsItemSelected gps_enabled: " + gps_enabled);
        Log.d("TAG", "onOptionsItemSelected network_enabled: " + network_enabled);
        if(!gps_enabled || !network_enabled) {
            // notify user
            MaterialAlertDialogBuilder builder =  new MaterialAlertDialogBuilder(MainActivity.this, R.style.AlertDialogTheme);
            LayoutInflater inflater = MainActivity.this.getLayoutInflater();

            View view_cost = inflater.inflate(R.layout.message_layout, null);
            builder.setView(view_cost);
            TextView message = view_cost.findViewById(R.id.textMessage);
            message.setText(R.string.gps_info);
            builder.setMessage(R.string.gps_info)
                    .setPositiveButton(R.string.gps_on, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.cancel_button,null)
                    .show();
        } else {
            Toast.makeText(context, context.getString(R.string.gps_ok), Toast.LENGTH_SHORT).show();
        }
    }
    public void phoneNumberChange() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);

        LayoutInflater inflater = this.getLayoutInflater();

        View view = inflater.inflate(R.layout.phone_settings_layout, null);

        builder.setView(view);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText phoneNumber = view.findViewById(R.id.phoneNumber);

        List<String> stringList =  logCursor(StartActivity.TABLE_USER_INFO, getApplicationContext());

        if(stringList.size() != 0) {
            phoneNumber.setText(stringList.get(2));


//        String result = phoneNumber.getText().toString();
        builder
                .setPositiveButton(R.string.cheng, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                            Log.d("TAG", "onClick befor validate: ");
                            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                            boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                            Log.d("TAG", "onClick No validate: " + val);
                            if (val == false) {
                                Toast.makeText(MainActivity.this, getString(format_phone) , Toast.LENGTH_SHORT).show();
                                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());

                            } else {
                                StartActivity.updateRecordsUser(phoneNumber.getText().toString());
                            }
                        }
                    }
                }).setNegativeButton(cancel_button, null)
                .show();
        } else {
            if (!verifyPhone(getApplicationContext())) {
                getPhoneNumber();
            }
            if (!verifyPhone(getApplicationContext())) {
                MyPhoneDialogFragment bottomSheetDialogFragment = new MyPhoneDialogFragment();
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
            }
        }
    }

    private static boolean verifyPhone(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
        boolean verify = true;
        if (cursor.getCount() == 1) {

            if (logCursor(StartActivity.TABLE_USER_INFO, context).get(2).equals("+380")) {
                verify = false;
            }
            cursor.close();
        }

        return verify;
    }

    private void getPhoneNumber () {
        String mPhoneNumber;
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.d("TAG", "Manifest.permission.READ_PHONE_NUMBERS: " + ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_NUMBERS));
            Log.d("TAG", "Manifest.permission.READ_PHONE_STATE: " + ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE));
            return;
        }
        mPhoneNumber = tMgr.getLine1Number();
//        mPhoneNumber = null;
        if(mPhoneNumber != null) {
            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
            boolean val = Pattern.compile(PHONE_PATTERN).matcher(mPhoneNumber).matches();
            Log.d("TAG", "onClick No validate: " + val);
            if (val == false) {
                Toast.makeText(this, format_phone , Toast.LENGTH_SHORT).show();
                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + mPhoneNumber);
//                getActivity().finish();

            } else {
                StartActivity.insertRecordsUser(mPhoneNumber);
            }
        }

    }

    private boolean connected() {

        Boolean hasConnect = false;

        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(
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
            Toast.makeText(this, verify_internet, Toast.LENGTH_LONG).show();
        }
        Log.d("TAG", "connected: " + hasConnect);
        return hasConnect;
    }


    @Override
    protected void onStart() {
        registerReceiver(networkChangeReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        super.onStart();
        // Создание фильтра намерений для отслеживания изменений подключения к интернету
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        // Регистрация BroadcastReceiver с фильтром намерений
        registerReceiver(networkChangeReceiver, filter);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeReceiver);
        super.onStop();
    }
    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
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
        return list;
    }

}