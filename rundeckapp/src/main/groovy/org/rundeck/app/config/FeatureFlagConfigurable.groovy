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
            "Enable Job Conditional Step (Beta)",
            "(Beta Feature) Enable support for conditional steps in job definitions and the GUI.",
            'app_admin',
            'Early Access'
        ),
        guiConfig(
            'rundeck.feature.guiAceEditorMinLines',
            'Code Editor - Minimum Lines',
            'Minimum number of visible lines in the ACE code editor rendered inside plugin configuration forms. Default: 12.',
            '12',
            'Integer'
        ),
        guiConfig(
            'rundeck.feature.guiAceEditorMaxLines',
            'Code Editor - Maximum Lines',
            'Maximum number of visible lines in the ACE code editor rendered inside plugin configuration forms. Set to 0 for unlimited (default).',
            '0',
            'Integer'
        ),
        //TODO: include additional feature flags here
    ]

    private static SysConfigProp guiConfig(String configKey, String configLabel, String configDescription, String configDefaultValue, String configDatatype) {
        SystemConfig.builder().with {
            key(configKey)
                .datatype(configDatatype)
                .label(configLabel)
                .description(configDescription)
                .defaultValue(configDefaultValue)
                .category('GUI')
                .visibility('Advanced')
                .strata('default')
                .required(false)
                .restart(false)
                .authRequired('app_admin')
                .build()
        } as SysConfigProp
    }

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
