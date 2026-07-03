package com.taxi.easy.ua.ui.finish;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;
import com.taxi.easy.ua.utils.cost.CostParseHelper;

/**
 * Ответ {@code /android/startAddCostWithAddBottomUpdate/{uid}/{addCost}}.
 * При успехе — новый uid и итоговая стоимость; при ошибке — поле response.
 */
public class AddCostBottomUpdateResponse {

    @SerializedName("response")
    private String response;

    @SerializedName("uid")
    private String uid;

    @SerializedName("web_cost")
    private String webCost;

    @SerializedName("client_cost")
    private String clientCost;

    @Nullable
    public String getResponse() {
        return response;
    }

    @Nullable
    public String getUid() {
        return uid;
    }

    @Nullable
    public String getWebCost() {
        return webCost;
    }

    @Nullable
    public String getClientCost() {
        return clientCost;
    }

    public boolean hasRecreatedOrder() {
        return uid != null && !uid.trim().isEmpty();
    }

    /** Итоговая сумма с сервера (client_cost приоритетнее web_cost). */
    @Nullable
    public String resolveDisplayCostGrivna() {
        String fromClient = CostParseHelper.normalizeCostString(clientCost);
        if (fromClient != null) {
            return fromClient;
        }
        return CostParseHelper.normalizeCostString(webCost);
    }
}
