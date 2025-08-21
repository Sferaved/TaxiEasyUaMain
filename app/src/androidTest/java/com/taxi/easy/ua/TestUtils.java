package com.taxi.easy.ua;

import android.app.Activity;

import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ActivityScenario;

import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseUser;

import java.lang.reflect.Method;

public class TestUtils {

    public static void mockLogin(ActivityScenario<MainActivity> scenario,
                                 FirebaseAuthUIAuthenticationResult mockAuthResult) {
        scenario.onActivity(activity -> {
            try {
                // Эмулируем успешный вход
                org.mockito.Mockito.when(mockAuthResult.getResultCode())
                        .thenReturn(Activity.RESULT_OK);

                // Создаём мок-пользователя
                FirebaseUser mockUser = org.mockito.Mockito.mock(FirebaseUser.class);
                org.mockito.Mockito.when(mockUser.getEmail()).thenReturn("test@example.com");

                // Подставляем пользователя в MainActivity
                activity.setCurrentUserForTest(mockUser);

                // Вызываем приватный метод onSignInResult
                Method onSignInResult = MainActivity.class.getDeclaredMethod(
                        "onSignInResult",
                        FirebaseAuthUIAuthenticationResult.class,
                        FragmentManager.class
                );
                onSignInResult.setAccessible(true);
                onSignInResult.invoke(activity, mockAuthResult, activity.getSupportFragmentManager());

            } catch (Exception e) {
                throw new RuntimeException("Не удалось выполнить mockLogin", e);
            }
        });
    }

    /**
     * Эмулирует успешный вход пользователя в уже запущенной MainActivity
     */
    public static void mockLoginDirect(MainActivity activity, FirebaseAuthUIAuthenticationResult mockAuthResult) {
        try {
            // Говорим: авторизация прошла успешно
            org.mockito.Mockito.when(mockAuthResult.getResultCode())
                    .thenReturn(Activity.RESULT_OK);

            // Находим приватный метод onSignInResult
            Method onSignInResult = MainActivity.class.getDeclaredMethod(
                    "onSignInResult",
                    FirebaseAuthUIAuthenticationResult.class,
                    FragmentManager.class
            );
            onSignInResult.setAccessible(true);

            // Вызываем метод напрямую
            onSignInResult.invoke(activity, mockAuthResult, activity.getSupportFragmentManager());

        } catch (Exception e) {
            throw new RuntimeException("Не удалось выполнить mockLoginDirect", e);
        }
    }

}
