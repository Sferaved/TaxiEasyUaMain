package com.taxi.easy.ua.utils.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
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
    public static void evaluateAsync(Location location,
                                     Context context,
                                     LocationEvaluationCallback callback) {

        Logger.d(context, "LocationValidator", "=== evaluateAsync() START ===");

        if (location == null) {
            if (callback != null) callback.onResult(RiskLevel.BLOCK);
            return;
        }

        int riskScore = 0;

        // =========================
        // MOCK CHECK
        // =========================

        if (location.isFromMockProvider()) {
            Logger.w(context, "LocationValidator", "Mock provider detected");
            riskScore += 100;
        }

        // =========================
        // ACCURACY
        // =========================

        if (!location.hasAccuracy()) {
            Logger.w(context, "LocationValidator", "No accuracy");
            riskScore += 40;
        } else {

            float accuracy = location.getAccuracy();

            Logger.d(context, "LocationValidator",
                    "Accuracy = " + accuracy);

            if (accuracy > 100) {
                riskScore += 60;
            } else if (accuracy > 50) {
                riskScore += 30;
            } else if (accuracy > 20) {
                riskScore += 10;
            }
        }

        // =========================
        // SPEED
        // =========================

        if (location.hasSpeed()) {

            float speed = location.getSpeed();

            // > 216 km/h
            if (speed > 60f) {
                Logger.w(context, "LocationValidator",
                        "Unrealistic speed = " + speed);

                riskScore += 80;
            }

            // > 144 km/h
            else if (speed > 40f) {
                riskScore += 40;
            }
        }

        // =========================
        // COORDINATES
        // =========================

        if (location.getLatitude() == 0.0
                && location.getLongitude() == 0.0) {

            Logger.w(context, "LocationValidator", "(0,0) coordinates");

            riskScore += 100;
        }

        // =========================
        // ALTITUDE
        // =========================

        if (location.hasAltitude()) {

            double altitude = location.getAltitude();

            if (altitude < -100 || altitude > 9000) {

                Logger.w(context, "LocationValidator",
                        "Suspicious altitude = " + altitude);

                riskScore += 30;
            }
        }

        // =========================
        // PROVIDER
        // =========================

        String provider = location.getProvider();

        if (provider == null) {

            riskScore += 80;

        } else {

            Logger.d(context, "LocationValidator",
                    "Provider = " + provider);

            if (!provider.equals(LocationManager.GPS_PROVIDER)
                    && !provider.equals(LocationManager.NETWORK_PROVIDER)
                    && !provider.equals("fused")) {

                Logger.w(context, "LocationValidator",
                        "Unknown provider");

                riskScore += 60;
            }
        }

        // =========================
        // LOCATION AGE
        // =========================

        long ageMs =
                System.currentTimeMillis() - location.getTime();

        Logger.d(context, "LocationValidator",
                "Location age = " + ageMs);

        if (ageMs > 30000) {
            riskScore += 60;
        } else if (ageMs > 15000) {
            riskScore += 30;
        }

        // =========================
        // ELAPSED REALTIME CHECK
        // =========================

        long realtimeAgeMs =
                (android.os.SystemClock.elapsedRealtimeNanos()
                        - location.getElapsedRealtimeNanos())
                        / 1_000_000;

        Logger.d(context,
                "LocationValidator",
                "Realtime age = " + realtimeAgeMs);

        if (realtimeAgeMs > 10000) {

            Logger.w(context,
                    "LocationValidator",
                    "Old realtime location");

            riskScore += 40;
        }


        // =========================
        // GNSS CHECK
        // =========================

        if (gnssCheckDisabled) {

            finishRiskEvaluation(riskScore, callback);

        } else {

            final int currentRisk = riskScore;

            isRealGnssWorkingAsync(context, gnssWorking -> {

                int finalRisk = currentRisk;

                if (!gnssWorking) {

                    Logger.w(context,
                            "LocationValidator",
                            "GNSS not confirmed");

                    // НЕ BLOCK!
                    finalRisk += 35;
                }

                finishRiskEvaluation(finalRisk, callback);
            });
        }
    }


    private static void finishRiskEvaluation(int riskScore,
                                             LocationEvaluationCallback callback) {

        RiskLevel result;

        if (riskScore >= 100) {

            result = RiskLevel.BLOCK;

        } else if (riskScore >= 40) {

            result = RiskLevel.SUSPICIOUS;

        } else {

            result = RiskLevel.SAFE;
        }

        Logger.d(MyApplication.getContext(),
                "LocationValidator",
                "Final riskScore = " + riskScore
                        + ", result = " + result);

        if (callback != null) {
            callback.onResult(result);
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

                if (used >= 2 && total >= 5 && !callbackCalled.getAndSet(true)){
                    Logger.d(context, "LocationValidator", "Has fix! (used>=3) -> true");
                    handler.post(() -> {
                        try {
                            lm.unregisterGnssStatusCallback(this);
                        } catch (Exception e) {
                            Logger.e(context, "LocationValidator", "Error unregistering: " + e.getMessage());
                        }
                        if (callback != null) callback.onResult(true);
                        handler.removeCallbacksAndMessages(null);
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