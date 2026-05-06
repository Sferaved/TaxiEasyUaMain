package com.taxi.easy.ua.utils.time_ut;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private final ExecutionStatusViewModel viewModel;
    private static final String TAG = "TimeUtils";
    private static Handler handler;
    private static Runnable runnable;
    private String required_time;
    private static final long INTERVAL = 30000; // 30 секунд в миллисекундах

    public TimeUtils(String required_time, ExecutionStatusViewModel viewModel) {
        this.required_time = required_time;
        this.viewModel = viewModel;
    }

    public static String convertAndSubtractMinutes(String requiredTime) {
        if (requiredTime == null || requiredTime.isEmpty()) {
            return null;
        }

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());

        try {
            inputFormat.setLenient(false);
            Date date = inputFormat.parse(requiredTime);
            if (date == null) return requiredTime;
            return outputFormat.format(date);
        } catch (ParseException e) {
            return requiredTime;
        }
    }

    public void startTimer() {
        // Не запускаем таймер, если required_time - пустая дата
        if (required_time == null || required_time.isEmpty() ||
                required_time.contains("1970") || required_time.startsWith("01.01.1970")) {
            Log.d(TAG, "Timer not started - invalid date: " + required_time);
            return;
        }

        handler = new Handler(Looper.getMainLooper());

        runnable = new Runnable() {
            @Override
            public void run() {
                isTenMinutesRemainingFunction();
                if (handler != null) {
                    handler.postDelayed(this, INTERVAL);
                }
            }
        };

        handler.post(runnable);
    }

    // Метод для остановки таймера
    public void stopTimer() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
            handler = null;
            runnable = null;
        }
    }

    // Проверка, является ли дата пустой (1970 год)
    private boolean isDefaultDate(String dateString) {
        if (dateString == null) return true;
        return dateString.contains("1970") ||
                dateString.startsWith("01.01.1970") ||
                dateString.startsWith("1970-01-01");
    }

    // Парсинг даты с поддержкой нескольких форматов
    private Date parseDate(String dateString) {
        String[] dateFormats = {
                "yyyy-MM-dd'T'HH:mm",
                "dd.MM.yyyy HH:mm",
                "yyyy-MM-dd HH:mm:ss",
                "dd.MM.yyyy"
        };

        for (String format : dateFormats) {
            try {
                @SuppressLint("SimpleDateFormat")
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                sdf.setLenient(false);
                Date date = sdf.parse(dateString);
                if (date != null) {
                    Log.d(TAG, "Successfully parsed with format: " + format);
                    return date;
                }
            } catch (ParseException e) {
                // Продолжаем со следующим форматом
            }
        }

        Log.e(TAG, "Failed to parse date: " + dateString);
        return null;
    }

    // Основная функция проверки времени
    private void isTenMinutesRemainingFunction() {
        if (required_time == null || required_time.isEmpty()) {
            Log.d(TAG, "required_time is null or empty");
            return;
        }

        Log.d(TAG, "required_time: " + required_time);

        // Проверяем, не является ли дата пустой (1970 год)
        if (isDefaultDate(required_time)) {
            Log.d(TAG, "Default date detected, stopping timer");
            stopTimer();
            return;
        }

        Date requiredDate = parseDate(required_time);
        if (requiredDate == null) {
            Log.e(TAG, "Failed to parse required_time: " + required_time);
            return;
        }

        Date currentDate = new Date();

        // Разница в миллисекундах
        long diffInMillis = requiredDate.getTime() - currentDate.getTime();

        // Конвертируем в минуты
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);

        Log.d(TAG, "diffInMinutes: " + diffInMinutes);

        // Если время уже прошло
        if (diffInMinutes <= 0) {
            Log.d(TAG, "Time has passed, setting isTenMinutesRemaining to true");
            viewModel.setIsTenMinutesRemaining(true);
            stopTimer();
            return;
        }

        // Проверка на оставшиеся 10 минут
        long tenMinutes = 10;
        boolean isTenMinutesRemaining = diffInMinutes <= tenMinutes && diffInMinutes > 0;

        Log.d(TAG, "diffInMinutes: " + diffInMinutes);
        Log.d(TAG, "tenMinutes: " + tenMinutes);
        Log.d(TAG, "isTenMinutesRemaining: " + isTenMinutesRemaining);

        viewModel.setIsTenMinutesRemaining(isTenMinutesRemaining);

        // Если осталось 10 минут или меньше, останавливаем таймер
        if (isTenMinutesRemaining) {
            Log.d(TAG, "10 minutes or less remaining, stopping timer");
            stopTimer();
        }
    }
}