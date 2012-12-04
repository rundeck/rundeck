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
* PluginStepPropertyRetriever.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/3/12 3:43 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.PropertyRetriever;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.step.PluginStepItem;
import com.dtolabs.rundeck.plugins.step.PropertyResolver;


/**
 * gets plugin step instance configuration properties
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class PluginStepPropertyRetriever implements PropertyRetriever {
    private PluginStepItem item;

    public PluginStepPropertyRetriever(final PluginStepItem item) {
        this.item = item;
    }

    @Override
    public String getProperty(final String name) {
        if (null != item.getStepConfiguration() && null != item.getStepConfiguration().get(name)) {
            return item.getStepConfiguration().get(name).toString();
        }
        return null;
    }

}
