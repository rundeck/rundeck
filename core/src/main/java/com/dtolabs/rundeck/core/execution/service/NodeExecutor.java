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
* NodeExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:05 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;

import java.io.InputStream;


/**
 * NodeExecutor executes a command on a node.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface NodeExecutor {
    /**
     * Execute a command on a node and return the result.
     *
     * @param context the execution context
     * @param command the array of strings for the command line, with any necessary data context references replaced.
     * @param node    the node to execute on
     *
     * @return a result
     */
    public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node);

    /**
     * Execute a command on a node and return the result, with an inputstream to provide input to the command.
     *
     * @param context the execution context
     * @param command the array of strings for the command line, with any necessary data context references replaced.
     * @param inputStream an inputstream to provide input to the command
     * @param node    the node to execute on
     * @return a result
     */
    default NodeExecutorResult executeCommand(
            ExecutionContext context,
            String[] command,
            InputStream inputStream,
            INodeEntry node
    )
    {
        if (inputStream != null) {
            context
                    .getExecutionLogger()
                    .log(1, "Cannot send some data to the input stream, it is not supported by this NodeExecutor implementation");
        }
        return executeCommand(context, command, node);
    }

    /**
     * To indicate if the command execution suppports rd_variable injection
     * @return boolean
     */
    default boolean supportVariableInjection(){return false;}

}
