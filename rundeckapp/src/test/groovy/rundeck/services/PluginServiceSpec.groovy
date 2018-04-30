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

package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.server.plugins.PluginRegistry
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * @author greg
 * @since 6/12/17
 */
@TestFor(PluginService)
class PluginServiceSpec extends Specification {
    def "configure plugin does not exist"() {
        given:
        service.rundeckPluginRegistry = Mock(PluginRegistry)
        def name = 'atest'
        def config = [:]
        def project = 'aproject'
        def framework = null
        def providerservice = Mock(PluggableProviderService)

        when:
        def result = service.configurePlugin(name, config, project, framework, providerservice)

        then:
        result == null
        //validatePluginByName returns null
        1 * service.rundeckPluginRegistry.validatePluginByName('atest', providerservice, null, 'aproject', config) >>
                null


    }
}
