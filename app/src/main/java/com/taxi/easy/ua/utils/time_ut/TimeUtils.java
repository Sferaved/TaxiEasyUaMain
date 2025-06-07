package com.taxi.easy.ua.utils.time_ut;



import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.taxi.easy.ua.ui.finish.model.ExecutionStatusViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    private final ExecutionStatusViewModel viewModel;
    public TimeUtils(String required_time,  ExecutionStatusViewModel viewModel) {

        this.required_time = required_time;
        this.viewModel = viewModel;
    }

    private static final String TAG = "TimeUtils";
    private static Handler handler;

    private static Runnable runnable;

    private String required_time;
    private static final long INTERVAL = 30000; // 30 секунд в миллисекундах

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



    public void startTimer() {
        handler = new Handler(Looper.getMainLooper());

        runnable = new Runnable() {
            @Override
            public void run() {
                isTenMinutesRemainingFunction();
                // Повторный запуск через 30 секунд, если таймер не остановлен
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
            handler = null; // Очищаем handler для предотвращения утечек
            runnable = null; // Очищаем runnable
        }
    }

    // Ваша исходная функция
    private void isTenMinutesRemainingFunction() {


        if (!this.required_time.isEmpty()) {
            Log.e(TAG, "required_time " + required_time);

            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

            try {
                Date requiredDate = inputFormat.parse(required_time);
                Date currentDate = new Date(); // Текущее время

                // Разница в миллисекундах
                assert requiredDate != null;
                long diffInMillis = requiredDate.getTime() - currentDate.getTime();

                // Конвертируем в минуты
                long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);

                if (diffInMinutes <=0 && !required_time.contains("1970-01-01")) {

                    viewModel.setIsTenMinutesRemaining(true);
                    stopTimer();
                } else {
                    long tenMinutes = 10; // 10 минут
                    // Проверка на оставшиеся 10 минут
                    Log.d("isTenMinutesRemainingFunction", "tenMinutes " + tenMinutes);
                    Log.d("isTenMinutesRemainingFunction", "diffInMinutes " + diffInMinutes);


                    boolean isTenMinutesRemaining = diffInMinutes <= tenMinutes && diffInMinutes > 0;
                    Log.d("isTenMinutesRemainingFunction", "isTenMinutesRemaining " + isTenMinutesRemaining);
                    viewModel.setIsTenMinutesRemaining(diffInMinutes <= tenMinutes && diffInMinutes > 0);
                    if (isTenMinutesRemaining) {
                        stopTimer();
                    }
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

}
