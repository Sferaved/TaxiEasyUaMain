package com.taxi.easy.ua.utils.bottom_sheet;

public final class ErrorBottomSheetDismissHelper {

    private ErrorBottomSheetDismissHelper() {
    }

    public static boolean allowsDismissOnOutsideTap(String errorMessageKey) {
        if (errorMessageKey == null || errorMessageKey.isEmpty()) {
            return false;
        }
        switch (errorMessageKey) {
            case "server_error_connected":
            case "sentNotifyMessage":
            case "order_to_cancel":
            case "order_to_cancel_true":
            case "black_list_message":
            case "black_list_message_err":
            case "server_error_card_payment":
            case "card_payment_false":
            case "ex_st_2":
            case "cost_error":
            case "error_5_min_cancel_card_order":
            case "no_cards_info":
            case "google_pay_hold_failed":
            case "google_pay_hold_server_error":
            case "google_pay_hold_network_error":
                return true;
            default:
                return false;
        }
    }

    public static boolean shouldRestoreOrderButtonsOnDismiss(String errorMessageKey) {
        return "no_cards_info".equals(errorMessageKey);
    }
}
