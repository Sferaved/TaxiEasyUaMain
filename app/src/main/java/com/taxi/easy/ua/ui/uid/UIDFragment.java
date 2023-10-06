package com.taxi.easy.ua.ui.uid;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.R.string.verify_internet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.NetworkChangeReceiver;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentUidBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponse;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;

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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUidBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        listView = binding.listView;
        progressBar = binding.progressBar;
        networkChangeReceiver = new NetworkChangeReceiver();

        if(connected()) {
            @SuppressLint("UseRequireInsteadOfGet") String email = logCursor(MainActivity.TABLE_USER_INFO, Objects.requireNonNull(getActivity())).get(3);
            fetchRoutes(email);
        }
        return root;
    }
    private boolean connected() {

        Boolean hasConnect = false;

        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(
                CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected()) {
            hasConnect = true;
        }
        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null && mobileNetwork.isConnected()) {
            hasConnect = true;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected()) {
            hasConnect = true;
        }

        if (!hasConnect) {
            Toast.makeText(getActivity(), verify_internet, Toast.LENGTH_LONG).show();
        }
        Log.d("TAG", "connected: " + hasConnect);
        return hasConnect;
    }

    String baseUrl = "https://m.easy-order-taxi.site";
    private List<RouteResponse> routeList = new ArrayList<>();

    private void fetchRoutes(String value) {
        String url = baseUrl + "/android/UIDStatusShowEmail/" + value;
        Call<List<RouteResponse>> call = ApiClient.getApiService().getRoutes(url);
        Log.d("TAG", "fetchRoutes: " + url);
        call.enqueue(new Callback<List<RouteResponse>>() {
            @Override
            public void onResponse(Call<List<RouteResponse>> call, Response<List<RouteResponse>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    List<RouteResponse> routes = response.body();

                    if (routes != null && !routes.isEmpty()) {
                        routeList.addAll(routes);
                        processRouteList();
                    } else {
                        binding.textUid.setVisibility(View.VISIBLE);
                        binding.textUid.setText(R.string.no_routs);
                    }
                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
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

                String routeInfo = getString(R.string.close_resone_from) + routeFrom + " " + routefromnumber
                        + getString(R.string.close_resone_to) + routeTo + " " + routeTonumber
                        + getString(R.string.close_resone_cost) + webCost + " " + getString(R.string.UAH)
                        + getString(R.string.close_resone_time)
                        + createdAt + getString(R.string.close_resone_text) + closeReasonText;
                array[i] = routeInfo;

        }
        Log.d("TAG", "processRouteList: array " + Arrays.toString(array));
        if(array != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.drop_down_layout, array);
            listView.setAdapter(adapter);

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