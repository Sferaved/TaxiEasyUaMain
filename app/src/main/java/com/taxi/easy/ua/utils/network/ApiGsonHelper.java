package com.taxi.easy.ua.utils.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Gson для Retrofit: API иногда присылает {@code ""} в числовых полях.
 */
public final class ApiGsonHelper {

    private static final TypeAdapter<Integer> INT_ADAPTER = new TypeAdapter<Integer>() {
        @Override
        public void write(JsonWriter out, Integer value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value);
            }
        }

        @Override
        public Integer read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return 0;
            }
            if (token == JsonToken.STRING) {
                String value = in.nextString();
                if (value == null || value.trim().isEmpty()) {
                    return 0;
                }
                try {
                    return (int) Double.parseDouble(value.trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
            if (token == JsonToken.NUMBER) {
                return (int) in.nextDouble();
            }
            if (token == JsonToken.BOOLEAN) {
                return in.nextBoolean() ? 1 : 0;
            }
            in.skipValue();
            return 0;
        }
    };

    private static final TypeAdapter<Double> DOUBLE_ADAPTER = new TypeAdapter<Double>() {
        @Override
        public void write(JsonWriter out, Double value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value);
            }
        }

        @Override
        public Double read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return 0.0;
            }
            if (token == JsonToken.STRING) {
                String value = in.nextString();
                if (value == null || value.trim().isEmpty()) {
                    return 0.0;
                }
                try {
                    return Double.parseDouble(value.trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
            if (token == JsonToken.NUMBER) {
                return in.nextDouble();
            }
            if (token == JsonToken.BOOLEAN) {
                return in.nextBoolean() ? 1.0 : 0.0;
            }
            in.skipValue();
            return 0.0;
        }
    };

    private static final TypeAdapter<Boolean> BOOLEAN_ADAPTER = new TypeAdapter<Boolean>() {
        @Override
        public void write(JsonWriter out, Boolean value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value);
            }
        }

        @Override
        public Boolean read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.NULL) {
                in.nextNull();
                return false;
            }
            if (token == JsonToken.STRING) {
                String value = in.nextString();
                if (value == null || value.trim().isEmpty()) {
                    return false;
                }
                return "1".equals(value.trim()) || Boolean.parseBoolean(value.trim());
            }
            if (token == JsonToken.BOOLEAN) {
                return in.nextBoolean();
            }
            if (token == JsonToken.NUMBER) {
                return in.nextDouble() != 0.0;
            }
            in.skipValue();
            return false;
        }
    };

    private ApiGsonHelper() {
    }

    public static Gson create() {
        return new GsonBuilder()
                .setLenient()
                .registerTypeAdapter(int.class, INT_ADAPTER)
                .registerTypeAdapter(Integer.class, INT_ADAPTER)
                .registerTypeAdapter(double.class, DOUBLE_ADAPTER)
                .registerTypeAdapter(Double.class, DOUBLE_ADAPTER)
                .registerTypeAdapter(boolean.class, BOOLEAN_ADAPTER)
                .registerTypeAdapter(Boolean.class, BOOLEAN_ADAPTER)
                .create();
    }
}
