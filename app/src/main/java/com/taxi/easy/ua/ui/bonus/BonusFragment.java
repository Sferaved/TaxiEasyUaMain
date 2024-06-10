package com.taxi.easy.ua.ui.bonus;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.NetworkChangeReceiver;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentBonusBinding;
import com.taxi.easy.ua.ui.finish.ApiClient;
import com.taxi.easy.ua.ui.finish.BonusResponse;
import com.taxi.easy.ua.ui.home.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.connect.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BonusFragment extends Fragment {

    private @NonNull FragmentBonusBinding binding;
    private AppCompatButton btnBonus, btnOrder;
    private TextView textView;
    private NetworkChangeReceiver networkChangeReceiver;
    private ProgressBar progressBar;
    private TextView text0;
    NavController navController;
    Activity context;
    @SuppressLint("SourceLockedOrientationActivity")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        context = requireActivity();
        
        navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
        if (!NetworkUtils.isNetworkAvailable(context)) {
            navController.navigate(R.id.nav_visicom);
        }
        binding = FragmentBonusBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        text0 =  binding.text0;
        networkChangeReceiver = new NetworkChangeReceiver();
        progressBar = binding.progressBar;
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();

        textView = binding.textBonus;
        String bonus = logCursor(MainActivity.TABLE_USER_INFO, context).get(5);
        if(bonus == null) {
            bonus = getString(R.string.upd_bonus_info);
        } else {
            textView.setText(getString(R.string.my_bonus) + bonus);
        }
        btnBonus  = binding.btnBonus;
        btnBonus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(context, R.id.nav_host_fragment_content_main);
                if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                    navController.navigate(R.id.nav_visicom);
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
            }
        });


        btnOrder = binding.btnOrder;
        btnOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Удаляем последний фрагмент из стека навигации и переходим к новому фрагменту
                navController.popBackStack();
                navController.navigate(R.id.nav_visicom);
            }
        });
    }

    String baseUrl = "https://m.easy-order-taxi.site";

    private void fetchBonus(String value, Context context) {
        btnOrder.setVisibility(View.INVISIBLE);
        String url = baseUrl + "/bonus/bonusUserShow/" + value + "/" + context.getString(R.string.application);
//        String url = baseUrl + "/bonus/bonusUserShow/" + value;
        Call<BonusResponse> call = ApiClient.getApiService().getBonus(url);
        Log.d("TAG", "fetchBonus: " + url);
        String bonusText = context.getString(R.string.my_bonus);
        call.enqueue(new Callback<BonusResponse>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onResponse(@NonNull Call<BonusResponse> call, @NonNull Response<BonusResponse> response) {

                BonusResponse bonusResponse = Objects.requireNonNull(response).body();
                if (response.isSuccessful()) {
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


                    Log.d("TAG", "onResponse: " + bonus);
                } else {
                    MyBottomSheetErrorFragment bottomSheetDialogFragment = new MyBottomSheetErrorFragment(getString(R.string.verify_internet));
                    bottomSheetDialogFragment.show(getChildFragmentManager(), bottomSheetDialogFragment.getTag());
                }
                btnOrder.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(@NonNull Call<BonusResponse> call, @NonNull Throwable t) {
                // Обработка ошибок сети или других ошибок
                btnOrder.setVisibility(View.VISIBLE);
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
}