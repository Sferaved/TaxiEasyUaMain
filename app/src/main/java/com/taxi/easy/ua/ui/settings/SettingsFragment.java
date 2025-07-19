package com.taxi.easy.ua.ui.settings;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentSettingsBinding;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        FragmentSettingsBinding binding = FragmentSettingsBinding.inflate(inflater, container, false);

        View root = binding.getRoot();
        if(button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }
        // Настройка выпадающего списка
        Spinner languageSpinner = binding.languageSpinner;
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.languages,
                R.layout.custom_spinner_item // Кастомный макет для текущего элемента
        );
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item); // Кастомный макет для выпадающего списка
        languageSpinner.setAdapter(adapter);


        // Получение текущего языка из SharedPreferences
        String currentLocale = (String) sharedPreferencesHelperMain.getValue("locale", Locale.getDefault().toString());
        Logger.i(requireContext(), TAG, String.valueOf(currentLocale));
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
        binding.saveButton.setOnClickListener(view -> {
            String selectedLanguage = languageSpinner.getSelectedItem().toString();
            setLocale(selectedLanguage);
        });

        return root;
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = requireContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        Logger.i(requireContext(), TAG, "locale Code" +  localeCode);
        // Сохранение нового языка
        sharedPreferencesHelperMain.saveValue("locale", localeCode);

        // Установка локали
        Locale locale = new Locale(localeCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(getResources().getConfiguration());
        config.setLocale(locale);

        // Перезапуск приложения для применения локали
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}

