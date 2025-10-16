package org.rundeck.util.container

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.CompileStatic
import okhttp3.ConnectionPool
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import org.jetbrains.annotations.NotNull

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * Simple Rundeck API client, in general methods that start with "do*" return the raw okhttp3 Response object, without checking succes status or
 * consuming the response body, and other methods such as get/post/put will check for success status and map the json body to a class.
 */
@CompileStatic
class RdClient {

    /**
     *  The current (latest) released version of the Rundeck API
     */
    public static final int API_CURRENT_VERSION = 56

    final ObjectMapper mapper = new ObjectMapper()
    String baseUrl
    OkHttpClient httpClient
    private AtomicBoolean logNextRequest = new AtomicBoolean()

    // Api version used by this client
    int apiVersion = API_CURRENT_VERSION

    RdClient(String baseUrl, OkHttpClient.Builder builder) {
        this.baseUrl = baseUrl
        this.httpClient = builder.addInterceptor (new TempLogInterceptor(logNextRequest)).build()
    }

    static RdClient create(final String baseUrl, final String apiToken, Map<String, Integer> config = Collections.emptyMap() ) {
        new RdClient(
                baseUrl,
                new OkHttpClient.Builder().
                        addInterceptor(new HeaderInterceptor("X-Rundeck-Auth-token", apiToken)).
                        connectTimeout(config.getOrDefault("connectTimeout", 25), TimeUnit.SECONDS).
                        readTimeout(config.getOrDefault("readTimeout", 25), TimeUnit.SECONDS).
                        writeTimeout(config.getOrDefault("writeTimeout", 25), TimeUnit.SECONDS).
                        connectionPool(new ConnectionPool(2, 25, TimeUnit.SECONDS))
        )
    }
    static class TempLogInterceptor implements Interceptor{
        AtomicBoolean logNextRequest
        TempLogInterceptor(AtomicBoolean logNextRequest){
            this.logNextRequest=logNextRequest
        }
        @Override
        Response intercept(@NotNull Chain chain) throws IOException {
            def request = chain.request()
            if(logNextRequest.getAndSet(false)){
                println("---- REQUEST ----")
                println("${request.method()} ${request.url()}")
                request.headers().toMultimap().each { key, value ->
                    println("$key: $value")
                }
                if(request.body() != null){
                    def buffer = new Buffer()
                    request.body().writeTo(buffer)
                    println()
                    println(buffer.readUtf8())
                }
                println("---- /REQUEST ----")
            }
            return chain.proceed(request)
        }
    }
    /**
     * Log the next request made by this client
     * @return
     */
    RdClient logNextRequest(){
        logNextRequest.set(true)
        return this
    }

    /**
     * Perform a GET request to the specified path
     * @param path The API path, starting with /
     * @return The raw Response object
     */
    Response doGet(final String path) {
        httpClient.newCall(
                new Request.Builder().
                        url(apiUrl(path)).
                        header('Accept', 'application/json').
                        get().
                        build()
        ).execute()
    }

    /**
     * Perform a GET request to the specified path with additional headers
     * @param path The API path, starting with /
     * @param headers Additional headers to include in the request
     * @return The raw Response object
     */
    Response doGetAddHeaders(final String path, Headers headers) {
        httpClient.newCall(
                new Request.Builder().
                        url(apiUrl(path)).
                        headers(headers).
                        get().
                        build()
        ).execute()
    }

