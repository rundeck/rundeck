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


import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionLogger
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import spock.lang.Specification
/**
 * Created by greg on 7/15/16.
 */
class DefaultScriptFileNodeStepUtilsSpec extends Specification {


    public static final String PROJECT_NAME = 'DefaultScriptFileNodeStepUtilsSpec'

    def setup() {
    }

    def cleanup() {
    }

    def "write script string to temp file"() {
        given:
            File tempFile = File.createTempFile("test", ".script");
            def utils = new DefaultScriptFileNodeStepUtils()
            utils.fileCopierUtil = Mock(FileCopierUtil)
            StepExecutionContext context = mockContext(null, null)
            def node = new NodeEntryImpl('node')
            node.setOsFamily('unix')
            String script = 'echo "hello"\n'
            def modifier = Mock(FileCopierUtil.ContentModifier)
        when:
            def result = utils.writeScriptToTempFile(context, node, script, null, null, true, modifier)
        then:
            result == tempFile
            1 * utils.fileCopierUtil.writeScriptTempFile(
                context,
                null,
                null,
                script,
                node,
                true,
                modifier
            ) >> tempFile
    }

    def "write script file to temp file"() {
        given:
            File srcFile = File.createTempFile("test", ".script");
            File tempFile = File.createTempFile("test", ".script");
            def utils = new DefaultScriptFileNodeStepUtils()
            utils.fileCopierUtil = Mock(FileCopierUtil)
            StepExecutionContext context = mockContext(null, null)
            def node = new NodeEntryImpl('node')
            node.setOsFamily('unix')
            def modifier = Mock(FileCopierUtil.ContentModifier)
        when:
            def result = utils.writeScriptToTempFile(context, node, null, srcFile.absolutePath, null, true, modifier)
        then:
            result == tempFile
            1 * utils.fileCopierUtil.writeScriptTempFile(
                context,
                null,
                !null,
                null,
                node,
                true,
                modifier
            ) >> tempFile
    }
    def "write script stream to temp file"() {
        given:

            File tempFile = File.createTempFile("test", ".script")
            def utils = new DefaultScriptFileNodeStepUtils()
            utils.fileCopierUtil = Mock(FileCopierUtil)
            StepExecutionContext context = mockContext(null, null)
            def node = new NodeEntryImpl('node')
            node.setOsFamily('unix')
            ByteArrayInputStream scriptStream = new ByteArrayInputStream('echo "hello"\n'.bytes)
            def modifier = Mock(FileCopierUtil.ContentModifier)
        when:
            def result = utils.writeScriptToTempFile(context, node, null, null, scriptStream, true, modifier)
        then:
            result == tempFile
            1 * utils.fileCopierUtil.writeScriptTempFile(
                context,
                null,
                scriptStream,
                null,
                node,
                true,
                modifier
            ) >> tempFile
    }

    def "basic execute script file"() {
        given:
        def utils = new DefaultScriptFileNodeStepUtils()
        utils.fileCopierUtil = Mock(FileCopierUtil)

        File scriptFile = File.createTempFile("test", ".script");
        scriptFile.deleteOnExit()
        def fwkProps = ['rundeck.feature.quoting.backwardCompatible': 'false']
        ExecutionService executionService = Mock(ExecutionService)
        StepExecutionContext context = mockContext(fwkProps, executionService)
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
                !null,
                _,
                scriptFile.getName(),
                null,
                null

        ) >> testRemotePath

            1 * utils.fileCopierUtil.writeScriptTempFile(context, null, _, null, node, true, null) >> scriptFile
        1 * executionService.fileCopyFile(context, _, node, testRemotePath) >> testRemotePath

        1 * executionService.executeCommand(context, {
            (((ExecArgList) it).asFlatStringList()) == ['chmod', '+x', testRemotePath]
        }, node
        ) >> Mock(NodeExecutorResult) {
            isSuccess() >> true
        }

