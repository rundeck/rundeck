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

import com.dtolabs.rundeck.core.cli.ExecTool;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.dispatch.ParallelNodeDispatcher;
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameterGenerator;
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameterGeneratorImpl;
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameters;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.utils.FormattedOutputStream;
import com.dtolabs.rundeck.core.utils.LogReformatter;
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.ExecTask;
import org.apache.tools.ant.taskdefs.Sequential;
import org.apache.tools.ant.types.Commandline;

import java.io.OutputStream;
import java.util.HashMap;
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
                                             final INodeEntry node) throws
        ExecutionException {
        final ExecutionListener listener = context.getExecutionListener();
        final Project project = new Project();
        final LogReformatter gen;
        if (null != listener && listener.isTerse()) {
            gen = null;
        } else {
            String logformat = ExecTool.DEFAULT_LOG_FORMAT;
            if (null != listener && null != listener.getLogFormat()) {
                logformat = listener.getLogFormat();
            }
            final HashMap<String, String> contextData = new HashMap<String, String>();
            //discover node name and username
            contextData.put("node", node.getNodename());
            contextData.put("user", node.extractUserName());
            gen = new LogReformatter(logformat, contextData);
        }

        //bind System printstreams to the thread
        final ThreadBoundOutputStream threadBoundSysOut = ThreadBoundOutputStream.bindSystemOut();
        final ThreadBoundOutputStream threadBoundSysErr = ThreadBoundOutputStream.bindSystemErr();

        //get outputstream for reformatting destination
        final OutputStream origout = threadBoundSysOut.getThreadStream();
        final OutputStream origerr = threadBoundSysErr.getThreadStream();

        //replace any existing logreformatter
        final FormattedOutputStream outformat;
        if (origout instanceof FormattedOutputStream) {
            final OutputStream origsink = ((FormattedOutputStream) origout).getOriginalSink();
            outformat = new FormattedOutputStream(gen, origsink);
        } else {
            outformat = new FormattedOutputStream(gen, origout);
        }
        outformat.setContext("level", "INFO");

        final FormattedOutputStream errformat;
        if (origerr instanceof FormattedOutputStream) {
            final OutputStream origsink = ((FormattedOutputStream) origerr).getOriginalSink();
            errformat = new FormattedOutputStream(gen, origsink);
        } else {
            errformat = new FormattedOutputStream(gen, origerr);
        }
        errformat.setContext("level", "ERROR");

        //install the OutputStreams for the thread
        threadBoundSysOut.installThreadStream(outformat);
        threadBoundSysErr.installThreadStream(errformat);

        String propName = System.currentTimeMillis() + ".node." + node.getNodename() + ".LocalNodeExecutor.result";

        boolean success = false;
        final ExecTask execTask;
        try {
            //perform jsch sssh command
            final Map<String, Map<String, String>> dataContext =
                DataContextUtils.addContext("node", DataContextUtils.nodeData(node), context.getDataContext());
            execTask = buildExecTask(project, parameterGenerator.generate(node, true, null,
                DataContextUtils.replaceDataReferences(command, dataContext)), dataContext);
            execTask.setResultProperty(propName);

            final Task task = createTaskSequence(node, project, execTask);

            task.execute();
            success = true;
        } finally {
            threadBoundSysOut.removeThreadStream();
            threadBoundSysErr.removeThreadStream();
        }
        int result = success ? 0 : -1;
        if (project.getProperty(propName) != null) {
            try {
                result = Integer.parseInt(project.getProperty(propName));
            } catch (NumberFormatException e) {

            }
        }
        final boolean status = success;
        final int resultCode = result;
        return new NodeExecutorResult() {
            public int getResultCode() {
                return resultCode;
            }

            public boolean isSuccess() {
                return status;
            }
        };
    }

    /**
     * Create a task to execute the command on the local node, using the specified exec task parameters and data
     * context.
     *
     * @param nodeentry  the node
     * @param project    ant project
     * @param nestedTask
     *
     * @return a Task
     */
    protected Task createTaskSequence(final INodeEntry nodeentry, final Project project,
                                      final ExecTask nestedTask) throws
        ExecutionException {
        final Sequential seq = new Sequential();
        seq.setProject(project);
        ParallelNodeDispatcher.addNodeContextTasks(nodeentry, project, seq);

        seq.addTask(nestedTask);

        //add success report for current node execution, for use by parallel execution failed nodes listener
        ParallelNodeDispatcher.addNodeContextSuccessReport(nodeentry, project, seq);

        return seq;

    }

    private ExecTask buildExecTask(Project project, ExecTaskParameters taskParameters,
                                   Map<String, Map<String, String>> dataContext) {
        final ExecTask execTask = new ExecTask();
        execTask.setTaskType("exec");
        execTask.setFailonerror(true);
        execTask.setProject(project);
        final Commandline.Argument arg = execTask.createArg();

//        verbose("exectask");

        execTask.setExecutable(taskParameters.getCommandexecutable());
        arg.setLine(taskParameters.getCommandargline());

        //add Env elements to pass environment variables to the exec

        DataContextUtils.addEnvVarsFromContextForExec(execTask, dataContext);
        return execTask;
    }

}
