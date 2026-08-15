package com.taxi.easy.ua.utils.route;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.utils.PolylineEncoder;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Запрос маршрута OSRM через OkHttp (надёжнее osmbonuspack HttpUrlConnection)
 * и проверка, что ответ — реальная геометрия, а не прямая из двух точек.
 */
public final class OsrmRouteHelper {

    public static final String SERVICE_OSM_DE = "https://routing.openstreetmap.de/";
    public static final String MEAN_CAR_OSM_DE = OSRMRoadManager.MEAN_BY_CAR;
    public static final String SERVICE_PROJECT_OSRM = "https://router.project-osrm.org/";
    public static final String MEAN_CAR_PROJECT = "route/v1/driving/";

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build();

    public static final class Endpoint {
        public final String serviceUrl;
        public final String meanUrl;

        public Endpoint(String serviceUrl, String meanUrl) {
            this.serviceUrl = serviceUrl;
            this.meanUrl = meanUrl;
        }
    }

    private OsrmRouteHelper() {
    }

    public static List<Endpoint> fallbackEndpoints() {
        return Collections.unmodifiableList(Arrays.asList(
                new Endpoint(SERVICE_OSM_DE, MEAN_CAR_OSM_DE),
                new Endpoint(SERVICE_PROJECT_OSRM, MEAN_CAR_PROJECT)
        ));
    }

    public static boolean isUsableRoad(@Nullable Road road) {
        return road != null
                && road.mStatus == Road.STATUS_OK
                && road.mRouteHigh != null
                && road.mRouteHigh.size() >= 2;
    }

    public static int pointCount(@Nullable Road road) {
        if (road == null || road.mRouteHigh == null) {
            return 0;
        }
        return road.mRouteHigh.size();
    }

    public static String resolveUserAgent(@Nullable String preferred, @Nullable String packageName) {
        if (preferred != null && !preferred.trim().isEmpty()) {
            return preferred.trim();
        }
        if (packageName != null && !packageName.trim().isEmpty()) {
            return packageName.trim();
        }
        return "osmdroid";
    }

    public static String buildUrl(Endpoint endpoint, List<GeoPoint> waypoints) {
        StringBuilder url = new StringBuilder();
        url.append(endpoint.serviceUrl).append(endpoint.meanUrl);
        for (int i = 0; i < waypoints.size(); i++) {
            if (i > 0) {
                url.append(';');
            }
            GeoPoint p = waypoints.get(i);
            url.append(p.getLongitude()).append(',').append(p.getLatitude());
        }
        url.append("?alternatives=false&overview=full&steps=false");
        return url.toString();
    }

    /**
     * Парсит JSON ответа OSRM v5. Возвращает null, если маршрут невалиден.
     */
    @Nullable
    public static Road parseOsrmResponse(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject root = new JSONObject(json);
            if (!"Ok".equals(root.optString("code"))) {
                return null;
            }
            JSONArray routes = root.optJSONArray("routes");
            if (routes == null || routes.length() == 0) {
                return null;
            }
            JSONObject route = routes.getJSONObject(0);
            String geometry = route.optString("geometry", "");
            if (geometry.isEmpty()) {
                return null;
            }
            ArrayList<GeoPoint> points = PolylineEncoder.decode(geometry, 10, false);
            if (points == null || points.size() < 2) {
                return null;
            }
            Road road = new Road();
            road.mStatus = Road.STATUS_OK;
            road.mRouteHigh = points;
            road.mLength = route.optDouble("distance", 0) / 1000.0;
            road.mDuration = route.optDouble("duration", 0);
            return road;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static Road fetchDrivingRoute(@Nullable String userAgent, List<GeoPoint> waypoints) {
        if (waypoints == null || waypoints.size() < 2) {
            return null;
        }
        String ua = resolveUserAgent(userAgent, null);
        for (Endpoint endpoint : fallbackEndpoints()) {
            try {
                Road road = fetchFromEndpoint(HTTP, ua, endpoint, waypoints);
                if (isUsableRoad(road)) {
                    return road;
                }
            } catch (Exception ignored) {
                // следующий endpoint
            }
        }
        return null;
    }

    @Nullable
    static Road fetchFromEndpoint(OkHttpClient client, String userAgent,
                                  Endpoint endpoint, List<GeoPoint> waypoints) throws IOException {
        String url = buildUrl(endpoint, waypoints);
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                return null;
            }
            return parseOsrmResponse(response.body().string());
        }
    }

    public static Road straightFallbackRoad(List<GeoPoint> waypoints) {
        return new Road(new java.util.ArrayList<>(waypoints));
    }
}
