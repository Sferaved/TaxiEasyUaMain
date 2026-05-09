package com.taxi.easy.ua.ui.weather.finish;

import static android.view.View.GONE;

import android.content.Context;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.taxi.easy.ua.utils.log.Logger; // Импортируйте ваш Logger

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import com.taxi.easy.ua.R;

import okhttp3.OkHttpClient;

public class PassengerNotifier {
    private static final String TAG = "PassengerNotifier";
    private CityInfoHelper apiHelper;
    private long searchStartTime;
    private Context appContext; // Сохраняем контекст для логов

    public PassengerNotifier(Context context) {
        this.appContext = context.getApplicationContext();
        apiHelper = new CityInfoHelper();
        Logger.d(appContext, TAG, "PassengerNotifier инициализирован");
    }

    // Вызовите этот метод, когда начался поиск машины
    public void onSearchStarted() {
        searchStartTime = System.currentTimeMillis();
        Logger.d(appContext, TAG, "onSearchStarted: поиск начат, время старта=" + searchStartTime);
    }

    // Вызывайте этот метод для проверки (например, через 1 секунду и через 2 минуты)
    public void checkAndNotify(Context context, String city) {
        // Нормализуем название города для API
        String normalizedCity = normalizeCityName(city);

        long currentTime = System.currentTimeMillis();
        long elapsedMs = currentTime - searchStartTime;
        long elapsedSeconds = elapsedMs / 1000;

        Logger.d(context, TAG, "checkAndNotify: исходный город='" + city + "', нормализованный='" + normalizedCity +
                "', elapsedSeconds=" + elapsedSeconds + " (лимит 120 сек)");

        // Проверяем только первые 2 минуты
        if (elapsedSeconds > 120) {
            Logger.d(context, TAG, "checkAndNotify: превышен лимит 120 секунд, пропускаем");
            return;
        }

        Logger.d(context, TAG, "checkAndNotify: отправляем запрос к API для города=" + normalizedCity);

        apiHelper.getCityInfo(normalizedCity, new CityInfoHelper.CityInfoCallback() {
            @Override
            public void onSuccess(CityInfo info) {
                Logger.d(context, TAG, "API onSuccess: получены данные - weather=" + info.getWeather() +
                        ", temperature=" + info.getTemperature() +
                        ", airAlarm=" + info.isAirAlarm() +
                        ", rebActive=" + info.isRebActive());

                String message = buildNotificationMessage(info);
                if (message != null) {
                    Logger.d(context, TAG, "onSuccess: показываем уведомление: " + message);
                    showNotification(context, message);
                } else {
                    Logger.d(context, TAG, "onSuccess: проблем не обнаружено, уведомление не показываем");
                }
            }

            @Override
            public void onError(String error) {
                Logger.d(context, TAG, "API onError: ошибка получения данных - " + error);
            }
        });
    }
    private String normalizeCityName(String city) {
        if (city == null) return "kiev";

        Logger.d(appContext, TAG, "normalizeCityName: входное значение city='" + city + "'");

        String normalizedCity;

        switch (city) {
            case "Kyiv City":
                normalizedCity = "kiev";
                break;
            case "Dnipropetrovsk Oblast":
                normalizedCity = "dnipro";
                break;
            case "Odessa":
            case "OdessaTest":
                normalizedCity = "odesa";
                break;
            case "Zaporizhzhia":
                normalizedCity = "zaporizhia";
                break;
            case "Cherkasy Oblast":
                normalizedCity = "cherkasy";
                break;
            case "Lviv":
                normalizedCity = "lviv";
                break;
            case "Ivano_frankivsk":
                normalizedCity = "ivano-frankivsk";
                break;
            case "Vinnytsia":
                normalizedCity = "vinnytsia";
                break;
            case "Poltava":
                normalizedCity = "poltava";
                break;
            case "Sumy":
                normalizedCity = "sumy";
                break;
            case "Kharkiv":
                normalizedCity = "kharkov";
                break;
            case "Chernihiv":
                normalizedCity = "chernihiv";
                break;
            case "Rivne":
                normalizedCity = "rivne";
                break;
            case "Ternopil":
                normalizedCity = "ternopil";
                break;
            case "Khmelnytskyi":
                normalizedCity = "khmelnytskyi";
                break;
            case "Zakarpattya":
                normalizedCity = "zakarpattya";
                break;
            case "Zhytomyr":
                normalizedCity = "zhytomyr";
                break;
            case "Kropyvnytskyi":
                normalizedCity = "kropyvnytskyi";
                break;
            case "Mykolaiv":
                normalizedCity = "mykolaiv";
                break;
            case "Chernivtsi":
                normalizedCity = "chernivtsi";
                break;
            case "Lutsk":
                normalizedCity = "lutsk";
                break;
            default:
                Logger.d(appContext, TAG, "normalizeCityName: неизвестный город '" + city + "', используем 'kiev'");
                normalizedCity = "kiev";
                break;
        }

        Logger.d(appContext, TAG, "normalizeCityName: '" + city + "' -> '" + normalizedCity + "'");
        return normalizedCity;
    }

