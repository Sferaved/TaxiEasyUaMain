package com.taxi.easy.ua.utils.user.save_firebase;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FirebaseUserManager {
    private static final String TAG = "FirebaseUserManager";
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    public FirebaseUserManager() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    // Метод для сохранения номера телефона пользователя

    public void saveUserPhone(String phone) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // Ссылка на документ
            DocumentReference userDocRef = firestore.collection("users").document(userId);

            // Создание документа с полем "phone" или обновление существующего документа
            userDocRef.set(new HashMap<String, Object>() {{
                        put("phone", phone);
                    }}, SetOptions.merge()) // Используем SetOptions.merge() чтобы добавить поле если документ существует
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Phone successfully saved"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving phone", e));
        } else {
            Log.e(TAG, "No current user available to save phone");
        }

    }

    // Метод для получения номера телефона пользователя по userId
    public void getUserPhoneById(String userId, final UserPhoneCallback callback) {
        Log.d(TAG, "getUserPhoneById userId: " + userId);
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String phone = documentSnapshot.getString("phone");
                        Log.d(TAG, "getUserPhoneById phone: " + phone);
                        if (phone != null) {
                            callback.onUserPhoneRetrieved(phone);
                        } else {
                            Log.e(TAG, "Phone is null");
                        }
                    } else {
                        Log.e(TAG, "No document found for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "getUserPhoneById: Error reading data", e));
    }

    // Интерфейс обратного вызова для получения номера телефона
    public interface UserPhoneCallback {
        void onUserPhoneRetrieved(String phone);
    }

    public void deleteUserPhone() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            // Ссылка на документ
            DocumentReference userDocRef = firestore.collection("users").document(userId);

            // Удаление поля "phone" из документа
            Map<String, Object> updates = new HashMap<>();
            updates.put("phone", FieldValue.delete());

            userDocRef.update(updates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Phone successfully deleted"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error deleting phone", e));
        } else {
            Log.e(TAG, "No current user available to delete phone");
        }
    }



    /**
     * Удаляет данные пользователя из Firestore (документ users/{userId}).
     * @return Task<Void>, который завершается успешно, если данные удалены или документ не существует, или с ошибкой в случае сбоя.
     */
    public Task<Void> deleteUserData() {
        Log.d(TAG, "deleteUserData(): Начало выполнения");

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.i(TAG, "deleteUserData(): Пользователь аутентифицирован, UID: " + userId);

            // Проверяем токен перед операцией
            return currentUser.getIdToken(true).continueWithTask(tokenTask -> {
                if (tokenTask.isSuccessful()) {
                    Log.d(TAG, "deleteUserData(): Токен успешно обновлен для UID: " + userId);
                } else {
                    Log.e(TAG, "deleteUserData(): Ошибка обновления токена: " + tokenTask.getException().getMessage(), tokenTask.getException());
                    throw tokenTask.getException();
                }

                DocumentReference userDocRef = firestore.collection("users").document(userId);
                Log.d(TAG, "deleteUserData(): Создана ссылка на документ: users/" + userId);

                // Прямое удаление без проверки существования
                Log.i(TAG, "deleteUserData(): Прямое удаление документа users/" + userId);
                return userDocRef.delete();
            }).addOnSuccessListener(aVoid -> {
                Log.i(TAG, "deleteUserData(): Успешно удален документ users/" + userId);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "deleteUserData(): Ошибка удаления документа users/" + userId + ": " + e.getMessage(), e);
                if (e instanceof com.google.firebase.firestore.FirebaseFirestoreException) {
                    com.google.firebase.firestore.FirebaseFirestoreException firestoreException =
                            (com.google.firebase.firestore.FirebaseFirestoreException) e;
                    Log.e(TAG, "deleteUserData(): Код ошибки Firestore: " + firestoreException.getCode());
                    if (firestoreException.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.e(TAG, "deleteUserData(): Причина: Недостаточно прав, несмотря на правила Firestore");
                    } else if (firestoreException.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE) {
                        Log.e(TAG, "deleteUserData(): Причина: Служба Firestore недоступна, проверьте сеть");
                    }
                }
            }).addOnCompleteListener(task -> {
                Log.d(TAG, "deleteUserData(): Операция для users/" + userId + " завершена с состоянием: " + (task.isSuccessful() ? "успех" : "ошибка"));
                if (!task.isSuccessful() && task.getException() != null) {
                    Log.e(TAG, "deleteUserData(): Подробности ошибки: " + task.getException().getMessage(), task.getException());
                }
            });
        } else {
            Log.e(TAG, "deleteUserData(): Пользователь не аутентифицирован, невозможно удалить данные");
            return Tasks.forException(new Exception("Пользователь не аутентифицирован"));
        }
    }




}
