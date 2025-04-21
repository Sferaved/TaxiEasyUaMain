package com.taxi.easy.ua.ui.cities.api;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class CityLastAddressResponse {
    @SerializedName("routefrom")
    private String routefrom;

    @SerializedName("startLat")
    private String startLat;

    @SerializedName("startLan")
    private String startLan;

    public String getRoutefrom() {
        return routefrom;
    }

    public String getStartLat() {
        return startLat;
    }

    public String getStartLan() {
        return startLan;
    }

    @NonNull
    @Override
    public String toString() {
        return "CityLastAddressResponse{" +
                "routefrom='" + routefrom + '\'' +
                ", startLat='" + startLat + '\'' +
                ", startLan='" + startLan + '\'' +
                '}';
    }
}
