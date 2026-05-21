package com.taxi.easy.ua.utils.route;

import java.util.Locale;

/**
 * Сравнение адресов «откуда» и «куда», в т.ч. когда Visicom вернул uk/ru варианты одного населённого пункта.
 */
public final class RoutePlaceMatcher {

    private RoutePlaceMatcher() {
    }

    public static boolean isSameRoute(String routeFrom, String routeFromNumber,
                                    String routeTo, String routeToNumber) {
        if (routeFrom == null || routeTo == null) {
            return false;
        }
        String from = normalize(routeFrom + " " + safe(routeFromNumber));
        String to = normalize(routeTo + " " + safe(routeToNumber));
        if (from.isEmpty() || to.isEmpty()) {
            return false;
        }
        if (from.equals(to)) {
            return true;
        }
        if (from.contains(to) || to.contains(from)) {
            return true;
        }
        String fromCore = extractPlaceToken(from);
        String toCore = extractPlaceToken(to);
        return !fromCore.isEmpty() && fromCore.equals(toCore);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String raw) {
        String s = raw.toLowerCase(Locale.ROOT).trim();
        s = s.replace('ы', 'и').replace('э', 'е').replace('ё', 'е');
        s = s.replace("поселок", " ")
                .replace("посёлок", " ")
                .replace("селище", " ")
                .replace("село", " ")
                .replace("місто", " ")
                .replace("город", " ")
                .replace("м.", " ");
        return s.replaceAll("[^\\p{L}\\p{N}]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static String extractPlaceToken(String normalized) {
        if (normalized.isEmpty()) {
            return "";
        }
        String[] parts = normalized.split(" ");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].length() >= 3) {
                return parts[i];
            }
        }
        return parts[parts.length - 1];
    }
}
