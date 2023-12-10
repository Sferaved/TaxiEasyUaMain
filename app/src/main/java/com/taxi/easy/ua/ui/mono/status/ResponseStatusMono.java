package com.taxi.easy.ua.ui.mono.status;

import com.google.gson.annotations.SerializedName;

public class ResponseStatusMono {
        @SerializedName("invoiceId")
        private String invoiceId;

        @SerializedName("status")
        private String status;

        @SerializedName("failureReason")
        private String failureReason;

        @SerializedName("amount")
        private int amount;

        @SerializedName("ccy")
        private int ccy;

        @SerializedName("finalAmount")
        private int finalAmount;

        @SerializedName("createdDate")
        private String createdDate;

        @SerializedName("modifiedDate")
        private String modifiedDate;

        @SerializedName("reference")
        private String reference;

        @SerializedName("cancelList")
        private CancelListItem[] cancelList;

    public String getInvoiceId() {
        return invoiceId;
    }

    public String getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public int getAmount() {
        return amount;
    }

    public int getCcy() {
        return ccy;
    }

    public int getFinalAmount() {
        return finalAmount;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public String getModifiedDate() {
        return modifiedDate;
    }

    public String getReference() {
        return reference;
    }

    public CancelListItem[] getCancelList() {
        return cancelList;
    }

    public WalletData getWalletData() {
        return walletData;
    }

    @SerializedName("walletData")
        private WalletData walletData;

        // Геттеры и сеттеры для всех полей

        public static class CancelListItem {
            @SerializedName("status")
            private String status;

            @SerializedName("amount")
            private int amount;

            @SerializedName("ccy")
            private int ccy;

            @SerializedName("createdDate")
            private String createdDate;

            @SerializedName("modifiedDate")
            private String modifiedDate;

            public String getStatus() {
                return status;
            }

            public int getAmount() {
                return amount;
            }

            public int getCcy() {
                return ccy;
            }

            public String getCreatedDate() {
                return createdDate;
            }

            public String getModifiedDate() {
                return modifiedDate;
            }

            public String getApprovalCode() {
                return approvalCode;
            }

            public String getRrn() {
                return rrn;
            }

            public String getExtRef() {
                return extRef;
            }
            @SerializedName("approvalCode")
            private String approvalCode;

            @SerializedName("rrn")
            private String rrn;

            @SerializedName("extRef")
            private String extRef;

            // Геттеры и сеттеры для всех полей

        }

        public static class WalletData {
            @SerializedName("cardToken")
            private String cardToken;

            @SerializedName("walletId")
            private String walletId;

            @SerializedName("status")
            private String status;


            // Геттеры и сеттеры для всех полей
            public String getCardToken() {
                return cardToken;
            }

            public String getWalletId() {
                return walletId;
            }

            public String getStatus() {
                return status;
            }
        }
    }

