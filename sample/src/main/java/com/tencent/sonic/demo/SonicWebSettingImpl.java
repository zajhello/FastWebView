package com.tencent.sonic.demo;

import android.app.Activity;
import android.content.Intent;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.just.agentweb.AbsAgentWebSettings;
import com.just.agentweb.AgentWeb;
import com.just.agentweb.DefaultDownloadImpl;
import com.just.agentweb.IAgentWebSettings;
import com.just.agentweb.WebListenerManager;

public class SonicWebSettingImpl extends AbsAgentWebSettings {

    private SonicSessionClientImpl mSonicSessionClient;
    protected AgentWeb mAgentWeb;
    private Activity mActivity;
    private Intent mIntent;

    public SonicWebSettingImpl(Activity activity, Intent intent, SonicSessionClientImpl mSonicSessionClient) {
        super();
        this.mActivity = activity;
        this.mIntent = intent;
        this.mSonicSessionClient = mSonicSessionClient;
    }

    @Override
    protected void bindAgentWebSupport(AgentWeb agentWeb) {
        this.mAgentWeb = agentWeb;
    }


    @Override
    public IAgentWebSettings toSetting(WebView webView) {
        super.toSetting(webView);
        WebSettings webSettings = webView.getSettings();

        // add java script interface
        // note:if api level lower than 17(android 4.2), addJavascriptInterface has security
        // issue, please use x5 or see https://developer.android.com/reference/android/webkit/
        // WebView.html#addJavascriptInterface(java.lang.Object, java.lang.String)
        webSettings.setJavaScriptEnabled(true);
        webView.removeJavascriptInterface("searchBoxJavaBridge_");
        mIntent.putExtra(SonicJavaScriptInterface.PARAM_LOAD_URL_TIME, System.currentTimeMillis());
        webView.addJavascriptInterface(new SonicJavaScriptInterface(mSonicSessionClient, mIntent), "sonic");

        // init webview settings
        webSettings.setAllowContentAccess(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        return this;
    }


    @Override
    public WebListenerManager setDownloader(WebView webView, DownloadListener downloadListener) {
        return super.setDownloader(webView,
                DefaultDownloadImpl.create(this.mActivity
                        , webView, mAgentWeb.getPermissionInterceptor()));
    }
}

