package com.taxi.easy.ua.utils.city;

import com.google.gson.annotations.SerializedName;

public class CityResponse {
    @SerializedName("city")
    private String city;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
