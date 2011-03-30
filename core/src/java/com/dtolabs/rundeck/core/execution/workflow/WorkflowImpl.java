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
* WorkflowImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 16, 2010 1:01:34 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.ExecutionItem;

import java.util.*;

/**
 * WorkflowImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class WorkflowImpl implements IWorkflow {
    private List<ExecutionItem> commands;
    private int threadcount;
    private boolean keepgoing;
    private String strategy;

    public WorkflowImpl(final List<ExecutionItem> commands, final int threadcount, final boolean keepgoing,
                        final String strategy) {
        this.commands = commands;
        this.threadcount = threadcount;
        this.keepgoing= keepgoing;
        this.strategy = strategy;
    }

    public List<ExecutionItem> getCommands() {
        return commands;
    }

    public void setCommands(final List<ExecutionItem> commands) {
        this.commands = commands;
    }

    public int getThreadcount() {
        return threadcount;
    }

    public void setThreadcount(final int threadcount) {
        this.threadcount = threadcount;
    }

    public boolean isKeepgoing() {
        return keepgoing;
    }

    public void setKeepgoing(boolean keepgoing) {
        this.keepgoing = keepgoing;
    }

    @Override
    public String toString() {
        return "WorkflowImpl{" +
               "commands=" + commands +
               ", threadcount=" + threadcount +
               ", keepgoing=" + keepgoing +
               ", strategy=" + strategy +
               '}';
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
