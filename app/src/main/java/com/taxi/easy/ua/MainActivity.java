package com.taxi.easy.ua;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
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
import com.taxi.easy.ua.ui.start.StartActivity;

import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    NetworkChangeReceiver networkChangeReceiver;
    public static boolean verifyOrder;
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
                R.id.nav_home, R.id.nav_about)
                .setOpenableLayout(drawer)
                .build();



        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        networkChangeReceiver = new NetworkChangeReceiver();

        Toast.makeText(this, "Ласкаво просимо. Сформуйте маршрут або виберіть улюблений.", Toast.LENGTH_LONG).show();
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
        if(item.getItemId() == R.id.action_settings) {
           settings();
        }
        if (item.getItemId() == R.id.phone_settings) {
                phoneNumberChange();
        }
        if (item.getItemId() == R.id.nav_driver) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.taxieasyua.job"));
            startActivity(browserIntent);
        }
        if (item.getItemId() == R.id.action_exit) {
                this.finish();
        }
        if (item.getItemId() == R.id.send_email) {
            String subject = "Андроїд-додаток для швидких та дешевих поїздок по Києву та області.";
            String body = "Мої вітання. \n \n Знайшов чудовий додаток для поїздок на таксі. \n \n Раджу спробувати за посиланням в офіційному магазині Google: \n\n https://play.google.com/store/apps/details?id=com.taxieasyua.job \n\n Гарного дня. \n Ще побачимось.";

            String[] CC = {""};
            Intent emailIntent = new Intent(Intent.ACTION_SEND);

            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.setType("text/plain");
            emailIntent.putExtra(Intent.EXTRA_CC, CC);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);

            try {
                startActivity(Intent.createChooser(emailIntent, "Порадити другові додаток..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "Поштовий клієнт не встановлено.", Toast.LENGTH_SHORT).show();
            }

        }

        return false;
    }

    public void phoneNumberChange() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);

        LayoutInflater inflater = this.getLayoutInflater();

        View view = inflater.inflate(R.layout.phone_verify_layout, null);

        builder.setView(view);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText phoneNumber = view.findViewById(R.id.phoneNumber);

        List<String> stringList =  StartActivity.logCursor(StartActivity.TABLE_USER_INFO);
        Log.d("TAG", "phoneNumberChange stringList: " + stringList.size());
        if(stringList.size() != 0) {
            phoneNumber.setText(stringList.get(1));


//        String result = phoneNumber.getText().toString();
        builder.setTitle("Перевірка телефону")
                .setPositiveButton("Змінити", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(connected()) {
                            Log.d("TAG", "onClick befor validate: ");
                            String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
                            boolean val = Pattern.compile(PHONE_PATTERN).matcher(phoneNumber.getText().toString()).matches();
                            Log.d("TAG", "onClick No validate: " + val);
                            if (val == false) {
                                Toast.makeText(MainActivity.this, "Формат вводу номера телефону: +380936665544" , Toast.LENGTH_SHORT).show();
                                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());

                            } else {
                                StartActivity.updateRecordsUser(phoneNumber.getText().toString());
                            }
                        }
                    }
                }).setNegativeButton("Відміна", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                })
                .show();
        } else {
            getPhoneNumber ();
            Cursor cursor = StartActivity.database.query(StartActivity.TABLE_USER_INFO, null, null, null, null, null, null);
            if (cursor.getCount() == 0) {
                Toast.makeText(MainActivity.this, "Формат вводу номера телефону: +380936665544", Toast.LENGTH_SHORT).show();
                phoneNumber();
                cursor.close();
            }
        }
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
                Toast.makeText(this, "Формат вводу номера телефону: +380936665544" , Toast.LENGTH_SHORT).show();
                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + mPhoneNumber);
//                getActivity().finish();

            } else {
                StartActivity.insertRecordsUser(mPhoneNumber);
            }
        }

    }
    private void phoneNumber() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);

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
                                Toast.makeText(MainActivity.this, "Формат вводу номера телефону: +380936665544" , Toast.LENGTH_SHORT).show();
                                Log.d("TAG", "onClick:phoneNumber.getText().toString() " + phoneNumber.getText().toString());
                                MainActivity.this.finish();

                            } else {
                                StartActivity.insertRecordsUser(phoneNumber.getText().toString());
                            }
                        }
                    }
                })
                .show();

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
            Toast.makeText(this, "Перевірте інтернет-підключення або зателефонуйте оператору.", Toast.LENGTH_LONG).show();
        }
        Log.d("TAG", "connected: " + hasConnect);
        return hasConnect;
    }
    private String[] tariffArr = tariffs();
    private String tariff;
    private void settings () {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.settings_layout, null);


        ArrayAdapter<String> adapterTariff = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, tariffArr);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Spinner spinner = view.findViewById(R.id.list_tariff);
        spinner.setAdapter(adapterTariff);
        spinner.setPrompt("Title");
        StartActivity.cursorDb = StartActivity.database.query(StartActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        String tariffOld =  StartActivity.logCursor(StartActivity.TABLE_SETTINGS_INFO).get(2);
        if (StartActivity.cursorDb != null && !StartActivity.cursorDb.isClosed())
            StartActivity.cursorDb.close();
        for (int i = 0; i < tariffArr.length; i++) {
            if(tariffArr[i].equals(tariffOld)) {
                spinner.setSelection(i);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
               tariff = tariffArr[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });







        builder.setView(view)
                        .setPositiveButton("Зберегти", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ContentValues cv = new ContentValues();
                                cv.put("tarif", tariff);

                                // обновляем по id
                                StartActivity.database.update(StartActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                                        new String[] { "1" });
                                Log.d("TAG", "onClick: " + StartActivity.logCursor(StartActivity.TABLE_SETTINGS_INFO));
                                Intent intent =  new Intent(MainActivity.this, MainActivity.class);
                                startActivity(intent);

                           }
                        }).setNegativeButton("Відміна", new DialogInterface.OnClickListener() {
                                 @Override
                                 public void onClick(DialogInterface dialog, int which) {
                                     Intent intent =  new Intent(MainActivity.this, MainActivity.class);
                                     startActivity(intent);
                                 }
                             })

                        .show();
    }
    
    private String[] tariffs () {
        return new String[]{
                "Базовий онлайн",
                "Базовый",
                "Универсал",
                "Бизнес-класс",
                "Премиум-класс",
                "Эконом-класс",
                "Микроавтобус",
            };
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
}