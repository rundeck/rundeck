package com.dtolabs.rundeck.app.api.jobs.options

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema


@CompileStatic
@Schema
class OptionValidateResponse {
    boolean valid
    @Schema(description = "Mapping of input key to list of validation error messages")
    Map<String,List<String>> messages=[:]
}
