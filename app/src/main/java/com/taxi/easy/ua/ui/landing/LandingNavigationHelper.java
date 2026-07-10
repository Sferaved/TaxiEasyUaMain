package com.taxi.easy.ua.ui.landing;

import androidx.annotation.Nullable;

/**
 * Правила навигации с гостевой страницы: авторизация и выбор города.
 */
public final class LandingNavigationHelper {

    private LandingNavigationHelper() {
    }

    /** Звонок оператору, приложение водителя и смена языка — без входа. */
    public static boolean requiresAuth(@Nullable LandingAction action) {
        return action != LandingAction.OPERATOR
                && action != LandingAction.DRIVER
                && action != LandingAction.LANGUAGE;
    }

    /**
     * Перед переходом в раздел показать {@link com.taxi.easy.ua.ui.cities.check.CityCheckActivity},
     * если город ещё не выбирали. Кнопка «Город», звонок оператору и водитель — исключения.
     */
    /**
     * Оплата и язык: без полноэкранного выбора города — Киев по умолчанию.
     */
    public static boolean appliesDefaultCityInsteadOfPicker(@Nullable LandingAction action) {
        return action == LandingAction.PAYMENT || action == LandingAction.LANGUAGE;
    }

    public static boolean shouldPromptCityBeforeAction(@Nullable LandingAction action) {
        return action != null
                && action != LandingAction.CITY
                && action != LandingAction.OPERATOR
                && action != LandingAction.DRIVER
                && !appliesDefaultCityInsteadOfPicker(action);
    }

    /**
     * После входа CityCheckActivity только для заказа/просчёта без выбранного города.
     */
    public static boolean shouldLaunchCityCheckAfterAuth(@Nullable LandingAction pendingAction) {
        return shouldPromptCityBeforeAction(pendingAction);
    }
}
