package com.dtolabs.rundeck.app.api.authorizations

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import groovy.transform.EqualsAndHashCode
import io.swagger.v3.oas.annotations.media.Schema

@ApiResource
@Schema(description = "Action authorization")
@EqualsAndHashCode
class ActionAuthorization {
    @Schema(description = "Action name", example = "read", requiredMode = Schema.RequiredMode.REQUIRED)
    String actionName

    @Schema(description = "Action authorization flag", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    Boolean isAuthorized
}
