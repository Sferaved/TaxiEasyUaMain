package com.taxi.easy.ua.utils.permissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * PermissionsResponse is a Retrofit body type. If R8 strips the class, Gson
 * returns LinkedTreeMap and {@code response.body()} throws ClassCastException.
 */
public class PermissionsResponseGsonTest {

    @Test
    public void fromJson_readsPayFlags() {
        PermissionsResponse response = new Gson().fromJson(
                "{\"card_pay\":\"1\",\"bonus_pay\":\"0\"}",
                PermissionsResponse.class);

        assertNotNull(response);
        assertEquals("1", response.getCardPay());
        assertEquals("0", response.getBonusPay());
    }
}
