package com.taxi.easy.ua.utils.time_ut;

import android.annotation.SuppressLint;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {
    public static String convertAndSubtractMinutes(String requiredTime) {
        if (requiredTime == null || requiredTime.isEmpty()) {
            return null; // Возвращаем null, если входные данные некорректны
        }

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());

        try {
            // Проверяем, соответствует ли requiredTime нужному формату
            inputFormat.setLenient(false); // Запрещаем автоматически исправлять ошибки в формате
            Date date = inputFormat.parse(requiredTime);
            if (date == null) return requiredTime; // Если парсинг не удался, возвращаем исходное значение


            // Форматируем в нужный формат
            return outputFormat.format(date);
        } catch (ParseException e) {
            return requiredTime; // Если формат неверный, возвращаем оригинальное значение
        }
    }
}
