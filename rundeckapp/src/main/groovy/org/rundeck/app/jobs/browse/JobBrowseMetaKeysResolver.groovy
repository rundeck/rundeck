package org.rundeck.app.jobs.browse

import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.JobMetadataComponent

/**
 * Resolves the effective metadata key set for jobs/browse when {@code metaExclude} is used (API v58+).
 */
@CompileStatic
class JobBrowseMetaKeysResolver {

    static Set<String> resolve(
        String meta,
        String metaExclude,
        Map<String, JobMetadataComponent> jobMetadataComponents
    ) {
        List<String> includeTokens = meta.split(',').collect { it.trim() }.findAll { it }
        Set<String> excluded = new HashSet<>(
            metaExclude.split(',').collect { it.trim() }.findAll { it }
        )
        Set<String> includes = new HashSet<>(includeTokens)
        if (includes.contains('*')) {
            Set<String> expanded = new HashSet<>()
            jobMetadataComponents.each { String beanName, JobMetadataComponent component ->
                expanded.addAll(component.getAvailableMetadataNames())
            }
            expanded.removeAll(excluded)
            return expanded
        }
        includes.removeAll(excluded)
        return includes
    }
}
