package com.taxi.easy.ua.utils.log;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {

    private static final String TAG = "MyAppLogger";
    private static final String LOG_FILE_NAME = "app_log.txt";

    public static void writeLog(Context context, String log) {
        if (isExternalStorageWritable()) {
            File logFile = new File(context.getExternalFilesDir(null), LOG_FILE_NAME);
            try {
                FileOutputStream fos = new FileOutputStream(logFile, true);
                OutputStreamWriter osw = new OutputStreamWriter(fos);
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                osw.write(timestamp + " - " + log);
                osw.write("\n");
                osw.close();
                fos.close();
                Log.d(TAG, "Log written to " + logFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Failed to write log", e);
            }
        } else {
            Log.e(TAG, "External storage is not writable");
        }
    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

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
        writeLog(context, "INFO: " + tag + ": " + message);
    }

    public static String getLogcat() {
        StringBuilder log = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("logcat -d");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                log.append(line).append("\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to get logcat", e);
        }
        return log.toString();
    }
}
