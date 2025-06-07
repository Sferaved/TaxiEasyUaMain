package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentCardBinding;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.preferences.SharedPreferencesHelper;
import com.uxcam.UXCam;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CardFragment extends Fragment {

    private FragmentCardBinding binding;
    public static AppCompatButton btnCardLink, btnOrder;

    private String baseUrl;

    @SuppressLint("StaticFieldLeak")
    public static ProgressBar progressBar;
    private final String TAG = "CardFragment";
    String email;

    @SuppressLint("StaticFieldLeak")
    public static TextView textCard;

    @SuppressLint("StaticFieldLeak")
    public static ListView listView;
    public static String table;
    String pay_method;

    Activity context;
    WebView webView;
    FragmentManager fragmentManager;
    View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        binding = FragmentCardBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        return root;
    }

    private void cardViews() throws MalformedURLException, UnsupportedEncodingException {


        pay_method = "wfp_payment";

        textCard.setVisibility(View.VISIBLE);
        listView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

        Logger.d(context, TAG, "onResponse:pay_method "+pay_method);
        ArrayList<Map<String, String>> cardMaps = getCardMapsFromDatabase();
        table = MainActivity.TABLE_WFP_CARDS;

        Logger.d(context, TAG, "onResponse:cardMaps " + cardMaps);
        if (!cardMaps.isEmpty()) {
            CustomCardAdapter listAdapter = new CustomCardAdapter(context, cardMaps, table, pay_method);
            listView.setAdapter(listAdapter);
            textCard.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
        } else {
            textCard.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            textCard.setText(R.string.no_cards);
            paymentNal(context);
            progressBar.setVisibility(View.GONE);
        }
    }

    private void paymentNal(Context context) {
        ContentValues cv = new ContentValues();
        cv.put("payment_type", "nal_payment");
        // обновляем по id

        SQLiteDatabase db = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        db.update(MainActivity.TABLE_SETTINGS_INFO, cv, "id = ?",
                new String[] { "1" });
        db.close();
    }
    private  void getCardTokenWfp() {
        String city = logCursor(MainActivity.CITY_INFO, context).get(1);
        progressBar.setVisibility(View.VISIBLE);
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .connectTimeout(30, TimeUnit.SECONDS) // Тайм-аут на соединение
                .readTimeout(30, TimeUnit.SECONDS)    // Тайм-аут на чтение данных
                .writeTimeout(30, TimeUnit.SECONDS)   // Тайм-аут на запись данных
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl) // Замените на фактический URL вашего сервера
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        // Создайте сервис
        CallbackServiceWfp service = retrofit.create(CallbackServiceWfp.class);
        Logger.d(context, TAG, "getCardTokenWfp: ");
        String userEmail = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);

        // Выполните запрос
        Call<CallbackResponseWfp> call = service.handleCallbackWfpCardsId(
                context.getString(R.string.application),
                city,
                userEmail,
                "wfp"
        );
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseWfp> call, @NonNull Response<CallbackResponseWfp> response) {
                Logger.d(context, TAG, "onResponse: " + response.body());
                if (response.isSuccessful() && response.body() != null) {
                    CallbackResponseWfp callbackResponse = response.body();
                    List<CardInfo> cards = callbackResponse.getCards();
                    Logger.d(context, TAG, "onResponse: cards" + cards);
                    String tableName = MainActivity.TABLE_WFP_CARDS; // Например, "wfp_cards"


                    SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);


                    database.execSQL("DELETE FROM " + tableName + ";");

                    if (cards != null && !cards.isEmpty()) {
                        for (CardInfo cardInfo : cards) {
                            String masked_card = cardInfo.getMasked_card(); // Маска карты
                            String card_type = cardInfo.getCard_type(); // Тип карты
                            String bank_name = cardInfo.getBank_name(); // Название банка
                            String rectoken = cardInfo.getRectoken(); // Токен карты
                            String merchant = cardInfo.getMerchant(); //
                            String active = cardInfo.getActive();

                            Logger.d(context, TAG, "onResponse: card_token: " + rectoken);
                            ContentValues cv = new ContentValues();
                            cv.put("masked_card", masked_card);
                            cv.put("card_type", card_type);
                            cv.put("bank_name", bank_name);
                            cv.put("rectoken", rectoken);
                            cv.put("merchant", merchant);
                            cv.put("rectoken_check", active);
                            database.insert(MainActivity.TABLE_WFP_CARDS, null, cv);
                        }
                    }
                    database.close();
                    try {
                        cardViews();
                    } catch (MalformedURLException | UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseWfp> call, @NonNull Throwable t) {
                // Обработка ошибки запроса
                Logger.d(context, TAG, "onResponse: failure " + t);
                FirebaseCrashlytics.getInstance().recordException(t);
            }
        });
        progressBar.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onResume() {
        super.onResume();

        fragmentManager = getParentFragmentManager();
        webView = binding.webView;
        context = requireActivity();
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());
        }
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        btnCardLink  = binding.btnCardLink;
        btnOrder = binding.btnOrder;
        btnOrder.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
            }
            // Удаляем последний фрагмент из стека навигации и переходим к новому фрагменту
            SharedPreferencesHelper sharedPreferencesHelper = new SharedPreferencesHelper(context);
            sharedPreferencesHelper.saveValue("gps_upd", true);
            sharedPreferencesHelper.saveValue("gps_upd_address", true);

            MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());
        });

        AppCompatButton btnCallAdmin = binding.btnCallAdmin;
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, requireActivity()).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });
        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.swipeRefreshLayout);
        TextView svButton = root.findViewById(R.id.sv_button);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Скрываем TextView (⬇️) сразу после появления индикатора свайпа
            svButton.setVisibility(View.GONE);
            getCardTokenWfp();
