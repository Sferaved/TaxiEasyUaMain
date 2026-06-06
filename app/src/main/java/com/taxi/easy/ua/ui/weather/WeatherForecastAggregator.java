package com.taxi.easy.ua.ui.weather;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class WeatherForecastAggregator {

    private static final int MAX_DAYS = 5;

    private WeatherForecastAggregator() {
    }

    static List<WeatherDailyForecast> aggregate(List<WeatherResponse.ForecastItem> items) {
        List<WeatherDailyForecast> result = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return result;
        }

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Map<String, List<WeatherResponse.ForecastItem>> byDay = new LinkedHashMap<>();

        for (WeatherResponse.ForecastItem item : items) {
            if (item == null || item.getDtTxt() == null || item.getDtTxt().length() < 10) {
                continue;
            }
            String dayKey = item.getDtTxt().substring(0, 10);
            List<WeatherResponse.ForecastItem> dayItems = byDay.get(dayKey);
            if (dayItems == null) {
                dayItems = new ArrayList<>();
                byDay.put(dayKey, dayItems);
            }
            dayItems.add(item);
        }

        for (Map.Entry<String, List<WeatherResponse.ForecastItem>> entry : byDay.entrySet()) {
            if (result.size() >= MAX_DAYS) {
                break;
            }
            List<WeatherResponse.ForecastItem> dayItems = entry.getValue();
            WeatherDailyForecast daily = buildDaily(entry.getKey(), dayItems, inputFormat);
            if (daily != null) {
                result.add(daily);
            }
        }

        return result;
    }

    private static WeatherDailyForecast buildDaily(String dayKey,
                                                   List<WeatherResponse.ForecastItem> dayItems,
                                                   SimpleDateFormat inputFormat) {
        if (dayItems.isEmpty()) {
            return null;
        }

        double minTemp = Double.MAX_VALUE;
        double maxTemp = -Double.MAX_VALUE;
        double maxPop = 0;
        WeatherResponse.ForecastItem representative = dayItems.get(0);
        long bestMiddayDistance = Long.MAX_VALUE;

        for (WeatherResponse.ForecastItem item : dayItems) {
            if (item.getMain() != null) {
                minTemp = Math.min(minTemp, item.getMain().getTemp());
                maxTemp = Math.max(maxTemp, item.getMain().getTemp());
            }
            maxPop = Math.max(maxPop, item.getPop());

            try {
                Calendar cal = Calendar.getInstance(Locale.US);
                cal.setTime(inputFormat.parse(item.getDtTxt()));
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                long distance = Math.abs(hour - 12);
                if (distance < bestMiddayDistance) {
                    bestMiddayDistance = distance;
                    representative = item;
                }
            } catch (ParseException ignored) {
                // keep current representative
            }
        }

        if (minTemp == Double.MAX_VALUE) {
            minTemp = 0;
        }
        if (maxTemp == -Double.MAX_VALUE) {
            maxTemp = 0;
        }

        String iconCode = "01d";
        String description = "";
        int humidity = 0;
        if (representative.getWeather() != null && !representative.getWeather().isEmpty()) {
            iconCode = representative.getWeather().get(0).getIcon();
            description = representative.getWeather().get(0).getDescription();
        }
        if (representative.getMain() != null) {
            humidity = representative.getMain().getHumidity();
        }

        return new WeatherDailyForecast(
                representative,
                dayKey,
                (int) Math.round(minTemp),
                (int) Math.round(maxTemp),
                (int) Math.round(maxPop * 100),
                iconCode,
                description,
                humidity
        );
    }
}
