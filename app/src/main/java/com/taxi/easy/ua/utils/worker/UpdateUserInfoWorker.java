package com.taxi.easy.ua.utils.worker;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.worker.utils.UserUtils;

public class UpdateUserInfoWorker extends Worker {
    private static final String TAG = "UpdateUserInfoWorker";

    public UpdateUserInfoWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String emailUser = getInputData().getString("emailUser");
        try {
            UserUtils.updateRecordsUserInfo("email", emailUser, getApplicationContext());
            ContentValues cv = new ContentValues();
            cv.put("verifyOrder", "1");
            SQLiteDatabase database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?", new String[]{"1"});
            database.close();
            Log.d(TAG, "settingsNewUser: " + emailUser);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка в updateUserInfo: " + e.getMessage());
            return Result.failure();
        }
    }
}