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
* PluginStepItemImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 4:57 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.plugins.step.PluginStepItem;

import java.util.*;


/**
* PluginStepItemImpl is ...
*
* @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
*/
public class PluginStepItemImpl implements PluginStepItem {

    String type;
    Map<String, Object> configuration;

    public PluginStepItemImpl(String type, Map<String, Object> configuration) {
        this.type = type;
        this.configuration = configuration;
    }

    @Override
    public Map<String, Object> getStepConfiguration() {
        return configuration;
    }

    @Override
    public String getType() {
        return type;
    }
}
