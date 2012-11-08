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
package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.io.File;


/**
 * ExecFileCommandInterpreter uses ExecutionService to execute a script on a node.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptFileNodeStepExecutor implements NodeStepExecutor {
    public static final String SERVICE_IMPLEMENTATION_NAME = "script";
    private Framework framework;

    public ScriptFileNodeStepExecutor(Framework framework) {
        this.framework = framework;
    }

    public NodeStepResult executeNodeStep(ExecutionContext context, StepExecutionItem item, INodeEntry node) throws
                                                                                                         NodeStepException {
        final ScriptFileCommand script = (ScriptFileCommand) item;
        final ExecutionService executionService = framework.getExecutionService();
        final String filepath; //result file path
        try {
            if (null != script.getScript()) {
                filepath = executionService.fileCopyScriptContent(context, script.getScript(), node);
            } else if (null != script.getServerScriptFilePath()) {
                filepath = executionService.fileCopyFile(context, new File(
                    script.getServerScriptFilePath()), node);
            } else {
                filepath = executionService.fileCopyFileStream(context, script.getScriptAsStream(), node);
            }
        } catch (FileCopierException e) {
            throw new NodeStepException(e, node.getNodename());
        }

        try {
            /**
             * TODO: Avoid this horrific hack. Discover how to get SCP task to preserve the execute bit.
             */
            if (!"windows".equalsIgnoreCase(node.getOsFamily())) {
                //perform chmod+x for the file

                final NodeExecutorResult nodeExecutorResult = framework.getExecutionService().executeCommand(
                    context, new String[]{"chmod", "+x", filepath}, node);
                if (!nodeExecutorResult.isSuccess()) {
                    return nodeExecutorResult;
                }
            }

            final String[] args = script.getArgs();
            //replace data references
            String[] newargs=null;
            if(null!=args && args.length>0) {
                newargs = new String[args.length + 1];
                final String[] replargs= DataContextUtils.replaceDataReferences(args, context.getDataContext());
                newargs[0]=filepath;
                System.arraycopy(replargs, 0, newargs, 1, replargs.length);
            }else{
                newargs= new String[]{filepath};
            }
            //XXX: windows specific call?

            return framework.getExecutionService().executeCommand(context, newargs, node);
            //TODO: remove remote temp file after exec?
        } catch (ExecutionException e) {
            throw new NodeStepException(e, node.getNodename());
        }
    }
}
