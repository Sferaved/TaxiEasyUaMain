package com.taxi.easy.ua.utils.user.save_firebase;

import android.util.Log;

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

}
