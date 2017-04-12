/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.core.execution.ServiceThreadBase
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.WorkflowExecutionServiceThread
import com.dtolabs.rundeck.core.execution.workflow.ControlBehavior
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItemImpl
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.WorkflowImpl
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.core.utils.ThreadBoundOutputStream
import com.dtolabs.rundeck.execution.ExecutionItemFactory
import grails.transaction.NotTransactional
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.services.logging.ExecutionLogWriter

import static org.apache.tools.ant.util.StringUtils.getStackTrace

/**
 * Non-transactional service for execution utility methods
 */
class ExecutionUtilService {
    static transactional = false
    def metricService
    def ThreadBoundOutputStream sysThreadBoundOut
    def ThreadBoundOutputStream sysThreadBoundErr

    def finishExecution(Map execMap) {
        finishExecutionMetrics(execMap)
        finishExecutionLogging(execMap)
    }
    def  finishExecutionMetrics(Map execMap) {
        def ServiceThreadBase thread = execMap.thread
        if (!thread.isSuccessful()) {
            metricService.markMeter(ExecutionService.name, 'executionFailureMeter')
        } else {
            metricService.markMeter(ExecutionService.name, 'executionSuccessMeter')
        }
    }
    def  finishExecutionLogging(Map execMap) {
        def ServiceThreadBase<WorkflowExecutionResult> thread = execMap.thread
        def ExecutionLogWriter loghandler = execMap.loghandler

        try {
            WorkflowExecutionResult object = thread.resultObject
            if (!thread.isSuccessful()) {
                Throwable exc = thread.getThrowable()
                def errmsgs = []

                if (exc && exc instanceof com.dtolabs.rundeck.core.NodesetEmptyException) {
                    errmsgs << exc.getMessage()
                } else if (exc) {
                    errmsgs << exc.getMessage()
                    if (exc.getCause()) {
                        errmsgs << "Caused by: " + exc.getCause().getMessage()
                    }
                } else if (object) {
                    loghandler.logVerbose(object.toString())
                }
                if (errmsgs) {
                    log.error(
                            "Execution failed: " + execMap.execution.id +
                                    " in project ${execMap.execution.project}: " +
                                    errmsgs.join(",")
                    )

                    loghandler.logError(errmsgs.join(','))
                    if (exc) {
                        loghandler.logVerbose(getStackTrace(exc))
                    }
                } else {
                    if (object?.controlBehavior == ControlBehavior.Halt && object?.statusString) {
                        def msg = "Execution halted (\"${object.statusString}\"):${execMap.execution.id}" +
                                " in project ${execMap.execution.project}: " +
                                object?.toString()

                        log.error(msg)
                        loghandler.logWarn(msg)
                    } else {
                        def msg = "Execution failed: ${execMap.execution.id}" +
                                " in project ${execMap.execution.project}: " +
                                object?.toString()
                        log.error(msg)
                        loghandler.logError(msg)
                    }
                }
            } else {
                if (object?.controlBehavior == ControlBehavior.Halt) {
                    def msg = "Execution halted (succeeded):${execMap.execution.id}" +
                            " in project ${execMap.execution.project}: " +
                            object?.toString()

                    log.info(msg)
                    loghandler.log(msg)
                } else {
                    log.info(
                            "Execution successful: " + execMap.execution.id + " in project ${execMap.execution.project}"
                    )
                }
            }
        } finally {
            sysThreadBoundOut.close()
            sysThreadBoundOut.removeThreadStream()
            sysThreadBoundErr.close()
            sysThreadBoundErr.removeThreadStream()
            loghandler.close()
        }
    }


    /**
     * Create an WorkflowExecutionItem instance for the given Workflow,
     * suitable for the ExecutionService layer
     */
    public WorkflowExecutionItem createExecutionItemForWorkflow(Workflow workflow) {
        if (!workflow.commands || workflow.commands.size() < 1) {
            throw new Exception("Workflow is empty")
        }

        def impl = new WorkflowImpl(
                workflow.commands.collect {
                    itemForWFCmdItem(
                            it,
                            it.errorHandler ? itemForWFCmdItem(it.errorHandler) : null
                    )
                },
                workflow.threadcount,
                workflow.keepgoing,
                workflow.strategy ? workflow.strategy : "node-first"
        )
        impl.setPluginConfig(workflow.pluginConfigMap)
        final WorkflowExecutionItemImpl item = new WorkflowExecutionItemImpl(impl)
        return item
    }


