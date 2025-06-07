package com.taxi.easy.ua.ui.active_order;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentCancelBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;
import com.taxi.easy.ua.utils.connect.NetworkChangeReceiver;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;
import com.uxcam.UXCam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CancelFragment extends Fragment {

    private static final String TAG = "CancelFragment";
    private @NonNull FragmentCancelBinding binding = null;
    private ListView listView;
    private String[] array;

    private NetworkChangeReceiver networkChangeReceiver;
    ProgressBar progressBar;

    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;

    private List<RouteResponseCancel> routeList;

    AppCompatButton upd_but;
    private ImageButton scrollButtonDown, scrollButtonUp;
    private TextView textUid;
    private String email;
    private FragmentManager fragmentManager;
    Context context;
    private final int desiredHeight = 1200;
    public static AppCompatButton btnCallAdmin;
    private Handler handler;
    private Runnable taskRunnable;

    public CancelFragment() {
    }

    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

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
        networkChangeReceiver = new NetworkChangeReceiver();

        email = logCursor(MainActivity.TABLE_USER_INFO, Objects.requireNonNull(requireActivity())).get(3);

        databaseHelper = new DatabaseHelper(getContext());
        databaseHelperUid = new DatabaseHelperUid(getContext());

        btnCallAdmin = binding.btnCallAdmin;
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, requireActivity()).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

        textUid  = binding.textUid;
        upd_but = binding.updBut;
        upd_but.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
            }
        });
        scrollButtonUp = binding.scrollButtonUp;
        scrollButtonDown = binding.scrollButtonDown;
        scrollButtonDown.setOnClickListener(v -> {
            // Определяем следующую позицию для прокрутки
            int nextVisiblePosition = listView.getLastVisiblePosition() + 1;

            // Проверяем, чтобы не прокручивать за пределы списка
            if (nextVisiblePosition < array.length) {
                // Плавно прокручиваем к следующей позиции
                listView.smoothScrollToPosition(nextVisiblePosition);
            }
        });

        scrollButtonUp.setOnClickListener(v -> {
            int offset = -1; // или другое значение, чтобы указать направление прокрутки
            listView.smoothScrollByOffset(offset);
        });

        startRepeatingTask();
        return root;
    }

    private void fetchRoutesCancel(String value) {
        progressBar.setVisibility(VISIBLE);
        listView.setVisibility(View.GONE);
        scrollButtonDown.setVisibility(View.GONE);
        scrollButtonUp.setVisibility(View.GONE);

        databaseHelper.clearTableCancel();
        databaseHelperUid.clearTableCancel();

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
                        textUid.setVisibility(VISIBLE);
                        textUid.setText(R.string.no_routs);
                    }
                } else {
                    MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());

                }
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                });
            }

            public void onFailure(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                FirebaseCrashlytics.getInstance().recordException(t);
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                });
            }


        });
        if (isAdded()) {
            upd_but.setText(context.getString(R.string.order));
            progressBar.setVisibility(View.GONE);
        }
    }

    private void processCancelList() {
        // В этом методе вы можете использовать routeList для выполнения дополнительных действий с данными.

        // Создайте массив строк
        array = new String[routeList.size()];

        String closeReasonText = context.getString(R.string.close_resone_def);

        for (int i = 0; i < routeList.size(); i++) {
            RouteResponseCancel route = routeList.get(i);
            String uid = route.getUid();
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

            switch (closeReason) {
                case "101":
                case "-1":
                    closeReasonText = context.getString(R.string.close_resone_in_work);
                    break;
                case "102":
                    closeReasonText = context.getString(R.string.close_resone_in_start_point);
                    break;
                case "103":
                    closeReasonText = context.getString(R.string.close_resone_in_rout);
                    break;
                case "104":
                case "8":
                    closeReasonText = context.getString(R.string.close_resone_8);
                    break;
                case "0":
                    closeReasonText = context.getString(R.string.close_resone_0);
                    break;
                case "1":
                    closeReasonText = context.getString(R.string.close_resone_1);
                    break;
                case "2":
                    closeReasonText = context.getString(R.string.close_resone_2);
                    break;
                case "3":
                    closeReasonText = context.getString(R.string.close_resone_3);
                    break;
                case "4":
                    closeReasonText = context.getString(R.string.close_resone_4);
                    break;
                case "5":
                    closeReasonText = context.getString(R.string.close_resone_5);
                    break;
                case "6":
                    closeReasonText = context.getString(R.string.close_resone_6);
                    break;
                case "7":
                    closeReasonText = context.getString(R.string.close_resone_7);
                    break;
                case "9":
                    closeReasonText = context.getString(R.string.close_resone_9);
                    break;
                default:
                    // ничего не меняем или задаём значение по умолчанию
                    break;
            }


            if(routeFrom.equals("Місце відправлення")) {
                routeFrom = context.getString(R.string.start_point_text);
            }


            if(routeTo.equals("Точка на карте")) {
                routeTo = context.getString(R.string.end_point_marker);
            }
            if(routeTo.contains("по городу")) {
                routeTo = context.getString(R.string.on_city);
            }
            if(routeTo.contains("по місту")) {
                routeTo = context.getString(R.string.on_city);
            }
            String routeInfo = "";

            if(auto == null) {
                auto = "??";
            }

            String required_time_text = requireActivity().getString(R.string.ex_st_5) + required_time;

            if(required_time.contains("01.01.1970")) {
                required_time_text = "";
            }
            if (routeFrom.equals(routeTo)) {
                routeInfo = routeFrom + ", " + routeFromNumber
                        + getString(R.string.close_resone_to)
                        + getString(R.string.on_city)
                        + required_time_text  + "#"
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)  + "#"
                        + getString(R.string.auto_info) + " " + auto + "#"
                        + getString(R.string.close_resone_time)
                        + createdAt  + "#"
                        + getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = routeFrom + ", " + routeFromNumber
                        + getString(R.string.close_resone_to) + routeTo + " " + routeToNumber + "."
                        + required_time_text + "#"
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)  + "#"
                        + getString(R.string.auto_info) + " " + auto + "#"
                        + getString(R.string.close_resone_time) + createdAt  + "#"
                        + getString(R.string.close_resone_text) + closeReasonText;
            }

