package com.taxi.easy.ua.utils.orders;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.route.RoutePlaceMatcher;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсинг required_time с API для UI и cancel_info.
 */
public final class RequiredTimeParseHelper {

    private static final Pattern API_DATE_TIME = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}) (\\d{2}:\\d{2})");
    private static final Pattern DISPLAY_DATE_TIME =
            Pattern.compile("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}(:\\d{2})?");
    private static final DateTimeFormatter API_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter SQL_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private RequiredTimeParseHelper() {
    }

    @NonNull
    public static String formatForStorage(@Nullable String requiredTime) {
        if (requiredTime == null || requiredTime.isEmpty()) {
            return "";
        }
        if (RoutePlaceMatcher.isEpochRequiredTime(requiredTime)) {
            return "";
        }

        Matcher matcher = API_DATE_TIME.matcher(requiredTime);
        if (!matcher.find()) {
            return "";
        }

        String dateTimeString = matcher.group(1) + " " + matcher.group(2);
        return LocalDateTime.parse(dateTimeString, API_FORMAT).toString();
    }

    public static boolean isKnownRequiredTime(@Nullable String requiredTime) {
        return !formatPickupTimeForDisplay(requiredTime).isEmpty();
    }

    @NonNull
    public static String formatExpectedPickupLine(@NonNull Context context, @Nullable String requiredTime) {
        String formatted = formatPickupTimeForDisplay(requiredTime);
        if (formatted.isEmpty()) {
            return "";
        }
        return context.getString(R.string.ex_st_5) + formatted;
    }

    @NonNull
    public static String formatPickupTimeForDisplay(@Nullable String requiredTime) {
        if (requiredTime == null) {
            return "";
        }
        String trimmed = requiredTime.trim();
        if (trimmed.isEmpty() || RoutePlaceMatcher.isEpochRequiredTime(trimmed)) {
            return "";
        }
        if (DISPLAY_DATE_TIME.matcher(trimmed).matches()) {
            if (trimmed.length() == 16) {
                return trimmed + ":00";
            }
            return trimmed;
        }
        Matcher matcher = API_DATE_TIME.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1) + " " + matcher.group(2) + ":00";
        }
        try {
            if (trimmed.contains("T")) {
                LocalDateTime parsed = LocalDateTime.parse(trimmed.substring(0, Math.min(trimmed.length(), 16)), ISO_LOCAL);
                return parsed.format(DISPLAY_FORMAT);
            }
            LocalDateTime parsed = LocalDateTime.parse(trimmed, SQL_UTC);
            return parsed.format(DISPLAY_FORMAT);
        } catch (DateTimeParseException ignored) {
            return "";
        }
    }

    @NonNull
    public static String buildCancelListRouteInfo(
            @NonNull Context context,
            @NonNull String routeHead,
            @NonNull String webCost,
            @Nullable String auto,
            @NonNull String createdAt,
            @Nullable String requiredTimeRaw,
            @NonNull String closeReasonText
    ) {
        String autoText = auto != null ? auto : "??";
        String expectedLine = formatExpectedPickupLine(context, requiredTimeRaw);
        return routeHead + "#"
                + context.getString(R.string.close_resone_cost) + webCost + " " + context.getString(R.string.UAH) + "#"
                + context.getString(R.string.auto_info) + " " + autoText + "#"
                + context.getString(R.string.close_resone_time) + createdAt + "#"
                + expectedLine + "#"
                + context.getString(R.string.close_resone_text) + closeReasonText;
    }
}
