package com.taxi.easy.ua.ui.weather;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {
    @SerializedName("coord")
    private Coord coord;
    @SerializedName("weather")
    private List<Weather> weather;
    private String base;
    @SerializedName("main")
    private Main main;
    private int visibility;
    @SerializedName("wind")
    private Wind wind;
    @SerializedName("clouds")
    private Clouds clouds;
    private long dt;
    @SerializedName("sys")
    private Sys sys;
    private int timezone;
    private int id;
    @SerializedName("name")
    private String name;
    private int cod;

    // Прогноз на 5 дней
    @SerializedName("list")
    private List<ForecastItem> forecastList;

    public static class Coord {
        @SerializedName("lon")
        private double lon;
        @SerializedName("lat")
        private double lat;

        public double getLon() { return lon; }
        public double getLat() { return lat; }

        // Сеттеры для Coord
        public void setLon(double lon) { this.lon = lon; }
        public void setLat(double lat) { this.lat = lat; }
    }

    public static class Weather {
        private int id;
        @SerializedName("main")
        private String main;
        @SerializedName("description")
        private String description;
        @SerializedName("icon")
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
        @SerializedName("temp")
        private double temp;
        @SerializedName("feels_like")
        private double feelsLike;
        @SerializedName("temp_min")
        private double tempMin;
        @SerializedName("temp_max")
        private double tempMax;
        @SerializedName("pressure")
        private int pressure;
        @SerializedName("humidity")
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
        @SerializedName("speed")
        private double speed;
        @SerializedName("deg")
        private int deg;

        public double getSpeed() { return speed; }
        public int getDeg() { return deg; }

        // Сеттеры для Wind
        public void setSpeed(double speed) { this.speed = speed; }
        public void setDeg(int deg) { this.deg = deg; }
    }

    public static class Clouds {
        @SerializedName("all")
        private int all;

        public int getAll() { return all; }

        // Сеттер для Clouds
        public void setAll(int all) { this.all = all; }
    }

    public static class Sys {
        private int type;
        private int id;
        @SerializedName("country")
        private String country;
        @SerializedName("sunrise")
        private long sunrise;
        @SerializedName("sunset")
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
        @SerializedName("main")
        private Main main;
        @SerializedName("weather")
        private List<Weather> weather;
        @SerializedName("wind")
        private Wind wind;
        @SerializedName("clouds")
        private Clouds clouds;
        @SerializedName("dt_txt")
        private String dtTxt;
        /** Probability of precipitation, 0..1 */
        @SerializedName("pop")
        private double pop;

        public long getDt() { return dt; }
        public Main getMain() { return main; }
        public List<Weather> getWeather() { return weather; }
        public Wind getWind() { return wind; }
        public Clouds getClouds() { return clouds; }
        public String getDtTxt() { return dtTxt; }
        public double getPop() { return pop; }

        // Сеттеры для ForecastItem
        public void setDt(long dt) { this.dt = dt; }
        public void setMain(Main main) { this.main = main; }
        public void setWeather(List<Weather> weather) { this.weather = weather; }
        public void setWind(Wind wind) { this.wind = wind; }
        public void setClouds(Clouds clouds) { this.clouds = clouds; }
        public void setDtTxt(String dtTxt) { this.dtTxt = dtTxt; }
        public void setPop(double pop) { this.pop = pop; }
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