package org.rundeck.app.data.workflow

import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.execution.BaseExecutionItem
import com.dtolabs.rundeck.core.execution.PluginStepExecutionItemImpl
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet
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
import rundeck.services.feature.FeatureService

class WorkflowDataWorkflowExecutionItemFactory implements WorkflowExecutionItemFactory {
    FeatureService featureService
    /**
     * Create an WorkflowExecutionItem instance for the given WorkflowData,
     * suitable for the ExecutionService layer
     */
    WorkflowExecutionItem createExecutionItemForWorkflow(WorkflowData workflow) {
        if (!workflow.steps || workflow.steps.size() < 1) {
            throw new Exception("Workflow is empty")
        }

        // Flatten conditional steps into flat list of StepExecutionItems
        List<StepExecutionItem> flattenedSteps = consolidateWorkflowSteps(workflow.steps)

        def impl = new WorkflowImpl(
                flattenedSteps,
                workflow.threadcount,
                workflow.keepgoing,
                workflow.strategy ? workflow.strategy : "node-first"
        )
        impl.setPluginConfig(workflow.pluginConfigMap)
        final WorkflowExecutionItemImpl item = new WorkflowExecutionItemImpl(impl)
        return item
    }

    /**
     * Consolidate WorkflowStepData list into a flat list of StepExecutionItems, flattening any ConditionalSteps
     * and attaching their ConditionSets to the resulting StepExecutionItems.
     * @param steps
     * @return
     */
    List<StepExecutionItem> consolidateWorkflowSteps(List<WorkflowStepData> steps) {
        List<StepExecutionItem> flattened = []
        
        steps.each { WorkflowStepData step ->
            // Check if this is a ConditionalStep
            if (step instanceof ConditionalStep && featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL)) {
                ConditionalStep conditionalStep = (ConditionalStep) step
                
                // Convert data model ConditionalSet to core ConditionSet
                ConditionalSet conditionSet = ConditionalSetImpl.fromDataModel(conditionalStep.conditionSet)
                
                // Flatten sub-steps and attach conditionSet to each
                if (conditionalStep.subSteps) {
                    conditionalStep.subSteps.each { WorkflowStepData subStep ->
                        StepExecutionItem subStepItem = itemForWFCmdItem(
                            subStep,
                            subStep.errorHandler ? itemForWFCmdItem(subStep.errorHandler, null) : null
                        )
                        
                        // Attach conditionSet to the sub-step
                        if (subStepItem instanceof BaseExecutionItem) {
                            ((BaseExecutionItem) subStepItem).setConditions(conditionSet)
                        } else if (subStepItem instanceof PluginStepExecutionItemImpl) {
                            ((PluginStepExecutionItemImpl) subStepItem).setConditions(conditionSet)
                        }
                        
                        flattened.add(subStepItem)
                    }
                }
            } else {
                // Regular step - convert normally
                StepExecutionItem stepItem = itemForWFCmdItem(
                    step,
                    step.errorHandler ? itemForWFCmdItem(step.errorHandler, null) : null
                )
                flattened.add(stepItem)
            }
        }
        
        return flattened
    }

    static StepExecutionItem itemForWFCmdItem(final WorkflowStepData step, final StepExecutionItem handler=null) throws FileNotFoundException {
        // Check if this is a ConditionalStep - should not happen here as it's handled in flattenWorkflowSteps
        if (step instanceof ConditionalStep) {
            throw new IllegalArgumentException("ConditionalStep should be flattened before calling itemForWFCmdItem")
        }
        
        if (step.pluginType == WorkflowStepConstants.TYPE_JOB_REF) {
            createStepForJobRefType(step, handler)
        } else {
            createStepForPluginType(step, handler)
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
        def logFilterConfigs = WorkflowStepDataUtil.createLogFilterConfigs(WorkflowStepDataUtil.getPluginConfigListForType(step, ServiceNameConstants.LogFilter))
        
        if(step.nodeStep) {
            return ExecutionItemFactory.createPluginNodeStepItem(
                    step.pluginType,
                    step.configuration,
                    !!step.keepgoingOnSuccess,
                    handler,
                    step.description,
                    logFilterConfigs
            )
        } else {
            return ExecutionItemFactory.createPluginStepItem(
                    step.pluginType,
                    step.configuration,
                    !!step.keepgoingOnSuccess,
                    handler,
                    step.description,
                    logFilterConfigs
            )
        }
    }
}
