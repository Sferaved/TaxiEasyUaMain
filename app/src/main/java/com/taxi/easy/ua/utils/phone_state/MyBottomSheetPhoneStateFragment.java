package com.taxi.easy.ua.utils.phone_state;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.ui.visicom.VisicomFragment;
import com.taxi.easy.ua.utils.log.Logger;

public class MyBottomSheetPhoneStateFragment extends BottomSheetDialogFragment {

    AppCompatButton btn_ok, btn_no;
    TextView text_message;
    private final String TAG = "MyBottomSheetPhoneStateFragment";

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.gps_layout, container, false);

        text_message= view.findViewById(R.id.text_message);
        text_message.setText(R.string.id_phone_text);

        btn_ok = view.findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(v -> {
            dismiss();
            checkPermission();
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

        if(VisicomFragment.progressBar != null) {
            VisicomFragment.progressBar.setVisibility(View.INVISIBLE);
        }
        return view;
    }
    private static final int PERMISSION_REQUEST_READ_PHONE_STATE = 1;

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED) {
            performPhoneStateOperation();


        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSION_REQUEST_READ_PHONE_STATE);
        }
    }

    // Обработка ответа пользователя на запрос разрешения
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Logger.d(getActivity(), TAG, "onRequestPermissionsResult: " + requestCode);
        if (requestCode == PERMISSION_REQUEST_READ_PHONE_STATE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение получено, можно выполнять операции, требующие доступа к состоянию телефона
                Logger.d(getActivity(), TAG, "onRequestPermissionsResult: Разрешение получено");
                performPhoneStateOperation();
            }
        }
    }

    // Проверка наличия разрешения и выполнение операции
    private void performPhoneStateOperation() {
        TelephonyManager telephonyManager = (TelephonyManager) requireActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Начиная с Android 10, IMEI может быть недоступен без разрешения READ_PHONE_STATE
                if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity(), "IMEI недоступен без разрешения", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            String imei = telephonyManager.getImei();
            if (imei != null) {
                // Делаем что-то с IMEI
                Logger.d(getActivity(), TAG, "performPhoneStateOperation: IMEI: " + imei);
                Toast.makeText(requireActivity(), "IMEI: " + imei, Toast.LENGTH_SHORT).show();
            } else {
                // IMEI недоступен
                Logger.d(getActivity(), TAG, "performPhoneStateOperation: IMEI недоступен");
                Toast.makeText(requireActivity(), "IMEI недоступен", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Устройство не поддерживает функции телефона
            Toast.makeText(requireActivity(), "Функции телефона недоступны", Toast.LENGTH_SHORT).show();
        }
    }

}

