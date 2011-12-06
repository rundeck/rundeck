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

    private ExecutionContextImpl(final String frameworkProject, final String user, final NodesSelector nodeSet,
                                 final String[] args, final int loglevel,
                                 final Map<String, Map<String, String>> dataContext,
                                 final ExecutionListener executionListener,
                                 final Framework framework, final File nodesFile, final int threadCount, final boolean keepgoing) {
        this.frameworkProject = frameworkProject;
        this.user = user;
        this.nodeSet = nodeSet;
        this.args = args;
        this.loglevel = loglevel;
        this.dataContext = dataContext;
        this.executionListener = executionListener;
        this.framework = framework;
        this.nodesFile = nodesFile;
        this.threadCount=threadCount;
        this.keepgoing=keepgoing;
    }

    private ExecutionContextImpl(final String frameworkProject, final String user, final NodesSelector nodeSet,
                                 final String[] args, final int loglevel,
                                 final Map<String, Map<String, String>> dataContext,
                                 final Map<String, Map<String, String>> privateDataContext,
                                 final ExecutionListener executionListener,
                                 final Framework framework, final File nodesFile, final int threadCount, final boolean keepgoing) {
        this.frameworkProject = frameworkProject;
        this.user = user;
        this.nodeSet = nodeSet;
        this.args = args;
        this.loglevel = loglevel;
        this.dataContext = dataContext;
        this.privateDataContext = privateDataContext;
        this.executionListener = executionListener;
        this.framework = framework;
        this.nodesFile = nodesFile;
        this.threadCount=threadCount;
        this.keepgoing=keepgoing;
    }

    /**
     * Create a new ExecutionContext with the specified values
     */
    public static ExecutionContextImpl createExecutionContextImpl(final String frameworkProject, final String user,
                                                                  final NodesSelector nodeSet,
                                                                  final String[] args, final int loglevel,
                                                                  final Map<String, Map<String, String>> dataContext,
                                                                  final ExecutionListener executionListener,
                                                                  final Framework framework) {
        return createExecutionContextImpl(frameworkProject, user, nodeSet, args, loglevel, dataContext,
            executionListener, framework, null,1,false);
    }
    /**
     * Create a new ExecutionContext with the specified values
     */
    public static ExecutionContextImpl createExecutionContextImpl(final String frameworkProject, final String user,
                                                                  final NodesSelector nodeSet,
                                                                  final String[] args, final int loglevel,
                                                                  final Map<String, Map<String, String>> dataContext,
                                                                  final Map<String, Map<String, String>> privateDataContext,
                                                                  final ExecutionListener executionListener,
                                                                  final Framework framework, final int threadCount,
                                                                  final boolean keepgoing) {
        return createExecutionContextImpl(frameworkProject, user, nodeSet, args, loglevel, dataContext, privateDataContext,
            executionListener, framework, null,threadCount,keepgoing);
    }
    /**
     * Create a new ExecutionContext with the specified values
     */
    public static ExecutionContextImpl createExecutionContextImpl(final String frameworkProject, final String user,
                                                                  final NodesSelector nodeSet,
                                                                  final String[] args, final int loglevel,
                                                                  final Map<String, Map<String, String>> dataContext,
                                                                  final ExecutionListener executionListener,
                                                                  final Framework framework, final int threadCount,
                                                                  final boolean keepgoing) {
        return createExecutionContextImpl(frameworkProject, user, nodeSet, args, loglevel, dataContext,
            executionListener, framework, null,threadCount,keepgoing);
    }

    /**
     * Create a new ExecutionContext with the specified values
     */
    public static ExecutionContextImpl createExecutionContextImpl(final String frameworkProject, final String user,
                                                                  final NodesSelector nodeSet,
                                                                  final String[] args, final int loglevel,
                                                                  final Map<String, Map<String, String>> dataContext,
                                                                  final ExecutionListener executionListener,
                                                                  final Framework framework, final File nodesFile,
                                                                  final int threadCount, final boolean keepgoing) {
        return new ExecutionContextImpl(frameworkProject, user, nodeSet, args, loglevel, dataContext,
            executionListener, framework, nodesFile,threadCount,keepgoing);
    }
    /**
     * Create a new ExecutionContext with the specified values
     */
    public static ExecutionContextImpl createExecutionContextImpl(final String frameworkProject, final String user,
                                                                  final NodesSelector nodeSet,
                                                                  final String[] args, final int loglevel,
                                                                  final Map<String, Map<String, String>> dataContext,
                                                                  final Map<String, Map<String, String>> privateDataContext,
                                                                  final ExecutionListener executionListener,
                                                                  final Framework framework, final File nodesFile,
                                                                  final int threadCount, final boolean keepgoing) {
        return new ExecutionContextImpl(frameworkProject, user, nodeSet, args, loglevel, dataContext, privateDataContext,
            executionListener, framework, nodesFile,threadCount,keepgoing);
    }


    /**
     * Create a new ExecutionContext with a single node nodeset value, and all other values from the input context
     */
    public static ExecutionContextImpl createExecutionContextImpl(final ExecutionContext context,
                                                                  final INodeEntry singleNode) {
        return new ExecutionContextImpl(context.getFrameworkProject(), context.getUser(), SelectorUtils.singleNode(
            singleNode.getNodename()),
            context.getArgs(), context.getLoglevel(), context.getDataContext(), context.getPrivateDataContext(),
            context.getExecutionListener(), context.getFramework(), context.getNodesFile(), context.getThreadCount(),
            context.isKeepgoing());
    }

    /**
     * Create a new ExecutionContext from an original substituting a specific datacontext.
     */
    public static ExecutionContextImpl createExecutionContextImpl(final ExecutionContext context,
                                                                  final Map<String, Map<String, String>> dataContext) {
        return new ExecutionContextImpl(context.getFrameworkProject(), context.getUser(), context.getNodeSelector(),
            context.getArgs(), context.getLoglevel(), dataContext, context.getPrivateDataContext(),
            context.getExecutionListener(),
            context.getFramework(), context.getNodesFile(), context.getThreadCount(), context.isKeepgoing());
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

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public boolean isKeepgoing() {
        return keepgoing;
    }

    public void setKeepgoing(boolean keepgoing) {
        this.keepgoing = keepgoing;
    }

    public Map<String, Map<String, String>> getPrivateDataContext() {
        return privateDataContext;
    }
}
