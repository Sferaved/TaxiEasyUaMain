package com.taxi.easy.ua.utils.city;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.ip.ApiServiceCountry;
import com.taxi.easy.ua.utils.ip.CountryResponse;
import com.taxi.easy.ua.utils.location.AutoLocationAfterCityHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.worker.utils.WfpUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.taxi.easy.ua.utils.db.CursorReadHelper;

public class CityFinder {
    private static final String TAG = "CityFinder";
    Context context;
    String countryState;
    String baseUrl;
    String pay_method;
    String phoneNumber;

    double startLat;
    double startLan;
    String position;

    // ✅ Флаги для защиты от перезаписи
    private static boolean isProcessing = false;
    private static String lastProcessedCity = "";
    private static long lastProcessedTime = 0;
    private static final long PROCESSING_TIMEOUT = 5000; // 5 секунд
    private static final Object lock = new Object();

    // ✅ Флаг для защиты от повторного показа диалога
    private static boolean isCityChangeDialogShowing = false;

    public static boolean isCityFinderBusy() {
        return isProcessing;
    }

    public static void resetFlags() {
        synchronized (lock) {
            isProcessing = false;
            isCityChangeDialogShowing = false;
            lastProcessedCity = "";
            Logger.d(null, TAG, "CityFinder flags reset");
        }
    }

    public CityFinder() {
        // Пустой конструктор без аргументов
    }

    private WeakReference<Activity> activityRef;
    String cityMenu;
    boolean cangedCity;

    public CityFinder(
            Context context,
            double startLat,
            double startLan,
            String position,
            Activity activity
    ) {
        this.context = context;
        this.startLat = startLat;
        this.startLan = startLan;
        this.position = position;
        this.activityRef = new WeakReference<>(activity);
    }

