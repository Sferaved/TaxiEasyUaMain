package com.taxi.easy.ua.utils.orders;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActiveOrdersResponseHelperTest {

    private static final String PRELIMINARY_ORDER_JSON = "["
            + "{"
            + "\"uid\":\"3b2fcbc1c9fd48b5bc6c7ab48cdfcedc\","
            + "\"routefrom\":\"вул. Хрещатик\","
            + "\"routefromnumber\":\"1\","
            + "\"routeto\":\"вул. Богдана Хмельницького\","
            + "\"routetonumber\":\"\","
            + "\"web_cost\":\"150\","
            + "\"closeReason\":\"-1\","
            + "\"auto\":\"\","
            + "\"required_time\":\"07.07.2026 21:47\","
            + "\"created_at\":\"07.06.2026 18:30:00\""
            + "}"
            + "]";

    private static final String EMPTY_PLACEHOLDER_JSON = "["
            + "{"
            + "\"routefrom\":\"*\","
            + "\"routefromnumber\":\"*\","
            + "\"routeto\":\"*\","
            + "\"routetonumber\":\"*\","
            + "\"web_cost\":\"*\","
            + "\"closeReason\":\"*\","
            + "\"auto\":\"*\","
            + "\"created_at\":\"*\""
            + "}"
            + "]";

    @Test
    public void preliminaryOrderResponse_shouldDisplayInActiveOrders() {
        List<RouteResponseCancel> routes = parseRoutes(PRELIMINARY_ORDER_JSON);

        assertTrue(ActiveOrdersResponseHelper.shouldDisplayActiveOrders(routes));
        assertFalse(ActiveOrdersResponseHelper.hasRouteWithAsterisk(routes));
        assertEquals("-1", routes.get(0).getCloseReason());
        assertEquals("07.07.2026 21:47", routes.get(0).getRequired_time());
    }

    @Test
    public void emptyPlaceholderResponse_shouldNotDisplayActiveOrders() {
        List<RouteResponseCancel> routes = parseRoutes(EMPTY_PLACEHOLDER_JSON);

        assertFalse(ActiveOrdersResponseHelper.shouldDisplayActiveOrders(routes));
        assertTrue(ActiveOrdersResponseHelper.isEmptyPlaceholder(routes.get(0)));
    }

    private static List<RouteResponseCancel> parseRoutes(String json) {
        Type listType = new TypeToken<List<RouteResponseCancel>>() {
        }.getType();
        return new Gson().fromJson(json, listType);
    }
}
