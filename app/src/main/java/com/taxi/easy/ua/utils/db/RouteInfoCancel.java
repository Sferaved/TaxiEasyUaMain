package com.taxi.easy.ua.utils.db;

import com.google.gson.annotations.SerializedName;

public class RouteInfoCancel {

    public RouteInfoCancel(
            String dispatchingOrderUid,
            String orderCost,
            String routeFrom,
            String routeFromNumber,
            String routeTo,
            String toNumber,
            String dispatchingOrderUidDouble,
            String pay_method,
            String required_time,
            String flexible_tariff_name,
            String comment_info,
            String extra_charge_codes

    ) {
        this.dispatchingOrderUid = dispatchingOrderUid;
        this.orderCost = orderCost;
        this.routeFrom = routeFrom;
        this.routeFromNumber = routeFromNumber;
        this.routeTo = routeTo;
        this.toNumber = toNumber;
        this.dispatchingOrderUidDouble = dispatchingOrderUidDouble;
        this.Pay_method = pay_method;
        this.required_time = required_time;
        this.flexible_tariff_name = flexible_tariff_name;
        this.comment_info = comment_info;
        this.extra_charge_codes = extra_charge_codes;
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

    @SerializedName("flexible_tariff_name")
    private String flexible_tariff_name;

    @SerializedName("comment_info")
    private String comment_info;
    @SerializedName("extra_charge_codes")
    private String extra_charge_codes;

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

    public String getFlexible_tariff_name() {
        return flexible_tariff_name;
    }
    public String getComment_info() {
        return comment_info;
    }
    public String getExtra_charge_codes() {
        return extra_charge_codes;
    }

}