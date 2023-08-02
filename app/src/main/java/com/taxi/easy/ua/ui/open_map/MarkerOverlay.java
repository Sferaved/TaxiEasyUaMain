package com.taxi.easy.ua.ui.open_map;


import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;

import org.json.JSONException;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.net.MalformedURLException;

public class MarkerOverlay extends Overlay {

    public static Marker marker;

    public MarkerOverlay(Context context) {
        super(context);
        marker = null;
    }

    @Override
    public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView) {
        if (marker != null) {
            mapView.getOverlays().remove(marker);
            mapView.invalidate();
        }
        if (OpenStreetMapActivity.m != null) {
            mapView.getOverlays().remove(OpenStreetMapActivity.m);
            mapView.invalidate();
        }


        OpenStreetMapActivity.endPoint = (GeoPoint) mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
        String target = OpenStreetMapActivity.epm;
        OpenStreetMapActivity.setMarker(OpenStreetMapActivity.endPoint.getLatitude(), OpenStreetMapActivity.endPoint.getLongitude(), target);
//        OpenStreetMapActivity.buttonAddServices.setVisibility(View.VISIBLE);
//        OpenStreetMapActivity.buttonAddServices.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                MyServicesDialogFragment bottomSheetDialogFragment = new MyServicesDialogFragment();
//                bottomSheetDialogFragment.show(bottomSheetDialogFragment.getChildFragmentManager(), bottomSheetDialogFragment.getTag());
//            }
//        });
        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                OpenStreetMapActivity.dialogMarkers(OpenStreetMapActivity.fragmentManager);
            }
        } catch (MalformedURLException | JSONException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return true;
    }


}

