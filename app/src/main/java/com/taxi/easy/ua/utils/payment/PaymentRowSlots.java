package com.taxi.easy.ua.utils.payment;

import android.view.View;

/**
 * The check mark keeps a fixed slot. The delete control is not shown:
 * a saved card is removed by swiping the row.
 */
public final class PaymentRowSlots {

    private PaymentRowSlots() {
    }

    public static int markVisibility(boolean active) {
        return active ? View.VISIBLE : View.INVISIBLE;
    }

    public static int deleteVisibility(boolean canDelete) {
        // Basket stays off the row; a saved card is removed by swipe.
        return View.GONE;
    }
}
