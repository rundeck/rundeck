/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
* FrameworkSingleProjectResolver.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/12/11 3:52 PM
* 
*/
package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;

import java.util.*;

/**
 * FrameworkSingleProjectResolver resolves the single project using a framework instance.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class FrameworkSingleProjectResolver implements SingleProjectResolver {
    private Framework framework;

    public FrameworkSingleProjectResolver(final Framework framework) {
        this.framework = framework;
    }

    public boolean hasSingleProject() {
        return framework.getFrameworkProjectMgr().listFrameworkProjects().size() == 1;
    }

    public String getSingleProjectName() {
        final FrameworkProject project =
            (FrameworkProject) framework.getFrameworkProjectMgr().listFrameworkProjects().iterator().next();
        return project.getName();
    }
}
