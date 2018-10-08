/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rundeckapp.init.servlet

import com.dtolabs.rundeck.core.init.CustomWebAppInitializer
import org.eclipse.jetty.security.ConstraintSecurityHandler
import org.eclipse.jetty.security.SecurityHandler
import org.eclipse.jetty.security.authentication.FormAuthenticator
import org.eclipse.jetty.webapp.WebAppContext
import spock.lang.Specification


class JettyConfigPropsInitParameterConfigurationTest extends Specification {

    def "PreConfigure no jetty customizer"() {
        given:
        JettyConfigPropsInitParameterConfiguration cfg = new JettyConfigPropsInitParameterConfiguration([:])
        cfg.metaClass.getJettyCustomizers = { [] }
        WebAppContext context = Mock(WebAppContext)

        when:
        0 * context.getSecurityHandler()
        cfg.preConfigure(context)

        then:
        noExceptionThrown()

    }

    def "PreConfigure bad jetty customizer throws error"() {
        given:
        JettyConfigPropsInitParameterConfiguration cfg = new JettyConfigPropsInitParameterConfiguration([:])
        cfg.metaClass.getJettyCustomizers = { [
                new CustomWebAppInitializer<WebAppContext>() {

                    @Override
                    void customizeWebAppContext(final WebAppContext webAppContext) {
                        webAppContext.getSecurityHandler().setAuthenticator("not a real authenticator class")
                    }
                }
        ] }
        WebAppContext context = Mock(WebAppContext)
        SecurityHandler securityHandler = new ConstraintSecurityHandler()

        when:
        1 * context.getSecurityHandler() >> securityHandler
        cfg.preConfigure(context)

        then:
        thrown(Exception)

    }

    def "PreConfigure jetty customizer"() {
        given:
        JettyConfigPropsInitParameterConfiguration cfg = new JettyConfigPropsInitParameterConfiguration([:])
        def formAuth = new FormAuthenticator()

        cfg.metaClass.getJettyCustomizers = { [
                new CustomWebAppInitializer<WebAppContext>() {

                    @Override
                    void customizeWebAppContext(final WebAppContext webAppContext) {
                        webAppContext.getSecurityHandler().setAuthenticator(formAuth)
                    }
                }
        ] }
        WebAppContext context = Mock(WebAppContext)
        SecurityHandler securityHandler = new ConstraintSecurityHandler()

        when:
        1 * context.getSecurityHandler() >> securityHandler
        cfg.preConfigure(context)

        then:
        securityHandler.authenticator == formAuth

    }

}
