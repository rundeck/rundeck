package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.workflow.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.*;
import com.dtolabs.rundeck.core.plugins.configuration.*;
import com.dtolabs.rundeck.core.tools.AbstractBaseTest;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import org.apache.tools.ant.BuildListener;

import java.io.File;
import java.io.InputStream;
import java.util.*;

import static com.dtolabs.rundeck.core.execution.workflow.TestBaseWorkflowStrategy.TEST_PROJECT;

public class StepPluginAdapterTest extends AbstractBaseTest {

    Framework testFramework;

    public StepPluginAdapterTest(java.lang.String name) {
        super(name);
    }

    static enum TestFailureReason implements FailureReason {
        TestFailure
    }


    public void setUp(){
        testFramework = createTestFramework();
    }

    static class testListener implements ExecutionListenerOverride {
        NodeRecorder testFailedNodesListener;

        public NodeRecorder getNodeRecorder(){
            return this.testFailedNodesListener;
        }

        @Override public void ignoreErrors(boolean ignore){}


        public void log(int i, java.lang.String s) {
            System.err.println(i + ": " + s);
        }

        @Override
        public void log(final int level, final java.lang.String message, final Map eventMeta) {
            System.err.println(level + ": " + message);
        }

        @Override
        public void event(java.lang.String eventType, java.lang.String message, Map eventMeta) {
            System.err.println(eventType + ": " + message);
        }

        public FailedNodesListener getFailedNodesListener() {
            return this.testFailedNodesListener != null ? this.testFailedNodesListener : new NodeRecorder();
        }

        public void beginNodeExecution(ExecutionContext context, java.lang.String[] command, INodeEntry node) {
        }

        public void finishNodeExecution(NodeExecutorResult result, ExecutionContext context, java.lang.String[] command,
                                        INodeEntry node) {
        }

        public void beginNodeDispatch(ExecutionContext context, StepExecutionItem item) {
        }

        public void beginNodeDispatch(ExecutionContext context, Dispatchable item) {
        }

        public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, StepExecutionItem item) {
        }

        public void finishNodeDispatch(DispatcherResult result, ExecutionContext context, Dispatchable item) {
        }

        public void beginFileCopyFileStream(ExecutionContext context, InputStream input, INodeEntry node) {
        }

        public void beginFileCopyFile(ExecutionContext context, File input, INodeEntry node) {
        }

        public void beginFileCopyFile(ExecutionContext context, List<File> input, INodeEntry node) {
        }

        public void beginFileCopyScriptContent(ExecutionContext context, java.lang.String input, INodeEntry node) {
        }

        public void finishFileCopy(java.lang.String result, ExecutionContext context, INodeEntry node) {
        }

        public void finishMultiFileCopy(java.lang.String[] result, ExecutionContext context, INodeEntry node) {
        }

        public BuildListener getBuildListener() {
            return null;
        }

        public ExecutionListenerOverride createOverride() {
            return this;
        }

        public void setFailedNodesListener(FailedNodesListener listener) {
            testFailedNodesListener = new NodeRecorder();
        }
    }


    private StepExecutionContext createStepExecutionContext() throws NodeFileParserException {
        HashSet<String> failedNodeList = new HashSet<>();
        final NodeSetImpl set1 = new NodeSetImpl();
        failedNodeList.add("node-1");
        NodeEntryImpl node1 = new NodeEntryImpl("nodea.host", "node-1");

        set1.putNode(node1);

        return  new ExecutionContextImpl.Builder()
                .frameworkProject(TEST_PROJECT)
                .user("user1")
                .nodeSelector(SelectorUtils.nodeList(failedNodeList))
                .executionListener(new testListener())
                .framework(testFramework)
                .nodes(set1)
                .stepNumber(1)
                .build();
    }

    public void testFailedNodes() throws StepException, NodeFileParserException {
        // Step Plugin Adapter Instance with a Test plugin
        StepPluginAdapter stepPluginAdapter = new StepPluginAdapter(new TestPlugin());
        // Step Plugin Adapter Execution Context
        StepExecutionContext context = createStepExecutionContext();
        // Execution Context
        StepExecutionItem item = new StepExecutionItem() {
            @Override
            public java.lang.String getType() {
                return null;
            }

            @Override
            public java.lang.String getLabel() {
                return null;
            }
        };
        // Execution from the adapter with a Test Plugin
        StepExecutionResult result = stepPluginAdapter.executeWorkflowStep(context, item);
        assertTrue(stepPluginAdapter.getNodesWithFailuresInPluginStep().size() > 0);
        assertNotNull(result);

    }

    static class TestPlugin implements StepPlugin, Describable{

        Map<java.lang.String, java.lang.String> propMap = new HashMap<java.lang.String, java.lang.String>(){{
            put("A prop", "A value");
        }};

        @Override
        public Description getDescription() {
            Description desc = getPluginDescriptionForTests(
                    "A plugin",
                    "A title",
                    "A Desc",
                    new ArrayList<Property>(Arrays.asList(getPropertyForTest())),
                    propMap,
                    null
            );
            return desc;
        };

        @Override
        public void executeStep(PluginStepContext context, Map<java.lang.String, Object> configuration) throws StepException {
            throw new StepException("A message", TestFailureReason.TestFailure);
        }
    }

    static Description getPluginDescriptionForTests(
            java.lang.String name,
            java.lang.String title,
            java.lang.String desc,
            List<Property> properties,
            Map<java.lang.String, java.lang.String> propMapping,
            Map<java.lang.String, java.lang.String> frameworkMapping
            ){
        Description descTest = new Description() {
            @Override
            public java.lang.String getName() {
                return name;
            }

            @Override
            public java.lang.String getTitle() {
                return title;
            }

            @Override
            public java.lang.String getDescription() {
                return desc;
            }

            @Override
            public List<Property> getProperties() {
                return properties;
            }

            @Override
            public Map<java.lang.String, java.lang.String> getPropertiesMapping() {
                return propMapping;
            }

            @Override
            public Map<java.lang.String, java.lang.String> getFwkPropertiesMapping() {
                return frameworkMapping;
            }
        };
        return descTest;
    };

    static Property getPropertyForTest(){
        Property property = new Property() {
            @Override
            public java.lang.String getTitle() {
                return "A prop";
            }

            @Override
            public java.lang.String getName() {
                return "A name";
            }

            @Override
            public java.lang.String getDescription() {
                return "A desc";
            }

            @Override
            public Type getType() {
                return Type.String;
            }

            @Override
            public PropertyValidator getValidator() {
                return null;
            }

            @Override
            public boolean isRequired() {
                return false;
            }

            @Override
            public java.lang.String getDefaultValue() {
                return null;
            }

            @Override
            public List<java.lang.String> getSelectValues() {
                return null;
            }

            @Override
            public Map<java.lang.String, java.lang.String> getSelectLabels() {
                return null;
            }

            @Override
            public PropertyScope getScope() {
                return null;
            }

            @Override
            public Map<java.lang.String, Object> getRenderingOptions() {
                return null;
            }
        };
        return property;
    }

}
