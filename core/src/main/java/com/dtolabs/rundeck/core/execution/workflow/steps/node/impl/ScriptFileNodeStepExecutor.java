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
* ExecFileCommandInterpreter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:26 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.HandlerExecutionItem;
import com.dtolabs.rundeck.core.execution.HasFailureHandler;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;


/**
 * ExecFileCommandInterpreter uses ExecutionService to execute a script on a node.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptFileNodeStepExecutor implements NodeStepExecutor {
    public static final String SERVICE_IMPLEMENTATION_NAME = "script";
    private Framework framework;
    private ScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

    public ScriptFileNodeStepExecutor(Framework framework) {
        this.framework = framework;
    }

    public NodeStepResult executeNodeStep(
            StepExecutionContext context,
            NodeStepExecutionItem item,
            INodeEntry node
    )
    throws NodeStepException
    {
        ScriptFileCommand command = (ScriptFileCommand) item;
        boolean expandTokens = true;
        if (context.getFramework().hasProperty("execution.script.tokenexpansion.enabled")) {
            expandTokens = "true".equals(context.getFramework().getProperty("execution.script.tokenexpansion.enabled"));
        }
        return scriptUtils.executeScriptFile(
                context,
                node,
                command.getScript(),
                command.getServerScriptFilePath(),
                command.getScriptAsStream(),
                command.getFileExtension(),
                command.getArgs(),
                command.getScriptInterpreter(),
                command.getInterpreterArgsQuoted(),
                framework.getExecutionService(),
                expandTokens
        );
    }

    public ScriptFileNodeStepUtils getScriptUtils() {
        return scriptUtils;
    }

    public void setScriptUtils(ScriptFileNodeStepUtils scriptUtils) {
        this.scriptUtils = scriptUtils;
    }
}
