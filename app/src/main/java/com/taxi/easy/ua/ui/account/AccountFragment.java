package com.taxi.easy.ua.ui.account;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.R.string.format_phone;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
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
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.redmadrobot.inputmask.MaskedTextChangedListener;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentAccountBinding;
import com.taxi.easy.ua.ui.exit.ExitActivity;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.RouteResponseCancel;
import com.taxi.easy.ua.ui.keyboard.KeyboardUtils;
import com.taxi.easy.ua.utils.auth.FirebaseConsentManager;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.db.DatabaseHelper;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.user.del_server.UserRepository;
import com.taxi.easy.ua.utils.user.save_firebase.FirebaseUserManager;
import com.uxcam.UXCam;

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
    TextView text_model;
    TextView text_androidVersion;

    ProgressBar progressBar;
    AppCompatButton upd_but;
    AppCompatButton del_but;
    AppCompatButton out_but;
    AppCompatButton in_but;
    AppCompatButton btnCallAdmin;
    AppCompatButton btnOrder;
    View root;
    UserRepository userRepository;
    String userEmail;
    DatabaseHelper dbH;
    DatabaseHelperUid dbHUid;
    private String[] array;
    private Context context;
    private List<RouteResponseCancel> routeList;
    private FirebaseConsentManager consentManager;

    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);
        button1.setVisibility(View.VISIBLE);
        binding = FragmentAccountBinding.inflate(inflater, container, false);


        root = binding.getRoot();
        context = requireActivity();
        userManager = new FirebaseUserManager();

        upd_but = binding.updBut;
        del_but = binding.delBut;
        del_but.setVisibility(View.GONE);
        btnCallAdmin = binding.btnCallAdmin;

        String model = Build.MODEL;
        text_model = binding.textModel;
        text_model.setText(model);
