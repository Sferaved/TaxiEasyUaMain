package com.taxi.easy.ua.utils.auth;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taxi.easy.ua.MainActivity;


public class FirebaseConsentManager {

    private static final String TAG = "FirebaseConsentManager";

    private final Activity activity;
    private final FirebaseAuth firebaseAuth;
    private final Handler mainHandler;

    public FirebaseConsentManager(Activity activity) {
        this.activity = activity;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    private void navigateToConsentScreen() {
        activity.runOnUiThread(() -> {
             Intent intent = new Intent(activity, MainActivity.class);
             activity.startActivity(intent);
        });
    }

    public void checkUserConsent(ConsentCallback callback) {
        checkUserConsentInternal(callback, 0);
    }

    private void checkUserConsentInternal(ConsentCallback callback, int nullUserAttempt) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            if (FirebaseConsentTokenHelper.shouldRetryForNullUser(nullUserAttempt)) {
                Log.w(TAG, "Firebase user not ready yet, retry "
                        + (nullUserAttempt + 1) + "/" + FirebaseConsentTokenHelper.NULL_USER_MAX_ATTEMPTS);
                mainHandler.postDelayed(
                        () -> checkUserConsentInternal(callback, nullUserAttempt + 1),
                        FirebaseConsentTokenHelper.NULL_USER_RETRY_DELAY_MS);
                return;
            }
            Log.w(TAG, "Пользователь не вошел в систему.");
            callback.onConsentInvalid();
            return;
        }

        Log.i(TAG, "Пользователь найден: " + currentUser.getEmail());
        requestToken(currentUser, callback);
    }

    private void requestToken(FirebaseUser currentUser, ConsentCallback callback) {
        currentUser.getIdToken(false).addOnCompleteListener(cachedTask -> {
            String cachedToken = cachedTask.isSuccessful()
                    ? FirebaseConsentTokenHelper.extractToken(cachedTask.getResult())
                    : null;

            if (FirebaseConsentTokenHelper.isNonEmptyToken(cachedToken)) {
                Log.d(TAG, "Кэшированный токен действителен.");
                callback.onConsentValid();
                return;
            }

            currentUser.getIdToken(true).addOnCompleteListener(refreshTask -> {
                String refreshedToken = refreshTask.isSuccessful()
                        ? FirebaseConsentTokenHelper.extractToken(refreshTask.getResult())
                        : null;

                if (FirebaseConsentTokenHelper.isNonEmptyToken(refreshedToken)) {
                    Log.d(TAG, "Токен пользователя обновлён.");
                    callback.onConsentValid();
                    return;
                }

                if (FirebaseConsentTokenHelper.isNonEmptyToken(cachedToken)) {
                    Log.w(TAG, "Обновление токена не удалось, используем кэшированный токен.");
                    callback.onConsentValid();
                    return;
                }

                Exception error = refreshTask.getException() != null
                        ? refreshTask.getException()
                        : cachedTask.getException();
                Log.e(TAG, "Ошибка при получении токена: ", error);
                callback.onConsentInvalid();
            });
        });
    }

    public void revokeTokenAndSignOut() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseAuth.signOut();
            Log.i(TAG, "Токен удалён, пользователь вышел из системы.");
        }
    }


    public interface ConsentCallback {
        void onConsentValid();
        void onConsentInvalid();
    }

}
