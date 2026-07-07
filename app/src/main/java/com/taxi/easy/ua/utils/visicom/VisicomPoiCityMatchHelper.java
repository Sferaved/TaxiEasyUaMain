package com.taxi.easy.ua.utils.visicom;

import androidx.annotation.Nullable;

import java.util.Locale;

/** City filter for Visicom POI results (address field vs selected PAS city). */
public final class VisicomPoiCityMatchHelper {

    private static final String[] KYIV_CITY_SEARCH = {"київ", "киев", "kyiv", "kiev"};
    private static final String[] KYIV_IN_ADDRESS = {"київ", "киев", "kyiv", "kiev", "м. київ", "м. киев"};

    private VisicomPoiCityMatchHelper() {
    }

    public static boolean matches(
            @Nullable String poiAddress,
            @Nullable String citySearch,
            @Nullable String[] kyivRegionSettlements) {
        if (poiAddress == null || citySearch == null) {
            return false;
        }
        if ("FC".equals(citySearch)) {
            return true;
        }
        String address = poiAddress.toLowerCase(Locale.ROOT);
        String city = citySearch.toLowerCase(Locale.ROOT);
        if (!city.isEmpty() && address.contains(city)) {
            return true;
        }
        if (isKyivCitySearch(city)) {
            for (String variant : KYIV_IN_ADDRESS) {
                if (address.contains(variant)) {
                    return true;
                }
            }
            if (kyivRegionSettlements != null) {
                for (String settlement : kyivRegionSettlements) {
                    if (settlement != null && !settlement.isEmpty()
                            && address.contains(settlement.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isKyivCitySearch(String cityLower) {
        for (String variant : KYIV_CITY_SEARCH) {
            if (cityLower.equals(variant) || cityLower.contains(variant)) {
                return true;
            }
        }
        return false;
    }
}

