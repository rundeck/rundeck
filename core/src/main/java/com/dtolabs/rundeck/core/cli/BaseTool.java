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

/*
* BaseTool.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 30, 2010 4:15:02 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.VersionConstants;
import org.apache.commons.cli.*;
import org.apache.log4j.PropertyConfigurator;

import java.util.ArrayList;
import java.util.List;

/**
 * BaseTool provides a base lifecyle for a commandline tool, and allows {@link com.dtolabs.rundeck.core.cli.CLIToolOptions}
 * objects to be used for modular options processing.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public abstract class BaseTool implements CLITool {
    /**
     * reference to the command line {@link org.apache.commons.cli.Options} instance.
     */
    private final Options options;
    /**
     * Commandline
     */
    private CommandLine commandLine;
    private final List<CLIToolOptions> toolOptions;
    private boolean shouldExit = false;

    /**
     * Add a new CLIToolOptions object to the options used by this tool.
     *
     * @param option options
     */
    protected void addToolOptions(final CLIToolOptions option) {
        toolOptions.add(option);
    }

    private HelpOptions helpOptions;

    protected BaseTool() {
        options = new Options();
        toolOptions = new ArrayList<CLIToolOptions>();
        if (isUseHelpOption()) {
            helpOptions = new HelpOptions();
            toolOptions.add(helpOptions);
        }
    }

    /**
     * @return true if the -h/--help option should be added to the options automatically.
     */
    protected abstract boolean isUseHelpOption();

    private boolean optionsHaveInited=false;
    /**
     * initialize any options, will apply this for each CLIToolOptions added to the tool. subclasses may override this
     * but should call super
     */
    protected void initOptions() {
        if (null != toolOptions && !optionsHaveInited) {
            for (CLIToolOptions toolOpts : toolOptions) {
                toolOpts.addOptions(options);
            }
            optionsHaveInited=true;
        }
    }

    protected Options getOptions() {
        return options;
    }

    /**
     * Run the tool's lifecycle given the input arguments.
     *
     * @param args the cli arg vector
     *
     * @throws CLIToolException if an error occurs
     */
    public void run(final String[] args) throws CLIToolException {
        PropertyConfigurator.configure(Constants.getLog4jProperties(Constants.getSystemBaseDir()));
        CommandLine cli = parseArgs(args);
        validateOptions(cli,args);
        go();
    }

    /**
     * Parse the options, will apply this for each CLIToolOptions added to the tool. subclasses may override this but
     * should call super
     */
    public CommandLine parseArgs(final String[] args) throws CLIToolOptionsException {
        initOptions();
        final CommandLineParser parser = new PosixParser();
        try {
            commandLine = parser.parse(getOptions(), args);
        } catch (ParseException e) {
            help();
            throw new CLIToolOptionsException(e);
        }
        if (null != toolOptions) {
            for (final CLIToolOptions toolOpts : toolOptions) {
                toolOpts.parseArgs(commandLine, args);
            }
        }
        return commandLine;
    }

    /**
     * Validate the values parsed by the options, will apply this for each CLIToolOptions added to the tool. subclasses
     * may override this but should call super
     * @param cli cli
     * @param args args
     * @throws CLIToolOptionsException if an error occurs
     */
    public void validateOptions(final CommandLine cli, final String[] args) throws CLIToolOptionsException {
        if (null != toolOptions) {
            for (final CLIToolOptions toolOpts : toolOptions) {
                toolOpts.validate(cli,args);
            }
        }
    }

    /**
     * Perform the actions for the tool
     * @throws CLIToolException on error
     */
    protected abstract void go() throws CLIToolException;

    protected CommandLine getCommandLine() {
        return commandLine;
    }

    /**
     * @return the help string used when -h option is specified.
     */
    public abstract String getHelpString();

    /**
     * Writes help message .
     */
    public void help() {
        final HelpFormatter formatter = new HelpFormatter();
        final String helpString = getHelpString();
        formatter.printHelp(80,
            helpString,
            "options:",
            getOptions(),
                "[RUNDECK version " + VersionConstants.VERSION + " (" + VersionConstants.BUILD + ")]");
    }


    /**
     * Calls the exit method
     *
     * @param exitcode return code to exit with
     */
    public void exit(final int exitcode) {
        if (isShouldExit()) {
            System.exit(exitcode);
        }
    }

    private boolean isShouldExit() {
        return shouldExit;
    }

    /**
     * Set whether the {@link #exit(int)} method should call System.exit.
     * @param shouldExit true to exit
     */
    protected void setShouldExit(final boolean shouldExit) {
        this.shouldExit = shouldExit;
    }
}
