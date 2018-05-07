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

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory


class JettyServletContainerCustomizer implements EmbeddedServletContainerCustomizer {

    //This class must be registered in resources.groovy in order for it to be picked up

    @Override
    void customize(final ConfigurableEmbeddedServletContainer container) {
        if(container instanceof JettyEmbeddedServletContainerFactory) {
            //customize jetty here if we need to
        }
    }
}
