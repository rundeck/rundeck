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
* PluginStepContext.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/26/12 3:04 PM
* 
*/
package com.dtolabs.rundeck.plugins.step;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.plugins.PluginLogger;

import java.util.List;
import java.util.Map;


/**
 * PluginStepContext is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface PluginStepContext {
    /**
     * Return the logger
     */
    public PluginLogger getLogger();
    /**
     * Return the property resolver
     */
    public PropertyResolver getPropertyResolver();
    /**
     * Return the project name
     */
    public String getFrameworkProject();
    /**
     * Return the data context
     */
    public Map<String, Map<String, String>> getDataContext();

    /**
     * Return the nodes used for this execution
     */
    public INodeSet getNodes();
    /**
     * Return the step number within the current workflow
     */
    public int getStepNumber();
    /**
     * Return the context path of step numbers within the larger workflow context.
     */
    public List<Integer> getStepContext();
}
