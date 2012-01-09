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
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodesSelector;
import com.dtolabs.rundeck.core.common.SelectorUtils;

import java.io.File;
import java.util.*;

/**
 * ExecutionContextImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionContextImpl implements ExecutionContext {
    private String frameworkProject;
    private String user;
    private NodesSelector nodeSet;
    private int threadCount;
    private boolean keepgoing;
    private String[] args;
    private int loglevel;
    private Map<String, Map<String, String>> dataContext;
    private Map<String, Map<String, String>> privateDataContext;
    private ExecutionListener executionListener;
    private Framework framework;
    private File nodesFile;
    private String nodeRankAttribute;
    private boolean nodeRankOrderAscending=true;

    private ExecutionContextImpl(final Builder builder) {
        this.frameworkProject = builder.frameworkProject;
        this.user = builder.user;
        this.nodeSet = builder.nodeSet;
        this.args = builder.args;
        this.loglevel = builder.loglevel;
        this.dataContext = builder.dataContext;
        this.privateDataContext = builder.privateDataContext;
        this.executionListener = builder.executionListener;
        this.framework = builder.framework;
        this.nodesFile = builder.nodesFile;
        this.threadCount = builder.threadCount;
        this.keepgoing = builder.keepgoing;
        this.nodeRankAttribute = builder.nodeRankAttribute;
        this.nodeRankOrderAscending = builder.nodeRankOrderAscending;
    }

    public static class Builder {
        private String frameworkProject;
        private String user;
        private NodesSelector nodeSet;
        private String[] args;
        private int loglevel;
        private Map<String, Map<String, String>> dataContext;
        private Map<String, Map<String, String>> privateDataContext;
        private ExecutionListener executionListener;
        private Framework framework;
        private File nodesFile;
        private int threadCount;
        private boolean keepgoing;
        private String nodeRankAttribute;
        private boolean nodeRankOrderAscending=true;

        public Builder() {
        }

        public Builder(final ExecutionContext original) {
            this.frameworkProject = original.getFrameworkProject();
            this.user = original.getUser();
            this.nodeSet = original.getNodeSelector();
            this.args = original.getArgs();
            this.loglevel = original.getLoglevel();
            this.dataContext = original.getDataContext();
            this.privateDataContext = original.getPrivateDataContext();
            this.executionListener = original.getExecutionListener();
            this.framework = original.getFramework();
            this.nodesFile = original.getNodesFile();
            this.threadCount = original.getThreadCount();
            this.keepgoing = original.isKeepgoing();
            this.nodeRankAttribute = original.getNodeRankAttribute();
            this.nodeRankOrderAscending = original.isNodeRankOrderAscending();

        }

        public Builder frameworkProject(String frameworkProject) {
            this.frameworkProject = frameworkProject;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder nodeSelector(NodesSelector nodeSet) {
            this.nodeSet = nodeSet;
            return this;
        }

        public Builder args(String[] args) {
            this.args = args;
            return this;
        }

        public Builder loglevel(int loglevel) {
            this.loglevel = loglevel;
            return this;
        }

        public Builder dataContext(Map<String, Map<String, String>> dataContext) {
            this.dataContext = dataContext;
            return this;
        }

        public Builder privateDataContext(Map<String, Map<String, String>> privateDataContext) {
            this.privateDataContext = privateDataContext;
            return this;
        }

        public Builder executionListener(ExecutionListener executionListener) {
            this.executionListener = executionListener;
            return this;
        }

        public Builder framework(Framework framework) {
            this.framework = framework;
            return this;
        }

        public Builder nodesFile(File nodesFile) {
            this.nodesFile = nodesFile;
            return this;
        }

        public Builder threadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        public Builder keepgoing(boolean keepgoing) {
            this.keepgoing = keepgoing;
            return this;
        }

        public Builder nodeRankAttribute(final String nodeRankAttribute) {
            this.nodeRankAttribute = nodeRankAttribute;
            return this;
        }

        public Builder nodeRankOrderAscending(boolean nodeRankOrderAscending) {
            this.nodeRankOrderAscending = nodeRankOrderAscending;
            return this;
        }

        public ExecutionContextImpl build() {
            return new ExecutionContextImpl(this);
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

    public String[] getArgs() {
        return null != args ? args.clone() : null;
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
}
