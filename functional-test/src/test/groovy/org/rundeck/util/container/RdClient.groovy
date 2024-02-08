package org.rundeck.util.container

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import okhttp3.ConnectionPool
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.jetbrains.annotations.NotNull

import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

@CompileStatic
class RdClient {
    final ObjectMapper mapper = new ObjectMapper()
    String baseUrl
    OkHttpClient httpClient
    int apiVersion = 45

    RdClient(String baseUrl, OkHttpClient httpClient) {
        this.baseUrl = baseUrl
        this.httpClient = httpClient
    }

    static RdClient create(final String baseUrl, final String apiToken) {
        new RdClient(
                baseUrl,
                new OkHttpClient.Builder().
                        addInterceptor(new HeaderInterceptor("X-Rundeck-Auth-token", apiToken)).
                        connectionPool(new ConnectionPool(2, 10, TimeUnit.SECONDS)).
                        build()
        )
    }


    Response doGetCustomApiVersion(final String path, final String customApiVersion) {
        httpClient.newCall(
                new Request.Builder().
                        url(apiUrlCustomApiVersion(path, customApiVersion)).
                        header('Accept', '*/*').
                        get().
                        build()
        ).execute()
    }

    Response doGet(final String path) {
        httpClient.newCall(
                new Request.Builder().
                        url(apiUrl(path)).
                        header('Accept', 'application/json').
                        get().
                        build()
        ).execute()
    }

    Response doGetAddHeaders(final String path, Headers headers) {
        httpClient.newCall(
                new Request.Builder().
                        url(apiUrl(path)).
                        headers(headers).
                        get().
                        build()
        ).execute()
    }

    Response doGetAcceptAll(final String path) {
        httpClient.newCall(
                new Request.Builder().
                        url(apiUrl(path)).
                        header('Accept', '*/*').
                        get().
                        build()
        ).execute()
    }

    Response request(final String path, Consumer<Request.Builder> builderConsumer) {
        def builder = new Request.Builder()
        builder.
                url(apiUrl(path)).
                header('Accept', 'application/json').
                get()
        if (builderConsumer) {
            builderConsumer.accept(builder)
        }
        def req = builder.build()
        httpClient.newCall(
                req
        ).execute()
    }

    Response doDelete(final String path) {
        httpClient.newCall(
                new Request.Builder().
                        url(apiUrl(path)).
                        header('Accept', 'application/json').
                        delete().
                        build()
        ).execute()
    }


    private String apiUrl(String path) {
        baseUrl + "/api/${apiVersion}" + path
    }

    private String apiUrlCustomApiVersion(String path, String customApiVersion) {
        baseUrl + "/api/${customApiVersion}" + path
    }

    <T> T get(final String path, Class<T> clazz) {
        mapper.readValue(doGet(path).body().byteStream(), clazz)
    }

    Response doPost(final String path, final Object body = null) {
        RequestBody requestBuilder
        if (body) {
            requestBuilder = RequestBody.create(
                    mapper.writeValueAsBytes(body),
                    MediaType.parse("application/json"))
        } else {
            requestBuilder = RequestBody.create()
        }
        def builder = new Request.Builder()
                .url(apiUrl(path))
                .header('Accept', 'application/json')
                .method("POST", requestBuilder)

        httpClient.newCall(
                builder.build()
        ).execute()
    }

    Response doPut(final String path, final File file) {
        RequestBody body = RequestBody.create(file, MediaType.parse("application/zip"))
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("PUT", body)
                .build()
        httpClient.newCall(request).execute()
    }

    Response doPutWithJsonBody(final String path, final Object body) {
        RequestBody requestBody = RequestBody.create(
                mapper.writeValueAsBytes(body),
                MediaType.parse("application/json")
        )
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("PUT", requestBody)
                .build()
        httpClient.newCall(request).execute()
    }

    Response doPost(final String path, final File file, final String contentType) {
        RequestBody body = RequestBody.create(file, MediaType.parse(contentType))
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("POST", body)
                .build()
        httpClient.newCall(request).execute()
    }

    Response doPostWithRawText(final String path, final String contentType, final String rawBody) {
        RequestBody body = RequestBody.create(rawBody, MediaType.parse(contentType))
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("POST", body)
                .build()
        httpClient.newCall(request).execute()
    }

    Response doPostWithoutBody(final String path) {
        RequestBody body = RequestBody.create(null, new byte[]{});
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("POST", body)
                .build()
        httpClient.newCall(request).execute()
    }

    <T> T post(final String path, final Object body = null, Class<T> clazz = Map) {
        jsonValue(doPost(path, body).body(), clazz)
    }

    <T> T jsonValue(ResponseBody body, Class<T> clazz) {
        mapper.readValue(body.byteStream(), clazz)
    }


    static class HeaderInterceptor implements Interceptor {
        String header
        String value

        HeaderInterceptor(final String header, final String value) {
            this.header = header
            this.value = value
        }

        @Override
        Response intercept(@NotNull final Chain chain) throws IOException {
            return chain.proceed(
                    chain.request().newBuilder().
                            addHeader(header, value).
                            build()
            )
        }
    }
}