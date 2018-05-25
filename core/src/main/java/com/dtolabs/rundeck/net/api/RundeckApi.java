package com.dtolabs.rundeck.net.api;

import com.dtolabs.rundeck.net.model.ProjectImportStatus;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface RundeckApi {

    @Headers("Accept: application/json")
    @PUT("/api/14/project/{project}/import")
    Call<ProjectImportStatus> importProjectArchive(
            @Path("project") String project,
            @Query("jobUuidOption") String jobUuidOption,
            @Query("importExecutions") Boolean importExecutions,
            @Query("importConfig") Boolean importConfig,
            @Query("importACL") Boolean importACL,
            @Body RequestBody body
    );

}
