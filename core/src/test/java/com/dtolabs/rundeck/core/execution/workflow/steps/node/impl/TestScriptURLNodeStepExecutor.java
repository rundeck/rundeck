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

/*
* TestScriptURLNodeStepExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 5/2/12 6:12 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.data.BaseDataContext;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.core.utils.FileUtils;

import java.io.File;
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

    public static class testFileCopier implements FileCopier {
        String testResult;
        ExecutionContext testContext;
        InputStream testInput;
        INodeEntry testNode;
        boolean throwException;

        @Override
        public String copyFileStream(
                final ExecutionContext context, final InputStream input, final INodeEntry node, final String destination
        ) throws FileCopierException
        {
            testContext = context;
            testNode = node;
            testInput = input;
            if (throwException) {
                throw new FileCopierException("copyFileStream test",TestReason.Test);
            }

            return destination;
        }

        File testFile;

        @Override
        public String copyFile(
                final ExecutionContext context,
                final File file,
                final INodeEntry node,
                final String destination
        )
                throws FileCopierException
        {
            testContext = context;
            testNode = node;
            testFile = file;
            if (throwException) {
                throw new FileCopierException("copyFile test", TestReason.Test);
            }
            return destination;
        }

        String testScript;

        @Override
        public String copyScriptContent(
                final ExecutionContext context, final String script, final INodeEntry node, final String destination
        ) throws FileCopierException
        {
            testContext = context;
            testNode = node;
            testScript = script;

            if (throwException) {
                throw new FileCopierException("copyScriptContent test", TestReason.Test);
            }
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

    public void testExpandUrlString() throws Exception {
        final WFSharedContext stringMapMap = new WFSharedContext();
        HashMap<String, String> nodeData = new HashMap<String, String>();
        nodeData.put("name", "node/name");

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("value", "some value ? for things & stuff");

            String value = null;
            try {
                value = ScriptURLNodeStepExecutor.expandUrlString(
                    "http://example.com/path/${node.name}?query=${data.value}",
                    stringMapMap,
                    "anodename");
                fail("should not succeed");
            } catch (DataContextUtils.UnresolvedDataReferenceException e) {
                assertEquals("${node.name}", e.getReferenceName());
            }
        }

    public void testExpandUrlString2() throws Exception {
        final WFSharedContext stringMapMap = new WFSharedContext();
        HashMap<String, String> nodeData = new HashMap<String, String>();
        nodeData.put("name", "node/name");

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("value", "some value ? for things & stuff");//path expansion
        stringMapMap.merge(ContextView.node("anodename"), new BaseDataContext("node", nodeData));
            String value = null;
            try {
                value = ScriptURLNodeStepExecutor.expandUrlString(
                    "http://example.com/path/${node.name}?query=${data.value}",
                    stringMapMap,
                    "anodename");
                fail("should not succeed");
            } catch (DataContextUtils.UnresolvedDataReferenceException e) {
                assertEquals("${data.value}", e.getReferenceName());
            }
        }

    public void testExpandUrlString3() throws Exception {
        final WFSharedContext stringMapMap = new WFSharedContext();
        HashMap<String, String> nodeData = new HashMap<String, String>();
        nodeData.put("name", "node/name");

        HashMap<String, String> data = new HashMap<String, String>();
        data.put("value", "some value ? for things & stuff");//dataexpansion
        stringMapMap.merge(ContextView.node("anodename"), new BaseDataContext("node", nodeData));
        stringMapMap.merge(ContextView.global(), new BaseDataContext("data", data));
            String value = ScriptURLNodeStepExecutor.expandUrlString(
                "http://example.com/path/${node.name}?query=${data.value}",
                stringMapMap,
                "anodename");
            assertEquals("http://example.com/path/node/name?query=some%20value%20%3F%20for%20things%20%26%20stuff", value);
    }
}
