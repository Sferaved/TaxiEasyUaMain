package com.taxi.easy.ua.utils.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {

    private static final String PREFS_NAME = "my_prefs";

    private SharedPreferences sharedPreferences;

    public SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Метод для сохранения значения
    public void saveValue(String key, Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else {
            throw new IllegalArgumentException("Unsupported value type");
        }

        editor.apply();
    }

    // Метод для получения значения
    public Object getValue(String key, Object defaultValue) {
        if (defaultValue instanceof String) {
            return sharedPreferences.getString(key, (String) defaultValue);
        } else if (defaultValue instanceof Integer) {
            return sharedPreferences.getInt(key, (Integer) defaultValue);
        } else if (defaultValue instanceof Boolean) {
            return sharedPreferences.getBoolean(key, (Boolean) defaultValue);
        } else if (defaultValue instanceof Float) {
            return sharedPreferences.getFloat(key, (Float) defaultValue);
        } else if (defaultValue instanceof Long) {
            return sharedPreferences.getLong(key, (Long) defaultValue);
        } else {
            throw new IllegalArgumentException("Unsupported default value type");
        }
    }

    // Метод для удаления значения
    public void removeValue(String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }

    // Метод для проверки существования значения
    public boolean contains(String key) {
        return sharedPreferences.contains(key);
    }

    // Метод для очистки всех значений
    public void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
}

