package rundeck.jetty.auth

import org.eclipse.jetty.security.ConstraintMapping
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.authentication.FormAuthenticator
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import rundeck.webrealms.config.WebreamlsConfigLoader
import spock.lang.Specification

class WebrealmsJettyCustomizerTest extends Specification {
    def "customize jetty server with webrealms configuration"() {
        setup:
            def testResults = [:]
            def server = createMockJettyServer(testResults)

        when:
            new WebrealmsJettyCustomizer(WebreamlsConfigLoader.loadWebrealmsConfig()).customize(server)

        then:
            ConstraintSecurityHandler securityHandler = testResults.securityHandler
            assert securityHandler != null
            assert testResults.setSecurityHandlerCalled == 1
            assert securityHandler.roles.size() == 1
            assert securityHandler.roles[0] == "user"
            assert securityHandler.authenticator instanceof FormAuthenticator
            assert securityHandler.authMethod == "FORM"
            assert securityHandler.realmName == "rundeckrealm"
            assert securityHandler.constraintMappings.size() == 4
            ConstraintMapping staticResources = securityHandler.constraintMappings.find { it.constraint.name == "Static" }
            assert staticResources.constraint.roles == null
            assert staticResources.constraint.authenticate == false
            ConstraintMapping all = securityHandler.constraintMappings.find { it.constraint.name == "all" }
            assert all.constraint.authenticate == true
            assert all.constraint.roles.size() == 1
            assert all.constraint.roles[0] == "*"
            assert all.pathSpec == "/*"
    }

    private def createMockJettyServer(def testResults) {
        testResults.setSecurityHandlerCalled = 0
        def mockServer = [
                getHandler : [
                        setSecurityHandler : { ConstraintSecurityHandler securityHandler ->
                            testResults.setSecurityHandlerCalled++
                            testResults.securityHandler = securityHandler
                        }
                ] as ServletContextHandler
        ] as Server
    }
}
