package com.taxi.easy.ua.ui.uid;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.R.string.verify_internet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.NetworkChangeReceiver;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentUidBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponse;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.db.DatabaseHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UIDFragment extends Fragment {

    private @NonNull FragmentUidBinding binding;
    private ListView listView;
    private String[] array;
    private static TextView textView;
    private NetworkChangeReceiver networkChangeReceiver;
    ProgressBar progressBar;
    NavController navController;
    DatabaseHelper databaseHelper;
    String baseUrl = "https://m.easy-order-taxi.site";
    private List<RouteResponse> routeList;

    AppCompatButton upd_but;
    private ImageButton scrollButtonDown, scrollButtonUp;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        binding = FragmentUidBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        listView = binding.listView;
        progressBar = binding.progressBar;
        networkChangeReceiver = new NetworkChangeReceiver();

        @SuppressLint("UseRequireInsteadOfGet") String email = logCursor(MainActivity.TABLE_USER_INFO, Objects.requireNonNull(requireActivity())).get(3);
        routeList = new ArrayList<>();
        databaseHelper = new DatabaseHelper(getContext());
        array = databaseHelper.readRouteInfo();

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
            fetchRoutes(email);
            scrollButtonDown.setVisibility(View.INVISIBLE);
            scrollButtonUp.setVisibility(View.INVISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            upd_but.setVisibility(View.VISIBLE);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.drop_down_layout, array);
            listView.setAdapter(adapter);
            scrollButtonDown.setVisibility(View.VISIBLE);
            scrollButtonUp.setVisibility(View.VISIBLE);
        }
        scrollButtonUp = binding.scrollButtonUp;
        scrollButtonDown = binding.scrollButtonDown;
        int desiredHeight = 1200; // Ваше желаемое значение высоты в пикселях
        ViewGroup.LayoutParams layoutParams = listView.getLayoutParams();
        layoutParams.height = desiredHeight;
        listView.setLayoutParams(layoutParams);

        return root;
    }



    private void fetchRoutes(String value) {
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
                            binding.textUid.setVisibility(View.VISIBLE);
                            binding.textUid.setText(R.string.no_routs);
                        }

                    } else {
                        binding.textUid.setVisibility(View.VISIBLE);
                        binding.textUid.setText(R.string.no_routs);
                    }
                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<RouteResponse>> call, Throwable t) {
                // Обработка ошибок сети или других ошибок
                String errorMessage = t.getMessage();
                t.printStackTrace();
                // Дополнительная обработка ошибки
            }
        });
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
            String routeInfo = "";

            if(auto != null) {
                routeInfo = getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        + getString(R.string.close_resone_to) + routeTo + " " + routeTonumber
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)
                        + getString(R.string.auto_info) + " " + auto + " "
                        + getString(R.string.close_resone_time)
                        + createdAt + getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        + getString(R.string.close_resone_to) + routeTo + " " + routeTonumber
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)
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