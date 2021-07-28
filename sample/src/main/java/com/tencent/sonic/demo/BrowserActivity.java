/*
 * Tencent is pleased to support the open source community by making VasSonic available.
 *
 * Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package com.tencent.sonic.demo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.just.agentweb.AgentWeb;
import com.just.agentweb.AgentWebUIControllerImplBase;
import com.just.agentweb.IAgentWebSettings;
import com.just.agentweb.MiddlewareWebChromeBase;
import com.just.agentweb.MiddlewareWebClientBase;
import com.just.agentweb.PermissionInterceptor;
import com.just.agentweb.WebChromeClient;
import com.tencent.sonic.R;
import com.tencent.sonic.sdk.SonicCacheInterceptor;
import com.tencent.sonic.sdk.SonicConfig;
import com.tencent.sonic.sdk.SonicConstants;
import com.tencent.sonic.sdk.SonicEngine;
import com.tencent.sonic.sdk.SonicSession;
import com.tencent.sonic.sdk.SonicSessionConfig;
import com.tencent.sonic.sdk.SonicSessionConnection;
import com.tencent.sonic.sdk.SonicSessionConnectionInterceptor;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A demo browser activity
 * In this demo there are three modes,
 * sonic mode: sonic mode means webview loads html by sonic,
 * offline mode: offline mode means webview loads html from local offline packages,
 * default mode: default mode means webview loads html in the normal way.
 */

public class BrowserActivity extends Activity {


    public final static String PARAM_URL = "param_url";

    public final static String PARAM_MODE = "param_mode";

    private SonicSession sonicSession;
    private AgentWeb mAgentWeb;
    private FrameLayout mContainer;
    private MiddlewareWebChromeBase mMiddleWareWebChrome;
    private MiddlewareWebClientBase mMiddleWareWebClient;

    private SonicSessionClientImpl mSonicSessionClient;
    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIntent = getIntent();
        String url = mIntent.getStringExtra(PARAM_URL);
        int mode = mIntent.getIntExtra(PARAM_MODE, -1);
        if (TextUtils.isEmpty(url) || -1 == mode) {
            finish();
            return;
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        // init sonic engine if necessary, or maybe u can do this when application created
        if (!SonicEngine.isGetInstanceAllowed()) {
            SonicEngine.createInstance(new SonicRuntimeImpl(getApplication()), new SonicConfig.Builder().build());
        }


        // if it's sonic mode , startup sonic session at first time
        if (MainActivity.MODE_DEFAULT != mode) { // sonic mode
            SonicSessionConfig.Builder sessionConfigBuilder = new SonicSessionConfig.Builder();
            sessionConfigBuilder.setSupportLocalServer(true);

            // if it's offline pkg mode, we need to intercept the session connection
            if (MainActivity.MODE_SONIC_WITH_OFFLINE_CACHE == mode) {
                sessionConfigBuilder.setCacheInterceptor(new SonicCacheInterceptor(null) {
                    @Override
                    public String getCacheData(SonicSession session) {
                        return null; // offline pkg does not need cache
                    }
                });

                sessionConfigBuilder.setConnectionInterceptor(new SonicSessionConnectionInterceptor() {
                    @Override
                    public SonicSessionConnection getConnection(SonicSession session, Intent intent) {
                        return new OfflinePkgSessionConnection(BrowserActivity.this, session, intent);
                    }
                });
            }

            // create sonic session and run sonic flow
            sonicSession = SonicEngine.getInstance().createSession(url, sessionConfigBuilder.build());
            if (null != sonicSession) {
                sonicSession.bindClient(mSonicSessionClient = new SonicSessionClientImpl());
            } else {
                // this only happen when a same sonic session is already running,
                // u can comment following codes to feedback as a default mode.
                // throw new UnknownError("create session fail!");
                Toast.makeText(this, "create sonic session fail!", Toast.LENGTH_LONG).show();
            }
        }

        // start init flow ...
        // in the real world, the init flow may cost a long time as startup
        // runtime、init configs....
        setContentView(R.layout.activity_browser);
        mContainer = this.findViewById(R.id.container);
        initAgentWeb();


        FloatingActionButton btnFab = (FloatingActionButton) findViewById(R.id.btn_refresh);
        btnFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sonicSession != null) {
                    sonicSession.refresh();
                }
            }
        });

        // init webview
