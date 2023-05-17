package com.taxi.easy.ua.ui.maps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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
import java.util.Map;

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

        public void onMapReady(GoogleMap googleMap) {
            mMap = googleMap;

//            myPositionStart = new MyPosition(50.568235937668135, 30.26999524844567);
//         Тест
            myPositionStart = new MyPosition(46.4775,  30.7326);

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


//                        Log.d("TAG", "onMapClick: " + getTaxiUrl(origin, dest));

                        String urlCost = getTaxiUrl(origin, dest, "costMap");

                        // Start downloading json data from Google Directions API
//                        downloadTaskCost.execute(url);
                        try {
//                            Log.d("TAG", "onMapClick: + sendURL(urlCost)" + CostJSONParser.sendURL(urlCost).get("order_cost")) ;
                            Map sendUrlMapCost = CostJSONParser.sendURL(urlCost);

                            String orderCost = (String) sendUrlMapCost.get("order_cost");

                            if(!orderCost.equals("0")) {
                                // Getting URL to the Google Directions API
                                String url = getDirectionsUrl(origin, dest);

                                DownloadTask downloadTask = new DownloadTask();

                                // Start downloading json data from Google Directions API
                                downloadTask.execute(url);
                                new MaterialAlertDialogBuilder(getActivity())
                                        .setMessage("Вартість поїздки: " + orderCost + "грн")
                                        .setPositiveButton("Замовити", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Log.d("TAG", "onClick: " + "Замовити");

                                                String urlOrder = getTaxiUrl(origin, dest, "orderMap");
                                                try {
                                                    Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);

                                                    String orderWeb = (String) sendUrlMap.get("order_cost");
                                                    if(!orderWeb.equals("0")) {
                                                        String from_name = (String) sendUrlMap.get("from_name");
                                                        String to_name = (String) sendUrlMap.get("to_name");
                                                        new MaterialAlertDialogBuilder(getActivity())
                                                                .setMessage("Дякуемо за замовлення зі " +
                                                                        from_name + " до " +
                                                                        to_name + ". " +
                                                                        "Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн")
                                                                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        Log.d("TAG", "onClick ");
                                                                        markerPoints.clear();
                                                                        mMap.clear();
                                                                    }
                                                                })
                                                                .show();
                                                    } else {
                                                        String message = (String) sendUrlMap.get("message");
                                                        new MaterialAlertDialogBuilder(getActivity())
                                                                .setMessage(message +
                                                                        ". Спробуйте ще або зателефонуйте оператору.")
                                                                .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        Intent intent = new Intent(Intent.ACTION_CALL);
                                                                        intent.setData(Uri.parse("tel:0934066749"));
                                                                        if (ActivityCompat.checkSelfPermission(getActivity(),
                                                                                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                            Toast.makeText(getActivity(), "Дозвольте застосунку отримати доступ у панелі налаштувань телефону та спробуйте ще.", Toast.LENGTH_LONG).show();
//                    final Intent i = new Intent();
//                    i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                    i.addCategory(Intent.CATEGORY_DEFAULT);
//                    i.setData(Uri.parse("package:" + this.getPackageName()));
//                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                    this.startActivity(i);
                                                                            return;
                                                                        }
                                                                        startActivity(intent);
                                                                    }
                                                                })
                                                                .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {
                                                                        Log.d("TAG", "onClick: " + "Спробуйте ще");
                                                                    }
                                                                })
                                                                .show();
                                                    }


                                                } catch (MalformedURLException e) {
                                                    throw new RuntimeException(e);
                                                } catch (InterruptedException e) {
                                                    throw new RuntimeException(e);
                                                } catch (JSONException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }
                                        })
                                        .setNegativeButton("Відміна", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Log.d("TAG", "onClick: " + "Відміна");
                                            }
                                        })
                                        .show();
                            }
                            else {
                                markerPoints.clear();
                                mMap.clear();
                                String message = (String) sendUrlMapCost.get("message");
                                new MaterialAlertDialogBuilder(getActivity())
                                        .setMessage(message +
                                                ". Спробуйте ще або зателефонуйте оператору.")
                                        .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Intent intent = new Intent(Intent.ACTION_CALL);
                                                intent.setData(Uri.parse("tel:0934066749"));
                                                if (ActivityCompat.checkSelfPermission(getActivity(),
                                                        Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                    Toast.makeText(getActivity(), "Дозвольте застосунку отримати доступ у панелі налаштувань телефону та спробуйте ще.", Toast.LENGTH_LONG).show();
//                    final Intent i = new Intent();
//                    i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                    i.addCategory(Intent.CATEGORY_DEFAULT);
//                    i.setData(Uri.parse("package:" + this.getPackageName()));
//                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//                    this.startActivity(i);
                                                    return;
                                                }
                                                startActivity(intent);
                                            }
                                        })
                                        .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                Log.d("TAG", "onClick: " + "Спробуйте ще");
                                            }
                                        })
                                        .show();
                            }

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

    private String getTaxiUrl(LatLng origin, LatLng dest, String urlAPI) {

        // Origin of route
        String str_origin = origin.latitude + "/" + origin.longitude;

        // Destination of route
        String str_dest = dest.latitude + "/" + dest.longitude;

        // Building the parameters to the web service
        String parameters = str_origin + "/" + str_dest;

        // Output format
        String output = "json";

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/api/android/" + urlAPI + "/" + parameters;

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