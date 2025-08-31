package com.taxi.easy.ua.utils.bottom_sheet;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;
import static com.taxi.easy.ua.ui.visicom.VisicomFragment.setBtnBonusName;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.gallery.GalleryFragment;
import com.taxi.easy.ua.ui.home.CustomArrayAdapter;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.permissions.UserPermissions;
import com.taxi.easy.ua.utils.retrofit.cost_json_parser.CostJSONParserRetrofit;
import com.uxcam.UXCam;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MyBottomSheetBonusFragment extends BottomSheetDialogFragment {

    private static final String TAG = "MyBottomSheetBonusFragment";
    long cost;
    String rout;
    String api;
    TextView textView;

    ListView listView;
    String[] array, arrayCode;
    AppCompatButton btn_ok;
    int pos;
    ProgressBar progressBar;
    CustomArrayAdapter adapter;
    private static SQLiteDatabase database;
    private static String[] userPayPermissions;
    private static String email;
    String city;
    Activity context;
    int fistItem, finishItem;
    private TextView tvSelectedTime, tvSelectedDate;
    View view;

    public MyBottomSheetBonusFragment() {
    }

//    private final String baseUrl = "https://m.easy-order-taxi.site";
    private final  String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");


    public MyBottomSheetBonusFragment(long cost, String rout, String api, TextView textView) {
        this.cost = cost;
        this.rout = rout;
        this.api = api;
        this.textView = textView;

    }

    @SuppressLint({"MissingInflatedId", "Range"})
     
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        view = inflater.inflate(R.layout.bonus_list_layout, container, false);
        context = requireActivity();

        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        } catch (Exception e) {
            Logger.d(context, TAG, "Инициализация базы данных не удалась" + e);
            FirebaseCrashlytics.getInstance().recordException(e);
            // Обработайте ошибку корректно, возможно, покажите сообщение пользователю
        }
        email = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        UserPermissions.getPermissions(email, context);

        progressBar = view.findViewById(R.id.progress);
        listView = view.findViewById(R.id.listViewBonus);
        array = new  String[]{
                getString(R.string.nal_payment),
                getString(R.string.bonus_payment),
                getString(R.string.card_payment),
        };
        arrayCode = new  String[]{
                "nal_payment",
                "bonus_payment",
                "wfp_payment",
        };

        adapter = new CustomArrayAdapter(context, R.layout.services_adapter_layout, Arrays.asList(array));
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        btn_ok = view.findViewById(R.id.btn_ok);

        btn_ok.setOnClickListener(v -> dismiss());


        userPayPermissions = UserPermissions.getUserPayPermissions(context);

        String bonus = logCursor(MainActivity.TABLE_USER_INFO).get(5);

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        city = stringList.get(1);
        Log.d(TAG, "onCreateView: " + city);

        switch (city) {
            case "foreign countries":
            case "Dnipropetrovsk Oblast":
            case "Odessa":
            case "Zaporizhzhia":
            case "Cherkasy Oblast":
                listView.setItemChecked(0, true);
                adapter.setItemEnabled(1, false);
                adapter.setItemEnabled(2, false);
                break;
            case "Kyiv City":
            case "OdessaTest":
                if(Long.parseLong(bonus) <= cost * 100 ) {
                    adapter.setItemEnabled(1, false);

                } else {
                    if(userPayPermissions[0].equals("0")) {
                        adapter.setItemEnabled(1, false);
                    }
                    if(userPayPermissions[1].equals("0")) {
                        adapter.setItemEnabled(2, false);
                    }
                }
                fistItem();
                break;
            default:
                listView.setItemChecked(0, true);
                adapter.setItemEnabled(1, false);
                fistItem();

        }

        if(userPayPermissions[1].equals("0")) {
            adapter.setItemEnabled(2, false);
        }

        listView.setOnItemClickListener((parent, view1, position, id) -> {
            progressBar.setVisibility(View.VISIBLE);
            btn_ok.setVisibility(GONE);

            pos = position;
            finishItem = pos;
            Log.d(TAG, "onItemClick: pos " + pos);
            if (pos == 2) {
                String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS, context);
                Logger.d(context, TAG, "payWfp: rectoken " + rectoken);
                if (rectoken.isEmpty()) {
                    String message = context.getString(R.string.no_cards_info);
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                    bottomSheetDialogFragment.show(getParentFragmentManager(), bottomSheetDialogFragment.getTag());
                    dismiss();
                } else {
                    if(userPayPermissions[0].equals("0")) {
                        adapter.setItemEnabled(2, false);
                    } else {
                        try {
                            Logger.d(context, TAG, "paymentType: 1 ");
                            paymentType(arrayCode [pos], context);
                        } catch (MalformedURLException | UnsupportedEncodingException e) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                            throw new RuntimeException(e);
                        }

                    }
                }

            } else if ((boolean) sharedPreferencesHelperMain.getValue("verifyUserOrder", false)) {
                String message = context.getString(R.string.black_list_message_err);

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                try {
                    reCount();
                } catch (UnsupportedEncodingException | MalformedURLException e) {
                    throw new RuntimeException(e);
                }
                dismiss();
            }
                else {
                try {
                    Logger.d(context, TAG, "paymentType: 2 ");
                    paymentType(arrayCode [pos], context);
                } catch (MalformedURLException | UnsupportedEncodingException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                    throw new RuntimeException(e);
                }
            }

        });
        return view;
    }
    private void timeVerify() {
        String TAG = "TimeVerify"; // Тег для логирования
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        // Текущие дата и время в Киеве
        LocalDateTime currentDateTimeInKyiv = LocalDateTime.now(ZoneId.of("Europe/Kiev"));
        LocalDate currentDate = LocalDate.now(ZoneId.of("Europe/Kiev"));
        Logger.d(context, TAG, "Текущая дата и время в Киеве: " + currentDateTimeInKyiv);

        // Получение сохраненных значений
        String time = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String date = (String) sharedPreferencesHelperMain.getValue("date", "no_date");
        Logger.d(context, TAG, "Сохраненные значения -> время: " + time + ", дата: " + date);

        // Если дата не выбрана, используем текущую дату с экрана
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvSelectedTime = view.findViewById(R.id.tv_selected_time);
        if (tvSelectedDate != null && tvSelectedTime != null) {
            if (date.equals("no_date")) {
                date = tvSelectedDate.getText().toString();
                Logger.d(context, TAG, "Дата не выбрана, взята с экрана: " + date);
            }

            // Если время не выбрано, используем время с экрана
            if (time.equals("no_time")) {
                time = tvSelectedTime.getText().toString();
                Logger.d(context, TAG, "Время не выбрано, взято с экрана: " + time);
            }
        }

        try {
            // Проверка, если дата или время все еще не определены
            if (date.equals("no_date") || time.equals("no_time")) {
                Logger.d(context, TAG, "Дата или время не определены, сброс значений");
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                return;
            }

            // Преобразование строки в LocalDateTime
            LocalDateTime dateTimeFromString = LocalDateTime.parse(date + " " + time, formatter);
            Logger.d(context, TAG, "Преобразованная дата и время: " + dateTimeFromString);

            // Проверка, если выбранное время раньше текущего
            if (dateTimeFromString.isBefore(currentDateTimeInKyiv)) {
                Logger.d(context, TAG, "Выбранное время в прошлом, сброс значений");
                Toast.makeText(context, context.getString(R.string.resettimetoorder), Toast.LENGTH_SHORT).show();
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                return;
            }

            // Вычисление разницы во времени
            long minutesDifference = Duration.between(currentDateTimeInKyiv, dateTimeFromString).toMinutes();
            Logger.d(context, TAG, "Разница во времени: " + minutesDifference + " минут");

            // Если разница меньше или равна 10 минут, сбрасываем
            if (minutesDifference <= 10 && minutesDifference >= 0) {
                Logger.d(context, TAG, "Разница <= 10 минут, сброс значений");
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                Toast.makeText(context, context.getString(R.string.resettimetoorder), Toast.LENGTH_SHORT).show();
            } else {
                // Сохраняем валидные значения
                sharedPreferencesHelperMain.saveValue("time", time);
                sharedPreferencesHelperMain.saveValue("date", date);
                Logger.d(context, TAG, "Сохранены значения -> время: " + time + ", дата: " + date);
            }
        } catch (DateTimeParseException e) {
            Logger.e(context, TAG, "Ошибка парсинга даты/времени: " + e.getMessage());
            sharedPreferencesHelperMain.saveValue("time", "no_time");
            sharedPreferencesHelperMain.saveValue("date", "no_date");
            Toast.makeText(context, "Неверный формат даты или времени", Toast.LENGTH_SHORT).show();
        } finally {
            database.close();
            Logger.d(context, TAG, "База данных закрыта");
        }
    }


    private void paymentType(String paymentCode, Context context) throws MalformedURLException, UnsupportedEncodingException {
        ContentValues cv = new ContentValues();
        Log.d(TAG, "paymentType: paymentCode 1111" + paymentCode);

        cv.put("payment_type", paymentCode);
        // обновляем по id

        SQLiteDatabase db = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        db.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        db.close();
        reCount();
        dismiss();

    }

    @SuppressLint("Range")
    private void fistItem() {

        String payment_type = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(4);

        Log.d(TAG, "fistItem: " + payment_type);
        switch (payment_type) {
            case "nal_payment":
                listView.setItemChecked(0, true);
                pos = 0;

                adapter.setItemEnabled(2, !userPayPermissions[1].equals("0"));
                break;
            case "bonus_payment":

                if(userPayPermissions[0].equals("0")) {
                    adapter.setItemEnabled(1, false);
                } else {
                    listView.setItemChecked(1, true);
                    Logger.d(context, TAG, "paymentType: 3 ");
                    pos = 1;
                }

                adapter.setItemEnabled(2, !userPayPermissions[1].equals("0"));
                break;

            case "card_payment":
            case "mono_payment":
            case "wfp_payment":
                String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS, context);
                Logger.d(context, TAG, "payWfp: rectoken " + rectoken);
                if (rectoken.isEmpty()) {
                    pos = 0;
                    listView.setItemChecked(0, true);
                    try {
                        Logger.d(context, TAG, "paymentType: 4 ");
                        paymentType("nal_payment", context);
                    } catch (MalformedURLException | UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                    String message = context.getString(R.string.no_cards_info);
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                    bottomSheetDialogFragment.show(getParentFragmentManager(), bottomSheetDialogFragment.getTag());
                    dismiss();
                } else  {
                    if(userPayPermissions[1].equals("0")) {
                        adapter.setItemEnabled(2, false);
                    } else  {
                        listView.setItemChecked(2, true);
                        pos = 2;
                    }
                }


                break;
        }
        fistItem = pos;
        finishItem = pos;
   }

    @SuppressLint("Range")
    private static String getCheckRectoken(String table, Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String[] columns = {"rectoken"}; // Указываем нужное поле
        String selection = "rectoken_check = ?";
        String[] selectionArgs = {"1"};
        String result = "";

        Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                result = cursor.getString(cursor.getColumnIndex("rectoken"));
                Logger.d(context, TAG, "Found rectoken with rectoken_check = 1" + ": " + result);
                return result;
            } while (cursor.moveToNext());
        }
        cursor.close();

        database.close();

        return result;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (rout != null && rout.equals("home")) {
            HomeFragment.setBtnBonusName(context);
        }
        if (rout != null && rout.equals("visicom")) {
            VisicomFragment.setBtnBonusName(context);
            VisicomFragment.tariffBtnColor();
        }
        UserPermissions.getPermissions(email, context);

        if(fistItem == finishItem) {
            try {
                reCount();
            } catch (UnsupportedEncodingException | MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void reCount() throws UnsupportedEncodingException, MalformedURLException {
        Log.d(TAG, "onDismiss: rout " + rout);
        if (rout != null && rout.equals("home")) {
            textView.setText("");
            String urlCost = getTaxiUrlSearch("costSearch", context);
            String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(3);

            CostJSONParserRetrofit parser = new CostJSONParserRetrofit();
            parser.sendURL(urlCost, new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                    Map<String, String> sendUrlMapCost = response.body();
                    assert sendUrlMapCost != null;
                    String orderCost = sendUrlMapCost.get("order_cost");

                    assert orderCost != null;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!orderCost.equals("0")) {
                            long discountInt = Integer.parseInt(discountText);
                            long discount;
                            long firstCost = Long.parseLong(orderCost);
                            discount = firstCost * discountInt / 100;

                            firstCost = firstCost + discount;
                            //                        updateAddCost(String.valueOf(discount));


                            HomeFragment.costFirstForMin = firstCost;
                            String costUpdate = String.valueOf(firstCost);
                            textView.setText(costUpdate);
                            HomeFragment.btnVisible(View.VISIBLE);
                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            if (pos == 1 || pos == 2) {
                                changePayMethodToNal();
                            }
                        }
                    });


                }

                @Override
                public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                    FirebaseCrashlytics.getInstance().recordException(t);
                    Logger.d(getActivity(), TAG, " onFailure home" + t);
                }
            });


        }
        if (rout != null && rout.equals("visicom")) {

            timeVerify();
            Toast.makeText(context, context.getString(R.string.check_cost_message), Toast.LENGTH_SHORT).show();
            NavController navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());
