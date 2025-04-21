package com.taxi.easy.ua.utils.permissions;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserPermissions {
    private static final String TAG = "TAG_PERM";

    public UserPermissions() {
    }

    public static void getPermissions(String email, Context context) {
        ApiServicePermissions apiServicePermissions = ApiClientPermissions.getRetrofitInstance().create(ApiServicePermissions.class);

        Call<PermissionsResponse> call = apiServicePermissions.getPermissions(email);

        call.enqueue(new Callback<PermissionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<PermissionsResponse> call, @NonNull Response<PermissionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PermissionsResponse permissionsResponse = response.body();
                    if (permissionsResponse != null) {
                        Log.d(TAG, "Bonus Pay: " + permissionsResponse.getBonusPay());
                        Log.d(TAG, "Card Pay: " + permissionsResponse.getCardPay());

                        ContentValues cv = new ContentValues();
                        cv.put("bonus_pay", permissionsResponse.getBonusPay());
                        cv.put("card_pay", permissionsResponse.getCardPay());

                        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
                        database.close();
                    }
                } else {
                        Log.d(TAG,"Request failed with code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<PermissionsResponse> call, @NonNull Throwable t) {
                Log.d(TAG,"Request failed: " + t.getMessage());
            }
        });
    }

    public static String[] getUserPayPermissions(Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        String[] columns = {"card_pay", "bonus_pay"};
        Cursor cursor = database.query(MainActivity.TABLE_USER_INFO, columns, null, null, null, null, null);
        String[] values = new String[2];
        if (cursor.moveToFirst()) {
            // Получение значений из курсора
            @SuppressLint("Range") String cardPayValue = cursor.getString(cursor.getColumnIndex("card_pay"));
            @SuppressLint("Range") String bonusPayValue = cursor.getString(cursor.getColumnIndex("bonus_pay"));

            // Используйте значения, как вам необходимо
            values[0] = bonusPayValue;
            values[1] = cardPayValue;
        }
        Log.d(TAG, "getUserPayPermissions: values" + Arrays.toString(values));
        cursor.close();
        return values;
    }

}

