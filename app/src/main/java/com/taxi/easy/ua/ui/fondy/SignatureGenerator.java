package com.taxi.easy.ua.ui.fondy;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public class SignatureGenerator {
    public static String generateSignature(String merchantPassword, Map<String, String> params) {
        // Сортируем параметры по ключам (алфавитный порядок)
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        Log.d("TAG1", "generateSignature: " + params);
        // Создаем строку для подписи
        StringBuilder signatureData = new StringBuilder();
        signatureData.append(merchantPassword); // Добавляем пароль мерчанта

        // Добавляем все отсортированные параметры, разделенные символом вертикальной черты
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            signatureData.append("|");
            signatureData.append(entry.getValue());
        }

        try {
            // Вычисляем SHA-1 хеш
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(signatureData.toString().getBytes());

            // Преобразуем байты в строку HEX
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            Log.d("TAG1", "generateSignature: hexString.toString()" + hexString.toString());
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null; // Обработка ошибки
        }
    }
}
