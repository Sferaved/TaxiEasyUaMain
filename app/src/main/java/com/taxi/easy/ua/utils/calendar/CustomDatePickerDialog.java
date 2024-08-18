package com.taxi.easy.ua.utils.calendar;

import android.app.Dialog;
import android.content.Context;
import android.widget.NumberPicker;

import androidx.appcompat.widget.AppCompatButton;

import com.taxi.easy.ua.R;

import java.util.Calendar;

public class CustomDatePickerDialog {

    private Context context;
    private Calendar calendar;
    private OnDateSetListener onDateSetListener;

    public interface OnDateSetListener {
        void onDateSet(Calendar calendar);
    }

    public CustomDatePickerDialog(Context context, Calendar calendar, OnDateSetListener listener) {
        this.context = context;
        this.calendar = calendar;
        this.onDateSetListener = listener;
    }

    private void showDataPickerDialog() {
        Dialog dataPickerDialog = new Dialog(context);
        dataPickerDialog.setContentView(R.layout.custom_date_picker);

        NumberPicker npYear = dataPickerDialog.findViewById(R.id.npYear);
        NumberPicker npMonth = dataPickerDialog.findViewById(R.id.npMonth);
        NumberPicker npDay = dataPickerDialog.findViewById(R.id.npDay);
        AppCompatButton okButton = dataPickerDialog.findViewById(R.id.okButton);
        // Установка значений для NumberPicker
        npYear.setMinValue(2024);
        npYear.setMaxValue(2100);
        npYear.setValue(calendar.get(Calendar.YEAR));

        npMonth.setMinValue(1);
        npMonth.setMaxValue(12);
        npMonth.setValue(calendar.get(Calendar.MONTH) + 1);

        npDay.setMinValue(1);
        npDay.setMaxValue(getMaxDayOfMonth(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)));
        npDay.setValue(calendar.get(Calendar.DAY_OF_MONTH));

        npMonth.setOnValueChangedListener((picker, oldVal, newVal) -> {
            npDay.setMaxValue(getMaxDayOfMonth(calendar.get(Calendar.YEAR), newVal - 1));
        });


        okButton.setOnClickListener(v -> {
                    calendar.set(npYear.getValue(), npMonth.getValue() - 1, npDay.getValue());
                    if (onDateSetListener != null) {
                        onDateSetListener.onDateSet(calendar);
                    }
        });

        // Show the dialog
        dataPickerDialog.show();
    }

    private int getMaxDayOfMonth(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
}


