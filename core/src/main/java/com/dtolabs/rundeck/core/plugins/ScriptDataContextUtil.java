/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
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
* ScriptDataContextUtil.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 5/16/12 9:30 AM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;

import java.io.File;
import java.util.*;

/**
 * ScriptDataContextUtil is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptDataContextUtil {

    private static File getVarDirForProject(final Framework framework, final String projectName) {
        final FrameworkProject frameworkProject = framework.getFrameworkProjectMgr().getFrameworkProject(
            projectName);
        return new File(Constants.getBaseVar(frameworkProject.getBaseDir().getAbsolutePath()));
    }

    /**
     * @return a data context for executing a script plugin or provider, which contains two datasets:
     * plugin: {vardir: [dir], tmpdir: [dir]}
     * and
     * rundeck: {base: [basedir]}
     * @param framework framework
     *
     */
    public static Map<String, Map<String, String>> createScriptDataContext(final Framework framework) {
        final Map<String, String> rundeckDataContext = new HashMap<String, String>();
        final Map<String, String> pluginDataContext = new HashMap<String, String>();

        rundeckDataContext.put("base", framework.getBaseDir().getAbsolutePath());

        final File vardir = new File(Constants.getBaseVar(framework.getBaseDir().getAbsolutePath()));
        final File tmpdir = new File(vardir, "tmp");
        pluginDataContext.put("vardir", vardir.getAbsolutePath());
        pluginDataContext.put("tmpdir", tmpdir.getAbsolutePath());

        final Map<String, Map<String, String>> scriptPluginDataContext = new HashMap<String, Map<String, String>>();

        scriptPluginDataContext.put("plugin", pluginDataContext);
        scriptPluginDataContext.put("rundeck", rundeckDataContext);

        return scriptPluginDataContext;
    }

    /**
     * @return Create a data context for executing a script plugin or provider, for a project context. Extends the context
     * provided by {@link #createScriptDataContext(com.dtolabs.rundeck.core.common.Framework)} by setting the plugin.vardir to be
     * a dir specific to the project's basedir.
     * @param framework fwk
     * @param projectName project
     */
    public static Map<String, Map<String, String>> createScriptDataContextForProject(final Framework framework,
                                                                                     final String projectName) {
        final Map<String, Map<String, String>> localDataContext = new HashMap<String, Map<String, String>>();

        //add script-plugin context data
        final Map<String, Map<String, String>> scriptDataContext = createScriptDataContext(framework);
        localDataContext.putAll(scriptDataContext);

        //reset vardir to point to project-specific dir.
        final Map<String, String> plugin1 = new HashMap<String, String>(scriptDataContext.get("plugin"));

        plugin1.put("vardir", getVarDirForProject(framework, projectName).getAbsolutePath());

        localDataContext.put("plugin", plugin1);

        //add project context to rundeck dataset
        final Map<String, String> rundeck = new HashMap<String, String>(scriptDataContext.get("rundeck"));

        rundeck.put("project", projectName);

        localDataContext.put("rundeck", rundeck);

        return localDataContext;
    }
}
