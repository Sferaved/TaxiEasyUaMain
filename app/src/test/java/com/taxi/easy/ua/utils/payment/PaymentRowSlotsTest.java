package com.taxi.easy.ua.utils.payment;

import static org.junit.Assert.assertEquals;

import android.view.View;

import org.junit.Test;

public class PaymentRowSlotsTest {

    @Test
    public void markStaysInColumnWhenInactive() {
        assertEquals(View.VISIBLE, PaymentRowSlots.markVisibility(true));
        assertEquals(View.INVISIBLE, PaymentRowSlots.markVisibility(false));
    }

    @Test
    public void deleteIconIsHiddenSoCheckmarkIsNotCrowded() {
        assertEquals(View.GONE, PaymentRowSlots.deleteVisibility(true));
        assertEquals(View.GONE, PaymentRowSlots.deleteVisibility(false));
    }
}
