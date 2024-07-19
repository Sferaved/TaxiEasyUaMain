package com.taxi.easy.ua.ui.uid;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    NavController navController;
    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;
    String baseUrl = "https://m.easy-order-taxi.site";
    private List<RouteResponse> routeList;

    AppCompatButton upd_but;
    private ImageButton scrollButtonDown, scrollButtonUp;
    private TextView textUid;
    private String email;
    private FragmentManager fragmentManager;
    private final int desiredHeight = 1200;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        binding = FragmentUidBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fragmentManager = getParentFragmentManager();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        listView = binding.listView;
        progressBar = binding.progressBar;
        networkChangeReceiver = new NetworkChangeReceiver();

        email = logCursor(MainActivity.TABLE_USER_INFO, Objects.requireNonNull(requireActivity())).get(3);
        routeList = new ArrayList<>();
        databaseHelper = new DatabaseHelper(getContext());
        array = databaseHelper.readRouteInfo();

        databaseHelperUid = new DatabaseHelperUid(getContext());

        textUid  = binding.textUid;
        upd_but = binding.updBut;
        upd_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    navController.navigate(R.id.nav_visicom);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    fetchRoutes(email);

                }

            }
        });
        scrollButtonUp = binding.scrollButtonUp;
        scrollButtonDown = binding.scrollButtonDown;
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
                int offset = -1; // или другое значение, чтобы указать направление прокрутки
                listView.smoothScrollByOffset(offset);
            }
        });

        if (array == null || array.length == 0) {
            // Вызов метода fetchRoutes(email) только если массив пуст
            progressBar.setVisibility(View.VISIBLE);
//            fetchRoutes(email);
            progressBar.setVisibility(View.GONE);
            scrollButtonDown.setVisibility(View.INVISIBLE);
            scrollButtonUp.setVisibility(View.INVISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            upd_but.setVisibility(View.VISIBLE);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.drop_down_layout, array);
            listView.setAdapter(adapter);
            scrollButtonDown.setVisibility(View.VISIBLE);
            scrollButtonUp.setVisibility(View.VISIBLE);

            scrollButtonUp = binding.scrollButtonUp;
            scrollButtonDown = binding.scrollButtonDown;

            ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
            layoutParams.height = desiredHeight;
            listView.setLayoutParams(layoutParams);
            registerForContextMenu(listView);
            listView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
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

                    // Убираем слушатель, чтобы он не срабатывал многократно
//                    listView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
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

            navController.navigate(R.id.nav_visicom);
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

    private void fetchRoutes(String value) {

        upd_but.setText(getString(R.string.cancel_gps));
        upd_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.nav_visicom);
            }
        });

        String url = baseUrl + "/android/UIDStatusShowEmail/" + value;
        Call<List<RouteResponse>> call = ApiClient.getApiService().getRoutes(url);
        Log.d("TAG", "fetchRoutes: " + url);
        call.enqueue(new Callback<List<RouteResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<RouteResponse>> call, @NonNull Response<List<RouteResponse>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    List<RouteResponse> routes = response.body();
                    Log.d("TAG", "onResponse: " + routes);
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
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                }
            }

            public void onFailure(@NonNull Call<List<RouteResponse>> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                FirebaseCrashlytics.getInstance().recordException(t);
            }


        });
        if (isAdded()) {
            upd_but.setText(requireActivity().getString(R.string.order));
            progressBar.setVisibility(View.GONE);
        }
    }

    private void processRouteList() {
        // В этом методе вы можете использовать routeList для выполнения дополнительных действий с данными.

        // Создайте массив строк
        array = new String[routeList.size()];

        databaseHelper.clearTable();

        String closeReasonText = getString(R.string.close_resone_def);

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

            switch (closeReason){
                case "-1":
                    closeReasonText = getString(R.string.close_resone_in_work);
                    break;
                case "0":
                    closeReasonText = getString(R.string.close_resone_0);
                    break;
                case "1":
                    closeReasonText = getString(R.string.close_resone_1);
                    break;
                case "2":
                    closeReasonText = getString(R.string.close_resone_2);
                    break;
                case "3":
                    closeReasonText = getString(R.string.close_resone_3);
                    break;
                case "4":
                    closeReasonText = getString(R.string.close_resone_4);
                    break;
                case "5":
                    closeReasonText = getString(R.string.close_resone_5);
                    break;
                case "6":
                    closeReasonText = getString(R.string.close_resone_6);
                    break;
                case "7":
                    closeReasonText = getString(R.string.close_resone_7);
                    break;
                case "8":
                    closeReasonText = getString(R.string.close_resone_8);
                    break;
                case "9":
                    closeReasonText = getString(R.string.close_resone_9);
                    break;

            }

            if(routeFrom.equals("Місце відправлення")) {
                routeFrom = getString(R.string.start_point_text);
            }


            if(routeTo.equals("Точка на карте")) {
                routeTo = getString(R.string.end_point_marker);
            }
            if(routeTo.contains("по городу")) {
                routeTo = getString(R.string.on_city);
            }
            if(routeTo.contains("по місту")) {
                routeTo = getString(R.string.on_city);
            }
            String routeInfo = "";

            if(auto == null) {
                auto = "??";
            }

            if(routeFrom.equals(routeTo)) {
                routeInfo = getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        + getString(R.string.close_resone_to)
                        + getString(R.string.on_city)
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)
                        + getString(R.string.auto_info) + " " + auto + " "
                        + getString(R.string.close_resone_time)
                        + createdAt + getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        + getString(R.string.close_resone_to) + routeTo + " " + routeTonumber
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)
                        + getString(R.string.auto_info) + " " + auto + " "
                        + getString(R.string.close_resone_time)
                        + createdAt + getString(R.string.close_resone_text) + closeReasonText;
            }

//                array[i] = routeInfo;
                databaseHelper.addRouteInfo(routeInfo);

        }
        array = databaseHelper.readRouteInfo();
        Log.d("TAG", "processRouteList: array " + Arrays.toString(array));
        if(array != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.drop_down_layout, array);
            listView.setAdapter(adapter);
            scrollButtonDown.setVisibility(View.VISIBLE);
            scrollButtonUp.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            upd_but.setVisibility(View.VISIBLE);

            listView.setAdapter(adapter);

            int desiredHeight = 1200; // Ваше желаемое значение высоты в пикселях
            ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
            layoutParams.height = desiredHeight;
            listView.setLayoutParams(layoutParams);
            registerForContextMenu(listView);
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