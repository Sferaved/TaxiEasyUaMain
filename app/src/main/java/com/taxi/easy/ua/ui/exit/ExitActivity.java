package com.taxi.easy.ua.ui.exit;


import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.clear.AppDataUtils;
import com.taxi.easy.ua.utils.auth.FirebaseConsentManager;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.user.del_server.UserRepository;
import com.taxi.easy.ua.utils.user.save_firebase.FirebaseUserManager;

import java.util.ArrayList;
import java.util.List;


public class ExitActivity extends AppCompatActivity {

    private static final String TAG = "ExitActivity";
    AppCompatButton btn_enter;
    AppCompatButton btnCallAdmin;
    AppCompatButton btn_exit;
    AppCompatButton btn_ok;
    private FirebaseUserManager userManager;
    UserRepository userRepository;

    private FirebaseConsentManager consentManager;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.exit_activity_layout);
        btn_enter = findViewById(R.id.btn_enter);
        btnCallAdmin = findViewById(R.id.btnCallAdmin);
        btn_exit = findViewById(R.id.btn_exit);
        btn_ok = findViewById(R.id.btn_ok);


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

        btn_ok.setOnClickListener(view16 -> {

            userManager = new FirebaseUserManager();
//            userManager.deleteUserPhone();
            userManager.saveUserPhone("+38");

            userRepository = new UserRepository();



            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                String userId = currentUser.getUid();
                userManager.getUserPhoneById(userId, new FirebaseUserManager.UserPhoneCallback() {
                    @Override
                    public void onUserPhoneRetrieved(String phone) {
                        if (phone != null) {
                            // Используйте phone по своему усмотрению
                            Logger.d(getApplicationContext(), TAG, "User phone: " + phone);

                            if (!phone.equals("+38")) {
                                Toast.makeText(getApplicationContext(), R.string.err_del_phone_fb, Toast.LENGTH_SHORT).show();
                            } else {
                                List<String> stringList =  logCursor(MainActivity.TABLE_USER_INFO, getApplicationContext());
                                String userEmail = stringList.get(3);
                                userRepository.destroyEmail(userEmail);
                                AppDataUtils.clearData(getApplicationContext());
                                Toast.makeText(getApplicationContext(), R.string.del_info, Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            List<String> stringList =  logCursor(MainActivity.TABLE_USER_INFO, getApplicationContext());
                            String userEmail = stringList.get(3);
                            userRepository.destroyEmail(userEmail);
                            Logger.d(getApplicationContext(), TAG, "Phone is null");
                            AppDataUtils.clearData(getApplicationContext());
                            Toast.makeText(getApplicationContext(), R.string.del_info, Toast.LENGTH_SHORT).show();

                        }
                    }
                });
            }

        });



    }
    private void resetUserInfo() {
        SQLiteDatabase database = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.beginTransaction();

        try {
            // Обновление первой записи
            String updateSql = "UPDATE " + MainActivity.TABLE_USER_INFO
                    + " SET verifyOrder = ?," +
                    " phone_number = ?," +
                    " email = ?," +
                    " username = ?," +
                    " bonus = ?," +
                    " card_pay = ?, " +
                    "bonus_pay = ? " +
                    "WHERE rowid = (SELECT rowid FROM " + MainActivity.TABLE_USER_INFO + " LIMIT 1);";
            SQLiteStatement updateStatement = database.compileStatement(updateSql);

            updateStatement.clearBindings();
            updateStatement.bindString(1, "0");
            updateStatement.bindString(2, "+38");
            updateStatement.bindString(3, "email");
            updateStatement.bindString(4, "username");
            updateStatement.bindString(5, "0");
            updateStatement.bindString(6, "1");
            updateStatement.bindString(7, "1");

            updateStatement.execute();
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        database.close();
    }
    private void updateRecordsUserInfo(String userInfo, String result, Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        ContentValues cv = new ContentValues();

        cv.put(userInfo, result);

        // обновляем по id
        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

    private void closeApplication() {
        // Полный выход из приложения
        finishAffinity();
        System.exit(0);
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = db.query(table, null, null, null, null, null, null);
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
        assert c != null;
        c.close();
        db.close();
        return list;
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Проверяем, идет ли приложение в фон
        if (isFinishing()) {
            // Закрываем приложение полностью
            closeApplication();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Проверяем, идет ли приложение в фон
        if (isFinishing()) {
            // Закрываем приложение полностью
            closeApplication();
        }
    }

    @Override
    public void onBackPressed() {
        // Ничего не делать, блокируя действие кнопки "назад"
        super.onBackPressed();
        closeApplication();
    }
}

