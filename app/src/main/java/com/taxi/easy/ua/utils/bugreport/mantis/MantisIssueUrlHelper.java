package com.taxi.easy.ua.utils.bugreport.mantis;

import androidx.annotation.NonNull;

public final class MantisIssueUrlHelper {

    private static final int SUMMARY_MAX_LENGTH = 128;

    private MantisIssueUrlHelper() {
    }

    @NonNull
    public static String buildIssueViewUrl(@NonNull String baseUrl, int issueId) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/view.php?id=" + issueId;
    }

    @NonNull
    public static String buildIssueSummary(@NonNull String appLabel,
                                           @NonNull String versionName,
                                           @NonNull String description) {
        String prefix = "[" + appLabel + " " + versionName + "] ";
        int maxDescriptionLength = Math.max(1, SUMMARY_MAX_LENGTH - prefix.length());
        String trimmedDescription = description.trim();
        if (trimmedDescription.length() > maxDescriptionLength) {
            trimmedDescription = trimmedDescription.substring(0, maxDescriptionLength - 3) + "...";
        }
        return prefix + trimmedDescription;
    }

    @NonNull
    public static String normalizeBaseUrl(@NonNull String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    @NonNull
    public static String buildRetrofitBaseUrl(@NonNull String baseUrl) {
        return normalizeBaseUrl(baseUrl) + "/api/rest/";
    }
}
