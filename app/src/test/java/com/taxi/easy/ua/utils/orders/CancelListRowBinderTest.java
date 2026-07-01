package com.taxi.easy.ua.utils.orders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CancelListRowBinderTest {

    @Test
    public void rawCostFromItem_legacyFivePartFormat() {
        String item = "route#11#auto#time#status";
        assertEquals("11", CancelListRowBinder.rawCostFromItem(item));
    }

    @Test
    public void rawCostFromItem_newSevenPartFormat_extractsAmount() {
        String item = "route#вартість: 15 грн#оплата#авто#time##";
        assertEquals("15", CancelListRowBinder.rawCostFromItem(item));
    }

    @Test
    public void rawCostFromItem_emptyItem_returnsEmpty() {
        assertEquals("", CancelListRowBinder.rawCostFromItem(null));
        assertEquals("", CancelListRowBinder.rawCostFromItem(""));
    }
}
