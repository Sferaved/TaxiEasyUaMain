package com.taxi.easy.ua.ui.open_map.visicom.resp;

import com.google.gson.annotations.SerializedName;

public class GeocodeProperties {

    @SerializedName("name")
    private String name;

    @SerializedName("categories")
    private String categories;

    @SerializedName("country_code")
    private String countryCode;

    @SerializedName("country")
    private String country;

    @SerializedName("postal_code")
    private String postalCode;

    @SerializedName("street_id")
    private String streetId;

    @SerializedName("lang")
    private String lang;

    @SerializedName("street")
    private String street;

    @SerializedName("street_type")
    private String streetType;

    @SerializedName("settlement_id")
    private String settlementId;

    @SerializedName("settlement")
    private String settlement;

    @SerializedName("settlement_type")
    private String settlementType;

    @SerializedName("copyright")
    private String copyright;

    @SerializedName("relevance")
    private double relevance;

    @SerializedName("settlement_url")
    private String settlementUrl;

    @SerializedName("street_url")
    private String streetUrl;

    public String getName() {
        return name;
    }

    // Add getters for the other fields as needed
}
