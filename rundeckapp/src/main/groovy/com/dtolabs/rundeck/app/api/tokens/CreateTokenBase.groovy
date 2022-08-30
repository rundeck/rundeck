package com.dtolabs.rundeck.app.api.tokens

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema


@CompileStatic
abstract class CreateTokenBase {
    String user
    @Schema(
        title = 'Time Duration, e.g. 1d2h3m15s',
        type = 'string',
        format = 'timeduration',
        description = 'since: v19'
    )
    String duration
    @Schema(description = 'since: v19')
    String name
}
