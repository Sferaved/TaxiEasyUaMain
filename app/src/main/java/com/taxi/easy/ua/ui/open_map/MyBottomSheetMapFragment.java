package com.taxi.easy.ua.ui.open_map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.uxcam.UXCam;


public class MyBottomSheetMapFragment extends BottomSheetDialogFragment {

    AppCompatButton btn_ok, btn_no;

    private static final String TAG = "MyBottomSheetMapFragment";

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        View view = inflater.inflate(R.layout.map_layout, container, false);


        btn_ok = view.findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                startActivity(new Intent(requireActivity(), MainActivity.class));
            }
        });

        btn_no = view.findViewById(R.id.btn_no);
        btn_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });



        return view;
    }

   }