//        WebView webView = (WebView) findViewById(R.id.webview);
//        WebSettings webSettings = webView.getSettings();
//
//        // add java script interface
//        // note:if api level lower than 17(android 4.2), addJavascriptInterface has security
//        // issue, please use x5 or see https://developer.android.com/reference/android/webkit/
//        // WebView.html#addJavascriptInterface(java.lang.Object, java.lang.String)
//        webSettings.setJavaScriptEnabled(true);
//        webView.removeJavascriptInterface("searchBoxJavaBridge_");
//        intent.putExtra(SonicJavaScriptInterface.PARAM_LOAD_URL_TIME, System.currentTimeMillis());
//        webView.addJavascriptInterface(new SonicJavaScriptInterface(sonicSessionClient, intent), "sonic");
//
//        // init webview settings
//        webSettings.setAllowContentAccess(true);
//        webSettings.setDatabaseEnabled(true);
//        webSettings.setDomStorageEnabled(true);
//        webSettings.setAppCacheEnabled(true);
//        webSettings.setSavePassword(false);
//        webSettings.setSaveFormData(false);
//        webSettings.setUseWideViewPort(true);
//        webSettings.setLoadWithOverviewMode(true);


        // webview is ready now, just tell session client to bind
        if (mSonicSessionClient != null) {
            mSonicSessionClient.bindWebView(mAgentWeb.getWebCreator().getWebView());
            mSonicSessionClient.clientReady();
        } else { // default mode
            mAgentWeb.getWebCreator().getWebView().loadUrl(url);
        }
    }

    private void initAgentWeb() {
        mAgentWeb = AgentWeb.with(this)
                .setAgentWebParent(mContainer, new FrameLayout.LayoutParams(-1, -1))
                .useDefaultIndicator(getIndicatorColor(), getIndicatorHeight())
                .setWebChromeClient(getWebChromeClient())
                .setWebViewClient(getWebViewClient())
//                .setWebView(getWebView())
                .setPermissionInterceptor(getPermissionInterceptor())
//                .setWebLayout(getWebLayout())
                .setAgentWebUIController(getAgentWebUIController())
                .interceptUnkownUrl()
//                .setOpenOtherPageWays(getOpenOtherAppWay())
                .useMiddlewareWebChrome(getMiddleWareWebChrome())
                .useMiddlewareWebClient(getMiddleWareWebClient())
                .setAgentWebWebSettings(getAgentWebSettings())
//                .setMainFrameErrorView(mErrorLayoutEntity.layoutRes, mErrorLayoutEntity.reloadId)
                .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK)
                .createAgentWeb()
                .ready()
                .get();
    }

    protected @NonNull
    MiddlewareWebChromeBase getMiddleWareWebChrome() {
        return this.mMiddleWareWebChrome = new MiddlewareWebChromeBase() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
//                setTitle(view, title);
            }
        };
    }

    public @Nullable
    IAgentWebSettings getAgentWebSettings() {
        return new SonicWebSettingImpl(this, mIntent, mSonicSessionClient);
    }

    protected @NonNull
    MiddlewareWebClientBase getMiddleWareWebClient() {
        return this.mMiddleWareWebClient = new MiddlewareWebClientBase() {
        };
    }

//    public @Nullable
//    DefaultWebClient.OpenOtherPageWays getOpenOtherAppWay() {
//        return null;
//    }


    public @Nullable
    AgentWebUIControllerImplBase getAgentWebUIController() {
        return null;
    }

//    public @Nullable
//    IWebLayout getWebLayout() {
//        return null;
//    }

    public @Nullable
    PermissionInterceptor getPermissionInterceptor() {
        return null;
    }


    public int getIndicatorColor() {
        return Color.parseColor("#ff0000");
    }

    public int getIndicatorHeight() {
        return 3;
    }

    public @Nullable
    WebChromeClient getWebChromeClient() {
        return null;
    }

    protected @Nullable
    WebView getWebView() {
        return null;
    }


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
        };
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (null != sonicSession) {
            sonicSession.destroy();
            sonicSession = null;
        }
        super.onDestroy();
    }


    private static class OfflinePkgSessionConnection extends SonicSessionConnection {

        private final WeakReference<Context> context;

        public OfflinePkgSessionConnection(Context context, SonicSession session, Intent intent) {
            super(session, intent);
            this.context = new WeakReference<Context>(context);
        }

        @Override
        protected int internalConnect() {
            Context ctx = context.get();
            if (null != ctx) {
                try {
                    InputStream offlineHtmlInputStream = ctx.getAssets().open("sonic-demo-index.html");
                    responseStream = new BufferedInputStream(offlineHtmlInputStream);
                    return SonicConstants.ERROR_CODE_SUCCESS;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            return SonicConstants.ERROR_CODE_UNKNOWN;
        }

        @Override
        protected BufferedInputStream internalGetResponseStream() {
            return responseStream;
        }

        @Override
        protected String internalGetCustomHeadFieldEtag() {
            return SonicSessionConnection.CUSTOM_HEAD_FILED_ETAG;
        }

        @Override
        public void disconnect() {
            if (null != responseStream) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public int getResponseCode() {
            return 200;
        }

        @Override
        public Map<String, List<String>> getResponseHeaderFields() {
            return new HashMap<>(0);
        }

        @Override
        public String getResponseHeaderField(String key) {
            return "";
        }
    }
}
