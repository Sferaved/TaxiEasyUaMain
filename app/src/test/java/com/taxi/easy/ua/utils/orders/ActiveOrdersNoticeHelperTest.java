package com.taxi.easy.ua.utils.orders;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ActiveOrdersNoticeHelperTest {

    private static final int NAV_VISICOM = 1;
    private static final int NAV_FINISH = 2;
    private static final int NAV_CACHE = 3;
    private static final int NAV_OTHER = 4;

    private boolean offer(
            boolean suppressAfterCancel,
            int dest,
            boolean submitInProgress,
            boolean earlyNavDone,
            boolean gpayFrozen,
            boolean gpayUi,
            boolean persistedUid
    ) {
        return ActiveOrdersNoticeHelper.shouldOfferOnOrderPage(
                suppressAfterCancel,
                dest,
                NAV_VISICOM,
                NAV_FINISH,
                NAV_CACHE,
                submitInProgress,
                earlyNavDone,
                gpayFrozen,
                gpayUi,
                persistedUid
        );
    }

    @Test
    public void offersOnOrderPageWhenIdleOnVisicom() {
        assertTrue(offer(false, NAV_VISICOM, false, false, false, false, false));
    }

    @Test
    public void suppressesOnFinishScreen() {
        assertFalse(offer(false, NAV_FINISH, false, false, false, false, false));
    }

    @Test
    public void suppressesOnCacheOrderScreen() {
        assertFalse(offer(false, NAV_CACHE, false, false, false, false, false));
    }

    @Test
    public void suppressesDuringSubmitOrEarlyNav() {
        assertFalse(offer(false, NAV_VISICOM, true, false, false, false, false));
        assertFalse(offer(false, NAV_VISICOM, false, true, false, false, false));
    }

    @Test
    public void suppressesDuringGooglePayFlow() {
        assertFalse(offer(false, NAV_VISICOM, false, false, true, false, false));
        assertFalse(offer(false, NAV_VISICOM, false, false, false, true, false));
    }

    @Test
    public void suppressesWithPersistedActiveOrder() {
        assertFalse(offer(false, NAV_VISICOM, false, false, false, false, true));
    }

    @Test
    public void suppressesAfterCancelWindow() {
        assertFalse(offer(true, NAV_VISICOM, false, false, false, false, false));
    }

    @Test
    public void suppressesOnNonOrderPageDestination() {
        assertFalse(offer(false, NAV_OTHER, false, false, false, false, false));
    }
}
