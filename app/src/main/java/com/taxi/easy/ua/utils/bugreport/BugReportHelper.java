package com.taxi.easy.ua.utils.bugreport;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.helpers.TelegramUtils;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BugReportHelper {

    private static final String TAG = "BugReportHelper";
    private final Context context;
    private final MainActivity mainActivity;
    private AlertDialog currentDialog;

    private String deviceInfo;
    private String appInfo;
    private String userInfo;
    private String systemInfo;
    private String settingsInfo;
    private String currentReport = null;
    public BugReportHelper(MainActivity activity) {
        this.context = activity;
        this.mainActivity = activity;
    }

    public void showBugReportManager() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bug_report_manager, null);


        Button btnSendTelegramWithLogs = view.findViewById(R.id.btnSendTelegramWithLogs);
        Button btnClearLogs = view.findViewById(R.id.btnClearLogs);

        ProgressBar progressBar = view.findViewById(R.id.progressBar);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .setNegativeButton(context.getString(R.string.cancel), (d, which) -> d.dismiss())
                .create();
        dialog.show();
        currentDialog = dialog;

        // Генерация отчета в фоне
        progressBar.setVisibility(View.VISIBLE);
        new Thread(() -> {
            currentReport = generateFullReport();
            mainActivity.runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(context, context.getString(R.string.report_generated), Toast.LENGTH_SHORT).show();
            });
        }).start();




        btnSendTelegramWithLogs.setOnClickListener(v -> {
            if (currentReport == null) {
                Toast.makeText(context, context.getString(R.string.generate_report_first), Toast.LENGTH_SHORT).show();
                return;
            }
            sendToTelegramWithLogs(currentReport);
            dialog.dismiss();
        });

        btnClearLogs.setOnClickListener(v -> clearLogs());

    }

    private String generateFullReport() {
        collectAllData();

        StringBuilder report = new StringBuilder();
        report.append("🐞 ").append(context.getString(R.string.bug_report_header)).append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
        report.append(context.getString(R.string.report_date)).append(": ").append(getCurrentTimestamp()).append("\n\n");

        report.append("📱 ").append(context.getString(R.string.device_info_header)).append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append(deviceInfo).append("\n");

        report.append("🚕 ").append(context.getString(R.string.app_info_header)).append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append(appInfo).append("\n");

        report.append("👤 ").append(context.getString(R.string.user_info_header)).append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append(userInfo).append("\n");

        report.append("⚙️ ").append(context.getString(R.string.settings_info_header)).append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append(settingsInfo).append("\n");

        report.append("🔧 ").append(context.getString(R.string.system_info_header)).append("\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append(systemInfo).append("\n");

        return report.toString();
    }

    private void collectAllData() {
        collectDeviceInfo();
        collectAppInfo();
        collectUserInfo();
        collectSettingsInfo();
        collectSystemInfo();
    }

    private void collectDeviceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("  • ").append(context.getString(R.string.device_model)).append(": ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        info.append("  • ").append(context.getString(R.string.android_version)).append(": ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        info.append("  • ").append(context.getString(R.string.processor)).append(": ").append(Build.HARDWARE).append("\n");
        info.append("  • ").append(context.getString(R.string.screen_resolution)).append(": ").append(getScreenResolution()).append("\n");
        info.append("  • ").append(context.getString(R.string.ram)).append(": ").append(getTotalRAM()).append(" MB\n");
        info.append("  • ").append(context.getString(R.string.free_storage)).append(": ").append(getFreeStorage()).append(" MB\n");
        info.append("  • ").append(context.getString(R.string.android_id)).append(": ").append(getAndroidId()).append("\n");
        info.append("  • ").append(context.getString(R.string.device_brand)).append(": ").append(Build.BRAND).append("\n");
        deviceInfo = info.toString();
    }

    private void collectAppInfo() {
        StringBuilder info = new StringBuilder();
        try {
            String versionName = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            int versionCode = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionCode;
            info.append("  • ").append(context.getString(R.string.app_version)).append(": ").append(versionName).append(" (").append(versionCode).append(")\n");
        } catch (Exception e) {
            info.append("  • ").append(context.getString(R.string.app_version)).append(": ").append(context.getString(R.string.unknown)).append("\n");
        }
        info.append("  • ").append(context.getString(R.string.current_city)).append(": ").append(getCurrentCity()).append("\n");
        info.append("  • ").append(context.getString(R.string.payment_type)).append(": ").append(getPaymentType()).append("\n");
        info.append("  • ").append(context.getString(R.string.network_status)).append(": ").append(isNetworkAvailable() ? "✅ " + context.getString(R.string.connected) : "❌ " + context.getString(R.string.no_network)).append("\n");
        appInfo = info.toString();
    }

    /**
     * Сбор информации о пользователе
     */
    private void collectUserInfo() {
        StringBuilder info = new StringBuilder();

        List<String> userList = mainActivity.logCursor(MainActivity.TABLE_USER_INFO);

        if (userList.size() >= 6) {
            // Показываем email полностью, без маскирования
            String email = userList.get(3);
            if (email == null || email.isEmpty() || email.equals("email")) {
                info.append("  • Email: ").append(context.getString(R.string.not_specified)).append("\n");
            } else {
                info.append("  • Email: ").append(email).append("\n");
            }

            // Показываем имя пользователя
            String userName = userList.get(4);
            if (userName == null || userName.isEmpty() || userName.equals("username")) {
                info.append("  • ").append(context.getString(R.string.user_name)).append(": ").append(context.getString(R.string.not_specified)).append("\n");
            } else {
                info.append("  • ").append(context.getString(R.string.user_name)).append(": ").append(userName).append("\n");
            }

            // Показываем телефон полностью, без маскирования
            String phone = userList.get(2);
            if (phone == null || phone.isEmpty() || phone.equals("+38")) {
                info.append("  • ").append(context.getString(R.string.phone)).append(": ").append(context.getString(R.string.not_specified)).append("\n");
            } else {
                info.append("  • ").append(context.getString(R.string.phone)).append(": ").append(phone).append("\n");
            }

            // Показываем бонусы
            String bonus = userList.get(5);
            info.append("  • ").append(context.getString(R.string.bonus)).append(": ").append(bonus).append(" ").append(context.getString(R.string.currency_uah)).append("\n");
        } else {
            info.append("  • ").append(context.getString(R.string.user_data_not_available)).append("\n");
        }

        userInfo = info.toString();
        Logger.d(context, TAG, "User info собрана");
    }

    private void collectSettingsInfo() {
        StringBuilder info = new StringBuilder();
        List<String> settingsList = mainActivity.logCursor(MainActivity.TABLE_SETTINGS_INFO);
        if (settingsList.size() >= 5) {
            info.append("  • ").append(context.getString(R.string.car_type)).append(": ").append(settingsList.get(1)).append("\n");
            info.append("  • ").append(context.getString(R.string.tariff)).append(": ").append(settingsList.get(2)).append("\n");
            info.append("  • ").append(context.getString(R.string.discount)).append(": ").append(settingsList.get(3)).append("%\n");
            info.append("  • ").append(context.getString(R.string.payment_type)).append(": ").append(settingsList.get(4)).append("\n");
            info.append("  • ").append(context.getString(R.string.additional_cost)).append(": ").append(settingsList.get(5)).append(" ").append(context.getString(R.string.currency_uah)).append("\n");
        }
        settingsInfo = info.toString();
    }

    private void collectSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("  • 📍 ").append(context.getString(R.string.gps_permission)).append(": ").append(checkLocationPermission() ? "✅" : "❌").append("\n");
        info.append("  • 🔔 ").append(context.getString(R.string.notification_permission)).append(": ").append(checkNotificationPermission() ? "✅" : "❌").append("\n");
        info.append("  • 🔋 ").append(context.getString(R.string.battery)).append(": ").append(getBatteryLevel()).append("%\n");
        info.append("  • ⚡ ").append(context.getString(R.string.charging)).append(": ").append(isCharging() ? context.getString(R.string.yes) : context.getString(R.string.no)).append("\n");
        info.append("  • 📄 ").append(context.getString(R.string.logs)).append(": ").append(getLogFileSize()).append("\n");
        systemInfo = info.toString();
    }

    private void sendToTelegramWithLogs(String report) {
        showDescriptionDialog(report);
    }

    private void showDescriptionDialog(String baseReport) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bug_description, null);

        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etSteps = view.findViewById(R.id.etSteps);
        EditText etExpected = view.findViewById(R.id.etExpected);
        EditText etActual = view.findViewById(R.id.etActual);
        TextView tvCharCounter = view.findViewById(R.id.tvCharCounter);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        Button btnSend = view.findViewById(R.id.btnSend);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        etDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                tvCharCounter.setText(length + "/500");
                tvCharCounter.setTextColor(length > 500 ?
                        ContextCompat.getColor(context, android.R.color.holo_red_dark) :
                        ContextCompat.getColor(context, android.R.color.darker_gray));
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.problem_description_title))
                .setView(view)
                .setCancelable(false)
                .create();
        dialog.show();

        btnSend.setOnClickListener(v -> {
            String description = etDescription.getText().toString().trim();
            String steps = etSteps.getText().toString().trim();
            String expected = etExpected.getText().toString().trim();
            String actual = etActual.getText().toString().trim();

            if (description.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.enter_problem_description), Toast.LENGTH_SHORT).show();
                return;
            }
            if (description.length() > 500) {
                Toast.makeText(context, context.getString(R.string.description_too_long), Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            progressBar.setVisibility(View.VISIBLE);
            btnSend.setEnabled(false);

            // Формируем полный отчет для лог-файла
            StringBuilder fullReport = new StringBuilder();
            fullReport.append(baseReport);
            fullReport.append("\n📝 ").append(context.getString(R.string.problem_description_header)).append("\n");
            fullReport.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            fullReport.append("🔴 ").append(context.getString(R.string.problem_label)).append(": ").append(description).append("\n\n");
            if (!steps.isEmpty()) fullReport.append("📋 ").append(context.getString(R.string.steps_label)).append(":\n").append(steps).append("\n\n");
            if (!expected.isEmpty()) fullReport.append("✅ ").append(context.getString(R.string.expected_label)).append(":\n").append(expected).append("\n\n");
            if (!actual.isEmpty()) fullReport.append("❌ ").append(context.getString(R.string.actual_label)).append(":\n").append(actual).append("\n\n");

            // Формируем краткое сообщение для Telegram
            String shortMessage = "🐞 " + context.getString(R.string.bug_report_header) + "\n" +
                    "━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                    "📝 " + context.getString(R.string.problem_label) + ": " + description + "\n\n" +
                    "📱 " + context.getString(R.string.device_info_header) + ": " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                    "📅 " + context.getString(R.string.report_date) + ": " + getCurrentTimestamp() + "\n\n" +
                    "📄 " + context.getString(R.string.logs) + ": " + getLogFileSize();

            new Thread(() -> {
                try {
                    // Добавляем полный отчет в лог-файл
                    appendToLogFile(fullReport.toString());

                    // Отправляем краткое сообщение + лог-файл
                    File logFile = new File(context.getExternalFilesDir(null), "app_log.txt");
                    if (logFile.exists() && logFile.length() > 0) {
                        TelegramUtils.sendErrorToTelegram(shortMessage, logFile.getAbsolutePath());
                        mainActivity.runOnUiThread(() ->
                                Toast.makeText(context, context.getString(R.string.sent_to_telegram_with_logs, logFile.length() / 1024), Toast.LENGTH_LONG).show());
                    } else {
                        TelegramUtils.sendErrorToTelegram(shortMessage, null);
                        mainActivity.runOnUiThread(() ->
                                Toast.makeText(context, context.getString(R.string.sent_to_telegram), Toast.LENGTH_SHORT).show());
                    }
                    dialog.dismiss();
                } catch (Exception e) {
                    Logger.e(context, TAG, "Error: " + e.getMessage());
                    mainActivity.runOnUiThread(() ->
                            Toast.makeText(context, context.getString(R.string.error_sending, e.getMessage()), Toast.LENGTH_SHORT).show());
                } finally {
                    mainActivity.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSend.setEnabled(true);
                    });
                }
            }).start();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

    }

    private void appendToLogFile(String content) {
        try {
            File logFile = new File(context.getExternalFilesDir(null), "app_log.txt");
            FileOutputStream fos = new FileOutputStream(logFile, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write("\n\n========== БАГ-РЕПОРТ ==========\n");
            osw.write(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()) + "\n");
            osw.write(content);
            osw.write("\n========== КОНЕЦ РЕПОРТА ==========\n\n");
            osw.close();
            Logger.d(context, TAG, "Report appended to log file");
        } catch (IOException e) {
            Logger.e(context, TAG, "Error appending to log: " + e.getMessage());
        }
    }




    private void clearLogs() {
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.clear_logs_title))
                .setMessage(context.getString(R.string.clear_logs_message))
                .setPositiveButton(context.getString(R.string.clear), (dialog, which) -> {
                    File logFile = new File(context.getExternalFilesDir(null), "app_log.txt");
                    if (logFile.exists()) logFile.delete();
                    Toast.makeText(context, context.getString(R.string.logs_cleared), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(context.getString(R.string.cancel), null)
                .show();
    }



    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private String getScreenResolution() {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels + "x" + metrics.heightPixels;
    }

    private String getTotalRAM() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/meminfo"));
            String line = reader.readLine();
            reader.close();
            if (line != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) return String.valueOf(Integer.parseInt(parts[1]) / 1024);
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Error getting RAM: " + e.getMessage());
        }
        return context.getString(R.string.unknown);
    }

    private String getFreeStorage() {
        try {
            android.os.StatFs statFs = new android.os.StatFs(Environment.getDataDirectory().getPath());
            long freeBytes = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
            return String.valueOf(freeBytes / (1024 * 1024));
        } catch (Exception e) {
            return context.getString(R.string.unknown);
        }
    }

    private String getAndroidId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private String getCurrentCity() {
        List<String> cityList = mainActivity.logCursor(MainActivity.CITY_INFO);
        if (cityList.size() >= 2) return getCityDisplayName(cityList.get(1));
        return context.getString(R.string.unknown);
    }

    private String getCityDisplayName(String cityCode) {
        switch (cityCode) {
            case "Kyiv City": return context.getString(R.string.city_kyiv);
            case "Dnipropetrovsk Oblast": return context.getString(R.string.city_dnipro);
            case "Odessa": return context.getString(R.string.city_odessa);
            case "Zaporizhzhia": return context.getString(R.string.city_zaporizhzhia);
            case "Cherkasy Oblast": return context.getString(R.string.city_cherkassy);
            case "Lviv": return context.getString(R.string.city_lviv);
            case "Ivano_frankivsk": return context.getString(R.string.city_ivano_frankivsk);
            case "Vinnytsia": return context.getString(R.string.city_vinnytsia);
            case "Poltava": return context.getString(R.string.city_poltava);
            case "Sumy": return context.getString(R.string.city_sumy);
            case "Kharkiv": return context.getString(R.string.city_kharkiv);
            case "Chernihiv": return context.getString(R.string.city_chernihiv);
            case "Rivne": return context.getString(R.string.city_rivne);
            case "Ternopil": return context.getString(R.string.city_ternopil);
            case "Khmelnytskyi": return context.getString(R.string.city_khmelnytskyi);
            case "Zakarpattya": return context.getString(R.string.city_zakarpattya);
            case "Zhytomyr": return context.getString(R.string.city_zhytomyr);
            case "Kropyvnytskyi": return context.getString(R.string.city_kropyvnytskyi);
            case "Mykolaiv": return context.getString(R.string.city_mykolaiv);
            case "Chernivtsi": return context.getString(R.string.city_chernivtsi);
            case "Lutsk": return context.getString(R.string.city_lutsk);
            default: return cityCode;
        }
    }

    private String getPaymentType() {
        SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
        String payment = (String) prefs.getValue("payment_type", "nal_payment");
        switch (payment) {
            case "nal_payment": return context.getString(R.string.cash_payment);
            case "card_payment": return context.getString(R.string.card_payment);
            default: return payment;
        }
    }

    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private String getBatteryLevel() {
        android.os.BatteryManager batteryManager = (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return String.valueOf(batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY));
    }

    private boolean isCharging() {
        android.os.BatteryManager batteryManager = (android.os.BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager.isCharging();
    }

    private String getLogFileSize() {
        File logFile = new File(context.getExternalFilesDir(null), "app_log.txt");
        if (logFile.exists()) return (logFile.length() / 1024) + " KB";
        return context.getString(R.string.no_logs);
    }

}