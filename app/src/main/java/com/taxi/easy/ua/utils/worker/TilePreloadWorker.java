package com.taxi.easy.ua.utils.worker;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.net.HttpHeaders;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.utils.log.Logger;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class TilePreloadWorker extends Worker {
    private static final String TAG = "TilePreloadWorker";
    private static final double COORDINATE_THRESHOLD = 0.001; // ~100 метров
    private static final int OPTIMAL_ZOOM = 15; // Уменьшен для оптимизации
    private static final double OFFSET = 0.005;
    private static final double TAIL = 2;

    // Retrofit интерфейс
    interface TileService {
        @GET("{z}/{x}/{y}.png")
        Call<ResponseBody> downloadTile(
                @Path("z") int zoom,
                @Path("x") long x,
                @Path("y") long y
        );
    }

    public TilePreloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            GeoPoint newStartPoint = getStartPointFromDatabase();
            if (newStartPoint == null) {
                Logger.e(getApplicationContext(), TAG, "Точка старта не найдена в ROUT_MARKER");
                return Result.failure();
            }

            if (!hasStartPointChanged(newStartPoint)) {
                Logger.d(getApplicationContext(), TAG, "Точка старта не изменилась значительно, пропуск предзагрузки");
                return Result.success();
            }

            clearTileCache();
            preloadTiles(newStartPoint);
            saveLastStartPoint(newStartPoint);

            Logger.d(getApplicationContext(), TAG, "Предзагрузка тайлов завершена успешно");
            return Result.success();
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Ошибка в TilePreloadWorker: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            return Result.failure();
        }
    }

    private GeoPoint getStartPointFromDatabase() {
        SQLiteDatabase database = null;
        Cursor cursor = null;
        GeoPoint startPoint = null;
        try {
            database = getApplicationContext().openOrCreateDatabase(MainActivity.DB_NAME, Context.MODE_PRIVATE, null);
            cursor = database.rawQuery("SELECT startLat, startLan FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);
            if (cursor.moveToFirst()) {
                @SuppressLint("Range") double startLat = cursor.getDouble(cursor.getColumnIndex("startLat"));
                @SuppressLint("Range") double startLan = cursor.getDouble(cursor.getColumnIndex("startLan"));
                startPoint = new GeoPoint(startLat, startLan);
                Logger.d(getApplicationContext(), TAG, "Загружена точка старта из ROUT_MARKER: " + startPoint);
            } else {
                Logger.i(getApplicationContext(), TAG, "Данные в ROUT_MARKER не найдены");
            }
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Ошибка чтения ROUT_MARKER: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (cursor != null) cursor.close();
            if (database != null && database.isOpen()) database.close();
        }
        return startPoint;
    }

    private boolean hasStartPointChanged(GeoPoint newStartPoint) {
        String lastLat = (String) sharedPreferencesHelperMain.getValue("lastStartPointLat", "0.0");
        String lastLan = (String) sharedPreferencesHelperMain.getValue("lastStartPointLon", "0.0");
        double lastStartLat = Double.parseDouble(lastLat);
        double lastStartLan = Double.parseDouble(lastLan);

        double latDiff = Math.abs(newStartPoint.getLatitude() - lastStartLat);
        double lanDiff = Math.abs(newStartPoint.getLongitude() - lastStartLan);
        return latDiff > COORDINATE_THRESHOLD || lanDiff > COORDINATE_THRESHOLD;
    }

    private void clearTileCache() {
        File cacheDir = Configuration.getInstance().getOsmdroidTileCache();
        if (cacheDir.exists()) {
            for (File file : Objects.requireNonNull(cacheDir.listFiles())) {
                if (!file.getName().equals("cache.db")) {
                    deleteDirectory(file);
                }
            }
            Logger.d(getApplicationContext(), TAG, "Кэш тайлов очищен");
        }
    }

    private void deleteDirectory(File file) {
        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                deleteDirectory(child);
            }
        }
        file.delete();
    }


    private void preloadTiles(GeoPoint startPoint) {
    SqlTileWriter tileWriter = null;
    ExecutorService executor = null;

    try {
        File cacheDir = new File(getApplicationContext().getCacheDir(), "osmdroid");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Logger.e(getApplicationContext(), TAG, "Не удалось создать директорию кэша: " + cacheDir.getAbsolutePath());
            return;
        }
        Configuration.getInstance().setOsmdroidTileCache(cacheDir);
        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());

        if (!isConnected()) {
            Logger.e(getApplicationContext(), TAG, "Нет интернет-соединения");
            return;
        }

        File httpCacheDir = new File(getApplicationContext().getCacheDir(), "http_cache");
        if (!httpCacheDir.exists() && !httpCacheDir.mkdirs()) {
            Logger.e(getApplicationContext(), TAG, "Не удалось создать директорию кэша HTTP: " + httpCacheDir.getAbsolutePath());
        }
        Cache cache = new Cache(httpCacheDir, 10 * 1024 * 1024);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .cache(cache)
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header(HttpHeaders.USER_AGENT, getApplicationContext().getPackageName())
                                .build()))
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(TileSourceFactory.MAPNIK.getBaseUrl())
                .client(okHttpClient)
                .build();

        TileService tileService = retrofit.create(TileService.class);
        tileWriter = new SqlTileWriter();

        GeoPoint topLeft = new GeoPoint(startPoint.getLatitude() + OFFSET, startPoint.getLongitude() - OFFSET);
        GeoPoint bottomRight = new GeoPoint(startPoint.getLatitude() - OFFSET, startPoint.getLongitude() + OFFSET);

        double n = Math.pow(2, OPTIMAL_ZOOM); // Зум 16
        long xMin = (long) ((topLeft.getLongitude() + 180.0) / 360.0 * n);
        long xMax = (long) ((bottomRight.getLongitude() + 180.0) / 360.0 * n);
        long yMin = (long) ((1.0 - Math.log(Math.tan(Math.toRadians(topLeft.getLatitude())) + 1.0 / Math.cos(Math.toRadians(topLeft.getLatitude()))) / Math.PI) / 2.0 * n);
        long yMax = (long) ((1.0 - Math.log(Math.tan(Math.toRadians(bottomRight.getLatitude())) + 1.0 / Math.cos(Math.toRadians(bottomRight.getLatitude()))) / Math.PI) / 2.0 * n);

        if (xMin > xMax) { long t = xMin; xMin = xMax; xMax = t; }
        if (yMin > yMax) { long t = yMin; yMin = yMax; yMax = t; }

        int threadCount = 4; // Уменьшен для стабильности
        executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        int maxTiles = 50; // Ограничение на 50 тайлов
        int tileCount = 0;

        for (long x = xMin; x <= xMax && tileCount < maxTiles; x += TAIL) {
            for (long y = yMin; y <= yMax && tileCount < maxTiles; y += TAIL) {
                final long tileX = x;
                final long tileY = y;
                tileCount++;

                long tileIndex = MapTileIndex.getTileIndex(OPTIMAL_ZOOM, (int) tileX, (int) tileY);
                if (tileWriter.exists(TileSourceFactory.MAPNIK, tileIndex)) {
                    Logger.d(getApplicationContext(), TAG, "Тайл уже в кэше: Z=OPTIMAL_ZOOM, X=" + tileX + ", Y=" + tileY);
                    continue;
                }

                SqlTileWriter finalTileWriter = tileWriter;
                Future<?> future = executor.submit(() -> {
                    if (cancelFlag.get()) return;

                    if (!isConnected()) {
                        Logger.e(getApplicationContext(), TAG, "Потеряно соединение, отмена загрузки");
                        cancelFlag.set(true);
                        return;
                    }

                    try {
                        Response<ResponseBody> response = tileService.downloadTile(OPTIMAL_ZOOM, tileX, tileY).execute();
                        if (response.isSuccessful() && response.body() != null) {
                            byte[] data = response.body().bytes();
                            try (ByteArrayInputStream stream = new ByteArrayInputStream(data)) {
                                finalTileWriter.saveFile(TileSourceFactory.MAPNIK, tileIndex, stream,
                                        System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);
                                Logger.d(getApplicationContext(), TAG, "Сохранен тайл: Z=16, X=" + tileX + ", Y=" + tileY);
                            }
                        } else {
                            Logger.w(getApplicationContext(), TAG, "Ошибка загрузки: Z=16, X=" + tileX + ", Y=" + tileY);
                        }
                    } catch (IOException e) {
                        Logger.e(getApplicationContext(), TAG, "Ошибка при загрузке тайла: " + e.getMessage());
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                });
                futures.add(future);
            }
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                Logger.e(getApplicationContext(), TAG, "Ошибка ожидания завершения загрузки: " + e.getMessage());
            }
        }

        Logger.i(getApplicationContext(), TAG, "✅ Все тайлы успешно загружены и сохранены.");
        Logger.d(getApplicationContext(), TAG, "Пропущено " + (tileCount - futures.size()) + " тайлов, так как они уже в кэше");

    } catch (Exception e) {
        Logger.e(getApplicationContext(), TAG, "❌ Ошибка preloadTiles: " + e.getMessage());
        FirebaseCrashlytics.getInstance().recordException(e);
    } finally {
        if (tileWriter != null) {
            tileWriter.onDetach();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
    private void saveLastStartPoint(GeoPoint startPoint) {
        sharedPreferencesHelperMain.saveValue("lastStartPointLat", String.valueOf(startPoint.getLatitude()));
        sharedPreferencesHelperMain.saveValue("lastStartPointLon", String.valueOf(startPoint.getLongitude()));
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
