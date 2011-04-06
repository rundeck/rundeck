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
import com.dtolabs.rundeck.core.utils.NodeSet;

import java.util.*;

/**
 * ExecutionContextImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionContextImpl implements ExecutionContext {
    private String frameworkProject;
    private String user;
    private NodeSet nodeSet;
    private String[] args;
    private int loglevel;
    private Map<String, Map<String, String>> dataContext;
    private ExecutionListener executionListener;
    private Framework framework;

    private ExecutionContextImpl(String frameworkProject, String user, NodeSet nodeSet, String[] args, int loglevel,
                                 Map<String, Map<String, String>> dataContext, ExecutionListener executionListener,
                                 final Framework framework) {
        this.frameworkProject = frameworkProject;
        this.user = user;
        this.nodeSet = nodeSet;
        this.args = args;
        this.loglevel = loglevel;
        this.dataContext = dataContext;
        this.executionListener = executionListener;
        this.framework = framework;
    }

    /**
     * Create a new ExecutionContext with the specified values
     */
    public static ExecutionContextImpl createExecutionContextImpl(String frameworkProject, String user, NodeSet nodeSet,
                                                                  String[] args, int loglevel,
                                                                  Map<String, Map<String, String>> dataContext,
                                                                  ExecutionListener executionListener,
                                                                  final Framework framework) {
        return new ExecutionContextImpl(frameworkProject, user, nodeSet, args, loglevel, dataContext,
            executionListener, framework);
    }

    /**
     * Create a new ExecutionContext with a single node nodeset value, and all other values specified
     */
    public static ExecutionContextImpl createExecutionContextImpl(String frameworkProject, String user,
                                                                  INodeEntry singleNode,
                                                                  String[] args, int loglevel,
                                                                  Map<String, Map<String, String>> dataContext,
                                                                  ExecutionListener executionListener,
                                                                  final Framework framework) {
        return new ExecutionContextImpl(frameworkProject, user, new NodeSet(singleNode), args, loglevel, dataContext,
            executionListener, framework);
    }

    /**
     * Create a new ExecutionContext with a single node nodeset value, and all other values from the input context
     */
    public static ExecutionContextImpl createExecutionContextImpl(final ExecutionContext context,
                                                                  final INodeEntry singleNode) {
        return new ExecutionContextImpl(context.getFrameworkProject(), context.getUser(), new NodeSet(singleNode),
            context.getArgs(), context.getLoglevel(), context.getDataContext(), context.getExecutionListener(),
            context.getFramework());
    }

    /**
     * Create a new ExecutionContext from an original substituting a specific datacontext.
     */
    public static ExecutionContextImpl createExecutionContextImpl(final ExecutionContext context,
                                                                  final Map<String, Map<String, String>> dataContext) {
        return new ExecutionContextImpl(context.getFrameworkProject(), context.getUser(), context.getNodeSet(),
            context.getArgs(), context.getLoglevel(), dataContext, context.getExecutionListener(),
            context.getFramework());
    }

    public String getFrameworkProject() {
        return frameworkProject;
    }

    public String getUser() {
        return user;
    }

    public NodeSet getNodeSet() {
        return nodeSet;
    }

    public String[] getArgs() {
        return args;
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

    public void setFramework(Framework framework) {
        this.framework = framework;
    }
}
