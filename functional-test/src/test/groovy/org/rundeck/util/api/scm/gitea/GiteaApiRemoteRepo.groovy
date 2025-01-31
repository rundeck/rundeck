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

    GiteaApiRemoteRepo setupRepo(){
        doPost(CREATE_REPO_ENDPOINT, new CreateRepoRequest(name: this.repoName))
        return this
    }

    /**
     *  Creates a file in the repo with the provided content
     * @param filePath
     * @param content
     * @param commitMessage
     */
    void createFile(String filePath, String content, String commitMessage = "Default commit message") {
        final String createFileEndpoint = "/repos/${GITEA_USER}/${repoName}/contents/"
        final String url = GITEA_API_BASE_URL + createFileEndpoint + filePath
        String encodedContent = content.bytes.encodeBase64().toString()

        def body = [
                message: commitMessage,
                content: encodedContent
        ]

        RequestBody requestBody = RequestBody.create(
                new ObjectMapper().writeValueAsBytes(body),
                MediaType.parse("application/json")
        )

        Request request = new Request.Builder()
                .url(url)
                .header('Accept', 'application/json')
                .header('Authorization', AUTH_CREDS)
                .method("POST", requestBody)
                .build()

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to create a file  in gitea: " + response.code() + " " + response.body().string())
            }
        }
    }

    /**
     * Retrieves the SHA of a file in the repository.
     * @param filePath The path of the file in the repository.
     * @return The SHA of the file.
     * @throws IOException If the request fails.
     */
    String getFileSha(String filePath) throws IOException {
        final String getFileEndpoint = "/repos/${GITEA_USER}/${repoName}/contents/"
        final String url = GITEA_API_BASE_URL + getFileEndpoint + filePath

        Request getRequest = new Request.Builder()
                .url(url)
                .header('Accept', 'application/json')
                .header('Authorization', AUTH_CREDS)
                .method("GET", null)
                .build()

        try (Response getResponse = httpClient.newCall(getRequest).execute()) {
            if (!getResponse.isSuccessful()) {
                throw new IOException("Failed to get file SHA: " + getResponse.code() + " " + getResponse.body().string())
            }
            def responseBody = new ObjectMapper().readValue(getResponse.body().string(), Map)
            return responseBody.sha
        }
    }

    /**
     * Updates a file in the repository with the provided content.
     * @param filePath The path of the file in the repository.
     * @param content The content to update the file with.
     * @param commitMessage The commit message.
     * @throws IOException If the request fails.
     */
    void updateFile(String filePath, String content, String commitMessage = "Default commit message") {
        final String updateFileEndpoint = "/repos/${GITEA_USER}/${repoName}/contents/"
        final String url = GITEA_API_BASE_URL + updateFileEndpoint + filePath
        String encodedContent = content.bytes.encodeBase64().toString()

        String fileSha = getFileSha(filePath)

        def body = [
                message: commitMessage,
                content: encodedContent,
                sha    : fileSha
        ]

        RequestBody requestBody = RequestBody.create(
                new ObjectMapper().writeValueAsBytes(body),
                MediaType.parse("application/json")
        )

        Request request = new Request.Builder()
                .url(url)
                .header('Accept', 'application/json')
                .header('Authorization', AUTH_CREDS)
                .method("PUT", requestBody)
                .build()

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to update the file in Gitea: " + response.code() + " " + response.body().string())
            }
        }
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
     * @param path including the key name but not the "keys/" prefix
     * @return true if successful
     */
    Response storeRepoPassInRundeck(KeyStorageApiClient client, String path){
        this.repoPassStoragePathForRundeck = "keys/${path}"
        client.callUploadKey(path, 'PASSWORD', GITEA_PASSWORD)
    }
}
