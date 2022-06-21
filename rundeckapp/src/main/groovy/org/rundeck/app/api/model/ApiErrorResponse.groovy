package org.rundeck.app.api.model

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema(description = 'Error Response')
class ApiErrorResponse {
    boolean error = true
    int apiversion
    String errorCode
    String message
}