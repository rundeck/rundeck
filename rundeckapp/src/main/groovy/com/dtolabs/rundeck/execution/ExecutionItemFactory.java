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

import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandBase;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandBase;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLCommandBase;
import com.dtolabs.rundeck.core.jobs.JobReferenceItem;
import com.dtolabs.rundeck.core.plugins.PluginConfiguration;

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
            final String scriptInterpreter,
            final String fileExtension,
            final boolean interpreterArgsQuoted,
            final String script,
            final String[] strings,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label,
            final boolean enabled
    )
    {
        return createScriptFileItem(
                scriptInterpreter,
                fileExtension,
                interpreterArgsQuoted,
                script,
                strings,
                handler,
                keepgoingOnSuccess,
                label,
                null,
                enabled
        );
    }

    public static StepExecutionItem createScriptFileItem(
            final String scriptInterpreter,
            final String fileExtension,
            final boolean interpreterArgsQuoted,
            final String script,
            final String[] strings,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label,
            final List<PluginConfiguration> filterConfigs,
            final boolean enabled
    )
    {
        return new ScriptFileItem(
                label,
                null,
                script,
                strings,
                handler,
                keepgoingOnSuccess,
                fileExtension,
                scriptInterpreter,
                interpreterArgsQuoted,
                filterConfigs,
                false,
                enabled
        );
    }

    public static StepExecutionItem createScriptFileItem(
            final String scriptInterpreter,
            final String fileExtension,
            final boolean interpreterArgsQuoted,
            final File file,
            final String[] strings,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess, final String label,
            final boolean enabled
    )
    {
        return createScriptFileItem(
                scriptInterpreter,
                fileExtension,
                interpreterArgsQuoted,
                file,
                strings,
                handler,
                keepgoingOnSuccess,
                label,
                null,
                false,
                enabled
        );
    }

    public static StepExecutionItem createScriptFileItem(
            final String scriptInterpreter,
            final String fileExtension,
            final boolean interpreterArgsQuoted,
            final File file,
            final String[] strings,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label,
            final List<PluginConfiguration> filterConfigs,
            final boolean expandTokenInScriptFile,
            final boolean enabled
    )
    {
        final String filepath = file.getAbsolutePath();
        return new ScriptFileItem(
                label,
                filepath,
                null,
                strings,
                handler,
                keepgoingOnSuccess,
                fileExtension,
                scriptInterpreter,
                interpreterArgsQuoted,
                filterConfigs,
                expandTokenInScriptFile,
                enabled
        );
    }

    public static StepExecutionItem createScriptURLItem(
            final String scriptInterpreter,
            final String fileExtension,
            final boolean interpreterArgsQuoted,
            final String urlString, final String[] strings,
            final StepExecutionItem handler, final boolean keepgoingOnSuccess, final String label,
            final boolean enabled
    )
    {
        return createScriptURLItem(
                scriptInterpreter,
                fileExtension,
                interpreterArgsQuoted,
                urlString,
                strings,
                handler,
                keepgoingOnSuccess,
                label,
                null,
                false,
                enabled
        );
    }

    public static StepExecutionItem createScriptURLItem(
            final String scriptInterpreter,
            final String fileExtension,
            final boolean interpreterArgsQuoted,
            final String urlString, final String[] strings,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label,
            final List<PluginConfiguration> filterConfigs,
            final boolean enabled
    )
    {
        return new ScriptURLItem(
                label,
                urlString,
                strings,
                handler,
                keepgoingOnSuccess,
                interpreterArgsQuoted,
                fileExtension,
                scriptInterpreter,
                filterConfigs,
                false,
                enabled
        );
    }

    public static StepExecutionItem createScriptURLItem(
            final String scriptInterpreter,
            final String fileExtension,
            final boolean interpreterArgsQuoted,
            final String urlString, final String[] strings,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label,
            final List<PluginConfiguration> filterConfigs,
            final boolean expandTokenInScriptFile,
            final boolean enabled
    )
    {
        return new ScriptURLItem(
                label,
                urlString,
                strings,
                handler,
                keepgoingOnSuccess,
                interpreterArgsQuoted,
                fileExtension,
                scriptInterpreter,
                filterConfigs,
                expandTokenInScriptFile,
                enabled
        );
    }

    public static StepExecutionItem createExecCommand(
            final String[] command,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label,
            final boolean enabled
    )
    {
        return createExecCommand(command, handler, keepgoingOnSuccess, label, null, enabled);

    }

    public static StepExecutionItem createExecCommand(
            final String[] command,
            final StepExecutionItem handler,
            final boolean keepgoingOnSuccess,
            final String label,
            final List<PluginConfiguration> filterConfigs,
            final boolean enabled
    )
    {

        return new CommandItem(label, command, handler, keepgoingOnSuccess, filterConfigs, enabled);
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
                true
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
            final Boolean childNodes,
            final boolean enabled
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
                childNodes,
                enabled
        );
    }

    /**
     * Create a workflow execution item for a plugin node step.
     */
    public static StepExecutionItem createPluginNodeStepItem(
            final String type,
            final Map configuration,
            final boolean keepgoingOnSuccess,
            final StepExecutionItem handler, final String label,
            final boolean enabled
    )
    {
        return createPluginNodeStepItem(type, configuration, keepgoingOnSuccess, handler, label, null, enabled);
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
            final List<PluginConfiguration> filterConfigurations,
            final boolean enabled
    )
    {

        return new PluginNodeStepExecutionItemImpl(
                type,
                configuration,
                keepgoingOnSuccess,
                handler,
                label,
                filterConfigurations,
                enabled
        );
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
            final boolean enabled
    )
    {
        return createPluginStepItem(type, configuration, keepgoingOnSuccess, handler, label, null, enabled);
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
            final List<PluginConfiguration> filterConfigurations,
            final boolean enabled
    )
    {

        return new PluginStepExecutionItemImpl(
                type,
                configuration,
                keepgoingOnSuccess,
                handler,
                label,
                filterConfigurations,
                enabled
        );
    }

    private static class ScriptFileItem extends ScriptFileCommandBase {
        private final String label;
        private final String filepath;
        private final String script;
        private final String[] strings;
        private final StepExecutionItem handler;
        private final boolean keepgoingOnSuccess;
        private final String fileExtension;
        private final String scriptInterpreter;
        private final boolean interpreterArgsQuoted;
        private final List<PluginConfiguration> filterConfigs;
        private final boolean expandTokenInScriptFile;
        private final boolean enabled;

        public ScriptFileItem(
                final String label,
                final String filepath,
                final String script,
                final String[] strings,
                final StepExecutionItem handler,
                final boolean keepgoingOnSuccess,
                final String fileExtension,
                final String scriptInterpreter,
                final boolean interpreterArgsQuoted,
                final List<PluginConfiguration> filterConfigs,
                final boolean expandTokenInScriptFile,
                final boolean enabled
        )
        {
            this.label = label;
            this.filepath = filepath;
            this.script = script;
            this.strings = strings;
            this.handler = handler;
            this.keepgoingOnSuccess = keepgoingOnSuccess;
            this.fileExtension = fileExtension;
            this.scriptInterpreter = scriptInterpreter;
            this.interpreterArgsQuoted = interpreterArgsQuoted;
            this.filterConfigs = filterConfigs;
            this.expandTokenInScriptFile = expandTokenInScriptFile;
            this.enabled = enabled;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public String getServerScriptFilePath() {
            return filepath;
        }

        @Override
        public String[] getArgs() {
            return strings;
        }

        @Override
        public StepExecutionItem getFailureHandler() {
            return handler;
        }

        @Override
        public boolean isKeepgoingOnSuccess() {
            return keepgoingOnSuccess;
        }

        @Override
        public String getFileExtension() {
            return fileExtension;
        }

        public String getScriptInterpreter() {
            return scriptInterpreter;
        }

        public boolean getInterpreterArgsQuoted() {
            return interpreterArgsQuoted;
        }

        @Override
        public List<PluginConfiguration> getFilterConfigurations() {
            return filterConfigs;
        }

        @Override
        public String getScript() {
            return script;
        }

        @Override
        public boolean isExpandTokenInScriptFile() {
            return expandTokenInScriptFile;
        }

        @Override
        public Boolean isEnabled() {
            return this.enabled;
        }

        @Override
        public String toString() {
            return "ScriptFileItem{" +
                   (label != null ? "label='" + label + "', " : "") +
                   (null != filepath ?
                    "filepath='" + filepath + '\'' :
                    "script=[" + script.length() + " chars]") +
                   "}";
        }

    }

    private static class ScriptURLItem extends ScriptURLCommandBase {
        private final String label;
        private final String urlString;
        private final String[] strings;
        private final StepExecutionItem handler;
        private final boolean keepgoingOnSuccess;
        private final boolean interpreterArgsQuoted;
        private final String fileExtension;
        private final String scriptInterpreter;
        private final List<PluginConfiguration> filterConfigs;
        private final boolean expandTokenInScriptFile;
        private final boolean enabled;

        public ScriptURLItem(
                final String label,
                final String urlString,
                final String[] strings,
                final StepExecutionItem handler,
                final boolean keepgoingOnSuccess,
                final boolean interpreterArgsQuoted,
                final String fileExtension,
                final String scriptInterpreter,
                final List<PluginConfiguration> filterConfigs,
                final boolean expandTokenInScriptFile,
                final boolean enabled
        )
        {
            this.label = label;
            this.urlString = urlString;
            this.strings = strings;
            this.handler = handler;
            this.keepgoingOnSuccess = keepgoingOnSuccess;
            this.interpreterArgsQuoted = interpreterArgsQuoted;
            this.fileExtension = fileExtension;
            this.scriptInterpreter = scriptInterpreter;
            this.filterConfigs = filterConfigs;
            this.expandTokenInScriptFile = expandTokenInScriptFile;
            this.enabled = enabled;
        }

        @Override
        public String getLabel() {
            return label;
        }

        public String getURLString() {
            return urlString;
        }

        public String[] getArgs() {
            return strings;
        }

        @Override
        public StepExecutionItem getFailureHandler() {
            return handler;
        }

        @Override
        public boolean isKeepgoingOnSuccess() {
            return keepgoingOnSuccess;
        }

        public boolean getInterpreterArgsQuoted() {
            return interpreterArgsQuoted;
        }

        @Override
        public String getFileExtension() {
            return fileExtension;
        }

        public String getScriptInterpreter() {
            return scriptInterpreter;
        }

        @Override
        public List<PluginConfiguration> getFilterConfigurations() {
            return filterConfigs;
        }

        @Override
        public boolean isExpandTokenInScriptFile() {
            return expandTokenInScriptFile;
        }

        @Override
        public Boolean isEnabled() {
            return this.enabled;
        }

        @Override
        public String toString() {
            return "ScriptURLItem{" +
                   (label != null ? "label='" + label + ", " : "") +
                   "urlString='" + urlString + '\'' +
                   "}";
        }
    }

    private static class CommandItem extends ExecCommandBase {
        private final String label;
        private final String[] command;
        private final StepExecutionItem handler;
        private final boolean keepgoingOnSuccess;
        private final List<PluginConfiguration> filterConfigs;
        private final boolean enabled;

        public CommandItem(
                final String label,
                final String[] command,
                final StepExecutionItem handler,
                final boolean keepgoingOnSuccess,
                final List<PluginConfiguration> filterConfigs,
                final boolean enabled
        )
        {
            this.label = label;
            this.command = command;
            this.handler = handler;
            this.keepgoingOnSuccess = keepgoingOnSuccess;
            this.filterConfigs = filterConfigs;
            this.enabled = enabled;
        }

        @Override
        public String getLabel() {
            return label;
        }

        public String[] getCommand() {
            return command;
        }

        @Override
        public StepExecutionItem getFailureHandler() {
            return handler;
        }

        @Override
        public boolean isKeepgoingOnSuccess() {
            return keepgoingOnSuccess;
        }

        @Override
        public List<PluginConfiguration> getFilterConfigurations() {
            return filterConfigs;
        }

        @Override
        public Boolean isEnabled() {
            return this.enabled;
        }

        @Override
        public String toString() {
            return "CommandItem{" +
                   (label != null ? "label='" + label + "', " : "") +
                   (command != null ? "command=[" + command.length + " words]" : "") +
                   "}";
        }
    }
}
