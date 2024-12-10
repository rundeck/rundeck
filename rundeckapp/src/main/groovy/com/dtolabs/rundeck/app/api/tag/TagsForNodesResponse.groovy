package com.dtolabs.rundeck.app.api.tag

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import io.swagger.v3.oas.annotations.media.Schema

@ApiResource
@Schema(description = "Tags")
class TagsForNodesResponse {

    @Schema(description = "List of tags", requiredMode = Schema.RequiredMode.REQUIRED)
    List<TagForNodes> tags
}
