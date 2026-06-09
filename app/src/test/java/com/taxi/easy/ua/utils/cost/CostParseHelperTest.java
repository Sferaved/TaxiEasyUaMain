package com.taxi.easy.ua.utils.cost;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CostParseHelperTest {

    @Test
    public void parsePositiveCost_nullOrEmpty_returnsZero() {
        assertEquals(0L, CostParseHelper.parsePositiveCost(null));
        assertEquals(0L, CostParseHelper.parsePositiveCost(""));
        assertEquals(0L, CostParseHelper.parsePositiveCost("   "));
    }

    @Test
    public void parsePositiveCost_zeroOrInvalid_returnsZero() {
        assertEquals(0L, CostParseHelper.parsePositiveCost("0"));
        assertEquals(0L, CostParseHelper.parsePositiveCost("-10"));
        assertEquals(0L, CostParseHelper.parsePositiveCost("abc"));
    }

    @Test
    public void parsePositiveCost_floatFromApi_roundsToWholeHryvnia() {
        assertEquals(164L, CostParseHelper.parsePositiveCost("164.34"));
        assertEquals(164L, CostParseHelper.parsePositiveCost("164.339999"));
        assertEquals(165L, CostParseHelper.parsePositiveCost("164.50"));
    }

    @Test
    public void parsePositiveCost_commaDecimal_supported() {
        assertEquals(165L, CostParseHelper.parsePositiveCost("164,50"));
    }

    @Test
    public void hasDisplayableCost_matchesParsedValue() {
        assertFalse(CostParseHelper.hasDisplayableCost(null));
        assertFalse(CostParseHelper.hasDisplayableCost("0"));
        assertTrue(CostParseHelper.hasDisplayableCost("150"));
    }

    @Test
    public void normalizeCostString_returnsWholeNumberOrNull() {
        assertNull(CostParseHelper.normalizeCostString(null));
        assertNull(CostParseHelper.normalizeCostString("0"));
        assertEquals("164", CostParseHelper.normalizeCostString("164.34"));
    }
}
