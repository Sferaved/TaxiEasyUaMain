package com.taxi.easy.ua.ui.open_map;


import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.utils.log.Logger;

import org.json.JSONException;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.net.MalformedURLException;

public class MarkerOverlay extends Overlay {
    private static final String TAG = "MarkerOverlay";
    Marker marker;
    public MarkerOverlay(Context context) {
        super(context);
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView) {

        OpenStreetMapActivity.progressBar.setVisibility(View.VISIBLE);
    // Удаление старого маркера
        if(marker != null) {
            mapView.getOverlays().remove(marker);
            mapView.invalidate();
            marker = null;
        }
        mapView.invalidate();
        OpenStreetMapActivity.m = null;

        GeoPoint endPoint = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
        OpenStreetMapActivity.endPoint = endPoint;

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                OpenStreetMapActivity.dialogMarkers(OpenStreetMapActivity.fragmentManager, OpenStreetMapActivity.map.getContext());
            }
        } catch (MalformedURLException | JSONException | InterruptedException e) {
            Logger.d(mapView.getContext(), TAG, "onCreate:" + new RuntimeException(e));
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        return true;
    }


}

