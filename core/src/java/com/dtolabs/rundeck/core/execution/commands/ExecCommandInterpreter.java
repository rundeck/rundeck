/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* ExecCommandInterpreter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:10 PM
* 
*/
package com.dtolabs.rundeck.core.execution.commands;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;

/**
 * ExecCommandInterpreter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecCommandInterpreter implements CommandInterpreter {

    public static final String SERVICE_IMPLEMENTATION_NAME = "exec";

    public ExecCommandInterpreter(Framework framework) {
        this.framework = framework;
    }

    private Framework framework;

    public InterpreterResult interpretCommand(ExecutionContext context, ExecutionItem item, INodeEntry node) throws
        InterpreterException {
        final ExecCommand cmd = (ExecCommand) item;
        final NodeExecutor nodeExecutorForNode;
        try {
            nodeExecutorForNode = framework.getNodeExecutorForNode(node);
        } catch (ExecutionServiceException e) {
            throw new InterpreterException(e);
        }
        NodeExecutorResult result;
        try {
            result = nodeExecutorForNode.executeCommand(context, cmd.getCommand(), node);
        } catch (ExecutionException e) {
            throw new InterpreterException(e);
        }
        return result;
    }
}
