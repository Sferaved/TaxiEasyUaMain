package com.taxi.easy.ua.utils.helpers;

import android.app.AlertDialog;
import android.os.Message;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.utils.log.Logger;

public final class WfpWebViewHelper {

    private static final String TAG = "WfpWebViewHelper";
    private static final String CLOSING_URL = "https://secure.wayforpay.com/closing";

    public interface ClosingUrlListener {
        void onClosingUrl();
    }

    private WfpWebViewHelper() {
    }

    public static void loadPaymentUrl(WebView webView, String checkoutUrl, ClosingUrlListener listener) {
        configureWebSettings(webView.getSettings());

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                Logger.d(view.getContext(), TAG, "onCreateWindow triggered");
                try {
                    WebView popupWebView = new WebView(view.getContext());
                    configureWebSettings(popupWebView.getSettings());

                    popupWebView.setWebViewClient(new WebViewClient());
                    popupWebView.setWebChromeClient(this);

                    new AlertDialog.Builder(view.getContext())
                            .setView(popupWebView)
                            .setPositiveButton(R.string.close, (dialog, which) -> popupWebView.destroy())
                            .show();

                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(popupWebView);
                    resultMsg.sendToTarget();
                    return true;
                } catch (Exception e) {
                    Logger.e(view.getContext(), TAG, "onCreateWindow error: " + e.getMessage());
                    return false;
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString(), listener);
            }

            @Override
            @Deprecated
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url, listener);
            }
        });

        if (checkoutUrl != null && URLUtil.isValidUrl(checkoutUrl)) {
            webView.loadUrl(checkoutUrl);
        } else {
            Logger.e(webView.getContext(), TAG, "Checkout URL is null or invalid");
        }
    }

    private static void configureWebSettings(WebSettings webSettings) {
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PAYMENT_REQUEST)) {
            WebSettingsCompat.setPaymentRequestEnabled(webSettings, false);
        }
    }

    private static boolean handleUrl(String url, ClosingUrlListener listener) {
        if (url != null && url.contains(CLOSING_URL) && listener != null) {
            listener.onClosingUrl();
        }
        return false;
    }
}
