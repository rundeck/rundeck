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
* StepExecutionItem.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 11:07:47 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.INodeEntry;

/**
 * StepExecutionItem is the base interface for any step execution item to be submitted to the ExecutionService
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface StepExecutionItem {
    /**
     * @return type of the execution item
     */
    public String getType();

    /**
     * @return label/id or description of this step
     */
    public String getLabel();

    /**
     * Returns the runner node for this step, if applicable.
     * <p>
     * The runner node represents the specific {@link INodeEntry} on which this step should be executed.
     * This is typically relevant for Workflow Steps that are intended
     * to run on a particular Remote Runner.
     * </p>
     * <p>
     * For step types that are not node-specific (e.g., workflow steps that are not tied to a node),
     * or if the runner node is not defined, this method returns {@code null}.
     * </p>
     *
     * @return the runner {@link INodeEntry} for this step, or {@code null} if not applicable or not defined
     */
    public default INodeEntry getRunner() {
        return null;
    }

}
