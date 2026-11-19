package com.taxi.easy.ua.utils.payment;

/**
 * Шторка неудачной оплаты: один показ на нехватку денег.
 * Повтор на экране отмены — только если пользователь её ещё не видел.
 */
public final class PaymentErrorSheetRepeatPolicy {

    private PaymentErrorSheetRepeatPolicy() {
    }

    public static boolean allowOnCancelScreen(boolean alreadyShownForThisDecline) {
        return !alreadyShownForThisDecline;
    }
}
