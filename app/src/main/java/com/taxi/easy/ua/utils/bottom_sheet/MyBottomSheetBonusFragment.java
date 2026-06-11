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
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.gallery.GalleryFragment;
import com.taxi.easy.ua.ui.home.ButtonVisibilityCallback;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.retrofit.cost_json_parser.CostJSONParserRetrofit;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.payment.PaymentTypeHelper;

import android.widget.ImageView;
import com.taxi.easy.ua.utils.permissions.UserPermissions;
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
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.taxi.easy.ua.utils.db.CursorReadHelper;

public class MyBottomSheetBonusFragment extends BottomSheetDialogFragment {

    private static final String TAG = "MyBottomSheetBonusFragment";
    long cost;
    String rout;
    String api;
    TextView textView;

    RadioButton radioNal, radioBonus, radioCard, radioGooglePay;
    int pos;
    ProgressBar progressBar;
    private static SQLiteDatabase database;
    private static String[] userPayPermissions;
    private static String email;
    String city;
    Activity context;
    int fistItem, finishItem;
    private TextView tvSelectedTime, tvSelectedDate;
    View view;
    private FirestoreHelper firestoreHelper;
    private View rowPaymentCash;
    private View rowPaymentBonus;
    private View rowPaymentCard;
    private View rowPaymentGooglePay;
    private ButtonVisibilityCallback callback;
    private final String[] arrayCode = {
            "nal_payment",
            "bonus_payment",
            "wfp_payment",
            PaymentTypeHelper.GOOGLE_PAY,
    };

    public MyBottomSheetBonusFragment() {
    }

    private final String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

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

        view = inflater.inflate(R.layout.bonus_radio_layout, container, false);
        context = requireActivity();
        sharedPreferencesHelperMain.saveValue("old_cost", "0");

        try {
            database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        } catch (Exception e) {
            Logger.d(context, TAG, "Инициализация базы данных не удалась" + e);
            FirebaseCrashlytics.getInstance().recordException(e);
        }

        email = logCursor(MainActivity.TABLE_USER_INFO).get(3);
        UserPermissions.getPermissions(email, context);

        progressBar = view.findViewById(R.id.progress);
        radioNal = view.findViewById(R.id.radioNal);
        radioBonus = view.findViewById(R.id.radioBonus);
        radioCard = view.findViewById(R.id.radioCard);
        radioGooglePay = view.findViewById(R.id.radioGooglePay);
        rowPaymentCash = view.findViewById(R.id.rowPaymentCash);
        rowPaymentBonus = view.findViewById(R.id.rowPaymentBonus);
        rowPaymentCard = view.findViewById(R.id.rowPaymentCard);
        rowPaymentGooglePay = view.findViewById(R.id.rowPaymentGooglePay);
        bindPaymentRows();

        userPayPermissions = UserPermissions.getUserPayPermissions(context);

        List<String> stringList = logCursor(MainActivity.CITY_INFO);
        city = stringList.get(1);
        Log.d(TAG, "onCreateView: " + city);
        firestoreHelper = new FirestoreHelper(context);

        checkCardPaymentForCity(city);

        initRadioButtons();

        rowPaymentCash.setOnClickListener(v -> onPaymentRowSelected(0));
        rowPaymentBonus.setOnClickListener(v -> onPaymentRowSelected(1));
        rowPaymentCard.setOnClickListener(v -> onPaymentRowSelected(2));
        rowPaymentGooglePay.setOnClickListener(v -> onPaymentRowSelected(3));

