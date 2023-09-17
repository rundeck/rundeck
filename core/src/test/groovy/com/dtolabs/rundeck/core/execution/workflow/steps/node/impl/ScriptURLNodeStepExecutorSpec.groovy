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

package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl


import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IExecutionProviders
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ServiceSupport
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionServiceImpl
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher
import com.dtolabs.rundeck.core.execution.service.FileCopier
import com.dtolabs.rundeck.core.execution.service.FileCopierException
import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 5/9/17
 */
class ScriptURLNodeStepExecutorSpec extends Specification {
    String projName
    ServiceSupport serviceSupport
    ExecutionServiceImpl executionServiceImpl
    Framework frameworkInstance
    def setup(){
        projName = "TestScriptFileNodeStepExecutor"
        serviceSupport=new ServiceSupport()

        executionServiceImpl = new ExecutionServiceImpl()

        IExecutionProviders frameworkPlugins = Mock(IExecutionProviders) {
            _ * getStepExecutorForItem(_, _) >> Mock(StepExecutor)
            _ * getFileCopierForNodeAndProject(_, _) >> Mock(FileCopier)
            _ * getNodeDispatcherForContext(_) >> Mock(NodeDispatcher)
            _ * getNodeExecutorForNodeAndProject(_, _) >> Mock(NodeExecutor)
            _ * getNodeStepExecutorForItem(_, _) >> Mock(NodeStepExecutor)
        }
        executionServiceImpl.setExecutionProviders(frameworkPlugins)
        serviceSupport.executionProviders = frameworkPlugins
        serviceSupport.executionService = executionServiceImpl
        frameworkInstance = AbstractBaseTest.createTestFramework(serviceSupport)
        final IRundeckProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
                projName);
        AbstractBaseTest.generateProjectResourcesFile(
                new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                frameworkProject
        );
    }

    @Shared
    MockWebServer server

    def setupSpec() {
        server = new MockWebServer()
        server.start()
    }

    def cleanupSpec() {
        server.shutdown()
    }

    @Unroll
    def "shared context data in url string"() {
        given:
        final WFSharedContext datacontext = new WFSharedContext();
        datacontext.merge(
                ContextView.global(),
                new BaseDataContext("data", [value: 'some value ? for things & stuff'])
        );
        datacontext.merge(ContextView.node("anodename"), new BaseDataContext("node", [name: 'node/name']));
        datacontext.merge(ContextView.node("bnodename"), new BaseDataContext("node", [name: 'bogus']));
        datacontext.merge(ContextView.node("bnodename"), new BaseDataContext("data", [value: 'hotenntot']));
        when:
        def result = ScriptURLNodeStepExecutor.expandUrlString(
                url,
                datacontext,
                curnode
        )
        then:
        result != null
        result == expected

        where:
        url                                                                            | curnode     | expected

        'http://example.com/path/${node.name}?query=${data.value}'                     | "anodename" |
                'http://example.com/path/node/name?query=some%20value%20%3F%20for%20things%20%26%20stuff'
        'http://example.com/path/${node.name}?query=${data.value}'                     | "bnodename" |
                'http://example.com/path/bogus?query=hotenntot'
        'http://example.com/path/${node.name@bnodename}?query=${data.value}'           | "anodename" |
                'http://example.com/path/bogus?query=some%20value%20%3F%20for%20things%20%26%20stuff'
        'http://example.com/path/${node.name@bnodename}?query=${data.value@bnodename}' | "anodename" |
                'http://example.com/path/bogus?query=hotenntot'

    }

    private void setupNodeExecutor(NodeExecutor testexec, FileCopier fileCopier) {
        def frameworkPlugins = Mock(IExecutionProviders) {
            _ * getNodeExecutorForNodeAndProject(_, _) >> testexec
            _ * getFileCopierForNodeAndProject(_, _) >> fileCopier
        }
        executionServiceImpl.setExecutionProviders(frameworkPlugins)
        serviceSupport.executionProviders = frameworkPlugins
    }



    def "interpret command script file local"() {
        given:
        ScriptURLNodeStepExecutor interpret = new ScriptURLNodeStepExecutor(frameworkInstance)

        //setup nodeexecutor for local node
        MultiTestNodeExecutor testexec = new MultiTestNodeExecutor()
        TestFileCopier testcopier = new TestFileCopier()
        setupNodeExecutor(testexec,testcopier)

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1")
        test1.setOsFamily("unix")


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(projName)
                .framework(frameworkInstance)
                .user("blah")
                .dataContext(new BaseDataContext("data", [value: 'a text value']))
                .build()
        final String urlString = server.url("/").toString()
        ScriptURLCommandBase command = createCommandBase(urlString, expandToken)
        
        when:
        final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>()
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null))
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null))
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null))
        testexec.testResult = nodeExecutorResults
        testcopier.testResult = "/test/file/path"

        String testcontent = "test script content @data.value@"
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type","text/plain").setBody(testcontent))

        final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1)

        then:
        null != interpreterResult
        interpreterResult.isSuccess()
        nodeExecutorResults.get(1) == interpreterResult

        context == testcopier.testContext
        null == testcopier.testScript
        null == testcopier.testInput
        null != testcopier.testFile.getAbsolutePath()
        test1 == testcopier.testNode
        expectedTextContent == testcopier.fileContent

        testexec.index <= testexec.testResult.size()

        //test nodeexecutor was called twice
        3 == testexec.index
        //first call is chmod +x filepath
        final String[] strings = testexec.testCommand.get(0);
        3 == strings.length
        "chmod" == strings[0]
        "+x" == strings[1]
        String filepath = strings[2];
        filepath.endsWith(".sh")
        test1 == testexec.testNode.get(0)

        //second call is to exec the filepath
        final String[] strings2 = testexec.testCommand.get(1);
        1 == strings2.length
        strings2[0] == filepath
        filepath == strings2[0]
        test1 == testexec.testNode.get(1)

        //third call is to remove remote file
        final String[] strings3 = testexec.testCommand.get(2);
        3 == strings3.length
        "rm" == strings3[0]
        "-f" == strings3[1]
        filepath == strings3[2]
        test1 == testexec.testNode.get(2)

        where:
        expandToken | expectedTextContent
        true        | "test script content a text value\n"
        false       | "test script content @data.value@"
        true        | "test script content a text value\n"
        false       | "test script content @data.value@"
    }

    def "interpret command script file local changing file extension"() {
        given:

        ScriptURLNodeStepExecutor interpret = new ScriptURLNodeStepExecutor(frameworkInstance)

        //setup nodeexecutor for local node
            MultiTestNodeExecutor testexec = new MultiTestNodeExecutor()
            TestFileCopier testcopier = new TestFileCopier()
            setupNodeExecutor(testexec,testcopier)

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1")
        test1.setOsFamily("unix")


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(projName)
                .framework(frameworkInstance)
                .user("blah")
                .build()
        final String urlString = server.url("/").toString()
        String fileExtension = "myext";
        ScriptURLCommandBase command = createCommandBase(urlString, false, fileExtension)

        when:
        final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>()
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null))
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null))
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null))
        testexec.testResult = nodeExecutorResults
        testcopier.testResult = "/test/file/path"

        String testcontent = "test script content"
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type","text/plain").setBody(testcontent))

        final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1)

        then:

        null != interpreterResult
        interpreterResult.isSuccess()
        nodeExecutorResults.get(1) == interpreterResult

        context == testcopier.testContext
        null == testcopier.testScript
        null == testcopier.testInput
        null != testcopier.testFile.getAbsolutePath()
        test1 == testcopier.testNode

        testexec.index <= testexec.testResult.size()

        //test nodeexecutor was called twice
        3 == testexec.index
        //first call is chmod +x filepath
        final String[] strings = testexec.testCommand.get(0);
        3 == strings.length
        "chmod" == strings[0]
        "+x" == strings[1]
        String filepath = strings[2];
        filepath.endsWith("." + fileExtension)
    }

    def "interpret command script file local changing invocation"() {
        given:

        ScriptURLNodeStepExecutor interpret = new ScriptURLNodeStepExecutor(frameworkInstance)

        //setup nodeexecutor for local node
            MultiTestNodeExecutor testexec = new MultiTestNodeExecutor()
            TestFileCopier testcopier = new TestFileCopier()
            setupNodeExecutor(testexec,testcopier)

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1")
        test1.setOsFamily("unix")


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject(projName)
                .framework(frameworkInstance)
                .user("blah")
                .build()
        final String urlString = server.url("get-script").toString()
        String invocation = 'mycommand ${scriptfile}';
        ScriptURLCommandBase command = createCommandBase(urlString, false, null, invocation)

        when:
        final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>()
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null))
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null))
        nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null))
        testexec.testResult = nodeExecutorResults
        testcopier.testResult = "/test/file/path"

        String testcontent = "test script content"
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type","text/plain").setBody(testcontent))

        final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1)

        then:

        null != interpreterResult
        interpreterResult.isSuccess()
        nodeExecutorResults.get(1) == interpreterResult

        context == testcopier.testContext
        null == testcopier.testScript
        null == testcopier.testInput
        null != testcopier.testFile.getAbsolutePath()
        test1 == testcopier.testNode

        testexec.index <= testexec.testResult.size()

        //second call is to exec the filepath
        final String[] strings2 = testexec.testCommand.get(1);
        2 == strings2.length
        "mycommand" == strings2[0]
        String filepath = strings2[1];
        filepath == strings2[1]

        test1 == testexec.testNode.get(1)
    }

    private ScriptURLCommandBase createCommandBase(String urlString, boolean expandToken = false, String fileExtension = null, String interpreter = null) {
        return new ScriptURLCommandBase() {
            public String getURLString() {
                return urlString
            }

            public String[] getArgs() {
                return new String[0]
            }

            public StepExecutionItem getFailureHandler() {
                return null
            }

            public boolean isKeepgoingOnSuccess() {
                return false
            }

            public String getScriptInterpreter() {
                return interpreter
            }

            @Override
            public String getFileExtension() {
                return fileExtension
            }

            public boolean getInterpreterArgsQuoted() {
                return false
            }

            @Override
            public boolean isExpandTokenInScriptFile() {
                return expandToken
            }
        }
    }

    public static class MultiTestNodeExecutor implements NodeExecutor {
        List<ExecutionContext> testContext = new ArrayList<ExecutionContext>()
        List<String[]> testCommand = new ArrayList<String[]>()
        List<INodeEntry> testNode = new ArrayList<INodeEntry>()
        List<NodeExecutorResult> testResult = new ArrayList<NodeExecutorResult>()
        int index = 0

        public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) {
            this.testContext.add(context)
            this.testCommand.add(command)
            this.testNode.add(node)

            return testResult.get(index++)
        }

    }

    public static class TestFileCopier implements FileCopier {
        String testResult
        ExecutionContext testContext
        InputStream testInput
        INodeEntry testNode
        boolean throwException

        @Override
        public String copyFileStream(
                final ExecutionContext context, final InputStream input, final INodeEntry node, final String destination
        ) throws FileCopierException {
            testContext = context
            testNode = node
            testInput = input
            if (throwException) {
                throw new FileCopierException("copyFileStream test", TestScriptURLNodeStepExecutor.TestReason.Test)
            }

            return destination
        }

        File testFile
        String fileContent

        @Override
        public String copyFile(
                final ExecutionContext context,
                final File file,
                final INodeEntry node,
                final String destination
        )
                throws FileCopierException {
            testContext = context
            testNode = node
            testFile = file
            fileContent = file?.text
            if (throwException) {
                throw new FileCopierException("copyFile test", TestScriptURLNodeStepExecutor.TestReason.Test)
            }
            return destination
        }

        String testScript

        @Override
        public String copyScriptContent(
                final ExecutionContext context, final String script, final INodeEntry node, final String destination
        ) throws FileCopierException {
            testContext = context
            testNode = node
            testScript = script

            if (throwException) {
                throw new FileCopierException("copyScriptContent test", TestScriptURLNodeStepExecutor.TestReason.Test)
            }
            return destination
        }

    }

}
