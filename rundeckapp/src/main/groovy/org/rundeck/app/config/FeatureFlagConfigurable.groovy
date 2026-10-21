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
            'Minimum number of visible lines in the ACE code editor rendered inside plugin configuration forms. Set to 0 (default) to make the editor manually resizable via drag handle instead of auto-sizing to a fixed number of lines.',
            '0',
            'Integer'
        ),
        guiConfig(
            'rundeck.feature.guiAceEditorMaxLines',
            'Code Editor - Maximum Lines',
            'Maximum number of visible lines in the ACE code editor rendered inside plugin configuration forms. Set to 0 for unlimited (default).',
            '0',
            'Integer'
        ),
        scmSecurityConfig(
            'rundeck.scm.git.allowedSchemes',
            'String',
            '',
            'Git SCM allowed URL schemes',
            'Comma-separated Git URL schemes for SCM import and export, for example https,http,ssh. ' +
                'ssh includes ssh:// and git@host:path. file includes file:// and local paths. ' +
                'Leave blank for no restriction; a blank value does not reject every scheme. ' +
                'Applies on the next Git operation, without a restart. ' +
                'A non-blank value overrides framework.properties.'
        ),
        scmSecurityConfig(
            'rundeck.scm.git.blockInternalAddresses',
            'Boolean',
            'false',
            'Block internal addresses for Git SCM',
            'When enabled, Git SCM rejects hosts that resolve to loopback, link-local, or private addresses, including HTTP redirects. ' +
                'Leave disabled for existing internal Git servers. ' +
                'Applies on the next Git operation, without a restart. ' +
                'A value saved here overrides framework.properties.'
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

    /**
     * Git SCM security control. {@code restart} is false so a System Configuration save applies
     * on the next Git operation.
     */
    private static SysConfigProp scmSecurityConfig(
        String configKey,
        String configDatatype,
        String configDefaultValue,
        String configLabel,
        String configDescription
    ) {
        SystemConfig.builder().with {
            key(configKey)
                .datatype(configDatatype)
                .label(configLabel)
                .description(configDescription)
                .defaultValue(configDefaultValue)
                .category('SCM')
                .visibility('Standard')
                .strata('default')
                .required(false)
                .restart(false)
                .authRequired('ops_admin')
                .build()
        } as SysConfigProp
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
