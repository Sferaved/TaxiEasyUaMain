package com.taxi.easy.ua.utils.visicom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VisicomGeocodeQueryHelperTest {

    @Test
    public void isPoiIntentQuery_cafeAndResto() {
        assertTrue(VisicomGeocodeQueryHelper.isPoiIntentQuery("кафе"));
        assertTrue(VisicomGeocodeQueryHelper.isPoiIntentQuery("ресто"));
        assertTrue(VisicomGeocodeQueryHelper.isPoiIntentQuery("ресторан"));
    }

    @Test
    public void isPoiIntentQuery_streetWithNumber_notPoi() {
        assertFalse(VisicomGeocodeQueryHelper.isPoiIntentQuery("вул. Хрещатик 1"));
        assertFalse(VisicomGeocodeQueryHelper.isPoiIntentQuery("просп. Перемоги"));
    }

    @Test
    public void buildQueryParams_poiIntent_usesPoiCategoriesAndNear() {
        String params = VisicomGeocodeQueryHelper.buildQueryParams(
                "кафе",
                false,
                "Kyiv City",
                0, 0,
                0, 0);
        assertTrue(params.contains("poi_cafe_bar"));
        assertTrue(params.contains("poi_restaurant"));
        assertFalse(params.contains("adr_street"));
        assertFalse(params.contains("adm_settlement"));
        assertTrue(params.contains("near=30.524907,50.451107"));
        assertTrue(params.contains("radius=50000"));
        assertTrue(params.contains("&l=15"));
        assertTrue(params.contains("text=кафе"));
    }

    @Test
    public void buildQueryParams_addressSearch_includesStreetsAndNear() {
        String params = VisicomGeocodeQueryHelper.buildQueryParams(
                "Хрещатик",
                false,
                "Kyiv City",
                0, 0,
                0, 0);
        assertTrue(params.contains("adr_street"));
        assertTrue(params.contains("near="));
    }

    @Test
    public void resolveNear_prefersGpsNearCity() {
        VisicomGeocodeQueryHelper.Near near = VisicomGeocodeQueryHelper.resolveNear(
                "Kyiv City",
                50.512825, 30.507257,
                48.0, 25.0);
        assertNotNull(near);
        assertTrue(Math.abs(near.lat - 50.512825) < 1e-6);
        assertTrue(Math.abs(near.lon - 30.507257) < 1e-6);
        assertTrue(near.radiusMeters <= 20_000);
    }

    @Test
    public void resolveNear_unknownCity_returnsNull() {
        assertNull(VisicomGeocodeQueryHelper.resolveNear(
                "UnknownCity", 0, 0, 0, 0));
    }

    @Test
    public void resolveNear_foreignCountries_ignoresOverseasGpsAndWarsawRoute() {
        // Mantis #49: foreign countries + Warsaw stub route → empty Visicom
        assertNull(VisicomGeocodeQueryHelper.resolveNear(
                "foreign countries",
                0, 0,
                52.13472, 21.00424));
        assertTrue(VisicomGeocodeQueryHelper.isNationwideGeocodeCity("foreign countries"));
    }

    @Test
    public void resolveNear_all_ignoresOverseasGps() {
        // Mantis #50: city all + Lima GPS → empty Visicom for UA streets
        assertNull(VisicomGeocodeQueryHelper.resolveNear(
                "all",
                -12.042056, -77.058333,
                -12.042056, -77.058333));
        assertTrue(VisicomGeocodeQueryHelper.isNationwideGeocodeCity("all"));
    }

    @Test
    public void buildQueryParams_foreignCountries_omitsNear() {
        String params = VisicomGeocodeQueryHelper.buildQueryParams(
                "при",
                false,
                "foreign countries",
                52.13472, 21.00424,
                52.13472, 21.00424);
        assertFalse(params.contains("near="));
        assertFalse(params.contains("radius="));
        assertTrue(params.contains("text=при"));
    }
}
