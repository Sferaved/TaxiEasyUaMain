package com.taxi.easy.ua.utils.worker.utils;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.log.Logger;

public class OrderStatusDialogActivity extends AppCompatActivity {
    private static final String TAG = "OrderStatusDialogActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(
                ContextCompat.getColor(this, R.color.background_color_new)
        );

        Logger.d(this, TAG, "OrderStatusDialogActivity запущена");

        String message = getIntent().getStringExtra("bottomSheetMessage");
        if (message == null || message.isEmpty()) {
            message = getString(R.string.ex_st_2); // дефолтный текст
        }

        MyBottomSheetErrorFragment bottomSheetDialogFragment =
                new MyBottomSheetErrorFragment(message);

        bottomSheetDialogFragment.setOnDismissListener(() -> {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        });

        bottomSheetDialogFragment.show(getSupportFragmentManager(), "OrderStatusDialog");
    }
}
