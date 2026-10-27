package com.taxi.easy.ua.utils.bugreport;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.BuildConfig;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.bugreport.mantis.MantisBugReportSender;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CrashScreenBugReportSender {

    private static final String TAG = "CrashScreenBugReport";

    private CrashScreenBugReportSender() {
    }

    @NonNull
    public static MantisBugReportSender.DeliveryResult send(@NonNull Context context) throws Exception {
        Context appContext = context.getApplicationContext();
        String screenMessage = appContext.getString(R.string.anr_mes);
        String deviceReport = collectDeviceReport(appContext);
        String lastCrash = readLastCrash(appContext);
        String summary = CrashScreenReportText.problemSummary(screenMessage);
        String description = CrashScreenReportText.fullDescription(screenMessage, deviceReport, lastCrash);

        File logFile = new File(appContext.getExternalFilesDir(null), "app_log.txt");
        appendReport(appContext, logFile, description);

        String timestamp = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        return MantisBugReportSender.sendWithFallback(
                appContext,
                appContext.getString(R.string.bug_report_header),
                appContext.getString(R.string.problem_label),
                appContext.getString(R.string.device_info_header),
                appContext.getString(R.string.report_date),
                timestamp,
                appContext.getString(R.string.logs),
                appContext.getString(R.string.no_logs),
                appContext.getString(R.string.bug_report_header),
                BuildConfig.VERSION_NAME,
                summary,
                description,
                logFile
        );
    }

    @NonNull
    private static String collectDeviceReport(@NonNull Context context) {
        return "Device: " + Build.MANUFACTURER + " " + Build.MODEL
                + "\nAndroid: " + Build.VERSION.RELEASE
                + "\nApp: " + BuildConfig.VERSION_NAME;
    }

    @NonNull
    private static String readLastCrash(@NonNull Context context) {
        try {
            Object value = new SharedPreferencesHelper(context).getValue("last_crash", "");
            return value == null ? "" : value.toString();
        } catch (Exception e) {
            Logger.e(context, TAG, "Last crash skipped: " + e.getMessage());
            return "";
        }
    }

    private static void appendReport(@NonNull Context context, @NonNull File logFile, @NonNull String description) {
        try {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return;
            }
            FileOutputStream fos = new FileOutputStream(logFile, true);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write("\n\n========== БАГ-РЕПОРТ ==========\n");
            writer.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            writer.write("\n");
            writer.write(description);
            writer.write("\n========== КОНЕЦ РЕПОРТА ==========\n\n");
            writer.close();
        } catch (Exception e) {
            Logger.e(context, TAG, "Report append skipped: " + e.getMessage());
        }
    }
}
