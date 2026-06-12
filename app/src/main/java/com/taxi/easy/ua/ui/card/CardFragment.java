package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.button1;
import static com.taxi.easy.ua.androidx.startup.MyApplication.getCurrentActivity;
import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentCardBinding;
import com.taxi.easy.ua.ui.card.unlink.UnlinkApi;
import com.taxi.easy.ua.ui.fondy.payment.UniqueNumberGenerator;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseSetActivCardWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackResponseWfp;
import com.taxi.easy.ua.ui.wfp.token.CallbackServiceWfp;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.keys.FirestoreHelper;
import com.taxi.easy.ua.utils.log.Logger;
import com.taxi.easy.ua.utils.payment.PaymentTypeHelper;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.widget.ImageView;
import com.taxi.easy.ua.utils.network.RetryInterceptor;
import com.taxi.easy.ua.utils.phone_state.PhoneCallHelper;
import com.taxi.easy.ua.utils.worker.utils.WfpUtils;
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
import com.taxi.easy.ua.utils.db.CursorReadHelper;

public class CardFragment extends Fragment {

    private FragmentCardBinding binding;
    public static MaterialButton btnCardLink, btnOrder;

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
    View root;

    // WebView элементы
    private ConstraintLayout webViewContainer;
    private NestedScrollView mainContent;
    private Toolbar webViewToolbar;
    private ProgressBar webViewProgressBar;
    private boolean isWebViewVisible = false;
    private LinearLayout emptyStateContainer;
    private ImageView image_text_card;
    FragmentManager fragmentManager;
    FirestoreHelper firestoreHelper;
    private PaymentCardsAdapter cardsAdapter;
    private RecyclerView cardsRecycler;
    private View rowCashView;
    private View rowGooglePayView;
    private View rowAddCardView;
    private ImageView cashCheckView;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);
        if(button1 != null) {
            button1.setVisibility(VISIBLE);
        }
        binding = FragmentCardBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        context = requireActivity();
        firestoreHelper = new FirestoreHelper(context);
        fragmentManager = getParentFragmentManager();
        binding.btnClose.setOnClickListener(v -> {
            if (MainActivity.navController != null) {
                MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
            }
        });
        checkCardPaymentForCity();
        return root;
    }
    private void checkCardPaymentForCity() {
        List<String> stringList = logCursor(MainActivity.CITY_INFO, context);
        String cityName = stringList.get(1);

        firestoreHelper.getCardPaymentKeyForCity(
                new FirestoreHelper.OnCardPaymentKeyFetchedListener() {
                    @Override
                    public void onSuccess(Boolean cardPaymentEnabled) {
                        Logger.d(context, TAG, "Успешно получено значение: " + cardPaymentEnabled);

                        if (cardPaymentEnabled) {
                            Logger.d(context, TAG, "Оплата картой ДОСТУПНА для города " + cityName);
                            try {
                                setupPaymentScreen();
                            } catch (MalformedURLException | UnsupportedEncodingException e) {
                                Logger.e(context, TAG, "setupPaymentScreen: " + e.getMessage());
                            }
                        } else {
                            Logger.d(context, TAG, "Оплата картой НЕДОСТУПНА для города " + cityName);
                            String message = context.getString(R.string.card_payment_false);
                            MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(message);
                            bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                        }
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
    private void setupPaymentScreen() throws MalformedURLException, UnsupportedEncodingException {
        if (isWebViewVisible || binding == null) {
            return;
        }

        pay_method = PaymentTypeHelper.getPaymentType(context);
        table = MainActivity.TABLE_WFP_CARDS;

        if (cardsRecycler == null) {
            cardsRecycler = binding.cardsRecycler;
            cardsRecycler.setLayoutManager(new LinearLayoutManager(context));
            cardsAdapter = new PaymentCardsAdapter();
            cardsRecycler.setAdapter(cardsAdapter);
            cardsAdapter.setListener(new PaymentCardsAdapter.Listener() {
                @Override
                public void onCardSelected(@NonNull Map<String, String> card, int position) {
                    String rectoken = card.get("rectoken");
                    if (rectoken != null) {
                        CustomCardAdapter.rectoken = rectoken;
                        CustomCardAdapter adapter = new CustomCardAdapter(
                                context, getCardMapsFromDatabase(), table, pay_method);
                        adapter.selectCard(rectoken);
                    }
                    PaymentTypeHelper.setCard(context);
                    pay_method = PaymentTypeHelper.CARD;
                    refreshPaymentSelectionState();
                }

                @Override
                public void onCardDelete(@NonNull Map<String, String> card, int position) {
                    confirmDeleteCard(card);
                }
            });
        }

        rowCashView = binding.rowCash.getRoot();
        rowAddCardView = binding.rowAddCard.getRoot();
        cashCheckView = rowCashView.findViewById(R.id.paymentRowCheck);
        ImageView cashIcon = rowCashView.findViewById(R.id.paymentRowIcon);
        TextView cashTitle = rowCashView.findViewById(R.id.paymentRowTitle);
        TextView cashSubtitle = rowCashView.findViewById(R.id.paymentRowSubtitle);
        cashIcon.setImageResource(R.drawable.ic_cash_payment);
        cashTitle.setText(R.string.nal_payment);
        cashSubtitle.setText(R.string.payment_cash_subtitle);
        cashSubtitle.setVisibility(View.VISIBLE);
        rowCashView.setOnClickListener(v -> {
            PaymentTypeHelper.setCash(context);
            pay_method = PaymentTypeHelper.NAL;
            refreshPaymentSelectionState();
        });

        rowGooglePayView = binding.rowGooglePay.getRoot();
        ImageView googlePayIcon = rowGooglePayView.findViewById(R.id.paymentRowIcon);
        TextView googlePayTitle = rowGooglePayView.findViewById(R.id.paymentRowTitle);
        TextView googlePaySubtitle = rowGooglePayView.findViewById(R.id.paymentRowSubtitle);
        googlePayIcon.setImageResource(R.drawable.ic_google_pay);
        googlePayTitle.setText(R.string.btn_pay_google);
        googlePaySubtitle.setText(R.string.payment_google_pay_subtitle);
        googlePaySubtitle.setVisibility(View.VISIBLE);
        rowGooglePayView.setOnClickListener(v -> {
            PaymentTypeHelper.setGooglePay(context);
            pay_method = PaymentTypeHelper.GOOGLE_PAY;
            refreshPaymentSelectionState();
        });

        ImageView addIcon = rowAddCardView.findViewById(R.id.paymentRowIcon);
        TextView addTitle = rowAddCardView.findViewById(R.id.paymentRowTitle);
        addIcon.setImageResource(R.drawable.ic_add_card);
        addTitle.setText(R.string.payment_add_card);
        rowAddCardView.findViewById(R.id.paymentRowSubtitle).setVisibility(View.GONE);
        rowAddCardView.findViewById(R.id.paymentRowCheck).setVisibility(View.GONE);
        rowAddCardView.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                Toast.makeText(context, R.string.network_no_internet, Toast.LENGTH_LONG).show();
                return;
            }
            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(getActivity());
            Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)
                    .navigate(R.id.nav_card_ver, null, new NavOptions.Builder().build());
        });

        cardViews();
    }

    private void refreshCashSelection(boolean selected) {
        if (rowCashView != null) {
            View check = rowCashView.findViewById(R.id.paymentRowCheck);
            if (check != null) {
                check.setVisibility(selected ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void refreshGooglePaySelection(boolean selected) {
        if (rowGooglePayView != null) {
            View check = rowGooglePayView.findViewById(R.id.paymentRowCheck);
            if (check != null) {
                check.setVisibility(selected ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void refreshPaymentSelectionState() {
        String paymentType = PaymentTypeHelper.getPaymentType(context);
        boolean cashSelected = PaymentTypeHelper.isCash(paymentType);
        boolean googlePaySelected = PaymentTypeHelper.isGooglePay(paymentType);
        refreshCashSelection(cashSelected);
        refreshGooglePaySelection(googlePaySelected);

        if (cardsAdapter != null) {
            ArrayList<Map<String, String>> cardMaps = getCardMapsFromDatabase();
            int selectedCardPos = (cashSelected || googlePaySelected)
                    ? -1
                    : findSelectedCardPosition(cardMaps);
            cardsAdapter.submitList(cardMaps, selectedCardPos);
        }
        pay_method = paymentType;
    }

    private void confirmDeleteCard(@NonNull Map<String, String> card) {
        if (binding == null || !isAdded()) {
            return;
        }
        String rectoken = card.get("rectoken");
        if (rectoken == null || rectoken.isEmpty()) {
            return;
        }
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(context, R.string.network_no_internet, Toast.LENGTH_LONG).show();
            return;
        }
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_delete_card, null);
        TextView maskedView = dialogView.findViewById(R.id.dialogCardMasked);
        String masked = card.get("masked_card");
        maskedView.setText(masked != null ? masked : "");

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialogView.findViewById(R.id.dialogBtnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.dialogBtnDelete).setOnClickListener(v -> {
            dialog.dismiss();
            deleteCardToken(rectoken);
        });
        dialog.show();
    }

    private void deleteCardToken(@NonNull String rectoken) {
        progressBar.setVisibility(VISIBLE);
        WfpUtils.prepareForCardDeletion(context, rectoken);
        refreshCardUiAfterSync();

        String url = baseUrl != null ? baseUrl
                : (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        UnlinkApi apiService = retrofit.create(UnlinkApi.class);
        apiService.deleteCardToken(rectoken).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<CallbackResponseSetActivCardWfp> call,
                                   @NonNull Response<CallbackResponseSetActivCardWfp> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Logger.d(context, TAG, "deleteCardToken: ok rectoken=" + rectoken);
                    if (isAdded()) {
                        Toast.makeText(context, R.string.un_link_token, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Logger.d(context, TAG, "deleteCardToken: failed code=" + response.code());
                }
                syncCardsAfterDelete();
            }

            @Override
            public void onFailure(@NonNull Call<CallbackResponseSetActivCardWfp> call, @NonNull Throwable t) {
                Logger.d(context, TAG, "deleteCardToken: failure " + t.getMessage());
                FirebaseCrashlytics.getInstance().recordException(t);
                syncCardsAfterDelete();
            }
        });
    }

    private void syncCardsAfterDelete() {
        String city = logCursor(MainActivity.CITY_INFO, context).get(1);
        WfpUtils.fetchCardTokenWfpAsync(city, context, success -> refreshCardUiAfterSync());
    }

    private void refreshCardUiAfterSync() {
        if (!isAdded() || binding == null) {
            return;
        }
        progressBar.setVisibility(View.GONE);
        try {
            cardViews();
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private void cardViews() throws MalformedURLException, UnsupportedEncodingException {
        if (binding == null || !isAdded()) {
            return;
        }
        if (isWebViewVisible) {
            mainContent.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(VISIBLE);
        email = logCursor(MainActivity.TABLE_USER_INFO, context).get(3);
        ArrayList<Map<String, String>> cardMaps = getCardMapsFromDatabase();

        refreshPaymentSelectionState();

        boolean hasCards = !cardMaps.isEmpty();
        if (cardsRecycler != null) {
            cardsRecycler.setVisibility(hasCards ? View.VISIBLE : View.GONE);
        }
        if (binding.dividerBeforeAddCard != null) {
            binding.dividerBeforeAddCard.setVisibility(hasCards ? View.VISIBLE : View.GONE);
        }
        emptyStateContainer.setVisibility(hasCards ? View.GONE : View.VISIBLE);
        textCard.setVisibility(hasCards ? View.GONE : View.VISIBLE);
        if (!hasCards) {
            textCard.setText(R.string.no_cards);
            String currentType = PaymentTypeHelper.getPaymentType(context);
            if (!PaymentTypeHelper.isCash(currentType) && !PaymentTypeHelper.isGooglePay(currentType)) {
                PaymentTypeHelper.setCash(context);
                refreshPaymentSelectionState();
            }
        }
        progressBar.setVisibility(View.GONE);
    }

    private int findSelectedCardPosition(ArrayList<Map<String, String>> cardMaps) {
        for (int i = 0; i < cardMaps.size(); i++) {
            String check = cardMaps.get(i).get("rectoken_check");
            if ("1".equals(check)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Инициализация WebView
     */
    private void initWebView() {
        webView = binding.webView;
        webViewContainer = binding.webViewContainer;
        mainContent = binding.mainContent;
        webViewToolbar = binding.webViewToolbar;
        webViewProgressBar = binding.webViewProgressBar;

        // Настройка WebView
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        // WebViewClient для обработки навигации
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                webViewProgressBar.setVisibility(VISIBLE);
                Logger.d(context, TAG, "WebView loading started: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                webViewProgressBar.setVisibility(View.GONE);
                Logger.d(context, TAG, "WebView loading finished: " + url);

                // Проверка завершения платежа
                if (url.contains("success") || url.contains("thankyou") || url.contains("approved")) {
                    handlePaymentSuccess();
                } else if (url.contains("failure") || url.contains("error") || url.contains("cancel")) {
                    handlePaymentFailure();
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                webViewProgressBar.setVisibility(View.GONE);
                Logger.e(context, TAG, "WebView error: " + error.getDescription());
                showWebViewError(getString(R.string.webview_error_loading));
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("tel:")) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } else if (url.startsWith("mailto:")) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });

        // WebChromeClient для прогресса
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                webViewProgressBar.setProgress(newProgress);
            }
        });

        // Настройка тулбара
        webViewToolbar.setNavigationOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                hideWebView();
            }
        });
    }

    /**
     * Показать WebView
     */
    private void showWebView(String url) {
        if (webView == null) {
            initWebView();
        }

        isWebViewVisible = true;
        webViewContainer.setVisibility(VISIBLE);
        mainContent.setVisibility(View.GONE);

        // Загрузка URL
        webView.loadUrl(url);
        Logger.d(context, TAG, "Loading WebView URL: " + url);
    }

    /**
     * Скрыть WebView
     */
    private void hideWebView() {
        isWebViewVisible = false;
        webViewContainer.setVisibility(View.GONE);
        mainContent.setVisibility(VISIBLE);

        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
        }

        // Обновить список карт
        try {
            cardViews();
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            Logger.e(context, TAG, "Error updating card views: " + e.getMessage());
        }
    }

    /**
     * Обработка успешного платежа
     */
    private void handlePaymentSuccess() {
        Logger.d(context, TAG, "Payment successful via WebView");

        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(context, getString(R.string.payment_success), Toast.LENGTH_SHORT).show();
                // Через 2 секунды скрыть WebView и обновить карты
                webView.postDelayed(() -> {
                    hideWebView();
                    getCardTokenWfp();
                }, 2000);
            });
        }
    }

    /**
     * Обработка неудачного платежа
     */
    private void handlePaymentFailure() {
        Logger.d(context, TAG, "Payment failed via WebView");

        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(context, getString(R.string.payment_failed), Toast.LENGTH_SHORT).show();
                hideWebView();
            });
        }
    }

    /**
     * Показать ошибку WebView
     */
    private void showWebViewError(String message) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                hideWebView();
            });
        }
    }

    /**
     * Загрузить платежную страницу
     */
    private void loadPaymentPage() {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            showWebViewError(getString(R.string.verify_internet));
            return;
        }

        try {
            MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(getActivity());

            // Формирование URL для привязки карты
            String paymentUrl = buildPaymentUrl();

            // Показать WebView
            showWebView(paymentUrl);

        } catch (Exception e) {
            Logger.e(context, TAG, "Error loading payment page: " + e.getMessage());
            showWebViewError(getString(R.string.payment_page_error));
        }
    }

    /**
     * Построить URL платежной страницы
     */
    private String buildPaymentUrl() {
        List<String> userInfo = logCursor(MainActivity.TABLE_USER_INFO, context);
        List<String> cityInfo = logCursor(MainActivity.CITY_INFO, context);

        Map<String, String> params = new HashMap<>();
        params.put("order_id", MainActivity.order_id);
        params.put("email", userInfo.size() > 3 ? userInfo.get(3) : "");
        params.put("city", cityInfo.size() > 1 ? cityInfo.get(1) : "");
        params.put("app", getString(R.string.application));
        params.put("payment_system", "wfp");

        StringBuilder urlBuilder = new StringBuilder(baseUrl + "/payment/card-binding?");
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                urlBuilder.append("&");
            }
            urlBuilder.append(entry.getKey())
                    .append("=")
                    .append(Uri.encode(entry.getValue()));
            first = false;
        }

        return urlBuilder.toString();
    }

    private void getCardTokenWfp() {
        String city = logCursor(MainActivity.CITY_INFO, context).get(1);
        if (!WfpUtils.isCityValidForCardFetch(city)) {
            progressBar.setVisibility(View.GONE);
            return;
        }
        progressBar.setVisibility(VISIBLE);
        WfpUtils.fetchCardTokenWfpAsync(city, context, success -> refreshCardUiAfterSync());
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onResume() {
        super.onResume();

        context = requireActivity();
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

        // Проверка сети
        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());
        }

        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Получение элементов
        btnCardLink = binding.btnCardLink;
        btnOrder = binding.btnOrder;
        FloatingActionButton btnCallAdmin = binding.btnCallAdmin;

        // Инициализация WebView
        initWebView();

        // Настройка элементов
        progressBar = binding.progressBar;
        textCard = binding.textCard;
        emptyStateContainer = binding.emptyStateContainer;
        image_text_card = binding.imageTextCard;

        listView = binding.listView;

        // Кнопка Заказа
        btnOrder.setOnClickListener(v -> {
            if (NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                Toast.makeText(requireActivity(), R.string.network_no_internet, Toast.LENGTH_LONG).show();
                Logger.w(context, TAG, "NO INTERNET - Showing toast message");
            }
            sharedPreferencesHelperMain.saveValue("gps_upd", true);
            MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_visicom, true)
                    .build());
        });

        // Кнопка Звонка
        btnCallAdmin.setOnClickListener(v -> {
            PhoneCallHelper.callWithFallback(() -> {
                List<String> stringListPhone = logCursor(MainActivity.CITY_INFO, requireContext());
                return stringListPhone.size() > 3 ? stringListPhone.get(3) : "";
            });
//            Intent intent = new Intent(Intent.ACTION_DIAL);
//            String phone = logCursor(MainActivity.CITY_INFO, requireActivity()).get(3);
//            intent.setData(Uri.parse(phone));
//            startActivity(intent);
        });

        // Кнопка Привязки карты
