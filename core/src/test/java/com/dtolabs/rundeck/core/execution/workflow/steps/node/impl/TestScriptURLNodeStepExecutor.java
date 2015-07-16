/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* TestScriptURLNodeStepExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 5/2/12 6:12 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.common.impl.URLFileUpdater;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * TestScriptURLNodeStepExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class TestScriptURLNodeStepExecutor extends AbstractBaseTest {
    private static final String PROJ_NAME = "TestScriptFileNodeStepExecutor";

    public TestScriptURLNodeStepExecutor(String name) {
        super(name);
    }

    public void setUp() {


        final Framework frameworkInstance = getFrameworkInstance();
        final IRundeckProject frameworkProject = frameworkInstance.getFrameworkProjectMgr().createFrameworkProject(
                PROJ_NAME);
        generateProjectResourcesFile(
                new File("src/test/resources/com/dtolabs/rundeck/core/common/test-nodes1.xml"),
                frameworkProject
        );

    }

    public void tearDown() throws Exception {
        super.tearDown();
        File projectdir = new File(getFrameworkProjectsBase(), PROJ_NAME);
        FileUtils.deleteDir(projectdir);

    }

    public void testInterpretCommand() throws Exception {

    }
    static enum TestReason implements FailureReason{
        Test
    }

    public static class testFileCopier implements FileCopier,DestinationFileCopier {
        String testResult;
        ExecutionContext testContext;
        InputStream testInput;
        INodeEntry testNode;
        boolean throwException;

        public String copyFileStream(ExecutionContext context, InputStream input, INodeEntry node) throws
            FileCopierException {
            testContext = context;
            testNode = node;
            testInput = input;
            if (throwException) {
                throw new FileCopierException("copyFileStream test",TestReason.Test);
            }
            return testResult;
        }

        @Override
        public String copyFileStream(
                final ExecutionContext context, final InputStream input, final INodeEntry node, final String destination
        ) throws FileCopierException
        {
            testResult = copyFileStream(context, input, node);

            return destination;
        }

        File testFile;

        public String copyFile(ExecutionContext context, File file, INodeEntry node) throws FileCopierException {
            testContext = context;
            testNode = node;
            testFile = file;
            if (throwException) {
                throw new FileCopierException("copyFile test", TestReason.Test);
            }
            return testResult;
        }

        @Override
        public String copyFile(
                final ExecutionContext context,
                final File file,
                final INodeEntry node,
                final String destination
        )
                throws FileCopierException
        {
            testResult = copyFile(context, file, node);
            return destination;
        }

        String testScript;

        public String copyScriptContent(ExecutionContext context, String script, INodeEntry node) throws
            FileCopierException {
            testContext = context;
            testNode = node;
            testScript = script;

            if (throwException) {
                throw new FileCopierException("copyScriptContent test", TestReason.Test);
            }
            return testResult;
        }

        @Override
        public String copyScriptContent(
                final ExecutionContext context, final String script, final INodeEntry node, final String destination
        ) throws FileCopierException
        {
            testResult = copyScriptContent(context, script, node);
            return destination;
        }
    }

    public static class multiTestNodeExecutor implements NodeExecutor {
        List<ExecutionContext> testContext = new ArrayList<ExecutionContext>();
        List<String[]> testCommand = new ArrayList<String[]>();
        List<INodeEntry> testNode = new ArrayList<INodeEntry>();
        List<NodeExecutorResult> testResult = new ArrayList<NodeExecutorResult>();
        int index = 0;

        public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) {
            this.testContext.add(context);
            this.testCommand.add(command);
            this.testNode.add(node);
            assertTrue(
                    "expected " + testResult.size() + " commands, but was called " + (index + 1) + " times. command: " +
                    Arrays.asList(command) + " node: " + node.getNodename(),
                    index + 1 <= testResult.size()
            );
            return testResult.get(index++);
        }

    }

    static class test1 implements URLFileUpdater.httpClientInteraction {
        int httpResultCode = 0;
        private String httpStatusText;
        InputStream bodyStream;
        HttpMethod method;
        HttpClient client;
        IOException toThrowExecute;
        IOException toThrowResponseBody;
        boolean releaseConnectionCalled;
        Boolean followRedirects;
        HashMap<String, String> requestHeaders = new HashMap<String, String>();
        HashMap<String, Header> responseHeaders = new HashMap<String, Header>();

        public void setMethod(HttpMethod method) {
            this.method = method;
        }

        public void setClient(HttpClient client) {
            this.client = client;
        }

        public int executeMethod() throws IOException {
            return httpResultCode;
        }

        public String getStatusText() {
            return httpStatusText;
        }

        public InputStream getResponseBodyAsStream() throws IOException {
            return bodyStream;
        }

        public void releaseConnection() {
            releaseConnectionCalled = true;
        }

        public void setRequestHeader(String name, String value) {
            requestHeaders.put(name, value);
        }

        public Header getResponseHeader(String name) {
            return responseHeaders.get(name);
        }


        public void setFollowRedirects(boolean follow) {
            followRedirects = follow;
        }
    }

    /**
     * Use script file specifier in execution item
     */
    public void testInterpretCommandScriptFileLocal() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptURLNodeStepExecutor interpret = new ScriptURLNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        final StepExecutionContext context = ExecutionContextImpl.builder()
            .frameworkProject(PROJ_NAME)
            .framework(frameworkInstance)
            .user("blah")
            .build();
        final String urlString = "http://test.com";

        ScriptURLCommandBase command = new ScriptURLCommandBase() {
            public String getURLString() {
                return urlString;
            }

            public String[] getArgs() {
                return new String[0];
            }

            public StepExecutionItem getFailureHandler() {
                return null;
            }

            public boolean isKeepgoingOnSuccess() {
                return false;
            }

            public String getScriptInterpreter() {
                return null;
            }

            @Override
            public String getFileExtension() {
                return null;
            }

            public boolean getInterpreterArgsQuoted() {
                return false;
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final test1 interaction = new TestScriptURLNodeStepExecutor.test1();

            interaction.httpResultCode = 200;
            interaction.httpStatusText = "OK";
            interaction.responseHeaders.put("Content-Type", new Header("Content-Type", "text/plain"));
            String testcontent = "test script content";
            ByteArrayInputStream stringStream = new ByteArrayInputStream(testcontent.getBytes());
            interaction.bodyStream = stringStream;

            interpret.setInteraction(interaction);

            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNull(testcopier.testInput);
            assertNotNull(testcopier.testFile.getAbsolutePath());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String filepath = strings[2];
            assertTrue(filepath.endsWith(".sh"));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(strings2[0], filepath);
            assertEquals(filepath, strings2[0]);
            assertEquals(test1, testexec.testNode.get(1));

            //third call is to remove remote file
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
            assertEquals(test1, testexec.testNode.get(2));
        }
    }
    /**
     * Use file extension
     */
    public void testExecuteNodeStep_fileExtension() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptURLNodeStepExecutor interpret = new ScriptURLNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        final StepExecutionContext context = ExecutionContextImpl.builder()
            .frameworkProject(PROJ_NAME)
            .framework(frameworkInstance)
            .user("blah")
            .build();
        final String urlString = "http://test.com";
        final String fileExtension = "myext";

        ScriptURLCommandBase command = new ScriptURLCommandBase() {
            public String getURLString() {
                return urlString;
            }

            public String[] getArgs() {
                return new String[0];
            }

            public StepExecutionItem getFailureHandler() {
                return null;
            }

            public boolean isKeepgoingOnSuccess() {
                return false;
            }

            public String getScriptInterpreter() {
                return null;
            }

            @Override
            public String getFileExtension() {
                return fileExtension;
            }

            public boolean getInterpreterArgsQuoted() {
                return false;
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final test1 interaction = new TestScriptURLNodeStepExecutor.test1();

            interaction.httpResultCode = 200;
            interaction.httpStatusText = "OK";
            interaction.responseHeaders.put("Content-Type", new Header("Content-Type", "text/plain"));
            String testcontent = "test script content";
            ByteArrayInputStream stringStream = new ByteArrayInputStream(testcontent.getBytes());
            interaction.bodyStream = stringStream;

            interpret.setInteraction(interaction);

            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNull(testcopier.testInput);
            assertNotNull(testcopier.testFile.getAbsolutePath());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            final String filepath=strings[2];
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            assertTrue("file extension not correct", filepath.endsWith("." + fileExtension));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(1, strings2.length);
            assertEquals(strings2[0], strings[2]);
            assertEquals(filepath, strings2[0]);
            assertEquals(test1, testexec.testNode.get(1));

            //third call is to remove remote file
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
            assertEquals(test1, testexec.testNode.get(2));
        }
    }
    /**
     * Use invocation string
     */
    public void testExecuteNodeStep_invocation() throws Exception {
        final Framework frameworkInstance = getFrameworkInstance();
        ScriptURLNodeStepExecutor interpret = new ScriptURLNodeStepExecutor(frameworkInstance);

        //setup nodeexecutor for local node
        multiTestNodeExecutor testexec = new multiTestNodeExecutor();
        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(getFrameworkInstance());
        service.registerInstance("local", testexec);

        testFileCopier testcopier = new testFileCopier();
        FileCopierService copyservice = FileCopierService.getInstanceForFramework(getFrameworkInstance());
        copyservice.registerInstance("local", testcopier);

        //execute command interpreter on local node
        final NodeEntryImpl test1 = new NodeEntryImpl("testhost1", "test1");
        test1.setOsFamily("unix");

        final StepExecutionContext context = ExecutionContextImpl.builder()
            .frameworkProject(PROJ_NAME)
            .framework(frameworkInstance)
            .user("blah")
            .build();
        final String urlString = "http://test.com";
        final String invocation = "mycommand ${scriptfile}";

        ScriptURLCommandBase command = new ScriptURLCommandBase() {
            public String getURLString() {
                return urlString;
            }

            public String[] getArgs() {
                return new String[0];
            }

            public StepExecutionItem getFailureHandler() {
                return null;
            }

            public boolean isKeepgoingOnSuccess() {
                return false;
            }

            public String getScriptInterpreter() {
                return invocation;
            }

            @Override
            public String getFileExtension() {
                return null;
            }

            public boolean getInterpreterArgsQuoted() {
                return false;
            }
        };
        {
            final ArrayList<NodeExecutorResult> nodeExecutorResults = new ArrayList<NodeExecutorResult>();
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            nodeExecutorResults.add(NodeExecutorResultImpl.createSuccess(null));
            testexec.testResult = nodeExecutorResults;
            testcopier.testResult = "/test/file/path";
            final test1 interaction = new TestScriptURLNodeStepExecutor.test1();

            interaction.httpResultCode = 200;
            interaction.httpStatusText = "OK";
            interaction.responseHeaders.put("Content-Type", new Header("Content-Type", "text/plain"));
            String testcontent = "test script content";
            ByteArrayInputStream stringStream = new ByteArrayInputStream(testcontent.getBytes());
            interaction.bodyStream = stringStream;

            interpret.setInteraction(interaction);

            final NodeStepResult interpreterResult = interpret.executeNodeStep(context, command, test1);

            assertNotNull(interpreterResult);
            assertTrue(interpreterResult.isSuccess());
            assertEquals(interpreterResult, nodeExecutorResults.get(1));

            assertEquals(context, testcopier.testContext);
            assertNull(testcopier.testScript);
            assertNull(testcopier.testInput);
            assertNotNull(testcopier.testFile.getAbsolutePath());
            assertEquals(test1, testcopier.testNode);

            //test nodeexecutor was called twice
            assertEquals(3, testexec.index);
            //first call is chmod +x filepath
            final String[] strings = testexec.testCommand.get(0);
            final String filepath=strings[2];
            assertEquals(3, strings.length);
            assertEquals("chmod", strings[0]);
            assertEquals("+x", strings[1]);
            String UNIX_EXT = "sh";
            assertTrue("file extension not correct", filepath.endsWith("." + UNIX_EXT));
            assertEquals(test1, testexec.testNode.get(0));

            //second call is to exec the filepath
            final String[] strings2 = testexec.testCommand.get(1);
            assertEquals(2, strings2.length);
            assertEquals("mycommand",strings2[0]);
            assertEquals(filepath, strings2[1]);
            assertEquals(test1, testexec.testNode.get(1));

            //third call is to remove remote file
            final String[] strings3 = testexec.testCommand.get(2);
            assertEquals(3, strings3.length);
            assertEquals("rm", strings3[0]);
            assertEquals("-f", strings3[1]);
            assertEquals(filepath, strings3[2]);
            assertEquals(test1, testexec.testNode.get(2));
        }
    }

    public void testExpandUrlString() throws Exception {
        final Map<String, Map<String, String>> stringMapMap = new HashMap<String, Map<String, String>>();
        HashMap<String, String> nodeData = new HashMap<String, String>();
        nodeData.put("name", "node/name");

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("value", "some value ? for things & stuff");

        t:{//no expansion
            String value = null;
            try {
                value = ScriptURLNodeStepExecutor.expandUrlString(
                    "http://example.com/path/${node.name}?query=${data.value}",
                    stringMapMap);
                fail("should not succeed");
            } catch (DataContextUtils.UnresolvedDataReferenceException e) {
                assertEquals("${node.name}", e.getReferenceName());
            }
        }
        t:
        {//path expansion
            stringMapMap.put("node", nodeData);
            String value = null;
            try {
                value = ScriptURLNodeStepExecutor.expandUrlString(
                    "http://example.com/path/${node.name}?query=${data.value}",
                    stringMapMap);
                fail("should not succeed");
            } catch (DataContextUtils.UnresolvedDataReferenceException e) {
                assertEquals("${data.value}", e.getReferenceName());
            }
        }
        t:
        {//dataexpansion
            stringMapMap.put("node", nodeData);
            stringMapMap.put("data", data);
            String value = ScriptURLNodeStepExecutor.expandUrlString(
                "http://example.com/path/${node.name}?query=${data.value}",
                stringMapMap);
            assertEquals("http://example.com/path/node%2Fname?query=some%20value%20%3F%20for%20things%20%26%20stuff", value);
        }
    }
}
