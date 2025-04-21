package com.taxi.easy.ua.utils.auth;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taxi.easy.ua.MainActivity;


public class FirebaseConsentManager {

    private Activity activity;
    private FirebaseAuth firebaseAuth;

    public FirebaseConsentManager(Activity activity) {
        this.activity = activity;
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    private void navigateToConsentScreen() {
        // Здесь вы можете реализовать переход на экран повторного запроса согласия
        // Например:
        activity.runOnUiThread(() -> {
            // Создайте Intent для нового экрана
             Intent intent = new Intent(activity, MainActivity.class);
             activity.startActivity(intent);
        });
    }

    public void checkUserConsent(ConsentCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            // Логируем отсутствие пользователя
            Log.w("FirebaseConsentManager", "Пользователь не вошел в систему.");
            callback.onConsentInvalid();
        } else {
            // Логируем, что пользователь найден
            Log.i("FirebaseConsentManager", "Пользователь найден: " + currentUser.getEmail());

            // Проверяем токен пользователя
            currentUser.getIdToken(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Логируем успешное получение токена
                    Log.d("FirebaseConsentManager", "Токен пользователя действителен.");
                    callback.onConsentValid();
                } else {
                    // Логируем ошибку при получении токена
                    Log.e("FirebaseConsentManager", "Ошибка при получении токена: ", task.getException());
                    callback.onConsentInvalid();
                }
            });
        }
    }

    // Метод для удаления токена и выхода пользователя
    public void revokeTokenAndSignOut() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            // Выход из Firebase
            firebaseAuth.signOut();
            Log.i("FirebaseConsentManager", "Токен удалён, пользователь вышел из системы.");
        }
    }


    public interface ConsentCallback {
        void onConsentValid();
        void onConsentInvalid();
    }

}