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
        featureConfig(
            Features.EARLY_ACCESS_JOB_CONDITIONAL,
            "Enable Job Conditional Step (Experimental)",
            "(Beta Feature) Enable support for conditional steps in job definitions and the GUI.",
            'app_admin',
            'Early Access'
        ),
        //TODO: include additional feature flags here
    ]

    private static SysConfigProp featureConfig(Features feature, String label, String description, String auth) {
        featureConfig(feature, label, description, auth, "Feature")
    }

    private static SysConfigProp featureConfig(Features feature, String label, String description, String auth, String category) {
        SystemConfig.builder().with {
            key("rundeck.feature.${feature.propertyName}.enabled")
                .datatype("Boolean")
                .label(label)
                .description(description)
                .defaultValue("false")
                .category(category)
                .visibility("Advanced")
                .strata("default")
                .required(false)
                .restart(false)
                .authRequired(auth)
                .build()
        } as SysConfigProp
    }
}
