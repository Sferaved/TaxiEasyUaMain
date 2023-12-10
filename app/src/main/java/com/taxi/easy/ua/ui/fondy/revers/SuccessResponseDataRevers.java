package com.taxi.easy.ua.ui.fondy.revers;

import com.google.gson.annotations.SerializedName;

public class SuccessResponseDataRevers {
    @SerializedName("response_status")
    private String responseStatus;

    @SerializedName("response_code")
    private String responseCode;
    @SerializedName("reverse_status")
    private String reverseStatus;
    @SerializedName("response_description")
    private String responseDescription;
    @SerializedName("merchant_id")
    private String merchantId;
    @SerializedName("error_message")
    private String errorMessage;

    public String getResponseStatus() {
        return responseStatus;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public String getReverseStatus() {
        return reverseStatus;
    }

    public String getResponseDescription() {
        return responseDescription;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    @SerializedName("error_code")
    private String errorCode;

    @Override
    public String toString() {
        return "SuccessResponseDataRevers{" +
                "responseStatus='" + responseStatus + '\'' +
                ", responseCode='" + responseCode + '\'' +
                ", reverseStatus='" + reverseStatus + '\'' +
                ", responseDescription='" + responseDescription + '\'' +
                ", merchantId='" + merchantId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }
}
