package com.dtolabs.rundeck.app.api.execution

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema
class DeleteBulkRequestLong {
    List<Long> ids = []
}
