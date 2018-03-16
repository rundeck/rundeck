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

import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import spock.lang.Specification

class RDMultiPluginLoaderSpec extends Specification {
    static interface TestPluginClass {

    }

    static class TestProvider implements TestPluginClass {

    }

    def "test"() {
        given:
        def registry = Mock(RundeckPluginRegistry)
        def pluginService = Mock(PluginService) {
            getRundeckPluginRegistry() >> registry
        }

        def framework = Mock(IFramework)

        def instance = new TestProvider()


        def loader = new RDMultiPluginLoader(
            pluginService: pluginService,
            projectName: 'aproject',
            framework: framework
        )

        when:
        def result = loader.load(TestPluginClass, provider, config)

        then:

        1 * registry.createPluggableService(TestPluginClass)
        1 * pluginService.configurePlugin(provider, config, project, framework, _, loader) >>
        new ConfiguredPlugin<TestPluginClass>(instance, config)

        result == instance

        where:
        config          | provider       | project

        [some: 'value'] | 'testprovider' | 'aproject'
    }
}
