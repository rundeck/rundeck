package org.rundeck.util.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import okhttp3.Response

class RundeckResponse<T> {
    T response
    int httpCode
    ApiError error

    RundeckResponse(Response response, Class<T> clazz){
        this.httpCode = response.code()
        String bodyText = response.body().string()
        try {
            this.response = new ObjectMapper().readValue(
                    bodyText,
                    clazz
            )
        } catch (UnrecognizedPropertyException ignored){
            this.error = new ObjectMapper().readValue(
                    bodyText,
                    ApiError.class
            )
        }
    }

    static class ApiError {
        String errorCode
        Integer apiversion
        String message
        Boolean error
    }
}
