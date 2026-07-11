package com.taxi.easy.ua.utils.worker.utils;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.bottom_sheet.MyBottomSheetErrorFragment;
import com.taxi.easy.ua.utils.log.Logger;

public class DialogHostActivity extends AppCompatActivity {
    private static final String TAG = "DialogHostActivity";
    public static MyBottomSheetErrorFragment bottomSheetDialogFragment;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_host);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().getDecorView().setBackgroundColor(Color.TRANSPARENT);

        Logger.d(this, TAG, "DialogHostActivity запущена");

        String sentNotifyMessage = getString(R.string.sentNotifyMessage);
        bottomSheetDialogFragment =
                new MyBottomSheetErrorFragment(sentNotifyMessage);

        bottomSheetDialogFragment.setOnDismissListener(() -> {
            Logger.d(this, TAG, "BottomSheet закрыт — завершаем Activity");
            finish();
        });

        bottomSheetDialogFragment.show(getSupportFragmentManager(), "PushDialog");
    }
}
