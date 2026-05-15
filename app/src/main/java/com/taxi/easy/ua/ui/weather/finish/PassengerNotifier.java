package com.taxi.easy.ua.ui.weather.finish;

import static android.view.View.GONE;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.ui.weather.WeatherApiHelper;
import com.taxi.easy.ua.ui.weather.WeatherResponse;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PassengerNotifier {
    private static final String TAG = "PassengerNotifier";
    private CityInfoHelper apiHelper;
    private long searchStartTime;
    private Context appContext;
    private String pendingCity;

    public PassengerNotifier(Context context) {
        this.appContext = context.getApplicationContext();
        apiHelper = new CityInfoHelper(this.appContext);
        Logger.d(appContext, TAG, "PassengerNotifier инициализирован");
    }

    public void onSearchStarted() {
        searchStartTime = System.currentTimeMillis();
        Logger.d(appContext, TAG, "onSearchStarted: поиск начат, время старта=" + searchStartTime);
    }

    public void checkAndNotify(Context context, String city) {
        this.pendingCity = city;
        String normalizedCity = normalizeCityName(city);

        long currentTime = System.currentTimeMillis();
        long elapsedMs = currentTime - searchStartTime;
        long elapsedSeconds = elapsedMs / 1000;

        Logger.d(context, TAG, "checkAndNotify: elapsedSeconds=" + elapsedSeconds);

        if (elapsedSeconds > 120) {
            Logger.d(context, TAG, "checkAndNotify: превышен лимит 120 секунд");
            return;
        }

        apiHelper.getCityInfo(normalizedCity, new CityInfoHelper.CityInfoCallback() {
            @Override
            public void onSuccess(CityInfo info) {
                Logger.d(context, TAG, "API onSuccess: airAlarm=" + info.isAirAlarm() +
                        ", rebActive=" + info.isRebActive());

                CityInfo alertOnlyInfo = new CityInfo();
                alertOnlyInfo.setAirAlarm(info.isAirAlarm());
                alertOnlyInfo.setRebActive(info.isRebActive());

                fetchWeatherOnly(context, normalizedCity, alertOnlyInfo);
            }

            @Override
            public void onError(String error) {
                Logger.d(context, TAG, "API onError: " + error);
                fetchWeatherOnly(context, normalizedCity, null);
            }
        });
    }

    /**
     * Получает текущую локаль приложения
     */
    private static String getLanguage(Context context) {
        Configuration config = context.getResources().getConfiguration();

        // Правильный способ для Android 7+
        Locale locale;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            locale = config.getLocales().get(0);
        } else {
            locale = config.locale;
        }

        String language = locale.getLanguage();
        Logger.d(context, TAG, "getLanguage: системная локаль = " + language);

        // Возвращаем код для OpenWeather API
        switch (language) {
            case "uk": return "uk";
            case "ru": return "ru";
            case "en": return "en";
            default: return "uk";
        }
    }

    private void fetchWeatherOnly(Context context, String city, CityInfo alertInfo) {
        Logger.d(context, TAG, "fetchWeatherOnly: город=" + city);

        // Получаем текущую локаль приложения
        String localeCode = getLanguage(context);
        Logger.d(context, TAG, "fetchWeatherOnly: запрашиваем погоду на языке - " + localeCode);

        String apiKey = WeatherApiHelper.getApiKey(context);

        if (apiKey == null || apiKey.isEmpty()) {
            loadApiKeyAndFetchWeatherOnly(context, city, alertInfo);
            return;
        }

        doFetchWeatherOnly(context, city, alertInfo, apiKey, localeCode);
    }

    private void loadApiKeyAndFetchWeatherOnly(Context context, String city, CityInfo alertInfo) {
        FirestoreHelper firestoreHelper = new FirestoreHelper(context);

        firestoreHelper.getWeatherKey(new FirestoreHelper.OnVisicomKeyFetchedListener() {
            @Override
            public void onSuccess(String vKey) {
                if (vKey != null && !vKey.isEmpty()) {
                    MainActivity.weatherKey = vKey;
                    WeatherApiHelper.saveApiKey(context, vKey);
                    String localeCode = getLanguage(context);
                    doFetchWeatherOnly(context, city, alertInfo, vKey, localeCode);
                } else {
                    showFinalNotification(context, alertInfo, null, -273);
                }
            }

            @Override
            public void onFailure(Exception e) {
                showFinalNotification(context, alertInfo, null, -273);
            }
        });
    }

    private void doFetchWeatherOnly(Context context, String city, CityInfo alertInfo, String apiKey, String localeCode) {
        String displayCity = getCityDisplayName(pendingCity);

        // Передаём локаль в запрос погоды
        WeatherApiHelper.fetchWeatherAsyncWithLocale(context, displayCity, apiKey, localeCode, new WeatherApiHelper.WeatherCallback() {
            @Override
            public void onSuccess(WeatherResponse weather) {
                Logger.d(context, TAG, "Погода получена на языке " + localeCode);

                String weatherDescription = null;
                int temperature = -273;

                if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
                    weatherDescription = weather.getWeather().get(0).getDescription();
                    Logger.d(context, TAG, "Описание погоды: " + weatherDescription);
                }

                if (weather.getMain() != null) {
                    temperature = (int) Math.round(weather.getMain().getTemp());
                }

                showFinalNotification(context, alertInfo, weatherDescription, temperature);
            }

            @Override
            public void onFailure(String error) {
                Logger.e(context, TAG, "Ошибка получения погоды: " + error);
                showFinalNotification(context, alertInfo, null, -273);
            }
        });
    }

    private void showFinalNotification(Context context, CityInfo alertInfo, String weather, int temperature) {
        // Сохраняем текущую локаль ДО создания fullInfo
        String currentLocale = getLanguage(context);
        Logger.d(context, TAG, "showFinalNotification: текущая локаль=" + currentLocale);

        CityInfo fullInfo = new CityInfo();

        if (alertInfo != null) {
            fullInfo.setAirAlarm(alertInfo.isAirAlarm());
            fullInfo.setRebActive(alertInfo.isRebActive());
        }

        if (weather != null) {
            fullInfo.setWeather(weather);
        }
        fullInfo.setTemperature(temperature);

        // Передаём сохранённую локаль в buildNotificationMessage
        String message = buildNotificationMessage(fullInfo, currentLocale);
        if (message != null) {
            Logger.d(context, TAG, "Показываем уведомление: " + message);
            showNotification(context, message);
        }
    }




    private String getCityDisplayName(String cityCode) {
        if (cityCode == null) return "Київ";

        switch (cityCode) {
            case "Kyiv City": return "Київ";
            case "Dnipropetrovsk Oblast": return "Дніпро";
            case "Odessa":
            case "OdessaTest": return "Одеса";
            case "Zaporizhzhia": return "Запоріжжя";
            case "Cherkasy Oblast": return "Черкаси";
            case "Lviv": return "Львів";
            case "Ivano_frankivsk": return "Івано-Франківськ";
            case "Vinnytsia": return "Вінниця";
            case "Poltava": return "Полтава";
            case "Sumy": return "Суми";
            case "Kharkiv": return "Харків";
            case "Chernihiv": return "Чернігів";
            case "Rivne": return "Рівне";
            case "Ternopil": return "Тернопіль";
            case "Khmelnytskyi": return "Хмельницький";
            case "Zakarpattya": return "Ужгород";
            case "Zhytomyr": return "Житомир";
            case "Kropyvnytskyi": return "Кропивницький";
            case "Mykolaiv": return "Миколаїв";
            case "Chernivtsi": return "Чернівці";
            case "Lutsk": return "Луцьк";
            default: return "Київ";
        }
    }

    private String normalizeCityName(String city) {
        if (city == null) return "kiev";

        switch (city) {
            case "Kyiv City": return "kiev";
            case "Dnipropetrovsk Oblast": return "dnipro";
            case "Odessa":
            case "OdessaTest": return "odesa";
            case "Zaporizhzhia": return "zaporizhia";
            case "Cherkasy Oblast": return "cherkasy";
            case "Lviv": return "lviv";
            case "Ivano_frankivsk": return "ivano-frankivsk";
            case "Vinnytsia": return "vinnytsia";
            case "Poltava": return "poltava";
            case "Sumy": return "sumy";
            case "Kharkiv": return "kharkov";
            case "Chernihiv": return "chernihiv";
            case "Rivne": return "rivne";
            case "Ternopil": return "ternopil";
            case "Khmelnytskyi": return "khmelnytskyi";
            case "Zakarpattya": return "zakarpattya";
            case "Zhytomyr": return "zhytomyr";
            case "Kropyvnytskyi": return "kropyvnytskyi";
            case "Mykolaiv": return "mykolaiv";
            case "Chernivtsi": return "chernivtsi";
            case "Lutsk": return "lutsk";
            default: return "kiev";
        }
    }

    private void applyLocale(String localeCode) {
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(appContext.getResources().getConfiguration());
        config.setLocale(locale);
        appContext.getResources().updateConfiguration(config, appContext.getResources().getDisplayMetrics());
    }

    private String buildNotificationMessage(CityInfo info, String localeCode) {
        Context context = MyApplication.getContext();

        // Создаём временный Context с нужной локалью для получения строк
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(new Locale(localeCode));
        Context localizedContext = context.createConfigurationContext(config);

        Logger.d(context, TAG, "buildNotificationMessage: используем локаль=" + localeCode);

        List<String> problems = new ArrayList<>();

        if (info.isAirAlarm()) {
            problems.add(localizedContext.getString(R.string.air_alarm));
            Logger.d(context, TAG, "buildNotificationMessage: обнаружена воздушная тревога");
        }

        if (info.isRebActive()) {
            problems.add(localizedContext.getString(R.string.reb_alarm));
            Logger.d(context, TAG, "buildNotificationMessage: обнаружена работа РЭБ");
        }

        String weather = info.getWeather();
        int temperature = (int) info.getTemperature();

        if (weather != null && isBadWeather(weather)) {
            problems.add(weather);
            Logger.d(context, TAG, "buildNotificationMessage: обнаружена плохая погода - " + weather);
        }

        String message;

        if (problems.isEmpty()) {
            if (weather != null && temperature > -50) {
                message = localizedContext.getString(R.string.weather_good_message, weather, temperature);
                Logger.d(context, TAG, "message1: " + message);
            } else if (weather != null) {
                message = localizedContext.getString(R.string.weather_good_no_temp, weather);
                Logger.d(context, TAG, "message2: " + message);
            } else if (temperature > -50) {
                message = localizedContext.getString(R.string.weather_good_temp_only, temperature);
                Logger.d(context, TAG, "message3: " + message);
            } else {
                Logger.d(context, TAG, "buildNotificationMessage: нет данных о погоде, уведомление не показываем");
                return null;
            }
        } else {
            message = localizedContext.getString(R.string.dificult_find_car) + " " + String.join(", ", problems);
            Logger.d(context, TAG, "buildNotificationMessage: сообщение о проблемах - " + message);
        }

        return message;
    }

    private boolean isBadWeather(String weather) {
        String lowerWeather = weather.toLowerCase();

        String[] rainConditions = {"дождь", "дощ", "ливень", "злива", "моросящий дождь", "морось", "мряка", "проливной дождь", "зливовий дощ", "затяжной дождь", "очень сильный дождь", "дуже сильний дощ"};
        String[] snowConditions = {"снег", "сніг", "снегопад", "снігопад", "метель", "завірюха", "поземок", "поземка", "снежные зерна", "снігові зерна", "ледяной дождь", "крижаний дощ", "град", "крупа"};
        String[] thunderConditions = {"гроза", "гроза з дощем", "гроза с дождем", "гроза з градом", "гроза с градом", "сильная гроза", "сильна гроза"};
        String[] fogConditions = {"туман", "густой туман", "густий туман", "дымка", "імла", "смог", "пыль", "пил", "песок", "пісок"};
        String[] windConditions = {"ураган", "шторм", "сильный ветер", "сильний вітер", "шквал", "торнадо"};
        String[] extremeConditions = {"экстремальная жара", "екстремальна спека", "экстремальный холод", "екстремальний холод", "крижаний дощ", "ледяной дождь"};

        for (String condition : rainConditions) {
            if (lowerWeather.contains(condition)) return true;
        }
        for (String condition : snowConditions) {
            if (lowerWeather.contains(condition)) return true;
        }
        for (String condition : thunderConditions) {
            if (lowerWeather.contains(condition)) return true;
        }
        for (String condition : fogConditions) {
            if (lowerWeather.contains(condition)) return true;
        }
        for (String condition : windConditions) {
            if (lowerWeather.contains(condition)) return true;
        }
        for (String condition : extremeConditions) {
            if (lowerWeather.contains(condition)) return true;
        }

        return false;
    }

    private void showNotification(Context context, String message) {
        Logger.d(context, TAG, "showNotification: показываем кастомный AlertDialog");

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_verification_simple, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);

        tvTitle.setText(R.string.attantion_mes);
        tvMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button btnNegative = dialogView.findViewById(R.id.btnNegative);
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);

        btnNegative.setVisibility(GONE);
        btnPositive.setText(R.string.ok_error);

        btnPositive.setOnClickListener(v -> {
            Logger.d(context, TAG, "showNotification: нажата кнопка 'Зрозуміло'");
            dialog.dismiss();
        });

        dialog.show();
    }
}