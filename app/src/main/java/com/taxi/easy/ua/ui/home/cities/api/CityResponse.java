package com.taxi.easy.ua.ui.home.cities.api;

import com.google.gson.annotations.SerializedName;

public class CityResponse {
    @SerializedName("card_max_pay")
    private int cardMaxPay;

    @SerializedName("bonus_max_pay")
    private int bonusMaxPay;
    @SerializedName("black_list")
    private String black_list;

    public int getCardMaxPay() {
        return cardMaxPay;
    }

    public int getBonusMaxPay() {
        return bonusMaxPay;
    }


    public String getBlack_list() {
        return black_list;
    }
}
