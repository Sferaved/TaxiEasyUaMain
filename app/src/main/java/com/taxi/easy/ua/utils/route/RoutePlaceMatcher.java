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
        String shorter = from.length() <= to.length() ? from : to;
        String longer = from.length() <= to.length() ? to : from;
        return shorter.length() >= 12 && longer.contains(shorter);
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

}
