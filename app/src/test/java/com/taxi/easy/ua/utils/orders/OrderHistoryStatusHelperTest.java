package com.taxi.easy.ua.utils.orders;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OrderHistoryStatusHelperTest {

    @Test
    public void scheduledBookingWithStaleCloseReason_showsWaitingDispatch() {
        OrderHistoryStatusHelper.StatusKind kind = OrderHistoryStatusHelper.resolveKind(
                "-1",
                "WaitingCarSearch",
                "07.07.2026 21:47",
                null);

        assertEquals(OrderHistoryStatusHelper.StatusKind.WAITING_DISPATCH, kind);
    }

    @Test
    public void scheduledBookingWithoutExecutionStatus_usesRequiredTime() {
        OrderHistoryStatusHelper.StatusKind kind = OrderHistoryStatusHelper.resolveKind(
                "-1",
                null,
                "07.07.2026 21:47",
                null);

        assertEquals(OrderHistoryStatusHelper.StatusKind.WAITING_DISPATCH, kind);
    }

    @Test
    public void activeSearchWithoutBooking_showsInWork() {
        OrderHistoryStatusHelper.StatusKind kind = OrderHistoryStatusHelper.resolveKind(
                "-1",
                "SearchesForCar",
                null,
                null);

        assertEquals(OrderHistoryStatusHelper.StatusKind.IN_WORK, kind);
    }

    @Test
    public void canceledExecutionStatus_withActiveCloseReason_showsWaitingDispatch() {
        OrderHistoryStatusHelper.StatusKind kind = OrderHistoryStatusHelper.resolveKind(
                "-1",
                "Canceled",
                "07.07.2026 21:47",
                null);

        assertEquals(OrderHistoryStatusHelper.StatusKind.WAITING_DISPATCH, kind);
        assertFalse(OrderHistoryStatusHelper.isCanceled("-1", "Canceled", null));
    }

    @Test
    public void canceledExecutionStatus_withActiveCloseReason_withoutBooking_isNotCanceled() {
        OrderHistoryStatusHelper.StatusKind kind = OrderHistoryStatusHelper.resolveKind(
                "-1",
                "Canceled",
                null,
                null);

        assertEquals(OrderHistoryStatusHelper.StatusKind.IN_WORK, kind);
        assertFalse(OrderHistoryStatusHelper.isCanceled("-1", "Canceled", null));
    }

    @Test
    public void canceledExecutionStatus_withCancelCloseReason_isCanceled() {
        assertTrue(OrderHistoryStatusHelper.isCanceled("1", "Canceled", null));
    }

    @Test
    public void canceledExecutionStatus_whileClientCancelAwaiting_staysActiveUntilCloseReason() {
        assertFalse(OrderHistoryStatusHelper.isCanceled("-1", "Canceled", null, true));
        assertTrue(OrderHistoryStatusHelper.isCanceled("1", "Canceled", null, true));
    }

    @Test
    public void canceledExecutionStatus_withoutAwaiting_staysActive() {
        assertFalse(OrderHistoryStatusHelper.isCanceled("-1", "Canceled", null, false));
    }

    @Test
    public void closeReasonOne_isCanceled() {
        OrderHistoryStatusHelper.StatusKind kind = OrderHistoryStatusHelper.resolveKind(
                "1",
                null,
                null,
                null);

        assertEquals(OrderHistoryStatusHelper.StatusKind.CANCELED, kind);
    }

    @Test
    public void shouldDeferCancelUiDuringAddCost_whenInFlightOrRecoveryWindow() {
        long future = System.currentTimeMillis() + 60_000L;
        assertTrue(OrderHistoryStatusHelper.shouldDeferCancelUiDuringAddCost(true, 0L));
        assertTrue(OrderHistoryStatusHelper.shouldDeferCancelUiDuringAddCost(false, future));
        assertFalse(OrderHistoryStatusHelper.shouldDeferCancelUiDuringAddCost(false, 0L));
    }

    @Test
    public void nextAddCostRecoveryUntilMs_clearsWindowWhenCancelReEnabled() {
        long now = 1_000_000L;
        long window = 120_000L;
        long previous = now + window;
        assertEquals(0L, OrderHistoryStatusHelper.nextAddCostRecoveryUntilMs(
                true, false, now, window, previous));
        assertEquals(0L, OrderHistoryStatusHelper.nextAddCostRecoveryUntilMs(
                true, true, now, window, previous));
    }

    @Test
    public void nextAddCostRecoveryUntilMs_opensWindowWhenCancelDisabledDuringAddCost() {
        long now = 1_000_000L;
        long window = 120_000L;
        assertEquals(now + window, OrderHistoryStatusHelper.nextAddCostRecoveryUntilMs(
                false, true, now, window, 0L));
        assertEquals(55L, OrderHistoryStatusHelper.nextAddCostRecoveryUntilMs(
                false, false, now, window, 55L));
    }

    @Test
    public void shouldAbortResumeStatusPolling_blocksOnlyCancelOrArchive() {
        assertTrue(OrderHistoryStatusHelper.shouldAbortResumeStatusPolling(
                true, false, false, false, false));
        assertTrue(OrderHistoryStatusHelper.shouldAbortResumeStatusPolling(
                false, true, false, false, false));
        assertTrue(OrderHistoryStatusHelper.shouldAbortResumeStatusPolling(
                false, false, true, false, false));
        assertTrue(OrderHistoryStatusHelper.shouldAbortResumeStatusPolling(
                false, false, false, true, false));
        assertTrue(OrderHistoryStatusHelper.shouldAbortResumeStatusPolling(
                false, false, false, false, true));
        // add-cost / recovery alone — onResume must restart polling (Mantis #120)
        assertFalse(OrderHistoryStatusHelper.shouldAbortResumeStatusPolling(
                false, false, false, false, false));
    }

    @Test
    public void isScheduledBooking_rejectsEpochPlaceholder() {
        assertFalse(OrderHistoryStatusHelper.isScheduledBooking("01.01.1970 03:00:00"));
        assertTrue(OrderHistoryStatusHelper.isScheduledBooking("07.07.2026 21:47"));
    }
}