//                array[i] = routeInfo;
            databaseHelper.addRouteCancel(uid, routeInfo);
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
        array = databaseHelper.readRouteCancel();
        Logger.d(context, TAG, "processRouteList: array " + Arrays.toString(array));

        if(array != null) {
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
            scrollButtonDown.setVisibility(VISIBLE);
            scrollButtonUp.setVisibility(VISIBLE);
            progressBar.setVisibility(View.GONE);
            upd_but.setVisibility(VISIBLE);

            listView.setAdapter(adapter);


            ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
            layoutParams.height = desiredHeight;
            listView.setLayoutParams(layoutParams);

            listView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                int totalItemHeight = 0;
                for (int i = 0; i < listView.getChildCount(); i++) {
                    totalItemHeight += listView.getChildAt(i).getHeight();
                }

                if (totalItemHeight > desiredHeight) {
                    scrollButtonUp.setVisibility(VISIBLE);
                    scrollButtonDown.setVisibility(VISIBLE);
                } else {
                    scrollButtonUp.setVisibility(View.GONE);
                    scrollButtonDown.setVisibility(View.GONE);
                }

                // Убираем слушатель, чтобы он не срабатывал многократно
//                    listView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            });


        } else {
            listView.setVisibility(View.GONE);
            scrollButtonDown.setVisibility(View.GONE);
            scrollButtonUp.setVisibility(View.GONE);
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
                        str = str.concat(cn + " = " + c.getString(c.getColumnIndex(cn)) + "; ");
                        list.add(c.getString(c.getColumnIndex(cn)));

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