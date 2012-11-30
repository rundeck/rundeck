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
* BaseStepPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/21/12 10:45 AM
* 
*/
package com.dtolabs.rundeck.plugins.step;

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;



/**
 * BaseStepPlugin provides a base class for {@link StepPlugin} classes. Subclasses should implement {@link
 * #performStep(PluginStepContext)}
 *
 * @see AbstractBasePlugin
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class BaseStepPlugin extends AbstractBasePlugin implements StepPlugin, Describable {

    @Override
    public final boolean executeStep(final PluginStepContext context, final PluginStepItem item) throws StepException {
        configureDescribedProperties(item.getStepConfiguration());
        return performStep(context);
    }

    /**
     * Perform the step and return true if successful
     */
    protected abstract boolean performStep(PluginStepContext context);

}
