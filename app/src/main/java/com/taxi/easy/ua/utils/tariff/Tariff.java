package com.taxi.easy.ua.utils.tariff;

import com.google.gson.annotations.SerializedName;

public class Tariff {
    @SerializedName("flexible_tariff_name")
    private String flexibleTariffName;
    @SerializedName("order_cost_details")
    private OrderCostDetails orderCostDetails;

    @Override
    public String toString() {
        return "Tariff{" +
                "flexibleTariffName='" + flexibleTariffName + '\'' +
                ", orderCostDetails=" + orderCostDetails +
                '}';
    }

    public Tariff(String flexibleTariffName, OrderCostDetails orderCostDetails) {
    }

    public String getFlexibleTariffName() {
        return flexibleTariffName;
    }

    public OrderCostDetails getOrderCostDetails() {
        return orderCostDetails;
    }
}
