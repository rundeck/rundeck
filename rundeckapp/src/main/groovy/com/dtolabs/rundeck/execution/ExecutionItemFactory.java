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
* ExecutionItemFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/28/11 6:51 PM
* 
*/
package com.dtolabs.rundeck.execution;

import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet;
import com.dtolabs.rundeck.core.execution.PluginNodeStepExecutionItemImpl;
import com.dtolabs.rundeck.core.execution.PluginStepExecutionItemImpl;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem;
import com.dtolabs.rundeck.core.jobs.JobReferenceItem;
import com.dtolabs.rundeck.core.plugins.PluginConfiguration;

import java.util.List;
import java.util.Map;


/**
 * ExecutionItemFactory is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionItemFactory {

    public static StepExecutionItem createScriptFileItem(
            final String type,
            final Map configuration,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label,
            final List<PluginConfiguration> filterConfigs
    ){
        return ExecutionItemFactory.createPluginNodeStepItem(
                type,
                configuration,
                keepgoingOnSuccess,
                handler,
                label,
                filterConfigs
        );
    }

    // New overload: accepts ConditionSet and forwards to plugin node step item with conditions
    public static StepExecutionItem createScriptFileItem(
            final String type,
            final Map configuration,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label,
            final List<PluginConfiguration> filterConfigs,
            final ConditionalSet conditions
    ){
        return ExecutionItemFactory.createPluginNodeStepItem(
                type,
                configuration,
                keepgoingOnSuccess,
                handler,
                label,
                filterConfigs,
                conditions
        );
    }

    /**
     * Create step execution item for a job reference
     *
     * @param jobIdentifier
     * @param args
     * @param nodeStep
     * @param handler
     * @param keepgoingOnSuccess
     *
     * @return
     *
     * @deprecated
     */
    @Deprecated
    public static StepExecutionItem createJobRef(
            final String jobIdentifier,
            final String[] args,
            final boolean nodeStep,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label
    )
    {
        return createJobRef(
                jobIdentifier,
                args,
                nodeStep,
                handler,
                keepgoingOnSuccess,
                null,
                null,
                null,
                null,
                null,
                label,
                false,
                null,
                false,
                false,
                null,
                false,
                false,
                false,
                null,
                null
        );
    }

    /**
     * Create step execution item for a job reference
     *
     * @param jobIdentifier
     * @param args
     * @param nodeStep
     * @param handler
     * @param keepgoingOnSuccess
     *
     * @return
     */
    public static StepExecutionItem createJobRef(
            final String jobIdentifier,
            final String[] args,
            final boolean nodeStep,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String nodeFilter,
            final Integer nodeThreadcount,
            final Boolean nodeKeepgoing,
            final String nodeRankAttribute,
            final Boolean nodeRankOrderAscending,
            final String label,
            final Boolean nodeIntersect,
            final String project,
            final Boolean failOnDisable,
            final Boolean importOptions,
            final String uuid,
            final Boolean useName,
            final Boolean ignoreNotifications,
            final Boolean childNodes
            )
    {
        return createJobRef(
                jobIdentifier,
                args,
                nodeStep,
                handler,
                keepgoingOnSuccess,
                nodeFilter,
                nodeThreadcount,
                nodeKeepgoing,
                nodeRankAttribute,
                nodeRankOrderAscending,
                label,
                nodeIntersect,
                project,
                failOnDisable,
                importOptions,
                uuid,
                useName,
                ignoreNotifications,
                childNodes,
                null,
                null
        );
    }


    /**
     * Create step execution item for a job reference with workflow
     *
     * @param jobIdentifier
     * @param args
     * @param nodeStep
     * @param handler
     * @param keepgoingOnSuccess
     *
     * @return
     */
    public static StepExecutionItem createJobRef(
            final String jobIdentifier,
            final String[] args,
            final boolean nodeStep,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String nodeFilter,
            final Integer nodeThreadcount,
            final Boolean nodeKeepgoing,
            final String nodeRankAttribute,
            final Boolean nodeRankOrderAscending,
            final String label,
            final Boolean nodeIntersect,
            final String project,
            final Boolean failOnDisable,
            final Boolean importOptions,
            final String uuid,
            final Boolean useName,
            final Boolean ignoreNotifications,
            final Boolean childNodes,
            final WorkflowExecutionItem workflow
    )
    {
        return createJobRef(
                jobIdentifier,
                args,
                nodeStep,
                handler,
                keepgoingOnSuccess,
                nodeFilter,
                nodeThreadcount,
                nodeKeepgoing,
                nodeRankAttribute,
                nodeRankOrderAscending,
                label,
                nodeIntersect,
                project,
                failOnDisable,
                importOptions,
                uuid,
                useName,
                ignoreNotifications,
                childNodes,
                workflow,
                null
        );
    }

    // New full overload: accepts optional workflow and conditions and constructs JobReferenceItem including conditions
    public static StepExecutionItem createJobRef(
            final String jobIdentifier,
            final String[] args,
            final boolean nodeStep,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String nodeFilter,
            final Integer nodeThreadcount,
            final Boolean nodeKeepgoing,
            final String nodeRankAttribute,
            final Boolean nodeRankOrderAscending,
            final String label,
            final Boolean nodeIntersect,
            final String project,
            final Boolean failOnDisable,
            final Boolean importOptions,
            final String uuid,
            final Boolean useName,
            final Boolean ignoreNotifications,
            final Boolean childNodes,
            final WorkflowExecutionItem workflow,
            final ConditionalSet conditions
    )
    {
        JobReferenceItem jobReferenceItem = new JobReferenceItem(
                label,
                jobIdentifier,
                args,
                nodeStep,
                handler,
                keepgoingOnSuccess,
                nodeKeepgoing,
                nodeFilter,
                nodeThreadcount,
                nodeRankAttribute,
                nodeRankOrderAscending,
                nodeIntersect,
                project,
                failOnDisable,
                importOptions,
                uuid,
                useName,
                ignoreNotifications,
                childNodes,
                workflow
        );
        if (conditions != null) {
            jobReferenceItem.setConditions(conditions);
        }
        return jobReferenceItem;
    }

    /**
     * Create a workflow execution item for a plugin node step.
     */
    public static StepExecutionItem createPluginNodeStepItem(
            final String type,
            final Map configuration,
            final boolean keepgoingOnSuccess,
            final StepExecutionItem handler, final String label
    )
    {
        return createPluginNodeStepItem(type, configuration, keepgoingOnSuccess, handler, label, null);
    }

    /**
     * Create a workflow execution item for a plugin node step.
     */
    public static StepExecutionItem createPluginNodeStepItem(
            final String type,
            final Map configuration,
            final boolean keepgoingOnSuccess,
            final StepExecutionItem handler,
            final String label,
            final List<PluginConfiguration> filterConfigurations
    )
    {
        return createPluginNodeStepItem(type, configuration, keepgoingOnSuccess, handler, label, filterConfigurations, null);
    }

    /**
     * Create a workflow execution item for a plugin node step with conditions.
     */
    public static StepExecutionItem createPluginNodeStepItem(
            final String type,
            final Map configuration,
            final boolean keepgoingOnSuccess,
            final StepExecutionItem handler,
            final String label,
            final List<PluginConfiguration> filterConfigurations,
            final ConditionalSet conditions
    )
    {
        return new PluginNodeStepExecutionItemImpl(
                type,
                configuration,
                keepgoingOnSuccess,
                handler,
                label,
                filterConfigurations,
                conditions
        );
    }

    /**
     * Create a workflow execution item for a plugin step.
     */
    public static StepExecutionItem createPluginStepItem(
            final String type,
            final Map configuration,
            final boolean keepgoingOnSuccess,
            final StepExecutionItem handler, final String label
    )
    {
        return createPluginStepItem(type, configuration, keepgoingOnSuccess, handler, label, null);
    }

    /**
     * Create a workflow execution item for a plugin step.
     */
    public static StepExecutionItem createPluginStepItem(
            final String type,
            final Map configuration,
            final boolean keepgoingOnSuccess,
            final StepExecutionItem handler,
            final String label,
            final List<PluginConfiguration> filterConfigurations
    )
    {
        return createPluginStepItem(type, configuration, keepgoingOnSuccess, handler, label, filterConfigurations, null);
    }

    /**
     * Create a workflow execution item for a plugin step with conditions.
     */
    public static StepExecutionItem createPluginStepItem(
            final String type,
            final Map configuration,
            final boolean keepgoingOnSuccess,
            final StepExecutionItem handler,
            final String label,
            final List<PluginConfiguration> filterConfigurations,
            final ConditionalSet conditions
    )
    {
        return new PluginStepExecutionItemImpl(
                type,
                configuration,
                keepgoingOnSuccess,
                handler,
                label,
                filterConfigurations,
                null,
                conditions
        );
    }
}
