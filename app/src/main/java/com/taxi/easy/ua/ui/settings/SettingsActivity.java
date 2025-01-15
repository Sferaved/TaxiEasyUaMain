package com.taxi.easy.ua.ui.settings;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetCityFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private String city;
    private String newTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Настройка выпадающего списка
        Spinner languageSpinner = findViewById(R.id.language_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.languages,
                R.layout.custom_spinner_item // Кастомный макет для текущего элемента
        );
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item); // Кастомный макет для выпадающего списка
        languageSpinner.setAdapter(adapter);


        // Получение текущего языка из SharedPreferences
        String currentLocale = (String) sharedPreferencesHelperMain.getValue("locale", "uk");
        Logger.i(this, "locale currentLocale", String.valueOf(currentLocale));
        // Установка текущего языка в Spinner


        switch (currentLocale) {
            case "en":
                languageSpinner.setSelection(0);
                break;
            case "ru":
                languageSpinner.setSelection(1);
                break;
            default:
                languageSpinner.setSelection(2);
        }

        // Обработчик кнопки сохранения
        findViewById(R.id.save_button).setOnClickListener(view -> {
            String selectedLanguage = languageSpinner.getSelectedItem().toString();
            setLocale(selectedLanguage);
        });
    }
    private void applyLocale(String localeCode) {
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    @Override
    protected void onResume() {
        super.onResume();
        String localeCode = (String) sharedPreferencesHelperMain.getValue("locale", "uk");
        Logger.i(this, "locale", localeCode);
        // Установка локали
        applyLocale(localeCode);

        // Устанавливаем Action Bar, если он доступен
        if (getSupportActionBar() != null) {
            // Устанавливаем пользовательский макет в качестве заголовка Action Bar
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Отключаем стандартный заголовок
            getSupportActionBar().setCustomView(R.layout.custom_action_bar_title);

            // Доступ к TextView в пользовательском заголовке
            View customView = getSupportActionBar().getCustomView();
            TextView titleTextView = customView.findViewById(R.id.action_bar_title);

            setCityAppbar();

            titleTextView.setText(newTitle);
            // Установка обработчика нажатий
            titleTextView.setOnClickListener(v -> {
                Logger.d(this, TAG, " Установка обработчика нажатий" + NetworkUtils.isNetworkAvailable(getApplicationContext()));
                if (NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                    // Ваш код при нажатии на заголовок
                    MyBottomSheetCityFragment bottomSheetDialogFragment = new MyBottomSheetCityFragment(city, SettingsActivity.this);
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                }

            });
        }
    }
    private void setCityAppbar()
    {
        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        city = stringList.get(1);
        String cityMenu;
        switch (city){
            case "Kyiv City":
                cityMenu = getString(R.string.city_kyiv);
                break;
            case "Dnipropetrovsk Oblast":
                cityMenu = getString(R.string.city_dnipro);
                break;
            case "Odessa":
                cityMenu = getString(R.string.city_odessa);
                break;
            case "Zaporizhzhia":
                cityMenu = getString(R.string.city_zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                cityMenu = getString(R.string.city_cherkassy);
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
            case "Сhernivtsi":
                cityMenu = getString(R.string.city_chernivtsi);
                break;
            case "Lutsk":
                cityMenu = getString(R.string.city_lutsk);
                break;
            case "OdessaTest":
                cityMenu = "Test";
                break;
            default:
                cityMenu = getString(R.string.foreign_countries);
        }
        newTitle =  getString(R.string.menu_city) + " " + cityMenu;
        sharedPreferencesHelperMain.saveValue("newTitle", newTitle);
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = db.query(table, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            String str;
            do {
                str = "";
                for (String cn : c.getColumnNames()) {
                    str = str.concat(cn + " = " + c.getString(c.getColumnIndex(cn)) + "; ");
                    list.add(c.getString(c.getColumnIndex(cn)));

                }

            } while (c.moveToNext());
        }
        db.close();
        return list;
    }
    private void setLocale(String selectedLanguage) {
        // Определение кода локали
        String localeCode;
        if (selectedLanguage.equals(getString(R.string.language_en))) {
            localeCode = "en";
        } else if (selectedLanguage.equals(getString(R.string.language_ru))) {
            localeCode = "ru";
        } else if (selectedLanguage.equals(getString(R.string.language_uk))) {
            localeCode = "uk";
        } else {
            localeCode = "en"; // Язык по умолчанию
        }
        Logger.i(this, "locale Code", localeCode);
        // Сохранение нового языка
         sharedPreferencesHelperMain.saveValue("locale", localeCode);

        // Установка локали
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());

        // Перезапуск приложения для применения локали
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


}

