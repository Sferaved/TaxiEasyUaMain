package com.taxi.easy.ua.utils.worker;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
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
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class TilePreloadWorker extends Worker {
    private static final String TAG = "TilePreloadWorker";
    private static final double COORDINATE_THRESHOLD = 0.001; // Coordinate change threshold (~100 meters)
    private static final int OPTIMAL_ZOOM = 16; // Optimal zoom level
    private static final double OFFSET = 0.03; // ~3 km in degrees

    // Retrofit service interface
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
            // Retrieve coordinates from ROUT_MARKER
            GeoPoint newStartPoint = getStartPointFromDatabase();
            if (newStartPoint == null) {
                Logger.e(getApplicationContext(), TAG, "No start point found in ROUT_MARKER");
                return Result.failure();
            }

            // Check if coordinates have changed significantly
            if (!hasStartPointChanged(newStartPoint)) {
                Logger.d(getApplicationContext(), TAG, "Start point has not changed significantly, skipping tile preload");
                return Result.success();
            }

            // Clear tile cache, preserving cache.db
            clearTileCache();

            // Preload tiles
            preloadTiles(newStartPoint);

            // Save new coordinates as last known
            saveLastStartPoint(newStartPoint);

            Logger.d(getApplicationContext(), TAG, "Tile preload completed successfully");
            return Result.success();
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Error in TilePreloadWorker: " + e.getMessage());
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
                Logger.d(getApplicationContext(), TAG, "Loaded start point from ROUT_MARKER: " + startPoint);
            } else {
                Logger.i(getApplicationContext(), TAG, "No data found in ROUT_MARKER");
            }
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Error reading ROUT_MARKER: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (cursor != null) cursor.close();
            if (database != null && database.isOpen()) database.close();
        }
        return startPoint;
    }

    private boolean hasStartPointChanged(GeoPoint newStartPoint) {
        String lastLat = (String) sharedPreferencesHelperMain.getValue("lastStartLat", "0.0");
        String lastLan = (String) sharedPreferencesHelperMain.getValue("lastStartLan", "0.0");
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
            Logger.d(getApplicationContext(), TAG, "Tile cache cleared");
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
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            // Check and create cache directory
            File cacheDir = Configuration.getInstance().getOsmdroidTileCache();
            File dbFile = new File(cacheDir, "cache.db");
            if (!Objects.requireNonNull(dbFile.getParentFile()).isDirectory()) {
                cacheDir.mkdirs();
                Logger.d(getApplicationContext(), TAG, "Created cache directory: " + cacheDir.getPath());
            }

            // Check and update/create cache database
            if (!dbFile.exists() || !isDatabaseValid(dbFile)) {
                Logger.d(getApplicationContext(), TAG, "Cache database missing or invalid, creating new one");
                createNewTileCacheDatabase();
            }

            // Check network connectivity
            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            if (!isConnected) {
                Logger.e(getApplicationContext(), TAG, "No network connection, cannot preload tiles");
                return;
            }

            // Configure OSMDroid
            Configuration.getInstance().setCacheMapTileCount((short) 12);
            Configuration.getInstance().setTileFileSystemCacheMaxBytes(1024 * 1024 * 50L);
            Configuration.getInstance().setTileDownloadMaxQueueSize((short) 40);
            Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());

            // Initialize SqlTileWriter
            SqlTileWriter tileWriter = new SqlTileWriter();

            try {
                db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            } catch (SQLiteDiskIOException e) {
                Logger.e(getApplicationContext(), TAG, "Ошибка ввода-вывода при открытии базы данных: " + e.getMessage());
                // Логика восстановления или уведомление пользователя
            } finally {
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }

            // Setup Retrofit
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .readTimeout(Duration.ofSeconds(5))
                    .addInterceptor(chain -> chain.proceed(
                            chain.request().newBuilder()
                                    .header(HttpHeaders.USER_AGENT, getApplicationContext().getPackageName())
                                    .build()
                    ))
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(TileSourceFactory.MAPNIK.getBaseUrl())
                    .client(okHttpClient)
                    .build();

            TileService tileService = retrofit.create(TileService.class);

            // Define tile download area
            GeoPoint topLeft = new GeoPoint(startPoint.getLatitude() + OFFSET, startPoint.getLongitude() - OFFSET);
            GeoPoint bottomRight = new GeoPoint(startPoint.getLatitude() - OFFSET, startPoint.getLongitude() + OFFSET);

            // Calculate tile indices
            double n = Math.pow(2, OPTIMAL_ZOOM);
            long tileXMin = (long) ((topLeft.getLongitude() + 180.0) / 360.0 * n);
            long tileXMax = (long) ((bottomRight.getLongitude() + 180.0) / 360.0 * n);
            long tileYMin = (long) ((1.0 - Math.log(Math.tan(Math.toRadians(topLeft.getLatitude())) + 1.0 / Math.cos(Math.toRadians(topLeft.getLatitude()))) / Math.PI) / 2.0 * n);
            long tileYMax = (long) ((1.0 - Math.log(Math.tan(Math.toRadians(bottomRight.getLatitude())) + 1.0 / Math.cos(Math.toRadians(bottomRight.getLatitude()))) / Math.PI) / 2.0 * n);

            // Adjust min/max order
            if (tileXMin > tileXMax) {
                long temp = tileXMin;
                tileXMin = tileXMax;
                tileXMax = temp;
            }
            if (tileYMin > tileYMax) {
                long temp = tileYMin;
                tileYMin = tileYMax;
                tileYMax = temp;
            }

            // Download tiles using Retrofit
            String providerId = "mapnik"; // Hardcoded provider identifier for MAPNIK
            for (long x = tileXMin; x <= tileXMax; x++) {
                for (long y = tileYMin; y <= tileYMax; y++) {
                    if (!isConnected()) {
                        Logger.e(getApplicationContext(), TAG, "Network disconnected during tile download");
                        return;
                    }
                    try {
                        long tileKey = MapTileIndex.getTileIndex(OPTIMAL_ZOOM, (int) x, (int) y);

                        // Check if tile exists in cache using SQL
                        cursor = db.rawQuery("SELECT 1 FROM tiles WHERE [key] = ? AND provider = ?",
                                new String[]{String.valueOf(tileKey), providerId});
                        boolean tileExists = cursor.moveToFirst();
                        cursor.close();

                        if (!tileExists) {
                            // Download tile using Retrofit
                            Call<ResponseBody> call = tileService.downloadTile(OPTIMAL_ZOOM, x, y);
                            retrofit2.Response<ResponseBody> response = call.execute();
                            if (response.isSuccessful() && response.body() != null) {
                                byte[] tileData = response.body().bytes();
                                // Convert byte[] to InputStream
                                ByteArrayInputStream tileInputStream = new ByteArrayInputStream(tileData);
                                tileWriter.saveFile(TileSourceFactory.MAPNIK, tileKey, tileInputStream, System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
                                tileInputStream.close();
                                Logger.i(getApplicationContext(), TAG,"Loaded tile: z=" + OPTIMAL_ZOOM + ", x=" + x + ", y=" + y);
                            } else {
                                Logger.e(getApplicationContext(), TAG, "Failed to download tile: z=" + OPTIMAL_ZOOM + ", x=" + x + ", y=" + y + ", HTTP code: " + response.code());
                            }
                        } else {
                            Logger.i(getApplicationContext(), TAG, "Tile already in cache: z=" + OPTIMAL_ZOOM + ", x=" + x + ", y=" + y);
                        }
                    } catch (IOException e) {
                        Logger.e(getApplicationContext(), TAG, "Failed to load tile: z=" + OPTIMAL_ZOOM + ", x=" + x + ", y=" + y + ", error: " + e.getMessage());
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                }
            }

            // Check cache
            long tileCount = tileWriter.getRowCount("tiles");
            if (tileCount < 1) {
                Logger.e(getApplicationContext(), TAG, "No tiles loaded: " + tileCount);
            } else {
                Logger.i(getApplicationContext(), TAG,"Total tiles in cache: " + tileCount);
            }

            Logger.d(getApplicationContext(), TAG, "Tile preload completed around: " + startPoint);
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Error preloading tiles: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    @SuppressLint("Range")
    private boolean isDatabaseValid(File dbFile) {
        SQLiteDatabase db = null;
        try {
            db = SQLiteDatabase.openDatabase(dbFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);

            Cursor tableCursor = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'tiles'", null);
            boolean hasTable = tableCursor.moveToFirst();
            tableCursor.close();
            if (!hasTable) {
                Logger.e(getApplicationContext(), TAG, "Cache database is invalid: tiles table missing");
                return false;
            }

            // Check for expires column
            Cursor columnCursor = db.rawQuery("PRAGMA table_info(tiles)", null);
            boolean hasExpiresColumn = false;
            while (columnCursor.moveToNext()) {
                if ("expires".equalsIgnoreCase(columnCursor.getString(columnCursor.getColumnIndex("name")))) {
                    hasExpiresColumn = true;
                    break;
                }
            }
            columnCursor.close();

            if (!hasExpiresColumn) {
                db.execSQL("ALTER TABLE tiles ADD COLUMN expires INTEGER");
                Logger.i(getApplicationContext(), TAG,"Added expires column to tiles table");
                db.execSQL("CREATE INDEX IF NOT EXISTS expires_index ON tiles (expires)");
                Logger.i(getApplicationContext(),TAG, "Created expires index on tiles table");
            }

            return true;
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Cache database is invalid: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    private void createNewTileCacheDatabase() {
        SQLiteDatabase db = null;
        try {
            File dbFile = new File(getApplicationContext().getCacheDir(), "tiles.db");
            try {
                db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
            } catch (SQLiteDiskIOException e) {
                Logger.e(getApplicationContext(), TAG, "Ошибка ввода-вывода при открытии базы данных: " + e.getMessage());
                // Логика восстановления или уведомление пользователя
            } finally {
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }

            // Create tiles table
            assert db != null;
            db.execSQL("CREATE TABLE IF NOT EXISTS tiles ([key] INTEGER PRIMARY KEY, provider TEXT NOT NULL, tile BLOB NOT NULL, expires INTEGER NOT NULL)");

            // Check for expires column
            Cursor cursor = db.rawQuery("PRAGMA table_info(tiles)", null);
            boolean hasExpiresColumn = false;
            while (cursor.moveToNext()) {
                if ("expires".equalsIgnoreCase(String.valueOf(cursor.getColumnIndex("name")))) {
                    hasExpiresColumn = true;
                    break;
                }
            }
            cursor.close();

            if (!hasExpiresColumn) {
                db.execSQL("ALTER TABLE tiles ADD COLUMN expires INTEGER NOT NULL");
                Logger.i(getApplicationContext(), TAG,"Added expires column to tiles table");
                db.execSQL("CREATE INDEX IF NOT EXISTS expires_index ON tiles (expires)");
                Logger.i(getApplicationContext(), TAG,"Created expires index on tiles table");
            }

            Logger.d(getApplicationContext(), TAG, "Tile cache database created at: " + dbFile.getPath());
        } catch (Exception e) {
            Logger.e(getApplicationContext(), TAG, "Error creating tiles database: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
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