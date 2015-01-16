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

package com.dtolabs.rundeck.core.cli;

import org.apache.commons.cli.CommandLine;

/**
 * Classes that implement this interface provide a shell tool with a command line interface.
 */
public interface CLITool extends CLIToolLogger {
    /**
     * Reads the argument vector and constructs a {@link CommandLine} object containing params
     *
     * @param args the cli arg vector
     * @return a new instance of CommandLine
     * @throws com.dtolabs.rundeck.core.cli.CLIToolOptionsException parse error
     */
    CommandLine parseArgs(String[] args) throws CLIToolOptionsException;

    /**
     * The run method carries out the lifecycle of the tool, parsing args, handling exceptions,
     * and exiting with a suitable exit code.
     *
     * @param args the cli arg vector
     * @throws CLIToolException error
     */
    void run(String[] args) throws CLIToolException;

    /**
     * Calls the exit method
     *
     * @param exitcode return code to exit with
     */
    void exit(int exitcode);

    /**
     * Writes help message to implementation specific output channel.
     */
    void help();
}
