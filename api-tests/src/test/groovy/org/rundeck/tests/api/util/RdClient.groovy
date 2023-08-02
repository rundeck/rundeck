package org.rundeck.tests.api.util

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.jetbrains.annotations.NotNull

import java.util.function.Consumer

@CompileStatic
class RdClient {
    ObjectMapper mapper = new ObjectMapper()
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
                build()
        )
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

    <T> T get(final String path, Class<T> clazz) {
        mapper.readValue(doGet(path).body().byteStream(), clazz)
    }

    Response doPost(final String path, final Object body = null) {
        def builder = new Request.Builder().
            url(apiUrl(path)).
            header('Accept', 'application/json')
        if (body) {
            builder.post(
                RequestBody.create(
                    mapper.writeValueAsBytes(body),
                    MediaType.parse("application/json")
                )
            )
        }
        httpClient.newCall(
            builder.build()
        ).execute()
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
