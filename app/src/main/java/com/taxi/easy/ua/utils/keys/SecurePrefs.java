package com.taxi.easy.ua.utils.keys;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class SecurePrefs {
    private static final String PREFS_NAME = "secure_prefs";
    private static final String KEY_API_KEY = "api_key";

    // ДОБАВИТЬ ЭТУ СТРОКУ - кеш для ключа
    private static String cachedKey = null;

    /**
     * Сохраняет ключ в EncryptedSharedPreferences.
     */
    public static void saveKey(Context context, String key) throws GeneralSecurityException, IOException {
        SharedPreferences prefs = getEncryptedSharedPreferences(context);
        prefs.edit().putString(KEY_API_KEY, key).apply();
        cachedKey = key;  // ДОБАВИТЬ ЭТУ СТРОКУ - обновляем кеш
    }

    /**
     * Извлекает ключ из EncryptedSharedPreferences.
     */
    public static String getKey(Context context) throws GeneralSecurityException, IOException {
        // ДОБАВИТЬ ЭТИ 3 СТРОКИ - проверка кеша
        if (cachedKey != null) {
            return cachedKey;
        }

        SharedPreferences prefs = getEncryptedSharedPreferences(context);
        cachedKey = prefs.getString(KEY_API_KEY, null);  // ИЗМЕНИТЬ - сохранить в кеш
        return cachedKey;  // ИЗМЕНИТЬ - вернуть из кеша
    }

    /**
     * Создаёт или возвращает экземпляр EncryptedSharedPreferences.
     */
    private static SharedPreferences getEncryptedSharedPreferences(Context context)
            throws GeneralSecurityException, IOException {

        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}