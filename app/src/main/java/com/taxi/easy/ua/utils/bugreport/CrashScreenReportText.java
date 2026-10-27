package com.taxi.easy.ua.utils.bugreport;

public final class CrashScreenReportText {

    static final String DEFAULT_SUMMARY = "Crash screen";

    private CrashScreenReportText() {
    }

    public static String problemSummary(String screenMessage) {
        if (screenMessage == null) {
            return DEFAULT_SUMMARY;
        }
        String text = screenMessage.trim();
        return text.isEmpty() ? DEFAULT_SUMMARY : text;
    }

    public static String fullDescription(String screenMessage, String deviceReport, String lastCrash) {
        StringBuilder body = new StringBuilder();
        body.append(problemSummary(screenMessage));
        if (deviceReport != null && !deviceReport.trim().isEmpty()) {
            body.append("\n\n").append(deviceReport.trim());
        }
        if (lastCrash != null && !lastCrash.trim().isEmpty()) {
            body.append("\n\nLast crash:\n").append(lastCrash.trim());
        }
        return body.toString();
    }
}
