package com.taxi.easy.ua.ui.open_map;


import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import org.json.JSONException;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import java.net.MalformedURLException;

public class MarkerOverlay extends Overlay {

    public MarkerOverlay(Context context) {
        super(context);
//        marker = null;
//        OpenStreetMapActivity.mEnd = null;
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView) {




        OpenStreetMapActivity.endPoint = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
        String target = OpenStreetMapActivity.epm;
//        Log.d("TAG", "onSingleTapConfirmed:OpenStreetMapActivity.endPoint " + OpenStreetMapActivity.endPoint);
        OpenStreetMapActivity.setMarker(OpenStreetMapActivity.endPoint.getLatitude(), OpenStreetMapActivity.endPoint.getLongitude(), target);

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                OpenStreetMapActivity.dialogMarkers(OpenStreetMapActivity.fragmentManager);
            }
        } catch (MalformedURLException | JSONException | InterruptedException e) {
            Log.d("TAG", "onCreate:" + new RuntimeException(e));
        }

        return true;
    }


}

