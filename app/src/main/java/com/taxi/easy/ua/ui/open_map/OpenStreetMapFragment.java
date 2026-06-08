package com.taxi.easy.ua.ui.open_map;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.GestureDetector;
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
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
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
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.FragmentOpenstreetmapBinding;
import com.taxi.easy.ua.ui.maps.FromJSONParser;
import com.taxi.easy.ua.ui.open_map.api.ApiResponse;
import com.taxi.easy.ua.ui.open_map.api.ApiService;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.city.CityFinder;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.phone_state.PhoneCallHelper;

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
import com.taxi.easy.ua.utils.db.CursorReadHelper;

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
    private Marker previousMarker; // Предыдущий маркер для замены
    private Polyline roadOverlay; // Оверлей маршрута
    private int routeBuildGeneration;
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
    private boolean userMovedMap = false;
    /** Второй пин «по городу»/пустой — показываем только после сдвига или тапа по карте. */
    private boolean deferFinishPinUntilInteraction;
    private boolean finishPinRevealedByUser;
    private GestureDetectorCompat gestureDetector;
    private long lastZoomTime = 0;
    private static final long ZOOM_COOLDOWN_MS = 500;

    private boolean isMapDragging = false;
    private float downX, downY;
    private static final float MIN_DRAG_DISTANCE = 20;
    private String unuString;
    /** Отдельные текстовые подписи для 1-й и 2-й точек маршрута. */
    private Overlay startTextOverlay;
    private Overlay finishTextOverlay;
    private ExecutionStatusViewModel viewModel;

    @SuppressLint({"MissingInflatedId", "InflateParams", "UseCompatLoadingForDrawables"})
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOpenstreetmapBinding.inflate(inflater, container, false);
        map = binding.map;
        View root = binding.getRoot();
        unuString = new String(Character.toChars(0x1F449));
        viewModel = new ViewModelProvider(requireActivity()).get(ExecutionStatusViewModel.class);
        ctx = requireContext();
        if(button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }
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
        setupMapTapZoom();
        // Инициализация остальных компонентов
        initializeArguments();
        initializePermissionLauncher();
        configureMap();
        initializeMapPosition();
        initializeMarkerIcon();
        initializeUI();
        fragmentManager = getChildFragmentManager();

        return root;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
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
            updateCenterMarkerVisibility();

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
        updateCenterMarkerVisibility();

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
        updateCenterMarkerIcon();
    }

    private void updateCenterMarkerIcon() {
        if (center_marker == null) {
            return;
        }
        int iconRes = "finishMarker".equals(markerType)
                ? R.drawable.ic_map_pin_finish
                : R.drawable.ic_map_pin_start;
        center_marker.setImageResource(iconRes);
    }

    @Nullable
    private BitmapDrawable createMarkerIconDrawable(Context context, @DrawableRes int drawableRes) {
        android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(context, drawableRes);
        if (drawable == null) {
            return null;
        }
        int sizePx = (int) (44f * context.getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, sizePx, sizePx);
        drawable.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    @Nullable
    private BitmapDrawable getMarkerIconForRole(Context context, String role) {
        int drawableRes = "finishMarker".equals(role)
                ? R.drawable.ic_map_pin_finish
                : R.drawable.ic_map_pin_start;
        return createMarkerIconDrawable(context, drawableRes);
    }

    // Инициализация аргументов фрагмента
    private void initializeArguments() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            String explicitMarkerType = arguments.getString("markerType", null);
            if ("startMarker".equals(explicitMarkerType) || "finishMarker".equals(explicitMarkerType)) {
                markerType = explicitMarkerType;
                return;
            }
            // Фолбэк: если оба "ok", то лучше считать что редактируем финиш (2-я точка),
            // иначе снова будет эффект "тап по 2-й точке открывает 1-ю".
            boolean startOk = "ok".equals(arguments.getString("startMarker", ""));
            boolean finishOk = "ok".equals(arguments.getString("finishMarker", ""));
            if (finishOk) {
                markerType = "finishMarker";
            } else if (startOk) {
                markerType = "startMarker";
            } else {
                markerType = null;
            }
        }
    }

    // Инициализация UI элементов
    private void initializeUI() {
        progressBar = binding.progressBar;
        center_marker = binding.centerMarker;
        updateCenterMarkerVisibility();

        progressBar.setVisibility(View.VISIBLE);

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
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());

        });

        gpsSwitch.setChecked(switchState());
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gpsSwitch.setVisibility(View.INVISIBLE);
        }

        fabCall.setOnClickListener(v -> {
            PhoneCallHelper.callWithFallback(() -> {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, MyApplication.getContext());
                return stringList.size() > 3 ? stringList.get(3) : "";
            });
//            List<String> stringList = logCursor(MainActivity.CITY_INFO, ctx);
//            String phone = stringList.get(3);
//            Intent intent = new Intent(Intent.ACTION_DIAL);
//            intent.setData(Uri.parse(phone));
//            startActivity(intent);
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
        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.ALWAYS);
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
    // Настройка слушателя касаний карты
    @SuppressLint("ClickableViewAccessibility")
    private void setupMapTouchListener() {
        map.setOnTouchListener((v, event) -> {
            int action = event.getAction();

            // Передаем события в GestureDetector для обработки тапов (зум)
            if (gestureDetector != null) {
                gestureDetector.onTouchEvent(event);
            }

            if (action == MotionEvent.ACTION_DOWN) {
                // Запоминаем начальные координаты
                downX = event.getX();
                downY = event.getY();
                isMapDragging = false;
                if (isMarkerEditMode()) {
                    updateCenterMarkerVisibility();
                }

            } else if (action == MotionEvent.ACTION_MOVE) {
                // Проверяем, было ли перемещение
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);

                if (distance > MIN_DRAG_DISTANCE) {
                    isMapDragging = true;
                    if ("finishMarker".equals(markerType)) {
                        revealFinishPinFromUserInteraction();
                    }
                }
                if (isMarkerEditMode()) {
                    updateCenterMarkerVisibility();
                }
                updateZoomLevel();

            } else if (action == MotionEvent.ACTION_UP) {
                GeoPoint centerPoint = (GeoPoint) map.getMapCenter();
                if ("finishMarker".equals(markerType)) {
                    revealFinishPinFromUserInteraction();
                    userMovedMap = true;
                    applyFinishFromMapCenter(centerPoint);
                } else if ("startMarker".equals(markerType)) {
                    userMovedMap = true;
                    applyStartFromMapCenter(centerPoint);
                } else if (isMapDragging) {
                    userMovedMap = true;
                    handleTouchUp(centerPoint);
                }

                v.performClick();
                return true;
            }

            return false;
        });
    }

    // Обработка окончания касания
    private void handleTouchUp(GeoPoint centerPoint) {
        try {
            if ("startMarker".equals(markerType)) {
                sharedPreferencesHelperMain.saveValue("on_gps", false);
                startPoint = centerPoint;
                dialogMarkerStartPoint(ctx);
            }
        } catch (MalformedURLException e) {
            Logger.e(ctx, TAG, "Error handling marker point on ACTION_UP" + e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private boolean isFinishMarkerEditMode() {
        return "finishMarker".equals(markerType);
    }

    private void applyFinishFromMapCenter(GeoPoint centerPoint) {
        if (centerPoint == null || map == null) {
            return;
        }
        endPoint = centerPoint;
        finishLat = centerPoint.getLatitude();
        finishLan = centerPoint.getLongitude();
        if (startPoint == null && isValidRouteCoordinate(startLat, startLan)) {
            startPoint = new GeoPoint(startLat, startLan);
        }
        updateCenterMarkerVisibility();
        redrawRouteToFinish(endPoint);
        try {
            dialogMarkersEndPoint(ctx);
        } catch (MalformedURLException | JSONException | InterruptedException e) {
            Logger.e(ctx, TAG, "applyFinishFromMapCenter: " + e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void applyStartFromMapCenter(GeoPoint centerPoint) {
        if (centerPoint == null || map == null) {
            return;
        }
        startPoint = centerPoint;
        startLat = centerPoint.getLatitude();
        startLan = centerPoint.getLongitude();
        updateCenterMarkerVisibility();
        redrawRouteFromStart();
        try {
            dialogMarkerStartPoint(ctx);
        } catch (MalformedURLException e) {
            Logger.e(ctx, TAG, "applyStartFromMapCenter: " + e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void redrawRouteFromStart() {
        GeoPoint finish = resolveFinishPointForRoute();
        if (finish != null && startPoint != null) {
            redrawRouteToFinish(finish);
        }
    }

    @Nullable
    private GeoPoint resolveFinishPointForRoute() {
        if (endPoint != null) {
            return endPoint;
        }
        RouteMarkerData route = loadRouteMarkerData();
        if (route == null) {
            return null;
        }
        if (!isValidRouteCoordinate(route.finishLat, route.finishLon)
                || isCityOnlyAddress(route.finishAddress)) {
            return null;
        }
        endPoint = new GeoPoint(route.finishLat, route.finishLon);
        finishLat = route.finishLat;
        finishLan = route.finishLon;
        return endPoint;
    }

    private void removeFinishPinFromMap() {
        if (map == null) {
            return;
        }
        if (finishMarkerObj != null) {
            map.getOverlays().remove(finishMarkerObj);
            finishMarkerObj = null;
        }
    }

    private void removeFinishMarkerFromMap() {
        removeFinishPinFromMap();
        removeTextOverlay("finishMarker");
    }

    /** Подпись «куда» на карте (без пина — точка задаётся крестиком в центре). */
    private void showFinishAddressLabel(GeoPoint point, String address) {
        if (map == null || point == null || address == null || address.trim().isEmpty()) {
            return;
        }
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> showFinishAddressLabel(point, address));
            return;
        }
        removeTextOverlay("finishMarker");
        String prefix = unuString != null ? unuString : "";
        Overlay textOverlay = createTextOverlay(point, "2." + prefix + address.trim(), "finishMarker");
        finishTextOverlay = textOverlay;
        map.getOverlays().add(textOverlay);
        map.invalidate();
    }

    private void removeRoadOverlayFromMap() {
        if (map == null) {
            roadOverlay = null;
            return;
        }
        List<Overlay> overlays = map.getOverlays();
        for (int i = overlays.size() - 1; i >= 0; i--) {
            if (overlays.get(i) instanceof Polyline) {
                overlays.remove(i);
            }
        }
        roadOverlay = null;
        map.invalidate();
    }

    /** Перерисовка линии маршрута без сброса всех оверлеев (центр = 2-я точка). */
    private void redrawRouteToFinish(GeoPoint finish) {
        if (map == null || finish == null) {
            return;
        }
        if (startPoint == null && isValidRouteCoordinate(startLat, startLan)) {
            startPoint = new GeoPoint(startLat, startLan);
        }
        if (startPoint == null) {
            Logger.w(ctx, TAG, "redrawRouteToFinish: startPoint is null");
            return;
        }
        if (!isFinishDistinctFromStart(startPoint.getLatitude(), startPoint.getLongitude(),
                finish.getLatitude(), finish.getLongitude())) {
            removeRoadOverlayFromMap();
            map.invalidate();
            return;
        }
        showRout(startPoint, finish);
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

    private static final class RouteMarkerData {
        double startLat;
        double startLon;
        double finishLat;
        double finishLon;
        String startAddress = "";
        String finishAddress = "";
    }

    @Nullable
    private RouteMarkerData loadRouteMarkerData() {
        RouteMarkerData data = new RouteMarkerData();
        SQLiteDatabase database = null;
        Cursor cursor = null;
        try {
            database = ctx.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            cursor = database.rawQuery(
                    "SELECT startLat, startLan, to_lat, to_lng, start, finish FROM "
                            + MainActivity.ROUT_MARKER + " LIMIT 1",
                    null);
            if (!cursor.moveToFirst()) {
                return null;
            }
            data.startLat = cursor.getDouble(0);
            data.startLon = cursor.getDouble(1);
            data.finishLat = cursor.getDouble(2);
            data.finishLon = cursor.getDouble(3);
            data.startAddress = cursor.getString(4) != null ? cursor.getString(4) : "";
            data.finishAddress = cursor.getString(5) != null ? cursor.getString(5) : "";
            return data;
        } catch (Exception e) {
            Logger.e(ctx, TAG, "loadRouteMarkerData: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (database != null && database.isOpen()) {
                database.close();
            }
        }
    }

    private void applyPendingFinishFromBundle(RouteMarkerData data) {
        Bundle arguments = getArguments();
        if (arguments == null || data == null || !"finishMarker".equals(markerType)) {
            return;
        }
        String pendingAddress = arguments.getString("pendingFinishAddress");
        if (pendingAddress != null && !pendingAddress.trim().isEmpty()) {
            data.finishAddress = pendingAddress.trim();
        }
        if (arguments.containsKey("pendingFinishLat") && arguments.containsKey("pendingFinishLng")) {
            double pendingLat = arguments.getDouble("pendingFinishLat");
            double pendingLon = arguments.getDouble("pendingFinishLng");
            if (isValidRouteCoordinate(pendingLat, pendingLon)) {
                data.finishLat = pendingLat;
                data.finishLon = pendingLon;
            }
        }
    }

    private void newShowRout() {
        if (map == null || ctx == null) {
            initializeRegion();
            updateCenterMarkerVisibility();
            return;
        }

        map.getOverlays().clear();
        startTextOverlay = null;
        finishTextOverlay = null;
        map.invalidate();

        RouteMarkerData route = loadRouteMarkerData();
        if (route == null) {
            initializeRegion();
            updateCenterMarkerVisibility();
            return;
        }

        applyPendingFinishFromBundle(route);

        startLat = route.startLat;
        startLan = route.startLon;
        finishLat = route.finishLat;
        finishLan = route.finishLon;

        if (!isValidRouteCoordinate(startLat, startLan)) {
            initializeRegion();
            updateCenterMarkerVisibility();
            return;
        }

        fromAddressString = route.startAddress;
        String finishAddress = route.finishAddress.trim();
        boolean finishCityOnly = isFinishCityOnlyRoute(finishAddress);
        boolean hasFinishCoords = isValidRouteCoordinate(finishLat, finishLan);
        boolean finishDistinct = isFinishDistinctFromStart(startLat, startLan, finishLat, finishLan);
        boolean showFinishAndRoute = hasFinishCoords && finishDistinct && !finishCityOnly;

        toAddressString = resolveFinishAddressLabel(finishAddress);
        if (finishCityOnly) {
            toAddressString = "";
        }

        Logger.d(ctx, TAG, "newShowRout finish=" + finishAddress
                + " hasFinishCoords=" + hasFinishCoords
                + " showFinishAndRoute=" + showFinishAndRoute);

        startPoint = new GeoPoint(startLat, startLan);
        setMarker(startLat, startLan, fromAddressString, ctx, "1.");

        GeoPoint finishPoint = new GeoPoint(finishLat, finishLan);

        if (!userMovedMap && mapController != null) {
            if ("finishMarker".equals(markerType) && hasFinishCoords && !finishCityOnly) {
                mapController.setCenter(finishPoint);
            } else {
                mapController.setCenter(startPoint);
            }
        }

        if (showFinishAndRoute) {
            endPoint = finishPoint;
            setMarker(finishLat, finishLan, toAddressString, ctx, "2.");
            showRout(startPoint, finishPoint);
        } else {
            endPoint = null;
            finishMarkerObj = null;
            removeRoadOverlayFromMap();
            Logger.d(ctx, TAG, "Skipping finish marker/route: cityOnly=" + finishCityOnly
                    + " distinct=" + finishDistinct);
        }

        updateFinishPinDeferState(finishCityOnly);
        updateCenterMarkerVisibility();

        map.invalidate();
    }

    private boolean isMarkerEditMode() {
        return "startMarker".equals(markerType) || "finishMarker".equals(markerType);
    }

    private void updateFinishPinDeferState(boolean finishCityOnly) {
        deferFinishPinUntilInteraction = "finishMarker".equals(markerType) && finishCityOnly;
        if (deferFinishPinUntilInteraction) {
            finishPinRevealedByUser = false;
        }
    }

    private boolean shouldHideDeferredFinishPin() {
        return deferFinishPinUntilInteraction && !finishPinRevealedByUser;
    }

    private void revealFinishPinFromUserInteraction() {
        if (!deferFinishPinUntilInteraction || finishPinRevealedByUser || map == null) {
            return;
        }
        finishPinRevealedByUser = true;
        userMovedMap = true;
        endPoint = (GeoPoint) map.getMapCenter();
        updateCenterMarkerVisibility();
        Logger.d(ctx, TAG, "Finish pin revealed after map interaction (city-only / empty)");
    }

    private void updateCenterMarkerVisibility() {
        if (center_marker == null) {
            return;
        }
        if (isMarkerEditMode()) {
            syncEditModeOverlays();
            updateCenterMarkerIcon();
            if (shouldHideDeferredFinishPin()) {
                hideCenterMarker();
                return;
            }
            center_marker.setVisibility(View.VISIBLE);
            center_marker.setTranslationZ(8f);
            center_marker.bringToFront();
        } else {
            hideCenterMarker();
        }
    }

    /** В режиме выбора точки — только центральный пин; маркер на карте убираем, чтобы не путать при перетаскивании. */
    private void syncEditModeOverlays() {
        if (!isMarkerEditMode() || map == null) {
            return;
        }
        if ("startMarker".equals(markerType)) {
            if (startMarkerObj != null) {
                map.getOverlays().remove(startMarkerObj);
                startMarkerObj = null;
                removeTextOverlay("startMarker");
            }
        } else if ("finishMarker".equals(markerType)) {
            if (finishMarkerObj != null) {
                map.getOverlays().remove(finishMarkerObj);
                finishMarkerObj = null;
                removeTextOverlay("finishMarker");
            }
        }
    }

    private void hideCenterMarker() {
        if (center_marker != null) {
            center_marker.setVisibility(View.GONE);
        }
    }

    private void updateFinishMarkerOnMap() {
        if (endPoint == null || map == null) {
            return;
        }
        String title = toAddressString;
        if (title == null || title.trim().isEmpty()) {
            title = getString(R.string.end_point_marker);
        }
        setMarker(finishLat, finishLan, title, ctx, "2.");
    }

    private void updateStartMarkerOnMap() {
        if (map == null) {
            return;
        }
        if (startPoint == null && !isValidRouteCoordinate(startLat, startLan)) {
            return;
        }
        double lat = startPoint != null ? startPoint.getLatitude() : startLat;
        double lon = startPoint != null ? startPoint.getLongitude() : startLan;
        String title = fromAddressString;
        if (title == null || title.trim().isEmpty()) {
            title = getString(R.string.startPoint);
        }
        setMarker(lat, lon, title, ctx, "1.");
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
                    @SuppressLint("Range") double originLatitude = CursorReadHelper.getDouble(cursor, "startLat");
                    @SuppressLint("Range") double originLongitude = CursorReadHelper.getDouble(cursor, "startLan");
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
            Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
            Logger.w(requireActivity(), TAG, "NO INTERNET - Showing toast message");
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
                .addInterceptor(new RetryInterceptor())
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
    private void showSearchProgress() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            Toast.makeText(ctx, ctx.getString(R.string.searching_address), Toast.LENGTH_SHORT).show();
        });
    }
    // Вызов API для получения адреса по координатам
    private void makeApiCall(double latitude, double longitude, Context context) {
        Logger.d(context, TAG, "makeApiCall started with latitude=" + latitude + ", longitude=" + longitude + ", map=" + map + ", markerType=" + markerType);
        showSearchProgress();
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
                hideSearchProgress(); // Скрываем прогресс при ошибке
                Logger.e(context, TAG, "API call failed: " + t.getMessage());
                FirebaseCrashlytics.getInstance().recordException(t);
                Toast.makeText(context, "Ошибка поиска: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void hideSearchProgress() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // Подготовка маркера перед API вызовом — показываем центральный пин (не дублируем маркером на подложке)
    private void prepareMarker() {
        if (map == null) {
            Logger.e(ctx, TAG, "MapView is null, cannot prepare marker");
            return;
        }
        if ("startMarker".equals(markerType) || "finishMarker".equals(markerType)) {
            updateCenterMarkerVisibility();
            Logger.d(ctx, TAG, "Center pin shown before geocode, markerType=" + markerType);
        } else {
            Logger.e(ctx, TAG, "Invalid markerType: " + markerType);
            FirebaseCrashlytics.getInstance().recordException(new Exception("Invalid markerType: " + markerType));
        }
    }

    // Обработка ответа API
    // Обработка ответа API
    private void handleApiResponse(Response<ApiResponse> response, Context localizedContext) {
        // Скрываем прогресс в любом случае
        hideSearchProgress();

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
            Toast.makeText(ctx, "Ошибка получения адреса", Toast.LENGTH_SHORT).show();
        }
    }

    // Обработка ответа для начального маркера
    private void handleStartMarkerResponse(String result, Context localizedContext) {
        hideSearchProgress();
        if (startPoint == null) {
            Logger.e(ctx, TAG, "startPoint is null");
            return;
        }

        fromAddressString = result.equals("Точка на карте") ? localizedContext.getString(R.string.startPoint) : result;
        Logger.d(ctx, TAG, "fromAddressString set to: " + fromAddressString);

        if (previousMarker != null) {
            map.getOverlays().remove(previousMarker);
        }

        // ✅ ВЫЗЫВАЕМ ПРОВЕРКУ ГОРОДА ВМЕСТО ПРЯМОГО ОБНОВЛЕНИЯ
        checkCityAndUpdateStartPoint(startLat, startLan, fromAddressString);

        // Остальной код updateRouteSettings("start") будет выполнен в колбэке после подтверждения
    }

    // Обработка ответа для конечного маркера
    private void handleFinishMarkerResponse(String result, Context localizedContext) {
        hideSearchProgress();

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

        // Установка строки адреса
        toAddressString = result.equals("Точка на карте") ? localizedContext.getString(R.string.end_point_marker) : result;
        Logger.d(ctx, TAG, "toAddressString set to: " + toAddressString);

        // ПРОВЕРКА: если адрес содержит "по городу" - не показываем маркер и не строим маршрут
        if (isCityOnlyAddress(toAddressString)) {
            Logger.d(ctx, TAG, "Address contains 'по городу', skipping marker and route");

            // Удаляем маркер, если он есть
            if (finishMarkerObj != null) {
                map.getOverlays().remove(finishMarkerObj);
                finishMarkerObj = null;
            }

            // Удаляем текстовый оверлей
            removeTextOverlay("finishMarker");

            // Очищаем конечную точку
            endPoint = null;

            removeRoadOverlayFromMap();
            return;
        }

        finishLat = endPoint.getLatitude();
        finishLan = endPoint.getLongitude();
        setMarker(finishLat, finishLan, toAddressString, ctx, "2.");

        try {
            if (VisicomFragment.textViewTo != null) {
                VisicomFragment.textViewTo.setText(toAddressString);
            }
            updateRouteSettings("finish");
            Logger.d(ctx, TAG, "Finish updated from map center");
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

        updateRoutMarker(settings, map.getContext());
        if (point.equals("start")) {
            updateMyPosition(startLat, startLan, fromAddressString, map.getContext());
            new Handler(Looper.getMainLooper()).post(this::newShowRout);
        } else if (point.equals("finish")) {
            endPoint = new GeoPoint(finishLat, finishLan);
            String finishLabel = toAddressString;
            new Handler(Looper.getMainLooper()).post(() -> {
                setMarker(finishLat, finishLan, finishLabel, ctx, "2.");
                redrawRouteToFinish(endPoint);
            });
        }
        Logger.d(ctx, TAG, "Updated route settings, point=" + point);
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
// Установка маркера
    // Установка маркера
    private void setMarker(double lat, double lon, String title, Context context, String prefix) {
        if (map == null) {
            Logger.e(context, TAG, "MapView is null, cannot set marker");
            return;
        }

        // Проверяем, что мы на главном потоке
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(() -> setMarker(lat, lon, title, context, prefix));
            return;
        }

        try {
            // Проверяем, что map всё ещё валиден
            if (map.getRepository() == null) {
                Logger.e(context, TAG, "Map repository is null, cannot create marker");
                return;
            }

            // В режиме отображения маршрута setMarker вызывается дважды (1. и 2.).
            // markerType отражает "что редактируем сейчас", но для отрисовки нужно различать роль по prefix.
            final String role;
            if ("1.".equals(prefix)) {
                role = "startMarker";
            } else if ("2.".equals(prefix)) {
                role = "finishMarker";
            } else {
                role = markerType;
            }

            if (isMarkerEditMode() && role.equals(markerType)) {
                updateCenterMarkerVisibility();
                showEditModeAddressLabel(role, lat, lon, title, prefix);
                Logger.d(context, TAG, "Edit mode: center pin + address label at " + lat + ", " + lon);
                return;
            }

            // Удаляем старый маркер и подпись для соответствующей роли
            if ("startMarker".equals(role)) {
                if (startMarkerObj != null) {
                    map.getOverlays().remove(startMarkerObj);
                }
                removeTextOverlay("startMarker");
            } else if ("finishMarker".equals(role)) {
                if (finishMarkerObj != null) {
                    map.getOverlays().remove(finishMarkerObj);
                }
                removeTextOverlay("finishMarker");
            }

            // Create new marker
            Marker marker = new Marker(map);
            // Маркер должен быть кликабельным, иначе при тапе по 2-й точке будет срабатывать "текущая" (markerType).
            marker.setVisible(true);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            BitmapDrawable markerIcon = getMarkerIconForRole(context, role);
            if (markerIcon != null) {
                marker.setIcon(markerIcon);
            }
            marker.setRelatedObject(role);
            marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                Object related = clickedMarker.getRelatedObject();
                if ("finishMarker".equals(related)) {
                    markerType = "finishMarker";
                } else if ("startMarker".equals(related)) {
                    markerType = "startMarker";
                }
                updateCenterMarkerVisibility();
                return false; // стандартное поведение (InfoWindow), если оно включено
            });

            // Set marker position
            marker.setPosition(new GeoPoint(lat, lon));

            // Add marker to map
            map.getOverlays().add(marker);

            // Add custom text overlay for persistent label (отдельно для старта и финиша)
            Overlay textOverlay = createTextOverlay(new GeoPoint(lat, lon), prefix + unuString + title, role);
            if ("startMarker".equals(role)) {
                if (startTextOverlay != null) {
                    map.getOverlays().remove(startTextOverlay);
                }
                startTextOverlay = textOverlay;
            } else if ("finishMarker".equals(role)) {
                if (finishTextOverlay != null) {
                    map.getOverlays().remove(finishTextOverlay);
                }
                finishTextOverlay = textOverlay;
            }
            map.getOverlays().add(textOverlay);

            // Update map
            map.invalidate();

            // Update marker references
            if ("startMarker".equals(role)) {
                startMarkerObj = marker;
            } else if ("finishMarker".equals(role)) {
                finishMarkerObj = marker;
            }

            Logger.d(context, TAG, "Marker set successfully at: " + lat + ", " + lon);

        } catch (Exception e) {
            Logger.e(context, TAG, "Error setting marker: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void removeTextOverlay(String role) {
        if (map == null) {
            return;
        }
        if ("startMarker".equals(role)) {
            if (startTextOverlay != null) {
                map.getOverlays().remove(startTextOverlay);
                startTextOverlay = null;
                Logger.d(ctx, TAG, "Start text overlay removed");
            }
            return;
        }
        if ("finishMarker".equals(role)) {
            if (finishTextOverlay != null) {
                map.getOverlays().remove(finishTextOverlay);
                finishTextOverlay = null;
                Logger.d(ctx, TAG, "Finish text overlay removed");
            }
        }
    }


    private void showEditModeAddressLabel(String role, double lat, double lon, String title, String prefix) {
        if (map == null) {
            return;
        }
        removeTextOverlay(role);
        String label = prefix + (unuString != null ? unuString : "") + title;
        Overlay overlay = createTextOverlay(new GeoPoint(lat, lon), label, role);
        if ("startMarker".equals(role)) {
            startTextOverlay = overlay;
        } else if ("finishMarker".equals(role)) {
            finishTextOverlay = overlay;
        }
        map.getOverlays().add(overlay);
        map.invalidate();
    }

    private int resolveRoleAccentColor(Context context, @Nullable String role) {
        if ("finishMarker".equals(role)) {
            return ContextCompat.getColor(context, R.color.map_pin_finish);
        }
        if ("startMarker".equals(role)) {
            return ContextCompat.getColor(context, R.color.map_pin_start);
        }
        return ContextCompat.getColor(context, R.color.zamov);
    }

    @Nullable
    private String resolveRoleHeader(Context context, @Nullable String role) {
        if ("startMarker".equals(role)) {
            return context.getString(R.string.startPoint);
        }
        if ("finishMarker".equals(role)) {
            return context.getString(R.string.end_point_marker);
        }
        return null;
    }

    private String stripLabelPrefix(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("1.") || cleaned.startsWith("2.")) {
            cleaned = cleaned.substring(2).trim();
        }
        if (unuString != null && cleaned.startsWith(unuString)) {
            cleaned = cleaned.substring(unuString.length()).trim();
        }
        return cleaned;
    }

    private Overlay createTextOverlay(GeoPoint point, String text, @Nullable String role) {
        return new Overlay() {
            @Override
            public void draw(Canvas canvas, MapView mapView, boolean shadow) {
                if (shadow) {
                    return;
                }

                Context context = mapView.getContext();
                float density = context.getResources().getDisplayMetrics().density;
                int accentColor = resolveRoleAccentColor(context, role);
                String header = resolveRoleHeader(context, role);
                String body = stripLabelPrefix(text);
                if (body.isEmpty()) {
                    return;
                }

                Projection projection = mapView.getProjection();
                Point screenPoint = new Point();
                projection.toPixels(point, screenPoint);

                int maxWidth = (int) (mapView.getWidth() * 0.38f);
                if (maxWidth < (int) (95 * density)) {
                    maxWidth = (int) (110 * density);
                }
                int paddingH = (int) (10 * density);
                int paddingV = (int) (6 * density);
                int accentBarHeight = header != null ? (int) (26 * density) : 0;
                int cornerRadius = (int) (8 * density);
                int arrowHeight = (int) (6 * density);
                int pinOffset = (int) (58 * density);

                TextPaint headerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                headerPaint.setColor(Color.WHITE);
                headerPaint.setTextSize(13f * density);
                headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
                bodyPaint.setColor(Color.parseColor("#1F2937"));
                bodyPaint.setTextSize(16f * density);
                bodyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

                int bodyWidth = maxWidth - paddingH * 2;
                StaticLayout bodyLayout = StaticLayout.Builder
                        .obtain(body, 0, body.length(), bodyPaint, bodyWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, 1.05f)
                        .build();

                int bodyHeight = bodyLayout.getHeight();
                int totalHeight = accentBarHeight + bodyHeight + paddingV * 2;

                float left = screenPoint.x - maxWidth / 2f;
                float top = screenPoint.y - totalHeight - arrowHeight - pinOffset;
                float right = left + maxWidth;
                float bottom = top + totalHeight;

                Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                shadowPaint.setColor(Color.argb(36, 0, 0, 0));
                RectF shadowRect = new RectF(
                        left + density,
                        top + 2 * density,
                        right + density,
                        bottom + 2 * density
                );
                canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint);

                Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                bgPaint.setColor(Color.WHITE);
                RectF bgRect = new RectF(left, top, right, bottom);
                canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, bgPaint);

                if (header != null && accentBarHeight > 0) {
                    Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    accentPaint.setColor(accentColor);
                    RectF accentRect = new RectF(left, top, right, top + accentBarHeight);
                    canvas.drawRoundRect(accentRect, cornerRadius, cornerRadius, accentPaint);
                    canvas.drawRect(left, top + accentBarHeight - cornerRadius, right, top + accentBarHeight, accentPaint);

                    float headerX = left + paddingH;
                    float headerY = top + accentBarHeight - (paddingV * 0.6f);
                    canvas.drawText(header, headerX, headerY, headerPaint);
                }

                Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                borderPaint.setColor(accentColor);
                borderPaint.setStyle(Paint.Style.STROKE);
                borderPaint.setStrokeWidth(Math.max(1f, density));
                canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, borderPaint);

                Path arrowPath = new Path();
                float arrowWidth = 10 * density;
                arrowPath.moveTo(screenPoint.x - arrowWidth / 2f, bottom);
                arrowPath.lineTo(screenPoint.x + arrowWidth / 2f, bottom);
                arrowPath.lineTo(screenPoint.x, bottom + arrowHeight);
                arrowPath.close();
                canvas.drawPath(arrowPath, bgPaint);
                canvas.drawPath(arrowPath, borderPaint);

                canvas.save();
                canvas.translate(left + paddingH, top + accentBarHeight + paddingV);
                bodyLayout.draw(canvas);
                canvas.restore();
            }
        };
    }
    // Отображение маршрута
    private void showRout(GeoPoint startP, GeoPoint endP) {
        if (startP == null || endP == null) {
            Logger.e(ctx, TAG, "Cannot show route: startP or endP is null");
            return;
        }
        if (map == null) {
            return;
        }
        final int generation = ++routeBuildGeneration;
        removeRoadOverlayFromMap();
        executor.execute(() -> {
            RoadManager roadManager = new OSRMRoadManager(map.getContext(), System.getProperty("http.agent"));
            ArrayList<GeoPoint> waypoints = new ArrayList<>();
            waypoints.add(startP);
            waypoints.add(endP);
            Road road = roadManager.getRoad(waypoints);
            Polyline newOverlay = RoadManager.buildRoadOverlay(road);
            newOverlay.getOutlinePaint().setStrokeWidth(10f);
            map.post(() -> {
                if (!isAdded() || map == null || generation != routeBuildGeneration) {
                    return;
                }
                removeRoadOverlayFromMap();
                roadOverlay = newOverlay;
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
            result = CursorReadHelper.getDouble(cursor, columnName);
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
                    list.add(CursorReadHelper.getString(c, cn));
                }
            } while (c.moveToNext());
            c.close();
        }
        database.close();
        return list;
    }

    private void setupMapTapZoom() {
        gestureDetector = new GestureDetectorCompat(requireContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (map == null) return false;

                if ("finishMarker".equals(markerType) && shouldHideDeferredFinishPin()) {
                    revealFinishPinFromUserInteraction();
                    return true;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastZoomTime < ZOOM_COOLDOWN_MS) {
                    return false;
                }
                lastZoomTime = currentTime;

                Projection proj = map.getProjection();
                GeoPoint tapPoint = (GeoPoint) proj.fromPixels((int) e.getX(), (int) e.getY());
                if (tapPoint == null) return false;

                GeoPoint currentMarkerPoint = null;
                if ("startMarker".equals(markerType) && startPoint != null) {
                    currentMarkerPoint = startPoint;
                } else if ("finishMarker".equals(markerType) && endPoint != null) {
                    currentMarkerPoint = endPoint;
                } else {
                    return false;
                }

                // Вычисляем середину отрезка
                double newLat = currentMarkerPoint.getLatitude() +
                        (tapPoint.getLatitude() - currentMarkerPoint.getLatitude()) * 0.75;
                double newLon = currentMarkerPoint.getLongitude() +
                        (tapPoint.getLongitude() - currentMarkerPoint.getLongitude()) * 0.75;
                GeoPoint newCenter = new GeoPoint(newLat, newLon);

                // Перемещаем центр карты
                mapController.setCenter(newCenter);

                // Увеличение зума
                double currentZoom = map.getZoomLevelDouble();
                double maxUsefulZoom = 19.0;
                double actualMaxZoom = Math.min(map.getMaxZoomLevel(), maxUsefulZoom);

                if (currentZoom < actualMaxZoom - 0.1) {
                    mapController.setZoom(Math.min(currentZoom + 2.0, actualMaxZoom));
                } else {
                    Toast.makeText(ctx, ctx.getString(R.string.max_zoom), Toast.LENGTH_SHORT).show();
                }

                // Обновляем координаты в БД и на карте
                if ("startMarker".equals(markerType)) {
                    startPoint = newCenter;
                    startLat = newCenter.getLatitude();
                    startLan = newCenter.getLongitude();

                    // Обновляем БД через существующий механизм
                    updateMyPosition(startLat, startLan, fromAddressString, ctx);

                    // Вызываем API и перерисовку через существующий поток
                    makeApiCall(startLat, startLan, ctx);

                } else if ("finishMarker".equals(markerType)) {
                    endPoint = newCenter;
                    finishLat = newCenter.getLatitude();
                    finishLan = newCenter.getLongitude();

                    // Обновляем finish координаты в БД через updateRouteSettings
                    // Для finish нужно также обновить to_lat/to_lng
                    updateFinishPointInDatabase(finishLat, finishLan, toAddressString);

                    // Вызываем API и перерисовку
                    makeApiCall(finishLat, finishLan, ctx);
                }

                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                if (map == null) return false;

                double currentZoom = map.getZoomLevelDouble();
                double maxUsefulZoom = 19.0;
                double actualMaxZoom = Math.min(map.getMaxZoomLevel(), maxUsefulZoom);

                if (currentZoom >= actualMaxZoom - 0.1) {
                    Toast.makeText(ctx, ctx.getString(R.string.max_zoom), Toast.LENGTH_SHORT).show();
                    return true;
                }

                mapController.zoomIn();
                return true;
            }
        });
    }
    private void updateFinishPointInDatabase(double lat, double lon, String address) {
        SQLiteDatabase database = ctx.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            ContentValues values = new ContentValues();
            values.put("to_lat", lat);
            values.put("to_lng", lon);
            values.put("finish", address);

            int rowsUpdated = database.update(MainActivity.ROUT_MARKER, values, null, null);
            if (rowsUpdated == 0) {
                // Если нет записи, создаём с начальными значениями
                values.put("startLat", startLat);
                values.put("startLan", startLan);
                values.put("start", fromAddressString);
                database.insert(MainActivity.ROUT_MARKER, null, values);
            }
            Logger.d(ctx, TAG, "Updated finish point: lat=" + lat + ", lon=" + lon);
        } catch (Exception e) {
            Logger.e(ctx, TAG, "Failed to update finish point: " + e.getMessage());
        } finally {
            database.close();
        }
    }

    private boolean isFinishCityOnlyRoute(String finishAddress) {
        if (isCityOnlyAddress(finishAddress)) {
            return true;
        }
        Bundle arguments = getArguments();
        return arguments != null && arguments.getBoolean("finishCityOnly", false);
    }

    /** Согласовано с VisicomFragment.isCityOnlyFinishInDatabase — пустой finish = «по городу». */
    private boolean isCityOnlyAddress(@Nullable String address) {
        if (address == null) {
            return true;
        }
        String trimmed = address.trim();
        if (trimmed.isEmpty()) {
            return true;
        }
        return trimmed.equals(getString(R.string.on_city_tv))
                || trimmed.equals(getString(R.string.on_city))
                || trimmed.contains("по місту")
                || trimmed.contains("по городу")
                || trimmed.contains("around the city");
    }

    private boolean isValidRouteCoordinate(double lat, double lon) {
        return Math.abs(lat) > 1e-6 || Math.abs(lon) > 1e-6;
    }

    private boolean isFinishDistinctFromStart(double startLat, double startLon, double finishLat, double finishLon) {
        if (!isValidRouteCoordinate(finishLat, finishLon)) {
            return false;
        }
        if (!isValidRouteCoordinate(startLat, startLon)) {
            return true;
        }
        double dLat = startLat - finishLat;
        double dLon = startLon - finishLon;
        return (dLat * dLat + dLon * dLon) > 4e-8;
    }

    private String resolveFinishAddressLabel(String finishAddress) {
        if (finishAddress != null && !finishAddress.trim().isEmpty()) {
            return finishAddress.trim();
        }
        return getString(R.string.end_point_marker);
    }

    private void checkCityAndUpdateStartPoint(double latitude, double longitude, String address) {
        Logger.d(ctx, TAG, "checkCityAndUpdateStartPoint: lat=" + latitude + ", lon=" + longitude + ", address=" + address);

        // Проверка, что фрагмент ещё активен
        if (!isAdded() || getActivity() == null) {
            Logger.d(ctx, TAG, "Fragment not attached, skipping city check");
            return;
        }

        // ✅ Устанавливаем StatusX в false, чтобы показать крестик на GPS кнопке
        sharedPreferencesHelperMain.saveValue("setStatusX", true);
        if (viewModel != null) {
            viewModel.setStatusX(true);
        }
        Logger.d(ctx, TAG, "setStatusX установлен в true - показываем крестик (выбор точки на карте)");

        // Проверяем, не занят ли CityFinder
        if (CityFinder.isCityFinderBusy()) {
            Logger.d(ctx, TAG, "CityFinder занят, пропускаем обновление города");
            return;
        }

        CityFinder cityFinder = new CityFinder(ctx, latitude, longitude, address, (Activity) ctx);

        cityFinder.findCityWithCallback(latitude, longitude, (cityChanged, userConfirmed) -> {
            // Проверка, что фрагмент всё ещё активен
            if (!isAdded() || getActivity() == null || map == null) {
                Logger.d(ctx, TAG, "Fragment detached or map null during city callback");
                return;
            }

            Logger.d(ctx, TAG, "═══════════════════════════════════════════");
            Logger.d(ctx, TAG, "🏁 CityCheckCallback ВЫЗВАН (OSM)");
            Logger.d(ctx, TAG, "═══════════════════════════════════════════");
            Logger.d(ctx, TAG, "cityChanged = " + cityChanged + ", userConfirmed = " + userConfirmed);

            boolean shouldUpdatePosition = !cityChanged || (cityChanged && userConfirmed);

            if (shouldUpdatePosition) {
                Logger.d(ctx, TAG, "✅ Обновляем позицию");

                // Сохраняем координаты
                startLat = latitude;
                startLan = longitude;
                startPoint = new GeoPoint(latitude, longitude);
                fromAddressString = address;

                // Обновляем БД
                updateStartPointInDatabase(latitude, longitude, address);

                // Обновляем заголовок города в ActionBar
                setCityAppbar();

                // Обновляем маркер на карте (уже в UI потоке)
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (map != null && isAdded()) {
                        // Удаляем старый маркер и текстовый оверлей
                        if (startMarkerObj != null) {
                            map.getOverlays().remove(startMarkerObj);
                        }
                        removeTextOverlay("startMarker"); // Удаляем старый текстовый оверлей

                        // Устанавливаем новый маркер
                        setMarker(latitude, longitude, address, ctx, "1.");
                        map.invalidate();
                        mapController.setCenter(startPoint);

                        Logger.d(ctx, TAG, "Marker and map updated successfully");
                        updateRouteAfterStartPointChange();
                    }
                });

            } else {
                Logger.d(ctx, TAG, "❌ Пользователь отказался от смены города");
                // Если пользователь отказался, возвращаем StatusX обратно в true
                sharedPreferencesHelperMain.saveValue("setStatusX", true);
                if (viewModel != null) {
                    viewModel.setStatusX(true);
                }
            }

            Logger.d(ctx, TAG, "═══════════════════════════════════════════");
        });
    }

    // Новый метод для обновления маршрута после смены стартовой точки
    private void updateRouteAfterStartPointChange() {
        try {
            // Получаем текущую конечную точку из БД
            SQLiteDatabase database = ctx.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            Cursor cursor = database.rawQuery("SELECT to_lat, to_lng, finish FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);

            double toLat = 0.0;
            double toLng = 0.0;
            String finish = "";

            if (cursor.moveToFirst()) {
                toLat = cursor.getDouble(0);
                toLng = cursor.getDouble(1);
                finish = cursor.getString(2) != null ? cursor.getString(2) : "";
            }
            cursor.close();
            database.close();

            // Если есть конечная точка и это не "по городу", показываем маршрут
            if (toLat != 0.0 && !isCityOnlyAddress(finish)) {
                GeoPoint endGeoPoint = new GeoPoint(toLat, toLng);
                setMarker(toLat, toLng, finish, ctx, "2.");
                showRout(startPoint, endGeoPoint);
            }

        } catch (Exception e) {
            Logger.e(ctx, TAG, "Error updating route: " + e.getMessage());
        }
    }

    private void updateStartPointInDatabase(double latitude, double longitude, String address) {
        SQLiteDatabase database = ctx.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        try {
            // Сохраняем текущую конечную точку
            Cursor cursor = database.rawQuery("SELECT to_lat, to_lng, finish FROM " + MainActivity.ROUT_MARKER + " LIMIT 1", null);
            double existingToLat = 0.0;
            double existingToLng = 0.0;
            String existingFinish = "";

            if (cursor.moveToFirst()) {
                existingToLat = cursor.getDouble(0);
                existingToLng = cursor.getDouble(1);
                existingFinish = cursor.getString(2) != null ? cursor.getString(2) : "";
            }
            cursor.close();

            ContentValues values = new ContentValues();
            values.put("startLat", latitude);
            values.put("startLan", longitude);
            values.put("start", address);
            values.put("to_lat", existingToLat);
            values.put("to_lng", existingToLng);
            values.put("finish", existingFinish);

            int rowsUpdated = database.update(MainActivity.ROUT_MARKER, values, "id = ?", new String[]{"1"});
            if (rowsUpdated == 0) {
                database.insert(MainActivity.ROUT_MARKER, null, values);
            }

            // Также обновляем TABLE_POSITION_INFO
            ContentValues posValues = new ContentValues();
            posValues.put("startLat", latitude);
            posValues.put("startLan", longitude);
            posValues.put("position", address);
            int posRows = database.update(MainActivity.TABLE_POSITION_INFO, posValues, "id = ?", new String[]{"1"});
            if (posRows == 0) {
                posValues.put("id", 1);
                database.insert(MainActivity.TABLE_POSITION_INFO, null, posValues);
            }

            Logger.d(ctx, TAG, "✅ Стартовая точка обновлена в БД: (" + latitude + ", " + longitude + ") -> " + address);

        } catch (Exception e) {
            Logger.e(ctx, TAG, "Ошибка обновления стартовой точки: " + e.getMessage());
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            database.close();
        }
    }

    private void setCityAppbar() {
        // Скопируйте этот метод из VisicomFragment или вызовите аналогичный
        // Обновляет заголовок в ActionBar в зависимости от текущего города
        List<String> stringList = logCursor(MainActivity.CITY_INFO, ctx);
        if (stringList.size() >= 2) {
            String city = stringList.get(1);
            String cityMenu = getCityDisplayName(city);
            String newTitle = getString(R.string.menu_city) + " " + cityMenu;
            sharedPreferencesHelperMain.saveValue("newTitle", newTitle);

            // Обновляем ActionBar, если Activity поддерживает
            if (requireActivity() instanceof AppCompatActivity) {
                androidx.appcompat.app.ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(newTitle);
                }
            }
        }
    }

    private String getCityDisplayName(String city) {
        switch (city) {
            case "Kyiv City": return getString(R.string.city_kyiv);
            case "Dnipropetrovsk Oblast": return getString(R.string.city_dnipro);
            case "Odessa": return getString(R.string.city_odessa);
            case "Zaporizhzhia": return getString(R.string.city_zaporizhzhia);
            case "Cherkasy Oblast": return getString(R.string.city_cherkassy);
            case "Lviv": return getString(R.string.city_lviv);
            case "Ivano_frankivsk": return getString(R.string.city_ivano_frankivsk);
            case "Vinnytsia": return getString(R.string.city_vinnytsia);
            case "Poltava": return getString(R.string.city_poltava);
            case "Sumy": return getString(R.string.city_sumy);
            case "Kharkiv": return getString(R.string.city_kharkiv);
            case "Chernihiv": return getString(R.string.city_chernihiv);
            case "Rivne": return getString(R.string.city_rivne);
            case "Ternopil": return getString(R.string.city_ternopil);
            case "Khmelnytskyi": return getString(R.string.city_khmelnytskyi);
            case "Zakarpattya": return getString(R.string.city_zakarpattya);
            case "Zhytomyr": return getString(R.string.city_zhytomyr);
            case "Kropyvnytskyi": return getString(R.string.city_kropyvnytskyi);
            case "Mykolaiv": return getString(R.string.city_mykolaiv);
            case "Chernivtsi": return getString(R.string.city_chernivtsi);
            case "Lutsk": return getString(R.string.city_lutsk);
            default: return getString(R.string.foreign_countries);
        }
    }
}