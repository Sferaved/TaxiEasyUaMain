package com.taxi.easy.ua.utils.orders;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActiveOrdersFetchGateTest {

    @Test
    public void tryBegin_blocksSecondCallUntilEnd() {
        ActiveOrdersFetchGate gate = new ActiveOrdersFetchGate();
        assertTrue(gate.tryBegin());
        assertFalse(gate.tryBegin());
        gate.end();
        assertTrue(gate.tryBegin());
    }
}
