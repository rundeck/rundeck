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
* ProjectGenericLoggerImpl.java
* 
* User: greg
* Created: Jun 16, 2008 3:52:11 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import org.apache.tools.ant.Project;

import com.dtolabs.rundeck.core.cli.CLIToolLogger;


/**
 * ProjectGenericLoggerImpl is a wrapper for {@link Project} to implement the {@link CLIToolLogger} interface.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ProjectGenericLoggerImpl implements CLIToolLogger {
    private final Project project;

    /**
     * Create a ProjectGenericLoggerImpl for the project.  calls to the various logging messages will be converted
     * to {@link Project#log(String, int)} of the appropriate message level.
     * @param project
     */
    public ProjectGenericLoggerImpl(Project project) {
        this.project = project;
    }

    /**
     * Log to the project at default message level.
     * @param message
     */
    public void log(String message) {
        project.log(message);
    }

    public void error(String message) {
        project.log(message,Project.MSG_ERR);
    }

    public void warn(String message) {
        project.log(message, Project.MSG_WARN);
    }

    public void verbose(String message) {
        project.log(message, Project.MSG_VERBOSE);
    }
}
