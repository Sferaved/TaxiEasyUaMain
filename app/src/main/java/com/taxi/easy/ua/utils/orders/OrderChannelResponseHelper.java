package com.taxi.easy.ua.utils.orders;

import java.util.Map;

/**
 * Проверка готовности карты заказа для {@code sendURLChannel}:
 * снимок маршрута из {@code markSubmitStarted} не содержит UID.
 */
public final class OrderChannelResponseHelper {

    private OrderChannelResponseHelper() {
    }

    public static boolean hasDispatchingOrderUid(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }
        String uid = map.get("dispatching_order_uid");
        if (uid == null) {
            return false;
        }
        uid = uid.trim();
        return !uid.isEmpty() && !"null".equalsIgnoreCase(uid);
    }
}
