package com.tencent.sonic.sdk.net.interceptor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import timber.log.Timber;

public class HttpLogInterceptor implements Interceptor {

    private final String logTag = "NetLog";
    private final String requestPrefixStart = "--->";
    private final String requestPrefixEnd = "--------------------------------------->";
    private final String responsePrefixStart = "<---";
    private final String responsePrefixEnd = "<-------------------------------------";
    private final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        RequestBody requestBody = request.body();
        Connection connection = chain.connection();
        String requestMessage = requestPrefixStart + request.method() + " " + request.url() + " " + connection.protocol() + "\n";
        //解析请求头
        requestMessage += parseHeaders(request.headers());

        if (bodyHasUnknownEncoding(request.headers())) {
            requestMessage += "\n" + requestPrefixEnd + " END " + request.method() + " (encoded body omitted)";
        } else if (requestBody != null) {
            requestMessage += "\n";
            MediaType contentType = requestBody.contentType();
            if (contentType.toString().contains("multipart")) {
                requestMessage += "\n" + requestPrefixEnd + " END " + request.method() + " (multipart binary " + requestBody.contentLength() + "} -byte body omitted)";
            } else {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                Charset charset = UTF8;
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }
                if (isPlaintext(buffer)) {
                    requestMessage += buffer.readString(charset);
                    requestMessage += "\n" + requestPrefixEnd + " END " + request.method();
                } else {
                    requestMessage += "\n" + requestPrefixEnd + " END " + request.method() + " (binary " + requestBody.contentLength() + " -byte body omitted)";
                }
            }
        } else {
            requestMessage += "\n" + requestPrefixEnd + " END " + request.method() + " (no request body)";
        }

        //打印请求信息
        Timber.tag(logTag).d(requestMessage);

        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            Timber.tag(logTag).d(responsePrefixStart + " HTTP FAILED: " + e);
            throw e;
        }

        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        String bodySize;

        if (contentLength != -1L)
            bodySize = contentLength + "-byte";
        else
            bodySize = "unknown-length";

        String responseMessage = responsePrefixStart + " " + response.code() + " " + request.method() + " " + response.message() + " ";
        responseMessage += response.request().url();
        responseMessage += " (" + tookMs + "ms" + ", " + bodySize + " body)\n";

        Headers headers = response.headers();
        responseMessage += parseHeaders(headers);
        if (!HttpHeaders.hasBody(response)) {
            responseMessage += "\n" + responsePrefixEnd + " END HTTP";
        } else if (bodyHasUnknownEncoding(response.headers())) {
            responseMessage += "\n" + responsePrefixEnd + " END HTTP (encoded body omitted)";
        } else {
            BufferedSource source = responseBody.source();
            source.request(java.lang.Long.MAX_VALUE); // Buffer the entire body.
            Buffer buffer = source.buffer();
            Long gzippedLength = null;
            if ("gzip".equalsIgnoreCase(headers.get("Content-Encoding"))) {
                gzippedLength = buffer.size();
                GzipSource gzipSource = new GzipSource(buffer.clone());

                buffer = new Buffer();
                while (gzipSource.read(buffer, 128) != 0) ;
            }

            Charset charset = UTF8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }

            responseMessage += "\n";
            if (!isPlaintext(buffer)) {
                responseMessage += "\n" + responsePrefixEnd + " END HTTP (binary " + buffer.size() + "-byte body omitted)";
                return response;
            }

            if (contentLength != 0L) {
                String responseData = buffer.clone().readString(charset);
                try {
                    responseMessage += new JSONObject(responseData).toString(2);
                } catch (JSONException e) {
                    responseMessage = responseData;
                }
            }


            if (gzippedLength != null) {
                responseMessage += "\n" + responsePrefixEnd + " END HTTP (" + buffer.size() + "-byte, " + gzippedLength + "-gzipped-byte body)";
            } else {
                responseMessage += "\n" + responsePrefixEnd + " END HTTP (" + buffer.size() + "-byte body)";
            }
        }

        Timber.tag(logTag).d(responseMessage);


        return response;
    }

    private String parseHeaders(Headers headers) {
        String headerStr = "";
        for (String name : headers.names()) {
            headerStr += "\n    " + name + ": " + headers.get(name);
        }
        return "headers =  " + headerStr + "\n}";
    }

    private boolean bodyHasUnknownEncoding(Headers headers) {
        String contentEncoding = headers.get("Content-Encoding");
        return (contentEncoding != null
                && !contentEncoding.equalsIgnoreCase("identity")
                && !contentEncoding.equalsIgnoreCase("gzip"));
    }

    private boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = 0;
            if (buffer.size() < 64)
                byteCount = buffer.size();
            else
                byteCount = 64;

            buffer.copyTo(prefix, 0, byteCount);

            for (int i = 0; i < 15; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }

            return true;
        } catch (EOFException e) {
            return false;// Truncated UTF-8 sequence.
        }
    }

}
