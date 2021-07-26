package com.tencent.sonic.sdk.net;

import com.tencent.sonic.sdk.net.interceptor.HttpInterceptor;
import com.tencent.sonic.sdk.net.interceptor.HttpLogInterceptor;
import com.tencent.sonic.sdk.provider.BaseHttpProvider;
import com.tencent.sonic.sdk.provider.IHttpProvider;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class OkHttpUtils {


    private volatile static OkHttpUtils mInstance;
    private static OkHttpClient mOkHttpClient;


    public OkHttpClient Get() {
        return mOkHttpClient;
    }

    public OkHttpUtils() {
        mOkHttpClient = createHttpClient(new BaseHttpProvider());
    }

    private OkHttpClient createHttpClient(IHttpProvider provider) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.connectTimeout(5, TimeUnit.SECONDS);
        builder.readTimeout(15, TimeUnit.SECONDS);

        builder.dispatcher(provider.dispatcher());
        builder.connectionPool(provider.connectionPool());

        RequestHandle operator = provider.requestOperator();
        builder.addInterceptor(new HttpInterceptor(operator));



//        if (provider.enableLog()) {
//            builder.addInterceptor(new HttpLogInterceptor());
//        }
        return builder.build();
    }

    public static OkHttpUtils initClient() {
        if (mInstance == null) {
            synchronized (OkHttpUtils.class) {
                if (mInstance == null) {
                    mInstance = new OkHttpUtils();
                }
            }
        }
        return mInstance;
    }

    public static OkHttpUtils getInstance() {
        return initClient();
    }
}
