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
* WorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 2:13:06 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.plugins.configuration.Validator;
import com.dtolabs.rundeck.core.rules.RuleEngine;

/**
 * WorkflowStrategy interface performs the workflow execution and returns an ExecutionResult
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface WorkflowStrategy {
    /**
     * @return appropriate threadcount for step execution, anything 0 or less indicates as many threads as needed
     */
    int getThreadCount();

    /**
     * setup rule engine
     *
     * @param ruleEngine
     */
    void setup(RuleEngine ruleEngine, StepExecutionContext context, IWorkflow workflow);

    /**
     * Validate configuration values in the context of the workflow
     *
     * @param workflow workflow input
     *
     * @return report of any input property validation errors
     */
    Validator.Report validate(IWorkflow workflow);

    /**
     * Profile for the workflow
     *
     * @return
     */
    WorkflowStrategyProfile getProfile();
}