    private String buildNotificationMessage(CityInfo info) {
        Logger.d(appContext, TAG, "buildNotificationMessage: начало анализа погодных условий");

        List<String> problems = new ArrayList<>();

        if (info.isAirAlarm()) {
            problems.add(appContext.getString(R.string.air_alarm));
            Logger.d(appContext, TAG, "buildNotificationMessage: обнаружена воздушная тревога");
        }

        if (info.isRebActive()) {
            problems.add(appContext.getString(R.string.reb_alarm));
            Logger.d(appContext, TAG, "buildNotificationMessage: обнаружена работа РЭБ");
        }

        String weather = info.getWeather();
        if (weather != null && isBadWeather(weather)) {
            problems.add(weather);
            Logger.d(appContext, TAG, "buildNotificationMessage: обнаружена плохая погода - " + weather);
        } else if (weather != null) {
            Logger.d(appContext, TAG, "buildNotificationMessage: погода нормальная - " + weather);
        } else {
            Logger.d(appContext, TAG, "buildNotificationMessage: данные о погоде отсутствуют");
        }
//        problems.add(appContext.getString(R.string.air_alarm));
//        problems.add(appContext.getString(R.string.reb_alarm));
//        problems.add(weather);
        if (problems.isEmpty()) {
            Logger.d(appContext, TAG, "buildNotificationMessage: проблем не обнаружено, возвращаем null");
            return null;
        }

        String message = appContext.getString(R.string.dificult_find_car) + " " + String.join(", ", problems) ;


        Logger.d(appContext, TAG, "buildNotificationMessage: сформировано сообщение - " + message);
        return message;
    }

    private boolean isBadWeather(String weather) {
        String[] badConditions = {"дождь", "ливень", "снег", "ураган",
                "гроза", "град", "метель"};

        Logger.d(appContext, TAG, "isBadWeather: проверяем погоду \"" + weather + "\"");

        for (String condition : badConditions) {
            if (weather.toLowerCase().contains(condition)) {
                Logger.d(appContext, TAG, "isBadWeather: погода \"" + weather +
                        "\" признана плохой (совпадение с \"" + condition + "\")");
                return true;
            }
        }

        Logger.d(appContext, TAG, "isBadWeather: погода \"" + weather + "\" нормальная");
        return false;
    }

    private void showNotification(Context context, String message) {
        Logger.d(context, TAG, "showNotification: начинаем показ уведомления");
        Logger.d(context, TAG, "showNotification: message=" + message);

        // Вариант 1: Toast (можно оставить или убрать)
//        Logger.d(context, TAG, "showNotification: показываем Toast");
//        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

//         Вариант 2: Кастомный диалог в стиле verification_simple
        Logger.d(context, TAG, "showNotification: показываем кастомный AlertDialog");

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_verification_simple, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);

        tvTitle.setText("Зверніть увагу");
        tvMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnNegative = dialogView.findViewById(R.id.btnNegative);
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);

        btnNegative.setVisibility(GONE);
        btnPositive.setText("Зрозуміло");



        btnPositive.setOnClickListener(v -> {
            Logger.d(context, TAG, "showNotification: нажата кнопка 'Зрозуміло'");
            dialog.dismiss();
        });

        dialog.show();

        Logger.d(context, TAG, "showNotification: кастомный диалог показан");
    }
}