/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.TestFor
import rundeck.services.scm.ScmPluginConfig
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(PluginConfigService)
class PluginConfigServiceSpec extends Specification {


    def "loadScmConfig dne"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        IRundeckProject project = Mock(IRundeckProject)

        when:
        def config = service.loadScmConfig('test1', 'a/path', integration)


        then:
        1 * service.frameworkService.getFrameworkProject('test1') >> project
        1 * project.existsFileResource('a/path') >> false
        config == null

        where:
        integration | _
        'import'    | _
        'export'    | _

    }

    def "loadScmConfig exists"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        IRundeckProject project = Mock(IRundeckProject)
        def propertiesString = prefix + '.config.a=b\n' + prefix + '.something=another\n'
        when:
        def config = service.loadScmConfig('test1', 'a/path', prefix)

        then:
        1 * service.frameworkService.getFrameworkProject('test1') >> project
        1 * project.existsFileResource('a/path') >> true
        1 * project.loadFileResource('a/path', _) >> { args ->
            args[1].write(propertiesString.bytes)
            propertiesString.length()
        }
        config != null
        config.prefix == prefix
        config.config == [a: 'b']
        config.getSetting('something') == 'another'


        where:
        prefix   | _
        'import' | _
        'export' | _

    }
    def "storeconfig"(){
        given:
        service.frameworkService = Mock(FrameworkService)
        IRundeckProject project = Mock(IRundeckProject)
        def props = [
                'a.b':'c',
                'a.config.d':'e'
        ] as Properties
        def config = new ScmPluginConfig(props, 'a.')

        when:
        service.storeConfig(config, 'test1', 'a/path')

        then:
        1 * service.frameworkService.getFrameworkProject('test1') >> project
        1 * project.storeFileResource('a/path', _)

    }
}
