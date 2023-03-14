package com.dtolabs.rundeck.app.api.execution

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema
class DeleteBulkResponse {
   @Schema(description = 'number of requested execution deletions')
   int requestCount
   @Schema(description = 'true if all deletions were successful')
   boolean allsuccessful
   @Schema(description = 'number of deletion attempts that succeeded')
   int successCount
   @Schema(description = 'number of deletion attempts that failed')
   int failedCount
   List<FailedItem> failures
}
