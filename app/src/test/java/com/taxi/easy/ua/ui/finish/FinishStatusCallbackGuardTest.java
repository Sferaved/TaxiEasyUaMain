package com.taxi.easy.ua.ui.finish;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FinishStatusCallbackGuardTest {

    @Test
    public void ignoreWhenFragmentAlreadyLeftTheScreen() {
        assertFalse(FinishStatusCallbackGuard.shouldApply(false, false));
    }

    @Test
    public void ignoreWhenStatusCallWasCanceled() {
        assertFalse(FinishStatusCallbackGuard.shouldApply(true, true));
    }

    @Test
    public void applyWhileFinishScreenIsStillShowing() {
        assertTrue(FinishStatusCallbackGuard.shouldApply(true, false));
    }
}
