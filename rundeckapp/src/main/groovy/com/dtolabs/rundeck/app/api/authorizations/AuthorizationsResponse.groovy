package com.dtolabs.rundeck.app.api.authorizations

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import io.swagger.v3.oas.annotations.media.Schema

@ApiResource
@Schema(description = "Authorizations")
class AuthorizationsResponse {

    @Schema(oneOf = [ApplicationAuthorizationContext.class, ProjectAuthorizationContext.class])
    Object authorizationContext

    @Schema(oneOf = [KindResource.class, TypeResource.class])
    Object resource

    @Schema(description = "Action authorization", requiredMode = Schema.RequiredMode.REQUIRED)
    Set<ActionAuthorization> actionAuthorizations
}

@Schema(description = "A resource kind")
class KindResource {
    @Schema(description = "Resource Type", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = ['project', 'project_acl', 'system_acl', 'system', 'plugin', 'user', 'apitoken'])
    String kind
}

@Schema(description = "A resource type with a specifier")
class TypeResource {
    @Schema(description = "Resource Type", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = ['project', 'project_acl'])
    String type

    @Schema(description = "Type Specifier")
    String specifier
}

@Schema(description = "Authorizations in the context of an application")
class ApplicationAuthorizationContext {
    @Schema(description = "Context type", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = ['application'])
    final String type = "application"
}

@Schema(description = "Authorizations in the context of a project")
class ProjectAuthorizationContext {
    @Schema(description = "Context type", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = ['project'])
    final String type = "project"

    @Schema(description = "Context name", requiredMode = Schema.RequiredMode.REQUIRED)
    String name
}