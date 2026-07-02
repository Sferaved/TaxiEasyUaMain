package com.taxi.easy.ua.utils.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class AddressSearchDisplayHelperTest {

    @Test
    public void toDisplayLabel_stripsMarkers() {
        assertEquals("пл. Сквер", AddressSearchDisplayHelper.toDisplayLabel("пл. Сквер\f"));
        assertEquals("вул. Тест 12", AddressSearchDisplayHelper.toDisplayLabel("вул. Тест 12\t"));
    }

    @Test
    public void hasHouseNumberDigit_detectsDigits() {
        assertTrue(AddressSearchDisplayHelper.hasHouseNumberDigit("вул. Тест 12\t"));
        assertFalse(AddressSearchDisplayHelper.hasHouseNumberDigit("пл. Сквер\f"));
        assertFalse(AddressSearchDisplayHelper.hasHouseNumberDigit(null));
    }

    @Test
    public void shouldOfferApplyWithoutHouse_whenNoDigits() {
        assertTrue(AddressSearchDisplayHelper.shouldOfferApplyWithoutHouse(
                Arrays.asList("пл. Сквер\f", "пл. Інша\f")));
        assertFalse(AddressSearchDisplayHelper.shouldOfferApplyWithoutHouse(
                Arrays.asList("вул. Тест 5\t", "вул. Тест 6\t")));
        assertFalse(AddressSearchDisplayHelper.shouldOfferApplyWithoutHouse(Collections.emptyList()));
    }
}
