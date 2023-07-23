package com.taxi.easy.ua.ui.maps;

import android.os.AsyncTask;
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
import java.util.concurrent.Exchanger;

import javax.net.ssl.HttpsURLConnection;

public class FromJSONParser {

    public FromJSONParser(String urlString) throws MalformedURLException, JSONException, InterruptedException {
        sendURL(urlString);
    }

    public static Map<String, String> sendURL(String urlString) throws MalformedURLException, InterruptedException, JSONException {
        URL url = new URL(urlString);
        Log.d("TAG", "sendURL: " + urlString);
        Map<String, String> costMap = new HashMap<>();
        Exchanger<String> exchanger = new Exchanger<>();

        AsyncTask.execute(() -> {
            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                if (urlConnection.getResponseCode() == 200) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    exchanger.exchange(convertStreamToString(in));
                } else {

                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            urlConnection.disconnect();
        });

        ResultFromThread first = new ResultFromThread(exchanger);

        JSONObject jsonarray = new JSONObject(first.message);

         if(!jsonarray.getString("order_cost").equals("0")) {
             costMap.put("order_cost", "100");
             costMap.put("route_address_from", jsonarray.getString("route_address_from"));
             costMap.put("name", jsonarray.getString("name"));
             costMap.put("house", jsonarray.getString("house"));

         } else {
             costMap.put("order_cost", "0");
             costMap.put("message", jsonarray.getString("Message"));
         }


//        Log.d(TAG, "servicesAll: " + costMap);
            return costMap;

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

    public static class ResultFromThread {
        public String message;

        public ResultFromThread(Exchanger<String> exchanger) throws InterruptedException {
            this.message = exchanger.exchange(message);
        }

    }

}