    public void findCity(double latitude, double longitude) {
        // ✅ Защита от множественных вызовов
        synchronized (lock) {
            if (isProcessing) {
                Logger.d(context, TAG, "CityFinder уже работает, пропускаем вызов");
                return;
            }
            isProcessing = true;
        }

        CityApiService apiService = RetrofitClient.getClient().create(CityApiService.class);
        Call<CityResponse> call = apiService.findCity(latitude, longitude);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<CityResponse> call, @NonNull Response<CityResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String city = response.body().getCity();
                    Logger.d(context, TAG, "City: " + city);
                    cityVerify(city);
                } else {
                    Logger.e(context, TAG, "Request failed: " + response.code());
                    synchronized (lock) {
                        isProcessing = false;
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.e(context, TAG, "Error: " + t.getMessage());
                synchronized (lock) {
                    isProcessing = false;
                }
            }
        });
    }

    private void cityVerify(String cityFromApi) {
        // ✅ Защита от очень частых вызовов
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastProcessedTime < PROCESSING_TIMEOUT &&
                    cityFromApi.equals(lastProcessedCity)) {
                Logger.d(context, TAG, "Слишком частые вызовы для одного города, пропускаем");
                isProcessing = false;
                return;
            }
            lastProcessedTime = currentTime;
            lastProcessedCity = cityFromApi;
        }

        String cityResult;
        switch (cityFromApi) {
            case "city_kiev":
                cityResult = "Kyiv City";
                break;
            case "city_cherkassy":
                cityResult = "Cherkasy Oblast";
                break;
            case "city_odessa":
                cityResult = "Odessa";
                break;
            case "city_zaporizhzhia":
                cityResult = "Zaporizhzhia";
                break;
            case "city_dnipro":
                cityResult = "Dnipropetrovsk Oblast";
                break;
            case "city_lviv":
                cityResult = "Lviv";
                break;
            case "city_ivano_frankivsk":
                cityResult = "Ivano_frankivsk";
                break;
            case "city_vinnytsia":
                cityResult = "Vinnytsia";
                break;
            case "city_poltava":
                cityResult = "Poltava";
                break;
            case "city_sumy":
                cityResult = "Sumy";
                break;
            case "city_kharkiv":
                cityResult = "Kharkiv";
                break;
            case "city_chernihiv":
                cityResult = "Chernihiv";
                break;
            case "city_rivne":
                cityResult = "Rivne";
                break;
            case "city_ternopil":
                cityResult = "Ternopil";
                break;
            case "city_khmelnytskyi":
                cityResult = "Khmelnytskyi";
                break;
            case "city_zakarpattya":
                cityResult = "Zakarpattya";
                break;
            case "city_zhytomyr":
                cityResult = "Zhytomyr";
                break;
            case "city_kropyvnytskyi":
                cityResult = "Kropyvnytskyi";
                break;
            case "city_mykolaiv":
                cityResult = "Mykolaiv";
                break;
            case "city_chernivtsi":
                cityResult = "Chernivtsi";
                break;
            case "city_lutsk":
                cityResult = "Lutsk";
                break;
            default:
                cityResult = "all";
        }

        // ✅ Получаем город из БД
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String currentCityFromDB = stringList.get(1);

        Logger.d(context, TAG, "currentCityFromDB: " + currentCityFromDB);
        Logger.d(context, TAG, "cityResult: " + cityResult);
        sharedPreferencesHelperMain.saveValue("setStatusX", false);
        VisicomFragment.updateGpsButtonCross(false);

        // ✅ Проверяем, отличается ли город
        String normalizedCityResult = normalizeCityName(cityResult);
        String normalizedCurrentCity = normalizeCityName(currentCityFromDB);

        if (!normalizedCityResult.equals(normalizedCurrentCity)) {
            Logger.d(context, TAG, "City отличается: " + currentCityFromDB + " -> " + cityResult);

            String Kyiv_City_phone = "tel:0674443804";
            String Dnipropetrovsk_Oblast_phone = "tel:0667257070";
            String Odessa_phone = "tel:0737257070";
            String Zaporizhzhia_phone = "tel:0687257070";
            String Cherkasy_Oblast_phone = "tel:0962294243";

            switch (cityResult) {
                case "Kyiv City":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_kyiv);
                    countryState = "UA";
                    break;
                case "Dnipropetrovsk Oblast":
                    phoneNumber = Dnipropetrovsk_Oblast_phone;
                    cityMenu = context.getString(R.string.city_dnipro);
                    countryState = "UA";
                    break;
                case "Odessa":
                    phoneNumber = Odessa_phone;
                    cityMenu = context.getString(R.string.city_odessa);
                    countryState = "UA";
                    break;
                case "Zaporizhzhia":
                    phoneNumber = Zaporizhzhia_phone;
                    cityMenu = context.getString(R.string.city_zaporizhzhia);
                    countryState = "UA";
                    break;
                case "Cherkasy Oblast":
                    phoneNumber = Cherkasy_Oblast_phone;
                    cityMenu = context.getString(R.string.city_cherkassy);
                    countryState = "UA";
                    break;
                case "Lviv":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_lviv);
                    countryState = "UA";
                    break;
                case "Ivano_frankivsk":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_ivano_frankivsk);
                    countryState = "UA";
                    break;
                case "Vinnytsia":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_vinnytsia);
                    countryState = "UA";
                    break;
                case "Poltava":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_poltava);
                    countryState = "UA";
                    break;
                case "Sumy":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_sumy);
                    countryState = "UA";
                    break;
                case "Kharkiv":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_kharkiv);
                    countryState = "UA";
                    break;
                case "Chernihiv":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_chernihiv);
                    countryState = "UA";
                    break;
                case "Rivne":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_rivne);
                    countryState = "UA";
                    break;
                case "Ternopil":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_ternopil);
                    countryState = "UA";
                    break;
                case "Khmelnytskyi":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_khmelnytskyi);
                    countryState = "UA";
                    break;
                case "Zakarpattya":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_zakarpattya);
                    countryState = "UA";
                    break;
                case "Zhytomyr":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_zhytomyr);
                    countryState = "UA";
                    break;
                case "Kropyvnytskyi":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_kropyvnytskyi);
                    countryState = "UA";
                    break;
                case "Mykolaiv":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_mykolaiv);
                    countryState = "UA";
                    break;
                case "Chernivtsi":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_chernivtsi);
                    countryState = "UA";
                    break;
                case "Lutsk":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.city_lutsk);
                    countryState = "UA";
                    break;
                case "OdessaTest":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = "Test";
                    countryState = "UA";
                    break;
                case "foreign countries":
                    phoneNumber = Kyiv_City_phone;
                    cityMenu = context.getString(R.string.foreign_countries);
                    break;
                default:
                    phoneNumber = Kyiv_City_phone;
                    getPublicIPAddress();
                    cityMenu = context.getString(R.string.city_kyiv);
                    countryState = "UA";
                    break;
            }

            String newTitle = context.getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
            sharedPreferencesHelperMain.saveValue("countryState", countryState);

            Logger.d(context, TAG, "onItemClick: pay_method" + pay_method);

            updateMyPosition(cityResult, startLat, startLan, position);
        } else {
            Logger.d(context, TAG, "Города одинаковые - диалог НЕ показываем");

            if (VisicomFragment.progressBar != null) {
                VisicomFragment.progressBar.setVisibility(View.GONE);
            }
            synchronized (lock) {
                isProcessing = false;
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateMyPosition(String city, double startLat, double startLan, String position) {


        Logger.d(context, TAG, "updateMyPosition:city " + city);

        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            Logger.e(context, TAG, "Activity is null or destroyed, cannot navigate");
            synchronized (lock) {
                isProcessing = false;
            }
            return;
        }

        // ✅ установка baseUrl
        String finalCityForUrl = city;
        switch (city){
            case "Dnipropetrovsk Oblast":
            case "Odessa":
            case "Zaporizhzhia":
            case "Cherkasy Oblast":
            case "Kyiv City":
            case "Lviv":
            case "Ivano_frankivsk":
            case "Vinnytsia":
            case "Poltava":
            case "Sumy":
            case "Kharkiv":
            case "Chernihiv":
            case "Rivne":
            case "Ternopil":
            case "Khmelnytskyi":
            case "Zakarpattya":
            case "Zhytomyr":
            case "Kropyvnytskyi":
            case "Mykolaiv":
            case "Chernivtsi":
            case "Lutsk":
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://m.easy-order-taxi.site");
                break;
            case "OdessaTest":
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://t.easy-order-taxi.site");
                break;
            default:
                sharedPreferencesHelperMain.saveValue("baseUrl", "https://m.easy-order-taxi.site");
                finalCityForUrl = "foreign countries";
                break;
        }

        // ✅ Используем finalCityForUrl для дальнейшей работы
        final String finalCity = finalCityForUrl;

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String cityOld = stringList.get(1);
        Logger.d(context, TAG, "updateMyPosition:cityOld '" + cityOld + "'");
        Logger.d(context, TAG, "updateMyPosition:finalCity '" + finalCity + "'");

        if (VisicomFragment.progressBar != null) {
            VisicomFragment.progressBar.setVisibility(View.GONE);
        }

        // ✅ Нормализация названий городов для сравнения
        String normalizedFinalCity = normalizeCityName(finalCity);
        String normalizedCityOld = normalizeCityName(cityOld);

        Logger.d(context, TAG, "normalizedFinalCity: '" + normalizedFinalCity + "'");
        Logger.d(context, TAG, "normalizedCityOld: '" + normalizedCityOld + "'");

        // ✅ Проверяем, действительно ли города разные
        if (!normalizedFinalCity.equals(normalizedCityOld)) {
            Logger.d(context, TAG, "Города разные, показываем диалог");

            // ✅ Защита от повторного показа диалога
            if (isCityChangeDialogShowing) {
                Logger.d(context, TAG, "Диалог уже показывается, пропускаем");
                synchronized (lock) {
                    isProcessing = false;
                }
                return;
            }

            cangedCity = true;
            isCityChangeDialogShowing = true;

            // ✅ Получаем название города для отображения в диалоге
            String cityDisplayName = getCityDisplayName(finalCity);

            new androidx.appcompat.app.AlertDialog.Builder(activity)
                    .setTitle(R.string.city_change_dialog)
                    .setMessage(activity.getString(R.string.find_new_city_mes) + cityDisplayName + activity.getString(R.string.turn_mes))
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        VisicomFragment.updateGpsButtonCross(false);
                        isCityChangeDialogShowing = false;
                        AutoLocationAfterCityHelper.markCityChangedViaGeo();
                        applyCityChange(finalCity, startLat, startLan, position);
                        synchronized (lock) {
                            isProcessing = false;
                        }
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        isCityChangeDialogShowing = false;
                        synchronized (lock) {
                            isProcessing = false;
                        }
                        VisicomFragment.updateGpsButtonCross(true);
                        VisicomFragment.btnStaticVisible(VISIBLE);

                        Logger.d(context, TAG, "User declined city change");
                    })
                    .setOnDismissListener(dialog -> {
                        isCityChangeDialogShowing = false;
                        synchronized (lock) {
                            isProcessing = false;
                        }
                    })
                    .setCancelable(false)
                    .show();
            return;
        } else {
            Logger.d(context, TAG, "Города одинаковые, применяем изменения без диалога");
            cangedCity = false;
            // ✅ Применяем изменения даже если город не менялся (обновляем позицию)
            applyCityChange(finalCity, startLat, startLan, position);
            synchronized (lock) {
                isProcessing = false;
            }
        }
    }

    // Нормализация названий городов
    private String normalizeCityName(String cityName) {
        if (cityName == null) return "";

        // Создаём множество синонимов для каждого города
        Map<String, String> citySynonyms = new HashMap<>();

        // Киев
        citySynonyms.put("KYIV CITY", "Kyiv City");
        citySynonyms.put("KYIV", "Kyiv City");
        citySynonyms.put("KIEV", "Kyiv City");
        citySynonyms.put("КИЕВ", "Kyiv City");
        citySynonyms.put("КИЇВ", "Kyiv City");
        citySynonyms.put("CITY_KIEV", "Kyiv City");
        citySynonyms.put("CITY_KYIV", "Kyiv City");

        // Одесса
        citySynonyms.put("ODESSA", "Odessa");
        citySynonyms.put("ОДЕССА", "Odessa");
        citySynonyms.put("ОДЕСА", "Odessa");
        citySynonyms.put("CITY_ODESSA", "Odessa");

        // Днепр
        citySynonyms.put("DNIPROPETROVSK OBLAST", "Dnipropetrovsk Oblast");
        citySynonyms.put("DNIPRO", "Dnipropetrovsk Oblast");
        citySynonyms.put("ДНЕПР", "Dnipropetrovsk Oblast");
        citySynonyms.put("ДНІПРО", "Dnipropetrovsk Oblast");
        citySynonyms.put("CITY_DNIPRO", "Dnipropetrovsk Oblast");

        // Львов
        citySynonyms.put("LVIV", "Lviv");
        citySynonyms.put("ЛЬВОВ", "Lviv");
        citySynonyms.put("ЛЬВІВ", "Lviv");
        citySynonyms.put("CITY_LVIV", "Lviv");

        // Івано-Франківськ
        citySynonyms.put("IVANO_FRANKIVSK", "Ivano_frankivsk");
        citySynonyms.put("ИВАНО-ФРАНКОВСК", "Ivano_frankivsk");
        citySynonyms.put("ІВАНО-ФРАНКІВСЬК", "Ivano_frankivsk");
        citySynonyms.put("CITY_IVANO_FRANKIVSK", "Ivano_frankivsk");

        // Чернігів
        citySynonyms.put("CHERNIHIV", "Chernihiv");
        citySynonyms.put("ЧЕРНИГОВ", "Chernihiv");
        citySynonyms.put("ЧЕРНІГІВ", "Chernihiv");
        citySynonyms.put("CITY_CHERNIHIV", "Chernihiv");

        // Запорожье
        citySynonyms.put("ZAPORIZHZHIA", "Zaporizhzhia");
        citySynonyms.put("ЗАПОРОЖЬЕ", "Zaporizhzhia");
        citySynonyms.put("ЗАПОРІЖЖЯ", "Zaporizhzhia");
        citySynonyms.put("CITY_ZAPORIZHZHIA", "Zaporizhzhia");

        // Черкассы
        citySynonyms.put("CHERKASY OBLAST", "Cherkasy Oblast");
        citySynonyms.put("CHERKASY", "Cherkasy Oblast");
        citySynonyms.put("ЧЕРКАССЫ", "Cherkasy Oblast");
        citySynonyms.put("ЧЕРКАСИ", "Cherkasy Oblast");
        citySynonyms.put("CITY_CHERKASY", "Cherkasy Oblast");

        citySynonyms.put("VINNYTSIA", "Vinnytsia");
        citySynonyms.put("ВИННИЦА", "Vinnytsia");
        citySynonyms.put("ВІННИЦЯ", "Vinnytsia");
        citySynonyms.put("CITY_VINNYTSIA", "Vinnytsia");

        citySynonyms.put("POLTAVA", "Poltava");
        citySynonyms.put("ПОЛТАВА", "Poltava");
        citySynonyms.put("CITY_POLTAVA", "Poltava");

        citySynonyms.put("SUMY", "Sumy");
        citySynonyms.put("СУМЫ", "Sumy");
        citySynonyms.put("СУМИ", "Sumy");
        citySynonyms.put("CITY_SUMY", "Sumy");

        citySynonyms.put("KHARKIV", "Kharkiv");
        citySynonyms.put("ХАРЬКОВ", "Kharkiv");
        citySynonyms.put("ХАРКІВ", "Kharkiv");
        citySynonyms.put("CITY_KHARKIV", "Kharkiv");

        citySynonyms.put("RIVNE", "Rivne");
        citySynonyms.put("РОВНО", "Rivne");
        citySynonyms.put("РІВНЕ", "Rivne");
        citySynonyms.put("CITY_RIVNE", "Rivne");

        citySynonyms.put("TERNOPIL", "Ternopil");
        citySynonyms.put("ТЕРНОПОЛЬ", "Ternopil");
        citySynonyms.put("ТЕРНОПІЛЬ", "Ternopil");
        citySynonyms.put("CITY_TERNOPIL", "Ternopil");

        citySynonyms.put("KHMELNYTSKYI", "Khmelnytskyi");
        citySynonyms.put("ХМЕЛЬНИЦКИЙ", "Khmelnytskyi");
        citySynonyms.put("ХМЕЛЬНИЦЬКИЙ", "Khmelnytskyi");
        citySynonyms.put("CITY_KHMELNYTSKYI", "Khmelnytskyi");

        citySynonyms.put("ZAKARPATTYA", "Zakarpattya");
        citySynonyms.put("УЖГОРОД", "Zakarpattya");
        citySynonyms.put("CITY_ZAKARPATTYA", "Zakarpattya");

        citySynonyms.put("ZHYTOMYR", "Zhytomyr");
        citySynonyms.put("ЖИТОМИР", "Zhytomyr");
        citySynonyms.put("CITY_ZHYTOMYR", "Zhytomyr");

        citySynonyms.put("KROPYVNYTSKYI", "Kropyvnytskyi");
        citySynonyms.put("КРОПИВНИЦКИЙ", "Kropyvnytskyi");
        citySynonyms.put("КРОПИВНИЦЬКИЙ", "Kropyvnytskyi");
        citySynonyms.put("CITY_KROPYVNYTSKYI", "Kropyvnytskyi");

        citySynonyms.put("MYKOLAIV", "Mykolaiv");
        citySynonyms.put("НИКОЛАЕВ", "Mykolaiv");
        citySynonyms.put("МИКОЛАЇВ", "Mykolaiv");
        citySynonyms.put("CITY_MYKOLAIV", "Mykolaiv");

        citySynonyms.put("CHERNIVTSI", "Chernivtsi");
        citySynonyms.put("ЧЕРНОВЦЫ", "Chernivtsi");
        citySynonyms.put("ЧЕРНІВЦІ", "Chernivtsi");
        citySynonyms.put("CITY_CHERNIVTSI", "Chernivtsi");

        citySynonyms.put("LUTSK", "Lutsk");
        citySynonyms.put("ЛУЦК", "Lutsk");
        citySynonyms.put("ЛУЦЬК", "Lutsk");
        citySynonyms.put("CITY_LUTSK", "Lutsk");

        String upperCity = cityName.trim().toUpperCase(Locale.US);

        if (citySynonyms.containsKey(upperCity)) {
            return citySynonyms.get(upperCity);
        }

        // Если не нашли в синонимах, возвращаем как есть
        return cityName.trim();
    }

    // Получение отображаемого названия города для диалога
    private String getCityDisplayName(String city) {
        if (city == null) return "";

        switch (city) {
            case "Kyiv City":
                return context.getString(R.string.city_kyiv);
            case "Dnipropetrovsk Oblast":
                return context.getString(R.string.city_dnipro);
            case "Odessa":
                return context.getString(R.string.city_odessa);
            case "Zaporizhzhia":
                return context.getString(R.string.city_zaporizhzhia);
            case "Cherkasy Oblast":
                return context.getString(R.string.city_cherkassy);
            case "Lviv":
                return context.getString(R.string.city_lviv);
            case "Ivano_frankivsk":
                return context.getString(R.string.city_ivano_frankivsk);
            case "Vinnytsia":
                return context.getString(R.string.city_vinnytsia);
            case "Poltava":
                return context.getString(R.string.city_poltava);
            case "Sumy":
                return context.getString(R.string.city_sumy);
            case "Kharkiv":
                return context.getString(R.string.city_kharkiv);
            case "Chernihiv":
                return context.getString(R.string.city_chernihiv);
            case "Rivne":
                return context.getString(R.string.city_rivne);
            case "Ternopil":
                return context.getString(R.string.city_ternopil);
            case "Khmelnytskyi":
                return context.getString(R.string.city_khmelnytskyi);
            case "Zakarpattya":
                return context.getString(R.string.city_zakarpattya);
            case "Zhytomyr":
                return context.getString(R.string.city_zhytomyr);
            case "Kropyvnytskyi":
                return context.getString(R.string.city_kropyvnytskyi);
            case "Mykolaiv":
                return context.getString(R.string.city_mykolaiv);
            case "Chernivtsi":
                return context.getString(R.string.city_chernivtsi);
            case "Lutsk":
                return context.getString(R.string.city_lutsk);
            default:
                return context.getString(R.string.foreign_countries);
        }
    }

    @SuppressLint("Range")
    private void applyCityChange(String city, double startLat, double startLan, String position) {
        String TAG = "applyCityChange";
        Logger.d(context, TAG, "applyCityChange: " + city);

        SQLiteDatabase database = null;
        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

            // Обновляем информацию о городе
            ContentValues cv = new ContentValues();
            cv.put("city", city);
            cv.put("phone", phoneNumber);
            database.update(MainActivity.CITY_INFO, cv, "id = ?", new String[]{"1"});

            // Обновляем позицию
            cv = new ContentValues();
            cv.put("startLat", startLat);
            cv.put("startLan", startLan);
            cv.put("position", position);
            database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?", new String[]{"1"});

            // Обновляем тариф
            cv = new ContentValues();
            cv.put("tarif", " ");
            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?", new String[]{"1"});

            // Обновляем тип оплаты
            cv = new ContentValues();
            cv.put("payment_type", "nal_payment");
            database.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?", new String[]{"1"});

            // Очистка состояний
            sharedPreferencesHelperMain.saveValue("time", "no_time");
            sharedPreferencesHelperMain.saveValue("date", "no_date");
            sharedPreferencesHelperMain.saveValue("comment", "no_comment");
            sharedPreferencesHelperMain.saveValue("tarif", " ");

            double toLatitude = startLat;
            double toLongitude = startLan;
            String finish = position;

            Logger.d(context, TAG, "cangedCity: " + cangedCity);

            if (!cangedCity) {
                Logger.d(context, TAG, "cangedCity: 2" + cangedCity);
                String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                Cursor cr = null;
                try {
                    cr = database.rawQuery(query, null);
                    if (cr.moveToFirst()) {
                        toLatitude = CursorReadHelper.getDouble(cr, "to_lat");
                        toLongitude = CursorReadHelper.getDouble(cr, "to_lng");
                        finish = CursorReadHelper.getString(cr, "finish");
                        Logger.d(context, TAG, "toLongitude1:" + toLongitude);
                        Logger.d(context, TAG, "position1:" + position);
                        Logger.d(context, TAG, "finish1:" + finish);
                    }
                } finally {
                    if (cr != null && !cr.isClosed()) {
                        cr.close();
                    }
                }
            }

            Logger.d(context, TAG, "startLat:" + startLat);
            Logger.d(context, TAG, "startLan:" + startLan);
            Logger.d(context, TAG, "toLatitude:" + toLatitude);
            Logger.d(context, TAG, "toLongitude:" + toLongitude);
            Logger.d(context, TAG, "position:" + position);
            Logger.d(context, TAG, "finish:" + finish);

            // Обновление маршрута
            List<String> settings = new ArrayList<>();
            settings.add(Double.toString(startLat));
            settings.add(Double.toString(startLan));
            settings.add(Double.toString(toLatitude));
            settings.add(Double.toString(toLongitude));
            settings.add(position);
            settings.add(finish);

            updateRoutMarker(settings);
            clearTABLE_SERVICE_INFO();

            sharedPreferencesHelperMain.saveValue("CityCheckActivity", "run");
            AutoLocationAfterCityHelper.markCityLoaded();
            WfpUtils.enqueueCardTokenFetch(context, city);

            // Навигация
            Activity activity = activityRef.get();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
            }

        } catch (Exception e) {
            Logger.e(context, TAG, "Error in applyCityChange: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }
    private void clearTABLE_SERVICE_INFO() {
        String[] arrayServiceCode = DataArr.arrayServiceCode();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            for (int i = 0; i < arrayServiceCode.length; i++) {
                ContentValues cv = new ContentValues();
                cv.put(arrayServiceCode[i], "0");
                database.update(MainActivity.TABLE_SERVICE_INFO, cv, "id = ?",
                        new String[]{"1"});
            }
        } catch (Exception e) {
            Logger.e(context, "clearTABLE_SERVICE_INFO", "Ошибка: " + e.getMessage());
        } finally {
            database.close();
        }
    }

    private void updateRoutMarker(List<String> settings) {
        String TAG = "updateRoutMarker";
        Logger.d(context, TAG, "updateRoutMarker: " + settings.toString());

        SQLiteDatabase database = null;
        try {
            ContentValues cv = new ContentValues();
            cv.put("startLat", Double.parseDouble(settings.get(0)));
            cv.put("startLan", Double.parseDouble(settings.get(1)));
            cv.put("to_lat", Double.parseDouble(settings.get(2)));
            cv.put("to_lng", Double.parseDouble(settings.get(3)));
            cv.put("start", settings.get(4));
            cv.put("finish", settings.get(5));

            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.ROUT_MARKER, cv, "id = ?", new String[]{"1"});
            Logger.d(context, TAG, "Маршрут успешно обновлен");
        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка обновления маршрута: " + e.getMessage());
        } finally {
            if (database != null) database.close();
        }
    }

    public void getPublicIPAddress() {
        getCountryByIP();
    }

    private void getCountryByIP() {
        ApiServiceCountry apiService = com.taxi.easy.ua.utils.ip.RetrofitClient.getClient().create(ApiServiceCountry.class);
        Call<CountryResponse> call = apiService.getCountryByIP("ipAddress");
        call.enqueue(new Callback<CountryResponse>() {
            @Override
            public void onResponse(@NonNull Call<CountryResponse> call, @NonNull Response<CountryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    CountryResponse countryResponse = response.body();
                    Logger.d(context, TAG, "onResponse:countryResponse.getCountry(); " + countryResponse.getCountry());
                    countryState = countryResponse.getCountry();
                } else {
                    countryState = "UA";
                }
                sharedPreferencesHelperMain.saveValue("countryState", countryState);
            }

            @Override
            public void onFailure(@NonNull Call<CountryResponse> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "Error: " + t.getMessage());
                FirebaseCrashlytics.getInstance().recordException(t);
                if (VisicomFragment.progressBar != null) {
                    VisicomFragment.progressBar.setVisibility(GONE);
                }
                sharedPreferencesHelperMain.saveValue("countryState", "UA");
            }
        });
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            c = db.query(table, null, null, null, null, null, null);
            if (c.moveToFirst()) {
                do {
                    for (String cn : c.getColumnNames()) {
                        list.add(CursorReadHelper.getString(c, cn));
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Logger.e(context, "logCursor", "Ошибка: " + e.getMessage());
        } finally {
            if (c != null) c.close();
            if (db != null) db.close();
        }
        return list;
    }
    // Добавьте этот интерфейс в класс CityFinder
    public interface CityCheckCallback {
        void onCityCheckCompleted(boolean cityChanged, boolean userConfirmed);
    }

    // Добавьте этот метод в класс CityFinder
    public void findCityWithCallback(double latitude, double longitude, CityCheckCallback callback) {
        synchronized (lock) {
            if (isProcessing) {
                Logger.d(context, TAG, "CityFinder уже работает, пропускаем вызов");
                if (callback != null) {
                    callback.onCityCheckCompleted(false, false);
                }
                return;
            }
            isProcessing = true;
        }

        CityApiService apiService = RetrofitClient.getClient().create(CityApiService.class);
        Call<CityResponse> call = apiService.findCity(latitude, longitude);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<CityResponse> call, @NonNull Response<CityResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String city = response.body().getCity();
                    Logger.d(context, TAG, "City: " + city);
                    cityVerifyWithCallback(city, callback);
                } else {
                    Logger.e(context, TAG, "Request failed: " + response.code());
                    synchronized (lock) {
                        isProcessing = false;
                    }
                    if (callback != null) {
                        callback.onCityCheckCompleted(false, false);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CityResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.e(context, TAG, "Error: " + t.getMessage());
                synchronized (lock) {
                    isProcessing = false;
                }
                if (callback != null) {
                    callback.onCityCheckCompleted(false, false);
                }
            }
        });
    }

    private void cityVerifyWithCallback(String cityFromApi, CityCheckCallback callback) {
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastProcessedTime < PROCESSING_TIMEOUT &&
                    cityFromApi.equals(lastProcessedCity)) {
                Logger.d(context, TAG, "Слишком частые вызовы для одного города, пропускаем");
                isProcessing = false;
                if (callback != null) {
                    callback.onCityCheckCompleted(false, false);
                }
                return;
            }
            lastProcessedTime = currentTime;
            lastProcessedCity = cityFromApi;
        }

        String cityResult = mapCityFromApi(cityFromApi);

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String currentCityFromDB = stringList.get(1);

        String normalizedCityResult = normalizeCityName(cityResult);
        String normalizedCurrentCity = normalizeCityName(currentCityFromDB);

        boolean cityChanged = !normalizedCityResult.equals(normalizedCurrentCity);

        // ✅ Используем final массивы или объекты для хранения результата
        final boolean[] userConfirmed = {false};

        if (cityChanged) {
            Logger.d(context, TAG, "City отличается:11 " + currentCityFromDB + " -> " + cityResult);

            // Настройка параметров для нового города
            setupCityParameters(cityResult);


            // Показываем диалог
            if (isCityChangeDialogShowing) {
                Logger.d(context, TAG, "Диалог уже показывается, пропускаем");
                synchronized (lock) {
                    isProcessing = false;
                }
                if (callback != null) {
                    callback.onCityCheckCompleted(true, false);
                }
                return;
            }

            isCityChangeDialogShowing = true;
            String cityDisplayName = getCityDisplayName(cityResult);

            Activity activity = activityRef.get();
            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                new androidx.appcompat.app.AlertDialog.Builder(activity)
                        .setTitle(R.string.city_change_dialog)
                        .setMessage(activity.getString(R.string.find_new_city_mes) + cityDisplayName + activity.getString(R.string.turn_mes))
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            isCityChangeDialogShowing = false;
                            userConfirmed[0] = true;  // ✅ Устанавливаем значение в массиве
                            AutoLocationAfterCityHelper.markCityChangedViaGeo();
                            applyCityChange(cityResult, startLat, startLan, position);
                            synchronized (lock) {
                                isProcessing = false;
                            }
                            if (callback != null) {
                                callback.onCityCheckCompleted(true, userConfirmed[0]);
                            }
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            isCityChangeDialogShowing = false;
                            userConfirmed[0] = false;  // ✅ Устанавливаем значение в массиве
                            synchronized (lock) {
                                isProcessing = false;
                            }
                            VisicomFragment.updateGpsButtonCross(true);
                            VisicomFragment.btnStaticVisible(View.VISIBLE);

                            if (callback != null) {
                                callback.onCityCheckCompleted(true, userConfirmed[0]);
                            }
                        })
                        .setOnDismissListener(dialog -> {
                            isCityChangeDialogShowing = false;
                            synchronized (lock) {
                                isProcessing = false;
                            }
                        })
                        .setCancelable(false)
                        .show();
            } else {
                synchronized (lock) {
                    isProcessing = false;
                }
                if (callback != null) {
                    callback.onCityCheckCompleted(true, false);
                }
            }
        } else {
            Logger.d(context, TAG, "Города одинаковые - диалог НЕ показываем");
            if (VisicomFragment.progressBar != null) {
                VisicomFragment.progressBar.setVisibility(View.GONE);
            }
            synchronized (lock) {
                isProcessing = false;
            }
            if (callback != null) {
                callback.onCityCheckCompleted(false, false);
            }
        }
    }

    // Вынесите эту логику в отдельный метод
    private String mapCityFromApi(String cityFromApi) {
        switch (cityFromApi) {
            case "city_kiev": return "Kyiv City";
            case "city_cherkassy": return "Cherkasy Oblast";
            case "city_odessa": return "Odessa";
            case "city_zaporizhzhia": return "Zaporizhzhia";
            case "city_dnipro": return "Dnipropetrovsk Oblast";
            case "city_lviv": return "Lviv";
            case "city_ivano_frankivsk": return "Ivano_frankivsk";
            case "city_vinnytsia": return "Vinnytsia";
            case "city_poltava": return "Poltava";
            case "city_sumy": return "Sumy";
            case "city_kharkiv": return "Kharkiv";
            case "city_chernihiv": return "Chernihiv";
            case "city_rivne": return "Rivne";
            case "city_ternopil": return "Ternopil";
            case "city_khmelnytskyi": return "Khmelnytskyi";
            case "city_zakarpattya": return "Zakarpattya";
            case "city_zhytomyr": return "Zhytomyr";
            case "city_kropyvnytskyi": return "Kropyvnytskyi";
            case "city_mykolaiv": return "Mykolaiv";
            case "city_chernivtsi": return "Chernivtsi";
            case "city_lutsk": return "Lutsk";
            default: return "all";
        }
    }

    // Вынесите настройку параметров города в отдельный метод
    private void setupCityParameters(String cityResult) {
        String Kyiv_City_phone = "tel:0674443804";
        String Dnipropetrovsk_Oblast_phone = "tel:0667257070";
        String Odessa_phone = "tel:0737257070";
        String Zaporizhzhia_phone = "tel:0687257070";
        String Cherkasy_Oblast_phone = "tel:0962294243";

        switch (cityResult) {
            case "Kyiv City":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_kyiv);
                countryState = "UA";
                break;
            case "Dnipropetrovsk Oblast":
                phoneNumber = Dnipropetrovsk_Oblast_phone;
                cityMenu = context.getString(R.string.city_dnipro);
                countryState = "UA";
                break;
            case "Odessa":
                phoneNumber = Odessa_phone;
                cityMenu = context.getString(R.string.city_odessa);
                countryState = "UA";
                break;
            case "Zaporizhzhia":
                phoneNumber = Zaporizhzhia_phone;
                cityMenu = context.getString(R.string.city_zaporizhzhia);
                countryState = "UA";
                break;
            case "Cherkasy Oblast":
                phoneNumber = Cherkasy_Oblast_phone;
                cityMenu = context.getString(R.string.city_cherkassy);
                countryState = "UA";
                break;
            case "Lviv":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_lviv);
                countryState = "UA";
                break;
            case "Ivano_frankivsk":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_ivano_frankivsk);
                countryState = "UA";
                break;
            case "Vinnytsia":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_vinnytsia);
                countryState = "UA";
                break;
            case "Poltava":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_poltava);
                countryState = "UA";
                break;
            case "Sumy":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_sumy);
                countryState = "UA";
                break;
            case "Kharkiv":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_kharkiv);
                countryState = "UA";
                break;
            case "Chernihiv":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_chernihiv);
                countryState = "UA";
                break;
            case "Rivne":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_rivne);
                countryState = "UA";
                break;
            case "Ternopil":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_ternopil);
                countryState = "UA";
                break;
            case "Khmelnytskyi":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_khmelnytskyi);
                countryState = "UA";
                break;
            case "Zakarpattya":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_zakarpattya);
                countryState = "UA";
                break;
            case "Zhytomyr":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_zhytomyr);
                countryState = "UA";
                break;
            case "Kropyvnytskyi":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_kropyvnytskyi);
                countryState = "UA";
                break;
            case "Mykolaiv":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_mykolaiv);
                countryState = "UA";
                break;
            case "Chernivtsi":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_chernivtsi);
                countryState = "UA";
                break;
            case "Lutsk":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.city_lutsk);
                countryState = "UA";
                break;
            case "OdessaTest":
                phoneNumber = Kyiv_City_phone;
                cityMenu = "Test";
                countryState = "UA";
                break;
            case "foreign countries":
                phoneNumber = Kyiv_City_phone;
                cityMenu = context.getString(R.string.foreign_countries);
                break;
            default:
                phoneNumber = Kyiv_City_phone;
                getPublicIPAddress();
                cityMenu = context.getString(R.string.city_kyiv);
                countryState = "UA";
                break;
        }
    }
}