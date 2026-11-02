package com.taxi.easy.ua.ui.visicom;

/**
 * Не даёт расчёту стоимости вызвать сам себя, пока предыдущий вызов ещё на стеке.
 */
public final class CostRequestGate {

    private boolean entered;

    public boolean tryEnter() {
        if (entered) {
            return false;
        }
        entered = true;
        return true;
    }

    public void exit() {
        entered = false;
    }

    public boolean isEntered() {
        return entered;
    }
}
