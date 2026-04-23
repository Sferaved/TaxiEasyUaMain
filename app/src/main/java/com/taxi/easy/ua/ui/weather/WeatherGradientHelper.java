package com.taxi.easy.ua.ui.weather;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import com.taxi.easy.ua.ui.weather.WeatherResponse;

public class WeatherGradientHelper {

    /**
     * Применяет градиент к контейнеру виджета в зависимости от погоды
     */
    public static void applyWeatherGradient(Context context, View container, WeatherResponse weather) {
        if (container == null || weather == null) return;

        int[] gradientColors = getGradientColorsForWeather(weather);
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, // направление: из левого верхнего в правый нижний
                gradientColors
        );

        // Скругление углов (24dp)
        float cornerRadius = 24 * context.getResources().getDisplayMetrics().density;
        gradientDrawable.setCornerRadius(cornerRadius);

        // Добавляем тонкую обводку для стеклянного эффекта
        gradientDrawable.setStroke(2, Color.parseColor("#33FFFFFF"));

        // Применяем фон
        container.setBackground(gradientDrawable);
    }

    /**
     * Возвращает массив цветов градиента в зависимости от погоды
     */
    private static int[] getGradientColorsForWeather(WeatherResponse weather) {
        if (weather.getWeather() == null || weather.getWeather().isEmpty()) {
            return getDefaultGradient();
        }

        String iconCode = weather.getWeather().get(0).getIcon();
        long currentHour = System.currentTimeMillis() % (24 * 60 * 60 * 1000) / (60 * 60 * 1000);
        boolean isDay = iconCode.endsWith("d") && (currentHour >= 6 && currentHour < 18);

        // Определяем градиент по коду погоды
        switch (iconCode) {
            case "01d": // Ясно день
                return isDay ? getClearDayGradient() : getClearNightGradient();
            case "01n": // Ясно ночь
                return getClearNightGradient();

            case "02d": // Малооблачно день
            case "03d": // Облачно
            case "04d":
                return isDay ? getFewCloudsDayGradient() : getFewCloudsNightGradient();
            case "02n":
            case "03n":
            case "04n":
                return getFewCloudsNightGradient();

            case "09d": // Дождь
            case "09n":
            case "10d":
            case "10n":
                return getRainGradient();

            case "11d": // Гроза
            case "11n":
                return getThunderstormGradient();

            case "13d": // Снег
            case "13n":
                return getSnowGradient();

            case "50d": // Туман
            case "50n":
                return getFogGradient();

            default:
                return getDefaultGradient();
        }
    }

    // Градиенты для разных погодных условий

    private static int[] getClearDayGradient() {
        // Солнечный день: от голубого к золотистому
        return new int[]{
                Color.parseColor("#4A90E2"),  // Голубой
                Color.parseColor("#FFB347")   // Золотистый
        };
    }

    private static int[] getClearNightGradient() {
        // Ясная ночь: от тёмно-синего к фиолетовому
        return new int[]{
                Color.parseColor("#1B1B3A"),  // Тёмно-синий
                Color.parseColor("#4A2F6D")   // Фиолетовый
        };
    }

    private static int[] getFewCloudsDayGradient() {
        // Облачный день: от серо-голубого к светлому
        return new int[]{
                Color.parseColor("#6B8DB5"),  // Серо-голубой
                Color.parseColor("#A8C4D6")   // Светло-голубой
        };
    }

    private static int[] getFewCloudsNightGradient() {
        // Облачная ночь: от тёмно-серого к синему
        return new int[]{
                Color.parseColor("#2C3E50"),  // Тёмно-серый
                Color.parseColor("#1A252F")   // Почти чёрный
        };
    }

    private static int[] getRainGradient() {
        // Дождливая погода: от тёмно-синего к серому
        return new int[]{
                Color.parseColor("#34495E"),  // Тёмно-синий
                Color.parseColor("#5D6D7E")   // Серо-синий
        };
    }

    private static int[] getThunderstormGradient() {
        // Гроза: от тёмно-фиолетового к угольно-чёрному
        return new int[]{
                Color.parseColor("#2C003E"),  // Тёмно-фиолетовый
                Color.parseColor("#1A1A2E")   // Почти чёрный
        };
    }

    private static int[] getSnowGradient() {
        // Снег: от ледяного голубого к белому
        return new int[]{
                Color.parseColor("#87CEEB"),  // Небесно-голубой
                Color.parseColor("#E0F0FF")   // Светло-голубой
        };
    }

    private static int[] getFogGradient() {
        // Туман: от серого к серебристому
        return new int[]{
                Color.parseColor("#8E9EAB"),  // Серый
                Color.parseColor("#B0C4DE")   // Светло-серый
        };
    }

    private static int[] getDefaultGradient() {
        // Дефолтный градиент (как в вашем оригинальном shape)
        return new int[]{
                Color.parseColor("#CC000000"),  // Тёмный полупрозрачный
                Color.parseColor("#88000000")   // Чуть светлее
        };
    }
}