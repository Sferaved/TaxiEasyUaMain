package com.taxi.easy.ua.utils.payment;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.taxi.easy.ua.ui.wfp.purchase.PurchaseResponse;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WalletAddCostHttpRecreateHelperTest {

    @Test
    public void shouldApplyHttpRecreate_whenHoldSuccessAndUidPresent() {
        assertTrue(WalletAddCostHttpRecreateHelper.shouldApplyHttpRecreateOnHoldSuccess(
                "WaitingAuthComplete", "2fe187d1abc74cc08c9a095969cfed02"));
        assertTrue(WalletAddCostHttpRecreateHelper.shouldApplyHttpRecreateOnHoldSuccess(
                "Approved", "abc"));
    }

    @Test
    public void shouldNotApplyHttpRecreate_withoutUid() {
        assertFalse(WalletAddCostHttpRecreateHelper.shouldApplyHttpRecreateOnHoldSuccess(
                "WaitingAuthComplete", null));
        assertFalse(WalletAddCostHttpRecreateHelper.shouldApplyHttpRecreateOnHoldSuccess(
                "WaitingAuthComplete", "  "));
        assertFalse(WalletAddCostHttpRecreateHelper.shouldApplyHttpRecreateOnHoldSuccess(
                "InProcessing", "abc"));
    }

    @Test
    public void resolveDisplayCost_prefersClientCostNumber() {
        assertEquals("16", WalletAddCostHttpRecreateHelper.resolveDisplayCostGrivna(
                JsonParser.parseString("16"),
                JsonParser.parseString("\"15\"")));
    }

    @Test
    public void purchaseResponse_parsesUidAndCostsFromAddCostJson() {
        String json = "{"
                + "\"uid\":\"2fe187d1abc74cc08c9a095969cfed02\","
                + "\"web_cost\":\"15\","
                + "\"client_cost\":16,"
                + "\"pay_system\":\"wfp_payment\","
                + "\"transactionStatus\":\"WaitingAuthComplete\""
                + "}";
        PurchaseResponse response = new Gson().fromJson(json, PurchaseResponse.class);

        assertTrue(response.hasRecreatedOrder());
        assertEquals("2fe187d1abc74cc08c9a095969cfed02", response.getUid());
        assertEquals("16", response.resolveDisplayCostGrivna());
        assertEquals("WaitingAuthComplete", response.getTransactionStatus());
        assertTrue(WalletAddCostHttpRecreateHelper.shouldApplyHttpRecreateOnHoldSuccess(
                response.getTransactionStatus(), response.getUid()));
    }

    @Test
    public void purchaseResponse_rawWfpWithoutUid_doesNotApplyRecreate() {
        String json = "{"
                + "\"orderReference\":\"V_20260711105346548_6PVL\","
                + "\"transactionStatus\":\"WaitingAuthComplete\","
                + "\"reason\":\"Ok\","
                + "\"reasonCode\":1100"
                + "}";
        PurchaseResponse response = new Gson().fromJson(json, PurchaseResponse.class);

        assertFalse(response.hasRecreatedOrder());
        assertFalse(WalletAddCostHttpRecreateHelper.shouldApplyHttpRecreateOnHoldSuccess(
                response.getTransactionStatus(), response.getUid()));
    }
}
