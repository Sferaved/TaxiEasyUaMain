package com.taxi.easy.ua.utils;

import java.util.Locale;

public class LocaleHelper {

    public static String getLocale() {
        return Locale.getDefault().getLanguage();
    }
}
