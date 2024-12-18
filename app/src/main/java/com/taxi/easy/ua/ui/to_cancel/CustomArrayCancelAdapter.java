package com.taxi.easy.ua.ui.to_cancel;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.navigation.NavOptions;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.db.DatabaseHelperUid;
import com.taxi.easy.ua.utils.db.RouteInfoCancel;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CustomArrayCancelAdapter extends ArrayAdapter<String> {

    private final String TAG = "CustomArrayCancelAdapter";
    private final int resource;
    private final int textViewId1;
    private final int textViewId2;
    private final int textViewId3;
    private final int textViewId4;
    private final int textViewId5;
    private final List<String> items;
    private RouteInfoCancel routeInfo;
    DatabaseHelperUid databaseHelperUid;
    Context context;
    AppCompatButton button;
    public CustomArrayCancelAdapter(
            Context context,
            int resource,
            int textViewId1,
            int textViewId2,
            int textViewId3,
            int textViewId4,
            int textViewId5,
            List<String> items
    ) {
        super(context, resource, items);  // Передаем список строк в ArrayAdapter
        this.resource = resource;
        this.textViewId1 = textViewId1;
        this.textViewId2 = textViewId2;
        this.textViewId3 = textViewId3;
        this.textViewId4 = textViewId4;
        this.textViewId5 = textViewId5;
        this.items = items;
        this.context = context;
        this.databaseHelperUid = new DatabaseHelperUid(getContext());
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(resource, parent, false);
        }

        // Получаем TextView для отображения текста

        // Получаем строку для данного элемента
        String item = items.get(position);

        // Разделяем строку на части
        String[] parts = item.split("#"); // Разделение по запятой

        // Получаем TextViews для отображения частей
        TextView textView1 = view.findViewById(R.id.text1);
        TextView textView2 = view.findViewById(R.id.text2);
        TextView textView3 = view.findViewById(R.id.text3);
        TextView textView4 = view.findViewById(R.id.text4);
        TextView textView5 = view.findViewById(R.id.text5);
        // Добавьте дополнительные TextViews, если нужно

        // Устанавливаем текст для TextView
        if (parts.length > 0) {
            textView1.setText(parts[0]); // Первая часть
        }
        if (parts.length > 1) {
            textView2.setText(parts[1]); // Вторая часть
        }
        if (parts.length > 2) {
            textView3.setText(parts[2]); // Вторая часть
        }
        if (parts.length > 3) {
            textView4.setText(parts[3]); // Вторая часть
        }
        if (parts.length > 4) {
            textView5.setText(parts[4]); // Вторая часть
        }

        // Получаем кнопку и устанавливаем обработчик нажатия
        textView1.setOnClickListener(v -> {
            Logger.d(getContext(), TAG, "position+1: " + position+1);
            routeInfo = databaseHelperUid.getCancelInfoById(position+1);
            if (routeInfo != null) {
                Logger.d(getContext(), TAG, "onContextItemSelected: " + routeInfo.toString());
            } else {
                Logger.d(getContext(), TAG, "onContextItemSelected: RouteInfo not found for id: " + (position + 1));
            }

            Map<String, String> costMap = getStringStringMap();
            Logger.d(getContext(), TAG, "costMap " + (position + 1));
            Logger.d(getContext(), TAG, "onContextItemSelected costMap: " + costMap);
            startFinishPage(costMap);

        });

        button = view.findViewById(R.id.button);
        button.setOnClickListener(v -> {
            Logger.d(getContext(), TAG, "position+1: " + position+1);
            routeInfo = databaseHelperUid.getCancelInfoById(position+1);
            if (routeInfo != null) {
                Logger.d(getContext(), TAG, "onContextItemSelected: " + routeInfo.toString());
            } else {
                Logger.d(getContext(), TAG, "onContextItemSelected: RouteInfo not found for id: " + (position + 1));
            }

            Map<String, String> costMap = getStringStringMap();
            Logger.d(getContext(), TAG, "costMap " + (position + 1));
            Logger.d(getContext(), TAG, "onContextItemSelected costMap: " + costMap);
            startFinishPage(costMap);

        });

        return view;
    }

    private @NonNull Map<String, String> getStringStringMap() {
        Map<String, String> costMap = new HashMap<>();

        costMap.put("dispatching_order_uid", routeInfo.getDispatchingOrderUid());
        costMap.put("order_cost", routeInfo.getOrderCost());
        costMap.put("routefrom", routeInfo.getRouteFrom());
        costMap.put("routefromnumber", routeInfo.getRouteFromNumber());
        costMap.put("routeto", routeInfo.getRouteTo());
        costMap.put("to_number", routeInfo.getToNumber());
        Logger.d(context, TAG, "uid_Double getStringStringMap" + routeInfo.getDispatchingOrderUidDouble());
        if (routeInfo.getDispatchingOrderUidDouble() != null) {
            costMap.put("dispatching_order_uid_Double", routeInfo.getDispatchingOrderUidDouble());
        } else {
            costMap.put("dispatching_order_uid_Double", " ");
        }
        costMap.put("pay_method", routeInfo.getToPay_method());
        costMap.put("orderWeb", routeInfo.getOrderCost());
        costMap.put("required_time", routeInfo.getRequired_time());
        costMap.put("flexible_tariff_name", routeInfo.getFlexible_tariff_name());
        costMap.put("comment_info", routeInfo.getComment_info());
        costMap.put("extra_charge_codes", routeInfo.getExtra_charge_codes());
        return costMap;
    }

    private void startFinishPage(Map<String, String> sendUrlMap)
    {
        String to_name;
        if (Objects.equals(sendUrlMap.get("routefrom"), sendUrlMap.get("routeto"))) {
            to_name = context.getString(R.string.on_city_tv);
            Logger.d(context, TAG, "startFinishPage: to_name 1 " + to_name);

        } else {

            if(Objects.equals(sendUrlMap.get("routeto"), "Точка на карте")) {
                to_name = context.getString(R.string.end_point_marker);
            } else {
                to_name = sendUrlMap.get("routeto") + " " + sendUrlMap.get("to_number");
            }
            Logger.d(context, TAG, "startFinishPage: to_name 2 " + to_name);
        }
        Logger.d(context, TAG, "startFinishPage: to_name 3" + to_name);
        String to_name_local = to_name;
        if(to_name.contains("по місту")
                ||to_name.contains("по городу")
                || to_name.contains("around the city")
        ) {
            to_name_local = context.getString(R.string.on_city_tv);
        }
        Logger.d(context, TAG, "startFinishPage: to_name 4" + to_name_local);
        String pay_method_message = context.getString(R.string.pay_method_message_main);
        switch (Objects.requireNonNull(sendUrlMap.get("pay_method"))) {
            case "bonus_payment":
                pay_method_message += " " + context.getString(R.string.pay_method_message_bonus);
                break;
            case "card_payment":
            case "fondy_payment":
            case "mono_payment":
            case "wfp_payment":
                pay_method_message += " " + context.getString(R.string.pay_method_message_card);
                break;
            default:
                pay_method_message += " " + context.getString(R.string.pay_method_message_nal);
        }

        String routeFrom = cleanString(sendUrlMap.get("routefrom") + " " +sendUrlMap.get("routefromnumber"));
        String toMessage = cleanString(context.getString(R.string.to_message));
        String toNameLocal = cleanString(to_name_local);
        String orderWeb = cleanString(Objects.requireNonNull(sendUrlMap.get("orderWeb")));
        String uah = cleanString(context.getString(R.string.UAH));
        String payMethodMessage = cleanString(pay_method_message);
        String required_time = sendUrlMap.get("required_time");

        String messageResult =
                routeFrom + " " +
                        toMessage + " " +
                        toNameLocal + ". " +
                        required_time;
        String messagePayment = orderWeb + " " + uah + " " + payMethodMessage;
        Logger.d(context, TAG, "messageResult: " + messageResult);
        Logger.d(context, TAG, "messagePayment: " + messagePayment);

        String messageFondy = context.getString(R.string.fondy_message) + " " +
                sendUrlMap.get("routefrom") + " " + context.getString(R.string.to_message) +
                to_name_local + ".";
        Logger.d(context, TAG, "startFinishPage: messageResult " + messageResult);
        Logger.d(context, TAG, "startFinishPage: to_name " + to_name);

        Logger.d(context, TAG, "orderWeb: " + orderWeb);
        Logger.d(context, TAG, "uah: " + uah);
        Logger.d(context, TAG, "payMethodMessage: " + payMethodMessage);


        Bundle bundle = new Bundle();
        bundle.putString("messageResult_key", messageResult);
        bundle.putString("messagePay_key", messagePayment);
        bundle.putString("messageFondy_key", messageFondy);
        bundle.putString("messageCost_key", Objects.requireNonNull(sendUrlMap.get("orderWeb")));
        bundle.putSerializable("sendUrlMap", new HashMap<>(sendUrlMap));
        bundle.putString("card_payment_key", "no");
        bundle.putString("UID_key", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid")));
        bundle.putString("dispatching_order_uid_Double", Objects.requireNonNull(sendUrlMap.get("dispatching_order_uid_Double")));

// Установите Bundle как аргументы фрагмента
        MainActivity.navController.navigate(R.id.nav_finish_separate, bundle, new NavOptions.Builder()
                .setPopUpTo(R.id.nav_visicom, true)
                .build());


    }

    private String cleanString(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ").replaceAll("\\s{2,}$", " ");
    }
}

