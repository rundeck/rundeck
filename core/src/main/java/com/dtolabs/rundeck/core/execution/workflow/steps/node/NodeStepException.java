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
* NodeStepException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 6:20 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;

import java.util.Map;


/**
 * Represents an exception when executing a node step
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeStepException extends StepException {
    private final String nodeName;

    public NodeStepException(String s, FailureReason reason, String nodeName) {
        super(s, reason);
        this.nodeName = nodeName;
    }

    public NodeStepException(String s, Throwable throwable, FailureReason reason, String nodeName) {
        super(s, throwable, reason);
        this.nodeName = nodeName;
    }

    public NodeStepException(Throwable throwable, FailureReason reason, String nodeName) {
        super(throwable, reason);
        this.nodeName = nodeName;
    }

    public NodeStepException(String s, FailureReason reason,Map<String,Object> failureData, String nodeName) {
        super(s, reason,failureData);
        this.nodeName = nodeName;
    }

    public NodeStepException(String s, Throwable throwable, FailureReason reason, Map<String,Object> failureData,String nodeName) {
        super(s, throwable, reason,failureData);
        this.nodeName = nodeName;
    }

    public NodeStepException(Throwable throwable, FailureReason reason,Map<String,Object> failureData, String nodeName) {
        super(throwable, reason,failureData);
        this.nodeName = nodeName;
    }

    public String getNodeName() {
        return nodeName;
    }

}
