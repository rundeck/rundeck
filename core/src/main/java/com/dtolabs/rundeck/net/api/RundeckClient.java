package com.dtolabs.rundeck.net.api;

import com.dtolabs.rundeck.net.model.ProjectImportStatus;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import java.io.IOException;
import java.util.Map;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.jaxb.JaxbConverterFactory;

import okhttp3.OkHttpClient;


public class RundeckClient {

    public static final String APPLICATION_ZIP = "application/zip";
    public static final MediaType MEDIA_TYPE_ZIP = MediaType.parse(APPLICATION_ZIP);

    private final RundeckApi rundeckApi;

    public RundeckClient(String url, String apiToken) {
        Retrofit retrofit;
        OkHttpClient.Builder okhttp = new OkHttpClient.Builder();
        okhttp.addInterceptor(chain -> chain.proceed(chain.request().newBuilder().header("X-Rundeck-Auth-Token", apiToken).build()));
        final String apiBaseUrl;
        apiBaseUrl = buildApiUrlForVersion(url, 39);

        retrofit = new Retrofit.Builder()
                .baseUrl(apiBaseUrl)
                .client(okhttp.build())
                .addConverterFactory(JacksonConverterFactory.create())
                .addConverterFactory(JaxbConverterFactory.create())
                .build();

        rundeckApi = retrofit.create(RundeckApi.class);

    }
    public Response<ProjectImportStatus> importProjectArchive(
            String projectName,
            String jobUuidOption,
            Boolean importExecutions,
            Boolean importConfig,
            Boolean importACL,
            Boolean importScm,
            Boolean importWebhooks,
            Boolean whkRegenAuthTokens,
            Boolean importNodesSources,
            Map<String,String> params,
            RequestBody requestBody
    ) throws IOException {

        return rundeckApi.importProjectArchive(
                projectName,
                jobUuidOption,
                importExecutions,
                importConfig,
                importACL,
                importScm,
                importWebhooks,
                whkRegenAuthTokens,
                importNodesSources,
                params,
                requestBody
        ).execute();
    }

    private static String buildApiUrlForVersion(String baseUrl, final int apiVers) {
        if (!baseUrl.matches("^.*/api/\\d+/?$")) {
            return normalizeUrlPath(baseUrl) + "api/" + (apiVers) + "/";
        }
        return normalizeUrlPath(baseUrl);
    }

    private static String normalizeUrlPath(String baseUrl) {
        if (!baseUrl.matches(".*/$")) {
            return baseUrl + "/";
        }
        return baseUrl;
    }
}
