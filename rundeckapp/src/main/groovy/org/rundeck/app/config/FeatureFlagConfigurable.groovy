package org.rundeck.app.config

import com.dtolabs.rundeck.core.config.Features
import groovy.transform.CompileStatic

/**
 * Describe configuration for feature flags
 */
@CompileStatic
class FeatureFlagConfigurable implements SystemConfigurable {
    List<SysConfigProp> systemConfigProps = [
        featureConfig(
            Features.MULTILINE_JOB_OPTIONS,
            "Multiline Job Options (Beta)",
            "(Beta Feature) Enable support for multiline job options in job definitions and the GUI.",
            'app_admin'
        ),
        //TODO: include additional feature flags here
    ]

    private static SysConfigProp featureConfig(Features feature, String label, String description, String auth) {
        SystemConfig.builder().with {
            key("rundeck.feature.${feature.propertyName}.enabled")
                .datatype("Boolean")
                .label(label)
                .description(description)
                .defaultValue("false")
                .category("Feature")
                .visibility("Advanced")
                .strata("default")
                .required(false)
                .restart(false)
                .authRequired(auth)
                .build()
        } as SysConfigProp
    }
}
