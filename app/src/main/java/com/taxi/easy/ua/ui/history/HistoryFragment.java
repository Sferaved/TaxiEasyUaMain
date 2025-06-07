package com.taxi.easy.ua.ui.history;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.MainActivity.supportEmail;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentHistoryBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponse;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.log.Logger;
import com.uxcam.UXCam;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class HistoryFragment extends Fragment {

    private static final String TAG = "UIDFragment";
    private FragmentHistoryBinding binding;
    private ListView listView;
    private String[] array;

    ProgressBar progressBar;
    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;
    String baseUrl;

    private List<RouteResponse> routeList;

    AppCompatButton upd_but;
    private ImageButton scrollButtonDown, scrollButtonUp;
    private TextView textUid;

    private int desiredHeight;
    Context context;
    View root;

    public HistoryFragment() {
    }

    @SuppressLint({"SourceLockedOrientationActivity", "IntentReset"})
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        binding = FragmentHistoryBinding.inflate(inflater, container, false);

        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        }


        root = binding.getRoot();

        context = requireActivity();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        listView = binding.listView;
        progressBar = binding.progressBar;


        databaseHelper = new DatabaseHelper(context);
        databaseHelperUid = new DatabaseHelperUid(context);

        array = databaseHelper.readRouteInfo();

        textUid  = binding.textUid;
        upd_but = binding.updBut;
        upd_but.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
            }

        });
        scrollButtonUp = binding.scrollButtonUp;
        scrollButtonDown = binding.scrollButtonDown;


        progressBar.setVisibility(View.VISIBLE);
        scrollButtonDown.setVisibility(View.GONE);
        scrollButtonUp.setVisibility(View.GONE);
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
            // Определяем следующую позицию для прокрутки
            int nextVisiblePosition = listView.getFirstVisiblePosition() - 1;

            // Проверяем, чтобы не прокручивать за пределы списка
            if (nextVisiblePosition >= 0) {
                // Плавно прокручиваем к предыдущей позиции
                listView.smoothScrollToPosition(nextVisiblePosition);
            }
        });

        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        } else {
            fetchRoutes();
        }


        routeList();

        registerForContextMenu(listView);

        AppCompatButton btnCallAdmin = binding.btnCallAdmin;
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, context).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

        AppCompatButton text_uid = binding.textUid;
        text_uid.setOnClickListener(v -> {
            sendEmailAdmin();
        });
        sharedPreferencesHelperMain.saveValue("carFound", false);
        return root;
    }

    @SuppressLint("IntentReset")
    private void sendEmailAdmin () {
        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String city;
        switch (stringList.get(1)){
            case "Dnipropetrovsk Oblast":
                city = getString(R.string.Dnipro_city);
                break;
            case "Zaporizhzhia":
                city = getString(R.string.Zaporizhzhia);
                break;
            case "Cherkasy Oblast":
                city = getString(R.string.Cherkasy);
                break;
            case "Odessa":
                city = getString(R.string.Odessa);
                break;
            case "OdessaTest":
                city = getString(R.string.OdessaTest);
                break;
            default:
                city = getString(R.string.Kyiv_city);
                break;
        }


        List<String> userList = logCursor(MainActivity.TABLE_USER_INFO, context);

        String subject = getString(R.string.SA_subject) + generateRandomString();

        String body = getString(R.string.SA_message_history) + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" + "\n" +
                getString(R.string.SA_info_pas) + "\n" +
                getString(R.string.SA_info_city) + " " + city + "\n" +
                getString(R.string.SA_pas_text) + " " + getString(R.string.version) + "\n" +
                getString(R.string.SA_user_text) + " " + userList.get(4) + "\n" +
                getString(R.string.SA_email) + " " + userList.get(3) + "\n" +
                getString(R.string.SA_phone_text) + " " + userList.get(2) + "\n" + "\n";

        String[] CC = {"cartaxi4@gmail.com"};
        String[] TO = {supportEmail};

        File logFile = new File(requireActivity().getExternalFilesDir(null), "app_log.txt");

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("message/rfc822");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        if (logFile.exists()) {
            Uri uri = FileProvider.getUriForFile(requireActivity(), requireActivity().getPackageName() + ".fileprovider", logFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            Logger.e(requireActivity(), "MainActivity", "Log file does not exist");
        }
        try {
            startActivity(Intent.createChooser(emailIntent, subject));
        } catch (android.content.ActivityNotFoundException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }


    }

    private String generateRandomString() {
        String characters = "012345678901234567890123456789";
        StringBuilder randomString = new StringBuilder();

        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(characters.length());
            char randomChar = characters.charAt(randomIndex);
            randomString.append(randomChar);
        }

        return randomString.toString();
    }

    private void fetchRoutes() {

        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

        routeList = new ArrayList<>();

        databaseHelper.clearTable();
        databaseHelperUid.clearTableUid();

        progressBar.setVisibility(View.VISIBLE);

        listView.setVisibility(View.GONE);
        routeList = new ArrayList<>();
        upd_but.setOnClickListener(v -> MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_visicom, true)
                .build()));
        List<String> stringList = logCursor(MainActivity.CITY_INFO,context);
        String city = stringList.get(1);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        String url = baseUrl + "/android/UIDStatusShowEmailCityApp/" + email + "/" + city + "/" +  context.getString(R.string.application);
        Call<List<RouteResponse>> call = ApiClient.getApiService().getRoutes(url);
        Logger.d (context, TAG, "fetchRoutes: " + url);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<RouteResponse>> call, @NonNull Response<List<RouteResponse>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<RouteResponse> routes = response.body();
                    Logger.d(context, TAG, "onResponse: " + routes);
                    if (!routes.isEmpty()) {
                        boolean hasRouteWithAsterisk = false;
                        for (RouteResponse route : routes) {
                            if ("*".equals(route.getRouteFrom())) {
                                // Найден объект с routefrom = "*"
                                hasRouteWithAsterisk = true;
                                break;  // Выход из цикла, так как условие уже выполнено
                            }
                        }
                        if (!hasRouteWithAsterisk) {
                            routeList.addAll(routes);
                            processRouteList();
                            textUid.setVisibility(View.VISIBLE);
                            textUid.setText(R.string.uid_menu);
                        } else {
                            textUid.setVisibility(View.VISIBLE);
                            textUid.setText(R.string.no_routs);
                        }

                    } else {
                        textUid.setVisibility(View.VISIBLE);
                        textUid.setText(R.string.no_routs);
                    }
                } else {
                    if (isAdded()) {
                        MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_restart, true)
                                .build());
                    }

                }
            }

            public void onFailure(@NonNull Call<List<RouteResponse>> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                FirebaseCrashlytics.getInstance().recordException(t);
            }


        });
        if (isAdded()) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void processRouteList() {
        // В этом методе вы можете использовать routeList для выполнения дополнительных действий с данными.

        // Создайте массив строк
        array = new String[routeList.size()];

        String closeReasonText = context.getString(R.string.close_resone_def);

        for (int i = 0; i < routeList.size(); i++) {
            RouteResponse route = routeList.get(i);
            String routeFrom = route.getRouteFrom();
            String routefromnumber = route.getRouteFromNumber();
            String routeTo = route.getRouteTo();
            String routeTonumber = route.getRouteToNumber();
            String webCost = route.getWebCost();
            String createdAt = route.getCreatedAt();
            String closeReason = route.getCloseReason();
            String auto = route.getAuto();
            String startLat = route.getStartLat();
            String startLan = route.getStartLan();
            String to_lat = route.getTo_lat();
            String to_lng = route.getTo_lng();

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
                    // оставляем старое значение
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
            String routeInfo;

            if(auto == null) {
                auto = "??";
            }

            if(routeFrom.equals(routeTo)) {
                routeInfo = routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to)
                        + context.getString(R.string.on_city)  + "#"
                        + context.getString(R.string.close_resone_cost) + webCost + " " + context.getString(R.string.UAH)  + "#"
                        + context.getString(R.string.auto_info) + " " + auto + "#"
                        + context.getString(R.string.close_resone_time) + createdAt + "#"
                        + context.getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to) + routeTo + " " + routeTonumber  + "#"
                        + context.getString(R.string.close_resone_cost) + webCost + " " + context.getString(R.string.UAH)  + "#"
                        + context.getString(R.string.auto_info) + " " + auto + "#"
                        + context.getString(R.string.close_resone_time) + createdAt + "#"
                        + context.getString(R.string.close_resone_text) + closeReasonText;
            }