// Получение версии Android
        String androidVersion = Build.VERSION.RELEASE;
        text_androidVersion = binding.textAndroidVersion;
        text_androidVersion.setText(androidVersion);

        userName = binding.userName;
        phoneNumber = binding.phoneNumber;
        email = binding.email;

        List<String> stringList =  logCursor(MainActivity.TABLE_USER_INFO);
        userEmail = stringList.get(3);

        userName.setText(stringList.get(4));
        phoneNumber.setText(formatPhoneNumber(stringList.get(2)));
        email.setText(userEmail);


        out_but = binding.outBut;
        consentManager = new FirebaseConsentManager(requireActivity());
        out_but.setOnClickListener(v -> {
            consentManager.revokeTokenAndSignOut();
            Toast.makeText(context, R.string.out_account, Toast.LENGTH_SHORT).show();
            NavController navController = MainActivity.navController;

            navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());

        });

        in_but = binding.btnInAccount;
        in_but.setOnClickListener(v -> {
            SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
            ContentValues cv = new ContentValues();
            cv.put("email", "email");
            cv.put("verifyOrder", "1");
            // обновляем по id
            database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                    new String[] { "1" });
            database.close();

            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

        });



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

        del_but.setOnClickListener(v -> {
            Logger.d(context, TAG, "Delete button clicked");
//            del_but.setVisibility(View.GONE);
//            progressBar.setVisibility(View.VISIBLE);
//
//            // Запустить указанный код
//
//            KeyboardUtils.hideKeyboard(requireActivity(), root);
//            userManager.deleteUserPhone();
//            userRepository = new UserRepository();
//            userRepository.destroyEmail(userEmail);
//
//            sharedPreferencesHelperMain.saveValue("CityCheckActivity", "**");
//            updateRecordsUserInfo("email", "email", context);
//            resetUserInfo();
//            consentManager.revokeTokenAndSignOut();
//            Toast.makeText(context, R.string.out_account, Toast.LENGTH_SHORT).show();
//            Toast.makeText(getActivity(), R.string.del_info, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getActivity(), ExitActivity.class));
        });


        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

        btnOrder = binding.btnOrder;
        btnOrder.setOnClickListener(v -> {

            if (NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
            }

//            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
//
//                MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                        .setPopUpTo(R.id.nav_restart, true)
//                        .build());
//            }

            sharedPreferencesHelperMain.saveValue("gps_upd", true);
            // Удаляем последний фрагмент из стека навигации и переходим к новому фрагменту
            
            MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true) 
                        .build());
        });

        googleVerifyAccount();

        MaskedTextChangedListener listener = new MaskedTextChangedListener(
                "+38 [000] [000] [00] [00]",
                phoneNumber,
                null
        );

        phoneNumber.addTextChangedListener(listener);
        phoneNumber.setOnFocusChangeListener(listener);

        return root;
    }

    private void googleVerifyAccount() {
        FirebaseConsentManager consentManager = new FirebaseConsentManager(requireActivity());

        consentManager.checkUserConsent(new FirebaseConsentManager.ConsentCallback() {
            @Override
            public void onConsentValid() {
                Logger.d(context, TAG, "Согласие пользователя действительное.");
                visibility (View.VISIBLE);
                fetchRoutesCancel();
            }

            @Override
            public void onConsentInvalid() {
                Logger.d(context, TAG, "Согласие пользователя НЕ действительное.");
                visibility (View.INVISIBLE);
            }
        });
    }

    private void visibility (int visible) {
        if (visible == View.INVISIBLE) {
            in_but.setVisibility(VISIBLE);
        } else {
            in_but.setVisibility(View.GONE);
        }
        phoneNumber.setVisibility(visible);
        userName.setVisibility(visible);
        email.setVisibility(visible);
        upd_but.setVisibility(visible);
        del_but.setVisibility(visible);
        out_but.setVisibility(visible);
        btnOrder.setVisibility(visible);
        del_but.setVisibility(visible);

        root.findViewById(R.id.text_name).setVisibility(visible);
        root.findViewById(R.id.text_phone).setVisibility(visible);
        root.findViewById(R.id.text_email).setVisibility(visible);
    }
    private String formatPhoneNumber(String phoneNumber) {
        String input = phoneNumber.replaceAll("[^+\\d]", "");

        StringBuilder formattedNumber = new StringBuilder();
        if (input.length() == 13) {
            formattedNumber.append(input.substring(0, 3)).append(" ");
            formattedNumber.append(input.substring(3, 6)).append(" ");
            formattedNumber.append(input.substring(6, 9)).append(" ");
            formattedNumber.append(input.substring(9, 11)).append(" ");
            formattedNumber.append(input.substring(11, 13));
            return formattedNumber.toString();
        } else {
            return input;
        }

    }

    private void fetchRoutesCancel() {
        dbH = new DatabaseHelper(getContext());
        dbHUid = new DatabaseHelperUid(getContext());

        dbH.clearTableCancel();
        dbHUid.clearTableCancel();

        routeList = new ArrayList<>();

//        String baseUrl = "https://m.easy-order-taxi.site";
        String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        String city = stringList.get(1);

        String url = baseUrl + "/android/UIDStatusShowEmailCancelApp/" + userEmail + "/" + city + "/" +  context.getString(R.string.application);
        Call<List<RouteResponseCancel>> call = ApiClient.getApiService().getRoutesCancel(url);
        Logger.d(context, TAG, "fetchRoutesCancel: " + url);
        call.enqueue(new Callback<List<RouteResponseCancel>>() {
            @Override
            public void onResponse(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Response<List<RouteResponseCancel>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<RouteResponseCancel> routes = response.body();
                    Logger.d(context, TAG, "onResponse: " + routes);
                    if (routes != null && !routes.isEmpty()) {
                        if (routes.size() == 1) {
                            RouteResponseCancel route = routes.get(0);
                            Logger.d(context, TAG, "Checking route: " + route);
                            if ("*".equals(route.getRouteFrom())) {
                                Logger.d(context, TAG, "Route with asterisk found, performing delete actions");
                                // Выполняем действия удаления
                                Logger.d(context, TAG, "processRouteList: Array is empty, showing delete button");
                                del_but.setVisibility(View.VISIBLE);
                            } else {
                                routeList.addAll(routes);
                                processCancelList();
                            }
                        } else {
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
                            }
                        }

                    }
                } else {
                    MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());

                }
            }

            public void onFailure(@NonNull Call<List<RouteResponseCancel>> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
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
            String required_time = route.getRequired_time();
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
                    // Можно оставить старое значение или назначить что-то по умолчанию
                    // closeReasonText = closeReasonText; // бесполезно, можно просто ничего не делать
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
            if(required_time != null && !required_time.contains("1970-01-01")) {
                required_time = " " + getString(R.string.time_order) + required_time;
            } else {
                required_time = "";
            }

            if(routeFrom.equals(routeTo)) {
                routeInfo = routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to)
                        + context.getString(R.string.on_city)
                        + required_time
                        + context.getString(R.string.close_resone_cost) + webCost + " " + context.getString(R.string.UAH)
                        + context.getString(R.string.auto_info) + " " + auto + " "
                        + context.getString(R.string.close_resone_time)
                        + createdAt + context.getString(R.string.close_resone_text) + closeReasonText;
            } else {
                routeInfo = routeFrom + " " + routefromnumber
                        + context.getString(R.string.close_resone_to) + routeTo + " " + routeTonumber + "."
                        + required_time
                        + context.getString(R.string.close_resone_cost) + webCost + " " + context.getString(R.string.UAH)
                        + context.getString(R.string.auto_info) + " " + auto + " "
                        + context.getString(R.string.close_resone_time)
                        + createdAt + context.getString(R.string.close_resone_text) + closeReasonText;
            }

