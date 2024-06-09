package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.connect.NetworkUtils;

import java.util.ArrayList;
import java.util.List;


public class MyBottomSheetErrorCardFragment extends BottomSheetDialogFragment {
    TextView textViewInfo;
    AppCompatButton btn_help;
    String errorMessage;

    public MyBottomSheetErrorCardFragment(String errorMessage) {
        this.errorMessage = errorMessage;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.error_list_layout, container, false);
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);

        btn_help = view.findViewById(R.id.btn_help);
        btn_help.setText(getString(R.string.order));
        btn_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.nav_visicom);
            }
        });
        textViewInfo = view.findViewById(R.id.textViewInfo);
        textViewInfo.setText(errorMessage);


        return view;
    }

    @SuppressLint("Range")
    private List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(table, null, null, null, null, null, null);
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
        assert c != null;
        c.close();
        return list;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}

