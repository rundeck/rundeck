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
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.core.utils.OptsUtil;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

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
     *
     * @throws ExecutionException if an error occurs
     * @param nodeentry node
     * @param command true if the args are a command
     * @param scriptfile file to execute
     * @param args command or scriptfile args
     * @return parameters
     */
    public ExecTaskParameters generate(final INodeEntry nodeentry, final boolean command, final File scriptfile,
                                       final String[] args) throws ExecutionException {
        final String commandexecutable;
        final String[] commandargs;

        if (!command && null == scriptfile) {
            throw new ExecutionException("Could not determine the command to dispatch");
        }
        if ("windows".equals(nodeentry.getOsFamily())) {
            //TODO: escape args properly for windows
            commandexecutable = "cmd.exe";
            if (command) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(0, "/c");
                list.addAll(Arrays.asList(args));
                commandargs = list.toArray(new String[list.size()]);
            } else if (null != scriptfile) {
                //commandString is the script file location
                ArrayList<String> list = new ArrayList<String>();
                list.add(scriptfile.getAbsolutePath());
                if(args!=null && args.length>0){
                    list.addAll(Arrays.asList(args));
                }
                list.add(0,"/c");
                commandargs = list.toArray(new String[list.size()]);
            } else {
                throw new ExecutionException("Could not determine the command to dispatch");
            }
        } else {
            if (command) {
                commandexecutable = "/bin/sh";
                commandargs = new String[]{"-c", StringUtils.join(args, " ")};
            } else if (null != scriptfile) {
                final String scriptPath = scriptfile.getAbsolutePath();
                commandexecutable = scriptPath;
                commandargs=args;
            } else {
                throw new ExecutionException("Could not determine the command to dispatch");
            }
        }
        return new ExecTaskParameters() {
            public String getCommandexecutable() {
                return commandexecutable;
            }

            public String[] getCommandArgs() {
                return commandargs;
            }
        };
    }
}
