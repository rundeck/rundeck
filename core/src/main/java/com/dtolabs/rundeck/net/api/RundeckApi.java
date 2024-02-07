package com.dtolabs.rundeck.net.api;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;
import com.dtolabs.rundeck.net.model.ProjectImportStatus;

import java.util.Map;


/**
 * Interface for Rundeck API using retrofit annotations
 */
@SuppressWarnings("JavaDoc")
public interface RundeckApi {

    /**
     * Import project archive (&lt;=v18)
     *
     * @param project project
     *
     * @return archive response
     */
    @Headers("Accept: application/json")
    @PUT("project/{project}/import")
    Call<ProjectImportStatus> importProjectArchive(
            @Path("project") String project,
            @Query("jobUuidOption") String jobUuidOption,
            @Query("importExecutions") Boolean importExecutions,
            @Query("importConfig") Boolean importConfig,
            @Query("importACL") Boolean importACL,
            @Query("importScm") Boolean importScm,
            @Query("importWebhooks") Boolean importWebhooks,
            @Query("whkRegenAuthTokens") Boolean whkRegenAuthTokens,
            @Query("importNodesSources") Boolean importNodesSources,
            @QueryMap Map<String,String> params,
            @Body RequestBody body
    );
}
