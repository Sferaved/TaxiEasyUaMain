package com.taxi.easy.ua.ui.to_cancel;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.taxi.easy.ua.databinding.FragmentCancelBinding;
import com.taxi.easy.ua.databinding.FragmentUidBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.FinishActivity;
import com.taxi.easy.ua.ui.finish.RouteResponse;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.db.RouteInfo;
import com.taxi.easy.ua.utils.db.RouteInfoCancel;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.to_json_parser.JsonResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class CancelFragment extends Fragment {

    private static final String TAG = "CancelFragment";
    private @NonNull FragmentCancelBinding binding = null;
    private ListView listView;
    private String[] array;
    private String[] arrayUid;
    private RouteInfoCancel routeInfo;
    private NetworkChangeReceiver networkChangeReceiver;
    ProgressBar progressBar;
    NavController navController;
    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;
    String baseUrl = "https://m.easy-order-taxi.site";
    private List<RouteResponseCancel> routeList;

    AppCompatButton upd_but;
    private ImageButton scrollButtonDown, scrollButtonUp;
    private TextView textUid;
    private String email;
    private FragmentManager fragmentManager;
    Context context;
    public CancelFragment() {
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        binding = FragmentCancelBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context = requireActivity();
        
        Logger.d(context, TAG, "onContextItemSelected: ");
    
        fragmentManager = getParentFragmentManager();
        requireActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        listView = binding.listView;
        progressBar = binding.progressBar;
        networkChangeReceiver = new NetworkChangeReceiver();

        email = logCursor(MainActivity.TABLE_USER_INFO, Objects.requireNonNull(requireActivity())).get(3);

        databaseHelper = new DatabaseHelper(getContext());
        array = databaseHelper.readRouteCancel();


        databaseHelperUid = new DatabaseHelperUid(getContext());

        textUid  = binding.textUid;
        upd_but = binding.updBut;
        upd_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    navController.navigate(R.id.nav_visicom);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    fetchRoutesCancel(email);
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
        fetchRoutesCancel(email);

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
//            Logger.d(context, TAG, "onContextItemSelected: " + position);
            Logger.d(context, TAG, "onContextItemSelected: " + array[position]);

            routeInfo = databaseHelperUid.getCancelInfoById(position+1);
            if (routeInfo != null) {
                Logger.d(context, TAG, "onContextItemSelected: " + routeInfo);
            } else {
                Logger.d(context, TAG, "onContextItemSelected: RouteInfo not found for id: " + (position + 1));
            }

            Map<String, String> costMap = getStringStringMap();

            startFinishPage(costMap);

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

    private @NonNull Map<String, String> getStringStringMap() {
        Map<String, String> costMap = new HashMap<>();

        costMap.put("dispatching_order_uid", routeInfo.getDispatchingOrderUid());
        costMap.put("order_cost", routeInfo.getOrderCost());
        costMap.put("routefrom", routeInfo.getRouteFrom());
        costMap.put("routefromnumber", routeInfo.getRouteFromNumber());
        costMap.put("routeto", routeInfo.getRouteTo());
        costMap.put("to_number", routeInfo.getToNumber());

        if (routeInfo.getDispatchingOrderUidDouble() != null) {
            costMap.put("dispatching_order_uid_Double", routeInfo.getDispatchingOrderUidDouble());
        } else {
            costMap.put("dispatching_order_uid_Double", " ");
        }
        costMap.put("pay_method", routeInfo.getToPay_method());
        costMap.put("orderWeb", routeInfo.getOrderCost());
        return costMap;
    }

    private void startFinishPage(Map<String, String> sendUrlMap)
     {
        String to_name;
        if (Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
            to_name = getString(R.string.on_city_tv);
            Logger.d(context, TAG, "startFinishPage: to_name 1 " + to_name);

        } else {

            if(Objects.equals(sendUrlMap.get("routeto"), "Точка на карте")) {
                to_name = context.getString(R.string.end_point_marker);
            } else {
                to_name = sendUrlMap.get("routeto") + " " + sendUrlMap.get("to_number");
            }
            Logger.d(context, TAG, "startFinishPage: to_name 2 " + to_name);
        }
        Logger.d(context, TAG, "startFinishPage: to_name 3" + to_name);
        String to_name_local = to_name;
        if(to_name.contains("по місту")
                ||to_name.contains("по городу")
                || to_name.contains("around the city")
        ) {
            to_name_local = getString(R.string.on_city_tv);
        }
        Logger.d(context, TAG, "startFinishPage: to_name 4" + to_name_local);
        String pay_method_message = getString(R.string.pay_method_message_main);
        switch (Objects.requireNonNull(sendUrlMap.get("pay_method"))) {
            case "bonus_payment":
                pay_method_message += " " + getString(R.string.pay_method_message_bonus);
                break;
            case "card_payment":
            case "fondy_payment":
            case "mono_payment":
            case "wfp_payment":
                pay_method_message += " " + getString(R.string.pay_method_message_card);
                break;
            default:
                pay_method_message += " " + getString(R.string.pay_method_message_nal);
        }
        String messageResult = getString(R.string.thanks_message) +
                sendUrlMap.get("routefrom") + " " + getString(R.string.to_message) +
                to_name_local + "." +
                getString(R.string.call_of_order) + Objects.requireNonNull(sendUrlMap.get("orderWeb")) + getString(R.string.UAH) + " " + pay_method_message;
        String messageFondy = getString(R.string.fondy_message) + " " +
                sendUrlMap.get("routefrom") + " " + getString(R.string.to_message) +
                to_name_local + ".";
        Logger.d(context, TAG, "startFinishPage: messageResult " + messageResult);
        Logger.d(context, TAG, "startFinishPage: to_name " + to_name);
        Intent intent = new Intent(context, FinishActivity.class);
        intent.putExtra("messageResult_key", messageResult);
        intent.putExtra("messageFondy_key", messageFondy);
        intent.putExtra("messageCost_key", Objects.requireNonNull(sendUrlMap.get("orderWeb")));
        intent.putExtra("sendUrlMap", new HashMap<>(sendUrlMap));
        intent.putExtra("card_payment_key", "no");
        intent.putExtra("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
        startActivity(intent);
    }

  
    private void fetchRoutesCancel(String value) {
        listView.setVisibility(View.GONE);
        scrollButtonDown.setVisibility(View.GONE);
        scrollButtonUp.setVisibility(View.GONE);

        databaseHelper.clearTableCancel();
        databaseHelperUid.clearTableCancel();
        routeList = new ArrayList<>();

        upd_but.setText(getString(R.string.cancel_gps));
        upd_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.nav_visicom);
            }
        });

        String url = baseUrl + "/android/UIDStatusShowEmailCancel/" + value;
        Call<List<RouteResponseCancel>> call = ApiClient.getApiService().getRoutesCancel(url);
        Logger.d(context, TAG, "fetchRoutesCancel: " + url);
        call.enqueue(new Callback<List<RouteResponseCancel>>() {
            @Override
            public void onResponse(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Response<List<RouteResponseCancel>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    List<RouteResponseCancel> routes = response.body();
                    Logger.d(context, TAG, "onResponse: " + routes);
                    if (routes != null && !routes.isEmpty()) {
                        boolean hasRouteWithAsterisk = false;
                        for (RouteResponseCancel route : routes) {
                            if ("*".equals(route.getRouteFrom())) {
                                // Найден объект с routefrom = "*"
                                hasRouteWithAsterisk = true;
                                break;  // Выход из цикла, так как условие уже выполнено
                            }
                        }
                        if (!hasRouteWithAsterisk) {
                            routeList.addAll(routes);
                            processCancelList();
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

            public void onFailure(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                FirebaseCrashlytics.getInstance().recordException(t);
            }


        });
        if (isAdded()) {
            upd_but.setText(requireActivity().getString(R.string.order));
            progressBar.setVisibility(View.GONE);
        }
    }

    private void processCancelList() {
        // В этом методе вы можете использовать routeList для выполнения дополнительных действий с данными.

        // Создайте массив строк
        array = new String[routeList.size()];



        String closeReasonText = getString(R.string.close_resone_def);

        for (int i = 0; i < routeList.size(); i++) {
            RouteResponseCancel route = routeList.get(i);
            String uid = route.getUid();
            String routeFrom = route.getRouteFrom();
            String routefromnumber = route.getRouteFromNumber();
            String routeTo = route.getRouteTo();
            String routeTonumber = route.getRouteToNumber();
            String webCost = route.getWebCost();
            String createdAt = route.getCreatedAt();
            String closeReason = route.getCloseReason();
            String auto = route.getAuto();
            String dispatchingOrderUidDouble = route.getDispatchingOrderUidDouble();
            String pay_method = route.getPay_method();

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
            databaseHelper.addRouteCancel(uid, routeInfo);
            List<String> settings = new ArrayList<>();

             settings.add(uid);
             settings.add(webCost);
             settings.add(routeFrom);
             settings.add(routefromnumber);
             settings.add(routeTo);
             settings.add(routeTonumber);
             settings.add(dispatchingOrderUidDouble);
             settings.add(pay_method);

            Logger.d(context, TAG, settings.toString());
            databaseHelperUid.addCancelInfoUid(settings);
        }
        array = databaseHelper.readRouteCancel();
        Logger.d(context, TAG, "processRouteList: array " + Arrays.toString(array));
        if(array != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireActivity(), R.layout.drop_down_layout, array);
            listView.setAdapter(adapter);
            listView.setVisibility(View.VISIBLE);
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
        } else {
            listView.setVisibility(View.GONE);
            scrollButtonDown.setVisibility(View.GONE);
            scrollButtonUp.setVisibility(View.GONE);
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