package com.taxi.easy.ua.utils.ui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VisicomOrderUiHelperTest {

    @Test
    public void shouldShowOrderActionButtons_requiresShedReadyAndNetwork() {
        assertFalse(VisicomOrderUiHelper.shouldShowOrderActionButtons("no", true));
        assertFalse(VisicomOrderUiHelper.shouldShowOrderActionButtons("ok", false));
        assertFalse(VisicomOrderUiHelper.shouldShowOrderActionButtons(null, true));
        assertFalse(VisicomOrderUiHelper.shouldShowOrderActionButtons("", true));
        assertTrue(VisicomOrderUiHelper.shouldShowOrderActionButtons("ok", true));
        assertTrue(VisicomOrderUiHelper.shouldShowOrderActionButtons("yes", true));
    }
}

