package com.dtolabs.rundeck.app.api.node

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import io.swagger.v3.oas.annotations.media.Schema

@ApiResource
@Schema(description = "Nodes")
class NodesResponse {

    @Schema(description = "List of nodes", requiredMode = Schema.RequiredMode.REQUIRED)
    List<Node> nodes
}
