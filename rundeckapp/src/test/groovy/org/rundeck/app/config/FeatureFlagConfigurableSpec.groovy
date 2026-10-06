package org.rundeck.app.config

import spock.lang.Specification

class FeatureFlagConfigurableSpec extends Specification {

    def "git scm security flags apply without restart"() {
        when:
            def props = new FeatureFlagConfigurable().systemConfigProps
            def schemes = props.find { it.key == 'rundeck.scm.git.allowedSchemes' }
            def block = props.find { it.key == 'rundeck.scm.git.blockInternalAddresses' }

        then:
            schemes != null
            schemes.datatype == 'String'
            schemes.defaultValue == ''
            !schemes.restart
            schemes.category == 'SCM'
            block != null
            block.datatype == 'Boolean'
            block.defaultValue == 'false'
            !block.restart
            block.category == 'SCM'
    }
}
