package com.taxi.easy.ua.ui.landing;

/**
 * Когда показывать гостевой лендинг при старте / после обновления.
 * Авторизованный пользователь идёт сразу на заказ — без лендинга.
 */
public final class LandingIntroHelper {

    private LandingIntroHelper() {
    }

    /**
     * Интро после обновления — только гостю.
     *
     * @param storedVersionCode версия, для которой лендинг уже показывали (0 = ещё не показывали)
     * @param currentVersionCode {@code BuildConfig.VERSION_CODE} текущей сборки
     * @param isGuest гостевая сессия (не вошёл / вышел)
     */
    public static boolean shouldShowIntroAfterUpdate(int storedVersionCode,
                                                     int currentVersionCode,
                                                     boolean isGuest) {
        return isGuest && storedVersionCode < currentVersionCode;
    }

    /**
     * Холодный старт: лендинг только гостю.
     *
     * @param savedInstanceStateNull {@code savedInstanceState == null} в {@code onCreate}
     * @param isGuest гостевая сессия
     */
    public static boolean shouldOpenLandingOnColdStart(boolean savedInstanceStateNull,
                                                       boolean isGuest) {
        return savedInstanceStateNull && isGuest;
    }

    /**
     * Холодный старт авторизованного — сразу экран заказа.
     */
    public static boolean shouldEnterOrderOnColdStart(boolean savedInstanceStateNull,
                                                      boolean isGuest) {
        return savedInstanceStateNull && !isGuest;
    }
}
