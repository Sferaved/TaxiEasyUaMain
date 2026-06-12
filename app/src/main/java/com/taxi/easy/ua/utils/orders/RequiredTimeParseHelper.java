package com.taxi.easy.ua.utils.orders;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсинг required_time с API для сохранения в cancel_info (формат yyyy-MM-dd'T'HH:mm).
 */
public final class RequiredTimeParseHelper {

    private static final Pattern API_DATE_TIME = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4}) (\\d{2}:\\d{2})");
    private static final DateTimeFormatter API_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private RequiredTimeParseHelper() {
    }

    @NonNull
    public static String formatForStorage(@Nullable String requiredTime) {
        if (requiredTime == null || requiredTime.isEmpty()) {
            return "";
        }
        if (requiredTime.contains("1970-01-01") || requiredTime.contains("01.01.1970")) {
            return "";
        }

        Matcher matcher = API_DATE_TIME.matcher(requiredTime);
        if (!matcher.find()) {
            return "";
        }

        String dateTimeString = matcher.group(1) + " " + matcher.group(2);
        return LocalDateTime.parse(dateTimeString, API_FORMAT).toString();
    }
}
