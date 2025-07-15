package com.taxi.easy.ua.utils.user.user_verify;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Context;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.HashMap;
import java.util.Map;

public class VerifyUserTask {
    private static final String TAG = "VerifyUserTask";
    private final Context context;

    private static ListenerRegistration listenerRegistration;

    public VerifyUserTask(Context context) {
        this.context = context;
    }

    public void execute() {
        Logger.d(context, TAG, "execute() started with Firestore");

        Map<String, String> userInfo = getUserInfoFromCursor();
        String userEmail = userInfo.get("email");

        if (userEmail == null || userEmail.isEmpty()) {
            Logger.e(context, TAG, "User email not found in DB");
            return;
        }
        Logger.d(context, TAG, "User is " + userEmail);
        listenerRegistration = FirebaseFirestore.getInstance().collection("blackList")
                .whereEqualTo("email", userEmail)
                .addSnapshotListener((QuerySnapshot snapshots, FirebaseFirestoreException e) -> {
                    Logger.d(context, TAG, "🔥 Firestore listener triggered");
                    if (e != null) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                        Logger.e(context, TAG, "Firestore listener error: " + e.getMessage());
                        return;
                    }

                    boolean inBlackList = snapshots != null && !snapshots.isEmpty();
                    Logger.d(context, TAG, "User is " + (inBlackList ? "in" : "not in") + " blackList");

                    // ✅ Сохраняем в SharedPreferences
                    sharedPreferencesHelperMain.saveValue("verifyUserOrder", inBlackList);
                    Logger.d(context, TAG, "sharedPreferencesHelperMain " + sharedPreferencesHelperMain.getValue("verifyUserOrder", false));

                });
    }

    public static void stopListener() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private Map<String, String> getUserInfoFromCursor() {
        Map<String, String> result = new HashMap<>();
        try (var database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
             var cursor = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null)) {

            if (cursor.moveToFirst()) {
                for (String column : cursor.getColumnNames()) {
                    result.put(column, cursor.getString(cursor.getColumnIndex(column)));
                }
            } else {
                Logger.w(context, TAG, "Cursor is empty for table: " + MainActivity.TABLE_USER_INFO);
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            Logger.e(context, TAG, "Error while reading user info: " + e.getMessage());
        }

        return result;
    }
}
