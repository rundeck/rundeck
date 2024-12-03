package com.dtolabs.rundeck.app.api.tag

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import groovy.transform.EqualsAndHashCode
import io.swagger.v3.oas.annotations.media.Schema

@ApiResource
@Schema(description = "Tag with node count")
@EqualsAndHashCode
class TagForNodes {
    @Schema(description = "Tag name", example = "infra", requiredMode = Schema.RequiredMode.REQUIRED)
    String name

    @Schema(description = "Count of nodes associated with the tag", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    Integer nodeCount
}
