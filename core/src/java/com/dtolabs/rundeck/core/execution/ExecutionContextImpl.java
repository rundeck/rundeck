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
    private Map<String,Map<String,String>> dataContext;
    private ExecutionListener executionListener;

    private ExecutionContextImpl(String frameworkProject, String user, NodeSet nodeSet, String[] args, int loglevel,
                                 Map<String, Map<String, String>> dataContext, ExecutionListener executionListener) {
        this.frameworkProject = frameworkProject;
        this.user = user;
        this.nodeSet = nodeSet;
        this.args = args;
        this.loglevel = loglevel;
        this.dataContext = dataContext;
        this.executionListener = executionListener;
    }

    public static ExecutionContextImpl createExecutionContextImpl(String frameworkProject, String user, NodeSet nodeSet,
                                                                  String[] args, int loglevel,
                                                                  Map<String, Map<String, String>> dataContext,
                                                                  ExecutionListener executionListener) {
        return new ExecutionContextImpl(frameworkProject, user, nodeSet, args, loglevel, dataContext,
            executionListener);
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
}
