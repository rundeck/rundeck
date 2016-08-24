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

package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.FileBasedGeneratedScript
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification

/**
 * Created by greg on 7/15/16.
 */
class DefaultScriptFileNodeStepUtilsSpec extends Specification {


    public static final String PROJECT_NAME = 'DefaultScriptFileNodeStepUtilsSpec'
    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def cleanup() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    def "basic execute script file"() {
        given:
        def utils = new DefaultScriptFileNodeStepUtils()
        utils.fileCopierUtil = Mock(FileCopierUtil)

        File scriptFile = File.createTempFile("test", ".script");
        scriptFile.deleteOnExit()
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
        }
        ExecutionService executionService = Mock(ExecutionService)
        framework.frameworkServices = Mock(IFrameworkServices) {
            getExecutionService() >> executionService
        }
        def node = new NodeEntryImpl('node')
        node.setOsFamily('unix')
        String filepath = scriptFile.absolutePath
        String[] args = ['someargs'].toArray()
        String testRemotePath = '/tmp/some-path-to-script'
        when:
        def result = utils.executeScriptFile(
                context,
                node,
                null,
                filepath,
                null,
                null,
                args,
                null,
                false,
                executionService

        )
        then:
        result != null
        1 * utils.fileCopierUtil.generateRemoteFilepathForNode(
                node,
                testProject,
                framework,
                scriptFile.getName(),
                null,
                null

        ) >> testRemotePath

        1 * executionService.fileCopyFile(context, _, node, testRemotePath) >> testRemotePath

        1 * executionService.executeCommand(context, {
            (((ExecArgList) it).asFlatStringList()) == ['chmod', '+x', testRemotePath]
        }, node
        ) >> Mock(NodeExecutorResult) {
            isSuccess() >> true
        }

        1 * executionService.executeCommand(context, {
            (((ExecArgList) it).asFlatStringList()) == [testRemotePath, 'someargs']
        }, node
        ) >> Mock(NodeExecutorResult) {
            isSuccess() >> true
        }

        1 * executionService.executeCommand(context, {
            (((ExecArgList) it).asFlatStringList()) == ['rm', '-f', testRemotePath]
        }, node
        ) >> Mock(NodeExecutorResult) {
            isSuccess() >> true
        }
    }

    def "basic execute script file with interpreter"() {
        given:
        def utils = new DefaultScriptFileNodeStepUtils()
        utils.fileCopierUtil = Mock(FileCopierUtil)

        File scriptFile = File.createTempFile("test", ".script");
        scriptFile.deleteOnExit()
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
        }
        ExecutionService executionService = Mock(ExecutionService)
        framework.frameworkServices = Mock(IFrameworkServices) {
            getExecutionService() >> executionService
        }
        def node = new NodeEntryImpl('node')
        node.setOsFamily('unix')
        String filepath = scriptFile.absolutePath
        String[] args = ['someargs'].toArray()
        String testRemotePath = '/tmp/some-path-to-script'
        when:
        def result = utils.executeScriptFile(
                context,
                node,
                null,
                filepath,
                null,
                null,
                args,
                "sudo -blah",
                false,
                executionService

        )
        then:
        result != null
        1 * utils.fileCopierUtil.generateRemoteFilepathForNode(
                node,
                testProject,
                framework,
                scriptFile.getName(),
                null,
                null

        ) >> testRemotePath

        1 * executionService.fileCopyFile(context, _, node, testRemotePath) >> testRemotePath

        1 * executionService.executeCommand(context, {
            (((ExecArgList) it).asFlatStringList()) == ['chmod', '+x', testRemotePath]
        }, node
        ) >> Mock(NodeExecutorResult) {
            isSuccess() >> true
        }

        1 * executionService.executeCommand(context, {
            (((ExecArgList) it).asFlatStringList()) == ['sudo', '-blah', testRemotePath, 'someargs']
        }, node
        ) >> Mock(NodeExecutorResult) {
            isSuccess() >> true
        }

        1 * executionService.executeCommand(context, {
            (((ExecArgList) it).asFlatStringList()) == ['rm', '-f', testRemotePath]
        }, node
        ) >> Mock(NodeExecutorResult) {
            isSuccess() >> true
        }
    }
}
