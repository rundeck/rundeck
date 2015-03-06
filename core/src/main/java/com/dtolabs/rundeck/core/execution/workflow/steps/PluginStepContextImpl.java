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
* PluginStepContextImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/26/12 3:07 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.FlowControl;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.plugins.PluginLogger;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;

import java.util.List;
import java.util.Map;


/**
 * PluginStepContextImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginStepContextImpl implements PluginStepContext {

    PluginLogger logger;
    private String frameworkProject;
    private Map<String, Map<String, String>> dataContext;
    private INodeSet nodes;
    private int stepNumber;
    private List<Integer> stepContext;
    private Framework framework;
    private ExecutionContext executionContext;
    private FlowControl flowControl;

    public PluginStepContextImpl() {
        stepNumber = -1;
    }

    @Override
    public String getFrameworkProject() {
        return frameworkProject;
    }

    @Override
    public Map<String, Map<String, String>> getDataContext() {
        return dataContext;
    }


    @Override
    public PluginLogger getLogger() {
        return logger;
    }

    public static PluginStepContextImpl from(final StepExecutionContext context) {
        final PluginStepContextImpl context1 = new PluginStepContextImpl();
        context1.dataContext = context.getDataContext();
        context1.frameworkProject = context.getFrameworkProject();
        context1.logger = context.getExecutionListener();
        context1.nodes = context.getNodes();
        context1.stepNumber = context.getStepNumber();
        context1.stepContext = context.getStepContext();
        context1.framework = context.getFramework();
        context1.executionContext = context;
        context1.flowControl = context.getFlowControl();
        return context1;
    }

    public INodeSet getNodes() {
        return nodes;
    }

    public int getStepNumber() {
        return stepNumber;
    }

    public List<Integer> getStepContext() {
        return stepContext;
    }

    public Framework getFramework() {
        return framework;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    @Override
    public FlowControl getFlowControl() {
        return flowControl;
    }
}
