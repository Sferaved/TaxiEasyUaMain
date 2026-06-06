package com.taxi.easy.ua.ui.weather;

import static com.taxi.easy.ua.androidx.startup.MyApplication.getCurrentActivity;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.log.Logger;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.ViewHolder> {

    private static final String TAG = "WeatherAdapter";
    private final Context context;
    private final List<WeatherDailyForecast> forecastList;

    String localCode = sharedPreferencesHelperMain.getValue("locale", "uk").toString();
    private final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale(localCode));
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", new Locale(localCode));

    public WeatherAdapter(Context context, List<WeatherDailyForecast> forecastList) {
        this.context = context;
        this.forecastList = forecastList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_forecast, parent, false);
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WeatherDailyForecast daily = forecastList.get(position);
        WeatherResponse.ForecastItem forecast = daily.getRepresentativeItem();

        try {
            Date date = inputFormat.parse(daily.getDayKey());
            if (date != null) {
                holder.tvDay.setText(dayFormat.format(date));
                holder.tvDate.setText(dateFormat.format(date));
            }
        } catch (Exception e) {
            holder.tvDay.setText(daily.getDayKey());
        }

        holder.tvTempMax.setText(formatTemp(daily.getTempMax()));
        holder.tvTempMin.setText(formatTemp(daily.getTempMin()));
        holder.ivWeatherIcon.setImageResource(getWeatherIcon(daily.getIconCode()));
        holder.tvDescription.setText(capitalizeFirstLetter(daily.getDescription()));
        holder.tvHumidity.setText(daily.getHumidity() + "%");

        if (daily.getMaxPopPercent() > 0) {
            holder.tvPop.setVisibility(View.VISIBLE);
            holder.tvPop.setText(context.getString(R.string.weather_pop, daily.getMaxPopPercent()));
        } else {
            holder.tvPop.setVisibility(View.GONE);
        }

        holder.ivOrderAction.setOnClickListener(v -> newOrderFromWeatherScreen(forecast));
    }

    private String formatTemp(int temp) {
        if (temp > 0) {
            return "+" + temp + "°";
        }
        return temp + "°";
    }

    private void newOrderFromWeatherScreen(WeatherResponse.ForecastItem forecast) {
        try {
            Date forecastDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .parse(forecast.getDtTxt());

            if (forecastDate != null) {
                LocalDate forecastLocalDate = forecastDate.toInstant()
                        .atZone(ZoneId.of("Europe/Kiev"))
                        .toLocalDate();

                LocalDate currentLocalDate = LocalDate.now(ZoneId.of("Europe/Kiev"));

                if (forecastLocalDate.equals(currentLocalDate)) {
                    sharedPreferencesHelperMain.saveValue("time", "no_time");
                    sharedPreferencesHelperMain.saveValue("date", "no_date");
                    Logger.d(context, TAG, "Текущий день (" + forecastLocalDate + ") - просто переход без сохранения");
                } else {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    String forecastDateOnly = forecastLocalDate.format(dateFormatter);

                    SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    Calendar ukraineCalendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Kiev"));
                    String currentTime = timeFormat.format(ukraineCalendar.getTime());

                    sharedPreferencesHelperMain.saveValue("date", forecastDateOnly);
                    sharedPreferencesHelperMain.saveValue("time", currentTime);

                    Logger.d(context, TAG, "Дата из прогноза: " + forecastDateOnly);
                    Logger.d(context, TAG, "Исходная строка прогноза: " + forecast.getDtTxt());
                    Logger.d(context, TAG, "Текущее украинское время: " + currentTime);
                    Logger.d(context, TAG, "Текущая дата: " + currentLocalDate);
                }

                NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());

            } else {
                Logger.e(context, TAG, "Ошибка: дата прогноза null");
                Toast.makeText(context, "Не удалось получить дату", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Logger.e(context, TAG, "Ошибка в newOrderFromWeatherScreen: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            Toast.makeText(context, "Ошибка при переходе к заказу", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return forecastList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvDate, tvTempMax, tvTempMin, tvDescription, tvHumidity, tvPop;
        ImageView ivWeatherIcon, ivOrderAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tv_day);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTempMax = itemView.findViewById(R.id.tv_temp_max);
            tvTempMin = itemView.findViewById(R.id.tv_temp_min);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvHumidity = itemView.findViewById(R.id.tv_humidity);
            tvPop = itemView.findViewById(R.id.tv_pop);
            ivWeatherIcon = itemView.findViewById(R.id.iv_weather_icon);
            ivOrderAction = itemView.findViewById(R.id.iv_order_action);
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
}
