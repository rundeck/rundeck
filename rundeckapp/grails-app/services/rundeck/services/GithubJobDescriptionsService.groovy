package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Log4j2
import okhttp3.*

@Log4j2
class GithubJobDescriptionsService {

    def apiUrl = "https://api.github.com"
    def branch = "main"
    OkHttpClient client = new OkHttpClient()

    String createOrUpdateFile(IRundeckProject projectProps, String path, String message, String content) {
        assert !!projectProps

        // TODO: Use key storage
        final String token = projectProps.getProperty('project.job-description-gen.storage.key')
        final String repo = projectProps.getProperty('project.job-description-gen.storage.repo')
        final String owner = projectProps.getProperty('project.job-description-gen.storage.owner')

        assert !!token
        assert !!repo
        assert !!owner

        path = path.startsWith("/") ? path.substring(1) : path
        path = "job_descriptions/$path"
        def getFileUrl = "$apiUrl/repos/$owner/$repo/contents/$path"
        def getFileRequest = new Request.Builder()
                .url(getFileUrl)
                .addHeader("Authorization", "Bearer $token")
                .build()

        Response getFileResponse = client.newCall(getFileRequest).execute()
        def sha = null
        if (getFileResponse.isSuccessful()) {
            def getFileResponseBody = getFileResponse.body().string()
            def getFileJson = new JsonSlurper().parseText(getFileResponseBody)
            sha = getFileJson.sha
        }

        def jsonMap = [
                message: message,
                content: content.bytes.encodeBase64().toString(),
                branch: branch
        ]

        if (sha) {
            jsonMap.sha = sha
        }

        def jsonBuilder = new JsonBuilder(jsonMap)

        String jsonPayload = jsonBuilder.toString()

        RequestBody requestBody = RequestBody.create(
                jsonPayload,
                MediaType.get("application/json; charset=utf-8")
        )

        def putFileUrl = "$apiUrl/repos/$owner/$repo/contents/$path"
        Request putFileRequest = new Request.Builder()
                .url(putFileUrl)
                .put(requestBody)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .build()

        try {
            Response putFileResponse = client.newCall(putFileRequest).execute()
            if (!putFileResponse.isSuccessful()) {
                throw new RuntimeException("Request failed: ${putFileResponse.code()} with body: ${putFileResponse.body().string()}")
            } else {
                String putFileResponseBody = putFileResponse.body().string()
                def jsonResponse = new JsonSlurper().parseText(putFileResponseBody)
                return jsonResponse.content.sha
            }
        } catch (IOException e) {
            throw new RuntimeException("An error occurred", e)
        }
    }
}