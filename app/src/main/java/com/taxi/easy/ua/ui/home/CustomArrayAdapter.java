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

    private final boolean[] itemEnabled; // Для хранения состояния активности элементов
    private final boolean[] itemChecked; // Для хранения состояния выбора элементов

    public CustomArrayAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
        itemEnabled = new boolean[objects.size()];
        itemChecked = new boolean[objects.size()];
        Arrays.fill(itemEnabled, true); // Все элементы активны по умолчанию
        Arrays.fill(itemChecked, false); // Ни один элемент не выбран по умолчанию
    }

    // Метод для установки состояния элемента
    public void setItemEnabled(int position, boolean isEnabled) {
        itemEnabled[position] = isEnabled;
        notifyDataSetChanged(); // Обновляем адаптер
    }

    // Метод для установки состояния выбора элемента
    public void setItemChecked(int position, boolean isChecked) {
        // Сбрасываем выбор у всех элементов для режима одиночного выбора
        Arrays.fill(itemChecked, false);
        if (isChecked) {
            itemChecked[position] = true; // Устанавливаем выбор для указанного элемента
        }
        notifyDataSetChanged(); // Обновляем адаптер
    }

    // Проверка, выделен ли элемент
    public boolean isItemChecked(int position) {
        return itemChecked[position];
    }

    @Override
    public boolean isEnabled(int position) {
        return itemEnabled[position]; // Возвращаем состояние активности элемента
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        TextView textView = view.findViewById(android.R.id.text1);

        // Устанавливаем цвет текста в зависимости от состояния элемента
        if (!isEnabled(position)) {
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.disabledText));
        } else if (itemChecked[position]) {
            // Если элемент выбран, то устанавливаем цвет для выбранного состояния
            textView.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        } else {
            // Если элемент активен, но не выбран
            textView.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
        }

        return view;
    }
}
