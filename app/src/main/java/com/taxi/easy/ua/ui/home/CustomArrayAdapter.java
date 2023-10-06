package com.taxi.easy.ua.ui.home;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.taxi.easy.ua.R;

import java.util.Arrays;
import java.util.List;

public class CustomArrayAdapter extends ArrayAdapter<String> {

    private boolean[] itemEnabled;

    public CustomArrayAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
        itemEnabled = new boolean[objects.size()]; // Создаем массив для хранения информации о состоянии элементов
        Arrays.fill(itemEnabled, true); // Устанавливаем начальное состояние (все элементы активны)
    }

    // Метод для установки состояния элемента
    public void setItemEnabled(int position, boolean isEnabled) {
        itemEnabled[position] = isEnabled;
        notifyDataSetChanged(); // Обновляем адаптер после изменения состояния элемента
    }

    @Override
    public boolean isEnabled(int position) {
        return itemEnabled[position]; // Возвращаем состояние элемента
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        // Устанавливаем цвет текста в зависимости от состояния элемента
        // Проверяем состояние элемента и устанавливаем цвет текста
        if (!isEnabled(position)) {
            TextView textView = view.findViewById(android.R.id.text1);
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.disabledText));
        } else {
            // Если элемент активен, то восстанавливаем стандартный цвет текста
            TextView textView = view.findViewById(android.R.id.text1);
            textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
        }

        return view;
    }
}
