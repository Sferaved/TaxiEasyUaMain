package com.taxi.easy.ua.ui.weather;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.widget.TextView;

import com.taxi.easy.ua.R;

public class WeatherGradientHelper {

    public static void applyWeatherGradient(Context context, View container, WeatherResponse weather) {
        if (container == null || weather == null) {
            return;
        }

        boolean isDay = isDaytimeAtLocation(weather);
        int[] gradientColors = getGradientColorsForWeather(weather, isDay);
        float cornerRadius = 20f * context.getResources().getDisplayMetrics().density;
        container.setBackground(buildHeroBackground(gradientColors, cornerRadius, isDay));
        applyHeroTypography(container, gradientColors, isDay);
    }

    /**
     * День/ночь по восходу и закату города (Unix UTC из OpenWeather).
     */
    static boolean isDaytimeAtLocation(WeatherResponse weather) {
        if (weather.getSys() != null) {
            long sunrise = weather.getSys().getSunrise();
            long sunset = weather.getSys().getSunset();
            if (sunrise > 0 && sunset > sunrise) {
                long now = weather.getDt() > 0
                        ? weather.getDt()
                        : System.currentTimeMillis() / 1000L;
                return now >= sunrise && now < sunset;
            }
        }

        if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
            String icon = weather.getWeather().get(0).getIcon();
            return icon != null && icon.endsWith("d");
        }

        return true;
    }

    private static Drawable buildHeroBackground(int[] gradientColors, float cornerRadius, boolean isDay) {
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                gradientColors
        );
        gradient.setCornerRadius(cornerRadius);
        gradient.setStroke((int) (1.5f * (cornerRadius / 20f)), Color.parseColor("#66FFFFFF"));

        double avgLuminance = averageLuminance(gradientColors);
        if (isDay && avgLuminance > 0.28) {
            int scrimAlpha = (int) Math.min(130, 55 + (avgLuminance - 0.28) * 180);
            GradientDrawable scrim = new GradientDrawable();
            scrim.setShape(GradientDrawable.RECTANGLE);
            scrim.setColor(Color.argb(scrimAlpha, 0, 0, 0));
            scrim.setCornerRadius(cornerRadius);
            return new LayerDrawable(new Drawable[]{gradient, scrim});
        }

        return gradient;
    }

    private static void applyHeroTypography(View container, int[] gradientColors, boolean isDay) {
        double avgLuminance = averageLuminance(gradientColors);
        if (isDay && avgLuminance > 0.28) {
            avgLuminance *= 0.7;
        }

        boolean useLightText = !isDay || avgLuminance < 0.55;
        int primary = useLightText ? Color.WHITE : Color.parseColor("#111111");
        int secondary = useLightText ? Color.parseColor("#F2FFFFFF") : Color.parseColor("#CC111111");

        setTextColor(container, R.id.tv_city_name, primary);
        setTextColor(container, R.id.tv_date, secondary);
        setTextColor(container, R.id.tv_temperature, primary);
        setTextColor(container, R.id.tv_weather_description, primary);
        setTextColor(container, R.id.tv_feels_like, secondary);
        setTextColor(container, R.id.tv_temp_range, primary);
        setTextColor(container, R.id.tv_sunrise, secondary);
        setTextColor(container, R.id.tv_sunset, secondary);
        setTextColor(container, R.id.tv_humidity, primary);
        setTextColor(container, R.id.tv_wind, primary);
        setTextColor(container, R.id.tv_pressure, primary);

        applyShadow(container, R.id.tv_city_name, useLightText);
        applyShadow(container, R.id.tv_temperature, useLightText);
        applyShadow(container, R.id.tv_weather_description, useLightText);
    }

    private static void setTextColor(View root, int viewId, int color) {
        View view = root.findViewById(viewId);
        if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        }
    }

    private static void applyShadow(View root, int viewId, boolean lightText) {
        View view = root.findViewById(viewId);
        if (!(view instanceof TextView)) {
            return;
        }
        TextView textView = (TextView) view;
        if (lightText) {
            textView.setShadowLayer(4f, 0f, 1f, Color.parseColor("#99000000"));
        } else {
            textView.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        }
    }

    private static double averageLuminance(int[] colors) {
        double sum = 0;
        for (int color : colors) {
            sum += relativeLuminance(color);
        }
        return sum / colors.length;
    }

    private static double relativeLuminance(int color) {
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;
        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static String getConditionCode(String iconCode) {
        if (iconCode == null || iconCode.length() < 2) {
            return "";
        }
        return iconCode.substring(0, 2);
    }

    private static int[] getGradientColorsForWeather(WeatherResponse weather, boolean isDay) {
        if (weather.getWeather() == null || weather.getWeather().isEmpty()) {
            return isDay ? getClearDayGradient() : getClearNightGradient();
        }

        switch (getConditionCode(weather.getWeather().get(0).getIcon())) {
            case "01":
                return isDay ? getClearDayGradient() : getClearNightGradient();

            case "02":
            case "03":
            case "04":
                return isDay ? getFewCloudsDayGradient() : getFewCloudsNightGradient();

            case "09":
            case "10":
                return isDay ? getRainDayGradient() : getRainNightGradient();

            case "11":
                return isDay ? getThunderstormDayGradient() : getThunderstormNightGradient();

            case "13":
                return isDay ? getSnowDayGradient() : getSnowNightGradient();

            case "50":
                return isDay ? getFogDayGradient() : getFogNightGradient();

            default:
                return isDay ? getClearDayGradient() : getClearNightGradient();
        }
    }

    private static int[] getClearDayGradient() {
        return new int[]{
                Color.parseColor("#FFD200"),
                Color.parseColor("#3A3A3A")
        };
    }

    private static int[] getClearNightGradient() {
        return new int[]{
                Color.parseColor("#1B1B3A"),
                Color.parseColor("#0A0A14")
        };
    }

    private static int[] getFewCloudsDayGradient() {
        return new int[]{
                Color.parseColor("#7EB4DC"),
                Color.parseColor("#4A7396")
        };
    }

    private static int[] getFewCloudsNightGradient() {
        return new int[]{
                Color.parseColor("#243444"),
                Color.parseColor("#0E141C")
        };
    }

    private static int[] getRainDayGradient() {
        return new int[]{
                Color.parseColor("#6289A8"),
                Color.parseColor("#3A556C")
        };
    }

    private static int[] getRainNightGradient() {
        return new int[]{
                Color.parseColor("#2A3848"),
                Color.parseColor("#101820")
        };
    }

    private static int[] getThunderstormDayGradient() {
        return new int[]{
                Color.parseColor("#5A4A78"),
                Color.parseColor("#3A2E52")
        };
    }

    private static int[] getThunderstormNightGradient() {
        return new int[]{
                Color.parseColor("#2C003E"),
                Color.parseColor("#0A0812")
        };
    }

    private static int[] getSnowDayGradient() {
        return new int[]{
                Color.parseColor("#8EC4E8"),
                Color.parseColor("#5A88A8")
        };
    }

    private static int[] getSnowNightGradient() {
        return new int[]{
                Color.parseColor("#2E4558"),
                Color.parseColor("#121C28")
        };
    }

    private static int[] getFogDayGradient() {
        return new int[]{
                Color.parseColor("#8A9AAA"),
                Color.parseColor("#5C6D7E")
        };
    }

    private static int[] getFogNightGradient() {
        return new int[]{
                Color.parseColor("#3A4652"),
                Color.parseColor("#161E26")
        };
    }
}
