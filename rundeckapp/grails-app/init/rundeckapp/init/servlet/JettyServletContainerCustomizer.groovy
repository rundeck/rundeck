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

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.AbstractConfiguration
import org.eclipse.jetty.webapp.WebAppContext
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory

/**
 * Customize embedded jetty
 */
class JettyServletContainerCustomizer implements EmbeddedServletContainerCustomizer {
    /**
     * Set of init parameters to set in the web app context
     */
    Map<String, String> initParams = [:]

    @Override
    void customize(final ConfigurableEmbeddedServletContainer container) {
        if(container instanceof JettyEmbeddedServletContainerFactory) {
            container.addConfigurations(new JettyConfigPropsInitParameterConfiguration(initParams))
        }
    }
}

/**
 * Set init params for the WebAppContext
 */
class JettyConfigPropsInitParameterConfiguration extends AbstractConfiguration {
    Map<String, String> initParams

    JettyConfigPropsInitParameterConfiguration(final Map<String, String> initParams) {
        this.initParams = initParams
    }

    @Override
    void preConfigure(final WebAppContext context) throws Exception {
        for (String key : initParams.keySet()) {
            context.setInitParameter(key, initParams[key])
        }
    }
}
