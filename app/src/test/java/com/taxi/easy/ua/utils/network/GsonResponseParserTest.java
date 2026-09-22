package com.taxi.easy.ua.utils.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GsonResponseParserTest {

    @Test
    public void as_returnsSameInstanceWhenAlreadyDto() {
        CallbackResponseWfp original = new CallbackResponseWfp();
        assertSame(original, GsonResponseParser.as(original, CallbackResponseWfp.class));
    }

    @Test
    public void as_reparsesLinkedTreeMapShapeFromR8() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cards", Collections.emptyList());

        CallbackResponseWfp parsed = GsonResponseParser.as(body, CallbackResponseWfp.class);

        assertNotNull(parsed);
        assertTrue(parsed.getCards() == null || parsed.getCards().isEmpty());
    }

    @Test
    public void as_nullBody_returnsNull() {
        assertNull(GsonResponseParser.as(null, CallbackResponseWfp.class));
    }

    @Test
    public void as_parsesCardList() {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("masked_card", "41**11");
        card.put("rectoken", "tok");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cards", Collections.singletonList(card));

        CallbackResponseWfp parsed = GsonResponseParser.as(body, CallbackResponseWfp.class);

        assertNotNull(parsed);
        assertEquals(1, parsed.getCards().size());
        assertEquals("41**11", parsed.getCards().get(0).getMasked_card());
        assertEquals("tok", parsed.getCards().get(0).getRectoken());
    }
}