//            try {
//
//                    String urlCost = getTaxiUrlSearchMarkers("costSearchMarkersTime", context);
//                    String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(3);
//                    long discountInt = Integer.parseInt(discountText);
//
//                    CostJSONParserRetrofit parser = new CostJSONParserRetrofit();
//                    parser.sendURL(urlCost, new Callback<>() {
//                        @Override
//                        public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
//                            Map<String, String> sendUrlMapCost = response.body();
//                            if (sendUrlMapCost != null) {
//                                String orderCost = sendUrlMapCost.get("order_cost");
//                                Log.d(TAG, "onDismiss: orderCost " + orderCost);
//                                assert orderCost != null;
//                                if (!orderCost.equals("0")) {
//                                    new Handler(Looper.getMainLooper()).post(() -> {
//
//                                        String costUpdate;
//
//                                        long discount;
//                                        long firstCost = Long.parseLong(orderCost);
//                                        discount = firstCost * discountInt / 100;
//                                        firstCost = firstCost + discount;
//                                        //                                updateAddCost(String.valueOf(discount));
//
//                                        VisicomFragment.firstCostForMin = firstCost;
//
//                                        VisicomFragment.startCost = firstCost;
//                                        VisicomFragment.finalCost = firstCost;
//
//                                        Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: startCost " + VisicomFragment.startCost);
//                                        Logger.d(context, TAG, "getTaxiUrlSearchMarkers cost: finalCost " + VisicomFragment.finalCost);
//
//                                        costUpdate = String.valueOf(firstCost);
//                                        Log.d(TAG, "onResponse:costUpdate " + costUpdate);
//
//                                        VisicomFragment.text_view_cost.setText(costUpdate);
//                                        VisicomFragment.btnStaticVisible(View.VISIBLE);
//
//                                    });
//                                } else {
//                                    new Handler(Looper.getMainLooper()).post(() -> {
//                                        progressBar.setVisibility(View.INVISIBLE);
//                                        if (pos == 1 || pos == 2) {
//                                            changePayMethodToNal();
//                                        }
//                                    });
//
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
//                            FirebaseCrashlytics.getInstance().recordException(t);
//                            Logger.d(getActivity(), TAG, " onFailure visicom" + t);
//                        }
//                    });
//
//            } catch (MalformedURLException e) {
//                FirebaseCrashlytics.getInstance().recordException(e);
//            }

        }
        if (rout != null && rout.equals("marker")) {
            try {
                if (isAdded()) {
                    String urlCost = getTaxiUrlSearchMarkers("costSearchMarkersTime", context);

                    String discountText = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(3);
                    long discountInt = Long.parseLong(discountText);

                    CostJSONParserRetrofit parser = new CostJSONParserRetrofit();
                    parser.sendURL(urlCost, new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                            Map<String, String> sendUrlMapCost = response.body();
                            assert sendUrlMapCost != null;
                            String orderCost = sendUrlMapCost.get("order_cost");
                            Log.d(TAG, "onDismiss: orderCost " + orderCost);
                            assert orderCost != null;
                            if (!orderCost.equals("0")) {
                                String costUpdate;

                                long discount;
                                long firstCost = Long.parseLong(orderCost);
                                discount = firstCost * discountInt / 100;

                                firstCost = firstCost + discount;
//                                updateAddCost(String.valueOf(discount));

                                GalleryFragment.costFirstForMin = firstCost;
                                costUpdate = String.valueOf(firstCost);
                                textView.setText(costUpdate);
                            } else {
                                progressBar.setVisibility(View.INVISIBLE);
                                if (pos == 1 || pos == 2) {
                                    changePayMethodToNal();
                                }
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                            FirebaseCrashlytics.getInstance().recordException(t);
                            Logger.d(getActivity(), TAG, " onFailure marker" + t);
                        }
                    });
                }
            } catch (MalformedURLException e) {
                Logger.d(getActivity(), TAG, "Ошибка при обработке платежа" + e);
                FirebaseCrashlytics.getInstance().recordException(e);
                // Обработайте ошибку корректно
            }

        }
        dismiss();
