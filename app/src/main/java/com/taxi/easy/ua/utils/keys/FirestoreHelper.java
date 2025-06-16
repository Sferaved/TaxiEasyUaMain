package com.taxi.easy.ua.utils.keys;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.ui.exit.AnrActivity;

import java.io.IOException;
import java.security.GeneralSecurityException;


public class FirestoreHelper {
    private final FirebaseFirestore firestore;
    private final Context context;
    ListenerRegistration listenerVisicomKey;
    ListenerRegistration listenerMapboxKey;
    ListenerRegistration listenerUixCamKey;

    public FirestoreHelper(Context context) {
        this.firestore = FirebaseFirestore.getInstance();
        this.context = context;
    }
    public void getVisicomKey(OnVisicomKeyFetchedListener listener) {
        DocumentReference docRef = firestore.collection("keys").document("visicom_key");

        listenerVisicomKey = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists() && documentSnapshot.contains("v_key")) {
                String vKey = documentSnapshot.getString("v_key");
                if (listener != null) {
                    try {
                        listener.onSuccess(vKey);
                    } catch (GeneralSecurityException | IOException ex) {
                        listener.onFailure(new RuntimeException(ex));
                    }
                }
            } else {
                if (listener != null) {
                    listener.onFailure(new Exception("Поле v_key не найдено в документе или документ отсутствует."));
                }
            }
        });
    }

    public void getMapboxKey(OnMapboxKeyFetchedListener listener) {
        DocumentReference docRef = firestore.collection("keys").document("mapbox_key");

        listenerMapboxKey = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists() && documentSnapshot.contains("m_key")) {
                String mKey = documentSnapshot.getString("m_key");
                if (listener != null) {
                    listener.onSuccess(mKey);
                }
            } else {
                if (listener != null) {
                    listener.onFailure(new Exception("Поле m_key не найдено в документе или документ отсутствует."));
                }
            }
        });
    }


    public void getUixCamKey(OnVisicomKeyFetchedListener listener) {
        DocumentReference docRef = firestore.collection("keys").document("uixcam_key");

        listenerUixCamKey = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists() && documentSnapshot.contains("u_key")) {
                String uKey = documentSnapshot.getString("u_key");
                if (listener != null) {
                    try {
                        listener.onSuccess(uKey);
                    } catch (GeneralSecurityException | IOException ex) {
                        listener.onFailure(new RuntimeException(ex));
                    }
                }
            } else {
                if (listener != null) {
                    listener.onFailure(new Exception("Поле u_key не найдено в документе или документ отсутствует."));
                }
            }
        });
    }
    public void getSupportEmail(OnSupportEmailFetchedListener listener) {
        DocumentReference docRef = firestore.collection("keys").document("mail");

        listenerMapboxKey = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists() && documentSnapshot.contains("email")) {
                String mKey = documentSnapshot.getString("email");
                if (listener != null) {
                    listener.onSuccess(mKey);
                }
            } else {
                if (listener != null) {
                    listener.onFailure(new Exception("Поле email не найдено в документе или документ отсутствует."));
                }
            }
        });
    }

    public void listenForResponseChanges() {
        firestore.collection("settings")
                .document("active")
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e("FirestoreHelper", "Listen failed: ", error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Boolean respons = documentSnapshot.getBoolean("respons");
                        if (respons != null && !respons) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                NavController navController = Navigation.findNavController(MyApplication.getCurrentActivity(), R.id.nav_host_fragment_content_main);
                                navController.navigate(R.id.nav_anr, null, new NavOptions.Builder()
                                        .build());
//                                Intent intent = new Intent(context, AnrActivity.class);
//                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                context.startActivity(intent);
                            });
                        } else if (respons != null) {
                            Activity activity = MyApplication.getCurrentActivity();
                            if (activity != null) {
                                Log.d("MyAppDebug", "Текущая активность: " + activity.getClass().getSimpleName());

                                if (activity instanceof AnrActivity) {
                                    Log.d("MyAppDebug", "Активность — AnrActivity. Выполняем переход.");

                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        Intent intent = new Intent(activity, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        activity.startActivity(intent);
                                    });
                                } else {
                                    Log.d("MyAppDebug", "Текущая активность не AnrActivity. Переход не выполняется.");
                                }
                            } else {
                                Log.w("MyAppDebug", "Не удалось получить текущую активность (null).");
                            }

                        }
                    }
                });
    }
    public void stopListening() {
        if (listenerVisicomKey != null) {
            listenerVisicomKey.remove();
            listenerVisicomKey = null;
        }
        if (listenerMapboxKey != null) {
            listenerMapboxKey.remove();
            listenerMapboxKey = null;
        }
        if (listenerUixCamKey != null) {
            listenerUixCamKey.remove();
            listenerUixCamKey = null;
        }
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
    public interface OnSupportEmailFetchedListener {
        void onSuccess(String supportEmail);
        void onFailure(Exception e);
    }
}
