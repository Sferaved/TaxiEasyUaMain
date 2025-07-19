package com.taxi.easy.ua.ui.bonus;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.VISIBLE;
import static com.taxi.easy.ua.MainActivity.button1;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentBonusBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.BonusResponse;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.auth.FirebaseConsentManager;
import com.taxi.easy.ua.utils.connect.NetworkUtils;
import com.taxi.easy.ua.utils.log.Logger;
import com.uxcam.UXCam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BonusFragment extends Fragment {

    private static final String TAG = "BonusFragment";
    private FragmentBonusBinding binding;
    private AppCompatButton btnBonus, btnOrder, in_but;
    private TextView textView;
    private ProgressBar progressBar;
    private TextView text0;

    Activity context;
    FragmentManager fragmentManager;
    private AppCompatButton btnCallAdmin;
    View root;

    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);
        if(button1 != null) {
            button1.setVisibility(View.VISIBLE);
        }
        binding = FragmentBonusBinding.inflate(inflater, container, false);
        root = binding.getRoot();
        context = requireActivity();
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        fragmentManager = getParentFragmentManager();

        if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
            navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_restart, true)
                    .build());
        }
//
//        if (!NetworkUtils.isNetworkAvailable(context)) {
//            MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                    .setPopUpTo(R.id.nav_restart, true)
//                    .build());
//        }
        text0 =  binding.text0;

        progressBar = binding.progressBar;

        btnCallAdmin = binding.btnCallAdmin;
        btnCallAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, requireActivity()).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

        in_but = binding.btnInAccount;
        in_but.setOnClickListener(v -> {
            startFireBase();

        });
        btnBonus  = binding.btnBonus;
        btnOrder = binding.btnOrder;
        googleVerifyAccount();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        textView = binding.textBonus;
        String bonus = logCursor(MainActivity.TABLE_USER_INFO, context).get(5);
        if(bonus == null) {
            bonus = getString(R.string.upd_bonus_info);
            textView.setText(bonus);
        } else {
            textView.setText(getString(R.string.my_bonus) + bonus);
        }

        btnBonus.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
            } else {
                @SuppressLint("UseRequireInsteadOfGet")
                String email = logCursor(MainActivity.TABLE_USER_INFO, Objects.requireNonNull(context)).get(3);
                progressBar.setVisibility(View.VISIBLE);
                textView.setVisibility(View.GONE);
                binding.text0.setVisibility(View.GONE);
                binding.text7.setVisibility(View.GONE);
                btnBonus.setVisibility(View.GONE);

                fetchBonus(email, context);
            }
        });



        btnOrder.setOnClickListener(v -> {
            if (!NetworkUtils.isNetworkAvailable(requireContext()) && isAdded()) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
                navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_restart, true)
                        .build());
            }

//
//            if (!NetworkUtils.isNetworkAvailable(requireContext())) {
//
//                MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
//                        .setPopUpTo(R.id.nav_restart, true)
//                        .build());
//            }
            // Удаляем последний фрагмент из стека навигации и переходим к новому фрагменту

            sharedPreferencesHelperMain.saveValue("gps_upd", true);
            
            MainActivity.navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true) 
                        .build());
        });
    }

    String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");

    private void fetchBonus(String value, Context context) {
        btnOrder.setVisibility(View.INVISIBLE);
        baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        String url = baseUrl + "/bonus/bonusUserShow/" + value + "/" + context.getString(R.string.application);
//        String url = baseUrl + "/bonus/bonusUserShow/" + value;
        Call<BonusResponse> call = ApiClient.getApiService().getBonus(url);
        Logger.d(context, TAG, "fetchBonus: " + url);
        String bonusText = context.getString(R.string.my_bonus);
        call.enqueue(new Callback<BonusResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<BonusResponse> call, @NonNull Response<BonusResponse> response) {

                BonusResponse bonusResponse = Objects.requireNonNull(response).body();
                if (response.isSuccessful() && response.body() != null) {
                    progressBar.setVisibility(View.INVISIBLE);
                    assert bonusResponse != null;
                    String bonus = String.valueOf(bonusResponse.getBonus());
                    ContentValues cv = new ContentValues();
                    cv.put("bonus", bonus);
                    SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                            new String[] { "1" });
                    database.close();

                    textView.setText(bonusText + bonus);
                    textView.setVisibility(View.VISIBLE);
                    text0.setVisibility(View.GONE);
                    text0.setText(R.string.bonus_upd_mes);


                    Logger.d(context, TAG, "onResponse: " + bonus);
                } else {
                    MainActivity.navController.navigate(R.id.nav_restart, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_restart, true)
                            .build());
                }
                btnOrder.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(@NonNull Call<BonusResponse> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                btnOrder.setVisibility(View.VISIBLE);
                FirebaseCrashlytics.getInstance().recordException(t);
                // Дополнительная обработка ошибки
            }
        });
    }


    @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = database.query(table, null, null, null, null, null, null);
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

    private void googleVerifyAccount() {
        FirebaseConsentManager consentManager = new FirebaseConsentManager(requireActivity());

        consentManager.checkUserConsent(new FirebaseConsentManager.ConsentCallback() {
            @Override
            public void onConsentValid() {
                Logger.d(context, TAG, "Согласие пользователя действительное.");
                visibility (View.VISIBLE);
            }

            @Override
            public void onConsentInvalid() {
                Logger.d(context, TAG, "Согласие пользователя НЕ действительное.");
                visibility (View.INVISIBLE);
            }
        });
    }
    @SuppressLint("SetTextI18n")
    private void visibility (int visible) {
        if (visible == View.INVISIBLE) {
            in_but.setVisibility(VISIBLE);
            text0.setText(R.string.in_google_bonus);
        } else {
            in_but.setVisibility(View.GONE);
            text0.setText(R.string.bonus_text_0);
        }
        btnBonus.setVisibility(visible);
    }
    private void startFireBase() {
        Toast.makeText(context, R.string.account_verify, Toast.LENGTH_SHORT).show();
        startSignIn();
    }

    private void startSignIn() {
        try {
            Logger.d(context, TAG, "run: ");
            List<AuthUI.IdpConfig> providers = Collections.singletonList(
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            Intent signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build();

            signInLauncher.launch(signInIntent);
        } catch (Exception e) {
            Logger.e(context, TAG, "Exception during sign-in launch " + e);
            FirebaseCrashlytics.getInstance().recordException(e);
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }
    }
    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(

            new FirebaseAuthUIActivityResultContract(),
            this::onSignInResult
    );

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        ContentValues cv = new ContentValues();
        Logger.d(context, TAG, "onSignInResult: ");

        // Попробуем выполнить вход
        try {
            int resultCode = result.getResultCode();
            Logger.d(context, TAG, "onSignInResult: result.getResultCode() " + resultCode);
            if (result.getResultCode() == Activity.RESULT_OK) {
                Logger.d(context,"SignIn", "Успешная авторизация!");
                NavController navController = MainActivity.navController;
                navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_visicom, true)
                        .build());
            } else {
                Toast.makeText(context, getString(R.string.firebase_error), Toast.LENGTH_SHORT).show();
                Logger.w(context,"SignIn", "Авторизация отменена или ошибка. Код: " + result.getResultCode());
            }

        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
}