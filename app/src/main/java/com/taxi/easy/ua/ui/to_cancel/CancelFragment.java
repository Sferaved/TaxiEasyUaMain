package com.taxi.easy.ua.ui.to_cancel;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
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
import com.taxi.easy.ua.utils.db.RouteInfoCancel;
import com.taxi.easy.ua.utils.log.Logger;

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
    private RouteInfoCancel routeInfo;
    private NetworkChangeReceiver networkChangeReceiver;
    ProgressBar progressBar;

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
    private final int desiredHeight = 1200;
    public static AppCompatButton btnCallAdmin;

    public CancelFragment() {
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
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
        upd_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NetworkUtils.isNetworkAvailable(context)) {
                    MainActivity.navController.popBackStack();
                    MainActivity.navController.navigate(R.id.nav_visicom);
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
            Logger.d(context, TAG, "onContextItemSelected: " + position);
            Logger.d(context, TAG, "onContextItemSelected: " + array[position]);

            routeInfo = databaseHelperUid.getCancelInfoById(position+1);
            if (routeInfo != null) {
                Logger.d(context, TAG, "onContextItemSelected: " + routeInfo);
            } else {
                Logger.d(context, TAG, "onContextItemSelected: RouteInfo not found for id: " + (position + 1));
            }

            Map<String, String> costMap = getStringStringMap();
            Logger.d(context, TAG, "onContextItemSelected costMap: " + costMap);
            startFinishPage(costMap);
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
            to_name = context.getString(R.string.on_city_tv);
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
            to_name_local = context.getString(R.string.on_city_tv);
        }
        Logger.d(context, TAG, "startFinishPage: to_name 4" + to_name_local);
        String pay_method_message = context.getString(R.string.pay_method_message_main);
        switch (Objects.requireNonNull(sendUrlMap.get("pay_method"))) {
            case "bonus_payment":
                pay_method_message += " " + context.getString(R.string.pay_method_message_bonus);
                break;
            case "card_payment":
            case "fondy_payment":
            case "mono_payment":
            case "wfp_payment":
                pay_method_message += " " + context.getString(R.string.pay_method_message_card);
                break;
            default:
                pay_method_message += " " + context.getString(R.string.pay_method_message_nal);
        }
         String thanksMessage = cleanString(context.getString(R.string.thanks_message));
         String routeFrom = cleanString(sendUrlMap.get("routefrom"));
         String toMessage = cleanString(context.getString(R.string.to_message));
         String toNameLocal = cleanString(to_name_local);
         String callOfOrder = cleanString(context.getString(R.string.call_of_order));
         String orderWeb = cleanString(Objects.requireNonNull(sendUrlMap.get("orderWeb")));
         String uah = cleanString(context.getString(R.string.UAH));
         String payMethodMessage = cleanString(pay_method_message);

         String messageResult = thanksMessage + " " +
                 routeFrom + " " +
                 toMessage + " " +
                 toNameLocal + ". " +
                 callOfOrder + " " +
                 orderWeb + " " +
                 uah + " " +
                 payMethodMessage;

         Logger.d(getActivity(), TAG, "messageResult: " + messageResult);

         String messageFondy = context.getString(R.string.fondy_message) + " " +
                sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
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
    private String cleanString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ").replaceAll("\\s{2,}$", " ");
    }

    private void fetchRoutesCancel(String value) {
        listView.setVisibility(View.GONE);
        scrollButtonDown.setVisibility(View.GONE);
        scrollButtonUp.setVisibility(View.GONE);

        databaseHelper.clearTableCancel();
        databaseHelperUid.clearTableCancel();

        routeList = new ArrayList<>();

        upd_but.setText(context.getString(R.string.cancel_gps));
        upd_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.navController.popBackStack();
                MainActivity.navController.navigate(R.id.nav_visicom);
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
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(context.getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());

                }
            }

            public void onFailure(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                FirebaseCrashlytics.getInstance().recordException(t);
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


        } else {
            listView.setVisibility(View.GONE);
            scrollButtonDown.setVisibility(View.GONE);
            scrollButtonUp.setVisibility(View.GONE);
        }
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
    }
}