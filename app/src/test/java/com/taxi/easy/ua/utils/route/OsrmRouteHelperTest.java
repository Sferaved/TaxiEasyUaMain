package com.taxi.easy.ua.utils.route;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OsrmRouteHelperTest {

    @Test
    public void usableRoad_requiresOkStatusAndAtLeastTwoPoints() {
        Road ok = new Road();
        ok.mStatus = Road.STATUS_OK;
        ok.mRouteHigh = new ArrayList<>(Arrays.asList(
                new GeoPoint(46.48, 30.73),
                new GeoPoint(46.481, 30.731),
                new GeoPoint(46.482, 30.732)
        ));
        assertTrue(OsrmRouteHelper.isUsableRoad(ok));
    }

    @Test
    public void straightFallback_notUsable() {
        List<GeoPoint> points = Arrays.asList(
                new GeoPoint(46.48, 30.73),
                new GeoPoint(46.49, 30.74)
        );
        Road fallback = OsrmRouteHelper.straightFallbackRoad(points);
        assertEquals(Road.STATUS_TECHNICAL_ISSUE, fallback.mStatus);
        assertFalse(OsrmRouteHelper.isUsableRoad(fallback));
    }

    @Test
    public void nullAndInvalid_notUsable() {
        assertFalse(OsrmRouteHelper.isUsableRoad(null));
        Road invalid = new Road();
        invalid.mStatus = Road.STATUS_INVALID;
        invalid.mRouteHigh = new ArrayList<>(Arrays.asList(
                new GeoPoint(1, 1), new GeoPoint(2, 2), new GeoPoint(3, 3)
        ));
        assertFalse(OsrmRouteHelper.isUsableRoad(invalid));
    }

    @Test
    public void fallbackEndpoints_includePrimaryAndProjectOsrm() {
        List<OsrmRouteHelper.Endpoint> endpoints = OsrmRouteHelper.fallbackEndpoints();
        assertEquals(2, endpoints.size());
        assertEquals(OsrmRouteHelper.SERVICE_OSM_DE, endpoints.get(0).serviceUrl);
        assertEquals(OsrmRouteHelper.SERVICE_PROJECT_OSRM, endpoints.get(1).serviceUrl);
    }

    @Test
    public void resolveUserAgent_prefersNonEmpty() {
        assertEquals("app.ua", OsrmRouteHelper.resolveUserAgent(null, "app.ua"));
        assertEquals("custom", OsrmRouteHelper.resolveUserAgent("custom", "app.ua"));
        assertEquals("osmdroid", OsrmRouteHelper.resolveUserAgent("  ", null));
    }

    @Test
    public void buildUrl_usesLonLatOrder() {
        OsrmRouteHelper.Endpoint ep = new OsrmRouteHelper.Endpoint(
                OsrmRouteHelper.SERVICE_PROJECT_OSRM, OsrmRouteHelper.MEAN_CAR_PROJECT);
        String url = OsrmRouteHelper.buildUrl(ep, Arrays.asList(
                new GeoPoint(46.48, 30.72),
                new GeoPoint(46.49, 30.74)
        ));
        assertTrue(url.startsWith("https://router.project-osrm.org/route/v1/driving/"));
        assertTrue(url.contains("30.72,46.48;30.74,46.49"));
        assertTrue(url.contains("overview=full"));
    }

    @Test
    public void parseOsrmResponse_rejectsErrors() {
        assertNull(OsrmRouteHelper.parseOsrmResponse(null));
        assertNull(OsrmRouteHelper.parseOsrmResponse("{\"code\":\"NoRoute\"}"));
        assertNull(OsrmRouteHelper.parseOsrmResponse("{\"code\":\"Ok\",\"routes\":[]}"));
        assertNull(OsrmRouteHelper.parseOsrmResponse(
                "{\"code\":\"Ok\",\"routes\":[{\"geometry\":\"\",\"distance\":1,\"duration\":1}]}"));
    }
}
