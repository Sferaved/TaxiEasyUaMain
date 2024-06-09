package com.taxi.easy.ua.utils.tariff;

import com.google.gson.annotations.SerializedName;

public class OrderCostDetails {
    @SerializedName("dispatching_order_uid")
    private String dispatchingOrderUid;
    @SerializedName("order_cost")
    private String orderCost;

    @SerializedName("add_cost")
    private String addCost;
    @SerializedName("recommended_add_cost")
    private String recommendedAddCost;

    @SerializedName("currency")
    private String currency;
    @SerializedName("discount_trip")
    private String discountTrip;
    @SerializedName("can_pay_bonuses")
    private String canPayBonuses;

    @SerializedName("can_pay_cashless")
    private String canPayCashless;

    public OrderCostDetails(
            String dispatchingOrderUid,
            String orderCost,
            String addCost,
            String recommendedAddCost,
            String currency,
            String discountTrip,
            String canPayBonuses,
            String canPayCashless) {
    }

    public String getDispatchingOrderUid() {
        return dispatchingOrderUid;
    }

    public String getOrderCost() {
        return orderCost;
    }

    public String getAddCost() {
        return addCost;
    }

    public String getRecommendedAddCost() {
        return recommendedAddCost;
    }

    public String getCurrency() {
        return currency;
    }

    public String isDiscountTrip() {
        return discountTrip;
    }

    public String isCanPayBonuses() {
        return canPayBonuses;
    }

    public String isCanPayCashless() {
        return canPayCashless;
    }

    @Override
    public String toString() {
        return "OrderCostDetails{" +
                "dispatchingOrderUid='" + dispatchingOrderUid + '\'' +
                ", orderCost='" + orderCost + '\'' +
                ", addCost='" + addCost + '\'' +
                ", recommendedAddCost='" + recommendedAddCost + '\'' +
                ", currency='" + currency + '\'' +
                ", discountTrip='" + discountTrip + '\'' +
                ", canPayBonuses='" + canPayBonuses + '\'' +
                ", canPayCashless='" + canPayCashless + '\'' +
                '}';
    }
}