            1 * executionService.executeCommand(
                context,
                {
                    (((ExecArgList) it).asFlatStringList()) == [testRemotePath, 'someargs']
                },
                _,
                node
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

    private StepExecutionContext mockContext(fwkProps, ExecutionService svc) {
        Mock(StepExecutionContext) {
            _ * getFrameworkProject() >> PROJECT_NAME
            _ * getIFramework() >> Mock(IFramework) {
                _ * getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever(fwkProps)
                _ * getFrameworkProjectMgr() >> Mock(ProjectManager) {
                    _ * getFrameworkProject(PROJECT_NAME) >> Mock(IRundeckProject)
                }
                _ * getExecutionService() >> svc
            }
        }
    }

    def "execute script file with content modifier"() {
        given:
            def utils = new DefaultScriptFileNodeStepUtils()
            utils.fileCopierUtil = Mock(FileCopierUtil)

            File scriptFile = File.createTempFile("test", ".script");
            scriptFile.deleteOnExit()
            def fwkProps = ['rundeck.feature.quoting.backwardCompatible': 'false']
            ExecutionService executionService = Mock(ExecutionService)
            StepExecutionContext context = mockContext(fwkProps, executionService)

            def node = new NodeEntryImpl('node')
            node.setOsFamily('unix')
            String filepath = scriptFile.absolutePath
            String[] args = ['someargs'].toArray()
            String testRemotePath = '/tmp/some-path-to-script'
            scriptFile.text = 'echo "hello"\n'
            def modifier = Mock(FileCopierUtil.ContentModifier)
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
                null,
                false,
                executionService,
                true,
                modifier
            )
        then:
            result != null

            1 * utils.fileCopierUtil.writeScriptTempFile(context, null, _, null, node, true, modifier) >> scriptFile
            1 * utils.fileCopierUtil.generateRemoteFilepathForNode(
                node,
                !null,
                _,
                scriptFile.getName(),
                null,
                null

            ) >> testRemotePath

            1 * executionService.fileCopyFile(context, scriptFile, node, testRemotePath) >> testRemotePath

            1 * executionService.executeCommand(
                context, {
                (((ExecArgList) it).asFlatStringList()) == ['chmod', '+x', testRemotePath]
            }, node
            ) >> Mock(NodeExecutorResult) {
                isSuccess() >> true
            }

            1 * executionService.executeCommand(
                context, {
                (((ExecArgList) it).asFlatStringList()) == [testRemotePath, 'someargs']
            },
                _,
                node
            ) >> Mock(NodeExecutorResult) {
                isSuccess() >> true
            }

            1 * executionService.executeCommand(
                context, {
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
        def fwkProps = ['rundeck.feature.quoting.backwardCompatible': 'false']
        ExecutionService executionService = Mock(ExecutionService)
            StepExecutionContext context = mockContext(fwkProps, executionService)
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
                !null,
                _,
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
        },
                                            _,
                                            node
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
        def fwkProps = ['rundeck.feature.quoting.backwardCompatible': 'false']
        ExecutionService executionService = Mock(ExecutionService)
            StepExecutionContext context = mockContext(fwkProps, executionService)
            _ * context.getExecutionLogger() >> executionLogger
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
                !null,
                _,
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
        }, _,
                                            node
        ) >> Mock(NodeExecutorResult) {
            isSuccess() >> false
            getFailureMessage() >> "Cannot run program \"/tmp/2048-42562-carlos-cgl-ho-dispatch-script.tmp.sh\": error=26, File busy error"
        }

        1 * executionService.executeCommand(context, {
            (((ExecArgList) it).asFlatStringList()) == [testRemotePath, 'someargs']
        },
                                            _,
                                            node
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

        def fwkProps = ['rundeck.feature.quoting.backwardCompatible': 'false']
        ExecutionService executionService = Mock(ExecutionService)
            StepExecutionContext context = mockContext(fwkProps, executionService)
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
                !null,
                _,
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
        },
                                            _,
                                            node
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
        def fwkProps = ['rundeck.feature.quoting.backwardCompatible': 'false']
        ExecutionService executionService = Mock(ExecutionService)
            StepExecutionContext context = mockContext(fwkProps, executionService)
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
                !null,
                _,
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
        },
                                            _,
                                            node
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
