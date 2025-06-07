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

    /**
     * Сохраняет ключ в EncryptedSharedPreferences.
     *
     * @param context Контекст приложения
     * @param key     Ключ для сохранения
     * @throws GeneralSecurityException Если произошла ошибка шифрования
     * @throws IOException              Если произошла ошибка ввода-вывода
     */
    public static void saveKey(Context context, String key) throws GeneralSecurityException, IOException {
        SharedPreferences prefs = getEncryptedSharedPreferences(context);
        prefs.edit().putString(KEY_API_KEY, key).apply();
    }

    /**
     * Извлекает ключ из EncryptedSharedPreferences.
     *
     * @param context Контекст приложения
     * @return Сохранённый ключ или null, если ключ не найден
     * @throws GeneralSecurityException Если произошла ошибка шифрования
     * @throws IOException              Если произошла ошибка ввода-вывода
     */
    public static String getKey(Context context) throws GeneralSecurityException, IOException {
        SharedPreferences prefs = getEncryptedSharedPreferences(context);
        return prefs.getString(KEY_API_KEY, null);
    }

    /**
     * Создаёт или возвращает экземпляр EncryptedSharedPreferences.
     *
     * @param context Контекст приложения
     * @return SharedPreferences с шифрованием
     * @throws GeneralSecurityException Если не удалось создать мастер-ключ
     * @throws IOException              Если произошла ошибка ввода-вывода
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
