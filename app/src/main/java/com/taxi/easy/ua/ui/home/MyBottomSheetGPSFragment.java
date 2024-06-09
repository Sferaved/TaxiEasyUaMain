package com.taxi.easy.ua.ui.home;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;


public class MyBottomSheetGPSFragment extends BottomSheetDialogFragment {
    public MyBottomSheetGPSFragment() {
        // Пустой конструктор обязателен для фрагментов
    }
    AppCompatButton btn_ok, btn_no;

    String errorMessage;

    public MyBottomSheetGPSFragment(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    @SuppressLint("MissingInflatedId")
     
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gps_layout, container, false);
        if (errorMessage.equals(getString(R.string.location_on))) {
            TextView text_message = view.findViewById(R.id.text_message);
            text_message.setText(errorMessage);
        }


        btn_ok = view.findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if(HomeFragment.progressBar != null) {
                    HomeFragment.progressBar.setVisibility(View.INVISIBLE);
                }
                if(VisicomFragment.progressBar != null) {
                    VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
                }
                Log.d("TAG", "onClick: " + errorMessage);
                if (errorMessage.equals(getString(R.string.location_on))) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        // Provide a fallback option, such as showing a message to the user

                    }

                } else {
                    requireActivity().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }

            }
        });

        btn_no = view.findViewById(R.id.btn_no);
        btn_no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if(HomeFragment.progressBar != null) {
                    HomeFragment.progressBar.setVisibility(View.INVISIBLE);
                }
                if(VisicomFragment.progressBar != null) {
                    VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
        if(HomeFragment.progressBar != null) {
            HomeFragment.progressBar.setVisibility(View.INVISIBLE);
        }
        if(VisicomFragment.progressBar != null) {
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }
        return view;
    }

}

