package com.taxi.easy.ua.utils.bottom_sheet;

import static android.content.Context.MODE_PRIVATE;

import static com.taxi.easy.ua.MainActivity.navController;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;


public class MyBottomSheetErrorFragment extends BottomSheetDialogFragment {
    private static final String TAG = "MyBottomSheetErrorFragment";
    TextView textViewInfo;
    AppCompatButton btn_help, btn_ok;
    String errorMessage;

    public MyBottomSheetErrorFragment() {
    }

    public MyBottomSheetErrorFragment(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    // Публичный безаргументный конструктор

     
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.error_list_layout, container, false);

//        setCancelable(false);

        btn_help = view.findViewById(R.id.btn_help);
        btn_help.setOnClickListener(v -> {
            List<String> stringList = logCursor(MainActivity.CITY_INFO, requireActivity());
            Intent intent = new Intent(Intent.ACTION_DIAL);
            String phone = stringList.get(3);

            intent.setData(Uri.parse(phone));
            startActivity(intent);
        });

        btn_ok = view.findViewById(R.id.btn_ok);

        textViewInfo = view.findViewById(R.id.textViewInfo);
        Logger.d(getActivity(), TAG, "onCreateView:errorMessage " + errorMessage);
        if (errorMessage != null && !errorMessage.equals("null")) {
            textViewInfo.setText(errorMessage);
            if (errorMessage.equals(getString(R.string.verify_internet))
                || errorMessage.equals(getString(R.string.error_message))
            ) {
                btn_ok.setVisibility(View.GONE);
                textViewInfo.setOnClickListener(v -> {
                    SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    ContentValues cv = new ContentValues();

                    cv.put("email", "email");

                    // обновляем по id
                    database.update(MainActivity.TABLE_USER_INFO, cv, "id = ?",
                            new String[] { "1" });
                    database.close();
                    dismiss();
                    startActivity(new Intent(requireContext(), MainActivity.class));
                });
            } else if(errorMessage.equals(getString(R.string.server_error_connected))) {
                textViewInfo.setOnClickListener(v -> dismiss());
                btn_ok.setOnClickListener(v -> dismiss());
            } else if (errorMessage.equals(getString(R.string.order_to_cancel_true))){
                textViewInfo.setOnClickListener(v -> dismiss());
                btn_ok.setText(getString(R.string.order_to_cancel_review));
                btn_ok.setOnClickListener(v -> {
                    navController.navigate(R.id.nav_cancel, null, new NavOptions.Builder()
                            .setPopUpTo(R.id.nav_cancel, true)
                            .build());


                    dismiss();
                });
            } else if (errorMessage.equals(getString(R.string.black_list_message))){
                textViewInfo.setOnClickListener(v -> dismiss());
                btn_ok.setText(getString(R.string.ok_error));
                btn_ok.setOnClickListener(v -> {
                    NavDestination currentDestination = navController.getCurrentDestination();

                    if (currentDestination == null || currentDestination.getId() != R.id.nav_visicom) {
                        navController.navigate(R.id.nav_visicom, null, new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_visicom, true)
                                .build());
                    }

                    dismiss();
                });
            } else {
                btn_ok.setOnClickListener(v -> dismiss());
            }
        } else {
            textViewInfo.setText(getString(R.string.error_message));
            btn_ok.setText(getString(R.string.try_again));
            btn_ok.setOnClickListener(v -> startActivity(new Intent(requireContext(), MainActivity.class)));
        }

        return view;
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        NavDestination currentDestination = navController.getCurrentDestination();

        if (currentDestination == null) {
            navController.navigate(currentDestination.getId(), null, new NavOptions.Builder()
                    .setPopUpTo(currentDestination.getId(), true)
                    .build());
        }
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
}