//                array[i] = routeInfo;
            dbH.addRouteCancel(uid, routeInfo);
            List<String> settings = new ArrayList<>();

            settings.add(uid);
            settings.add(webCost);
            settings.add(routeFrom);
            settings.add(routefromnumber);
            settings.add(routeTo);
            settings.add(routeTonumber);
            settings.add(dispatchingOrderUidDouble);
            settings.add(pay_method);
            settings.add(required_time);
            settings.add(flexible_tariff_name);
            settings.add(comment_info);
            settings.add(extra_charge_codes);

            Logger.d(context, TAG, settings.toString());
            dbHUid.addCancelInfoUid(settings);
        }
        array = dbH.readRouteCancel();
        Logger.d(context, TAG, "processRouteList: array " + Arrays.toString(array));
        if (array != null) {
            String message = getString(R.string.order_to_cancel_true);
            MyBottomSheetErrorFragment myBottomSheetMessageFragment = new MyBottomSheetErrorFragment(message);
            myBottomSheetMessageFragment.show(getChildFragmentManager(), myBottomSheetMessageFragment.getTag());
        } else {
            dbH.clearTableCancel();
            dbHUid.clearTableCancel();
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
        Logger.d(requireActivity(), TAG, "phoneNumber.getText().toString() " + phoneNumber.getText().toString());
        String phone = formatPhoneNumber(phoneNumber.getText().toString());
        Logger.d(requireActivity(), TAG, "phone " + phone);

        Logger.d(requireActivity(), TAG, "onClick befor validate: ");
        String PHONE_PATTERN = "\\+38 \\d{3} \\d{3} \\d{2} \\d{2}";


        boolean val = Pattern.compile(PHONE_PATTERN).matcher(phone).matches();
        Logger.d(requireActivity(), TAG, "onClick No validate: " + val);
        if (!val) {
            Toast.makeText(requireActivity(), getString(format_phone) , Toast.LENGTH_SHORT).show();
            Logger.d(requireActivity(), TAG, "accountSet" + phoneNumber.getText().toString());

        } else {
            phoneNumber.setText(phone);
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
            updateStatement.bindString(2, "+38");
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