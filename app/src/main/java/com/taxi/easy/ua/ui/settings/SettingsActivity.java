package com.taxi.easy.ua.ui.settings;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

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

