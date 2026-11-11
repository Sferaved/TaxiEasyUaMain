package com.taxi.easy.ua.utils.payment;

import android.view.View;

/**
 * Trailing icons of a payment row stay in fixed columns.
 * A hidden mark or delete control keeps its slot so the checkmarks line up.
 */
public final class PaymentRowSlots {

    private PaymentRowSlots() {
    }

    public static int markVisibility(boolean active) {
        return active ? View.VISIBLE : View.INVISIBLE;
    }

    public static int deleteVisibility(boolean canDelete) {
        return canDelete ? View.VISIBLE : View.INVISIBLE;
    }
}
