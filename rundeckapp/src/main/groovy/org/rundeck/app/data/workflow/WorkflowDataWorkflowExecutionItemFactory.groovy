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
     * (including nested ones) and attaching their combined ConditionSets to the resulting StepExecutionItems.
     * @param steps List of workflow steps to flatten
     * @param parentConditionSet Optional parent condition set to combine with nested conditionals
     * @return Flattened list of StepExecutionItems with conditions attached
     */
    List<StepExecutionItem> consolidateWorkflowSteps(
        List<WorkflowStepData> steps,
        ConditionalSet parentConditionSet = null
    ) {
        List<StepExecutionItem> flattened = []

        steps.each { WorkflowStepData step ->
            // Check if this is a ConditionalStep
            if (step instanceof ConditionalStep && featureService.featurePresent(Features.EARLY_ACCESS_JOB_CONDITIONAL)) {
                ConditionalStep conditionalStep = (ConditionalStep) step

                // Convert data model ConditionalSet to core ConditionSet
                ConditionalSet conditionSet = ConditionalSetImpl.fromDataModel(conditionalStep.conditionSet)

                // Combine parent and current conditions (AND logic)
                ConditionalSet combinedConditionSet = combineConditionSets(parentConditionSet, conditionSet)

                // RECURSIVE: Process subSteps with combined condition
                if (conditionalStep.subSteps) {
                    List<StepExecutionItem> subItems = consolidateWorkflowSteps(
                        conditionalStep.subSteps,
                        combinedConditionSet
                    )
                    flattened.addAll(subItems)
                }
            } else {
                // Leaf step - convert and attach combined conditions if any
                StepExecutionItem stepItem = itemForWFCmdItem(
                    step,
                    step.errorHandler ? itemForWFCmdItem(step.errorHandler, null) : null
                )

                if (parentConditionSet != null) {
                    attachConditionsToStep(stepItem, parentConditionSet)
                }

                flattened.add(stepItem)
            }
        }

        return flattened
    }

    /**
     * Combine two ConditionalSets using AND logic (Cartesian product of OR groups).
     * @param parent Parent condition set (can be null)
     * @param child Child condition set (can be null)
     * @return Combined condition set, or null if both are null
     */
    private ConditionalSet combineConditionSets(ConditionalSet parent, ConditionalSet child) {
        if (parent == null) return child
        if (child == null) return parent

        // Guard against null or empty condition groups
        def parentGroups = parent.conditionGroups
        def childGroups = child.conditionGroups

        if (parentGroups == null || parentGroups.isEmpty()) return child
        if (childGroups == null || childGroups.isEmpty()) return parent

        def combined = new ConditionalSetImpl()
        combined.nodeStep = parent.nodeStep || child.nodeStep

        // Cartesian product of OR groups to implement AND logic
        // If parent has groups [A, B] and child has groups [C, D]
        // Result: [A+C, A+D, B+C, B+D]
        List combinedGroups = []
        parentGroups.each { parentGroup ->
            childGroups.each { childGroup ->
                // Merge AND groups (concatenate conditions)
                List mergedGroup = []
                mergedGroup.addAll(parentGroup)
                mergedGroup.addAll(childGroup)
                combinedGroups.add(mergedGroup)
            }
        }
        combined.conditionGroups = combinedGroups

        return combined
    }

    /**
     * Attach a ConditionalSet to a StepExecutionItem.
     * @param stepItem The step item to attach conditions to
     * @param conditionSet The condition set to attach
     */
    private void attachConditionsToStep(StepExecutionItem stepItem, ConditionalSet conditionSet) {
        if (stepItem instanceof BaseExecutionItem) {
            ((BaseExecutionItem) stepItem).setConditions(conditionSet)
        } else if (stepItem instanceof PluginStepExecutionItemImpl) {
            ((PluginStepExecutionItemImpl) stepItem).setConditions(conditionSet)
        }
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
