package com.taxi.easy.ua.ui.finish;

/**
 * Ответ статуса заказа нельзя применять, если экран заказа уже снят
 * или запрос отменили при уходе с экрана. Иначе вызов FragmentManager падает.
 */
public final class FinishStatusCallbackGuard {

    private FinishStatusCallbackGuard() {
    }

    public static boolean shouldApply(boolean fragmentAdded, boolean callCanceled) {
        return fragmentAdded && !callCanceled;
    }
}
