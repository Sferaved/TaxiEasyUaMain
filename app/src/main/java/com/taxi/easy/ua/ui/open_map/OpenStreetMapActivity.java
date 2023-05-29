package com.taxi.easy.ua.ui.open_map;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.taxi.easy.ua.R;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.Date;

public class OpenStreetMapActivity extends AppCompatActivity {
    private LocationManager locationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.open_street_map_layout);

        MapView map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);


        IMapController mapController = map.getController();
        mapController.setZoom(12);
        map.setClickable(true);

        ;

        GeoPoint startPoint = new GeoPoint(50.568235937668135, 30.26999524844567);
        mapController.setCenter(startPoint);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    }

    public void onResume() {
        super.onResume();
        Toast.makeText(this, "Позначте на карті місця посадки та призначення", Toast.LENGTH_SHORT).show();
//
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
            checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 10, 10, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                1000 * 10, 10, locationListener);
        checkEnabled();
    }


    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            checkEnabled();
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onProviderEnabled(String provider) {
            checkEnabled();
            showLocation(locationManager.getLastKnownLocation(provider));
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                Log.d("TAG", "onStatusChanged GPS_PROVIDER: Status: " + status);

            } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                Log.d("TAG", "onStatusChanged NETWORK_PROVIDER: Status: " + status);

            }
        }
    };

    private void showLocation(Location location) {
        if (location == null)
            return;
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            Log.d("TAG", "showLocation GPS_PROVIDER: "  + formatLocation(location));
            Toast.makeText(this, "showLocation GPS_PROVIDER: "  + formatLocation(location), Toast.LENGTH_LONG).show();
        } else if (location.getProvider().equals(
                LocationManager.NETWORK_PROVIDER)) {
            Log.d("TAG", "showLocation NETWORK_PROVIDER: " + formatLocation(location));
            Toast.makeText(this, "showLocation NETWORK_PROVIDER: "  + formatLocation(location), Toast.LENGTH_LONG).show();

        }
    }

    @SuppressLint("DefaultLocale")
    private String formatLocation(Location location) {
        if (location == null)
            return "";
        return String.format("Coordinates: lat = %1$.4f, lon = %2$.4f, time = %3$tF %3$tT",
                location.getLatitude(), location.getLongitude(), new Date(location.getTime()));
    }

    @SuppressLint("SetTextI18n")
    private void checkEnabled() {
        Log.d("TAG", "checkEnabled: Enabled: " + locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        Log.d("TAG", "checkEnabled: Enabled: " + locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));

    }

    public void onClickLocationSettings(View view) {
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    };

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            Log.d("TAG", "checkPermission: " + new String[]{permission});
        }
    }
    // This function is called when user accept or decline the permission.
// Request Code is used to check which permission called this function.
// This request code is provided when user is prompt for permission.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
}