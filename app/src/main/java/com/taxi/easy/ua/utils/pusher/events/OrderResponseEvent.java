package com.taxi.easy.ua.utils.pusher.events;

import com.taxi.easy.ua.ui.finish.OrderResponse;

public class OrderResponseEvent {
    private final OrderResponse orderResponse;

    public OrderResponseEvent(OrderResponse orderResponse) {
        this.orderResponse = orderResponse;
    }

    public OrderResponse getOrderResponse() {
        return orderResponse;
    }
}