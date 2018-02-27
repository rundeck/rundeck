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

package authfilter

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer

import javax.servlet.DispatcherType

class AuthFilterJettyCustomizer implements JettyServerCustomizer {

    private static final Logger log = LoggerFactory.getLogger(this)

    @Override
    void customize(final Server server) {
        log.debug("Add auth filter to servlet context")
        ((WebAppContext)server.handler).addFilter("com.dtolabs.rundeck.server.filters.AuthFilter","/*",EnumSet.of(DispatcherType.REQUEST))
    }
}
