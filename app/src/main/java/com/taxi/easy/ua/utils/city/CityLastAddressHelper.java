package com.taxi.easy.ua.utils.city;

import androidx.annotation.Nullable;

/**
 * Проверка ответа lastAddressUser: не подставлять адрес прошлой поездки из другого города.
 */
public final class CityLastAddressHelper {

    /** Макс. расстояние от центра города, км — иначе адрес считаем чужим. */
    private static final double MAX_DISTANCE_KM = 45.0;

    private CityLastAddressHelper() {
    }

    public static boolean hasNoLastTrip(@Nullable String startLat, @Nullable String routefrom) {
        if (startLat == null || startLat.trim().isEmpty()) {
            return true;
        }
        try {
            if (Double.parseDouble(startLat.trim()) == 0.0) {
                return true;
            }
        } catch (NumberFormatException e) {
            return true;
        }
        return routefrom == null || routefrom.trim().isEmpty();
    }

    /**
     * Можно ли применить lastAddress к выбранному городу (есть поездка и координаты рядом с городом).
     */
    public static boolean shouldApplyLastAddress(
            @Nullable String cityCode,
            @Nullable String startLat,
            @Nullable String startLan,
            @Nullable String routefrom) {
        if (hasNoLastTrip(startLat, routefrom)) {
            return false;
        }
        try {
            double lat = Double.parseDouble(startLat.trim());
            double lon = Double.parseDouble(startLan != null ? startLan.trim() : "0");
            return isNearSelectedCity(cityCode, lat, lon);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isNearSelectedCity(@Nullable String cityCode, double lat, double lon) {
        double[] center = centerFor(cityCode);
        if (center == null) {
            return true;
        }
        return haversineKm(lat, lon, center[0], center[1]) <= MAX_DISTANCE_KM;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    @Nullable
    private static double[] centerFor(@Nullable String city) {
        if (city == null) {
            return null;
        }
        switch (city) {
            case "Kyiv City":
                return new double[]{50.451107, 30.524907};
            case "Dnipropetrovsk Oblast":
                return new double[]{48.4647, 35.0462};
            case "Odessa":
            case "OdessaTest":
                return new double[]{46.4694, 30.7404};
            case "Zaporizhzhia":
                return new double[]{47.84015, 35.13634};
            case "Cherkasy Oblast":
                return new double[]{49.44469, 32.05728};
            case "Lviv":
                return new double[]{49.83993, 24.02973};
            case "Ivano_frankivsk":
                return new double[]{48.92005, 24.71067};
            case "Vinnytsia":
                return new double[]{49.23325, 28.46865};
            case "Poltava":
                return new double[]{49.59325, 34.54938};
            case "Sumy":
                return new double[]{50.90775, 34.79865};
            case "Kharkiv":
                return new double[]{49.99358, 36.23191};
            case "Chernihiv":
                return new double[]{51.4933, 31.2972};
            case "Rivne":
                return new double[]{50.6198, 26.2406};
            case "Ternopil":
                return new double[]{49.54479, 25.5990};
            case "Khmelnytskyi":
                return new double[]{49.41548, 27.00674};
            case "Zakarpattya":
                return new double[]{48.61913, 22.29475};
            case "Zhytomyr":
                return new double[]{50.26801, 28.68026};
            case "Kropyvnytskyi":
                return new double[]{48.51159, 32.26982};
            case "Mykolaiv":
                return new double[]{46.97498, 31.99378};
            case "Chernivtsi":
                return new double[]{48.29306, 25.93484};
            case "Lutsk":
                return new double[]{50.73968, 25.32400};
            default:
                return null;
        }
    }
}
