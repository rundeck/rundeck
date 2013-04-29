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
* LocalNodeExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 5:42 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.local;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.impl.common.AntSupport;
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameterGenerator;
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameterGeneratorImpl;
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameters;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;

import java.util.Map;

/**
 * LocalNodeExecutor is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class LocalNodeExecutor implements NodeExecutor {
    public static final String SERVICE_PROVIDER_TYPE = "local";
    private Framework framework;
    private ExecTaskParameterGenerator parameterGenerator;

    public LocalNodeExecutor(final Framework framework) {
        this.framework = framework;
        parameterGenerator = new ExecTaskParameterGeneratorImpl();
    }

    public NodeExecutorResult executeCommand(final ExecutionContext context, final String[] command,
                                             final INodeEntry node)  {
        final ExecutionListener listener = context.getExecutionListener();
        final Project project = new Project();
        AntSupport.addAntBuildListener(listener, project);

        String propName = System.currentTimeMillis() + ".node." + node.getNodename() + ".LocalNodeExecutor.result";

        boolean success = false;
        final ExecTask execTask;
        //perform jsch sssh command
        try {
            execTask = buildExecTask(project, parameterGenerator.generate(node, true, null, command),
                context.getDataContext());
        } catch (ExecutionException e) {
            return NodeExecutorResultImpl.createFailure(StepFailureReason.ConfigurationFailure,
                                                        e.getMessage(),
                                                        node);
        }

        execTask.setResultProperty(propName);

        try {
            execTask.execute();
            success = true;
        } catch (BuildException e) {
            context.getExecutionListener().log(0, e.getMessage());
        }

        int result = success ? 0 : -1;
        if (project.getProperty(propName) != null) {
            try {
                result = Integer.parseInt(project.getProperty(propName));
            } catch (NumberFormatException e) {

            }
        }
        final boolean status = 0==result;
        if(status) {
            return NodeExecutorResultImpl.createSuccess(node);
        }else {
            return NodeExecutorResultImpl.createFailure(NodeStepFailureReason.NonZeroResultCode,
                                                        "Result code was " + result, node, result);
        }
    }

    private ExecTask buildExecTask(Project project, ExecTaskParameters taskParameters,
                                   Map<String, Map<String, String>> dataContext) {
        final ExecTask execTask = new ExecTask();
        execTask.setTaskType("exec");
        execTask.setFailonerror(false);
        execTask.setProject(project);

        execTask.setExecutable(taskParameters.getCommandexecutable());
        String[] commandargs = taskParameters.getCommandArgs();
        if(null!=commandargs){
            for (int i = 0; i < commandargs.length; i++) {
                String commandarg = commandargs[i];
                execTask.createArg().setValue(commandarg);
            }
        }

        //add Env elements to pass environment variables to the exec

        DataContextUtils.addEnvVarsFromContextForExec(execTask, dataContext);
        return execTask;
    }

}
