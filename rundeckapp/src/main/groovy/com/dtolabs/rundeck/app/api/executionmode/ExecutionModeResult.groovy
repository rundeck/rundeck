package com.dtolabs.rundeck.app.api.executionmode

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema
class ExecutionModeResult {
    @Schema(type = 'string', allowableValues = ['active','passive'])
    String executionMode
}
