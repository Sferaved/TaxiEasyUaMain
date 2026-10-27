package com.taxi.easy.ua.ui.exit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.FragmentAnrBinding;
import com.taxi.easy.ua.utils.bugreport.CrashScreenBugReportSender;
import com.taxi.easy.ua.utils.bugreport.mantis.MantisBugReportSender;
import com.taxi.easy.ua.utils.connect.NetworkMonitor;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.phone_state.PhoneCallHelper;

import java.util.ArrayList;
import java.util.List;
import com.taxi.easy.ua.utils.db.CursorReadHelper;

public class AnrActivity extends AppCompatActivity {

    private NetworkMonitor networkMonitor;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PhoneCallHelper.initWithActivity(this);
        //PhoneCallHelper.ensureCallPermission();
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
            PhoneCallHelper.callWithFallback(() -> {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, MyApplication.getContext());
                return stringList.size() > 3 ? stringList.get(3) : "";
            });
//            Intent intent = new Intent(Intent.ACTION_DIAL);
//            String phone = logCursor(MainActivity.CITY_INFO, AnrActivity.this).get(3);
//            intent.setData(Uri.parse(phone));
//            startActivity(intent);
            finish();
        });

        btnEmailAdmin.setOnClickListener(v -> sendCrashReport(btnEmailAdmin));

        btn_exit.setOnClickListener(v -> closeApplication());

        networkMonitor = new NetworkMonitor(this);
        networkMonitor.startMonitoring();
    }

    private void sendCrashReport(AppCompatButton button) {
        button.setEnabled(false);
        Toast.makeText(this, R.string.sending_report, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                MantisBugReportSender.DeliveryResult deliveryResult = CrashScreenBugReportSender.send(this);
                runOnUiThread(() -> showDeliveryResult(deliveryResult));
            } catch (Exception e) {
                Logger.e(this, "AnrActivity", "Crash report send failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(
                        this,
                        getString(R.string.error_sending, e.getMessage()),
                        Toast.LENGTH_LONG
                ).show());
            } finally {
                runOnUiThread(() -> button.setEnabled(true));
            }
        }).start();
    }

    private void showDeliveryResult(MantisBugReportSender.DeliveryResult deliveryResult) {
        if (isFinishing()) {
            return;
        }
        if (deliveryResult.usedMantis) {
            Toast.makeText(this, getString(R.string.sent_to_mantis, deliveryResult.issueId), Toast.LENGTH_LONG).show();
        } else if (deliveryResult.logFileSizeKb > 0) {
            Toast.makeText(
                    this,
                    getString(R.string.sent_to_telegram_with_logs, deliveryResult.logFileSizeKb),
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(this, R.string.sent_to_telegram, Toast.LENGTH_SHORT).show();
        }
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
                    list.add(CursorReadHelper.getString(c, cn));
                }
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }
}
