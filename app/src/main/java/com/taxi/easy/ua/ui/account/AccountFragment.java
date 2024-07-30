package com.taxi.easy.ua.ui.account;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.R.string.format_phone;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentAccountBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.ui.keyboard.KeyboardUtils;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.user.del_server.UserRepository;
import com.taxi.easy.ua.utils.user.save_firebase.FirebaseUserManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountFragment extends Fragment {

    private static final String TAG = "AccountFragment";
    private FragmentAccountBinding binding;
    private FirebaseUserManager userManager;
    EditText phoneNumber;
    EditText userName;
    TextView email;

    ProgressBar progressBar;
    AppCompatButton upd_but;
    AppCompatButton del_but;
    AppCompatButton btnCallAdmin;
    AppCompatButton btnOrder;
    View root;
    UserRepository userRepository;
    String userEmail;
    DatabaseHelper databaseHelper;
    DatabaseHelperUid databaseHelperUid;
    private String[] array;
    private List<RouteResponseCancel> routeListCancel;


    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAccountBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        userManager = new FirebaseUserManager();

        upd_but = binding.updBut;
        del_but = binding.delBut;
        btnCallAdmin = binding.btnCallAdmin;

        userName = binding.userName;
        phoneNumber = binding.phoneNumber;
        email = binding.email;

        List<String> stringList =  logCursor(MainActivity.TABLE_USER_INFO);
        userEmail = stringList.get(3);

        userName.setText(stringList.get(4));
        phoneNumber.setText(stringList.get(2));
        email.setText(userEmail);

        userName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                upd_but.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        phoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                upd_but.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        progressBar = binding.progressBar;
        email.setOnClickListener(v -> {
            KeyboardUtils.hideKeyboard(requireActivity(), root);
            Toast.makeText(getActivity(), R.string.email_upd, Toast.LENGTH_SHORT).show();
        });



        upd_but.setOnClickListener(v -> {
            accountSet();
        });

        databaseHelper = new DatabaseHelper(getActivity());
        databaseHelperUid = new DatabaseHelperUid(getActivity());

        del_but.setOnClickListener(v -> {
            del_but.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            array = databaseHelper.readRouteCancel();
            Logger.d(getActivity(), TAG, "processRouteList: array " + Arrays.toString(array));
            if (array.length != 0) {
                String message = getString(R.string.order_to_cancel_true);
                MyBottomSheetErrorFragment myBottomSheetMessageFragment = new MyBottomSheetErrorFragment(message);
                myBottomSheetMessageFragment.show(getChildFragmentManager(), myBottomSheetMessageFragment.getTag());
            } else {
                // Запустить указанный код
                KeyboardUtils.hideKeyboard(requireActivity(), root);
                userManager.deleteUserPhone();
                userRepository = new UserRepository();
                userRepository.destroyEmail(userEmail);
                resetUserInfo();
                Toast.makeText(getActivity(), R.string.del_info, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getActivity(), ExitActivity.class));
            }

        });
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

        btnOrder = binding.btnOrder;
        btnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Удаляем последний фрагмент из стека навигации и переходим к новому фрагменту
                MainActivity.navController.popBackStack();
                MainActivity.navController.navigate(R.id.nav_visicom);
            }
        });
        return root;
    }
    private void fetchRoutesCancel(String value) {
        Logger.d(getActivity(), TAG, "fetchRoutesCancel: ");

        routeListCancel = new ArrayList<>();
        String baseUrl = "https://m.easy-order-taxi.site";

        String url = baseUrl + "/android/UIDStatusShowEmailCancel/" + value;
        Call<List<RouteResponseCancel>> call = ApiClient.getApiService().getRoutesCancel(url);
        Logger.d(getActivity(), TAG, "fetchRoutesCancel: " + url);
        call.enqueue(new Callback<List<RouteResponseCancel>>() {
            @Override
            public void onResponse(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Response<List<RouteResponseCancel>> response) {
                if (response.isSuccessful()) {
                    List<RouteResponseCancel> routes = response.body();
                    assert routes != null;
                    Logger.d(getActivity(), TAG, "onResponse: " + response.body());

                    if (routes.size() == 1) {
                        RouteResponseCancel route = routes.get(0);
                        if ("*".equals(route.getRouteFrom()) && "*".equals(route.getRouteFromNumber()) &&
                                "*".equals(route.getRouteTo()) && "*".equals(route.getRouteToNumber()) &&
                                "*".equals(route.getWebCost()) && "*".equals(route.getCloseReason()) &&
                                "*".equals(route.getAuto()) && "*".equals(route.getCreatedAt())) {

                            // Запустить указанный код
                            KeyboardUtils.hideKeyboard(requireActivity(), root);
                            userManager.deleteUserPhone();
                            userRepository = new UserRepository();
                            userRepository.destroyEmail(userEmail);
                            resetUserInfo();
                            Toast.makeText(getActivity(), R.string.del_info, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getActivity(), ExitActivity.class));
                            return;
                        }
                    }
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
                            if (routeListCancel == null) {
                                routeListCancel = new ArrayList<>();
                            }
                            routeListCancel.addAll(routes);
                            processCancelList();
                        }

                    }
                }
            }


            public void onFailure(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
        progressBar.setVisibility(View.INVISIBLE);
    }

    private void processCancelList() {
        if (routeListCancel == null || routeListCancel.isEmpty()) {
            Logger.d(getActivity(), TAG, "routeListCancel is null or empty");
           return;
        }

        // Создайте массив строк
        array = new String[routeListCancel.size()];


        databaseHelper.clearTableCancel();
        databaseHelperUid.clearTableCancel();

        String closeReasonText = getString(R.string.close_resone_def);

        for (int i = 0; i < routeListCancel.size(); i++) {
            RouteResponseCancel route = routeListCancel.get(i);
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

            switch (closeReason) {
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

            if (routeFrom.equals("Місце відправлення")) {
                routeFrom = getString(R.string.start_point_text);
            }

            if (routeTo.equals("Точка на карте")) {
                routeTo = getString(R.string.end_point_marker);
            }
            if (routeTo.contains("по городу")) {
                routeTo = getString(R.string.on_city);
            }
            if (routeTo.contains("по місту")) {
                routeTo = getString(R.string.on_city);
            }
            String routeInfo = "";

            if (auto == null) {
                auto = "??";
            }

            if (routeFrom.equals(routeTo)) {
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

            Logger.d(getActivity(), TAG, settings.toString());
            databaseHelperUid.addCancelInfoUid(settings);
        }

        array = databaseHelper.readRouteCancel();
        Logger.d(getActivity(), TAG, "processRouteList: array " + Arrays.toString(array));
        if (array.length != 0) {
            String message = getString(R.string.order_to_cancel_true);
            MyBottomSheetErrorFragment myBottomSheetMessageFragment = new MyBottomSheetErrorFragment(message);
            myBottomSheetMessageFragment.show(getChildFragmentManager(), myBottomSheetMessageFragment.getTag());
        }

    }
    @Override
    public void onResume() {
        super.onResume();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void accountSet() {
        String phone = phoneNumber.getText().toString();

        Logger.d(requireActivity(), TAG, "onClick befor validate: ");
        String PHONE_PATTERN = "((\\+?380)(\\d{9}))$";
        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
        Logger.d(requireActivity(), TAG, "onClick No validate: " + val);
        if (!val) {
            Toast.makeText(requireActivity(), getString(format_phone) , Toast.LENGTH_SHORT).show();
            Logger.d(requireActivity(), TAG, "accountSet" + phoneNumber.getText().toString());

        } else {

            updateRecordsUser("phone_number", phone);

            userManager.saveUserPhone(phone);

            String newName = userName.getText().toString();
            if (newName.trim().isEmpty()) {
                newName = "No_name";
            }
            updateRecordsUser("username", newName);
        }
    }
    private void updateRecordsUser(String field, String result) {
        ContentValues cv = new ContentValues();

        cv.put(field, result);

        // обновляем по id
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                new String[] { "1" });
        database.close();

        KeyboardUtils.hideKeyboard(requireActivity(), root);
       Toast.makeText(getActivity(), R.string.info_upd, Toast.LENGTH_SHORT).show();
    }
    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = db.query(table, null, null, null, null, null, null);
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
        db.close();
        return list;
    }

    private void resetUserInfo() {
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.beginTransaction();

        try {
            // Обновление первой записи
            String updateSql = "UPDATE " + MainActivity.TABLE_USER_INFO
                    + " SET verifyOrder = ?," +
                    " phone_number = ?," +
                    " email = ?," +
                    " username = ?," +
                    " bonus = ?," +
                    " card_pay = ?, " +
                    "bonus_pay = ? " +
                    "WHERE rowid = (SELECT rowid FROM " + MainActivity.TABLE_USER_INFO + " LIMIT 1);";
            SQLiteStatement updateStatement = database.compileStatement(updateSql);

            updateStatement.clearBindings();
            updateStatement.bindString(1, "0");
            updateStatement.bindString(2, "+380");
            updateStatement.bindString(3, "email");
            updateStatement.bindString(4, "username");
            updateStatement.bindString(5, "0");
            updateStatement.bindString(6, "1");
            updateStatement.bindString(7, "1");

            updateStatement.execute();
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        database.close();
    }


}