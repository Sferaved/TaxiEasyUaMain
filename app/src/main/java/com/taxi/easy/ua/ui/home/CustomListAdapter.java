package com.taxi.easy.ua.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.taxi.easy.ua.R;

public class CustomListAdapter extends BaseAdapter {

    private final Context context;
    private final String[] dataList;
    private final int numItemsToShow; // Количество элементов, которые вы хотите показать

    public CustomListAdapter(Context context, String[] dataList, int numItemsToShow) {
        this.context = context;
        this.dataList = dataList;
        this.numItemsToShow = numItemsToShow;
    }

    @Override
    public int getCount() {
        return Math.min(numItemsToShow, dataList.length);
    }

    @Override
    public Object getItem(int position) {
        return dataList[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.services_adapter_layout, parent, false);
        }

        String data = dataList[position];
        TextView textView = (TextView) convertView;
        textView.setText(data);

        return convertView;
    }
}
