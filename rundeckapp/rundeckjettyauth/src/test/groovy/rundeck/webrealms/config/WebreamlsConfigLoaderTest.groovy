package rundeck.webrealms.config

import spock.lang.Specification

class WebreamlsConfigLoaderTest extends Specification {
    def "GetWebrealmsConfig"() {
        when:
            def actualConfig = WebreamlsConfigLoader.loadWebrealmsConfig()

        then:
            assert actualConfig != null
            assert actualConfig.webrealms != null
            assert actualConfig.webrealms.server != null
            assert actualConfig.webrealms.loginconfig != null
            assert actualConfig.webrealms.securityconstraint != null
            assert actualConfig.webrealms.securityroles != null

    }
}
