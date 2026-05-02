package com.taxi.easy.ua.utils.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class TaxiLocationValidator {

    public enum RiskLevel {
        SAFE, SUSPICIOUS, BLOCK
    }

    // Флаг для отключения GNSS проверки (для тестирования на эмуляторе)
    private static boolean gnssCheckDisabled = false;

    /**
     * Асинхронная проверка локации
     */
    public static void evaluateAsync(Location location, Context context, LocationEvaluationCallback callback) {
        Logger.d(context, "LocationValidator", "=== evaluateAsync() START ===");
        Logger.d(context, "LocationValidator", "Location: " + (location != null ?
                "lat=" + location.getLatitude() + ", lon=" + location.getLongitude() : "null"));

        if (location == null) {
            Logger.e(context, "LocationValidator", "Location is NULL -> BLOCK");
            if (callback != null) callback.onResult(RiskLevel.BLOCK);
            return;
        }

        // 1. Mock проверка
        if (location.isFromMockProvider()) {
            Logger.w(context, "LocationValidator", "isFromMockProvider=true -> BLOCK");
            if (callback != null) callback.onResult(RiskLevel.BLOCK);
            return;
        }
        Logger.d(context, "LocationValidator", "isFromMockProvider=false -> OK");

        // 2. Точность
        if (!location.hasAccuracy() || location.getAccuracy() > 50) {
            Logger.w(context, "LocationValidator", "Accuracy=" + location.getAccuracy() + " -> SUSPICIOUS");
            if (callback != null) callback.onResult(RiskLevel.SUSPICIOUS);
            return;
        }
        Logger.d(context, "LocationValidator", "Accuracy=" + location.getAccuracy() + " -> OK");

        // 3. Скорость
        float speed = location.getSpeed();
        if (speed > 50) {
            Logger.w(context, "LocationValidator", "Speed=" + speed + " m/s -> SUSPICIOUS");
            if (callback != null) callback.onResult(RiskLevel.SUSPICIOUS);
            return;
        }
        Logger.d(context, "LocationValidator", "Speed=" + speed + " -> OK");

        // 4. Нулевые координаты
        if (location.getLatitude() == 0.0 && location.getLongitude() == 0.0) {
            Logger.w(context, "LocationValidator", "Coordinates are (0,0) -> BLOCK");
            if (callback != null) callback.onResult(RiskLevel.BLOCK);
            return;
        }

        // 5. Высота
        if (location.hasAltitude()) {
            double altitude = location.getAltitude();
            if (altitude < -100 || altitude > 9000) {
                Logger.w(context, "LocationValidator", "Altitude=" + altitude + " -> SUSPICIOUS");
                if (callback != null) callback.onResult(RiskLevel.SUSPICIOUS);
                return;
            }
            Logger.d(context, "LocationValidator", "Altitude=" + altitude + " -> OK");
        }

        // 6. ★★★ ГЛАВНАЯ ПРОВЕРКА: реальные спутники (асинхронная) ★★★
        if (gnssCheckDisabled) {
            Logger.w(context, "LocationValidator", "GNSS check DISABLED -> SAFE");
            if (callback != null) callback.onResult(RiskLevel.SAFE);
        } else {
            isRealGnssWorkingAsync(context, gnssWorking -> {
                Logger.d(context, "LocationValidator", "GNSS async result: " + gnssWorking);
                if (!gnssWorking) {
                    Logger.w(context, "LocationValidator", "GNSS NOT working -> BLOCK");
                    if (callback != null) callback.onResult(RiskLevel.BLOCK);
                } else {
                    Logger.d(context, "LocationValidator", "All checks passed -> SAFE");
                    if (callback != null) callback.onResult(RiskLevel.SAFE);
                }
            });
        }
    }

    /**
     * Асинхронная проверка, что устройство реально видит и использует спутники GNSS
     */
    @SuppressLint("MissingPermission")
    public static void isRealGnssWorkingAsync(Context context, GnssCallback callback) {
        Logger.d(context, "LocationValidator", "=== isRealGnssWorkingAsync() START ===");

        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            Logger.e(context, "LocationValidator", "LocationManager is null -> false");
            if (callback != null) callback.onResult(false);
            return;
        }

        // Проверяем, включён ли GPS в настройках
        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Logger.d(context, "LocationValidator", "GPS Provider enabled: " + gpsEnabled);
        if (!gpsEnabled) {
            Logger.w(context, "LocationValidator", "GPS Provider DISABLED -> false");
            if (callback != null) callback.onResult(false);
            return;
        }

        // Для старых версий Android не можем проверить спутники
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Logger.w(context, "LocationValidator", "Android version < N, assume working");
            if (callback != null) callback.onResult(true);
            return;
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        final AtomicBoolean callbackCalled = new AtomicBoolean(false);
        final int[] totalSatellites = {0};
        final int[] usedInFix = {0};

        GnssStatus.Callback gnssCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                int total = 0;
                int used = 0;

                for (int i = 0; i < status.getSatelliteCount(); i++) {
                    total++;
                    if (status.usedInFix(i)) used++;
                }

                totalSatellites[0] = total;
                usedInFix[0] = used;

                Logger.d(context, "LocationValidator", "GNSS callback: total=" + total + ", used=" + used);

                if (used >= 3 && !callbackCalled.getAndSet(true)) {
                    Logger.d(context, "LocationValidator", "Has fix! (used>=3) -> true");
                    handler.post(() -> {
                        try {
                            lm.unregisterGnssStatusCallback(this);
                        } catch (Exception e) {
                            Logger.e(context, "LocationValidator", "Error unregistering: " + e.getMessage());
                        }
                        if (callback != null) callback.onResult(true);
                    });
                }
            }
        };

        try {
            Logger.d(context, "LocationValidator", "Registering GNSS callback...");
            lm.registerGnssStatusCallback(gnssCallback);

            // Таймаут через 5 секунд (не блокируем поток)
            handler.postDelayed(() -> {
                if (!callbackCalled.getAndSet(true)) {
                    Logger.d(context, "LocationValidator", "Timeout! total=" + totalSatellites[0] + ", used=" + usedInFix[0]);
                    try {
                        lm.unregisterGnssStatusCallback(gnssCallback);
                    } catch (Exception e) {
                        Logger.e(context, "LocationValidator", "Error unregistering: " + e.getMessage());
                    }
                    if (callback != null) callback.onResult(false);
                }
            }, 5000);

        } catch (Exception e) {
            Logger.e(context, "LocationValidator", "Exception: " + e.getMessage());
            if (callback != null) callback.onResult(false);
        }
    }

    /**
     * Синхронная версия (для совместимости, но лучше не использовать)
     * Вызывает ANR, поэтому рекомендуется использовать асинхронную версию
     */
    @Deprecated
    public static boolean isRealGnssWorking(Context context) {
        Logger.w(context, "LocationValidator", "Using deprecated sync method! Use async version!");

        final AtomicBoolean result = new AtomicBoolean(false);
        final Object lock = new Object();

        isRealGnssWorkingAsync(context, working -> {
            synchronized (lock) {
                result.set(working);
                lock.notify();
            }
        });

        synchronized (lock) {
            try {
                lock.wait(6000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return result.get();
    }

    /**
     * Включить/отключить проверку GNSS (для тестирования на эмуляторе)
     */
    public static void setGnssCheckDisabled(boolean disabled) {
        gnssCheckDisabled = disabled;
        Logger.d(MyApplication.getContext(), "LocationValidator", "setGnssCheckDisabled: " + disabled);
    }

    public static boolean isGnssCheckDisabled() {
        return gnssCheckDisabled;
    }

    // ========== Callback интерфейсы ==========

    public interface LocationEvaluationCallback {
        void onResult(RiskLevel riskLevel);
    }

    public interface GnssCallback {
        void onResult(boolean isWorking);
    }
}