//        progressBar.setVisibility(GONE);

//        btn_ok.setVisibility(View.VISIBLE);


    }
    private AlertDialog alertDialog;
    private void changePayMethodToNal() {
        // Инфлейтим макет для кастомного диалога
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);

        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setView(dialogView);
        alertDialog.setCancelable(false);
        // Настраиваем элементы макета


        TextView messageTextView = dialogView.findViewById(R.id.dialog_message);
        String messagePaymentType = context.getString(R.string.to_nal_payment_count);
        messageTextView.setText(messagePaymentType);

        Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
        okButton.setOnClickListener(v -> {
            listView.setItemChecked(0, true);
            try {
                Logger.d(context, TAG, "paymentType: 5 ");
                paymentType(arrayCode [0], context);
                setBtnBonusName(context);
            } catch (MalformedURLException | UnsupportedEncodingException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                throw new RuntimeException(e);
            }
            progressBar.setVisibility(GONE);
            dismiss();
            alertDialog.dismiss();
        });

        Button cancelButton = dialogView.findViewById(R.id.dialog_cancel_button);
        cancelButton.setOnClickListener(v -> {
            try {
                paymentType(arrayCode [0], context);
                setBtnBonusName(context);
            } catch (MalformedURLException | UnsupportedEncodingException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                throw new RuntimeException(e);
            }
            progressBar.setVisibility(GONE);
            alertDialog.dismiss();
            dismiss();
        });

        alertDialog.show();
    }



    private String getTaxiUrlSearch(String urlAPI, Context context) throws UnsupportedEncodingException {

        List<String> stringListRout = logCursor(MainActivity.ROUT_HOME);

        String originalString = stringListRout.get(1);
        int indexOfSlash = originalString.indexOf("/");
        String from = (indexOfSlash != -1) ? originalString.substring(0, indexOfSlash) : originalString;

        String from_number = stringListRout.get(2);

        originalString = stringListRout.get(3);
        indexOfSlash = originalString.indexOf("/");
        String to = (indexOfSlash != -1) ? originalString.substring(0, indexOfSlash) : originalString;

        String to_number = stringListRout.get(4);

        // Origin of route
        String str_origin = from + "/" + from_number;

        // Destination of route
        String str_dest = to + "/" + to_number;



        List<String> stringList = logCursor(MainActivity.TABLE_SETTINGS_INFO);
        String tarif =  stringList.get(2);
        String payment_type = stringList.get(4);

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO).get(4);

        if(urlAPI.equals("costSearch")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);
            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail  + "*" + payment_type;
        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i <= 14 ; i++) {
            if(services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if(servicesVer) {
            for (int i = 0; i < arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if(servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Log.d(TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);


        Log.d(TAG, "getTaxiUrlSearch: " + url);

        return url;
    }

    @SuppressLint("Range")

    private String getTaxiUrlSearchMarkers(String urlAPI, Context context) {

        double originLatitude = 0;
        double originLongitude = 0;
        double toLatitude = 0;
        double toLongitude = 0;

        String[] projection = {
                "startLat",
                "startLan",
                "to_lat",
                "to_lng"
        };

        String selection = "id = ?";
        String[] selectionArgs = { "1" }; // предполагается, что вы хотите получить данные для записи с id=1
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(
                MainActivity.ROUT_MARKER, // имя таблицы
                projection,   // столбцы, которые вы хотите получить
                selection,    // условие выборки
                selectionArgs,// аргументы условия выборки
                null,         // группировка строк
                null,         // условие группировки строк
                null          // порядок сортировки
        );

        if (cursor.moveToFirst()) {
            originLatitude = cursor.getDouble(cursor.getColumnIndex("startLat"));
            originLongitude = cursor.getDouble(cursor.getColumnIndex("startLan"));
            toLatitude = cursor.getDouble(cursor.getColumnIndex("to_lat"));
            toLongitude = cursor.getDouble(cursor.getColumnIndex("to_lng"));

            // Теперь у вас есть значения из базы данных
            Log.d(TAG, "StartLat: " + originLatitude + ", StartLan: " + originLongitude + ", ToLat: " + toLatitude + ", ToLng: " + toLongitude);

            cursor.close();
        } else {
            // Обработка случая, когда данных нет
            Logger.d(getActivity(), TAG, "No data found in ROUT_MARKER table");
        }


        // Origin of route
        String str_origin = originLatitude + "/" + originLongitude;

        // Destination of route
        String str_dest = toLatitude + "/" + toLongitude;

//        List<String> stringList = logCursor(MainActivity.TABLE_ADD_SERVICE_INFO);
//        String time = stringList.get(1);
//        String comment = stringList.get(2);
//        String date = stringList.get(3);

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO);
//        String tarif =  stringListInfo.get(2);
        String payment_type = stringListInfo.get(4);
        String addCost = stringListInfo.get(5);

        String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");

        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: tarif " + tarif);
        // Building the parameters to the web service

        String parameters = null;
        String phoneNumber = "no phone";
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        String displayName = logCursor(MainActivity.TABLE_USER_INFO).get(4);

        String time = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String comment = (String) sharedPreferencesHelperMain.getValue("comment", "no_comment");
        String date = (String) sharedPreferencesHelperMain.getValue("date", "no_date");

        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: time " + time);
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: comment " + comment);
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: date " + date);


        if(urlAPI.equals("costSearchMarkersTime")) {
            Cursor c = database.query(MainActivity.TABLE_USER_INFO, null, null, null, null, null, null);

            if (c.getCount() == 1) {
                phoneNumber = logCursor(MainActivity.TABLE_USER_INFO).get(2);
                c.close();
            }
            parameters = str_origin + "/" + str_dest + "/" + tarif + "/" + phoneNumber + "/"
                    + displayName + " (" + context.getString(R.string.version_code) + ") " + "*" + userEmail  + "*" + payment_type+ "/"
                    + time + "/" + date ;
        }

        // Building the url to the web service
        List<String> services = logCursor(MainActivity.TABLE_SERVICE_INFO);
        List<String> servicesChecked = new ArrayList<>();
        String result;
        boolean servicesVer = false;
        for (int i = 1; i < services.size()-1 ; i++) {
            if(services.get(i).equals("1")) {
                servicesVer = true;
                break;
            }
        }
        if(servicesVer) {
            for (int i = 0; i < DataArr.arrayServiceCode().length; i++) {
                if(services.get(i+1).equals("1")) {
                    servicesChecked.add(DataArr.arrayServiceCode()[i]);
                }
            }
            for (int i = 0; i < servicesChecked.size(); i++) {
                if(servicesChecked.get(i).equals("CHECK_OUT")) {
                    servicesChecked.set(i, "CHECK");
                }
            }
            result = String.join("*", servicesChecked);
            Log.d(TAG, "getTaxiUrlSearchGeo result:" + result + "/");
        } else {
            result = "no_extra_charge_codes";
        }

        List<String> listCity = logCursor(MainActivity.CITY_INFO);
        String city = listCity.get(1);
        String api = listCity.get(2);

        String url = "/" + api + "/android/" + urlAPI + "/"
                + parameters + "/" + result + "/" + city  + "/" + context.getString(R.string.application);
        Log.d(TAG, "getTaxiUrlSearchMarkers: " + url);
        database.close();
        return url;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (database != null && database.isOpen()) {
            database.close();
        }
    }
    public static String[] arrayServiceCode() {
        return new String[]{
                "BAGGAGE",
                "ANIMAL",
                "CONDIT",
                "MEET",
                "COURIER",
                "CHECK_OUT",
                "BABY_SEAT",
                "DRIVER",
                "NO_SMOKE",
                "ENGLISH",
                "CABLE",
                "FUEL",
                "WIRES",
                "SMOKE",
        };
    }
    @SuppressLint("Range")
    private List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
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

        c.close();
        database.close();
        return list;
    }
   }

