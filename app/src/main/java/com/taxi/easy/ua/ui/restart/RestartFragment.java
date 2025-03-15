package com.taxi.easy.ua.ui.restart;


import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentRestartBinding;
import com.taxi.easy.ua.utils.ui.BackPressBlocker;

import java.util.ArrayList;
import java.util.List;

public class RestartFragment extends Fragment {

    private FragmentRestartBinding binding;
    AppCompatButton btn_restart, btn_help;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentRestartBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        // Включаем блокировку кнопки "Назад" Применяем блокировку кнопки "Назад"
        BackPressBlocker backPressBlocker = new BackPressBlocker();
        backPressBlocker.setBackButtonBlocked(true);
        backPressBlocker.blockBackButtonWithCallback(this);

        return root;
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onResume() {
        super.onResume();

        btn_restart = binding.btnClearFromText;
        btn_restart.setText(requireActivity().getString(R.string.try_again));
        btn_restart.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MainActivity.class);
            startActivity(intent);

        });

        btn_help = binding.btnHelp;
        btn_help.setOnClickListener(v -> {
            List<String> stringList = logCursor(MainActivity.CITY_INFO);
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = stringList.get(3);

            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

    }


    @SuppressLint("Range")
    public List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        @SuppressLint("Recycle") Cursor c = db.query(table, null, null, null, null, null, null);
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
        db.close();
        return list;
    }
}

