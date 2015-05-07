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
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcher;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Category;


/**
 * archives and removes a project from the framework
 */
public class RemoveAction extends BaseAction {
    static Category logger = Category.getInstance(CreateAction.class.getName());

    final private StringBuffer unsetupArgs = new StringBuffer();

    /**
     * Create a new RemoveAction and parse the arguments from a {@link org.apache.commons.cli.CommandLine}
     *
     * @param main logger
     * @param framework framework
     * @param cli cli
     */
    public RemoveAction(final CLIToolLogger main, final IPropertyLookup framework, final CommandLine cli, final CentralDispatcher dispatcher) {
        this(main, framework, BaseAction.parseBaseActionArgs(cli), dispatcher);
    }

    /**
     * Create a new RemoveAction with argument specifiers
     *
     * @param main logger
     * @param framework framework object
     * @param baseArgs  base args
     */
    public RemoveAction(final CLIToolLogger main,
                        final IPropertyLookup framework,
                        final BaseActionArgs baseArgs,
                        final CentralDispatcher dispatcher) {
        super(main, framework, baseArgs,dispatcher);
        initArgs();
    }


    private void initArgs() {
        unsetupArgs.append("-name ");
        unsetupArgs.append(project);
    }

    /**
     * Execute the action.
     *
     * @throws Throwable unimplemented
     */
    public void exec() throws Throwable {
//        super.exec();
//        if (project == null) {
//            throw new IllegalStateException("project was null");
//        }
//        if (!framework.getFrameworkProjectMgr().existsFrameworkProject(project.getFrameworkProject())) {
//            throw new ProjectToolException("project does not exists: " + project.getFrameworkProject());
//        }
//        main.verbose("removing project: " + project.getFrameworkProject());
//        final FrameworkProject d = framework.getFrameworkProjectMgr().createFrameworkProject(project.getFrameworkProject());

        throw new RuntimeException("unimplemented: RemoveAction.exec");
    }

}
