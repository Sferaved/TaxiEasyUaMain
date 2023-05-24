package com.taxi.easy.ua;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.taxi.easy.ua.databinding.ActivityMainBinding;
import com.taxi.easy.ua.ui.start.StartActivity;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

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
        Intent intent;
        if(item.getItemId() == R.id.action_settings) {
           settings();
        }

        if (item.getItemId() == R.id.action_exit) {
                intent = new Intent(this, StartActivity.class);
                startActivity(intent);
                this.finish();
        }

        return false;
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
            "Базовый",
            "Универсал",
            "Бизнес-класс",
            "Премиум-класс",
            "Эконом-класс",
            "Микроавтобус",
            "Базовый онлайн",
            "Минивэн",
            "Такси Терминал",
            };
    }
}