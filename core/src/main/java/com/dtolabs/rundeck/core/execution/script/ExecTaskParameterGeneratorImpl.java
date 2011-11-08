/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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

package com.dtolabs.rundeck.core.execution.script;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.cli.CLIUtils;

import java.io.File;

/**
 * This class takes the input objects, and synthesizes the executable string and arguments string for the Ant Exec task
 */

public class ExecTaskParameterGeneratorImpl implements ExecTaskParameterGenerator {

    /**
     * Create the generator
     *
     */
    public ExecTaskParameterGeneratorImpl() {
    }

    /**
     * Generate the {@link #commandexecutable} and {@link #commandargline} values.
     *
     * @throws ExecutionException if an error occurs
     * @param nodeentry
     * @param command
     * @param scriptfile
     * @param args
     */
    public ExecTaskParameters generate(final INodeEntry nodeentry, final boolean command, final File scriptfile,
                                       final String[] args) throws ExecutionException {
        final String commandexecutable;
        final String commandargline;

        String commandString;
        if (!command && null == scriptfile) {
            throw new ExecutionException("Could not determine the command to dispatch");
        }
        if ("windows".equals(nodeentry.getOsFamily())) {
            if (command) {
                commandString = CLIUtils.generateArgline(null, args);
            } else if (null != scriptfile) {
                //commandString is the script file location
                commandString = CLIUtils.generateArgline(scriptfile.getAbsolutePath(), args);
            } else {
                throw new ExecutionException("Could not determine the command to dispatch");
            }
            commandexecutable = "cmd.exe";
            String argline;
            if (commandString.indexOf("\"") >= 0) {
                argline = "/c " + commandString;
            } else if (commandString.indexOf(" ") >= 0) {
                argline = "/c " + "\"" + commandString + "\"";
            } else {
                argline = "/c " + commandString;
            }
            commandargline = argline;
        } else {
            if (command) {
                commandexecutable = "/bin/sh";
                commandString = CLIUtils.generateArgline(null, args);
                commandargline = "-c " + (commandString.contains("\"") ? "'" + commandString + "'"
                                                                       : "\"" + commandString + "\"");
            } else if (null != scriptfile) {
                final String scriptPath = scriptfile.getAbsolutePath();
                commandexecutable = scriptPath;
                commandargline = CLIUtils.generateArgline(null, args);
            } else {
                throw new ExecutionException("Could not determine the command to dispatch");
            }
        }
        return new ExecTaskParameters() {
            public String getCommandexecutable() {
                return commandexecutable;
            }

            public String getCommandargline() {
                return commandargline;
            }
        };
    }
}