    public StepExecutionItem itemForWFCmdItem(final WorkflowStep step, final StepExecutionItem handler=null) throws FileNotFoundException {
        if(step instanceof CommandExec || step.instanceOf(CommandExec)){
            CommandExec cmd=step.asType(CommandExec)
            if (null != cmd.getAdhocRemoteString()) {

                final List<String> strings = OptsUtil.burst(cmd.getAdhocRemoteString());
                final String[] args = strings.toArray(new String[strings.size()]);

                return ExecutionItemFactory.createExecCommand(args, handler, !!cmd.keepgoingOnSuccess, step.description);
            } else if (null != cmd.getAdhocLocalString()) {
                final String script = cmd.getAdhocLocalString();
                final String[] args;
                if (null != cmd.getArgString()) {
                    final List<String> strings = OptsUtil.burst(cmd.getArgString());
                    args = strings.toArray(new String[strings.size()]);
                } else {
                    args = new String[0];
                }
                return ExecutionItemFactory.createScriptFileItem(
                        cmd.getScriptInterpreter(),
                        cmd.getFileExtension(),
                        !!cmd.interpreterArgsQuoted,
                        script,
                        args,
                        handler,
                        !!cmd.keepgoingOnSuccess,
                        step.description);

            } else if (null != cmd.getAdhocFilepath()) {
                final String filepath = cmd.getAdhocFilepath();
                final String[] args;
                if (null != cmd.getArgString()) {
                    final List<String> strings = OptsUtil.burst(cmd.getArgString());
                    args = strings.toArray(new String[strings.size()]);
                } else {
                    args = new String[0];
                }
                if(filepath ==~ /^(?i:https?|file):.*$/) {
                    return ExecutionItemFactory.createScriptURLItem(
                            cmd.getScriptInterpreter(),
                            cmd.getFileExtension(),
                            !!cmd.interpreterArgsQuoted,
                            filepath,
                            args,
                            handler,
                            !!cmd.keepgoingOnSuccess,
                            step.description)
                }else {
                    return ExecutionItemFactory.createScriptFileItem(
                            cmd.getScriptInterpreter(),
                            cmd.getFileExtension(),
                            !!cmd.interpreterArgsQuoted,
                            new File(filepath),
                            args,
                            handler,
                            !!cmd.keepgoingOnSuccess,
                            step.description);

                }
            }else {
                throw new IllegalArgumentException("Workflow step type was not expected: "+step);
            }
        }else if (step instanceof JobExec || step.instanceOf(JobExec)) {
            final JobExec jobcmditem = step as JobExec;

            final String[] args;
            if (null != jobcmditem.getArgString()) {
                final List<String> strings = OptsUtil.burst(jobcmditem.getArgString());
                args = strings.toArray(new String[strings.size()]);
            } else {
                args = new String[0];
            }

            return ExecutionItemFactory.createJobRef(
                    jobcmditem.getJobIdentifier(),
                    args,
                    !!jobcmditem.nodeStep,
                    handler,
                    !!jobcmditem.keepgoingOnSuccess,
                    jobcmditem.nodeFilter?:null,
                    jobcmditem.nodeThreadcount!=null && jobcmditem.nodeThreadcount>=1?jobcmditem.nodeThreadcount:null,
                    jobcmditem.nodeKeepgoing,
                    jobcmditem.nodeRankAttribute,
                    jobcmditem.nodeRankOrderAscending,
                    step.description,
                    jobcmditem.nodeIntersect
            )
        }else if(step instanceof PluginStep || step.instanceOf(PluginStep)){
            final PluginStep stepitem = step as PluginStep
            if(stepitem.nodeStep) {
                return ExecutionItemFactory.createPluginNodeStepItem(
                        stepitem.type,
                        stepitem.configuration,
                        !!stepitem.keepgoingOnSuccess,
                        handler,
                        step.description
                )
            }else {
                return ExecutionItemFactory.createPluginStepItem(
                        stepitem.type,
                        stepitem.configuration,
                        !!stepitem.keepgoingOnSuccess,
                        handler,
                        step.description
                )
            }
        } else {
            throw new IllegalArgumentException("Workflow step type was not expected: "+step);
        }
    }
}
