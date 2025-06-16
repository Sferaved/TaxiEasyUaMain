package com.taxi.easy.ua.ui.exit;


import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.databinding.FragmentAnrBinding;
import com.taxi.easy.ua.utils.connect.NetworkMonitor;

import java.util.ArrayList;
import java.util.List;


public class AnrFragment extends Fragment {

    private static final String TAG = "AnrFragment";
    AppCompatButton btn_enter;
    AppCompatButton btnCallAdmin;
    AppCompatButton btn_exit;
    AppCompatButton btn_ok;

    private NetworkMonitor networkMonitor;
    private FragmentAnrBinding binding;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAnrBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        
        btn_enter = binding.btnEnter;
        btnCallAdmin = binding.btnCallAdmin;
        btn_exit = binding.btnExit; 

        // Регистрируем обработчик нажатия кнопки "назад"
        requireActivity().getOnBackPressedDispatcher().addCallback(requireActivity(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Блокируем действие кнопки назад и вызываем вашу логику
                closeApplication();
            }
        });
        btn_enter.setOnClickListener(view15 -> {
            startActivity(new Intent(requireActivity(), MainActivity.class));
        });
        btnCallAdmin.setOnClickListener(view16 -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = logCursor(MainActivity.CITY_INFO, requireActivity()).get(3);
            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });
        btn_exit.setOnClickListener(view16 -> {
            closeApplication();
        });

        networkMonitor = new NetworkMonitor(requireActivity());
        networkMonitor.startMonitoring(requireActivity());
        return root;
    }

    private void closeApplication() {
        // Полный выход из приложения
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
        }
        requireActivity().finishAffinity();
        System.exit(0);
    }

    @SuppressLint("Range")
    public List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = db.query(table, null, null, null, null, null, null);
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
        db.close();
        return list;
    }



}

