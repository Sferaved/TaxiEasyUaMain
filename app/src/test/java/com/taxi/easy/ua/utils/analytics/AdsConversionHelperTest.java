package com.taxi.easy.ua.utils.analytics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AdsConversionHelperTest {

    @Test
    public void parseAnalyticsValue_parsesFloatAndComma() {
        assertEquals(164.34d, AdsConversionHelper.parseAnalyticsValue("164.34"), 0.001);
        assertEquals(120.5d, AdsConversionHelper.parseAnalyticsValue("120,5"), 0.001);
    }

    @Test
    public void parseAnalyticsValue_rejectsInvalid() {
        assertEquals(0d, AdsConversionHelper.parseAnalyticsValue(null), 0.001);
        assertEquals(0d, AdsConversionHelper.parseAnalyticsValue("0"), 0.001);
        assertEquals(0d, AdsConversionHelper.parseAnalyticsValue("abc"), 0.001);
    }

    @Test
    public void isNewFirebaseUser_falseWhenUserNull() {
        assertFalse(AdsConversionHelper.isNewFirebaseUser(null));
    }
}
