package rundeck.services

import spock.lang.Specification

/**
 * Created by greg on 1/11/16.
 */
class ArchiveOptionsSpec extends Specification {
    def "parseExecutionsIds single string"() {
        given:
        def opts = new ArchiveOptions()
        when:
        opts.parseExecutionsIds('123,456')
        then:
        opts.executionIds == ['123', '456'] as Set
    }
    def "parseExecutionsIds string list"() {
        given:
        def opts = new ArchiveOptions()
        when:
        opts.parseExecutionsIds(['123','456'])
        then:
        opts.executionIds == ['123', '456'] as Set
    }
}
