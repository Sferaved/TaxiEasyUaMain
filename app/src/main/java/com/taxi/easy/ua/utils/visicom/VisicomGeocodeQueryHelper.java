package com.taxi.easy.ua.utils.visicom;

import androidx.annotation.Nullable;

import com.taxi.easy.ua.utils.city.CityLastAddressHelper;
import com.taxi.easy.ua.utils.location.AddressSearchDisplayHelper;

import java.util.Locale;

/** Builds Visicom geocode.json query: categories, near/radius, limit. */
public final class VisicomGeocodeQueryHelper {

    static final int RESULT_LIMIT = 15;
    private static final int RADIUS_GPS_M = 20_000;
    private static final int RADIUS_CITY_M = 25_000;
    private static final int RADIUS_KYIV_M = 50_000;

    private static final String[] POI_KEYWORDS = {
            "кафе", "кав'ярня", "кавярня", "cafe", "coffee",
            "ресто", "ресторан", "restaurant",
            "бар", "паб", "pub",
            "магазин", "супермаркет", "маркет", "shop",
            "готель", "hotel", "мотель",
            "кінотеатр", "кинотеатр", "cinema",
            "аптека", "pharmacy",
            "банк", "bank",
            "пошта", "post",
            "стадіон", "стадион", "stadium",
    };

    private VisicomGeocodeQueryHelper() {
    }

    /**
     * Short queries like «кафе» / «ресто» — POI search only (no streets with «Республіканська»).
     */
    public static boolean isPoiIntentQuery(@Nullable String text) {
        if (text == null) {
            return false;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 3) {
            return false;
        }
        if (normalized.indexOf(AddressSearchDisplayHelper.STREET_MARKER) >= 0) {
            return false;
        }
        if (looksLikeAddress(normalized)) {
            return false;
        }
        for (String keyword : POI_KEYWORDS) {
            if (normalized.equals(keyword) || normalized.startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeAddress(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                return true;
            }
        }
        return text.startsWith("вул")
                || text.startsWith("ул")
                || text.startsWith("пр")
                || text.startsWith("пров")
                || text.startsWith("бул")
                || text.startsWith("пл")
                || text.contains("вулиця")
                || text.contains("улица");
    }

    /**
     * Query string without API key: categories=…&text=…&l=…[&near=…&radius=…]
     */
    public static String buildQueryParams(
            String searchText,
            boolean streetWithHouse,
            @Nullable String cityCode,
            double gpsLat,
            double gpsLon,
            double routeLat,
            double routeLon) {
        boolean poiIntent = !streetWithHouse && isPoiIntentQuery(searchText);
        String categories = streetWithHouse
                ? VisicomGeocodeCategoriesHelper.categoriesForStreetWithHouse()
                : (poiIntent
                ? VisicomGeocodeCategoriesHelper.categoriesForPoiSearch()
                : VisicomGeocodeCategoriesHelper.categoriesForFreeTextSearch());

        StringBuilder query = new StringBuilder(categories);
        query.append("&text=").append(searchText);
        query.append("&l=").append(RESULT_LIMIT);

        Near near = resolveNear(cityCode, gpsLat, gpsLon, routeLat, routeLon);
        if (near != null) {
            query.append("&near=")
                    .append(formatCoord(near.lon))
                    .append(',')
                    .append(formatCoord(near.lat));
            query.append("&radius=").append(near.radiusMeters);
        }
        return query.toString();
    }

    @Nullable
    static Near resolveNear(
            @Nullable String cityCode,
            double gpsLat,
            double gpsLon,
            double routeLat,
            double routeLon) {
        if (cityCode == null || cityCode.isEmpty()) {
            return null;
        }
        int radius = radiusForCity(cityCode);

        if (isValidCoord(gpsLat, gpsLon)
                && CityLastAddressHelper.isNearSelectedCity(cityCode, gpsLat, gpsLon)) {
            return new Near(gpsLat, gpsLon, Math.min(radius, RADIUS_GPS_M));
        }
        if (isValidCoord(routeLat, routeLon)
                && CityLastAddressHelper.isNearSelectedCity(cityCode, routeLat, routeLon)) {
            return new Near(routeLat, routeLon, radius);
        }
        double[] center = CityLastAddressHelper.getCityCenter(cityCode);
        if (center != null) {
            return new Near(center[0], center[1], radius);
        }
        return null;
    }

    private static int radiusForCity(String cityCode) {
        if ("Kyiv City".equals(cityCode)) {
            return RADIUS_KYIV_M;
        }
        return RADIUS_CITY_M;
    }

    private static boolean isValidCoord(double lat, double lon) {
        return lat != 0.0 || lon != 0.0;
    }

    private static String formatCoord(double value) {
        return String.format(Locale.US, "%.6f", value);
    }

    static final class Near {
        final double lat;
        final double lon;
        final int radiusMeters;

        Near(double lat, double lon, int radiusMeters) {
            this.lat = lat;
            this.lon = lon;
            this.radiusMeters = radiusMeters;
        }
    }
}
