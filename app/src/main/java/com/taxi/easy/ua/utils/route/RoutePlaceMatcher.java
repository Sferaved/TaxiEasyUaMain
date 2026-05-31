package com.taxi.easy.ua.utils.route;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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

    /** «По городу»: одинаковые адреса/координаты или явный текст, без ложного isSameRoute. */
    public static boolean isCityRideOrder(Map<String, String> map) {
        if (map == null) {
            return false;
        }
        String routeto = map.get("routeto");
        if (routeto != null && (routeto.contains("по місту")
                || routeto.contains("по городу")
                || routeto.contains("around the city"))) {
            return true;
        }
        if (Objects.equals(map.get("routefrom"), map.get("routeto"))) {
            return true;
        }
        String lat = map.get("lat");
        String lng = map.get("lng");
        String fromLat = map.get("from_lat");
        String fromLng = map.get("from_lng");
        return lat != null && lng != null && fromLat != null && fromLng != null
                && !"0".equals(lat) && lat.equals(fromLat) && lng.equals(fromLng);
    }

    public static boolean isEpochRequiredTime(String requiredTime) {
        if (requiredTime == null || requiredTime.isEmpty()) {
            return false;
        }
        return requiredTime.contains("1970") || requiredTime.contains("01.01.1970");
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
