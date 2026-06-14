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
    public void canceledExecutionStatus_withActiveCloseReason_staysInWork() {
        OrderHistoryStatusHelper.StatusKind kind = OrderHistoryStatusHelper.resolveKind(
                "-1",
                "Canceled",
                "07.07.2026 21:47",
                null);

        assertEquals(OrderHistoryStatusHelper.StatusKind.WAITING_DISPATCH, kind);
    }

    @Test
    public void canceledExecutionStatus_withActiveCloseReason_withoutBooking_isInWork() {
        OrderHistoryStatusHelper.StatusKind kind = OrderHistoryStatusHelper.resolveKind(
                "-1",
                "Canceled",
                null,
                null);

        assertEquals(OrderHistoryStatusHelper.StatusKind.IN_WORK, kind);
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
    public void isScheduledBooking_rejectsEpochPlaceholder() {
        assertFalse(OrderHistoryStatusHelper.isScheduledBooking("01.01.1970 03:00:00"));
        assertTrue(OrderHistoryStatusHelper.isScheduledBooking("07.07.2026 21:47"));
    }
}
