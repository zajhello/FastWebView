package com.tencent.sonic.sdk.net.interceptor;

import com.tencent.sonic.sdk.net.RequestHandle;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class HttpInterceptor implements Interceptor {

    private RequestHandle handle;

    public HttpInterceptor(RequestHandle handle) {
        this.handle = handle;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (handle != null)
            request = handle.onBeforeRequest(request, chain);

        Response response = chain.proceed(request);
        if (handle != null) {
            return handle.onAfterRequest(response, chain);
        }
        return response;
    }
}
