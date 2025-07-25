package com.taxi.easy.ua.service;

import java.util.List;

public class AutoOrderResponse {
    private String status;
    private String message;
    private int total_orders;
    private List<OrderItem> orders;

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getTotal_orders() {
        return total_orders;
    }

    public void setTotal_orders(int total_orders) {
        this.total_orders = total_orders;
    }

    public List<OrderItem> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderItem> orders) {
        this.orders = orders;
    }

    // Вложенный класс для элемента orders
    public static class OrderItem {
        private int number;
        private String dispatching_order_uid;

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getDispatching_order_uid() {
            return dispatching_order_uid;
        }

        public void setDispatching_order_uid(String dispatching_order_uid) {
            this.dispatching_order_uid = dispatching_order_uid;
        }
    }
}
