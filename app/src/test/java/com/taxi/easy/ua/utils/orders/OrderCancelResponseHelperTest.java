package com.taxi.easy.ua.utils.orders;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrderCancelResponseHelperTest {

    private static final String CONFIRMED_SERVER =
            "Запит на скасування замовлення надіслано. Замовлення скасоване. ";

    private static final String PENDING_SERVER =
            "Запит на скасування замовлення надіслано. Очікуємо підтвердження диспетчера.";

    private static final String FAILURE_SERVER =
            "Замовлення не вдалося скасувати на диспетчері. Спробуйте ще раз або зателефонуйте оператору.";

    @Test
    public void confirmedServerMessage_isConfirmed() {
        assertEquals(OrderCancelResponseHelper.Kind.CONFIRMED,
                OrderCancelResponseHelper.classify(CONFIRMED_SERVER));
        assertTrue(OrderCancelResponseHelper.isConfirmed(CONFIRMED_SERVER));
        assertFalse(OrderCancelResponseHelper.isPending(CONFIRMED_SERVER));
        assertFalse(OrderCancelResponseHelper.isFailed(CONFIRMED_SERVER));
    }

    @Test
    public void pendingServerMessage_isPending_notConfirmed() {
        assertEquals(OrderCancelResponseHelper.Kind.PENDING,
                OrderCancelResponseHelper.classify(PENDING_SERVER));
        assertTrue(OrderCancelResponseHelper.isPending(PENDING_SERVER));
        assertFalse(OrderCancelResponseHelper.isConfirmed(PENDING_SERVER));
        assertFalse(OrderCancelResponseHelper.isFailed(PENDING_SERVER));
    }

    @Test
    public void failureServerMessage_isFailed() {
        assertEquals(OrderCancelResponseHelper.Kind.FAILED,
                OrderCancelResponseHelper.classify(FAILURE_SERVER));
        assertTrue(OrderCancelResponseHelper.isFailed(FAILURE_SERVER));
        assertFalse(OrderCancelResponseHelper.isConfirmed(FAILURE_SERVER));
        assertFalse(OrderCancelResponseHelper.isPending(FAILURE_SERVER));
    }

    @Test
    public void emptyOrNull_isFailed() {
        assertTrue(OrderCancelResponseHelper.isFailed(null));
        assertTrue(OrderCancelResponseHelper.isFailed(""));
        assertEquals(OrderCancelResponseHelper.Kind.FAILED, OrderCancelResponseHelper.classify(null));
    }

    @Test
    public void sentWithoutCanceledOrWaiting_isPending() {
        String onlySent = "Запит на скасування замовлення надіслано. ";
        assertTrue(OrderCancelResponseHelper.isPending(onlySent));
        assertFalse(OrderCancelResponseHelper.isConfirmed(onlySent));
    }

    @Test
    public void englishFailurePhrase_isFailed() {
        assertTrue(OrderCancelResponseHelper.isFailed("Order did not cancel on dispatch"));
    }

    @Test
    public void unknownMessage_isUnknown() {
        assertEquals(OrderCancelResponseHelper.Kind.UNKNOWN,
                OrderCancelResponseHelper.classify("200"));
    }

    @Test
    public void orderNotFound_isNotConfirmedPendingOrFailed() {
        assertEquals(OrderCancelResponseHelper.Kind.UNKNOWN,
                OrderCancelResponseHelper.classify("Замовлення не знайдено."));
    }

    @Test
    public void ignoreCanceledPush_whenCancelAwaitingButOrderStillActive() {
        assertFalse(OrderCancelResponseHelper.shouldAcceptServerCanceledPush(
                false, true, false, -1));
    }

    @Test
    public void acceptCanceledPush_whenCancelAwaitingAndPollShowsCanceled() {
        assertTrue(OrderCancelResponseHelper.shouldAcceptServerCanceledPush(
                false, true, true, -1));
    }

    @Test
    public void acceptCanceledPush_whenCancelAwaitingAndCloseReasonSettled() {
        assertTrue(OrderCancelResponseHelper.shouldAcceptServerCanceledPush(
                false, true, false, 1));
    }

    @Test
    public void ignoreCanceledPush_whenOrderStillActiveAndNoCancelRequest() {
        assertFalse(OrderCancelResponseHelper.shouldAcceptServerCanceledPush(
                false, false, false, -1));
    }

    @Test
    public void acceptCanceledPush_whenPollShowsCanceled() {
        assertTrue(OrderCancelResponseHelper.shouldAcceptServerCanceledPush(
                false, false, true, -1));
    }

    @Test
    public void refreshStatus_whenCanceledPushArrivesWhileWaitingForOperator() {
        assertTrue(OrderCancelResponseHelper.shouldRefreshStatusAfterCanceledPush(
                false, true, false));
    }

    @Test
    public void doNotRefreshStatus_whenPushAlreadyAcceptedOrOrderClosed() {
        assertFalse(OrderCancelResponseHelper.shouldRefreshStatusAfterCanceledPush(
                false, true, true));
        assertFalse(OrderCancelResponseHelper.shouldRefreshStatusAfterCanceledPush(
                true, true, false));
        assertFalse(OrderCancelResponseHelper.shouldRefreshStatusAfterCanceledPush(
                false, false, false));
    }

    @Test
    public void keepCancelWatch_whenSameOrderResumeWaitsForDispatch() {
        assertTrue(OrderCancelResponseHelper.shouldKeepCancelWatchOnSameOrder(true, true));
        assertTrue(OrderCancelResponseHelper.shouldKeepCancelWatchOnSameOrder(true, false));
        assertTrue(OrderCancelResponseHelper.shouldKeepCancelWatchOnSameOrder(false, true));
        assertFalse(OrderCancelResponseHelper.shouldKeepCancelWatchOnSameOrder(false, false));
    }

    @Test
    public void resumeCancelStatusWatch_afterBackgroundUntilScreenShowsCanceled() {
        assertTrue(OrderCancelResponseHelper.shouldResumeCancelStatusWatch(false, true, false));
        assertTrue(OrderCancelResponseHelper.shouldResumeCancelStatusWatch(false, false, true));
        assertFalse(OrderCancelResponseHelper.shouldResumeCancelStatusWatch(true, true, true));
        assertFalse(OrderCancelResponseHelper.shouldResumeCancelStatusWatch(false, false, false));
    }
}
