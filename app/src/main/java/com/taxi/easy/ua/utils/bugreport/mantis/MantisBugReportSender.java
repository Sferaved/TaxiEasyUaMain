package com.taxi.easy.ua.utils.bugreport.mantis;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.taxi.easy.ua.utils.helpers.TelegramUtils;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;

import java.io.File;

public final class MantisBugReportSender {

    private static final String TAG = "MantisBugReportSender";

    public static final class Result {
        public final int issueId;
        public final String issueUrl;

        public Result(int issueId, @NonNull String issueUrl) {
            this.issueId = issueId;
            this.issueUrl = issueUrl;
        }
    }

    public static final class DeliveryResult {
        public final boolean usedMantis;
        public final int issueId;
        public final long logFileSizeKb;

        public DeliveryResult(boolean usedMantis, int issueId, long logFileSizeKb) {
            this.usedMantis = usedMantis;
            this.issueId = issueId;
            this.logFileSizeKb = logFileSizeKb;
        }
    }

    private MantisBugReportSender() {
    }

    @NonNull
    public static Result send(@NonNull Context context,
                              @NonNull String appLabel,
                              @NonNull String versionName,
                              @NonNull String problemSummary,
                              @NonNull String fullDescription,
                              @Nullable File logFile) throws Exception {
        MantisConfig mantisConfig = new FirestoreHelper(context).fetchMantisConfigBlocking();
        String issueSummary = MantisIssueUrlHelper.buildIssueSummary(appLabel, versionName, problemSummary);
        File attachment = (logFile != null && logFile.exists() && logFile.length() > 0) ? logFile : null;

        int issueId = new MantisBugReportClient().createIssue(
                mantisConfig,
                issueSummary,
                fullDescription,
                attachment
        );

        String issueUrl = MantisIssueUrlHelper.buildIssueViewUrl(mantisConfig.baseUrl, issueId);
        return new Result(issueId, issueUrl);
    }

    @NonNull
    public static DeliveryResult sendWithFallback(@NonNull Context context,
                                                  @NonNull String bugReportHeader,
                                                  @NonNull String problemLabel,
                                                  @NonNull String deviceInfoHeader,
                                                  @NonNull String reportDateLabel,
                                                  @NonNull String timestamp,
                                                  @NonNull String logsLabel,
                                                  @NonNull String noLogsLabel,
                                                  @NonNull String appLabel,
                                                  @NonNull String versionName,
                                                  @NonNull String problemSummary,
                                                  @NonNull String fullDescription,
                                                  @Nullable File logFile) throws Exception {
        long logFileSizeKb = getLogFileSizeKb(logFile);

        try {
            Result result = send(context, appLabel, versionName, problemSummary, fullDescription, logFile);
            String telegramMessage = buildTelegramNotification(
                    bugReportHeader,
                    problemLabel,
                    problemSummary,
                    deviceInfoHeader,
                    reportDateLabel,
                    timestamp,
                    result.issueId,
                    result.issueUrl
            );
            TelegramUtils.sendBugReportCreatedNotification(telegramMessage);
            return new DeliveryResult(true, result.issueId, logFileSizeKb);
        } catch (Exception mantisError) {
            Log.w(TAG, "Mantis unavailable, fallback to legacy Telegram: " + mantisError.getMessage());
            String legacyMessage = buildLegacyTelegramMessage(
                    bugReportHeader,
                    problemLabel,
                    problemSummary,
                    deviceInfoHeader,
                    reportDateLabel,
                    timestamp,
                    logsLabel,
                    formatLogFileSize(logFile, noLogsLabel)
            );
            String logPath = (logFile != null && logFile.exists() && logFile.length() > 0)
                    ? logFile.getAbsolutePath()
                    : null;
            TelegramUtils.sendErrorToTelegram(legacyMessage, logPath);
            return new DeliveryResult(false, -1, logFileSizeKb);
        }
    }

    @NonNull
    public static String buildTelegramNotification(@NonNull String bugReportHeader,
                                                   @NonNull String problemLabel,
                                                   @NonNull String description,
                                                   @NonNull String deviceInfoHeader,
                                                   @NonNull String reportDateLabel,
                                                   @NonNull String timestamp,
                                                   int issueId,
                                                   @NonNull String issueUrl) {
        return "🐞 " + bugReportHeader + "\n" +
                "📝 " + problemLabel + ": " + description + "\n\n" +
                "📱 " + deviceInfoHeader + ": " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                "📅 " + reportDateLabel + ": " + timestamp + "\n\n" +
                "🔗 Mantis #" + issueId + ": " + issueUrl;
    }

    @NonNull
    public static String buildLegacyTelegramMessage(@NonNull String bugReportHeader,
                                                    @NonNull String problemLabel,
                                                    @NonNull String description,
                                                    @NonNull String deviceInfoHeader,
                                                    @NonNull String reportDateLabel,
                                                    @NonNull String timestamp,
                                                    @NonNull String logsLabel,
                                                    @NonNull String logFileSize) {
        return "🐞 " + bugReportHeader + "\n" +
                "📝 " + problemLabel + ": " + description + "\n\n" +
                "📱 " + deviceInfoHeader + ": " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                "📅 " + reportDateLabel + ": " + timestamp + "\n\n" +
                "📄 " + logsLabel + ": " + logFileSize;
    }

    @NonNull
    private static String formatLogFileSize(@Nullable File logFile, @NonNull String noLogsLabel) {
        if (logFile != null && logFile.exists() && logFile.length() > 0) {
            return (logFile.length() / 1024) + " KB";
        }
        return noLogsLabel;
    }

    private static long getLogFileSizeKb(@Nullable File logFile) {
        if (logFile != null && logFile.exists() && logFile.length() > 0) {
            return logFile.length() / 1024;
        }
        return 0;
    }
}
