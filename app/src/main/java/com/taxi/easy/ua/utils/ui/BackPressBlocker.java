package com.taxi.easy.ua.utils.ui;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

public class BackPressBlocker {

    private boolean isBackButtonBlocked = false;

    // Метод для блокировки кнопки "Назад" с помощью OnBackPressedCallback
    public void blockBackButtonWithCallback(Fragment fragment) {
        // Добавляем колбэк на обработку кнопки "Назад"
        fragment.requireActivity().getOnBackPressedDispatcher().addCallback(
                fragment.getViewLifecycleOwner(), // Используем viewLifecycleOwner
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // Кнопка "Назад" заблокирована, ничего не делаем
                    }
                }
        );
    }

    // Метод для включения/выключения блокировки кнопки "Назад"
    public void setBackButtonBlocked(boolean block) {
        isBackButtonBlocked = block;
    }
}
