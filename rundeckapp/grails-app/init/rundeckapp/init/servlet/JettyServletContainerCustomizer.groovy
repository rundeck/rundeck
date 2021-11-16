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
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.webapp.AbstractConfiguration
import org.eclipse.jetty.webapp.WebAppContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer

/**
 * Customize embedded jetty
 */
class JettyServletContainerCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {
    /**
     * Set of init parameters to set in the web app context
     */
    Map<String, String> initParams = [:]

    @Override
    void customize(final JettyServletWebServerFactory factory) {
        factory.addServerCustomizers(new JettyServerCustomizer() {
            @Override
            void customize(Server server) {
                for (Handler handler : server.getHandlers()) {
                    if (handler instanceof ContextHandler) {
                        ((ContextHandler) handler).setMaxFormKeys(2000)
                    }
                }
            }
        })
        factory.addConfigurations(new JettyConfigPropsInitParameterConfiguration(initParams))
    }
}

/**
 * Set init params for the WebAppContext
 */
class JettyConfigPropsInitParameterConfiguration extends AbstractConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(this)
    Map<String, String> initParams = [:]

    JettyConfigPropsInitParameterConfiguration(final Map<String, String> initParams) {
        this.initParams = initParams
    }

    @Override
    void preConfigure(final WebAppContext context) throws Exception {
        super.preConfigure(context)
        if(!initParams){
            initParams = [:]
        }
        for (String key : initParams.keySet()) {
            context.setInitParameter(key, initParams[key])
        }
        //Call custom initialization code
        try {
            def jettyCustomizers = getJettyCustomizers()
            jettyCustomizers.each { customizer ->
               LOG.debug("Customizing jetty with: ${customizer.class.canonicalName}")
               customizer.customizeWebAppContext(context)
            }
        } catch(Exception ex) {
            LOG.error("Unable to configure embedded container with custom client authenticator or login service code",ex)
            throw ex
        }
    }

    def getJettyCustomizers() {
        return ServiceLoader.load(CustomWebAppInitializer)
    }
}
