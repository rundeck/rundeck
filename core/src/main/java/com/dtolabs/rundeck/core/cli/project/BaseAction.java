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

import com.dtolabs.rundeck.core.cli.Action;
import com.dtolabs.rundeck.core.cli.CLIToolLogger;
import com.dtolabs.rundeck.core.common.FrameworkResource;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcher;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import org.apache.commons.cli.CommandLine;

import java.util.List;

/**
 * Base class for implementing project setup actions
 */
public class BaseAction implements Action {

    final protected CLIToolLogger main;
    private boolean verbose=false;

    protected String project;
    private CentralDispatcher centralDispatcher;
    final protected IPropertyLookup frameworkProperties;


    public BaseAction(final CLIToolLogger main, final IPropertyLookup frameworkProperties, final BaseActionArgs args, final CentralDispatcher dispatcher) {
        if (null == main) {
            throw new NullPointerException("main parameter was null");
        }
        if (null == args) {
            throw new NullPointerException("args parameter was null");

        }
        this.main = main;
        this.frameworkProperties = frameworkProperties;
        this.centralDispatcher=dispatcher;
        initArgs(args, true);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    protected static BaseActionArgs parseBaseActionArgs(CommandLine cli){               
        final String project = cli.getOptionValue('p');
        // validate that project name is just alpha-numeric
        if (null != project
                && !project.matches(FrameworkResource.VALID_RESOURCE_NAME_REGEX)) {
           throw new ProjectToolException(
                   "Error: CreateAction: project names can only contain these characters: "
                           + FrameworkResource.VALID_RESOURCE_NAME_REGEX);
        }
        return createArgs(project, cli.hasOption('v'));
    }

    /**
     * Create BaseActionArgs instance
     * @param project project name
     * @param verbose true if verbose output is on
     * @return args instance
     */
    public static BaseActionArgs createArgs(final String project, final boolean verbose){
        return new BaseActionArgs(){
            public String getProject() {
                return project;
            }

            public boolean isVerbose() {
                return verbose;
            }
        };

    }

    public CentralDispatcher getCentralDispatcher() {
        return centralDispatcher;
    }

    public void setCentralDispatcher(final CentralDispatcher centralDispatcher) {
        this.centralDispatcher = centralDispatcher;
    }


    /**
     * Arguments for the BaseAction.
     */
    public static interface BaseActionArgs{
        /**
         * @return Name of project to use
         *
         */
        public String getProject();

        /**
         * @return true to turn verbose logging on.
         */
        public boolean isVerbose();
    }
    protected String getSingleProjectName() throws CentralDispatcherException {
        List<String> strings = getCentralDispatcher().listProjectNames();
        if(strings.size()==1) {
            return strings.get(0);
        }
        return null;
    }


    private void initArgs(BaseActionArgs args, final boolean allowDefaultProject) {
        if (null != args.getProject()) {
            project = args.getProject();
        }
        verbose = args.isVerbose();
    }

    void validate(){
        if(null==project){
            throw new InvalidArgumentsException("-p option not specified");
        }
    }

    /**

    /**
     * Execute the action.  Currently checks if installation is valid.
     *
     * @throws Throwable any throwable
     */
    public void exec() throws Throwable {
        validate();
    }


}
