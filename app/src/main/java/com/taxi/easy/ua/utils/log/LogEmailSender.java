package com.taxi.easy.ua.utils.log;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LogEmailSender {

    private static final String TAG = "LogEmailSender";

    private final Context context; 

    public LogEmailSender(Context context) {
        this.context = context; 
    }

    /**
     * Отправка лог-файла по Email как вложение
     */
    public void sendLog() {
        File logFile = new File(context.getExternalFilesDir(null), "app_log.txt");
        if (!logFile.exists()) {
            Logger.e(context, TAG, "Log file does not exist");
            return;
        }

      

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city = stringList.get(1);

        List<String> userList = logCursor(MainActivity.TABLE_USER_INFO);

        String subject = context.getString(R.string.SA_subject) + generateRandomString(10);
        String body = context.getString(R.string.SA_message_start) + "\n\n" +
                context.getString(R.string.SA_info_pas) + "\n" +
                context.getString(R.string.SA_info_city) + " " + city + "\n" +
                context.getString(R.string.SA_pas_text) + " " + context.getString(R.string.version) + "\n" +
                context.getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
                context.getString(R.string.SA_email) + " " + userList.get(3) + "\n" +
                context.getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n\n";


        try {
            // Получаем безопасный URI через FileProvider
            Uri fileUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    logFile
            );

            String[] CC = {"cartaxi4@gmail.com"};
            String[] TO = {MainActivity.supportEmail};
            
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822");
            emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
            emailIntent.putExtra(Intent.EXTRA_CC, CC);
            
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);
            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

            // Разрешаем временный доступ к файлу
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(emailIntent, "Send log via email"));
        } catch (Exception e) {
            Logger.e(context, TAG, "Failed to send log email: " + e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    /** Генерация случайной строки для уникальной темы письма */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = MyApplication.getCurrentActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = db.query(table, null, null, null, null, null, null);
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
        db.close();
        return list;
    }
}
