package com.dtolabs.rundeck.app.api.execution

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@Schema
@CompileStatic
final class DeleteBulkRequest {
    List<String> ids = []
}
