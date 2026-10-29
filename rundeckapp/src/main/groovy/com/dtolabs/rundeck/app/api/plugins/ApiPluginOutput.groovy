package com.dtolabs.rundeck.app.api.plugins

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A value a plugin computes and exposes for reference by a Conditional Logic step
 * (declared with {@code @PluginOutput}), whether or not it is also a configurable property.
 */
@CompileStatic
class ApiPluginOutput {
    @Schema(description = 'Output name; the value a condition references as "<Step Label> - <name>"')
    String name
    @Schema(description = 'Data context group the value is captured under')
    String group
    @Schema(description = 'Description of the output')
    String description
    @Schema(description = 'Title of the property that declares the output, for display')
    String title

    /**
     * @param values map with name, group, description and title keys
     * @return the corresponding output
     */
    static ApiPluginOutput from(Map<String, String> values) {
        new ApiPluginOutput(
            name: values.get('name'),
            group: values.get('group'),
            description: values.get('description'),
            title: values.get('title')
        )
    }
}
