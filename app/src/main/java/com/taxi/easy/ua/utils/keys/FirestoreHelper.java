package com.taxi.easy.ua.utils.keys;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FirestoreHelper {
    private final FirebaseFirestore firestore;

    public FirestoreHelper() {
        firestore = FirebaseFirestore.getInstance();
    }

    public void getVisicomKey(OnVisicomKeyFetchedListener listener) {
        // Ссылка на коллекцию и документ
        DocumentReference docRef = firestore.collection("keys").document("visicom_key");

        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("v_key")) {
                        String vKey = documentSnapshot.getString("v_key");
                        if (listener != null) {
                            listener.onSuccess(vKey);
                        }
                    } else {
                        if (listener != null) {
                            listener.onFailure(new Exception("Поле v_key не найдено в документе или документ отсутствует."));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    public void getMapboxKey(OnMapboxKeyFetchedListener listener) {
        // Ссылка на коллекцию и документ
        DocumentReference docRef = firestore.collection("keys").document("mapbox_key");

        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("m_key")) {
                        String vKey = documentSnapshot.getString("m_key");
                        if (listener != null) {
                            listener.onSuccess(vKey);
                        }
                    } else {
                        if (listener != null) {
                            listener.onFailure(new Exception("Поле m_key не найдено в документе или документ отсутствует."));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    // Интерфейс для передачи результатов через callback
    public interface OnVisicomKeyFetchedListener {
        void onSuccess(String vKey);
        void onFailure(Exception e);
    }

    public interface OnMapboxKeyFetchedListener {
        void onSuccess(String mKey);
        void onFailure(Exception e);
    }
}
