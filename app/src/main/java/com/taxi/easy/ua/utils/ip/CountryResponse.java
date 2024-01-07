package com.taxi.easy.ua.utils.ip;

import com.google.gson.annotations.SerializedName;

public class CountryResponse {
    @SerializedName("response")
    private String country;

    public String getCountry() {
        return country;
    }
}
