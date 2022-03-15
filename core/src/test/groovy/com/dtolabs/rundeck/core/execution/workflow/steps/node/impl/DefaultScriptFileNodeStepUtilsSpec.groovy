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
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionLogger
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.FileBasedGeneratedScript
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
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
        def fwkProps = ['rundeck.feature.quoting': 'false']
        def iFrameworkMock = Mock(IFramework){
            getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
        }
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
            getIFramework() >> iFrameworkMock
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
                executionService,
                true
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

    def "basic execute script file disabling sync command"() {
        given:
        def utils = new DefaultScriptFileNodeStepUtils()
        utils.fileCopierUtil = Mock(FileCopierUtil)

        File scriptFile = File.createTempFile("test", ".script");
        scriptFile.deleteOnExit()
        def fwkProps = ['rundeck.feature.quoting': 'false']
        def iFrameworkMock = Mock(IFramework){
            getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
        }
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
            getIFramework() >> iFrameworkMock
        }
        ExecutionService executionService = Mock(ExecutionService)
        framework.frameworkServices = Mock(IFrameworkServices) {
            getExecutionService() >> executionService
        }
        def node = new NodeEntryImpl('node')
        node.setOsFamily('unix')
        node.setAttribute('enable-sync', 'false')
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
                executionService,
                true
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

    def "basic execute script file with retry if script file is busy"() {
        given:
        def utils = new DefaultScriptFileNodeStepUtils()
        utils.fileCopierUtil = Mock(FileCopierUtil)

        File scriptFile = File.createTempFile("test", ".script");
        scriptFile.deleteOnExit()
        ExecutionLogger executionLogger = Mock(ExecutionLogger)
        def fwkProps = ['rundeck.feature.quoting': 'false']
        def iFrameworkMock = Mock(IFramework){
            getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
        }
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
            getExecutionLogger() >> executionLogger
            getIFramework() >> iFrameworkMock
        }
        ExecutionService executionService = Mock(ExecutionService)
        framework.frameworkServices = Mock(IFrameworkServices) {
            getExecutionService() >> executionService
        }
        def node = new NodeEntryImpl('node')
        node.setOsFamily('unix')
        node.setAttribute('file-busy-err-retry', 'true')
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
                executionService,
                true
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
            isSuccess() >> false
            getFailureMessage() >> "Cannot run program \"/tmp/2048-42562-carlos-cgl-ho-dispatch-script.tmp.sh\": error=26, File busy error"
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

    def "basic execute script file sync command"() {
        given:
        def utils = new DefaultScriptFileNodeStepUtils()
        utils.fileCopierUtil = Mock(FileCopierUtil)

        File scriptFile = File.createTempFile("test", ".script");
        scriptFile.deleteOnExit()
        ExecutionLogger executionLogger = Mock(ExecutionLogger)

        def fwkProps = ['rundeck.feature.quoting': 'false']
        def iFrameworkMock = Mock(IFramework){
            getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
        }
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
            getExecutionLogger() >> executionLogger
            getIFramework() >> iFrameworkMock
        }
        ExecutionService executionService = Mock(ExecutionService)
        framework.frameworkServices = Mock(IFrameworkServices) {
            getExecutionService() >> executionService
        }
        def node = new NodeEntryImpl('node')
        node.setOsFamily('unix')
        node.setAttribute('enable-sync', 'true')
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
                executionService,
                true
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
            (((ExecArgList) it).asFlatStringList()) == ['sync']
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
        def fwkProps = ['rundeck.feature.quoting': 'false']
        def iFrameworkMock = Mock(IFramework){
            getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
        }
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
            getIFramework() >> iFrameworkMock
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
                executionService,
                true
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
