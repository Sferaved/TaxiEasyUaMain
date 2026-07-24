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

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.taxi.easy.ua.utils.bugreport.mantis.MantisConfig;
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
    ListenerRegistration listenerWeatherKey;
    ListenerRegistration listenerCardPaymentKey;
    ListenerRegistration listenerUixCamKey;
    ListenerRegistration listenerBaseUrl;
    ListenerRegistration listenerBaseUrlDefaults;

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

    private static final String TAG = "FirestoreHelper";

    /**
     * Live-слушатель {@code keys/base_urls} — поля prod / test (без хардкода в APK).
     */
    public void listenBaseUrlDefaults(OnBaseUrlDefaultsFetchedListener listener) {
        if (listenerBaseUrlDefaults != null) {
            listenerBaseUrlDefaults.remove();
            listenerBaseUrlDefaults = null;
        }
        DocumentReference docRef = firestore.collection("keys").document("base_urls");
        listenerBaseUrlDefaults = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                String prod = documentSnapshot.getString("prod");
                String test = documentSnapshot.getString("test");
                if ((prod != null && !prod.trim().isEmpty())
                        || (test != null && !test.trim().isEmpty())) {
                    if (listener != null) {
                        listener.onSuccess(
                                prod != null ? prod.trim() : null,
                                test != null ? test.trim() : null);
                    }
                    return;
                }
            }
            if (listener != null) {
                listener.onFailure(new Exception(
                        "Документ keys/base_urls или поля prod/test не найдены."));
            }
        });
    }

    /**
     * Live-слушатель {@code city/{city}.base_url} — как у Visicom key.
     */
    public void listenBaseUrlForCity(
            @androidx.annotation.NonNull String city,
            OnBaseUrlFetchedListener listener
    ) {
        if (city.isEmpty()) {
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("City name cannot be null or empty"));
            }
            return;
        }
        if (listenerBaseUrl != null) {
            listenerBaseUrl.remove();
            listenerBaseUrl = null;
        }
        DocumentReference docRef = firestore.collection("city").document(city);
        listenerBaseUrl = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()
                    && documentSnapshot.contains("base_url")) {
                String baseUrl = documentSnapshot.getString("base_url");
                if (baseUrl != null && !baseUrl.trim().isEmpty()) {
                    if (listener != null) {
                        listener.onSuccess(baseUrl.trim());
                    }
                    return;
                }
            }
            if (listener != null) {
                listener.onFailure(new Exception(
                        "Поле base_url не найдено в документе city/" + city));
            }
        });
    }

    public void getCardPaymentKeyForCity(
            OnCardPaymentKeyFetchedListener listener,
            String city
    ) {
        Log.d(TAG, "=== getCardPaymentKeyForCity вызван ===");
        Log.d(TAG, "Город: '" + city + "'");

        // Проверка входных параметров
        if (city == null || city.isEmpty()) {
            Log.e(TAG, "ОШИБКА: название города null или пустое");
            if (listener != null) {
                listener.onFailure(new IllegalArgumentException("City name cannot be null or empty"));
            }
            return;
        }

        Log.d(TAG, "Создание ссылки на документ: city/" + city);
        DocumentReference docRef = firestore.collection("city").document(city);

        Log.d(TAG, "Добавление snapshot listener для города: " + city);
        listenerCardPaymentKey = docRef.addSnapshotListener((documentSnapshot, e) -> {

            if (e != null) {
                Log.e(TAG, "Ошибка Firestore: " + e.getMessage(), e);
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }

            Log.d(TAG, "Получен snapshot документа");

            if (documentSnapshot != null && documentSnapshot.exists()) {
                Log.d(TAG, "Документ существует, ID: " + documentSnapshot.getId());

                // Логируем все поля документа для отладки
                Log.d(TAG, "Все поля документа: " + documentSnapshot.getData());

                Boolean cardPayment = documentSnapshot.getBoolean("card_payment");
                Log.d(TAG, "Значение card_payment: " + cardPayment);

                if (cardPayment != null && listener != null) {
                    Log.d(TAG, "УСПЕХ: получено значение card_payment = " + cardPayment);
                    try {
                        listener.onSuccess(cardPayment);
                        Log.d(TAG, "Callback onSuccess выполнен успешно");
                    } catch (GeneralSecurityException | IOException ex) {
                        Log.e(TAG, "Исключение в onSuccess: " + ex.getMessage(), ex);
                        throw new RuntimeException(ex);
                    }
                } else if (listener != null) {
                    Log.w(TAG, "ПРЕДУПРЕЖДЕНИЕ: поле card_payment отсутствует в документе");
                    listener.onFailure(new Exception("Поле card_payment не найдено в документе"));
                }
            } else if (listener != null) {
                Log.w(TAG, "Документ для города '" + city + "' не существует или равен null");
                listener.onFailure(new Exception("Документ для города " + city + " не найден"));
            }

            Log.d(TAG, "=== Завершение обработки snapshot ===");
        });

        Log.d(TAG, "Snapshot listener добавлен, ожидание данных...");
    }

    public void getWeatherKey(OnVisicomKeyFetchedListener listener) {
        DocumentReference docRef = firestore.collection("keys").document("weather_key");

        listenerWeatherKey = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists() && documentSnapshot.contains("weather_key")) {
                String vKey = documentSnapshot.getString("weather_key");
                if (listener != null) {
                    try {
                        listener.onSuccess(vKey);
                    } catch (GeneralSecurityException | IOException ex) {
                        listener.onFailure(new RuntimeException(ex));
                    }
                }
            } else {
                if (listener != null) {
                    listener.onFailure(new Exception("Поле weather_key не найдено в документе или документ отсутствует."));
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
    public void getCrispKey(OnSupportCrispFetchedListener listener) {
        DocumentReference docRef = firestore.collection("keys").document("crispKey");

        listenerMapboxKey = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists() && documentSnapshot.contains("crispKey")) {
                String mKey = documentSnapshot.getString("crispKey");
                if (listener != null) {
                    listener.onSuccess(mKey);
                }
            } else {
                if (listener != null) {
                    listener.onFailure(new Exception("Поле crispKey не найдено в документе или документ отсутствует."));
                }
            }
        });
    }
  public MantisConfig fetchMantisConfigBlocking() throws Exception {
        DocumentSnapshot documentSnapshot = Tasks.await(
                firestore.collection("keys").document("mantis_key").get()
        );

        if (documentSnapshot == null || !documentSnapshot.exists()) {
            throw new Exception("Документ keys/mantis_key не знайдено.");
        }

        String apiToken = documentSnapshot.getString("api_token");
        String baseUrl = documentSnapshot.getString("base_url");
        Long projectId = documentSnapshot.getLong("project_id");
        Long categoryId = documentSnapshot.getLong("category_id");

        if (apiToken == null || apiToken.isEmpty()) {
            throw new Exception("Поле api_token не знайдено в keys/mantis_key.");
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new Exception("Поле base_url не знайдено в keys/mantis_key.");
        }
        if (projectId == null || categoryId == null) {
            throw new Exception("Поля project_id або category_id не знайдено в keys/mantis_key.");
        }

        return new MantisConfig(apiToken, baseUrl, projectId.intValue(), categoryId.intValue());
    }

    public void getUtaxKey(OnSupportUtaxFetchedListener listener) {
        DocumentReference docRef = firestore.collection("keys").document("utaxKey");

        listenerMapboxKey = docRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                if (listener != null) {
                    listener.onFailure(e);
                }
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists() && documentSnapshot.contains("utaxKey")) {
                String mKey = documentSnapshot.getString("utaxKey");
                if (listener != null) {
                    listener.onSuccess(mKey);
                }
            } else {
                if (listener != null) {
                    listener.onFailure(new Exception("Поле utaxKey не найдено в документе или документ отсутствует."));
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
                            if (MyApplication.sharedPreferencesHelperMain != null
                                    && "**".equals(MyApplication.sharedPreferencesHelperMain
                                    .getValue("CityCheckActivity", "**"))) {
                                Log.d("FirestoreHelper", "Skip nav_anr during first setup");
                                return;
                            }
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Activity activity = MyApplication.getCurrentActivity();
                                if (activity == null) {
                                    return;
                                }
                                NavController navController = Navigation.findNavController(
                                        activity, R.id.nav_host_fragment_content_main);
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
        if (listenerCardPaymentKey != null) {
            listenerCardPaymentKey.remove();
            listenerCardPaymentKey = null;
        }
        // listenerBaseUrl намеренно не снимаем: нужен на всё время жизни приложения
    }

    // Интерфейс для передачи результатов через callback
    public interface OnVisicomKeyFetchedListener {
        void onSuccess(String vKey) throws GeneralSecurityException, IOException;
        void onFailure(Exception e);
    }
    public interface OnCardPaymentKeyFetchedListener {
        void onSuccess(Boolean vKey) throws GeneralSecurityException, IOException;
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
    public interface OnSupportUtaxFetchedListener {
        void onSuccess(String utaxKey);
        void onFailure(Exception e);
    }
    public interface OnSupportCrispFetchedListener {
        void onSuccess(String crispKey);
        void onFailure(Exception e);
    }

    public interface OnBaseUrlFetchedListener {
        void onSuccess(String baseUrl);
        void onFailure(Exception e);
    }

    public interface OnBaseUrlDefaultsFetchedListener {
        void onSuccess(String prod, String test);
        void onFailure(Exception e);
    }
}
