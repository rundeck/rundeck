package org.rundeck.app.config

import com.dtolabs.rundeck.core.config.Features
import groovy.transform.CompileStatic

/**
 * Describe configuration for feature flags
 */
@CompileStatic
class FeatureFlagConfigurable implements SystemConfigurable {
    List<SysConfigProp> systemConfigProps = [
        SystemConfig.builder().with {
            key("rundeck.feature.${Features.MULTILINE_JOB_OPTIONS.propertyName}.enabled")
                .datatype("Boolean")
                .label("Multiline Job Options (Beta)")
                .description("(Beta Feature) Enable support for multiline job options in job definitions and the GUI.")
                .defaultValue("false")
                .category("Feature")
                .visibility("Advanced")
                .strata("default")
                .required(false)
                .restart(false)
                .authRequired('app_admin')
                .build()
        } as SysConfigProp,
        //TODO: include additional feature flags here
    ]
}
