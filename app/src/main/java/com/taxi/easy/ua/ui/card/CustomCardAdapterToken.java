package com.taxi.easy.ua.ui.card;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.taxi.easy.ua.R;

import java.util.ArrayList;
import java.util.Map;

public class CustomCardAdapterToken extends ArrayAdapter<Map<String, String>> {
    private ArrayList<Map<String, String>> cardMaps;
    private int selectedPosition = 0;
    public static String rectoken;

    public CustomCardAdapterToken(
            Context context, ArrayList<Map<String, String>> cardMaps) {
        super(context, R.layout.tokens_adapter_layout, cardMaps);
        this.cardMaps = cardMaps;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.tokens_adapter_layout, parent, false);
            holder = new ViewHolder();

            holder.cardImage = view.findViewById(R.id.cardImage);
            holder.cardText = view.findViewById(R.id.cardText);
            holder.bankText = view.findViewById(R.id.bankText);
            holder.checkBox = view.findViewById(R.id.checkBox);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        final Map<String, String> cardMap = getItem(position);

        if (cardMap != null) {
            String cardType = cardMap.get("card_type");
            if ("VISA".equals(cardType)) {
                holder.cardImage.setImageResource(R.drawable.visa);
            } else if ("MasterCard".equals(cardType)) {
                holder.cardImage.setImageResource(R.drawable.mastercard);
            } else {
                holder.cardImage.setImageResource(R.drawable.default_card);
            }
            String masked_card = cardMap.get("masked_card");
            holder.cardText.setText(masked_card);
            String bank_name = cardMap.get("bank_name");

            Log.d("TAG", "getView:bank_name " + bank_name);
            if(!bank_name.equals("SOME BANK IN UA COUNTRY")) {
                holder.bankText.setText(bank_name);
            } else {
                holder.bankText.setText("");
            }


            holder.checkBox.setChecked(position == selectedPosition);
            rectoken = cardMap.get("rectoken");
            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedPosition = position;
                    notifyDataSetChanged();

                    rectoken = cardMap.get("rectoken");

                    Toast.makeText(getContext(), "rectoken: " + rectoken, Toast.LENGTH_SHORT).show();
                }
            });
        }

        return view;
    }

    static class ViewHolder {
        ImageView cardImage;
        TextView cardText, bankText;
        CheckBox checkBox;
    }
}
