package com.taxi.easy.ua.utils.db;

import com.google.gson.annotations.SerializedName;

public class RouteInfoCancel {

    public RouteInfoCancel(String dispatchingOrderUid, String orderCost, String routeFrom, String routeFromNumber, String routeTo, String toNumber, String dispatchingOrderUidDouble, String pay_method, String required_time) {
        this.dispatchingOrderUid = dispatchingOrderUid;
        this.orderCost = orderCost;
        this.routeFrom = routeFrom;
        this.routeFromNumber = routeFromNumber;
        this.routeTo = routeTo;
        this.toNumber = toNumber;
        this.dispatchingOrderUidDouble = dispatchingOrderUidDouble;
        this.Pay_method = pay_method;
        this.required_time = required_time;
    }
    @SerializedName("dispatching_order_uid")
    private String dispatchingOrderUid;

    @SerializedName("order_cost")
    private String orderCost;

    @SerializedName("routefrom")
    private String routeFrom;

    @SerializedName("routefromnumber")
    private String routeFromNumber;

    @SerializedName("routeto")
    private String routeTo;

    @SerializedName("to_number")
    private String toNumber;

    @SerializedName("dispatching_order_uid_Double")
    private String dispatchingOrderUidDouble;

    @SerializedName("pay_method")
    private String Pay_method;

    @SerializedName("required_time")
    private String required_time;

    // геттеры и сеттеры


    public String getDispatchingOrderUid() {
        return dispatchingOrderUid;
    }

    public String getOrderCost() {
        return orderCost;
    }

    public String getRouteFrom() {
        return routeFrom;
    }

    public String getRouteFromNumber() {
        return routeFromNumber;
    }

    public String getRouteTo() {
        return routeTo;
    }

    public String getToNumber() {
        return toNumber;
    }

    public String getDispatchingOrderUidDouble() {
        return dispatchingOrderUidDouble;
    }

    public String getToPay_method() {
        return Pay_method;
    }

    public String getRequired_time() {
        return required_time;
    }
}