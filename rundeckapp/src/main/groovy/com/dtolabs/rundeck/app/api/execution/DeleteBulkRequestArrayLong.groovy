package com.dtolabs.rundeck.app.api.execution
import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@ArraySchema(schema = @Schema(type='integer'))
class DeleteBulkRequestArrayLong {
    List<Long> ids = []
}
