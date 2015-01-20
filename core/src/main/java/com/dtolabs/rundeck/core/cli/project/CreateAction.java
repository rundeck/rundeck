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

import com.dtolabs.rundeck.core.cli.CLIToolLogger;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Category;

import java.io.File;
import java.io.IOException;
import java.util.Properties;


/**
 * Creates and initializes the project structure. This involves creating the project repository
 */
public class CreateAction extends BaseAction {
    static Category logger = Category.getInstance(CreateAction.class.getName());

    private boolean cygwin;
    private Properties properties;

    /**
     * Create a new CreateAction, and parse the args from the CommandLine, using {@link BaseAction#parseBaseActionArgs(org.apache.commons.cli.CommandLine)} and
     * {@link #parseCreateActionArgs(org.apache.commons.cli.CommandLine)} to create the argument specifiers.
     * @param main logger
     * @param framework framework
     * @param cli cli
     */
    public CreateAction(final CLIToolLogger main, final Framework framework, final CommandLine cli) {
        this(main, framework, parseBaseActionArgs(cli), parseCreateActionArgs(cli));
    }
    /**
     * Create a new CreateAction, and parse the args from the CommandLine, using {@link BaseAction#parseBaseActionArgs(org.apache.commons.cli.CommandLine)} and
     * {@link #parseCreateActionArgs(org.apache.commons.cli.CommandLine)} to create the argument specifiers.
     * @param main logger
     * @param framework framework
     * @param cli cli
     * @param properties properties
     */
    public CreateAction(final CLIToolLogger main, final Framework framework, final CommandLine cli,
                        final Properties properties) {
        this(main, framework, parseBaseActionArgs(cli), parseCreateActionArgs(cli), properties);
    }

    /**
     * Create a new CreateAction
     * @param main logger
     * @param framework framework object
     * @param baseArgs base args
     * @param createArgs create args
     * @param projectProperties properties
     */
    public CreateAction(final CLIToolLogger main,
                        final Framework framework,
                        final BaseActionArgs baseArgs,
                        final CreateActionArgs createArgs,
                        final Properties projectProperties) {
        super(main, framework, baseArgs);
        properties = projectProperties;
        initArgs(createArgs);

    }
    /**
     * Create a new CreateAction
     * @param main logger
     * @param framework framework object
     * @param baseArgs base args
     *                 @param createArgs create args
     */
    public CreateAction(final CLIToolLogger main,
                        final Framework framework,
                        final BaseActionArgs baseArgs,
                        final CreateActionArgs createArgs) {
        this(main, framework, baseArgs, createArgs, null);
    }

    /**
     * @return is cygwin
     */
    public boolean isCygwin() {
        return cygwin;
    }

    public void setCygwin(boolean cygwin) {
        this.cygwin = cygwin;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    /**
     * Arguments for the CreateAction
     */
    public static interface CreateActionArgs {
        /**
         * @return true if the node is using cygwin
         */
         public boolean isCygwin();
    }

    public static CreateActionArgs parseCreateActionArgs(final CommandLine cli) {
        final boolean cygwin = cli.hasOption('G');
        return new CreateActionArgs() {

            public boolean isCygwin() {
                return cygwin;
            }
        };
    }

    /**
     * Create args instance
     * @param cygwin cygwin
     * @return args
     */
    public static CreateActionArgs createArgs(final boolean cygwin){
        return new CreateActionArgs(){
            public boolean isCygwin() {
                return cygwin;
            }
        };
    }

    private void initArgs(CreateActionArgs args) {
        setCygwin(args.isCygwin());
    }

    /**
     * Execute the action.
     *
     * @throws Throwable any throwable
     */
    public void exec() throws Throwable {
        super.exec();
        if (project == null) {
            throw new IllegalStateException("project was null");

        }
        final File projectDir = new File(framework.getFrameworkProjectsBaseDir(), project);
        main.verbose("project directory exists: " + projectDir.exists());
        try {
            main.verbose("creating project structure in: " + projectDir.getAbsolutePath() + "...");
            FrameworkProject.createFileStructure(projectDir);
            main.log("Project structure created: "+projectDir.getAbsolutePath());
        } catch (IOException e) {
            main.error(e.getMessage());
            throw new ProjectToolException("failed creating project structure", e);
        }
        main.verbose("initializing project: " + project);
        final FrameworkProject d = framework.getFrameworkProjectMgr().createFrameworkProject(
            project, properties);
        if (!d.getBaseDir().exists() && !d.getBaseDir().mkdir()) {
            throw new ProjectToolException("Failed to create project dir: " + d.getBaseDir());
        }
    }


}
