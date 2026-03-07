package com.taxi.easy.ua.utils.log;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

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
            Toast.makeText(context, "Лог-файл не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        // Получаем данные из БД с проверкой
        List<String> stringList = getCityInfo();
        if (stringList == null || stringList.size() < 2) {
            Logger.e(context, TAG, "Failed to get city info");
            Toast.makeText(context, "Ошибка получения информации о городе", Toast.LENGTH_SHORT).show();
            return;
        }

        String city = stringList.get(1);

        List<String> userList = getUserInfo();
        if (userList == null || userList.size() < 5) {
            Logger.e(context, TAG, "Failed to get user info");
            Toast.makeText(context, "Ошибка получения информации о пользователе", Toast.LENGTH_SHORT).show();
            return;
        }

        String subject = context.getString(R.string.SA_subject) + generateRandomString(10);
        String body = context.getString(R.string.SA_message_start) + "\n\n" +
                context.getString(R.string.SA_info_pas) + "\n" +
                context.getString(R.string.SA_info_city) + " " + city + "\n" +
                context.getString(R.string.SA_pas_text) + " " + context.getString(R.string.version) + "\n" +
                context.getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
                context.getString(R.string.SA_email) + " " + userList.get(3) + "\n" +
                context.getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n\n";

        sendEmail(logFile, subject, body);
    }

    private List<String> getCityInfo() {
        try {
            return logCursor(MainActivity.CITY_INFO);
        } catch (Exception e) {
            Logger.e(context, TAG, "Error getting city info: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            return null;
        }
    }

    private List<String> getUserInfo() {
        try {
            return logCursor(MainActivity.TABLE_USER_INFO);
        } catch (Exception e) {
            Logger.e(context, TAG, "Error getting user info: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            return null;
        }
    }

    private void sendEmail(File logFile, String subject, String body) {
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
            Toast.makeText(context, "Ошибка отправки email: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

        // Просто используем context, который точно не null
        try (SQLiteDatabase db = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
             Cursor c = db.query(table, null, null, null, null, null, null)) {

            if (c.moveToFirst()) {
                do {
                    for (String cn : c.getColumnNames()) {
                        String value = c.getString(c.getColumnIndex(cn));
                        if (value != null) {
                            list.add(value);
                        }
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Error in logCursor: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        return list;
    }

    @SuppressLint("Range")
    private List<String> queryDatabase(SQLiteDatabase db, String table) {
        List<String> list = new ArrayList<>();
        Cursor c = null;

        try {
            c = db.query(table, null, null, null, null, null, null);
            if (c.moveToFirst()) {
                do {
                    for (String cn : c.getColumnNames()) {
                        String value = c.getString(c.getColumnIndex(cn));
                        if (value != null) {
                            list.add(value);
                        } else {
                            list.add("");
                        }
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Error querying database: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (c != null && !c.isClosed()) {
                c.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }

        return list;
    }

    /**
     * Альтернативный метод с использованием колбэка для асинхронной работы
     */
    public void sendLogAsync(OnLogDataReadyListener listener) {
        new Thread(() -> {
            try {
                File logFile = new File(context.getExternalFilesDir(null), "app_log.txt");
                if (!logFile.exists()) {
                    listener.onError("Log file does not exist");
                    return;
                }

                List<String> stringList = logCursor(MainActivity.CITY_INFO);
                if (stringList.isEmpty()) {
                    listener.onError("Failed to get city info");
                    return;
                }

                String city = stringList.size() > 1 ? stringList.get(1) : "";

                List<String> userList = logCursor(MainActivity.TABLE_USER_INFO);
                if (userList.isEmpty()) {
                    listener.onError("Failed to get user info");
                    return;
                }

                String subject = context.getString(R.string.SA_subject) + generateRandomString(10);
                String body = buildEmailBody(city, userList);

                listener.onSuccess(logFile, subject, body);

            } catch (Exception e) {
                listener.onError(e.getMessage());
            }
        }).start();
    }

    private String buildEmailBody(String city, List<String> userList) {
        return context.getString(R.string.SA_message_start) + "\n\n" +
                context.getString(R.string.SA_info_pas) + "\n" +
                context.getString(R.string.SA_info_city) + " " + city + "\n" +
                context.getString(R.string.SA_pas_text) + " " + context.getString(R.string.version) + "\n" +
                context.getString(R.string.SA_user_text) + " " + (userList.size() > 4 ? userList.get(4) : "") + "\n" +
                context.getString(R.string.SA_email) + " " + (userList.size() > 3 ? userList.get(3) : "") + "\n" +
                context.getString(R.string.SA_phone_text) + " " + (userList.size() > 2 ? userList.get(2) : "") + "\n\n";
    }

    public interface OnLogDataReadyListener {
        void onSuccess(File logFile, String subject, String body);
        void onError(String error);
    }
}