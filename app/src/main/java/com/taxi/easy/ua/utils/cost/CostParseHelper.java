package com.taxi.easy.ua.utils.cost;

import androidx.annotation.Nullable;

/**
 * Парсинг order_cost с API / Pusher (часто float: 164.34, 164.339999…).
 */
public final class CostParseHelper {

    private CostParseHelper() {
    }

    /** Округлённая стоимость в гривнах; 0 если невалидно. */
    public static long parsePositiveCost(@Nullable String cost) {
        if (cost == null) {
            return 0L;
        }
        String trimmed = cost.trim();
        if (trimmed.isEmpty() || "0".equals(trimmed)) {
            return 0L;
        }
        try {
            double value = Double.parseDouble(trimmed.replace(',', '.'));
            if (value <= 0 || Double.isNaN(value) || Double.isInfinite(value)) {
                return 0L;
            }
            return Math.round(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static boolean hasDisplayableCost(@Nullable String cost) {
        return parsePositiveCost(cost) > 0L;
    }

    /** Строка для UI и сравнений (целые гривны). */
    @Nullable
    public static String normalizeCostString(@Nullable String cost) {
        long parsed = parsePositiveCost(cost);
        return parsed > 0L ? String.valueOf(parsed) : null;
    }
}
