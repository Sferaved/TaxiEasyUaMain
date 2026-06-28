package com.taxi.easy.ua.utils.bottom_sheet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ErrorBottomSheetDismissHelperTest {

    @Test
    public void allowsDismissOnOutsideTap_forNoCardsInfo() {
        assertTrue(ErrorBottomSheetDismissHelper.allowsDismissOnOutsideTap("no_cards_info"));
    }

    @Test
    public void allowsDismissOnOutsideTap_forBlockingErrors() {
        assertFalse(ErrorBottomSheetDismissHelper.allowsDismissOnOutsideTap("verify_address"));
        assertFalse(ErrorBottomSheetDismissHelper.allowsDismissOnOutsideTap("google_verify_mes"));
        assertFalse(ErrorBottomSheetDismissHelper.allowsDismissOnOutsideTap(null));
        assertFalse(ErrorBottomSheetDismissHelper.allowsDismissOnOutsideTap(""));
    }

    @Test
    public void shouldRestoreOrderButtonsOnDismiss_onlyForNoCardsInfo() {
        assertTrue(ErrorBottomSheetDismissHelper.shouldRestoreOrderButtonsOnDismiss("no_cards_info"));
        assertFalse(ErrorBottomSheetDismissHelper.shouldRestoreOrderButtonsOnDismiss("cost_error"));
    }
}
