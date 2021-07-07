package com.dtolabs.rundeck.app.api.plugins

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.ApiVersion

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
        String iconUrl
        @ApiVersion(40)
        LinkedHashMap<Object, Object> providerMetadata
}