/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dtolabs.rundeck.core.http;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;

import java.net.ProxySelector;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class ApacheHttpClient implements HttpClient<HttpResponse> {

    HttpClientBuilder clientBuilder =
            HttpClients.custom()
                    .useSystemProperties()
                    .setRoutePlanner(new SystemDefaultRoutePlanner(
                            ProxySelector.getDefault()
                    ));
    RequestConfig.Builder rqConfigBuilder = RequestConfig.custom();
    HttpClientContext clientContext = null;
    HttpRequestBase request = null;
    URI                uri;
    Map<String,String> headers = new HashMap<>();

    @Override
    public HttpClient<HttpResponse> setUri(final URI uri) {
        this.uri = uri;
        this.request = new HttpGet(uri);
        return this;
    }

    @Override
    public HttpClient<HttpResponse> setFollowRedirects(final boolean redirects) {
        rqConfigBuilder.setRedirectsEnabled(redirects);
        return this;
    }

    @Override
    public HttpClient<HttpResponse> setBasicAuthCredentials(final String username, final String password) {
        if(uri == null) throw new IllegalStateException("The URI must be set first. Please call setUri(yourTargetUri)");
        BasicCredentialsProvider credProvider = new BasicCredentialsProvider();
        credProvider.setCredentials(new AuthScope(uri.getHost(),uri.getPort(),AuthScope.ANY_REALM,"BASIC"), new UsernamePasswordCredentials(username, password));
        clientBuilder.setDefaultCredentialsProvider(credProvider);
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        HttpHost target = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
        authCache.put(target, basicAuth);
        clientContext = HttpClientContext.create();
        clientContext.setAuthCache(authCache);
        return this;
    }

    @Override
    public HttpClient<HttpResponse> setTimeout(final int timeoutMs) {
        rqConfigBuilder.setConnectTimeout(timeoutMs)
                       .setSocketTimeout(timeoutMs);
        return this;
    }

    @Override
    public HttpClient<HttpResponse> setMethod(final Method method) {
        if(uri == null) throw new IllegalStateException("The URI must be set first. Please call setUri(yourTargetUri)");
        if(method == Method.GET) request = new HttpGet(uri);
        else if(method == Method.POST) request = new HttpPost(uri);
        else if(method == Method.PUT) request = new HttpPut(uri);
        else throw new UnsupportedOperationException(String.format("Method %s not implemented",method.name()));
        return this;
    }

    @Override
    public HttpClient<HttpResponse> setRetryCount(final int count) {
        clientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(count,false));
        return this;
    }

    @Override
    public HttpClient<HttpResponse> addHeader(final String name, final String value) {
        headers.put(name,value);
        return this;
    }

    @Override
    public HttpClient<HttpResponse> addPayload(final String contentType, final String payload) {
        if(uri == null) throw new IllegalStateException("The URI must be set first. Please call setUri(yourTargetUri)");
        if(!(request instanceof HttpEntityEnclosingRequestBase)) throw new IllegalStateException("Request must be either Post or Put. Make sure to call setMethod(Method.POST | Method.PUT) before attempting to add a payload.");
        ((HttpEntityEnclosingRequestBase)request).setEntity(new StringEntity(payload, ContentType.create(contentType, "UTF-8")));
        return this;
    }

    @Override
    public void execute(RequestProcessor<HttpResponse> processor) throws Exception {
        if(uri == null) throw new IllegalStateException("The URI must be set before executing the request");

        try (CloseableHttpClient client = clientBuilder.setDefaultRequestConfig(rqConfigBuilder.build()).build()) {
            headers.forEach((n, v) -> request.addHeader(n, v));
            HttpResponse rsp = clientContext != null ? client.execute(request, clientContext) : client.execute(request);
            processor.accept(rsp);
        }
    }
}
