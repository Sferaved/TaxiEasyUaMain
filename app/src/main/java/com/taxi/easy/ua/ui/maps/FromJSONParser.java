package com.taxi.easy.ua.ui.maps;

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public class FromJSONParser {

    public FromJSONParser(String urlString) throws MalformedURLException, JSONException, InterruptedException {
        sendURL(urlString);
    }

public Map<String, String> sendURL(String urlString) throws MalformedURLException, InterruptedException, JSONException {
    URL url = new URL(urlString);
    Log.d("TAG", "sendURL: 55555555555 " + urlString);
    Map<String, String> costMap = new HashMap<>();

    Callable<String> asyncTaskCallable = () -> {
        HttpsURLConnection urlConnection = null;
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            if (urlConnection.getResponseCode() == 200) {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                return convertStreamToString(in);
            }
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    };

    Future<String> asyncTaskFuture = Executors.newSingleThreadExecutor().submit(asyncTaskCallable);

    try {
        String response = asyncTaskFuture.get(60, TimeUnit.SECONDS);
        if (response != null) {

            JSONObject jsonarray = new JSONObject(response);

            if (!jsonarray.getString("order_cost").equals("0")) {
                costMap.put("order_cost", "100");
                costMap.put("route_address_from", jsonarray.getString("route_address_from"));
                costMap.put("name", jsonarray.getString("name"));
                costMap.put("house", jsonarray.getString("house"));
            } else {
                costMap.put("order_cost", "0");
                costMap.put("message", jsonarray.getString("Message"));
            }
        } else {
            costMap.put("order_cost", "0");
            costMap.put("message", "Сталася помилка");
        }
        return costMap;
    } catch (Exception e) {
        FirebaseCrashlytics.getInstance().recordException(e);
        asyncTaskFuture.cancel(true);
        costMap.put("order_cost", "0");
        costMap.put("message", "Сталася помилка");
        return costMap;
    }

}
    private static String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }

        return sb.toString();
    }

}
