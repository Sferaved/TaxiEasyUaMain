package com.taxi.easy.ua.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import  com.taxi.easy.ua.MainActivity;
import  com.taxi.easy.ua.R;


public class MyGeoDialogFragment extends BottomSheetDialogFragment {
    public TextView geoText;
    Button button;
    public static MyGeoDialogFragment newInstance(String fromGeo) {
        MyGeoDialogFragment fragment = new MyGeoDialogFragment();
        Bundle args = new Bundle();
        args.putString("from_geo", fromGeo);
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.geo_first_layout, container, false);
        geoText = view.findViewById(R.id.textGeo);

        // Получение значения from_geo из аргументов
        Bundle args = getArguments();
        if (args != null) {
            String fromGeo = args.getString("from_geo");
            if (fromGeo != null) {
                geoText.setText(fromGeo);
            }
        }

        geoText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        button = view.findViewById(R.id.change);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), MainActivity.class));
            }
        });
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}


