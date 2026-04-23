package com.taxi.easy.ua.ui.weather;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherResponse {
    private Coord coord;
    private List<Weather> weather;
    private String base;
    private Main main;
    private int visibility;
    private Wind wind;
    private Clouds clouds;
    private long dt;
    private Sys sys;
    private int timezone;
    private int id;
    private String name;
    private int cod;

    // Прогноз на 5 дней
    @SerializedName("list")
    private List<ForecastItem> forecastList;

    public static class Coord {
        private double lon;
        private double lat;

        public double getLon() { return lon; }
        public double getLat() { return lat; }

        // Сеттеры для Coord
        public void setLon(double lon) { this.lon = lon; }
        public void setLat(double lat) { this.lat = lat; }
    }

    public static class Weather {
        private int id;
        private String main;
        private String description;
        private String icon;

        public int getId() { return id; }
        public String getMain() { return main; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }

        // Сеттеры для Weather
        public void setId(int id) { this.id = id; }
        public void setMain(String main) { this.main = main; }
        public void setDescription(String description) { this.description = description; }
        public void setIcon(String icon) { this.icon = icon; }
    }

    public static class Main {
        private double temp;
        @SerializedName("feels_like")
        private double feelsLike;
        @SerializedName("temp_min")
        private double tempMin;
        @SerializedName("temp_max")
        private double tempMax;
        private int pressure;
        private int humidity;

        public double getTemp() { return temp; }
        public double getFeelsLike() { return feelsLike; }
        public double getTempMin() { return tempMin; }
        public double getTempMax() { return tempMax; }
        public int getPressure() { return pressure; }
        public int getHumidity() { return humidity; }

        // Сеттеры для Main
        public void setTemp(double temp) { this.temp = temp; }
        public void setFeelsLike(double feelsLike) { this.feelsLike = feelsLike; }
        public void setTempMin(double tempMin) { this.tempMin = tempMin; }
        public void setTempMax(double tempMax) { this.tempMax = tempMax; }
        public void setPressure(int pressure) { this.pressure = pressure; }
        public void setHumidity(int humidity) { this.humidity = humidity; }
    }

    public static class Wind {
        private double speed;
        private int deg;

        public double getSpeed() { return speed; }
        public int getDeg() { return deg; }

        // Сеттеры для Wind
        public void setSpeed(double speed) { this.speed = speed; }
        public void setDeg(int deg) { this.deg = deg; }
    }

    public static class Clouds {
        private int all;

        public int getAll() { return all; }

        // Сеттер для Clouds
        public void setAll(int all) { this.all = all; }
    }

    public static class Sys {
        private int type;
        private int id;
        private String country;
        private long sunrise;
        private long sunset;

        public String getCountry() { return country; }
        public long getSunrise() { return sunrise; }
        public long getSunset() { return sunset; }

        // Сеттеры для Sys
        public void setType(int type) { this.type = type; }
        public void setId(int id) { this.id = id; }
        public void setCountry(String country) { this.country = country; }
        public void setSunrise(long sunrise) { this.sunrise = sunrise; }
        public void setSunset(long sunset) { this.sunset = sunset; }
    }

    public static class ForecastItem {
        private long dt;
        private Main main;
        private List<Weather> weather;
        private Wind wind;
        private Clouds clouds;
        @SerializedName("dt_txt")
        private String dtTxt;

        public long getDt() { return dt; }
        public Main getMain() { return main; }
        public List<Weather> getWeather() { return weather; }
        public Wind getWind() { return wind; }
        public Clouds getClouds() { return clouds; }
        public String getDtTxt() { return dtTxt; }

        // Сеттеры для ForecastItem
        public void setDt(long dt) { this.dt = dt; }
        public void setMain(Main main) { this.main = main; }
        public void setWeather(List<Weather> weather) { this.weather = weather; }
        public void setWind(Wind wind) { this.wind = wind; }
        public void setClouds(Clouds clouds) { this.clouds = clouds; }
        public void setDtTxt(String dtTxt) { this.dtTxt = dtTxt; }
    }

    // Геттеры
    public Coord getCoord() { return coord; }
    public List<Weather> getWeather() { return weather; }
    public Main getMain() { return main; }
    public Wind getWind() { return wind; }
    public Clouds getClouds() { return clouds; }
    public long getDt() { return dt; }
    public Sys getSys() { return sys; }
    public int getTimezone() { return timezone; }
    public String getName() { return name; }
    public int getCod() { return cod; }
    public List<ForecastItem> getForecastList() { return forecastList; }

    // Сеттеры для WeatherResponse
    public void setCoord(Coord coord) { this.coord = coord; }
    public void setWeather(List<Weather> weather) { this.weather = weather; }
    public void setBase(String base) { this.base = base; }
    public void setMain(Main main) { this.main = main; }
    public void setVisibility(int visibility) { this.visibility = visibility; }
    public void setWind(Wind wind) { this.wind = wind; }
    public void setClouds(Clouds clouds) { this.clouds = clouds; }
    public void setDt(long dt) { this.dt = dt; }
    public void setSys(Sys sys) { this.sys = sys; }
    public void setTimezone(int timezone) { this.timezone = timezone; }
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCod(int cod) { this.cod = cod; }
    public void setForecastList(List<ForecastItem> forecastList) { this.forecastList = forecastList; }
}