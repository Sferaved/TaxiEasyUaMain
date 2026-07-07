package com.taxi.easy.ua.utils.visicom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VisicomPoiCityMatchHelperTest {

    @Test
    public void matches_cityNameInAddress() {
        assertTrue(VisicomPoiCityMatchHelper.matches(
                "Нова пошта, поштомат №47444. Київ, Дубищанська вул., 4",
                "Київ",
                null));
    }

    @Test
    public void matches_kyivRussianLocaleAgainstUkrainianAddress() {
        assertTrue(VisicomPoiCityMatchHelper.matches(
                "Нова пошта, поштомат №47444. Київ, Дубищанська вул., 4",
                "Киев",
                null));
    }

    @Test
    public void matches_kyivRegionSettlementInAddress() {
        assertTrue(VisicomPoiCityMatchHelper.matches(
                "Поштомат, Бровари, вул. Київська 1",
                "Київ",
                new String[]{"Бровари"}));
    }

    @Test
    public void matches_foreignCountryMode() {
        assertTrue(VisicomPoiCityMatchHelper.matches("Any address", "FC", null));
    }

    @Test
    public void matches_rejectsOtherCity() {
        assertFalse(VisicomPoiCityMatchHelper.matches(
                "Нова пошта, Одеса, Дерибасівська 1",
                "Київ",
                null));
    }
}