        return view;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        callback = resolveButtonVisibilityCallback();
    }

    @Nullable
    private ButtonVisibilityCallback resolveButtonVisibilityCallback() {
        if (getParentFragment() instanceof ButtonVisibilityCallback) {
            return (ButtonVisibilityCallback) getParentFragment();
        }
        if (getActivity() instanceof ButtonVisibilityCallback) {
            return (ButtonVisibilityCallback) getActivity();
        }
        if (getActivity() == null) {
            return null;
        }
        Fragment navHost = getActivity().getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHost == null) {
            return null;
        }
        Fragment current = navHost.getChildFragmentManager().getPrimaryNavigationFragment();
        if (current instanceof ButtonVisibilityCallback) {
            return (ButtonVisibilityCallback) current;
        }
        return null;
    }

    private void bindPaymentRows() {
        configurePaymentRow(rowPaymentCash, R.drawable.ic_cash_payment, R.string.nal_payment, R.string.payment_cash_subtitle);
        configurePaymentRow(rowPaymentBonus, R.drawable.ic_star, R.string.bonus_payment, 0);
        configurePaymentRow(rowPaymentCard, R.drawable.ic_credit_card, R.string.card_payment, 0);
        configurePaymentRow(rowPaymentGooglePay, R.drawable.ic_google_pay, R.string.btn_pay_google, R.string.payment_google_pay_subtitle);
    }

    private void configurePaymentRow(View row, int iconRes, int titleRes, int subtitleRes) {
        ImageView icon = row.findViewById(R.id.paymentRowIcon);
        TextView title = row.findViewById(R.id.paymentRowTitle);
        TextView subtitle = row.findViewById(R.id.paymentRowSubtitle);
        icon.setImageResource(iconRes);
        title.setText(titleRes);
        if (subtitleRes != 0) {
            subtitle.setText(subtitleRes);
            subtitle.setVisibility(View.VISIBLE);
        } else {
            subtitle.setVisibility(View.GONE);
        }
        row.findViewById(R.id.paymentRowCheck).setVisibility(View.GONE);
    }

    private void onPaymentRowSelected(int position) {
        progressBar.setVisibility(View.VISIBLE);
        pos = position;
        finishItem = pos;
        Log.d(TAG, "onPaymentRowSelected: pos " + pos);
        handleRadioSelection(position);
    }

    private void refreshPaymentRowChecks() {
        setRowChecked(rowPaymentCash, pos == 0);
        setRowChecked(rowPaymentBonus, pos == 1);
        setRowChecked(rowPaymentCard, pos == 2);
        setRowChecked(rowPaymentGooglePay, pos == 3);
    }

    private void setRowChecked(View row, boolean checked) {
        row.findViewById(R.id.paymentRowCheck).setVisibility(checked ? View.VISIBLE : View.GONE);
    }

    private void initRadioButtons() {
        String bonus = logCursor(MainActivity.TABLE_USER_INFO).get(5);
        String payment_type = logCursor(MainActivity.TABLE_SETTINGS_INFO).get(4);

        // Базовые настройки доступности в зависимости от города
        switch (city) {
            case "foreign countries":
            case "Dnipropetrovsk Oblast":
            case "Odessa":
            case "Zaporizhzhia":
            case "Cherkasy Oblast":
                radioBonus.setEnabled(false);
                radioCard.setEnabled(false);
                radioGooglePay.setEnabled(false);
                radioBonus.setAlpha(0.5f);
                radioCard.setAlpha(0.5f);
                radioGooglePay.setAlpha(0.5f);
                pos = 0;
                break;

            case "Kyiv City":
            case "OdessaTest":
                if(Long.parseLong(bonus) <= cost * 100) {
                    radioBonus.setEnabled(false);
                    radioBonus.setAlpha(0.5f);
                } else {
                    if(userPayPermissions[0].equals("0")) {
                        radioBonus.setEnabled(false);
                        radioBonus.setAlpha(0.5f);
                    }
                    if(userPayPermissions[1].equals("0")) {
                        setCardAndGooglePayEnabled(false);
                    }
                }
                initPaymentTypeSelection(payment_type);
                break;

            default:
                radioBonus.setEnabled(false);
                radioBonus.setAlpha(0.5f);
                initPaymentTypeSelection(payment_type);
        }

        if(userPayPermissions[1].equals("0")) {
            setCardAndGooglePayEnabled(false);
        }
        syncRowEnabledState();
        refreshPaymentRowChecks();
    }

    private void syncRowEnabledState() {
        rowPaymentCash.setEnabled(radioNal.isEnabled());
        rowPaymentCash.setAlpha(radioNal.getAlpha());
        rowPaymentBonus.setEnabled(radioBonus.isEnabled());
        rowPaymentBonus.setAlpha(radioBonus.getAlpha());
        rowPaymentCard.setEnabled(radioCard.isEnabled());
        rowPaymentCard.setAlpha(radioCard.getAlpha());
        rowPaymentGooglePay.setEnabled(radioGooglePay.isEnabled());
        rowPaymentGooglePay.setAlpha(radioGooglePay.getAlpha());
    }

    private void setCardAndGooglePayEnabled(boolean enabled) {
        float alpha = enabled ? 1f : 0.5f;
        radioCard.setEnabled(enabled);
        radioCard.setAlpha(alpha);
        radioGooglePay.setEnabled(enabled);
        radioGooglePay.setAlpha(alpha);
        rowPaymentCard.setEnabled(enabled);
        rowPaymentCard.setAlpha(alpha);
        rowPaymentGooglePay.setEnabled(enabled);
        rowPaymentGooglePay.setAlpha(alpha);
    }

    private void initPaymentTypeSelection(String payment_type) {
        switch (payment_type) {
            case "nal_payment":
                pos = 0;
                break;

            case "bonus_payment":
                if(userPayPermissions[0].equals("0")) {
                    radioBonus.setEnabled(false);
                    radioBonus.setAlpha(0.5f);
                    pos = 0;
                } else {
                    pos = 1;
                }
                break;

            case "card_payment":
            case "mono_payment":
            case "wfp_payment":
                checkCardPaymentForCity(city);
                String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS, context);
                if (rectoken.isEmpty() || userPayPermissions[1].equals("0")) {
                    setCardAndGooglePayEnabled(false);
                    pos = 0;

                    String message = context.getString(R.string.no_cards_info);
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                    bottomSheetDialogFragment.show(getParentFragmentManager(), bottomSheetDialogFragment.getTag());

                    try {
                        paymentType("nal_payment", context);
                    } catch (MalformedURLException | UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    pos = 2;
                }
                break;

            case "google_pay_payment":
                if (userPayPermissions[1].equals("0")) {
                    setCardAndGooglePayEnabled(false);
                    pos = 0;
                } else {
                    pos = 3;
                }
                break;

            default:
                pos = 0;
                break;
        }
        fistItem = pos;
        finishItem = pos;
        syncRowEnabledState();
        refreshPaymentRowChecks();
    }

    private int getPositionFromId(int checkedId) {
        if (checkedId == R.id.radioNal) return 0;
        if (checkedId == R.id.radioBonus) return 1;
        return 2; // R.id.radioCard
    }

    private void handleRadioSelection(int position) {
        RadioButton selectedRadio = getRadioButton(position);

        if (!selectedRadio.isEnabled()) {
            progressBar.setVisibility(GONE);
            return;
        }

        if (position == 2) { // Карта
            String rectoken = getCheckRectoken(MainActivity.TABLE_WFP_CARDS, context);
            Logger.d(context, TAG, "payWfp: rectoken " + rectoken);
            if (rectoken.isEmpty()) {
                String message = context.getString(R.string.no_cards_info);
                MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                bottomSheetDialogFragment.show(getParentFragmentManager(), bottomSheetDialogFragment.getTag());
                dismiss();
            } else {
                if(userPayPermissions[1].equals("0")) {
                    setCardAndGooglePayEnabled(false);
                } else {
                    try {
                        paymentType(arrayCode[position], context);
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
        } else {
            try {
                paymentType(arrayCode[position], context);
            } catch (MalformedURLException | UnsupportedEncodingException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                throw new RuntimeException(e);
            }
        }
    }

    private RadioButton getRadioButton(int position) {
        switch (position) {
            case 0: return radioNal;
            case 1: return radioBonus;
            case 2: return radioCard;
            case 3: return radioGooglePay;
            default: return radioNal;
        }
    }

    private void timeVerify() {
        String TAG = "TimeVerify";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        LocalDateTime currentDateTimeInKyiv = LocalDateTime.now(ZoneId.of("Europe/Kiev"));
        LocalDate currentDate = LocalDate.now(ZoneId.of("Europe/Kiev"));
        Logger.d(context, TAG, "Текущая дата и время в Киеве: " + currentDateTimeInKyiv);

        String time = (String) sharedPreferencesHelperMain.getValue("time", "no_time");
        String date = (String) sharedPreferencesHelperMain.getValue("date", "no_date");
        Logger.d(context, TAG, "Сохраненные значения -> время: " + time + ", дата: " + date);

        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvSelectedTime = view.findViewById(R.id.tv_selected_time);
        if (tvSelectedDate != null && tvSelectedTime != null) {
            if (date.equals("no_date")) {
                date = tvSelectedDate.getText().toString();
                Logger.d(context, TAG, "Дата не выбрана, взята с экрана: " + date);
            }

            if (time.equals("no_time")) {
                time = tvSelectedTime.getText().toString();
                Logger.d(context, TAG, "Время не выбрано, взято с экрана: " + time);
            }
        }

        try {
            if (date.equals("no_date") || time.equals("no_time")) {
                Logger.d(context, TAG, "Дата или время не определены, сброс значений");
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                return;
            }

            LocalDateTime dateTimeFromString = LocalDateTime.parse(date + " " + time, formatter);
            Logger.d(context, TAG, "Преобразованная дата и время: " + dateTimeFromString);

            if (dateTimeFromString.isBefore(currentDateTimeInKyiv)) {
                Logger.d(context, TAG, "Выбранное время в прошлом, сброс значений");
                Toast.makeText(context, context.getString(R.string.resettimetoorder), Toast.LENGTH_SHORT).show();
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                return;
            }

            long minutesDifference = Duration.between(currentDateTimeInKyiv, dateTimeFromString).toMinutes();
            Logger.d(context, TAG, "Разница во времени: " + minutesDifference + " минут");

            if (minutesDifference <= 10 && minutesDifference >= 0) {
                Logger.d(context, TAG, "Разница <= 10 минут, сброс значений");
                sharedPreferencesHelperMain.saveValue("time", "no_time");
                sharedPreferencesHelperMain.saveValue("date", "no_date");
                Toast.makeText(context, context.getString(R.string.resettimetoorder), Toast.LENGTH_SHORT).show();
            } else {
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
            if (database != null && database.isOpen()) {
                database.close();
                Logger.d(context, TAG, "База данных закрыта");
            }
        }
    }

    private void paymentType(String paymentCode, Context context) throws MalformedURLException, UnsupportedEncodingException {
        Log.d(TAG, "paymentType: paymentCode " + paymentCode);
        PaymentTypeHelper.setPaymentType(context, paymentCode);
        refreshPaymentRowChecks();
        reCount();
        dismiss();
    }

    @SuppressLint("Range")
    private static String getCheckRectoken(String table, Context context) {
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        String[] columns = {"rectoken"};
        String selection = "rectoken_check = ?";
        String[] selectionArgs = {"1"};
        String result = "";

        Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                result = CursorReadHelper.getString(cursor, "rectoken");
                Logger.d(context, TAG, "Found rectoken with rectoken_check = 1: " + result);
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
                            HomeFragment.costFirstForMin = firstCost;
                            String costUpdate = String.valueOf(firstCost);
                            textView.setText(costUpdate);
                            if (callback != null) {
                                callback.onShowButtons(View.VISIBLE);
                            }
                        } else {
                            progressBar.setVisibility(View.INVISIBLE);
                            if (pos == 1 || pos == 2 || pos == 3) {
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
                                GalleryFragment.costFirstForMin = firstCost;
                                costUpdate = String.valueOf(firstCost);
                                textView.setText(costUpdate);
                            } else {
                                progressBar.setVisibility(View.INVISIBLE);
                                if (pos == 1 || pos == 2 || pos == 3) {
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
            }
        }
        dismiss();
    }

    private AlertDialog alertDialog;

    private void changePayMethodToNal() {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.custom_dialog_layout, null);

        alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setView(dialogView);
        alertDialog.setCancelable(false);

        TextView messageTextView = dialogView.findViewById(R.id.dialog_message);
        String messagePaymentType = context.getString(R.string.to_nal_payment_count);
        messageTextView.setText(messagePaymentType);

        Button okButton = dialogView.findViewById(R.id.dialog_ok_button);
        okButton.setOnClickListener(v -> {
            pos = 0;
            refreshPaymentRowChecks();
            try {
                paymentType(arrayCode[0], context);
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
                paymentType(arrayCode[0], context);
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

        String str_origin = from + "/" + from_number;
        String str_dest = to + "/" + to_number;

        List<String> stringList = logCursor(MainActivity.TABLE_SETTINGS_INFO);
        String tarif = stringList.get(2);
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
        String[] selectionArgs = { "1" };
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(
                MainActivity.ROUT_MARKER,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            originLatitude = CursorReadHelper.getDouble(cursor, "startLat");
            originLongitude = CursorReadHelper.getDouble(cursor, "startLan");
            toLatitude = CursorReadHelper.getDouble(cursor, "to_lat");
            toLongitude = CursorReadHelper.getDouble(cursor, "to_lng");

            Log.d(TAG, "StartLat: " + originLatitude + ", StartLan: " + originLongitude + ", ToLat: " + toLatitude + ", ToLng: " + toLongitude);
            cursor.close();
        } else {
            Logger.d(getActivity(), TAG, "No data found in ROUT_MARKER table");
        }

        String str_origin = originLatitude + "/" + originLongitude;
        String str_dest = toLatitude + "/" + toLongitude;

        List<String> stringListInfo = logCursor(MainActivity.TABLE_SETTINGS_INFO);
        String payment_type = stringListInfo.get(4);
        String addCost = stringListInfo.get(5);

        String tarif = (String) sharedPreferencesHelperMain.getValue("tarif", " ");
        Logger.d(context, TAG, "getTaxiUrlSearchMarkers: tarif " + tarif);

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
                    str = str.concat(cn + " = " + CursorReadHelper.getString(c, cn) + "; ");
                    list.add(CursorReadHelper.getString(c, cn));
                }
            } while (c.moveToNext());
        }
        c.close();
        database.close();
        return list;
    }


    private void checkCardPaymentForCity(String cityName) {
        String TAG = "checkCardPaymentForCity";

        Logger.d(context, TAG, "Проверка оплаты картой для города: " + cityName);


        firestoreHelper.getCardPaymentKeyForCity(
                new FirestoreHelper.OnCardPaymentKeyFetchedListener() {
                    @Override
                    public void onSuccess(Boolean cardPaymentEnabled) {
                        Logger.d(context, TAG, "Успешно получено значение: " + cardPaymentEnabled);

                        if (cardPaymentEnabled) {
                            Logger.d(context, TAG, "Оплата картой ДОСТУПНА для города " + cityName);
                            if (!userPayPermissions[1].equals("0")) {
                                setCardAndGooglePayEnabled(true);
                            }
                        } else {
                            Logger.d(context, TAG, "Оплата картой НЕДОСТУПНА для города " + cityName);
                            setCardAndGooglePayEnabled(false);
                        }
                        syncRowEnabledState();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Logger.e(context,TAG, "Ошибка получения настроек: " + e.getMessage());

                        // Показываем ошибку пользователю
                        Toast.makeText(context,
                                "Ошибка загрузки настроек: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                },
                cityName
        );
    }
}