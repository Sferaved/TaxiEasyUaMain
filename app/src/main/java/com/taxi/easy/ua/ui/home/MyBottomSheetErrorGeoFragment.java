package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.open_map.OpenStreetMapActivity;

import java.util.ArrayList;
import java.util.List;


public class MyBottomSheetErrorGeoFragment extends BottomSheetDialogFragment {
    TextView textViewInfo;
    AppCompatButton btn_help, btn_geo;
    String errorMessage;

    public MyBottomSheetErrorGeoFragment(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.error_geo_layout, container, false);

        btn_help = view.findViewById(R.id.btn_help);
        btn_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> stringList = logCursor(MainActivity.CITY_INFO, getContext());
                Intent intent = new Intent(Intent.ACTION_DIAL);
                String phone = stringList.get(3);

                intent.setData(Uri.parse(phone));
                startActivity(intent);
            }
        });

        btn_geo = view.findViewById(R.id.btn_geo);
        btn_geo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                    checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);

                    HomeFragment.btnGeo.setVisibility(View.VISIBLE);
                }  else {
                    HomeFragment.progressBar.setVisibility(View.INVISIBLE);
                    HomeFragment.btnGeo.setVisibility(View.INVISIBLE);
                    dismiss();
                    Intent intent = new Intent(requireActivity(), OpenStreetMapActivity.class);
                    startActivity(intent);
                }
            }
        });
        textViewInfo = view.findViewById(R.id.textViewInfo);
        textViewInfo.setText(errorMessage);

        return view;
    }

    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(requireActivity(), permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{permission}, requestCode);

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
        return list;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        HomeFragment.progressBar.setVisibility(View.INVISIBLE);
    }
}

