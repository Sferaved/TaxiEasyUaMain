package com.taxi.easy.ua.ui.card;

import static org.junit.Assert.assertEquals;

import com.google.gson.Gson;

import org.junit.Test;

/**
 * CardInfo has no @SerializedName: Gson maps JSON keys to Java field names.
 * R8 must keep those fields or card tokens break after minify.
 */
public class CardInfoGsonFieldNamesTest {

    @Test
    public void fromJson_usesRawFieldNames() {
        CardInfo info = new Gson().fromJson(
                "{\"masked_card\":\"41**11\",\"card_type\":\"visa\","
                        + "\"bank_name\":\"pumb\",\"rectoken\":\"tok-1\",\"merchant\":\"m\","
                        + "\"active\":\"1\"}",
                CardInfo.class);

        assertEquals("41**11", info.getMasked_card());
        assertEquals("visa", info.getCard_type());
        assertEquals("pumb", info.getBank_name());
        assertEquals("tok-1", info.getRectoken());
        assertEquals("m", info.getMerchant());
        assertEquals("1", info.getActive());
    }
}
