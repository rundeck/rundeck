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
* JobRefCommandBase.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/23/11 1:57 PM
* 
*/
package com.dtolabs.rundeck.core.jobs;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.HasFailureHandler;
import com.dtolabs.rundeck.core.execution.workflow.HasParentStepContext;


/**
 * JobRefCommandBase implementation returns a null value for jobIdentifier, can be subclassed.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class JobRefCommandBase extends JobRefCommand implements HasFailureHandler, HasParentStepContext {
    /** -1 sentinel indicates this item is a flat top-level step (not a flattened conditional sub-step). */
    private int parentStepNumber = -1;
    private int subStepNumber = -1;
    /** 1-based logical step number in the original job definition; -1 when not set. */
    private int logicalStepNumber = -1;
    public String getJobIdentifier() {
        return null;
    }

    public String getUuid() {
        return null;
    }

    public String[] getArgs() {
        return new String[0];
    }

    @Override
    public boolean isNodeStep() {
        return false;
    }

    public StepExecutionItem getFailureHandler() {
        return null;
    }

    public boolean isKeepgoingOnSuccess() {
        return false;
    }

    @Override public Boolean getNodeKeepgoing() {
        return null;
    }

    @Override public String getNodeFilter() {
        return null;
    }

    @Override public Integer getNodeThreadcount() {
        return null;
    }

    @Override public String getNodeRankAttribute() {
        return null;
    }

    @Override public Boolean getNodeRankOrderAscending() {
        return null;
    }

    @Override public Boolean getNodeIntersect() {
        return null;
    }

    public Boolean isFailOnDisable() {
        return false;
    }
    public Boolean isImportOptions() {
        return false;
    }

    public Boolean isUseName(){
        return false;
    }

    public Boolean isIgnoreNotifications(){
        return false;
    }

    public Boolean isChildNodes() {
        return false;
    }

    @Override
    public int getParentStepNumber() {
        return parentStepNumber;
    }

    public void setParentStepNumber(int parentStepNumber) {
        this.parentStepNumber = parentStepNumber;
    }

    @Override
    public int getSubStepNumber() {
        return subStepNumber;
    }

    public void setSubStepNumber(int subStepNumber) {
        this.subStepNumber = subStepNumber;
    }

    @Override
    public int getLogicalStepNumber() {
        return logicalStepNumber;
    }

    public void setLogicalStepNumber(int logicalStepNumber) {
        this.logicalStepNumber = logicalStepNumber;
    }
}
