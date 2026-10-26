package rundeck.services.scm

import com.dtolabs.rundeck.core.common.PropertyRetriever
import groovy.transform.CompileStatic
import rundeck.services.ConfigurationService

/**
 * Reads Git SCM security properties at call time.
 * <p>
 * A non-blank value from {@link ConfigurationService} wins, so a System Configuration
 * save applies on the next Git operation. A blank value is treated as unset and the
 * framework property retriever is used instead. An empty scheme list is therefore not
 * an allowlist that rejects every scheme.
 */
@CompileStatic
class ScmSecurityConfigLookup implements PropertyRetriever {

    private static final String RUNDECK_PREFIX = 'rundeck.'

    private final ConfigurationService configurationService
    private final PropertyRetriever frameworkLookup

    /**
     * @param configurationService live application config, or {@code null}
     * @param frameworkLookup framework.properties, or {@code null}
     */
    ScmSecurityConfigLookup(ConfigurationService configurationService, PropertyRetriever frameworkLookup) {
        this.configurationService = configurationService
        this.frameworkLookup = frameworkLookup
    }

    /**
     * {@inheritDoc}
     * <p>
     * Keys under {@code rundeck.} are read from application config first.
     */
    @Override
    String getProperty(String name) {
        String live = liveValue(name)
        if (live != null) {
            return live
        }
        return frameworkLookup?.getProperty(name)
    }

    /**
     * @param name full property name
     * @return trimmed application value, or {@code null} when unset or blank
     */
    private String liveValue(String name) {
        if (configurationService == null || name == null || !name.startsWith(RUNDECK_PREFIX)) {
            return null
        }
        Object value = configurationService.getValue(name.substring(RUNDECK_PREFIX.length()), null)
        if (value == null) {
            return null
        }
        String text = String.valueOf(value).trim()
        return text ? text : null
    }
}
