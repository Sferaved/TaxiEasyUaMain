package com.taxi.easy.ua.utils.orders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class OrderCreatedAtDisplayHelperTest {

    @Test
    public void formatForDisplay_convertsUtcSqlToKyiv() {
        assertEquals(
                "13.06.2026 11:28:19",
                OrderCreatedAtDisplayHelper.formatForDisplay("2026-06-13 08:28:19")
        );
    }

    @Test
    public void formatForDisplay_passesThroughAlreadyFormatted() {
        assertEquals(
                "13.06.2026 11:28:19",
                OrderCreatedAtDisplayHelper.formatForDisplay("13.06.2026 11:28:19")
        );
    }

    @Test
    public void formatForDisplay_handlesPlaceholder() {
        assertEquals("*", OrderCreatedAtDisplayHelper.formatForDisplay("*"));
        assertNull(OrderCreatedAtDisplayHelper.formatForDisplay(null));
    }
}
