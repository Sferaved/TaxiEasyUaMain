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

public class ToJSONParser {

    public ToJSONParser(String urlString) throws MalformedURLException, JSONException, InterruptedException {
        sendURL(urlString);
    }

    public static Map<String, String> sendURL(String urlString) throws MalformedURLException, InterruptedException, JSONException {

        Map<String, String> costMap = new HashMap<>();

        URL url = new URL(urlString);

        Exchanger<String> exchanger = new Exchanger<>();

        AsyncTask.execute(() -> {
            HttpsURLConnection urlConnection = null;
            try {
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
//                Log.d("TAG", "sendURL: + ++++" + urlConnection.getResponseCode());
                if (urlConnection.getResponseCode() == 200) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                     exchanger.exchange(convertStreamToString(in));
                } else {
                    exchanger.exchange("400");
                }
            } catch (IOException e) {
                Log.d("TAG", "onCreate:" + new RuntimeException(e));
            } catch (InterruptedException e) {
                Log.d("TAG", "onCreate:" + new RuntimeException(e));
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });



            ResultFromThread first = new ResultFromThread(exchanger);
            if (first.message.equals("400")) {

                costMap.put("order_cost", "0");
                costMap.put("message", "Спробуйте ще або зателефонуйте оператору");
               return costMap;
            } else {
            JSONObject jsonarray = new JSONObject(first.message);
            Log.d("TAG", "sendURL jsonarray: " + jsonarray);
            if (!jsonarray.getString("order_cost").equals("0")) {
                costMap.put("from_lat", jsonarray.getString("from_lat"));
                costMap.put("from_lng", jsonarray.getString("from_lng"));

                costMap.put("lat", jsonarray.getString("lat"));
                costMap.put("lng", jsonarray.getString("lng"));

                costMap.put("dispatching_order_uid", jsonarray.getString("dispatching_order_uid"));
                costMap.put("order_cost", jsonarray.getString("order_cost"));
                costMap.put("add_cost", jsonarray.getString("add_cost"));
//             costMap.put("recommended_add_cost", jsonarray.getString("recommended_add_cost"));
                costMap.put("currency", jsonarray.getString("currency"));
                costMap.put("discount_trip", jsonarray.getString("discount_trip"));

                costMap.put("routefrom", jsonarray.getString("routefrom"));
                costMap.put("routefromnumber", jsonarray.getString("routefromnumber"));

                costMap.put("routeto", jsonarray.getString("routeto"));
                costMap.put("to_number", jsonarray.getString("to_number"));


            }
            if (jsonarray.getString("order_cost").equals("0")) {
                Log.d("TAG", "sendURL: " + jsonarray.getString("Message"));
                costMap.put("order_cost", "0");
                costMap.put("message", jsonarray.getString("Message"));
            }
            Log.d("TAG", "sendURL to Json costMap: " + costMap);
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
