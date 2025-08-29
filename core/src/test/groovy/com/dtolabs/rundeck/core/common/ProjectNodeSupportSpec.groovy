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

package com.dtolabs.rundeck.core.common

import com.dtolabs.rundeck.core.plugins.ExtPluginConfiguration
import com.dtolabs.rundeck.core.plugins.PluginConfiguration
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.core.utils.FileUtils
import spock.lang.Specification

class ProjectNodeSupportSpec extends Specification {
    static final String PROJECT_NAME = 'ProjectNodeSupportSpec'
    Framework framework
    FrameworkProject testProject
    File directory

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
        directory = new File(testProject.getBaseDir(), "testGetNodesMultiFile");
        FileUtils.deleteDir(directory)
        directory.mkdirs();
    }

    def cleanup() {
        if (directory.exists()) {
            FileUtils.deleteDir(directory)
        }
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    def "reloads after closed"() {
        given:
        Date modifiedTime = new Date()
        def config = Mock(IRundeckProjectConfig) {
            getName() >> PROJECT_NAME
            getProperties() >> ([
                    'resources.source.1.type'                            : 'file',
                    'resources.source.1.config.file'                     : '/tmp/file',
                    'resources.source.1.config.requireFileExists'        : 'false',
                    'resources.source.1.config.includeServerNode'        : 'true',
                    'resources.source.1.config.generateFileAutomatically': 'false',
                    'resources.source.1.config.format'                   : 'resourcexml',
            ] as Properties)
            getConfigLastModifiedTime() >> modifiedTime
        }
        def generatorService = ResourceFormatGeneratorService.getInstanceForFramework(framework,framework)
        def sourceService = ResourceModelSourceService.getInstanceForFramework(framework,framework)

        def support = new ProjectNodeSupport(config, generatorService, sourceService)

        when:

        def result = support.getNodeSet()
        support.close()
        def result2 = support.getNodeSet()

        then:
        result != null

        result.nodeNames != null
        result.nodeNames.size() == 1

        result2 != null
        result2.nodeNames != null
        result2.nodeNames.size() == 1

    }

    def "supports factory function for source"() {

        given:
        Date modifiedTime = new Date()
        def config = Mock(IRundeckProjectConfig) {
            getName() >> PROJECT_NAME
            getProperties() >> (
                [
                    'resources.source.1.type'                            : 'file',
                    'resources.source.1.config.file'                     : '/tmp/file',
                    'resources.source.1.config.requireFileExists'        : 'false',
                    'resources.source.1.config.includeServerNode'        : 'true',
                    'resources.source.1.config.generateFileAutomatically': 'false',
                    'resources.source.1.config.format'                   : 'resourcexml',
                ] as Properties
            )
            getConfigLastModifiedTime() >> modifiedTime
        }
        def generatorService = ResourceFormatGeneratorService.getInstanceForFramework(framework,framework)
        def sourceService = ResourceModelSourceService.getInstanceForFramework(framework,framework)

        def support = new ProjectNodeSupport(config, generatorService, sourceService, null)

        when:

        def result = support.getNodeSet()
        support.close()
        def result2 = support.getNodeSet()

        then:
        result != null

        result.nodeNames != null
        result.nodeNames.size() == 1

        result2 != null
        result2.nodeNames != null
        result2.nodeNames.size() == 1
    }

    def "serialize plugin config"() {
        given:
            def prefix = "test1.abc"
            List<PluginConfiguration> configs = [
                    new SimplePluginConfiguration.SimplePluginConfigurationBuilder()
                            .service('AService')
                            .provider('provider1')
                            .configuration(
                            [
                                    a: 'b',
                                    c: 'd'
                            ]
                    ).build(),

                    new SimplePluginConfiguration.SimplePluginConfigurationBuilder()
                            .service('AService')
                            .provider('provider2')
                            .configuration(
                            [
                                    x: 'y',
                                    z: 'w'
                            ]
                    ).build(),

            ]
        when:
            def result = ProjectNodeSupport.serializePluginConfigurations(prefix, configs)
        then:
            result
            result.size() == 6
            result == [
                    (prefix + '.1.type'): 'provider1',
                    (prefix + '.1.config.a'): 'b',
                    (prefix + '.1.config.c'): 'd',
                    (prefix + '.2.type'): 'provider2',
                    (prefix + '.2.config.x'): 'y',
                    (prefix + '.2.config.z'): 'w',

            ]
    }

    def "serialize plugin config with extra"() {
        given:
            def prefix = "test1.abc"
            List<ExtPluginConfiguration> configs = [
                    new SimplePluginConfiguration.SimplePluginConfigurationBuilder()
                            .service('AService')
                            .provider('provider1')
                            .extra([q: 't', r: 'v', type: 'nogo', 'config.blah': 'alsonot'])
                            .configuration(
                            [
                                    a: 'b',
                                    c: 'd'
                            ]
                    ).build(),

                    new SimplePluginConfiguration.SimplePluginConfigurationBuilder()
                            .service('AService')
                            .provider('provider2')
                            .configuration(
                            [
                                    x: 'y',
                                    z: 'w'
                            ]
                    ).build(),

            ]
        when:
            def result = ProjectNodeSupport.serializePluginConfigurations(prefix, configs, true)
        then:
            result
            result.size() == 9
            result == [
                    (prefix + '.1.type')    : 'provider1',
                    (prefix + '.1.config.a'): 'b',
                    (prefix + '.1.config.c'): 'd',
                    (prefix + '.1.config.blah'): 'alsonot',
                    (prefix + '.1.q')       : 't',
                    (prefix + '.1.r')       : 'v',
                    (prefix + '.2.type')    : 'provider2',
                    (prefix + '.2.config.x'): 'y',
                    (prefix + '.2.config.z'): 'w',

            ]
    }

    def "read plugin configs with extra"() {

        given:
            def prefix = "xyz"
            def props = [
                    (prefix + '.1.type')    : 'provider1',
                    (prefix + '.1.config.a'): 'b',
                    (prefix + '.1.config.c'): 'd',
                    (prefix + '.1.q')       : 't',
                    (prefix + '.1.r')       : 'v',
                    (prefix + '.2.type')    : 'provider2',
                    (prefix + '.2.config.x'): 'y',
                    (prefix + '.2.config.z'): 'w',

            ]
            def svc = "asdf"
        when:

            def result = ProjectNodeSupport.listPluginConfigurations(props, prefix, svc, true)
        then:
            result.size() == 2
            result[0].service == svc
            result[0].provider == 'provider1'
            result[0].configuration == [
                    a: 'b',
                    c: 'd'
            ]
            result[0].extra == [
                    q: 't',
                    r: 'v'
            ]


            result[1].service == svc
            result[1].provider == 'provider2'
            result[1].configuration == [
                    x: 'y',
                    z: 'w'
            ]
            result[1].extra == [:]


    }

    def "read plugin configs with extra 2"() {

        given:
        def prefix = "xyz"
        def props = [
                (prefix + '.1.type')    : 'provider1',
                (prefix + '.1.config.a'): 'b',
                (prefix + '.1.config.c'): 'd',
                (prefix + '.1.q')       : 't',
                (prefix + '.1.r')       : 'v',
                (prefix + '.1.z.y.a')       : 'config1',
                (prefix + '.1.z.y.b')   : 'config2',
                (prefix + '.1.r')       : 'v',
                (prefix + '.2.type')    : 'provider2',
                (prefix + '.2.config.x'): 'y',
                (prefix + '.2.config.z'): 'w',

        ]
        def svc = "asdf"
        when:

        def result = ProjectNodeSupport.listPluginConfigurations(props, prefix, svc, true)
        then:
        result.size() == 2
        result[0].service == svc
        result[0].provider == 'provider1'
        result[0].configuration == [
                a: 'b',
                c: 'd'
        ]
        result[0].extra == [
                q: 't',
                r: 'v',
                z: [
                    y: [
                            a: 'config1',
                            b: 'config2',
                    ]
                ]

            ]

        result[1].service == svc
        result[1].provider == 'provider2'
        result[1].configuration == [
                x: 'y',
                z: 'w'
        ]
        result[1].extra == [:]


    }


    def "load listResourceModelConfigurations"(){
        given:
        def prefix = "resources.source"
        def props = [
                (prefix + '.1.type')    : 'provider1',
                (prefix + '.1.config.a'): 'b',
                (prefix + '.1.config.c'): 'd',
                (prefix + '.1.z.y.a')       : 'config1',
                (prefix + '.1.z.y.b')   : 'config2',
                (prefix + '.2.type')    : 'provider2',
                (prefix + '.2.config.x'): 'y',
                (prefix + '.2.config.z'): 'w',

        ]
        Properties properties = new Properties()
        properties.putAll(props)


        when:
        def result = ProjectNodeSupport.listResourceModelConfigurations(properties)

        then:
        result!=null
        result[0].type == "provider1"
        result[0].props == ['a':'b','c':'d']
        result[0].extraProps == ['z.y.a':'config1','z.y.b':'config2']
        result[1].type == "provider2"
        result[1].props == ['x':'y','z':'w']
        result[1].extraProps == [:]
    }

}
