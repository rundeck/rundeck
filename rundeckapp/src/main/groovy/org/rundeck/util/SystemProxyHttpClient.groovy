package org.rundeck.util

import com.dtolabs.rundeck.core.http.HttpClient
import com.dtolabs.rundeck.core.http.RequestProcessor
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.apache.http.util.EntityUtils

class SystemProxyHttpClient implements HttpClient<HttpResponse> {
    private final CloseableHttpClient apache
    private URI uri
    private Method method = Method.GET
    private Map<String,String> headers = [:]
    private String payload

    SystemProxyHttpClient() {
        def routePlanner = new SystemDefaultRoutePlanner(ProxySelector.getDefault())
        apache = HttpClientBuilder.create()
                .useSystemProperties()
                .setRoutePlanner(routePlanner)
                .build()
    }

    @Override
    HttpClient<HttpResponse> setUri(URI uri) { this.uri = uri; return this }

    @Override
    HttpClient<HttpResponse> setMethod(Method method) { this.method = method; return this }

    @Override
    HttpClient<HttpResponse> addHeader(String name, String value) {
        headers[name] = value
        return this
    }

    @Override
    HttpClient<HttpResponse> addPayload(String contentType, String payload) {
        this.payload = payload
        return this
    }

    @Override
    void execute(RequestProcessor<HttpResponse> processor) throws Exception {
        def req = (method == Method.POST) ? new HttpPost(uri) : new HttpGet(uri)
        headers.each { k, v -> req.addHeader(k, v) }
        if (req instanceof HttpPost && payload) {
            req.setEntity(new StringEntity(payload, "UTF-8"))
        }
        def resp = apache.execute(req)
        try {
            processor.accept(resp)
        } finally {
            EntityUtils.consumeQuietly(resp.getEntity())
        }
    }

    @Override HttpClient<HttpResponse> setFollowRedirects(boolean redirects) { this }
    @Override HttpClient<HttpResponse> setBasicAuthCredentials(String u, String p) { this }
    @Override HttpClient<HttpResponse> setTimeout(int timeoutMs) { this }
    @Override HttpClient<HttpResponse> setRetryCount(int count) { this }
}
