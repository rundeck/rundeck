package org.rundeck.util.api.responses.common

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import okhttp3.Response

class RundeckResponse<T> {
    T response
    int httpCode
    ApiError error

    /**
     * Instantiates a new Rundeck response of type T.
     *
     * The `response` attribute is set if the provided http response is valid (appears in validResponseHttpCodes) and body can be parsed to type T.
     * Otherwise, the `error` attribute  is set or an exception is thrown if throwOnInvalidHttpCode is true.
     * @param httpResponse the http response
     * @param clazz Type a valid response should be deserialized to
     * @param validResponseHttpCodes a range of response http codes that are considered valid
     * @param throwOnInvalidHttpCode if true, the the http response not in the range will throw
     * @throws IllegalStateException if ensure2xxResponse and the http response is not successful
     */
    RundeckResponse(Response httpResponse, Class<T> clazz, IntRange validResponseHttpCodes = 200..299, throwOnInvalidHttpCode = true)  {
        this.httpCode = httpResponse.code()
        String bodyText = httpResponse.body().string()
        try {
            if (validResponseHttpCodes.contains(httpResponse.code())) {
                this.response = new ObjectMapper().readValue(
                        bodyText,
                        clazz
                )
            } else {
                if (throwOnInvalidHttpCode) {
                    throw new IllegalStateException("HTTP response must be 2xx, but it's: ${httpResponse.code()} - ${bodyText}")
                }
                this.error = new ObjectMapper().readValue(bodyText, ApiError.class)
            }
        } catch (UnrecognizedPropertyException ignored){
            this.error = new ObjectMapper().readValue(bodyText, ApiError.class)
        }
    }

    static class ApiError {
        String errorCode
        Integer apiversion
        String message
        Boolean error
        List<String> args  // Grails 7: Added to match API error response format
    }
}
