package com.taxi.easy.ua.ui.active_order;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavOptions;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.androidx.startup.MyApplication;
import com.taxi.easy.ua.databinding.FragmentCancelBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.orders.OrderHistoryStatusHelper;
import com.taxi.easy.ua.utils.phone_state.PhoneCallHelper;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;
import com.taxi.easy.ua.utils.ui.ListScrollPaginationHelper;
import com.uxcam.UXCam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.taxi.easy.ua.utils.db.CursorReadHelper;
import com.taxi.easy.ua.utils.model.ExecutionStatusViewModel;


public class ActiveOrderFragment extends Fragment {

    private static final String TAG = "CancelFragment";
    private @NonNull FragmentCancelBinding binding = null;
    private ListView listView;
    private String[] array;


    ProgressBar progressBar;

    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;

    private List<RouteResponseCancel> routeList;

    AppCompatButton upd_but;
    private View scrollButtonDown, scrollButtonUp;
    private ListScrollPaginationHelper scrollPagination;
    private TextView textUid;
    private String email;
    private FragmentManager fragmentManager;
    Context context;
    private final int desiredHeight = 1200;
    public static AppCompatButton btnCallAdmin;
    private Handler handler;
    private Runnable taskRunnable;

    public ActiveOrderFragment() {
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);
        if(button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }
        binding = FragmentCancelBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context = requireActivity();

        // Включаем блокировку кнопки "Назад" Применяем блокировку кнопки "Назад"
        BackPressBlocker  backPressBlocker = new BackPressBlocker();
        backPressBlocker.setBackButtonBlocked(true);
        backPressBlocker.blockBackButtonWithCallback(this);

        Logger.d(context, TAG, "onContextItemSelected: ");

        fragmentManager = getParentFragmentManager();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        listView = binding.listView;
        progressBar = binding.progressBar;


        email = logCursor(MainActivity.TABLE_USER_INFO, Objects.requireNonNull(requireActivity())).get(3);

        databaseHelper = new DatabaseHelper(getContext());
        databaseHelperUid = new DatabaseHelperUid(getContext());

        btnCallAdmin = binding.btnCallAdmin;
        btnCallAdmin.setOnClickListener(v -> {
            PhoneCallHelper.callWithFallback(() -> {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, MyApplication.getContext());
                return stringList.size() > 3 ? stringList.get(3) : "";
            });
//            Intent intent = new Intent(Intent.ACTION_DIAL);
//            String phone = logCursor(MainActivity.CITY_INFO, requireActivity()).get(3);
//            intent.setData(Uri.parse(phone));
//            startActivity(intent);
        });

        textUid  = binding.textUid;
        upd_but = binding.updBut;
        upd_but.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
                Logger.w(context, TAG, "NO INTERNET - Showing toast message");