//            // Выполняем необходимое действие (например, запуск новой активности)
//            MainActivity.navController.navigate(R.id.nav_card, null, new NavOptions.Builder()
//                    .setPopUpTo(R.id.nav_card, true)
//                    .build());
            // Эмулируем окончание обновления с задержкой
            swipeRefreshLayout.postDelayed(() -> {
                // Отключаем индикатор загрузки
                swipeRefreshLayout.setRefreshing(false);

                // Показываем TextView (⬇️) снова после завершения обновления
                svButton.setVisibility(View.VISIBLE);
            }, 500); // Задержка 500 мс
        });




        progressBar = binding.progressBar;
        progressBar.setVisibility(View.VISIBLE);
        textCard = binding.textCard;
        listView = binding.listView;
        if(textCard.getVisibility() == View.VISIBLE) {
            btnCardLink.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
        }
        btnCardLink.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);

            Logger.d(context, TAG, "onClick: " + pay_method);

            if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
            }

//
//            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
//                MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
//                        .setPopUpTo(R.id.nav_visicom, true)
//                        .build());
//            }

            else {
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(getActivity());

                MyBottomSheetCardVerificationWithOneUah bottomSheetDialogFragment = new MyBottomSheetCardVerificationWithOneUah();
                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                progressBar.setVisibility(View.GONE);
            }
        });
        try {
            cardViews();
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
//        getCardTokenWfp();

    }

    @SuppressLint("Range")
    private ArrayList<Map<String, String>> getCardMapsFromDatabase() {
        ArrayList<Map<String, String>> cardMaps = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        // Выполните запрос к таблице TABLE_FONDY_CARDS и получите данные
        Cursor cursor = database.query(MainActivity.TABLE_WFP_CARDS, null, null, null, null, null, null);
        Logger.d(context, TAG, "getCardMapsFromDatabase: card count: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> cardMap = new HashMap<>();
                cardMap.put("card_type", cursor.getString(cursor.getColumnIndex("card_type")));
                cardMap.put("bank_name", cursor.getString(cursor.getColumnIndex("bank_name")));
                cardMap.put("masked_card", cursor.getString(cursor.getColumnIndex("masked_card")));
                cardMap.put("rectoken", cursor.getString(cursor.getColumnIndex("rectoken")));
                cardMap.put("rectoken_check", cursor.getString(cursor.getColumnIndex("rectoken_check")));

                cardMaps.add(cardMap);
            } while (cursor.moveToNext());
        }
        cursor.close();

        database.close();

        return cardMaps;
    }

    // Интерфейс для обработки результата и ошибки
    public interface PaySystemCallback {
        void onPaySystemResult(String paymentCode);
        void onPaySystemFailure(String errorMessage);
    }


    @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}