//                array[i] = routeInfo;
            databaseHelper.addRouteInfo(routeInfo);
            List<String> settings = new ArrayList<>();

            settings.add(startLat);
            settings.add(startLan);
            settings.add(to_lat);
            settings.add(to_lng);
            settings.add(routeFrom + " " + routefromnumber);
            settings.add(routeTo + " " + routeTonumber);
            Logger.d(context, TAG, settings.toString());
            databaseHelperUid.addRouteInfoUid(settings);
            Logger.d(context, TAG, settings.toString());
        }
        array = databaseHelper.readRouteInfo();
        Logger.d (context, TAG, "processRouteList: array " + Arrays.toString(array));
        if(array != null) {
            List<String> itemList = Arrays.asList(array); // Преобразование в List

            CustomArrayUidAdapter adapter = new CustomArrayUidAdapter(
                    context,
                    R.layout.drop_down_layout_uid,  // Ваш макет элемента списка
                    R.id.text1,  // ID TextView в вашем макете
                    R.id.text2,  // ID TextView в вашем макете
                    R.id.text3,  // ID TextView в вашем макете
                    R.id.text4,  // ID TextView в вашем макете
                    R.id.text5,  // ID TextView в вашем макете
                    itemList  // Список строк
            );
            listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    // Теперь мы можем получить высоту фрагмента
                    desiredHeight = root.getHeight()/2;
                    ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
                    layoutParams.height = desiredHeight;
                    listView.setLayoutParams(layoutParams);

                    int totalItemHeight = 0;
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        totalItemHeight += listView.getChildAt(i).getHeight();
                    }
                    Log.d(TAG, "totalItemHeight: " + totalItemHeight);
                    Log.d(TAG, "desiredHeight: " + desiredHeight);
                    if (totalItemHeight > desiredHeight) {
                        scrollButtonUp.setVisibility(View.VISIBLE);
                        scrollButtonDown.setVisibility(View.VISIBLE);
                    } else {
                        scrollButtonUp.setVisibility(View.GONE);
                        scrollButtonDown.setVisibility(View.GONE);
                    }
                }
            });



            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            upd_but.setVisibility(View.VISIBLE);

            listView.setAdapter(adapter);


            ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
            layoutParams.height = desiredHeight;
            listView.setLayoutParams(layoutParams);
