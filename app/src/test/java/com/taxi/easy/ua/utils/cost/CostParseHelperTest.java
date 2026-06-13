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

    @Test
    public void extractClientCostFromOrderUrl_readsCostSegment() {
        String url = "https://m.easy-order-taxi.site/orderClientCostMyApi/"
                + "50.787184/30.304899/50.787184/30.304899/ /+38 093 346 47 47/66/"
                + "noname (1.1030) *taxialfa@gmail.com*google_pay_payment/-35/no_comment/no_date/no_time/no_start/no_finish/V_123";
        assertEquals("66", CostParseHelper.extractClientCostFromOrderUrl(url));
    }

    @Test
    public void extractClientCostFromOrderUrl_invalidOrMissing_returnsNull() {
        assertNull(CostParseHelper.extractClientCostFromOrderUrl(null));
        assertNull(CostParseHelper.extractClientCostFromOrderUrl("https://example.com/other"));
        assertNull(CostParseHelper.extractClientCostFromOrderUrl(
                "https://example.com/orderClientCostMyApi/1/2/3/4/5/6"));
    }
}
