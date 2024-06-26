package com.taxi.easy.ua.utils.ip;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class IPUtil {

    public static String getPublicIPAddress() {
        try {
            URL url = new URL("https://api64.ipify.org?format=json");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();

            // Парсинг JSON, чтобы получить глобальный IP-адрес
            // Предполагается, что в ответе есть поле "ip"
            // Вам может потребоваться использовать библиотеку JSON для более надежного парсинга
            // В этом примере используется простой метод substring
            String jsonResponse = response.toString();
            int startIndex = jsonResponse.indexOf("\"ip\":\"") + 6;
            int endIndex = jsonResponse.indexOf("\"", startIndex);
            return jsonResponse.substring(startIndex, endIndex);

        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        return null;
    }
}
