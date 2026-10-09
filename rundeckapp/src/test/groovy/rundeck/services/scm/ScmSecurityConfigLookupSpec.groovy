package rundeck.services.scm

import com.dtolabs.rundeck.core.common.PropertyRetriever
import rundeck.services.ConfigurationService
import spock.lang.Specification

class ScmSecurityConfigLookupSpec extends Specification {

    def "live value wins over framework"() {
        given:
            def config = Mock(ConfigurationService)
            def framework = Mock(PropertyRetriever)
            def lookup = new ScmSecurityConfigLookup(config, framework)

        when:
            def value = lookup.getProperty('rundeck.scm.git.allowedSchemes')

        then:
            1 * config.getValue('scm.git.allowedSchemes', null) >> 'https,ssh'
            0 * framework.getProperty(_)
            value == 'https,ssh'
    }

    def "blank live value falls back to framework"() {
        given:
            def config = Mock(ConfigurationService)
            def framework = Mock(PropertyRetriever)
            def lookup = new ScmSecurityConfigLookup(config, framework)

        when:
            def value = lookup.getProperty('rundeck.scm.git.allowedSchemes')

        then:
            1 * config.getValue('scm.git.allowedSchemes', null) >> '   '
            1 * framework.getProperty('rundeck.scm.git.allowedSchemes') >> 'ssh'
            value == 'ssh'
    }

    def "false boolean is an explicit value"() {
        given:
            def config = Mock(ConfigurationService)
            def framework = Mock(PropertyRetriever)
            def lookup = new ScmSecurityConfigLookup(config, framework)

        when:
            def value = lookup.getProperty('rundeck.scm.git.blockInternalAddresses')

        then:
            1 * config.getValue('scm.git.blockInternalAddresses', null) >> Boolean.FALSE
            0 * framework.getProperty(_)
            value == 'false'
    }

    def "null live value falls back to framework"() {
        given:
            def config = Mock(ConfigurationService)
            def framework = Mock(PropertyRetriever)
            def lookup = new ScmSecurityConfigLookup(config, framework)

        when:
            def value = lookup.getProperty('rundeck.scm.git.blockInternalAddresses')

        then:
            1 * config.getValue('scm.git.blockInternalAddresses', null) >> null
            1 * framework.getProperty('rundeck.scm.git.blockInternalAddresses') >> 'true'
            value == 'true'
    }

    def "keys outside rundeck prefix skip application config"() {
        given:
            def config = Mock(ConfigurationService)
            def framework = Mock(PropertyRetriever)
            def lookup = new ScmSecurityConfigLookup(config, framework)

        when:
            def value = lookup.getProperty('framework.server.name')

        then:
            0 * config.getValue(_, _)
            1 * framework.getProperty('framework.server.name') >> 'x'
            value == 'x'
    }

    def "null configuration service uses framework"() {
        given:
            def framework = Mock(PropertyRetriever)
            def lookup = new ScmSecurityConfigLookup(null, framework)

        when:
            def value = lookup.getProperty('rundeck.scm.git.allowedSchemes')

        then:
            1 * framework.getProperty('rundeck.scm.git.allowedSchemes') >> null
            value == null
    }
}
