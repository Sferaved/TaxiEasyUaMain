package com.taxi.easy.ua.utils.orders;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Время заказа для UI: UTC с сервера → часовой пояс Киева.
 * Строка dd.MM.yyyy HH:mm:ss с API возвращается без изменений.
 */
public final class OrderCreatedAtDisplayHelper {

    private static final ZoneId KYIV = resolveKyivZone();
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter SQL_UTC =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private OrderCreatedAtDisplayHelper() {
    }

    private static ZoneId resolveKyivZone() {
        try {
            return ZoneId.of("Europe/Kyiv");
        } catch (Exception ignored) {
            try {
                return ZoneId.of("Europe/Kiev");
            } catch (Exception ignored2) {
                return ZoneId.of("UTC+02:00");
            }
        }
    }

    @Nullable
    public static String formatForDisplay(@Nullable String createdAt) {
        if (createdAt == null || createdAt.isEmpty() || "*".equals(createdAt)) {
            return createdAt;
        }
        String trimmed = createdAt.trim();
        if (trimmed.matches("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2}")) {
            return trimmed;
        }
        try {
            if (trimmed.contains("T")) {
                String iso = trimmed.endsWith("Z") ? trimmed : trimmed + "Z";
                return DISPLAY.withZone(KYIV).format(Instant.parse(iso));
            }
            LocalDateTime utc = LocalDateTime.parse(trimmed, SQL_UTC);
            return utc.atZone(ZoneId.of("UTC"))
                    .withZoneSameInstant(KYIV)
                    .format(DISPLAY);
        } catch (DateTimeParseException e) {
            return trimmed;
        }
    }
}
