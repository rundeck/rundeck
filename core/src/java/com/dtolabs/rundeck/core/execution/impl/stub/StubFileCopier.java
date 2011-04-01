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
package com.dtolabs.rundeck.core.execution.impl.stub;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.plugins.Plugin;

import java.io.File;
import java.io.InputStream;

/**
 * StubFileCopier stub provider for the FileCopier service
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "stub",service = "FileCopier")
public class StubFileCopier implements FileCopier {
    public String copyFileStream(final ExecutionContext context, final InputStream input, final INodeEntry node) throws
        FileCopierException {

        final String resultpath = BaseFileCopier.generateRemoteFilepathForNode(node, "stub-script");
        context.getExecutionListener().log(Constants.WARN_LEVEL,
            "[stub] copy inputstream to node " + node.getNodename() + ": " + resultpath);
        return resultpath;
    }

    public String copyFile(final ExecutionContext context, final File file, final INodeEntry node) throws
        FileCopierException {

        final String resultpath = BaseFileCopier.generateRemoteFilepathForNode(node, file.getName());
        context.getExecutionListener().log(Constants.WARN_LEVEL,
            "[stub] copy local file to node " + node.getNodename() + ": " + resultpath);
        return resultpath;
    }

    public String copyScriptContent(final ExecutionContext context, final String script, final INodeEntry node) throws
        FileCopierException {

        final String resultpath = BaseFileCopier.generateRemoteFilepathForNode(node, "stub-script");
        final int linecount = script != null ? script.split("(\\r?\\n)").length : 0;
        context.getExecutionListener().log(Constants.WARN_LEVEL,
            "[stub] copy [" + linecount + " lines] to node " + node.getNodename() + ": " + resultpath);
        return resultpath;
    }
}