//                
            }
        });
        scrollButtonUp = binding.scrollControls.scrollButtonUp;
        scrollButtonDown = binding.scrollControls.scrollButtonDown;
        scrollPagination = new ListScrollPaginationHelper(
                listView,
                binding.scrollControls.tvScrollPosition,
                scrollButtonUp,
                scrollButtonDown);
        scrollPagination.bind();
        scrollPagination.wireScrollButtons();

        startRepeatingTask();
        return root;
    }

    private void fetchRoutesCancel(String value) {
        progressBar.setVisibility(VISIBLE);
        listView.setVisibility(View.GONE);
        if (scrollPagination != null) {
            scrollPagination.update();
        }

        routeList = new ArrayList<>();

        upd_but.setText(context.getString(R.string.cancel_gps));
        upd_but.setOnClickListener(v -> MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_visicom, true)
                .build()));

        String  baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");


        List<String> stringList = logCursor(MainActivity.CITY_INFO,context);
        String city = stringList.get(1);

        String url = baseUrl + "/android/UIDStatusShowEmailCancelApp/" + value + "/" + city + "/" +  context.getString(R.string.application);
        Logger.d(context, TAG, "fetchRoutesCancel: " + url);
        Call<List<RouteResponseCancel>> call = ApiClient.getApiService().getRoutesCancel(url);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Response<List<RouteResponseCancel>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<RouteResponseCancel> routes = response.body();
                    Logger.d(context, TAG, "onResponse: " + routes.toString());
                    if (!routes.isEmpty()) {

                        boolean hasRouteWithAsterisk = false;
                        for (RouteResponseCancel route : routes) {
                            if ("*".equals(route.getRouteFrom())) {
                                // Найден объект с routefrom = "*"
                                hasRouteWithAsterisk = true;
                                break;  // Выход из цикла, так как условие уже выполнено
                            }
                        }
                        if (!hasRouteWithAsterisk) {
                            //  stopRepeatingTask();
                            routeList.addAll(routes);
                            processCancelList();
                            textUid.setVisibility(VISIBLE);
                            textUid.setText(R.string.order_to_cancel);
                            stopRepeatingTask();
                        } else {
                            textUid.setVisibility(VISIBLE);
                            textUid.setText(R.string.no_routs);
                        }

                    } else {
                        clearCancelTables();
                        listView.setVisibility(View.GONE);
                        if (scrollPagination != null) {
                            scrollPagination.update();
                        }
                        textUid.setVisibility(VISIBLE);
                        textUid.setText(R.string.no_routs);
                    }
                } else {
                    Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
                    Logger.w(context, TAG, "NO INTERNET - Showing toast message");
                }
                if (!isAdded() || isDetached() || getActivity() == null) {
                    return;
                }

                // Безопасное выполнение на UI потоке
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> {
                    if (isAdded() && !isDetached() && progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }

            public void onFailure(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Throwable t) {
                FirebaseCrashlytics.getInstance().recordException(t);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                    });
                } else {
                    Logger.i(requireContext(), TAG, "Фрагмент не привязан к активности, пропуск обработки UI");
                }
            }


        });
        if (isAdded()) {
            upd_but.setText(context.getString(R.string.order));
            progressBar.setVisibility(View.GONE);
        }
    }

    private void clearCancelTables() {
        databaseHelper.clearTableCancel();
        databaseHelperUid.clearTableCancel();
    }

    private void processCancelList() {
        // Фильтрация дубликатов по уникальному uid
        Set<String> uniqueUids = new HashSet<>();
        routeList.removeIf(route -> !uniqueUids.add(route.getUid())); // Удаляем дубликаты

        clearCancelTables();

        // Создайте массив строк
        array = new String[routeList.size()];

        for (int i = 0; i < routeList.size(); i++) {
            RouteResponseCancel route = routeList.get(i);
            String uid = route.getUid();
            Logger.d(getContext(), TAG, "uid processCancelList: " + uid);
            String routeFrom = route.getRouteFrom();
            String routeFromNumber = route.getRouteFromNumber();
            String routeTo = route.getRouteTo();
            String routeToNumber = route.getRouteToNumber();
            String webCost = route.getWebCost();
            String createdAt = route.getCreatedAt();
            String closeReason = route.getCloseReason();
            String auto = route.getAuto();
            String dispatchingOrderUidDouble = route.getDispatchingOrderUidDouble();
            Logger.d(getContext(), TAG, "uid_Double processCancelList: " + dispatchingOrderUidDouble);
            String pay_method = route.getPay_method();
            String required_time = route.getRequired_time();
            String required_time_clear = route.getRequired_time();
            String flexible_tariff_name = route.getFlexible_tariff_name();
            String comment_info = route.getComment_info();
            String extra_charge_codes = route.getExtra_charge_codes();

            String closeReasonText = OrderHistoryStatusHelper.resolveStatusText(
                    context,
                    closeReason,
                    route.getExecution_status(),
                    required_time,
                    uid
            );

            if (routeFrom.equals("Місце відправлення")) {
                routeFrom = context.getString(R.string.start_point_text);
            }

            if (routeTo.equals("Точка на карте")) {
                routeTo = context.getString(R.string.end_point_marker);
            }
            if (routeTo.contains("по городу") || routeTo.contains("по місту")) {
                routeTo = context.getString(R.string.on_city);
            }

            String required_time_text = "";
            if (required_time != null && !required_time.contains("01.01.1970")) {
                required_time_text = requireActivity().getString(R.string.ex_st_5) + required_time;
            }

            String routeInfo = "";
            if (auto == null) {
                auto = "??";
            }

            if (routeFrom.equals(routeTo)) {
                routeInfo = routeFrom + ", " + routeFromNumber
                        + getString(R.string.close_resone_to)
                        + getString(R.string.on_city)
                        + required_time_text + "#"
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH) + "#"
                        + getString(R.string.auto_info) + " " + auto + "#"
                        + getString(R.string.close_resone_time) + createdAt + "#"
                        + getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = routeFrom + ", " + routeFromNumber
                        + getString(R.string.close_resone_to) + routeTo + " " + routeToNumber + "."
                        + required_time_text + "#"
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH) + "#"
                        + getString(R.string.auto_info) + " " + auto + "#"
                        + getString(R.string.close_resone_time) + createdAt + "#"
                        + getString(R.string.close_resone_text) + closeReasonText;
            }

            array[i] = routeInfo;
            databaseHelper.addRouteCancel(uid, routeInfo); // Добавляем только уникальные записи

            List<String> settings = new ArrayList<>();
            settings.add(uid);
            settings.add(webCost);
            settings.add(routeFrom);
            settings.add(routeFromNumber);
            settings.add(routeTo);
            settings.add(routeToNumber);
            settings.add(dispatchingOrderUidDouble);
            settings.add(pay_method);
            settings.add(takeData(required_time));
            settings.add(flexible_tariff_name);
            settings.add(comment_info);
            settings.add(extra_charge_codes);

            Logger.d(context, TAG, settings.toString());
            databaseHelperUid.addCancelInfoUid(settings);
        }

        if (array != null && array.length > 0) {
            List<String> itemList = Arrays.asList(array); // Преобразование в List

            CustomArrayCancelAdapter adapter = new CustomArrayCancelAdapter(
                    context,
                    R.layout.drop_down_layout,  // Ваш макет элемента списка
                    R.id.text1,  // ID TextView в вашем макете
                    R.id.text2,  // ID TextView в вашем макете
                    R.id.text3,  // ID TextView в вашем макете
                    R.id.text4,  // ID TextView в вашем макете
                    R.id.text5,  // ID TextView в вашем макете
                    itemList  // Список строк
            );
            listView.setAdapter(adapter);

            listView.setVisibility(VISIBLE);
            progressBar.setVisibility(View.GONE);
            upd_but.setVisibility(VISIBLE);
            listView.post(() -> scrollPagination.update());

            stopRepeatingTask();
        } else {
            listView.setVisibility(View.GONE);
            scrollPagination.update();
        }
    }


    private String takeData(String text) {

        LocalDateTime dateTime = null;
        // Регулярное выражение для поиска даты и времени
        String regex = "(\\d{2}\\.\\d{2}\\.\\d{4}) (\\d{2}:\\d{2})";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String dateTimeString = matcher.group(1) + " " + matcher.group(2); // "10.02.2025 14:10"

            // Парсим строку в LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            dateTime = LocalDateTime.parse(dateTimeString, formatter);
        }
        assert dateTime != null;
        return dateTime.toString();
    }


    @Override
    public void onResume() {
        super.onResume();
        ExecutionStatusViewModel.clearActiveOrderNoticeSuppress();
        if (email != null && context != null && isAdded()) {
            fetchRoutesCancel(email);
        }
    }
     @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                String str;
                do {
                    str = "";
                    for (String cn : c.getColumnNames()) {
                        str = str.concat(cn + " = " + CursorReadHelper.getString(c, cn) + "; ");
                        list.add(CursorReadHelper.getString(c, cn));

                    }

                } while (c.moveToNext());
            }
        }
        database.close();
        return list;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        stopRepeatingTask();
    }



    public void startRepeatingTask() {
        handler = new Handler(Looper.getMainLooper());
        final int intervalMillis = 5000;
        final int[] runCount = {0};

        taskRunnable = new Runnable() {
            @Override
            public void run() {
                fetchRoutesCancel(email);
                Logger.d(context, TAG, "Выполнение задачи #" + (runCount[0] + 1));

                runCount[0]++;
                handler.postDelayed(this, intervalMillis); // бесконечный повтор
            }
        };

        handler.post(taskRunnable); // запускаем сразу
    }

    public void stopRepeatingTask() {
        if (handler != null && taskRunnable != null) {
            handler.removeCallbacks(taskRunnable);
            Logger.d(context, TAG, "Задача остановлена вручную");
        }
    }

}