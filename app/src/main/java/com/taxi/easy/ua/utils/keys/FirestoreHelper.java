package com.taxi.easy.ua.utils.keys;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.security.GeneralSecurityException;

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
                            try {
                                listener.onSuccess(vKey);
                            } catch (GeneralSecurityException | IOException e) {
                                throw new RuntimeException(e);
                            }
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


    public void getUixCamKey(OnVisicomKeyFetchedListener listener) {
        // Ссылка на коллекцию и документ
        DocumentReference docRef = firestore.collection("keys").document("uixcam_key");

        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("u_key")) {
                        String uKey = documentSnapshot.getString("u_key");
                        if (listener != null) {
                            try {
                                listener.onSuccess(uKey);
                            } catch (GeneralSecurityException | IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        if (listener != null) {
                            listener.onFailure(new Exception("Поле u_key не найдено в документе или документ отсутствует."));
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
        void onSuccess(String vKey) throws GeneralSecurityException, IOException;
        void onFailure(Exception e);
    }

    public interface OnMapboxKeyFetchedListener {
        void onSuccess(String mKey);
        void onFailure(Exception e);
    }
}
