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
* JobRefCommandBase.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/23/11 1:57 PM
* 
*/
package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.HasFailureHandler;


/**
 * JobRefCommandBase implementation returns a null value for jobIdentifier, can be subclassed.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class JobRefCommandBase extends JobRefCommand implements HasFailureHandler {
    public String getJobIdentifier() {
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
}
