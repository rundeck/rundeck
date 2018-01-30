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
        def generatorService = new ResourceFormatGeneratorService(framework)
        def sourceService = new ResourceModelSourceService(framework)

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
}
