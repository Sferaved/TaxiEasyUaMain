package com.taxi.easy.ua.utils.visicom;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VisicomGeocodeCategoriesHelperTest {

    @Test
    public void freeTextSearch_includesStreetsAndExpressMail() {
        String categories = VisicomGeocodeCategoriesHelper.categoriesForFreeTextSearch();
        assertTrue(categories.contains("adr_street"));
        assertTrue(categories.contains("poi_express_mail"));
        assertTrue(categories.contains("poi_post_office"));
    }

    @Test
    public void poiSearch_excludesStreets() {
        String categories = VisicomGeocodeCategoriesHelper.categoriesForPoiSearch();
        assertTrue(categories.contains("poi_cafe_bar"));
        assertTrue(categories.contains("poi_restaurant"));
        assertTrue(!categories.contains("adr_street"));
        assertTrue(!categories.contains("adm_settlement"));
    }

    @Test
    public void streetWithHouse_includesAddressesAndExpressMail() {
        String categories = VisicomGeocodeCategoriesHelper.categoriesForStreetWithHouse();
        assertTrue(categories.contains("adr_address"));
        assertTrue(categories.contains("poi_express_mail"));
        assertTrue(categories.contains("poi_post_office"));
        assertTrue(!categories.contains("adr_street"));
    }
}
