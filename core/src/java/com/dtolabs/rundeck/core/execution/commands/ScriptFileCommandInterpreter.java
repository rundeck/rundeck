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
* ExecFileCommandInterpreter.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:26 PM
* 
*/
package com.dtolabs.rundeck.core.execution.commands;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.execution.service.*;

import java.io.File;

/**
 * ExecFileCommandInterpreter is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptFileCommandInterpreter implements CommandInterpreter {
    public static final String SERVICE_IMPLEMENTATION_NAME = "script";
    private Framework framework;

    public ScriptFileCommandInterpreter(Framework framework) {
        this.framework = framework;
    }

    public InterpreterResult interpretCommand(ExecutionContext context, ExecutionItem item, INodeEntry node) throws
        InterpreterException {
        ScriptFileCommand script = (ScriptFileCommand) item;

        final FileCopier fileCopier;
        try {
            fileCopier = framework.getFileCopierForNode(node);
        } catch (ExecutionServiceException e) {
            throw new InterpreterException(e);
        }
        String filepath = null; //result file path

        try {
            if (null != script.getScript()) {
                filepath = fileCopier.copyScriptContent(context, script.getScript(), node);
            } else if (null != script.getServerScriptFilePath()) {
                filepath = fileCopier.copyFile(context, new File(script.getServerScriptFilePath()), node);
            } else {
                filepath = fileCopier.copyFileStream(context, script.getScriptAsStream(), node);
            }
        } catch (FileCopierException e) {
            throw new InterpreterException(e);
        }

        final NodeExecutor nodeExecutor;
        try {
            nodeExecutor = framework.getNodeExecutorForNode(node);
        } catch (ExecutionServiceException e) {
            throw new InterpreterException(e);
        }


        try {
            /**
             * TODO: Avoid this horrific hack. Discover how to get SCP task to preserve the execute bit.
             */
            if (!"windows".equalsIgnoreCase(node.getOsFamily())) {
                //perform chmod+x for the file
                final NodeExecutorResult nodeExecutorResult = nodeExecutor.executeCommand(context,
                    new String[]{"chmod", "+x", filepath}, node);
                if(!nodeExecutorResult.isSuccess()){
                    return nodeExecutorResult;
                }
            }

            return nodeExecutor.executeCommand(context, new String[]{filepath}, node);
        } catch (ExecutionException e) {
            throw new InterpreterException(e);
        }
    }
}
