package com.taxi.easy.ua.utils.to_json_parser;

public final class OrderChannelHttpFailure {

    private OrderChannelHttpFailure() {
    }

    /** Тост «нет интернета» не нужен, если uid заказа уже пришёл и HTTP отменён специально. */
    public static boolean shouldShowNetworkErrorToast(boolean uidAlreadyReceived, boolean callCanceled) {
        return !uidAlreadyReceived && !callCanceled;
    }
}
