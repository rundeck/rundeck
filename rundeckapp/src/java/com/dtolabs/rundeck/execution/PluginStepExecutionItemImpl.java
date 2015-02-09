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
* PluginStepExecutionItemImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/13/12 2:30 PM
* 
*/
package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem;
import com.dtolabs.rundeck.core.execution.HandlerExecutionItem;
import com.dtolabs.rundeck.core.execution.HasFailureHandler;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;

import java.util.*;


/**
 * PluginStepExecutionItemImpl base implementation of a StepExecutionItem that supports PluginStepItem configuration,
 * and HandlerExecutionItem handler properties.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginStepExecutionItemImpl implements StepExecutionItem, ConfiguredStepExecutionItem, HandlerExecutionItem,
                                                    HasFailureHandler {
    private String type;
    private Map stepConfiguration;
    private boolean keepgoingOnSuccess;
    private StepExecutionItem failureHandler;

    public PluginStepExecutionItemImpl(final String type,
                                       final Map stepConfiguration,
                                       final boolean keepgoingOnSuccess,
                                       final StepExecutionItem failureHandler) {
        this.type = type;
        this.stepConfiguration = stepConfiguration;
        this.keepgoingOnSuccess = keepgoingOnSuccess;
        this.failureHandler = failureHandler;
    }

    public Map getStepConfiguration() {
        return stepConfiguration;
    }

    public void setStepConfiguration(final Map stepConfiguration) {
        this.stepConfiguration = stepConfiguration;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public boolean isKeepgoingOnSuccess() {
        return keepgoingOnSuccess;
    }

    public void setKeepgoingOnSuccess(final boolean keepgoingOnSuccess) {
        this.keepgoingOnSuccess = keepgoingOnSuccess;
    }

    public StepExecutionItem getFailureHandler() {
        return failureHandler;
    }

    public void setFailureHandler(StepExecutionItem failureHandler) {
        this.failureHandler = failureHandler;
    }

    @Override
    public String toString() {
        return "StepExecutionItem{" +
               "type='" + type + '\'' +
               ", keepgoingOnSuccess=" + keepgoingOnSuccess +
               ", hasFailureHandler=" + (null!=failureHandler) +
               '}';
    }
}
