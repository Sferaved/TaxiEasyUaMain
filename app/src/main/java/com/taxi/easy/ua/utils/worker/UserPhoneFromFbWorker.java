package com.taxi.easy.ua.utils.worker;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.user.save_firebase.FirebaseUserManager;

import java.util.regex.Pattern;

public class UserPhoneFromFbWorker extends ListenableWorker {
    private static final String TAG = "UserPhoneFromFbWorker";

    public UserPhoneFromFbWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        SettableFuture<Result> future = SettableFuture.create();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Logger.d(getApplicationContext(), TAG, "User not logged in");
            future.set(Result.failure());
            return future;
        }

        String userId = currentUser.getUid();
        FirebaseUserManager userManager = new FirebaseUserManager();

        userManager.getUserPhoneById(userId, phone -> {
            if (phone != null) {
                Logger.d(getApplicationContext(), TAG, "User phone: " + phone);

                String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";
                boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();

                if (val) {
                    updateRecordsUser("phone_number", phone);
                    future.set(Result.success());
                } else {
                    Logger.d(getApplicationContext(), TAG, "Phone does not match pattern");
                    future.set(Result.failure());
                }
            } else {
                Logger.d(getApplicationContext(), TAG, "Phone is null");
                future.set(Result.failure());
            }
        });

        return future;
    }

    private void updateRecordsUser(String field, String result) {
        ContentValues cv = new ContentValues();

        cv.put(field, result);

        // обновляем по id
        SQLiteDatabase database = MyApplication.getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();



    }

}
