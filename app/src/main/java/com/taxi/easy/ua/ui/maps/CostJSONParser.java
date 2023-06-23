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

public class CostJSONParser {

    public CostJSONParser(String urlString) throws MalformedURLException, JSONException, InterruptedException {
        sendURL(urlString);
    }

    public static Map<String, String> sendURL(String urlString) throws MalformedURLException, InterruptedException, JSONException {

        Log.d("TAG", "sendURL: " + urlString);
        Map<String, String> costMap = new HashMap<>();

        URL url = new URL(urlString);

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
             costMap.put("dispatching_order_uid", jsonarray.getString("dispatching_order_uid"));
             costMap.put("order_cost", jsonarray.getString("order_cost"));
             costMap.put("add_cost", jsonarray.getString("add_cost"));
             costMap.put("recommended_add_cost", jsonarray.getString("recommended_add_cost"));
             costMap.put("currency", jsonarray.getString("currency"));
             costMap.put("discount_trip", jsonarray.getString("discount_trip"));
             costMap.put("can_pay_bonuses", jsonarray.getString("can_pay_bonuses"));
             costMap.put("can_pay_cashless", jsonarray.getString("can_pay_cashless"));
         }else {
             Log.d("TAG", "sendURL: " + jsonarray.getString("Message"));
             costMap.put("order_cost", "0");
             costMap.put("message", jsonarray.getString("Message"));
         }
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
