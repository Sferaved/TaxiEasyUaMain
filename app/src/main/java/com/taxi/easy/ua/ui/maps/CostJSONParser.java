package com.taxi.easy.ua.ui.maps;


import android.util.Log;

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
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

public class CostJSONParser {

    public CostJSONParser(String urlString) throws MalformedURLException, JSONException, InterruptedException {
        sendURL(urlString);
    }

    public static Map<String, String> sendURL(String urlString) throws MalformedURLException {

        URL url = new URL(urlString);
        Log.d("TAG", "sendURL: " + urlString);
        Map<String, String> costMap = new HashMap<>();

        Callable<String> asyncTaskCallable = () -> {
            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                if (urlConnection.getResponseCode() == 200) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    return convertStreamToString(in);
                } else {
                    return "400";
                }
            } catch (IOException ignored) {
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return null;
        };

        Future<String> asyncTaskFuture = Executors.newSingleThreadExecutor().submit(asyncTaskCallable);

        try {
            String response = asyncTaskFuture.get(10, TimeUnit.SECONDS);
            Log.d("TAG", "sendURL: response " + response);
            if (response != null) {
                if (response.equals("400")) {
                    costMap.put("order_cost", "0");
                    costMap.put("message", "Сталася помилка");

                } else {
                    JSONObject jsonarray = new JSONObject(response);

                    if (!jsonarray.getString("order_cost").equals("0")) {
                         costMap.put("order_cost", jsonarray.getString("order_cost"));
                    }else {
                        Log.d("TAG", "sendURL: " + jsonarray.getString("Message"));
                        costMap.put("order_cost", "0");
                        costMap.put("message", jsonarray.getString("Message"));
                    }
                }
            } else {
                costMap.put("order_cost", "0");
                costMap.put("message", "Сталася помилка");
            }
            return costMap;
        } catch (TimeoutException e) {
            e.printStackTrace();
            asyncTaskFuture.cancel(true);
            costMap.put("order_cost", "0");
            costMap.put("message", "Сталася помилка");
            return costMap;
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

}
