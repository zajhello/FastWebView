package com.tencent.sonic.sdk.provider;


import com.tencent.sonic.sdk.net.RequestHandle;

import java.util.List;

import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * 网络配置接口，通过实现此接口以完成自定义网络配置
 */
public interface IHttpProvider {

    /**
     * 连接超时设置
     */
    long connectTimeOut = 10;


    /**
     * 读数据超时
     */
    long readTimeOut = 10;

    /**
     * 写数据超时
     */
    long writeTimeOut = 10;

    /**
     * 是否开启日志
     */
    boolean enableLog();

    /**
     * 拦截器
     */
    List<Interceptor> interceptors();

    List<Interceptor> networkInterceptors();

    /**
     * 网络请求自定义操作
     */
    RequestHandle requestOperator();


    /**
     * okhttpclient builder配置,对外开放扩展
     */
    void httpBuilder(OkHttpClient.Builder builder);

    Dispatcher dispatcher();

    ConnectionPool connectionPool();
}
