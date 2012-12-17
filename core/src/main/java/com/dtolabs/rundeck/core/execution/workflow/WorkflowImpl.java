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

import com.dtolabs.rundeck.core.execution.StepExecutionItem;

import java.util.*;

/**
 * WorkflowImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class WorkflowImpl implements IWorkflow {
    private List<StepExecutionItem> commands;
    private int threadcount;
    private boolean keepgoing;
    private String strategy;

    public WorkflowImpl(final List<StepExecutionItem> commands, final int threadcount, final boolean keepgoing,
                        final String strategy) {
        this.commands = commands;
        this.threadcount = threadcount;
        this.keepgoing= keepgoing;
        this.strategy = strategy;
    }

    public List<StepExecutionItem> getCommands() {
        return commands;
    }

    public void setCommands(final List<StepExecutionItem> commands) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkflowImpl)) {
            return false;
        }

        WorkflowImpl workflow = (WorkflowImpl) o;

        if (keepgoing != workflow.keepgoing) {
            return false;
        }
        if (threadcount != workflow.threadcount) {
            return false;
        }
        if (commands != null ? !commands.equals(workflow.commands) : workflow.commands != null) {
            return false;
        }
        if (strategy != null ? !strategy.equals(workflow.strategy) : workflow.strategy != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = commands != null ? commands.hashCode() : 0;
        result = 31 * result + threadcount;
        result = 31 * result + (keepgoing ? 1 : 0);
        result = 31 * result + (strategy != null ? strategy.hashCode() : 0);
        return result;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
