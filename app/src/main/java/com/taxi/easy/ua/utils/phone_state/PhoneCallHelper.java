package com.taxi.easy.ua.utils.phone_state;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;

import java.lang.ref.WeakReference;

public class PhoneCallHelper {

    private static final String TAG = "PhoneCallHelper";
    private static final String KEY_CITY_PHONE = "city_phone_number";

    private static SharedPreferencesHelper preferencesHelper;
    private static WeakReference<FragmentActivity> currentActivityRef;
    private static WeakReference<Fragment> currentFragmentRef;
    private static String pendingPhoneNumber = null;
    private static boolean isInitialized = false;

    // Launcher для разрешений
    private static ActivityResultLauncher<String> permissionLauncher;
    private static boolean launcherReady = false;

    /**
     * Инициализация хелпера (вызывать в MainActivity или в базовом фрагменте)
     */
    public static void init(FragmentActivity activity) {
        currentActivityRef = new WeakReference<>(activity);
        preferencesHelper = new SharedPreferencesHelper(activity);
        isInitialized = true;
    }

    public static void initWithActivity(FragmentActivity activity) {
        currentActivityRef = new WeakReference<>(activity);
        preferencesHelper = new SharedPreferencesHelper(activity);

        permissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (pendingPhoneNumber != null) {
                            makeCallDirect(pendingPhoneNumber);
                            pendingPhoneNumber = null;
                        }
                    } else {
                        if (pendingPhoneNumber != null) {
                            showPermissionDeniedMessage(getContext());
                            openDialer(pendingPhoneNumber, getContext());
                            pendingPhoneNumber = null;
                        }
                    }
                }
        );

        launcherReady = true;
        isInitialized = true;
    }
    public static void ensureCallPermission() {
        Context context = getContext();

        if (!(context instanceof FragmentActivity)) return;

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {

            if (launcherReady && permissionLauncher != null) {
                permissionLauncher.launch(Manifest.permission.CALL_PHONE);
            }
        }
    }
    /**
     * Инициализация с фрагментом (для RegisterForActivityResult)
     */
    public static void initWithFragment(Fragment fragment) {
        currentFragmentRef = new WeakReference<>(fragment);
        if (fragment.getActivity() != null) {
            currentActivityRef = new WeakReference<>(fragment.getActivity());
        }
        preferencesHelper = new SharedPreferencesHelper(fragment.requireContext());

        permissionLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        if (pendingPhoneNumber != null) {
                            makeCallDirect(pendingPhoneNumber);
                            pendingPhoneNumber = null;
                        }
                    } else {
                        if (pendingPhoneNumber != null) {
                            showPermissionDeniedMessage(getContext());
                            openDialer(pendingPhoneNumber, getContext());
                            pendingPhoneNumber = null;
                        }
                    }
                }
        );
        launcherReady = true;
        isInitialized = true;
    }

    /**
     * Сохранить номер телефона для города
     */
    public static void saveCityPhoneNumber(String phoneNumber) {
        if (preferencesHelper == null) return;
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            String cleanNumber = cleanPhoneNumber(phoneNumber);
            preferencesHelper.saveValue(KEY_CITY_PHONE, cleanNumber);
        }
    }

    /**
     * Получить сохранённый номер
     */
    public static String getCityPhoneNumber() {
        if (preferencesHelper == null) return "";
        Object phoneObj = preferencesHelper.getValue(KEY_CITY_PHONE, "");
        return phoneObj instanceof String ? (String) phoneObj : "";
    }

    /**
     * Проверить, сохранён ли номер
     */
    public static boolean hasCityPhoneNumber() {
        if (preferencesHelper == null) return false;
        return preferencesHelper.contains(KEY_CITY_PHONE);
    }

    /**
     * Позвонить (использует сохранённый номер)
     */
    public static void callCity() {
        Context context = getContext();

        if (!isInitialized) {
            showError(getString(context, R.string.phone_error_not_initialized), context);
            return;
        }

        if (!hasCityPhoneNumber()) {
            showError(getString(context, R.string.phone_error_no_saved_number), context);
            return;
        }

        String phoneNumber = getCityPhoneNumber();
        if (phoneNumber.isEmpty()) {
            showError(getString(context, R.string.phone_error_empty_phone), context);
            return;
        }

        if (context == null) {
            showError(getString(context, R.string.phone_error_context_unavailable), null);
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                == PackageManager.PERMISSION_GRANTED) {
            makeCallDirect(phoneNumber);
        } else {
            pendingPhoneNumber = phoneNumber;
            requestCallPermission();
        }
    }

    /**
     * Позвонить с автоматической загрузкой из провайдера, если нет в кеше
     * @param phoneProvider - интерфейс для получения номера
     */
    public static void callWithFallback(PhoneProvider phoneProvider) {
        Context context = getContext();

        if (!isInitialized) {
            showError(getString(context, R.string.phone_error_not_initialized), context);
            return;
        }

        if (hasCityPhoneNumber()) {
            callCity();
        } else if (phoneProvider != null) {
            String phoneFromProvider = phoneProvider.getPhoneNumber();
            if (phoneFromProvider != null && !phoneFromProvider.isEmpty()) {
                saveCityPhoneNumber(phoneFromProvider);
                callCity();
            } else {
                showError(getString(context, R.string.phone_error_no_phone_number), context);
            }
        } else {
            showError(getString(context, R.string.phone_error_no_phone_number), context);
        }
    }

    /**
     * Очистить сохранённый номер
     */
    public static void clearCityPhoneNumber() {
        if (preferencesHelper != null) {
            preferencesHelper.removeValue(KEY_CITY_PHONE);
        }
    }

    /**
     * Открыть набор номера
     */
    public static void openDialer(String phoneNumber) {
        openDialer(phoneNumber, getContext());
    }

    private static void openDialer(String phoneNumber, Context context) {
        if (context == null) return;
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + cleanPhoneNumber(phoneNumber)));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    /**
     * Позвонить водителю
     * @param phoneNumber - номер телефона водителя
     * @param context - контекст
     */
    public static void callDriver(String phoneNumber, Context context) {
        if (context == null) return;

        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(context, R.string.phone_error_driver_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanNumber = phoneNumber.replaceAll("[^\\d+]", "");
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + cleanNumber));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    private static void makeCallDirect(String phoneNumber) {
        Context context = getContext();
        if (context == null) return;

        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (SecurityException e) {
            showError(getString(context, R.string.phone_error_call_permission), context);
            openDialer(phoneNumber, context);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private static void requestCallPermission() {
        if (launcherReady && permissionLauncher != null) {
            permissionLauncher.launch(Manifest.permission.CALL_PHONE);
        } else {
            if (pendingPhoneNumber != null) {
                openDialer(pendingPhoneNumber);
                pendingPhoneNumber = null;
            }
        }
    }

    private static String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        return phoneNumber.replaceAll("[^\\d+]", "");
    }

    private static Context getContext() {
        if (currentFragmentRef != null && currentFragmentRef.get() != null) {
            return currentFragmentRef.get().getContext();
        }
        if (currentActivityRef != null && currentActivityRef.get() != null) {
            return currentActivityRef.get();
        }
        return null;
    }

    private static String getString(Context context, int resId) {
        if (context == null) return "";
        return context.getString(resId);
    }

    private static void showError(String message, Context context) {
        if (context == null || message.isEmpty()) return;
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private static void showPermissionDeniedMessage(Context context) {
        if (context == null) return;
        try {
            Toast.makeText(context,
                    getString(context, R.string.phone_permission_denied_message),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    // Универсальный интерфейс для получения номера
    public interface PhoneProvider {
        String getPhoneNumber();
    }
}