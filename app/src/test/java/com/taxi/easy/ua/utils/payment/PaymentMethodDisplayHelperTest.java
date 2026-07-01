package com.taxi.easy.ua.utils.payment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PaymentMethodDisplayHelperTest {

    @Test
    public void hasCarInfo_emptyOrPlaceholder_returnsFalse() {
        assertFalse(PaymentMethodDisplayHelper.hasCarInfo(null));
        assertFalse(PaymentMethodDisplayHelper.hasCarInfo(""));
        assertFalse(PaymentMethodDisplayHelper.hasCarInfo("??"));
        assertFalse(PaymentMethodDisplayHelper.hasCarInfo("   "));
    }

    @Test
    public void hasCarInfo_withPlate_returnsTrue() {
        assertTrue(PaymentMethodDisplayHelper.hasCarInfo("AA1234BB"));
    }
}
