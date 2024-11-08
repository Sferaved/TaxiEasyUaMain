package com.taxi.easy.ua.utils.to_json_parser;

import com.google.gson.annotations.SerializedName;

public class JsonResponse {

    @SerializedName("from_lat")
    private String fromLat;

    @SerializedName("from_lng")
    private String fromLng;

    @SerializedName("lat")
    private String lat;

    @SerializedName("lng")
    private String lng;

    @SerializedName("dispatching_order_uid")
    private String dispatchingOrderUid;

    @SerializedName("order_cost")
    private String orderCost;

    @SerializedName("currency")
    private String currency;

    @SerializedName("routefrom")
    private String routeFrom;

    @SerializedName("routefromnumber")
    private String routeFromNumber;

    @SerializedName("routeto")
    private String routeTo;

    @SerializedName("to_number")
    private String toNumber;

    @SerializedName("doubleOrder")
    private String doubleOrder;

    @SerializedName("dispatching_order_uid_Double")
    private String dispatchingOrderUidDouble;

    @SerializedName("Message")
    private String message;

    @SerializedName("required_time")
    private String required_time;

    @SerializedName("flexible_tariff_name")
    private String flexible_tariff_name;
    @SerializedName("comment_info")
    private String comment_info;
    @SerializedName("extra_charge_codes")
    private String extra_charge_codes;

    // геттеры и сеттеры

    public String getFromLat() {
        return fromLat;
    }

    public String getFromLng() {
        return fromLng;
    }

    public String getLat() {
        return lat;
    }

    public String getLng() {
        return lng;
    }

    public String getDispatchingOrderUid() {
        return dispatchingOrderUid;
    }

    public String getOrderCost() {
        return orderCost;
    }

    public String getCurrency() {
        return currency;
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

    public String getDoubleOrder() {
        return doubleOrder;
    }

    public String getDispatchingOrderUidDouble() {
        return dispatchingOrderUidDouble;
    }

    public String getMessage() {
        return message;
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

