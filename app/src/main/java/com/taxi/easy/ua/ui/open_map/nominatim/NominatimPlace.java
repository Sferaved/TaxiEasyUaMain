package com.taxi.easy.ua.ui.open_map.nominatim;

import java.util.List;

public class NominatimPlace {

    private long place_id;
    private String licence;
    private String osm_type;
    private long osm_id;
    private String lat;
    private String lon;
    private String category;
    private String type;
    private int place_rank;
    private double importance;
    private String addresstype;
    private String name;
    private String display_name;
    private Address address;
    private List<String> boundingbox;

    @Override
    public String toString() {
        return "NominatimPlace{" +
                "place_id=" + place_id +
                ", licence='" + licence + '\'' +
                ", osm_type='" + osm_type + '\'' +
                ", osm_id=" + osm_id +
                ", lat='" + lat + '\'' +
                ", lon='" + lon + '\'' +
                ", category='" + category + '\'' +
                ", type='" + type + '\'' +
                ", place_rank=" + place_rank +
                ", importance=" + importance +
                ", addresstype='" + addresstype + '\'' +
                ", name='" + name + '\'' +
                ", display_name='" + display_name + '\'' +
                ", address=" + address +
                ", boundingbox=" + boundingbox +
                '}';
    }
// Геттеры и сеттеры

    public static class Address {
        private String shop;
        private String road;
        private String neighbourhood;
        private String suburb;
        private String borough;
        private String city;
        private String ISO3166_2_lvl4;
        private String postcode;
        private String country;
        private String country_code;

        @Override
        public String toString() {
            return "Address{" +
                    "shop='" + shop + '\'' +
                    ", road='" + road + '\'' +
                    ", neighbourhood='" + neighbourhood + '\'' +
                    ", suburb='" + suburb + '\'' +
                    ", borough='" + borough + '\'' +
                    ", city='" + city + '\'' +
                    ", ISO3166_2_lvl4='" + ISO3166_2_lvl4 + '\'' +
                    ", postcode='" + postcode + '\'' +
                    ", country='" + country + '\'' +
                    ", country_code='" + country_code + '\'' +
                    '}';
        }
// Геттеры и сеттеры
    }
}
