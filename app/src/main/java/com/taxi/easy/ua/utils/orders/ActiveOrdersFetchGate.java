package com.taxi.easy.ua.utils.orders;

/**
 * Prevents overlapping API requests for the active-orders list.
 */
public final class ActiveOrdersFetchGate {

    private boolean inFlight;

    public synchronized boolean tryBegin() {
        if (inFlight) {
            return false;
        }
        inFlight = true;
        return true;
    }

    public synchronized void end() {
        inFlight = false;
    }

    public synchronized boolean isInFlight() {
        return inFlight;
    }
}
