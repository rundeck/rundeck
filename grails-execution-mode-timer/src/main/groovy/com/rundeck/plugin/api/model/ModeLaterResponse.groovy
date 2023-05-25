package com.rundeck.plugin.api.model

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema
class ModeLaterResponse {
    boolean saved
    String msg
}
