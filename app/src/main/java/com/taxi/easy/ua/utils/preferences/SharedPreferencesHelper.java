package com.taxi.easy.ua.utils.preferences;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {

    private static final String PREFS_NAME = "my_prefs";
    private static final String TAG = "SharedPreferencesHelper";

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveValueAsync(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply(); // Асинхронное сохранение
    }
    // Метод для сохранения значения
    public void saveValue(String key, Object value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        switch (value == null ? "null" : value.getClass().getSimpleName()) {
            case "String":
                editor.putString(key, (String) value);
                break;
            case "Integer":
                editor.putInt(key, (Integer) value);
                break;
            case "Boolean":
                editor.putBoolean(key, (Boolean) value);
                break;
            case "Float":
                editor.putFloat(key, (Float) value);
                break;
            case "Long":
                editor.putLong(key, (Long) value);
                break;
            case "null":
            default:
                throw new IllegalArgumentException("Unsupported value type");
        }


        editor.apply();
    }

    // Метод для получения значения
    public Object getValue(String key, Object defaultValue) {
        String typeName = (defaultValue == null) ? "null" : defaultValue.getClass().getSimpleName();
        switch (typeName) {
            case "String":
                return sharedPreferences.getString(key, (String) defaultValue);
            case "Integer":
                return sharedPreferences.getInt(key, (Integer) defaultValue);
            case "Boolean":
                return sharedPreferences.getBoolean(key, (Boolean) defaultValue);
            case "Float":
                return sharedPreferences.getFloat(key, (Float) defaultValue);
            case "Long":
                return sharedPreferences.getLong(key, (Long) defaultValue);
            case "null":
            default:
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

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

}

