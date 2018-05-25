package com.dtolabs.rundeck.net.api;


import com.dtolabs.rundeck.net.api.util.StaticHeaderInterceptor;
import com.dtolabs.rundeck.net.model.ErrorDetail;
import com.dtolabs.rundeck.net.model.ErrorResponse;
import com.dtolabs.rundeck.net.model.ProjectImportStatus;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.log4j.Logger;
import retrofit2.Call;
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

    static Logger projectLogger = Logger.getLogger("org.rundeck.project.events");
    private static final String APPLICATION_ZIP = "application/zip";
    private static final MediaType MEDIA_TYPE_ZIP = MediaType.parse(APPLICATION_ZIP);

    private final long HTTP_TIMEOUT_MIN = 10;

    public Client(String url, String authToken){
        this.url = url;
        this.authToken = authToken;
        this.builder = new OkHttpClient.Builder();
        builder.addInterceptor(new StaticHeaderInterceptor("X-Rundeck-Auth-Token", authToken));
        builder.readTimeout(HTTP_TIMEOUT_MIN, TimeUnit.MINUTES);
        builder.connectTimeout(HTTP_TIMEOUT_MIN, TimeUnit.MINUTES);
        builder.writeTimeout(HTTP_TIMEOUT_MIN, TimeUnit.MINUTES);

        retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .client(builder.build())
                .addConverterFactory(JacksonConverterFactory.create()
                )
                .build();
    }
    public void setApi(RundeckApi api){
        this.api = api;
    }


    public ProjectImportStatus importProjectArchive(String project, File file, boolean preserveUuids,
                                                    boolean importExecutions,
                                                    boolean importConfig,boolean importACL) throws IOException{
        ProjectImportStatus response = new ProjectImportStatus();
        response.successful = true;
        RequestBody body = RequestBody.create(MEDIA_TYPE_ZIP, file);
        Response<ProjectImportStatus> status = getApi().importProjectArchive(project,preserveUuids? "remove" : "preserve",importExecutions,importConfig,importACL,body).execute();
        if(status.isSuccessful()){
            if(null != status.body()) {
                response = status.body();
                if(!response.getResultSuccess()){
                    if(null != response.errors) {
                        projectLogger.error(
                                String.format("Error on import jobs to new project: %d", response.errors.size())
                        );
                    }
                    if(null != response.executionErrors) {
                        projectLogger.error(
                                String.format("Error on import executions to new project: %d", response.executionErrors.size())
                        );
                    }if(null != response.aclErrors) {
                        projectLogger.error(
                                String.format("Error on import acls to new project: %d", response.aclErrors.size())
                        );
                    }
                }
            }else{
                projectLogger.error("Null body on response");
                response.successful=false;
            }

        }else{
            ResponseBody responseBody = status.errorBody();
            Converter<ResponseBody, ErrorResponse> errorConverter = retrofit.responseBodyConverter(
                    ErrorResponse.class,
                    new Annotation[0]
            );
            ErrorDetail error = errorConverter.convert(responseBody);
            if (status.code() == 401 || status.code() == 403) {
                throw new RuntimeException(
                        String.format("Authorization failed: %d %s", status.code(), error.getErrorMessage()));
            }
            if (status.code() == 409) {
                throw new RuntimeException(String.format(
                        "Could not create resource: %d %s",
                        status.code(),
                        error.getErrorMessage()
                ));
            }
            if (status.code() == 404) {
                throw new RuntimeException(String.format(
                        "Could not find resource:  %d %s",
                        status.code(),
                        error.getErrorMessage()
                ));
            }
            throw new RuntimeException(
                    String.format("Request failed:  %d %s", status.code(), error.getErrorMessage()));
        }

        return response;
    }

    private RundeckApi getApi(){
        if(api == null){
            api = retrofit.create(RundeckApi.class);
        }
        return api;
    }

}
