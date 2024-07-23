package com.taxi.easy.ua.ui.uid;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.NetworkChangeReceiver;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentUidBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponse;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.db.RouteInfo;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class UIDFragment extends Fragment {

    private static final String TAG = "UIDFragment";
    private @NonNull FragmentUidBinding binding;
    private ListView listView;
    private String[] array;
    private RouteInfo routeInfo;
    private static TextView textView;
    private NetworkChangeReceiver networkChangeReceiver;
    ProgressBar progressBar;
    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;
    String baseUrl = "https://m.easy-order-taxi.site";
    private List<RouteResponse> routeList;

    AppCompatButton upd_but;
    private ImageButton scrollButtonDown, scrollButtonUp;
    private TextView textUid;
    private String email;
    private FragmentManager fragmentManager;
    private int desiredHeight;
    Context context;
    
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUidBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fragmentManager = getParentFragmentManager();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        context = requireActivity();
        listView = binding.listView;
        progressBar = binding.progressBar;
        networkChangeReceiver = new NetworkChangeReceiver();

        databaseHelper = new DatabaseHelper(context);
        databaseHelperUid = new DatabaseHelperUid(context);

        array = databaseHelper.readRouteInfo();

        textUid  = binding.textUid;
        upd_but = binding.updBut;
        upd_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    MainActivity.navController.popBackStack();
                    MainActivity.navController.navigate(R.id.nav_visicom);
                }

            }
        });
        scrollButtonUp = binding.scrollButtonUp;
        scrollButtonDown = binding.scrollButtonDown;

        FloatingActionButton fab_call = binding.fabCall;
        fab_call.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
            String phone = stringList.get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

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


        fetchRoutes();
        registerForContextMenu(listView);
        listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // Теперь мы можем получить высоту фрагмента
                desiredHeight = root.getHeight() - 350;
                ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
                layoutParams.height = desiredHeight;
                listView.setLayoutParams(layoutParams);

                int totalItemHeight = 0;
                for (int i = 0; i < listView.getChildCount(); i++) {
                    totalItemHeight += listView.getChildAt(i).getHeight();
                }

                if (totalItemHeight > desiredHeight) {
                    scrollButtonUp.setVisibility(View.VISIBLE);
                    scrollButtonDown.setVisibility(View.VISIBLE);
                } else {
                    scrollButtonUp.setVisibility(View.GONE);
                    scrollButtonDown.setVisibility(View.GONE);
                }
            }
        });
        return root;
    }


    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = requireActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        assert info != null;
        int position = info.position;

        if (item.getItemId() == R.id.action_order) {

            // Обработка действия "Edit"
//            Toast.makeText(requireActivity(), "Edit: " + array[position], Toast.LENGTH_SHORT).show();
//            Log.d(TAG, "onContextItemSelected: " + position);
            Log.d(TAG, "onContextItemSelected: " + array[position]);

            routeInfo = databaseHelperUid.getRouteInfoById(position+1);
            if (routeInfo != null) {
                Log.d(TAG, "onContextItemSelected: " + routeInfo);
            } else {
                Log.d(TAG, "onContextItemSelected: RouteInfo not found for id: " + (position + 1));
            }
            List<String> settings = new ArrayList<>();
            settings.add(routeInfo.getStartLat());
            settings.add(routeInfo.getStartLan());
            settings.add(routeInfo.getToLat());
            settings.add(routeInfo.getToLng());
            settings.add(routeInfo.getStart());
            settings.add(routeInfo.getFinish());

            updateRoutMarker(settings);
            MainActivity.navController.popBackStack();
            MainActivity.navController.navigate(R.id.nav_visicom);
            MainActivity.gps_upd = false;
            return true;
        } else if (item.getItemId() == R.id.action_exit) {
// Обработка действия "Delete"

            return true;
        }   else {
            return super.onContextItemSelected(item);
        }

    }
    private void updateRoutMarker(List<String> settings) {
        Log.d(TAG, "updateRoutMarker: " + settings.toString());
        ContentValues cv = new ContentValues();

        cv.put("startLat", Double.parseDouble(settings.get(0)));
        cv.put("startLan", Double.parseDouble(settings.get(1)));
        cv.put("to_lat", Double.parseDouble(settings.get(2)));
        cv.put("to_lng", Double.parseDouble(settings.get(3)));
        cv.put("start", settings.get(4));
        cv.put("finish", settings.get(5));
        if(isAdded()) {
            // обновляем по id
            SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            database.update(MainActivity.ROUT_MARKER, cv, "id = ?",
                    new String[]{"1"});
            database.close();
        }
    }

    private void fetchRoutes() {

        String email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

        routeList = new ArrayList<>();

        databaseHelper.clearTable();
        databaseHelperUid.clearTableUid();

        progressBar.setVisibility(View.VISIBLE);

        listView.setVisibility(View.GONE);
        routeList = new ArrayList<>();
        upd_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.navController.popBackStack();
                MainActivity.navController.navigate(R.id.nav_visicom);
            }
        });
        List<String> stringList = logCursor(MainActivity.CITY_INFO,requireActivity());
        String city = stringList.get(1);
        String url = baseUrl + "/android/UIDStatusShowEmailCityApp/" + email + "/" + city + "/" +  context.getString(R.string.application);
        Call<List<RouteResponse>> call = ApiClient.getApiService().getRoutes(url);
        Logger.d (getActivity(), TAG, "fetchRoutes: " + url);

        call.enqueue(new Callback<List<RouteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RouteResponse>> call, @NonNull Response<List<RouteResponse>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    List<RouteResponse> routes = response.body();
                    Logger.d (getActivity(), TAG, "onResponse: " + routes);
                    if (routes != null && !routes.isEmpty()) {
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
                        }  else {
                            textUid.setVisibility(View.VISIBLE);
                            textUid.setText(R.string.no_routs);
                        }

                    } else {
                        textUid.setVisibility(View.VISIBLE);
                        textUid.setText(R.string.no_routs);
                    }
                } else {
                    if (isAdded()) {
                        MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                        bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
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

            switch (closeReason){
                case "-1":
                    closeReasonText = context.getString(R.string.close_resone_in_work);
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
                case "8":
                    closeReasonText = context.getString(R.string.close_resone_8);
                    break;
                case "9":
                    closeReasonText = context.getString(R.string.close_resone_9);
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

            if(routeFrom.equals(routeTo)) {
                routeInfo = context.getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to)
                        + context.getString(R.string.on_city)
                        + context.getString(R.string.close_resone_cost) + webCost + " " + context.getString(R.string.UAH)
                        + context.getString(R.string.auto_info) + " " + auto + " "
                        + context.getString(R.string.close_resone_time)
                        + createdAt + context.getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = context.getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to) + routeTo + " " + routeTonumber
                        + context.getString(R.string.close_resone_cost) + webCost + " " + context.getString(R.string.UAH)
                        + context.getString(R.string.auto_info) + " " + auto + " "
                        + context.getString(R.string.close_resone_time)
                        + createdAt + context.getString(R.string.close_resone_text) + closeReasonText;
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
            Logger.d(getActivity(), TAG, settings.toString());
            databaseHelperUid.addRouteInfoUid(settings);
            Logger.d(getActivity(), TAG, settings.toString());
        }
        array = databaseHelper.readRouteInfo();
        Logger.d (getActivity(), TAG, "processRouteList: array " + Arrays.toString(array));
        if(array != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.drop_down_layout, array);
            listView.setAdapter(adapter);
            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            upd_but.setVisibility(View.VISIBLE);

            listView.setAdapter(adapter);


            ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
            layoutParams.height = desiredHeight;
            listView.setLayoutParams(layoutParams);
            registerForContextMenu(listView);

            scrollButtonDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Определяем следующую позицию для прокрутки
                    int nextVisiblePosition = listView.getLastVisiblePosition() + 1;

                    // Проверяем, чтобы не прокручивать за пределы списка
                    if (nextVisiblePosition < array.length) {
                        // Плавно прокручиваем к следующей позиции
                        listView.smoothScrollToPosition(nextVisiblePosition);
                    }
                }
            });

            scrollButtonUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Определяем следующую позицию для прокрутки
                    int nextVisiblePosition = listView.getFirstVisiblePosition() - 1;

                    // Проверяем, чтобы не прокручивать за пределы списка
                    if (nextVisiblePosition >= 0) {
                        // Плавно прокручиваем к предыдущей позиции
                        listView.smoothScrollToPosition(nextVisiblePosition);
                    }
                }
            });
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
    }
}