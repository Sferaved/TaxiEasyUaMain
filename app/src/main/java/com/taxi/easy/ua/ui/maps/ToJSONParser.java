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

public class ToJSONParser {

    public ToJSONParser(String urlString) throws MalformedURLException, JSONException, InterruptedException {
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
                if (response.equals("400")) {
                    costMap.put("order_cost", "0");
                    costMap.put("message", "Сталася помилка");
                } else {
                    JSONObject jsonarray = new JSONObject(response);

                    if (!jsonarray.getString("order_cost").equals("0")) {
                        costMap.put("from_lat", jsonarray.getString("from_lat"));
                        costMap.put("from_lng", jsonarray.getString("from_lng"));
                        costMap.put("lat", jsonarray.getString("lat"));
                        costMap.put("lng", jsonarray.getString("lng"));
                        costMap.put("dispatching_order_uid", jsonarray.getString("dispatching_order_uid"));
                        costMap.put("order_cost", jsonarray.getString("order_cost"));
                        costMap.put("currency", jsonarray.getString("currency"));
                        costMap.put("routefrom", jsonarray.getString("routefrom"));
                        costMap.put("routefromnumber", jsonarray.getString("routefromnumber"));
                        costMap.put("routeto", jsonarray.getString("routeto"));
                        costMap.put("to_number", jsonarray.getString("to_number"));

                        if(jsonarray.has("doubleOrder")) {
                            costMap.put("doubleOrder", jsonarray.getString("doubleOrder"));
                        }
                        if(jsonarray.has("dispatching_order_uid_Double")) {
                            costMap.put("dispatching_order_uid_Double", jsonarray.getString("dispatching_order_uid_Double"));
                        } else {
                            costMap.put("dispatching_order_uid_Double", " ");
                        }


                    } else {
                        costMap.put("order_cost", "0");
                        costMap.put("message", jsonarray.getString("Message"));
                    }
                }
            } else {
                costMap.put("order_cost", "0");
                costMap.put("message", "Сталася помилка");
            }
            return costMap;
        }  catch (Exception e) {
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
//    public static class ResultFromThread {
//        public String message;
//
//        public ResultFromThread(String message) {
//            this.message = message;
//        }
//    }
}
