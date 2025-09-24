package com.taxi.easy.ua.utils.log;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {

    private static final String TAG = "MyAppLogger";
    private static final String LOG_FILE_NAME = "app_log.txt";
    private static final long MAX_LOG_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /** Получаем файл логов в приватной директории приложения */
    private static File getLogFile(Context context) {
        return new File(context.getExternalFilesDir(null), LOG_FILE_NAME);
    }

    /** Записываем строку в лог и следим за размером файла */
    public static void writeLog(Context context, String log) {
        File logFile = getLogFile(context);
        try {
            // Добавляем новую запись
            FileOutputStream fos = new FileOutputStream(logFile, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());
            osw.write(timestamp + " - " + log);
            osw.write("\n");
            osw.close();
            fos.close();

            // Проверяем размер и подрезаем при необходимости
            trimIfNeeded(logFile);

            Log.d(TAG, "Log written to " + logFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log", e);
        }
    }

    /** Эффективное подрезание файла при превышении лимита */
    private static void trimIfNeeded(File logFile) {
        if (logFile.length() <= MAX_LOG_FILE_SIZE) return;

        try (RandomAccessFile raf = new RandomAccessFile(logFile, "rw")) {
            long fileLength = raf.length();

            // Сколько байт оставить (последние MAX_LOG_FILE_SIZE / 2 байт)
            long keep = MAX_LOG_FILE_SIZE / 2;
            if (keep > fileLength) keep = fileLength;

            raf.seek(fileLength - keep);
            byte[] buffer = new byte[(int) keep];
            raf.readFully(buffer);

            raf.seek(0);
            raf.write(buffer);
            raf.setLength(keep);

            Log.w(TAG, "Log file trimmed efficiently, old entries removed");
        } catch (IOException e) {
            Log.e(TAG, "Failed to trim log file", e);
        }
    }

    // ========= Методы для разных уровней логов =========
    public static void e(Context context, String tag, String message) {
        Log.e(tag, message);
        writeLog(context, "ERROR: " + tag + ": " + message);
    }

    public static void d(Context context, String tag, String message) {
        Log.d(tag, message);
        writeLog(context, "DEBUG: " + tag + ": " + message);
    }

    public static void i(Context context, String tag, String message) {
        Log.i(tag, message);
        writeLog(context, "INFO: " + tag + ": " + message);
    }

    public static void w(Context context, String tag, String message) {
        Log.w(tag, message);
        writeLog(context, "WARN: " + tag + ": " + message);
    }
}
