package com.dtolabs.rundeck.app.api.tokens

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema
class CreateToken {
    String user
    List<String> roles = []
    @Schema(
        title = 'Time Duration, e.g. 1d2h3m15s',
        type = 'string',
        format = 'timeduration'
    )
    String duration
    String name
}
