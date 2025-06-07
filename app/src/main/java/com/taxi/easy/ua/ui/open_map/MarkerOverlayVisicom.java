package com.taxi.easy.ua.ui.open_map;


import android.util.Log;
import android.view.MotionEvent;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.utils.log.Logger;

import org.json.JSONException;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.net.MalformedURLException;

public class MarkerOverlayVisicom extends Overlay {
    Marker marker;
    String point;
    private final String TAG = "MarkerOverlayVisicom";


    public MarkerOverlayVisicom(String point) {
        super();
        this.point = point;
    }


    @Override
    public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView) {


    // Удаление старого маркера
        if(marker != null) {
            mapView.getOverlays().remove(marker);
            mapView.invalidate();
            marker = null;
        }
        mapView.invalidate();

        GeoPoint pointGeo = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
        Logger.d(mapView.getContext(), TAG, "onSingleTapConfirmed: point " + point);
        switch (point) {
            case "startMarker":
                OpenStreetMapVisicomActivity.startPoint = pointGeo;
                try {
                        OpenStreetMapVisicomActivity.dialogMarkerStartPoint();

                } catch (MalformedURLException e) {
                    Log.d("TAG", "onCreate:" + new RuntimeException(e));
                    FirebaseCrashlytics.getInstance().recordException(e);
                }
                break;
            case "finishMarker":
                OpenStreetMapVisicomActivity.endPoint = pointGeo;
                try {


                        OpenStreetMapVisicomActivity.dialogMarkersEndPoint();

                } catch (MalformedURLException | JSONException | InterruptedException e) {
                    Log.d("TAG", "onCreate:" + new RuntimeException(e));
                }
                break;


        }





        return true;
    }


}

