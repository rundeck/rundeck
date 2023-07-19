package com.dtolabs.rundeck.app.api.plugins

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.ApiVersion
import io.swagger.v3.oas.annotations.media.Schema

@ApiResource
class ApiPluginListProvider {
        String service
        String artifactName
        String name
        String id
        Boolean builtin
        String pluginVersion
        String title
        String description
        String author
        @ApiVersion(40)
        @Schema(description='URL to icon file for the plugin if present. Since: v40')
        String iconUrl
        @ApiVersion(40)
        @Schema(description='Map of metadata about the plugin if present. Since: v40')
        Map<Object, Object> providerMetadata
}