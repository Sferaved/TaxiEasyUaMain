package com.taxi.easy.ua.ui.card;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taxi.easy.ua.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PaymentCardsAdapter extends RecyclerView.Adapter<PaymentCardsAdapter.CardHolder> {

    public interface Listener {
        void onCardSelected(@NonNull Map<String, String> card, int position);

        void onCardDelete(@NonNull Map<String, String> card, int position);
    }

    private final List<Map<String, String>> cards = new ArrayList<>();
    private int selectedPosition = -1;
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submitList(@NonNull List<Map<String, String>> items, int selectedPos) {
        cards.clear();
        cards.addAll(items);
        selectedPosition = selectedPos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_select_row, parent, false);
        return new CardHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardHolder holder, int position) {
        Map<String, String> card = cards.get(position);
        String cardType = card.get("card_type");
        if ("VISA".equalsIgnoreCase(cardType)) {
            holder.icon.setImageResource(R.drawable.visa);
        } else if ("MasterCard".equalsIgnoreCase(cardType)) {
            holder.icon.setImageResource(R.drawable.mastercard);
        } else {
            holder.icon.setImageResource(R.drawable.ic_credit_card);
        }

        String masked = card.get("masked_card");
        holder.title.setText(masked != null ? masked : "••••");
        holder.subtitle.setVisibility(View.GONE);

        boolean selected = position == selectedPosition;
        holder.check.setVisibility(selected ? View.VISIBLE : View.GONE);

        holder.delete.setVisibility(View.VISIBLE);
        holder.delete.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) {
                return;
            }
            if (listener != null) {
                listener.onCardDelete(cards.get(adapterPos), adapterPos);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) {
                return;
            }
            selectedPosition = adapterPos;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onCardSelected(cards.get(adapterPos), adapterPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static final class CardHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView subtitle;
        final ImageView check;
        final ImageView delete;

        CardHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.paymentRowIcon);
            title = itemView.findViewById(R.id.paymentRowTitle);
            subtitle = itemView.findViewById(R.id.paymentRowSubtitle);
            check = itemView.findViewById(R.id.paymentRowCheck);
            delete = itemView.findViewById(R.id.paymentRowDelete);
        }
    }
}
