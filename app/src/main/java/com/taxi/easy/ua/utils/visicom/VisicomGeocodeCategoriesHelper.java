package com.taxi.easy.ua.utils.visicom;

/** Visicom geocode.json category lists for address search. */
public final class VisicomGeocodeCategoriesHelper {

    private static final String POI_CATEGORIES =
            "poi_railway_station"
                    + ",poi_bus_station"
                    + ",poi_airport_terminal"
                    + ",poi_airport"
                    + ",poi_shopping_centre"
                    + ",poi_night_club"
                    + ",poi_hotel_and_motel"
                    + ",poi_cafe_bar"
                    + ",poi_restaurant"
                    + ",poi_entertaining_complex"
                    + ",poi_supermarket"
                    + ",poi_grocery"
                    + ",poi_swimming_pool"
                    + ",poi_sports_complexe"
                    + ",poi_post_office"
                    + ",poi_express_mail"
                    + ",poi_underground_railway_station"
                    + ",poi_hospital";

    private VisicomGeocodeCategoriesHelper() {
    }

    /** Free-text search: streets, settlements and POI (no house numbers in Visicom). */
    public static String categoriesForFreeTextSearch() {
        return "categories="
                + POI_CATEGORIES
                + ",adm_settlement"
                + ",adr_street";
    }

    /**
     * After user picked a street (\f) and types a house number: houses may be missing in Visicom,
     * but POI at that address (Nova Poshta postomat, post office) must still be searchable.
     */
    public static String categoriesForStreetWithHouse() {
        return "categories=adr_address," + POI_CATEGORIES;
    }
}

