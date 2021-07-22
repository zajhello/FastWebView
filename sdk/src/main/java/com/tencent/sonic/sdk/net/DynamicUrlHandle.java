package com.tencent.sonic.sdk.net;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class DynamicUrlHandle implements RequestHandle {
    @Override
    public Request onBeforeRequest(Request request, Interceptor.Chain chain) {
        return request;
    }

    @Override
    public Response onAfterRequest(Response response, Interceptor.Chain chain) {
        return response;
    }
}
