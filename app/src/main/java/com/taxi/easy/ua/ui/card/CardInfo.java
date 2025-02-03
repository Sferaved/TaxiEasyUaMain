package com.taxi.easy.ua.ui.card;

public class CardInfo {
    private String masked_card;
    private String card_type;
    private String bank_name;
    private String rectoken;
    private String merchant;
    private String active;

    public String getMerchant() {
        return merchant;
    }
    public String getMasked_card() {
        return masked_card;
    }

    public String getCard_type() {
        return card_type;
    }

    public String getBank_name() {
        return bank_name;
    }

    public String getRectoken() {
        return rectoken;
    }
    public String getActive() {
        return active;
    }
    @Override
    public String toString() {
        return "CardInfo{" +
                "masked_card='" + masked_card + '\'' +
                ", card_type='" + card_type + '\'' +
                ", bank_name='" + bank_name + '\'' +
                ", rectoken='" + rectoken + '\'' +
                '}';
    }


}