package com.dtolabs.rundeck.net.api;


import com.dtolabs.rundeck.net.api.util.StaticHeaderInterceptor;
import com.dtolabs.rundeck.net.model.ErrorDetail;
import com.dtolabs.rundeck.net.model.ErrorResponse;
import com.dtolabs.rundeck.net.model.ProjectImportStatus;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import okhttp3.OkHttpClient;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

public class Client {
    private String url;
    private String authToken;
    private RundeckApi api;
    private OkHttpClient.Builder builder;
    private Retrofit retrofit;

    private static final String APPLICATION_ZIP = "application/zip";
    private static final MediaType MEDIA_TYPE_ZIP = MediaType.parse(APPLICATION_ZIP);

    private final long HTTP_TIMEOUT_SEC = 180;

    public Client(String url, String authToken){
        this.url = url;
        this.authToken = authToken;
        this.builder = new OkHttpClient.Builder();
        api = createApi();
    }


    public ProjectImportStatus importProjectArchive(String project, File file, boolean preserveUuids,
                                                    boolean importExecutions,
                                                    boolean importConfig,boolean importACL) throws IOException{
        ProjectImportStatus response = new ProjectImportStatus();
        response.successful = true;
        RequestBody body = RequestBody.create(MEDIA_TYPE_ZIP, file);
        boolean anyerror = false;
        Response<ProjectImportStatus> status = api.importProjectArchive(project,preserveUuids? "remove" : "preserve",importExecutions,importConfig,importACL,body).execute();
        //TODO log errors
        if(status.isSuccessful()){
            if(null != status.body() && status.body().getResultSuccess()){
                response = status.body();
            }else{
                System.out.println(status.body());
                if(null != status.body()){
                    response = status.body();
                    System.out.println(status.body().aclErrors);
                    System.out.println(status.body().errors);
                    System.out.println(status.body().executionErrors);
                }
            }

        }else{
            ResponseBody responseBody = status.errorBody();
            Converter<ResponseBody, ErrorResponse> errorConverter = retrofit.responseBodyConverter(
                    ErrorResponse.class,
                    new Annotation[0]
            );
            ErrorDetail error = errorConverter.convert(responseBody);
            if (error.getErrorCode().equals("401") || error.getErrorCode().equals("403")) {
                //authorization
                throw new RuntimeException(
                        String.format("Authorization failed: %s %s", error.getErrorCode(), error.getErrorMessage()));
            }
            if (error.getErrorCode().equals("409")) {
                //authorization
                throw new RuntimeException(String.format(
                        "Could not create resource: %s %s",
                        error.getErrorCode(),
                        error.getErrorMessage()
                ));
            }
            if (error.getErrorCode().equals("404")) {
                //authorization
                throw new RuntimeException(String.format(
                        "Could not find resource:  %s %s",
                        error.getErrorCode(),
                        error.getErrorMessage()
                ));
            }
            throw new RuntimeException(
                    String.format("Request failed:  %s %s", error.getErrorCode(), error.getErrorMessage()));
        }

        return response;
    }

    private RundeckApi createApi(){
        builder.addInterceptor(new StaticHeaderInterceptor("X-Rundeck-Auth-Token", authToken));
        builder.readTimeout(HTTP_TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.connectTimeout(HTTP_TIMEOUT_SEC, TimeUnit.SECONDS);
        builder.writeTimeout(HTTP_TIMEOUT_SEC, TimeUnit.SECONDS);

        retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(builder.build())
                .addConverterFactory(JacksonConverterFactory.create()
                )
                .build();

        RundeckApi api = retrofit.create(RundeckApi.class);
        return api;
    }

}
