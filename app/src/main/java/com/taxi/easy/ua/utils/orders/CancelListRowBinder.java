package com.taxi.easy.ua.utils.orders;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.taxi.easy.ua.R;

/**
 * Привязка строки списка заказов (формат #) к drop_down_layout / drop_down_layout_uid.
 */
public final class CancelListRowBinder {

    private CancelListRowBinder() {
    }

    public static void bind(@NonNull View view, @NonNull Context context, @Nullable String item) {
        TextView textView1 = view.findViewById(R.id.text1);
        TextView textView2 = view.findViewById(R.id.text2);
        TextView textView3 = view.findViewById(R.id.text3);
        TextView textView4 = view.findViewById(R.id.text4);
        TextView textView5 = view.findViewById(R.id.text5);
        TextView textView6 = view.findViewById(R.id.text6);
        TextView textView7 = view.findViewById(R.id.text7);

        if (item == null || item.isEmpty()) {
            setOptionalLine(textView2, null);
            setOptionalLine(textView3, null);
            setOptionalLine(textView4, null);
            setOptionalLine(textView5, null);
            setOptionalLine(textView6, null);
            setOptionalLine(textView7, null);
            return;
        }

        String[] parts = item.split("#", -1);
        if (parts.length > 0) {
            textView1.setText(parts[0]);
        }

        if (parts.length == 5) {
            bindLegacyHistoryRow(context, parts, textView2, textView3, textView4, textView5, textView7);
            setOptionalLine(textView6, null);
            return;
        }

        if (parts.length >= 7) {
            setOptionalLine(textView2, partAt(parts, 1));
            setOptionalLine(textView3, partAt(parts, 2));
            setOptionalLine(textView4, partAt(parts, 3));
            setOptionalLine(textView5, partAt(parts, 4));
            setOptionalLine(textView6, partAt(parts, 5));
            setOptionalLine(textView7, partAt(parts, 6));
            return;
        }

        if (parts.length >= 6) {
            setOptionalLine(textView2, partAt(parts, 1));
            setOptionalLine(textView3, null);
            setOptionalLine(textView4, partAt(parts, 2));
            setOptionalLine(textView5, partAt(parts, 3));
            setOptionalLine(textView6, partAt(parts, 4));
            setOptionalLine(textView7, partAt(parts, 5));
        }
    }

    private static void bindLegacyHistoryRow(
            @NonNull Context context,
            @NonNull String[] parts,
            @NonNull TextView costView,
            @NonNull TextView paymentView,
            @NonNull TextView autoView,
            @NonNull TextView timeView,
            @Nullable TextView statusView
    ) {
        String rawCost = partAt(parts, 1);
        if (rawCost != null && !rawCost.trim().isEmpty()) {
            setOptionalLine(
                    costView,
                    context.getString(R.string.close_resone_cost) + rawCost + " " + context.getString(R.string.UAH)
            );
        } else {
            setOptionalLine(costView, null);
        }
        setOptionalLine(paymentView, null);
        setOptionalLine(autoView, partAt(parts, 2));
        setOptionalLine(timeView, partAt(parts, 3));
        setOptionalLine(statusView, partAt(parts, 4));
    }

    @Nullable
    private static String partAt(@NonNull String[] parts, int index) {
        if (index < 0 || index >= parts.length) {
            return null;
        }
        return parts[index];
    }

    private static void setOptionalLine(@Nullable TextView textView, @Nullable String text) {
        if (textView == null) {
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            textView.setText("");
            textView.setVisibility(View.GONE);
        } else {
            textView.setText(text);
            textView.setVisibility(View.VISIBLE);
        }
    }

    /** Сырая сумма для повторного заказа из истории (legacy: parts[1], новый формат: из строки стоимости). */
    @NonNull
    public static String rawCostFromItem(@Nullable String item) {
        if (item == null || item.isEmpty()) {
            return "";
        }
        String[] parts = item.split("#", -1);
        if (parts.length == 5 && parts.length > 1) {
            return parts[1].trim();
        }
        if (parts.length >= 7 && parts.length > 1) {
            String costLine = parts[1];
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile("([\\d]+(?:[.,]\\d+)?)").matcher(costLine);
            if (matcher.find()) {
                return matcher.group(1).replace(',', '.');
            }
        }
        return parts.length > 1 ? parts[1].trim() : "";
    }
}
