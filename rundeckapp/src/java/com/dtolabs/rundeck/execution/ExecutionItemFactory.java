/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import java.io.File;
import java.util.Map;


/**
 * ExecutionItemFactory is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionItemFactory {
    public static StepExecutionItem createScriptFileItem(final String scriptInterpreter, final boolean interpreterArgsQuoted,
            final String script,
            final String[] strings, final StepExecutionItem handler, final boolean keepgoingOnSuccess) {
        return new ScriptFileCommandBase() {
            @Override
            public String getScript() {
                return script;
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

            public String getScriptInterpreter() {
                return scriptInterpreter;
            }

            public boolean getInterpreterArgsQuoted() {
                return interpreterArgsQuoted;
            }
        };
    }

    public static StepExecutionItem createScriptFileItem(final String scriptInterpreter, final boolean
            interpreterArgsQuoted,
            final File file,
            final String[] strings,
            final StepExecutionItem handler, final boolean keepgoingOnSuccess) {
        final String filepath = file.getAbsolutePath();
        return new ScriptFileCommandBase() {
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

            public String getScriptInterpreter() {
                return scriptInterpreter;
            }

            public boolean getInterpreterArgsQuoted() {
                return interpreterArgsQuoted;
            }
        };
    }

    public static StepExecutionItem createScriptURLItem(final String scriptInterpreter, final boolean
            interpreterArgsQuoted,
            final String urlString, final String[] strings,
            final StepExecutionItem handler, final boolean keepgoingOnSuccess) {
        return new ScriptURLCommandBase() {
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

            public String getScriptInterpreter() {
                return scriptInterpreter;
            }
        };
    }

    public static StepExecutionItem createExecCommand(final String[] command,
            final StepExecutionItem handler, final boolean keepgoingOnSuccess) {

        return new ExecCommandBase() {
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
        };
    }

    public static StepExecutionItem createJobRef(final String jobIdentifier,
            final String[] args,
            final StepExecutionItem handler, final boolean keepgoingOnSuccess) {

        return new JobRefCommandBase() {
            public String getJobIdentifier() {
                return jobIdentifier;
            }

            @Override
            public String[] getArgs() {
                return args;
            }

            @Override
            public StepExecutionItem getFailureHandler() {
                return handler;
            }

            @Override
            public boolean isKeepgoingOnSuccess() {
                return keepgoingOnSuccess;
            }
        };
    }

    /**
     * Create a workflow execution item for a plugin node step.
     */
    public static StepExecutionItem createPluginNodeStepItem(final String type,
            final Map configuration,
            final boolean keepgoingOnSuccess,
            final StepExecutionItem handler) {

        return new PluginNodeStepExecutionItemImpl(type, configuration, keepgoingOnSuccess, handler);
    }

    /**
     * Create a workflow execution item for a plugin step.
     */
    public static StepExecutionItem createPluginStepItem(final String type,
            final Map configuration,
            final boolean keepgoingOnSuccess,
            final StepExecutionItem handler) {

        return new PluginStepExecutionItemImpl(type, configuration, keepgoingOnSuccess, handler);
    }
}
