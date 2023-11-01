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

import com.dtolabs.rundeck.core.execution.ScriptFileCommand;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.jobs.JobReferenceItem;
import com.dtolabs.rundeck.core.plugins.PluginConfiguration;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;

import java.io.File;
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
                false
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

        return new JobReferenceItem(
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
                childNodes
        );
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

        return new PluginNodeStepExecutionItemImpl(
                type,
                configuration,
                keepgoingOnSuccess,
                handler,
                label,
                filterConfigurations
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

        return new PluginStepExecutionItemImpl(
                type,
                configuration,
                keepgoingOnSuccess,
                handler,
                label,
                filterConfigurations
        );
    }
}
