package org.rundeck.app.jobs.browse

import org.rundeck.app.components.jobs.JobMetadataComponent
import spock.lang.Specification

class JobBrowseMetaKeysResolverSpec extends Specification {

    def "expands star and removes excluded"() {
        given:
        def comp1 = Mock(JobMetadataComponent) {
            getAvailableMetadataNames() >> ['authz', 'stats'].toSet()
        }
        def comp2 = Mock(JobMetadataComponent) {
            getAvailableMetadataNames() >> ['schedule', 'tags'].toSet()
        }
        when:
        Set<String> result = JobBrowseMetaKeysResolver.resolve('*', 'stats', ['a': comp1, 'b': comp2])
        then:
        result == ['authz', 'schedule', 'tags'].toSet()
    }

    def "explicit list minus exclude"() {
        when:
        Set<String> result = JobBrowseMetaKeysResolver.resolve('authz,schedule', 'schedule', [:])
        then:
        result == ['authz'].toSet()
    }

    def "trims tokens and ignores unknown excludes"() {
        given:
        def comp = Mock(JobMetadataComponent) {
            getAvailableMetadataNames() >> ['authz', 'stats'].toSet()
        }
        when:
        Set<String> result = JobBrowseMetaKeysResolver.resolve(' * ', ' stats , nosuch ', ['x': comp])
        then:
        result == ['authz'].toSet()
    }
}
