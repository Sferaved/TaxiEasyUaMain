package com.taxi.easy.ua.utils.payment;

/**
 * Returning from the background during a card order must not paint the Google Pay screen.
 * That screen is only for a Google Pay hold that is still in progress.
 */
public final class SubmitResumeUi {

    private SubmitResumeUi() {
    }

    public static boolean shouldShowGooglePayProcessing(String payMethod, boolean googlePayHoldInProgress) {
        if (googlePayHoldInProgress) {
            return true;
        }
        return PaymentTypeHelper.GOOGLE_PAY.equals(payMethod);
    }
}
