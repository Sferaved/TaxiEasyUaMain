package com.taxi.easy.ua.ui.exit;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.connect.NetworkMonitor;

import java.util.ArrayList;
import java.util.List;


public class AnrActivity extends AppCompatActivity {

    private static final String TAG = "AnrActivity";
    AppCompatButton btn_enter;
    AppCompatButton btnCallAdmin;
    AppCompatButton btn_exit;
    AppCompatButton btn_ok;

    private NetworkMonitor networkMonitor;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.anr_activity_layout);
        btn_enter = findViewById(R.id.btn_enter);
        btnCallAdmin = findViewById(R.id.btnCallAdmin);
        btn_exit = findViewById(R.id.btn_exit);
        btn_ok = findViewById(R.id.btn_ok);

        // Регистрируем обработчик нажатия кнопки "назад"
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Блокируем действие кнопки назад и вызываем вашу логику
                closeApplication();
            }
        });
        btn_enter.setOnClickListener(view15 -> {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        });
        btnCallAdmin.setOnClickListener(view16 -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, getApplicationContext()).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });
        btn_exit.setOnClickListener(view16 -> {
            closeApplication();
        });

        networkMonitor = new NetworkMonitor(this);
        networkMonitor.startMonitoring(this);

    }

    private void closeApplication() {
        // Полный выход из приложения
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
        }
        finishAffinity();
        System.exit(0);
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = db.query(table, null, null, null, null, null, null);
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
        c.close();
        db.close();
        return list;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Проверяем, идет ли приложение в фон
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
        }
        if (isFinishing()) {
            // Закрываем приложение полностью
            closeApplication();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
        }
        // Проверяем, идет ли приложение в фон
        if (isFinishing()) {
            // Закрываем приложение полностью
            closeApplication();
        }
    }

}

