package com.tencent.sonic.demo.agent;

import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;

import com.just.agentweb.AgentWeb;
import com.just.agentweb.AgentWebUIControllerImplBase;
import com.just.agentweb.IAgentWebSettings;
import com.just.agentweb.MiddlewareWebChromeBase;
import com.just.agentweb.MiddlewareWebClientBase;
import com.just.agentweb.PermissionInterceptor;
import com.just.agentweb.WebChromeClient;
import com.tencent.sonic.R;

public class AgentActivity extends AppCompatActivity {
    
    private SonicImpl mSonicImpl;
    private Intent mIntent;

    public final static String PARAM_URL = "param_url";

    private FrameLayout mContainer;
    private AgentWeb mAgentWeb;
    private MiddlewareWebChromeBase mMiddleWareWebChrome;
    private MiddlewareWebClientBase mMiddleWareWebClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        mIntent = getIntent();
        String url = mIntent.getStringExtra(PARAM_URL);

        // 1. 首先创建SonicImpl
        mSonicImpl = new SonicImpl(url, this);
        // 2. 调用 onCreateSession
        mSonicImpl.onCreateSession();
        //3. 创建AgentWeb ，注意创建AgentWeb的时候应该使用加入SonicWebViewClient中间件
        setContentView(R.layout.activity_agent);
        mContainer = this.findViewById(R.id.container);
        //5. 最后绑定AgentWeb
        bindAgentWeb();
    }

    private void bindAgentWeb() {
        mAgentWeb = AgentWeb.with(this)
                .setAgentWebParent(mContainer, new FrameLayout.LayoutParams(-1, -1))
                .useDefaultIndicator(getIndicatorColor(), getIndicatorHeight())
                .setWebChromeClient(getWebChromeClient())
                .setWebViewClient(mSonicImpl.getWebViewClient())
                .setPermissionInterceptor(getPermissionInterceptor())
                .setAgentWebUIController(getAgentWebUIController())
                .interceptUnkownUrl()
                .useMiddlewareWebChrome(getMiddleWareWebChrome())
                .useMiddlewareWebClient(getMiddleWareWebClient())
                .setAgentWebWebSettings(getAgentWebSettings())
//                .setMainFrameErrorView(mErrorLayoutEntity.layoutRes, mErrorLayoutEntity.reloadId)
                .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK)
                .createAgentWeb()
                .ready()
                .get();

        mSonicImpl.bindAgentWeb(mAgentWeb);
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

    public @Nullable
    PermissionInterceptor getPermissionInterceptor() {
        return null;
    }

    public @Nullable
    AgentWebUIControllerImplBase getAgentWebUIController() {
        return null;
    }

    public @NonNull
    MiddlewareWebChromeBase getMiddleWareWebChrome() {
        return this.mMiddleWareWebChrome = new MiddlewareWebChromeBase() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
//                setTitle(view, title);
            }
        };
    }

    protected @NonNull
    MiddlewareWebClientBase getMiddleWareWebClient() {
        return this.mMiddleWareWebClient = new MiddlewareWebClientBase() {
        };
    }

    public @Nullable
    IAgentWebSettings getAgentWebSettings() {
        return new SonicWebSettingImpl(this, mIntent, mSonicImpl.getSonicSessionClient());
    }

    @Override
    protected void onDestroy() {
        if (null != mSonicImpl) {
            mSonicImpl.destroy();
            mSonicImpl = null;
        }
        super.onDestroy();
    }
}