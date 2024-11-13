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

    def apiKey = System.getenv("OPENAI_API_KEY") ?: "SET_YOUR_API_KEY_IN_THE_ENV_VARIABLE"
    def apiUrl = "https://api.openai.com/v1/chat/completions"

    def DEFAULT_PROMPT = "Describe the job in a couple of sentences concentrating on the actual commands"

    String getJobDescriptionFromJobDefinition(String jobDefinition, String customPrompt = DEFAULT_PROMPT) {
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

}
