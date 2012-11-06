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

import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecCommandBase;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileCommandBase;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLCommandBase;

import java.io.File;

/**
 * ExecutionItemFactory is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionItemFactory {
    public static ExecutionItem createScriptFileItem(final String script,
                                                     final String[] strings,
                                                     final ExecutionItem handler, final boolean keepgoingOnSuccess) {
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
            public ExecutionItem getFailureHandler() {
                return handler;
            }

            @Override
            public boolean isKeepgoingOnSuccess() {
                return keepgoingOnSuccess;
            }
        };
    }
    public static ExecutionItem createScriptFileItem(final File file,
                                                     final String[] strings,
                                                     final ExecutionItem handler, final boolean keepgoingOnSuccess){
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
            public ExecutionItem getFailureHandler() {
                return handler;
            }

            @Override
            public boolean isKeepgoingOnSuccess() {
                return keepgoingOnSuccess;
            }
        };
    }
    public static ExecutionItem createScriptURLItem(final String urlString, final String[] strings,
                                                    final ExecutionItem handler, final boolean keepgoingOnSuccess){
        return new ScriptURLCommandBase() {
            public String getURLString() {
                return urlString;
            }

            public String[] getArgs() {
                return strings;
            }

            @Override
            public ExecutionItem getFailureHandler() {
                return handler;
            }

            @Override
            public boolean isKeepgoingOnSuccess() {
                return keepgoingOnSuccess;
            }
        };
    }
    public static ExecutionItem createExecCommand(final String[] command,
                                                  final ExecutionItem handler, final boolean keepgoingOnSuccess){

        return new ExecCommandBase() {
            public String[] getCommand() {
                return command;
            }

            @Override
            public ExecutionItem getFailureHandler() {
                return handler;
            }

            @Override
            public boolean isKeepgoingOnSuccess() {
                return keepgoingOnSuccess;
            }
        };
    }
    public static ExecutionItem createJobRef(final String jobIdentifier,
                                             final String[] args,
                                             final ExecutionItem handler, final boolean keepgoingOnSuccess){

        return new JobRefCommandBase() {
            public String getJobIdentifier() {
                return jobIdentifier;
            }

            @Override
            public String[] getArgs() {
                return args;
            }

            @Override
            public ExecutionItem getFailureHandler() {
                return handler;
            }

            @Override
            public boolean isKeepgoingOnSuccess() {
                return keepgoingOnSuccess;
            }
        };
    }
}