//            registerForContextMenu(listView);

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
                // Определяем следующую позицию для прокрутки
                int nextVisiblePosition = listView.getFirstVisiblePosition() - 1;

                // Проверяем, чтобы не прокручивать за пределы списка
                if (nextVisiblePosition >= 0) {
                    // Плавно прокручиваем к предыдущей позиции
                    listView.smoothScrollToPosition(nextVisiblePosition);
                }
            });
        }
    }

    private void routeList() {

        array = databaseHelper.readRouteInfo();
        Logger.d (context, TAG, "processRouteList: array " + Arrays.toString(array));
        if(array != null) {
            List<String> itemList = Arrays.asList(array); // Преобразование в List

            CustomArrayUidAdapter adapter = new CustomArrayUidAdapter(
                    context,
                    R.layout.drop_down_layout_uid,  // Ваш макет элемента списка
                    R.id.text1,  // ID TextView в вашем макете
                    R.id.text2,  // ID TextView в вашем макете
                    R.id.text3,  // ID TextView в вашем макете
                    R.id.text4,  // ID TextView в вашем макете
                    R.id.text5,  // ID TextView в вашем макете
                    itemList  // Список строк
            );
            listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    // Теперь мы можем получить высоту фрагмента
                    desiredHeight = root.getHeight()/2;
                    ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
                    layoutParams.height = desiredHeight;
                    listView.setLayoutParams(layoutParams);

                    int totalItemHeight = 0;
                    for (int i = 0; i < listView.getChildCount(); i++) {
                        totalItemHeight += listView.getChildAt(i).getHeight();
                    }
                    Log.d(TAG, "totalItemHeight: " + totalItemHeight);
                    Log.d(TAG, "desiredHeight: " + desiredHeight);
                    if (totalItemHeight > desiredHeight) {
                        scrollButtonUp.setVisibility(View.VISIBLE);
                        scrollButtonDown.setVisibility(View.VISIBLE);
                    } else {
                        scrollButtonUp.setVisibility(View.GONE);
                        scrollButtonDown.setVisibility(View.GONE);
                    }
                }
            });



            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            upd_but.setVisibility(View.VISIBLE);

            listView.setAdapter(adapter);


            ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
            layoutParams.height = desiredHeight;
            listView.setLayoutParams(layoutParams);
//            registerForContextMenu(listView);

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
                // Определяем следующую позицию для прокрутки
                int nextVisiblePosition = listView.getFirstVisiblePosition() - 1;

                // Проверяем, чтобы не прокручивать за пределы списка
                if (nextVisiblePosition >= 0) {
                    // Плавно прокручиваем к предыдущей позиции
                    listView.smoothScrollToPosition(nextVisiblePosition);
                }
            });
        }
    }

     @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = database.query(table, null, null, null, null, null, null);
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
         database.close();
        return list;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}