//        btnCardLink.setOnClickListener(v -> {
//            progressBar.setVisibility(View.VISIBLE);
//            Logger.d(context, TAG, "onClick: " + pay_method);
//
//            if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
//                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
//                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
//                        .setPopUpTo(R.id.nav_visicom, true)
//                        .build());
//            } else {
//                // Вместо перехода к другому фрагменту, открываем WebView
//                loadPaymentPage();
//            }
//            progressBar.setVisibility(View.GONE);
//        });
        btnCardLink.setOnClickListener(v -> {
            progressBar.setVisibility(VISIBLE);

            Logger.d(context, TAG, "onClick: " + pay_method);

            if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
            }


            else {
                MainActivity.order_id = UniqueNumberGenerator.generateUniqueNumber(getActivity());

                NavController navController = Navigation.findNavController(getCurrentActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_card_ver, null, new NavOptions.Builder().build());

//                MyBottomSheetCardVerificationWithOneUah bottomSheetDialogFragment = new MyBottomSheetCardVerificationWithOneUah();
//                bottomSheetDialogFragment.show(fragmentManager, bottomSheetDialogFragment.getTag());
                progressBar.setVisibility(View.GONE);
            }
        });
        // SwipeRefreshLayout
        SwipeRefreshLayout swipeRefreshLayout = binding.swipeRefreshLayout;
        TextView svButton = binding.svButton;

        swipeRefreshLayout.setOnRefreshListener(() -> {
            svButton.setVisibility(View.GONE);

            if (isWebViewVisible && webView != null) {
                webView.reload();
            } else {
                getCardTokenWfp();
            }

            swipeRefreshLayout.postDelayed(() -> {
                swipeRefreshLayout.setRefreshing(false);
                svButton.setVisibility(VISIBLE);
            }, 500);
        });

        try {
            if (getCardMapsFromDatabase().isEmpty()) {
                String city = logCursor(MainActivity.CITY_INFO, context).get(1);
                if (WfpUtils.isCityValidForCardFetch(city) && NetworkUtils.isNetworkAvailable(requireContext())) {
                    getCardTokenWfp();
                } else {
                    setupPaymentScreen();
                }
            } else {
                setupPaymentScreen();
            }
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            Logger.e(context, TAG, "onResume setupPaymentScreen: " + e.getMessage());
        }
    }

    @SuppressLint("Range")
    private ArrayList<Map<String, String>> getCardMapsFromDatabase() {
        ArrayList<Map<String, String>> cardMaps = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(MainActivity.TABLE_WFP_CARDS, null, null, null, null, null, null);
        Logger.d(context, TAG, "getCardMapsFromDatabase: card count: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> cardMap = new HashMap<>();
                cardMap.put("card_type", CursorReadHelper.getString(cursor, "card_type"));
                cardMap.put("bank_name", CursorReadHelper.getString(cursor, "bank_name"));
                cardMap.put("masked_card", CursorReadHelper.getString(cursor, "masked_card"));
                cardMap.put("rectoken", CursorReadHelper.getString(cursor, "rectoken"));
                cardMap.put("rectoken_check", CursorReadHelper.getString(cursor, "rectoken_check"));
                cardMaps.add(cardMap);
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();

        return cardMaps;
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
                    str = str.concat(cn + " = " + CursorReadHelper.getString(c, cn) + "; ");
                    list.add(CursorReadHelper.getString(c, cn));
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
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        binding = null;
    }
}