/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.dtolabs.rundeck.core.data.BaseDataContext;
import com.dtolabs.rundeck.core.data.DataContext;

import java.io.File;
import java.util.Map;

/**
 * ScriptDataContextUtil is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptDataContextUtil {

    private static File getVarDirForProject(final Framework framework, final String projectName) {
        return new File(
                Constants.getBaseVar(
                        new File(
                                framework.getFilesystemFramework().getFrameworkProjectsBaseDir(),
                                projectName
                        ).getAbsolutePath()
                )
        );
    }

    /**
     * @return a data context for executing a script plugin or provider, which contains two datasets:
     * plugin: {vardir: [dir], tmpdir: [dir]}
     * and
     * rundeck: {base: [basedir]}
     * @param framework framework
     *
     */
    public static DataContext createScriptDataContext(final Framework framework) {
        BaseDataContext data = new BaseDataContext();

        final File vardir = new File(Constants.getBaseVar(framework.getFilesystemFramework().getBaseDir().getAbsolutePath()));
        final File tmpdir = new File(vardir, "tmp");
        data.group("plugin").put("vardir", vardir.getAbsolutePath());
        data.group("plugin").put("tmpdir", tmpdir.getAbsolutePath());
        data.put("rundeck", "base", framework.getFilesystemFramework().getBaseDir().getAbsolutePath());

        return data;
    }

    /**
     * @return Create a data context for executing a script plugin or provider, for a project context. Extends the context
     * provided by {@link #createScriptDataContext(com.dtolabs.rundeck.core.common.Framework)} by setting the plugin.vardir to be
     * a dir specific to the project's basedir.
     * @param framework fwk
     * @param projectName project
     */
    public static Map<String, Map<String, String>> createScriptDataContextForProject(
            final Framework framework,
            final String projectName
    )
    {
        return createScriptDataContextObjectForProject(framework, projectName);
    }

    /**
     * @param framework   fwk
     * @param projectName project
     *
     * @return Create a data context for executing a script plugin or provider, for a project context. Extends the
     * context
     * provided by {@link #createScriptDataContext(com.dtolabs.rundeck.core.common.Framework)} by setting the
     * plugin.vardir to be
     * a dir specific to the project's basedir.
     */
    public static DataContext createScriptDataContextObjectForProject(
            final Framework framework,
            final String projectName
    )
    {
        BaseDataContext data = new BaseDataContext();

        //add script-plugin context data
        data.merge(createScriptDataContext(framework));

        //reset vardir to point to project-specific dir.
        data.put("plugin", "vardir", getVarDirForProject(framework, projectName).getAbsolutePath());
        //add project context to rundeck dataset
        data.put("rundeck", "project", projectName);

        return data;
    }
}
