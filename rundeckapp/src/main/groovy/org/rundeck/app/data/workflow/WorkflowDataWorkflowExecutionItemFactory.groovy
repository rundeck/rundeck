package org.rundeck.app.data.workflow

import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItemImpl
import com.dtolabs.rundeck.core.execution.workflow.WorkflowImpl
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.execution.ExecutionItemFactory
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import org.rundeck.app.execution.workflow.WorkflowExecutionItemFactory
import rundeck.data.constants.WorkflowStepConstants

class WorkflowDataWorkflowExecutionItemFactory implements WorkflowExecutionItemFactory {
    /**
     * Create an WorkflowExecutionItem instance for the given WorkflowData,
     * suitable for the ExecutionService layer
     */
    WorkflowExecutionItem createExecutionItemForWorkflow(WorkflowData workflow) {
        if (!workflow.steps || workflow.steps.size() < 1) {
            throw new Exception("Workflow is empty")
        }

        def impl = new WorkflowImpl(
                workflow.steps.collect {
                    itemForWFCmdItem(
                            it,
                            it.errorHandler ? itemForWFCmdItem(it.errorHandler,null) : null,
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

    static StepExecutionItem itemForWFCmdItem(final WorkflowStepData step, final StepExecutionItem handler=null) throws FileNotFoundException {
        if(WorkflowStepConstants.LIST_COMMAND_TYPES.contains(step.pluginType)){
            createStepForCommandType(step, handler)
        }else if (step.pluginType == WorkflowStepConstants.TYPE_JOB_REF) {
            createStepForJobRefType(step, handler)
        }else {
            createStepForPluginType(step)
        }
    }

    static StepExecutionItem createStepForCommandType(WorkflowStepData step, final StepExecutionItem handler=null) {
        if (step.pluginType == WorkflowStepConstants.TYPE_COMMAND) {

            final List<String> strings = OptsUtil.burst(step.configuration.exec);
            final String[] args = strings.toArray(new String[strings.size()]);

            return ExecutionItemFactory.createExecCommand(
                    args,
                    handler,
                    !!step.keepgoingOnSuccess,
                    step.description,
                    WorkflowStepDataUtil.createLogFilterConfigs(WorkflowStepDataUtil.getPluginConfigListForType(step, ServiceNameConstants.LogFilter))
            );
        } else if (step.pluginType == WorkflowStepConstants.TYPE_SCRIPT) {
            final String script = step.configuration.script;
            final String[] args;
            if (null != step.configuration.args) {
                final List<String> strings = OptsUtil.burst(step.configuration.args);
                args = strings.toArray(new String[strings.size()]);
            } else {
                args = new String[0];
            }
            return ExecutionItemFactory.createScriptFileItem(
                    step.configuration.scriptInterpreter,
                    step.configuration.fileExtension,
                    !!step.configuration.interpreterArgsQuoted,
                    script,
                    args,
                    handler,
                    !!step.keepgoingOnSuccess,
                    step.description,
                    WorkflowStepDataUtil.createLogFilterConfigs(WorkflowStepDataUtil.getPluginConfigListForType(step, ServiceNameConstants.LogFilter))
            );

        } else if (step.pluginType == WorkflowStepConstants.TYPE_SCRIPT_FILE) {
            final String filepath = step.configuration.scriptfile;
            final String[] args;
            if (null != step.configuration.args) {
                final List<String> strings = OptsUtil.burst(step.configuration.args);
                args = strings.toArray(new String[strings.size()]);
            } else {
                args = new String[0];
            }

            return ExecutionItemFactory.createScriptFileItem(
                    step.configuration.scriptInterpreter,
                    step.configuration.fileExtension,
                    !!step.configuration.interpreterArgsQuoted,
                    new File(filepath),
                    args,
                    handler,
                    !!step.keepgoingOnSuccess,
                    step.description,
                    WorkflowStepDataUtil.createLogFilterConfigs(WorkflowStepDataUtil.getPluginConfigListForType(step, ServiceNameConstants.LogFilter)),
                    !!step.configuration.expandTokenInScriptFile
            );


        }else if(step.pluginType == WorkflowStepConstants.TYPE_SCRIPT_URL) {
            final String[] args;
            if (null != step.configuration.args) {
                final List<String> strings = OptsUtil.burst(step.configuration.args);
                args = strings.toArray(new String[strings.size()]);
            } else {
                args = new String[0];
            }
            return ExecutionItemFactory.createScriptURLItem(
                    step.configuration.scriptInterpreter,
                    step.configuration.fileExtension,
                    !!step.configuration.interpreterArgsQuoted,
                    step.configuration.scripturl,
                    args,
                    handler,
                    !!step.keepgoingOnSuccess,
                    step.description,
                    WorkflowStepDataUtil.createLogFilterConfigs( WorkflowStepDataUtil.getPluginConfigListForType(step, ServiceNameConstants.LogFilter)),
                    !!step.configuration.expandTokenInScriptFile
            )
        }
    }

    static StepExecutionItem createStepForJobRefType(WorkflowStepData step, final StepExecutionItem handler=null) {

        final String[] args
        if (null != step.configuration.jobref.args) {
            final List<String> strings = OptsUtil.burst(step.configuration.jobref.args);
            args = strings.toArray(new String[strings.size()]);
        } else {
            args = new String[0];
        }

        return ExecutionItemFactory.createJobRef(
                WorkflowStepDataUtil.getJobIdentifier(step),
                args,
                !!step.nodeStep,
                handler,
                !!step.keepgoingOnSuccess,
                step.configuration.nodeFilter?:null,
                step.configuration.nodeThreadcount!=null && step.configuration.nodeThreadcount>=1?step.configuration.nodeThreadcount:null,
                step.configuration.nodeKeepgoing,
                step.configuration.nodeRankAttribute,
                step.configuration.nodeRankOrderAscending,
                step.description,
                step.configuration.nodeIntersect,
                step.configuration.jobref.jobProject,
                step.configuration.jobref.failOnDisable,
                step.configuration.jobref.importOptions,
                step.configuration.jobref.uuid,
                step.configuration.jobref.useName,
                step.configuration.jobref.ignoreNotifications,
                step.configuration.jobref.childNodes
        )
    }

    static StepExecutionItem createStepForPluginType(WorkflowStepData step, final StepExecutionItem handler=null) {
        if(step.nodeStep) {
            return ExecutionItemFactory.createPluginNodeStepItem(
                    step.pluginType,
                    step.configuration,
                    !!step.keepgoingOnSuccess,
                    handler,
                    step.description,
                    WorkflowStepDataUtil.createLogFilterConfigs(WorkflowStepDataUtil.getPluginConfigListForType(step, ServiceNameConstants.LogFilter))
            )
        }else {
            return ExecutionItemFactory.createPluginStepItem(
                    step.pluginType,
                    step.configuration,
                    !!step.keepgoingOnSuccess,
                    handler,
                    step.description,
                    WorkflowStepDataUtil.createLogFilterConfigs(WorkflowStepDataUtil.getPluginConfigListForType(step, ServiceNameConstants.LogFilter))
            )
        }
    }
}
