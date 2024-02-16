package org.rundeck.util.api.scm.gitea

import okhttp3.ConnectionPool
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Response
import org.rundeck.util.api.storage.KeyStorageApiClient

import java.util.concurrent.TimeUnit

class GiteaApiRemoteRepo {
    private static final String GITEA_RD_BASE_URL = "http://${GITEA_USER}@gitea:3000"
    private static final String GITEA_API_BASE_URL = 'http://localhost:3000/api/v1'
    private static final String GITEA_USER = 'rundeckgitea'
    private static final String GITEA_PASSWORD = 'rundeckgitea'
    private static final String AUTH_CREDS = Credentials.basic(GITEA_USER,GITEA_PASSWORD)
    private static final String CREATE_REPO_ENDPOINT = '/user/repos'

    private String repoName
    String repoPassStoragePathForRundeck
    private OkHttpClient httpClient

    GiteaApiRemoteRepo(String repoName){
        this.repoName = repoName
        this.httpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(2, 10, TimeUnit.SECONDS))
                .build()
    }

    void setupRepo(){
        doPost(CREATE_REPO_ENDPOINT, new CreateRepoRequest(name: this.repoName))
    }

    private Response doPost(final String path, final Object body = null){
        RequestBody requestBuilder
        if (body) {
            requestBuilder = RequestBody.create(
                    new ObjectMapper().writeValueAsBytes(body),
                    MediaType.parse("application/json"))
        } else {
            requestBuilder = RequestBody.create()
        }
        def builder = new Request.Builder()
                .url(GITEA_API_BASE_URL + path)
                .header('Accept', 'application/json')
                .header('Authorization', AUTH_CREDS)
                .method("POST", requestBuilder)

        httpClient.newCall(
                builder.build()
        ).execute()
    }

    String getRepoUrlForRundeck(){
        return "${GITEA_RD_BASE_URL}/${GITEA_USER}/${repoName}.git"
    }

    /**
     *
     * @param client
     * @param path
     * @return true if successful
     */
    Response storeRepoPassInRundeck(KeyStorageApiClient client, String path){
        this.repoPassStoragePathForRundeck = "keys/${path}"
        client.callUploadKey(path, 'PASSWORD', GITEA_PASSWORD)
    }
}
