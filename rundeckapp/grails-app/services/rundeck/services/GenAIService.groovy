package rundeck.services

import groovy.util.logging.Log4j2
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

@Log4j2
class GenAIService {

    def apiUrl = "https://api.openai.com/v1/chat/completions"

    def DEFAULT_PROMPT = "Describe the job in a couple of sentences concentrating on the actual commands"
    def DEFAULT_DIFF_PROMPT = "Provide a brief summary of changes between two Rundeck jobs"

    String getJobDescriptionFromJobDefinition(String apiKey, String jobDefinition, String customPrompt = DEFAULT_PROMPT) {
        assert !!apiKey
        assert !!jobDefinition

        OkHttpClient client = new OkHttpClient()

        def jsonBuilder = new JsonBuilder()
        jsonBuilder {
            model "gpt-4o-mini"
            messages([
                    [role: "system", content: "Your are an experienced Rundeck user"],
                    [role: "user", content: "$customPrompt using Rudeck job definition: ```$jobDefinition```"]
            ])
            temperature 0.9
            max_tokens 250
        }

        String jsonPayload = jsonBuilder.toString()

        RequestBody requestBody = RequestBody.create(
                jsonPayload,
                MediaType.get("application/json; charset=utf-8")
        )

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

        try {
            Response response = client.newCall(request).execute()
            if (!response.isSuccessful()) {
                throw new RuntimeException("Request failed: ${response.code()} with body: ${response.body().string()}")
            } else {
                String responseBody = response.body().string()
                def jsonResponse = new JsonSlurper().parseText(responseBody)
                return jsonResponse.choices[0].message.content
            }
        } catch (IOException e) {
            throw new RuntimeException("An error occurred", e)
        }
    }

    String getJobDiffDescription(String apiKey, String previousJobDefinition, String updatedJobDefinition, String customPrompt = DEFAULT_DIFF_PROMPT) {
        assert !!apiKey
        assert !!updatedJobDefinition

        if (!previousJobDefinition) {
            return null
        }

        OkHttpClient client = new OkHttpClient()

        def jsonBuilder = new JsonBuilder()
        jsonBuilder {
            model "gpt-4o-mini"
            messages([
                    [role: "system", content: "Your are an experienced Rundeck user"],
                    [role: "user", content: "$customPrompt. Original job definition: ```$previousJobDefinition``` Updated job definition: ```$updatedJobDefinition```"]
            ])
            temperature 0.9
            max_tokens 250
        }

        String jsonPayload = jsonBuilder.toString()

        RequestBody requestBody = RequestBody.create(
                jsonPayload,
                MediaType.get("application/json; charset=utf-8")
        )

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

        try {
            Response response = client.newCall(request).execute()
            if (!response.isSuccessful()) {
                throw new RuntimeException("Request failed: ${response.code()} with body: ${response.body().string()}")
            } else {
                String responseBody = response.body().string()
                def jsonResponse = new JsonSlurper().parseText(responseBody)
                return jsonResponse.choices[0].message.content
            }
        } catch (IOException e) {
            throw new RuntimeException("An error occurred", e)
        }
    }

}
