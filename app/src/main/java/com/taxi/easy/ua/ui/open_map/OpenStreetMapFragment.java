package com.taxi.easy.ua.ui.open_map;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.getCurrentActivity;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentOpenstreetmapBinding;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.open_map.api.ApiResponse;
import com.taxi.easy.ua.ui.open_map.api.ApiService;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.log.Logger;

import org.json.JSONException;
import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Фрагмент для отображения карты и управления маркерами
public class OpenStreetMapFragment extends Fragment {
    private static final String TAG = "OpenStreetMapFragment";

    // UI элементы и зависимости
    private FragmentOpenstreetmapBinding binding;
    private Context ctx;
    private MapView map;
    private IMapController mapController;
    private ProgressBar progressBar;
    private FloatingActionButton fabCall, fabOpenMap;
    private ImageButton fab;
    private Switch gpsSwitch;
    private FragmentManager fragmentManager;

    // Данные карты и маркеров
    private Marker startMarkerObj; // Маркер начальной точки
    private Marker finishMarkerObj; // Маркер конечной точки
    private GeoPoint startPoint; // Координаты начальной точки
    private GeoPoint endPoint; // Координаты конечной точки
    private String fromAddressString; // Адрес начальной точки
    private String toAddressString; // Адрес конечной точки
    private Drawable scaledDrawable; // Иконка маркера
    private Marker previousMarker; // Предыдущий маркер для замены
    private Polyline roadOverlay; // Оверлей маршрута
    private String markerType; // Тип маркера: "startMarker" или "finishMarker"
    private double startLat, startLan, finishLat, finishLan; // Координаты
    private String city; // Город
    private String api; // API ключ

    ImageView center_marker;

    // Геолокация
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    // API
    private ApiService apiService;

    // Запрос разрешений
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    // Поток для маршрутов
    private static final Executor executor = Executors.newSingleThreadExecutor();

