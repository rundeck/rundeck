package com.dtolabs.rundeck.app.api.plugins

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.ApiVersion
import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@ApiResource
class ApiPluginProviderDetail {
    String name
    String id
    String pluginVersion
    String title
    String description
    @Schema(description = 'URL to icon file for the plugin if present. Since: v40')
    String iconUrl
    @Schema(description = 'Map of metadata about the plugin if present. Since: v40')
    Map<String, String> providerMetadata
    String desc
    String ver
    String rundeckCompatibilityVersion
    String targetHostCompatibility
    String license
    String sourceLink
    String thirdPartyDependencies
    Map<String,String> projectMapping
    Map<String,String> fwkMapping
    Map<String,Object> dynamicProps
    Map<String,Object> dynamicDefaults
    String vueConfigComponent
    List<ApiProviderProp> props
}
