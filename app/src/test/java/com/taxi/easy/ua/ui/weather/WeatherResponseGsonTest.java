package com.taxi.easy.ua.ui.weather;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.taxi.easy.ua.utils.network.GsonResponseParser;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenWeather JSON must bind {@code main.temp}. If R8 renames fields, UI NPEs
 * on {@code weather.getMain().getTemp()}.
 */
public class WeatherResponseGsonTest {

    private static final String CURRENT_JSON =
            "{\"coord\":{\"lon\":30.52,\"lat\":50.45},"
                    + "\"weather\":[{\"id\":800,\"main\":\"Clear\","
                    + "\"description\":\"ясно\",\"icon\":\"01d\"}],"
                    + "\"main\":{\"temp\":18.4,\"feels_like\":17.1,"
                    + "\"temp_min\":16.0,\"temp_max\":20.0,"
                    + "\"pressure\":1013,\"humidity\":55},"
                    + "\"wind\":{\"speed\":3.2,\"deg\":180},"
                    + "\"clouds\":{\"all\":10},"
                    + "\"sys\":{\"country\":\"UA\",\"sunrise\":1758440000,"
                    + "\"sunset\":1758484000},"
                    + "\"timezone\":10800,\"name\":\"Kyiv\"}";

    @Test
    public void fromJson_bindsMainTempWindAndSys() {
        WeatherResponse weather = new Gson().fromJson(CURRENT_JSON, WeatherResponse.class);

        assertNotNull(weather);
        assertNotNull(weather.getMain());
        assertEquals(18.4, weather.getMain().getTemp(), 0.001);
        assertEquals(55, weather.getMain().getHumidity());
        assertNotNull(weather.getWind());
        assertEquals(3.2, weather.getWind().getSpeed(), 0.001);
        assertEquals("Kyiv", weather.getName());
        assertNotNull(weather.getSys());
        assertEquals("UA", weather.getSys().getCountry());
        assertEquals(1758440000L, weather.getSys().getSunrise());
    }

    @Test
    public void fromJson_missingMain_returnsNullMain() {
        WeatherResponse weather = new Gson().fromJson("{\"name\":\"Kyiv\"}", WeatherResponse.class);

        assertNotNull(weather);
        assertNull(weather.getMain());
        assertNull(weather.getWind());
    }

    @Test
    public void gsonResponseParser_reparsesR8MapWithNestedMain() {
        Map<String, Object> main = new LinkedHashMap<>();
        main.put("temp", 12.7);
        main.put("humidity", 40);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("main", main);
        body.put("name", "Odesa");

        WeatherResponse parsed = GsonResponseParser.as(body, WeatherResponse.class);

        assertNotNull(parsed);
        assertNotNull(parsed.getMain());
        assertEquals(12.7, parsed.getMain().getTemp(), 0.001);
        assertEquals("Odesa", parsed.getName());
    }
}
