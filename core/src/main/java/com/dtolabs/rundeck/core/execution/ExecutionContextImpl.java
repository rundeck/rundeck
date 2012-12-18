/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
* ExecutionContextImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/23/11 1:47 PM
* 
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.common.NodesSelector;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;

import java.io.File;
import java.util.*;


/**
 * ExecutionContextImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionContextImpl implements ExecutionContext, StepExecutionContext {
    private String frameworkProject;
    private String user;
    private NodesSelector nodeSet;
    private INodeSet nodes;
    private int threadCount;
    private boolean keepgoing;
    private int loglevel;
    private Map<String, Map<String, String>> dataContext;
    private Map<String, Map<String, String>> privateDataContext;
    private ExecutionListener executionListener;
    private Framework framework;
    private File nodesFile;
    private String nodeRankAttribute;
    private boolean nodeRankOrderAscending = true;
    private int stepNumber = 1;
    private List<Integer> stepContext;

    private ExecutionContextImpl() {
        stepContext = new ArrayList<Integer>();
        nodes = new NodeSetImpl();
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
                ctx.dataContext = original.getDataContext();
                ctx.privateDataContext = original.getPrivateDataContext();
                ctx.executionListener = original.getExecutionListener();
                ctx.framework = original.getFramework();
                ctx.nodesFile = original.getNodesFile();
                ctx.threadCount = original.getThreadCount();
                ctx.keepgoing = original.isKeepgoing();
                ctx.nodeRankAttribute = original.getNodeRankAttribute();
                ctx.nodeRankOrderAscending = original.isNodeRankOrderAscending();
            }
        }

        public Builder(final StepExecutionContext original) {
            this((ExecutionContext) original);
            if (null != original) {
                ctx.stepNumber = original.getStepNumber();
                ctx.stepContext = original.getStepContext();
            }
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

        public Builder loglevel(int loglevel) {
            ctx.loglevel = loglevel;
            return this;
        }

        public Builder dataContext(Map<String, Map<String, String>> dataContext) {
            ctx.dataContext = dataContext;
            return this;
        }

        public Builder privateDataContext(Map<String, Map<String, String>> privateDataContext) {
            ctx.privateDataContext = privateDataContext;
            return this;
        }

        public Builder executionListener(ExecutionListener executionListener) {
            ctx.executionListener = executionListener;
            return this;
        }

        public Builder framework(Framework framework) {
            ctx.framework = framework;
            return this;
        }

        public Builder nodesFile(File nodesFile) {
            ctx.nodesFile = nodesFile;
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
        public Builder pushContextStep(final int step) {
            ctx.stepContext.add(ctx.stepNumber);
            ctx.stepNumber = step;
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

    public ExecutionListener getExecutionListener() {
        return executionListener;
    }

    public Framework getFramework() {
        return framework;
    }

    public File getNodesFile() {
        return nodesFile;
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
}
