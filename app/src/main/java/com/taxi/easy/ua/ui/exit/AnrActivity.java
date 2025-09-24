package com.taxi.easy.ua.ui.exit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.databinding.FragmentAnrBinding;
import com.taxi.easy.ua.utils.connect.NetworkMonitor;
import com.taxi.easy.ua.utils.log.LogEmailSender;

import java.util.ArrayList;
import java.util.List;

public class AnrActivity extends AppCompatActivity {

    private NetworkMonitor networkMonitor;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.taxi.easy.ua.databinding.FragmentAnrBinding binding = FragmentAnrBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AppCompatButton btn_enter = binding.btnEnter;
        AppCompatButton btnCallAdmin = binding.btnCallAdmin;
        AppCompatButton btn_exit = binding.btnExit;
        AppCompatButton btnEmailAdmin = binding.btnEmailAdmin;

        btn_enter.setOnClickListener(v -> {
            startActivity(new Intent(AnrActivity.this, MainActivity.class));
            finish();
        });

        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, AnrActivity.this).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
            finish();
        });

        btnEmailAdmin.setOnClickListener(v -> {
            new LogEmailSender(this).sendLog();
        });

        btn_exit.setOnClickListener(v -> closeApplication());

        networkMonitor = new NetworkMonitor(this);
        networkMonitor.startMonitoring(this);
    }

    private void closeApplication() {
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
            do {
                for (String cn : c.getColumnNames()) {
                    list.add(c.getString(c.getColumnIndex(cn)));
                }
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }
}
