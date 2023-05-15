package com.taxi.easy.ua.ui.maps;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.taxi.easy.ua.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsFragment extends Fragment {
    public MyPosition myPositionStart , myPositionFinish;
    LatLng start, finish;
    private GoogleMap mMap;
    ArrayList markerPoints= new ArrayList();

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
//        @Override
//        public void onMapReady(GoogleMap googleMap) {
//
//            myPositionStart = new MyPosition(50.568235937668135, 30.26999524844567);
//            myPositionFinish = new MyPosition(50.51499815972034, 30.23909620059411);
//
//            start = new LatLng(myPositionStart.latitude, myPositionStart.longitude);
//            finish = new LatLng(myPositionFinish.latitude, myPositionFinish.longitude);
//
//            googleMap.addMarker(new MarkerOptions().position(start).title("Звідки"));
//            googleMap.addMarker(new MarkerOptions().position(finish).title("Куди"));
//            googleMap.moveCamera(CameraUpdateFactory.newLatLng(start));
//
//            CameraPosition cameraPosition = new CameraPosition.Builder()
//                    .target(start)
//                    .zoom(12)
//                    .bearing(45)
//                    .tilt(20)
//                    .build();
//            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
//            googleMap.animateCamera(cameraUpdate);
//
//
//            googleMap.setOnMapClickListener(latLng -> Log.d("TAG", "onMapClick: " + latLng.latitude + "," + latLng.longitude));
//
//            googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
//
//                @Override
//                public void onMapLongClick(LatLng latLng) {
//                    Log.d("TAG", "onMapLongClick: " + latLng.latitude + "," + latLng.longitude);
//                }
//
//            });
//
//            googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
//
//                @Override
//                public void onCameraChange(CameraPosition camera) {
//                    Log.d("TAG", "onCameraChange: " + camera.target.latitude + "," + camera.target.longitude);
//                }
//
//            });
//        }
        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;

            myPositionStart = new MyPosition(50.568235937668135, 30.26999524844567);

            start = new LatLng(myPositionStart.latitude, myPositionStart.longitude);

            googleMap.moveCamera(CameraUpdateFactory.newLatLng(start));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(start)
                    .zoom(12)
                    .bearing(45)
                    .tilt(20)
                    .build();
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
            googleMap.animateCamera(cameraUpdate);

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {

                    if (markerPoints.size() > 1) {
                        markerPoints.clear();
                        mMap.clear();
                    }

                    // Adding new item to the ArrayList
                    markerPoints.add(latLng);

                    // Creating MarkerOptions
                    MarkerOptions options = new MarkerOptions();

                    // Setting the position of the marker
                    options.position(latLng);

                    if (markerPoints.size() == 1) {
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    } else if (markerPoints.size() == 2) {
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                    }

                    // Add new marker to the Google Map Android API V2
                    mMap.addMarker(options);

                    // Checks, whether start and end locations are captured
                    if (markerPoints.size() >= 2) {
                        LatLng origin = (LatLng) markerPoints.get(0);
                        LatLng dest = (LatLng) markerPoints.get(1);
//*******************************************
//                            myPositionStart = new MyPosition(50.568235937668135, 30.26999524844567);
//                            myPositionFinish = new MyPosition(50.51499815972034, 30.23909620059411);
//
//                            start = new LatLng(myPositionStart.latitude, myPositionStart.longitude);
//                            finish = new LatLng(myPositionFinish.latitude, myPositionFinish.longitude);
//                            origin = start;
//                            dest = (LatLng) finish;
//*******************************************
                        // Getting URL to the Google Directions API
                        String url = getDirectionsUrl(origin, dest);

                        DownloadTask downloadTask = new DownloadTask();

                        // Start downloading json data from Google Directions API
                        downloadTask.execute(url);

                        Log.d("TAG", "onMapClick: " + getTaxiUrl(origin, dest));

                        String urlCost = getTaxiUrl(origin, dest);



                        // Start downloading json data from Google Directions API
//                        downloadTaskCost.execute(url);
                        try {
                            Log.d("TAG", "onMapClick: + sendURL(urlCost)" + CostJSONParser.sendURL(urlCost).get("order_cost")) ;
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }


                    }

                }
            });

        }

    };

//    public Map<String, String> sendURL (String urlString) throws MalformedURLException, InterruptedException, JSONException {
//        URL url = new URL(urlString);
//        final String TAG = "TAG";
//
//        Exchanger<String> exchanger = new Exchanger<>();
//
//        AsyncTask.execute(() -> {
//            HttpsURLConnection urlConnection = null;
//            try {
//                urlConnection = (HttpsURLConnection) url.openConnection();
//                urlConnection.setDoInput(true);
//                if (urlConnection.getResponseCode() == 200) {
//                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
//                    exchanger.exchange(convertStreamToString(in));
//                } else {
//
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            urlConnection.disconnect();
//        });
//
//        MapsFragment.ResultFromThread first = new ResultFromThread(exchanger);
//
//        JSONObject jsonarray = new JSONObject(first.message);
//
////        Log.d(TAG, "servicesAll contacts: " + jsonarray.length() );
//
//        Map<String, String> costMap = new HashMap<>();
//            costMap.put("dispatching_order_uid", jsonarray.getString("dispatching_order_uid"));
//            costMap.put("order_cost", jsonarray.getString("order_cost"));
//            costMap.put("add_cost", jsonarray.getString("add_cost"));
//            costMap.put("recommended_add_cost", jsonarray.getString("recommended_add_cost"));
//            costMap.put("currency", jsonarray.getString("currency"));
//            costMap.put("discount_trip", jsonarray.getString("discount_trip"));
//            costMap.put("can_pay_bonuses", jsonarray.getString("can_pay_bonuses"));
//            costMap.put("can_pay_cashless", jsonarray.getString("can_pay_cashless"));
//
//        Log.d(TAG, "servicesAll: " + costMap);
//
//        return costMap;
//    }
//    private String convertStreamToString(InputStream is) {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//        StringBuilder sb = new StringBuilder();
//
//        String line;
//        try {
//            while ((line = reader.readLine()) != null) {
//                sb.append(line).append('\n');
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                is.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        return sb.toString();
//    }
//
//    public static class ResultFromThread {
//        public String message;
//
//        public ResultFromThread(Exchanger<String> exchanger) throws InterruptedException {
//            this.message = exchanger.exchange(message);
//        }
//
//    }



    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }

            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);

        }
    }
    @SuppressLint("StaticFieldLeak")
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);

            }

// Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }
    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service

        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getResources().getString(R.string.google_maps_key_storage);

        return url;
    }

    private String getTaxiUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = origin.latitude + "/" + origin.longitude;

        // Destination of route
        String str_dest = dest.latitude + "/" + dest.longitude;

        // Building the parameters to the web service
        String parameters = str_origin + "/" + str_dest;

        // Output format
        String output = "json";

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/api/android/costMap/" + parameters;

        return url;
    }
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }
}