package com.dtolabs.rundeck.net.api.util;


import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

/**
 * Send a header value
 */
public class StaticHeaderInterceptor implements Interceptor {
    final String name;
    final String value;

    public StaticHeaderInterceptor(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder().header(name, value).build());
    }

}