    /**
     * Perform a GET request to the specified path with Accept: "*&#47;*"
     * @param path The API path, starting with /
     * @return The raw Response object
     */
    Response doGetAcceptAll(final String path) {
        httpClient.newCall(
                new Request.Builder().
                        url(apiUrl(path)).
                        header('Accept', '*/*').
                        get().
                        build()
        ).execute()
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

    private String apiUrlCustomApiVersion(String path, String customApiVersion) {
        baseUrl + "/api/${customApiVersion}" + path
    }

    Response doRequest(final String path, Consumer<Request.Builder> builderConsumer) {
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
        try (def resp = doGet(path)) {
            if(!resp.successful){
                throw new Exception("GET request failed: " + resp.code() + " " + resp.body().string())
            }
            return mapper.readValue(resp.body().byteStream(), clazz)
        }
    }

    Response doPost(final String path, final Object body = null) {
        doReq(path, "POST", body)
    }
    Response doReq(final String path, String method, final Object body = null) {
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
                .method(method, requestBuilder)

        httpClient.newCall(
                builder.build()
        ).execute()
    }

    void put(final String path, final File file, final String contentType) {
        try(def response = doPut(path, file, contentType)) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to put file to rundeck: " + response.code() + " " + response.body().string())
            }
        }
    }

    Response doPut(final String path, final File file, final String contentType='application/zip') {
        RequestBody body = RequestBody.create(file, MediaType.parse(contentType))
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("PUT", body)
                .build()
        httpClient.newCall(request).execute()
    }

    <T> T putWithJsonBody(final String path, final Object body, Class<T> clazz = Map) {
        try (def resp = doPutWithJsonBody(path, body)) {
            if(!resp.isSuccessful()) {
                throw new IOException("Failed to put json to rundeck: " + resp.code() + " " + resp.body().string())
            }
            return jsonValue(resp.body(), clazz)
        }
    }
    Response doPutWithJsonBody(final String path, final Object body) {
        RequestBody requestBody = RequestBody.create(
                mapper.writeValueAsBytes(body),
                MediaType.parse("application/json")
        )
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .header('Content-Type', 'application/json')
                .header('Accept', 'application/json')
                .method("PUT", requestBody)
                .build()
        httpClient.newCall(request).execute()
    }

    /**
     * Sends a POST request to a specified path with a file and its content type.
     * This method is designed to transmit files, such as images or documents, to a server.
     *
     * @param path The endpoint URL path where the file is to be posted.
     * @param file The file to be sent in the request body. This should be a valid {@link File} object pointing to the file intended for upload.
     * @param contentType The MIME type of the file being sent, e.g., "image/jpeg" for JPEG images. This string must correspond to the file's actual content type.
     * @return The response from the server as a {@link Response} object. This object contains the status code, headers, and body of the response.
     * @throws IOException If an error occurs during the network request. This includes file read errors, network connectivity issues, and server response errors. Callers should handle this exception.
     */
    Response doPost(final String path, final File file, final String contentType) {
        RequestBody body = RequestBody.create(file, MediaType.parse(contentType))
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("POST", body)
                .build()
        httpClient.newCall(request).execute()
    }
    /**
     * Sends a POST request to a specified path with a file and its content type, and returns successfully if the request is successful.
     * This method is designed to transmit files, such as images or documents, to a server.
     *
     * @param path The endpoint URL path where the file is to be posted.
     * @param file The file to be sent in the request body. This should be a valid {@link File} object pointing to the file intended for upload.
     * @param contentType The MIME type of the file being sent, e.g., "image/jpeg" for JPEG images. This string must correspond to the file's actual content type.
     * @throws IOException If an error occurs during the network request. This includes file read errors, network connectivity issues, and server response errors. Callers should handle this exception.
     */
    void post(final String path, final File file, final String contentType) {
        try (def resp = doPost(path, file, contentType)) {
            if (!resp.isSuccessful()) {
                throw new IOException("Failed to post file to rundeck: " + resp.code() + " " + resp.body().string())
            }
        }
    }

    /**
     * Performs a POST request to the specified path with the given file content and content type.
     * Optional headers can also be included in the request.
     *
     * @param path The URL path to which the request is sent.
     * @param file The content to be sent in the request body. This is typically the file content as a string.
     * @param contentType The MIME type of the content being sent, e.g., "application/json".
     * @param headers Optional. Additional headers to include in the request. Default is null, indicating no additional headers.
     * @return The response of the request.
     * @throws IOException If an error occurs during the execution of the request. This exception must be caught or declared to be thrown.
     */
    Response doPost(final String path, final String file, final String contentType, final Headers headers = null) {
        RequestBody body = RequestBody.create(file, MediaType.parse(contentType))
        Request.Builder request = new Request.Builder()
                .url(apiUrl(path))
                .method("POST", body)
        if (headers) {
            request.headers(headers)
        }
        httpClient.newCall(request.build()).execute()
    }

    /**
     * Performs a POST request with multipart content.
     *
     * @param path The path of the resource to which the request will be sent.
     * @param multipartBody The multipart request body.
     * @return The response of the request.
     * @throws IOException If an error occurs during the execution of the request.
     */
    Response doPostWithMultipart(final String path, MultipartBody multipartBody) {
        def request = new Request.Builder()
                .url(apiUrl(path))
                .post(multipartBody)
                .build()
        httpClient.newCall(request).execute()
    }

    /**
     * Performs a POST request with form data content.
     *
     * @param path The path of the resource to which the request will be sent.
     * @param formBody The form data request body.
     * @return The response of the request.
     * @throws IOException If an error occurs during the execution of the request.
     */
    Response doPostWithFormData(final String path, FormBody formBody) {
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .post(formBody)
                .build()
        httpClient.newCall(request).execute()
    }

    Response doPostWithRawText(final String path, final String contentType, final String rawBody) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap( rawBody.toCharArray()))
        RequestBody body = RequestBody.create(Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit()), MediaType.parse(contentType))
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("POST", body)
                .build()
        httpClient.newCall(request).execute()
    }
    Response doPutWithRawText(final String path, final String contentType, final String rawBody) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap( rawBody.toCharArray()))
        RequestBody body = RequestBody.create(Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit()), MediaType.parse(contentType))
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("PUT", body)
                .build()
        httpClient.newCall(request).execute()
    }

    Response doPostWithContentTypeWithoutBody(final String path,String  contentType) {
        RequestBody body = RequestBody.create(new byte[]{});
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("POST", body)
                .header('Content-Type', contentType)
                .build()
        httpClient.newCall(request).execute()
    }

    Response doPostWithFormData(
            final String path,
            final String fileParamName,
            final File file
    ){
        def formDataRequestBody = buildFormDataRequestBody(fileParamName, file)
        Request request = new Request.Builder()
                .url(apiUrl(path))
                .method("POST", formDataRequestBody)
                .header('Content-Type', 'multipart/form-data')
                .header('Accept', '*/*')
                .build()
        httpClient.newCall(request).execute()
    }

    static MultipartBody buildFormDataRequestBody(
            final String fileParamName,
            final File file
    ){
        return new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        fileParamName,
                        "file",
                        RequestBody.create(MediaType.parse("application/octet-stream"), file)
                ).build()
    }

    /**
     * Performs a POST request to the specified path with an optional body and maps the JSON response to the specified class type.
     * @param path
     * @param body
     * @param clazz
     * @return
     */
    <T> T post(final String path, final Object body = null, Class<T> clazz = Map) {
        def req = doReq(path, 'POST', body)
        if(!req.successful){
            throw new Exception("POST request failed: " + req.code() + " " + req.body().string())
        }
        jsonValue(req.body(), clazz)
    }
    /**
     * Performs a PUT request to the specified path with an optional body and maps the JSON response to the specified class type.
     * @param path
     * @param body
     * @param clazz
     * @return
     */
    <T> T put(final String path, final Object body = null, Class<T> clazz = Map) {
        def req = doReq(path, 'PUT', body)
        if(!req.successful){
            throw new Exception("PUT request failed: " + req.code() + " " + req.body().string())
        }
        jsonValue(req.body(), clazz)
    }

    /**
     * Maps the JSON response body to the specified class type, closes the underlying response stream.
     * @param body
     * @param clazz
     * @return
     */
    <T> T jsonValue(ResponseBody body, Class<T> clazz) {
        try (def b = body.byteStream()) {
            mapper.readValue(b, clazz)
        }
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