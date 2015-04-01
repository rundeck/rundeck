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
* StubFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 4:09 PM
* 
*/
package com.dtolabs.rundeck.plugin.stub;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.AbstractBaseDescription;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * StubFileCopier stub provider for the FileCopier service
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "stub",service = "FileCopier")
public class StubFileCopier implements FileCopier, Describable {
    public static final String SERVICE_PROVIDER_NAME = "stub";
    public String copyFileStream(final ExecutionContext context, final InputStream input, final INodeEntry node) throws
        FileCopierException {


        String identity = null!=context.getDataContext() && null!=context.getDataContext().get("job")?
                          context.getDataContext().get("job").get("execid"):null;
        final String resultpath = BaseFileCopier.generateRemoteFilepathForNode(
                node,
                context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                context.getFramework(),
                "stub-script",
                null,
                identity
        );
        context.getExecutionListener().log(Constants.WARN_LEVEL,
            "[stub] copy inputstream to node " + node.getNodename() + ": " + resultpath);
        return resultpath;
    }

    public String copyFile(final ExecutionContext context, final File file, final INodeEntry node) throws
        FileCopierException {

        String identity = null!=context.getDataContext() && null!=context.getDataContext().get("job")?
                          context.getDataContext().get("job").get("execid"):null;
        final String resultpath = BaseFileCopier.generateRemoteFilepathForNode(
                node,
                context.getFramework().getFrameworkProjectMgr().getFrameworkProject(context.getFrameworkProject()),
                context.getFramework(),
                file.getName(),
                null,
                identity
        );
        context.getExecutionListener().log(Constants.WARN_LEVEL,
            "[stub] copy local file to node " + node.getNodename() + ": " + resultpath);
        return resultpath;
    }

    public String copyScriptContent(final ExecutionContext context, final String script, final INodeEntry node) throws
        FileCopierException {

        String identity = null!=context.getDataContext() && null!=context.getDataContext().get("job")?
                          context.getDataContext().get("job").get("execid"):null;
        final String resultpath = BaseFileCopier.generateRemoteFilepathForNode(
                node,
                context.getFramework().getFrameworkProjectMgr().getFrameworkProject( context.getFrameworkProject()),
                context.getFramework(),
                "stub-script",
                null,
                identity
        );
        final int linecount = script != null ? script.split("(\\r?\\n)").length : 0;
        context.getExecutionListener().log(Constants.WARN_LEVEL,
            "[stub] copy [" + linecount + " lines] to node " + node.getNodename() + ": " + resultpath);
        return resultpath;
    }

    final static Description DESC = new AbstractBaseDescription(){
        public String getName() {
            return SERVICE_PROVIDER_NAME;
        }

        public String getTitle() {
            return "Stub";
        }

        public String getDescription() {
            return "Prints information about file copy request instead of copying it. (Useful for mocking processes.)";
        }
    };

    public Description getDescription() {
        return DESC;
    }
}
