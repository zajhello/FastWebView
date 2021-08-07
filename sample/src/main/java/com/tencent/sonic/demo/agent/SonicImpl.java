package com.tencent.sonic.demo.agent;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.Nullable;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.just.agentweb.AgentWeb;
import com.just.agentweb.MiddlewareWebClientBase;
import com.tencent.sonic.demo.SonicRuntimeImpl;
import com.tencent.sonic.sdk.SonicConfig;
import com.tencent.sonic.sdk.SonicEngine;
import com.tencent.sonic.sdk.SonicSession;
import com.tencent.sonic.sdk.SonicSessionConfig;

public class SonicImpl {

    private SonicSession sonicSession;
    private Context mContext;
    private String url;
    private SonicSessionClientImpl sonicSessionClient;

    public SonicImpl(String url, Context context) {
        this.url = url;
        this.mContext = context;
    }


    /**
     *
     */
    public void onCreateSession() {

        SonicSessionConfig.Builder sessionConfigBuilder = new SonicSessionConfig.Builder();
        sessionConfigBuilder.setSupportLocalServer(true);
        // init sonic engine if necessary, or maybe u can do this when application created
        if (!SonicEngine.isGetInstanceAllowed()) {
            SonicEngine.createInstance(new SonicRuntimeImpl(mContext), new SonicConfig.Builder().build());
        }
        // create sonic session and run sonic flow
        sonicSession = SonicEngine.getInstance().createSession(url, sessionConfigBuilder.build());
        if (null != sonicSession) {
            sonicSession.bindClient(sonicSessionClient = new SonicSessionClientImpl());
        } else {
            // throw new UnknownError("create session fail!");
            Toast.makeText(mContext, "create sonic session fail!", Toast.LENGTH_LONG).show();
        }
    }

    public SonicSessionClientImpl getSonicSessionClient() {
        return this.sonicSessionClient;
    }

    /**
     * 不使用中间件，使用普通的 WebViewClient 也是可以的。
     *
     * @return MiddlewareWebClientBase
     */
    public @Nullable
    com.just.agentweb.WebViewClient getWebViewClient() {
        return new com.just.agentweb.WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (sonicSession != null) {
                    sonicSession.getSessionClient().pageFinish(url);
                }
            }

            @TargetApi(21)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return shouldInterceptRequest(view, request.getUrl().toString());
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (sonicSession != null) {
                    return (WebResourceResponse) sonicSession.getSessionClient().requestResource(url);
                }
                return super.shouldInterceptRequest(view, url);
            }

            /**
             * 资源拦截
             * @param webView
             * @param url
             */
            @Override
            public void onLoadResource(WebView webView, String url) {
                super.onLoadResource(webView, url);
            }
        };
    }

    public void bindAgentWeb(AgentWeb agentWeb) {
        if (sonicSessionClient != null) {
            sonicSessionClient.bindWebView(agentWeb);
            sonicSessionClient.clientReady();
        } else { // default mode
            agentWeb.getUrlLoader().loadUrl(url);
        }
    }

    public void destroy() {
        if (sonicSession != null) {
            sonicSession.destroy();
        }
    }


}
