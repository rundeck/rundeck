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
import com.dtolabs.rundeck.core.resources.FileResourceModelPathPolicy
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
        File varDir = File.createTempFile("ProjectNodeSupportSpec-varDir", "-tmp")
        def config = Mock(IRundeckProjectConfig) {
            getName() >> PROJECT_NAME
            getProperties() >> ([
                    'resources.source.1.type'                            : 'file',
                    'resources.source.1.config.file'                     : new File(
                            testProject.baseDir,
                            'etc/file'
                    ).absolutePath,
                    'resources.source.1.config.requireFileExists'        : 'false',
                    'resources.source.1.config.includeServerNode'        : 'true',
                    'resources.source.1.config.generateFileAutomatically': 'false',
                    'resources.source.1.config.format'                   : 'resourcexml',
            ] as Properties)
            getConfigLastModifiedTime() >> modifiedTime
        }
        def generatorService = ResourceFormatGeneratorService.getInstanceForFramework(framework,framework)
        def sourceService = ResourceModelSourceService.getInstanceForFramework(framework,framework)

        def support = new ProjectNodeSupport(varDir, config, generatorService, sourceService)

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
        File varDir = File.createTempFile("ProjectNodeSupportSpec-varDir", "-tmp")
        def config = Mock(IRundeckProjectConfig) {
            getName() >> PROJECT_NAME
            getProperties() >> (
                [
                    'resources.source.1.type'                            : 'file',
                    'resources.source.1.config.file'                     : new File(
                            testProject.baseDir,
                            'etc/file'
                    ).absolutePath,
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

        def support = new ProjectNodeSupport(varDir, config, generatorService, sourceService, null)

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

    def "loads file source within project directory"() {
        given:
            File nodesFile = copyNodeFixture(new File(testProject.baseDir, "etc/resources.xml"))
            ProjectNodeSupport support = createFileSourceSupport(nodesFile)

        when:
            def result = support.nodeSet

        then:
            !support.hasInvalidFileResourceModelPaths()
            result.nodeNames.containsAll(['test1', 'testnode2'])
            support.resourceModelSourceExceptionsMap.isEmpty()

        cleanup:
            support?.close()
    }

    def "loads file source within configured allowed base path"() {
        given:
            File allowedDirectory = File.createTempDir("ProjectNodeSupportSpec-allowed", "")
            File nodesFile = copyNodeFixture(new File(allowedDirectory, "resources.xml"))
            ProjectNodeSupport support = createFileSourceSupport(nodesFile, allowedDirectory.absolutePath)

        when:
            def result = support.nodeSet

        then:
            !support.hasInvalidFileResourceModelPaths()
            result.nodeNames.containsAll(['test1', 'testnode2'])
            support.resourceModelSourceExceptionsMap.isEmpty()

        cleanup:
            support?.close()
            FileUtils.deleteDir(allowedDirectory)
    }

    def "rejects file source outside permitted directories without leaking path"() {
        given:
            File outsideDirectory = File.createTempDir("ProjectNodeSupportSpec-outside", "")
            File nodesFile = copyNodeFixture(new File(outsideDirectory, "sensitive-resources.xml"))
            ProjectNodeSupport support = createFileSourceSupport(nodesFile)

        when:
            boolean invalidBeforeLoad = support.hasInvalidFileResourceModelPaths()
            def result = support.nodeSet

        then:
            invalidBeforeLoad
            !result.nodeNames.contains('test1')
            support.resourceModelSourceExceptionsMap['1.source']
            support.resourceModelSourceExceptionsMap['1.source'].message.contains(
                    'File path must be within the project directory'
            )
            !support.resourceModelSourceExceptionsMap['1.source'].message.contains(nodesFile.absolutePath)

        cleanup:
            support?.close()
            FileUtils.deleteDir(outsideDirectory)
    }

    def "does not apply file path policy to non-file source types"() {
        given:
            Properties properties = new Properties()
            properties.setProperty('resources.source.1.type', 'local')
            properties.setProperty('resources.source.1.config.file', '/outside/project/resources.xml')
            ProjectNodeSupport support = createSupport(
                    properties,
                    new FileResourceModelPathPolicy(testProject.baseDir, null)
            )

        when:
            def result = support.nodeSet

        then:
            result != null
            support.resourceModelSourceExceptionsMap.isEmpty()

        cleanup:
            support?.close()
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

    /**
     * Creates project node support for a configured file source.
     */
    private ProjectNodeSupport createFileSourceSupport(
            final File nodesFile,
            final String allowedBasePaths = null
    ) {
        Properties properties = new Properties()
        properties.setProperty('resources.source.1.type', 'file')
        properties.setProperty('resources.source.1.config.file', nodesFile.absolutePath)
        properties.setProperty('resources.source.1.config.format', 'resourcexml')
        properties.setProperty('resources.source.1.config.includeServerNode', 'false')
        createSupport(
                properties,
                new FileResourceModelPathPolicy(testProject.baseDir, allowedBasePaths)
        )
    }

    /**
     * Creates project node support with the supplied source configuration and path policy.
     */
    private ProjectNodeSupport createSupport(
            final Properties properties,
            final FileResourceModelPathPolicy pathPolicy
    ) {
        IRundeckProjectConfig config = Mock(IRundeckProjectConfig) {
            getName() >> PROJECT_NAME
            getProperties() >> properties
            getConfigLastModifiedTime() >> new Date()
        }
        new ProjectNodeSupport(
                File.createTempDir("ProjectNodeSupportSpec-var", ""),
                config,
                ResourceFormatGeneratorService.getInstanceForFramework(framework, framework),
                ResourceModelSourceService.getInstanceForFramework(framework, framework),
                null,
                pathPolicy
        )
    }

    /**
     * Copies the shared XML node fixture to the requested file.
     */
    private static File copyNodeFixture(final File destination) {
        destination.parentFile.mkdirs()
        destination.bytes = new File(
                'src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml'
        ).bytes
        destination
    }

}
