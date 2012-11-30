/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* BaseNodeStepPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/21/12 2:07 PM
* 
*/
package com.dtolabs.rundeck.plugins.step;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;


/**
 * BaseNodeStepPlugin provides a base class for a NodeStepPlugin subclasses. Subclasses should implement {@link #performNodeStep(PluginStepContext, com.dtolabs.rundeck.core.common.INodeEntry)}.
 *
 * @see AbstractBasePlugin
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class BaseNodeStepPlugin extends AbstractBasePlugin implements NodeStepPlugin, Describable {
    @Override
    public final boolean executeNodeStep(final PluginStepContext context,
                                         final PluginStepItem item,
                                         final INodeEntry entry)
        throws NodeStepException {

        configureDescribedProperties(item.getStepConfiguration());
        return performNodeStep(context, entry);
    }

    /**
     * Perform the step for the node and return true if successful
     */
    protected abstract boolean performNodeStep(PluginStepContext context, INodeEntry entry);
}