    @SuppressLint({"MissingInflatedId", "InflateParams", "UseCompatLoadingForDrawables"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOpenstreetmapBinding.inflate(inflater, container, false);
        map = binding.map;
        View root = binding.getRoot();
        ctx = requireContext();

        // Настройка конфигурации OSMDroid
        try {
            File cacheDir = new File(ctx.getCacheDir(), "osmdroid");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
                Logger.d(ctx, TAG, "Created osmdroid cache directory: " + cacheDir.getPath());
            }
            Configuration.getInstance().setOsmdroidTileCache(cacheDir);
            Configuration.getInstance().setCacheMapTileCount((short) 12);
            Configuration.getInstance().setTileFileSystemCacheMaxBytes(1024 * 1024 * 50);
            Configuration.getInstance().setTileDownloadMaxQueueSize((short) 40);
            Configuration.getInstance().setUserAgentValue(ctx.getPackageName());
            Configuration.getInstance().load(ctx, androidx.preference.PreferenceManager.getDefaultSharedPreferences(ctx));
            Logger.d(ctx, TAG, "OSMDroid configuration initialized, cache path: " + cacheDir.getPath());

            // Проверка сети
            if (!isNetworkAvailable()) {
                Logger.w(ctx, TAG, "No network connection, relying on cached tiles");
                Toast.makeText(ctx, "Нет интернета, используются кэшированные тайлы", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Logger.e(ctx, TAG, "Error configuring OSMDroid: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        // Инициализация MapView

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        map.setVisibility(GONE); // Скрыть карту до готовности
        mapController = map.getController();
        mapController.setZoom(16.0); // Начальный зум
        mapController.setCenter(new GeoPoint(0.0, 0.0)); // Временный центр
        Logger.d(ctx, TAG, "MapView initialized with zoom=16.0");

        // Инициализация остальных компонентов
        initializeArguments();
        initializePermissionLauncher();
        configureMap();
        initializeMapPosition();
        initializeMarkerIcon();
        fromAddressString = getString(R.string.startPoint);
        toAddressString = getString(R.string.end_point_marker);
        initializeUI();
        fragmentManager = getChildFragmentManager();

        return root;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Показать объяснение, почему нужно разрешение
                    Toast.makeText(ctx, "Разрешение на доступ к файлам необходимо для кэширования карт", Toast.LENGTH_LONG).show();
                    locationPermissionLauncher.launch(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
                } else if (!(boolean) sharedPreferencesHelperMain.getValue("storagePermissionRequested", false)) {
                    // Первый запрос разрешения
                    locationPermissionLauncher.launch(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE});
                    sharedPreferencesHelperMain.saveValue("storagePermissionRequested", true);
                }
            } else {
                Logger.d(ctx, TAG, "Storage permission already granted");
                configureMap();
                initializeMapPosition();
            }
        } else {
            Logger.d(ctx, TAG, "No storage permission needed for Android Q and above");
            configureMap();
            initializeMapPosition();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        map = binding.map;
        mapController = map.getController();
        apiServiceActivate();
        gpsSwitch.setChecked(switchState());

        Logger.d(ctx, TAG, "onResume: markerType=" + markerType);

        WorkManager.getInstance(ctx).getWorkInfosByTagLiveData("TilePreloadWork")
                .observe(getViewLifecycleOwner(), workInfos -> {
                    boolean preloadFinished = false;

                    for (WorkInfo workInfo : workInfos) {
                        if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            Logger.d(ctx, TAG, "TilePreloadWorker завершён");
                            preloadFinished = true;
                            break;
                        } else if (workInfo.getState() == WorkInfo.State.FAILED) {
                            Logger.e(ctx, TAG, "TilePreloadWorker завершился с ошибкой");
                            showMapWithFallback("TilePreloadWorker failed");
                            return;
                        }
                    }

                    if (preloadFinished) {
                        if (isTileCacheAvailable()) {
                            Logger.d(ctx, TAG, "Кэш тайлов найден, отображаем карту");
                            renderMap();
                        } else {
                            Logger.w(ctx, TAG, "Кэш отсутствует, fallback");
                            showMapWithFallback("Кэш пустой");
                        }
                    }
                });

        setupMapTouchListener();
    }
    private void renderMap() {
        try {
            configureMap();
            initializeMapPosition();

            map.setUseDataConnection(isNetworkAvailable());
            map.setVisibility(View.VISIBLE);
            map.onResume();
            map.invalidate();

            progressBar.setVisibility(View.GONE);
//            center_marker.setVisibility(View.VISIBLE);

            Logger.d(ctx, TAG, "Карта отрисована");
        } catch (Exception e) {
            Logger.e(ctx, TAG, "Ошибка в renderMap(): " + e.getMessage());
            showMapWithFallback("Ошибка renderMap");
        }
    }



    private void showMapWithFallback(String reason) {
        Logger.w(ctx, TAG, "Fallback карта: " + reason);
        configureMap();
        initializeMapPosition();

        map.setUseDataConnection(isNetworkAvailable());
        map.setVisibility(View.VISIBLE);
        map.onResume();
        map.invalidate();

        progressBar.setVisibility(View.GONE);
        center_marker.setVisibility(View.VISIBLE);

//        Toast.makeText(ctx, "Карта загружена с fallback: " + reason, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
        map.onPause();
    }

    // Инициализация лаунчера для запроса разрешений
    private void initializePermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    Boolean fineLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    Boolean storageGranted = permissions.getOrDefault(Manifest.permission.WRITE_EXTERNAL_STORAGE, false);

                    Logger.d(ctx, TAG, "Permissions result: fineLocation=" + fineLocationGranted + ", coarseLocation=" + coarseLocationGranted + ", storage=" + storageGranted);

                    // Обработка разрешений на геолокацию
                    if (Boolean.TRUE.equals(fineLocationGranted) && Boolean.TRUE.equals(coarseLocationGranted)) {
                        Logger.d(ctx, TAG, "Location permissions granted, initializing location updates");
                        initializeLocationUpdates();
                    } else {
                        Logger.w(ctx, TAG, "Location permissions denied");
                        Toast.makeText(ctx, R.string.check_permition, Toast.LENGTH_SHORT).show();
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", ctx.getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    }
                    Logger.d(ctx, TAG, "No storage permission needed for Android Q and above");
                    configureMap();
                    initializeMapPosition();

                }
        );
    }

    // Запрос разрешений на геолокацию
    private void requestLocationPermissions() {
        Logger.d(ctx, TAG, "Requesting location permissions");
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    // Инициализация иконки маркера
    private void initializeMarkerIcon() {
        Drawable originalDrawable = ContextCompat.getDrawable(ctx, R.drawable.marker_green);
        if (originalDrawable != null) {
            int width = 48, height = 48;
            Bitmap bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) originalDrawable).getBitmap(), width, height, false);
            scaledDrawable = new BitmapDrawable(getResources(), bitmap);
        }
    }

    // Инициализация аргументов фрагмента
    private void initializeArguments() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            markerType = arguments.getString("startMarker", "").equals("ok") ? "startMarker" :
                    arguments.getString("finishMarker", "").equals("ok") ? "finishMarker" : null;
        }
    }

    // Инициализация UI элементов
    private void initializeUI() {
        progressBar = binding.progressBar;
        center_marker = binding.centerMarker;

        progressBar.setVisibility(View.VISIBLE);
        center_marker.setVisibility(View.INVISIBLE);

        fab = binding.fab;
        fab.setVisibility(View.INVISIBLE);
        fabCall = binding.fabCall;
        fabOpenMap = binding.fabOpenMap;
        gpsSwitch = binding.gpsSwitch;

        fabOpenMap.setOnClickListener(v -> {
            Logger.e(ctx, "setStatusX 10", "markerType " + markerType);

            if ("startMarker".equals(markerType)) {
                sharedPreferencesHelperMain.saveValue("setStatusX", true);
            } else {
                Logger.e(ctx, "setStatusX 11", "markerType " + (boolean)sharedPreferencesHelperMain.getValue("setStatusX", false));
                if(!(boolean)sharedPreferencesHelperMain.getValue("setStatusX", false)) {
                    sharedPreferencesHelperMain.saveValue("setStatusX", false);
                }

            }
            NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());
//            Intent intent = new Intent(requireActivity(), MainActivity.class);
//            intent.putExtra("gps_upd", false);
//            startActivity(intent);
        });

        gpsSwitch.setChecked(switchState());
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gpsSwitch.setVisibility(View.INVISIBLE);
        }

        fabCall.setOnClickListener(v -> {
            List<String> stringList = logCursor(MainActivity.CITY_INFO, ctx);
            String phone = stringList.get(3);
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

        gpsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            gpsSwitch.setChecked(switchState());
        });
    }

    // Настройка карты
    private boolean mapConfigured = false;

    private void configureMap() {
        if (map == null || mapConfigured) {
            Logger.d(ctx, TAG, "Map уже сконфигурирован или null");
            return;
        }

        File cacheDir = new File(ctx.getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidTileCache(cacheDir);
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.setTilesScaledToDpi(true);
        map.setMinZoomLevel(0.0);
        map.setMaxZoomLevel(20.0);
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        map.setScrollableAreaLimitDouble(null);

        Logger.d(ctx, TAG, "Map настроен");
        mapConfigured = true;

    }

    private boolean isTileCacheAvailable() {
        File tileCacheDir = Configuration.getInstance().getOsmdroidTileCache();
        File cacheDb = new File(tileCacheDir, "cache.db");
        boolean exists = cacheDb.exists() && cacheDb.length() > 0;

        Logger.d(ctx, TAG, "Проверка кэша тайлов: " + cacheDb.getAbsolutePath() + ", exists=" + exists);
        return exists;
    }

    // Настройка слушателя касаний карты
    @SuppressLint("ClickableViewAccessibility")
    private void setupMapTouchListener() {
        map.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            GeoPoint centerPoint = (GeoPoint) map.getMapCenter();
            Logger.d(ctx, TAG, "Map touch event: action=" + action + ", centerPoint=" + centerPoint);

            if (action == MotionEvent.ACTION_DOWN) {
                binding.centerMarker.setVisibility(VISIBLE);
                removeMarkerOnTouchDown();

            } else if (action == MotionEvent.ACTION_UP) {
                binding.centerMarker.setVisibility(GONE);
                handleTouchUp(centerPoint);
                return true;
            } else if (action == MotionEvent.ACTION_MOVE) {
                updateZoomLevel();
            }
            return false;
        });

    }

    // Удаление маркера при начале касания
    private void removeMarkerOnTouchDown() {
        if (map == null) {
            Logger.e(ctx, TAG, "MapView is null, cannot remove marker");
            return;
        }
        if ("startMarker".equals(markerType)) {

            map.getOverlays().clear();
            map.invalidate();
            Logger.d(ctx, TAG, "Removed all overlays");
        } else if ("finishMarker".equals(markerType)) {
            if (finishMarkerObj != null) {
                map.getOverlays().remove(finishMarkerObj);
                finishMarkerObj = null;
                map.invalidate();
                Logger.d(ctx, TAG, "Removed finishMarkerObj");
            }
        } else {
            Logger.i(ctx, TAG, "Invalid markerType in removeMarkerOnTouchDown: " + markerType);
        }
    }

    // Обработка окончания касания
    private void handleTouchUp(GeoPoint centerPoint) {
        try {
            if ("startMarker".equals(markerType)) {
                sharedPreferencesHelperMain.saveValue("on_gps", false);
                startPoint = centerPoint;
                dialogMarkerStartPoint(ctx);
            } else if ("finishMarker".equals(markerType)) {
                endPoint = centerPoint;
                dialogMarkersEndPoint(ctx);
            }

        } catch (MalformedURLException | JSONException | InterruptedException e) {
            Logger.e(ctx, TAG, "Error handling marker point on ACTION_UP" + e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }



    // Обновление уровня зума
    private void updateZoomLevel() {
        map.invalidate(); // Убедитесь, что маркеры сохраняются
        double newZoomLevel = map.getZoomLevelDouble();
        Logger.d(ctx, TAG, "Zoom level: " + newZoomLevel);
        SQLiteDatabase database = ctx.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        ContentValues cv = new ContentValues();
        cv.put("newZoomLevel", newZoomLevel);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?", new String[]{"1"});
        database.close();
    }

    // Инициализация позиции карты
    private void initializeMapPosition() {
        newShowRout();
    }

    private void newShowRout() {
        if (map == null || ctx == null) {

            initializeRegion();
            return;
        }
        // Удалить все маркеры и наложения с карты
        map.getOverlays().clear();
        map.invalidate(); // Обновить карту

        double savedStartLat = getFromTablePositionInfo(ctx, "startLat");
        if (savedStartLat != 0) {

            List<String> startList = logCursor(MainActivity.ROUT_MARKER, ctx);

            fromAddressString = startList.get(5);
            Logger.d(ctx, TAG, "address11 fromAddressString" + fromAddressString);

            boolean isShowRout = true;
            if(!startList.get(6).trim().isEmpty()) {
                toAddressString = startList.get(6);
            } else {
                toAddressString = fromAddressString;
                isShowRout = false;

            }

            Logger.d(ctx, TAG, "address11 startList.get(6)" + startList.get(6));
            Logger.d(ctx, TAG, "address11 toAddressString" + toAddressString);

            startLat = Double.parseDouble(startList.get(1));
            startLan = Double.parseDouble(startList.get(2));

            finishLat = Double.parseDouble(startList.get(3));
            finishLan = Double.parseDouble(startList.get(4));

            startPoint = new GeoPoint(startLat, startLan);


            startMarkerObj = new Marker(map);
            startMarkerObj.setPosition(startPoint);

            setMarker(startLat, startLan, fromAddressString, ctx, "1.");

            GeoPoint finishPoint = new GeoPoint(finishLat, finishLan);
            finishMarkerObj = new Marker(map);
            finishMarkerObj.setPosition(finishPoint);


            if(markerType.equals("startMarker")) {
                mapController.setCenter(startPoint);
                if(isShowRout) {
                    setMarker(finishLat, finishLan, toAddressString, ctx, "2.");
                    showRout(startPoint, finishPoint);
                }
            } else {
                    binding.centerMarker.setVisibility(GONE);
                    mapController.setCenter(finishPoint);
                if(isShowRout) {
                    setMarker(finishLat, finishLan, toAddressString, ctx, "2.");
                    showRout(startPoint, finishPoint);
                }
            }


            map.invalidate();

        } else {
            initializeRegion();
        }
    }

      // Инициализация региона, если нет сохраненной позиции
    private void initializeRegion() {
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            // Получение данных из CITY_INFO
            List<String> cityInfo = logCursor(MainActivity.CITY_INFO, ctx);
            if (cityInfo.size() >= 3) {
                city = cityInfo.get(1);
                api = cityInfo.get(2);
                Logger.d(ctx, TAG, "City and API loaded: city=" + city + ", api=" + api);
            } else {
                Logger.e(ctx, TAG, "Insufficient data in CITY_INFO, size: " + cityInfo.size());
                return;
            }

            // Проверка разрешений на геолокацию
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Попытка получить координаты из ROUT_MARKER
                database = ctx.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                String query = "SELECT startLat, startLan FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
                cursor = database.rawQuery(query, null);
                if (cursor.moveToFirst()) {
                    @SuppressLint("Range") double originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
                    @SuppressLint("Range") double originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
                    startPoint = new GeoPoint(originLatitude, originLongitude);
                    Logger.d(ctx, TAG, "Loaded start point from ROUT_MARKER: " + startPoint);
                } else {
                    Logger.i(ctx, TAG, "No data found in ROUT_MARKER");
                }
            }

            // Установка позиции карты и маркера, если есть начальная точка
            if (startPoint != null) {
                startLat = startPoint.getLatitude();
                startLan = startPoint.getLongitude();
                mapController.setCenter(startPoint);
                // Установка оптимального зума из сохраненных данных или по умолчанию
                double zoomLevel = getFromTablePositionInfo(ctx, "newZoomLevel");
                if (zoomLevel == 0) {
                    zoomLevel = 16.0; // Оптимальный зум по умолчанию
                }
                mapController.setZoom(zoomLevel);
                setMarker(startLat, startLan, fromAddressString, ctx, "1.");
                map.invalidate();
                Logger.d(ctx, TAG, "Map centered at: " + startPoint + ", zoom: " + zoomLevel);
            } else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Инициализация геолокации, если есть разрешение
                initializeLocationUpdates();
                Logger.d(ctx, TAG, "Initializing location updates");
            } else {
                // Запрос разрешений
                requestLocationPermissions();
                Logger.d(ctx, TAG, "Requesting location permissions");
            }
        } catch (Exception e) {
            Logger.e(ctx, TAG, "Error in initializeRegion: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            // Закрытие курсора и базы данных
            if (cursor != null) {
                cursor.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    // Инициализация обновлений геолокации
    private void initializeLocationUpdates() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ctx);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                stopLocationUpdates();
                List<Location> locations = locationResult.getLocations();
                Logger.d(ctx, TAG, "onLocationResult: locations=" + locations);
                if (!locations.isEmpty()) {
                    Location firstLocation = locations.get(0);
                    if (startLat != firstLocation.getLatitude() || startLan != firstLocation.getLongitude()) {
                        startLat = firstLocation.getLatitude();
                        startLan = firstLocation.getLongitude();
                        updateLocation();
                    }
                }
            }
        };
        startLocationUpdates();
    }

    // Обновление позиции на основе геолокации
    private void updateLocation() {
        String language = Locale.getDefault().getLanguage();
        String BASE_URL = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        String urlFrom = BASE_URL + "/" + api + "/android/fromSearchGeoLocal/" + startLat + "/" + startLan + "/" + language;

        try {
            FromJSONParser parser = new FromJSONParser(urlFrom);
            Map<String, String> sendUrlFrom = parser.sendURL(urlFrom);
            if (sendUrlFrom != null) {
                fromAddressString = sendUrlFrom.get("route_address_from");
                if (fromAddressString != null && fromAddressString.equals("Точка на карте")) {
                    fromAddressString = getString(R.string.startPoint);
                }
            }
            updateMyPosition(startLat, startLan, fromAddressString, ctx);
            startPoint = new GeoPoint(startLat, startLan);
            mapController.setCenter(startPoint);
            setMarker(startLat, startLan, fromAddressString, ctx, "1.");
            map.invalidate();
        } catch (MalformedURLException | InterruptedException | JSONException e) {
            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder().setPopUpTo(R.id.nav_restart, true).build());
        }
    }

    // Запуск обновлений геолокации
    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(100)
                .build();
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    // Остановка обновлений геолокации
    private void stopLocationUpdates() {
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    // Инициализация API сервиса
    private void apiServiceActivate() {
        String BASE_URL = sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site") + "/";
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> Logger.d(ctx, TAG, message));
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(10, TimeUnit.SECONDS) // Уменьшить таймаут подключения
                .readTimeout(10, TimeUnit.SECONDS) // Уменьшить таймаут чтения
                .writeTimeout(10, TimeUnit.SECONDS)
                .cache(new Cache(new File(ctx.getCacheDir(), "okhttp_cache"), 50 * 1024 * 1024)) // 50 МБ кэша
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
        Logger.d(ctx, TAG, "Request URL: " + retrofit.baseUrl());
    }

    // Вызов API для получения адреса по координатам
    private void makeApiCall(double latitude, double longitude, Context context) {
        Logger.d(context, TAG, "makeApiCall started with latitude=" + latitude + ", longitude=" + longitude + ", map=" + map + ", markerType=" + markerType);
        if (map == null) {
            Logger.e(context, TAG, "MapView is null, aborting API call");
            return;
        }
        if (markerType == null || (!"startMarker".equals(markerType) && !"finishMarker".equals(markerType))) {
            Logger.i(context, TAG, "Invalid markerType: " + markerType + ", defaulting to startMarker");
            markerType = "startMarker"; // Fallback
        }
        String localeCode = (String) sharedPreferencesHelperMain.getValue("locale", Locale.getDefault().toString());
        Logger.d(context, TAG, "localeCode=" + localeCode);
        Locale locale = new Locale(localeCode.split("_")[0]);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        android.content.res.Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        Context localizedContext = context.createConfigurationContext(config);
        Logger.d(context, TAG, "language currentLocale=" + localeCode);

        if (map == null) {
            Logger.e(context, TAG, "MapView is null");
            return;
        }

        Call<ApiResponse> call = apiService.reverseAddressLocal(latitude, longitude, localeCode);
        Logger.d(context, TAG, "API call created for reverseAddressLocal");

        prepareMarker();
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                handleApiResponse(response, localizedContext);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                Logger.e(context, TAG, "API call failed: " + t.getMessage());
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
    }

    // Подготовка маркера перед API вызовом
    private void prepareMarker() {
        if (map == null) {
            Logger.e(ctx, TAG, "MapView is null, cannot prepare marker");
            return;
        }
        if ("startMarker".equals(markerType)) {
            if (startMarkerObj != null) {
                map.getOverlays().remove(startMarkerObj);
                Logger.d(ctx, TAG, "Removed startMarkerObj from overlays");
            }
            startMarkerObj = new Marker(map);
            Logger.d(ctx, TAG, "Created new start marker: " + startMarkerObj);
        } else if ("finishMarker".equals(markerType)) {
            if (finishMarkerObj != null) {
                map.getOverlays().remove(finishMarkerObj);
                Logger.d(ctx, TAG, "Removed finishMarkerObj from overlays");
            }
            finishMarkerObj = new Marker(map);
            Logger.d(ctx, TAG, "Created new finish marker: " + finishMarkerObj);
        } else {
            Logger.e(ctx, TAG, "Invalid markerType: " + markerType);
            FirebaseCrashlytics.getInstance().recordException(new Exception("Invalid markerType: " + markerType));
        }
    }

    // Обработка ответа API
    private void handleApiResponse(Response<ApiResponse> response, Context localizedContext) {
        Logger.d(ctx, TAG, "onResponse called, response successful: " + response.isSuccessful());
        if (!isAdded()) {
            Logger.e(ctx, TAG, "Fragment is not attached");
            return;
        }
        if (response.isSuccessful() && response.body() != null) {
            ApiResponse apiResponse = response.body();
            String result = apiResponse.getResult();
            Logger.d(ctx, TAG, "API response result: " + result);

            if (map == null || map.getRepository() == null) {
                Logger.e(ctx, TAG, "Map or repository is null");
                return;
            }

            if ("startMarker".equals(markerType)) {
                handleStartMarkerResponse(result, localizedContext);
            } else if ("finishMarker".equals(markerType)) {
                handleFinishMarkerResponse(result, localizedContext);
            }


        } else {
            Logger.e(ctx, TAG, "API response unsuccessful or body is null, response code: " + response.code());
        }
    }

    // Обработка ответа для начального маркера
    private void handleStartMarkerResponse(String result, Context localizedContext) {
        if (startPoint == null) {
            Logger.e(ctx, TAG, "startPoint is null");
            return;
        }
        if (startMarkerObj == null) {
            Logger.e(ctx, TAG, "startMarkerObj is null, cannot set marker position");
            return;
        }
        fromAddressString = result.equals("Точка на карте") ? localizedContext.getString(R.string.startPoint) : result;
        Logger.d(ctx, TAG, "fromAddressString set to: " + fromAddressString);

        if (previousMarker != null) {
            map.getOverlays().remove(previousMarker);
            Logger.d(ctx, TAG, "Removed previousMarker from overlays");
        }

        updateRouteSettings("start");
    }

    // Обработка ответа для конечного маркера
    private void handleFinishMarkerResponse(String result, Context localizedContext) {
        // Проверка всех зависимостей
        if (endPoint == null) {
            Logger.e(ctx, TAG, "endPoint is null, cannot handle finish marker");
            FirebaseCrashlytics.getInstance().recordException(new Exception("endPoint is null in handleFinishMarkerResponse"));
            return;
        }
        if (map == null) {
            Logger.e(ctx, TAG, "MapView is null, cannot handle finish marker");
            FirebaseCrashlytics.getInstance().recordException(new Exception("MapView is null in handleFinishMarkerResponse"));
            return;
        }
        if (mapController == null) {
            Logger.e(ctx, TAG, "MapController is null, cannot set map center");
            FirebaseCrashlytics.getInstance().recordException(new Exception("MapController is null in handleFinishMarkerResponse"));
            return;
        }

        // Инициализация finishMarkerObj, если он null


        if (finishMarkerObj == null) {
            Logger.w(ctx, TAG, "finishMarkerObj is null, creating new Marker");
            try {
                finishMarkerObj = new Marker(map);
            } catch (Exception e) {
                Logger.e(ctx, TAG, "Failed to create finishMarkerObj: " + e.getMessage());
                FirebaseCrashlytics.getInstance().recordException(new Exception("Failed to create finishMarkerObj in handleFinishMarkerResponse", e));
                return;
            }
        }

        // Установка строки адреса
        toAddressString = result.equals("Точка на карте") ? localizedContext.getString(R.string.end_point_marker) : result;
        Logger.d(ctx, TAG, "toAddressString set to: " + toAddressString);

        // Конфигурация маркера и добавление на карту
        try {


//            // Удаляем маркер из overlays, если он уже добавлен, чтобы избежать дублирования
            map.getOverlays().remove(finishMarkerObj);
            map.getOverlays().removeAll(Collections.singleton(roadOverlay));

            map.invalidate();
            Logger.d(ctx, TAG, "Map invalidated after finishMarker setup");

            updateRouteSettings("finish");
        } catch (Exception e) {
            Logger.e(ctx, TAG, "Error configuring finish marker: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(new Exception("Error configuring finish marker in handleFinishMarkerResponse", e));
        }
    }


    // Обновление настроек маршрута

    private void updateRouteSettings(String point) {
        if (map == null || map.getContext() == null || VisicomFragment.textViewTo == null) {
            Logger.e(ctx, TAG, "Map, context, or textViewTo is null");
            String message = getString(R.string.error_message);
            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
            return;
        }

        List<String> settings = new ArrayList<>();
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = map.getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            // Убедимся, что таблица существует с правильной схемой
            database.execSQL("CREATE TABLE IF NOT EXISTS " + MainActivity.ROUT_MARKER + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "startLat REAL, startLan REAL, to_lat REAL, to_lng REAL, " +
                    "start TEXT, finish TEXT)");

            // Получение текущих данных
            String query = "SELECT * FROM " + MainActivity.ROUT_MARKER + " LIMIT 1";
            cursor = database.rawQuery(query, null);
            double currentFromLatitude = 0.0;
            double currentFromLongitude = 0.0;
            String currentStartAddress = "";

            double currentToLatitude = 0.0;
            double currentToLongitude = 0.0;
            String currentFinishAddress = "";

            if (cursor.moveToFirst()) {
                int fromLatIndex = cursor.getColumnIndex("startLat");
                int fromLngIndex = cursor.getColumnIndex("startLan");
                int startIndex = cursor.getColumnIndex("start");

                int toLatIndex = cursor.getColumnIndex("to_lat");
                int toLngIndex = cursor.getColumnIndex("to_lng");
                int finishIndex = cursor.getColumnIndex("finish");

                if (fromLatIndex != -1) {
                    currentFromLatitude = cursor.getDouble(fromLatIndex);
                } else {
                    Logger.i(ctx, TAG, "from_lat column not found in ROUT_MARKER");
                }
                if (fromLngIndex != -1) {
                    currentFromLongitude = cursor.getDouble(fromLngIndex);
                } else {
                    Logger.i(ctx, TAG, "from_lng column not found in ROUT_MARKER");
                }
                if (startIndex != -1) {
                    currentStartAddress = cursor.getString(startIndex) != null ? cursor.getString(startIndex) : "";
                } else {
                    Logger.i(ctx, TAG, "start column not found in ROUT_MARKER");
                }



                if (toLatIndex != -1) {
                    currentToLatitude = cursor.getDouble(toLatIndex);
                } else {
                    Logger.i(ctx, TAG, "to_lat column not found in ROUT_MARKER");
                }
                if (toLngIndex != -1) {
                    currentToLongitude = cursor.getDouble(toLngIndex);
                } else {
                    Logger.i(ctx, TAG, "to_lng column not found in ROUT_MARKER");
                }
                if (finishIndex != -1) {
                    currentFinishAddress = cursor.getString(finishIndex) != null ? cursor.getString(finishIndex) : "";
                } else {
                    Logger.i(ctx, TAG, "finish column not found in ROUT_MARKER");
                }
                Logger.d(ctx, TAG, "Current data: toLatitude=" + currentToLatitude + ", toLongitude=" + currentToLongitude + ", finishAddress=" + currentFinishAddress);
            }

            // Подготовка новых данных


            if(point.equals("start")) {
                settings.add(String.valueOf(startLat));
                settings.add(String.valueOf(startLan));
                settings.add(String.valueOf(currentToLatitude));
                settings.add(String.valueOf(currentToLongitude));
                settings.add(fromAddressString);
                settings.add(currentFinishAddress);
                Logger.d(ctx, TAG, "New settings for route start point update: " + settings);
            }

            if(point.equals("finish")) {
                settings.add(String.valueOf(currentFromLatitude));
                settings.add(String.valueOf(currentFromLongitude));
                settings.add(String.valueOf(finishLat));
                settings.add(String.valueOf(finishLan));
                settings.add(currentStartAddress);
                settings.add(toAddressString);
                Logger.d(ctx, TAG, "New settings for route finish point update: " + settings);
            }


            // Обновление таблицы
            ContentValues values = new ContentValues();
            values.put("startLat", Double.parseDouble(settings.get(0)));
            values.put("startLan", Double.parseDouble(settings.get(1)));
            values.put("to_lat", Double.parseDouble(settings.get(2)));
            values.put("to_lng", Double.parseDouble(settings.get(3)));
            values.put("start", settings.get(4));
            values.put("finish", settings.get(5));

            int rowsUpdated = database.update(MainActivity.ROUT_MARKER, values, null, null);
            if (rowsUpdated == 0) {
                database.insert(MainActivity.ROUT_MARKER, null, values);
                Logger.d(ctx, TAG, "Inserted new route marker data");
            } else {
                Logger.d(ctx, TAG, "Updated route marker data, rows affected: " + rowsUpdated);
            }
        } catch (Exception e) {
            Logger.e(ctx, TAG, "Failed to update ROUT_MARKER: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }

        // Обновление маршрута и позиции
        updateRoutMarker(settings, map.getContext());
        updateMyPosition(startLat, startLan, fromAddressString, map.getContext());
        new Handler(Looper.getMainLooper()).post(this::newShowRout);
        Logger.d(ctx, TAG, "Updated route settings and position");
    }


    // Обработка начального маркера
    private void dialogMarkerStartPoint(Context context) throws MalformedURLException {
        Logger.d(context, TAG, "dialogMarkerStartPoint: " + startPoint);
        if (startPoint != null) {
            startLat = startPoint.getLatitude();
            startLan = startPoint.getLongitude();
            makeApiCall(startLat, startLan, context);
        }
    }

    // Обработка конечного маркера
    private void dialogMarkersEndPoint(Context context) throws MalformedURLException, JSONException, InterruptedException {
        if (endPoint != null) {
            finishLat = endPoint.getLatitude();
            finishLan = endPoint.getLongitude();
            makeApiCall(finishLat, finishLan, context);
        }
    }

    // Установка маркера
    private void setMarker(double lat, double lon, String title, Context context, String prefix) {
        if (map == null) {
            Logger.e(context, TAG, "MapView is null, cannot set marker");
            return;
        }

        // Create new marker
        Marker marker = new Marker(map);
        marker.setTextLabelBackgroundColor(Color.TRANSPARENT);
        marker.setTextLabelForegroundColor(Color.RED);
        marker.setTextLabelFontSize(40);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        String unuString = new String(Character.toChars(0x1F449));

        // Set title for the marker (used by InfoWindow if clicked)
        marker.setTitle(prefix + unuString + title);
        Logger.d(context, TAG, "scaledDrawable: " + scaledDrawable);

        // Set custom icon
        initializeMarkerIcon();
        if (scaledDrawable != null) {
            marker.setIcon(scaledDrawable);
        } else {
            Logger.w(context, TAG, "scaledDrawable is null, no icon set");
        }

        // Set marker position
        marker.setPosition(new GeoPoint(lat, lon));

        // Add marker to map
        map.getOverlays().add(marker);

        // Add custom text overlay for persistent label
        addTextOverlay(new GeoPoint(lat, lon), prefix + unuString + title);

        // Update map on the main thread
        new Handler(Looper.getMainLooper()).post(() -> map.invalidate());

        // Update marker references
        if ("startMarker".equals(markerType)) {
            startMarkerObj = marker;
            binding.centerMarker.setVisibility(View.GONE);
        } else if ("finishMarker".equals(markerType)) {
            finishMarkerObj = marker;
        }
    }


    private void addTextOverlay(GeoPoint point, String text) {
        Overlay textOverlay = new Overlay() {
            @Override
            public void draw(Canvas canvas, MapView mapView, boolean shadow) {
                if (shadow) return; // Skip shadow pass

                Projection projection = mapView.getProjection();
                Point screenPoint = new Point();
                projection.toPixels(point, screenPoint);

                // Настройка TextPaint для текста
                TextPaint textPaint = new TextPaint();
                textPaint.setColor(Color.BLACK); // Черный текст
                textPaint.setTextSize(48); // ~14sp
                textPaint.setAntiAlias(true);

                // Настройка Paint для фона
                Paint backgroundPaint = new Paint();
                backgroundPaint.setColor(Color.WHITE); // Белый фон
                backgroundPaint.setAlpha(230); // Полупрозрачность
                backgroundPaint.setAntiAlias(true);

                // Настройка Paint для обводки
                Paint borderPaint = new Paint();
                borderPaint.setColor(Color.GRAY); // Серая обводка
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(2);
                borderPaint.setAntiAlias(true);

                // Параметры для фона
                int padding = 10; // Отступ внутри фона
                int fixedWidth = 700; // Фиксированная ширина
                int arrowHeight = 30; // Высота стрелки

                // Создаем StaticLayout для переноса текста
                StaticLayout textLayout = new StaticLayout(
                        text, textPaint, fixedWidth - padding * 2, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false
                );
                int textHeight = textLayout.getHeight();
                int height = textHeight + padding * 2;

                // Координаты для фона (центрируем над маркером)
                float left = screenPoint.x - (float) fixedWidth / 2;
                float top = screenPoint.y - height - arrowHeight - 120; // Подняли еще выше (было -90, теперь -140)
                float right = left + fixedWidth;
                float bottom = top + height;

                // Включаем тень для фона
                backgroundPaint.setShadowLayer(4, 2, 2, Color.argb(100, 0, 0, 0));

                // Рисуем фон (прямоугольник с закругленным углом)
                RectF backgroundRect = new RectF(left, top, right, bottom);
                canvas.drawRoundRect(backgroundRect, 10, 10, backgroundPaint);
                canvas.drawRoundRect(backgroundRect, 10, 10, borderPaint);

                // Рисуем "стрелку" (треугольник) внизу фона
                Path arrowPath = new Path();
                float arrowWidth = 20;
                arrowPath.moveTo(screenPoint.x - arrowWidth / 2, bottom);
                arrowPath.lineTo(screenPoint.x + arrowWidth / 2, bottom);
                arrowPath.lineTo(screenPoint.x, bottom + arrowHeight);
                arrowPath.close();
                canvas.drawPath(arrowPath, backgroundPaint); // Фон стрелки
                canvas.drawPath(arrowPath, borderPaint); // Обводка стрелки

                // Рисуем текст с переносом
                canvas.save();
                canvas.translate(left + padding, top + padding);
                textLayout.draw(canvas);
                canvas.restore();
            }
        };

        new Handler(Looper.getMainLooper()).post(() -> {
            map.getOverlays().add(textOverlay);
            map.invalidate();
        });
    }
    // Отображение маршрута
    private void showRout(GeoPoint startP, GeoPoint endP) {
        if (startP == null || endP == null) {
            Logger.e(ctx, TAG, "Cannot show route: startP or endP is null");
            return;
        }
        map.getOverlays().removeAll(Collections.singleton(roadOverlay));
        executor.execute(() -> {
            RoadManager roadManager = new OSRMRoadManager(map.getContext(), System.getProperty("http.agent"));
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(startP);
            waypoints.add(endP);
            Road road = roadManager.getRoad(waypoints);
            roadOverlay = RoadManager.buildRoadOverlay(road);
            roadOverlay.getOutlinePaint().setStrokeWidth(10f);
            map.post(() -> {
                map.getOverlays().add(roadOverlay);
                map.invalidate();
            });
        });
    }

    // Проверка состояния GPS
    private boolean switchState() {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false, network_enabled = false;
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return gps_enabled && network_enabled;
    }

    // Обновление текущей позиции
    private void updateMyPosition(double startLat, double startLan, String position, Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        ContentValues cv = new ContentValues();
        cv.put("startLat", startLat);
        cv.put("startLan", startLan);
        cv.put("position", position);
        database.update(MainActivity.TABLE_POSITION_INFO, cv, "id = ?", new String[]{"1"});
        database.close();
    }

    // Обновление данных маршрута
    private void updateRoutMarker(List<String> settings, Context context) {
        ContentValues cv = new ContentValues();
        cv.put("startLat", Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_MARKER, cv, "id = ?", new String[]{"1"});
        database.close();
    }

    // Получение данных из таблицы POSITION_INFO
    @SuppressLint("Range")
    private double getFromTablePositionInfo(Context context, String columnName) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.rawQuery("SELECT " + columnName + " FROM " + MainActivity.TABLE_POSITION_INFO + " WHERE id = ?", new String[]{"1"});
        double result = 0.0;
        if (cursor.moveToFirst()) {
            result = cursor.getDouble(cursor.getColumnIndex(columnName));
        }
        cursor.close();
        database.close();
        return result;
    }

    // Логирование данных из таблицы
    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                for (String cn : c.getColumnNames()) {
                    list.add(c.getString(c.getColumnIndex(cn)));
                }
            } while (c.moveToNext());
            c.close();
        }
        database.close();
        return list;
    }
}