package com.taxi.easy.ua.ui.weather;

/**
 * Aggregated daily forecast from 3-hour OpenWeather forecast entries.
 */
public class WeatherDailyForecast {

    private final WeatherResponse.ForecastItem representativeItem;
    private final String dayKey;
    private final int tempMin;
    private final int tempMax;
    private final int maxPopPercent;
    private final String iconCode;
    private final String description;
    private final int humidity;

    public WeatherDailyForecast(WeatherResponse.ForecastItem representativeItem,
                                String dayKey,
                                int tempMin,
                                int tempMax,
                                int maxPopPercent,
                                String iconCode,
                                String description,
                                int humidity) {
        this.representativeItem = representativeItem;
        this.dayKey = dayKey;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.maxPopPercent = maxPopPercent;
        this.iconCode = iconCode;
        this.description = description;
        this.humidity = humidity;
    }

    public WeatherResponse.ForecastItem getRepresentativeItem() {
        return representativeItem;
    }

    public String getDayKey() {
        return dayKey;
    }

    public int getTempMin() {
        return tempMin;
    }

    public int getTempMax() {
        return tempMax;
    }

    public int getMaxPopPercent() {
        return maxPopPercent;
    }

    public String getIconCode() {
        return iconCode;
    }

    public String getDescription() {
        return description;
    }

    public int getHumidity() {
        return humidity;
    }
}
