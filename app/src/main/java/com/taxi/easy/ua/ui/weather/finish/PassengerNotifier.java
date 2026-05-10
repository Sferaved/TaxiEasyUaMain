package com.taxi.easy.ua.ui.weather.finish;

import static android.view.View.GONE;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.weather.WeatherApiHelper;
import com.taxi.easy.ua.ui.weather.WeatherResponse;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PassengerNotifier {
    private static final String TAG = "PassengerNotifier";
    private CityInfoHelper apiHelper;
    private long searchStartTime;
    private Context appContext;
    private String pendingCity; // Сохраняем город для повторных попыток

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
        // Сохраняем город для повторных попыток
        this.pendingCity = city;

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

                // Если погода от CityInfoHelper отсутствует или некорректна, пробуем получить из OpenWeather
                if (!isWeatherValid(info.getWeather())) {
                    Logger.d(context, TAG, "Погода от CityInfoHelper невалидна: " + info.getWeather());
                    fetchWeatherFromOpenWeather(context, normalizedCity, info);
                } else {
                    String message = buildNotificationMessage(info);
                    if (message != null) {
                        Logger.d(context, TAG, "onSuccess: показываем уведомление: " + message);
                        showNotification(context, message);
                    } else {
                        Logger.d(context, TAG, "onSuccess: проблем не обнаружено, уведомление не показываем");
                    }
                }
            }

            @Override
            public void onError(String error) {
                Logger.d(context, TAG, "API onError: ошибка получения данных - " + error);
                // При ошибке CityInfoHelper пытаемся получить хотя бы погоду
                fetchWeatherFromOpenWeather(context, normalizedCity, null);
            }
        });
    }

    /**
     * Проверяет, является ли погодное условие валидным
     */
    private boolean isWeatherValid(String weather) {
        if (weather == null || weather.isEmpty()) {
            return false;
        }
        // Проверяем, что это не "unknown" или подобные значения
        String lowerWeather = weather.toLowerCase();
        return !lowerWeather.equals("unknown") &&
                !lowerWeather.equals("none") &&
                !lowerWeather.contains("null");
    }

    /**
     * Получение погоды из OpenWeather API
     */
    private void fetchWeatherFromOpenWeather(Context context, String city, CityInfo existingInfo) {
        Logger.d(context, TAG, "fetchWeatherFromOpenWeather: пытаемся получить погоду для города " + city);

        String apiKey = WeatherApiHelper.getApiKey(context);

        if (apiKey == null || apiKey.isEmpty()) {
            Logger.e(context, TAG, "API ключ OpenWeather не найден, загружаем из Firestore");
            loadApiKeyAndFetchWeather(context, city, existingInfo);
            return;
        }

        doFetchWeather(context, city, existingInfo, apiKey);
    }

    /**
     * Загружаем API ключ из Firestore и затем получаем погоду
     */
    private void loadApiKeyAndFetchWeather(Context context, String city, CityInfo existingInfo) {
        FirestoreHelper firestoreHelper = new FirestoreHelper(context);

        firestoreHelper.getWeatherKey(new FirestoreHelper.OnVisicomKeyFetchedListener() {
            @Override
            public void onSuccess(String vKey) {
                if (vKey != null && !vKey.isEmpty()) {
                    MainActivity.weatherKey = vKey;
                    WeatherApiHelper.saveApiKey(context, vKey);
                    Logger.d(context, TAG, "weatherKey получен из Firestore: " + vKey);
                    doFetchWeather(context, city, existingInfo, vKey);
                } else {
                    Logger.e(context, TAG, "weatherKey из Firestore пуст");
                    showNoWeatherNotification(context, existingInfo);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Logger.e(context, TAG, "Ошибка получения weatherKey: " + e.getMessage());
                showNoWeatherNotification(context, existingInfo);
            }
        });
    }

    /**
     * Выполняет фактический запрос погоды
     */
    // В методе fetchWeatherFromOpenWeather, обновите doFetchWeather:

    private void doFetchWeather(Context context, String city, CityInfo existingInfo, String apiKey) {
        // Получаем полное название города для отображения
        String displayCity = getCityDisplayName(pendingCity);

        WeatherApiHelper.fetchWeatherAsync(context, displayCity, apiKey, new WeatherApiHelper.WeatherCallback() {
            @Override
            public void onSuccess(WeatherResponse weather) {
                Logger.d(context, TAG, "OpenWeather успешно получена: temp=" +
                        (weather.getMain() != null ? weather.getMain().getTemp() : "null"));

                // Создаем обновленный CityInfo с погодой из OpenWeather
                CityInfo updatedInfo = existingInfo != null ? existingInfo : new CityInfo();

                if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
                    String weatherDescription = weather.getWeather().get(0).getDescription();
                    updatedInfo.setWeather(weatherDescription);
                    Logger.d(context, TAG, "Установлена погода из OpenWeather: " + weatherDescription);
                }

                if (weather.getMain() != null) {
                    updatedInfo.setTemperature((int) Math.round(weather.getMain().getTemp()));
                    Logger.d(context, TAG, "Установлена температура из OpenWeather: " + weather.getMain().getTemp());
                }

                String message = buildNotificationMessage(updatedInfo);
                if (message != null) {
                    Logger.d(context, TAG, "Показываем уведомление: " + message);
                    showNotification(context, message);
                } else {
                    Logger.d(context, TAG, "Нет причин для уведомления");
                }
            }

            @Override
            public void onFailure(String error) {
                Logger.e(context, TAG, "Ошибка получения погоды из OpenWeather: " + error);
                // Если нет погоды, но есть другие проблемы - показываем их
                if (existingInfo != null && (existingInfo.isAirAlarm() || existingInfo.isRebActive())) {
                    String message = buildNotificationMessage(existingInfo);
                    if (message != null) {
                        showNotification(context, message);
                    }
                }
            }
        });
    }

    /**
     * Показывает уведомление с информацией о проблемах (если есть)
     */
    private void showNoWeatherNotification(Context context, CityInfo info) {
        if (info == null) {
            Logger.d(context, TAG, "Нет данных ни от CityInfoHelper, ни от OpenWeather");
            return;
        }

        String message = buildNotificationMessage(info);
        if (message != null) {
            Logger.d(context, TAG, "Показываем уведомление (только с проблемами, без погоды): " + message);
            showNotification(context, message);
        } else {
            Logger.d(context, TAG, "Нет данных для уведомления");
        }
    }

    /**
     * Получает отображаемое имя города на основе кода города
     */
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

    // PassengerNotifier.java - обновленный метод buildNotificationMessage

    private String buildNotificationMessage(CityInfo info) {
        Logger.d(appContext, TAG, "buildNotificationMessage: начало анализа погодных условий");

        List<String> problems = new ArrayList<>();

        // Проверяем тревоги
        if (info.isAirAlarm()) {
            problems.add(appContext.getString(R.string.air_alarm));
            Logger.d(appContext, TAG, "buildNotificationMessage: обнаружена воздушная тревога");
        }

        if (info.isRebActive()) {
            problems.add(appContext.getString(R.string.reb_alarm));
            Logger.d(appContext, TAG, "buildNotificationMessage: обнаружена работа РЭБ");
        }

        String weather = info.getWeather();
        int temperature = (int) info.getTemperature();

        // Проверяем погоду

        if (weather != null && isBadWeather(weather)) {
            problems.add(weather);
            Logger.d(appContext, TAG, "buildNotificationMessage: обнаружена плохая погода - " + weather);
        } else if (weather != null) {
            Logger.d(appContext, TAG, "buildNotificationMessage: погода нормальная - " + weather);
        } else {
            Logger.d(appContext, TAG, "buildNotificationMessage: данные о погоде отсутствуют");
        }

        // Формируем сообщение
        String message;

        if (problems.isEmpty()) {
            // Нет проблем - показываем хорошую погоду и пожелание
            if (weather != null && temperature > -50) {
                message = appContext.getString(R.string.weather_good_message, weather, temperature);
            } else if (weather != null) {
                message = appContext.getString(R.string.weather_good_no_temp, weather);
            } else if (temperature > -50) {
                message = appContext.getString(R.string.weather_good_temp_only, temperature);
            } else {
                // Если нет данных о погоде, не показываем уведомление
                Logger.d(appContext, TAG, "buildNotificationMessage: нет данных о погоде, уведомление не показываем");
                return null;
            }
            Logger.d(appContext, TAG, "buildNotificationMessage: хорошая погода, сообщение - " + message);
        } else {
            // Есть проблемы - показываем их
            message = appContext.getString(R.string.dificult_find_car) + " " + String.join(", ", problems);
            Logger.d(appContext, TAG, "buildNotificationMessage: сформировано сообщение о проблемах - " + message);
        }

        return message;
    }

    private boolean isBadWeather(String weather) {
        // Приводим к нижнему регистру для сравнения
        String lowerWeather = weather.toLowerCase();

        // Группа "Дождь" (Rain)
        String[] rainConditions = {
                "дождь", "дощ", // rain
                "ливень", "злива", // heavy rain
                "моросящий дождь", "морось", "мряка", // drizzle
                "проливной дождь", "зливовий дощ", // shower rain
                "затяжной дождь", // heavy intensity rain
                "очень сильный дождь", "дуже сильний дощ" // very heavy rain
        };

        // Группа "Снег" (Snow)
        String[] snowConditions = {
                "снег", "сніг", // snow
                "снегопад", "снігопад", // heavy snow
                "метель", "завірюха", // blizzard
                "поземок", "поземка", // drifting snow
                "снежные зерна", "снігові зерна", // snow grains
                "ледяной дождь", "крижаний дощ", // freezing rain
                "град", "град", // hail
                "крупа", "крупа" // snow pellets
        };

        // Группа "Гроза" (Thunderstorm)
        String[] thunderConditions = {
                "гроза", "гроза", // thunderstorm
                "гроза с дождем", "гроза з дощем", // thunderstorm with rain
                "гроза с градом", "гроза з градом", // thunderstorm with hail
                "сильная гроза", "сильна гроза" // heavy thunderstorm
        };

        // Группа "Туман/Плохая видимость" (Mist/Fog)
        String[] fogConditions = {
                "туман", "туман", // fog
                "густой туман", "густий туман", // heavy fog
                "дымка", "імла", // mist
                "смог", "смог", // smoke/haze
                "пыль", "пил", // dust
                "песок", "пісок", // sand
                "вулканический пепел", "вулканічний попіл" // volcanic ash
        };

        // Группа "Ветер" (Extreme Wind)
        String[] windConditions = {
                "ураган", "ураган", // hurricane
                "шторм", "шторм", // storm
                "сильный ветер", "сильний вітер", // strong wind
                "шквал", "шквал", // squall
                "торнадо", "торнадо" // tornado
        };

        // Группа "Экстремальные условия"
        String[] extremeConditions = {
                "экстремальная жара", "екстремальна спека", // extreme heat
                "экстремальный холод", "екстремальний холод", // extreme cold
                "ледяной дождь", "крижаний дощ" // freezing rain
        };

        // Проверяем все группы
        for (String condition : rainConditions) {
            if (lowerWeather.contains(condition)) {
                Logger.d(appContext, TAG, "isBadWeather: плохая погода (дождь) - " + weather);
                return true;
            }
        }

        for (String condition : snowConditions) {
            if (lowerWeather.contains(condition)) {
                Logger.d(appContext, TAG, "isBadWeather: плохая погода (снег/град) - " + weather);
                return true;
            }
        }

        for (String condition : thunderConditions) {
            if (lowerWeather.contains(condition)) {
                Logger.d(appContext, TAG, "isBadWeather: плохая погода (гроза) - " + weather);
                return true;
            }
        }

        for (String condition : fogConditions) {
            if (lowerWeather.contains(condition)) {
                Logger.d(appContext, TAG, "isBadWeather: плохая погода (туман/плохая видимость) - " + weather);
                return true;
            }
        }

        for (String condition : windConditions) {
            if (lowerWeather.contains(condition)) {
                Logger.d(appContext, TAG, "isBadWeather: плохая погода (сильный ветер) - " + weather);
                return true;
            }
        }

        for (String condition : extremeConditions) {
            if (lowerWeather.contains(condition)) {
                Logger.d(appContext, TAG, "isBadWeather: плохая погода (экстремальные условия) - " + weather);
                return true;
            }
        }

        Logger.d(appContext, TAG, "isBadWeather: погода \"" + weather + "\" нормальная");
        return false;
    }

    private void showNotification(Context context, String message) {
        Logger.d(context, TAG, "showNotification: начинаем показ уведомления");
        Logger.d(context, TAG, "showNotification: message=" + message);

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