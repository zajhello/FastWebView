package com.tencent.sonic.sdk.net;

import android.text.TextUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Dns;

public class HttpDns implements Dns {

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
//        String ip = HttpDnsHelper.getIpByHost(hostname);
//        if (TextUtils.isEmpty(ip)) {
//            //返回自己解析的地址列表
//            return Arrays.asList(InetAddress.getAllByName(ip));
//        } else {
//            // 解析失败，使用系统解析
//            return Dns.SYSTEM.lookup(hostname);
//        }

        return  null;
    }
}
