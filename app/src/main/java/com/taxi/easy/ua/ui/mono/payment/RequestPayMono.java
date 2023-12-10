package com.taxi.easy.ua.ui.mono.payment;

import com.google.gson.annotations.SerializedName;
public class RequestPayMono {
    @SerializedName("amount")
    private int amount;

    @SerializedName("ccy")
    private int ccy;

    public RequestPayMono(
            int amount,
            String reference,
            String comment
    ) {
        this.amount = amount;
        this.ccy = 980;
        this.merchantPaymInfo = new MerchantPaymInfo(
                reference,
                comment
        );
        this.redirectUrl = "https://m.easy-order-taxi.site/mono/redirectUrl";
        this.webHookUrl = "https://m.easy-order-taxi.site/mono/webHookUrl";
        this.validity = 3600;
        this.paymentType = "hold";
        this.qrId = "";
        this.code = "";
        this.saveCardData = new SaveCardData();
    }

    @SerializedName("merchantPaymInfo")
    private MerchantPaymInfo merchantPaymInfo;

    @SerializedName("redirectUrl")
    private String redirectUrl;

    @SerializedName("webHookUrl")
    private String webHookUrl;

    @SerializedName("validity")
    private int validity;

    @SerializedName("paymentType")
    private String paymentType;

    @SerializedName("qrId")
    private String qrId;

    @SerializedName("code")
    private String code;

    @SerializedName("saveCardData")
    private SaveCardData saveCardData;

    // Геттеры и сеттеры для всех полей

    public static class MerchantPaymInfo {
        @SerializedName("reference")
        private String reference;

        @SerializedName("destination")
        private String destination;

        @SerializedName("comment")
        private String comment;

        @SerializedName("basketOrder")
        private BasketOrderItem[] basketOrder;

        public MerchantPaymInfo(
                String reference,
                String comment
        ) {
            this.reference = reference;
            this.destination = comment;
            this.comment = comment;
            this.basketOrder = null;
        }
// Геттеры и сеттеры для всех полей
    }

    public static class BasketOrderItem {
        @SerializedName("name")
        private String name;

        @SerializedName("qty")
        private int qty;

        @SerializedName("sum")
        private int sum;

        @SerializedName("icon")
        private String icon;

        @SerializedName("unit")
        private String unit;

        @SerializedName("code")
        private String code;

        @SerializedName("barcode")
        private String barcode;

        @SerializedName("header")
        private String header;

        @SerializedName("footer")
        private String footer;

        @SerializedName("tax")
        private String[] tax;

        @SerializedName("uktzed")
        private String uktzed;

        // Геттеры и сеттеры для всех полей
    }

    public static class SaveCardData {
        @SerializedName("saveCard")
        private boolean saveCard;

        @SerializedName("walletId")
        private String walletId;

        public SaveCardData() {
            this.saveCard = true;
            this.walletId = "69f780d841a0434aa535b08821f4822c";
        }
// Геттеры и сеттеры для всех полей
    }
}
