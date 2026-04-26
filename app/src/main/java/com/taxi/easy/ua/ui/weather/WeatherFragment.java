package com.taxi.easy.ua.ui.weather;

import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.androidx.startup.MyApplication.getCurrentActivity;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentWeatherBinding;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.uxcam.UXCam;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WeatherFragment extends Fragment {

    private static final String TAG = "WeatherFragment";
    private String API_KEY; // Замените на ваш API ключ
    //    https://api.openweathermap.org/data/2.5/weather?q=Kyiv&appid=f5790978f87a638e2eee88a858c03ec4&units=metric
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";

    private FragmentWeatherBinding binding;
    private ProgressBar progressBar;
    private RecyclerView recyclerForecast;
    private WeatherAdapter weatherAdapter;
    private TextView tvCityName, tvDate, tvTemperature, tvWeatherDescription;
    private TextView tvHumidity, tvWind, tvPressure;
    private ImageView ivWeatherIcon;
    private Button btnWeatherDetails;
    private Button btnShowMap;
    private FloatingActionButton fabUpdate;
    private View emptyState;

    private List<WeatherResponse.ForecastItem> forecastList = new ArrayList<>();
    private Context context;

    private WeatherApiService weatherApiService;
    FirestoreHelper firestoreHelper;
    SharedPreferences prefs;
    public interface WeatherApiService {
        @GET("weather")
        Call<WeatherResponse> getCurrentWeather(
                @Query("q") String cityName,
                @Query("appid") String apiKey,
                @Query("units") String units,
                @Query("lang") String lang
        );

        @GET("forecast")
        Call<WeatherResponse> getForecast(
                @Query("q") String cityName,
                @Query("appid") String apiKey,
                @Query("units") String units,
                @Query("lang") String lang
        );
    }

    public WeatherFragment() {
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        UXCam.tagScreenName(TAG);

        if (button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }

        binding = FragmentWeatherBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        context = requireActivity();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // ИНИЦИАЛИЗАЦИЯ FirestoreHelper и SharedPreferences
        firestoreHelper = new FirestoreHelper(context);
        prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        API_KEY = getApiKey();
        initViews();
        setupRetrofit();
        setupClickListeners();

        checkNetworkAndLoadWeather();

        return root;
    }
    private String getApiKey() {
        // Сначала проверяем в MainActivity
        if (MainActivity.weatherKey != null && !MainActivity.weatherKey.isEmpty()) {
            return MainActivity.weatherKey;
        }

        // Затем проверяем в SharedPreferences
        if (prefs != null) {
            String savedKey = prefs.getString("weather_api_key", null);
            if (savedKey != null && !savedKey.isEmpty()) {
                return savedKey;
            }
        }

        // Если ключа нет, загружаем из Firestore
        weatherKeyFromFb();

        // Возвращаем пустую строку, пока ключ загружается
        return "";
    }
    private void weatherKeyFromFb() {
        // Проверяем, что firestoreHelper не null
        if (firestoreHelper == null) {
            firestoreHelper = new FirestoreHelper(context);
        }

        firestoreHelper.getWeatherKey(new FirestoreHelper.OnVisicomKeyFetchedListener() {
            @Override
            public void onSuccess(String vKey) {
                if (vKey != null && !vKey.isEmpty()) {
                    MainActivity.weatherKey = vKey;
                    if (prefs != null) {
                        prefs.edit().putString("weather_api_key", vKey).apply();
                    }
                    Logger.d(context, TAG, "weatherKey получен: " + vKey);

                    // После получения ключа перезагружаем погоду
                    if (isAdded() && NetworkUtils.isNetworkAvailable(requireContext())) {
                        loadWeatherData();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                Logger.e(context, TAG, "Ошибка получения ключа: " + e.getMessage());
                showErrorState();
            }
        });
    }
    private void initViews() {
        progressBar = binding.progressBar;
        recyclerForecast = binding.recyclerForecast;
        ViewCompat.setNestedScrollingEnabled(recyclerForecast, false);
        tvCityName = binding.tvCityName;
        tvDate = binding.tvDate;
        tvTemperature = binding.tvTemperature;
        tvWeatherDescription = binding.tvWeatherDescription;
        tvHumidity = binding.tvHumidity;
        tvWind = binding.tvWind;
        tvPressure = binding.tvPressure;
        ivWeatherIcon = binding.ivWeatherIcon;
        btnWeatherDetails = binding.btnWeatherDetails;
        btnShowMap = binding.btnShowMap;

        emptyState = binding.emptyState;

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        recyclerForecast.setLayoutManager(new LinearLayoutManager(context));
        weatherAdapter = new WeatherAdapter(context, forecastList);
        recyclerForecast.setAdapter(weatherAdapter);
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        weatherApiService = retrofit.create(WeatherApiService.class);
    }

    private void setupClickListeners() {
        btnWeatherDetails.setOnClickListener(v -> showWeatherDetails());
        btnShowMap.setOnClickListener(v -> showWeatherMap());
        binding.currentWeatherCard.setOnClickListener(v -> showOrderScreen());
        binding.btnRefresh.setOnClickListener(v -> refreshWeather());
    }

    private void showOrderScreen() {
        sharedPreferencesHelperMain.saveValue("time", "no_time");
        sharedPreferencesHelperMain.saveValue("date", "no_date");
        NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
        navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_visicom, true)
                .build());
    }

    private void checkNetworkAndLoadWeather() {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            if (isAdded()) {
                navigateToRestart();
            }
        } else {
            loadWeatherData();
        }
    }

    private void loadWeatherData() {
        progressBar.setVisibility(View.VISIBLE);
        showEmptyState(false);

        String city = getCityFromDatabase();
        if (city.isEmpty()) {
            city = "Kyiv";
        }

        fetchCurrentWeather(city);
        fetchForecast(city);
    }

    private void fetchCurrentWeather(String city) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            Logger.e(context, TAG, "API_KEY пуст, ждем загрузки из Firestore");
            showErrorState();
            checkAndHideProgress();
            return;
        }

        Logger.e(context, TAG, "fetchCurrentWeather API_KEY: " + API_KEY);
        String localCode = sharedPreferencesHelperMain.getValue("locale", "uk").toString();
        Call<WeatherResponse> call = weatherApiService.getCurrentWeather(
                city, API_KEY, "metric", localCode
        );

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateCurrentWeatherUI(response.body());
                } else {
                    Logger.e(context, TAG, "Failed to fetch weather: " + response.code());
                    showErrorState();
                }
                checkAndHideProgress();
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.e(context, TAG, "Network error: " + t.getMessage());
                showErrorState();
                checkAndHideProgress();
            }
        });
    }

    private void fetchForecast(String city) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            Logger.e(context, TAG, "API_KEY пуст, ждем загрузки из Firestore");
            showErrorState();
            checkAndHideProgress();
            return;
        }
        Logger.e(context, TAG, "fetchCurrentWeather API_KEY: " + API_KEY);
        String localCode = sharedPreferencesHelperMain.getValue("locale", "uk").toString();

        Call<WeatherResponse> call = weatherApiService.getForecast(
                city, API_KEY, "metric", localCode
        );

        call.enqueue(new Callback<>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null && response.body().getForecastList() != null) {
                    List<WeatherResponse.ForecastItem> allForecasts = response.body().getForecastList();
                    forecastList.clear();

                    // Берем прогноз на каждый день в 12:00
                    for (int i = 0; i < allForecasts.size(); i++) {
                        WeatherResponse.ForecastItem item = allForecasts.get(i);
                        if (item.getDtTxt().contains("12:00:00") && forecastList.size() < 5) {
                            forecastList.add(item);
                        }
                    }

                    weatherAdapter.notifyDataSetChanged();
                    updateUI();
                } else {
                    showErrorState();
                }
                checkAndHideProgress();
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                FirebaseCrashlytics.getInstance().recordException(t);
                Logger.e(context, TAG, "Forecast network error: " + t.getMessage());
                showErrorState();
                checkAndHideProgress();
            }
        });
    }

    private void updateCurrentWeatherUI(WeatherResponse weather) {

        String cityName = getCityFromDatabase();

        tvCityName.setText(cityName);
//        tvCityName.setText(weather.getName());
        String localCode = sharedPreferencesHelperMain.getValue("locale", "uk").toString();
        // Дата
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM, EEEE", new Locale(localCode));
        tvDate.setText(dateFormat.format(new Date()));

        // Температура
        int temp = (int) Math.round(weather.getMain().getTemp());
        tvTemperature.setText(temp + "°C");

        // Описание
        if (weather.getWeather() != null && !weather.getWeather().isEmpty()) {
            String description = weather.getWeather().get(0).getDescription();
            tvWeatherDescription.setText(capitalizeFirstLetter(description));

            // Иконка
            String iconCode = weather.getWeather().get(0).getIcon();
            ivWeatherIcon.setImageResource(getWeatherIcon(iconCode));
        }

        // Влажность
        tvHumidity.setText(weather.getMain().getHumidity() + "%");

        // Ветер
        double windSpeed = weather.getWind().getSpeed();
        String speed = windSpeed + " " + context.getString(R.string.speed);
        tvWind.setText(speed);

        // Давление (из гПа в мм рт. ст.)
        int pressureMmHg = (int) (weather.getMain().getPressure() * 0.750064);
        String pressure_high = pressureMmHg + " " + context.getString(R.string.pressure_high);
        tvPressure.setText(pressure_high);
    }

    private void updateUI() {
        if (forecastList.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            binding.forecastTitleText.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerForecast.setVisibility(View.GONE);
            binding.forecastTitleText.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerForecast.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorState() {
        showEmptyState(true);
        Toast.makeText(context, R.string.weather_load_error, Toast.LENGTH_SHORT).show();
    }

    private void checkAndHideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    private void refreshWeather() {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            loadWeatherData();
        } else {
            Toast.makeText(context, R.string.no_connection, Toast.LENGTH_SHORT).show();
        }
    }


    private void showWeatherDetails() {
        String methodTag = "showWeatherDetails";

        try {
            String city = getCityFromDatabase();
            Logger.d(context, methodTag, "Город: " + city);

            if (city != null && !city.isEmpty()) {
                String url = "https://www.google.com/search?q=" +
                        java.net.URLEncoder.encode("погода " + city, "UTF-8");

                Logger.d(context, methodTag, "URL: " + url);

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);

                Logger.d(context, methodTag, "Браузер открыт успешно");
                Toast.makeText(context, "Відкриваю прогноз для " + city, Toast.LENGTH_SHORT).show();
            } else {
                Logger.e(context, methodTag, "Город не найден");
                Toast.makeText(context, "Не вдалося визначити місто", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Logger.e(context, methodTag, "Ошибка: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(context, "Помилка відкриття прогнозу", Toast.LENGTH_SHORT).show();
        }
    }

    private void showWeatherMap() {
        String TAG = "showWeatherMap";

        try {
            Logger.d(context, TAG, "showWeatherMap() вызван");

            String city = getCityFromDatabase();
            Logger.d(context, TAG, "Город: '" + city + "'");

            if (!city.isEmpty()) {
                String searchQuery = "карта погоди " + city;
                String encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8");
                String url = "https://www.google.com/search?q=" + encodedQuery;

                Logger.d(context, TAG, "URL: " + url);

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);

                Logger.d(context, TAG, "Карта погоды открыта для: " + city);
                Toast.makeText(context, "Відкриваю карту погоди для " + city, Toast.LENGTH_SHORT).show();
            } else {
                Logger.e(context, TAG, "Город пустой");
                Toast.makeText(context, "Не вдалося визначити місто", Toast.LENGTH_SHORT).show();
            }



        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(context, "Помилка при відкритті карти", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void navigateToRestart() {
        if (getActivity() != null) {
            Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
            Logger.w(context, TAG, "NO INTERNET - Showing toast message");
         }
    }

    private String getCityFromDatabase() {
        // Получаем город из вашей БД как в HistoryFragment


        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireContext());
        String city = stringList.get(1);
        String cityMenu;
        switch (city){
            case "Kyiv City":
                cityMenu =  getString(R.string.Kyiv_city);
                break;
            case "Dnipropetrovsk Oblast":
                cityMenu = getString(R.string.Dnipro_city);
                break;
            case "Odessa":
            case "OdessaTest":
                cityMenu = getString(R.string.city_odessa);
                break;
            case "Zaporizhzhia":
                cityMenu = getString(R.string.city_zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                cityMenu = getString(R.string.Cherkasy);
                break;
            case "Lviv":
                cityMenu = getString(R.string.city_lviv);
                break;
            case "Ivano_frankivsk":
                cityMenu = getString(R.string.city_ivano_frankivsk);
                break;
            case "Vinnytsia":
                cityMenu = getString(R.string.city_vinnytsia);
                break;
            case "Poltava":
                cityMenu = getString(R.string.city_poltava);
                break;
            case "Sumy":
                cityMenu = getString(R.string.city_sumy);
                break;
            case "Kharkiv":
                cityMenu = getString(R.string.city_kharkiv);
                break;
            case "Chernihiv":
                cityMenu = getString(R.string.city_chernihiv);
                break;
            case "Rivne":
                cityMenu = getString(R.string.city_rivne);
                break;
            case "Ternopil":
                cityMenu = getString(R.string.city_ternopil);
                break;
            case "Khmelnytskyi":
                cityMenu = getString(R.string.city_khmelnytskyi);
                break;
            case "Zakarpattya":
                cityMenu = getString(R.string.city_zakarpattya);
                break;
            case "Zhytomyr":
                cityMenu = getString(R.string.city_zhytomyr);
                break;
            case "Kropyvnytskyi":
                cityMenu = getString(R.string.city_kropyvnytskyi);
                break;
            case "Mykolaiv":
                cityMenu = getString(R.string.city_mykolaiv);
                break;
            case "Chernivtsi":
                cityMenu = getString(R.string.city_chernivtsi);
                break;
            case "Lutsk":
                cityMenu = getString(R.string.city_lutsk);
                break;
            default:
                cityMenu = getString(R.string.Kyiv_city);
        }


//        return getCityName(city);
        return  cityMenu;
    }


    private String getCityIdForWeather() {
        // Получаем город из базы данных как в getCityFromDatabase()
        List<String> stringList = logCursor(MainActivity.CITY_INFO, requireContext());
        String city = stringList.get(1); // Получаем код города из БД

        String cityMenu;
        switch (city) {
            case "Kyiv City":
                cityMenu = getString(R.string.Kyiv_city);
                return "703448"; // ID для Киева

            case "Dnipropetrovsk Oblast":
                cityMenu = getString(R.string.Dnipro_city);
                return "709930"; // ID для Днепра

            case "Odessa":
            case "OdessaTest":
                cityMenu = getString(R.string.city_odessa);
                return "698740"; // ID для Одессы

            case "Zaporizhzhia":
                cityMenu = getString(R.string.city_zaporizhzhia);
                return "687700"; // ID для Запорожья

            case "Cherkasy Oblast":
                cityMenu = getString(R.string.Cherkasy);
                return "710791"; // ID для Черкасс

            case "Lviv":
                cityMenu = getString(R.string.city_lviv);
                return "702550"; // ID для Львова

            case "Ivano_frankivsk":
                cityMenu = getString(R.string.city_ivano_frankivsk);
                return "707470"; // ID для Ивано-Франковска

            case "Vinnytsia":
                cityMenu = getString(R.string.city_vinnytsia);
                return "689558"; // ID для Винницы

            case "Poltava":
                cityMenu = getString(R.string.city_poltava);
                return "696643"; // ID для Полтавы

            case "Sumy":
                cityMenu = getString(R.string.city_sumy);
                return "692194"; // ID для Сум

            case "Kharkiv":
                cityMenu = getString(R.string.city_kharkiv);
                return "706483"; // ID для Харькова

            case "Chernihiv":
                cityMenu = getString(R.string.city_chernihiv);
                return "710734"; // ID для Чернигова

            case "Rivne":
                cityMenu = getString(R.string.city_rivne);
                return "695594"; // ID для Ровно

            case "Ternopil":
                cityMenu = getString(R.string.city_ternopil);
                return "691650"; // ID для Тернополя

            case "Khmelnytskyi":
                cityMenu = getString(R.string.city_khmelnytskyi);
                return "706369"; // ID для Хмельницкого

            case "Zakarpattya":
                cityMenu = getString(R.string.city_zakarpattya);
                return "690548"; // ID для Ужгорода (Закарпатье)

            case "Zhytomyr":
                cityMenu = getString(R.string.city_zhytomyr);
                return "686967"; // ID для Житомира

            case "Kropyvnytskyi":
                cityMenu = getString(R.string.city_kropyvnytskyi);
                return "705812"; // ID для Кропивницкого

            case "Mykolaiv":
                cityMenu = getString(R.string.city_mykolaiv);
                return "700569"; // ID для Николаева

            case "Chernivtsi":
                cityMenu = getString(R.string.city_chernivtsi);
                return "710719"; // ID для Черновцов

            case "Lutsk":
                cityMenu = getString(R.string.city_lutsk);
                return "702569"; // ID для Луцка

            default:
                cityMenu = getString(R.string.Kyiv_city);
                return "703448"; // Киев по умолчанию
        }
    }

    private int getWeatherIcon(String iconCode) {
        switch (iconCode) {
            case "01d": case "01n": return R.drawable.ic_clear_sky;
            case "02d": case "02n": return R.drawable.ic_few_clouds;
            case "03d": case "03n": case "04d": case "04n": return R.drawable.ic_broken_clouds;
            case "09d": case "09n": return R.drawable.ic_shower_rain;
            case "10d": case "10n": return R.drawable.ic_rain;
            case "11d": case "11n": return R.drawable.ic_thunderstorm;
            case "13d": case "13n": return R.drawable.ic_snow;
            case "50d": case "50n": return R.drawable.ic_mist;
            default: return R.drawable.ic_weather_default;
        }
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        android.database.sqlite.SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);

        try (android.database.Cursor c = database.query(table, null, null, null, null, null, null)) {
            if (c.moveToFirst()) {
                do {
                    for (String cn : c.getColumnNames()) {
                        list.add(c.getString(c.getColumnIndex(cn)));
                    }
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Logger.e(context, TAG, "Error reading from database: " + e.getMessage());
        } finally {
            database.close();
        }
        return list;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}