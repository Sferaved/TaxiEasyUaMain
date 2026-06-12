package com.taxi.easy.ua.utils.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.taxi.easy.ua.ui.finish.FinishCostResponse;
import com.taxi.easy.ua.ui.finish.OrderResponse;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ApiGsonHelperTest {

    private final Gson gson = ApiGsonHelper.create();

    @Test
    public void orderResponse_emptyNumericStrings_defaultToZero() {
        String json = "{"
                + "\"change_cost_allowed\":\"\","
                + "\"change_cost_not_allowed_reason\":\"\","
                + "\"close_reason\":\"\","
                + "\"driver_execution_status\":\"\","
                + "\"find_car_timeout\":\"\","
                + "\"find_car_delay\":\"\","
                + "\"cancellation_reason\":\"\","
                + "\"corporate_account_id\":\"\","
                + "\"push_type\":\"\","
                + "\"dispatching_order_uid\":\"uid-1\","
                + "\"order_cost\":\"150\","
                + "\"execution_status\":\"WaitingCarSearch\""
                + "}";

        OrderResponse response = gson.fromJson(json, OrderResponse.class);

        assertNotNull(response);
        assertFalse(response.isChangeCostAllowed());
        assertEquals(0, response.getChangeCostNotAllowedReason());
        assertEquals(0, response.getCloseReason());
        assertEquals(0, response.getDriverExecutionStatus());
        assertEquals(0, response.getFindCarTimeout());
        assertEquals(0, response.getFindCarDelay());
        assertEquals(0, response.getCancellationReason());
        assertEquals(0, response.getCorporateAccountId());
        assertEquals(0, response.getPushType());
        assertEquals("uid-1", response.getDispatchingOrderUid());
        assertEquals("150", response.getOrderCost());
    }

    @Test
    public void orderResponse_numericValues_parsedNormally() {
        String json = "{"
                + "\"change_cost_allowed\":true,"
                + "\"close_reason\":-1,"
                + "\"find_car_timeout\":120,"
                + "\"push_type\":3"
                + "}";

        OrderResponse response = gson.fromJson(json, OrderResponse.class);

        assertEquals(true, response.isChangeCostAllowed());
        assertEquals(-1, response.getCloseReason());
        assertEquals(120, response.getFindCarTimeout());
        assertEquals(3, response.getPushType());
    }

    @Test
    public void finishCostResponse_emptyNumericStrings_defaultToZero() {
        String json = "{"
                + "\"result\":\"ok\","
                + "\"message\":\"\","
                + "\"finish_cost\":\"\","
                + "\"order_id\":\"\""
                + "}";

        FinishCostResponse response = gson.fromJson(json, FinishCostResponse.class);

        assertEquals("ok", response.getResult());
        assertEquals(0.0, response.getFinishCost(), 0.0001);
        assertEquals(0, response.getOrderId());
    }

    @Test
    public void routeResponseCancel_nullRequiredTime_parsedAsList() {
        String json = "[{"
                + "\"uid\":\"abc\","
                + "\"routefrom\":\"вул. Хрещатик\","
                + "\"closeReason\":\"-1\","
                + "\"required_time\":null"
                + "}]";

        Type type = new TypeToken<List<RouteResponseCancel>>() {}.getType();
        List<RouteResponseCancel> routes = gson.fromJson(json, type);

        assertNotNull(routes);
        assertEquals(1, routes.size());
        assertEquals("abc", routes.get(0).getUid());
    }
}
