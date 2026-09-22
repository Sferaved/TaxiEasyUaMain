package com.taxi.easy.ua.utils.permissions;

import com.google.gson.annotations.SerializedName;

public class PermissionsResponse {
    @SerializedName("card_pay")
    private String card_pay;
    @SerializedName("bonus_pay")
    private String bonus_pay;

    public String getCardPay() {
        return card_pay;
    }

    public String getBonusPay() {
        return bonus_pay;
    }
}
