package com.taxi.easy.ua.utils.helpers;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import java.util.Locale;

public class LocaleHelper {

    public static String getLocale() {
        String localeCode = (String) sharedPreferencesHelperMain.getValue("locale", Locale.getDefault().toString());
        return localeCode.split("_")[0];
    }
}
