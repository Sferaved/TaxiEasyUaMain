package com.taxi.easy.ua.ui.finish;

import com.google.gson.annotations.SerializedName;

public class RouteResponse {
    @SerializedName("routefrom")
    private String routeFrom;

    @SerializedName("routefromnumber")
    private String routeFromNumber;

    @SerializedName("routeto")
    private String routeTo;

    @SerializedName("routetonumber")
    private String routeToNumber;

    @SerializedName("web_cost")
    private String webCost;

    @SerializedName("closeReason")
    private String closeReason;

    @SerializedName("created_at")
    private String createdAt;

    // Геттеры и сеттеры для полей (не обязательно, но может быть полезным)

    public String getRouteFrom() {
        return routeFrom;
    }

    public void setRouteFrom(String routeFrom) {
        this.routeFrom = routeFrom;
    }

    public String getRouteFromNumber() {
        return routeFromNumber;
    }

    public void setRouteFromNumber(String routeFromNumber) {
        this.routeFromNumber = routeFromNumber;
    }

    public String getRouteTo() {
        return routeTo;
    }

    public void setRouteTo(String routeTo) {
        this.routeTo = routeTo;
    }

    public String getRouteToNumber() {
        return routeToNumber;
    }

    public void setRouteToNumber(String routeToNumber) {
        this.routeToNumber = routeToNumber;
    }

    public String getWebCost() {
        return webCost;
    }

    public void setWebCost(String webCost) {
        this.webCost = webCost;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
