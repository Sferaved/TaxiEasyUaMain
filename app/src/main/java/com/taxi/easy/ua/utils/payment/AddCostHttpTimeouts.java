package com.taxi.easy.ua.utils.payment;

/**
 * Доплата готівкою ждёт, пока диспетчер закроет старый заказ (на сервере до 60 с),
 * и только потом создаёт новый. Короткий таймаут обрывает запрос раньше.
 */
public final class AddCostHttpTimeouts {

    public static final int CASH_SECONDS = 90;

    public static final int WALLET_SECONDS = 90;

    private AddCostHttpTimeouts() {
    }
}
