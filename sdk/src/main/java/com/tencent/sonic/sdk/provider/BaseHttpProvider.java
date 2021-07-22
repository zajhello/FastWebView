package com.tencent.sonic.sdk.provider;

import com.tencent.sonic.sdk.net.DynamicUrlHandle;
import com.tencent.sonic.sdk.net.RequestHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.internal.Util;

public class BaseHttpProvider implements IHttpProvider {


    @Override
    public boolean enableLog() {
        return true;
    }

    @Override
    public List<Interceptor> interceptors() {
        return new ArrayList<>();
    }

    @Override
    public List<Interceptor> networkInterceptors() {
        return new ArrayList<>();
    }

    @Override
    public RequestHandle requestOperator() {
        return new DynamicUrlHandle();
    }

    @Override
    public void httpBuilder(OkHttpClient.Builder builder) {

    }

    @Override
    public Dispatcher dispatcher() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 10, 60, TimeUnit.SECONDS,
                new SynchronousQueue(), Util.threadFactory("OkHttp Dispatcher", false));
        Dispatcher dispatcher = new Dispatcher(executor);
        dispatcher.setMaxRequests(10);
        return dispatcher;
    }

    @Override
    public ConnectionPool connectionPool() {
        return new ConnectionPool(5, 10, TimeUnit.SECONDS);
    }
}
