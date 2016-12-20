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

package com.dtolabs.rundeck.core.cli.project;

import com.dtolabs.client.services.DispatcherConfig;
import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.cli.Action;
import com.dtolabs.rundeck.core.cli.ActionMaker;
import com.dtolabs.rundeck.core.cli.BaseTool;
import com.dtolabs.rundeck.core.cli.CLITool;
import com.dtolabs.rundeck.core.common.FrameworkFactory;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcher;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import org.apache.commons.cli.*;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Main class for creating new projects. This is called via rd-project shell command.
 */
public class ProjectTool implements ActionMaker, CLITool {
    public final static String ACTION_CREATE = "create";
    public final static String ACTION_REMOVE = "remove";

    /**
     * control property to overwrite existing installations
     */
    boolean overwrite;

    /**
     * Controls output verbosity
     */
    boolean verbose;

    /**
     * Reference to command line params
     */
    protected CommandLine cli;

    Properties extraProperties;

    /**
     * reference to the command line {@link org.apache.commons.cli.Options} instance.
     */
    protected static final Options options = new Options();

    /**
     * Add the commandline options common to any command
     */
    static {
        options.addOption("h", "help", false, "print this message");
        options.addOption("p", "project", true, "project name");
        options.addOption("a", "action", true, "action to run {create}");
        options.addOption("v", "verbose", false, "verbose messages");
//        options.addOption("G", "cygwin", false, "for create, indicate that the node is using cygwin");
        //options.addOption("N", "nodeslist", true, "Path to arbitrary nodes.properties file");
    }

    CentralDispatcher dispatcher;
    IPropertyLookup frameworkProperties;


    public ProjectTool(final DispatcherConfig config,final File baseDir) {
        /**
         * Initialize the log4j logger
         */
        PropertyConfigurator.configure(Constants.getLog4jPropertiesFile().getAbsolutePath());
        frameworkProperties = FrameworkFactory.createFilesystemFramework(baseDir).getPropertyLookup();
        dispatcher = FrameworkFactory.createDispatcher(config);
        extraProperties = new Properties();
    }

    /**
     * Creates an instance and executes {@link #run(String[])}.
     *
     * @param args args
     * @throws Exception if an error occurs
     */
    public static void main(final String[] args) throws Exception {
        final ProjectTool c = new ProjectTool(
                BaseTool.createDefaultDispatcherConfig(),
                new File(Constants.getSystemBaseDir())
        );
        c.run(args);
    }

    /**
     * Runs the initArgs and go methods.
     *
     * @param args Command line arg vector
     */
    public final void run(final String[] args) {
        int exitCode = 1; //pessimistic initial value

        try {
            parseArgs(args);
            executeAction();
            exitCode = 0;
        } catch (Throwable t) {
            if (null == t.getMessage() || verbose || "true".equals(System.getProperty("rdeck.traceExceptions"))) {
                t.printStackTrace();
            } else {
                error(t);
            }
        }
        exit(exitCode);
    }

    /**
     * Calls the exit method
     *
     * @param code return code to exit with
     */
    public void exit(final int code) {
        System.exit(code);
    }

    /**
     * prints usage info
     */
    public void help() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80,
                "rd-project [options] [ --property=value ... ]",
                "options:",
                options,
                "Examples:\n"
                + "\trd-project -p project --action create; # Initialize project\n"
//                + "\trd-project -p project --action remove ; # Remove the project\n"
                + "\n");
    }

    /**
     * Executes the setup helper actions
     *
     * @throws com.dtolabs.rundeck.core.cli.project.ProjectToolException thrown if action failed
     */
    public void executeAction() throws ProjectToolException {
        final String actionName;
        if (cli.hasOption('a')) {
            String optAction =  cli.getOptionValue('a');           
            actionName = optAction;             
        } else {
            actionName = ACTION_CREATE; // default action
        }

        final Action action = createAction(actionName);
        try {
            action.exec();
        } catch (Throwable t) {
            throw new ProjectToolException(t);
        }
    }



    /**
     * processes the command line input
     *
     * @param args command line arg vector
     */
    public CommandLine parseArgs(final String[] args) throws ProjectToolException {
        //extract any extra property definitions
        parseExtendedProperties(args, extraProperties);
        final String[] cleaned=removeExtendedProperties(args);
        final CommandLineParser parser = new PosixParser();
        try {
            cli = parser.parse(options, cleaned);
        } catch (ParseException e) {
            help();
            throw new ProjectToolException(e);
        }
        initArgs();
        return cli;
    }

    static String[] removeExtendedProperties(String[] args) {
        final ArrayList<String> list=new ArrayList<String>();
        for (final String s : args) {
            if(!isExtendedPropertyArg(s)) {
                list.add(s);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    static boolean isExtendedPropertyArg(final String s) {
        return s.startsWith("--") && s.indexOf("=") > 2;
    }

    static void parseExtendedProperties(String[] args, final Properties properties) {
        for (final String s : args) {
            parsePropertyArg(properties, s);
        }
    }

    static void parsePropertyArg(Properties props, String s) {
        if (isExtendedPropertyArg(s)) {
            final int ei = s.indexOf("=");
            String key = s.substring(2, ei);
            String val = s.substring(ei + 1);
            props.setProperty(key, val);
        }
    }

    /**
     * ActionMaker interface implementations
     */

    public void initArgs() {
        if (null == cli) {
            throw new IllegalStateException("parseArgs must be called to instantiate the cli");
        }
        if (cli.hasOption("h")) {
            help();
            exit(1);
        }
        if (cli.hasOption('v')) {
            verbose = true;
        }
        if (cli.hasOption("S")) {
            verbose("strict flag set. will use registration info from resources.properties file");
        }

        if (cli.hasOption('n')) {
            //validate this is a passable name or pattern
            try {
                Pattern.compile(cli.getOptionValue('n'));
            } catch (PatternSyntaxException e) {
               throw new ProjectToolException("--name argument is not a valid name or expression: \""
                       + cli.getOptionValue('n') +"\"", e);
            }
        }
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public Action createAction(final String actionName) {
        try {
            if (ACTION_CREATE.equals(actionName)) {
                return new CreateAction(this, frameworkProperties, cli, extraProperties,dispatcher);
            } else if (ACTION_REMOVE.equals(actionName)) {
                return new RemoveAction(this, frameworkProperties, cli,dispatcher);
            } else {
                throw new IllegalArgumentException("unknown action name: " + actionName);
            }
        } catch (InvalidArgumentsException e) {
            help();
            throw e;
        }
    }


    /**
     * Interfaces for the CLIToolLogger
     */

    public void error(final String output) {
        System.err.println("error: " + output);
    }

    public void warn(final String output) {
        System.err.println("warn: " + output);
    }

    private void error(final Throwable t) {
        log(t.getMessage());
    }

    public void log(final String message) {
        System.out.println(message);
    }

    public void verbose(final String message) {
        if (verbose) {
            log(message);
        }
    }

    public void debug(final String message) {
        verbose(message);
    }

}
