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
* ExecutionContextImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/23/11 1:47 PM
* 
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.common.NodesSelector;
import com.dtolabs.rundeck.core.common.OrchestratorConfig;
import com.dtolabs.rundeck.core.common.SelectorUtils;
import com.dtolabs.rundeck.core.data.*;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.workflow.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeExecutionContext;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.core.logging.LoggingManager;
import com.dtolabs.rundeck.core.nodes.ProjectNodeService;
import com.dtolabs.rundeck.core.storage.StorageTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * ExecutionContextImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionContextImpl implements ExecutionContext, StepExecutionContext, NodeExecutionContext {
    private String frameworkProject;
    private String user;
    private NodesSelector nodeSet;
    private INodeSet nodes;
    private INodeEntry singleNodeContext;
    private int threadCount;
    private boolean keepgoing;
    private int loglevel;
    private String charsetEncoding;
    private DataContext dataContext;
    private DataContext privateDataContext;
    private MultiDataContext<ContextView, DataContext> sharedDataContext;
    private ExecutionListener executionListener;
    private WorkflowExecutionListener workflowExecutionListener;
    private ExecutionLogger executionLogger;
    private Framework framework;
    private AuthContext authContext;
    private String nodeRankAttribute;
    private boolean nodeRankOrderAscending = true;
    private int stepNumber = 1;
    private List<Integer> stepContext;
    private StorageTree storageTree;
    private JobService jobService;
    private ProjectNodeService nodeService;
    private FlowControl flowControl;
    private SharedOutputContext outputContext;
    private LoggingManager loggingManager;

    private OrchestratorConfig orchestrator;
    private ExecutionContextImpl() {
        stepContext = new ArrayList<>();
        nodes = new NodeSetImpl();
        dataContext = new BaseDataContext();
        privateDataContext = new BaseDataContext();
        sharedDataContext = new MultiDataContextImpl<>();
        outputContext = SharedDataContextUtils.outputContext(ContextView.global());
    }

    public static Builder builder() {
        return new Builder();
    }
    public static Builder builder(ExecutionContext context) {
        return new Builder(context);
    }
    public static Builder builder(StepExecutionContext context) {
        return new Builder(context);
    }

    @Override
    public MultiDataContext<ContextView, DataContext> getSharedDataContext() {
        return sharedDataContext;
    }

    public AuthContext getAuthContext() {
        return authContext;
    }

    public void setAuthContext(AuthContext authContext) {
        this.authContext = authContext;
    }

    public StorageTree getStorageTree() {
        return storageTree;
    }

    @Override
    public FlowControl getFlowControl() {
        return flowControl;
    }

    @Override
    public String getCharsetEncoding() {
        return charsetEncoding;
    }

    public void setCharsetEncoding(String charsetEncoding) {
        this.charsetEncoding = charsetEncoding;
    }
    public SharedOutputContext getOutputContext() {
        return outputContext;
    }

    public INodeEntry getSingleNodeContext() {
        return singleNodeContext;
    }

    @Override
    public LoggingManager getLoggingManager() {
        return loggingManager;
    }


    public static class Builder {
        private ExecutionContextImpl ctx;


        public Builder() {
            ctx = new ExecutionContextImpl();
        }

        public Builder(final ExecutionContext original) {
            this();
            if(null!=original){
                ctx.frameworkProject = original.getFrameworkProject();
                ctx.user = original.getUser();
                ctx.nodeSet = original.getNodeSelector();
                ctx.nodes = original.getNodes();
                ctx.loglevel = original.getLoglevel();
                ctx.charsetEncoding = original.getCharsetEncoding();
                ctx.dataContext = original.getDataContextObject();
                ctx.privateDataContext = original.getPrivateDataContextObject();
                ctx.executionListener = original.getExecutionListener();
                ctx.workflowExecutionListener = original.getWorkflowExecutionListener();
                ctx.executionLogger = original.getExecutionLogger();
                ctx.framework = original.getFramework();
                ctx.authContext = original.getAuthContext();
                ctx.threadCount = original.getThreadCount();
                ctx.keepgoing = original.isKeepgoing();
                ctx.nodeRankAttribute = original.getNodeRankAttribute();
                ctx.nodeRankOrderAscending = original.isNodeRankOrderAscending();
                ctx.storageTree = original.getStorageTree();
                ctx.jobService = original.getJobService();
                ctx.nodeService = original.getNodeService();
                ctx.orchestrator = original.getOrchestrator();
                ctx.outputContext = original.getOutputContext();
                ctx.sharedDataContext = MultiDataContextImpl.with(original.getSharedDataContext());
                ctx.loggingManager = original.getLoggingManager();
                if(original instanceof NodeExecutionContext){
                    NodeExecutionContext original1 = (NodeExecutionContext) original;
                    ctx.singleNodeContext = original1.getSingleNodeContext();
                }
            }
        }

        public Builder loggingManager(LoggingManager loggingManager) {
            ctx.loggingManager = loggingManager;
            return this;
        }

        public Builder storageTree(StorageTree storageTree) {
            ctx.storageTree = storageTree;
            return this;
        }

        public Builder jobService(JobService jobService) {
            ctx.jobService=jobService;
            return this;
        }

        public Builder nodeService(ProjectNodeService nodeService) {
            ctx.nodeService=nodeService;
            return this;
        }

        public Builder(final StepExecutionContext original) {
            this((ExecutionContext) original);
            if (null != original) {
                ctx.stepNumber = original.getStepNumber();
                ctx.stepContext = null != original.getStepContext() ? new ArrayList<>(original.getStepContext()) : null;
                ctx.flowControl = original.getFlowControl();
            }
        }

        public Builder flowControl(FlowControl flowControl) {
            ctx.flowControl = flowControl;
            return this;
        }
        public Builder outputContext(SharedOutputContext outputContext) {
            ctx.outputContext = outputContext;
            return this;
        }

        public Builder frameworkProject(String frameworkProject) {
            ctx.frameworkProject = frameworkProject;
            return this;
        }

        public Builder user(String user) {
            ctx.user = user;
            return this;
        }

        public Builder nodeSelector(NodesSelector nodeSet) {
            ctx.nodeSet = nodeSet;
            return this;
        }

        public Builder nodes(INodeSet nodeSet) {
            ctx.nodes = nodeSet;
            return this;
        }

        /**
         * Set node set/selector to single node context, and optionally merge node-specific context data
         * @param node node
         * @param setContextData context
         * @return builder
         */
        public Builder singleNodeContext(INodeEntry node, boolean setContextData) {
            nodeSelector(SelectorUtils.singleNode(node.getNodename()));
            nodes(NodeSetImpl.singleNodeSet(node));
            if(setContextData) {
                //merge in any node-specific data context
                nodeContextData(node);

                ctx.singleNodeContext=node;

//                if (null != ctx.sharedDataContext && null != ctx.sharedDataContext.getData(node.getNodename())) {
                //XXX: merge shared node data at this point?
//                    ctx.dataContext.merge(ctx.sharedDataContext.getData(node.getNodename()));
//                }
            }
            return this;
        }

        public Builder nodeContextData(INodeEntry node) {
            ctx.dataContext.merge(new BaseDataContext("node", DataContextUtils.nodeData(node)));
            ctx.sharedDataContext.merge(
                    ContextView.node(node.getNodename()),
                    new BaseDataContext("node", DataContextUtils.nodeData(node))
            );
            return this;
        }

        /**
         * Add/replace a context data set
         * @param key key
         * @param data data
         * @return builder
         */
        public Builder setContext(final String key, final Map<String,String> data) {
            return dataContext(DataContextUtils.addContext(key, data, ctx.dataContext));
        }

        /**
         * Merge a context data set
         * @param data data
         * @return builder
         */
        public Builder mergeContext(final Map<String,Map<String,String>> data) {
            return mergeContext(new BaseDataContext(data));
        }

        /**
         * Merge a context data set
         *
         * @param data data
         *
         * @return builder
         */
        public Builder mergeContext(final DataContext data) {
            ctx.dataContext.merge(data);
            ctx.sharedDataContext.merge(ContextView.global(), data);
            return this;
        }

        /**
         * merge a context data set
         * @param key key
         * @param data data
         * @return builder
         */
        public Builder mergeContext(final String key, final Map<String,String> data) {
            HashMap<String, Map<String, String>> tomerge = new HashMap<>();
            tomerge.put(key, data);
            return mergeContext(tomerge);
        }

        public Builder loglevel(int loglevel) {
            ctx.loglevel = loglevel;
            return this;
        }

        public Builder charsetEncoding(String charsetEncoding) {
            ctx.charsetEncoding = charsetEncoding;
            return this;
        }

        public Builder dataContext(Map<String, Map<String, String>> dataContext) {
            ctx.dataContext = new BaseDataContext(dataContext);
            ctx.sharedDataContext.getData().put(ContextView.global(), new BaseDataContext(dataContext));

            return this;
        }

        public Builder dataContext(DataContext dataContext) {
            ctx.dataContext = new BaseDataContext(dataContext);
            ctx.sharedDataContext.getData().put(ContextView.global(), new BaseDataContext(dataContext));
            return this;
        }

        public Builder privateDataContext(Map<String, Map<String, String>> privateDataContext) {
            ctx.privateDataContext = new BaseDataContext(privateDataContext);
            return this;
        }

        public Builder privateDataContext(DataContext privateDataContext) {
            ctx.privateDataContext = new BaseDataContext(privateDataContext);
            return this;
        }

        public Builder executionListener(ExecutionListener executionListener) {
            ctx.executionListener = executionListener;
            ctx.executionLogger = executionListener;
            return this;
        }
        public Builder workflowExecutionListener(WorkflowExecutionListener workflowExecutionListener) {
            ctx.workflowExecutionListener = workflowExecutionListener;
            return this;
        }

        public Builder executionLogger(ExecutionLogger executionLogger) {
            ctx.executionLogger = executionLogger;
            return this;
        }

        public Builder framework(Framework framework) {
            ctx.framework = framework;
            return this;
        }

        public Builder authContext(AuthContext authContext) {
            ctx.authContext = authContext;
            return this;
        }

        public Builder threadCount(int threadCount) {
            ctx.threadCount = threadCount;
            return this;
        }

        public Builder keepgoing(boolean keepgoing) {
            ctx.keepgoing = keepgoing;
            return this;
        }

        public Builder nodeRankAttribute(final String nodeRankAttribute) {
            ctx.nodeRankAttribute = nodeRankAttribute;
            return this;
        }

        public Builder nodeRankOrderAscending(boolean nodeRankOrderAscending) {
            ctx.nodeRankOrderAscending = nodeRankOrderAscending;
            return this;
        }

        public Builder stepNumber(int number) {
            ctx.stepNumber = number;
            return this;
        }

        public Builder stepContext(List<Integer> stepContext) {
            ctx.stepContext = stepContext;
            return this;
        }
        
        public Builder orchestrator(OrchestratorConfig orchestrator) {
            ctx.orchestrator = orchestrator;
            return this;
        }
        
        public Builder pushContextStep(final int step) {
            ctx.stepContext.add(ctx.stepNumber);
            ctx.stepNumber = step;
            return this;
        }

        public Builder nodeDataContext(final String nodeName, final Map<String,Map<String,String>> dataContext) {
            ctx.sharedDataContext.merge(
                    ContextView.nodeStep(ctx.stepNumber, nodeName),
                    new BaseDataContext(dataContext)
            );
            ctx.sharedDataContext.merge(
                    ContextView.node(nodeName),
                    new BaseDataContext(dataContext)
            );
            return this;
        }

        public Builder sharedDataContext(MultiDataContext<ContextView, DataContext> shared) {
            ctx.sharedDataContext = new MultiDataContextImpl<>(shared);
            if (null != ctx.dataContext) {
                ctx.sharedDataContext.merge(ContextView.global(), ctx.dataContext);
            }
            return this;
        }
        public Builder mergeSharedContext(MultiDataContext<ContextView, DataContext> shared) {
            ctx.sharedDataContext.merge(shared);
            return this;
        }

        public Builder sharedDataContextClear() {
            ctx.sharedDataContext = new MultiDataContextImpl<>();
            return this;
        }

        public ExecutionContextImpl build() {
            return ctx;
        }
    }

    public String getFrameworkProject() {
        return frameworkProject;
    }

    public String getUser() {
        return user;
    }

    public NodesSelector getNodeSelector() {
        return nodeSet;
    }

    public INodeSet getNodes() {
        return nodes;
    }

    public int getLoglevel() {
        return loglevel;
    }

    public Map<String, Map<String, String>> getDataContext() {
        return dataContext;
    }

    @Override
    public DataContext getDataContextObject() {
        return dataContext;
    }

    @Override
    public DataContext getPrivateDataContextObject() {
        return privateDataContext;
    }

    public ExecutionListener getExecutionListener() {
        return executionListener;
    }

    @Override
    public WorkflowExecutionListener getWorkflowExecutionListener() {
        return workflowExecutionListener;
    }

    @Override
    public ExecutionLogger getExecutionLogger() {
        return executionLogger;
    }

    public Framework getFramework() {
        return framework;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public boolean isKeepgoing() {
        return keepgoing;
    }

    public Map<String, Map<String, String>> getPrivateDataContext() {
        return privateDataContext;
    }

    public String getNodeRankAttribute() {
        return nodeRankAttribute;
    }

    public boolean isNodeRankOrderAscending() {
        return nodeRankOrderAscending;
    }

    @Override
    public int getStepNumber() {
        return stepNumber;
    }

    @Override
    public List<Integer> getStepContext() {
        return stepContext;
    }

    @Override
    public JobService getJobService() {
        return jobService;
    }

    @Override
    public ProjectNodeService getNodeService(){
        return nodeService;
    }


    @Override
    public OrchestratorConfig getOrchestrator() {
    	return orchestrator;
    }
}
